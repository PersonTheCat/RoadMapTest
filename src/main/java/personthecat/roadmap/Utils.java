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
    rand.setSeed((long) cX * 341873128712L + (long) cY * 132897987541L + base);
  }

  public static long getFeatureSeed(final long base, final int cX, final int cY) {
    return (long) cX * 341873128712L + (long) cY * 132897987541L + base;
  }

  public static double stdDev(final double[] ds) {
    double sum = 0.0;
    for (final double d : ds) {
      sum += d;
    }
    final int len = ds.length;
    final double mean = sum / len;

    double stdDev = 0.0;
    for (double d : ds) {
      stdDev += Math.pow(d - mean, 2);
    }
    return Math.sqrt(stdDev / len);
  }
}
