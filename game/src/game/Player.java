package game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList; // Import untuk ArrayList

public class Player {

    private GamePanel gp; // Referensi ke GamePanel
    public double x, y; // Posisi pemain (floating point untuk pergerakan yang lebih halus)
    private double velX = 0; // Kecepatan horizontal pemain
    public double velY = 0; // Kecepatan vertikal pemain
    public int width; // Diubah dari private menjadi public
    public int height; // Diubah dari private menjadi public
    private final double moveSpeed = 4.0; // Kecepatan gerakan horizontal
    private final double jumpStrength = -23.0; // Kecepatan awal ke atas saat melompat
    private final double gravity = 0.8; // Gravitasi yang diterapkan setiap frame
    public boolean onGround = false; // Diubah dari private menjadi public
    private String direction = "right"; // Arah hadap pemain ("left" atau "right")
    private BufferedImage spriteSheet; // Gambar lembar sprite penuh
    private BufferedImage[] idleFrames, walkFrames; // Array untuk menyimpan frame animasi
    private int animationFrame = 0; // Frame animasi saat ini
    private int animationTick = 0; // Penghitung untuk mengontrol kecepatan animasi
    private final int animationSpeed = 8; // Berapa banyak tick game sebelum frame animasi berikutnya
    private BufferedImage[] previousAnimation = null; // Untuk mendeteksi perubahan animasi dan mereset frame

    public int health;
    private boolean invincible = false; // True jika pemain sementara tidak bisa diserang setelah menerima damage
    private int invincibleCounter = 0; // Timer untuk durasi invincibility
    private final int INVINCIBLE_DURATION_FRAMES = 120; // Durasi invincibility (2 detik pada 60 FPS)
    private final int MAX_HEALTH = 5; // Kesehatan maksimum yang bisa dimiliki pemain

    // Variabel Jump Buffer
    private int jumpBufferTimer = 0;
    private final int JUMP_BUFFER_DURATION = 8; // Durasi jump buffer dalam frame (misal: 8 frame = 0.13 detik)

    // Variabel Coyote Time
    private int coyoteTimeCounter = 0;
    private final int COYOTE_TIME_DURATION = 6; // Durasi coyote time dalam frame (misal: 6 frame = 0.1 detik)

    // Referensi ke platform bergerak saat ini (jika pemain berdiri di atasnya)
    private MovingPlatform currentMovingPlatform = null;


    public Player(GamePanel gp) {
        this.gp = gp;
        fullReset(); // Menginisialisasi status pemain
        loadSprite(); // Memuat gambar sprite pemain
    }

    /**
     * Meningkatkan kesehatan pemain, hingga MAX_HEALTH.
     * Juga memperbarui ukuran pemain berdasarkan kesehatan.
     */
    public void gainHealth() {
        if (health < MAX_HEALTH) {
            health++;
            updateSize(); // Pemain sedikit membesar dengan lebih banyak kesehatan
        }
    }

    /**
     * Memperbarui ukuran kotak tabrakan pemain berdasarkan kesehatan saat ini.
     * Karakter pemain secara visual akan berskala dengan kesehatan.
     */
    private void updateSize() {
        double baseWidth = 16 * gp.scale;
        double baseHeight = 28 * gp.scale;
        double scaleFactor = 0.5 + (double)health / MAX_HEALTH * 0.5;
        width = (int)(baseWidth * scaleFactor);
        height = (int)(baseHeight * scaleFactor);
    }

    /**
     * Mereset posisi, kecepatan, kesehatan, dan status invincibility pemain ke nilai awal.
     * Dipanggil saat memulai game baru atau restart.
     */
    public void fullReset() {
        x = gp.tileSize * 2;
        y = gp.tileSize * 11;
        velX = 0;
        velY = 0;
        health = MAX_HEALTH;
        updateSize();
        invincible = false;
        invincibleCounter = 0;
        jumpBufferTimer = 0;
        coyoteTimeCounter = 0;
        onGround = false; // Pastikan direset
        currentMovingPlatform = null; // Pastikan direset
    }

    /**
     * Mengurangi kesehatan pemain jika tidak sedang invincible.
     * Memulai periode invincibility setelah menerima damage.
     */
    public void takeDamage() {
        if (invincible) {
            return;
        }

        health--;

        if (health > 0) {
            updateSize();
            invincible = true;
            invincibleCounter = INVINCIBLE_DURATION_FRAMES;
        }
    }

    /**
     * Menerapkan efek pantulan pada pemain, biasanya digunakan setelah menginjak musuh.
     */
    public void stompBounce() {
        this.velY = jumpStrength * 0.7;
        onGround = false;
        coyoteTimeCounter = 0;
        currentMovingPlatform = null;
    }

    /**
     * Meminta pemain untuk melompat, memutar efek suara lompat jika di tanah.
     * Mengatur jump buffer jika tidak di tanah, atau melompat jika coyote time aktif.
     */
    public void requestJump() {
        if (onGround || coyoteTimeCounter > 0) {
            gp.soundManager.playSoundEffect("res/jump.wav");
            velY = jumpStrength;
            onGround = false;
            jumpBufferTimer = 0;
            coyoteTimeCounter = 0;
            currentMovingPlatform = null;
            System.out.println("DEBUG Player: Lompat berhasil! velY: " + velY + ", onGround: " + onGround + (onGround ? " (Langsung)" : " (Dari Coyote Time)"));
        } else {
            jumpBufferTimer = JUMP_BUFFER_DURATION;
            System.out.println("DEBUG Player: Gagal melompat (tidak di tanah), jumpBuffer diaktifkan: " + jumpBufferTimer + ". onGround: " + onGround + ", velY: " + velY);
        }
    }


    /**
     * Mengembalikan kotak batas tabrakan pemain.
     * @return Sebuah Rectangle yang merepresentasikan posisi dan dimensi pemain saat ini.
     */
    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, width, height);
    }

    /**
     * Mengembalikan kesehatan pemain saat ini.
     * @return Nilai kesehatan saat ini.
     */
    public int getLives() {
        return health;
    }

    /**
     * Memuat lembar sprite pemain dari sumber daya dan mengekstrak frame animasi.
     * Menyediakan fallback jika lembar sprite tidak dapat dimuat atau tidak valid.
     */
    private void loadSprite() {
        try {
            InputStream is = getClass().getResourceAsStream("/res/AnimationSheet_Character.png");
            if (is != null) {
                spriteSheet = ImageIO.read(is);
                System.out.println("DEBUG Player: Lembar sprite pemain berhasil dimuat. Dimensi: " + spriteSheet.getWidth() + "x" + spriteSheet.getHeight());
            } else {
                System.err.println("ERROR Player: Lembar sprite pemain tidak ditemukan di /res/AnimationSheet_Character.png. Menggunakan kotak default.");
                spriteSheet = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = spriteSheet.createGraphics();
                g2.setColor(Color.BLUE);
                g2.fillRect(0, 0, 100, 100);
                g2.dispose();
            }

            int spriteWidth = 16;
            int spriteHeight = 28;

            if (spriteSheet != null && spriteSheet.getWidth() >= 168 + spriteWidth && spriteSheet.getHeight() >= 161 + spriteHeight) {
                idleFrames = new BufferedImage[2];
                walkFrames = new BufferedImage[6];

                idleFrames[0] = spriteSheet.getSubimage(8, 5, spriteWidth, spriteHeight);
                idleFrames[1] = spriteSheet.getSubimage(40, 5, spriteWidth, spriteHeight);
                System.out.println("DEBUG Player: Idle frames berhasil diekstrak.");

                walkFrames[0] = spriteSheet.getSubimage(8, 69, spriteWidth, spriteHeight);
                walkFrames[1] = spriteSheet.getSubimage(40, 69, spriteWidth, spriteHeight);
                walkFrames[2] = spriteSheet.getSubimage(72, 69, spriteWidth, spriteHeight);
                walkFrames[3] = spriteSheet.getSubimage(104, 69, spriteWidth, spriteHeight);
                walkFrames[4] = spriteSheet.getSubimage(136, 69, spriteWidth, spriteHeight);
                walkFrames[5] = spriteSheet.getSubimage(168, 69, spriteWidth, spriteHeight);
                System.out.println("DEBUG Player: Walk frames berhasil diekstrak.");
            } else {
                System.err.println("ERROR Player: Lembar sprite pemain null atau terlalu kecil untuk ekstraksi subimage. Menginisialisasi frame animasi kosong.");
                idleFrames = new BufferedImage[0];
                walkFrames = new BufferedImage[0];
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR Player: Membaca gambar lembar sprite pemain gagal. Menggunakan kotak default dan frame animasi kosong.");
            spriteSheet = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = spriteSheet.createGraphics();
            g2.setColor(Color.BLUE);
            g2.fillRect(0, 0, 100, 100);
            g2.dispose();
            idleFrames = new BufferedImage[0];
            walkFrames = new BufferedImage[0];
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("ERROR Player: Koordinat subimage tidak valid. Pastikan lembar sprite dan koordinat benar.");
            idleFrames = new BufferedImage[0];
            walkFrames = new BufferedImage[0];
        }
    }

    /**
     * Memperbarui status pemain, termasuk pergerakan, gravitasi, tabrakan, dan animasi.
     * @param levelMap Array 2D yang merepresentasikan level game.
     * @param tileSize Ukuran satu tile dalam piksel.
     * @param jumpPressed True jika tombol lompat ditekan.
     * @param leftPressed True jika tombol gerakan kiri ditekan.
     * @param rightPressed True jika tombol gerakan kanan ditekan.
     * @param movingPlatforms Daftar platform bergerak di level.
     */
    public void update(int[][] levelMap, int tileSize, boolean jumpPressed, boolean leftPressed, boolean rightPressed, ArrayList<MovingPlatform> movingPlatforms) {
        if (invincible) {
            invincibleCounter--;
            if (invincibleCounter <= 0) {
                invincible = false;
            }
        }

        velX = 0;
        if (rightPressed) { velX = moveSpeed; direction = "right"; }
        if (leftPressed) { velX = -moveSpeed; direction = "left"; }

        // Menerapkan kecepatan platform bergerak ke pemain jika sedang berdiri di atasnya
        // Pastikan currentMovingPlatform masih solid (tidak null dan pemain masih di atasnya)
        if (onGround && currentMovingPlatform != null) {
            // Cek apakah pemain masih berinteraksi dengan platform bergerak
            Rectangle playerFeetBounds = new Rectangle((int)x + width/4, (int)y + height - 5, width/2, 5); // Area kaki pemain
            if (playerFeetBounds.intersects(currentMovingPlatform.getBounds())) {
                x += currentMovingPlatform.getVelX();
                y += currentMovingPlatform.getVelY();
                System.out.println("DEBUG Player Update: Bergerak bersama MovingPlatform. Player X: " + x + ", Y: " + y);
            } else {
                currentMovingPlatform = null; // Pemain sudah tidak di platform ini
                onGround = false; // Set onGround menjadi false agar gravitasi bekerja
                System.out.println("DEBUG Player Update: Terjatuh dari MovingPlatform.");
            }
        }


        if (!onGround) {
            velY += gravity;
            if (coyoteTimeCounter > 0) {
                coyoteTimeCounter--;
            }
        } else {
            coyoteTimeCounter = COYOTE_TIME_DURATION;
        }

        if (jumpBufferTimer > 0) {
            jumpBufferTimer--;
            if (onGround) {
                gp.soundManager.playSoundEffect("res/jump.wav");
                velY = jumpStrength;
                onGround = false;
                jumpBufferTimer = 0;
                coyoteTimeCounter = 0;
                currentMovingPlatform = null;
                System.out.println("DEBUG Player: Lompat berhasil (dari buffer)! velY: " + velY + ", onGround: " + onGround);
            }
        }

        // Panggil handleCollisions dengan movingPlatforms
        move(levelMap, tileSize, movingPlatforms);
        updateAnimationTick(leftPressed, rightPressed);
        System.out.println("DEBUG Player Update: Posisi X: " + x + ", Y: " + y + ", velY: " + velY + ", onGround: " + onGround + ", JumpBuffer: " + jumpBufferTimer + ", CoyoteTime: " + coyoteTimeCounter);
    }

    /**
     * Applies horizontal and vertical movement and handles collisions with level tiles and moving platforms.
     * @param levelMap The 2D array representing the game level.
     * @param tileSize The size of a single tile in pixels.
     * @param movingPlatforms List of active moving platforms.
     */
    private void move(int[][] levelMap, int tileSize, ArrayList<MovingPlatform> movingPlatforms) {
        x += velX;
        handleCollisions(levelMap, tileSize, 'x', movingPlatforms); // Pass movingPlatforms

        y += velY;
        handleCollisions(levelMap, tileSize, 'y', movingPlatforms); // Pass movingPlatforms
    }

    /**
     * Handles collisions between the player and level tiles for a given axis, including moving platforms.
     * @param levelMap The 2D array representing the game level.
     * @param tileSize The size of a single tile in pixels.
     * @param axis The axis to check collision for ('x' or 'y').
     * @param movingPlatforms List of active moving platforms.
     */
    private void handleCollisions(int[][] levelMap, int tileSize, char axis, ArrayList<MovingPlatform> movingPlatforms) {
        boolean wasOnGroundBeforeCollisionCheck = onGround;
        if (axis == 'y') {
            onGround = false; // Asumsikan tidak di tanah di awal pemeriksaan tabrakan vertikal
            currentMovingPlatform = null; // Reset platform saat ini untuk setiap cek Y-axis
        }

        Rectangle playerBounds = getBounds();

        int startCol = Math.max(0, (int) (playerBounds.getX() / tileSize) - 1);
        int endCol = Math.min(gp.maxWorldCol - 1, (int) ((playerBounds.getX() + playerBounds.getWidth()) / tileSize) + 1);
        int startRow = Math.max(0, (int) (playerBounds.getY() / tileSize) - 1);
        int endRow = Math.min(gp.maxWorldRow - 1, (int) ((playerBounds.getY() + playerBounds.getHeight()) / tileSize) + 1);

        // --- Cek tabrakan dengan Moving Platforms terlebih dahulu ---
        if (axis == 'y') {
            for (MovingPlatform platform : movingPlatforms) {
                Rectangle platformBounds = platform.getBounds();
                if (playerBounds.intersects(platformBounds)) {
                    // Cek jika pemain mendarat di atas platform bergerak
                    // Player velY > 0 (jatuh) atau mendekati 0. Tambahkan toleransi yang lebih besar.
                    if (velY >= 0 && (playerBounds.getMaxY() <= platformBounds.getMinY() + (velY + 8))) { // Toleransi pendaratan yang lebih besar (misal 8 piksel)
                        y = platformBounds.y - height; // Atur posisi pemain tepat di atas platform
                        velY = 0;
                        onGround = true;
                        currentMovingPlatform = platform; // Tandai platform ini sebagai pijakan
                        System.out.println("DEBUG Player Collision: Mendarat di MovingPlatform di X:" + platform.x + ", Y:" + platform.y + ", tol: " + (velY + 8));
                        return; // Penting: Kembali setelah tabrakan platform karena posisi sudah diatur
                    } else if (velY < 0 && playerBounds.getMinY() <= platformBounds.getMaxY()) { // Membentur bagian bawah platform
                        y = platformBounds.y + platform.height;
                        velY = 0;
                        System.out.println("DEBUG Player Collision: Membentur bagian bawah MovingPlatform di X:" + platform.x + ", Y:" + platform.y);
                        return;
                    }
                }
            }
        }

        // --- Kemudian, cek tabrakan dengan tile statis level map ---
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (row < 0 || row >= gp.maxWorldRow || col < 0 || col >= gp.maxWorldCol) {
                    continue;
                }

                int tileType = levelMap[row][col];

                // Lewati tile yang tidak solid atau sudah ditangani di GamePanel (seperti DEATH_ZONE)
                if (tileType == LevelManager.TILE_EMPTY || 
                    tileType == LevelManager.TILE_COIN || 
                    tileType == LevelManager.TILE_HEALTH || 
                    tileType == LevelManager.TILE_EXIT || 
                    tileType == LevelManager.TILE_DEATH_ZONE ||
                    tileType == LevelManager.TILE_MOVING_H || // Sudah ditangani di atas
                    tileType == LevelManager.TILE_MOVING_V) continue; // Sudah ditangani di atas

                Rectangle tileBounds = new Rectangle(col * tileSize, row * tileSize, tileSize, tileSize);

                if (playerBounds.intersects(tileBounds)) {
                    if (tileType == LevelManager.TILE_SPIKE) {
                        takeDamage();
                        return;
                    }
                    if (axis == 'x') {
                        if (velX > 0) {
                            x = tileBounds.x - width;
                            System.out.println("DEBUG Player Collision: Tabrakan horizontal (kanan) di tile [" + row + "," + col + "] Tipe: " + tileType + ". velX direset ke 0. Pemain di X: " + x);
                        } else if (velX < 0) {
                            x = tileBounds.x + tileSize;
                            System.out.println("DEBUG Player Collision: Tabrakan horizontal (kiri) di tile [" + row + "," + col + "] Tipe: " + tileType + ". velX direset ke 0. Pemain di X: " + x);
                        }
                        velX = 0;
                        return; // Kembali setelah tabrakan horizontal
                    } else { // axis == 'y'
                        if (velY > 0) { // Jatuh ke bawah
                            y = tileBounds.y - height;
                            onGround = true;
                            velY = 0;
                            if (!wasOnGroundBeforeCollisionCheck) {
                                System.out.println("DEBUG Player Collision: Mendarat di tile [" + row + "," + col + "] Tipe: " + tileType + ". onGround: true, velY: 0. Pemain di Y: " + y);
                            }
                        } else if (velY < 0) { // Bergerak ke atas (melompat)
                            y = tileBounds.y + tileSize;
                            velY = 0;
                            System.out.println("DEBUG Player Collision: Membentur langit-langit di tile [" + row + "," + col + "] Tipe: " + tileType + ". velY: 0. Pemain di Y: " + y);
                        }

                        if (tileType == LevelManager.TILE_BREAKABLE) {
                            gp.setTile(row, col, LevelManager.TILE_EMPTY);
                            System.out.println("DEBUG Player Collision: Blok pecah di [" + row + "," + col + "] dihancurkan.");
                        }
                        return; // Kembali setelah tabrakan vertikal
                    }
                }
            }
        }
        
        // Jika tidak bertabrakan dengan platform atau tile statis di sumbu Y
        if (axis == 'y' && !onGround && wasOnGroundBeforeCollisionCheck) {
            System.out.println("DEBUG Player Collision: Meninggalkan tanah, onGround: false.");
        }
    }

    /**
     * Memperbarui frame animasi pemain berdasarkan status pergerakan.
     */
    private void updateAnimationTick(boolean leftPressed, boolean rightPressed) {
        if (walkFrames == null || idleFrames == null || idleFrames.length == 0 || walkFrames.length == 0) return;

        BufferedImage[] currentAnimation = getCurrentAnimation(leftPressed, rightPressed);
        if (currentAnimation != previousAnimation) {
            animationFrame = 0;
            animationTick = 0;
        }
        previousAnimation = currentAnimation;

        animationTick++;
        if (animationTick >= animationSpeed) {
            animationTick = 0;
            animationFrame++;
            if (animationFrame >= currentAnimation.length) {
                animationFrame = 0;
            }
        }
    }

    /**
     * Menentukan array animasi mana (idle atau walk) yang harus digunakan saat ini.
     */
    private BufferedImage[] getCurrentAnimation(boolean leftPressed, boolean rightPressed) {
        if (idleFrames == null || walkFrames == null || idleFrames.length == 0) {
            return new BufferedImage[0];
        }

        if (!onGround) return idleFrames;
        if (leftPressed || rightPressed) return walkFrames;
        return idleFrames;
    }

    /**
     * Menggambar karakter pemain di layar.
     */
    public void draw(Graphics g) {
        if (invincible) {
            if (invincibleCounter % 20 < 10) {
                return;
            }
        }

        BufferedImage imageToDraw = null;
        if (previousAnimation != null && previousAnimation.length > 0 && animationFrame < previousAnimation.length) {
            imageToDraw = previousAnimation[animationFrame];
        }

        if (imageToDraw != null) {
            Graphics2D g2d = (Graphics2D) g.create();

            g2d.translate((int)x, (int)y);

            if (direction.equals("left")) {
                g2d.translate(width, 0);
                g2d.scale(-1, 1);
            }

            g2d.drawImage(imageToDraw, 0, 0, width, height, null);
            g2d.dispose();
        } else {
            g.setColor(Color.RED);
            g.fillRect((int)x, (int)y, width, height);
        }
    }
}
