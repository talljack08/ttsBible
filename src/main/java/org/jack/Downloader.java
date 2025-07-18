package org.jack;

import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// responsible for deciding how many chapters and getting final text per section
public class Downloader
{

    public Downloader(String changingBit, String translation) throws IOException {
        Files.createDirectories(Paths.get("bible-tts")); // if it doesn't exist already

        String nextChapter = "GEN.1";
        String chapter = nextChapter;

        // preserves previous data by using custom CsvWriter method
        CSVWriter writer = new CSVWriter(new FileWriter("bible-tts/" + translation.toLowerCase() + ".csv"));

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
