// ===== File: graphics/Animation.java =====
package graphics;

import java.awt.image.BufferedImage;

public class Animation {
    private BufferedImage[] frames;
    private int currentFrame;
    private long startTime;
    private long delay;
    private boolean loop;
    private boolean finished;

    public Animation(BufferedImage[] frames, long delay) {
        this.frames = frames;
        this.delay = delay;
        this.loop = true;
        this.currentFrame = 0;
        this.startTime = System.nanoTime();
        this.finished = false;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public void reset() {
        currentFrame = 0;
        startTime = System.nanoTime();
        finished = false;
    }

    public void update() {
        if (finished) return;

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        if (elapsed > delay) {
            currentFrame++;
            if (currentFrame >= frames.length) {
                if (loop) {
                    currentFrame = 0;
                } else {
                    currentFrame = frames.length - 1;
                    finished = true;
                    System.out.println("Animation finished.");
                }
            }
            startTime = System.nanoTime();
        }
    }

    public BufferedImage getCurrentFrame() {
        return frames[currentFrame];
    }

    public boolean isFinished() {
        return finished;
    }
}