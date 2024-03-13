package personthecat.roadmap.gen;

import personthecat.roadmap.Config;
import personthecat.roadmap.Utils;

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
    this.cellDetails = new HashMap<>(Road.MAX_DISTANCE);
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

    int len = 0;
    while (this.hasNext() && len++ < Road.MAX_LENGTH) {
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
    if (!this.isClosed(x, y) && eH >= 0 && dH < 2) {
      final Cell cell = this.getDetails(x, y);
      final double gNew = getG(cell) + d;
      final double hNew = calculateHValue(x, y, dest);
      final double r = getCurve(x, y);
      final double sd = Utils.stdDev(this.getSamples(x, y, eH));
      double fNew = gNew + hNew + r + (dH * dH) * 3 + sd * 2;
      final int minCutoff = this.config.getShorelineCutoff();
      final int maxCutoff = this.config.getMountainCutoff();
      if (eH < minCutoff) {
        fNew += (minCutoff - eH) * (minCutoff - eH);
      } else if (eH > maxCutoff) {
        fNew += (eH - maxCutoff) * (eH - maxCutoff);
      }
      if (cell == null || cell.f > fNew) {
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

  protected double[] getSamples(final int x, final int y, final double c) {
    return new double[] {
      c,
      this.gen.sample(x + 2, y),
      this.gen.sample(x, y + 2),
      this.gen.sample(x - 2, y),
      this.gen.sample(x, y - 2),
    };
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
    return Math.sqrt((x - dest.x) * (x - dest.x) + (y - dest.y) * (y - dest.y));
  }

  protected static double getCurve(final int x, final int y) { // will take: dest, h
    return Math.sin(x * y);
  }

  private static class Cell {
    int pX;
    int pY;
    double f;
    double g;
    double h;

    Cell() {}

    Cell(final int pX, final int pY, final double f, final double g, final double h) {
      this.pX = pX;
      this.pY = pY;
      this.f = f;
      this.g = g;
      this.h = h;
    }
  }
}
