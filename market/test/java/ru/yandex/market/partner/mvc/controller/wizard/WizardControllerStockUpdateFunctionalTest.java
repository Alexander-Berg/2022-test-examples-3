package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.stocks.DebugStatus;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Функциональные тесты для шага wizard'a "Шаг автоматическое обновление данных об остатках".
 * См {@link ru.yandex.market.core.wizard.step.StockUpdateStepStatusCalculator}
 */
@DbUnitDataSet(before = "csv/commonBlueWizardData.before.csv")
class WizardControllerStockUpdateFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    private FF4ShopsClient ff4ShopsClient;

    @Autowired
    private LMSClient lmsClient;


    @Test
    @DisplayName("Не DROPSHIP. Ошибка, если шаг недоступен")
    void testNotDropship() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.STOCK_UPDATE)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("Принимает заказы через через партнерский интерфейс")
    void testDropshipCpaIsPartnerInterfaceTrue() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(DROPSHIP_WITH_CPA_PARTNER_INTERFACE_CAMPAIGN_ID,
                        WizardStepType.STOCK_UPDATE)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("Доступность crossdock")
    void testCrossdockAvailable() {
        final ResponseEntity<String> response = requestStep(CROSSDOCK_SUPPLIER_CAMPAIGN_ID,
                WizardStepType.STOCK_UPDATE);
        assertResponse(response, makeResponseStepStatus(Status.NONE));
    }

    @Test
    @DisplayName("Проверить, что нет ни одной записи partner_ff_service_link с типом dropship/supplier")
    void testStockUpdateNoLinks() {
        final ResponseEntity<String> response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.STOCK_UPDATE);
        assertResponse(response, makeResponseStepStatus(Status.NONE));
    }

    @Test
    @DisplayName("Ошибка при обращении к шагу, к которому нет доступа")
    void testFfSupplier() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.STOCK_UPDATE)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("Дропшип через ПИ")
    void testDropshipThroughPI() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(DROPSHIP_WITH_CPA_PARTNER_INTERFACE_CAMPAIGN_ID, WizardStepType.STOCK_UPDATE)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("Запись в partner_ff_service_link с типом dropship/supplier есть, запрос в Ff4shops с ошибкой")
    @DbUnitDataSet(before = "csv/testStockUpdateWithLinks.csv")
    void testStockUpdateNoData() {
        String testExceptionString = "FF4Shops exception";
        Mockito.when(ff4ShopsClient.getDebugStockStatus(Mockito.anyLong()))
                .thenThrow(new RuntimeException(testExceptionString));
        Mockito.when(lmsClient.getPartner(Mockito.eq(101L))).thenReturn(
                Optional.of(
                        PartnerResponse
                                .newBuilder()
                                .stockSyncEnabled(Boolean.FALSE)
                                .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                                .build()
                ));
        final ResponseEntity<String> response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.STOCK_UPDATE);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.STOCK_UPDATE)
                .withStatus(Status.INTERNAL_ERROR)
                .withDetails(ImmutableMap.of("error", testExceptionString))
                .build());
    }

    @Test
    @DisplayName("Проверить, что запись в partner_ff_service_link с типом dropship/supplier есть, NO_STOCKS ")
    @DbUnitDataSet(before = "csv/testStockUpdateWithLinks.csv")
    void testStockUpdateNoStocks() {
        Mockito.when(ff4ShopsClient.getDebugStockStatus(Mockito.anyLong())).thenReturn(DebugStatus.NO_STOCKS);
        Mockito.when(lmsClient.getPartner(Mockito.eq(101L))).thenReturn(
                Optional.of(
                        PartnerResponse
                                .newBuilder()
                                .stockSyncEnabled(Boolean.FALSE)
                                .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                                .build()
                ));
        final ResponseEntity<String> response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.STOCK_UPDATE);
        assertResponse(response, makeResponseStepStatus(Status.NONE));
    }

    @Test
    @DisplayName("Проверить, что запись в partner_ff_service_link с типом dropship/supplier есть, NO_REQUESTS ")
    @DbUnitDataSet(before = "csv/testStockUpdateWithLinks.csv")
    void testStockUpdateNoRequests() {
        Mockito.when(ff4ShopsClient.getDebugStockStatus(Mockito.anyLong())).thenReturn(DebugStatus.NO_REQUESTS);
        Mockito.when(lmsClient.getPartner(Mockito.eq(101L))).thenReturn(
                Optional.of(
                        PartnerResponse
                                .newBuilder()
                                .stockSyncEnabled(Boolean.FALSE)
                                .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                                .build()
                ));
        final ResponseEntity<String> response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.STOCK_UPDATE);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    @Test
    @DisplayName("ЛМС ответил ошибкой")
    @DbUnitDataSet(before = "csv/testStockUpdateWithLinks.csv")
    void testStockUpdateLmsError() {
        String testExceptionString = "lms exception";
        Mockito.when(ff4ShopsClient.getDebugStockStatus(Mockito.anyLong())).thenReturn(DebugStatus.SUCCESS);
        Mockito.when(lmsClient.getPartner(Mockito.eq(101L)))
                .thenThrow(new RuntimeException(testExceptionString));
        final ResponseEntity<String> response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.STOCK_UPDATE);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.STOCK_UPDATE)
                .withStatus(Status.INTERNAL_ERROR)
                .withDetails(ImmutableMap.of("error", testExceptionString))
                .build());
    }

    @Test
    @DisplayName("нет данных в ЛМС")
    @DbUnitDataSet(before = "csv/testStockUpdateWithLinks.csv")
    void testStockUpdateEmptyResponseLms() {
        Mockito.when(ff4ShopsClient.getDebugStockStatus(Mockito.anyLong())).thenReturn(DebugStatus.SUCCESS);
        Mockito.when(lmsClient.getPartner(Mockito.eq(101L))).thenReturn(Optional.empty());
        final ResponseEntity<String> response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.STOCK_UPDATE);
        assertResponse(response, makeResponseStepStatus(Status.NONE));
    }

    @Test
    @DisplayName("Статус EMPTY stockSyncEnabled = false И  stockSyncSwitchReason = NEW")
    @DbUnitDataSet(before = "csv/testStockUpdateWithLinks.csv")
    void testStockUpdateEmpty() {
        Mockito.when(ff4ShopsClient.getDebugStockStatus(Mockito.anyLong())).thenReturn(DebugStatus.SUCCESS);
        Mockito.when(lmsClient.getPartner(Mockito.eq(101L))).thenReturn(
                Optional.of(
                        PartnerResponse
                                .newBuilder()
                                .stockSyncEnabled(Boolean.FALSE)
                                .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                                .build()
                ));
        final ResponseEntity<String> response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.STOCK_UPDATE);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    @Test
    @DisplayName("Статус EMPTY stockSyncEnabled = false И  stockSyncSwitchReason != NEW")
    @DbUnitDataSet(before = "csv/testStockUpdateWithLinks.csv")
    void testStockUpdateFailed() {
        Mockito.when(ff4ShopsClient.getDebugStockStatus(Mockito.anyLong())).thenReturn(DebugStatus.SUCCESS);
        Mockito.when(lmsClient.getPartner(Mockito.eq(101L))).thenReturn(
                Optional.of(
                        PartnerResponse
                                .newBuilder()
                                .stockSyncEnabled(Boolean.FALSE)
                                .stockSyncSwitchReason(StockSyncSwitchReason.UNKNOWN)
                                .build()
                ));
        final ResponseEntity<String> response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.STOCK_UPDATE);
        assertResponse(response, makeResponseStepStatus(Status.FAILED));
    }

    @Test
    @DisplayName("Статус EMPTY stockSyncEnabled = true ff4shops -> NO_STOCKS")
    @DbUnitDataSet(before = "csv/testStockUpdateWithLinks.csv")
    void testStockUpdateFull() {
        Mockito.when(ff4ShopsClient.getDebugStockStatus(Mockito.anyLong())).thenReturn(DebugStatus.NO_STOCKS);
        Mockito.when(lmsClient.getPartner(Mockito.eq(101L))).thenReturn(
                Optional.of(
                        PartnerResponse
                                .newBuilder()
                                .stockSyncEnabled(Boolean.TRUE)
                                .build()
                ));
        final ResponseEntity<String> response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.STOCK_UPDATE);
        assertResponse(response, makeResponseStepStatus(Status.FULL));
    }

    private static WizardStepStatus makeResponseStepStatus(Status status) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.STOCK_UPDATE)
                .withStatus(status)
                .build();
    }
}
