package ru.yandex.market.core.moderation.recommendation;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.moderation.recommendation.service.PartnerSettingsRecommendationService;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

/**
 * Абстрактный класс для тестов {@link PartnerSettingsRecommendationService}.
 */
@DbUnitDataSet(before = "csv/PartnerSettingsRecommendationServiceTest.before.csv")
public abstract class PartnerSettingsRecommendationServiceAbstractTest extends FunctionalTest {

    @Autowired
    protected WireMockServer tarifficatorWireMockServer;

    @Autowired
    PartnerSettingsRecommendationService partnerSettingsRecommendationService;

    @BeforeEach
    protected void beforeEach() {
        tarifficatorWireMockServer.resetMappings();
        tarifficatorWireMockServer.removeServeEventsMatching(RequestPattern.everything());

        tarifficatorWireMockServer.stubFor(get(urlMatching("/v2/shops/.*/region-groups"))
                .atPriority(5)
                .willReturn(aResponse().withStatus(200).withBody("{}"))
        );
    }

    protected boolean getCheckerResult(SettingType type, long partnerId) {
        final Map<SettingType, Boolean> settingsRecommendations =
                partnerSettingsRecommendationService.getSettingsRecommendations(partnerId);

        Assertions.assertTrue(settingsRecommendations.containsKey(type));
        return settingsRecommendations.get(type);
    }
}
