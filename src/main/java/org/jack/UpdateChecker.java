package org.jack;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.IOException;

public class UpdateChecker extends Thread
{
    private final Gui gui;

    public UpdateChecker(Gui gui)
    {
        this.gui = gui;
    }

    public void run()
    {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://raw.githubusercontent.com/talljack08/ttsBible/refs/heads/main/pom.xml").build();
        try
        {
            Response response = client.newCall(request).execute();
            if(response.isSuccessful())
            {
                assert response.body() != null;
                String xml = response.body().string();

                for(int i = 0; i < xml.length(); i++)
                {
                    if(xml.startsWith("<version>", i))
                    {
                        i+=9;
                        StringBuilder version = new StringBuilder();
                        while(xml.substring(i, i+1).matches("[1234567890.]"))
                        {
                            version.append(xml.charAt(i));
                            i++;
                        }

                        ComparableVersion latestVersion = new ComparableVersion(version.toString());
                        ComparableVersion currentVersion = new ComparableVersion("2.0.0");

                        if(currentVersion.compareTo(latestVersion) < 0)
                        {
                            gui.setTitle("ttsBible (version " + version + " available)");
                        }

                        break;
                    }
                }
            } else {
                System.out.println("Github Server Error: " + response.code());
            }
        } catch (IOException ignored) {
            System.out.println("Failed to fetch latest version (are you offline?)");

            gui.setOfflineMode();
        }
    }
}
