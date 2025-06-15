package game;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {

    private static JFrame window;
    private static GamePanel gamePanel;
    private static CutscenePanel cutscenePanel;
    private static EndingCutscenePanel endingCutscenePanel; // Deklarasi panel ending

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            window = new JFrame("Ghost Mario");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);

            showCutscene();
        });
    }

    public static void showCutscene() {
        cutscenePanel = new CutscenePanel();
        window.add(cutscenePanel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        cutscenePanel.startCutsceneThread();
        cutscenePanel.requestFocusInWindow();
    }

    public static void showGame() {
        if (cutscenePanel != null) {
            window.remove(cutscenePanel);
            cutscenePanel = null;
        }
        // Hapus panel ending jika ada (misal, dari restart game setelah ending)
        if (endingCutscenePanel != null) {
            window.remove(endingCutscenePanel);
            endingCutscenePanel = null;
        }

        gamePanel = new GamePanel();
        window.add(gamePanel);
        gamePanel.requestFocusInWindow();
        window.pack();
        window.revalidate();
        window.repaint();
        gamePanel.startGameThread();
    }

    /**
     * Menampilkan ending cutscene.
     * Dipanggil dari GamePanel ketika game dimenangkan.
     */
    public static void showEndingCutscene() {
        if (gamePanel != null) {
            window.remove(gamePanel);
            gamePanel = null;
        }

        endingCutscenePanel = new EndingCutscenePanel();
        window.add(endingCutscenePanel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        endingCutscenePanel.startCutsceneThread();
        endingCutscenePanel.requestFocusInWindow();
    }
}
