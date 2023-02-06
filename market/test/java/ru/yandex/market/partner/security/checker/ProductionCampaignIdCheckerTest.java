package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.mbi.environment.TestEnvironmentService;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

/**
 * Тесты для {@link ProductionCampaignIdChecker}
 */
public class ProductionCampaignIdCheckerTest extends FunctionalTest {

    @Autowired
    private ProductionCampaignIdChecker productionCampaignIdChecker;

    @Autowired
    private TestEnvironmentService environmentService;

    @BeforeEach
    void before() {
        environmentService.setEnvironmentType(EnvironmentType.PRODUCTION);
    }

    @AfterEach
    void after() {
        System.clearProperty("environment");
    }

    @ParameterizedTest
    @CsvSource(value = {"100; 100, 200; true", "101; 100, 200; false"}, delimiter = ';')
    void testChecker(long campaignId, String authParam, boolean expectedResult) {
        boolean result = productionCampaignIdChecker.checkTyped(
                new MockPartnerRequest(0, 0, campaignId, campaignId),
                new Authority("test", authParam)
        );
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    void testEnvironment() {
        environmentService.setEnvironmentType(EnvironmentType.TESTING);
        boolean result = productionCampaignIdChecker.checkTyped(
                new MockPartnerRequest(0, 0, 1, 1),
                new Authority("test", "2,3,4")
        );
        Assertions.assertTrue(result);
    }

}
