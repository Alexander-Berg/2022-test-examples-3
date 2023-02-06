package ru.yandex.direct.sql.normalizer;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Проверяет корректность полной нормализации запроса путем сравнения его с эталоном, так как написать тесты
 * на каждую возможную значимую комбинацию символов внутри запроса не представляется реальным в разумные сроки.
 * Поэтому все возможные варианты были учтены в эталонных тестах. Путь Яндекс.Контеста.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class NormalizerTest {
    private static Map<Integer, Map<TestQueryFileTypeEnum, String>> testFilesMap;
    private static QueryNormalizer normalizer;

    @BeforeAll
    public static void beforeAll() {
        testFilesMap = TestQueriesResourcesLoader.loadTestQueries();
        normalizer = TestQueriesResourcesLoader.getNormalizer();
    }

    @Test
    public void testQueryNormalizing() {
        for (Map.Entry<Integer, Map<TestQueryFileTypeEnum, String>> entry: testFilesMap.entrySet()) {
            int fileIndex = entry.getKey();
            Map<TestQueryFileTypeEnum, String> sourcesMap = entry.getValue();
            String query = sourcesMap.get(TestQueryFileTypeEnum.ORIGINAL);
            String goodResult = sourcesMap.get(TestQueryFileTypeEnum.NORMALIZED);
            String result = normalizer.normalizeQuery(query);
            assertEquals(goodResult, result, String.format("Query with index %d has failed test", fileIndex));
        }
    }

    @Test
    public void testQueryNormalizingDouble() {
        for (Map.Entry<Integer, Map<TestQueryFileTypeEnum, String>> entry: testFilesMap.entrySet()) {
            int fileIndex = entry.getKey();
            Map<TestQueryFileTypeEnum, String> sourcesMap = entry.getValue();
            String query = sourcesMap.get(TestQueryFileTypeEnum.ORIGINAL);
            String goodResult = sourcesMap.get(TestQueryFileTypeEnum.NORMALIZED);
            String result = normalizer.normalizeQuery(query);
            result = normalizer.normalizeQuery(result);
            assertEquals(goodResult, result, String.format("Query with index %d has failed test", fileIndex));
        }
    }
}
