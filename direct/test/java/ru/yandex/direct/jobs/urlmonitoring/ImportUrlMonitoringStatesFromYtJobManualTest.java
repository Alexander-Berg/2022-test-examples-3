package ru.yandex.direct.jobs.urlmonitoring;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.domain.service.DomainService;
import ru.yandex.direct.core.entity.urlmonitoring.service.UrlMonitoringService;
import ru.yandex.direct.jobs.configuration.JobsConfiguration;
import ru.yandex.direct.solomon.SolomonPushClient;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        JobsConfiguration.class,
})
@ParametersAreNonnullByDefault
// если вдруг захотелось запустить, нужно закомментировать @Disabled
@Disabled("Для запуска вручную")
class ImportUrlMonitoringStatesFromYtJobManualTest {
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private DomainService domainService;
    @Autowired
    private UrlMonitoringService urlMonitoringService;
    @Autowired
    private SolomonPushClient solomonPushClient;

    private ImportUrlMonitoringStatesFromYtJob urlMonitoringEventReceivingJob;

    @BeforeEach
    void setUp() {
        this.urlMonitoringEventReceivingJob = new ImportUrlMonitoringStatesFromYtJob(
                ppcPropertiesSupport, domainService, urlMonitoringService, solomonPushClient);
    }

    @Test
    void fire() {
        this.urlMonitoringEventReceivingJob.execute();
    }

}
