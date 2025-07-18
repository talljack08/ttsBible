package org.jack;


import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.*;
import java.util.ArrayList;

public class Mp3Stitcher {
    public static void stitchMp3Files(int numFiles, String outputPath) throws IOException, UnsupportedTagException, InvalidDataException {
        ArrayList<String> inputPaths = new ArrayList<>();
        for(int i = 1; i < numFiles; i++)
        {
            inputPaths.add("bible-tts/chunks/chunk-" + i + ".mp3");
        }
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            for (String path : inputPaths) {
                Mp3File mp3 = new Mp3File(path);
                if (mp3.hasId3v1Tag() || mp3.hasId3v2Tag()) {
                    mp3.removeId3v1Tag();
                    mp3.removeId3v2Tag(); // remove metadata to avoid corruption
                }

                byte[] bytes = readMp3Frames(new File(path));
                fos.write(bytes);
            }
        }
    }

    private static byte[] readMp3Frames(File file) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
    }
}
