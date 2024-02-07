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
        final TerrainGenerator generator = new TerrainGenerator(this.tracker, this.config, this.rand.nextInt());
        AppWindow window;

        void run() {
            final var window = new AppWindow(this.config, this.tracker, this.createNextImage(true, true));
            window.onKeyPressed(KeyEvent.VK_SPACE, w -> w.render(this.createNextImage(false, true)));
            window.onKeyPressed('r', w -> w.render(this.createNextImage(true, false)));
            window.onKeyPressed(KeyEvent.VK_ESCAPE, AppWindow::close);

            window.onKeyPressed('m', w -> {
                this.tracker.toggleMountains();
                w.render(this.createNextImage(true, false));
            });
            window.onKeyPressed('s', (w, e) -> {
                if (e.isControlDown()) {
                    this.config.save();
                } else {
                    this.tracker.toggleSideView();
                    w.render(this.createNextImage(true, false));
                }
            });
            window.onKeyPressed(new int[] { KeyEvent.VK_EQUALS, KeyEvent.VK_PLUS }, (w, e) -> {
                if (e.isControlDown()) {
                    this.tracker.angleUp();
                } else {
                    this.tracker.zoomIn();
                }
                w.render(this.createNextImage(false, false));
            });
            window.onKeyPressed(KeyEvent.VK_MINUS, (w, e) -> {
                if (e.isControlDown()) {
                    this.tracker.angleDown();
                } else {
                    this.tracker.zoomOut();
                }
                w.render(this.createNextImage(false, false));
            });
            window.onKeyPressed(new int[] { KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT }, (w, e, ks) -> {
                final int speed = e.isShiftDown() ? 3 : 1;
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
                w.render(this.createNextImage(false, false));
            });
            this.window = window;
        }

        BufferedImage createNextImage(final boolean reload, final boolean newSeed) {
            if (reload) {
                final int oH = this.config.getChunkHeight();
                final int oW = this.config.getChunkWidth();
                final boolean oSideView = this.config.isSideView();
                final boolean oMountains = this.config.isMountains();
                final float oZoom = this.config.getZoom();
                final float oSideViewAngle = this.config.getSideViewAngle();
                this.config.reloadFromDisk();
                if (oH != this.config.getChunkHeight() || oW != this.config.getChunkWidth()) {
                    if (this.window != null) {
                        this.window.pack();
                    }
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
                this.generator.reload();
            }
            if (newSeed) {
                this.generator.next(this.rand.nextInt());
            }
            return this.generator.generate(reload);
        }
    }
}
