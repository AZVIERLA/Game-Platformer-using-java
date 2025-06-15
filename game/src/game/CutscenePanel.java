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
import java.util.ArrayList;
import javax.swing.SwingUtilities;
import java.awt.RenderingHints; // Import untuk RenderingHints

public class CutscenePanel extends JPanel implements Runnable {

    private Thread cutsceneThread;
    private BufferedImage[] backgroundLayers;
    private SoundManager soundManager;

    private BufferedImage characterSpriteSheet; // Lembar sprite karakter untuk cutscene
    // Frame-frame spesifik karakter yang akan digunakan dalam cutscene
    private BufferedImage cutsceneCharIdle;
    private BufferedImage cutsceneCharConfused;
    private BufferedImage cutsceneCharDetermined;
    private BufferedImage cutsceneCharAction;

    private int timer = 0; // Penghitung frame global untuk seluruh cutscene
    private final int FPS = 60; // Frame per detik untuk animasi
    private final int TOTAL_CUTSCENE_DURATION_SECONDS = 24; // Durasi total cutscene (dapat disesuaikan)
    private final int TOTAL_CUTSCENE_DURATION_FRAMES = TOTAL_CUTSCENE_DURATION_SECONDS * FPS;

    private ArrayList<ComicFrame> comicFrames; // Daftar panel komik
    private int currentFrameIndex = 0; // Indeks panel komik yang sedang ditampilkan
    private int frameTimerInCurrentPanel = 0; // Timer untuk panel yang sedang aktif

    private boolean cutsceneFinished = false;

    /**
     * Inner class untuk merepresentasikan satu panel/frame komik.
     * Setiap frame memiliki dialog, indeks lapisan latar belakang, durasi,
     * gambar karakter spesifik, dan apakah karakter perlu dibalik.
     */
    private static class ComicFrame {
        String dialogue;
        int backgroundLayerIndex; // Indeks dari array backgroundLayers (-1 untuk latar belakang hitam polos)
        int durationFrames; // Berapa lama frame ini harus ditampilkan
        BufferedImage characterImage; // Gambar sprite karakter spesifik untuk panel ini
        boolean flipCharacter; // Apakah karakter perlu dibalik secara horizontal

        public ComicFrame(String dialogue, int backgroundLayerIndex, int durationSeconds, BufferedImage characterImage, boolean flipCharacter) {
            this.dialogue = dialogue;
            this.backgroundLayerIndex = backgroundLayerIndex;
            this.durationFrames = durationSeconds * 60; // Konversi detik ke frame
            this.characterImage = characterImage;
            this.flipCharacter = flipCharacter;
        }
    }

    public CutscenePanel() {
        setPreferredSize(new Dimension(960, 720));
        setBackground(Color.BLACK);
        setFocusable(true);

        loadAssets(); // Memuat gambar latar belakang dan sprite karakter
        initializeComicFrames(); // Menginisialisasi urutan panel komik dengan narasi baru

        soundManager = new SoundManager();
        // Pastikan file audio Anda sudah dalam format WAV atau yang didukung Java
        // Ganti "res/game_bgm.wav" dengan nama file musik cutscene Anda jika berbeda.
        soundManager.playMusic("res/game-overdrive-253440.wav", true); // Memulai musik cutscene

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    finishCutscene(); // Memungkinkan pemain untuk melewati cutscene
                }
            }
        });
    }

    /**
     * Memuat semua lapisan latar belakang dan sprite karakter untuk digunakan dalam panel komik.
     */
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
                } else {
                    System.err.println("Tidak dapat menemukan file latar untuk cutscene: " + layerNames[i]);
                }
            }

            // Muat lembar sprite karakter
            InputStream charIs = getClass().getResourceAsStream("/res/AnimationSheet_Character.png");
            if (charIs != null) {
                characterSpriteSheet = ImageIO.read(charIs);
                // Ekstrak frame-frame spesifik dari sprite sheet untuk cutscene
                int charSpriteWidth = 16;
                int charSpriteHeight = 28;
                // Pastikan ukuran sprite sheet cukup besar untuk mengambil subimages
                if (characterSpriteSheet.getWidth() >= 168 + charSpriteWidth && characterSpriteSheet.getHeight() >= 161 + charSpriteHeight) {
                    cutsceneCharIdle = characterSpriteSheet.getSubimage(8, 5, charSpriteWidth, charSpriteHeight); // Frame idle awal
                    cutsceneCharConfused = characterSpriteSheet.getSubimage(8, 37, charSpriteWidth, charSpriteHeight); // Frame pose bingung/melihat sekitar
                    cutsceneCharDetermined = characterSpriteSheet.getSubimage(72, 69, charSpriteWidth, charSpriteHeight); // Frame pose tekad (dari animasi jalan)
                    cutsceneCharAction = characterSpriteSheet.getSubimage(136, 133, charSpriteWidth, charSpriteHeight); // Frame pose aksi/perjuangan
                } else {
                    System.err.println("Lembar sprite karakter terlalu kecil atau rusak. Karakter mungkin tidak muncul di cutscene.");
                }
            } else {
                System.err.println("Tidak dapat menemukan file sprite karakter untuk cutscene: AnimationSheet_Character.png");
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error memuat aset cutscene (backgrounds atau character sprite).");
        }
    }

    /**
     * Menginisialisasi urutan panel komik dengan dialog, latar belakang, durasinya,
     * dan gambar karakter yang spesifik untuk narasi yang lebih menarik.
     */
    private void initializeComicFrames() {
        comicFrames = new ArrayList<>();
        // Format: (dialog, bgLayerIndex, durationSeconds, characterImage, flipCharacter)
        comicFrames.add(new ComicFrame("DI TENGAH KEGELAPAN TOTAL...", 0, 3, cutsceneCharIdle, false)); // Paling gelap, karakter pasif
        comicFrames.add(new ComicFrame("...SEBUAH JIWA BANGKIT DARI KEKOSONGAN.", 1, 3, cutsceneCharIdle, false)); // Sedikit lebih terang, karakter masih pasif
        comicFrames.add(new ComicFrame("TERJEBAK DI ANTARA REALITAS...", 3, 3, cutsceneCharConfused, false)); // Latar belakang lebih detail, karakter bingung
        comicFrames.add(new ComicFrame("...IA MENCARI APA YANG TELAH HILANG.", 5, 3, cutsceneCharDetermined, false)); // Latar belakang lebih maju, karakter mulai bergerak
        comicFrames.add(new ComicFrame("NAMUN, KENANGANNYA TERSEBAR...", 7, 3, cutsceneCharConfused, true)); // Latar belakang lebih dekat, karakter terlihat sedih/bingung (dibalik untuk variasi)
        comicFrames.add(new ComicFrame("...SEPERTI FRAGMEN MIMPI YANG HANCUR.", 8, 3, cutsceneCharDetermined, false)); // Latar belakang hampir paling terang, karakter menunjukkan tekad
        comicFrames.add(new ComicFrame("UNTUK MENGUMPULKAN KEMBALI KEPINGAN TAKDIRNYA...", 9, 3, cutsceneCharAction, false)); // Latar belakang paling terang, karakter dalam pose aksi
        comicFrames.add(new ComicFrame("...IA HARUS MENEMPUH PERJALANAN BERBAHAYA!", 9, 3, cutsceneCharAction, true)); // Karakter siap bertarung (dibalik)
        comicFrames.add(new ComicFrame("PERJALANAN JIWA DIMULAI.", -1, 3, null, false)); // Panel terakhir, latar belakang hitam, hanya teks (null karakter)
    }

    /**
     * Memulai thread animasi cutscene.
     */
    public void startCutsceneThread() {
        cutsceneThread = new Thread(this);
        cutsceneThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double drawInterval = 1000000000.0 / FPS;
        double delta = 0;

        // Loop cutscene hingga semua panel ditampilkan atau dilewati
        while (cutsceneThread != null && currentFrameIndex < comicFrames.size()) {
            long currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                timer++; // Timer global
                frameTimerInCurrentPanel++; // Timer untuk panel saat ini

                // Cek apakah panel saat ini sudah habis waktunya
                if (currentFrameIndex < comicFrames.size() && frameTimerInCurrentPanel >= comicFrames.get(currentFrameIndex).durationFrames) {
                    currentFrameIndex++; // Pindah ke panel berikutnya
                    frameTimerInCurrentPanel = 0; // Reset timer panel
                }

                repaint(); // Minta repaint untuk memperbarui tampilan
                delta--;
            }

            try {
                Thread.sleep(1); // Tidur sebentar untuk memberi kesempatan CPU, FPS aktual dikontrol oleh delta
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        finishCutscene(); // Pastikan cutscene diakhiri dengan benar setelah loop selesai
    }

    /**
     * Mengakhiri cutscene, menghentikan musik, dan beralih ke game utama.
     */
    private void finishCutscene(){
        if(!cutsceneFinished) {
            cutsceneFinished = true;
            soundManager.stopMusic();
            cutsceneThread = null;
            SwingUtilities.invokeLater(Main::showGame); // Beralih ke game utama di EDT
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Aktifkan Anti-aliasing dan Rendering Hints untuk kualitas yang lebih baik
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);


        // Gambar latar belakang panel komik saat ini
        if (currentFrameIndex < comicFrames.size()) {
            ComicFrame currentComicFrame = comicFrames.get(currentFrameIndex);
            int bgIndex = currentComicFrame.backgroundLayerIndex;
            // Jika bgIndex adalah -1 atau latar belakang tidak ditemukan, gambar hitam polos
            if (bgIndex != -1 && backgroundLayers != null && bgIndex >= 0 && bgIndex < backgroundLayers.length && backgroundLayers[bgIndex] != null) {
                g2d.drawImage(backgroundLayers[bgIndex], 0, 0, getWidth(), getHeight(), null);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        } else {
            // Jika semua panel sudah ditampilkan, tampilkan latar belakang hitam
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        // Overlay gelap untuk membuat teks dan karakter lebih mudah dibaca
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Gambar panel komik dengan dialog dan karakternya
        drawComicPanel(g2d, frameTimerInCurrentPanel);

        g2d.dispose();
    }

    /**
     * Menggambar panel komik tunggal dengan teks dialog yang memudar dan sprite karakter.
     * @param g2d Objek Graphics2D untuk menggambar.
     * @param panelFrameTimer Waktu frame di dalam panel saat ini.
     */
    private void drawComicPanel(Graphics2D g2d, int panelFrameTimer) {
        g2d.setFont(new Font("Impact", Font.BOLD, 48));

        if(currentFrameIndex >= comicFrames.size()) return;

        ComicFrame currentComicFrame = comicFrames.get(currentFrameIndex);
        String text = currentComicFrame.dialogue;

        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int x = (getWidth() - textWidth) / 2; // Pusatkan teks secara horizontal
        int y = getHeight() - 100; // Posisikan teks di dekat bagian bawah layar

        float alpha = 0; // Transparansi untuk teks dan karakter
        int fadeInDuration = FPS; // 1 detik fade in
        int fadeOutDuration = FPS; // 1 detik fade out
        int opaqueDuration = currentComicFrame.durationFrames - fadeInDuration - fadeOutDuration;

        if (panelFrameTimer < fadeInDuration) { // Fade in
            alpha = (float) panelFrameTimer / fadeInDuration;
        } else if (panelFrameTimer < fadeInDuration + opaqueDuration) { // Tetap opaque
            alpha = 1.0f;
        } else { // Fade out
            alpha = (float) (currentComicFrame.durationFrames - panelFrameTimer) / fadeOutDuration;
        }
        if(alpha < 0) alpha = 0; // Pastikan alpha tidak di bawah 0

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)); // Terapkan transparansi

        // Gambar sprite karakter jika ada untuk panel ini
        if (currentComicFrame.characterImage != null) {
            int charWidth = (int)(16 * 4); // Skala karakter sedikit lebih besar
            int charHeight = (int)(28 * 4);
            int charX = (getWidth() - charWidth) / 2;
            // Posisikan karakter sedikit di atas tengah layar
            int charY = (getHeight() / 2) - (charHeight / 2) - 80;

            Graphics2D charG2d = (Graphics2D) g2d.create(); // Buat salinan G2D untuk transformasi karakter
            charG2d.translate(charX, charY);

            if (currentComicFrame.flipCharacter) {
                charG2d.translate(charWidth, 0);
                charG2d.scale(-1, 1);
            }
            charG2d.drawImage(currentComicFrame.characterImage, 0, 0, charWidth, charHeight, null);
            charG2d.dispose();
        }


        // Gambar latar belakang gelembung dialog
        g2d.setColor(new Color(255, 220, 0, (int)(255 * alpha))); // Warna kuning transparan
        g2d.fillRect(x - 20, y - 50, textWidth + 40, 70);
        g2d.setColor(new Color(0, 0, 0, (int)(255 * alpha))); // Warna hitam transparan
        g2d.drawRect(x - 20, y - 50, textWidth + 40, 70);

        // Gambar teks dialog
        g2d.setColor(new Color(0, 0, 0, (int)(255 * alpha))); // Warna hitam transparan
        g2d.drawString(text, x, y);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f)); // Reset opacity

        // Gambar instruksi "Tekan Enter untuk Melewati" yang berkedip
        if (panelFrameTimer % FPS < FPS / 2) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.ITALIC, 16));
            g2d.drawString("Tekan Enter untuk Melewati", getWidth() - 220, getHeight() - 20);
        }
    }
}
