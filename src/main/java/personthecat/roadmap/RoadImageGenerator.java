package personthecat.roadmap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class RoadImageGenerator {
  // assume a road will not deviate > 64 blocks from its corners
  private static final int DRAW_PADDING = 64;

  private final Config config;
  private final Tracker tracker;
  private final RoadMap roadMap;
  private final HeightmapGenerator mapGen;
  private BufferedImage prevImage;

  public RoadImageGenerator(final Config config, final Tracker tracker, final HeightmapGenerator mapGen) {
    this.config = config;
    this.tracker = tracker;
    this.mapGen = mapGen;
    this.roadMap = new RoadMap(config, tracker);
  }

  public BufferedImage getRoadOverlay(final int h, final int w, final boolean reload) {
    final BufferedImage overlay = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    if (this.prevImage == null || reload) {
      this.writeNewImage(overlay);
    } else {
      this.writePartialImage(overlay);
    }
    this.prevImage = overlay;
    return overlay;
  }

  private void writeNewImage(final BufferedImage image) {
    // simulate generating in chunks for now
    // the logic is easier to reuse this way,
    // but we may still want to optimize for this application
    final int cXO = this.tracker.getXOffset() >> 4;
    final int cYO = this.tracker.getYOffset() >> 4;
    final long seed = this.tracker.getSeed();
    final Random rand = new Random(seed);
    // absolute chunks
    for (int cX = cXO; cX < cXO + this.config.getChunkWidth(); cX++) {
      for (int cY = cYO; cY < cYO + this.config.getChunkHeight(); cY++) {
        rand.setSeed(31 * (seed + cX) + cY);
        this.generateChunk(image, cX, cY, rand);
      }
    }
  }

  private void writePartialImage(final BufferedImage image) {
    final int cXOffset = this.tracker.getXOffset() >> 4;
    final int cYOffset = this.tracker.getYOffset() >> 4;
    final int cXD = cXOffset - (this.tracker.getPrevXOffset() >> 4);
    final int cYD = cYOffset - (this.tracker.getPrevYOffset() >> 4);

    final long seed = this.tracker.getSeed();
    final Random rand = new Random(seed);
    final int w = this.config.getChunkWidth();
    final int h = this.config.getChunkHeight();
    // image relative chunks
    for (int cX = 0; cX < w; cX++) {
      final int cXO = cX + cXD;
      for (int cY = 0; cY < h; cY++) {
        final int cYO = cY + cYD;
        if (cXO >= 0 && cXO < w && cYO >= 0 && cYO < h) {
          this.copyChunk(image, cX, cY, cXO, cYO);
        } else {
          Utils.setFeatureSeed(rand, seed, cX + cXOffset, cY + cYOffset);
          this.generateChunk(image, cX + cXOffset, cY + cYOffset, rand);
        }
      }
    }
  }

  private void generateChunk(final BufferedImage image, final int cX, final int cY, final Random rand) {
    final int aX = cX << 4;
    final int aY = cY << 4;
    final short rX = RoadRegion.getRegionCoord(aX);
    final short rY = RoadRegion.getRegionCoord(aY);
    final RoadRegion region = this.roadMap.getRegion(this.mapGen, rX, rY);
    for (final Road road : region.getData()) {
      if (!isInsideRoad(road, rX, rY, aX, aY)) {
        continue;
      }
      for (final RoadVertex vertex : road.vertices()) {
        final int vAX = RoadRegion.getAbsoluteCoord(rX) + vertex.relX;
        final int vAY = RoadRegion.getAbsoluteCoord(rY) + vertex.relY;
        final double d = distance(vAX, vAY, aX + 8, aY + 8);
        if (d < vertex.radius + 16) {
          this.trace(image, vertex, rand, aX, aY, vAX, vAY);
        }
      }
    }
  }

  private void copyChunk(final BufferedImage image, final int cX, final int cY, final int cXO, final int cYO) {
    final int aCX = cX << 4;
    final int aCY = cY << 4;
    final int aCXO = cXO << 4;
    final int aCYO = cYO << 4;
    final BufferedImage src = this.prevImage;
    // image relative chunks and blocks
    for (int x = 0; x < 16; x++) {
      for (int y = 0; y < 16; y++) {
        image.setRGB(aCX + x, aCY + y, src.getRGB(aCXO + x, aCYO + y));
      }
    }
  }

  private static boolean isInsideRoad(final Road road, final int rX, final int rY, final int aX, final int aY) {
    final int aRX = RoadRegion.getAbsoluteCoord(rX);
    final int aRY = RoadRegion.getAbsoluteCoord(rY);
    final int x1 = aRX + road.minX();
    final int y1 = aRY + road.minY();
    final int x2 = aRX + road.maxX();
    final int y2 = aRY + road.maxY();
    return aX > x1 - DRAW_PADDING
        && aX < x2 + DRAW_PADDING
        && aY > y1 - DRAW_PADDING
        && aY < y2 + DRAW_PADDING;
  }

  private static double distance(final int x1, final int y1, final int x2, final int y2) {
    return Math.sqrt(((x1 - x2) * (x1 - x2)) + ((y1 - y2) * (y1 - y2)));
  }

  private void trace(
      BufferedImage image, RoadVertex vertex, Random rand, int aX, int aY, int vAX, int vAY) {
    final int xO = this.tracker.getXOffset();
    final int yO = this.tracker.getYOffset();
    final int color =
        this.config.isHighlightRoadEndpoints() && !vertex.hasFlag(RoadVertex.MIDPOINT)
            ? Color.RED.getRGB() : vertex.color;
    // absolute blocks
    for (int x = aX; x < aX + 16; x++) {
      final int dX2 = (x - vAX) * (x - vAX);
      for (int y = aY; y < aY + 16; y++) {
        final int dY2 = (y - vAY) * (y - vAY);
        if (dX2 + dY2 < vertex.radius) {
          if (vertex.integrity == 1 || rand.nextFloat() <= vertex.integrity) {
            image.setRGB(x - xO, y - yO, color);
          }
        }
      }
    }
  }
}
