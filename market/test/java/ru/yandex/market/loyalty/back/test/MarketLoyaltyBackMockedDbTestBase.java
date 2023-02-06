package ru.yandex.market.loyalty.back.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.back.config.BackTestConfig;
import ru.yandex.market.loyalty.back.config.MarketLoyaltyBack;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.test.TestCoverageRule;

import static ru.yandex.market.loyalty.back.config.BackTestConfig.TestTvmTicketProvider.TestMode.DEFAULT;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 05.04.17
 */
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = BackTestConfig.class)
public abstract class MarketLoyaltyBackMockedDbTestBase extends MarketLoyaltyCoreMockedDbTestBase {

    @Rule
    @Autowired
    public TestCoverageRule testCoverageRule;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    @MarketLoyaltyBack
    protected ObjectMapper objectMapper;
    @Autowired
    protected MarketLoyaltyClient marketLoyaltyClient;
    @Autowired
    protected BackTestConfig.TestTvmTicketProvider testTvmTicketProvider;

    @Before
    public void init() {
        testTvmTicketProvider.setTestMode(DEFAULT);
    }
}
