package ru.yandex.yt.yqltest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.yt.ytclient.tables.TableSchema;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 01.09.2021
 */
public class YqlTestRunnerTest {
    public static final String BASE_PATH = "//base/path";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private YtClient ytClient;
    private JdbcTemplate yqlJdbcTemplate;
    private YqlTestRunner runner;

    @BeforeEach
    public void init() {
        ytClient = mock(YtClient.class);
        yqlJdbcTemplate = mock(JdbcTemplate.class);
        runner = new YqlTestRunner(
            ytClient,
            yqlJdbcTemplate,
            BASE_PATH
        );
    }

    @Test
    public void testRunner() {
        YPath resultPath = runner.runTest(
            YqlTestScript.fromFile("/yql/simple_query.sql"),
            "/yql/simple_test_expected.json",
            (s, s2) -> {
            },
            "/yql/simple_mock.mock"
        );

        ArgumentCaptor<YPath> mockName = ArgumentCaptor.forClass(YPath.class);

        verify(ytClient, times(1)).createTable(mockName.capture(), any(TableSchema.class));
        verify(ytClient, times(1)).append(eq(mockName.getValue()), anyList());

        // remove mock and result
        verify(ytClient, times(2)).remove(any(YPath.class));
        verify(ytClient, times(1)).remove(eq(mockName.getValue()));
        verify(ytClient, times(1)).remove(eq(resultPath));

        // source table should not be removed
        verify(ytClient, times(0)).remove(eq(YPath.simple("//prod/table")));

        ArgumentCaptor<String> yqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(yqlJdbcTemplate, times(1)).update(yqlCaptor.capture());

        log.info(yqlCaptor.getValue());

        assertTrue(yqlCaptor.getValue().contains(
            "DEFINE SUBQUERY $_table($tablename) as\n" +
                "    $target_name = case\n" +
                "    when $tablename = '//prod/table' then '" + mockName.getValue() + "'\n" +
                "    else 'unknown-'||$tablename end\n" +
                ";\n" +
                "    SELECT * FROM $target_name with inline;\n" +
                "END DEFINE;"
        ));
        assertTrue(yqlCaptor.getValue().contains(
            "select * from $_table('//prod/table')\n" +
                "\n" +
                "insert into `" + resultPath + "`\n" +
                "select * from $yql"
        ));
    }
}
