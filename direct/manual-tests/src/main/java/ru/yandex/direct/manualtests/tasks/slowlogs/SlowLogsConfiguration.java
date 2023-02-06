package ru.yandex.direct.manualtests.tasks.slowlogs;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.TaskScheduler;

import ru.yandex.direct.cloud.iam.IIamTokenProvider;
import ru.yandex.direct.cloud.iam.service.CloudIamTokenProviderService;
import ru.yandex.direct.cloud.mdb.mysql.api.service.CloudMdbMySqlApiService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.manualtests.configuration.BaseConfiguration;
import ru.yandex.direct.mysql.slowlog.writer.states.MySqlClusterSlowLogsWriterStateProvider;

import static ru.yandex.direct.config.EssentialConfiguration.CONFIG_SCHEDULER_BEAN_NAME;

@Configuration
@ComponentScan(
        basePackages = "ru.yandex.direct.manualtests.tasks.slowlogs",
        excludeFilters = {
                @ComponentScan.Filter(value = Configuration.class, type = FilterType.ANNOTATION),
        }
)
public class SlowLogsConfiguration extends BaseConfiguration {
    @Bean
    public MySqlClusterSlowLogsWriterStateProvider mySqlClusterSlowLogsWriterStateProvider(
            PpcPropertiesSupport ppcPropertiesSupport) {
        return new MySqlClusterSlowLogsWriterStateProvider(ppcPropertiesSupport);
    }

    @Bean()
    public IIamTokenProvider iamTokenProvider(
            DirectConfig directConfig,
            @Qualifier(CONFIG_SCHEDULER_BEAN_NAME) TaskScheduler liveConfigChangeTaskScheduler) {
        return CloudIamTokenProviderService.create(
                directConfig.getBranch("cloud"),
                directConfig.getBranch("cloud_iam_default"),
                liveConfigChangeTaskScheduler);
    }

    @Bean()
    public CloudMdbMySqlApiService cloudMdbMySqlApi(DirectConfig directConfig, IIamTokenProvider iamTokenProvider) {
        return CloudMdbMySqlApiService.create(directConfig.getBranch("cloud"), iamTokenProvider);
    }

}
