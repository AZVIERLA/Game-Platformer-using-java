// SpriteSheetMage.java
package game;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.awt.Graphics2D; 
import java.awt.Color; 
import java.awt.Font; // PERBAIKAN: Import Font

public class SpriteSheetMage {
    private BufferedImage sheet;
    private int spriteWidth;
    private int spriteHeight;

    public SpriteSheetMage(String path, int spriteWidth, int spriteHeight) {
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        URL imageUrl = null;
        try {
            // Path harus absolut dari root classpath. 
            // Jika folder 'game' Anda ada di 'src', maka pathnya harus '/game/resources/namafile.png'
            System.out.println("Mencoba memuat sprite sheet dari classpath: " + path);
            imageUrl = SpriteSheetMage.class.getResource(path);
            
            if (imageUrl == null) {
                 throw new IOException("Resource sprite sheet TIDAK DITEMUKAN. Path yang digunakan: '" + path + "'. Pastikan file gambar ada di lokasi yang benar dalam struktur proyek Anda (misalnya, 'src/game/resources/') dan path tersebut sesuai dengan yang Anda berikan.");
            }
            sheet = ImageIO.read(imageUrl);
            System.out.println("Sprite sheet berhasil dimuat dari: " + imageUrl.getPath());
        } catch (IOException e) {
            System.err.println("Error Kritis saat memuat sprite sheet '" + path + "': " + e.getMessage());
            // e.printStackTrace(); // Uncomment untuk detail error lebih lanjut
            sheet = null; // Set sheet ke null agar getSprite mengembalikan placeholder
        }  catch (IllegalArgumentException e) {
            System.err.println("Error Kritis: Path resource null atau tidak valid saat memuat sprite sheet: " + path + " - Pesan: " + e.getMessage());
            // e.printStackTrace();
            sheet = null;
        }
    }

    public BufferedImage getSprite(int col, int row) {
        if (sheet == null) {
            BufferedImage placeholder = new BufferedImage(spriteWidth, spriteHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = placeholder.createGraphics();
            g.setColor(Color.MAGENTA); 
            g.fillRect(0, 0, spriteWidth, spriteHeight);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, Math.min(10, spriteHeight / 2 - 2) )); 
            g.drawString("N/A", 3, spriteHeight / 2 + 3);
            g.dispose();
            System.err.println("PERINGATAN: Sprite sheet utama tidak termuat, menggunakan placeholder untuk getSprite("+col+","+row+").");
            return placeholder;
        }
        
        int frameX = col * spriteWidth;
        int frameY = row * spriteHeight;

        if (col < 0 || row < 0 || frameX + spriteWidth > sheet.getWidth() || frameY + spriteHeight > sheet.getHeight()){
            System.err.println("Error: getSprite("+col+","+row+") di luar batas sheet. Dimensi Frame: " + spriteWidth+"x"+spriteHeight + ". Ukuran sheet: " + sheet.getWidth() + "x" + sheet.getHeight() + " @ " + frameX + "," + frameY);
            BufferedImage placeholder = new BufferedImage(spriteWidth, spriteHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = placeholder.createGraphics();
            g.setColor(Color.RED); 
            g.fillRect(0, 0, spriteWidth, spriteHeight);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, Math.min(8, spriteHeight / 2 - 2))); 
            g.drawString("OOB", 2, spriteHeight / 2 + 2); // Out Of Bounds
            g.dispose();
            return placeholder;
        }
        return sheet.getSubimage(frameX, frameY, spriteWidth, spriteHeight);
    }
}
