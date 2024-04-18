package personthecat.roadmap.gen.road;

import personthecat.roadmap.util.Utils;
import personthecat.roadmap.io.ByteReader;
import personthecat.roadmap.io.ByteWriter;

import java.io.IOException;

public record Road(byte level, int minX, int minY, int maxX, int maxY, RoadVertex[] vertices) {
  public static final int MAX_DISTANCE = RoadRegion.LEN / 2;
  public static final int MAX_LENGTH = RoadRegion.LEN;
  public static final int STEP = 2;
  public static final int PADDING = 32; // assume vertices will never exit these bounds

  public int length() {
    return this.vertices.length * STEP;
  }

  public int distance() {
    return (int) Math.sqrt(
        (this.minX - this.maxX) * (this.minX - this.maxX)
            + (this.minY - this.maxY) * (this.minY - this.maxY));
  }

  public float broadAngle() {
    final RoadVertex s = this.vertices[0];
    final RoadVertex e = this.vertices[this.vertices.length - 1];
    return (float) Math.atan2(e.y - s.y, e.x - s.x);
  }

  public boolean containsPoint(final int x, final int t) {
    return x > this.minX - PADDING && x < this.maxX + PADDING && t > this.minY - PADDING && t < this.maxY + PADDING;
  }

  public boolean isInRegion(final short rX, final short rY) {
    final int x1a = RoadRegion.regionToAbs(rX);
    final int y1a = RoadRegion.regionToAbs(rY);
    return Utils.rectanglesOverlap(x1a, y1a, x1a + RoadRegion.LEN, y1a + RoadRegion.LEN, this.minX, this.minY, this.maxX, this.maxY);
  }

  public RoadVertex first() {
    return this.vertices[0];
  }

  public RoadVertex last() {
    return this.vertices[this.vertices.length - 1];
  }

  public boolean isBranch() {
    return this.vertices[0].hasFlag(RoadVertex.INTERSECTION);
  }

  public static Road fromReader(final ByteReader reader) throws IOException {
    final byte level = reader.read();
    final int minX = reader.readInt32();
    final int minY = reader.readInt32();
    final int maxX = reader.readInt32();
    final int maxY = reader.readInt32();
    final int numVertices = reader.readInt32();
    final RoadVertex[] vertices = new RoadVertex[numVertices];
    for (int i = 0; i < numVertices; i++) {
      vertices[i] = RoadVertex.fromReader(reader);
    }
    return new Road(level, minX, minY, maxX, maxY, vertices);
  }

  public void writeTo(final ByteWriter writer) throws IOException {
    writer.write(this.level);
    writer.writeInt32(this.minX);
    writer.writeInt32(this.minY);
    writer.writeInt32(this.maxX);
    writer.writeInt32(this.maxY);
    writer.writeInt32(this.vertices.length);
    for (final RoadVertex vertex : this.vertices) {
      vertex.writeToStream(writer);
    }
  }

  @Override
  public String toString() {
    if (this.vertices.length == 0) {
      return "Road[?,?]";
    }
    final RoadVertex o = this.vertices()[0];
    return "Road[" + o.x + ',' + o.y + ']';
  }
}
