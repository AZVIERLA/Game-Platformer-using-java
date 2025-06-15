package entity;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import graphics.Animation;

public class Enemy {
    private int x, y, width = 64, height = 64; // <-- FIXED
    private Animation animation;

    public Enemy(int x, int y) {
        this.x = x;
        this.y = y;
        loadAnimation();
    }

    private void loadAnimation() {
        try {
            BufferedImage sheet = ImageIO.read(new java.io.File("res/sprites/slime.png"));
            int frameCount = sheet.getWidth() / width; // 512/64 = 8
            BufferedImage[] frames = new BufferedImage[frameCount];
            for (int i = 0; i < frameCount; i++) {
                frames[i] = sheet.getSubimage(i * width, 0, width, height);
            }
            animation = new Animation(frames, 120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g, int cameraX) {
        if (animation != null) {
            animation.update();
            g.drawImage(animation.getCurrentFrame(), x - cameraX, y, width, height, null);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}
