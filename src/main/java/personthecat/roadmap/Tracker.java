package personthecat.roadmap;

public class Tracker {

    private int xOffset = 0;
    private int yOffset = 0;
    private int prevXOffset = 0;
    private int prevYOffset = 0;
    private int seed = 0;
    private boolean sideView;
    private boolean mountains;
    private float zoom;

    public Tracker(final Config config) {
        this.zoom = config.getZoom();
        this.sideView = config.isSideView();
        this.mountains = config.isMountains();
    }

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

    public void setSideView(final boolean sideView) {
        this.sideView = sideView;
    }

    public void toggleSideView() {
        this.sideView = !this.sideView;
    }

    public boolean isMountains() {
        return this.mountains;
    }

    public void setMountains(final boolean mountains) {
        this.mountains = mountains;
    }

    public void toggleMountains() {
        this.mountains = !this.mountains;
    }

    public float getZoom() {
        return this.zoom;
    }

    public void setZoom(final float zoom) {
        this.zoom = zoom;
    }

    public void zoomIn() {
        this.zoom += 0.05;
    }

    public void zoomOut() {
        this.zoom -= 0.05;
    }

    public void reset() {
        this.prevXOffset = this.xOffset;
        this.prevYOffset = this.yOffset;
    }
}
