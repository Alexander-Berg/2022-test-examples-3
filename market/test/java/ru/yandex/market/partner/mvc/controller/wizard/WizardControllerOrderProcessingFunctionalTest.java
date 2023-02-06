package ru.yandex.market.partner.mvc.controller.wizard;


import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.experiment.WizardExperimentsConfig;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.mvc.controller.wizard.utils.DatacampFlagResponseMocker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для шага wizard'a "Шаг настройка обработки заказов".
 * См {@link ru.yandex.market.core.wizard.step.OrderProcessingStepCalculator}
 */
@DbUnitDataSet(before = {"csv/commonBlueWizardData.before.csv", "csv/orderProcessing.before.csv"})
class WizardControllerOrderProcessingFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    CheckouterAPI checkouterClient;

    @Autowired
    @Qualifier("environmentService")
    EnvironmentService environmentService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    private static final long CROSSDOCK_DONT_WANT_NO_PRICES = 16003L;
    private static final long CROSSDOCK_DONT_WANT_CAN_ENABLE = 16004L;
    private static final long CROSSDOCK_NO_OFFER_IN_INDEX = 16005L;
    private static final long CROSSDOCK_PRICE_WITHOUT_STOCKS = 16006L;

    private DatacampFlagResponseMocker datacampMocker;

    @BeforeEach
    public void init() {
        datacampMocker = new DatacampFlagResponseMocker(dataCampShopClient);
    }

    @Test
    @DisplayName("Ошибка, если шаг недоступен")
    void notAvailableForFulfillment() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.ORDER_PROCESSING)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("КД - Status = NONE, фича CROSSDOCK в статусе DONT_WANT, нет цен")
    void crossdockStatusNoneNoPrices() {
        environmentService.setValue(WizardExperimentsConfig.SAAS_STATUS_EXP_VAR, "1");
        mockSaasService(0);
        var response = requestStep(CROSSDOCK_DONT_WANT_NO_PRICES, WizardStepType.ORDER_PROCESSING);
        assertResponse(response, makeResponseStepStatus(Status.NONE));
    }

    @Test
    @DisplayName("КД - Status = ENABLING автоматом, Фича DROPSHIP в статусе DONT_WANT, можно включить")
    void crossdockFilledStatusEnablingAutomatically() {
        mockSaaSWithoutStocks(1);
        mockSaaSWithStocks(1);
        var response = requestStep(CROSSDOCK_DONT_WANT_CAN_ENABLE, WizardStepType.ORDER_PROCESSING);
        assertResponse(response, makeResponseStepStatus(Status.ENABLING));
    }

    @Test
    @DisplayName("КД - Status = ENABLING, когда нет ни одного оффера в индексе")
    void crossdockEnablingStatusWhenNoOffersInIndex() {
        mockSaaSWithoutStocks(1);
        mockSaaSWithStocks(1);
        var response = requestStep(CROSSDOCK_NO_OFFER_IN_INDEX, WizardStepType.ORDER_PROCESSING);
        assertResponse(response, makeResponseStepStatus(Status.ENABLING));
    }

    @Test
    @DisplayName("КД - Status = FULL, когда есть хотя бы один оффер в индексе")
    void crossdockFullStatusWhenHasOffersInIndex() {
        mockSaaSWithoutStocks(1);
        mockSaaSWithStocks(1);
        var response = requestStep(CROSSDOCK_SUPPLIER_CAMPAIGN_ID, WizardStepType.ORDER_PROCESSING);
        assertResponse(response, makeResponseStepStatus(Status.FULL));
    }

    @Test
    @DisplayName("КД - Status = NONE, в Datacamp залита цена, но не указаны стоки")
    void crossdockStatusNonePriceWithoutStocks() {
        mockSaaSWithoutStocks(1);
        mockSaaSWithStocks(0);

        var response = requestStep(CROSSDOCK_PRICE_WITHOUT_STOCKS, WizardStepType.ORDER_PROCESSING);
        assertResponse(response, makeResponseStepStatus(Status.NONE));
    }

    private void mockOrderCount(long shopId, int full, int processed, int shipped) {
        when(checkouterClient.getOrdersCount(
                argThat(r -> r != null && CollectionUtils.isEmpty(r.statuses)),
                eq(ClientRole.SHOP_USER),
                eq(shopId))
        ).thenReturn(full);
        when(checkouterClient.getOrdersCount(
                argThat(r -> r != null && !CollectionUtils.isEmpty(r.statuses)
                        && CollectionUtils.isEmpty(r.substatuses)),
                eq(ClientRole.SHOP_USER),
                eq(shopId))
        ).thenReturn(processed);
        when(checkouterClient.getOrdersCount(
                argThat(r -> r != null && !CollectionUtils.isEmpty(r.statuses)
                        && !CollectionUtils.isEmpty(r.substatuses)),
                eq(ClientRole.SHOP_USER),
                eq(shopId))
        ).thenReturn(shipped);
    }

    private static WizardStepStatus makeResponseStepStatus(Status status) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.ORDER_PROCESSING)
                .withStatus(status)
                .build();
    }
}
