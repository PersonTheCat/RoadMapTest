package personthecat.roadmap.gen.road;

import personthecat.roadmap.Config;
import personthecat.roadmap.data.Tracker;
import personthecat.roadmap.gen.HeightmapGenerator;
import personthecat.roadmap.data.Point;

import java.awt.Color;
import java.util.List;

public class AStarRoadGenerator extends RoadGenerator {
  private static final byte DEMO_RADIUS_0 = 3;
  private static final byte DEMO_RADIUS_1 = 2;
  private static final byte DEMO_RADIUS_2 = 2;
  private static final int DEMO_COLOR_0 = Color.DARK_GRAY.getRGB();
  private static final int DEMO_COLOR_1 = new Color(128, 128, 64).getRGB();
  private static final int DEMO_COLOR_2 = new Color(105, 105, 50).getRGB();
  private static final float DEMO_INTEGRITY = 0.65F;

  private final AStar aStar;

  public AStarRoadGenerator(final RoadMap map, final Config config, final Tracker tracker) {
    super(map, config, tracker);
    this.aStar = new AStar(config, this.graph);
  }

  @Override
  protected Road trace(HeightmapGenerator gen, Point src, Destination dest) {
    this.aStar.reset(gen);
    final List<Point> path = this.aStar.search(src, dest);
    if (path == null) {
      return null;
    }
    final int l = dest.getRoadLevel();
    final int color;
    final byte radius;
    switch (l) {
      case 0 -> {
        color = DEMO_COLOR_0;
        radius = DEMO_RADIUS_0;
      }
      case 1 -> {
        color = DEMO_COLOR_1;
        radius = DEMO_RADIUS_1;
      }
      default -> {
        color = DEMO_COLOR_2;
        radius = DEMO_RADIUS_2;
      }
    }
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    final int len = path.size();
    final RoadVertex[] vertices = new RoadVertex[len];
    for (int i = len - 1; i >= 0; i--) {
      final Point p = path.get(i);
      final float theta;
      final float xAngle;
      if (i > 0 && i < len - 1) {
        // Check ahead by 2, if possible. Lazy way to get the angle over a large distance
        final Point prev = i < len - 2 ? path.get(i + 2) : path.get(i + 1);
        final Point next = i > 1 ? path.get(i - 2) : path.get(0); // i - 1 = 0
        final float a1 = (float) Math.atan2(prev.y - p.y, prev.x - p.x);
        final float a2 = (float) Math.atan2(next.y - p.y, next.x - p.x);
        final float t = a2 - a1;
        theta = t < 0 ? t + TAU : t;
        xAngle = a2;
      } else {
        theta = -1;
        xAngle = -1;
      }
      if (p.x < minX) minX = p.x;
      if (p.y < minY) minY = p.y;
      if (p.x > maxX) maxX = p.x;
      if (p.y > maxY) maxY = p.y;
      final RoadVertex v = new RoadVertex(p.x, p.y, radius, color, DEMO_INTEGRITY, theta, xAngle, (short) 0);
      vertices[path.size() - i - 1] = v;
      if (i == len - 1) {
        v.addFlag(RoadVertex.START);
      } else if (i == 0) {
        v.addFlag(RoadVertex.END);
      } else {
        v.addFlag(RoadVertex.MIDPOINT);
      }
    }
    this.smoothAngles(vertices);
    return new Road((byte) l, minX, minY, maxX, maxY, vertices);
  }

  private void smoothAngles(final RoadVertex[] vertices) {
    final int amount = 1;
    final float[] angles = new float[vertices.length - amount];
    for (int i = amount; i < vertices.length - amount; i++) {
      float sum = 0;
      for (int s = -amount; s <= amount; s++) {
        sum += vertices[i + s].theta;
      }
      angles[i - amount] = sum / (amount * 2 + 1);
    }
    for (int i = 0; i < angles.length; i++) {
      vertices[i + amount].theta = angles[i];
    }
  }

}
