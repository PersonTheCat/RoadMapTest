package personthecat.roadmap;

import java.util.Random;

public class Tracker {

    private int xOffset = 0;
    private int yOffset = 0;
    private int prevXOffset = 0;
    private int prevYOffset = 0;
    private int seed;
    private boolean sideView;
    private boolean mountains;
    private float zoom;
    private float sideViewAngle;
    private final History history = new History();

    public Tracker(final Config config) {
        this.seed = config.getSeed();
        this.xOffset = config.getXOffset();
        this.yOffset = config.getYOffset();
        this.sideView = config.isSideView();
        this.mountains = config.isMountains();
        this.zoom = config.getZoom();
        this.sideViewAngle = config.getSideViewAngle();
        this.history.recordHistory(this);
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

    public void setYOffset(final int yOffset) {
        this.yOffset = yOffset;
    }

    public int getXOffset() {
        return this.xOffset;
    }

    public void setXOffset(final int xOffset) {
        this.xOffset = xOffset;
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
        this.history.jumpToEnd();
        this.seed = seed;
        this.history.recordHistory(this);
    }

    public void nextSeed(final Random rand) {
        final HistoryItem item = this.history.next();
        if (item != null) {
            this.applyHistory(item);
            return;
        }
        this.seed = rand.nextInt();
        this.history.recordHistory(this);
    }

    public void previousSeed() {
        final HistoryItem item = this.history.previous();
        if (item != null) {
            this.applyHistory(item);
        }
    }

    private void applyHistory(final HistoryItem item) {
        this.seed = item.seed;
        this.xOffset = item.x;
        this.yOffset = item.y;
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

    public float getSideViewAngle() {
        return this.sideViewAngle;
    }

    public void setSideViewAngle(final float sideViewAngle) {
        this.sideViewAngle = sideViewAngle;
    }

    public void angleUp() {
        this.sideViewAngle += 0.05;
    }

    public void angleDown() {
        this.sideViewAngle -= 0.05;
    }

    public void reset() {
        this.prevXOffset = this.xOffset;
        this.prevYOffset = this.yOffset;
    }

    private static class History {
        private static final int HISTORY_SIZE = 10;

        private final HistoryItem[] items = new HistoryItem[HISTORY_SIZE];
        private int index = 0;
        private int count = 0;

        void recordHistory(final Tracker t) {
            this.items[this.index] = new HistoryItem(t.seed, t.xOffset, t.yOffset);
            this.count++;
        }

        HistoryItem next() {
            this.index++;
            if (this.index == HISTORY_SIZE) {
                this.shiftHistory();
            }
            return this.getItem();
        }

        HistoryItem previous() {
            this.index--;
            if (this.index < 0) {
                this.index = 0;
                return null;
            }
            return this.getItem();
        }

        HistoryItem getItem() {
            if (this.index < 0) {
                throw new IllegalStateException("History not recorded");
            } else if (this.index >= HISTORY_SIZE) {
                throw new IllegalStateException("History not culled");
            }
            return this.items[this.index];
        }

        void shiftHistory() {
            if (this.items.length > 0) {
                System.arraycopy(this.items, 1, this.items, 0, this.items.length - 1);
                this.items[this.items.length - 1] = null;
            }
            this.index--;
            this.count--;
        }

        void jumpToEnd() {
            if (this.count == HISTORY_SIZE) {
                this.shiftHistory();
                this.index = HISTORY_SIZE - 1;
            } else {
                this.index++;
            }
        }
    }

    private record HistoryItem(int seed, int x, int y) {}
}
