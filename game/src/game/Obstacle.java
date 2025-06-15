package game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.IOException;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Random; // Import Random

public class Obstacle {

    private GamePanel gp;
    public double x, y;
    public double velX, velY;
    private int width, height;

    public enum ObstacleType {
        BIRD,
        ROCK,
        CANNON
    }
    public ObstacleType type;

    private double initialY;
    private double amplitude = 0;
    private double frequency = 0;
    private double angle = 0;

    private BufferedImage rockSpriteSheet;
    private BufferedImage[] rockFrames;
    private int rockAnimationFrame = 0;
    private int rockAnimationTick = 0;
    private final int rockAnimationSpeed = 15;

    private BufferedImage birdSpriteSheet;
    private BufferedImage[] birdFlyingFrames;
    private int birdAnimationFrame = 0;
    private int birdAnimationTick = 0;
    private final int birdAnimationSpeed = 8;

    // Aset Meriam
    private BufferedImage cannonSpriteSheet;
    private BufferedImage[] cannonIdleFrames;
    private BufferedImage[] cannonFiringFrames;
    private int cannonAnimationFrame = 0;
    private int cannonAnimationTick = 0;
    private final int cannonIdleAnimationSpeed = 25;
    private final int cannonFiringAnimationSpeed = 10;

    private int fireTimer = 0;
    private final int FIRE_INTERVAL = 90;
    private final double PROJECTILE_SPEED = 7.0;
    public ArrayList<Projectile> projectiles = new ArrayList<>();

    private boolean isFiring = false;

    private Random random = new Random(); // Untuk variasi burung/batu


    /**
     * Inner class untuk merepresentasikan proyektil meriam.
     */
    public class Projectile {
        public double projX, projY;
        public double projVelX, projVelY;
        public int projWidth, projHeight;

        public Projectile(double startX, double startY, double targetVelX, double targetVelY) {
            this.projX = startX;
            this.projY = startY;
            this.projVelX = targetVelX;
            this.projVelY = targetVelY;
            this.projWidth = gp.tileSize / 2;
            this.projHeight = gp.tileSize / 2;
        }

        public void update() {
            projX += projVelX;
            projY += projVelY;
        }

        public void draw(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            g2d.setColor(Color.DARK_GRAY);
            g2d.fillOval((int)projX, (int)projY, projWidth, projHeight);
        }

        public Rectangle getBounds() {
            return new Rectangle((int)projX, (int)projY, projWidth, projHeight);
        }

        public boolean isOffScreen() {
            return projX < gp.cameraX - projWidth || projX > gp.cameraX + gp.screenWidth + projWidth ||
                   projY < -projHeight || projY > gp.worldHeight + projHeight;
        }
    }


    /**
     * Konstruktor untuk membuat objek rintangan.
     * @param x Posisi X awal.
     * @param y Posisi Y awal.
     * @param type Tipe rintangan (BIRD, ROCK, atau CANNON).
     * @param gp Referensi ke GamePanel.
     * @param extraParams Opsional: [playerY saat spawn untuk burung, rockScale untuk batu]
     */
    public Obstacle(int x, int intY, ObstacleType type, GamePanel gp, double... extraParams) { // Mengubah parameter
        this.x = x;
        this.y = intY;
        this.initialY = intY;
        this.type = type;
        this.gp = gp;

        if (type == ObstacleType.ROCK) {
            loadRockSprite();
        } else if (type == ObstacleType.BIRD) {
            loadBirdSprite();
        } else if (type == ObstacleType.CANNON) {
            loadCannonSprite();
        }

        switch (type) {
            case BIRD:
                int originalBirdWidth = 32;
                int originalBirdHeight = 22;
                this.width = (int)(originalBirdWidth * gp.scale / 1.5);
                this.height = (int)(originalBirdHeight * gp.scale / 1.5);

                this.velX = -4; // Kecepatan burung ditingkatkan
                this.amplitude = gp.tileSize / 4;
                this.frequency = 0.05;

                // Burung lebih pintar: target Y pemain saat spawn
                if (extraParams.length > 0) {
                    double playerY = extraParams[0];
                    // Atur initialY burung agar sedikit di atas/bawah playerY
                    this.initialY = playerY + (random.nextBoolean() ? 1 : -1) * (random.nextInt(gp.tileSize / 2));
                    // Pastikan tidak terlalu tinggi atau terlalu rendah
                    this.initialY = Math.max(gp.tileSize, Math.min(gp.worldHeight - gp.tileSize * 2, this.initialY));
                }
                break;
            case ROCK:
                int originalRockWidth = 54;
                int originalRockHeight = 52;
                
                double rockScale = 1.0; // Default scale
                if (extraParams.length > 0) {
                    rockScale = extraParams[0]; // Gunakan scale dari parameter
                }
                // Variasi ukuran batu
                this.width = (int)(originalRockWidth * gp.scale / 3.0 * rockScale);
                this.height = (int)(originalRockHeight * gp.scale / 3.0 * rockScale);
                
                this.velY = 5;
                this.velX = 0;
                break;
            case CANNON:
                int originalCannonWidth = 32;
                int originalCannonHeight = 32;
                this.width = (int)(originalCannonWidth * gp.scale / 1.0);
                this.height = (int)(originalCannonHeight * gp.scale / 1.0);

                this.velX = 0;
                this.velY = 0;
                this.fireTimer = FIRE_INTERVAL;
                this.y -= (this.height - gp.tileSize);
                break;
        }
    }

    /**
     * Memuat lembar sprite untuk rintangan ROCK dan mengekstrak frame animasi.
     */
    private void loadRockSprite() {
        try {
            InputStream is = getClass().getResourceAsStream("/res/Blink (54x52).png");
            if (is != null) {
                rockSpriteSheet = ImageIO.read(is);
                System.out.println("DEBUG Obstacle: rockSpriteSheet (Blink (54x52).png) dimuat. Dimensi: " + rockSpriteSheet.getWidth() + "x" + rockSpriteSheet.getHeight());
                int frameWidth = 54;
                int frameHeight = 52;
                rockFrames = new BufferedImage[4];
                for (int i = 0; i < 4; i++) {
                    if (rockSpriteSheet.getWidth() >= (i + 1) * frameWidth && rockSpriteSheet.getHeight() >= frameHeight) {
                        rockFrames[i] = rockSpriteSheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
                        System.out.println("DEBUG Obstacle: rockFrame[" + i + "] berhasil diekstrak.");
                    } else {
                        System.err.println("WARNING Obstacle: Tidak cukup frame ditemukan di Blink (54x52).png untuk animasi batu, frame " + i + ". Ukuran SpriteSheet: " + rockSpriteSheet.getWidth() + "x" + rockSpriteSheet.getHeight() + ". Diperlukan: " + ((i + 1) * frameWidth) + "x" + frameHeight);
                        rockFrames[i] = null;
                    }
                }
            } else {
                System.err.println("ERROR Obstacle: Tidak dapat menemukan Blink (54x52).png untuk rintangan batu. Pastikan file ada di 'res/'.");
                rockSpriteSheet = null;
                rockFrames = new BufferedImage[0];
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR Obstacle: Gagal memuat atau membaca Blink (54x52).png untuk rintangan batu.");
            rockSpriteSheet = null;
            rockFrames = new BufferedImage[0];
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("ERROR Obstacle: Koordinat subimage tidak valid untuk Blink (54x52).png. Periksa dimensi sprite.");
            rockSpriteSheet = null;
            rockFrames = new BufferedImage[0];
        }
    }

    /**
     * Memuat lembar sprite untuk rintangan BIRD dan mengekstrak frame animasi terbang.
     */
    private void loadBirdSprite() {
        try {
            InputStream is = getClass().getResourceAsStream("/res/bird_flying_anim_strip_3.png");
            if (is != null) {
                birdSpriteSheet = ImageIO.read(is);
                System.out.println("DEBUG Obstacle: birdSpriteSheet (bird_flying_anim_strip_3.png) dimuat. Dimensi: " + birdSpriteSheet.getWidth() + "x" + birdSpriteSheet.getHeight());
                
                int frameCount = 3;
                int calculatedFrameWidth = birdSpriteSheet.getWidth() / frameCount;
                int calculatedFrameHeight = birdSpriteSheet.getHeight();

                this.width = (int)(calculatedFrameWidth * gp.scale / 1.5);
                this.height = (int)(calculatedFrameHeight * gp.scale / 1.5);

                birdFlyingFrames = new BufferedImage[frameCount];
                for (int i = 0; i < frameCount; i++) {
                    if (birdSpriteSheet.getWidth() >= (i + 1) * calculatedFrameWidth && birdSpriteSheet.getHeight() >= calculatedFrameHeight) {
                        birdFlyingFrames[i] = birdSpriteSheet.getSubimage(i * calculatedFrameWidth, 0, calculatedFrameWidth, calculatedFrameHeight);
                        System.out.println("DEBUG Obstacle: birdFlyingFrames[" + i + "] berhasil diekstrak (W:" + calculatedFrameWidth + ", H:" + calculatedFrameHeight + ").");
                    } else {
                        System.err.println("WARNING Obstacle: Tidak cukup frame ditemukan di bird_flying_anim_strip_3.png untuk animasi burung, frame " + i + ". Ukuran SpriteSheet: " + birdSpriteSheet.getWidth() + "x" + birdSpriteSheet.getHeight() + ". Diperlukan: " + ((i + 1) * calculatedFrameWidth) + "x" + calculatedFrameHeight);
                        birdFlyingFrames[i] = null;
                    }
                }
            } else {
                System.err.println("ERROR Obstacle: Tidak dapat menemukan bird_flying_anim_strip_3.png untuk rintangan burung. Pastikan file ada di 'res/'.");
                birdSpriteSheet = null;
                birdFlyingFrames = new BufferedImage[0];
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR Obstacle: Gagal memuat atau membaca bird_flying_anim_strip_3.png untuk rintangan burung.");
            birdSpriteSheet = null;
            birdFlyingFrames = new BufferedImage[0];
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("ERROR Obstacle: Koordinat subimage tidak valid untuk bird_flying_anim_strip_3.png. Periksa dimensi sprite.");
            birdSpriteSheet = null;
            birdFlyingFrames = new BufferedImage[0];
        }
    }

    /**
     * Memuat lembar sprite untuk CANNON dari cannon_spritesheet.png.
     */
    private void loadCannonSprite() {
        try {
            InputStream is = getClass().getResourceAsStream("/res/cannon_spritesheet.png");
            if (is != null) {
                cannonSpriteSheet = ImageIO.read(is);
                System.out.println("DEBUG Obstacle: cannonSpriteSheet (cannon_spritesheet.png) dimuat. Dimensi: " + cannonSpriteSheet.getWidth() + "x" + cannonSpriteSheet.getHeight());
                
                int frameWidth = 32; // Asumsi lebar frame 32px
                int frameHeight = 32; // Asumsi tinggi frame 32px

                // Inisialisasi frame arrays, default ke array kosong jika ada masalah loading
                cannonIdleFrames = new BufferedImage[0];
                cannonFiringFrames = new BufferedImage[0];

                // --- Pengecekan dimensi minimum yang lebih ketat ---
                int expectedWidthForFiring = (4 + 7) * frameWidth; // Kolom 4 (indeks) + 7 frame = 11 kolom * 32px = 352px
                int expectedHeightForFiring = 2 * frameHeight + frameHeight; // Baris 3 (indeks 2) + 1 baris frame = 3 baris * 32px = 96px
                
                if (cannonSpriteSheet.getWidth() < expectedWidthForFiring || cannonSpriteSheet.getHeight() < expectedHeightForFiring) {
                    System.err.println("ERROR Obstacle: cannon_spritesheet.png terlalu kecil. Diperlukan minimal " + expectedWidthForFiring + "x" + expectedHeightForFiring + ". Dimensi saat ini: " + cannonSpriteSheet.getWidth() + "x" + cannonSpriteSheet.getHeight());
                    cannonSpriteSheet = null; // Set null agar fallback digambar
                    return;
                }

                // Cannon Idle (2 frames, baris 1, frame 0 & 1)
                cannonIdleFrames = new BufferedImage[2];
                if (cannonSpriteSheet.getWidth() >= 2 * frameWidth && cannonSpriteSheet.getHeight() >= frameHeight) {
                    cannonIdleFrames[0] = cannonSpriteSheet.getSubimage(0 * frameWidth, 0, frameWidth, frameHeight);
                    cannonIdleFrames[1] = cannonSpriteSheet.getSubimage(1 * frameWidth, 0, frameWidth, frameHeight);
                    System.out.println("DEBUG Obstacle: cannonIdleFrames berhasil diekstrak (Pos: 0,0 dan 32,0).");
                } else {
                    System.err.println("WARNING Obstacle: Tidak cukup frame untuk cannonIdleFrames. SpriteSheet: " + cannonSpriteSheet.getWidth() + "x" + cannonSpriteSheet.getHeight());
                    cannonIdleFrames = new BufferedImage[0];
                }
                
                // Cannon Firing (7 frames, baris 3 (indeks 2), mulai dari kolom 5 (indeks 4))
                cannonFiringFrames = new BufferedImage[7]; // 7 frames untuk menembak
                for (int i = 0; i < 7; i++) {
                    int srcX = (4 + i) * frameWidth; // Dimulai dari kolom ke-5 (indeks 4)
                    int srcY = 2 * frameHeight; // Baris ke-3 (indeks 2)
                    if (cannonSpriteSheet.getWidth() >= srcX + frameWidth && cannonSpriteSheet.getHeight() >= srcY + frameHeight) {
                         cannonFiringFrames[i] = cannonSpriteSheet.getSubimage(srcX, srcY, frameWidth, frameHeight);
                         System.out.println("DEBUG Obstacle: cannonFiringFrames[" + i + "] diekstrak dari (" + srcX + "," + srcY + ").");
                    } else {
                        System.err.println("WARNING Obstacle: Tidak cukup frame di cannon_spritesheet.png untuk menembak, frame " + i + ". Dimensi SpriteSheet: " + cannonSpriteSheet.getWidth() + "x" + cannonSpriteSheet.getHeight() + ". Diperlukan: " + (srcX + frameWidth) + "x" + (srcY + frameHeight));
                        cannonFiringFrames[i] = null;
                    }
                }

            } else {
                System.err.println("ERROR Obstacle: Tidak dapat menemukan cannon_spritesheet.png untuk meriam. Pastikan file ada di 'res/'.");
                cannonSpriteSheet = null;
                cannonIdleFrames = new BufferedImage[0];
                cannonFiringFrames = new BufferedImage[0];
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR Obstacle: Gagal memuat atau membaca cannon_spritesheet.png.");
            cannonSpriteSheet = null;
            cannonIdleFrames = new BufferedImage[0];
            cannonFiringFrames = new BufferedImage[0];
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("ERROR Obstacle: Koordinat subimage tidak valid untuk cannon_spritesheet.png. Periksa dimensi sprite.");
            cannonSpriteSheet = null;
            cannonIdleFrames = new BufferedImage[0];
            cannonFiringFrames = new BufferedImage[0];
        }
    }


    public void update() {
        switch (type) {
            case BIRD:
                x += velX;
                angle += frequency;
                y = initialY + Math.sin(angle) * amplitude;
                if (birdFlyingFrames != null && birdFlyingFrames.length > 0) {
                    birdAnimationTick++;
                    if (birdAnimationTick >= birdAnimationSpeed) {
                        birdAnimationTick = 0;
                        birdAnimationFrame++;
                        if (birdAnimationFrame >= birdFlyingFrames.length) {
                            birdAnimationFrame = 0;
                        }
                    }
                }
                break;
            case ROCK:
                y += velY;
                if (rockFrames != null && rockFrames.length > 0) {
                    rockAnimationTick++;
                    if (rockAnimationTick >= rockAnimationSpeed) {
                        rockAnimationTick = 0;
                        rockAnimationFrame++;
                        if (rockAnimationFrame >= rockFrames.length) {
                            rockAnimationFrame = 0;
                        }
                    }
                }
                break;
            case CANNON:
                fireTimer--;
                if (fireTimer <= 0) {
                    if (cannonFiringFrames != null && cannonFiringFrames.length > 0 && !isFiring) {
                        projectiles.add(new Projectile(this.x + this.width * 0.1, this.y + this.height * 0.4, -PROJECTILE_SPEED, 0));
                        fireTimer = FIRE_INTERVAL;
                        isFiring = true;
                        cannonAnimationFrame = 0;
                        cannonAnimationTick = 0;
                        System.out.println("DEBUG Obstacle Cannon: Proyektil ditembakkan. isFiring: true, cannonAnimationFrame: " + cannonAnimationFrame);
                    } else if (cannonFiringFrames == null || cannonFiringFrames.length == 0) {
                        System.err.println("WARNING Obstacle Cannon: Tidak dapat menembak proyektil karena cannonFiringFrames tidak valid. Menunda penembakan.");
                        fireTimer = FIRE_INTERVAL / 2;
                    }
                }

                if (isFiring) {
                    cannonAnimationTick++;
                    if (cannonAnimationTick >= cannonFiringAnimationSpeed) {
                        cannonAnimationTick = 0;
                        cannonAnimationFrame++;
                        if (cannonAnimationFrame >= cannonFiringFrames.length) {
                            System.out.println("DEBUG Obstacle Cannon: Animasi menembak selesai. Kembali ke idle.");
                            isFiring = false;
                            cannonAnimationFrame = 0;
                            cannonAnimationTick = 0;
                        }
                    }
                } else {
                    cannonAnimationTick++;
                    if (cannonAnimationTick >= cannonIdleAnimationSpeed) {
                        cannonAnimationTick = 0;
                        cannonAnimationFrame++;
                        if (cannonAnimationFrame >= cannonIdleFrames.length) {
                            cannonAnimationFrame = 0;
                        }
                    }
                }

                projectiles.removeIf(p -> p.isOffScreen());
                for (Projectile p : projectiles) {
                    p.update();
                }
                break;
        }
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        switch (type) {
            case BIRD:
                if (birdFlyingFrames != null && birdFlyingFrames.length > 0 && birdAnimationFrame < birdFlyingFrames.length && birdFlyingFrames[birdAnimationFrame] != null) {
                    if (velX > 0) {
                        g2d.translate(x + width, y);
                        g2d.scale(-1, 1);
                        g2d.drawImage(birdFlyingFrames[birdAnimationFrame], 0, 0, width, height, null);
                    } else {
                        g2d.drawImage(birdFlyingFrames[birdAnimationFrame], (int)x, (int)y, width, height, null);
                    }
                } else {
                    System.err.println("WARNING Obstacle: Sprite burung tidak dapat digambar (null atau frame tidak valid). Menggambar fallback.");
                    g2d.setColor(new Color(150, 0, 150));
                    g2d.fillRect((int)x, (int)y, width, height);
                }
                break;
            case ROCK:
                if (rockFrames != null && rockFrames.length > 0 && rockAnimationFrame < rockFrames.length && rockAnimationFrame != -1 && rockFrames[rockAnimationFrame] != null) {
                    g2d.drawImage(rockFrames[rockAnimationFrame], (int)x, (int)y, width, height, null);
                } else {
                    System.err.println("WARNING Obstacle: Sprite batu tidak dapat digambar (null atau frame tidak valid). Menggambar fallback.");
                    g2d.setColor(new Color(80, 80, 80));
                    g2d.fillOval((int)x, (int)y, width, height);
                }
                break;
            case CANNON:
                BufferedImage currentCannonFrame = null;
                String debugAnimState = "None";

                if (isFiring) {
                    if (cannonFiringFrames != null && cannonFiringFrames.length > 0 && cannonAnimationFrame < cannonFiringFrames.length && cannonFiringFrames[cannonAnimationFrame] != null) {
                        currentCannonFrame = cannonFiringFrames[cannonAnimationFrame];
                        debugAnimState = "Firing";
                    }
                } else {
                    if (cannonIdleFrames != null && cannonIdleFrames.length > 0 && cannonAnimationFrame < cannonIdleFrames.length && cannonIdleFrames[cannonAnimationFrame] != null) {
                        currentCannonFrame = cannonIdleFrames[cannonAnimationFrame];
                        debugAnimState = "Idle";
                    }
                }

                if (currentCannonFrame != null) {
                    g2d.drawImage(currentCannonFrame, (int)x, (int)y, width, height, null);
                } else {
                    System.err.println("WARNING Obstacle: Sprite meriam tidak dapat digambar (status: " + debugAnimState + ", frameIndex: " + cannonAnimationFrame + ", isFiring: " + isFiring + "). Menggambar fallback.");
                    g2d.setColor(new Color(50, 50, 50));
                    g2d.fillRect((int)x, (int)y, width, height);
                }

                for (Projectile p : projectiles) {
                    p.draw(g2d);
                }
                break;
        }
        g2d.dispose();
    }

    public Rectangle getBounds() {
        if (type == ObstacleType.BIRD) {
            int hitboxWidth = (int)(width * 0.75);
            int hitboxHeight = (int)(height * 0.75);
            int hitboxX = (int)x + (width - hitboxWidth) / 2;
            int hitboxY = (int)y + (height - hitboxHeight) / 2;
            return new Rectangle(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
        } else if (type == ObstacleType.CANNON) {
            int hitboxWidth = (int)(width * 0.8);
            int hitboxHeight = (int)(height * 0.8);
            int hitboxX = (int)x + (width - hitboxWidth) / 2;
            int hitboxY = (int)y + (height - hitboxHeight);
            return new Rectangle(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
        }
        return new Rectangle((int)x, (int)y, width, height);
    }

    public boolean isOffScreen() {
        if (type == ObstacleType.BIRD) {
            return x + width < gp.cameraX - width;
        }
        else if (type == ObstacleType.ROCK) {
            return y > gp.worldHeight;
        }
        return false;
    }
}
