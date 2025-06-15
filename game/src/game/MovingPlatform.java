package game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage; // Import untuk BufferedImage
import javax.imageio.ImageIO;     // Import untuk ImageIO
import java.io.InputStream;         // Import untuk InputStream
import java.io.IOException;         // Import untuk IOException
import java.awt.RenderingHints;     // Import untuk RenderingHints

public class MovingPlatform {

    private GamePanel gp;
    public double x, y;
    // Mengubah width dan height menjadi public
    public int width;
    public int height;
    private double velX = 0;
    private double velY = 0;
    private int startX, startY; // Posisi awal platform
    private int endX, endY;     // Posisi akhir platform (untuk batasan gerakan)
    private boolean movingRight = true; // Untuk horizontal
    private boolean movingDown = true;  // Untuk vertikal

    public enum PlatformType {
        HORIZONTAL,
        VERTICAL
    }
    private PlatformType type;

    private BufferedImage platformSprite; // Sprite untuk platform bergerak

    /**
     * Konstruktor untuk MovingPlatform.
     * @param x Posisi X awal tile.
     * @param y Posisi Y awal tile.
     * @param type Tipe platform (HORIZONTAL atau VERTICAL).
     * @param moveRange Jarak pergerakan platform dalam piksel.
     * @param speed Kecepatan pergerakan platform.
     * @param gp Referensi ke GamePanel.
     */
    public MovingPlatform(int x, int y, PlatformType type, int moveRange, double speed, GamePanel gp) {
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.type = type;
        this.gp = gp;

        this.width = gp.tileSize;
        this.height = gp.tileSize;

        // Muat sprite platform
        try {
            InputStream is = getClass().getResourceAsStream("/res/Idle.png"); // Menggunakan Idle.png
            if (is != null) {
                platformSprite = ImageIO.read(is);
                System.out.println("DEBUG MovingPlatform: Sprite Idle.png dimuat. Dimensi: " + platformSprite.getWidth() + "x" + platformSprite.getHeight());
            } else {
                System.err.println("ERROR MovingPlatform: Tidak dapat menemukan Idle.png untuk platform bergerak.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR MovingPlatform: Gagal memuat atau membaca Idle.png.");
        }


        if (type == PlatformType.HORIZONTAL) {
            this.velX = speed;
            this.endX = startX + moveRange;
        } else { // Vertical
            this.velY = speed;
            this.endY = startY + moveRange;
        }
    }

    public void update() {
        if (type == PlatformType.HORIZONTAL) {
            x += velX;
            if (movingRight) {
                if (x >= endX) {
                    movingRight = false;
                    velX = -Math.abs(velX);
                }
            } else {
                if (x <= startX) {
                    movingRight = true;
                    velX = Math.abs(velX);
                }
            }
        } else { // Vertical
            y += velY;
            if (movingDown) {
                if (y >= endY) {
                    movingDown = false;
                    velY = -Math.abs(velY);
                }
            } else {
                if (y <= startY) {
                    movingDown = true;
                    velY = Math.abs(velY);
                }
            }
        }
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        // Aktifkan Rendering Hints untuk pixel art agar tidak blur
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        if (platformSprite != null) {
            g2d.drawImage(platformSprite, (int)x, (int)y, width, height, null);
        } else {
            // Fallback: Gambar kotak hijau jika sprite tidak dimuat
            g2d.setColor(new Color(0, 150, 0)); // Hijau gelap
            g2d.fillRect((int)x, (int)y, width, height);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, width, height);
    }

    public double getVelX() { return velX; }
    public double getVelY() { return velY; }
}
