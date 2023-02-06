package ru.yandex.market.pbcat;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

/**
 * Тест проверяет, коррекную работу компрессоров
 *
 * @author s-ermakov
 */
public class CompressorValidationTest {

    private final static String TEST_GZIP_PB_FILE = "title-maker-templates.pbf.gz";
    private final static String TEST_SNAP_PB_FILE = "fpcat.pbuf.sn";

    private File gzipPbFile;
    private File snapPbFile;
    private File gzipJsonFile;
    private File snapJsonFile;

    @Before
    public void setUp() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        gzipPbFile = new File(classLoader.getResource(TEST_GZIP_PB_FILE).getFile());
        snapPbFile = new File(classLoader.getResource(TEST_SNAP_PB_FILE).getFile());

        gzipJsonFile = new File(gzipPbFile.getParentFile(), TEST_GZIP_PB_FILE + ".json");
        snapJsonFile = new File(snapPbFile.getParentFile(), TEST_SNAP_PB_FILE + ".json");

        gzipJsonFile.delete();
        snapJsonFile.delete();
    }

    @Test
    public void validateGzipCompressor() throws Exception {
        // arrange
        String[] args = new String[] {
                "--input-file", gzipPbFile.getAbsolutePath(),
                "--output-file", gzipJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--magic", "MBOM",
                "--compressor", "gzip"
        };

        // act
        JavaPbcat.main(args);

        // assert
        try (Reader reader = new FileReader(gzipJsonFile)) {
            Gson gson = new Gson();
            gson.fromJson(reader, Object.class);
        }
    }

    @Test
    public void validateAutomateCompressorByGzip() throws Exception {
        // arrange
        String[] args = new String[] {
                "--input-file", gzipPbFile.getAbsolutePath(),
                "--output-file", gzipJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--magic", "MBOM"
        };

        // act
        JavaPbcat.main(args);

        // assert
        try (Reader reader = new FileReader(gzipJsonFile)) {
            Gson gson = new Gson();
            gson.fromJson(reader, Object.class);
        }
    }

    @Test
    public void validateAutomateCompressorBySnap() throws Exception {
        // arrange
        String[] args = new String[] {
                "--input-file", snapPbFile.getAbsolutePath(),
                "--output-file", snapJsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--delimer", "le"
        };

        // act
        JavaPbcat.main(args);

        // assert
        try (Reader reader = new FileReader(snapJsonFile)) {
            Gson gson = new Gson();
            gson.fromJson(reader, Object.class);
        }
    }
}
