package glassfolio.gui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;

/**
 * The startup chime: a short, warm, rising arpeggio made from sine waves in
 * code, so the app ships no sound file. If the computer has no sound device the
 * app stays silent and only logs it.
 */
public final class Chime {
    private static final float SAMPLE_RATE = 44_100;
    private static final int BITS = 16;
    private static final int CHANNELS = 1;
    private static final int BYTES_PER_SAMPLE = BITS / Byte.SIZE;
    private static final double SECONDS = 2.2;
    /** The notes in hertz (C5, G5, C6, E6) and when each starts, in seconds. */
    private static final double[] NOTES = {523.25, 783.99, 1046.50, 1318.51};
    private static final double[] STARTS = {0.0, 0.16, 0.32, 0.48};
    private static final double DECAY = 3.2;        // higher fades each note faster
    private static final double ATTACK = 0.008;     // a few milliseconds of fade-in, so notes don't click
    private static final double OVERTONE = 0.18;    // a little of the octave above makes it bell-like
    private static final double VOLUME = 0.22;      // kept low; it plays without warning
    private static final double FADE_OUT = 0.25;
    private static final String THREAD_NAME = "startup-chime";
    private static final System.Logger LOG = System.getLogger(Chime.class.getName());

    private Chime() {
    }

    /** Plays the chime on its own thread, so the window never waits for audio. */
    public static void play() {
        Thread player = new Thread(Chime::playNow, THREAD_NAME);
        player.setDaemon(true);
        player.start();
    }

    private static void playNow() {
        byte[] pcm = synthesize();
        AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS, CHANNELS, true, false);
        try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(pcm), format,
                pcm.length / format.getFrameSize())) {
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            try {
                clip.open(stream);
            } catch (LineUnavailableException | IOException | IllegalArgumentException e) {
                clip.close(); // the STOP listener never fires if open fails
                throw e;
            }
            clip.start();
        } catch (LineUnavailableException | IOException | IllegalArgumentException e) {
            LOG.log(System.Logger.Level.INFO, "No startup chime: sound is unavailable", e);
        }
    }

    /**
     * Builds the chime as 16-bit little-endian mono samples.
     *
     * @return the raw audio bytes
     */
    public static byte[] synthesize() {
        int samples = (int) (SAMPLE_RATE * SECONDS);
        byte[] pcm = new byte[samples * BYTES_PER_SAMPLE];
        for (int n = 0; n < samples; n++) {
            double t = n / SAMPLE_RATE;
            double value = 0;
            for (int i = 0; i < NOTES.length; i++) {
                double local = t - STARTS[i];
                if (local < 0) {
                    continue;
                }
                double envelope = Math.min(1, local / ATTACK) * Math.exp(-DECAY * local);
                double phase = 2 * Math.PI * NOTES[i] * local;
                value += envelope * (Math.sin(phase) + OVERTONE * Math.sin(2 * phase));
            }
            double tail = Math.min(1, (SECONDS - t) / FADE_OUT); // ends at silence, no pop
            short sample = (short) Math.round(Math.max(-1, Math.min(1, value * VOLUME * tail)) * Short.MAX_VALUE);
            pcm[BYTES_PER_SAMPLE * n] = (byte) sample;                       // low byte first
            pcm[BYTES_PER_SAMPLE * n + 1] = (byte) (sample >> Byte.SIZE);
        }
        return pcm;
    }
}
