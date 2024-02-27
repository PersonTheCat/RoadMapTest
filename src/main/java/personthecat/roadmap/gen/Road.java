package personthecat.roadmap.gen;

import personthecat.roadmap.io.ByteReader;
import personthecat.roadmap.io.ByteWriter;

import java.io.IOException;

public record Road(byte level, short minX, short minY, short maxX, short maxY, RoadVertex[] vertices) {
  public static final int MAX_DISTANCE = RoadRegion.LEN / 2;
  public static final int MAX_LENGTH = RoadRegion.LEN;
  public static final int STEP = 2;
  private static final int PADDING = 32; // assume vertices will never exit these bounds

  public boolean containsPoint(final short rX, final short rY, final int aX, final int aY) {
    final int aRX = RoadRegion.getAbsoluteCoord(rX);
    final int aRY = RoadRegion.getAbsoluteCoord(rY);
    final int x1 = aRX + this.minX;
    final int y1 = aRY + this.minY;
    final int x2 = aRX + this.maxX;
    final int y2 = aRY + this.maxY;
    return aX > x1 - PADDING && aX < x2 + PADDING && aY > y1 - PADDING && aY < y2 + PADDING;
  }

  public boolean isInRegionBounds() {
    return this.minX >= 0 || this.maxX < RoadRegion.LEN || this.minY >= 0 || this.maxY < RoadRegion.LEN;
  }

  public static Road fromReader(final ByteReader br) throws IOException {
    final byte level = br.read();
    final short minX = br.readInt16();
    final short minY = br.readInt16();
    final short maxX = br.readInt16();
    final short maxY = br.readInt16();
    final int numVertices = br.readInt32();
    final RoadVertex[] vertices = new RoadVertex[numVertices];
    for (int i = 0; i < numVertices; i++) {
      vertices[i] = RoadVertex.fromReader(br);
    }
    return new Road(level, minX, minY, maxX, maxY, vertices);
  }

  public void writeToStream(final ByteWriter bw) throws IOException {
    bw.write(this.level);
    bw.writeInt16(this.minX);
    bw.writeInt16(this.minY);
    bw.writeInt16(this.maxX);
    bw.writeInt16(this.maxY);
    bw.writeInt32(this.vertices.length);
    for (final RoadVertex vertex : this.vertices) {
      vertex.writeToStream(bw);
    }
  }
}
