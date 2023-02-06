package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

/**
 * Функциональные тесты для шага wizard'a "Доставка".
 * См {@link ru.yandex.market.core.wizard.step.DeliveryStepStatusCalculator}
 *
 * @author Vladislav Bauer
 */
@DbUnitDataSet(before = "csv/commonWhiteWizardData.before.csv")
class WizardControllerDeliveryFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    private WireMockServer tarifficatorWireMockServer;

    /**
     * Проверить, что доставка не настроена.
     */
    @Test
    void testDeliveryStepEmpty() {
        prepareTarifficatorResponseEmpty(SHOP_ID);

        var response = requestStep(CAMPAIGN_ID, WizardStepType.DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    /**
     * Проверить, что доставка настроена точкой ПВЗ {@link ru.yandex.market.core.outlet.OutletType#DEPOT}.
     */
    @Test
    @DbUnitDataSet(before = "csv/testDeliveryStepFilledDepot.before.csv")
    void testDeliveryStepFilledDepot() {
        var response = requestStep(CAMPAIGN_ID, WizardStepType.DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }

    /**
     * Проверить, что доставка настроена смешенной точкой {@link ru.yandex.market.core.outlet.OutletType#MIXED}.
     */
    @Test
    @DbUnitDataSet(before = "csv/testDeliveryStepFilledMixed.before.csv")
    void testDeliveryStepFilledMixed() {
        var response = requestStep(CAMPAIGN_ID, WizardStepType.DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }

    /**
     * Проверить, что доставка считается настроенной, если задан самовывоз.
     */
    @Test
    @DbUnitDataSet(before = "csv/testDeliveryStepOutlets.before.csv")
    void testDeliveryOutlets() {
        var response = requestStep(CAMPAIGN_ID, WizardStepType.DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }

    /**
     * Проверить, что доставка считается настроенной, если есть доставка в shops_web.shop_self_delivery
     */
    @Test
    @DbUnitDataSet(before = "csv/testDeliveryStepFilledRegions.before.csv")
    void testDeliveryStepFilledRegionsByDB() {
        var response = requestStep(CAMPAIGN_ID, WizardStepType.DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }

    /**
     * Проверить, что доставка считается настроенной, если есть доставка в тарификаторе
     */
    @Test
    @DbUnitDataSet(before = "csv/testDeliveryStepFilledRegions.before.csv")
    void testDeliveryStepFilledRegionsByTarifficator() {
        prepareTarifficatorResponseOK(SHOP_ID);

        var response = requestStep(CAMPAIGN_ID, WizardStepType.DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }

    /**
     * Проверить, что доставка считается настроенной, если есть сохраненные настройки включенной ЯДо.
     */
    @Test
    @DbUnitDataSet(before = "csv/testDeliveryStepFilledYaDo.before.csv")
    void testYaDoSettings() {
        prepareTarifficatorResponseOK(SHOP_ID);

        var response = requestStep(CAMPAIGN_ID, WizardStepType.DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }


    private static WizardStepStatus makeResponseStepStatus(Status status) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.DELIVERY)
                .withStatus(status)
                .build();
    }

    private void prepareTarifficatorResponseOK(long shopId) {
        ResponseDefinitionBuilder shopStateResponse = aResponse().withStatus(200)
                .withBody(getString(this.getClass(), "json/tarifficatorCPCStateResponseOk.json"));

        tarifficatorWireMockServer.stubFor(post("/v2/shops/delivery/state")
                .withRequestBody(equalToJson(String.format(
                        "{\"shopIds\": %s}", List.of(shopId)
                )))
                .willReturn(shopStateResponse));
    }

    private void prepareTarifficatorResponseEmpty(long shopId) {
        ResponseDefinitionBuilder shopStateResponse = aResponse().withStatus(200)
                .withBody("{}");

        tarifficatorWireMockServer.stubFor(post("/v2/shops/delivery/state")
                .withRequestBody(equalToJson(String.format(
                        "{\"shopIds\": %s}", List.of(shopId)
                )))
                .willReturn(shopStateResponse));
    }
}
