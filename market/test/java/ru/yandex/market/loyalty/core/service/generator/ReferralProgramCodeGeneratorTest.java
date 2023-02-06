package ru.yandex.market.loyalty.core.service.generator;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.config.Default;
import ru.yandex.market.loyalty.core.dao.coupon.CodeDao;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.monitoring.PushMonitor;

public class ReferralProgramCodeGeneratorTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final Logger log = LogManager.getLogger(ReferralProgramCodeGeneratorTest.class);

    @Autowired
    @Default
    private PushMonitor pushMonitor;
    @Autowired
    private CodeDao codeDao;

    @Test
    public void canGenerateEnoughUniquePromocodes() throws GeneratorException {
        final int amount = 10_000;
        long startTime = clock.millis();
        ReferralProgramCodeGenerator generator = getNewCodeGenerator();
        for (int i = 0; i < amount; i++) {
            generator.generate();
        }
        clock.reset();
        log.debug("{} codes was generated in {} sec.", amount, (clock.millis() - startTime) / 1000F);
    }

    @Test
    public void generateCodesWithBatches() throws GeneratorException {
        final int collectionSize = 200_000;
        ReferralProgramCodeGenerator generator = getNewCodeGenerator();
        long startTime = clock.millis();
        Set<String> codes = generator.generateCollection(collectionSize);
        clock.reset();
        log.debug("{} codes was generated in {} sec.", codes.size(), (clock.millis() - startTime) / 1000F);
        MatcherAssert.assertThat(codes.size(), Matchers.equalTo(collectionSize));
    }


    private ReferralProgramCodeGenerator getNewCodeGenerator() {
        return new ReferralProgramCodeGenerator(
                codeDao, pushMonitor
        );
    }

}
