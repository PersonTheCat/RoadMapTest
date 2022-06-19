package personthecat.roadmap;

import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.FractalType;
import personthecat.fastnoise.data.NoiseDescriptor;
import personthecat.fastnoise.data.NoiseType;

public class HeightmapGenerator {

    private final Config config;
    private final Tracker tracker;
    private FastNoise generator;
    private float[][] prevMap;

    public HeightmapGenerator(final Config config, final Tracker tracker, final int seed) {
        this.config = config;
        this.tracker = tracker;
        this.next(seed);
    }

    public void next(final int seed) {
        this.tracker.setSeed(seed);
        this.reload();
    }

    public void reload() {
        this.generator = this.createGenerator();
    }

    public float sample(final int x, final int y) {
        final float n =
            this.generator.getNoiseScaled(x + this.tracker.getXOffset(), y + this.tracker.getYOffset());
        return n > 0 ? n * this.config.getSurfaceScale() : n;
    }

    public float[][] generate(final int h, final int w, final boolean reload) {
        final float[][] map = new float[w][h];
        if (this.prevMap == null || reload) {
            this.writeNewMap(map);
        } else {
            this.writePartialMap(map);
        }
        this.tracker.reset();
        this.prevMap = map;
        return map;
    }

    private void writeNewMap(final float[][] map) {
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[0].length; y++) {
                map[x][y] = this.sample(x, y);
            }
        }
    }

    private void writePartialMap(final float[][] map) {
        final int xD = this.tracker.getXOffset() - this.tracker.getPrevXOffset();
        final int yD = this.tracker.getYOffset() - this.tracker.getPrevYOffset();

        for (int x = 0; x < map.length; x++) {
            final int xO = x + xD;
            for (int y = 0; y < map[0].length; y++) {
                final int yO = y + yD;
                if (xO >= 0 && xO < map.length && yO >= 0 && yO < map[0].length) {
                    map[x][y] = this.prevMap[xO][yO];
                } else {
                    map[x][y] = this.sample(x, y);
                }
            }
        }
    }

    private FastNoise createGenerator() {
        final NoiseDescriptor cfg = new NoiseDescriptor()
            .noise(NoiseType.MULTI)
            .noiseLookup(this.primaryMap(), this.grooves());
        if (this.config.isMountains()) {
            return cfg.scalar(HeightmapGenerator::scaleMountains).generate();
        }
        return cfg.generate();
    }

    private NoiseDescriptor primaryMap() {
        return new NoiseDescriptor()
            .noise(NoiseType.SIMPLEX)
            .range(this.config.getMinY(), this.config.getMaxY())
            .frequency(this.config.getFrequency())
            .seed(this.tracker.getSeed());
    }

    private NoiseDescriptor grooves() {
        return new NoiseDescriptor()
            .noise(NoiseType.PERLIN)
            .range(-this.config.getGrooveSize(), this.config.getGrooveSize())
            .frequency(this.config.getGrooveFrequency())
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
