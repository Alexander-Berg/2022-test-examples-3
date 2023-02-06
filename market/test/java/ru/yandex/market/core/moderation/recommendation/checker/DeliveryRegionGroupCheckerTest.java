package ru.yandex.market.core.moderation.recommendation.checker;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.moderation.recommendation.PartnerSettingsRecommendationServiceAbstractTest;
import ru.yandex.market.core.moderation.recommendation.SettingType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

/**
 * Тест для {@link DeliveryRegionGroupChecker}.
 */
class DeliveryRegionGroupCheckerTest extends PartnerSettingsRecommendationServiceAbstractTest {

    @Test
    void testHaveOnlySelfRegion() {
        init(774L, "testHaveOnlySelfRegion");
        Assertions.assertTrue(getCheckerResult(SettingType.DELIVERY_REGION_GROUP, 774L));
    }

    @Test
    void testHaveOtherRegions() {
        init(775L, "testHaveOtherRegions");
        Assertions.assertFalse(getCheckerResult(SettingType.DELIVERY_REGION_GROUP, 775L));
    }

    @Test
    void testHaveRegionsWithDsOnly() {
        init(776L, "testHaveRegionsWithDsOnly");
        Assertions.assertFalse(getCheckerResult(SettingType.DELIVERY_REGION_GROUP, 776L));
    }

    private void init(long shopId, String fileName) {
        ResponseDefinitionBuilder response = aResponse().withStatus(200)
                .withBody(getString(this.getClass(), "json/" + fileName + ".response.json"));
        tarifficatorWireMockServer.stubFor(get("/v2/shops/" + shopId + "/region-groups").atPriority(1)
                .willReturn(response));
    }
}
