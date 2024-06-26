package personthecat.roadmap.gen;

import personthecat.roadmap.Config;
import personthecat.roadmap.data.Tracker;
import personthecat.roadmap.gen.road.RoadImageGenerator;
import personthecat.roadmap.gen.road.RoadMap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class TerrainGenerator {
  private static final int MIN_COLOR_VALUE = 1;

  private final HeightmapGenerator mapGenerator;
  private final RoadImageGenerator roadGenerator;
  private final Config config;
  private final Tracker tracker;
  private final Random rand;
  private BufferedImage buffer;
  private Graphics2D graphics;

  public TerrainGenerator(final Tracker tracker, final Config config) {
    this.tracker = tracker;
    this.mapGenerator = new HeightmapGenerator(config, this.tracker);
    this.roadGenerator = new RoadImageGenerator(config, this.tracker, this.mapGenerator);
    this.config = config;
    this.rand = new Random();
  }

  public void next(final Random rand) {
    this.tracker.nextSeed(rand);
    this.rand.setSeed(this.tracker.getSeed());
  }

  public void previous() {
    this.tracker.previousSeed();
    this.rand.setSeed(this.tracker.getSeed());
  }

  public void reload() {
    this.mapGenerator.reload();
  }

  public BufferedImage getBuffer() {
    return this.buffer;
  }

  public BufferedImage generate(final boolean reload) {
    final int w = this.config.getChunkWidth() << 4;
    final int h = this.config.getChunkHeight() << 4;
    final float[][] map = this.mapGenerator.generate(h, w, reload);
    final BufferedImage overlay = this.roadGenerator.getRoadOverlay(h, w, reload);
    if (this.tracker.isSideView()) {
      this.resetBuffer(h, w, reload, true);
      this.drawSideView(map, this.buffer, overlay);
    } else {
      this.resetBuffer(h, w, reload, false);
      this.drawMap(map, this.buffer, overlay);
    }
    return this.buffer;
  }

  private void resetBuffer(final int h, final int w, final boolean reload, final boolean needsFreshImage) {
    if (reload || this.buffer == null) {
      this.buffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      this.graphics = this.buffer.createGraphics();
    } else if (needsFreshImage) {
      this.graphics.setBackground(new Color(0, 0, 0, 0));
      this.graphics.clearRect(0, 0, w, h);
    }
  }

  private void drawSideView(final float[][] map, final BufferedImage image, final BufferedImage overlay) {
    int o = 0;
    for (int y = map[0].length - 1; y >= 0; y--) {
      this.drawSlice(image, map, y, o++, overlay);
    }
    this.drawBackground(image);
  }

  private void drawSlice(
      final BufferedImage image, final float[][] map, final int y, final int o, final BufferedImage overlay) {
    for (int x = 0; x < map.length; x++) {
      final int n = (int) map[x][y];
      final int a = n + (int) ((double) o * this.tracker.getSideViewAngle());
      final int r = overlay.getRGB(x, y);
      final int base = r == 0 ? this.getColor(n).getRGB() : r;
      final int c = this.darken(base, o / 32);
      for (int h = Math.min(a, map[0].length - 1); h >= 0; h--) {
        if (image.getRGB(x, map[0].length - h - 1) != 0) {
          break;
        }
        image.setRGB(x, map[0].length - h - 1, c);
      }
    }
  }

  private void drawBackground(final BufferedImage image) {
    final int bg = this.config.getBackgroundColor().getRGB();
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        if (image.getRGB(x, y) != 0) {
          break;
        }
        image.setRGB(x, y, bg);
      }
    }
  }

  private void drawMap(final float[][] map, final BufferedImage image, final BufferedImage overlay) {
    this.colorize(map, image);
    this.drawOverlay(image, overlay);
    this.drawGridLines(image);
  }

  private void drawOverlay(final BufferedImage image, final BufferedImage overlay) {
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        final int c = overlay.getRGB(x, y);
        if (c != 0) {
          image.setRGB(x, y, c);
        }
      }
    }
  }

  private void colorize(final float[][] map, final BufferedImage image) {
    for (int x = 0; x < map.length; x++) {
      for (int y = 0; y < map[0].length; y++) {
        image.setRGB(x, y, this.getColor(map[x][y]).getRGB());
      }
    }
  }

  private Color getColor(final float n) {
    if (n >= 0) {
      final int green = 75 + (int) n;
      final int step = green % this.config.getResolution();
      final int capped = this.cap(green - step);
      if (capped > 175) {
        final int rb = (int) ((double) (175 + (((int) n - 175) / 4)) * 0.75);
        final int rbStep = rb % this.config.getResolution();
        final int rbCapped = this.cap(rb - rbStep);
        final int gCapped = this.cap(rb - rbStep + 25);
        return new Color(rbCapped, gCapped, rbCapped);
      }
      return new Color(50, capped, 0);
    }
    return new Color(25, 25, this.cap(100 + (int) n));
  }

  private void drawGridLines(final BufferedImage image) {
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
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
    return new Color(Math.max(color.getRed() - amount, MIN_COLOR_VALUE),
        Math.max(color.getGreen() - amount, MIN_COLOR_VALUE),
        Math.max(color.getBlue() - amount, MIN_COLOR_VALUE),
        color.getAlpha()).getRGB();
  }

  private int cap(final int channel) {
    return Math.max(0, Math.min(255, channel));
  }

  public RoadMap getRoadMap() {
    return this.roadGenerator.getRoadMap();
  }

  public HeightmapGenerator getMapGenerator() {
    return this.mapGenerator;
  }
}
