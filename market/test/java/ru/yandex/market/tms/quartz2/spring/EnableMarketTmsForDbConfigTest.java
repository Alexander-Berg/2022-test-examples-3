package ru.yandex.market.tms.quartz2.spring;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.tms.quartz2.config.FunctionalTest;
import ru.yandex.market.tms.quartz2.config.FunctionalTestConfig;
import ru.yandex.market.tms.quartz2.model.Executor;

/**
 * Тест проверяет, что:
 * <ul>
 * <li>контекст успешно поднимается с персистентым хранилищем в виде базы данных H2</li>
 * <li>проперти, определенные в бине {@code quartzProperties} успешно доезжают до шедулера</li>
 * <li>настроенная TMS задача успешно выполняется с такими настройками</li>
 * <li>факт выполнения задачи логгируется в БД</li>
 * </ul>
 */
@ContextConfiguration
class EnableMarketTmsForDbConfigTest extends FunctionalTest {

    private static final TestExecutionStateHolder HOLDER = new TestExecutionStateHolder();
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testEnableMarketTmsContextInit() throws InterruptedException {
        HOLDER.getLatch().await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(HOLDER.isSuccess());

        MarkerTmsAsserts.assertExistTestQrtzLogRecords(jdbcTemplate);
    }

    @Configuration
    @Import({
            FunctionalTestConfig.class
    })
    static class Config {

        @Bean
        @CronTrigger(
                cronExpression = "0/1 * * * * ?",
                description = "Test executor"
        )
        public Executor testExecutor() {
            return context -> {
                HOLDER.markSuccess();
                HOLDER.getLatch().countDown();
            };
        }

    }

}
