package personthecat.roadmap;

import java.util.Random;

public class Tracker {

    private int xOffset;
    private int yOffset;
    private int prevXOffset = 0;
    private int prevYOffset = 0;
    private int seed;
    private boolean sideView;
    private boolean mountains;
    private float zoom;
    private float sideViewAngle;
    private float frequency;
    private float grooveFrequency;
    private final History history = new History();

    public Tracker(final Config config) {
        this.seed = config.getSeed();
        this.xOffset = config.getXOffset();
        this.yOffset = config.getYOffset();
        this.sideView = config.isSideView();
        this.mountains = config.isMountains();
        this.zoom = config.getZoom();
        this.sideViewAngle = config.getSideViewAngle();
        this.frequency = config.getFrequency();
        this.grooveFrequency = config.getGrooveFrequency();
        this.history.addHistory(this);
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
        this.history.addHistory(this);
    }

    public void nextSeed(final Random rand) {
        final HistoryItem item = this.history.next();
        if (item != null) {
            this.applyHistory(item);
            return;
        }
        this.seed = rand.nextInt();
        this.history.addHistory(this);
    }

    public void previousSeed() {
        this.history.updateHistory(this);
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

    public void zoomIn(final int amount) {
        this.zoom += 0.05 * amount;
    }

    public void zoomOut(final int amount) {
        this.zoom -= 0.05 * amount;
    }

    public float getSideViewAngle() {
        return this.sideViewAngle;
    }

    public void setSideViewAngle(final float sideViewAngle) {
        this.sideViewAngle = sideViewAngle;
    }

    public void angleUp(final int amount) {
        this.sideViewAngle += amount * 0.05;
    }

    public void angleDown(final int amount) {
        this.sideViewAngle -= amount * 0.05;
    }

    public float getFrequency() {
        return this.frequency;
    }

    public void setFrequency(final float frequency) {
        this.frequency = frequency;
    }

    private void adjustFrequencyAnchored(final Config config, final float inc) {
        if (this.frequency + inc <= 0 || this.grooveFrequency + inc <= 0) {
            return;
        }
        final float frequency = this.frequency + inc;
        final int lX = (config.getChunkWidth() << 4) / 2;
        final int lY = (config.getChunkHeight() << 4) / 2;
        final int cX = this.xOffset + lX;
        final int cY = this.yOffset + lY;
        if (this.frequency == 0) {
            this.xOffset = (int) ((cX / (double) frequency) - lX);
            this.yOffset = (int) ((cY / (double) frequency) - lY);
        } else {
            this.xOffset = (int) ((cX * (double) this.frequency) / (double) frequency) - lX;
            this.yOffset = (int) ((cY * (double) this.frequency) / (double) frequency) - lY;
        }
        this.frequency = frequency;
        this.grooveFrequency += inc;
    }

    public void anchoredFrequencyUp(final Config config, final int amount) {
        this.adjustFrequencyAnchored(config, amount * 0.000025F);
    }

    public void anchoredFrequencyDown(final Config config, final int amount) {
        this.adjustFrequencyAnchored(config, -amount * 0.000025F);
    }

    public float getGrooveFrequency() {
        return this.grooveFrequency;
    }

    public void setGrooveFrequency(final float frequency) {
        this.grooveFrequency = frequency;
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

        void addHistory(final Tracker t) {
            this.updateHistory(t);
            this.count++;
        }

        void updateHistory(final Tracker t) {
            this.items[this.index] = new HistoryItem(t.seed, t.xOffset, t.yOffset);
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
