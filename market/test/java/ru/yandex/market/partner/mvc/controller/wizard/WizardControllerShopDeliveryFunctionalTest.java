package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.junit.jupiter.api.BeforeEach;
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
 * Функциональные тесты для шага wizard'a "Доставка магазина".
 * См {@link ru.yandex.market.core.wizard.step.ShopDeliveryStepStatusCalculator}
 *
 * @author natalokshina
 */
@DbUnitDataSet(before = "csv/commonWhiteWizardData.before.csv")
class WizardControllerShopDeliveryFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    private WireMockServer tarifficatorWireMockServer;

    @BeforeEach
    private void beforeEach() {
        tarifficatorWireMockServer.resetMappings();
        tarifficatorWireMockServer.removeServeEventsMatching(RequestPattern.everything());
    }

    /**
     * Проверить, что доставка не настроена.
     */
    @Test
    void testDeliveryStepEmpty() {
        prepareTarifficatorResponseEmpty(DSBS_SHOP_ID);

        var response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    /**
     * Проверить, что доставка настроена точкой ПВЗ {@link ru.yandex.market.core.outlet.OutletType#DEPOT}.
     */
    @Test
    @DbUnitDataSet(before = "csv/testShopDeliveryStepFilledDepot.before.csv")
    void testDeliveryStepFilledDepot() {
        var response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.FULL));
    }

    /**
     * Проверить, что доставка настроена смешенной точкой {@link ru.yandex.market.core.outlet.OutletType#MIXED}.
     */
    @Test
    @DbUnitDataSet(before = "csv/testShopDeliveryStepFilledMixed.before.csv")
    void testDeliveryStepFilledMixed() {
        var response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.FULL));
    }

    /**
     * Проверить, что доставка считается настроенной, если тариф задан для какого то из регионов.
     */
    @Test
    @DbUnitDataSet(before = "csv/testShopDeliveryStepRegions.before.csv")
    void testDeliveryRegions() {
        var response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.FULL));
    }

    /**
     * Проверить, что доставка считается настроенной, если задан самовывоз.
     */
    @Test
    @DbUnitDataSet(before = "csv/testShopDeliveryStepOutlets.before.csv")
    void testDeliveryOutlets() {
        var response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.FULL));
    }

    /**
     * Проверить, что у магазина настроена доставка, т.к. есть стостояние доставки в SHOPS_WEB.SHOP_SELF_DELIVERY
     */
    @Test
    @DbUnitDataSet(before = "csv/testShopDeliveryStepFilledRegions.before.csv")
    void testDeliveryStepFilledRegionsByDB() {
        var response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.FULL));
    }

    /**
     * Проверить, что у магазина настроена доставка в тарификаторе
     */
    @Test
    void testDeliveryStepFilledRegionsByTarifficator() {
        prepareTarifficatorResponseOK(DSBS_SHOP_ID);

        var response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_DELIVERY);
        assertResponse(response, makeResponseStepStatus(Status.FULL));
    }


    private static WizardStepStatus makeResponseStepStatus(Status status) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.SHOP_DELIVERY)
                .withStatus(status)
                .build();
    }

    private void prepareTarifficatorResponseOK(long shopId) {
        ResponseDefinitionBuilder shopStateResponse = aResponse().withStatus(200)
                .withBody(getString(this.getClass(), "json/tarifficatorShopStateResponseOk.json"));

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
