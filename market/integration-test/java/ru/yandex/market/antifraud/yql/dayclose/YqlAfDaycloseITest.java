package ru.yandex.market.antifraud.yql.dayclose;

import com.google.common.base.Joiner;
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
import ru.yandex.market.antifraud.util.IntDateUtil;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.YtConfig;
import ru.yandex.market.antifraud.yql.model.YtLogConfig;
import ru.yandex.market.antifraud.yql.step.LfTimestampParser;
import ru.yandex.market.antifraud.yql.validate.UnvalidatedDaysHelperITest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.antifraud.yql.model.UnvalidatedDay.Scale.ARCHIVE;
import static ru.yandex.market.antifraud.yql.model.UnvalidatedDay.Scale.RECENT;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YqlAfDaycloseITest {
    private static final int DAY = 20171120;
    public static final YtLogConfig LOG_CONF = new YtLogConfig("market-new-shows-log");

    @Autowired
    private YqlAfDayclose showsDayclose;

    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Autowired
    private YtConfig ytConfig;

    @Before
    public void setPrevClosedDay() {
        jdbcTemplate.exec("truncate step_events");
        jdbcTemplate.exec("truncate closed_days");
        jdbcTemplate.exec("insert into closed_days (log, day, reason) values " +
                "('" + UnvalidatedDaysHelperITest.LOG_CONF.getLogName().getLogName() + "', " + (DAY - 1) + ", 'test_day_before')");
    }

    @Test
    public void mustCloseWhenAllRecentValidated() {
        String recentScale = LOG_CONF.getScales().get(RECENT);
        insertEvents(YtLogConfig.scaleNameToPartitionQty.get(recentScale));
        showsDayclose.closeDay();
        assertClosed();
    }

    @Test
    public void mustNotCloseWhenNotAllRecentValidated() {
        String recentScale = LOG_CONF.getScales().get(RECENT);
        insertEvents(YtLogConfig.scaleNameToPartitionQty.get(recentScale) - 1);
        showsDayclose.closeDay();
        assertNotClosed();
    }

    @Test
    public void mustNotCloseWhenNoEventsValidated() {
        showsDayclose.closeDay();
        assertNotClosed();
    }

    @Test
    public void mustCloseWhenRecentDroppedButArchiveValidated() {
        String recentScale = LOG_CONF.getScales().get(RECENT);
        insertEvents(YtLogConfig.scaleNameToPartitionQty.get(recentScale));
        jdbcTemplate.exec("update step_events set partition_dropped = true, validated = false where event_id = (select event_id from step_events limit 1)");
        insertStepEvent(
                LOG_CONF.getScales().get(ARCHIVE),
                IntDateUtil.hyphenated(DAY) + "T00:00:00",
                true,
                false);
        showsDayclose.closeDay();
        assertClosed();
    }

    public long insertStepEvent(String scale, String timestamp, boolean validated, boolean dropped) {
        return insertStepEvent(scale, timestamp, validated, dropped, null);
    }

    private void assertClosed() {
        assertHelper(1);
    }

    private void assertNotClosed() {
        assertHelper(0);
    }

    private void assertHelper(int count) {
        assertThat(jdbcTemplate.query("select count(*) from closed_days where log = :log and day = :day",
                "log", UnvalidatedDaysHelperITest.LOG_CONF.getLogName().getLogName(),
                "day", DAY,
                Integer.class), is(count));
    }

    private void insertEvents(int count) {
        for(int i = 0; i < count; i++) {
            insertStepEvent(
                    UnvalidatedDaysHelperITest.LOG_CONF.getScales().get(RECENT),
                    IntDateUtil.hyphenated(DAY) + "T03:00:00", true, false);
        }
    }

    public long insertStepEvent(String scale, String timestamp, boolean validated, boolean dropped, String stepTimeCreated) {
        Map<String, Object> params = new HashMap<>();
        params.put("event_step_id", RndUtil.randomAlphabetic(16));
        params.put("cluster", ytConfig.getCluster());
        params.put("event_name", "cluster_table_publish");
        params.put("log", LOG_CONF.getLogName().getLogName());
        params.put("scale", scale);
        params.put("day", LfTimestampParser.lfTimestampToDay(timestamp));
        params.put("timestamp", timestamp);
        params.put("validated", validated);
        params.put("partition_dropped", dropped);
        Set<String> cols = params.keySet();
        String colNames = Joiner.on(", ").join(cols);
        String placeholders = Joiner.on(", ").join(cols.stream()
                .map(c -> ":" + c)
                .collect(Collectors.toList()));
        if(stepTimeCreated == null) {
            return jdbcTemplate.query("" +
                            "insert into step_events " +
                            "(" + colNames + ", step_time_created) values " +
                            "(" + placeholders + ", to_timestamp(:timestamp, 'YYYY-MM-DD\"T\"HH24:MI:SS')) " +
                            "returning event_id",
                    params,
                    Long.class);
        }
        else {
            params.put("step_time_created", stepTimeCreated);
            return jdbcTemplate.query("" +
                            "insert into step_events " +
                            "(" + colNames + ", step_time_created) values " +
                            "(" + placeholders + ", to_timestamp(:step_time_created, 'YYYY-MM-DD\"T\"HH24:MI:SS')) " +
                            "returning event_id",
                    params,
                    Long.class);
        }
    }
}
