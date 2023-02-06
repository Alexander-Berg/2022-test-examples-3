package ru.yandex.market.pbcat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.google.gson.Gson;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Тест проверяет, что формат выходных данных (xml и json) валидный.
 *
 * @author s-ermakov
 */
public class OutputValidationTest {

    private final static String TEST_PB_FILE = "parameters_997520.pb";
    private final static String TEST_TEXT_FILE = "parameters_997520.txt";
    private final static String TEST_JSON_FILE = "parameters_997520.json";
    private final static String TEST_XML_FILE = "parameters_997520.xml";

    private File pbFile;
    private File textFile;
    private File jsonFile;
    private File xmlFile;

    @Before
    public void setUp() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        pbFile = new File(classLoader.getResource(TEST_PB_FILE).getFile());

        textFile = new File(pbFile.getParentFile(), TEST_TEXT_FILE);
        jsonFile = new File(pbFile.getParentFile(), TEST_JSON_FILE);
        xmlFile = new File(pbFile.getParentFile(), TEST_XML_FILE);

        textFile.delete();
        jsonFile.delete();
        xmlFile.delete();
    }

    @Test
    public void validateCorrectJsonOutputFile() throws Exception {
        // arrange
        String[] args = new String[] {
                "--input-file", pbFile.getAbsolutePath(),
                "--output-file", jsonFile.getAbsolutePath(),
                "--output-format", "json"
        };

        // act
        JavaPbcat.main(args);

        // assert
        try (Reader reader = new FileReader(jsonFile)) {
            Gson gson = new Gson();
            gson.fromJson(reader, Object.class);
        }
    }

    @Test
    public void validateCorrectXmlOutputFile() throws Exception {
        // arrange
        String[] args = new String[] {
                "--input-file", pbFile.getAbsolutePath(),
                "--output-file", xmlFile.getAbsolutePath(),
                "--output-format", "xml"
        };

        // act
        JavaPbcat.main(args);

        StringBuilder fileText = readFile(xmlFile);

        // assert total xml parsing
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        documentBuilder.parse(new ByteArrayInputStream(fileText.toString().getBytes()));

        // assert text correctness (MBO-11102)
        Assert.assertThat(fileText, new CharSequenceMatcher("<messages><Category>"));
    }

    @Test
    public void validateRussianSymbolsParsingInText() throws Exception {
        // arrange
        String[] args = new String[] {
                "--input-file", pbFile.getAbsolutePath(),
                "--output-file", textFile.getAbsolutePath(),
                "--output-format", "text"
        };

        // act
        JavaPbcat.main(args);

        // assert
        StringBuilder fileText = readFile(textFile);

        Assert.assertThat(fileText, new CharSequenceMatcher("Мини-печи, ростеры"));
        Assert.assertThat(fileText, new CharSequenceMatcher("Минимальная цена"));
        Assert.assertThat(fileText, new CharSequenceMatcher("Модель-источник при копировании"));
    }

    @Test
    public void validateRussianSymbolsParsingInXml() throws Exception {
        // arrange
        String[] args = new String[] {
                "--input-file", pbFile.getAbsolutePath(),
                "--output-file", xmlFile.getAbsolutePath(),
                "--output-format", "xml"
        };

        // act
        JavaPbcat.main(args);

        // assert
        StringBuilder fileText = readFile(xmlFile);

        Assert.assertThat(fileText, new CharSequenceMatcher("Мини-печи, ростеры"));
        Assert.assertThat(fileText, new CharSequenceMatcher("Минимальная цена"));
        Assert.assertThat(fileText, new CharSequenceMatcher("Модель-источник при копировании"));
    }

    @Test
    public void validateRussianSymbolsParsingInJson() throws Exception {
        // arrange
        String[] args = new String[] {
                "--input-file", pbFile.getAbsolutePath(),
                "--output-file", jsonFile.getAbsolutePath(),
                "--output-format", "json"
        };

        // act
        JavaPbcat.main(args);

        // assert
        StringBuilder fileText = readFile(jsonFile);

        Assert.assertThat(fileText, new CharSequenceMatcher("Мини-печи, ростеры"));
        Assert.assertThat(fileText, new CharSequenceMatcher("Минимальная цена"));
        Assert.assertThat(fileText, new CharSequenceMatcher("Модель-источник при копировании"));
    }

    private StringBuilder readFile(File input) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(input))) {
            char[] buffer = new char[2048];

            int readChars;
            while ((readChars = bufferedReader.read(buffer)) > 0) {
                stringBuilder.append(buffer, 0, readChars);
            }
        }
        return stringBuilder;
    }

    private class CharSequenceMatcher extends TypeSafeMatcher<CharSequence> {

        private final CharSequence substring;

        public CharSequenceMatcher(CharSequence substring) {
            this.substring = substring;
        }

        @Override
        protected boolean matchesSafely(CharSequence item) {
            Pattern pattern = Pattern.compile(substring.toString());
            Matcher matcher = pattern.matcher(item);
            return matcher.find();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a string containing ")
                       .appendValue(substring);
        }
    }
}
