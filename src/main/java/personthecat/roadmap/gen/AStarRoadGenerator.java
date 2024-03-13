package personthecat.roadmap.gen;

import personthecat.roadmap.Config;
import personthecat.roadmap.Tracker;

import java.awt.Color;
import java.util.List;

public class AStarRoadGenerator extends RoadGenerator {
  private static final byte DEMO_RADIUS = 9;
  private static final int DEMO_COLOR = Color.DARK_GRAY.getRGB();
  private static final float DEMO_INTEGRITY = 0.65F;

  private final AStar aStar;

  public AStarRoadGenerator(
      final Config config, final Tracker tracker) {
    super(config, tracker);
    this.aStar = new AStar(config);
  }

  @Override
  protected Road trace(final HeightmapGenerator gen, final Point src, final Point dest, final int x, final int y) {
    this.aStar.reset(gen);
    final List<Point> path = this.aStar.search(src, dest);
    if (path == null) {
      return null;
    }
    short minX = Short.MAX_VALUE;
    short minY = Short.MAX_VALUE;
    short maxX = Short.MIN_VALUE;
    short maxY = Short.MIN_VALUE;
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
        final float a1 = (float) Math.atan2(p.y - prev.y, p.x - prev.x);
        final float a2 = (float) Math.atan2(next.y - p.y, next.x - p.x);
        theta = a2 - a1;
        xAngle = a2;
      } else {
        theta = -1;
        xAngle = -1;
      }
      final short relX = (short) (p.x - RoadRegion.getAbsoluteCoord(x));
      final short relY = (short) (p.y - RoadRegion.getAbsoluteCoord(y));
      if (relX < minX) minX = relX;
      if (relY < minY) minY = relY;
      if (relX > maxX) maxX = relX;
      if (relY > maxY) maxY = relY;
      final RoadVertex v = new RoadVertex(relX, relY, DEMO_RADIUS, DEMO_COLOR, DEMO_INTEGRITY, theta, xAngle, (short) 0);
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
    return new Road((byte) 0, minX, minY, maxX, maxY, vertices);
  }

  private void smoothAngles(final RoadVertex[] vertices) {
    final int amount = 2;
//    final float[] angles = new float[vertices.length - amount];
    for (int i = amount; i < vertices.length - amount; i++) {
      float sum = 0;
      for (int s = -amount; s <= amount; s++) {
        sum += vertices[i + s].theta;
      }
//      angles[i - amount] = sum / (amount * 2 + 1);
      vertices[i].theta = sum / (amount * 2 + 1);
    }
//    for (int i = 0; i < angles.length; i++) {
//      vertices[i + amount].angle = angles[i];
//    }
  }
}
