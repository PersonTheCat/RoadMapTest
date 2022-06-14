package personthecat.roadmap;

import java.util.List;

public abstract class RoadGenerator {

    protected final int distance;
    protected final float jitter;
    protected final int depth;

    protected RoadGenerator(final int distance, final float jitter, final int depth) {
        this.distance = distance;
        this.jitter = jitter;
        this.depth = depth;
    }

    public abstract List<RoadVertex> generateRoads();
}
