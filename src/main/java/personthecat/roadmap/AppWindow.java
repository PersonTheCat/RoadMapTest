package personthecat.roadmap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AppWindow extends WindowAdapter {
    private final JLabel label;
    private final JFrame window;
    private final Config config;
    private final Tracker tracker;
    private BufferedImage output;
    private Graphics2D graphics;

    public AppWindow(final Config config, final Tracker tracker, final BufferedImage image) {
        this.label = new JLabel();
        this.window = createWindow(this.label);
        this.config = config;
        this.tracker = tracker;
        this.window.addWindowListener(this);
        this.output = image;
        this.pack();
        this.render(image);
    }

    public void onKeyPressed(final int key, final Consumer<AppWindow> event) {
        this.onKeyPressed(key, (w, e) -> event.accept(w));
    }

    public void onKeyPressed(final int[] anyKey, final BiConsumer<AppWindow, KeyEvent> event) {
        for (final int key : anyKey) {
            this.onKeyPressed(key, event);
        }
    }

    public void onKeyPressed(final int key, final BiConsumer<AppWindow, KeyEvent> event) {
        this.window.addKeyListener(new KeyTypedListener(key, event));
    }

    public void onKeyPressed(final int[] anyKey, final MultiKeyEvent event) {
        this.window.addKeyListener(new MultiKeyTypedListener(anyKey, event));
    }

    public void render(final BufferedImage image) {
        final float zoom = this.tracker.getZoom();
        if (zoom == 1) {
            this.drawImage(image);
        } else if (zoom < 1) {
            this.drawZoomedOut(image, zoom);
        } else {
            this.drawZoomedIn(image, zoom);
        }
        this.label.repaint();
    }

    private void drawImage(final BufferedImage image) {
        int[] src = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int[] dst = ((DataBufferInt) this.output.getRaster().getDataBuffer()).getData();
        System.arraycopy(src, 0, dst, 0, dst.length);
    }

    private void drawZoomedOut(final BufferedImage image, final float zoom) {
        this.graphics.setBackground(this.config.getBackgroundColor());
        this.graphics.clearRect(0, 0, this.output.getWidth(), this.output.getHeight());
        final float bx = image.getWidth() * (1 - zoom) / 2;
        final float by = image.getHeight() * (1 - zoom) / 2;
        this.graphics.drawImage(image, (int) bx, (int) by, (int) (image.getWidth() - bx), (int) (image.getHeight() - by), 0, 0, image.getWidth() - 1, image.getHeight() - 2, null);
    }

    private void drawZoomedIn(final BufferedImage image, final float zoom) {
        final float bx = image.getWidth() * (1 - zoom) / 2;
        final float by = image.getHeight() * (1 - zoom);
        this.graphics.drawImage(image, (int) bx, (int) by, (int) (image.getWidth() - bx), image.getHeight(), 0, 0, image.getWidth() - 1, image.getHeight() - 2, this.config.getBackgroundColor(), null);
    }

    public void pack() {
        final int width = this.config.getChunkWidth() << 4;
        final int height = this.config.getChunkHeight() << 4;
        this.window.setSize(width, height);
        this.resetOutput();
    }

    private void resetOutput() {
        this.output = this.createNewImage();
        if (this.graphics != null) {
            this.graphics.dispose();
        }
        this.graphics = this.output.createGraphics();
        this.label.setIcon(new ImageIcon(this.output));
    }

    private BufferedImage createNewImage() {
        final int w = this.config.getChunkWidth() << 4;
        final int h = this.config.getChunkHeight() << 4;
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void windowClosing(final WindowEvent e) {
        this.close();
    }

    public void close() {
        this.config.saveIfUpdated(this.tracker);
        this.window.dispose();
        System.exit(0);
    }

    private static JFrame createWindow(final JLabel label) {
        final var window = new JFrame();
        window.setTitle("Road Map Test");
        window.add(label);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.pack();
        window.setVisible(true);
        return window;
    }

    private class KeyTypedListener implements KeyListener {
        final int key;
        final BiConsumer<AppWindow, KeyEvent> event;
        long lastUpdate;

        public KeyTypedListener(final int key, final BiConsumer<AppWindow, KeyEvent> event) {
            this.key = key;
            this.event = event;
            this.lastUpdate = 0;
        }

        @Override
        public void keyTyped(final KeyEvent e) {
            if (this.isUpdateReady() && this.key == e.getKeyChar()) {
                this.event.accept(AppWindow.this, e);
                this.lastUpdate = System.currentTimeMillis();
            }
        }

        @Override
        public void keyPressed(final KeyEvent e) {
            if (this.isUpdateReady() && this.key == e.getKeyCode() && e.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
                this.event.accept(AppWindow.this, e);
                this.lastUpdate = System.currentTimeMillis();
            }
        }

        @Override
        public void keyReleased(final KeyEvent e) {
            this.lastUpdate = 0;
        }

        private boolean isUpdateReady() {
            return System.currentTimeMillis() - this.lastUpdate > config.getScrollCoolDown();
        }
    }

    private class MultiKeyTypedListener implements KeyListener {
        final int[] anyKey;
        final MultiKeyEvent event;
        final Set<Integer> keysPressed;
        long lastUpdate;

        public MultiKeyTypedListener(final int[] anyKey, final MultiKeyEvent event) {
            this.anyKey = anyKey;
            this.event = event;
            this.keysPressed = new HashSet<>();
            this.lastUpdate = 0;
        }

        @Override
        public void keyTyped(final KeyEvent e) {
            this.keyPressed(e);
        }

        @Override
        public void keyPressed(final KeyEvent e) {
            if (!this.isUpdateReady()) {
                return;
            }
            for (int key : this.anyKey) {
                if (key == e.getKeyCode()) {
                    this.keysPressed.add(key);
                    this.event.accept(AppWindow.this, e, this.keysPressed::contains);
                    this.lastUpdate = System.currentTimeMillis();
                    return;
                }
            }
        }

        @Override
        public void keyReleased(final KeyEvent e) {
            for (int key : this.anyKey) {
                if (key == e.getKeyCode()) {
                    this.keysPressed.remove(key);
                    this.lastUpdate = 0;
                    return;
                }
            }
        }

        private boolean isUpdateReady() {
            return System.currentTimeMillis() - this.lastUpdate > config.getScrollCoolDown();
        }
    }

    @FunctionalInterface
    public interface MultiKeyEvent {
        void accept(final AppWindow w, final KeyEvent k, final Predicate<Integer> ks);
    }
}
