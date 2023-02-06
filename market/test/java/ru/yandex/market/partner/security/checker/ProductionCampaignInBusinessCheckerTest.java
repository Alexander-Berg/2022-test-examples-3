package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.security.DefaultCampaignable;
import ru.yandex.market.mbi.environment.TestEnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "ProductionCampaignInBusinessCheckerTest.before.csv")
public class ProductionCampaignInBusinessCheckerTest extends FunctionalTest {

    @Autowired
    private ProductionCampaignInBusinessChecker checker;

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
    @CsvSource(value = {"101, true", "102, false"})
    void testCheck(long campaignId, boolean expectedResult) {
        assertThat(checker.checkTyped(new DefaultCampaignable(campaignId, 0, 0), new Authority("", "10,11")))
                .isEqualTo(expectedResult);
    }
}
