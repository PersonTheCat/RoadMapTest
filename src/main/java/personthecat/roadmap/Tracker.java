package personthecat.roadmap;

public class Tracker {

    private int xOffset = 0;
    private int yOffset = 0;
    private int prevXOffset = 0;
    private int prevYOffset = 0;
    private int seed = 0;
    private float zoom = 0;
    private boolean sideView = false;

    public Tracker() {}

    public void up(final int count) {
        this.yOffset -= 32 * count;
    }

    public void down(final int count) {
        this.yOffset += 32 * count;
    }

    public void left(final int count) {
        this.xOffset -= 32 * count;
    }

    public void right(final int count) {
        this.xOffset += 32 * count;
    }

    public int getYOffset() {
        return this.yOffset;
    }

    public int getXOffset() {
        return this.xOffset;
    }

    public int getPrevXOffset() {
        return this.prevXOffset;
    }

    public int getPrevYOffset() {
        return this.prevYOffset;
    }

    public int getSeed() {
        return this.seed;
    }

    public void setSeed(final int seed) {
        this.seed = seed;
    }

    public boolean isSideView() {
        return this.sideView;
    }

    public void toggleSideView() {
        this.sideView = !this.sideView;
    }

    public float getZoom() {
        return this.zoom;
    }

    public void zoomIn() {
        this.zoom += 0.05;
    }

    public void zoomOut() {
        this.zoom -= 0.05;
    }

    public void reload(final Config config) {
        this.sideView = config.isSideView();
        this.zoom = config.getZoom();
    }

    public void reset() {
        this.prevXOffset = this.xOffset;
        this.prevYOffset = this.yOffset;
    }
}
