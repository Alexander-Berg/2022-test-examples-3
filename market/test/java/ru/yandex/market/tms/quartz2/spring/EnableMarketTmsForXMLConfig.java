package ru.yandex.market.tms.quartz2.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.spring.config.DatabaseSchedulerFactoryConfig;
import ru.yandex.market.tms.quartz2.spring.config.RAMSchedulerFactoryConfig;

@Configuration
@Import({DatabaseSchedulerFactoryConfig.class, RAMSchedulerFactoryConfig.class})
public class EnableMarketTmsForXMLConfig {

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
