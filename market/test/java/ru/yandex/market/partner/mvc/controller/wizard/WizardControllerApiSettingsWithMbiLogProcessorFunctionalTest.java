package ru.yandex.market.partner.mvc.controller.wizard;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.api.cpa.log.model.LogStat;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.logprocessor.client.MbiLogProcessorClient;
import ru.yandex.market.mbi.logprocessor.client.model.PushApiLogStatsResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Функциональные тесты для шага wizard'a "Шаг проверки настройки АПИ".
 * См {@link ru.yandex.market.core.wizard.step.ApiSettingsStepStatusCalculator}
 * В тестах включено получение push-api логов из компонента mbi-log-processor
 */
@DbUnitDataSet(before = {"csv/commonBlueWizardData.before.csv", "csv/pushapilogsBasedTest.before.csv"})
class WizardControllerApiSettingsWithMbiLogProcessorFunctionalTest extends AbstractWizardControllerFunctionalTest {
    @Autowired
    private MbiLogProcessorClient logProcessorClient;
    @Autowired
    private TestableClock clock;
    @Autowired
    @Qualifier("environmentService")
    private EnvironmentService environmentService;

    @BeforeEach
    void setup() {
        environmentService.setValue("mbi-log-processor.enabled", "false");
    }

    @Test
    @DisplayName("Не DROPSHIP. Ошибка, если шаг недоступен")
    void testNotDropship() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.API_SETTINGS)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("CROSSDOCK. Ошибка, если шаг недоступен")
    void testNotAvailableIfCrossdock() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(CROSSDOCK_SUPPLIER_CAMPAIGN_ID, WizardStepType.API_SETTINGS)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("Принимает заказы через через партнерский интерфейс")
    void testDropshipCpaIsPartnerInterfaceTrue() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(DROPSHIP_WITH_CPA_PARTNER_INTERFACE_CAMPAIGN_ID,
                        WizardStepType.API_SETTINGS));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("Шаг доступен для click and collect")
    void testClickAndCollectSupport() {
        final ResponseEntity<String> response = requestStep(DS_CLICK_AND_COLLECT_CAMPAIGN_ID,
                WizardStepType.API_SETTINGS);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.API_SETTINGS)
                .withStatus(Status.EMPTY)
                .build(), JSONCompareMode.LENIENT);
    }

    @Test
    @DisplayName("Статус FULL. Сто процентов успешных запросов.")
    void testStatusFull() {
        OffsetDateTime today = LocalDate.of(2020, 5, 27).atStartOfDay()
                .atZone(ZoneId.systemDefault()).toOffsetDateTime();

        clock.setFixed(today.toInstant(), ZoneId.systemDefault());
        PushApiLogStatsResponse statsResponse = new PushApiLogStatsResponse().count(3L).errorCount(0L).successCount(3L);
        Mockito.when(logProcessorClient.getLogStats(2202, today, null))
                .thenReturn(statsResponse);
        final ResponseEntity<String> response = requestStep(MORE_THAN_100_PUSHAPI_SUCCESS_LOG_RECORD_CAMPAIGN_ID,
                WizardStepType.API_SETTINGS);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.API_SETTINGS)
                .withStatus(Status.FULL)
                .withDetails(Map.of("logStat", LogStat.newBuilder()
                        .withSuccessCount(3L)
                        .withErrorCount(0L)
                        .withCount(3L)
                        .build()))
                .build(), JSONCompareMode.LENIENT);
    }

    @Test
    @DisplayName("Статус RESTRICTED. Юридические данные не заапрувлены АБО.")
    void testStatusRestricted_prepayRequestNotCompleted() {
        final ResponseEntity<String> response = requestStep(NOT_APPROVED_PREPAY_REQUEST_CAMPAIGN_ID,
                WizardStepType.API_SETTINGS);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.API_SETTINGS)
                .withStatus(Status.NONE)
                .build(), JSONCompareMode.LENIENT);
    }

    @Test
    @DisplayName("Статус RESTRICTED. Менее ста процентов успешных запросов.")
    void testStatusRestricted() {
        OffsetDateTime today = LocalDate.of(2020, 5, 27).atStartOfDay()
                .atZone(ZoneId.systemDefault()).toOffsetDateTime();

        clock.setFixed(today.toInstant(), ZoneId.systemDefault());
        PushApiLogStatsResponse statsResponse = new PushApiLogStatsResponse().count(2L).errorCount(1L).successCount(1L);
        Mockito.when(logProcessorClient.getLogStats(2100, today, null))
                .thenReturn(statsResponse);
        final ResponseEntity<String> response = requestStep(DS_SUPPLIER_CAMPAIGN_ID,
                WizardStepType.API_SETTINGS);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.API_SETTINGS)
                .withStatus(Status.RESTRICTED)
                .withDetails(Map.of("logStat", LogStat.newBuilder()
                        .withSuccessCount(1L)
                        .withErrorCount(1L)
                        .withCount(2L)
                        .build()))
                .build(), JSONCompareMode.LENIENT);
    }

    @Test
    @DisplayName("Статус FAILED. Нет успешных запросов.")
    void testStatusFail() {
        OffsetDateTime today = LocalDate.of(2020, 5, 27).atStartOfDay()
                .atZone(ZoneId.systemDefault()).toOffsetDateTime();

        clock.setFixed(today.toInstant(), ZoneId.systemDefault());
        PushApiLogStatsResponse statsResponse = new PushApiLogStatsResponse().count(1L).errorCount(1L).successCount(0L);
        Mockito.when(logProcessorClient.getLogStats(2201, today, null))
                .thenReturn(statsResponse);
        final ResponseEntity<String> response = requestStep(NO_PUSHAPI_SUCCESS_LOGS_CAMPAIGN_ID,
                WizardStepType.API_SETTINGS);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.API_SETTINGS)
                .withStatus(Status.FAILED)
                .withDetails(Map.of("logStat", LogStat.newBuilder()
                        .withSuccessCount(0L)
                        .withErrorCount(1L)
                        .withCount(1L)
                        .build()))
                .build(), JSONCompareMode.LENIENT);
    }


    @Test
    @DisplayName("Статус FILLED. Нет ни одного запроса.")
    void testStatusEmpty() {
        OffsetDateTime today = LocalDate.of(2020, 5, 27).atStartOfDay()
                .atZone(ZoneId.systemDefault()).toOffsetDateTime();

        clock.setFixed(today.toInstant(), ZoneId.systemDefault());
        PushApiLogStatsResponse statsResponse = new PushApiLogStatsResponse().count(0L).errorCount(0L).successCount(0L);
        Mockito.when(logProcessorClient.getLogStats(2200, today, null))
                .thenReturn(statsResponse);
        final ResponseEntity<String> response = requestStep(NO_PUSHAPI_LOGS_LOGS_CAMPAIGN_ID,
                WizardStepType.API_SETTINGS);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.API_SETTINGS)
                .withStatus(Status.FILLED)
                .withDetails(Map.of("logStat", LogStat.newBuilder()
                        .withSuccessCount(0L)
                        .withErrorCount(0L)
                        .withCount(0L)
                        .build()))
                .build(), JSONCompareMode.LENIENT);
    }

    @AfterEach
    void tearDown() {
        clock.clearFixed();
    }
}
