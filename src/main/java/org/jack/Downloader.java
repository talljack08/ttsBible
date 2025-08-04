package org.jack;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.jack.OfflineTtsHelper.checkForPiper;
import static org.jack.OfflineTtsHelper.getCommandStart;

// responsible for deciding how many chapters and getting final text per section
public class Downloader extends Thread
{
    private final DownloaderGUI gui;
    private final String changingBit;
    private final String translation;

    public Downloader(String changingBit, String translation)
    {
        this.gui = null;
        this.changingBit = changingBit;
        this.translation = translation;
        start();
    }

    public Downloader(String changingBit, String translation, DownloaderGUI gui) throws IOException, InterruptedException
    {
        this.gui = gui;
        this.changingBit = changingBit;
        this.translation = translation;
        start();
    }

    public void run() {
        try {
            Files.createDirectories(Paths.get("bible-tts")); // if it doesn't exist already
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(gui != null)
        {
            gui.addLine("Installing required python packages");
        }
        String out;
        try {
            out = checkForPiper(getCommandStart());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(!out.isEmpty())
        {
            print(out);
        }


        if(!translation.contains("tts"))
        {
            String nextChapter = "GEN.1";
            String chapter = nextChapter;

            // preserves previous data by using custom CsvWriter method
            CSVWriter writer;
            try {
                writer = new CSVWriter(new FileWriter("bible-tts/offline/" + translation.toLowerCase() + ".csv"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            while(!chapter.equalsIgnoreCase("rev.22"))
            {
                chapter = nextChapter;
                Parser r = new Parser("download", changingBit, chapter, translation);

                print("Ripped " + r.getHumanChapter());

                String[] record = {chapter, r.getHumanChapter(), r.getNextChapter(), String.valueOf(r.getFinalVerse()),
                        r.getRawText().replace("\"", "")}; // can't have quotations in a csv
                writer.writeNext(record);
                nextChapter = r.getNextChapter();
            }

            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if(gui != null)
        {
            gui.getOptions().setDownloaderComplete();
        }
    }

    private void print(String text)
    {
        System.out.println(text);
        if(gui != null)
        {
            gui.addLine(text);
        }
    }
}
