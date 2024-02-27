package personthecat.roadmap;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class RoadRegion {
  private static final String TEMP_SAVE_DIR = "roads";
  private static final String EXTENSION = "rr";
  public static final int LEN = 2048;
  public static final int CHUNK_LEN = LEN / 16;
  private static final int MASK = LEN - 1;
  private static final int SHIFT = (int) (Math.log(LEN) / Math.log(2));
  public static final int OFFSET = -LEN / 2;
  public static final int CHUNK_OFFSET = OFFSET / 16;

  public final short x;
  public final short y;
  private final Road[] data;

  public RoadRegion(final short x, final short y, final Road[] data) {
    this.x = x;
    this.y = y;
    this.data = data;
  }

  public boolean containsPoint(final int x, final int y) {
    final int aX = getAbsoluteCoord(this.x);
    final int aY = getAbsoluteCoord(this.y);
    return x >= aX && x < (aX + LEN) && y >= aY && y < (aY + LEN);
  }

  public Road[] getData() {
    return this.data;
  }

  public void saveToDisk(final long seed) {
    try (final ByteWriter bw = new ByteWriter(getOutputFile(seed, this.x, this.y))) {
      this.writeTo(bw);
    } catch (final IOException e) {
      System.err.printf("Error saving region (%s, %s) to disk\n", this.x, this.y);
      e.printStackTrace();
    }
  }

  private static File getOutputFile(final long seed, final int x, final int y) {
    final File f = new File(TEMP_SAVE_DIR, String.format("%s/%sx%s.%s", seed, x, y, EXTENSION));
    try {
      FileUtils.forceMkdir(f.getParentFile());
    } catch (final IOException e) {
      e.printStackTrace(); // this exception will get handled down the line
    }
    return f;
  }

  private void writeTo(final ByteWriter bw) throws IOException {
    bw.writeInt16(this.x);
    bw.writeInt16(this.y);
    bw.writeInt32(this.data.length);
    for (final Road road : this.data) {
      road.writeToStream(bw);
    }
  }

  public static RoadRegion loadFromDisk(final long seed, final int x, final int y) {
    try (final ByteReader br = new ByteReader(getOutputFile(seed, x, y))) {
      return readFrom(br);
    } catch (final FileNotFoundException fnf) {
      return null;
    } catch (final IOException e) {
      System.err.printf("Error loading region (%s, %s) from disk\n", x, y);
      e.printStackTrace();
      return null;
    }
  }

  private static RoadRegion readFrom(final ByteReader br) throws IOException {
    final short x = br.readInt16();
    final short y = br.readInt16();
    final int len = br.readInt32();
    final Road[] data = new Road[len];
    for (int i = 0; i < len; i++) {
      data[i] = Road.fromReader(br);
    }
    return new RoadRegion(x, y, data);
  }

  public static void deleteAllRegions() {
    try {
      FileUtils.forceDelete(new File(TEMP_SAVE_DIR));
    } catch (final IOException e) {
      System.err.println("Error cleaning region files");
      e.printStackTrace();
    } catch (final NullPointerException ignored) {
      // nothing to delete
    }
  }

  public static short getRelativeCoord(final int c) {
    return (short) ((c + OFFSET) & MASK);
  }

  public static short getRegionCoord(final int c) {
    return (short) ((c - OFFSET) >> SHIFT);
  }

  public static int getRegionOffset(final int c) {
    return (getRegionCoord(c) * LEN) + OFFSET;
  }

  public static int getAbsoluteCoord(final int c) {
    return (c * LEN) + OFFSET;
  }

  public static int getFirstChunk(final int c) {
    return (getRegionCoord(c) * CHUNK_LEN) + CHUNK_OFFSET;
  }

  public static int toChunkCoord(final int c) {
    return (c * CHUNK_LEN) + CHUNK_OFFSET;
  }
}
