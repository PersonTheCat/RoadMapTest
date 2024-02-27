package personthecat.roadmap;

import java.io.IOException;

public class RoadVertex {
  public static final short START = 1;
  public static final short END = 1 << 1;
  public static final short MIDPOINT = 1 << 2;
  public static final short INTERSECTION = 1 << 3;

  public final short relX;
  public final short relY;
  public byte radius;
  public int color;
  public float integrity;
  public short flags;

  public RoadVertex(
      final short relX,
      final short relY,
      final byte radius,
      final int color,
      final float integrity,
      final short flags) {
    this.relX = relX;
    this.relY = relY;
    this.radius = radius;
    this.color = color;
    this.integrity = integrity;
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
        br.readInt16(),
        br.readInt16(),
        br.read(),
        br.readInt32(),
        br.readFloat32(),
        br.readInt16());
  }

  public void writeToStream(final ByteWriter bw) throws IOException {
    bw.writeInt16(this.relX);
    bw.writeInt16(this.relY);
    bw.write(this.radius);
    bw.writeInt32(this.color);
    bw.writeFloat32(this.integrity);
    bw.writeInt16(this.flags);
  }
}
