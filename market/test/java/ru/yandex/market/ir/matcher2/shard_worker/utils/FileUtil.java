package ru.yandex.market.ir.matcher2.shard_worker.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileUtil {
    private FileUtil() {
    }

    public static String getAbsolutePath(String path) {
        return getAbsolutePath(path, null);
    }

    public static String getAbsolutePath(String path, String testFileName) {
        String result = new File(FileUtil.class.getResource(path).getPath()).getAbsolutePath();
        if (testFileName != null && Files.notExists(Paths.get(result + "/" + testFileName))) {
            result = new File("./" + path).getAbsolutePath();
        }
        return result;
    }
}
