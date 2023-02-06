package ru.yandex.market.mbi.health.service;

import java.time.OffsetDateTime;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import ru.yandex.market.mbi.health.service.solomon.AlertConfig;
import ru.yandex.market.mbi.health.service.solomon.CachedSolomonClient;
import ru.yandex.market.mbi.health.service.solomon.ProjectAlertConfig;
import ru.yandex.market.mbi.health.service.solomon.SolomonAlertState;
import ru.yandex.market.mbi.health.service.solomon.SolomonAlertStateStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static ru.yandex.market.mbi.health.service.AlertHealthStatusCalculator.STATUS_ALARM;
import static ru.yandex.market.mbi.health.service.AlertHealthStatusCalculator.STATUS_OK;
import static ru.yandex.market.mbi.health.service.AlertHealthStatusCalculator.STATUS_WARN;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AlertHealthStatusCalculatorTest {

    private static final String TEST_DASH_URL = "https://test-solomon.yandex-team.ru/";

    private static final List<ProjectAlertConfig> projects = List.of(
            new ProjectAlertConfig(
                    "Dashboard A",
                    TEST_DASH_URL + "?project=test&dashboard=a",
                    List.of(
                            new AlertConfig("a", "alertA1", 1.0),
                            new AlertConfig("b", "alertB1", 0.6),
                            new AlertConfig("b", "alertB2", 0.5),
                            new AlertConfig("b", "alertB3", 0.3)
                    )
            )
    );

    private AlertHealthStatusCalculator calculator;
    private CachedSolomonClient solomonClient;


    @BeforeAll
    public void init() {
        this.solomonClient = Mockito.mock(CachedSolomonClient.class);
        this.calculator = new AlertHealthStatusCalculator(solomonClient);
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(solomonClient);
    }

    @Test
    public void shouldHaveOkStatusIfAllAlertsOk() {
        Mockito.when(solomonClient.getAlert(eq("a"), eq("alertA1")))
                .thenReturn(createAlertState("a", "alertA1", STATUS_OK));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB1")))
                .thenReturn(createAlertState("b", "alertB1", STATUS_OK));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB2")))
                .thenReturn(createAlertState("b", "alertB2", STATUS_OK));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB3")))
                .thenReturn(createAlertState("b", "alertB3", STATUS_OK));

        String actual = calculator.getHealthStatus(projects).get(0).getProjectStatus();

        assertEquals(STATUS_OK, actual);
    }


    @Test
    public void shouldHaveAlarmStatusIfOneAlertHasAlarmState() {
        Mockito.when(solomonClient.getAlert(eq("a"), eq("alertA1")))
                .thenReturn(createAlertState("a", "alertA1", STATUS_ALARM));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB1")))
                .thenReturn(createAlertState("b", "alertB1", STATUS_OK));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB2")))
                .thenReturn(createAlertState("b", "alertB2", STATUS_OK));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB3")))
                .thenReturn(createAlertState("b", "alertB3", STATUS_OK));

        String actual = calculator.getHealthStatus(projects).get(0).getProjectStatus();

        assertEquals(STATUS_ALARM, actual);
    }

    @Test
    public void shouldHaveOkStatusIfNotReachedWarnThreshold() {
        Mockito.when(solomonClient.getAlert(eq("a"), eq("alertA1")))
                .thenReturn(createAlertState("a", "alertA1", STATUS_OK));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB1")))
                .thenReturn(createAlertState("b", "alertB1", STATUS_WARN));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB2")))
                .thenReturn(createAlertState("b", "alertB2", STATUS_OK));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB3")))
                .thenReturn(createAlertState("b", "alertB3", STATUS_OK));

        String actual = calculator.getHealthStatus(projects).get(0).getProjectStatus();

        assertEquals(STATUS_OK, actual);
    }

    @Test
    public void shouldHaveWarnStatusIfReachedWarnThreshold() {
        Mockito.when(solomonClient.getAlert(eq("a"), eq("alertA1")))
                .thenReturn(createAlertState("a", "alertA1", STATUS_OK));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB1")))
                .thenReturn(createAlertState("b", "alertB1", STATUS_WARN));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB2")))
                .thenReturn(createAlertState("b", "alertB2", STATUS_WARN));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB3")))
                .thenReturn(createAlertState("b", "alertB3", STATUS_OK));

        String actual = calculator.getHealthStatus(projects).get(0).getProjectStatus();

        assertEquals(STATUS_WARN, actual);
    }

    @Test
    public void shouldHaveAlarmStatusIfReachedAlarmThreshold() {
        Mockito.when(solomonClient.getAlert(eq("a"), eq("alertA1")))
                .thenReturn(createAlertState("a", "alertA1", STATUS_WARN));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB1")))
                .thenReturn(createAlertState("b", "alertB1", STATUS_ALARM));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB2")))
                .thenReturn(createAlertState("b", "alertB2", STATUS_ALARM));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB3")))
                .thenReturn(createAlertState("b", "alertB3", STATUS_WARN));

        String actual = calculator.getHealthStatus(projects).get(0).getProjectStatus();

        assertEquals(STATUS_ALARM, actual);
    }


    @Test
    public void shouldAcceptNullValues() {
        Mockito.when(solomonClient.getAlert(eq("a"), eq("alertA1")))
                .thenReturn(createAlertState("a", "alertA1", STATUS_WARN));
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB1"))).thenReturn(null);
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB2"))).thenReturn(null);
        Mockito.when(solomonClient.getAlert(eq("b"), eq("alertB3"))).thenReturn(null);

        String actual = calculator.getHealthStatus(projects).get(0).getProjectStatus();

        assertEquals(STATUS_WARN, actual);
    }

    private SolomonAlertState createAlertState(String projectId, String alertId, String status) {
        return new SolomonAlertState(alertId, projectId, OffsetDateTime.now(), OffsetDateTime.now(),
                new SolomonAlertStateStatus(status));
    }

    private SolomonAlertState createAlertStateWithTime(String projectId, String alertId, String status,
                                                       OffsetDateTime since, OffsetDateTime latestEval) {
        return new SolomonAlertState(alertId, projectId, since, latestEval, new SolomonAlertStateStatus(status));
    }
}
