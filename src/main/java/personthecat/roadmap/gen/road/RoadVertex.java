package personthecat.roadmap.gen.road;

import personthecat.roadmap.io.ByteReader;
import personthecat.roadmap.io.ByteWriter;

import java.io.IOException;

public class RoadVertex {
  public static final byte MAX_RADIUS = 8;
  public static final short START = 1;
  public static final short END = 1 << 1;
  public static final short MIDPOINT = 1 << 2;
  public static final short INTERSECTION = 1 << 3;
  public static final short BEND = 1 << 4;
  public static final short TEST = 1 << 5;
  public static final short TEST_2 = 1 << 6;

  public final int x;
  public final int y;
  public byte radius;
  public int color;
  public float integrity;
  public float theta;
  public float xAngle;
  public short flags;

  public RoadVertex(int x, int y, byte radius, int color, float integrity, float theta, float xAngle, short flags) {
    this.x = x;
    this.y = y;
    this.radius = radius;
    this.color = color;
    this.integrity = integrity;
    this.theta = theta;
    this.xAngle = xAngle;
    this.flags = flags;
  }

  public boolean hasFlag(final short flag) {
    return (this.flags & flag) == flag;
  }

  public void addFlag(final short flag) {
    this.flags |= flag;
  }

  public void removeFlag(final short flag) {
    this.flags &= ~flag;
  }

  public static RoadVertex fromReader(final ByteReader br) throws IOException {
    return new RoadVertex(
        br.readInt32(),
        br.readInt32(),
        br.read(),
        br.readInt32(),
        br.readInt16() / 1_000F,
        br.readInt16() / 1_000F,
        br.readInt16() / 1_000F,
        br.readInt16());
  }

  public void writeToStream(final ByteWriter bw) throws IOException {
    bw.writeInt32(this.x);
    bw.writeInt32(this.y);
    bw.write(this.radius);
    bw.writeInt32(this.color);
    bw.writeInt16((short) (this.integrity * 1_000F));
    bw.writeInt16((short) (this.theta * 1_000F));
    bw.writeInt16((short) (this.xAngle * 1_000F));
    bw.writeInt16(this.flags);
  }

  @Override
  public String toString() {
    return "Vertex[" + this.x + ',' + this.y + ']';
  }
}
