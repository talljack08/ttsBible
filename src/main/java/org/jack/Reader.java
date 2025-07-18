package org.jack;

import com.opencsv.exceptions.CsvException;

import java.io.IOException;

// responsible for deciding how many chapters and getting final text per section
public class Reader
{
    private String finalText = "";
    private final String chapter;

    private double goal;
    private int read;
    private final String translation;

    public Reader(String mode, String changingBit, String chapter, String translation, double goal) throws IOException, CsvException {
        this.translation = translation;
        this.goal = goal;
        int read = 0;

        chapter = removeIntro(chapter);

        while(true)
        {
            Parser chapterData = new Parser(mode, changingBit, chapter, translation);

            // if you are within goal or if the amount you are going to read make you closer to the goal or if long psalm
            if(read + chapterData.getFinalVerse() <= goal || Math.abs(goal - read - chapterData.getFinalVerse()) < Math.abs(goal - read) || (goal == -99999 && chapter.equalsIgnoreCase("psa.119")))
            {
                read += chapterData.getFinalVerse();

                // Makes all book title numbers alphanumeric
                if(!chapterData.getHumanChapter().contains("Introduction"))
                {
                    finalText += chapterData.getHumanChapter().substring(0, 1)
                            .replace("1", "first").replace("2", "second")
                            .replace("3", "third") + chapterData.getHumanChapter()
                            .substring(1) + ". ";

                    // adds text with formatting for tts
                    finalText += chapterData.getRawText().toLowerCase() + ". ";



                    if(mode.equals("online"))
                    {
                        System.out.print("Pulled ");
                    } else {
                        System.out.print("Found ");
                    }
                    System.out.println(chapterData.getHumanChapter());
                }

                chapter = chapterData.getNextChapter();
            }
            else
            {
                this.chapter = chapter;
                this.read = read;
                break;
            }

        }
    }

    public String getFinalText()
    {
        return finalText;
    }

    public String getNextChapter()
    {
        return removeIntro(chapter);
    }

    public double getReadingOffset()
    {
        // subtraction without floating point subtraction
        return (goal * 10 - read * 10)/10;
    }

    public void setOffset(double offset)
    {
        goal = offset;
        read = 0;
    }

    private String removeIntro(String chapter)
    {
        if(chapter.toLowerCase().contains("intro"))
        {
            chapter = chapter.substring(0, 4) + "1";
            if(chapter.equalsIgnoreCase("esg.1") && translation.equalsIgnoreCase("nabre"))
            {
                chapter += "_1";
            }

        }

        return chapter;
    }
}
