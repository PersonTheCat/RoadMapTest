package personthecat.roadmap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public record RoadMapGenerator(Config config, Tracker tracker, HeightmapGenerator mapGenerator) {

    private static final int DEMO_RADIUS = 21;
    private static final int DEMO_COLOR = Color.DARK_GRAY.getRGB();
    private static final float DEMO_INTEGRITY = 0.65F;

    public void placeRoads(final BufferedImage image, final Random rand) {
        final RoadMap roadMap = this.generateRoads(image.getWidth(), image.getHeight());
        for (int x = 0; x < image.getWidth(); x++) {
            final int aX = x + this.tracker.getXOffset();
            for (int y = 0; y < image.getHeight(); y++) {
                final int aY = y + this.tracker.getYOffset();
                // Demo algorithm, should probably be optimized.
                for (final RoadVertex vertex : roadMap.get(aX, aY)) {
                    this.trace(image, rand, vertex);
                }
            }
        }
    }

    private RoadMap generateRoads(final int w, final int h) {
        final RoadMap roadMap = new RoadMap();
        final int cX = (w / 2) + this.tracker.getXOffset();
        final int cY = (h / 2) + this.tracker.getYOffset();
        final Point pos = new Point(cX, cY);
        final RoadVertex demoVertex =
            new RoadVertex(pos, DEMO_RADIUS, DEMO_COLOR, DEMO_INTEGRITY);
        roadMap.put(pos, demoVertex);
        return roadMap;
    }

    private void trace(final BufferedImage image, final Random rand, final RoadVertex vertex) {
        final int cX = vertex.pos().x() - this.tracker.getXOffset();
        final int cY = vertex.pos().y() - this.tracker.getYOffset();
        final int r = vertex.radius();

        for (int x = cX - r; x < cX + r; x++) {
            final int dX = x - cX;
            final int dX2 = dX * dX;

            for (int y = cY - r; y < cY + r; y++) {
                final int dY = y - cY;
                final int dY2 = dY * dY;

                if (dX2 + dY2 < r) {
                    if (vertex.integrity() == 1.0 || rand.nextFloat() <= vertex.integrity()) {
                        image.setRGB(x, y, vertex.color());
                    }
                }
            }
        }
    }
}
