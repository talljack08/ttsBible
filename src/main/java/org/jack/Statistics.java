package org.jack;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Statistics
{
    public static void getStats(String translation) throws IOException, CsvException {
        File offlineBible = new File("bible-tts/" + translation.toLowerCase() + ".csv");
        if(!offlineBible.exists())
        {
            System.out.println("ERROR: Please download bible first.");
        }
        else
        {
            System.out.println("Stats for " + translation + ":");
            CSVReader reader = new CSVReader(new FileReader("bible-tts/" + translation.toLowerCase() + ".csv"));
            List<String[]> records = reader.readAll();

            int totalOT = 0;
            int totalNT = 0;
            int totalPSA = 0;
            boolean ot = true;

            for(String[] record: records)
            {
                if(record[1].contains("Introduction"))
                {
                    continue;
                }
                else if(!ot || record[1].contains("Matthew"))
                {
                    ot = false;
                }

                if(ot)
                {
                    totalOT += Integer.parseInt(record[3]);
                    if(record[1].contains("Psalm"))
                    {
                        totalPSA += Integer.parseInt(record[3]);
                    }
                }
                else
                {
                    totalNT += Integer.parseInt(record[3]);
                }

            }

            System.out.println("OT Verses: " + totalOT);
            System.out.println("\tPsalms: " + totalPSA);
            System.out.println("\tOthers: " + (totalOT-totalPSA));
            System.out.println("NT Verses: " + totalNT);
        }

    }
}
