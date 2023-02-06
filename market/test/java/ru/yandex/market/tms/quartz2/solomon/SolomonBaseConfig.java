package ru.yandex.market.tms.quartz2.solomon;

import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tms.quartz2.spring.TmsSettings;
import ru.yandex.market.tms.quartz2.spring.config.DatabaseSchedulerFactoryConfig;
import ru.yandex.market.tms.quartz2.spring.config.TmsDataSourceConfig;

@Configuration
public class SolomonBaseConfig extends DatabaseSchedulerFactoryConfig {

    public SolomonBaseConfig(ApplicationContext applicationContext,
                             TmsDataSourceConfig tmsDataSourceConfig) {
        super(applicationContext, tmsDataSourceConfig);
    }

    @Override
    public TmsSettings tmsSettings() {
        return () -> false;
    }


    @Bean(name = "quartzProperties")
    public Properties quartzProperties() {
        Properties props = new Properties();

        props.put("org.quartz.jobStore.tablePrefix", "TEST_QRTZ_");
        return props;
    }
}
