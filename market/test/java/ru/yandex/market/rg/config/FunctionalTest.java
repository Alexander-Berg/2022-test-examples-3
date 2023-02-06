package ru.yandex.market.rg.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;
import ru.yandex.market.yql_test.test_listener.YqlTestListener;

@SpringJUnitConfig(locations = "classpath:/ru/yandex/market/rg/config/functional-test-context.xml")
@ActiveProfiles(profiles = {"functionalTest", "development"})
@PreserveDictionariesDbUnitDataSet
@TestPropertySource("classpath:ru/yandex/market/rg/config/functional-yql-test.properties")
@TestExecutionListeners(value = {
        YqlTestListener.class
})
public abstract class FunctionalTest extends JupiterDbUnitTest {
    @Autowired
    @Qualifier("baseUrl")
    protected String baseUrl;
}
