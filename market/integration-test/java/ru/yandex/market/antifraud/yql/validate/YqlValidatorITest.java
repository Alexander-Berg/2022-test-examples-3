package ru.yandex.market.antifraud.yql.validate;

import com.google.common.collect.ImmutableSet;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.model.SessionStatusEnum;
import ru.yandex.market.antifraud.util.IntDateUtil;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YqlSession;
import ru.yandex.market.antifraud.yql.model.YtConfig;
import ru.yandex.market.antifraud.yql.yt.YtTablesHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.antifraud.yql.validate.UnvalidatedDaysHelperITest.insertStepEvent;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YqlValidatorITest {

    @Autowired
    private YqlValidator limitedShowsValidator;

    @Autowired
    private YqlValidator firstFilterShowsValidator;

    @Autowired
    private YqlValidatorHelper yqlValidatorHelper;

    @Autowired
    private YtTablesHelper ytTablesHelper;

    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Autowired
    private YtTestDataGenerator testDataGenerator;

    @Autowired
    private YtConfig ytConfig;

    @Autowired
    private YqlSessionToTxMap sessionToTxMap;

    @Autowired
    private UnvalidatedDaysHelper unvalidatedDaysHelper;

    private UnvalidatedDay day;

    @Before
    @SneakyThrows
    public void checkTestData() {
        testDataGenerator.initOnce();
        day = new UnvalidatedDay(
            testDataGenerator.getArchiveDay(), 1, UnvalidatedDay.Scale.ARCHIVE);
        jdbcTemplate.exec("truncate step_events");
        jdbcTemplate.exec("truncate sessions");
        Field sessionToTxField = sessionToTxMap.getClass().getDeclaredField("sessionToTx");
        sessionToTxField.setAccessible(true);
        sessionToTxField.set(sessionToTxMap, new HashMap<>());
    }

    @Test
    public void testArchiveValidation() {
        YqlSession session = limitedShowsValidator.validate(day);
        assertSessionStatus(session.getId(), SessionStatusEnum.FILTERS_EXECUTED);
        assertTrue(ytTablesHelper.exists(session.getFinalRollbacksFile()));
    }

    @Test
    public void testSkipValidatedFilters() {
        YqlSession oldSession = yqlValidatorHelper.createSession(day);
        yqlValidatorHelper.addSuccessfulFilter(oldSession, 1000);
        YqlSession session = limitedShowsValidator.validate(day);
        assertThat(session.getId(), is(oldSession.getId()));
        assertSuccessfulFilters(session,1000, 1002);
        assertSessionStatus(session.getId(), SessionStatusEnum.FILTERS_EXECUTED);
        assertTrue(ytTablesHelper.exists(session.getFinalRollbacksFile()));
    }

    @Test
    public void testValidationHaltAndContinue() {
        YqlSession firstFilterReady = firstFilterShowsValidator.validate(day);
        assertSessionStatus(firstFilterReady.getId(), SessionStatusEnum.FILTERS_EXECUTED);
        assertSuccessfulFilters(firstFilterReady,1000);

        // кабуто сессия была прервана после первого фильтра
        ytTablesHelper.remove(firstFilterReady.getFinalRollbacksFile());
        jdbcTemplate.update("update sessions set status = :pending where session_id = :id",
            "pending", SessionStatusEnum.PENDING,
            "id", firstFilterReady.getId());

        YqlSession session = limitedShowsValidator.validate(day);
        assertThat(session.getId(), is(firstFilterReady.getId()));
        assertSuccessfulFilters(session,1000, 1002);
        assertSessionStatus(session.getId(), SessionStatusEnum.FILTERS_EXECUTED);
        assertTrue(ytTablesHelper.exists(session.getFinalRollbacksFile()));
    }

    @Test
    public void testValidationFailesSessionOnScaleChange() {
        jdbcTemplate.exec("truncate step_events");
        long sessionId = jdbcTemplate.query("insert into sessions " +
                "(day, scale, log, cluster, last_seen_event_id, host, status, seen_partitions) values " +
                "(" + testDataGenerator.getArchiveDay() + ", '" + UnvalidatedDay.Scale.RECENT + "', " +
                "'" + testDataGenerator.log().getLogName() + "', '" + ytConfig.getCluster() + "', 0, 'host1'," +
                "'" + SessionStatusEnum.PENDING + "'," +
                "'[\"" + IntDateUtil.hyphenated(testDataGenerator.getArchiveDay()) + "T00:00:00\"]') returning session_id",
            Long.class);

        firstFilterShowsValidator.validate(new UnvalidatedDay(testDataGenerator.getArchiveDay(),
            Long.MAX_VALUE, UnvalidatedDay.Scale.ARCHIVE));

        assertEquals(SessionStatusEnum.FAILED.toString(),
            jdbcTemplate.query("select status from sessions where session_id = " + sessionId, String.class));
    }

    @Test
    public void testDaysSort() {
        insertStepEvent(ytConfig, jdbcTemplate, "30min", "2018-10-24T00:00:00", false, true, null);
        insertStepEvent(ytConfig, jdbcTemplate, "1d", "2018-10-24T00:00:00", false, false, "1998-11-24 00:00:00");

        insertStepEvent(ytConfig, jdbcTemplate, "30min", "2018-10-21T00:00:00", false, false, null);
        insertStepEvent(ytConfig, jdbcTemplate, "30min", "2018-10-21T00:30:00", false, false, null);
        insertStepEvent(ytConfig, jdbcTemplate, "30min", "2018-10-21T01:00:00", false, false, null);

        insertStepEvent(ytConfig, jdbcTemplate, "30min", "2018-10-22T00:00:00", false, true, null);
        insertStepEvent(ytConfig, jdbcTemplate, "1d", "2018-10-22T00:00:00", false, false, "1998-11-23 00:00:00");

        insertStepEvent(ytConfig, jdbcTemplate, "30min", "2018-10-23T00:00:00", false, false, null);
        insertStepEvent(ytConfig, jdbcTemplate, "30min", "2018-10-23T00:30:00", false, false, null);

        List<Integer> days = limitedShowsValidator
            .sortDays(new ArrayList<>(unvalidatedDaysHelper.getUnvalidatedDays(testDataGenerator.log()))).stream()
            .map(d -> d.getDay().getDay())
            .collect(Collectors.toList());

        assertEquals(days, Arrays.asList(20181024, 20181022, 20181023, 20181021));
    }


    private void assertSessionStatus(long id, SessionStatusEnum expected) {
        assertThat(
            jdbcTemplate.query("select status from sessions where session_id = :session_id",
                "session_id", id,
                String.class),
            is(expected.toString()));
    }

    private void assertSuccessfulFilters(YqlSession session, Integer... filterIds) {
        assertThat(yqlValidatorHelper.getSuccessfulFilters(session), is(ImmutableSet.copyOf(filterIds)));
    }

}
