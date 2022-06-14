package personthecat.roadmap;

import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.FractalType;
import personthecat.fastnoise.data.NoiseDescriptor;
import personthecat.fastnoise.data.NoiseType;

import java.awt.*;
import java.awt.image.BufferedImage;

public class HeightmapGenerator {

    private final Config config;
    private FastNoise generator;
    private int xOffset = 0;
    private int yOffset = 0;
    private int prevXOffset = 0;
    private int prevYOffset = 0;
    private int seed;
    private int prevSeed;
    private float[][] prevMap;

    public HeightmapGenerator(final Config config, final int seed) {
        this.config = config;
        this.next(seed);
    }

    public void next(final int seed) {
        this.seed = seed;
        this.reload();
    }

    public void reload() {
        this.generator = this.createGenerator();
    }

    public void up(final int count) {
        this.yOffset -= 32 * count;
    }

    public void down(final int count) {
        this.yOffset += 32 * count;
    }

    public void left(final int count) {
        this.xOffset -= 32 * count;
    }

    public void right(final int count) {
        this.xOffset += 32 * count;
    }

    public BufferedImage generate() {
        final int w = this.config.getChunkWidth() << 4;
        final int h = this.config.getChunkHeight() << 4;
        final float[][] map = new float[w][h];
        if (this.prevMap == null
                || this.prevMap.length != map.length
                || this.prevMap[0].length != map[0].length
                || this.seed != this.prevSeed) {
            this.writeNewMap(map);
        } else {
            this.writePartialMap(map);
        }
        this.prevXOffset = this.xOffset;
        this.prevYOffset = this.yOffset;
        this.prevSeed = this.seed;
        this.prevMap = map;
        return this.colorize(map);
    }

    private void writeNewMap(final float[][] map) {
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[0].length; y++) {
                map[x][y] = this.generator.getNoiseScaled(x + this.xOffset, y + this.yOffset);
            }
        }
    }

    private void writePartialMap(final float[][] map) {
        final int xD = this.xOffset - this.prevXOffset;
        final int yD = this.yOffset - this.prevYOffset;

        for (int x = 0; x < map.length; x++) {
            final int xO = x + xD;
            for (int y = 0; y < map[0].length; y++) {
                final int yO = y + yD;
                if (xO >= 0 && xO < map.length && yO >= 0 && yO < map[0].length) {
                    map[x][y] = this.prevMap[xO][yO];
                } else {
                    map[x][y] = this.generator.getNoiseScaled(x + this.xOffset, y + this.yOffset);
                }
            }
        }
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
        final int grid = this.getGridLine(x, y);
        if (n >= 0) {
            final int green = 75 + (int) n;
            final int step = green % this.config.getResolution();
            return new Color(0, this.cap(green - step - grid), 0);
        }
        final int blue = 100 + (int) n;
        return new Color(0, 0, this.cap(blue - grid));
    }

    private int getGridLine(final int x, final int y) {
        if (x % 32 == 0 || y % 32 == 0) {
            return this.config.getGridOpacity();
        } else if (x % 16 == 0 || y % 16 == 0) {
            return (this.config.getGridOpacity() + 1) / 2;
        }
        return 0;
    }

    private int cap(final int channel) {
        return Math.max(0, Math.min(255, channel));
    }

    private FastNoise createGenerator() {
        return new NoiseDescriptor()
            .noise(NoiseType.MULTI)
            .noiseLookup(this.primaryMap(), this.grooves())
            .generate();
    }

    private NoiseDescriptor primaryMap() {
        return new NoiseDescriptor()
            .noise(NoiseType.SIMPLEX)
            .range(this.config.getMinY(), this.config.getMaxY())
            .frequency(this.config.getFrequency())
            .seed(this.seed);
    }

    private NoiseDescriptor grooves() {
        return new NoiseDescriptor()
            .noise(NoiseType.PERLIN)
            .range(-this.config.getGrooveSize(), this.config.getGrooveSize())
            .frequency(this.config.getGrooveFrequency())
            .fractal(FractalType.FBM)
            .seed(this.seed);
    }
}
