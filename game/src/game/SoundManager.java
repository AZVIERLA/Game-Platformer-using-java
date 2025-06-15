package game;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

public class SoundManager {

    private Clip musicClip; // Mengganti 'clip' menjadi 'musicClip' untuk membedakan dengan SFX

    /**
     * Memutar musik dari jalur file yang ditentukan.
     * CATATAN PENTING: Pustaka bawaan Java (javax.sound.sampled) seringkali memiliki
     * dukungan terbatas untuk format MP3. WAV, AIFF, atau AU umumnya lebih andal.
     * Jika Anda mengalami "UnsupportedAudioFileException", coba ubah file audio Anda ke WAV.
     * @param filePath Jalur ke file audio (misalnya, "res/music.wav").
     * @param loop Jika true, musik akan diputar secara berulang.
     */
    public void playMusic(String filePath, boolean loop) {
        // Hentikan musik yang sedang diputar sebelum memulai yang baru
        stopMusic();

        try {
            InputStream is = getClass().getResourceAsStream(filePath.startsWith("/") ? filePath : "/" + filePath);
            if (is == null) {
                System.err.println("ERROR SoundManager: File musik tidak ditemukan di jalur: " + filePath + ". Pastikan nama file dan lokasi sudah benar.");
                return;
            }

            AudioInputStream audioInput = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
            musicClip = AudioSystem.getClip();
            musicClip.open(audioInput);

            FloatControl gainControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(-10.0f); // Volume musik -10 dB

            if (loop) {
                musicClip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                musicClip.start();
            }

        } catch (UnsupportedAudioFileException e) {
            System.err.println("ERROR SoundManager: Format file musik tidak didukung: " + filePath + ". Pastikan Anda menggunakan format WAV atau yang didukung Java.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("ERROR SoundManager: Terjadi masalah I/O saat membaca file musik: " + filePath);
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            System.err.println("ERROR SoundManager: Jalur audio tidak tersedia untuk musik: " + filePath);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("ERROR SoundManager: Terjadi kesalahan umum saat memutar musik: " + filePath);
            e.printStackTrace();
        }
    }

    /**
     * Memutar efek suara satu kali. Efek suara akan dimuat dan dimainkan
     * secara terpisah, memungkinkan beberapa efek suara dimainkan secara bersamaan.
     * @param filePath Jalur ke file audio efek suara (misalnya, "res/jump.wav").
     */
    public void playSoundEffect(String filePath) {
        try {
            InputStream is = getClass().getResourceAsStream(filePath.startsWith("/") ? filePath : "/" + filePath);
            if (is == null) {
                System.err.println("ERROR SoundManager: File efek suara tidak ditemukan di jalur: " + filePath);
                return;
            }

            AudioInputStream audioInput = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
            Clip sfxClip = AudioSystem.getClip();
            sfxClip.open(audioInput);

            FloatControl gainControl = (FloatControl) sfxClip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(-5.0f); // Volume SFX -5 dB (contoh)

            sfxClip.start();

            sfxClip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    sfxClip.close();
                }
            });

        } catch (UnsupportedAudioFileException e) {
            System.err.println("ERROR SoundManager: Format file efek suara tidak didukung: " + filePath + ". Pastikan menggunakan format WAV.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("ERROR SoundManager: Terjadi masalah I/O saat membaca file efek suara: " + filePath);
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            System.err.println("ERROR SoundManager: Jalur audio tidak tersedia untuk efek suara: " + filePath);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("ERROR SoundManager: Terjadi kesalahan umum saat memutar efek suara: " + filePath);
            e.printStackTrace();
        }
    }

    /**
     * Menghentikan musik yang sedang diputar dan menutup audio clip-nya.
     */
    public void stopMusic() {
        if (musicClip != null && musicClip.isRunning()) {
            musicClip.stop();
            musicClip.close();
        }
    }
}
