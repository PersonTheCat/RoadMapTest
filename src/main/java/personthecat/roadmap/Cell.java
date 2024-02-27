package personthecat.roadmap;

public class Cell {
  public int pX;
  public int pY;
  public double f;
  public double g;
  public double h;

  public Cell() {
    this(0, 0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  public Cell(final int pX, final int pY, final double f, final double g, final double h) {
    this.pX = pX;
    this.pY = pY;
    this.f = f;
    this.g = g;
    this.h = h;
  }
}
