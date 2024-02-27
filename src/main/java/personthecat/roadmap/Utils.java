package personthecat.roadmap;

import java.util.Random;

public final class Utils {

  private Utils() {}

  public static void printMemUsage(final String event) {
    final Runtime rt = Runtime.getRuntime();
    final double used = (double) ((rt.totalMemory() - rt.freeMemory()) / 1000) / 1000;
    final long available = rt.totalMemory() / 1000 / 1000;
    System.out.printf("%s: %smb / %smb\n", event, used, available);
  }

  public static void setFeatureSeed(
      final Random rand, final long base, final int cX, final int cY) {
    rand.setSeed(base + (long) cX + (10000L * cY));
  }

  public static long getFeatureSeed(final long base, final int cX, final int cY) {
    return base + (long) cX + (10000L * cY);
  }
}
