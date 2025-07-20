package org.jack;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

// responsible for deciding how many chapters and getting final text per section
public class Downloader
{

    public Downloader(String changingBit, String translation) throws IOException, InterruptedException {
        Files.createDirectories(Paths.get("bible-tts")); // if it doesn't exist already

        Tts t = new Tts("");
        String out = t.checkForPiper();
        if(!out.isEmpty())
        {
            System.out.println(out);
        }


        if(!translation.contains("tts"))
        {
            String nextChapter = "GEN.1";
            String chapter = nextChapter;

            // preserves previous data by using custom CsvWriter method
            CSVWriter writer = new CSVWriter(new FileWriter("bible-tts/offline/" + translation.toLowerCase() + ".csv"));

            while(!chapter.equalsIgnoreCase("rev.22"))
            {
                chapter = nextChapter;
                Parser r = new Parser("download", changingBit, chapter, translation);


                System.out.println("Ripped " + r.getHumanChapter());
                String[] record = {chapter, r.getHumanChapter(), r.getNextChapter(), String.valueOf(r.getFinalVerse()),
                        r.getRawText().replace("\"", "")}; // can't have quotations in a csv
                writer.writeNext(record);


                nextChapter = r.getNextChapter();
            }

            writer.close();
        }
    }
}
