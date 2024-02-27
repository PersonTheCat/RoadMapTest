package personthecat.roadmap.gen;

@SuppressWarnings("ClassCanBeRecord")
public class Point {
  public final int x;
  public final int y;

  public Point(final int x, final int y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public int hashCode() {
    return 31 * this.x + this.y;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Point p) {
      return this.x == p.x && this.y == p.y;
    }
    return false;
  }

  @Override
  public String toString() {
    return "(" + this.x + "," + this.y + ")";
  }
}