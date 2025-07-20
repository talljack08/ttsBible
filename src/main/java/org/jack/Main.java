package org.jack;

import com.opencsv.exceptions.CsvException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class Main
{
    public final static double otDaily = 82.8;
    public final static double ntDaily = 26.3;
    public final static double psDaily = 16.8;
    public static boolean doDouble = false;
    public static String voice = "";
    public static int dayOffset = 0;

    public static void main(String[] args) throws IOException, CsvException, InterruptedException {
        if(args.length > 1 && args[0].toLowerCase().contains("-download"))
        {
            new Downloader(getChangingBit(), args[1]);
        }
        else if (args.length > 1 && args[0].toLowerCase().contains("-stats"))
        {
            Statistics.getStats(args[1]);
        }
        else
        {
            new Gui();
        }
    }

    // gets the part of the url that changes every few days/hours
    public static String getChangingBit() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("https://www.bible.com/").build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                assert response.body() != null;
                String html = response.body().string();
                for(int i = 0; i < html.length(); i++)
                {
                    if(html.startsWith("_buildManifest", i))
                    {
                        return html.substring(i-22, i-1);
                    }
                }
            } else {
                System.out.println("Request failed: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }
}