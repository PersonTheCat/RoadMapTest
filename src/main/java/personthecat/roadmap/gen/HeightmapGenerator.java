package personthecat.roadmap.gen;

import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.*;
import personthecat.roadmap.Config;
import personthecat.roadmap.data.Tracker;

public class HeightmapGenerator {
  private final Config config;
  private final Tracker tracker;
  private FastNoise generator;
  private float[][] prevMap;
  private int h;
  private int w;

  public HeightmapGenerator(final Config config, final Tracker tracker) {
    this.config = config;
    this.tracker = tracker;
    this.reload();
  }

  public void reload() {
    this.generator = this.createGenerator();
  }

  public float sample(final int x, final int y) {
    if (this.isInBounds(x, y)) {
      return this.prevMap[x - this.tracker.getXOffset()][y - this.tracker.getYOffset()];
    }
    return this.getNoise(x, y);
  }

  private boolean isInBounds(final int x, final int y) {
    final int xO = this.tracker.getXOffset();
    final int yO = this.tracker.getYOffset();
    return x >= xO && x < (xO + this.w) && y >= yO && y < (yO + this.h);
  }

  private float getNoise(final int x, final int y) {
    final float n = this.generator.getNoiseScaled(x, y);
    return n > 0 ? n * this.config.getSurfaceScale() : n;
  }

  public float[][] generate(final int h, final int w, final boolean reload) {
    final float[][] map = new float[w][h];
    if (this.prevMap == null || reload) {
      this.writeNewMap(map);
    } else {
      this.writePartialMap(map);
    }
    this.prevMap = map;
    this.h = h;
    this.w = w;
    return map;
  }

  private void writeNewMap(final float[][] map) {
    final int xOffset = this.tracker.getXOffset();
    final int yOffset = this.tracker.getYOffset();
    for (int x = 0; x < map.length; x++) {
      for (int y = 0; y < map[0].length; y++) {
        map[x][y] = this.getNoise(x + xOffset, y + yOffset);
      }
    }
  }

  private void writePartialMap(final float[][] map) {
    final int xOffset = this.tracker.getXOffset();
    final int yOffset = this.tracker.getYOffset();
    final int xD = xOffset - this.tracker.getPrevXOffset();
    final int yD = yOffset - this.tracker.getPrevYOffset();

    for (int x = 0; x < map.length; x++) {
      final int xO = x + xD;
      for (int y = 0; y < map[0].length; y++) {
        final int yO = y + yD;
        if (xO >= 0 && xO < map.length && yO >= 0 && yO < map[0].length) {
          map[x][y] = this.prevMap[xO][yO];
        } else {
          map[x][y] = this.getNoise(x + xOffset, y + yOffset);
        }
      }
    }
  }

  private FastNoise createGenerator() {
    final NoiseDescriptor cfg = new NoiseDescriptor()
        .noise(NoiseType.MULTI)
        .multi(MultiType.SUM)
        .noiseLookup(this.primaryMap(), this.grooves());
    if (this.tracker.isMountains()) {
      return cfg.scalar(HeightmapGenerator::scaleMountains).generate();
    }
    return cfg.generate();
  }

  private NoiseDescriptor primaryMap() {
    return new NoiseDescriptor()
        .noise(this.config.getMapType())
        .range(this.config.getMinY(), this.config.getMaxY())
        .frequency(this.tracker.getFrequency())
        .seed(this.tracker.getSeed());
  }

  private NoiseDescriptor grooves() {
    return new NoiseDescriptor()
        .noise(this.config.getGrooveType())
        .range(-this.config.getGrooveSize(), this.config.getGrooveSize())
        .frequency(this.tracker.getGrooveFrequency())
        .fractal(FractalType.FBM)
        .seed(this.tracker.getSeed());
  }

  private static float scaleMountains(final float y) {
    if (y <= 0) {
      return y;
    }
    return (float) (((0.000000002 * Math.pow(y, 6)) / 6) + (9 * Math.sqrt(y)));
  }
}
