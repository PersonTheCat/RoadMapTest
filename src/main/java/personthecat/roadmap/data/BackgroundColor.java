package personthecat.roadmap.data;

import java.awt.Color;
import java.util.stream.Stream;

public enum BackgroundColor {
  RED(Color.RED),
  GREEN(Color.GREEN),
  BLUE(Color.BLUE),
  WHITE(Color.WHITE),
  BLACK(Color.BLACK),
  GRAY(Color.GRAY),
  YELLOW(Color.YELLOW),
  ORANGE(Color.ORANGE),
  PINK(Color.PINK),
  PURPLE(new Color(255, 0, 255)),
  MAGENTA(Color.MAGENTA),
  CYAN(Color.CYAN);

  private final Color color;

  BackgroundColor(final Color color) {
    this.color = color;
  }

  public static BackgroundColor from(final String color) {
    return valueOf(color.toUpperCase());
  }

  public static String format(final Color color) {
    return Stream.of(values())
        .filter(c -> c.color.equals(color))
        .findFirst()
        .map(BackgroundColor::name)
        .orElse(null);
  }

  public Color get() {
    return this.color;
  }
}
