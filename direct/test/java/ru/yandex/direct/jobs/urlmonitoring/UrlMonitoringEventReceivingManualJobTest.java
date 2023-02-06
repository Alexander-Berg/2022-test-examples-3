package ru.yandex.direct.jobs.urlmonitoring;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.urlmonitoring.service.UrlMonitoringService;
import ru.yandex.direct.jobs.configuration.JobsConfiguration;
import ru.yandex.direct.solomon.SolomonPushClient;
import ru.yandex.kikimr.persqueue.consumer.SyncConsumer;

import static ru.yandex.direct.jobs.configuration.JobsEssentialConfiguration.URL_MONITORING_LB_SYNC_CONSUMER_PROVIDER;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        JobsConfiguration.class,
})
@ParametersAreNonnullByDefault
// если вдруг захотелось запустить, нужно закомментировать @Disabled
@Disabled("Для запуска вручную")
class UrlMonitoringEventReceivingManualJobTest {
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    @Qualifier(URL_MONITORING_LB_SYNC_CONSUMER_PROVIDER)
    private Provider<SyncConsumer> syncConsumerProvider;
    @Autowired
    private UrlMonitoringService urlMonitoringService;
    @Autowired
    private SolomonPushClient solomonPushClient;
    private UrlMonitoringEventReceivingJob urlMonitoringEventReceivingJob;

    @BeforeEach
    void setUp() {
        this.urlMonitoringEventReceivingJob = new UrlMonitoringEventReceivingJob(
                ppcPropertiesSupport,
                syncConsumerProvider,
                urlMonitoringService, solomonPushClient);
    }

    @Test
    void fire() throws Exception {
        this.urlMonitoringEventReceivingJob.execute();
    }

}
