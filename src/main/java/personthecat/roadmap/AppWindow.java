package personthecat.roadmap;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AppWindow {
    private final JLabel label;
    private final JFrame window;
    private final Config config;

    public AppWindow(final Config config, final BufferedImage image) {
        this.label = new JLabel();
        this.render(image);
        this.window = createWindow(this.label);
        this.config = config;
    }

    public void onKeyPressed(final int key, final Consumer<AppWindow> event) {
        this.onKeyPressed(key, (w, e) -> event.accept(w));
    }

    public void onKeyPressed(final int key, final BiConsumer<AppWindow, KeyEvent> event) {
        this.window.addKeyListener(new KeyTypedListener(key, event));
    }

    public void render(final BufferedImage image) {
        this.label.setIcon(new ImageIcon(image));
    }

    public void pack() {
        final int width = this.config.getChunkWidth() << 4;
        final int height = this.config.getChunkHeight() << 4;
        this.window.setSize(width, height);
    }

    public void close() {
        this.window.dispose();
        System.exit(0);
    }

    private static JFrame createWindow(final JLabel label) {
        final var window = new JFrame();
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
            if (this.key == e.getKeyChar()) {
                this.event.accept(AppWindow.this, e);
            }
        }

        @Override
        public void keyPressed(final KeyEvent e) {
            if (this.isUpdateReady() && this.key == e.getKeyCode()) {
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
}
