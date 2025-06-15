package main;

import entity.Coin;
import entity.Enemy;
import entity.Player;
import manager.KeyManager;
import tile.TileMap;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel implements Runnable {

    public static final int WIDTH = 1280; // was 640
    public static final int HEIGHT = 600; // was 480

    private Thread thread;
    private boolean running = false;

    private TileMap tileMap;
    private Player player;
    private KeyManager keyManager;

    private int cameraX = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        requestFocusInWindow();
        keyManager = new KeyManager();
        addKeyListener(keyManager);
    }

    public void addNotify() {
        super.addNotify();
        if (thread == null) {
            thread = new Thread(this);
            running = true;
            thread.start();
        }
    }

    private void init() {
        tileMap = new TileMap(); // TileMap now loads from CSV
        int[] spawn = tileMap.getPlayerSpawn();
        player = new Player(spawn[0], spawn[1], keyManager, tileMap);
    }

    public void run() {
        init();

        long lastTime = System.nanoTime();
        double nsPerUpdate = 1000000000.0 / 60.0;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerUpdate;
            lastTime = now;

            while (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    private void update() {
        keyManager.update();
        player.update();

        // Coin collection
        java.util.List<Coin> coins = tileMap.getCoins();
        for (Coin coin : coins) {
            if (!coin.isCollected() && coin.intersects(player.getX(), player.getY(), 16, 16)) {
                coin.collect();
                // (optional) Add score, sound, etc.
            }
        }

        // Camera follow player and lock to player position
        int tileMapPixelWidth = tileMap.getWidth() * tileMap.getTileSize();
        int halfScreen = WIDTH / 4; // Because you scale by 2x, use WIDTH/4 for logic
        cameraX = player.getX() - halfScreen;

        // Clamp cameraX so it doesn't go out of bounds
        if (cameraX < 0) cameraX = 0;
        int maxCameraX = tileMapPixelWidth - (WIDTH / 2); // again, WIDTH/2 because of 2x scale
        if (cameraX > maxCameraX) cameraX = maxCameraX;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Scale up everything by 2x
        g2.scale(2.0, 2.0);

        // Use nearest neighbor for pixel art
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Draw background color
        g2.setColor(new Color(100, 180, 255)); // sky blue
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw a solid ground rectangle at the bottom of the screen
        g.setColor(new Color(80, 40, 20)); // brown color
        int groundHeight = 32; // or any height you want
        g.fillRect(0, GamePanel.HEIGHT - groundHeight, GamePanel.WIDTH, groundHeight);

        // Only render if tileMap and player are initialized
        if (tileMap != null) {
            tileMap.draw(g2, cameraX);

            // Render coins
            for (Coin coin : tileMap.getCoins()) {
                coin.draw(g2, cameraX);
            }

            // Render enemies
            for (Enemy enemy : tileMap.getEnemies()) {
                enemy.draw(g2, cameraX);
            }
        }

        if (player != null) {
            player.draw(g2, cameraX);
        }
    }
}
