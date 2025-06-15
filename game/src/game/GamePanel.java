package game;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.IOException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable, KeyListener {

    // Pengaturan Layar
    final int originalTileSize = 16;
    final int scale = 3;
    public final int tileSize = originalTileSize * scale;
    public final int screenWidth = tileSize * 20;
    public final int screenHeight = tileSize * 15;

    // Pengaturan Dunia Game
    public final int maxWorldCol = 120;
    public final int maxWorldRow = 15;
    public final int worldWidth = tileSize * maxWorldCol;
    public final int worldHeight = tileSize * maxWorldRow;

    // Sistem Kamera
    public int cameraX = 0;

    // FPS
    int FPS = 60;

    // Enum untuk mengelola status permainan
    public enum GameState {
        MENU,
        PLAYING,
        GAME_OVER,
        GAME_WON
    }
    private GameState gameState;

    // Sistem Game dan Entitas
    Thread gameThread;
    Player player = new Player(this);
    ArrayList<Enemy> enemies = new ArrayList<>();
    ArrayList<Obstacle> obstacles = new ArrayList<>();
    ArrayList<MovingPlatform> movingPlatforms = new ArrayList<>(); // Daftar platform bergerak
    private LevelManager levelManager;
    public SoundManager soundManager;

    // Flag input pemain
    private boolean jumpPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    // Variabel Game
    private int coinsCollected = 0;
    private BufferedImage coinImage;
    private BufferedImage[] backgroundLayers;
    private double[] parallaxFactors;

    private int menuChoice = 0;

    private Random random = new Random();
    private int obstacleSpawnTimer = 0;
    private final int OBSTACLE_SPAWN_INTERVAL = 120;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(new Color(20, 80, 70));
        this.setDoubleBuffered(true);
        this.addKeyListener(this);
        this.setFocusable(true);

        gameState = GameState.MENU;
        loadAssets();
        levelManager = new LevelManager(this);

        soundManager = new SoundManager();
    }

    /**
     * Mereset status game, pemain, koin, level, dan entitas untuk memulai game baru.
     * Menginisialisasi platform bergerak dari level map.
     */
    private void restartGame() {
        gameState = GameState.PLAYING;
        player.fullReset();
        coinsCollected = 0;
        levelManager.createLevel(); // Membuat ulang level
        
        enemies.clear();
        obstacles.clear();
        movingPlatforms.clear(); // Hapus platform bergerak lama
        obstacleSpawnTimer = 0;

        // Memunculkan musuh dan meriam pada posisi tetap di level
        int groundLevelY = (maxWorldRow - 2) * tileSize; // Y koordinat baris ground atas

        enemies.add(new Enemy(tileSize * 15, groundLevelY - (tileSize * 1), this));
        enemies.add(new Enemy(tileSize * 26, groundLevelY - (tileSize * 1), this));
        enemies.add(new Enemy(tileSize * 37, groundLevelY - (tileSize * 1), this));
        enemies.add(new Enemy(tileSize * 45, groundLevelY - (tileSize * 1), this));
        enemies.add(new Enemy(tileSize * 54, groundLevelY - (tileSize * 1), this));
        enemies.add(new Enemy(tileSize * 63, groundLevelY - (tileSize * 1), this));
        enemies.add(new Enemy(tileSize * 70, groundLevelY - (tileSize * 1), this));
        enemies.add(new Enemy(tileSize * 85, groundLevelY - (tileSize * 1), this));
        enemies.add(new Enemy(tileSize * 95, groundLevelY - (tileSize * 1), this));
        enemies.add(new Enemy(tileSize * 105, groundLevelY - (tileSize * 1), this));

        // Meriam
        obstacles.add(new Obstacle(tileSize * 30, groundLevelY - (tileSize * 1), Obstacle.ObstacleType.CANNON, this));
        obstacles.add(new Obstacle(tileSize * 80, groundLevelY - (tileSize * 1), Obstacle.ObstacleType.CANNON, this));
        
        // --- Inisialisasi Moving Platforms dari Level Map ---
        for (int r = 0; r < maxWorldRow; r++) {
            for (int c = 0; c < maxWorldCol; c++) {
                if (levelManager.originalLevelMap[r][c] == LevelManager.TILE_MOVING_H) {
                    movingPlatforms.add(new MovingPlatform(c * tileSize, r * tileSize, MovingPlatform.PlatformType.HORIZONTAL, tileSize * 6, 1.8, this));
                    levelManager.levelMap[r][c] = LevelManager.TILE_EMPTY;
                } else if (levelManager.originalLevelMap[r][c] == LevelManager.TILE_MOVING_V) {
                    movingPlatforms.add(new MovingPlatform(c * tileSize, r * tileSize, MovingPlatform.PlatformType.VERTICAL, tileSize * 5, 1.2, this));
                    levelManager.levelMap[r][c] = LevelManager.TILE_EMPTY;
                }
            }
        }

        System.out.println("DEBUG GamePanel: Game dimulai ulang. Player di X: " + player.x + ", Y: " + player.y);

        // Musik latar (jika ada file WAV Anda):
        // soundManager.playMusic("res/game_bgm.wav", true);
    }

    /**
     * Memuat gambar dan aset yang diperlukan untuk game.
     */
    private void loadAssets() {
        try {
            InputStream coinIs = getClass().getResourceAsStream("/res/coins_hud.png");
            if (coinIs != null) {
                coinImage = ImageIO.read(coinIs);
                System.out.println("DEBUG GamePanel: Gambar koin (coins_hud.png) dimuat.");
            } else {
                System.err.println("ERROR GamePanel: Tidak dapat menemukan file gambar koin! (coins_hud.png). Pastikan file ada di 'res/'.");
            }

            backgroundLayers = new BufferedImage[10];
            String[] layerNames = {
                "Layer_0011_0.png", "Layer_0010_1.png", "Layer_0009_2.png",
                "Layer_0008_3.png", "Layer_0006_4.png", "Layer_0005_5.png",
                "Layer_0003_6.png", "Layer_0002_7.png", "Layer_0001_8.png",
                "Layer_0000_9.png"
            };

            for(int i = 0; i < layerNames.length; i++) {
                InputStream bgIs = getClass().getResourceAsStream("/res/" + layerNames[i]);
                if (bgIs != null) {
                    backgroundLayers[i] = ImageIO.read(bgIs);
                } else {
                    System.err.println("ERROR GamePanel: Tidak dapat menemukan file latar: " + layerNames[i] + ". Pastikan file ada di 'res/'.");
                }
            }

            parallaxFactors = new double[] {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.8, 0.9, 1.0};

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR GamePanel: Terjadi kesalahan memuat aset game.");
        }
    }

    /**
     * Memunculkan rintangan baru (burung atau batu) secara berkala.
     */
    private void spawnObstacles() {
        obstacleSpawnTimer++;
        if (obstacleSpawnTimer >= OBSTACLE_SPAWN_INTERVAL) {
            obstacleSpawnTimer = 0;

            Obstacle.ObstacleType typeToSpawn;
            if (random.nextDouble() < 0.7) {
                typeToSpawn = Obstacle.ObstacleType.ROCK;
            } else {
                typeToSpawn = Obstacle.ObstacleType.BIRD;
            }

            int spawnX;
            int spawnY;
            double speedFactor = 1.0 + (random.nextDouble() * 0.5);
            
            if (typeToSpawn == Obstacle.ObstacleType.BIRD) {
                spawnX = cameraX + screenWidth + tileSize;
                // Mengakses player.y secara langsung karena GamePanel adalah kelas player
                spawnY = (int)player.y + (random.nextInt(tileSize * 2) - tileSize);
                spawnY = Math.max(tileSize, Math.min(screenHeight - tileSize * 2, spawnY));

                // Mengakses player.y secara langsung karena GamePanel adalah kelas player
                Obstacle newBird = new Obstacle(spawnX, spawnY, typeToSpawn, this, player.y);
                newBird.velX *= -Math.abs(newBird.velX) * speedFactor;
                obstacles.add(newBird);
            } else {
                spawnX = cameraX + random.nextInt(screenWidth - tileSize);
                spawnY = -tileSize;
                double rockScale = 0.7 + (random.nextDouble() * 0.6);
                Obstacle newRock = new Obstacle(spawnX, spawnY, typeToSpawn, this, rockScale);
                newRock.velY *= speedFactor;
                obstacles.add(newRock);
            }
        }

        obstacles.removeIf(o -> o.isOffScreen() && o.type != Obstacle.ObstacleType.CANNON);
    }

    public BufferedImage getCoinImage() { return coinImage; }
    public int getCoinsCollected() { return coinsCollected; }

    /**
     * Mengatur tile tertentu di peta level ke tipe tile baru.
     * @param row Indeks baris tile.
     * @param col Indeks kolom tile.
     * @param tileType Tipe baru untuk mengatur tile.
     */
    public void setTile(int row, int col, int tileType) {
        if (levelManager != null && row >= 0 && row < maxWorldRow && col >= 0 && col < maxWorldCol) {
            levelManager.levelMap[row][col] = tileType;
        }
    }

    /**
     * Memulai thread game utama, yang memanggil metode update dan repaint secara berkala.
     */
    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    /**
     * Memperbarui semua elemen game berdasarkan status game saat ini.
     */
    public void update() {
        if (gameState == GameState.PLAYING) {
            // Pemanggilan player.update() sekarang meneruskan movingPlatforms
            player.update(levelManager.levelMap, tileSize, jumpPressed, leftPressed, rightPressed, movingPlatforms);
            for (Enemy enemy : new ArrayList<>(enemies)) {
                enemy.update(levelManager.levelMap, tileSize);
            }
            for (Obstacle obstacle : new ArrayList<>(obstacles)) {
                obstacle.update();
            }
            // Update Moving Platforms
            for (MovingPlatform platform : movingPlatforms) {
                platform.update();
            }

            spawnObstacles();
            checkCollisions();
            updateCamera();

            if (player.getLives() <= 0) {
                gameState = GameState.GAME_OVER;
                soundManager.playSoundEffect("res/game_over_sound.wav");
            }

        }
        if (gameState == GameState.GAME_WON && soundManager != null) {
            Main.showEndingCutscene();
        }
    }

    /**
     * Memeriksa tabrakan antara pemain dan musuh, rintangan, proyektil, dan platform bergerak.
     */
    private void checkCollisions() {
        Rectangle playerBounds = player.getBounds();

        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            if (playerBounds.intersects(enemy.getBounds())) {
                if (player.velY > 0 && player.getBounds().getMaxY() < enemy.getBounds().getCenterY()) {
                    player.stompBounce();
                    soundManager.playSoundEffect("res/stomp.wav");
                    enemyIterator.remove();
                } else {
                    player.takeDamage();
                    soundManager.playSoundEffect("res/damage.wav");
                }
                return;
            }
        }

        Iterator<Obstacle> obstacleIterator = obstacles.iterator();
        while (obstacleIterator.hasNext()) {
            Obstacle obstacle = obstacleIterator.next();
            if (obstacle.type != Obstacle.ObstacleType.CANNON && playerBounds.intersects(obstacle.getBounds())) {
                player.takeDamage();
                soundManager.playSoundEffect("res/damage.wav");
                obstacleIterator.remove();
                return;
            }

            if (obstacle.type == Obstacle.ObstacleType.CANNON) {
                Iterator<Obstacle.Projectile> projectileIterator = obstacle.projectiles.iterator();
                while (projectileIterator.hasNext()) {
                    Obstacle.Projectile p = projectileIterator.next();
                    if (playerBounds.intersects(p.getBounds())) {
                        player.takeDamage();
                        soundManager.playSoundEffect("res/damage.wav");
                        projectileIterator.remove();
                        return;
                    }
                }
            }
        }

        // Logika tabrakan Moving Platforms telah dipindahkan ke Player.java.
        // for (MovingPlatform platform : movingPlatforms) {
        //     Rectangle platformBounds = platform.getBounds();
        //     if (playerBounds.intersects(platformBounds) && player.velY >= 0) {
        //         if (player.y + player.height - (platform.getVelY() * 0.5) <= platformBounds.getMinY() + 1) { 
        //              player.y = platformBounds.y - player.height;
        //              player.velY = 0;
        //              player.x += platform.getVelX();
        //              player.onGround = true;
        //         }
        //     }
        // }


        Rectangle collectionBounds = new Rectangle(
            playerBounds.x, playerBounds.y - 1, playerBounds.width, playerBounds.height + 2
        );

        int startCol = (int) (player.x / tileSize) - 1;
        int endCol = (int) ((player.x + player.getBounds().width) / tileSize) + 1;
        int startRow = (int) (player.y / tileSize) - 1;
        int endRow = (int) ((player.y + player.getBounds().height) / tileSize) + 1;

        startCol = Math.max(0, startCol);
        endCol = Math.min(maxWorldCol - 1, endCol);
        startRow = Math.max(0, startRow);
        endRow = Math.min(maxWorldRow - 1, endRow);


        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (row < 0 || row >= maxWorldRow || col < 0 || col >= maxWorldCol) continue;

                int tileType = levelManager.levelMap[row][col];
                
                // --- Logika Lubang Kematian Instan ---
                if (tileType == LevelManager.TILE_DEATH_ZONE) {
                    Rectangle deathZoneBounds = new Rectangle(col * tileSize, row * tileSize, tileSize, tileSize);
                    if (playerBounds.intersects(deathZoneBounds)) {
                        player.health = 0; // Mengakses player.health secara langsung
                        soundManager.playSoundEffect("res/game_over_sound.wav");
                        return;
                    }
                }
                // --- Akhir Logika Lubang Kematian Instan ---

                if (tileType == LevelManager.TILE_COIN || tileType == LevelManager.TILE_HEALTH || tileType == LevelManager.TILE_EXIT) {
                    Rectangle objectBounds = new Rectangle(col * tileSize, row * tileSize, tileSize, tileSize);
                    if (collectionBounds.intersects(objectBounds)) {
                        switch (tileType) {
                            case LevelManager.TILE_COIN:
                                levelManager.levelMap[row][col] = LevelManager.TILE_EMPTY;
                                coinsCollected++;
                                soundManager.playSoundEffect("res/coin_collect.wav");
                                break;
                            case LevelManager.TILE_HEALTH:
                                player.gainHealth();
                                levelManager.levelMap[row][col] = LevelManager.TILE_EMPTY;
                                break;
                            case LevelManager.TILE_EXIT:
                                if (coinsCollected >= 15) {
                                    gameState = GameState.GAME_WON;
                                }
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Memperbarui posisi kamera untuk mengikuti pemain, membatasi dalam batas dunia.
     */
    private void updateCamera() {
        cameraX = (int)player.x - (screenWidth / 2);
        if (cameraX < 0) cameraX = 0;
        if (cameraX > worldWidth - screenWidth) cameraX = worldWidth - screenWidth;
        if(player.y > worldHeight) {
             player.health = 0; // Mengakses player.health secara langsung
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        switch (gameState) {
            case MENU:
                drawMenuScreen(g);
                break;
            case PLAYING:
            case GAME_OVER:
            case GAME_WON:
                drawGameScreen(g);
                break;
        }
    }

    /**
     * Menggambar layar game utama, termasuk latar belakang, level, entitas, dan UI.
     */
    private void drawGameScreen(Graphics g) {
        drawBackground((Graphics2D)g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.translate(-cameraX, 0);

        levelManager.draw(g2d);
        for (Enemy enemy : enemies) {
            enemy.draw(g2d);
        }
        for (Obstacle obstacle : obstacles) {
            obstacle.draw(g2d);
            if (obstacle.type == Obstacle.ObstacleType.CANNON) {
                for (Obstacle.Projectile p : obstacle.projectiles) {
                    p.draw(g2d);
                }
            }
        }
        // Gambar Moving Platforms
        for (MovingPlatform platform : movingPlatforms) {
            platform.draw(g2d);
        }
        player.draw(g2d);

        g2d.dispose();

        drawUI(g);

        if (gameState == GameState.GAME_OVER) {
            drawGameOverScreen(g);
        } else if (gameState == GameState.GAME_WON) {
            drawGameWonScreen(g);
        }
    }

    /**
     * Menggambar layar menu utama.
     */
    private void drawMenuScreen(Graphics g) {
        drawBackground((Graphics2D)g);

        g.setFont(new Font("Arial", Font.BOLD, 70));
        String title = "Perjalanan Jiwa";
        int x = (screenWidth - g.getFontMetrics().stringWidth(title)) / 2;
        int y = screenHeight / 3;
        g.setColor(Color.BLACK);
        g.drawString(title, x + 3, y + 3);
        g.setColor(Color.WHITE);
        g.drawString(title, x, y);

        g.setFont(new Font("Arial", Font.BOLD, 30));
        String startText = "Memulai";
        int x2 = (screenWidth - g.getFontMetrics().stringWidth(startText)) / 2;
        int y2 = y + 100;
        if (menuChoice == 0) {
            g.setColor(Color.YELLOW);
            g.drawString(">", x2 - 40, y2);
        } else {
            g.setColor(Color.WHITE);
        }
        g.drawString(startText, x2, y2);

        String exitText = "Keluar";
        int x3 = (screenWidth - g.getFontMetrics().stringWidth(exitText)) / 2;
        int y3 = y2 + 50;
        if (menuChoice == 1) {
            g.setColor(Color.YELLOW);
            g.drawString(">", x3 - 40, y3);
        } else {
            g.setColor(Color.WHITE);
        }
        g.drawString(exitText, x3, y3);
    }

    /**
     * Menggambar lapisan latar belakang paralaks.
     * @param g2d Objek Graphics2D untuk menggambar.
     */
    public void drawBackground(Graphics2D g2d) {
        if (backgroundLayers != null) {
            for(int i = 0; i < backgroundLayers.length; i++) {
                BufferedImage layer = backgroundLayers[i];
                if (layer != null) {
                    int parallaxX = (int) (cameraX * parallaxFactors[i]);
                    int startX = -(parallaxX % screenWidth);
                    g2d.drawImage(layer, startX, 0, screenWidth, screenHeight, null);
                    g2d.drawImage(layer, startX + screenWidth, 0, screenWidth, screenHeight, null);
                }
            }
        } else {
            g2d.setColor(new Color(20, 80, 70));
            g2d.fillRect(0, 0, screenWidth, screenHeight);
        }
    }

    /**
     * Menggambar User Interface (UI) game, termasuk jumlah koin dan nyawa pemain.
     * @param g Objek Graphics untuk menggambar.
     */
    private void drawUI(Graphics g) {
        if (coinImage != null) g.drawImage(coinImage, 15, 8, 28, 28, null);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("x " + coinsCollected, 48, 32);

        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Nyawa: " + player.getLives(), screenWidth - 120, 32);
    }

    /**
     * Menggambar overlay layar "GAME OVER".
     * @param g Objek Graphics untuk menggambar.
     */
    private void drawGameOverScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(cameraX, 0, screenWidth, screenHeight);

        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 80));
        String text = "GAME OVER";
        int x = cameraX + (screenWidth - g.getFontMetrics().stringWidth(text)) / 2;
        int y = screenHeight / 2 - 20;
        g.drawString(text, x, y);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        String restartText = "Tekan Enter untuk Mulai Lagi";
        int x2 = cameraX + (screenWidth - g.getFontMetrics().stringWidth(restartText)) / 2;
        int y2 = y + 50;
        g.drawString(restartText, x2, y2);
    }

    /**
     * Menggambar overlay layar "ANDA MENANG!".
     * @param g Objek Graphics untuk menggambar.
     */
    private void drawGameWonScreen(Graphics g) {
        g.setColor(new Color(0, 100, 0, 150));
        g.fillRect(cameraX, 0, screenWidth, screenHeight);

        g.setColor(Color.GREEN);
        g.setFont(new Font("Arial", Font.BOLD, 80));
        String text = "ANDA MENANG!";
        int x = cameraX + (screenWidth - g.getFontMetrics().stringWidth(text)) / 2;
        int y = screenHeight / 2 - 40;
        g.drawString(text, x, y);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        String scoreText = "Koin Terkumpul: " + coinsCollected;
        int x2 = cameraX + (screenWidth - g.getFontMetrics().stringWidth(scoreText)) / 2;
        g.drawString(scoreText, x2, y + 60);

        String restartText = "Tekan Enter untuk Mulai Lagi";
        int x3 = cameraX + (screenWidth - g.getFontMetrics().stringWidth(restartText)) / 2;
        g.drawString(restartText, x3, y + 100);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        switch (gameState) {
            case MENU:
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
                    menuChoice--;
                    if (menuChoice < 0) menuChoice = 1;
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
                    menuChoice++;
                    if (menuChoice > 1) menuChoice = 0;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (menuChoice == 0) {
                        restartGame();
                    } else if (menuChoice == 1) {
                        System.exit(0);
                    }
                }
                break;
            case GAME_OVER:
            case GAME_WON:
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    gameState = GameState.MENU;
                }
                break;
            case PLAYING:
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) rightPressed = true;
                if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) leftPressed = true;
                if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
                    if (!jumpPressed) {
                        player.requestJump();
                    }
                    jumpPressed = true;
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) rightPressed = false;
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) leftPressed = false;
        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_W || code == KeyEvent.VK_UP) jumpPressed = false;
    }
}
