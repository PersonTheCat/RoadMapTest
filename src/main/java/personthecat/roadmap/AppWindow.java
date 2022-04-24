package personthecat.roadmap;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class AppWindow {

    private final JLabel label;
    private final JFrame window;

    public AppWindow(final BufferedImage image) {
        this.label = new JLabel();
        this.render(image);
        this.window = createWindow(this.label);
    }

    public void onKeyPressed(final int key, final Consumer<AppWindow> event) {
        this.window.addKeyListener(new KeyTypedListener(this, key, event));
    }

    public void render(final BufferedImage image) {
        this.label.setIcon(new ImageIcon(image));
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

    private record KeyTypedListener(AppWindow window, int key, Consumer<AppWindow> event) implements KeyListener {

        @Override
        public void keyTyped(final KeyEvent e) {
            if (this.key == e.getKeyChar()) {
                this.event.accept(this.window);
            }
        }

        @Override
        public void keyPressed(final KeyEvent e) {
            if (this.key == e.getKeyCode()) {
                this.event.accept(this.window);
            }
        }

        @Override
        public void keyReleased(final KeyEvent e) {}
    }
}
