package ru.yandex.market.partner.delivery.tariff;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.datacamp.model.category.CategoryInfo;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

@DbUnitDataSet(before = "csv/getRegionGroupTariff.before.csv")
public class GetRegionDeliveryTariffServantletTest extends FunctionalTest {

    private static final long SHOP_ID = 100L;
    private static final long CAMPAIGN_ID = 111111L;
    private static final long BUSINESS_ID = 200L;
    private static final long TARIFFICATOR_REGION_GROUP_ID = 17L;

    @Autowired
    private WireMockServer tarifficatorWireMockServer;
    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    public static Stream<Arguments> getTariffTestParameters() {
        return Stream.of(
                Arguments.of(
                        "Возвращение категорийного тарифа с айдишниками из тарификатора",
                        "tarifficator.tariff.json",
                        "tariff.tarifficatorIds.response.json"),
                Arguments.of(
                        "Возвращение категорийного тарифа с правилом на весь фид",
                        "tarifficator.categoryTariff.json",
                        "tariff.tarifficatorIds.category.response.json")
        );
    }

    @BeforeEach
    protected void beforeEach() {
        doReturn(readDatacampResponse("datacampCategoriesResponse.json"))
                .when(dataCampShopClient).getBusinessCategories(eq(BUSINESS_ID));
    }

    @MethodSource(value = "getTariffTestParameters")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Успешное получение тарифа из тарификатора")
    void testGetRegionDeliveryTariff(
            @SuppressWarnings("unused") String testName,
            String tarifficatorResponsePath,
            String expectedResponsePath) {
        mockTarifficatorResponse(SHOP_ID, TARIFFICATOR_REGION_GROUP_ID, tarifficatorResponsePath);

        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/getRegionDeliveryTariff?id={datasourceId}&format=json&regionGroupId={regionGroupId}",
                CAMPAIGN_ID,
                TARIFFICATOR_REGION_GROUP_ID);
        JsonTestUtil.assertEquals(response, this.getClass(), expectedResponsePath);
    }

    private void mockTarifficatorResponse(long shopId, long regionGroupId, String tarifficatorResponsePath) {
        tarifficatorWireMockServer.resetMappings();
        tarifficatorWireMockServer.removeServeEventsMatching(RequestPattern.everything());

        ResponseDefinitionBuilder response = aResponse().withStatus(200).withBody(getString(this.getClass(), tarifficatorResponsePath));
        tarifficatorWireMockServer.stubFor(get("/v2/shops/" + shopId + "/region-groups/" + regionGroupId + "/tariff?_user_id=11")
                .willReturn(response));
    }

    private Map<String, CategoryInfo> readDatacampResponse(String responseFilePath) {
        try {
            return OBJECT_MAPPER.readValue(
                    StringTestUtil.getString(this.getClass().getResourceAsStream(responseFilePath)),
                    TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, CategoryInfo.class)
            );
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing categories response from stroller", ex);
        }
    }
}
