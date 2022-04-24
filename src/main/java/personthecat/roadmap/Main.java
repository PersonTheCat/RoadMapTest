package personthecat.roadmap;

import xjs.serialization.JsonContext;
import xjs.serialization.writer.JsonWriterOptions;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

public class Main {

    public static void main(final String[] args) {
        JsonContext.setDefaultFormatting(new JsonWriterOptions().setSmartSpacing(true));
        new RoadMapTest().run();
    }

    private static class RoadMapTest {
        final Config config = new Config(new File("config.xjs"));
        final Random rand = new Random();
        final HeightmapGenerator generator = new HeightmapGenerator(this.config, this.rand.nextInt());

        void run() {
            final var window = new AppWindow(this.createNextImage(false, false));
            window.onKeyPressed(KeyEvent.VK_SPACE, w -> w.render(this.createNextImage(false, true)));
            window.onKeyPressed('r', w -> w.render(this.createNextImage(true, false)));
            window.onKeyPressed(KeyEvent.VK_ESCAPE, AppWindow::close);

            window.onKeyPressed(KeyEvent.VK_UP, w -> {
                this.generator.up();
                w.render(this.createNextImage(false, false));
            });
            window.onKeyPressed(KeyEvent.VK_DOWN, w -> {
                this.generator.down();
                w.render(this.createNextImage(false, false));
            });
            window.onKeyPressed(KeyEvent.VK_LEFT, w -> {
                this.generator.left();
                w.render(this.createNextImage(false, false));
            });
            window.onKeyPressed(KeyEvent.VK_RIGHT, w -> {
                this.generator.right();
                w.render(this.createNextImage(false, false));
            });
        }

        BufferedImage createNextImage(final boolean reload, final boolean newSeed) {
            if (reload) {
                this.config.reloadFromDisk();
                this.generator.reload();
            }
            if (newSeed) {
                this.generator.next(this.rand.nextInt());
            }
            return this.generator.generate();
        }
    }
}
