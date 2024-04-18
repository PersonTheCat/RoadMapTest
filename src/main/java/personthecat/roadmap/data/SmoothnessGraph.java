package personthecat.roadmap.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import personthecat.roadmap.util.Utils;
import personthecat.roadmap.gen.HeightmapGenerator;

public class SmoothnessGraph {
  protected final Long2ObjectMap<Data> graph = new Long2ObjectOpenHashMap<>();

  public float getSd(final HeightmapGenerator gen, final int x, final int y) {
    final int cX = x >> 4;
    final int cY = y >> 4;
    final int rX = x & 15;
    final int rY = y & 15;
    final int lX = lowerCorner(rX);
    final int lY = lowerCorner(rY);
    final Data data = this.getData(cX, cY);
    if (data.get(lX + 1, lY + 1) == 0) { // nothing interpolated between corners
      this.compute(gen, data, cX, cY, lX, lY);
    }
    return data.get(rX, rY);
  }

  protected Data getData(final int cX, final int cY) {
    return this.graph.computeIfAbsent((((long) cX) << 32) | (cY & 0xFFFFFFFFL), c -> new Data());
  }

  protected void compute(final HeightmapGenerator gen, final Data data, final int cX, final int cY, final int lX, final int lY) {
    final int uX = lX + 4;
    final int uY = lY + 4;
    // get samples around 4 corners
    for (int x = lX - 4; x <= uX + 4; x += 4) {
      for (int y = lY - 4; y <= uY + 4; y += 4) {
        if ((x == lX - 4 || x == uX + 4) && (y == lY - 4 || y == uY + 4)) { // is corner of area
          continue;
        }
        if (data.getSample(x, y) == 0) {
          data.setSample(x, y, gen.sample((cX << 4) + x, (cY << 4) + y));
        }
      }
    }
    // calculate SDs at each corner
    data.set(lX, lY, (float) this.computeSd(data, lX, lY));
    data.set(uX, uY, (float) this.computeSd(data, uX, uY));
    data.set(lX, uY, (float) this.computeSd(data, lX, uY));
    data.set(uX, lY, (float) this.computeSd(data, uX, lY));

    float a, b;
    // interpolate left column
    a = data.get(lX, lY);
    b = data.get(lX, uY);
    data.set(lX, lY, a);
    data.set(lX, lY + 1, Utils.lerp(a, b, 0.25F));
    data.set(lX, lY + 2, Utils.lerp(a, b, 0.5F));
    data.set(lX, lY + 3, Utils.lerp(a, b, 0.75F));

    // interpolate right column
    a = data.get(uX, lY);
    b = data.get(uX, uY);
    data.set(uX, lY, a);
    data.set(uX, lY + 1, Utils.lerp(a, b, 0.25F));
    data.set(uX, lY + 2, Utils.lerp(a, b, 0.5F));
    data.set(uX, lY + 3, Utils.lerp(a, b, 0.75F));

    // interpolate between columns
    for (int y = lY; y <= uY; y++) {
      a = data.get(lX, y);
      b = data.get(uX, y);
      data.set(lX + 1, y, Utils.lerp(a, b, 0.25F));
      data.set(lX + 2, y, Utils.lerp(a, b, 0.5F));
      data.set(lX + 3, y, Utils.lerp(a, b, 0.75F));
    }
  }

  protected double computeSd(final Data data, final int rX, final int rY) {
    return Utils.stdDev(
        data.getSample(rX, rY),
        data.getSample(rX, rY + 4),
        data.getSample(rX, rY - 4),
        data.getSample(rX + 4, rY),
        data.getSample(rX - 4, rY)
    );
  }

  public void clear() {
    this.graph.clear();
  }

  protected record Data(float[] sds, float[] samples) {

    public Data() {
      this(new float[256], new float[49]);
    }

    public float get(final int rX, final int rY) {
      return this.sds[indexOf(rX, rY)];
    }

    public void set(final int rX, final int rY, final float f) {
      this.sds[indexOf(rX, rY)] = f;
    }

    public float getSample(final int rX, final int rY) {
      return this.samples[indexOfSample(rX, rY)];
    }

    public void setSample(final int rX, final int rY, final float f) {
      this.samples[indexOfSample(rX, rY)] = f;
    }
  }

  protected static int lowerCorner(final int i) {
    return i >> 2 << 2;
  }

  protected static int indexOfSample(final int rX, final int rY) {
    return ((rX + 4) >> 2) * 7 + ((rY + 4) >> 2);
  }

  protected static int indexOf(final int x, final int y) {
    return ((x & 15) << 4) + (y & 15);
  }
}
