package personthecat.roadmap.gen.road;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import personthecat.roadmap.Config;
import personthecat.roadmap.gen.HeightmapGenerator;
import personthecat.roadmap.data.Point;
import personthecat.roadmap.data.SmoothnessGraph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class AStar {
  protected final PriorityQueue<Candidate> openList;
  protected final BitSet closedSet;
  protected final Long2ObjectMap<Cell> cellDetails;
  protected HeightmapGenerator gen;
  protected SmoothnessGraph smoothness;
  protected final Config config;
  protected Point found;

  public AStar(final Config config, final SmoothnessGraph smoothness) {
    this.openList = new PriorityQueue<>(Comparator.comparingDouble(c -> c.priority));
    this.closedSet = new BitSet(RoadRegion.LEN / 2 * RoadRegion.LEN / 2);
    this.cellDetails = new Long2ObjectOpenHashMap<>(Road.MAX_DISTANCE);
    this.smoothness = smoothness;
    this.config = config;
  }

  public void reset(final HeightmapGenerator gen) {
    this.openList.clear();
    this.closedSet.clear();
    this.cellDetails.clear();
    this.gen = gen;
  }

  public final List<Point> search(final Point src, final Destination dest) {
//    sw.logStart("generating road: %s -> %s, d = %.2f", src, dest, d);
//    sw.logEnd("generated road: %s -> %s, len = %s", src, dest, len);
//    sw.logEnd("generated nothing: %s -> %s, len = %s", src, dest, len);
//    sw.logAverage("path avg");
    return this.doSearch(src, dest);
  }

  protected List<Point> doSearch(final Point src, final Destination dest) {
    int x = src.x;
    int y = src.y;
    float n = this.gen.sample(x, y);

    this.setDetails(x, y, new Cell(x, y, 0, 0));
    this.open(0.0, x, y, n);

    int len = 0;
    while (this.hasNext() && len++ < Road.MAX_LENGTH) {
      final Candidate c = this.next();
      x = c.x;
      y = c.y;
      n = c.n;
      this.close(x, y);

      if (this.checkDirection(dest, n, 2, x, y, x - 2, y)
          || this.checkDirection(dest, n, 2, x, y, x + 2, y)
          || this.checkDirection(dest, n, 2, x, y, x , y + 2)
          || this.checkDirection(dest, n, 2, x, y, x, y - 2)
          || this.checkDirection(dest, n, 2.83, x, y, x - 2, y + 2)
          || this.checkDirection(dest, n, 2.83, x, y, x - 2, y - 2)
          || this.checkDirection(dest, n, 2.83, x, y, x + 2, y + 2)
          || this.checkDirection(dest, n, 2.83, x, y, x + 2, y - 2)) {
        return this.tracePath();
      }
    }
    return null;
  }

  private Cell getDetails(final int x, final int y) {
    return this.cellDetails.get((((long) x) << 32) | (y & 0xFFFFFFFFL));
  }

  private void setDetails(final int x, final int y, final Cell cell) {
    this.cellDetails.put((((long) x) << 32) | (y & 0xFFFFFFFFL), cell);
  }

  private void open(final double priority, final int x, final int y, final float n) {
    this.openList.offer(new Candidate(priority, x, y, n));
  }

  private boolean hasNext() {
    return !this.openList.isEmpty();
  }

  private Candidate next() {
    return this.openList.poll();
  }

  private void close(final int x, final int y) {
    final int xO = RoadRegion.absToRel(x + RoadRegion.OFFSET) / 2;
    final int yO = RoadRegion.absToRel(y + RoadRegion.OFFSET) / 2;
    this.closedSet.set(xO * (RoadRegion.LEN / 2) + yO);
  }

  private boolean isClosed(final int x, final int y) {
    final int xO = RoadRegion.absToRel(x + RoadRegion.OFFSET) / 2;
    final int yO = RoadRegion.absToRel(y + RoadRegion.OFFSET) / 2;
    return this.closedSet.get(xO * (RoadRegion.LEN / 2) + yO);
  }

  private boolean checkDirection(Destination dest, float sH, double d, int pX, int pY, int x, int y) {
    final double h = dest.distance(x, y, 2);
    if (h < 2) {
      this.setParentIndex(x, y, pX, pY);
      this.found = new Point(x, y);
      return true;
    }
    if (this.isClosed(x, y)) {
      return false;
    }
    final float eH = this.gen.sample(x, y);
    if (eH < 0) {
      return false;
    }
    final float dH = Math.abs(sH - eH);
    if (dH < 2) {
      final Cell cell = this.getDetails(x, y);
      final double g = getG(cell) + d;
      final double r = getCurve(x, y);
      final double sd = this.smoothness.getSd(this.gen, x, y);
      double f = g + h + r + (dH * dH) * 3 + sd * 2;
      final int minCutoff = this.config.getShorelineCutoff();
      final int maxCutoff = this.config.getMountainCutoff();
      if (eH < minCutoff) {
        f += (minCutoff - eH) * (minCutoff - eH);
      } else if (eH > maxCutoff) {
        f += (eH - maxCutoff) * (eH - maxCutoff);
      }
      if (cell == null || cell.f > f) {
        this.open(f, x, y, eH);
        this.setDetails(x, y, new Cell(pX, pY, f, g));
      }
    }
    return false;
  }

  private void setParentIndex(final int x, final int y, final int pX, final int pY) {
    Cell cell = this.getDetails(x, y);
    if (cell == null) {
      cell = new Cell();
      this.setDetails(x, y, cell);
    }
    cell.pX = pX;
    cell.pY = pY;
  }

  protected List<Point> tracePath() {
    int x = this.found.x;
    int y = this.found.y;
    Cell cell = this.getDetails(x, y);

    final List<Point> path = new ArrayList<>();
    while (!(cell.pX == x && cell.pY == y)) {
      path.add(new Point(x, y));
      x = cell.pX;
      y = cell.pY;
      cell = getDetails(x, y);
    }
    path.add(new Point(x, y));
    return path;
  }

  protected static double getG(final Cell cell) {
    return cell != null ? cell.g : 0;
  }

  protected static double getCurve(final int x, final int y) { // will take: dest, h
    return Math.sin(x * y);
  }

  private record Candidate(double priority, int x, int y, float n) {}

  private static class Cell {
    int pX;
    int pY;
    double f;
    double g;

    Cell() {}

    Cell(final int pX, final int pY, final double f, final double g) {
      this.pX = pX;
      this.pY = pY;
      this.f = f;
      this.g = g;
    }
  }
}
