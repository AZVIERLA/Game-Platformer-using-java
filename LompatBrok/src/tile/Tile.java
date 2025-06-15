package tile;

import java.awt.image.BufferedImage;

public class Tile {
    public static final int TILE_SIZE = 16;
    private BufferedImage image;
    private boolean solid;
    private boolean platform;

    public Tile(BufferedImage image, boolean solid, boolean platform) {
        this.image = image;
        this.solid = solid;
        this.platform = platform;
    }

    public BufferedImage getImage() {
        return image;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isPlatform() {
        return platform;
    }
}
