package ru.yandex.market.billing.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;

/**
 * Кофнигурация для джобы {@link TestingCleanerExecutor}.
 *
 * @author ogonek 27.07.2018
 */
@Configuration
public class TestingCleanerJobConfig {

    @Autowired
    private JdbcTemplate shopJdbcTemplate;

    @Autowired
    private ParamService paramService;

    @Autowired
    private ProtocolService protocolService;

    @CronTrigger(
            description = "Снятие флага тестовости у включённых магазинов.",
            cronExpression = "0 0/5 * * * ?"
    )
    @Bean
    public TestingCleanerExecutor testingCleanerExecutor() {
        return new TestingCleanerExecutor(
                shopJdbcTemplate,
                paramService,
                protocolService
        );
    }
}
