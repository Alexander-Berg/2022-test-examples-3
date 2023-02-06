package ru.yandex.direct.web.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmIntegrationImpl;

import static ru.yandex.direct.common.configuration.TvmIntegrationConfiguration.TVM_INTEGRATION;
import static ru.yandex.direct.config.EssentialConfiguration.CONFIG_SCHEDULER_BEAN_NAME;

@Configuration
@Import({TestingDirectWebAppConfiguration.class, CoreTestingConfiguration.class})
public class ManualTestingWithTvm {

    @Bean(name = TVM_INTEGRATION)
    public TvmIntegration tvmIntegration(
            DirectConfig directConfig,
            @Qualifier(CONFIG_SCHEDULER_BEAN_NAME) TaskScheduler liveConfigChangeTaskScheduler) {
        return TvmIntegrationImpl.create(directConfig, liveConfigChangeTaskScheduler);
    }

}
