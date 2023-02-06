package ru.yandex.market.antifraud.yql.web;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.model.SessionStatusEnum;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.YqlSession;
import ru.yandex.market.antifraud.yql.validate.YtTestDataGenerator;
import ru.yandex.market.antifraud.yql.yt.YtTablesHelper;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YqlForceRollbackITest {

    @Autowired
    private YqlForceRollback forceRollback;

    @Autowired
    private YtTablesHelper ytTablesHelper;

    @Autowired
    private LoggingJdbcTemplate yqlJdbcTemplate;

    @Autowired
    private LoggingJdbcTemplate pgaasJdbcTemplate;

    @Autowired
    private YtTestDataGenerator testDataGenerator;

    @Before
    public void prepareTestData() {
        testDataGenerator.initOnce();
        pgaasJdbcTemplate.exec("truncate sessions");
    }

    @Test
    public void mustDryRun() {
        long filterId = RndUtil.nextPostitiveLong();
        long count = forceRollback.dryRun(
            testDataGenerator.log(),
            testDataGenerator.getArchiveDay(),
            "select ${fields} from ${dayPartitions} limit 10",
            filterId,
            RndUtil.randomAlphabetic(8));
        assertThat(count, is(10L));
    }

    @Test
    public void mustRollback() {
        long filterId = RndUtil.nextPostitiveLong();
        YqlSession session = forceRollback.forceRollback(
            testDataGenerator.log(),
            testDataGenerator.getArchiveDay(),
            "select ${fields} from ${dayPartitions} limit 10",
            filterId,
            RndUtil.randomAlphabetic(8));
        assertThat(session.getStatus(), is(SessionStatusEnum.FILTERS_EXECUTED));
        assertTrue(ytTablesHelper.exists(session.getFinalRollbacksFile()));
        Map<String, Long> rowidToFilter = new HashMap<>();
        yqlJdbcTemplate.query("select rowid, filter " +
            "from CONCAT('" + session.getFinalRollbacksFile() + "')",
            rs -> {
                rowidToFilter.put(rs.getString("rowid"), rs.getLong("filter"));
            });
        assertThat(rowidToFilter.size(), is(10));
        for(long realFilterId: rowidToFilter.values()) {
            assertThat(realFilterId, is(filterId));
        }
    }
}
