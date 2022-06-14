package personthecat.roadmap;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class RoadMap {

    // This data structure is far from ideal and will change
    private final Object2ObjectMap<Point, RoadVertex[]> map = new Object2ObjectOpenHashMap<>();
    private static final RoadVertex[] EMPTY = new RoadVertex[0];

    public void put(final Point pos, final RoadVertex... vertices) {
        this.map.compute(pos, (p, a) -> a == null ? vertices : this.append(a, vertices));
    }

    private RoadVertex[] append(final RoadVertex[] a1, final RoadVertex[] a2) {
        final RoadVertex[] out = new RoadVertex[a1.length + a2.length];
        System.arraycopy(a1, 0, out, 0, a1.length);
        System.arraycopy(a2, 0, out, a1.length, a2.length);
        return out;
    }

    public RoadVertex[] get(final int x, final int y) {
        return this.map.getOrDefault(new Point(x, y), EMPTY);
    }
}
