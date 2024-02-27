package personthecat.roadmap.gen;

import personthecat.roadmap.Config;
import personthecat.roadmap.Tracker;
import personthecat.roadmap.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class RoadGenerator {
  protected static final int SCAN_RADIUS = 16;
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
    int minWeight = Integer.MAX_VALUE;
    int bestX = Integer.MAX_VALUE;
    int bestY = Integer.MAX_VALUE;
    for (int xO = point.x - SCAN_RADIUS; xO < point.x + SCAN_RADIUS; xO += SCAN_STEP) {
      for (int yO = point.y - SCAN_RADIUS; yO < point.y + SCAN_RADIUS; yO += SCAN_STEP) {
        final int weight = this.getWeight(gen, xO, yO);
        if (weight == Integer.MIN_VALUE) {
          return new Point(xO, yO);
        } else if (weight < minWeight) {
          minWeight = weight;
          bestX = xO;
          bestY = yO;
        }
      }
    }
    if (minWeight != Integer.MAX_VALUE) {
      return new Point(bestX, bestY);
    }
    return null;
  }

  protected int getWeight(final HeightmapGenerator gen, final int aX, final int aY) {
    final int min = this.config.getMinRoadStart();
    final int max = this.config.getMaxRoadStart();
    final int target = (min + max) / 2;
    final float n = gen.sample(aX, aY);
    if (n < min || n > max) {
      return Integer.MAX_VALUE;
    } else if (n == target) {
      return Integer.MIN_VALUE;
    }
    return (int) Math.abs(n - target);
  }

  protected void generateSystem(
      final List<Road> roads, final HeightmapGenerator gen, final Point src, final Random rand, final int x, final int y) {
    final Point dest = this.getDest(gen, src, rand);
    if (dest != null) {
      final Road road = this.trace(gen, src, dest, x, y);
      if (road != null && road.isInRegionBounds()) {
//        System.out.println("generated road: " + src + " -> " + dest);
        roads.add(road);
      }
    }
  }

  protected Point getDest(final HeightmapGenerator gen, final Point origin, final Random rand) {
    final float a = this.getAngle(gen, origin, rand);
    final int d = this.getDistance(gen, origin, rand);
    final int x = (int) (origin.x + d * Math.cos(a));
    final int y = (int) (origin.y + d * Math.sin(a));
    return getNearestSuitable(gen, new Point(x, y));
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
