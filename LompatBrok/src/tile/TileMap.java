package tile;

import entity.Coin;
import entity.Enemy;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

public class TileMap {
    public static final int TILE_SIZE = 16;

    private int[][] map;
    private int width, height;

    private BufferedImage tileset;
    private Tile[] tiles;

    private ArrayList<Coin> coins = new ArrayList<>();
    private ArrayList<Enemy> enemies = new ArrayList<>();
    private int[] playerSpawn = new int[]{0, 0};  // default spawn

    public TileMap() {
        loadTiles("res/tiles/Ghostly_grass_tileset.png");
        loadMapCSV("res/levels/level1.csv"); // Load from CSV
    }

    private void loadTiles(String path) {
        try {
            tileset = ImageIO.read(new File(path));
            int tilesPerRow = tileset.getWidth() / TILE_SIZE;
            int tilesPerCol = tileset.getHeight() / TILE_SIZE;
            int numTiles = tilesPerRow * tilesPerCol;
            tiles = new Tile[numTiles];

            int idx = 0;
            for (int y = 0; y < tilesPerCol; y++) {
                for (int x = 0; x < tilesPerRow; x++) {
                    BufferedImage img = tileset.getSubimage(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    boolean solid = false;
                    boolean platform = false;
                    tiles[idx] = new Tile(img, solid, platform);
                    idx++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMapCSV(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            ArrayList<String[]> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line.split(","));
            }
            height = lines.size();
            width = lines.get(0).length;
            map = new int[height][width];

            for (int y = 0; y < height; y++) {
                String[] row = lines.get(y);
                for (int x = 0; x < width; x++) {
                    int code = Integer.parseInt(row[x]);
                    switch (code) {
                        case 3:
                            coins.add(new Coin(x * TILE_SIZE, y * TILE_SIZE));
                            map[y][x] = 0;
                            break;
                        case 4:
                            playerSpawn = new int[]{x * TILE_SIZE, y * TILE_SIZE};
                            map[y][x] = 0;
                            break;
                        case 5:
                            enemies.add(new Enemy(x * TILE_SIZE, y * TILE_SIZE));
                            map[y][x] = 0;
                            break;
                        default:
                            map[y][x] = code; // Store the actual code (e.g., 12 for solid)
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g, int cameraX) {
        int startCol = cameraX / TILE_SIZE;
        int endCol = (cameraX + 640) / TILE_SIZE + 1;
        if (endCol > width) endCol = width;

        for (int row = 0; row < height; row++) {
            for (int col = startCol; col < endCol; col++) {
                int tileIndex = map[row][col];
                if (tileIndex == 12) {
                    // Draw ground tile from 5th row, 1st column (index 48)
                    g.drawImage(tiles[48].getImage(), col * TILE_SIZE - cameraX, row * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);
                } else if (tileIndex == 17) {
                    // Example: platform tile (change index as needed)
                    g.drawImage(tiles[6].getImage(), col * TILE_SIZE - cameraX, row * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);
                }
            }
        }
    }

    public boolean isSolidTileAt(int x, int y) {
        int col = x / TILE_SIZE;
        int row = y / TILE_SIZE;
        if (col < 0 || col >= width || row < 0 || row >= height) return false;
        return map[row][col] == 12; // 12 = wall/ground
    }

    public boolean isPlatformTileAt(int x, int y) {
        int col = x / TILE_SIZE;
        int row = y / TILE_SIZE;
        if (col < 0 || col >= width || row < 0 || row >= height) return false;
        return map[row][col] == 17; // 17 = platform
    }

    public int getTileSize() {
        return TILE_SIZE;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getPlayerSpawn() {
        return playerSpawn;
    }

    public ArrayList<Coin> getCoins() {
        return coins;
    }

    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }

    public void generateRandomMap(int width, int height) {
        this.width = width;
        this.height = height;
        map = new int[height][width];
        coins.clear();
        enemies.clear();

        // Fill ground at the bottom
        for (int x = 0; x < width; x++) {
            map[height - 1][x] = 0; // ground
        }
        // Random platforms
        for (int y = 2; y < height - 2; y += 2) {
            for (int x = 1; x < width - 1; x++) {
                if (Math.random() < 0.15) {
                    map[y][x] = 6; // platform
                }
            }
        }
        // Player spawn
        playerSpawn = new int[]{1 * TILE_SIZE, (height - 2) * TILE_SIZE};
        // Coin
        coins.add(new Coin((width - 2) * TILE_SIZE, (height - 3) * TILE_SIZE));
        // Enemy (optional)
        enemies.add(new Enemy((width - 4) * TILE_SIZE, (height - 2) * TILE_SIZE));
        
    }

    // private void update() {
    //     player.update();

    //     // Coin collection
    //     ArrayList<Coin> coins = getCoins();
    //     for (Coin coin : coins) {
    //         if (!coin.isCollected() && coin.intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
    //             coin.collect();
    //         }
    //     }

    //     // Camera follow player
    //     int cameraX = player.getX() - width / 2;
    //     if (cameraX < 0) cameraX = 0;
    //     int maxCameraX = getWidth() * getTileSize() - width;
    //     if (cameraX > maxCameraX) cameraX = maxCameraX;
    // }
}
