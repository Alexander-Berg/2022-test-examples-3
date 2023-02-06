package ru.yandex.direct.sql.normalizer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Класс для поиска всех тестовых запросов из ресурсов
 */
public class TestQueriesResourcesLoader {
    /**
     * Возвращает карту тестовых запросов
     * @return карта тестовых запросов
     */
    public static Map<Integer, Map<TestQueryFileTypeEnum, String>> loadTestQueries() {
        try {
            Map<Integer, Map<TestQueryFileTypeEnum, String>> testFilesMap = new HashMap<>();
            String nameFilter = "query";
            Enumeration<URL> resources = TestQueriesResourcesLoader.class
                    .getClassLoader().getResources("queries/");
            if (resources.hasMoreElements()) {
                URL testDirectoryUrl = resources.nextElement();
                File testDirectory = Paths.get(testDirectoryUrl.toURI()).toFile();
                for (File testFile : Objects.requireNonNull(testDirectory.listFiles())) {
                    if (testFile.isFile() && testFile.canRead()) {
                        String fileName = testFile.getName();
                        if (fileName.startsWith(nameFilter) && fileName.endsWith(".sql")) {
                            int index = Integer.parseInt(
                                    fileName.substring(nameFilter.length(), nameFilter.length() + 2));

                            Map<TestQueryFileTypeEnum, String> fileTypesMap =
                                    testFilesMap.computeIfAbsent(index, notUsed -> new HashMap<>());

                            String fileTypeSelector =
                                    fileName.substring(nameFilter.length() + 2, nameFilter.length() + 4);

                            TestQueryFileTypeEnum fileType;

                            switch (fileTypeSelector) {
                                case ".s":
                                    fileType = TestQueryFileTypeEnum.ORIGINAL;
                                    break;
                                case "_p":
                                    fileType = TestQueryFileTypeEnum.PARSED;
                                    break;
                                case "_s":
                                    fileType = TestQueryFileTypeEnum.SORTED;
                                    break;
                                case "_n":
                                    fileType = TestQueryFileTypeEnum.NORMALIZED;
                                    break;
                                default:
                                    throw new IllegalArgumentException(
                                            String.format("Unknown file type: %s, file name: %s",
                                                    fileTypeSelector, fileName));
                            }
                            fileTypesMap.put(fileType, Files.readString(testFile.toPath(), StandardCharsets.UTF_8));
                        }
                    }
                }
            } else {
                throw new IOException("Unable to find resource directory 'direct/' with test queries");
            }
            if (testFilesMap.isEmpty()) {
                throw new IllegalArgumentException("Resource directory 'direct/' has no valid test files");
            }
            for (Map.Entry<Integer, Map<TestQueryFileTypeEnum, String>> entry: testFilesMap.entrySet()) {
                if (entry.getValue().size() != 4) {
                    throw new IllegalArgumentException(String.format("Test query %d has only %d of 4 forms",
                            entry.getKey(), entry.getValue().size()));
                }
            }
            return testFilesMap;
        } catch (Exception ex) {
            throw new RuntimeException("Test queries not found", ex);
        }
    }

    /**
     * Возвращает нормализатор запроса, инициализированный параметрами
     * из ресурсного файла '/queries/normalizer.properties'
     * @return нормализатор запроса, инициализированный параметрами из ресурсного файла '/queries/normalizer.properties'
     */
    public static QueryNormalizer getNormalizer() {
        Properties prop = new Properties();
        try (InputStream inputStream =
                     TestQueriesResourcesLoader.class.getResourceAsStream("/queries/normalizer.properties")) {
            prop.load(inputStream);
            int maxClosingBracesInNormalForm = Integer.parseInt(prop.getProperty("max_closing_braces_in_normal_form"));
            int maxNormalFormLength = Integer.parseInt(prop.getProperty("max_normal_form_length"));
            int bufferSize  = Integer.parseInt(prop.getProperty("buffer_size"));
            return new QueryNormalizer(maxClosingBracesInNormalForm, maxNormalFormLength, bufferSize);
        } catch (RuntimeException runex) {
            throw new RuntimeException(
                    "Failed to load QueryNormalizer properties from '/queries/normalizer.properties' resource file",
                    runex);
        } catch (Exception ex) {
            throw new RuntimeException("QueryNormalizer properties not found", ex);
        }
    }
}
