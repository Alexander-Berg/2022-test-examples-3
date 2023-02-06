package ru.yandex.direct.jobs.autobudget;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetCpaAlertRepository;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetHourlyAlertRepository;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.entity.autobudget.model.AutobudgetAlertProperties.CPA;
import static ru.yandex.direct.core.entity.autobudget.model.AutobudgetAlertProperties.HOURLY;


/**
 * Тесты на внутренние методы джобы AutobudgetOutdatedAlertsCleanerJob
 */
@JobsTest
@ExtendWith(SpringExtension.class)
class OutdatedAutobudgetAlertsCleanerMethodTest {
    private static final int SHARD = 1;

    private AutobudgetHourlyAlertRepository hourlyAlertRepository;
    private AutobudgetCpaAlertRepository cpaAlertRepository;
    private OutdatedAutobudgetAlertsCleaner job;
    private LocalDateTime now;

    @BeforeEach
    void before() {
        hourlyAlertRepository = mock(AutobudgetHourlyAlertRepository.class);
        cpaAlertRepository = mock(AutobudgetCpaAlertRepository.class);
        job = new OutdatedAutobudgetAlertsCleaner(SHARD, hourlyAlertRepository, cpaAlertRepository);
        now = LocalDateTime.now();
    }


    @Test
    void checkDeleteOutdatedHourlyAlerts() {
        job.deleteOutdatedHourlyAlerts(now);
        LocalDateTime expectedBorderDateTime = now.minus(HOURLY.getTtlForActiveAlerts());

        verify(hourlyAlertRepository).deleteActiveAlertsOlderThanDateTime(job.getShard(), expectedBorderDateTime);
    }

    @Test
    void checkDeleteOutdatedCpaAlerts() {
        job.deleteOutdatedCpaAlerts(now);
        LocalDateTime expectedBorderDateTime = now.minus(CPA.getTtlForActiveAlerts());

        verify(cpaAlertRepository).deleteActiveAlertsOlderThanDateTime(job.getShard(), expectedBorderDateTime);
    }
}
