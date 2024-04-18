package personthecat.roadmap.gen.road;

import personthecat.roadmap.Config;
import personthecat.roadmap.data.Tracker;
import personthecat.roadmap.util.Utils;
import personthecat.roadmap.gen.HeightmapGenerator;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class RoadImageGenerator {
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
    if (this.tracker.isEnableRoads()) {
      if (this.prevImage == null || reload) {
        this.writeNewImage(overlay);
      } else {
        this.writePartialImage(overlay);
      }
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
        Utils.setFeatureSeed(rand, seed, cX, cY);
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
    final short rX = RoadRegion.absToRegion(aX);
    final short rY = RoadRegion.absToRegion(aY);
    final RoadRegion region = this.roadMap.getRegion(this.mapGen, rX, rY);
    for (final RoadNetwork network : region) {
      if (!network.containsPoint(aX, aY)) {
        continue;
      }
      for (final Road road : network.roads) {
        if (!road.containsPoint(aX, aY)) {
          continue;
        }
        for (final RoadVertex vertex : road.vertices()) {
          final double d = Utils.distance(vertex.x, vertex.y, aX + 8, aY + 8);
          if (d < vertex.radius + 16) {
            this.trace(image, vertex, rand, road.level(), aX, aY, vertex.x, vertex.y);
          }
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

  private void trace(
      BufferedImage image, RoadVertex vertex, Random rand, int l, int aX, int aY, int vAX, int vAY) {
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
        if (Math.sqrt(dX2 + dY2) < vertex.radius) {
          if (vertex.integrity == 1 || rand.nextFloat() <= vertex.integrity) {
            image.setRGB(x - xO, y - yO, color);
          }
        }
      }
    }
  }

  public RoadMap getRoadMap() {
    return this.roadMap;
  }
}
