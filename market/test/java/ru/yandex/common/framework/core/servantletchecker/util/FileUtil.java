package ru.yandex.common.framework.core.servantletchecker.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author agorbunov @ Oct 25, 2010
 */
public class FileUtil {

    public static String readFile(String fileName) {
        try {
            return readFileImp(fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String readFileImp(String fileName) throws IOException {
        File file = new File(fileName);
        StringBuilder contents = new StringBuilder();

        BufferedReader input = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
        } finally {
            input.close();
        }
        return contents.toString();
    }
}
