package ru.yandex.yt.yqltest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.yt.yqltest.impl.YqlTestMock;
import ru.yandex.yt.yqltest.impl.YqlTestMockParser;
import ru.yandex.yt.yqltest.impl.YqlTestMockTable;
import ru.yandex.yt.yqltestable.YqlTestable;

/**
 * Test paths:
 * - base_path - base path for generated tables
 * - base_path/src - source tables. Generates with test+uuid as name.
 * Generated result is converted to json and then compared with expected file
 * After test run - ensure to remove all affected tables (could be disabled for debug)
 *
 * @author Ilya Kislitsyn / ilyakis@ / 31.08.2021
 */
public class YqlTestRunner {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final YtClient ytClient;
    private final JdbcTemplate yqlJdbcTemplate;
    private final YPath mocksDir;
    private final YPath resultDir;

    private boolean cleanAfterRun = true;

    public YqlTestRunner(YtClient ytClient, JdbcTemplate yqlJdbcTemplate, String basePath) {
        this.ytClient = ytClient;
        this.yqlJdbcTemplate = yqlJdbcTemplate;
        this.mocksDir = YPath.simple(basePath).child("mock");
        this.resultDir = YPath.simple(basePath).child("result");
    }

    /**
     * Check script with mocked tables.
     *
     * @param script       Script definition.
     * @param expectedPath Path to file with expected json data.
     * @param mocks        Paths to mocked tables.
     */
    public YPath runTest(YqlTestScript script,
                         String expectedPath,
                         BiConsumer<String, String> checker,
                         String... mocks) {
        YqlTestMock mock = YqlTestMockParser.parseFiles(mocksDir, mocks);

        loadMockTables(mock.getTablesToCreate());

        String yql = mock.mockYql(script.getYql());

        // process request and fetch response json records
        log.info("Generate query result");
        YPath resultPath = resultDir.child(UUID.randomUUID().toString());
        yqlJdbcTemplate.update(
            yql +
                "\n" +
                "insert into `" + resultPath.toString() + "`\n" +
                "select * from $" + script.getRequestProperty()
        );

        log.info("Result generated {}", resultPath);

        log.info("Read generated table");
        String actualJsonList = toJson(ytClient.readNodes(resultPath));

        List<YPath> cleanPaths = new ArrayList<>(mock.getPathsToCleanup());
        cleanPaths.add(resultPath);

        cleanAfterTest(cleanPaths);

        log.info("Check verdict");
        checker.accept(
            YqlTestable.readFile(expectedPath),
            actualJsonList
        );

        return resultPath;
    }

    private String toJson(Object item) {
        try {
            return MAPPER.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to write data", e);
        }
    }

    private void loadMockTables(List<YqlTestMockTable> mockTables) {
        log.info("Create table mocks");

        mockTables.forEach(table -> {
            log.info("Started table mock {}", table.getPath());
            ytClient.createTable(table.getMockPath(), table.getSchema().buildYtSchema());
            ytClient.append(table.getMockPath(), table.getData());
            log.info("Table created {}", table.getMockPath());
        });
    }

    private void cleanAfterTest(List<YPath> paths) {
        if (!cleanAfterRun) {
            log.info("Skip results cleaning");
            return;
        }

        paths.forEach(path -> {
            log.info("Remove mock table {}", path);
            ytClient.remove(path);
        });
    }

    public void setCleanAfterRun(boolean cleanAfterRun) {
        this.cleanAfterRun = cleanAfterRun;
    }

}
