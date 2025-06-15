package game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.IOException;

public class Enemy {
    private double x, y; // Enemy's position
    private int width, height; // Enemy's dimensions
    private double velX = -1.0; // Horizontal velocity (starts moving left)
    private double velY = 0; // Vertical velocity
    private final double gravity = 0.8; // Gravity effect
    private boolean onGround = false; // Flag to check if enemy is on the ground

    private GamePanel gp; // Reference to the GamePanel

    private BufferedImage[] walkFrames; // Animation frames for walking
    private int animationFrame = 0; // Current frame in the animation
    private int animationTick = 0; // Counter for animation speed
    private final int animationSpeed = 25; // How many game ticks before next animation frame

    /**
     * Constructor for the Enemy class.
     * @param x Initial X position of the enemy.
     * @param y Initial Y position of the enemy.
     * @param gp Reference to the GamePanel.
     */
    public Enemy(int x, int y, GamePanel gp) {
        this.gp = gp;

        // Define scaling and visual dimensions for the enemy sprite
        final int SCALE = 2;
        final int VISUAL_SPRITE_WIDTH = 32;
        final int VISUAL_SPRITE_HEIGHT = 24;

        this.width = VISUAL_SPRITE_WIDTH * SCALE; // Scaled width for collision and drawing
        this.height = VISUAL_SPRITE_HEIGHT * SCALE; // Scaled height for collision and drawing

        this.x = x;
        this.y = y;

        loadSprite(); // Load enemy sprite images
    }

    /**
     * Loads the enemy sprite sheet from resources and extracts animation frames.
     * Provides error handling if the sprite sheet is not found.
     */
    private void loadSprite() {
        try {
            // Attempt to load resource from current class path first
            InputStream is = getClass().getResourceAsStream("/res/slime.png");
            if (is == null) {
                // Fallback to class loader if direct resource stream fails (though usually getClass() is enough)
                is = Enemy.class.getResourceAsStream("/res/slime.png");
            }
            if (is == null) {
                System.err.println("KRITIS: Tidak dapat menemukan file 'res/slime.png'.");
                return; // Exit if sprite not found
            }

            BufferedImage spriteSheet = ImageIO.read(is); // Read the sprite sheet image

            final int SPRITE_WIDTH = 32; // Original pixel width of a single sprite frame
            final int SPRITE_HEIGHT = 24; // Original pixel height of a single sprite frame
            final int Y_OFFSET = 0; // Y-coordinate offset within the sprite sheet for the animation row

            walkFrames = new BufferedImage[2]; // 2 frames for walking animation
            // Extract individual frames from the sprite sheet
            walkFrames[0] = spriteSheet.getSubimage(0, Y_OFFSET, SPRITE_WIDTH, SPRITE_HEIGHT);
            walkFrames[1] = spriteSheet.getSubimage(32, Y_OFFSET, SPRITE_WIDTH, SPRITE_HEIGHT);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading enemy sprite sheet.");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("Error extracting enemy subimages (invalid coordinates or dimensions).");
        }
    }

    /**
     * Updates the enemy's state, including movement, gravity, collisions, and animation.
     * @param levelMap The 2D array representing the game level.
     * @param tileSize The size of a single tile in pixels.
     */
    public void update(int[][] levelMap, int tileSize) {
        // Apply gravity if not on ground
        if (!onGround) {
            velY += gravity;
        }

        // Apply vertical movement and check for collisions
        y += velY;
        checkVerticalCollisions(levelMap, tileSize);

        // Check for ledges to prevent falling off platforms (and reverse direction)
        checkLedge(levelMap, tileSize);

        // Apply horizontal movement and check for collisions
        x += velX;
        checkHorizontalCollisions(levelMap, tileSize);

        updateAnimationTick(); // Update enemy animation frame
    }

    /**
     * Updates the animation frame for the enemy.
     * Cycles through walk frames.
     */
    private void updateAnimationTick() {
        if (walkFrames == null || walkFrames.length == 0) return; // Prevent errors if sprites not loaded

        animationTick++;
        if (animationTick >= animationSpeed) {
            animationTick = 0;
            animationFrame++;
            // Loop animation frames
            if (animationFrame >= walkFrames.length) {
                animationFrame = 0;
            }
        }
    }

    /**
     * Checks for horizontal collisions between the enemy and solid tiles.
     * If a collision occurs, the enemy reverses its horizontal direction.
     * @param levelMap The 2D array representing the game level.
     * @param tileSize The size of a single tile in pixels.
     */
    private void checkHorizontalCollisions(int[][] levelMap, int tileSize) {
        Rectangle enemyBounds = getBounds(); // Get enemy's collision bounding box

        // Iterate through tiles to check for collisions
        // Optimize by checking only tiles near the enemy
        int startCol = Math.max(0, (int) (enemyBounds.getX() / tileSize) - 1);
        int endCol = Math.min(gp.maxWorldCol - 1, (int) ((enemyBounds.getX() + enemyBounds.getWidth()) / tileSize) + 1);
        int startRow = Math.max(0, (int) (enemyBounds.getY() / tileSize) - 1);
        int endRow = Math.min(gp.maxWorldRow - 1, (int) ((enemyBounds.getY() + enemyBounds.getHeight()) / tileSize) + 1);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                // Only consider solid tiles (tileType == 1) for horizontal collision
                if (levelMap[row][col] == 1) {
                    Rectangle tileBounds = new Rectangle(col * tileSize, row * tileSize, tileSize, tileSize);
                    if (enemyBounds.intersects(tileBounds)) {
                        velX = -velX; // Reverse horizontal direction
                        // Adjust position slightly to prevent sticking
                        x += (velX > 0 ? 1 : -1); // Move 1 pixel away from the collision
                        return; // Only handle one collision per update
                    }
                }
            }
        }
    }

    /**
     * Checks for vertical collisions between the enemy and solid tiles.
     * Adjusts enemy's vertical position and velocity, and updates onGround status.
     * @param levelMap The 2D array representing the game level.
     * @param tileSize The size of a single tile in pixels.
     */
    private void checkVerticalCollisions(int[][] levelMap, int tileSize) {
        onGround = false; // Assume not on ground unless a collision proves otherwise
        Rectangle enemyBounds = getBounds(); // Get enemy's collision bounding box

        // Iterate through tiles to check for collisions
        int startCol = Math.max(0, (int) (enemyBounds.getX() / tileSize) - 1);
        int endCol = Math.min(gp.maxWorldCol - 1, (int) ((enemyBounds.getX() + enemyBounds.getWidth()) / tileSize) + 1);
        int startRow = Math.max(0, (int) (enemyBounds.getY() / tileSize) - 1);
        int endRow = Math.min(gp.maxWorldRow - 1, (int) ((enemyBounds.getY() + enemyBounds.getHeight()) / tileSize) + 1);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                // Only consider solid tiles (tileType == 1) for vertical collision
                if (levelMap[row][col] == 1) {
                    Rectangle tileBounds = new Rectangle(col * tileSize, row * tileSize, tileSize, tileSize);
                    if (enemyBounds.intersects(tileBounds)) {
                        if (velY >= 0) { // Falling (hit ground)
                            y = tileBounds.y - height; // Position on top of the tile
                            velY = 0; // Stop falling
                            onGround = true; // Enemy is now on the ground
                        } else { // Jumping (hit ceiling)
                            y = tileBounds.y + tileSize; // Position below the tile
                            velY = 0; // Stop moving up
                        }
                        // Important: Do not return here if you want to handle multiple potential collisions
                        // (e.g., if enemy is large and spans multiple tiles, but for small enemies it's fine)
                    }
                }
            }
        }
    }

    /**
     * Checks if the enemy is about to walk off a ledge.
     * If so, it reverses the enemy's horizontal direction.
     * This makes enemies patrol back and forth on platforms.
     * @param levelMap The 2D array representing the game level.
     * @param tileSize The size of a single tile in pixels.
     */
    private void checkLedge(int[][] levelMap, int tileSize) {
        if (!onGround) {
            return; // Only check for ledges if the enemy is on the ground
        }

        int checkX; // The X-coordinate to check for a tile below
        // Check slightly ahead in the direction of movement
        if (velX < 0) { // Moving left
            checkX = (int) (x + velX); // Check the tile to the left
        } else { // Moving right
            checkX = (int) (x + width + velX); // Check the tile to the right
        }
        int checkY = (int) (y + height + 1); // Check one pixel below the enemy's feet

        int col = checkX / tileSize;
        int row = checkY / tileSize;

        // Ensure check coordinates are within world bounds
        if (col >= 0 && col < gp.maxWorldCol && row >= 0 && row < gp.maxWorldRow) {
            // If the tile below (in the direction of movement) is empty (0), reverse direction
            if (levelMap[row][col] == 0) {
                velX = -velX; // Reverse horizontal direction
            }
        }
    }

    /**
     * Draws the enemy character on the screen.
     * Flips the sprite horizontally based on movement direction.
     * @param g Graphics object for drawing.
     */
    public void draw(Graphics g) {
        if (walkFrames != null && animationFrame < walkFrames.length && walkFrames[animationFrame] != null) {
            BufferedImage image = walkFrames[animationFrame];
            Graphics2D g2d = (Graphics2D) g.create(); // Create a copy of Graphics2D for transformations

            g2d.translate((int)x, (int)y); // Translate to enemy's position

            // Flip the sprite horizontally if moving right (default sprite assumes facing left)
            if (velX > 0) {
                g2d.translate(width, 0); // Move origin to right edge
                g2d.scale(-1, 1); // Flip horizontally
            }

            // Draw the current animation frame, scaled to enemy's size
            g2d.drawImage(image, 0, 0, width, height, null);
            g2d.dispose(); // Dispose the Graphics2D copy
        } else {
            // Fallback: draw a green rectangle if sprite failed to load or frames are missing
            g.setColor(Color.GREEN);
            g.fillRect((int)x, (int)y, width, height);
        }
    }

    /**
     * Returns the enemy's collision bounding box.
     * An inset is applied to make the collision box slightly smaller than the visual sprite,
     * often used to make collisions feel more forgiving.
     * @return A Rectangle representing the enemy's current collision area.
     */
    public Rectangle getBounds() {
        int insetX = width / 6; // Inset from left/right
        int insetY = height / 4; // Inset from top/bottom

        return new Rectangle(
            (int)x + insetX,
            (int)y + insetY,
            width - (insetX * 2), // Reduced width
            height - (insetY * 2)  // Reduced height
        );
    }
}
