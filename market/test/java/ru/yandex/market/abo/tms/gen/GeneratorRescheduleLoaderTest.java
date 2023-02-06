package ru.yandex.market.abo.tms.gen;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import ru.yandex.market.abo.core.dynamic.service.GeneratorTaskService;
import ru.yandex.market.abo.gen.model.GeneratorProfile;
import ru.yandex.market.monitoring.MonitoringUnit;
import ru.yandex.market.tms.quartz2.group.GroupManager;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 07.03.19
 */
class GeneratorRescheduleLoaderTest {
    private static final int GEN_ID = 0;
    private static final Date EXECUTION_DATE = DateUtils.addHours(new Date(), -1);
    private static final long MAX_ATTEMPTS_COUNT = GeneratorRescheduleLoader.TIMEOUT_BY_ATTEMPT.length;

    @InjectMocks
    GeneratorRescheduleLoader generatorRescheduleLoader;

    @Mock
    GeneratorTaskService generatorTaskService;
    @Mock
    JdbcTemplate pgRoJdbcTemplate;
    @Mock
    GroupManager generatorsGroupManager;
    @Mock
    ConfigurationService coreCounterService;
    @Mock
    ResultSet resultSet;
    @Mock
    MonitoringUnit generatorsRetryMonitoring;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        doAnswer(invocation -> {
            ((RowCallbackHandler) invocation.getArguments()[1]).processRow(resultSet);
            return null;
        }).when(pgRoJdbcTemplate).query(anyString(), any(RowCallbackHandler.class), any());
        when(resultSet.getInt("id")).thenReturn(GEN_ID);
        when(resultSet.getTimestamp("execution_date")).thenReturn(new Timestamp(EXECUTION_DATE.getTime()));
    }

    @Test
    void badCronExprTest() {
        when(generatorTaskService.getTasks(any())).thenReturn(createSingleGenerator(""));
        assertTrue(generatorRescheduleLoader.generatorsToRestart().isEmpty());
    }

    @Test
    void scheduleSoonTest() {
        when(generatorTaskService.getTasks(any())).thenReturn(createSingleGenerator("0 0/5 * * * ?"));
        assertTrue(generatorRescheduleLoader.generatorsToRestart().isEmpty());
    }

    @Test
    void toManyAttempts() {
        when(generatorTaskService.getTasks(any())).thenReturn(createSingleGenerator("0 0 * * * ? 2099"));
        when(coreCounterService.getValueAsLong(any())).thenReturn(MAX_ATTEMPTS_COUNT + 1);
        assertTrue(generatorRescheduleLoader.generatorsToRestart().isEmpty());
        verify(generatorsRetryMonitoring).critical(anyString());
    }

    @ParameterizedTest
    @CsvSource({"2, true", "3, false"})
    void lastExecutionWasRecently(long attempt, boolean needSchedule) throws Exception {
        Date now_minus45min = DateUtils.addMinutes(new Date(), -45);
        when(resultSet.getTimestamp("execution_date")).thenReturn(new Timestamp(now_minus45min.getTime()));
        when(coreCounterService.getValueAsLong(any())).thenReturn(attempt);
        when(generatorTaskService.getTasks(any())).thenReturn(createSingleGenerator("0 0 * * * ? 2099"));

        assertEquals(needSchedule, !generatorRescheduleLoader.generatorsToRestart().isEmpty());
    }

    private static List<GeneratorProfile> createSingleGenerator(String cronExpr) {
        GeneratorProfile task = new GeneratorProfile(GEN_ID, 0, cronExpr);
        return Collections.singletonList(task);
    }
}
