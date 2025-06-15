package game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Random; // Import untuk Random

public class LevelManager {

    private GamePanel gp;
    public int[][] levelMap;
    public int[][] originalLevelMap;
    private BufferedImage coinImage;
    private BufferedImage groundTileset;
    private BufferedImage groundSurfaceTile;
    private BufferedImage groundBaseTile;
    private Random random = new Random(); // Inisialisasi Random

    // Definisikan konstanta untuk tipe tile baru
    public static final int TILE_EMPTY = 0;
    public static final int TILE_GROUND = 1;
    public static final int TILE_BREAKABLE = 2; // Blok yang bisa dihancurkan
    public static final int TILE_SPIKE = 3;     // Duri
    public static final int TILE_COIN = 4;
    public static final int TILE_HEALTH = 5;
    public static final int TILE_EXIT = 6;
    public static final int TILE_MOVING_H = 7; // Platform bergerak horizontal
    public static final int TILE_MOVING_V = 8; // Platform bergerak vertikal
    public static final int TILE_DEATH_ZONE = 9; // Lubang kematian instan


    public LevelManager(GamePanel gp) {
        this.gp = gp;
        this.coinImage = gp.getCoinImage();
        loadGroundAssets();
        createLevel();
    }

    /**
     * Memuat aset gambar untuk ground dari tileset_64x64(new).png.
     */
    private void loadGroundAssets() {
        try {
            InputStream isTileset = getClass().getResourceAsStream("/res/tileset_64x64(new).png");
            if (isTileset != null) {
                groundTileset = ImageIO.read(isTileset);
                System.out.println("DEBUG LevelManager: Ground tileset (tileset_64x64(new).png) dimuat. Dimensi: " + groundTileset.getWidth() + "x" + groundTileset.getHeight());
            } else {
                System.err.println("ERROR LevelManager: Tidak dapat menemukan file tileset_64x64(new).png! Pastikan file ada di 'res/'.");
                return;
            }

            int tileSetTileSize = 64;

            if (groundTileset.getWidth() >= tileSetTileSize && groundTileset.getHeight() >= tileSetTileSize) {
                groundSurfaceTile = groundTileset.getSubimage(0, 0, tileSetTileSize, tileSetTileSize);
                System.out.println("DEBUG LevelManager: groundSurfaceTile berhasil diekstrak dari (0,0).");
            } else {
                System.err.println("ERROR LevelManager: Tileset terlalu kecil untuk mengekstrak groundSurfaceTile dari (0,0). Dimensi tileset: " + groundTileset.getWidth() + "x" + groundTileset.getHeight());
            }

            if (groundTileset.getWidth() >= tileSetTileSize && groundTileset.getHeight() >= 2 * tileSetTileSize) {
                groundBaseTile = groundTileset.getSubimage(0, 64, tileSetTileSize, tileSetTileSize);
                System.out.println("DEBUG LevelManager: groundBaseTile berhasil diekstrak dari (0,64).");
            } else {
                System.err.println("ERROR LevelManager: Tileset terlalu kecil untuk mengekstrak groundBaseTile dari (0,64). Dimensi tileset: " + groundTileset.getWidth() + "x" + groundTileset.getHeight());
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR LevelManager: Terjadi kesalahan saat memuat atau memotong gambar tileset_64x64(new).png.");
            groundTileset = null;
            groundSurfaceTile = null;
            groundBaseTile = null;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("ERROR LevelManager: Koordinat subimage tidak valid untuk tileset_64x64(new).png. Periksa dimensi sprite atau koordinat pemotongan.");
            groundTileset = null;
            groundSurfaceTile = null;
            groundBaseTile = null;
        }
    }

    /**
     * Membuat dan menginisialisasi peta level dengan desain yang lebih mudah dan penempatan koin acak.
     */
    public void createLevel() {
        originalLevelMap = new int[gp.maxWorldRow][gp.maxWorldCol];

        // Inisialisasi semua dengan ruang kosong
        for (int r = 0; r < gp.maxWorldRow; r++) {
            for (int c = 0; c < gp.maxWorldCol; c++) {
                originalLevelMap[r][c] = TILE_EMPTY;
            }
        }

        // --- DESAIN LEVEL LEBIH MUDAH & SEIMBANG ---
        // Player start di (2, gp.maxWorldRow - 3) = (2, 12)

        // Lantai awal yang panjang dan aman
        for (int c = 0; c < 15; c++) { 
            originalLevelMap[gp.maxWorldRow - 2][c] = TILE_GROUND;
            originalLevelMap[gp.maxWorldRow - 1][c] = TILE_GROUND;
        }

        // Lubang kecil 1 (2 tile) dengan pijakan yang jelas
        for (int c = 15; c < 17; c++) originalLevelMap[gp.maxWorldRow - 1][c] = TILE_DEATH_ZONE;
        originalLevelMap[gp.maxWorldRow - 2][17] = TILE_GROUND; originalLevelMap[gp.maxWorldRow - 1][17] = TILE_GROUND; // Pijakan setelah lubang

        // Platform naik sederhana dan aman
        originalLevelMap[gp.maxWorldRow - 4][19] = TILE_GROUND;
        originalLevelMap[gp.maxWorldRow - 5][20] = TILE_GROUND;
        originalLevelMap[gp.maxWorldRow - 6][21] = TILE_GROUND; // Pijakan tinggi

        // Lubang dengan platform bergerak horizontal yang dapat dicapai (jarak lebih pendek, platform bergerak lebih besar)
        for (int c = 22; c < 26; c++) originalLevelMap[gp.maxWorldRow - 1][c] = TILE_DEATH_ZONE; // Lubang 4 tile
        originalLevelMap[gp.maxWorldRow - 7][24] = TILE_MOVING_H; // Range 3 tiles, speed 1.0 (lebih lambat, lebih dekat)

        // Platform setelah moving H (lebih lebar)
        for (int c = 26; c < 32; c++) { // Lebih lebar
            originalLevelMap[gp.maxWorldRow - 3][c] = TILE_GROUND;
            originalLevelMap[gp.maxWorldRow - 2][c] = TILE_GROUND;
            originalLevelMap[gp.maxWorldRow - 1][c] = TILE_GROUND;
        }

        // Area dengan duri & blok pecah (lebih jarang)
        originalLevelMap[gp.maxWorldRow - 4][30] = TILE_SPIKE; // Duri
        originalLevelMap[gp.maxWorldRow - 4][31] = TILE_BREAKABLE; // Blok pecah

        // Meriam pertama (ditempatkan dengan ruang yang lebih aman)
        originalLevelMap[gp.maxWorldRow - 3][35] = TILE_GROUND; originalLevelMap[gp.maxWorldRow - 2][35] = TILE_GROUND;

        // Lubang dengan platform bergerak vertikal (mulai lebih rendah, range lebih pendek)
        for (int c = 37; c < 41; c++) originalLevelMap[gp.maxWorldRow - 1][c] = TILE_DEATH_ZONE; // Lubang 4 tile
        originalLevelMap[gp.maxWorldRow - 8][39] = TILE_MOVING_V; // Range 2 tiles, speed 0.8 (lebih lambat)

        // Platform setelah moving V (lebih lebar)
        for (int c = 41; c < 48; c++) { // Lebih lebar
            originalLevelMap[gp.maxWorldRow - 3][c] = TILE_GROUND;
            originalLevelMap[gp.maxWorldRow - 2][c] = TILE_GROUND;
            originalLevelMap[gp.maxWorldRow - 1][c] = TILE_GROUND;
        }

        // Area dengan kesehatan dan duri (lebih mudah diakses)
        originalLevelMap[gp.maxWorldRow - 4][45] = TILE_HEALTH; // Health pack
        originalLevelMap[gp.maxWorldRow - 4][47] = TILE_SPIKE; // Duri di ujung platform

        // Meriam kedua
        originalLevelMap[gp.maxWorldRow - 3][53] = TILE_GROUND; originalLevelMap[gp.maxWorldRow - 2][53] = TILE_GROUND;

        // Rangkaian platform melayang dengan jarak yang lebih mudah
        originalLevelMap[gp.maxWorldRow - 5][58] = TILE_GROUND; // Platform 1
        originalLevelMap[gp.maxWorldRow - 5][61] = TILE_GROUND; // Platform 2
        originalLevelMap[gp.maxWorldRow - 5][64] = TILE_GROUND; // Platform 3

        // Area panjang menuju Exit (lebih aman)
        for (int c = 68; c < gp.maxWorldCol - 6; c++) {
            originalLevelMap[gp.maxWorldRow - 2][c] = TILE_GROUND;
            originalLevelMap[gp.maxWorldRow - 1][c] = TILE_GROUND;
        }
        
        // Duri di jalur exit (lebih sedikit dan lebih mudah dihindari)
        originalLevelMap[gp.maxWorldRow - 3][gp.maxWorldCol - 10] = TILE_SPIKE; // Hanya satu duri

        // Lubang terakhir sebelum exit (lebih pendek)
        for (int c = gp.maxWorldCol - 8; c < gp.maxWorldCol - 6; c++) originalLevelMap[gp.maxWorldRow - 1][c] = TILE_DEATH_ZONE; // Lubang 2 tile

        // Exit
        originalLevelMap[gp.maxWorldRow - 3][gp.maxWorldCol - 5] = TILE_EXIT;


        // --- Penempatan Koin Acak (Tepat 15 koin) ---
        int coinsToPlace = 15;
        int placedCoins = 0;
        int maxAttempts = 1000;

        while (placedCoins < coinsToPlace && maxAttempts > 0) {
            int r = random.nextInt(gp.maxWorldRow - 4) + 1; // Koin di atas baris paling bawah, hindari baris 0 (paling atas)
            int c = random.nextInt(gp.maxWorldCol);

            // Cek apakah tile kosong dan di atas pijakan yang valid
            if (originalLevelMap[r][c] == TILE_EMPTY) {
                // Periksa tile di bawahnya harus ground, moving H, atau moving V
                if (r + 1 < gp.maxWorldRow && 
                    (originalLevelMap[r+1][c] == TILE_GROUND || 
                     originalLevelMap[r+1][c] == TILE_MOVING_H || 
                     originalLevelMap[r+1][c] == TILE_MOVING_V)) 
                {
                    // Tambahan cek: Pastikan tidak spawn di atau terlalu dekat dengan duri atau death zone atau langsung di atas moving platform
                    boolean safeToSpawn = true;
                    // Cek tile koin itu sendiri dan 1 tile di bawahnya, kiri, kanan
                    if (originalLevelMap[r][c] == TILE_SPIKE || originalLevelMap[r][c] == TILE_DEATH_ZONE ||
                        (r + 1 < gp.maxWorldRow && (originalLevelMap[r+1][c] == TILE_SPIKE || originalLevelMap[r+1][c] == TILE_DEATH_ZONE)) ||
                        (c - 1 >= 0 && (originalLevelMap[r][c-1] == TILE_SPIKE || originalLevelMap[r][c-1] == TILE_DEATH_ZONE)) ||
                        (c + 1 < gp.maxWorldCol && (originalLevelMap[r][c+1] == TILE_SPIKE || originalLevelMap[r][c+1] == TILE_DEATH_ZONE))
                        ) {
                        safeToSpawn = false;
                    }
                    // Pastikan tidak tumpang tindih dengan platform bergerak secara visual
                    if (originalLevelMap[r][c] == TILE_MOVING_H || originalLevelMap[r][c] == TILE_MOVING_V) {
                        safeToSpawn = false; // Hindari spawn koin langsung di tile moving platform
                    }

                    if (safeToSpawn) {
                        originalLevelMap[r][c] = TILE_COIN;
                        placedCoins++;
                    }
                }
            }
            maxAttempts--;
        }
        System.out.println("DEBUG LevelManager: " + placedCoins + " koin ditempatkan secara acak.");

        // Fallback jika tidak semua koin bisa ditempatkan secara acak (untuk memastikan 15 koin selalu ada)
        if (placedCoins < coinsToPlace) {
            System.out.println("WARNING LevelManager: Gagal menempatkan semua koin secara acak. Menempatkan sisa koin di posisi fallback.");
            for (int i = placedCoins; i < coinsToPlace; i++) {
                // Tempatkan koin di area awal level yang aman dan di atas ground yang solid
                if (originalLevelMap[gp.maxWorldRow - 3][i + 2] == TILE_EMPTY && originalLevelMap[gp.maxWorldRow - 2][i+2] == TILE_GROUND) {
                    originalLevelMap[gp.maxWorldRow - 3][i + 2] = TILE_COIN;
                } else if (originalLevelMap[gp.maxWorldRow - 3][i + 3] == TILE_EMPTY && originalLevelMap[gp.maxWorldRow - 2][i+3] == TILE_GROUND){
                    originalLevelMap[gp.maxWorldRow - 3][i + 3] = TILE_COIN;
                } else {
                    // Jika bahkan fallback pun sulit, tempatkan di lokasi default yang dijamin ada
                    originalLevelMap[gp.maxWorldRow - 5][3] = TILE_COIN;
                }
            }
        }


        levelMap = new int[gp.maxWorldRow][gp.maxWorldCol];
        for(int i = 0; i < gp.maxWorldRow; i++) {
            System.arraycopy(originalLevelMap[i], 0, levelMap[i], 0, gp.maxWorldCol);
        }
    }

    /**
     * Menggambar tile level di layar.
     * Mengulang melalui peta level dan menggambar elemen yang berbeda berdasarkan jenis tile.
     * @param g Objek Graphics untuk menggambar.
     */
    public void draw(Graphics g) {
        // Aktifkan Rendering Hints untuk kualitas yang lebih baik pada gambar
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        for (int row = 0; row < gp.maxWorldRow; row++) {
            for (int col = 0; col < gp.maxWorldCol; col++) {
                int tileType = levelMap[row][col];
                switch (tileType) {
                    case TILE_GROUND: // Ground/Platform
                        if (groundSurfaceTile != null && groundBaseTile != null) {
                            int surfaceHeight = (int)(gp.tileSize * 0.50);
                            g2d.drawImage(groundSurfaceTile, col * gp.tileSize, row * gp.tileSize, gp.tileSize, surfaceHeight, null);
                            int baseHeight = gp.tileSize - surfaceHeight;
                            g2d.drawImage(groundBaseTile, col * gp.tileSize, row * gp.tileSize + surfaceHeight, gp.tileSize, baseHeight, null);
                        } else {
                            System.err.println("WARNING LevelManager: Gambar ground Surface/Base tile tidak dimuat. Menggambar fallback.");
                            g2d.setColor(new Color(70, 120, 70));
                            g2d.fillRect(col * gp.tileSize, row * gp.tileSize, gp.tileSize, gp.tileSize);
                        }
                        break;
                    case TILE_SPIKE: // Duri/Bahaya
                        g2d.setColor(Color.BLACK);
                        int[] xPoints = {col * gp.tileSize, (col * gp.tileSize) + (gp.tileSize / 2), (col * gp.tileSize) + gp.tileSize};
                        int[] yPoints = {(row * gp.tileSize) + gp.tileSize, row * gp.tileSize, (row * gp.tileSize) + gp.tileSize};
                        g2d.fillPolygon(xPoints, yPoints, 3);
                        break;
                    case TILE_COIN: // Koin
                        if (coinImage != null) {
                            int coinSize = (int)(gp.tileSize * 0.6);
                            int offset = (gp.tileSize - coinSize) / 2;
                            g2d.drawImage(coinImage, col * gp.tileSize + offset, row * gp.tileSize + offset, coinSize, coinSize, null);
                        } else {
                            System.err.println("WARNING LevelManager: Gambar koin (coinImage) tidak dimuat. Koin tidak akan terlihat.");
                        }
                        break;
                    case TILE_HEALTH: // Pengumpul Kesehatan
                        g2d.setColor(Color.RED);
                        int heartSize = (int)(gp.tileSize * 0.7);
                        int offset = (gp.tileSize - heartSize) / 2;
                        g2d.fillRect(col * gp.tileSize + offset, row * gp.tileSize + offset, heartSize, heartSize);
                        break;
                    case TILE_EXIT: // Keluar/Tujuan
                        if (gp.getCoinsCollected() < 15) { // Sekarang perlu 15 koin untuk menang
                            g2d.setColor(new Color(139, 69, 19, 150));
                        } else {
                            g2d.setColor(new Color(218, 165, 32));
                        }
                        g2d.fillRect(col * gp.tileSize, row * gp.tileSize, gp.tileSize, gp.tileSize);
                        break;
                    case TILE_MOVING_H: // Platform Bergerak Horizontal (placeholder)
                        // Moving Platforms digambar oleh MovingPlatform.java, bukan di sini
                        break;
                    case TILE_MOVING_V: // Platform Bergerak Vertikal (placeholder)
                        // Moving Platforms digambar oleh MovingPlatform.java, bukan di sini
                        break;
                    case TILE_DEATH_ZONE: // Lubang Kematian Instan (placeholder visual)
                        g2d.setColor(new Color(0, 0, 0, 200));
                        g2d.fillRect(col * gp.tileSize, row * gp.tileSize, gp.tileSize, gp.tileSize);
                        break;
                    case TILE_EMPTY: // Ruang kosong, tidak digambar apa-apa
                        break;
                    case TILE_BREAKABLE: // Blok yang bisa pecah (placeholder)
                        g2d.setColor(new Color(150, 100, 50));
                        g2d.fillRect(col * gp.tileSize, row * gp.tileSize, gp.tileSize, gp.tileSize);
                        break;
                }
            }
        }
    }
}
