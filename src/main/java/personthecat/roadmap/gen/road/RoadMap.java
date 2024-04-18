package personthecat.roadmap.gen.road;

import personthecat.roadmap.Config;
import personthecat.roadmap.data.Tracker;
import personthecat.roadmap.gen.HeightmapGenerator;
import personthecat.roadmap.data.Point;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoadMap {
  private static final ExecutorService BACKGROUND = Executors.newSingleThreadExecutor();
  private static final int CACHE_SIZE = 16;
  private final RoadRegion[] regionCache = new RoadRegion[CACHE_SIZE];
  private final ReferenceQueue<RoadNetwork> networkReferences = new ReferenceQueue<>();
  private final Map<Point, WeakReference<RoadNetwork>> networks = new HashMap<>();
  private final RoadGenerator generator;
  private final Config config;
  private final Tracker tracker;
  private int seed;

  public RoadMap(final Config config, final Tracker tracker) {
    this.config = config;
    this.tracker = tracker;
    this.generator = this.newGenerator();
    this.seed = tracker.getSeed();
  }

  public void pregen(final HeightmapGenerator mapGen, final short x, final short y) {
    Pregenerator.create(this.config, this).run(mapGen, x, y);
  }

  public RoadGenerator newGenerator() {
    return new AStarRoadGenerator(this, this.config, this.tracker);
  }

  public RoadRegion getRegion(final HeightmapGenerator mapGen, final short x, final short y) {
    if (this.seed != this.tracker.getSeed()) {
      Arrays.fill(this.regionCache, null);
      this.seed = this.tracker.getSeed();
    }
    final RoadRegion r = this.loadPartial(x, y);
    if (!r.isFullyGenerated()) {
      this.generateRegion(r, mapGen);
    }
    this.cleanupNetworkCache();
    return this.cacheRegion(r);
  }

  public RoadRegion loadPartial(final short x, final short y) {
    RoadRegion r = this.getCachedRegion(x, y);
    if (r != null) return r;
    r = this.loadRegionFromDisk(x, y);
    if (r != null) return r;
    return new RoadRegion(x, y);
  }

  private RoadRegion getCachedRegion(final short x, final short y) {
    for (int i = 0; i < this.regionCache.length; i++) {
      final RoadRegion r = this.regionCache[i];
      if (r != null && r.x == x && r.y == y) {
        if (i > 0) {
          final RoadRegion up = this.regionCache[i - 1];
          this.regionCache[i] = up;
          this.regionCache[i - 1] = r;
        }
        return r;
      }
    }
    return null;
  }

  public RoadRegion loadRegionFromDisk(final short x, final short y) {
    return RoadRegion.loadFromDisk(this, this.tracker.getSeed(), x, y);
  }

  public void generateRegion(final HeightmapGenerator mapGen, final short x, final short y) {
    this.generateRegion(new RoadRegion(x, y), this.generator, mapGen, this.config.isGeneratePartial());
  }

  public void generateRegion(
      RoadGenerator gen, HeightmapGenerator mapGen, short x, short y, final boolean partial) {
    this.generateRegion(new RoadRegion(x, y), gen, mapGen, partial);
  }

  protected void generateRegion(RoadRegion region, HeightmapGenerator mapGen) {
    this.generateRegion(region, this.generator, mapGen, this.config.isGeneratePartial());
  }

  protected void generateRegion(
      RoadRegion region, RoadGenerator gen, HeightmapGenerator mapGen, boolean partial) {
    final Map<Point, RoadRegion> generated = gen.generateRegion(region, mapGen, region.x, region.y, partial);
    for (final RoadRegion r : generated.values()) {
      if (r != region) {
        this.cacheRegion(r);
      }
    }
    if (this.config.isPersistRoads()) {
      final int seed = this.seed;
      this.runInBackground(() -> {
        region.saveToDisk(seed);
        region.forEach(n -> n.saveToDisk(seed));
      });
    }
  }

  public void runInBackground(final Runnable task) {
    BACKGROUND.submit(task);
  }

  private RoadRegion cacheRegion(final RoadRegion r) {
    for (int i = 0; i < this.regionCache.length; i++) {
      final RoadRegion cached = this.regionCache[i];
      if (cached == r) {
        if (i > 0) {
          final RoadRegion up = this.regionCache[i - 1];
          this.regionCache[i] = up;
          this.regionCache[i - 1] = r;
        }
        return r;
      }
    }
    this.pushToCache(r);
    return r;
  }

  private void pushToCache(final RoadRegion r) {
    System.arraycopy(this.regionCache, 0, this.regionCache, 1, this.regionCache.length - 1);
    this.regionCache[0] = r;
  }

  public void clearCache() {
    Arrays.fill(this.regionCache, null);
    this.networks.clear();
    System.out.println("Cleaned region cache for seed: " + this.seed);
  }

  public void addNetwork(final int x, final int y, final RoadNetwork n) {
    this.networks.put(new Point(x, y), new WeakReference<>(n, this.networkReferences));
  }

  public RoadNetwork getNetwork(final int x, final int y) {
    RoadNetwork n = this.lookupNetwork(x, y);
    if (n != null) return n;
    n = this.loadNetworkFromDisk(x, y);
    if (n != null) {
      this.addNetwork(x, y, n);
    }
    return n;
  }

  protected RoadNetwork lookupNetwork(final int x, final int y) {
    final WeakReference<RoadNetwork> ref = this.networks.get(new Point(x, y));
    return ref != null ? ref.get() : null;
  }

  public RoadNetwork loadNetworkFromDisk(final int x, final int y) {
    return RoadNetwork.loadFromDisk(this.tracker.getSeed(), x, y);
  }

  public void cleanupNetworkCache() {
    WeakReference<?> ref;
    while ((ref = (WeakReference<?>) this.networkReferences.poll()) != null) {
      this.networks.values().remove(ref);
    }
  }
}
