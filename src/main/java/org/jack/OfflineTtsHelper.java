package org.jack;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class OfflineTtsHelper
{
    public static String voiceModel = "en_US-joe-medium";

    public static ArrayList<String> getCommandStart()
    {
        ArrayList<String> command = new ArrayList<>();

        if(System.getProperty("os.name").toLowerCase().startsWith("windows"))
        {
            command.add("cmd.exe");
            command.add("/c");
        }
        else
        {
            command.add("/bin/bash");
            command.add("-c");
        }

        return command;
    }

    public static String checkForPiper(ArrayList<String> command) throws IOException, InterruptedException {
        command.add(formatCommand("python3 -m piper -h"));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File("."));
        Process process = processBuilder.start();
        process.waitFor();


        File model = new File("bible-tts/offline/" + voiceModel + ".onnx");
        if(!model.exists())
        {
            return handleMissingMod(command);
        }

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
        {
            if(reader.readLine() != null)
            {
                command.remove(command.size()-1);
                command.add(formatCommand("python3 -m lameenc"));
                processBuilder = new ProcessBuilder(command);
                processBuilder.directory(new File("."));
                process = processBuilder.start();
                process.waitFor();

                try(BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    if ((line = errorReader.readLine()) != null) {
                        if (!line.contains("mod")) {
                            if(Main.voice.equals("offline"))
                            {
                                System.out.println("Piper-tts & pydub found!");
                                return "";
                            }
                            else
                            {
                                System.out.println("Piper-tts & pydub already installed.");
                                return "";
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // windows / bash errors (python not installed)
        try(BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream())))
        {
            String line;
            try {
                if((line = errorReader.readLine()) != null) {
                    if(line.contains("command"))
                    {
                        String out = "Error: Please install python3 and run with flags \"--download tts\"";
                        if(Main.voice.equals("offline"))
                        {
                            System.err.println();
                            return out;
                        }
                        else
                        {
                            return out + " to finish the download";
                        }
                    }
                    else if(line.contains("module"))
                    {
                        return handleMissingMod(command);
                    }
                }
            } catch (IOException ignored) {
                return handleMissingMod(command);
            }

        }

        return "";
    }

    public static String handleMissingMod(ArrayList<String> command) throws InterruptedException, IOException {
        if(!Main.voice.equals("offline"))
        {
            System.out.print("Downloading piper-tts... ");

            command.remove(command.size()-1); // removes previous command
            command.add(formatCommand("python3 -m pip install piper-tts --break-system-packages"));
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            process.waitFor();

            System.out.println("Done.");
            System.out.print("Downloading lameenc... ");

            command.remove(command.size()-1);
            command.add(formatCommand("python3 -m pip install lameenc --break-system-packages"));
            processBuilder = new ProcessBuilder(command);
            Path offlineFolder = Paths.get(System.getProperty("user.dir"), "bible-tts", "offline");
            Files.createDirectories(offlineFolder);
            processBuilder.directory(offlineFolder.toFile());
            process = processBuilder.start();
            process.waitFor();

            System.out.println("Done.");
            System.out.print("Downloading TTS model... ");

            File model = Paths.get(System.getProperty("user.dir"), "bible-tts", "offline", voiceModel + ".onnx").toFile();
            if(!model.exists())
            {
                command.remove(command.size()-1);
                command.add(formatCommand("python3 -m piper.download_voices " + voiceModel));
                processBuilder = new ProcessBuilder(command);
                Files.createDirectories(offlineFolder);
                processBuilder.directory(Paths.get(System.getProperty("user.dir"), "bible-tts", "offline").toFile());
                process = processBuilder.start();
                process.waitFor();

                System.out.println("Done.");
            }
            else {
                System.out.println("Skipped.");
            }

        } else {
            return "Error: Python found with no piper-tts/lameenc/model, please run with flags " +
                    "\"--download tts\" when internet is available";
        }

        return "";
    }

    public static String formatCommand(String text)
    {
        if(System.getProperty("os.name").toLowerCase().startsWith("windows"))
        {
            text = text.replace("python3", "py");
        }

        return text;
    }
}
