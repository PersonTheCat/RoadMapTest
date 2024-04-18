package personthecat.roadmap.gen.road;

import personthecat.roadmap.Config;
import personthecat.roadmap.util.Stopwatch;
import personthecat.roadmap.data.Tracker;
import personthecat.roadmap.util.Utils;
import personthecat.roadmap.gen.HeightmapGenerator;
import personthecat.roadmap.data.Point;
import personthecat.roadmap.data.SmoothnessGraph;
import personthecat.roadmap.data.VertexGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class RoadGenerator {
  protected static final short QUAD_RADIUS = 2;
  protected static final int SCAN_RADIUS = 32;
  protected static final int SCAN_STEP = Road.STEP;
  protected static final float TAU = (float) (Math.PI * 2);

  protected final RoadMap map;
  protected final Config config;
  protected final Tracker tracker;
  protected final SmoothnessGraph graph;
  protected final Stopwatch sw = new Stopwatch();

  protected RoadGenerator(final RoadMap map, final Config config, final Tracker tracker) {
    this.map = map;
    this.config = config;
    this.tracker = tracker;
    this.graph = new SmoothnessGraph();
  }

  public final Map<Point, RoadRegion> generateRegion(
      RoadRegion region, HeightmapGenerator gen, short x, short y, boolean partial) {
    this.sw.logStart("Generating road map %s, %s", x, y);
    final Map<Point, RoadRegion> generated = this.generate(region, gen, x, y, partial);
    this.sw.logEnd("map done %s, %s", x, y);
    this.sw.logAverage("average");
    this.graph.clear();
    return generated;
  }

  protected Map<Point, RoadRegion> generate(
      RoadRegion region, HeightmapGenerator gen, short x, short y, boolean partial) {
    final Map<Point, RoadRegion> generated = new HashMap<>();
    final short cQX = (short) ((x * 2) + 1);
    final short cQY = (short) ((y * 2) + 1);
    generated.put(new Point(x, y), region);

    for (short qX = (short) (cQX - QUAD_RADIUS); qX < cQX + QUAD_RADIUS; qX++) {
      for (short qY = (short) (cQY - QUAD_RADIUS); qY < cQY + QUAD_RADIUS; qY++) {
        final short xO = (short) (qX / 2);
        final short yO = (short) (qY / 2);
        // in current region
        if (xO == x && yO == y) {
          if (!region.hasQuad(qX, qY)) {
            this.generateQuad(region, region, gen, qX, qY, x, y, partial);
            region.setQuadGenerated(qX, qY);
          }
          continue;
        }
        final Point pO = new Point(xO, yO);
        final RoadRegion rO = generated.computeIfAbsent(pO, p -> this.map.loadPartial((short) p.x, (short) p.y));
        if (rO.hasQuad(qX, qY)) {
          rO.copyQuadInto(region, qX, qY);
        } else {
          this.generateQuad(region, rO, gen, qX, qY, x, y, partial);
          rO.setQuadGenerated(qX, qY);
        }
        generated.put(pO, rO);
      }
    }
    // Previously kept all networks to avoid some overlap. Can safely remove them now.
    region.getData().removeIf(n -> !n.isInRegion(x, y));
    return generated;
  }

  protected void generateQuad(
      RoadRegion region, RoadRegion rO, HeightmapGenerator gen, short qX, short qY, short pX, short pY, boolean partial) {
    final int cXO = RoadRegion.quadToChunk(qX);
    final int cYO = RoadRegion.quadToChunk(qY);
    final long seed = this.tracker.getSeed();
    final Random rand = new Random(seed);
    final float chance = this.config.getRoadChance();
    for (int cX = cXO; cX < cXO + RoadRegion.QUAD_CHUNK_LEN; cX++) {
      for (int cY = cYO; cY < cYO + RoadRegion.QUAD_CHUNK_LEN; cY++) {
        Utils.setFeatureSeed(rand, seed, cX, cY);
        if (rand.nextFloat() > chance) {
          continue;
        }
        final Point center = new Point((cX << 4) + 8, (cY << 4) + 8);
        final Point nearest = this.getNearestSuitable(gen, center);
        if (nearest == null) {
          continue;
        }
        RoadNetwork n = this.map.getNetwork(nearest.x, nearest.y);
        if (n == null) {
          n = this.generateNetwork(region, gen, rand, nearest, pX, pY, partial);
          if (n != null) {
            this.map.addNetwork(nearest.x, nearest.y, n);
          }
        }
        if (n != null) {
          region.getData().add(n);
          if (partial && region != rO) {
            rO.getData().add(n);
          }
        }
      }
    }
  }

  protected RoadNetwork generateNetwork(
      RoadRegion region, HeightmapGenerator gen, Random rand, Point src, short pX, short pY, boolean partial) {
    // build main road
    final Road r0 = this.getMainRoad(region, gen, src, rand);
    if (r0 == null || (!partial && !r0.isInRegion(pX, pY))) {
      return null;
    }
    // calculate bounds of network
    final int w = r0.maxX() - r0.minX();
    final int h = r0.maxY() - r0.minY();
    final int bx1; // left
    final int by1; // up
    final int bx2; // right
    final int by2; // down
    if (w > h) {
      // increase h to w
      final int m = (w - h) / 2;
      final int pad = w / 10;
      bx1 = r0.minX() - pad;
      bx2 = r0.maxX() + pad;
      by1 = r0.minY() - m - pad;
      by2 = r0.maxY() + m + pad;
    } else {
      // increase w to h;
      final int m = (h - w) / 2;
      final int pad = h / 10;
      bx1 = r0.minX() - m - pad;
      bx2 = r0.maxX() + m + pad;
      by1 = r0.minY() - pad;
      by2 = r0.maxY() + pad;
    }
    // setup list of roads
    final List<Road> roads = new ArrayList<>();
    final VertexGraph graph = new VertexGraph();
    roads.add(r0);
    graph.plot(r0);
    // generate random points in circle from center
    final int max = (bx2 - bx1) / 2; // 1/2 from center
    final int min = max / 2;         // 1/4 from center
    final int cX = (bx2 + bx1) / 2;
    final int cY = (by2 + by1) / 2;
    final float a = r0.broadAngle();
    final VertexGraph.Target target = graph.getTarget(cX, cY, 10.0);
    assert target != null;
    for (int i = 0; i < this.config.getMaxBranches(); i++) {
      double aO = a + Math.PI / 2 + rand.nextFloat() * Math.PI;
      if (rand.nextBoolean()) aO = Math.PI - aO;
      final double d = min + rand.nextInt(max - min + 1);
      final int xO = (int) (cX + d * Math.cos(aO));
      final int yO = (int) (cY + d * Math.sin(aO));
      final Point s = this.getNearestSuitable(gen, new Point(xO, yO));
      if (s == null) {
        continue;
      }
      // trace road to the nearest vertex
      final Road rN = this.trace(gen, s, target);
      if (rN != null) {
        // to be correct, we need to flag all points in range.
        rN.last().addFlag(RoadVertex.INTERSECTION);
        roads.add(rN);
        graph.plot(rN);
      }
    }
    return new RoadNetwork(roads, graph);
  }

  protected Road getMainRoad(RoadRegion region, HeightmapGenerator gen, Point src, Random rand) {
    final float a = rand.nextFloat() * TAU; // any angle
    final int minL = this.config.getMinRoadLength();
    final int maxL = this.config.getMaxRoadLength();
    final int d = minL + rand.nextInt(maxL - minL);
    final int aX = (int) (src.x + d * Math.cos(a));
    final int aY = (int) (src.y + d * Math.sin(a));
    final Point e = new Point(aX, aY);
    if (this.isTooClose(region, src, e, d)) {
      return null;
    }
    final Point dest = this.getNearestSuitable(gen, e);
    return dest == null ? null : this.trace(gen, src, dest);
  }

  protected boolean isTooClose(RoadRegion region, Point src, Point dest, int d) {
    final int cX1 = (src.x + dest.x) / 2;
    final int cY1 = (src.y + dest.y) / 2;
    final int r1 = d / 2;

    for (final RoadNetwork n : region) {
      final Road r = n.getMainRoad();
      final RoadVertex cv = r.vertices()[r.vertices().length / 2];
      final int cX2 = cv.x;
      final int cY2 = cv.y;
      final int r2 = r.length() / 2;
      // too close if >30% overlap
      if (Utils.distance(cX1, cY1, cX2, cY2) <= (r1 + r2) * 0.7) {
        return true;
      }
    }
    return false;
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
    double w = this.graph.getSd(gen, aX, aY) * 100.0;
    if (h < minCutoff) {
      w += (minCutoff - h) * (minCutoff - h);
    } else if (h > maxCutoff) {
      w += (h - maxCutoff) * (h - maxCutoff);
    }
    return w;
  }

  protected abstract Road trace(final HeightmapGenerator gen, final Point src, final Destination dest);
}
