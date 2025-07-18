package org.jack;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class OfflineBible
{
    private static final Dictionary<String, String[]> bible = new Hashtable<>();

    public static void importBible(String translation) throws IOException, CsvException {
        CSVReader reader = new CSVReader(new FileReader("bible-tts/" + translation.toLowerCase() + ".csv"));
        List<String[]> records = reader.readAll();
        for (String[] record : records) {
            bible.put(record[0], new String[]{record[1], record[2], record[3], record[4]});

//            humanChapter = record[1];
//            nextChapter = record[2];
//            finalVerse = Integer.parseInt(record[3]);
//            rawText = record[4];
        }
        reader.close();
    }

    public static String[] getChapterInfo(String chapter)
    {
        return bible.get(chapter);
    }
}
