package ru.yandex.direct.sql.normalizer;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Проверяет корректность парсинга запроса путем сравнения его с эталоном, так как написать тесты на каждую возможную
 * значимую комбинацию символов внутри запроса не представляется реальным в разумные сроки. Поэтому все возможные
 * варианты были учтены в эталонных тестах. Путь Яндекс.Контеста.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class ParserTest {
    private static Map<Integer, Map<TestQueryFileTypeEnum, String>> testFilesMap;
    private static char[] buffer;
    @BeforeAll
    public static void beforeAll() {
        testFilesMap = TestQueriesResourcesLoader.loadTestQueries();
        buffer = new char[256 * 1024];
    }

    @Test
    public void testQueryParsing() {
        for (Map.Entry<Integer, Map<TestQueryFileTypeEnum, String>> entry: testFilesMap.entrySet()) {
            int fileIndex = entry.getKey();
            Map<TestQueryFileTypeEnum, String> sourcesMap = entry.getValue();
            String query = sourcesMap.get(TestQueryFileTypeEnum.ORIGINAL);
            String goodResult = sourcesMap.get(TestQueryFileTypeEnum.PARSED);
            int normalFormLength = QueryParser.parseQuery(query, buffer);
            String result = new String(buffer, 0, normalFormLength);
            assertEquals(goodResult, result, String.format("Query with index %d has failed test", fileIndex));
        }
    }

    @Test
    public void testQueryParsingDouble() {
        for (Map.Entry<Integer, Map<TestQueryFileTypeEnum, String>> entry: testFilesMap.entrySet()) {
            int fileIndex = entry.getKey();
            Map<TestQueryFileTypeEnum, String> sourcesMap = entry.getValue();
            String query = sourcesMap.get(TestQueryFileTypeEnum.ORIGINAL);
            String goodResult = sourcesMap.get(TestQueryFileTypeEnum.PARSED);
            int normalFormLength = QueryParser.parseQuery(query, buffer);
            String result = new String(buffer, 0, normalFormLength);
            normalFormLength = QueryParser.parseQuery(result, buffer);
            result = new String(buffer, 0, normalFormLength);
            assertEquals(goodResult, result, String.format("Query with index %d has failed test", fileIndex));
        }
    }
}
