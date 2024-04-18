package personthecat.roadmap.gen.road;

import personthecat.roadmap.Config;
import personthecat.roadmap.util.Stopwatch;
import personthecat.roadmap.util.Utils;
import personthecat.roadmap.data.Point;
import personthecat.roadmap.gen.HeightmapGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Pregenerator {
  protected final Config config;
  protected final RoadMap map;
  protected final Stopwatch sw = new Stopwatch();
  protected final boolean[][] shape;

  protected Pregenerator(final Config config, final RoadMap map) {
    this.config = config;
    this.map = map;
    if (config.isDebugPregenShape()) {
      final int r = config.getPregenRadius();
      final int d = r * 2 + 1;
      this.shape = new boolean[d][d];
    } else {
      this.shape = null;
    }
  }

  static Pregenerator create(final Config config, final RoadMap map) {
    if (config.getPregenThreadCount() < 2) {
      return new SingleThreaded(config, map);
    }
    return new Multithreaded(config, map);
  }

  public final void run(final HeightmapGenerator mapGen, final short x, final short y) {
    this.sw.logStart("pre-generating roads...");
    this.generateRegions(mapGen, x, y);
  }

  protected abstract void generateRegions(final HeightmapGenerator mapGen, final short x, final short y);

  protected List<Point> getSortedOrigins(final short x, final short y) {
    final List<Point> origins = new ArrayList<>();
    final int r = this.config.getPregenRadius();
    final int l = r * 2 + 1;
    final float s = this.config.getPregenSkew();
    final float a = s * (float) Math.sqrt(r);

    // generates a plus-shaped pattern to optimize number of regions generated
    for (int rX = x - r; rX <= x + r; rX++) {
      final int xO = rX - x;
      final int u = (int) -(Math.pow((1 / a) * xO, 2)) + r;
      final int d = -u;
      final int l1 = (int) (a * Math.sqrt(xO + r));
      final int l2 = -l1;
      final int r1 = (int) (a * Math.sqrt(-xO + r));
      final int r2 = -r1;
      for (int rY = y - r; rY <= y + r; rY++) {
        final int yO = rY - y;
        if ((yO <= u && yO >= d) || (xO < 0 ? (yO >= l2 && yO <= l1) : (yO >= r2 && yO <= r1))) {
          origins.add(new Point(rX, rY));
          this.addToShape(l - (yO + r) -1, xO + r);
        }
      }
    }
    // sort by distance from x, y
    origins.sort(Comparator.comparingDouble(p -> Utils.distance(p.x, p.y, x, y)));
    return origins;
  }

  protected void addToShape(final int x, final int y) {
    if (this.shape != null) {
      this.shape[y][x] = true;
    }
  }

  protected void printDiagnostics(final int count, final int max) {
    final int r = this.config.getPregenRadius();
    final int d = r * 2 + 1;
    final int dB = d * RoadRegion.LEN;
    final int dC = d * RoadRegion.CHUNK_LEN;
    this.printShape();
    this.sw.logEnd("pre-generated %s / %s regions = %s^2r = %s^2b = %s^2c", count, max, d, dB, dC);
  }

  protected void printShape() {
    if (this.shape != null) {
      final StringBuilder sb = new StringBuilder("\nShape generated:\n");
      final String border = "+-" + "-".repeat(this.shape.length * 2) + "+\n";
      sb.append(border);
      for (final boolean[] row : this.shape) {
        sb.append("| ");
        for (final boolean b : row) {
          sb.append(b ? '#' : ' ').append(' ');
        }
        sb.append("|\n");
      }
      sb.append(border);
      System.out.println(sb);
    }
  }

  public static class SingleThreaded extends Pregenerator {

    protected SingleThreaded(final Config config, final RoadMap map) {
      super(config, map);
    }

    @Override
    protected void generateRegions(final HeightmapGenerator mapGen, final short x, final short y) {
      final List<Point> origins = this.getSortedOrigins(x, y);
      int count = 0;
      for (final Point o : origins) {
        if (this.map.loadRegionFromDisk((short) o.x, (short) o.y) == null) {
          this.map.generateRegion(mapGen, (short) o.x, (short) o.y);
          count++;
        }
      }
      this.printDiagnostics(count, origins.size());
    }
  }

  public static class Multithreaded extends Pregenerator {
    protected Multithreaded(final Config config, final RoadMap map) {
      super(config, map);
    }

    @Override
    protected void generateRegions(final HeightmapGenerator mapGen, final short x, final short y) {
      final ExecutorService executor = Executors.newFixedThreadPool(this.config.getPregenThreadCount());
      final ThreadLocal<RoadGenerator> generators = ThreadLocal.withInitial(this.map::newGenerator);
      final AtomicInteger count = new AtomicInteger(0);

      final List<Future<Void>> futures = this.getSortedOrigins(x, y).stream()
          .map(o -> executor.<Void>submit(() -> {
            if (this.map.loadRegionFromDisk((short) o.x, (short) o.y) == null) {
              this.map.generateRegion(generators.get(), mapGen, (short) o.x, (short) o.y, false);
              count.incrementAndGet();
            }
          }, null))
          .toList();

      this.map.runInBackground(() -> this.awaitCompletion(executor, futures, count));
    }

    private void awaitCompletion(
        final ExecutorService executor, final List<Future<Void>> futures, final AtomicInteger count) {
      for (final Future<?> f : futures) {
        try {
          f.get();
        } catch (final ExecutionException | InterruptedException e) {
          System.err.println("Error generating region: " + e);
          e.printStackTrace();
        }
      }
      this.printDiagnostics(count.get(), futures.size());
      executor.shutdown();
    }
  }
}
