package personthecat.roadmap;

import java.awt.Color;
import java.util.Map;

public enum BackgroundColor {
  RED,
  GREEN,
  BLUE,
  WHITE,
  BLACK,
  YELLOW,
  PURPLE,
  CYAN;

  private static final Color PURPLE_COLOR = new Color(255, 0, 255);

  private static final Map<Color, BackgroundColor> INVERSE_MAP =
      Map.of(
          Color.RED, RED,
          Color.GREEN, GREEN,
          Color.BLUE, BLUE,
          Color.WHITE, WHITE,
          Color.BLACK, BLACK,
          Color.YELLOW, YELLOW,
          PURPLE_COLOR, PURPLE,
          Color.CYAN, CYAN);

  public static BackgroundColor from(final String color) {
    return valueOf(color.toUpperCase());
  }

  public static String format(final Color color) {
    final var bgColor = INVERSE_MAP.get(color);
    return bgColor != null ? bgColor.name() : null;
  }

  public Color get() {
    return switch (this) {
      case RED -> Color.RED;
      case GREEN -> Color.GREEN;
      case BLUE -> Color.BLUE;
      case WHITE -> Color.WHITE;
      case BLACK -> Color.BLACK;
      case YELLOW -> Color.YELLOW;
      case PURPLE -> PURPLE_COLOR;
      case CYAN -> Color.CYAN;
    };
  }
}
