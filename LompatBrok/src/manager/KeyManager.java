package manager;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyManager implements KeyListener {

    private boolean[] keys = new boolean[256];

    public boolean up, down, left, right, jump;

    public void update() {
        up    = keys[KeyEvent.VK_W];
        down  = keys[KeyEvent.VK_S];
        left  = keys[KeyEvent.VK_A];
        right = keys[KeyEvent.VK_D];
        jump  = keys[KeyEvent.VK_SPACE];
    }

    public boolean isKeyPressed(int keyCode) {
        return keys[keyCode];
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode >= 0 && keyCode < keys.length) { // <-- Tambahkan pengecekan ini
            keys[keyCode] = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode >= 0 && keyCode < keys.length) { // <-- Tambahkan pengecekan ini
            keys[keyCode] = false;
        }
    }
    @Override
    public void keyTyped(KeyEvent e) {}
}
