package ru.yandex.direct.jobs.offlinereport;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.offlinereport.model.OfflineReport;
import ru.yandex.direct.core.entity.offlinereport.model.OfflineReportJobParams;
import ru.yandex.direct.core.entity.offlinereport.model.OfflineReportJobResult;
import ru.yandex.direct.core.entity.offlinereport.model.OfflineReportType;
import ru.yandex.direct.core.entity.offlinereport.service.OfflineReportService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.dbqueue.model.DbQueueJob;
import ru.yandex.direct.jobs.configuration.JobsEssentialConfiguration;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.ytwrapper.client.YtProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.common.db.PpcPropertyNames.AGENCY_KPI_OFFLINE_REPORT_AGGREGATOR_AGENCY_IDS;
import static ru.yandex.direct.common.db.PpcPropertyNames.AGENCY_KPI_OFFLINE_REPORT_PREMIUM_AGENCY_IDS;
import static ru.yandex.direct.common.db.PpcPropertyNames.OFFLINE_REPORT_JOB_GRAB_DURATION_MINUTES;
import static ru.yandex.direct.common.db.PpcPropertyNames.OFFLINE_REPORT_JOB_MAX_ATTEMPTS;
import static ru.yandex.direct.jobs.offlinereport.OfflineReportJob.DEFAULT_OFFLINE_REPORT_JOB_GRAB_DURATION_MINUTES;
import static ru.yandex.direct.jobs.offlinereport.OfflineReportJob.DEFAULT_OFFLINE_REPORT_JOB_MAX_ATTEMPTS;
import static ru.yandex.direct.rbac.RbacRole.MANAGER;

/**
 * Для запуска требуется наличие ~/.yt/yql_token - найти его можно по 1 пункту инструкции
 * https://wiki.yandex-team.ru/april/userdoc/settings/yql-token/ .
 * <p>
 * За выполнением запросов можно следить на https://yql.yandex-team.ru
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        JobsEssentialConfiguration.class,
})
@Disabled("Для запуска вручную")
class OfflineReportJobManualTest {
    private static final int SHARD = 2;
    private static final long REPORT_ID = 1L;
    private static final Long UID = 1L;
    private static final LocalDate PERIOD_START = LocalDate.of(2022, 2, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2022, 2, 28);
    private static final Long PREMIUM_AGENCY_ID = 55440672L;
    private static final Long AGGREGATOR_AGENCY_ID = 472729L;
    private static final Long BASE_AGENCY_ID = 75377819L;

    @Autowired
    private YtProvider ytProvider;
    @Autowired
    DirectConfig directConfig;

    private PpcProperty<Set<Long>> mockedPropertyPremium;
    private PpcProperty<Set<Long>> mockedPropertyAggregator;

    private OfflineReportJob job;

    @BeforeEach
    void setUp() {
        var userService = mock(UserService.class);
        var rbacService = mock(RbacService.class);
        var ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        var offlineReportService = mock(OfflineReportService.class);
        var clientService = mock(ClientService.class);

        var user = new User()
                .withUid(UID)
                .withRole(MANAGER); // чтобы отобразились все клиенты агентства

        when(userService.getUser(UID)).thenReturn(user);
        when(offlineReportService.getOfflineReport(SHARD, REPORT_ID)).thenReturn(new OfflineReport().withUid(UID));
        when(clientService.getClient(any())).thenReturn(new Client()
                .withName("Тестовое название - будет актуальным в продакшене"));

        mockedPropertyPremium = mock(PpcProperty.class);
        when(ppcPropertiesSupport.get(AGENCY_KPI_OFFLINE_REPORT_PREMIUM_AGENCY_IDS)).thenReturn(mockedPropertyPremium);
        mockedPropertyAggregator = mock(PpcProperty.class);
        when(ppcPropertiesSupport.get(AGENCY_KPI_OFFLINE_REPORT_AGGREGATOR_AGENCY_IDS))
                .thenReturn(mockedPropertyAggregator);

        var mockedPropertyDuration = mock(PpcProperty.class);
        when(ppcPropertiesSupport.get(OFFLINE_REPORT_JOB_GRAB_DURATION_MINUTES)).thenReturn(mockedPropertyDuration);
        when(mockedPropertyDuration.getOrDefault(DEFAULT_OFFLINE_REPORT_JOB_GRAB_DURATION_MINUTES))
                .thenReturn(Set.of(DEFAULT_OFFLINE_REPORT_JOB_GRAB_DURATION_MINUTES));
        var mockedPropertyAttempts = mock(PpcProperty.class);
        when(ppcPropertiesSupport.get(OFFLINE_REPORT_JOB_MAX_ATTEMPTS)).thenReturn(mockedPropertyAttempts);
        when(mockedPropertyAttempts.getOrDefault(DEFAULT_OFFLINE_REPORT_JOB_MAX_ATTEMPTS))
                .thenReturn(Set.of(DEFAULT_OFFLINE_REPORT_JOB_MAX_ATTEMPTS));

        job = new OfflineReportJob(SHARD, null, offlineReportService, userService, rbacService, clientService,
                directConfig, new OfflineReportJobFileUploaderTestImpl(), ytProvider, ppcPropertiesSupport);
    }

    @Test
    @Disabled("Для запуска вручную отчета по доменам")
    void testJobWithDomainReport() {
        DbQueueJob<OfflineReportJobParams, OfflineReportJobResult> jobInfo = new DbQueueJob()
                .withArgs(new OfflineReportJobParams(REPORT_ID,
                        OfflineReportType.DOMAINS, 350487L,
                        "201903", "201903"))
                .withUid(UID)
                .withTryCount(1L)
                .withId(1L);

        job.processJob(jobInfo);
    }

    @Test
    @Disabled("Для запуска вручную отчета по квартальным KPI - вариант PREMIUM")
    void testJobWithAgencyKpiReport_Premium() {
        when(mockedPropertyPremium.getOrDefault(Set.of())).thenReturn(Set.of(PREMIUM_AGENCY_ID));
        DbQueueJob<OfflineReportJobParams, OfflineReportJobResult> jobInfo = new DbQueueJob()
                .withArgs(offlineReportJobParams(PREMIUM_AGENCY_ID))
                .withUid(UID)
                .withTryCount(1L)
                .withId(1L);

        job.processJob(jobInfo);
    }

    @Test
    @Disabled("Для запуска вручную отчета по квартальным KPI - вариант AGGREGATOR")
    void testJobWithAgencyKpiReport_Aggregator() {
        when(mockedPropertyPremium.getOrDefault(Set.of())).thenReturn(Set.of());
        when(mockedPropertyAggregator.getOrDefault(Set.of())).thenReturn(Set.of(AGGREGATOR_AGENCY_ID));
        DbQueueJob<OfflineReportJobParams, OfflineReportJobResult> jobInfo = new DbQueueJob()
                .withArgs(offlineReportJobParams(AGGREGATOR_AGENCY_ID))
                .withUid(UID)
                .withTryCount(1L)
                .withId(1L);

        job.processJob(jobInfo);
    }

    @Test
    @Disabled("Для запуска вручную отчета по квартальным KPI - вариант BASE")
    void testJobWithAgencyKpiReport_Base() {
        when(mockedPropertyPremium.getOrDefault(Set.of())).thenReturn(Set.of());
        when(mockedPropertyAggregator.getOrDefault(Set.of())).thenReturn(Set.of());
        DbQueueJob<OfflineReportJobParams, OfflineReportJobResult> jobInfo = new DbQueueJob()
                .withArgs(offlineReportJobParams(BASE_AGENCY_ID))
                .withUid(UID)
                .withTryCount(1L)
                .withId(1L);

        job.processJob(jobInfo);
    }

    private OfflineReportJobParams offlineReportJobParams(Long agencyId) {
        return new OfflineReportJobParams(REPORT_ID, OfflineReportType.AGENCY_KPI, agencyId,
                PERIOD_START, PERIOD_END);
    }
}
