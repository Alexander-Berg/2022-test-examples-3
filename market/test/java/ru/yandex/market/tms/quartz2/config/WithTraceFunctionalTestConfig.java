package ru.yandex.market.tms.quartz2.config;

import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.common.util.terminal.CommandExecutor;
import ru.yandex.market.tms.quartz2.spring.BaseTmsTestConfig;
import ru.yandex.market.tms.quartz2.spring.config.DatabaseSchedulerFactoryConfig;
import ru.yandex.market.tms.quartz2.spring.config.RAMSchedulerFactoryConfig;
import ru.yandex.market.tms.quartz2.spring.config.TmsCommandsConfig;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
        BaseTmsTestConfig.class,
        TmsCommandsConfig.class,
        DatabaseSchedulerFactoryConfig.class,
        RAMSchedulerFactoryConfig.class,
})
@PropertySource("classpath:/ru/yandex/market/tms/quartz2/spring/EnableMarketTmsTestTrace.properties")
public class WithTraceFunctionalTestConfig extends H2Config {

    @Bean(name = "quartzProperties")
    public Properties quartzProperties() {
        Properties props = new Properties();
        props.put("org.quartz.jobStore.tablePrefix", "TEST_QRTZ_");
        return props;
    }

    @Bean
    public CommandExecutor commandExecutor() {
        return mock(CommandExecutor.class);
    }

}
