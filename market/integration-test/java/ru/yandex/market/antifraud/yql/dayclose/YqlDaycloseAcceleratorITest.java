package ru.yandex.market.antifraud.yql.dayclose;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.model.SessionStatusEnum;
import ru.yandex.market.antifraud.util.IntDateUtil;
import ru.yandex.market.antifraud.util.SleepUtil;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YqlSessionType;
import ru.yandex.market.antifraud.yql.model.YtConfig;
import ru.yandex.market.antifraud.yql.model.YtLogConfig;
import ru.yandex.market.antifraud.yql.validate.YqlSessionToTxMap;
import ru.yandex.market.antifraud.yql.validate.YqlValidator;
import ru.yandex.market.antifraud.yql.validate.YtTestDataGenerator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YqlDaycloseAcceleratorITest {

    public static final YtLogConfig LOG_CONF = new YtLogConfig("market-new-shows-log");

    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Autowired
    private YqlDaycloseAccelerator yqlDaycloseAccelerator;

    @Autowired
    private YtConfig ytConfig;

    @Autowired
    private YtTestDataGenerator testDataGenerator;

    @Before
    public void checkTestData() {
        testDataGenerator.initOnce();
    }

    @Test
    public void thisSessionWontCloseDay() throws Exception {
        jdbcTemplate.exec("truncate sessions");
        long sessionId = insertSession(YqlSessionType.NORMAL);
        assertThat(yqlDaycloseAccelerator.getRunningSessionsThatWillNotCloseDay(20121012).size(),
            is(1));
        assertThat(yqlDaycloseAccelerator.getRunningSessionsThatWillNotCloseDay(20121012).get(0),
            is(sessionId));
    }

    @Test
    public void thisSessionMayCloseday() throws Exception {
        jdbcTemplate.exec("truncate sessions");
        long sessionId = insertSession(YqlSessionType.DAYCLOSING);
        assertThat(yqlDaycloseAccelerator.getRunningSessionsThatWillNotCloseDay(20121012).size(),
            is(0));
    }

    private long insertSession(YqlSessionType type) {
        return jdbcTemplate.query("insert into sessions " +
            "(day, scale, log, cluster, last_seen_event_id, host, status, type, seen_partitions) values " +
            "(20121012, 'RECENT', '" + LOG_CONF.getLogName() + "', '" + ytConfig.getCluster() + "', 0, 'host1'," +
            "'" + SessionStatusEnum.PENDING + "'," + "'" + type + "', " +
            "'[\"2012-10-12T13:00:00\", \"2012-10-12T23:00:00\"]') returning session_id", Long.class);

    }
}
