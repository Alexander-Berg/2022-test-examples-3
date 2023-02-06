package ru.yandex.market.tms.quartz2.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tms.quartz2.model.Executor;

@Configuration
public class EnableMarketTmsTasksConfig {

    @Bean
    public TestExecutionStateHolder testExecutionStateHolder() {
        return new TestExecutionStateHolder();
    }

    @Bean
    @CronTrigger(
            cronExpression = "0/1 * * * * ?",
            description = "Test executor"
    )
    public Executor executor() {
        return context -> testExecutionStateHolder().getLatch().countDown();
    }

}
