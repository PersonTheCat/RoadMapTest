package personthecat.roadmap;

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
    final RoadVertex[] vertices = new RoadVertex[path.size()];
    for (int i = path.size() - 1; i >= 0; i--) {
      final Point p = path.get(i);
      final short relX = (short) (p.x - RoadRegion.getAbsoluteCoord(x));
      final short relY = (short) (p.y - RoadRegion.getAbsoluteCoord(y));
      if (relX < minX) minX = relX;
      if (relY < minY) minY = relY;
      if (relX > maxX) maxX = relX;
      if (relY > maxY) maxY = relY;
      final RoadVertex v = new RoadVertex(relX, relY, DEMO_RADIUS, DEMO_COLOR, DEMO_INTEGRITY, (short) 0);
      vertices[path.size() - i - 1] = v;
      if (i == path.size() - 1) {
        v.addFlag(RoadVertex.START);
      } else if (i == 0) {
        v.addFlag(RoadVertex.END);
      } else {
        v.addFlag(RoadVertex.MIDPOINT);
      }
    }
    return new Road((byte) 0, minX, minY, maxX, maxY, vertices);
  }
}
