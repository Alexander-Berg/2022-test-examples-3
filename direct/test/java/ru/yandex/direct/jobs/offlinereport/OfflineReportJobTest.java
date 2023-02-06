package ru.yandex.direct.jobs.offlinereport;

import java.time.LocalDate;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.offlinereport.model.OfflineReport;
import ru.yandex.direct.core.entity.offlinereport.model.OfflineReportJobParams;
import ru.yandex.direct.core.entity.offlinereport.model.OfflineReportType;
import ru.yandex.direct.core.entity.offlinereport.service.OfflineReportService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.offlinereport.model.OfflineReportHeader;
import ru.yandex.direct.jobs.offlinereport.model.agencykpi.AgencyKpiOfflineReportType;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.ytwrapper.client.YtProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.common.db.PpcPropertyNames.AGENCY_KPI_OFFLINE_REPORT_AGGREGATOR_AGENCY_IDS;
import static ru.yandex.direct.common.db.PpcPropertyNames.AGENCY_KPI_OFFLINE_REPORT_PREMIUM_AGENCY_IDS;
import static ru.yandex.direct.rbac.RbacRole.MANAGER;

@ExtendWith(SpringExtension.class)
@JobsTest
public class OfflineReportJobTest {
    private static final int SHARD = 2;
    private static final long REPORT_ID = 1L;

    private static final Long AGENCY_ID = 350487L;
    private static final String AGENCY_NAME = "ArrowMedia";
    private static final Client AGENCY_CLIENT = new Client()
            .withId(AGENCY_ID)
            .withName(AGENCY_NAME);

    private static final Long UID = 1L;
    private static final String USER_LOGIN = "testLogin";
    private static final User USER = new User()
            .withUid(UID)
            .withAgencyClientId(AGENCY_ID)
            .withLogin(USER_LOGIN)
            .withRole(MANAGER);

    private static final LocalDate AGENCY_KPI_PERIOD_START = LocalDate.of(2022, 1, 1);
    private static final LocalDate AGENCY_KPI_PERIOD_END = LocalDate.of(2022, 1, 31);
    private static final String DOMAIN_DATE_FROM = "201903";
    private static final String DOMAIN_DATE_TO = "201905";

    @Autowired
    private YtProvider ytProvider;

    @Autowired
    DirectConfig directConfig;

    private OfflineReportJob offlineReportJob;

    private PpcProperty<Set<Long>> mockedPropertyPremium;
    private PpcProperty<Set<Long>> mockedPropertyAggregator;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        UserService userService = mock(UserService.class);
        RbacService rbacService = mock(RbacService.class);
        OfflineReportService offlineReportService = mock(OfflineReportService.class);
        ClientService clientService = mock(ClientService.class);
        PpcPropertiesSupport ppcPropertiesSupport = mock(PpcPropertiesSupport.class);

        when(offlineReportService.getOfflineReport(SHARD, REPORT_ID)).thenReturn(new OfflineReport().withUid(UID));
        when(userService.getUser(UID)).thenReturn(USER);
        when(clientService.getClient(any())).thenReturn(AGENCY_CLIENT);
        mockedPropertyPremium = mock(PpcProperty.class);
        when(ppcPropertiesSupport.get(AGENCY_KPI_OFFLINE_REPORT_PREMIUM_AGENCY_IDS)).thenReturn(mockedPropertyPremium);
        mockedPropertyAggregator = mock(PpcProperty.class);
        when(ppcPropertiesSupport.get(AGENCY_KPI_OFFLINE_REPORT_AGGREGATOR_AGENCY_IDS))
                .thenReturn(mockedPropertyAggregator);

        offlineReportJob = new OfflineReportJob(SHARD, null, offlineReportService, userService, rbacService,
                clientService, directConfig, new OfflineReportJobFileUploaderTestImpl(), ytProvider,
                ppcPropertiesSupport);
    }

    @Test
    void testGetHeader_AgencyKpiReport_Premium() {
        when(mockedPropertyPremium.getOrDefault(Set.of())).thenReturn(Set.of(AGENCY_ID));
        var agencyKpiJobParams = new OfflineReportJobParams(REPORT_ID, OfflineReportType.AGENCY_KPI, AGENCY_ID,
                AGENCY_KPI_PERIOD_START, AGENCY_KPI_PERIOD_END);
        var offlineReportJobHeader = offlineReportJob.getHeader(agencyKpiJobParams);
        checkOfflineReportJobHeader(offlineReportJobHeader, OfflineReportType.AGENCY_KPI, "2022-01-01", "2022-01-31",
                AgencyKpiOfflineReportType.PREMIUM);
    }

    @Test
    void testGetHeader_AgencyKpiReport_Aggregator() {
        when(mockedPropertyPremium.getOrDefault(Set.of())).thenReturn(Set.of());
        when(mockedPropertyAggregator.getOrDefault(Set.of())).thenReturn(Set.of(AGENCY_ID));
        var agencyKpiJobParams = new OfflineReportJobParams(REPORT_ID, OfflineReportType.AGENCY_KPI, AGENCY_ID,
                AGENCY_KPI_PERIOD_START, AGENCY_KPI_PERIOD_END);
        var offlineReportJobHeader = offlineReportJob.getHeader(agencyKpiJobParams);
        checkOfflineReportJobHeader(offlineReportJobHeader, OfflineReportType.AGENCY_KPI, "2022-01-01", "2022-01-31",
                AgencyKpiOfflineReportType.AGGREGATOR);
    }

    @Test
    void testGetHeader_AgencyKpiReport_Base() {
        when(mockedPropertyPremium.getOrDefault(Set.of())).thenReturn(Set.of());
        when(mockedPropertyAggregator.getOrDefault(Set.of())).thenReturn(Set.of());
        var agencyKpiJobParams = new OfflineReportJobParams(REPORT_ID, OfflineReportType.AGENCY_KPI, AGENCY_ID,
                AGENCY_KPI_PERIOD_START, AGENCY_KPI_PERIOD_END);
        var offlineReportJobHeader = offlineReportJob.getHeader(agencyKpiJobParams);
        checkOfflineReportJobHeader(offlineReportJobHeader, OfflineReportType.AGENCY_KPI, "2022-01-01", "2022-01-31",
                AgencyKpiOfflineReportType.BASE);
    }

    @Test
    void testGetHeader_DomainReport() {
        var domainJobParams = new OfflineReportJobParams(REPORT_ID, OfflineReportType.DOMAINS, AGENCY_ID,
                DOMAIN_DATE_FROM, DOMAIN_DATE_TO);
        var offlineReportJobHeader = offlineReportJob.getHeader(domainJobParams);
        checkOfflineReportJobHeader(offlineReportJobHeader, OfflineReportType.DOMAINS, DOMAIN_DATE_FROM,
                DOMAIN_DATE_TO, null);
    }

    private void checkOfflineReportJobHeader(OfflineReportHeader offlineReportJobHeader,
                                             OfflineReportType reportType,
                                             String stringFrom, String stringTo,
                                             AgencyKpiOfflineReportType agencyKpiOfflineReportType) {
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(offlineReportJobHeader.getAgencyId()).isEqualTo(AGENCY_ID);
            soft.assertThat(offlineReportJobHeader.getAgencyName()).isEqualTo(AGENCY_NAME);
            soft.assertThat(offlineReportJobHeader.getStringFrom()).isEqualTo(stringFrom);
            soft.assertThat(offlineReportJobHeader.getStringTo()).isEqualTo(stringTo);
            soft.assertThat(offlineReportJobHeader.getLogin()).isEqualTo(USER_LOGIN);
            soft.assertThat(offlineReportJobHeader.getReportType()).isEqualTo(reportType);
            soft.assertThat(offlineReportJobHeader.getAgencyKpiReportType()).isEqualTo(agencyKpiOfflineReportType);
        });
    }
}
