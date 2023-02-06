package ru.yandex.market.antifraud.yql.validate;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.model.SessionStatusEnum;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YtConfig;
import ru.yandex.market.antifraud.yql.model.YtLogConfig;
import ru.yandex.market.antifraud.yql.step.LfTimestampParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class UnvalidatedDaysHelperITest {

    public static final YtLogConfig LOG_CONF = new YtLogConfig("market-new-shows-log");

    @Autowired
    private UnvalidatedDaysHelper unvalidatedDaysHelper;

    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Autowired
    private YtConfig ytConfig;

    @Autowired
    private YqlSessionToTxMap sessionToTxMap;

    @Before
    public void truncate() throws NoSuchFieldException, IllegalAccessException {
        jdbcTemplate.exec("truncate step_events");
        jdbcTemplate.exec("truncate sessions");
    }

    @Test
    public void mustValidateMultipleDays() {
        long l1 = g_1_validateArchiveForDropped();
        long l2 = g_2_validateNewRecentPartition();
        long l3 = g_3_doNotValidateAlreadyValidated();
        long l4 = g_4_doNotValidateAlreadyValidatedWithUnvalidatedArchive();
        long l5 = g_5_doNotValidateDroppedRecentWithoutArchiveEvent();
        long l6 = g_6_validatePartiallyValidated();
        long l7 = g_7_validateArchiveIfOldEnough();
        long l8 = g_8_doNotValidateArchiveIfNotOldEnough();
        long l9 = g_9_validateMultipleNewRecentPartitions();
        long l10 = g_10_validateArchiveIfOneOfRecentWasDropped();
        long l11 = g_11_validateArchiveIfTooOldAndHasUndroppedRecent();

        Map<Integer, UnvalidatedDay> days = daysToMap(unvalidatedDaysHelper.getUnvalidatedDays(LOG_CONF));
        assert_1(days, l1);
        assert_2(days, l2);
        assert_3(days, l3);
        assert_4(days, l4);
        assert_5(days, l5);
        assert_6(days, l6);
        assert_7(days, l7);
        assert_8(days, l8);
        assert_9(days, l9);
        assert_10(days, l10);
        assert_11(days, l11);
    }

    private long g_1_validateArchiveForDropped() {
        insertStepEvent("30min", "2017-08-01T03:00:00", false, true);
        return insertStepEvent("1d", "2017-08-01T00:00:00", false, false);
    }

    private long g_2_validateNewRecentPartition() {
        return insertStepEvent("30min", "2017-08-02T03:00:00", false, false);
    }

    private long g_3_doNotValidateAlreadyValidated() {
        return insertStepEvent("30min", "2017-08-03T03:00:00", true, false);
    }

    private long g_4_doNotValidateAlreadyValidatedWithUnvalidatedArchive() {
        insertStepEvent("30min", "2017-08-04T03:00:00", true, false);
        return insertStepEvent("1d", "2017-08-04T03:00:00", false, false);
    }

    private long g_5_doNotValidateDroppedRecentWithoutArchiveEvent() {
        return insertStepEvent("30min", "2017-08-05T03:00:00", false, true);
    }

    private long g_6_validatePartiallyValidated() {
        insertStepEvent("30min", "2017-08-06T03:00:00", true, false);
        return insertStepEvent("30min", "2017-08-06T03:00:00", false, false);
    }

    private long g_7_validateArchiveIfOldEnough() {
        return insertStepEvent("1d", "2017-08-07T03:00:00", false, false, "2017-11-01T03:00:00");
    }

    private long g_8_doNotValidateArchiveIfNotOldEnough() {
        return insertStepEvent("1d", "2017-08-08T03:00:00", false, false);
    }

    private long g_9_validateMultipleNewRecentPartitions() {
        insertStepEvent("30min", "2017-08-09T03:00:00", false, false);
        insertStepEvent("30min", "2017-08-09T03:30:00", false, false);
        insertStepEvent("30min", "2017-08-09T04:00:00", false, false);
        return insertStepEvent("30min", "2017-08-09T04:30:00", false, false);
    }

    private long g_10_validateArchiveIfOneOfRecentWasDropped() {
        insertStepEvent("30min", "2017-08-10T03:00:00", false, true);
        insertStepEvent("30min", "2017-08-10T03:30:00", false, false);
        insertStepEvent("30min", "2017-08-10T04:00:00", false, false);
        insertStepEvent("30min", "2017-08-10T04:30:00", false, false);
        return insertStepEvent("1d", "2017-08-10T00:30:00", false, false);
    }

    // should not exist in real life
    private long g_11_validateArchiveIfTooOldAndHasUndroppedRecent() {
        insertStepEvent("30min", "2017-08-11T03:00:00", false, false);
        insertStepEvent("30min", "2017-08-11T03:30:00", false, false);
        insertStepEvent("30min", "2017-08-11T04:00:00", false, false);
        return insertStepEvent("1d", "2017-08-11T00:30:00", false, false, "2017-11-11T00:30:00");
    }

    private long g_12_doNotValidateWhatIsBeingValidatedRightNow() {
        long sessionId = insertSession(20170812);
        // this means, that session is actively running
        sessionToTxMap.put(sessionId, GUID.create());
        return insertStepEvent("30min", "2017-08-12T03:00:00", false, false);
    }

    private long g_13_doValidateWhatIsNotBeingValidatedRightNow() {
        long sessionId = insertSession(20170912);
        // this means, that session is actively running
        sessionToTxMap.put(sessionId, GUID.create());
        return insertStepEvent("30min", "2017-08-13T03:00:00", false, false);
    }

    private void assert_1(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertDay(days, 20170801, lastId, UnvalidatedDay.Scale.ARCHIVE);
    }

    private void assert_2(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertDay(days, 20170802, lastId, UnvalidatedDay.Scale.RECENT);
    }

    private void assert_3(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertThat(days.get(20170803), is(nullValue()));
    }

    private void assert_4(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertThat(days.get(20170804), is(nullValue()));
    }

    private void assert_5(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertThat(days.get(20170805), is(nullValue()));
    }

    private void assert_6(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertDay(days, 20170806, lastId, UnvalidatedDay.Scale.RECENT);
    }

    private void assert_7(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertDay(days, 20170807, lastId, UnvalidatedDay.Scale.ARCHIVE);
    }

    private void assert_8(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertThat(days.get(20170808), is(nullValue()));
    }

    private void assert_9(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertDay(days, 20170809, lastId, UnvalidatedDay.Scale.RECENT);
    }

    private void assert_10(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertDay(days, 20170810, lastId, UnvalidatedDay.Scale.ARCHIVE);
    }

    private void assert_11(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertDay(days, 20170811, lastId, UnvalidatedDay.Scale.ARCHIVE);
    }

    private void assert_12(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertThat(days.get(20170812), is(nullValue()));
    }

    private void assert_13(Map<Integer, UnvalidatedDay> days, long lastId) {
        assertDay(days, 20170813, lastId, UnvalidatedDay.Scale.ARCHIVE);
    }

    private Map<Integer, UnvalidatedDay> getUnvalidatedDays() {
        return daysToMap(unvalidatedDaysHelper.getUnvalidatedDays(LOG_CONF));
    }

    @Test
    public void t1() {
        long lastId = g_1_validateArchiveForDropped();
        assert_1(getUnvalidatedDays(), lastId);
    }

    @Test
    public void t2() {
        long lastId = g_2_validateNewRecentPartition();
        assert_2(getUnvalidatedDays(), lastId);
    }

    @Test
    public void t3() {
        g_3_doNotValidateAlreadyValidated();
        assert_3(getUnvalidatedDays(), 0);
    }

    @Test
    public void t4() {
        g_4_doNotValidateAlreadyValidatedWithUnvalidatedArchive();
        assert_4(getUnvalidatedDays(), 0);
    }

    @Test
    public void t5() {
        g_5_doNotValidateDroppedRecentWithoutArchiveEvent();
        assert_5(getUnvalidatedDays(), 0);
    }

    @Test
    public void t6() {
        long lastId = g_6_validatePartiallyValidated();
        assert_6(getUnvalidatedDays(), lastId);
    }

    @Test
    public void t7() {
        long lastId = g_7_validateArchiveIfOldEnough();
        assert_7(getUnvalidatedDays(), lastId);
    }

    @Test
    public void t8() {
        g_8_doNotValidateArchiveIfNotOldEnough();
        assert_8(getUnvalidatedDays(), 0);
    }

    @Test
    public void t9() {
        long lastId = g_9_validateMultipleNewRecentPartitions();
        assert_9(getUnvalidatedDays(), lastId);
    }

    @Test
    public void t10() {
        long lastId = g_10_validateArchiveIfOneOfRecentWasDropped();
        assert_10(getUnvalidatedDays(), lastId);
    }

    @Test
    public void t11() {
        long lastId = g_11_validateArchiveIfTooOldAndHasUndroppedRecent();
        assert_11(getUnvalidatedDays(), lastId);
    }

    @Test
    public void t12() {
        long lastId = g_12_doNotValidateWhatIsBeingValidatedRightNow();
        assert_12(getUnvalidatedDays(), lastId);
    }

    @Test
    public void t13() {
        long lastId = g_13_doValidateWhatIsNotBeingValidatedRightNow();
        assert_13(getUnvalidatedDays(), lastId);
    }

    @Test
    public void testMarkDropped() {
        long shouldBeMarked = insertStepEvent("30min", "2001-01-01T03:00:00", false, false);
        long validatedShouldNotBeMarked = insertStepEvent("30min", "2001-01-01T03:30:00", true, false);
        long markedShouldStayMarked = insertStepEvent("30min", "2001-01-01T04:00:00", false, true);

        SortedSet<String> tables = unvalidatedDaysHelper.getRecentTables(LOG_CONF);
        assertTrue(tables.size() > 0);
        String existing = tables.last();

        long existingShouldNotBeMarked = insertStepEvent("30min", existing, false, false);

        unvalidatedDaysHelper.markDropped(LOG_CONF);

        assertTrue(jdbcTemplate.query("select partition_dropped from step_events where event_id = :event_id",
                "event_id", shouldBeMarked, Boolean.class));
        assertFalse(jdbcTemplate.query("select partition_dropped from step_events where event_id = :event_id",
                "event_id", validatedShouldNotBeMarked, Boolean.class));
        assertTrue(jdbcTemplate.query("select partition_dropped from step_events where event_id = :event_id",
                "event_id", markedShouldStayMarked, Boolean.class));
        assertFalse(jdbcTemplate.query("select partition_dropped from step_events where event_id = :event_id",
                "event_id", existingShouldNotBeMarked, Boolean.class));
    }

    private void assertDay(Map<Integer, UnvalidatedDay> days, int day, long lastSeenId, UnvalidatedDay.Scale scale) {
        assertThat(days.get(day),
                is(new UnvalidatedDay(day, lastSeenId, scale)));
    }

    private Map<Integer, UnvalidatedDay> daysToMap(Set<UnvalidatedDay> dayset) {
        return dayset.stream().collect(Collectors.toMap(
                d -> d.getDay(),
                d -> d
        ));
    }

    public long insertStepEvent(String scale, String timestamp, boolean validated, boolean dropped) {
        return insertStepEvent(scale, timestamp, validated, dropped, null);
    }

    private long insertStepEvent(String scale, String timestamp, boolean validated, boolean dropped, String stepTimeCreated) {
        return insertStepEvent(ytConfig, jdbcTemplate, scale, timestamp, validated, dropped, stepTimeCreated);
    }

    public static long insertStepEvent(YtConfig ytConfig, LoggingJdbcTemplate jdbcTemplate,
                                       String scale, String timestamp, boolean validated, boolean dropped, String stepTimeCreated) {
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

    private long insertSession(int day) {
        return jdbcTemplate.query("insert into sessions " +
            "(day, scale, log, cluster, last_seen_event_id, host, status, seen_partitions) values " +
            "(" + day + ", 'ARCHIVE', '" + LOG_CONF.getLogName() + "', '" + ytConfig.getCluster() + "', 0, 'host1'," +
            "'" + SessionStatusEnum.PENDING + "'," +
            "'[]') returning session_id", Long.class);
    }
}
