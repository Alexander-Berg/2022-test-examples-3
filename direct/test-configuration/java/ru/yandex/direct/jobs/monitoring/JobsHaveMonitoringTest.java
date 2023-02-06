package ru.yandex.direct.jobs.monitoring;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.ansiblejuggler.model.notifications.ChangeType;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.env.EnvironmentTypeProvider;
import ru.yandex.direct.juggler.JugglerStatus;
import ru.yandex.direct.juggler.check.annotation.JugglerCheck;
import ru.yandex.direct.juggler.check.annotation.OnChangeNotification;
import ru.yandex.direct.juggler.check.model.CheckTag;
import ru.yandex.direct.scheduler.Hourglass;
import ru.yandex.direct.scheduler.hourglass.HourglassJob;
import ru.yandex.direct.utils.Condition;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.juggler.check.model.CheckTag.DIRECT_PRIORITY_0;
import static ru.yandex.direct.juggler.check.model.CheckTag.DIRECT_PRIORITY_1;
import static ru.yandex.direct.juggler.check.model.CheckTag.DIRECT_PRODUCT_TEAM;

/**
 * Тест проверяет, что для каждой джобы включен мониторинг (джагглерный агрегат с нотификацией)
 * с помощью аннотации JugglerCheck и правильно указан ttl в мониторинге.
 * <p>
 * В список исключений добавлены старые джобы, которые не мониторятся.
 * <p>
 * Если вы написали новую джобу, и сработал тест {@link #jobHasMonitoring()},
 * значит вы не настроили должным образом мониторинг. Добавьте аннотацию JugglerCheck
 * для продакшена с нотификацией.
 * Если сработал тест {@link #jobHasProperTtl()},
 * значит вы неправильно настроили ttl в мониторинге
 * (указали ttl равный в точности 2 или 3 периодам запуска джобы)
 * Стоит увеличить ttl на небольшую дельту во избежание флапов.
 * <p>
 * Как написать документацию: https://docs.yandex-team.ru/direct-dev/guide/dev/jobs-operation-doc
 * Как настроить нотификацию: https://docs.yandex-team.ru/direct-dev/concepts/dev/jobs-monitoring
 */
@ContextConfiguration(classes = {MonitoringTestConfiguration.class})
@ExtendWith(SpringExtension.class)
public class JobsHaveMonitoringTest {
    private static final Set<String> EXCLUDED_JOBS = ImmutableSet.of(
            "AbandonedAgencyOfflineReportCleaner",
            "AgencyClientProveCleaner",
            "AgencyOfflineReportCleaner",
            "AgencyOfflineReportDbQueueCleaner",
            "AggregatedStatusesMonitorJob",
            "AggregatedStatusesProcessor",
            "BalanceInfoQueueCleaner",
            "BillingOrderDomainsSumCheckJob",
            "BsClearIdHistoryJob",
            "BsExportFeedsProcessor",
            "BsExportLogsLbToYtJob",
            "BsExportWorker",
            "CalcEstimateJob",
            "CalcFactorsJob",
            "CampAutoPriceQueueCleaner",
            "CampFinishedByDateWarningSender",
            "CampaignStatusEventsProcessor",
            "CampaignsForBroadmatchExporter",
            "ChunksGeneratorJob",
            "ClearCampDayBudgetStopHistoryJob",
            "ClearXlsJob",
            "ClickHouseCleanerJob",
            "ClientAvatarsDeleteJob",
            "ClientBrandsImportJob",
            "CpmGeoPinBannerModerationEventsProcessor",
            "CrmClientStatExporter",
            "ExportFilterDomainsToYtJob",
            "ExportTrustedRedirectsToYtJob",
            "FeaturesChangesLogJob",
            "FrontendTimingsCollectorJob",
            "HolidaySynchronizerJob",
            "HomeDirectDbResamplingJob",
            "ImportMisprintFixlistJob",
            "ImportPagesJob",
            "InternalBannerModerationEventsProcessor",
            "JobSendWarnClientDomains",
            "JobUpdateShopRating",
            "MobileUrlsExporter",
            "ModerationReadyObjectsMonitoring",
            "ModerationResponsesTimeMonitoringJob",
            "MonitorTargetsCleaner",
            "MysqlYtChecksumJob",
            "MysqlYtMetricsCollectorJob",
            "NeverCheckJob",
            "NewUsersDataCleaner",
            "NotShardedJob",
            "OperatorsGetModeratedJob",
            "OutdatedAutobudgetAlertsCleaner",
            "PpcDataExportJob",
            "RecalculateCampaignsStatusJob",
            "RemoveBannerImagesJob",
            "RequestsCountJob",
            "ResendBlockedDomainsJob",
            "SendOptimizeNotificationJob",
            "ShardedJob",
            "StopWordsDownloader",
            "StoredVarsCleaner",
            "SwitchmanStatCollector",
            "TransferGenocideLogCrossClusterJob",
            "TransferGenocideLogInsideClusterJob",
            "UnlinkTrackingPhoneJob",
            "UrlsStatusExporter",
            "VideoGoalOutdoorUpdateJob",
            "VideoStatXLSReport",
            "WarnplaceCleaner",
            "YaContextAllBannersExporter");

    private static final Set<CheckTag> TAGS_WITH_NOTIFICATIONS = ImmutableSet.of(DIRECT_PRODUCT_TEAM,
            DIRECT_PRIORITY_0, DIRECT_PRIORITY_1);

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private EnvironmentTypeProvider environmentTypeProvider;

    @Autowired
    private ResourcePatternResolver resourceResolver;

    @Autowired
    private List<HourglassJob> jobs;

    @Test
    void jobHasProperTtl() {
        List<String> jobsWithImproperTtl =
                StreamEx.of(jobs)
                        .filter(this::needMonitoring)
                        .filter(this::haveMonitoring)
                        .remove(this::isProperTtlSet)
                        .map(job -> job.getClass().getSimpleName())
                        .remove(EXCLUDED_JOBS::contains)
                        .sorted()
                        .toList();

        assertThat(jobsWithImproperTtl)
                .describedAs("Не рекомендуется использовать ttl равный в точности 2 или 3 периодам запускам джобы.\n " +
                        "Стоит увеличить ttl на небольшую дельту во избежание флапов (см доку https://docs" +
                        ".yandex-team.ru/direct-dev/guide/dev/jobs-monitoring#onejob-ttl)")
                .isEmpty();
    }

    @Test
    void jobHasMonitoring() {
        List<String> notMonitoredJobs =
                StreamEx.of(jobs)
                        .filter(this::needMonitoring)
                        .remove(this::isProperMonitoringSet)
                        .map(job -> job.getClass().getSimpleName())
                        .remove(EXCLUDED_JOBS::contains)
                        .sorted()
                        .toList();

        assertThat(notMonitoredJobs)
                .describedAs("Джобы без корректно настроенного мониторинга " +
                        "(см доку https://docs.yandex-team.ru/direct-dev/concepts/dev/jobs-monitoring")
                .isEmpty();
    }

    private Set<String> getDocumentedJobs() throws IOException {
        Set<String> jobs = new HashSet<>();
        Resource[] resources = resourceResolver.getResources("classpath:list/*.md");
        for (Resource r : resources) {
            if (!r.isFile()) {
                continue;
            }
            File file = r.getFile();
            String name = file.getName();
            jobs.add(name.replace(".md", ""));
        }
        return jobs;
    }

    @Test
    void jobHasOperationDoc() throws IOException {
        Set<String> documentedJobs = getDocumentedJobs();

        List<String> notDocumentedJobs =
                StreamEx.of(jobs)
                        .filter(this::needMonitoring)
                        .filter(this::isOperationDocRequired)
                        .map(job -> job.getClass().getSimpleName())
                        .remove(documentedJobs::contains)
                        .sorted()
                        .toList();

        assertThat(notDocumentedJobs)
                .describedAs("Джобы переданные в app-duty без документации " +
                        "(https://docs.yandex-team.ru/direct-dev/guide/dev/jobs-operation-doc)").
                isEmpty();
    }

    @Test
    void noExcessExceptions() {
        List<String> excess =
                StreamEx.of(jobs)
                        .filter(this::needMonitoring)
                        .filter(this::isProperMonitoringSet)
                        .map(job -> job.getClass().getSimpleName())
                        .filter(EXCLUDED_JOBS::contains)
                        .sorted()
                        .toList();

        assertThat(excess)
                .describedAs("Лишние исключения (мониторинг уже настроен)")
                .isEmpty();
    }

    @BeforeEach
    private void setUpEnvironmentTypeProvider() {
        ((MonitoringTestConfiguration.MutableEnvironmentTypeProvider) environmentTypeProvider).set(EnvironmentType.PRODUCTION);
    }

    private boolean isOperationDocRequired(HourglassJob job) {
        var mandatoryTags = Set.of(DIRECT_PRIORITY_0, DIRECT_PRIORITY_1);
        return StreamEx.of(getChecks(job)).flatMap(c -> Arrays.stream(c.tags())).anyMatch(mandatoryTags::contains);
    }

    private boolean needMonitoring(HourglassJob job) {
        if (List.of("NotifyClientCashbackJob","ImportCashbackRewardsDetailsJob").contains(job.getClass().getSimpleName())) {
            assertThat(StreamEx.of(getHourglasses(job)).anyMatch(this::isProductionHourglass)).isTrue();
        }
        return StreamEx.of(getHourglasses(job)).anyMatch(this::isProductionHourglass);
    }

    private boolean haveMonitoring(HourglassJob job) {
        return StreamEx.of(getChecks(job)).anyMatch(this::isProductionCheck);
    }

    private boolean isProperMonitoringSet(HourglassJob job) {
        List<JugglerCheck> check = getChecks(job);
        if (List.of("NotifyClientCashbackJob","ImportCashbackRewardsDetailsJob").contains(job.getClass().getSimpleName())) {
            assertThat(StreamEx.of(check).findAny(this::isMonitoredCheck).isPresent()).isTrue();
        }
        return StreamEx.of(check).findAny(this::isMonitoredCheck).isPresent();
    }

    private boolean isProperTtlSet(HourglassJob job) {
        Optional<JugglerCheck> check = getProductionCheck(job);
        Optional<Hourglass> hourglass = getProductionHourglass(job);
        return check.isPresent() && hourglass.isPresent() && !checkTtlEqualsSeveralPeriods(check.get(),
                hourglass.get());
    }

    private List<JugglerCheck> getChecks(HourglassJob job) {
        Class<?> jobClass = job.getClass();
        return asList(jobClass.getAnnotationsByType(JugglerCheck.class));
    }

    private Optional<JugglerCheck> getProductionCheck(HourglassJob job) {
        return StreamEx.of(getChecks(job)).filter(this::isProductionCheck).findAny();
    }

    private List<Hourglass> getHourglasses(HourglassJob job) {
        Class<?> jobClass = job.getClass();
        return asList(jobClass.getAnnotationsByType(Hourglass.class));
    }

    private Optional<Hourglass> getProductionHourglass(HourglassJob job) {
        return StreamEx.of(getHourglasses(job)).filter(this::isProductionHourglass).findAny();
    }

    private boolean isMonitoredCheck(JugglerCheck check) {
        return isProductionCheck(check) && checkHasValidNotifications(check);
    }

    private boolean isProductionCheck(JugglerCheck check) {
        Condition condition = applicationContext.getBean(check.needCheck());
        return condition.evaluate();
    }

    private boolean isProductionHourglass(Hourglass hourglass) {
        Condition condition = applicationContext.getBean(hourglass.needSchedule());
        return condition.evaluate();
    }

    /**
     * Проверяет, что аннотация JugglerCheck настроена таким образом,
     * что генерирует нотификацию.
     * <p>
     * Нотификация может быть либо настроена явно в аннотации,
     * либо с помощью одного из тегов, по которым настроена нотификация
     * вручную в джагглере.
     */
    private boolean checkHasValidNotifications(JugglerCheck check) {
        Set<CheckTag> tags = new HashSet<>(asList(check.tags()));
        if (!Sets.intersection(tags, TAGS_WITH_NOTIFICATIONS).isEmpty()) {
            return true;
        }

        for (OnChangeNotification notification : check.notifications()) {
            if (notification.recipient().length == 0) {
                continue;
            }
            if (notification.status().length == 0) {
                continue;
            }
            if (notification.type() != ChangeType.STATUS) {
                continue;
            }
            Set<JugglerStatus> statuses = new HashSet<>(asList(notification.status()));
            if (!statuses.contains(JugglerStatus.CRIT)) {
                continue;
            }

            return true;
        }

        return false;
    }

    /**
     * Проверяет, что ttl в аннотации JugglerCheck
     * равен 2 или 3 периодам запуска джобы из аннотации Hourglass.
     * <p>
     * Не делает проверку, если запуск джобы настроен через cron.
     */
    private boolean checkTtlEqualsSeveralPeriods(JugglerCheck check, Hourglass hourglass) {
        int ttl = getTtlInSeconds(check);
        int period = hourglass.periodInSeconds();
        if (period == Hourglass.PERIOD_NOT_SPECIFIED) {
            return false;
        }
        return ttl == period * 2 || ttl == period * 3;
    }

    private int getTtlInSeconds(JugglerCheck check) {
        JugglerCheck.Duration ttl = check.ttl();
        Duration duration =
                Duration.ofDays(ttl.days())
                        .plusHours(ttl.hours())
                        .plusMinutes(ttl.minutes())
                        .plusSeconds(ttl.seconds());
        return (int) duration.toSeconds();
    }
}
