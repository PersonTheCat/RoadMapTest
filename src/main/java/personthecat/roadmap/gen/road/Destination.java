package personthecat.roadmap.gen.road;

public interface Destination {
  double distance(final int x, final int y, final double min);
  byte getRoadLevel();
}
