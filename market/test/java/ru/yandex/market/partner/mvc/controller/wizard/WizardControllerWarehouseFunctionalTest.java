package ru.yandex.market.partner.mvc.controller.wizard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.LogisticPartnerService;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Функциональные тесты для шага wizard'a "Шаг настройка склада".
 * См {@link ru.yandex.market.core.wizard.step.WarehouseStepStatusCalculator}
 */
@DbUnitDataSet(before = "csv/commonBlueWizardData.before.csv")
class WizardControllerWarehouseFunctionalTest extends AbstractWizardControllerFunctionalTest {
    @Autowired
    LogisticPartnerService logisticPartnerService;

    @Test
    @DisplayName("Не DROPSHIP. Ошибка, если шаг недоступен")
    void testNotDropship() {
        assertThatThrownBy(
                () -> requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.WAREHOUSE)
        ).isInstanceOfSatisfying(HttpClientErrorException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        });
    }

    @Test
    @DisplayName("Click and collect. Ошибка, если шаг недоступен")
    void testNotAvailableIfClickAndCollect() {
        assertThatThrownBy(
                () -> requestStep(DS_CLICK_AND_COLLECT_CAMPAIGN_ID, WizardStepType.WAREHOUSE)
        ).isInstanceOfSatisfying(HttpClientErrorException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        });
    }

    @Test
    @DisplayName("Доступность шага для crossdock")
    void testCrossdockAvailable() {
        var response = requestStep(CROSSDOCK_SUPPLIER_CAMPAIGN_ID, WizardStepType.WAREHOUSE);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    @Test
    @DisplayName("Проверить, что нет ни одной записи partner_ff_service_link с типом dropship/supplier")
    void testWarehouseNoLinks() {
        var response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.WAREHOUSE);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    @Test
    @DisplayName("Нет настроенного места отгрузки")
    @DbUnitDataSet(before = "csv/testStockUpdateWithLinks.csv")
    void testWarehouseNoConfiguredDeliveryServiceRelation() {
        // given
        doReturn(false).when(logisticPartnerService).hasActivePartnerRelation(any());

        // when
        var response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.WAREHOUSE);

        // then
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    @Test
    @DisplayName("Настроен склад, место отгрузки и все остальное")
    @DbUnitDataSet(before = "csv/testStockUpdateWithLinks.csv")
    void testWarehouseOk() {
        // given
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());

        // when
        var response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.WAREHOUSE);

        // then
        assertResponse(response, makeResponseStepStatus(Status.FULL));
    }

    @Test
    @DisplayName("Проверить, что шаг задизейблен для случае, когда не заапрувлена заявка")
    @DbUnitDataSet(before = "csv/pushapilogsBasedTest.before.csv")
    void testPrepayRequestNotCompleted() {
        var response = requestStep(NOT_APPROVED_PREPAY_REQUEST_CAMPAIGN_ID, WizardStepType.WAREHOUSE);
        assertResponse(response, makeResponseStepStatus(Status.NONE));
    }

    private static WizardStepStatus makeResponseStepStatus(Status status) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.WAREHOUSE)
                .withStatus(status)
                .build();
    }
}
