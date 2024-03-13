package personthecat.roadmap.gen;

import personthecat.roadmap.Config;
import personthecat.roadmap.Tracker;

import java.util.Arrays;

public class RoadMap {
  private static final int CACHE_SIZE = 12; // will use 4 for MC
  private static final int PREGEN_RADIUS = 2; // diameter = 5
  private final RoadRegion[] regionCache = new RoadRegion[CACHE_SIZE];
  private final RoadGenerator generator;
  private final Config config;
  private final Tracker tracker;
  private int seed;

  public RoadMap(final Config config, final Tracker tracker) {
    this.generator = new AStarRoadGenerator(config, tracker);
    this.config = config;
    this.tracker = tracker;
    this.seed = tracker.getSeed();
  }

  public void pregen(final HeightmapGenerator mapGen, final short x, final short y) {
    System.out.println("pre-generating roads...");
    for (int rX = x - PREGEN_RADIUS; rX <= x + PREGEN_RADIUS; rX++) {
      for (int rY = y - PREGEN_RADIUS; rY <= y + PREGEN_RADIUS; rY++) {
        this.getRegion(mapGen, (short) rX, (short) rY);
      }
    }
    final int n = (PREGEN_RADIUS * 2 + 1) * (PREGEN_RADIUS * 2 + 1);
    final int rB = n * RoadRegion.LEN;
    final int rC = n * RoadRegion.CHUNK_LEN;
    System.out.printf("pre-generated %s road regions (%s^2 blocks, %s^2 chunks)\n", n, rB, rC);
  }

  public RoadRegion getRegion(final HeightmapGenerator mapGen, final short x, final short y) {
    RoadRegion r = null;
    if (this.seed == this.tracker.getSeed()) {
      r = this.getCachedRegion(x, y);
    } else {
      Arrays.fill(this.regionCache, null);
      this.seed = this.tracker.getSeed();
    }
    if (r != null) return r;
    r = this.loadRegionFromDisk(x, y);
    if (r != null) return this.cacheRegion(r);
    return this.cacheRegion(this.generateRegion(mapGen, x, y));
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

  private RoadRegion loadRegionFromDisk(final short x, final short y) {
    return RoadRegion.loadFromDisk(this.tracker.getSeed(), x, y);
  }

  private RoadRegion generateRegion(final HeightmapGenerator mapGen, final short x, final short y) {
    final Road[] map = this.generator.generateMap(mapGen, x, y).toArray(new Road[0]);
    final RoadRegion region = new RoadRegion(x, y, map);
    if (this.config.isPersistRoads()) {
      region.saveToDisk(this.seed);
    }
    return region;
  }

  private RoadRegion cacheRegion(final RoadRegion r) {
    return this.regionCache[this.regionCache.length - 1] = r;
  }

  public void clearCache() {
    Arrays.fill(this.regionCache, null);
    System.out.println("Cleaned region cache for seed: " + this.seed);
  }
}
