package ru.yandex.market.pricelabs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.extern.slf4j.Slf4j;

import ru.yandex.market.pricelabs.misc.Utils;

@Slf4j
public class TempDirUtils {

    private static final String RAM_DISK_FOLDER = System.getenv("YA_TEST_RAM_DRIVE_PATH");

    private TempDirUtils() {
        //
    }

    public static String getTempDir() throws IOException {
        var path = String.valueOf(
                Utils.isEmpty(RAM_DISK_FOLDER) ?
                        Files.createTempDirectory("pricelabs-") :
                        Files.createTempDirectory(Path.of(RAM_DISK_FOLDER), "pricelabs-"));

        log.info("Using temp directory: {}", path);
        return path;
    }
}
