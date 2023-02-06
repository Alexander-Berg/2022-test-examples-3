package ru.yandex.market.pharmatestshop.config;


import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.tms.quartz2.controller.TmsController;
import ru.yandex.market.tms.quartz2.spring.config.DatabaseSchedulerFactoryConfig;
import ru.yandex.market.tms.quartz2.spring.config.RAMSchedulerFactoryConfig;

@Configuration
@Import({
        DatabaseSchedulerFactoryConfig.class,
        RAMSchedulerFactoryConfig.class,
        TmsController.class,
})
public class QuartzConfiguration {

    @Value("${org.quartz.jobStore.driverDelegateClass}")
    private String driverDelegate;

    @Value("${org.quartz.jobStore.tablePrefix}")
    private String tablePrefix;

    @Bean
    public Properties quartzProperties() {
        Properties properties = new Properties();
        properties.setProperty("org.quartz.jobStore.driverDelegateClass", driverDelegate);
        properties.setProperty("org.quartz.jobStore.tablePrefix", tablePrefix);
        return properties;
    }

}

