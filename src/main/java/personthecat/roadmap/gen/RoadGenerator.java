package personthecat.roadmap.gen;

import personthecat.roadmap.Config;
import personthecat.roadmap.Tracker;
import personthecat.roadmap.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class RoadGenerator {
  protected static final int SCAN_RADIUS = 32;
  protected static final int SCAN_STEP = Road.STEP;
  protected static final int SCAN_PAD = (Road.MAX_DISTANCE - SCAN_RADIUS) >> 4;
  protected static final float TAU = (float) (Math.PI * 2);

  protected final Config config;
  protected final Tracker tracker;

  protected RoadGenerator(
      final Config config, final Tracker tracker) {
    this.config = config;
    this.tracker = tracker;
  }

  public List<Road> generateMap(final HeightmapGenerator gen, final int x, final int y) {
    System.out.println("Generating road map " + x + "," + y);
    final List<Road> roads = new ArrayList<>();
    final int cXO = RoadRegion.toChunkCoord(x);
    final int cYO = RoadRegion.toChunkCoord(y);
    final long seed = this.tracker.getSeed();
    final Random rand = new Random(seed);
    final float chance = this.config.getRoadChance();
    // simulate generating in chunks
    for (int cX = cXO - SCAN_PAD; cX < cXO + RoadRegion.CHUNK_LEN + SCAN_PAD; cX++) {
      for (int cY = cYO - SCAN_PAD; cY < cYO + RoadRegion.CHUNK_LEN + SCAN_PAD; cY++) {
        Utils.setFeatureSeed(rand, seed, cX, cY);
        if (rand.nextFloat() <= chance) {
          final Point center = new Point((cX << 4) + 8, (cY << 4) + 8);
          final Point nearest = this.getNearestSuitable(gen, center);
          if (nearest != null) {
            this.generateSystem(roads, gen, nearest, rand, x, y);
          }
        }
      }
    }
    System.out.println("map done " + x + "," + y);
    return roads;
  }

  protected Point getNearestSuitable(final HeightmapGenerator gen, final Point point) {
    // avoid oceans (in game, can check continental-ness)
    if (gen.sample(point.x, point.y) < 0) {
      return null;
    }
    double minWeight = Double.MAX_VALUE;
    int bestX = Integer.MAX_VALUE;
    int bestY = Integer.MAX_VALUE;
    for (int xO = point.x - SCAN_RADIUS; xO < point.x + SCAN_RADIUS; xO += SCAN_STEP) {
      for (int yO = point.y - SCAN_RADIUS; yO < point.y + SCAN_RADIUS; yO += SCAN_STEP) {
        final double weight = this.getWeight(gen, xO, yO);
        if (weight < 0) {
          return new Point(xO, yO);
        } else if (weight < minWeight) {
          minWeight = weight;
          bestX = xO;
          bestY = yO;
        }
      }
    }
    if (minWeight != Double.MAX_VALUE) {
      return new Point(bestX, bestY);
    }
    return null;
  }

  protected double getWeight(final HeightmapGenerator gen, final int aX, final int aY) {
    final float h = gen.sample(aX, aY);
    if (h < 0) {
      return Double.MAX_VALUE;
    }
    final int maxCutoff = this.config.getMountainCutoff();
    if (h > maxCutoff + 20) {
      return Double.MAX_VALUE;
    }
    final int minCutoff = this.config.getShorelineCutoff();
    double w = Utils.stdDev(this.getSamples(gen, aX, aY, h)) * 100.0;
    if (h < minCutoff) {
      w += (minCutoff - h) * (minCutoff - h);
    } else if (h > maxCutoff) {
      w += (h - maxCutoff) * (h - maxCutoff);
    }
    return w;
  }

  protected double[] getSamples(final HeightmapGenerator gen, final int x, final int y, final float c) {
    return new double[] {
      c,
      gen.sample(x + 2, y),
      gen.sample(x, y + 2),
      gen.sample(x - 2, y),
      gen.sample(x, y - 2),
    };
  }

  protected void generateSystem(
      final List<Road> roads, final HeightmapGenerator gen, final Point src, final Random rand, final int x, final int y) {
    final Point dest = this.getDest(roads, gen, src, rand, x, y);
    if (dest != null) {
      final Road road = this.trace(gen, src, dest, x, y);
      if (road != null && road.isInRegionBounds()) {
//        System.out.println("generated road: " + src + " -> " + dest);
        roads.add(road);
      }
    }
  }

  protected Point getDest(
      final List<Road> roads, final HeightmapGenerator gen, final Point origin, final Random rand, final int x, final int y) {
    final float a = this.getAngle(gen, origin, rand);
    final int d = this.getDistance(gen, origin, rand);
    final int aX = (int) (origin.x + d * Math.cos(a));
    final int aY = (int) (origin.y + d * Math.sin(a));
    final Point dest = new Point(aX, aY);
    if (this.isTooClose(roads, origin, dest, x, y)) {
      return null;
    }
    return getNearestSuitable(gen, dest);
  }

  protected boolean isTooClose(final List<Road> roads, final Point src, final Point dest, final int x, final int y) {
    final int aX = RoadRegion.getAbsoluteCoord(x);
    final int aY = RoadRegion.getAbsoluteCoord(y);
    final int cX1 = (src.x + dest.x) / 2;
    final int cY1 = (src.y + dest.y) / 2;

    for (final Road road : roads) {
      final int cX2 = (aX + road.minX() + aX + road.maxX()) / 2;
      final int cY2 = (aY + road.minY() + aY + road.maxY()) / 2;
      final int d = (int) Math.sqrt((cX1 - cX2) * (cX1 - cX2) + (cY1 - cY2) * (cY1 - cY2));
      if (d < road.distance() * 2 / 6) { // too close if new road intersects with the center 2/3 of another
        return true;
      }
    }
    return false;
  }

  protected float getAngle(final HeightmapGenerator gen, final Point origin, final Random rand) {
    // for now: random direction. in the future, find best of 8 directions at distance of SCAN_RADIUS
    return rand.nextFloat() * TAU;
  }

  protected int getDistance(final HeightmapGenerator gen, final Point origin, final Random rand) {
    final int min = this.config.getMinRoadLength();
    final int max = this.config.getMaxRoadLength();
    return min + rand.nextInt(max - min);
  }

  protected abstract Road trace(final HeightmapGenerator gen, final Point src, final Point dest, final int x, final int y);
}
