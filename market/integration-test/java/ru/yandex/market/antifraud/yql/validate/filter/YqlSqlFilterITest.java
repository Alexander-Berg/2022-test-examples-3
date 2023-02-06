package ru.yandex.market.antifraud.yql.validate.filter;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.util.IntDateUtil;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YqlSession;
import ru.yandex.market.antifraud.yql.model.YtConfig;
import ru.yandex.market.antifraud.yql.validate.YtTestDataGenerator;
import ru.yandex.market.antifraud.yql.yt.YtTablesHelper;

import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YqlSqlFilterITest {

    @Autowired
    private YqlSqlFilter first100Filter;

    @Autowired
    private LoggingJdbcTemplate yqlJdbcTemplate;

    @Autowired
    private YtTablesHelper ytTablesHelper;

    @Autowired
    private YtTestDataGenerator testDataGenerator;

    @Autowired
    private YtConfig ytConfig;

    @Before
    public void initTestData() {
        testDataGenerator.initOnce();
    }

    @Test
    public void testArchiveFilter() throws NoSuchFieldException, IllegalAccessException {
        String archiveTable = ytConfig.getLogPath(
                testDataGenerator.log(),
                UnvalidatedDay.Scale.ARCHIVE,
                testDataGenerator.getArchiveDay());
        testFilter(
                new UnvalidatedDay(testDataGenerator.getArchiveDay(), 0, UnvalidatedDay.Scale.ARCHIVE),
                "[" + archiveTable + "]");
    }

    @Test
    public void testRecentFilter() throws NoSuchFieldException, IllegalAccessException {
        String recentDir = ytConfig.getLogPath(testDataGenerator.log(), UnvalidatedDay.Scale.RECENT, "_partition_placeholder_")
                .replace("/_partition_placeholder_", "");
        testFilter(new UnvalidatedDay(testDataGenerator.getRecentDay(), 0, UnvalidatedDay.Scale.RECENT),
                "CONCAT(" + Joiner.on(",").join(ytTablesHelper.list(recentDir)
                    .stream()
                    .map((p) -> "'" + recentDir + "/" + p + "'")
                    .collect(Collectors.toList())) + ")"
                );
    }

    private void testFilter(UnvalidatedDay d, String partitions) {
        String ytTmpRollbacksPath = ytConfig.getTmpRollbacksDir(testDataGenerator.log()) +
                "/20171030_test_0";
        try {
            YqlSession session = Mockito.mock(YqlSession.class);
            Mockito.when(session.getDay()).thenReturn(d);
            Mockito.when(session.getTmpRollbacksFile()).thenReturn(
                    ytTmpRollbacksPath
            );
            Mockito.when(session.getPartitionsForQuery()).thenReturn(partitions);
            Mockito.when(session.getPoolPragma()).thenReturn("");

            first100Filter.apply(session);

            long cnt = yqlJdbcTemplate.query("select count(*) " +
                            "from CONCAT('" + ytTmpRollbacksPath + "') " +
                            "where filter = :filter",
                    "filter", first100Filter.getId(),
                    Long.class);

            assertThat(cnt, is(100L));
        } finally {
            ytTablesHelper.remove(ytTmpRollbacksPath);
        }
    }
}
