package personthecat.roadmap;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TerrainGenerator {

    private final HeightmapGenerator mapGenerator;
    private final RoadMapGenerator roadGenerator;
    private final Config config;
    private final Tracker tracker;

    public TerrainGenerator(final Config config, final int seed) {
        this.tracker = new Tracker();
        this.mapGenerator = new HeightmapGenerator(config, this.tracker, seed);
        this.roadGenerator = new RoadMapGenerator(config, this.tracker, this.mapGenerator);
        this.config = config;
    }

    public void next(final int seed) {
        this.mapGenerator.next(seed);
    }

    public void reload() {
        this.mapGenerator.reload();
    }

    public void up(final int count) {
        this.tracker.up(count);
    }

    public void down(final int count) {
        this.tracker.down(count);
    }

    public void left(final int count) {
        this.tracker.left(count);
    }

    public void right(final int count) {
        this.tracker.right(count);
    }

    public BufferedImage generate() {
        final int w = this.config.getChunkWidth() << 4;
        final int h = this.config.getChunkHeight() << 4;
        final float[][] map = this.mapGenerator.generate(h, w);
        final BufferedImage image = this.colorize(map);
        this.roadGenerator.placeRoads(image);
        this.drawGridLines(image);
        return image;
    }

    private BufferedImage colorize(final float[][] map) {
        final BufferedImage image = new BufferedImage(map.length, map[0].length, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[0].length; y++) {
                image.setRGB(x, y, this.getColor(x, y, map[x][y]).getRGB());
            }
        }
        return image;
    }

    private Color getColor(final int x, final int y, final float n) {
        if (n >= 0) {
            final int green = 75 + (int) n;
            final int step = green % this.config.getResolution();
            return new Color(0, this.cap(green - step), 0);
        }
        return new Color(0, 0, 100 + (int) n);
    }

    private void drawGridLines(final BufferedImage image) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getWidth(); y++) {
                if (x % 32 == 0 || y % 32 == 0) {
                    image.setRGB(x, y, this.darken(image.getRGB(x, y), this.config.getGridOpacity()));
                } else if (x % 16 == 0 || y % 16 == 0) {
                    final int opacity = (this.config.getGridOpacity() + 1) / 2;
                    image.setRGB(x, y, this.darken(image.getRGB(x, y), opacity));
                }
            }
        }
    }

    private int darken(final int rgb, final int amount) {
        final Color color = new Color(rgb);
        return new Color(Math.max(color.getRed() - amount, 0),
            Math.max(color.getGreen() - amount, 0),
            Math.max(color.getBlue() - amount, 0),
            color.getAlpha()).getRGB();
    }

    private int cap(final int channel) {
        return Math.max(0, Math.min(255, channel));
    }
}
