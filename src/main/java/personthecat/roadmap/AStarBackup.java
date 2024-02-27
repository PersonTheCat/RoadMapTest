package personthecat.roadmap;

import java.util.BitSet;
import java.util.NavigableMap;
import java.util.TreeMap;

public class AStarBackup {
  protected final BitSet closedSet;
  protected final NavigableMap<Double, Point> openList = new TreeMap<>();
  protected final Cell[][] cellDetails;
  protected final float[][] grid;
  protected final Point src;
  protected final Point dest;
  protected final int h;
  protected final int w;

  public AStarBackup(final float[][] grid, final Point src, final Point dest) {
    this.grid = grid;
    this.src = new Point((src.x | 1) + 1, (src.y | 1) + 1);
    this.dest = new Point((dest.x | 1) + 1, (dest.y | 1) + 1);
    this.h = grid.length;
    this.w = grid[0].length;
    this.closedSet = new BitSet(this.h * this.w);
    this.cellDetails = new Cell[this.w][this.h];
  }

  public Cell[][] search() {
    int x = this.src.x;
    int y = this.src.y;

    this.cellDetails[x][y] = new Cell(x, y, 0, 0, 0);
    this.openList.put(0.0, new Point(x, y));

    while (this.hasNext()) {
      final Point p = this.next();
      x = p.x;
      y = p.y;
      this.close(x, y);

      if (this.checkDirection(x, y, x - 2, y)
          || this.checkDirection(x, y, x + 2, y)
          || this.checkDirection(x, y, x , y + 2)
          || this.checkDirection(x, y, x, y - 2)
          || this.checkDirection(x, y, x - 2, y + 2)
          || this.checkDirection(x, y, x - 2, y - 2)
          || this.checkDirection(x, y, x + 2, y + 2)
          || this.checkDirection(x, y, x + 2, y - 2)) {
        return this.cellDetails;
      }
    }
    return null;
  }

  private boolean hasNext() {
    return !this.openList.isEmpty();
  }

  private Point next() {
    return this.openList.pollFirstEntry().getValue();
  }

  private void close(final int x, final int y) {
    this.closedSet.set(x * this.h + y);
  }

  private boolean checkDirection(final int pX, final int pY, final int x, final int y) {
    if (this.isValid(x, y)) {
      if (isDestination(x, y, this.dest)) {
        this.setParentIndex(x, y, pX, pY);
        return true;
      }
      final float sH = this.grid[pX][pY];
      final float eH = this.grid[x][y];
      final float dH =  Math.abs(sH - eH);
      if (!this.isClosed(x, y) && eH >= 0 && dH < 3) {
        // todo: 1 is distance from previous point (don't hard code)
        final double gNew = getG(this.cellDetails[x][y]) + 1;
        final double hNew = calculateHValue(x, y, this.dest);
        double fNew = gNew + hNew + (dH * dH) * 2;
        if (eH < 20) {
          fNew += (20 - eH) * (20 - eH);
        }

        final Cell cell = this.cellDetails[x][y];
        if (cell == null || cell.f == Double.POSITIVE_INFINITY || cell.f > fNew) {
          this.openList.put(fNew, new Point(x, y));
          this.cellDetails[x][y] = new Cell(pX, pY, fNew, gNew, hNew);
        }
      }
    }
    return false;
  }

  protected boolean isValid(final int x, final int y) {
    return x >= 0 && x < this.w && y >= 0 && y < this.h;
  }

  private void setParentIndex(final int x, final int y, final int pX, final int pY) {
    Cell cell = this.cellDetails[x][y];
    if (cell == null) {
      cell = this.cellDetails[x][y] = new Cell();
    }
    cell.pX = pX;
    cell.pY = pY;
  }

  private boolean isClosed(final int x, final int y) {
    return this.closedSet.get(x * this.h + y);
  }

//  private List<Point> tracePath() {
//    int x = this.dest.x;
//    int y = this.dest.y;
//
//    final List<Point> path = new ArrayList<>();
//    while (!(this.cellDetails[x][y].pX == x && this.cellDetails[x][y].pY == y)) {
//      path.add(new Point(x, y));
//      x = this.cellDetails[x][y].pX;
//      y = this.cellDetails[x][y].pY;
//    }
//    path.add(new Point(x, y));
//    return path;
//  }

  protected static boolean isDestination(final int x, final int y, final Point dest) {
//    return x == dest.x && y == dest.y;
    return Math.abs(x - dest.x) < 2 && Math.abs(y - dest.y) < 2;
  }

  protected static double getG(final Cell cell) {
    return cell != null ? cell.g : 0;
  }

  protected static double calculateHValue(final int x, final int y, final Point dest) {
    final double d = Math.sqrt((x - dest.x) * (x - dest.x) + (y - dest.y) * (y - dest.y));
//    return Math.sin(d * 5) * 5 + d;
//    return Math.sin(x * y) * 10 + d;
    return d;
  }
}
