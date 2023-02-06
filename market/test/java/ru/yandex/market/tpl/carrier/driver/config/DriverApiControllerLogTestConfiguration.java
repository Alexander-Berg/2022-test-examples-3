package ru.yandex.market.tpl.carrier.driver.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.tpl.carrier.core.domain.log.CarrierControllerLogInterceptor;
import ru.yandex.market.tpl.carrier.core.domain.log.CarrierControllerLogService;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.common.logbroker.config.LogbrokerTestExternalConfig;
import ru.yandex.market.tpl.common.logbroker.producer.log.ControllerLogLogbrokerEventPublisher;

@Import(LogbrokerTestExternalConfig.class)
@Configuration
public class DriverApiControllerLogTestConfiguration {

    @Bean
    public CarrierControllerLogService carrierControllerLogService(
            ControllerLogLogbrokerEventPublisher controllerLogLogbrokerEventPublisher
    ) {
        return new CarrierControllerLogService(controllerLogLogbrokerEventPublisher);
    }

    @Bean
    public CarrierControllerLogInterceptor carrierControllerLogInterceptor(
            ConfigurationService configurationService,
            CarrierControllerLogService carrierControllerLogService
    ) {
        return new CarrierControllerLogInterceptor(configurationService, carrierControllerLogService);
    }

}
