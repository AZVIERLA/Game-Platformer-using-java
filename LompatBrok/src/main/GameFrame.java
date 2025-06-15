package main;

import javax.swing.JFrame;

public class GameFrame extends JFrame {

    public GameFrame() {
        // Menambahkan panel game ke dalam frame
        this.add(new GamePanel());

        this.setTitle("Lompat BROK!!!");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.pack(); // Menyesuaikan ukuran dengan komponen dalam (GamePanel)
        this.setLocationRelativeTo(null); // Tengah layar
        this.setVisible(true);
    }
}
