package ru.yandex.market.core.delivery;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.tariff.service.DeliveryTariffCategoryService;
import ru.yandex.market.mbi.datacamp.model.category.CategoryInfo;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

@DbUnitDataSet(before = "DeliveryTariffCategoryServiceTest.before.csv")
public class DeliveryTariffCategoryServiceTest extends FunctionalTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private DeliveryTariffCategoryService tested;
    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;
    @Autowired
    @Qualifier("tarifficatorWireMockServer")
    private WireMockServer tarifficatorWireMockServer;

    @BeforeEach
    protected void beforeEach() {
        tarifficatorWireMockServer.resetMappings();
        tarifficatorWireMockServer.removeServeEventsMatching(RequestPattern.everything());
    }

    @Test
    public void testGetCategoriesUsage() {
        long regionGroupId = 1111L;
        when(dataCampShopClient.getBusinessCategories(eq(200L))).thenReturn(readDatacampResponse("json/datacamp.category.response.json"));
        initTarifficatorMock(100L, 1111L, "json/tarifficator.tariff.response.json");

        Collection<ru.yandex.market.core.delivery.tariff.model.CategoryInfo> result = tested.getCategoriesUsage(100L, regionGroupId);
        assertNotNull(result);
        Assertions.assertThat(result)
                .hasSize(4)
                .contains(ru.yandex.market.core.delivery.tariff.model.CategoryInfo.builder()
                        .withId("100")
                        .withRegionGroupId(1111L)
                        .withName("Дизайнерская мягкая мебель")
                        .withChildrenCount(2L)
                        .withChildrenInGroup(Set.of((short) 0, (short) 1))
                        .withFeedId(200884669L)
                        .build())
                .contains(ru.yandex.market.core.delivery.tariff.model.CategoryInfo.builder()
                        .withId("101")
                        .withRegionGroupId(1111L)
                        .withParentId("100")
                        .withName("Двухместные дизайнерские диваны")
                        .withChildrenCount(0L)
                        .withChildrenInGroup(new HashSet<>())
                        .withOrderNum((short) 0)
                        .withFeedId(200884669L)
                        .build())
                .contains(ru.yandex.market.core.delivery.tariff.model.CategoryInfo.builder()
                        .withId("102")
                        .withRegionGroupId(1111L)
                        .withParentId("100")
                        .withName("Трехместные дизайнерские диваны")
                        .withChildrenCount(0L)
                        .withChildrenInGroup(new HashSet<>())
                        .withOrderNum((short) 1)
                        .withFeedId(200884669L)
                        .build())
                .contains(ru.yandex.market.core.delivery.tariff.model.CategoryInfo.builder()
                        .withRegionGroupId(1111L)
                        .withName("Feed 200884669")
                        .withChildrenCount(1L)
                        .withChildrenInGroup(Set.of((short) 0, (short) 1))
                        .withFeedId(200884669L)
                        .build());
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

    private void initTarifficatorMock(long shopId, long regionGroupId, String fileName) {
        ResponseDefinitionBuilder response = aResponse().withStatus(200)
                .withBody(getString(this.getClass(), fileName));
        tarifficatorWireMockServer.stubFor(
                get("/v2/shops/" + shopId + "/region-groups/" + regionGroupId + "/tariff?_user_id=11")
                        .atPriority(1)
                        .willReturn(response)
        );
    }
}
