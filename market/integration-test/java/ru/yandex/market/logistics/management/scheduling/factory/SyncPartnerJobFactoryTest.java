package ru.yandex.market.logistics.management.scheduling.factory;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.configuration.scheduling.factory.SyncPartnerJobFactory;
import ru.yandex.market.logistics.management.configuration.scheduling.job.AbstractSyncPartnerJob;
import ru.yandex.market.logistics.management.repository.SettingsMethodRepository;

import static ru.yandex.market.logistics.management.configuration.scheduling.factory.SyncPartnerJobFactory.REGISTER_TRIGGERS;

@DatabaseSetup(
    value = "/data/executor/before/sync_partner.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
@SuppressWarnings("unchecked")
public class SyncPartnerJobFactoryTest extends AbstractContextualTest {

    private static final JobKey SYNC_PARTNER_PICKUP_POINTS_JOB_KEY = JobKey.jobKey("syncPartnerPickupPointsJob");
    private static final JobKey SYNC_TIMETABLE_KEY = JobKey.jobKey("syncDeliveryIntervalRequestCreatorJob");

    @Autowired
    private SettingsMethodRepository settingsMethodRepository;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private List<AbstractSyncPartnerJob> jobs;

    @BeforeAll
    static void before() {
        System.setProperty(REGISTER_TRIGGERS, "true");
    }

    @AfterAll
    static void after() {
        System.clearProperty(REGISTER_TRIGGERS);
    }

    @AfterEach
    void clean() throws SchedulerException {
        scheduler.clear();
    }

    @Test
    @DisplayName("Job and triggers to sync partner pickup points were correctly initialized")
    void testSyncPartnerJobFactory() throws SchedulerException {
        new SyncPartnerJobFactory(settingsMethodRepository, jobs, scheduler).registerTriggers();

        softly.assertThat(scheduler.getTriggersOfJob(SYNC_TIMETABLE_KEY))
            .as("Should be exactly 1 trigger")
            .hasSize(1)
            .extracting(
                t -> t.getKey().getName(),
                t -> ((CronTrigger) t).getCronExpression()
            )
            .as("Triggers have proper keys and cron expressions")
            .containsExactlyInAnyOrder(
                new Tuple("syncDeliveryIntervalRequestCreatorJob_partner_1", "0 0 12 1 * ? 2042")
            );
    }
}
