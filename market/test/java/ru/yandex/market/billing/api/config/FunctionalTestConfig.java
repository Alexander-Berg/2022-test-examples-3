package ru.yandex.market.billing.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.billing.core.factoring.ContractPayoutFrequencyDao;
import ru.yandex.market.billing.security.config.PassportConfig;

/**
 * Основной конфиг для тестов
 */
@TestConfiguration
public class FunctionalTestConfig {
    @MockBean
    private PassportConfig passportConfig;
    @MockBean
    private MvcConfig mvcConfig;

    @MockBean
    private ContractPayoutFrequencyDao contractPayoutFrequencyDaoMock;

    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }
}
