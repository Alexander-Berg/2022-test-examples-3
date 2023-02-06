package utils;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;


// TODO: 16/03/17 IT IS TMP VERSION, NEED IMPROVEMENT

class FixtureLoader {
    static final String FIXTURE = "/fixture";

    String getWithReplacing(String path, HashMap<String, String> replacements) {
        String fileData = getFileContent(path);

        for (String key : replacements.keySet()) {
            String replacement = replacements.get(key);

            if (replacement == null) {
                replacement = "";
            }

            fileData = fileData.replace("{" + key + "}", replacement);
        }

        return fileData;
    }

    String getFileContent(String path) {
        return new String(getByteContent(path));
    }

    byte[] getByteContent(String path) {
        try {
            return Files.readAllBytes(Paths.get(getUrlResource(path).toURI()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private URL getUrlResource(String path) {
        return getClass().getResource(FIXTURE + path);
    }
}
