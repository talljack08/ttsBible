package org.jack;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Arrays;

// responsible for getting and parsing text
public class Parser
{
    private String humanChapter;
    private final int finalVerse;
    private String nextChapter;
    private String rawText;
    private final String mode;

    // online parser
    public Parser(String mode, String changingBit, String chapter, String translation)
    {
        this.mode = mode;

        JSONObject chapterJSON;
        if(!mode.equals("offline"))
        {
            String translationLowercase = translation.toLowerCase();
            int tid = switch(translationLowercase)
            {
                case "niv" -> 111;
                case "esv" -> 59;
                case "kvj" -> 1;
                case "cev" -> 392;
                default -> 463;
            };

            String url = "https://www.bible.com/_next/data/" + changingBit + "/en/audio-bible/" + tid + "/";
            chapterJSON = makeRequest(url + chapter + "." + translation + ".json?versionId=" + tid + "&usfm=" + chapter + "." + translation);

            try
            {
                // human chapter
                humanChapter = chapterJSON.getJSONObject("pageProps").getJSONObject("chapterInfo")
                        .getJSONObject("reference").getString("human");
            }
            // if request doesn't work, defaults to slower protocol
            catch (JSONException ignored)
            {
                System.out.println("Request failed, using slower protocol.");
                url = "https://www.bible.com/_next/data/" + changingBit + "/en/bible/" + tid + "/";
                chapterJSON = makeRequest(url + chapter + ".json");
                humanChapter = chapterJSON.getJSONObject("pageProps").getJSONObject("chapterInfo")
                        .getJSONObject("reference").getString("human");
            }

            // last verse & raw text
            if(humanChapter.contains("Introduction"))
            {
                rawText = "";
                finalVerse = 0;
            }
            else
            {
                // last verse
                finalVerse = getLastVerse(chapterJSON.getJSONObject("pageProps")
                        .getJSONObject("chapterInfo").getString("content"));

                // If it can't find the JSON entry, it has to be the other protocol; if the request failed earlier, the
                // program would have halted a while ago
                try
                {
                    rawText = chapterJSON.getJSONObject("pageProps").getString("chapterText")
                            .replace('\n', ' ');
                } catch (JSONException ignored)
                {
                    rawText = format(chapterJSON.getJSONObject("pageProps").getJSONObject("chapterInfo")
                            .getString("content"));
                }
            }

            // next chapter
            if(!chapter.equals("REV.22"))
            {
                JSONObject next = chapterJSON.getJSONObject("pageProps").getJSONObject("chapterInfo")
                        .getJSONObject("next");
                if (next == null)
                {
                    nextChapter = "none";
                }
                else
                {
                    nextChapter = next.getJSONArray("usfm").getString(0);
                }
            }
            else
            {
                // made up, sensical next for REV.22
                nextChapter = "GEN.INTRO1";
            }
        }
        else
        {
            String[] info = OfflineBible.getChapterInfo(chapter.toUpperCase());
            humanChapter = info[0];
            nextChapter = info[1];
            finalVerse = Integer.parseInt(info[2]);
            rawText = info[3];
        }


    }

    public String getHumanChapter()
    {
        return humanChapter;
    }

    public int getFinalVerse()
    {
        return finalVerse;
    }

    public String getNextChapter()
    {
        if(!mode.equals("download"))
        {
            switch (nextChapter) {
                case "PRO.INTRO1" -> nextChapter = "PSA.1";
                case "PSA.INTRO1" -> nextChapter = "PRO.1";
                case "MAT.INTRO1" -> nextChapter = "GEN.1";
                case "GEN.INTRO1" -> nextChapter = "MAT.1";
            }
        }

        return nextChapter;
    }

    private String format(String html)
    {
        // defines html and final text variables
        StringBuilder text = new StringBuilder();
        Document doc = Jsoup.parse(html);

        // filters divs to only divs that contain raw content
        Elements divs = doc.select("div");
        Elements filteredDivs = new Elements();
        for(Element div: divs)
        {
            boolean goodName = true;
            for(String className: div.classNames())
            {
                if (Arrays.asList("version", "book", "chapter").contains(className))
                {
                    goodName = false;
                    break;
                }
            }

            if(goodName)
            {
                filteredDivs.add(div);
            }
        }

        // goes through the content, removing any html bloat
        for(Element div: filteredDivs)
        {
            Elements verses = div.select("span.verse");

            for (Element verse : verses) {
                Elements contents = verse.select("span.content");
                for (Element content : contents) {
                    String rawText = content.wholeText();
                    text.append(rawText);
                }
            }

            text.append(" ");
        }

        // removes double spaces
        String scrapedText = text.toString();
        StringBuilder deDoubleSpacedText = new StringBuilder();
        for(int i = 0; i < scrapedText.length(); i++)
        {
            String letter = scrapedText.substring(i, i+1);
            if(letter.equals(" "))
            {
                while(i < scrapedText.length()-1 && scrapedText.charAt(i + 1) == ' ')
                {
                    i++;
                }
            }

            deDoubleSpacedText.append(letter);
        }

        // returns final StringBuilder as a regular String without trailing space
        return deDoubleSpacedText.substring(1);
    }

    public String getRawText()
    {
        return rawText;
    }

    // makes basic request using okhttp
    public static JSONObject makeRequest(String link)
    {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(link)
                .build();

        String responseString = "{}";

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                assert response.body() != null;
                responseString = response.body().string();
            } else {
                System.out.println("Request failed: " + response.code());
            }
        } catch (IOException ignored) {}

        return new JSONObject(responseString);
    }

    // returns the amount of verses in a given chapter
    private int getLastVerse(String html)
    {
        if(html.matches("^[0-9]*$"))
        {
            return Integer.parseInt(html);
        }

        int verse = 0;

        for(int i = 0; i < html.length(); i++)
        {
            if(html.startsWith("<span class=\"verse v", i))
            {
                i+=20;
                verse = 0;
                while(html.substring(i, i+1).matches("^\\d$"))
                {
                    verse *= 10;
                    verse += Integer.parseInt(html.substring(i, i+1));

                    i+=1;
                }
            }
        }

        return verse;
    }
}
