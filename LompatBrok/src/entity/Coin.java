package entity;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import graphics.Animation;

public class Coin {
    private int x, y, size = 16;
    private boolean collected = false;
    private Animation animation;

    public Coin(int x, int y) {
        this.x = x;
        this.y = y;
        loadAnimation();
    }

    private void loadAnimation() {
        try {
            BufferedImage sheet = ImageIO.read(new java.io.File("res/sprites/coins.png"));
            int frameCount = sheet.getWidth() / size;
            BufferedImage[] frames = new BufferedImage[frameCount];
            for (int i = 0; i < frameCount; i++) {
                frames[i] = sheet.getSubimage(i * size, 0, size, size);
            }
            animation = new Animation(frames, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g, int cameraX) {
        if (!collected && animation != null) {
            animation.update();
            g.drawImage(animation.getCurrentFrame(), x - cameraX, y, size, size, null);
        }
    }

    public boolean intersects(int px, int py, int pWidth, int pHeight) {
        Rectangle coinRect = new Rectangle(x, y, size, size);
        Rectangle playerRect = new Rectangle(px, py, pWidth, pHeight);
        return coinRect.intersects(playerRect);
    }

    public void collect() {
        collected = true;
    }

    public boolean isCollected() {
        return collected;
    }
}
