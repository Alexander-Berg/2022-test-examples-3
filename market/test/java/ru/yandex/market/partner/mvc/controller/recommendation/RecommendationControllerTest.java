package ru.yandex.market.partner.mvc.controller.recommendation;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.moderation.recommendation.SettingType;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

/**
 * Тесты на {@link RecommendationController}.
 *
 * @author serenitas
 */
@DbUnitDataSet(before = "RecommendationControllerTest.before.csv")
class RecommendationControllerTest extends FunctionalTest {

    @Autowired
    protected WireMockServer tarifficatorWireMockServer;

    @BeforeEach
    protected void beforeEach() {
        tarifficatorWireMockServer.resetMappings();
        tarifficatorWireMockServer.removeServeEventsMatching(RequestPattern.everything());

        initStub(1L);
        initStub(2L);
    }

    private void initStub(Long shopId) {
        ResponseDefinitionBuilder response = aResponse().withStatus(200)
                .withBody(getString(this.getClass(), "json/regions" + shopId + ".json"));
        tarifficatorWireMockServer.stubFor(get("/v2/shops/" + shopId + "/region-groups").atPriority(1)
                .willReturn(response));
    }

    @Test
    void testNoRecommendations() throws IOException {
        final PartnerRecommendationListDTO response = getResponse(1L);
        final List<RecommendationDTO> expected = List.of(
                new RecommendationDTO(SettingType.ROUND_THE_CLOCK, false),
                new RecommendationDTO(SettingType.DELIVERY_REGION_GROUP, false),
                new RecommendationDTO(SettingType.PICKUP_POINT, false),
                new RecommendationDTO(SettingType.PROMO, false)
        );
        Assertions.assertTrue(response.getRecommendations().containsAll(expected));
    }

    @Test
    void testHaveSomeRecommendations() throws IOException {
        final PartnerRecommendationListDTO response = getResponse(2L);
        final List<RecommendationDTO> expected = List.of(
                new RecommendationDTO(SettingType.ROUND_THE_CLOCK, false),
                new RecommendationDTO(SettingType.DELIVERY_REGION_GROUP, true),
                new RecommendationDTO(SettingType.PICKUP_POINT, true),
                new RecommendationDTO(SettingType.PROMO, false)
        );
        Assertions.assertTrue(response.getRecommendations().containsAll(expected));
    }

    private PartnerRecommendationListDTO getResponse(long campaignId) throws IOException {
        ResponseEntity<String> entity = FunctionalTestHelper.get(buildUrl(campaignId));
        Assertions.assertEquals(HttpStatus.OK, entity.getStatusCode());
        final String body = entity.getBody();
        final JsonElement jsonElement = JsonTestUtil.parseJson(Objects.requireNonNull(body));
        return OBJECT_MAPPER.readValue(jsonElement.getAsJsonObject().get("result").toString(), PartnerRecommendationListDTO.class);
    }

    private String buildUrl(long campaignId) {
        return baseUrl + "/recommendations?id=" + campaignId;
    }
}
