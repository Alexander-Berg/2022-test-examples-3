package ru.yandex.market.arbiter.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.arbiter.test.config.TestApplicationConfig;

/**
 * @author moskovkin@yandex-team.ru
 * @since 23.05.2020
 */
@WebAppConfiguration
@SpringJUnitConfig(TestApplicationConfig.class)
public class BaseUnitTest {
    @Autowired
    protected TestDataService testDataService;

    @BeforeEach
    public void setup() {
        testDataService.cleanDatabase();
    }
}
