package ru.yandex.market.pbcat;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

/**
 * Тест проверяет, коррекную работу тулзы при разных разделителях
 *
 * @author s-ermakov
 */
public class DelimerValidationTest {

    private final static String TEST_PB_FILE = "videoreviews-data.pb";
    private final static String TEST_JSON_FILE = "videoreviews-data.json";

    private File pbFile;
    private File jsonFile;

    @Before
    public void setUp() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        pbFile = new File(classLoader.getResource(TEST_PB_FILE).getFile());

        jsonFile = new File(pbFile.getParentFile(), TEST_JSON_FILE);
        jsonFile.delete();
    }

    @Test
    public void validateNoneDelimeter() throws Exception {
        // arrange
        String[] args = new String[] {
                "--input-file", pbFile.getAbsolutePath(),
                "--output-file", jsonFile.getAbsolutePath(),
                "--output-format", "json",
                "--magic", "MBVR",
                "--delimer", "none"
        };

        // act
        JavaPbcat.main(args);

        // assert
        try (Reader reader = new FileReader(jsonFile)) {
            Gson gson = new Gson();
            gson.fromJson(reader, Object.class);
        }
    }
}
