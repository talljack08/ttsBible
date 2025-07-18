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

public class Tts {
    private final ArrayList<String> chunks = new ArrayList<>();

    public Tts(String text) {
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

    public void createFiles() throws InvalidDataException, UnsupportedTagException, IOException {
        // creates paths
        Files.createDirectories(Paths.get("bible-tts")); // if it doesn't exist already
        Files.createDirectories(Paths.get("bible-tts/chunks"));

        // creating individual files
        int chunkNum = 1;
        for (String chunk : chunks)
        {
            System.out.print("Creating chunk " + chunkNum + "/" + chunks.size() + "... ");
            createFile(chunk, chunkNum);
            chunkNum++;
            System.out.println("Done");
        }

        // date
        LocalDateTime ldt = LocalDateTime.now();
        if(Main.dayOffset == -1)
        {
            ldt = ldt.minusDays(1);
        }
        String fileName = "bible-tts/" + DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.ENGLISH).format(ldt) + ".mp3";

        // combine chunks
        System.out.print("Stitching... ");
        Mp3Stitcher.stitchMp3Files(chunkNum, fileName);
        System.out.println("Done");
        FileUtils.deleteDirectory(new File("bible-tts/chunks"));
    }

    public void createFile(String text, int index) {
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