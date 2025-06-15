package game;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.IOException;
import java.awt.AlphaComposite;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.awt.RenderingHints;

public class EndingCutscenePanel extends JPanel implements Runnable {

    private Thread cutsceneThread;
    private BufferedImage[] backgroundLayers; // Gunakan background yang sama
    private SoundManager soundManager;

    // Aset karakter
    private BufferedImage characterSpriteSheet;
    private BufferedImage soulSprite; // Gambar jiwa (mungkin karakter idle)
    private BufferedImage whiteSoulSprite; // Gambar jiwa putih

    private ArrayList<EndingComicFrame> comicFrames; // Daftar panel komik ending
    private int currentFrameIndex = 0;
    private int frameTimerInCurrentPanel = 0;

    private boolean cutsceneFinished = false;

    // Inner class untuk merepresentasikan satu panel komik ending
    private static class EndingComicFrame {
        String dialogue;
        int backgroundLayerIndex; // Index dari backgroundLayers array (-1 untuk hitam, -2 untuk putih)
        int durationFrames;
        BufferedImage characterImage; // Gambar karakter/jiwa untuk panel ini
        boolean isWhiteSoul; // Jika true, gambar jiwa putih
        boolean fadeBackgroundToWhite; // Jika true, latar belakang memudar ke putih

        public EndingComicFrame(String dialogue, int bgIndex, int durationSec, BufferedImage charImg, boolean isWhite, boolean fadeBg) {
            this.dialogue = dialogue;
            this.backgroundLayerIndex = bgIndex;
            this.durationFrames = durationSec * 60; // Konversi detik ke frame
            this.characterImage = charImg;
            this.isWhiteSoul = isWhite;
            this.fadeBackgroundToWhite = fadeBg;
        }
    }

    public EndingCutscenePanel() {
        setPreferredSize(new Dimension(960, 720));
        setBackground(Color.BLACK);
        setFocusable(true);

        loadAssets();
        initializeEndingComicFrames();

        soundManager = new SoundManager();
        // Asumsi musik game sudah berhenti di GamePanel, jadi mungkin tidak perlu musik baru di sini
        // Atau Anda bisa memutar musik penutup yang berbeda
        // soundManager.playMusic("res/ending_music.wav", false); // Contoh: play musik ending sekali

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    finishCutscene();
                }
            }
        });
    }

    private void loadAssets() {
        try {
            backgroundLayers = new BufferedImage[10];
            String[] layerNames = {
                "Layer_0011_0.png", "Layer_0010_1.png", "Layer_0009_2.png",
                "Layer_0008_3.png", "Layer_0006_4.png", "Layer_0005_5.png",
                "Layer_0003_6.png", "Layer_0002_7.png", "Layer_0001_8.png",
                "Layer_0000_9.png"
            };

            for(int i = 0; i < layerNames.length; i++) {
                InputStream bgIs = getClass().getResourceAsStream("/res/" + layerNames[i]);
                if (bgIs != null) {
                    backgroundLayers[i] = ImageIO.read(bgIs);
                }
            }

            // Muat sprite sheet karakter
            InputStream charIs = getClass().getResourceAsStream("/res/AnimationSheet_Character.png");
            if (charIs != null) {
                characterSpriteSheet = ImageIO.read(charIs);
                int spriteWidth = 16;
                int spriteHeight = 28;
                // Ambil frame idle pertama untuk representasi jiwa
                if (characterSpriteSheet.getWidth() >= spriteWidth && characterSpriteSheet.getHeight() >= spriteHeight) {
                    soulSprite = characterSpriteSheet.getSubimage(8, 5, spriteWidth, spriteHeight);
                    // Buat versi putih dari sprite jiwa (jika tidak ada aset terpisah)
                    whiteSoulSprite = createWhiteVersion(soulSprite);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading ending cutscene assets.");
        }
    }

    /**
     * Membuat versi putih dari BufferedImage yang diberikan.
     * @param original Gambar asli.
     * @return Gambar baru yang putih.
     */
    private BufferedImage createWhiteVersion(BufferedImage original) {
        if (original == null) return null;
        BufferedImage whiteVersion = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = whiteVersion.createGraphics();
        // Gambarkan sprite asli
        g2d.drawImage(original, 0, 0, null);
        // Overlay dengan warna putih pada alpha tinggi
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1.0f)); // Atur alpha ke 1.0f (penuh)
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, original.getWidth(), original.getHeight());
        g2d.dispose();
        return whiteVersion;
    }


    private void initializeEndingComicFrames() {
        comicFrames = new ArrayList<>();
        // (dialog, bgLayerIndex, durationSec, characterImage, isWhiteSoul, fadeBackgroundToWhite)
        comicFrames.add(new EndingComicFrame("PERJALANAN TELAH SELESAI...", 9, 3, soulSprite, false, false)); // Latar belakang paling terang
        comicFrames.add(new EndingComicFrame("SEMUA FRAGMEN KENANGAN TERKUMPUL...", 8, 3, soulSprite, false, false)); // Latar belakang agak terang
        comicFrames.add(new EndingComicFrame("JIWA TELAH UTUH KEMBALI.", 6, 3, soulSprite, false, false)); // Latar belakang sedang
        comicFrames.add(new EndingComicFrame("CAHAYA BARU MENYAMBUT...", 5, 3, soulSprite, false, true)); // Mulai memudar ke putih
        comicFrames.add(new EndingComicFrame("REINKARNASI MENANTI...", -2, 4, whiteSoulSprite, true, false)); // Latar belakang putih, jiwa putih
        comicFrames.add(new EndingComicFrame("SAMPAI KETEMU DI KEHIDUPAN SELANJUTNYA.", -2, 4, null, false, false)); // Latar belakang putih, tanpa karakter
    }

    public void startCutsceneThread() {
        cutsceneThread = new Thread(this);
        cutsceneThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double drawInterval = 1000000000.0 / 60.0;
        double delta = 0;

        while (cutsceneThread != null && currentFrameIndex < comicFrames.size()) {
            long currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                frameTimerInCurrentPanel++;

                if (currentFrameIndex < comicFrames.size() && frameTimerInCurrentPanel >= comicFrames.get(currentFrameIndex).durationFrames) {
                    currentFrameIndex++;
                    frameTimerInCurrentPanel = 0;
                }

                repaint();
                delta--;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        finishCutscene();
    }

    private void finishCutscene(){
        if(!cutsceneFinished) {
            cutsceneFinished = true;
            soundManager.stopMusic(); // Stop any music (e.g., if you added ending music)
            cutsceneThread = null;
            SwingUtilities.invokeLater(() -> System.exit(0)); // Keluar dari aplikasi setelah ending cutscene
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Gambar latar belakang panel komik saat ini
        Color fadeColor = null;
        float fadeAlpha = 0f;

        if (currentFrameIndex < comicFrames.size()) {
            EndingComicFrame currentComicFrame = comicFrames.get(currentFrameIndex);
            int bgIndex = currentComicFrame.backgroundLayerIndex;

            if (currentComicFrame.fadeBackgroundToWhite) {
                float progress = (float)frameTimerInCurrentPanel / currentComicFrame.durationFrames;
                fadeAlpha = progress;
                // Pastikan alpha tidak melebihi 1.0f
                if (fadeAlpha > 1.0f) fadeAlpha = 1.0f;
                fadeColor = Color.WHITE;
            }


            if (bgIndex == -2) { // Latar belakang putih
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            } else if (bgIndex == -1) { // Latar belakang hitam
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            } else if (backgroundLayers != null && bgIndex >= 0 && bgIndex < backgroundLayers.length && backgroundLayers[bgIndex] != null) {
                g2d.drawImage(backgroundLayers[bgIndex], 0, 0, getWidth(), getHeight(), null);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }

            // Gambar efek fade background
            if (fadeColor != null) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
                g2d.setColor(fadeColor);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f)); // Reset composite
            }

            // Draw character and dialogue
            drawEndingComicPanel(g2d, currentComicFrame, frameTimerInCurrentPanel);

        } else {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        g2d.dispose();
    }

    private void drawEndingComicPanel(Graphics2D g2d, EndingComicFrame frame, int panelFrameTimer) {
        g2d.setFont(new Font("Impact", Font.BOLD, 48));

        String text = frame.dialogue;
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int x = (getWidth() - textWidth) / 2;
        int y = getHeight() - 100;

        float textAlpha = 0;
        int fadeInDuration = 60; // 1 detik fade in
        int fadeOutDuration = 60; // 1 detik fade out
        int opaqueDuration = frame.durationFrames - fadeInDuration - fadeOutDuration;

        if (panelFrameTimer < fadeInDuration) {
            textAlpha = (float) panelFrameTimer / fadeInDuration;
        } else if (panelFrameTimer < fadeInDuration + opaqueDuration) {
            textAlpha = 1.0f;
        } else {
            textAlpha = (float) (frame.durationFrames - panelFrameTimer) / fadeOutDuration;
        }
        if(textAlpha < 0) textAlpha = 0;

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha));

        // Gambar karakter/jiwa jika ada untuk panel ini
        if (frame.characterImage != null) {
            int charWidth = (int)(16 * 6); // Skala karakter lebih besar untuk ending
            int charHeight = (int)(28 * 6);
            int charX = (getWidth() - charWidth) / 2;
            int charY = (getHeight() / 2) - (charHeight / 2) - 50; // Posisikan di tengah

            BufferedImage charImageToDraw = frame.characterImage;
            if (frame.isWhiteSoul && whiteSoulSprite != null) {
                charImageToDraw = whiteSoulSprite;
            }

            g2d.drawImage(charImageToDraw, charX, charY, charWidth, charHeight, null);
        }

        // Gambar latar belakang gelembung dialog
        g2d.setColor(new Color(255, 220, 0, (int)(255 * textAlpha)));
        g2d.fillRect(x - 20, y - 50, textWidth + 40, 70);
        g2d.setColor(new Color(0, 0, 0, (int)(255 * textAlpha)));
        g2d.drawRect(x - 20, y - 50, textWidth + 40, 70);

        // Gambar teks dialog
        g2d.setColor(new Color(0, 0, 0, (int)(255 * textAlpha)));
        g2d.drawString(text, x, y);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f)); // Reset composite
    }
}
