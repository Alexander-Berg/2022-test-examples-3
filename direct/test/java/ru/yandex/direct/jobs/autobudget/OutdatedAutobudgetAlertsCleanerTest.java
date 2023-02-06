package ru.yandex.direct.jobs.autobudget;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetCommonAlertStatus;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetCpaAlertRepository;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetHourlyAlertRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


/**
 * Тесты на джобу OutdatedAutobudgetAlertsCleaner с использованием базы из докера.
 */
@JobsTest
@ExtendWith(SpringExtension.class)
class OutdatedAutobudgetAlertsCleanerTest {

    @Autowired
    private Steps steps;

    @Autowired
    private AutobudgetHourlyAlertRepository hourlyAlertRepository;

    @Autowired
    private AutobudgetCpaAlertRepository cpaAlertRepository;

    private OutdatedAutobudgetAlertsCleaner job;
    private AutobudgetAlertsHelper autobudgetAlertsHelper;
    private long cid;
    private int shard;

    private void executeJob() {
        assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();
    }

    @BeforeEach
    void before() {
        CampaignInfo defaultCampaign = steps.campaignSteps().createDefaultCampaign();
        shard = defaultCampaign.getShard();
        cid = defaultCampaign.getCampaignId();

        job = new OutdatedAutobudgetAlertsCleaner(shard, hourlyAlertRepository, cpaAlertRepository);
        autobudgetAlertsHelper = new AutobudgetAlertsHelper(hourlyAlertRepository, cpaAlertRepository);
    }


    @Test
    void checkOutdatedAutobudgetAlertsCleaner() {
        autobudgetAlertsHelper.addOutdatedActiveAlerts(shard, cid);
        assumeThat("алерт с типом HOURLY есть в базе",
                hourlyAlertRepository.getCidOfExistingAlerts(shard, singletonList(cid)), contains(cid));
        assumeThat("алерт с типом CPA есть в базе",
                cpaAlertRepository.getCidOfExistingAlerts(shard, singletonList(cid)), contains(cid));

        executeJob();

        Set<Long> existingHourlyAlerts = hourlyAlertRepository.getCidOfExistingAlerts(shard, singletonList(cid));
        assertThat("алерт с типом HOURLY должен удалиться", existingHourlyAlerts, empty());
        Set<Long> existingCpaAlerts = cpaAlertRepository.getCidOfExistingAlerts(shard, singletonList(cid));
        assertThat("алерт с типом CPA должен удалиться", existingCpaAlerts, empty());
    }

    @Test
    void checkNotCleanNotOutdatedAlerts() {
        autobudgetAlertsHelper.addNotOutdatedActiveAlerts(shard, cid);
        executeJob();

        Set<Long> existingHourlyAlerts = hourlyAlertRepository.getCidOfExistingAlerts(shard, singletonList(cid));
        assertThat("алерт с типом HOURLY не должен удалиться", existingHourlyAlerts, contains(cid));
        Set<Long> existingCpaAlerts = cpaAlertRepository.getCidOfExistingAlerts(shard, singletonList(cid));
        assertThat("алерт с типом CPA не должен удалиться", existingCpaAlerts, contains(cid));
    }

    @Test
    void checkNotCleanNotActiveOutdatedAutobudgetAlerts() {
        long cid2 = steps.campaignSteps().createDefaultCampaign().getCampaignId();
        autobudgetAlertsHelper.addNotActiveOutdatedAlerts(shard, cid, AutobudgetCommonAlertStatus.FROZEN);
        autobudgetAlertsHelper.addNotActiveOutdatedAlerts(shard, cid2, AutobudgetCommonAlertStatus.STOPPED);

        executeJob();

        Set<Long> existingHourlyAlerts = hourlyAlertRepository.getCidOfExistingAlerts(shard, Arrays.asList(cid, cid2));
        assertThat("алерты с типом HOURLY не должен удалиться", existingHourlyAlerts, containsInAnyOrder(cid, cid2));
        Set<Long> existingCpaAlerts = cpaAlertRepository.getCidOfExistingAlerts(shard, Arrays.asList(cid, cid2));
        assertThat("алерты с типом CPA не должен удалиться", existingCpaAlerts, containsInAnyOrder(cid, cid2));
    }
}
