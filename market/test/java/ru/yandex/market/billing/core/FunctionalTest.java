package ru.yandex.market.billing.core;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.billing.core.cpa_auction.CpaAuctionConfig;
import ru.yandex.market.billing.core.distribution.HardcodedClidsConfig;
import ru.yandex.market.billing.core.factoring.PayoutFrequencyConfig;
import ru.yandex.market.billing.core.partner.PartnerProgramConfig;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;

/**
 * Базовый класс для всех тестов
 */
@SpringJUnitConfig(
        classes = {
                EmbeddedPostgresConfig.class,
                FunctionalTest.Config.class
        }
)
@ActiveProfiles(profiles = {"functionalTest", "development"})
public class FunctionalTest extends JupiterDbUnitTest {
    @Configuration
    @Import({PayoutFrequencyConfig.class,
            HardcodedClidsConfig.class,
            PartnerProgramConfig.class,
            CpaAuctionConfig.class
    })
    static class Config {
        @Bean
        public Clock clock() {
            return new TestableClock();
        }
    }
}
