package personthecat.roadmap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class AStar {
  protected final NavigableMap<Double, Point> openList;
  protected final BitSet closedSet;
  protected final Map<Point, Cell> cellDetails;
  protected HeightmapGenerator gen;
  protected final Config config;

  public AStar(final Config config) {
    this.openList = new TreeMap<>();
    this.closedSet = new BitSet(RoadRegion.LEN / 2 * RoadRegion.LEN / 2);
    this.cellDetails = new HashMap<>(Road.MAX_LENGTH);
    this.config = config;
  }

  public void reset(final HeightmapGenerator gen) {
    this.openList.clear();
    this.closedSet.clear();
    this.cellDetails.clear();
    this.gen = gen;
  }

  public List<Point> search(final Point src, Point dest) {
    int x = src.x;
    int y = src.y;

    this.setDetails(x, y, new Cell(x, y, 0, 0, 0));
    this.open(0.0, x, y);

    while (this.hasNext()) {
      final Point p = this.next();
      x = p.x;
      y = p.y;
      this.close(x, y);

      if (this.checkDirection(dest, 2, x, y, x - 2, y)
          || this.checkDirection(dest, 2, x, y, x + 2, y)
          || this.checkDirection(dest, 2, x, y, x , y + 2)
          || this.checkDirection(dest, 2, x, y, x, y - 2)
          || this.checkDirection(dest, 2.83, x, y, x - 2, y + 2)
          || this.checkDirection(dest, 2.83, x, y, x - 2, y - 2)
          || this.checkDirection(dest, 2.83, x, y, x + 2, y + 2)
          || this.checkDirection(dest, 2.83, x, y, x + 2, y - 2)) {
        return this.tracePath(dest);
      }
    }
    return null;
  }

  private Cell getDetails(final int x, final int y) {
    return this.cellDetails.get(new Point(x, y));
  }

  private void setDetails(final int x, final int y, final Cell cell) {
    this.cellDetails.put(new Point(x, y), cell);
  }

  private void open(final double priority, final int x, final int y) {
    this.openList.put(priority, new Point(x, y));
  }

  private boolean hasNext() {
    return !this.openList.isEmpty();
  }

  private Point next() {
    return this.openList.pollFirstEntry().getValue();
  }

  private void close(final int x, final int y) {
    final int xO = RoadRegion.getRelativeCoord(x + RoadRegion.OFFSET) / 2;
    final int yO = RoadRegion.getRelativeCoord(y + RoadRegion.OFFSET) / 2;
    this.closedSet.set(xO * (RoadRegion.LEN / 2) + yO);
  }

  private boolean isClosed(final int x, final int y) {
    final int xO = RoadRegion.getRelativeCoord(x + RoadRegion.OFFSET) / 2;
    final int yO = RoadRegion.getRelativeCoord(y + RoadRegion.OFFSET) / 2;
    return this.closedSet.get(xO * (RoadRegion.LEN / 2) + yO);
  }

  private boolean checkDirection(Point dest, double d, int pX, int pY, int x, int y) {
    if (isDestination(x, y, dest)) {
      this.setParentIndex(dest.x, dest.y, pX, pY);
      return true;
    }
    final float sH = this.gen.sample(pX, pY);
    final float eH = this.gen.sample(x, y);
    final float dH =  Math.abs(sH - eH);
    if (!this.isClosed(x, y) && eH >= 0 && dH < 3) {
      final Cell cell = this.getDetails(x, y);
      final double gNew = getG(cell) + d;
      final double hNew = calculateHValue(x, y, dest);
      double fNew = gNew + hNew + (dH * dH) * 2;
      final int cutoff = this.config.getShorelineCutoff();
      if (eH < cutoff) {
        fNew += (cutoff - eH) * (cutoff - eH);
      }
      if (cell == null || cell.f == Double.POSITIVE_INFINITY || cell.f > fNew) {
        this.open(fNew, x, y);
        this.setDetails(x, y, new Cell(pX, pY, fNew, gNew, hNew));
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

  protected List<Point> tracePath(final Point dest) {
    int x = dest.x;
    int y = dest.y;
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

  protected static boolean isDestination(final int x, final int y, final Point dest) {
    return Math.abs(x - dest.x) < 2 && Math.abs(y - dest.y) < 2;
  }

  protected static double getG(final Cell cell) {
    return cell != null ? cell.g : 0;
  }

  protected static double calculateHValue(final int x, final int y, final Point dest) {
    final double d = Math.sqrt((x - dest.x) * (x - dest.x) + (y - dest.y) * (y - dest.y));
    return Math.sin(x * y) * 5 + d;
  }
}
