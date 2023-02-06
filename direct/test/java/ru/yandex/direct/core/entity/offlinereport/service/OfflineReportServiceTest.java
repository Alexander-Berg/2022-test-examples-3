package ru.yandex.direct.core.entity.offlinereport.service;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.offlinereport.model.OfflineReport;
import ru.yandex.direct.core.entity.offlinereport.model.OfflineReportState;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.feature.FeatureName;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.OFFLINE_REPORT;
import static ru.yandex.direct.core.entity.offlinereport.model.OfflineReportType.AGENCY_KPI;
import static ru.yandex.direct.core.entity.offlinereport.model.OfflineReportType.DOMAINS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OfflineReportServiceTest {
    @Autowired
    private OfflineReportService service;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private DbQueueSteps dbQueueSteps;

    @Autowired
    private FeatureSteps featureSteps;

    private UserInfo defaultUser;
    private int shard;

    @Before
    public void setUp() throws Exception {
        defaultUser = userSteps.createDefaultUser();
        shard = defaultUser.getShard();
        dbQueueSteps.registerJobType(OFFLINE_REPORT);
        featureSteps.addClientFeature(defaultUser.getClientId(), FeatureName.AGENCY_KPI_OFFLINE_REPORT_ENABLED, true);
    }

    @Test
    public void getDomainsOfflineReports() {
        service.createOfflineReportWithMonthlyInterval(defaultUser.getUser(), null, DOMAINS, "201903", "201903");
        service.createOfflineReportWithMonthlyInterval(defaultUser.getUser(), null, DOMAINS, "201903", "201904");
        service.createOfflineReportWithMonthlyInterval(defaultUser.getUser(), null, DOMAINS, "201903", "201905");

        List<OfflineReport> result = service.getAgencyOfflineReports(defaultUser.getUser(), DOMAINS);

        assertThat(result.size(), is(3));
        assertThat(result.get(0).getReportState(), is(OfflineReportState.NEW));
        assertThat(result.get(0).getUid(), is(defaultUser.getUid()));
        assertThat(result.get(0).getReportType(), is(DOMAINS));
    }

    @Test
    public void createDomainsOfflineReport() {
        Long reportId = service.createOfflineReportWithMonthlyInterval(defaultUser.getUser(), null, DOMAINS, "201903",
                "201903").getReportId();

        assertThat("данные записались в базу", service.getOfflineReport(shard, reportId), is(notNullValue()));
    }

    @Test
    public void getAgencyKpiOfflineReports() {
        service.createOfflineReportWithDailyInterval(defaultUser.getUser(), null, AGENCY_KPI,
                LocalDate.of(2020, 12, 1), LocalDate.of(2021, 2, 28));
        service.createOfflineReportWithDailyInterval(defaultUser.getUser(), null, AGENCY_KPI,
                LocalDate.of(2021, 1, 15), LocalDate.of(2021, 3, 15));

        List<OfflineReport> result = service.getAgencyOfflineReports(defaultUser.getUser(), AGENCY_KPI);

        assertThat(result.size(), is(2));
        assertThat(result.get(0).getReportState(), is(OfflineReportState.NEW));
        assertThat(result.get(0).getUid(), is(defaultUser.getUid()));
        assertThat(result.get(0).getReportType(), is(AGENCY_KPI));
    }

    @Test
    public void createAgencyKpiOfflineReport() {
        Long reportId = service.createOfflineReportWithDailyInterval(defaultUser.getUser(), null, AGENCY_KPI,
                LocalDate.of(2020, 12, 1), LocalDate.of(2021, 2, 28)).getReportId();

        assertThat("данные записались в базу", service.getOfflineReport(shard, reportId), is(notNullValue()));
    }
}
