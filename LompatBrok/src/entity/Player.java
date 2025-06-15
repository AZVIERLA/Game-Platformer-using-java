package entity;

import graphics.Animation;
import manager.KeyManager;
import tile.TileMap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Player {
    // Position and movement
    private double x, y;
    private double dx, dy;
    private boolean onGround;

    // Animation
    private Animation idleAnim, runAnim, jumpAnim;
    private enum AnimState { IDLE, RUN, JUMP }
    private AnimState currentAnimState = AnimState.IDLE;
    private AnimState lastAnimState = AnimState.IDLE;

    // Constants
    private static final int IDLE_WIDTH = 16, IDLE_HEIGHT = 16;
    private static final int RUN_WIDTH = 16, RUN_HEIGHT = 16;
    private static final int JUMP_WIDTH = 16, JUMP_HEIGHT = 16;
    private static final double MOVE_SPEED = 1.5;
    private static final double JUMP_SPEED = -4.5;
    private static final double GRAVITY = 0.25;
    private static final double MAX_FALL_SPEED = 4.5;

    // Input
    private KeyManager keyManager;

    // Reference to map
    private TileMap tileMap;

    private boolean facingLeft = false; // Add this field

    public Player(int x, int y, KeyManager keyManager, TileMap tileMap) {
        this.x = x;
        this.y = y;
        this.keyManager = keyManager;
        this.tileMap = tileMap;
        loadAnimations();
    }

    private void loadAnimations() {
        try {
            BufferedImage idleSheet = ImageIO.read(new java.io.File("res/sprites/idle.png"));
            BufferedImage runSheet = ImageIO.read(new java.io.File("res/sprites/run.png"));
            BufferedImage jumpSheet = ImageIO.read(new java.io.File("res/sprites/jump.png"));

            idleAnim = new Animation(getFrames(idleSheet, 8, 16, 16), 150);
            runAnim = new Animation(getFrames(runSheet, 6, 16, 16), 140);
            jumpAnim = new Animation(getFrames(jumpSheet, 4, 16, 16), 120);

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (idleAnim == null) idleAnim = new Animation(new BufferedImage[]{new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB)}, 100);
        if (runAnim == null) runAnim = idleAnim;
        if (jumpAnim == null) jumpAnim = idleAnim;
    }

    private BufferedImage[] getFrames(BufferedImage sheet, int count, int frameW, int frameH) {
        int maxFrames = sheet.getWidth() / frameW;
        if (count > maxFrames) {
            System.out.println("Warning: Requested " + count + " frames, but only " + maxFrames + " fit in the sheet.");
            count = maxFrames;
        }
        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            frames[i] = sheet.getSubimage(i * frameW, 0, frameW, frameH);
        }
        return frames;
    }

    public void update() {
        // Handle input
        double move = 0;
        if (keyManager.left) move -= MOVE_SPEED;
        if (keyManager.right) move += MOVE_SPEED;

        // Update facing direction
        if (move < 0) facingLeft = true;
        else if (move > 0) facingLeft = false;

        dx = move;

        // Jump
        if (keyManager.jump && onGround) {
            dy = JUMP_SPEED;
            onGround = false;
        }

        // Gravity
        dy += GRAVITY;
        if (dy > MAX_FALL_SPEED) dy = MAX_FALL_SPEED;

        // Move and check collisions
        move((int)dx, (int)dy);

        // Animation state logic
        double moveThreshold = 0.1;
        AnimState newState;
        if (!onGround) {
            newState = AnimState.JUMP;
        } else if (Math.abs(dx) > moveThreshold) {
            newState = AnimState.RUN;
        } else {
            newState = AnimState.IDLE;
        }

        // Only reset animation if state changed
        if (newState != currentAnimState) {
            switch (newState) {
                case JUMP: jumpAnim.reset(); break;
                case RUN: runAnim.reset(); break;
                case IDLE: idleAnim.reset(); break;
            }
        }
        currentAnimState = newState;

        // Update animation
        switch (currentAnimState) {
            case JUMP: jumpAnim.update(); break;
            case RUN: runAnim.update(); break;
            case IDLE: idleAnim.update(); break;
        }
    }

    private void move(int dx, int dy) {
        int newX = (int)x + dx;
        int newY = (int)y + dy;

        // Horizontal collision
        if (dx != 0) {
            int left = newX;
            int right = newX + IDLE_WIDTH - 1;
            int[] yChecks = {
                (int)y + 1, // just above feet
                (int)y + IDLE_HEIGHT / 2, // middle
                (int)y + IDLE_HEIGHT - 1 // feet
            };
            boolean blocked = false;
            for (int yy : yChecks) {
                if (tileMap.isSolidTileAt(dx > 0 ? right : left, yy)) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) {
                newX = (int)x; // Don't move horizontally
            }
        }

        // Vertical collision
        if (dy != 0) {
            int left = newX;
            int right = newX + IDLE_WIDTH - 1;
            int top = newY;
            int bottom = newY + IDLE_HEIGHT - 1;
            boolean blocked = false;
            for (int xx = left; xx <= right; xx += TileMap.TILE_SIZE) {
                if (tileMap.isSolidTileAt(xx, dy > 0 ? bottom : top)) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) {
                if (dy > 0) {
                    // Landing on ground
                    onGround = true;
                }
                this.dy = 0;
                newY = (int)y;
            } else {
                onGround = false;
            }
        }

        // Clamp to map bounds
        if (newX < 0) newX = 0;
        if (newY < 0) newY = 0;
        if (newX > tileMap.getWidth() * TileMap.TILE_SIZE - IDLE_WIDTH)
            newX = tileMap.getWidth() * TileMap.TILE_SIZE - IDLE_WIDTH;
        if (newY > tileMap.getHeight() * TileMap.TILE_SIZE - IDLE_HEIGHT) {
            newY = tileMap.getHeight() * TileMap.TILE_SIZE - IDLE_HEIGHT;
            this.dy = 0;
            onGround = true;
        }

        x = newX;
        y = newY;
    }

    public void draw(Graphics2D g, int cameraX) {
        BufferedImage frame;
        switch (currentAnimState) {
            case JUMP: frame = jumpAnim.getCurrentFrame(); break;
            case RUN: frame = runAnim.getCurrentFrame(); break;
            case IDLE: default: frame = idleAnim.getCurrentFrame(); break;
        }
        if (frame != null) {
            int drawX = (int)x - cameraX;
            int drawY = (int)y;
            if (facingLeft) {
                // Flip horizontally
                g.drawImage(frame, drawX + IDLE_WIDTH, drawY, -IDLE_WIDTH, IDLE_HEIGHT, null);
            } else {
                g.drawImage(frame, drawX, drawY, IDLE_WIDTH, IDLE_HEIGHT, null);
            }
        } else {
            g.setColor(Color.RED);
            g.fillRect((int)x - cameraX, (int)y, IDLE_WIDTH, IDLE_HEIGHT);
        }
        // System.out.println("Player draw at: " + ((int)x - cameraX) + "," + (int)y + " frame null? " + (frame == null));
    }

    public int getX() { return (int)x; }
    public int getY() { return (int)y; }
}
