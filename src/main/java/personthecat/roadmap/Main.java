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
                this.config.toggleMountains();
                w.render(this.createNextImage(true, false));
            });
            window.onKeyPressed('s', w -> {
                this.tracker.toggleSideView();
                w.pack();
                w.render(this.createNextImage(true, false));
            });
            window.onKeyPressed(new int[] { KeyEvent.VK_EQUALS, KeyEvent.VK_PLUS }, w -> {
                this.tracker.zoomIn();
                w.render(this.createNextImage(false, false));
            });
            window.onKeyPressed(KeyEvent.VK_MINUS, w -> {
                this.tracker.zoomOut();
                w.render(this.createNextImage(false, false));
            });
            window.onKeyPressed(KeyEvent.VK_UP, (w, e) -> {
                this.generator.up(e.isShiftDown() ? 3 : 1);
                w.render(this.createNextImage(false, false));
            });
            window.onKeyPressed(KeyEvent.VK_DOWN, (w, e) -> {
                this.generator.down(e.isShiftDown() ? 3 : 1);
                w.render(this.createNextImage(false, false));
            });
            window.onKeyPressed(KeyEvent.VK_LEFT, (w, e) -> {
                this.generator.left(e.isShiftDown() ? 3 : 1);
                w.render(this.createNextImage(false, false));
            });
            window.onKeyPressed(KeyEvent.VK_RIGHT, (w, e) -> {
                this.generator.right(e.isShiftDown() ? 3 : 1);
                w.render(this.createNextImage(false, false));
            });
            this.window = window;
        }

        BufferedImage createNextImage(final boolean reload, final boolean newSeed) {
            if (reload) {
                final int oH = this.config.getChunkHeight();
                final int oW = this.config.getChunkWidth();
                this.config.reloadFromDisk();
                this.generator.reload();
                if (oH != this.config.getChunkHeight() || oW != this.config.getChunkWidth()) {
                    if (this.window != null) {
                        this.window.pack();
                    }
                }
            }
            if (newSeed) {
                this.generator.next(this.rand.nextInt());
            }
            return this.generator.generate(reload);
        }
    }
}
