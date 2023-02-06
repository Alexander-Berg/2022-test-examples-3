package ru.yandex.market.pers.qa.tms.stat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.saas.IndexingMode;
import ru.yandex.market.pers.qa.model.saas.SaasIndexingState;
import ru.yandex.market.pers.qa.model.saas.UploadMethod;
import ru.yandex.market.util.db.ConfigurationService;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author vvolokh
 * 09.01.2019
 */
public class QaIndexingStatsServiceTest extends PersQaTmsTest {
    private static final double EPS = 0.1;
    @Autowired
    private QaIndexingStatsService qaIndexingStatsService;

    @Autowired
    private ComplexMonitoring complicatedMonitoring;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testEmptyDatabase() {
        qaIndexingStatsService = spy(qaIndexingStatsService);
        qaIndexingStatsService.logEntitiesIndexingTimeForLastHour(QaEntityType.QUESTION);
        verify(qaIndexingStatsService, times(7)).writeStatValue(any(), any(String.class), any(String.class), eq(0.0));
    }

    @Test
    public void testStats() {
        qaIndexingStatsService = spy(qaIndexingStatsService);

        //included in calculations
        for(int i=1; i<21; i++) {
            insertSaasIndexingTimeEntry(QaEntityType.QUESTION, i, "20190101_0000", nowMinusHours(i), Instant.now());
        }

        //excluded from calculations
        insertSaasIndexingTimeEntry(QaEntityType.QUESTION, -1, "20190101_0000", nowMinusHours(4), nowMinusHours(2));
        insertSaasIndexingTimeEntry(QaEntityType.QUESTION, -2, "20190101_0000", nowMinusHours(5), Instant.now().minus(1, ChronoUnit.HOURS).minus(1, ChronoUnit.MINUTES));


        qaIndexingStatsService.logEntitiesIndexingTimeForLastHour(QaEntityType.QUESTION);

        verifyQuestionStat("indexing-max-time", 1200, qaIndexingStatsService);
        verifyQuestionStat("indexing-min-time", 60, qaIndexingStatsService);
        verifyQuestionStat("indexing-avg-time", 630, qaIndexingStatsService);
        verifyQuestionStat("indexing-99-time", 1200, qaIndexingStatsService);
        verifyQuestionStat("indexing-95-time", 1140, qaIndexingStatsService);
        verifyQuestionStat("indexing-90-time", 1080, qaIndexingStatsService);
        verifyQuestionStat("indexing-count", 20, qaIndexingStatsService);
    }

    @Test
    public void testGenerationStats() {
        qaIndexingStatsService = spy(qaIndexingStatsService);

        insertSaasIndexingHistoryEntry(SaasIndexingState.COMPLETED, nowMinusHours(5), nowMinusHours(3), IndexingMode.DIFF, UploadMethod.LOGBROKER);
        insertSaasIndexingHistoryEntry(SaasIndexingState.PREPARATION, nowMinusHours(2), nowMinusHours(1), IndexingMode.DIFF, UploadMethod.LOGBROKER);
        insertSaasIndexingHistoryEntry(SaasIndexingState.PREPARATION, nowMinusHours(10), nowMinusMinutes(1), IndexingMode.REFRESH, UploadMethod.LOGBROKER);

        qaIndexingStatsService.logIndexAgeForLogbroker();

        verifyStat("indexing-cur-generation-age", UploadMethod.LOGBROKER.name() + "-" + IndexingMode.DIFF.name(), 180.0, qaIndexingStatsService);
        verifyStat("indexing-generation-life-time", UploadMethod.LOGBROKER.name() + "-" + IndexingMode.DIFF.name(), 120.0, qaIndexingStatsService);
        verifyStat("indexing-generation-life-time", UploadMethod.LOGBROKER.name() + "-" + IndexingMode.REFRESH.name(), 600.0, qaIndexingStatsService);
    }

    @Test
    public void testMonitoringLogbrokerDiffGenerationLifeTime() throws Exception {
        qaIndexingStatsService = spy(qaIndexingStatsService);
        configurationService.tryGetOrMergeVal("monitor.index.gen_life_time.logbroker_diff", Double.class, 5.0);

        //test critical
        insertSaasIndexingHistoryEntry(SaasIndexingState.PREPARATION, nowMinusMinutes(10), nowMinusMinutes(9), IndexingMode.DIFF, UploadMethod.LOGBROKER);

        qaIndexingStatsService.logIndexAgeForLogbroker();

        verifyStat("indexing-generation-life-time", UploadMethod.LOGBROKER.name() + "-" + IndexingMode.DIFF.name(), 10.0, qaIndexingStatsService);
        assertEquals(MonitoringStatus.CRITICAL, complicatedMonitoring.getResult().getStatus());
        assertEquals(MonitoringStatus.CRITICAL, complicatedMonitoring.getResult("monitor-indexing-generation-life-time-LOGBROKER-DIFF").getStatus());

        //test ok
        resetMonitoring();
        Mockito.reset(qaIndexingStatsService);
        insertSaasIndexingHistoryEntry(SaasIndexingState.PREPARATION, nowMinusMinutes(2), nowMinusMinutes(1), IndexingMode.DIFF, UploadMethod.LOGBROKER);

        qaIndexingStatsService.logIndexAgeForLogbroker();

        verifyStat("indexing-generation-life-time", UploadMethod.LOGBROKER.name() + "-" + IndexingMode.DIFF.name(), 2.0, qaIndexingStatsService);
        assertEquals(MonitoringStatus.OK, complicatedMonitoring.getResult().getStatus());
    }

    @Test
    public void testMonitoringQuestionAvgIndexTime() throws Exception {
        qaIndexingStatsService = spy(qaIndexingStatsService);
        configurationService.tryGetOrMergeVal("monitor.index.avg-time.question", Double.class, 80.0);

        //test critical
        insertSaasIndexingTimeEntry(QaEntityType.QUESTION, 1, "20190101_0000", nowMinusHours(1), Instant.now());
        insertSaasIndexingTimeEntry(QaEntityType.QUESTION, 2, "20190101_0000", nowMinusHours(2), Instant.now());

        qaIndexingStatsService.logEntitiesIndexingTimeForLastHour(QaEntityType.QUESTION);

        verifyQuestionStat("indexing-avg-time", 90, qaIndexingStatsService);
        assertEquals(MonitoringStatus.CRITICAL, complicatedMonitoring.getResult().getStatus());
        assertEquals(MonitoringStatus.CRITICAL, complicatedMonitoring.getResult("monitor-indexing-avg-time-question").getStatus());

        //test ok
        resetMonitoring();
        Mockito.reset(qaIndexingStatsService);
        insertSaasIndexingTimeEntry(QaEntityType.QUESTION, 3, "20190101_0000", nowMinusMinutes(3), Instant.now());

        qaIndexingStatsService.logEntitiesIndexingTimeForLastHour(QaEntityType.QUESTION);

        verifyQuestionStat("indexing-avg-time", 31, qaIndexingStatsService);
        assertEquals(MonitoringStatus.OK, complicatedMonitoring.getResult().getStatus());
    }

    @Test
    public void testMonitoringAnswerAvgIndexTime() throws Exception {
        qaIndexingStatsService = spy(qaIndexingStatsService);
        configurationService.tryGetOrMergeVal("monitor.index.avg-time.answer", Double.class, 80.0);

        //test critical
        insertSaasIndexingTimeEntry(QaEntityType.ANSWER, 1, "20190101_0000", nowMinusHours(1), Instant.now());
        insertSaasIndexingTimeEntry(QaEntityType.ANSWER, 2, "20190101_0000", nowMinusHours(2), Instant.now());

        qaIndexingStatsService.logEntitiesIndexingTimeForLastHour(QaEntityType.ANSWER);

        verifyAnswerStat("indexing-avg-time", 90, qaIndexingStatsService);
        assertEquals(MonitoringStatus.CRITICAL, complicatedMonitoring.getResult().getStatus());
        assertEquals(MonitoringStatus.CRITICAL, complicatedMonitoring.getResult("monitor-indexing-avg-time-answer").getStatus());

        //test ok
        resetMonitoring();
        Mockito.reset(qaIndexingStatsService);
        insertSaasIndexingTimeEntry(QaEntityType.ANSWER, 3, "20190101_0000", nowMinusMinutes(3), Instant.now());

        qaIndexingStatsService.logEntitiesIndexingTimeForLastHour(QaEntityType.ANSWER);

        verifyAnswerStat("indexing-avg-time", 31, qaIndexingStatsService);
        assertEquals(MonitoringStatus.OK, complicatedMonitoring.getResult().getStatus());
    }

    private Instant nowMinusMinutes(int minutes) {
        return Instant.now().minus(minutes, ChronoUnit.MINUTES);
    }

    private Instant nowMinusHours(int hours) {
        return Instant.now().minus(hours, ChronoUnit.HOURS);
    }

    private void verifyQuestionStat(String name, double value, QaIndexingStatsService spy) {
        verify(spy).writeStatValue(any(), eq(name), eq(QaEntityType.QUESTION.getSimpleName()), doubleThat(argument -> value - argument < EPS));
    }

    private void verifyAnswerStat(String name, double value, QaIndexingStatsService spy) {
        verify(spy).writeStatValue(any(), eq(name), eq(QaEntityType.ANSWER.getSimpleName()), doubleThat(argument -> value - argument < EPS));
    }

    private void verifyStat(String name, String subkey, double value, QaIndexingStatsService spy) {
        verify(spy).writeStatValue(any(), eq(name), eq(subkey), doubleThat(argument -> value - argument < EPS));
    }

    private int insertSaasIndexingTimeEntry(QaEntityType entityType, int entityId, String generationId, Instant crTime, Instant indexTime) {
        return jdbcTemplate.update(
            "INSERT INTO qa.saas_indexing_time (entity_type, entity_id, generation_id, cr_time, index_time) VALUES (?,?,?,?,?) ",
            entityType.getValue(), entityId, generationId, new Timestamp(crTime.toEpochMilli()), new Timestamp(indexTime.toEpochMilli()));
    }

    private void insertSaasIndexingHistoryEntry(SaasIndexingState indexingState, Instant crTime, Instant updTime, IndexingMode indexingMode, UploadMethod uploadMethod) {
        jdbcTemplate.update(
            "INSERT INTO qa.saas_indexing_history (generation_id, state, cr_time, upd_time, diff_fl, upload_method) \n" +
                "VALUES (?, ?, ?, ?, ?, ?)",
            UUID.randomUUID().toString(), indexingState.value(), new Timestamp(crTime.toEpochMilli()), new Timestamp(updTime.toEpochMilli()), indexingMode.getValue(), uploadMethod.getValue());
    }
}
