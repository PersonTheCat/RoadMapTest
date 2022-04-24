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
    private int seed;

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
        this.yOffset -= 16 * count;
    }

    public void down(final int count) {
        this.yOffset += 16 * count;
    }

    public void left(final int count) {
        this.xOffset -= 16 * count;
    }

    public void right(final int count) {
        this.xOffset += 16 * count;
    }

    public BufferedImage generate() {
        final int w = this.config.getChunkWidth() << 4;
        final int h = this.config.getChunkHeight() << 4;
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                final float n = this.generator.getNoiseScaled(x + this.xOffset, y + this.yOffset);
                image.setRGB(x, y, this.getColor(x, y, n).getRGB());
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
            return 10;
        } else if (x % 16 == 0 || y % 16 == 0) {
            return 5;
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
