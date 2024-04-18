package personthecat.roadmap.gen.road;

import org.apache.commons.io.FileUtils;
import personthecat.roadmap.data.Point;
import personthecat.roadmap.data.VertexGraph;
import personthecat.roadmap.io.ByteReader;
import personthecat.roadmap.io.ByteWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// todo: figure out how and whether to save networks in regions again
public class RoadNetwork implements Iterable<Road> {
  private static final String TEMP_SAVE_DIR = "networks";
  private static final String EXTENSION = "rn";
  private static final int PADDING = Road.PADDING;

  public final int minX;
  public final int maxX;
  public final int minY;
  public final int maxY;
  public final List<Road> roads;
  public final VertexGraph graph;

  public RoadNetwork(final List<Road> roads, final VertexGraph graph) {
    int minX = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxY = Integer.MIN_VALUE;
    for (final Road r : roads) {
      minX = Math.min(minX, r.minX());
      maxX = Math.max(maxX, r.maxX());
      minY = Math.min(minY, r.minY());
      maxY = Math.max(maxY, r.maxY());
    }
    this.roads = roads;
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;
    this.graph = graph;
  }

  private RoadNetwork(int minX, int maxX, int minY, int maxY, List<Road> roads, VertexGraph graph) {
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;
    this.roads = roads;
    this.graph = graph;
  }

  public boolean containsPoint(final int x, final int t) {
    return x > this.minX - PADDING && x < this.maxX + PADDING && t > this.minY - PADDING && t < this.maxY + PADDING;
  }

  public boolean isInQuad(final short qX, final short qY) {
    final RoadVertex o = this.origin();
    final int aX1 = RoadRegion.quadToAbsolute(qX);
    final int aY1 = RoadRegion.quadToAbsolute(qY);
    final int aX2 = aX1 + RoadRegion.QUAD_BLOCKS;
    final int aY2 = aY1 + RoadRegion.QUAD_BLOCKS;
    return o.x >= aX1 && o.x < aX2 && o.y >= aY1 && o.y < aY2;
  }

  public double distanceFrom(final int x, final int y, final double min) {
    return this.graph.distance(x, y, min);
  }

  public double checkDistance(final int x, final int y, final double min) {
    if (this.containsPoint(x, y)) {
      return this.distanceFrom(x, y, min);
    }
    return Double.MAX_VALUE;
  }

  public boolean isInRegion(final short rX, final short rY) {
    return this.getMainRoad().isInRegion(rX, rY);
  }

  public RoadVertex origin() {
    return this.getMainRoad().vertices()[0];
  }

  public RoadVertex dest() {
    final RoadVertex[] r0 = this.getMainRoad().vertices();
    return r0[r0.length - 1];
  }

  public Road getMainRoad() {
    if (this.roads.isEmpty()) {
      throw new IllegalStateException("Network regenerated empty");
    }
    final Road r0 = this.roads.get(0);
    if (r0.vertices().length == 0) {
      throw new IllegalStateException("Road generated empty");
    }
    return r0;
  }

  @Override
  public Iterator<Road> iterator() {
    return this.roads.iterator();
  }

  public void saveToDisk(final long seed) {
    final RoadVertex o = this.origin();
    try (final ByteWriter bw = new ByteWriter(getOutputFile(seed, o.x, o.y))) {
      this.writeTo(bw);
    } catch (final IOException e) {
      System.err.printf("Error saving network %s to disk\n", new Point(o.x, o.y));
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

  public void writeTo(final ByteWriter writer) throws IOException {
    writer.writeInt32(this.minX);
    writer.writeInt32(this.maxX);
    writer.writeInt32(this.minY);
    writer.writeInt32(this.maxY);
    writer.writeInt32(this.roads.size());
    for (final Road road : this.roads) {
      road.writeTo(writer);
    }
    this.graph.writeTo(writer);
  }

  public static RoadNetwork loadFromDisk(final long seed, final int x, final int y) {
    try (final ByteReader br = new ByteReader(getOutputFile(seed, x, y))) {
      return fromReader(br);
    } catch (final FileNotFoundException fnf) {
      return null;
    } catch (final IOException e) {
      System.err.printf("Error loading network (%s,%s) from disk\n", x, y);
      e.printStackTrace();
      return null;
    }
  }

  private static RoadNetwork fromReader(final ByteReader reader) throws IOException {
    final int minX = reader.readInt32();
    final int maxX = reader.readInt32();
    final int minY = reader.readInt32();
    final int maxY = reader.readInt32();
    final int len = reader.readInt32();
    final List<Road> roads = new ArrayList<>(len);
    for (int i = 0; i < len; i++) {
      roads.add(Road.fromReader(reader));
    }
    final VertexGraph graph = VertexGraph.fromReader(reader);
    return new RoadNetwork(minX, maxX, minY, maxY, roads, graph);
  }

  public static void deleteAllNetworks() {
    try {
      FileUtils.forceDelete(new File(TEMP_SAVE_DIR));
    } catch (final IOException e) {
      System.err.println("Error cleaning road files");
      e.printStackTrace();
    } catch (final NullPointerException ignored) {
      // nothing to delete
    }
  }

  @Override
  public String toString() {
    if (this.roads.isEmpty() || this.roads.get(0).vertices().length == 0) {
      return "Network[?,?]";
    }
    final RoadVertex o = this.roads.get(0).vertices()[0];
    return "Network[" + o.x + ',' + o.y + ']';
  }
}
