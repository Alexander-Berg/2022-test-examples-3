package ru.yandex.market.tms.quartz2.spring;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тест проверяет, что в случае настройки spring через XML конфигурацию, импорт конфигураций
 * {@link ru.yandex.market.tms.quartz2.spring.config.DatabaseSchedulerFactoryConfig} и
 * {@link ru.yandex.market.tms.quartz2.spring.config.RAMSchedulerFactoryConfig} работает корректно
 */
@SpringJUnitConfig(locations = "classpath:/ru/yandex/market/tms/quartz2/spring/EnableMarketTmsForXMLConfigTest.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EnableMarketTmsForXMLConfigTest {

    @Autowired
    private TestExecutionStateHolder testExecutionStateHolder;

    @Test
    @DbUnitDataSet
    void testEnableMarketTmsContextInit() throws InterruptedException {
        Assertions.assertTrue(testExecutionStateHolder.getLatch().await(5, TimeUnit.SECONDS));
    }

}
