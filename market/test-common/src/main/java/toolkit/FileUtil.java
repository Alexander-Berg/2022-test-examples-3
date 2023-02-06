package toolkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;

public class FileUtil {

    private FileUtil() {
    }

    /**
     * Получает String из файла с телом запроса.
     * Подставляет значения переданных аргументов вместо плейсхолдеров.
     */
    public static String bodyStringFromFile(String filePath, Object... args) {
        try {
            File file = getFile(filePath);

            String reqBodyString = new Scanner(file).useDelimiter("\\Z").next();

            return String.format(reqBodyString, args);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static JsonNode jsonNodeFromFile(String filePath) {
        try {
            File file = getFile(filePath);
            return Mapper.getDefaultMapper().readTree(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static File getFile(String filePath) {
        ClassLoader classLoader = FileUtil.class.getClassLoader();
        try {
            File tmpFile = File.createTempFile("temp", "");
            InputStream resourceAsStream = classLoader.getResourceAsStream(filePath);
            if (resourceAsStream == null) {
                throw new RuntimeException("Отсутствует файл по пути " + filePath);
            }
            FileUtils.copyInputStreamToFile(resourceAsStream, tmpFile);
            return tmpFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getBytes(String filePath) {
        try {
            return Files.readAllBytes(getFile(filePath).toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
