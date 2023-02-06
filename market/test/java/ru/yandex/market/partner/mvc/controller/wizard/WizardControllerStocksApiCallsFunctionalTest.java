package ru.yandex.market.partner.mvc.controller.wizard;

import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.cpa.log.model.LogStat;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.LogisticPartnerService;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.mbi.logprocessor.client.model.PushApiLogStatsResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * Функциональные тесты для шага wizard'a "Шаг настройки АПИ. Обработка запросов об остатках".
 * См {@link ru.yandex.market.core.wizard.step.StocksApiCallsStepStatusCalculator}
 */
@DbUnitDataSet(before = {"csv/commonBlueWizardData.before.csv", "csv/pushapilogsBasedTest.before.csv"})
class WizardControllerStocksApiCallsFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    private LogisticPartnerService logisticPartnerService;

    @Test
    @DisplayName("Не DROPSHIP. Ошибка, если шаг недоступен")
    void testNotDropship() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.STOCKS)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("Принимает заказы через через партнерский интерфейс")
    void testDropshipCpaIsPartnerInterfaceTrue() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(DROPSHIP_WITH_CPA_PARTNER_INTERFACE_CAMPAIGN_ID,
                        WizardStepType.STOCKS)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("Шаг не доступен для click and collect")
    void testClickAndCollectNotAvailable() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(DS_CLICK_AND_COLLECT_CAMPAIGN_ID,
                        WizardStepType.STOCKS)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("Доступность crossdock")
    void testCrossdockAvailable() {
        var response = requestStep(CROSSDOCK_SUPPLIER_CAMPAIGN_ID, WizardStepType.STOCKS);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.STOCKS)
                .withStatus(Status.NONE)
                .build());
    }

    @Test
    @DisplayName("Статус FULL. Сто процентов успешных запросов.")
    void testStatusFull() {
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        PushApiLogStatsResponse statsResponse = new PushApiLogStatsResponse().count(2L).errorCount(0L).successCount(2L);
        Mockito.when(logProcessorClient.getLogStats(eq(2202L), any(OffsetDateTime.class), eq("/stocks")))
                .thenReturn(statsResponse);

        var response = requestStep(MORE_THAN_100_PUSHAPI_SUCCESS_LOG_RECORD_CAMPAIGN_ID,
                WizardStepType.STOCKS);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.STOCKS)
                .withStatus(Status.FULL)
                .withDetails(Map.of("logStat", LogStat.newBuilder()
                        .withSuccessCount(2L)
                        .withErrorCount(0L)
                        .withCount(2L)
                        .build()))
                .build(), JSONCompareMode.LENIENT);
    }


    @Test
    @DisplayName("Статус EMPTY. Нет ни одного запроса.")
    void testStatusEmpty() {
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        PushApiLogStatsResponse statsResponse = new PushApiLogStatsResponse().count(0L).errorCount(0L).successCount(0L);
        Mockito.when(logProcessorClient.getLogStats(eq(2200L), any(OffsetDateTime.class), eq("/stocks")))
                .thenReturn(statsResponse);

        var response = requestStep(NO_PUSHAPI_LOGS_LOGS_CAMPAIGN_ID,
                WizardStepType.STOCKS);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.STOCKS)
                .withStatus(Status.EMPTY)
                .withDetails(Map.of("logStat", LogStat.newBuilder()
                        .withSuccessCount(0L)
                        .withErrorCount(0L)
                        .withCount(0L)
                        .build()))
                .build(), JSONCompareMode.LENIENT);
    }

    @Test
    @DisplayName("Статус RESTRICED. Менее ста процентов успешных запросов.")
    void testStatusRestricted() {
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        PushApiLogStatsResponse statsResponse = new PushApiLogStatsResponse().count(2L).errorCount(1L).successCount(1L);
        Mockito.when(logProcessorClient.getLogStats(eq(2100L), any(OffsetDateTime.class), eq("/stocks")))
                .thenReturn(statsResponse);

        var response = requestStep(DS_SUPPLIER_CAMPAIGN_ID,
                WizardStepType.STOCKS);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.STOCKS)
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
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        PushApiLogStatsResponse statsResponse = new PushApiLogStatsResponse().count(1L).errorCount(1L).successCount(0L);
        Mockito.when(logProcessorClient.getLogStats(eq(2201L), any(OffsetDateTime.class), eq("/stocks")))
                .thenReturn(statsResponse);

        var response = requestStep(NO_PUSHAPI_SUCCESS_LOGS_CAMPAIGN_ID,
                WizardStepType.STOCKS);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.STOCKS)
                .withStatus(Status.FAILED)
                .withDetails(Map.of("logStat", LogStat.newBuilder()
                        .withSuccessCount(0L)
                        .withErrorCount(1L)
                        .withCount(1L)
                        .build()))
                .build(), JSONCompareMode.LENIENT);
    }

    @Test
    @DisplayName("Статус NONE. CPA_API_PARAMS_READY=false.")
    void testStatusNone() {
        var response = requestStep(DROPSHIP_SUPPLIER_CAMPAIGN_ID,
                WizardStepType.STOCKS);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.STOCKS)
                .withStatus(Status.NONE)
                .build(), JSONCompareMode.LENIENT);
    }
}
