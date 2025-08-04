package org.jack;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

import static org.jack.OfflineTtsHelper.*;

public class Tts extends Thread {
    private final ArrayList<String> chunks = new ArrayList<>();
    private final Gui gui;

    public Tts(String text, Gui gui) {
        this.gui = gui;
        text = text
                .replace("“", "").replace("”", "")
                .replace("’", "").replace("ark ", "arc ")
                .replace("ark.", "arc.")
                .replace("philistine", "phill-ist-een")
                .replace("selah", "seelah.");

        // chunking system
        StringBuilder currentChunk = new StringBuilder();
        int relativeIndex = 0;
        for(int i = 0; i < text.length(); i++)
        {
            String letter = text.substring(i, i+1);

            if(relativeIndex < 7500 || !letter.equals("."))
            {
                currentChunk.append(letter);
            }
            else
            {
                currentChunk.append(".");
                relativeIndex = 0;
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
                continue;
            }

            relativeIndex++;
        }
        chunks.add(currentChunk.toString());
    }

    public void run() {
        // creates paths
        try {
            Files.createDirectories(Paths.get(System.getProperty("user.dir"), "bible-tts")); // if it doesn't exist already
            Files.createDirectories(Paths.get(System.getProperty("user.dir"), "bible-tts", "chunks"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // makes sure piper is installed for offline usage
        String error;
        if(Main.voice.equals("offline"))
        {
            try {
                error = checkForPiper(getCommandStart());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            if(!error.isEmpty())
            {
                Main.confirmation = error;
                return;
            }
        }

        // creating individual files
        int chunkNum = 1;
        String title = gui.getTitle();

        // regex to see if the program has already made a file before
        if(title.matches(".*\\(\\d.*"))
        {
            String temp = title;
            int pIndex = 0;
            while(temp.contains("("))
            {
                pIndex = temp.indexOf("(") + pIndex+1;
                temp = title.substring(pIndex);
            }

            title = title.substring(0, pIndex-1);
        }

        for (String chunk : chunks)
        {
            System.out.print("Creating chunk " + chunkNum + "/" + chunks.size() + "... ");
            gui.setTitle(title + " (" + (chunkNum-1) + "/" + chunks.size() + ")");
            createFile(chunk, chunkNum);
            chunkNum++;
            System.out.println("Done");
        }
        gui.setTitle(title + " (" + chunks.size() + "/" + chunks.size() + ")");

        // date
        LocalDateTime ldt = LocalDateTime.now();
        if(Main.dayOffset == -1)
        {
            ldt = ldt.minusDays(1);
        }
        String fileName = "bible-tts/" + DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.ENGLISH).format(ldt) + ".mp3";

        // combine chunks
        System.out.print("Stitching... ");
        try {
            Mp3Stitcher.stitchMp3Files(chunkNum, fileName);
        } catch (IOException | UnsupportedTagException | InvalidDataException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Done");
        try {
            FileUtils.deleteDirectory(Paths.get(System.getProperty("user.dir"), "bible-tts", "chunks").toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        gui.alert();
    }

    public void createFile(String text, int index)
    {
        if(Main.voice.equals("offline"))
        {
            try
            {
                FileWriter myWriter = new FileWriter("bible-tts/chunks/offline.py");
                myWriter.write("import wave\n" +
                        "import lameenc\n" +
                        "from piper import PiperVoice\n" +
                        "\n" +
                        "voice = PiperVoice.load(\"bible-tts/offline/" + voiceModel + ".onnx\")\n" +
                        "with wave.open(\"bible-tts/chunks/out.wav\", \"wb\") as wav_file:\n" +
                        "    voice.synthesize_wav(\"" + text + "\", wav_file)\n" +
                        "\n" +
                        "# Open WAV file (must be 16-bit PCM)\n" +
                        "with wave.open(\"bible-tts/chunks/out.wav\", \"rb\") as wav_file:\n" +
                        "    # Check format\n" +
                        "    assert wav_file.getsampwidth() == 2, \"Only 16-bit WAV supported\"\n" +
                        "    channels = wav_file.getnchannels()\n" +
                        "    sample_rate = wav_file.getframerate()\n" +
                        "    raw_pcm = wav_file.readframes(wav_file.getnframes())\n" +
                        "\n" +
                        "encoder = lameenc.Encoder()\n" +
                        "encoder.set_bit_rate(128)\n" +
                        "encoder.set_in_sample_rate(sample_rate)\n" +
                        "encoder.set_channels(channels)\n" +
                        "encoder.set_quality(2)\n" +
                        "\n" +
                        "mp3_data = encoder.encode(raw_pcm)\n" +
                        "mp3_data += encoder.flush()\n" +
                        "\n" +
                        "with open(\"bible-tts/chunks/chunk-" + index + ".mp3\", \"wb\") as f:\n" +
                        "    f.write(mp3_data)");
                myWriter.close();

                ArrayList<String> command = getCommandStart();
                command.add(formatCommand("python3 bible-tts/chunks/offline.py"));
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                Process process = processBuilder.start();
                process.waitFor();


            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        else
        {
            Voice voice = TTSVoice.provides().stream().filter(v -> v.getShortName().equals(Main.voice))
                    .toList().get(0);

            new TTS(voice, text)
                    .findHeadHook()
                    .isRateLimited(true)
                    .fileName("chunk-" + index)
                    .overwrite(true)
                    .storage("bible-tts/chunks/")
                    .formatMp3()
                    .trans();

        }
    }
}