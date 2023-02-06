package ru.yandex.direct.mysql.slowlog.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 *  Загружает набор исходных строк с нераспарсенными записями, а так же результат их эталонного парсинга в формате json
 */
public class TestDataResourcesLoader
{
    /**
     * Загружает массив непустых строк из ресурса resourceName
     * @param resourceName имя ресурса, откуда загружать строки
     * @return массив прочитанных непустых строк
     */
    public static String[] loadTestLinesFromResource(String resourceName) {
        List<String> lines = new ArrayList<>();
        try (InputStream inputStream =
                     TestDataResourcesLoader.class.getResourceAsStream(resourceName)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (RuntimeException runex) {
            throw new RuntimeException(
                    String.format("Failed to load non empty string lines from '%s' resource file", resourceName),
                    runex);
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Resource file '%s' not found", resourceName), ex);
        }
        return lines.toArray(String[]::new);
    }

    /**
     * Загружает массив исходных строк с записями slow query лога, и массив соответствующих им эталонных распарсенных
     * строк в формате json
     * @return массив из двух элементов. В элементе с индексом 0 - массив исходных строк с записями slow query лога,
     * в элементе с индексом 1 - массив соответствующих им эталонных распарсенных строк в формате json
     */
    public static String[][] loadTestData() {
        String[][] testData = new String[2][];
        testData[0] = loadTestLinesFromResource("/raws_input.txt");
        testData[1] = loadTestLinesFromResource("/raws_parsed.txt");
        if (testData[0].length != testData[1].length) {
            throw new RuntimeException(String.format(
                    "Input rows count (%d) from resource '/raws_input.txt' " +
                            "not equals parsed rows count (%d) from '/raws_parsed.txt'",
                    testData[0].length, testData[1].length));
        }
        if (testData[0].length == 0) {
            throw new RuntimeException("Test data is empty");
        }

        for (int i = 0; i < testData[0].length; i++) {
            testData[0][i] = testData[0][i]
                    .replace("\\n", "\n")
                    .replace("\\t", "\t");
        }

        return testData;
    }
}
