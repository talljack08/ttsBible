package org.jack;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class Tts {
    private final ArrayList<String> chunks = new ArrayList<>();
    public final ArrayList<String> command = new ArrayList<>();

    public Tts(String text) {
        text = text
                .replace("“", "").replace("”", "")
                .replace("’", "").replace("ark ", "arc ")
                .replace("ark.", "arc.")
                .replace("philistine", "phill-ist-een")
                .replace("selah", "seelah.");

        // chunking system
        StringBuilder currentChunk = new StringBuilder();
        int relativeIndex = 0;
        for(int i = 0; i < text.length(); i++)
        {
            String letter = text.substring(i, i+1);

            if(relativeIndex < 7500 || !letter.equals("."))
            {
                currentChunk.append(letter);
            }
            else
            {
                currentChunk.append(".");
                relativeIndex = 0;
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
                continue;
            }

            relativeIndex++;
        }
        chunks.add(currentChunk.toString());
    }

    public String checkForPiper() throws IOException, InterruptedException {
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

        command.add("python3 -m piper -h");
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File("."));
        Process process = processBuilder.start();
        process.waitFor();


        File model = new File("bible-tts/offline/en_US-lessac-medium.onnx");
        if(!model.exists())
        {
            return handleMissingMod(command);
        }

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
        {
            if(reader.readLine() != null)
            {
                command.remove(command.size()-1);
                command.add("python3 -m lameenc");
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

    public String handleMissingMod(ArrayList<String> command) throws InterruptedException, IOException {
        if(!Main.voice.equals("offline"))
        {
            System.out.print("Downloading piper-tts... ");

            command.remove(command.size()-1); // removes previous command
            command.add("python3 -m pip install piper-tts --break-system-packages");
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            process.waitFor();

            System.out.println("Done.");
            System.out.print("Downloading lameenc... ");

            command.remove(command.size()-1);
            command.add("python3 -m pip install lameenc --break-system-packages");
            processBuilder = new ProcessBuilder(command);
            Files.createDirectories(Paths.get("bible-tts/offline"));
            processBuilder.directory(new File(System.getProperty("user.dir") + "/bible-tts/offline"));
            process = processBuilder.start();
            process.waitFor();

            System.out.println("Done.");
            System.out.print("Downloading TTS model... ");

            File model = new File("bible-tts/offline/en_US-lessac-medium.onnx");
            if(!model.exists())
            {
                command.remove(command.size()-1);
                command.add("python3 -m piper.download_voices en_US-lessac-medium");
                processBuilder = new ProcessBuilder(command);
                Files.createDirectories(Paths.get("bible-tts/offline/"));
                processBuilder.directory(new File("bible-tts/offline/"));
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

    public String createFiles() throws InvalidDataException, UnsupportedTagException, IOException, InterruptedException {
        // creates paths
        Files.createDirectories(Paths.get("bible-tts")); // if it doesn't exist already
        Files.createDirectories(Paths.get("bible-tts/chunks"));

        // makes sure piper is installed for offline usage
        String error = "";
        if(Main.voice.equals("offline"))
        {
            error = checkForPiper();
            if(!error.isEmpty())
            {
                return error;
            }
        }

        // creating individual files
        int chunkNum = 1;
        for (String chunk : chunks)
        {
            System.out.print("Creating chunk " + chunkNum + "/" + chunks.size() + "... ");
            createFile(chunk, chunkNum);
            chunkNum++;
            System.out.println("Done");
        }

        // date
        LocalDateTime ldt = LocalDateTime.now();
        if(Main.dayOffset == -1)
        {
            ldt = ldt.minusDays(1);
        }
        String fileName = "bible-tts/" + DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.ENGLISH).format(ldt) + ".mp3";

        // combine chunks
        System.out.print("Stitching... ");
        Mp3Stitcher.stitchMp3Files(chunkNum, fileName);
        System.out.println("Done");
        FileUtils.deleteDirectory(new File("bible-tts/chunks"));

        return "";
    }

    public void createFile(String text, int index)
    {
        if(Main.voice.equals("offline"))
        {
            try
            {
                FileWriter myWriter = new FileWriter("bible-tts/chunks/offline.py");
                myWriter.write("import wave\n" +
                        "import lameenc\n" +
                        "from piper import PiperVoice\n" +
                        "\n" +
                        "voice = PiperVoice.load(\"bible-tts/offline/en_US-lessac-medium.onnx\")\n" +
                        "with wave.open(\"bible-tts/chunks/out.wav\", \"wb\") as wav_file:\n" +
                        "    voice.synthesize_wav(\"" + text + "\", wav_file)\n" +
                        "\n" +
                        "# Open WAV file (must be 16-bit PCM)\n" +
                        "with wave.open(\"bible-tts/chunks/out.wav\", \"rb\") as wav_file:\n" +
                        "    # Check format\n" +
                        "    assert wav_file.getsampwidth() == 2, \"Only 16-bit WAV supported\"\n" +
                        "    channels = wav_file.getnchannels()\n" +
                        "    sample_rate = wav_file.getframerate()\n" +
                        "    raw_pcm = wav_file.readframes(wav_file.getnframes())\n" +
                        "\n" +
                        "encoder = lameenc.Encoder()\n" +
                        "encoder.set_bit_rate(128)\n" +
                        "encoder.set_in_sample_rate(sample_rate)\n" +
                        "encoder.set_channels(channels)\n" +
                        "encoder.set_quality(2)\n" +
                        "\n" +
                        "mp3_data = encoder.encode(raw_pcm)\n" +
                        "mp3_data += encoder.flush()\n" +
                        "\n" +
                        "with open(\"bible-tts/chunks/chunk-" + index + ".mp3\", \"wb\") as f:\n" +
                        "    f.write(mp3_data)");
                myWriter.close();

                command.remove(command.size()-1); // removes previous command
                command.add("python3 bible-tts/chunks/offline.py");
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                Process process = processBuilder.start();
                process.waitFor();


            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        else
        {
            Voice voice = TTSVoice.provides().stream().filter(v -> v.getShortName().equals(Main.voice))
                    .toList().get(0);

            new TTS(voice, text)
                    .findHeadHook()
                    .isRateLimited(true)
                    .fileName("chunk-" + index)
                    .overwrite(true)
                    .storage("bible-tts/chunks/")
                    .formatMp3()
                    .trans();

        }
    }
}