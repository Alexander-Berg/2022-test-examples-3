package ru.yandex.direct.sql.normalizer;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Проверяет корректность парсинга и сортировки запроса путем сравнения его с эталоном, так как написать тесты
 * на каждую возможную значимую комбинацию символов внутри запроса не представляется реальным в разумные сроки.
 * Поэтому все возможные варианты были учтены в эталонных тестах. Путь Яндекс.Контеста.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class SorterTest {
    private static Map<Integer, Map<TestQueryFileTypeEnum, String>> testFilesMap;
    private static char[] inputBuffer;
    private static char[] outputBuffer;

    @BeforeAll
    public static void beforeAll() {
        testFilesMap = TestQueriesResourcesLoader.loadTestQueries();
        inputBuffer = new char[256 * 1024];
        outputBuffer = new char[256 * 1024];
    }

    @Test
    public void testQueryFieldsSorting() {
        for (Map.Entry<Integer, Map<TestQueryFileTypeEnum, String>> entry: testFilesMap.entrySet()) {
            int fileIndex = entry.getKey();
            Map<TestQueryFileTypeEnum, String> sourcesMap = entry.getValue();
            String query = sourcesMap.get(TestQueryFileTypeEnum.ORIGINAL);
            String goodResult = sourcesMap.get(TestQueryFileTypeEnum.SORTED);
            int normalFormLength = QueryParser.parseQuery(query, inputBuffer);
            QueryFieldsSorter sorter = new QueryFieldsSorter(inputBuffer, normalFormLength);
            IQueryReader reader = sorter.sortQueryFields();
            reader.copyToBuffer(outputBuffer);
            String result = new String(outputBuffer, 0, reader.getLength());
            assertEquals(goodResult, result, String.format("Query with index %d has failed test", fileIndex));
        }
    }

    @Test
    public void testQueryFieldsSortingDouble() {
        for (Map.Entry<Integer, Map<TestQueryFileTypeEnum, String>> entry: testFilesMap.entrySet()) {
            int fileIndex = entry.getKey();
            Map<TestQueryFileTypeEnum, String> sourcesMap = entry.getValue();
            String query = sourcesMap.get(TestQueryFileTypeEnum.ORIGINAL);
            String goodResult = sourcesMap.get(TestQueryFileTypeEnum.SORTED);
            int normalFormLength = QueryParser.parseQuery(query, inputBuffer);
            QueryFieldsSorter sorter = new QueryFieldsSorter(inputBuffer, normalFormLength);
            IQueryReader reader = sorter.sortQueryFields();
            reader.copyToBuffer(outputBuffer);
            sorter = new QueryFieldsSorter(outputBuffer, reader.getLength());
            reader = sorter.sortQueryFields();
            reader.copyToBuffer(inputBuffer);
            String result = new String(inputBuffer, 0, reader.getLength());
            assertEquals(goodResult, result, String.format("Query with index %d has failed test", fileIndex));
        }
    }
}
