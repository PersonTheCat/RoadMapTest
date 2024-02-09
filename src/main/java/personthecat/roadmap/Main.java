package personthecat.roadmap;

import xjs.serialization.JsonContext;
import xjs.serialization.writer.JsonWriterOptions;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

public class Main {

    public static void main(final String[] args) {
        JsonContext.setDefaultFormatting(
            new JsonWriterOptions().setSmartSpacing(true).setOmitQuotes(true));
        new RoadMapTest().run();
    }

    private static class RoadMapTest {
        final Config config = new Config(new File("config.xjs"));
        final Tracker tracker = new Tracker(this.config);
        final Random rand = new Random();
        final TerrainGenerator generator = new TerrainGenerator(this.tracker, this.config);
        AppWindow window;

        void run() {
            final var window = new AppWindow(this.config, this.tracker, this.createNextImage(true));
            window.onKeyPressed(KeyEvent.VK_ESCAPE, AppWindow::close);

            window.onKeyPressed(KeyEvent.VK_SPACE, w -> {
                this.generator.next(this.rand);
                w.render(this.createNextImage(true));
            });
            window.onKeyPressed(KeyEvent.VK_BACK_SPACE, w -> {
                this.generator.previous();
                w.render(this.createNextImage(true));
            });
            window.onKeyPressed('r', w -> {
                this.reloadConfig();
                w.render(this.createNextImage(true));
            });
            window.onKeyPressed('m', w -> {
                this.tracker.toggleMountains();
                w.render(this.createNextImage(true));
            });
            window.onKeyPressed(KeyEvent.VK_S, (w, e) -> {
                if (e.isControlDown()) {
                    this.config.saveIfUpdated(this.tracker);
                }
            });
            window.onKeyPressed('s', w -> {
                this.tracker.toggleSideView();
                w.render(this.createNextImage(false));
            });
            window.onKeyPressed(new int[] { KeyEvent.VK_EQUALS, KeyEvent.VK_PLUS }, (w, e) -> {
                final int amount = e.isShiftDown() ? 3 : 1;
                if (e.isControlDown()) {
                    this.tracker.angleUp(amount);
                    w.render(this.createNextImage(false));
                } else if (e.isAltDown()) {
                    this.tracker.anchoredFrequencyUp(this.config, amount);
                    w.render(this.createNextImage(true));
                } else {
                    this.tracker.zoomIn(amount);
                    w.render(this.generator.getBuffer());
                }
            });
            window.onKeyPressed(KeyEvent.VK_MINUS, (w, e) -> {
                final int amount = e.isShiftDown() ? 3 : 1;
                if (e.isControlDown()) {
                    this.tracker.angleDown(amount);
                    w.render(this.createNextImage(false));
                } else if (e.isAltDown()) {
                    this.tracker.anchoredFrequencyDown(this.config, amount);
                    w.render(this.createNextImage(true));
                } else {
                    this.tracker.zoomOut(amount);
                    w.render(this.generator.getBuffer());
                }
            });
            window.onKeyPressed(new int[] { KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT }, (w, e, ks) -> {
                final int speed = e.isShiftDown() ? this.config.getScrollAmount() : 1;
                if (ks.test(KeyEvent.VK_UP)) {
                    this.generator.up(speed);
                }
                if (ks.test(KeyEvent.VK_DOWN)) {
                    this.generator.down(speed);
                }
                if (ks.test(KeyEvent.VK_LEFT)) {
                    this.generator.left(speed);
                }
                if (ks.test(KeyEvent.VK_RIGHT)) {
                    this.generator.right(speed);
                }
                w.render(this.createNextImage(false));
            });
            this.window = window;
        }

        BufferedImage createNextImage(final boolean reload) {
            if (reload) {
                this.generator.reload();
            }
            return this.generator.generate(reload);
        }

        void reloadConfig() {
            final int oH = this.config.getChunkHeight();
            final int oW = this.config.getChunkWidth();
            final int oSeed = this.config.getSeed();
            final int oXOffset = this.config.getXOffset();
            final int oYOffset = this.config.getYOffset();
            final boolean oSideView = this.config.isSideView();
            final boolean oMountains = this.config.isMountains();
            final float oZoom = this.config.getZoom();
            final float oSideViewAngle = this.config.getSideViewAngle();
            final float oFrequency = this.config.getFrequency();
            final float oGrooveFrequency = this.config.getGrooveFrequency();
            this.config.reloadFromDisk();
            if (oH != this.config.getChunkHeight() || oW != this.config.getChunkWidth()) {
                if (this.window != null) {
                    this.window.pack();
                }
            }
            if (oSeed != this.config.getSeed()) {
                this.tracker.setSeed(this.config.getSeed());
            }
            if (oXOffset != this.config.getXOffset()) {
                this.tracker.setXOffset(this.config.getXOffset());
            }
            if (oYOffset != this.config.getYOffset()) {
                this.tracker.setYOffset(this.config.getYOffset());
            }
            if (oSideView != this.config.isSideView()) {
                this.tracker.setSideView(this.config.isSideView());
            }
            if (oMountains != this.config.isMountains()) {
                this.tracker.setMountains(this.config.isMountains());
            }
            if (oZoom != this.config.getZoom()) {
                this.tracker.setZoom(this.config.getZoom());
            }
            if (oSideViewAngle != this.config.getSideViewAngle()) {
                this.tracker.setSideViewAngle(this.config.getSideViewAngle());
            }
            if (oFrequency != this.config.getFrequency()) {
                this.tracker.setFrequency(this.config.getFrequency());
            }
            if (oGrooveFrequency != this.config.getGrooveFrequency()) {
                this.tracker.setGrooveFrequency(this.config.getGrooveFrequency());
            }
        }
    }
}
