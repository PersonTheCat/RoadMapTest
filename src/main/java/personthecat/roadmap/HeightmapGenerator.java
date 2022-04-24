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

    public void up() {
        this.yOffset -= 16;
    }

    public void down() {
        this.yOffset += 16;
    }

    public void left() {
        this.xOffset -= 16;
    }

    public void right() {
        this.xOffset += 16;
    }

    public BufferedImage generate() {
        final int w = this.config.getChunkWidth() << 4;
        final int h = this.config.getChunkHeight() << 4;
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                final float n = this.generator.getNoiseScaled(x + this.xOffset, y + this.yOffset);
                image.setRGB(x, y, this.getColor(n).getRGB());
            }
        }
        return image;
    }

    private Color getColor(final float n) {
        if (n >= 0) {
            final int g = 75 + (int) n;
            final int step = g % this.config.getResolution();
            return new Color(0, g - step, 0);
        }
        return new Color(0, 0, (int) Math.max(0, 100 + n));
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
