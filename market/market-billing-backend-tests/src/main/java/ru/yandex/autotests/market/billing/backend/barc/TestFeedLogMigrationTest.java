package ru.yandex.autotests.market.billing.backend.barc;

import java.io.IOException;
import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsoleFactory;
import ru.yandex.autotests.market.billing.backend.core.dao.barc.ImportIntervalDao;
import ru.yandex.autotests.market.billing.backend.core.dao.barc.TaskEventLogDao;
import ru.yandex.autotests.market.billing.backend.core.dao.barc.TaskQueueDao;
import ru.yandex.autotests.market.billing.backend.core.dao.barc.beans.MigrationBoundaryDefinition;
import ru.yandex.autotests.market.billing.backend.core.dao.barc.beans.MigrationJobDefinition;
import ru.yandex.autotests.market.billing.backend.core.dao.barc.beans.MigrationProcessDefinition;
import ru.yandex.autotests.market.billing.backend.core.dao.shops_web.configuration.ShopsWebDatasource;
import ru.yandex.autotests.market.billing.backend.core.dao.yql.YTFeedLogService;
import ru.yandex.autotests.market.billing.backend.core.dao.yql.configuration.MbiDataArchiverYQLConfiguration;
import ru.yandex.autotests.market.billing.backend.core.dao.yt.configuration.MbiDataArchiverYTConfiguration;
import ru.yandex.autotests.market.billing.backend.steps.BarcDaoSteps;
import ru.yandex.autotests.market.billing.backend.steps.BillingDaoSteps;
import ru.yandex.autotests.market.billing.backend.steps.archiver.ArchivingSteps;
import ru.yandex.autotests.market.billing.backend.steps.archiver.MigrationTestExecutionListener;
import ru.yandex.autotests.market.billing.util.ReactiveJUnitErrorRule;
import ru.yandex.autotests.market.common.spring.CommonSpringTest;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;


/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 9/2/15
 */
@Aqua.Test(title = "Тест на архивацию test_feed_log в YT")
@Feature("barc")
@Issue("AUTOTESTMARKET-6123")
@CommonSpringTest(classes = {
        ArchivingSteps.class, BarcDaoSteps.class, BillingDaoSteps.class,
        ImportIntervalDao.class, TaskQueueDao.class, TaskEventLogDao.class,
        MbiDataArchiverYQLConfiguration.class,
        MbiDataArchiverYTConfiguration.class,
        YTFeedLogService.class, MarketBillingConsoleFactory.class})
@ActiveProfiles(profiles = {"test", "migration"})
@ShopsWebDatasource
@ContextConfiguration({
        "classpath*:ru/yandex/autotests/market/billing/backend/barc/TestFeedLogMigrationTestSupport.xml",
        "classpath:billingDatabaseConfiguration.xml"})
@TestExecutionListeners(listeners = MigrationTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@RunWith(SpringRunner.class)
public class TestFeedLogMigrationTest {
    @ClassRule
    public static final ReactiveJUnitErrorRule ERROR_RULE = new ReactiveJUnitErrorRule();

    private static final LocalDateTime LAUNCH_DATE = LocalDateTime.now();
    @Autowired
    private ArchivingSteps archivingSteps;
    @Autowired
    private MigrationBoundaryDefinition boundaryDefinition;
    @Autowired
    private MigrationProcessDefinition migrationProcessDefinition;
    @Autowired
    private MigrationJobDefinition migrationJobDefinition;
    @Autowired
    private BarcDaoSteps barcDaoSteps;

    @Parameter("Граница архивации")
    private LocalDateTime boundaryDH;

    @Before
    public void setUp() {
        boundaryDH = boundaryDefinition.apply(LAUNCH_DATE);
    }

    @Test
    @Title("Джоба migrateFeedLogExecutor отработала без ошибок")
    public void testCorrectMigration() throws IOException {
        archivingSteps.checkArchiverJob(migrationJobDefinition.getJobName());
    }

    @Test
    @Title("Джоба привезла не пустую партицию")
    public void testDataExists() throws IOException {
        LocalDateTime partitionDate = boundaryDH.minus(boundaryDefinition.getPartitionsInterval());

        long count = barcDaoSteps.getYTTestFeedLogsCount(
                boundaryDefinition.getPartitionKeyFormatter(), partitionDate);

        assertThat(count,
                describedAs("Скопировали несколько строчек в YT",
                        greaterThan(0L)));
    }

    @Test
    @Title("Джоба скопировала все данные на нужную партицию")
    public void testEquivalency() throws IOException {
        archivingSteps.compareBillingAndYTArchivedTestFeedLog(
                boundaryDefinition.getPartitionKeyFormatter(),
                boundaryDH.minus(boundaryDefinition.getPartitionsInterval()),
                boundaryDH.minusNanos(1));
    }

    @Test
    @Title("Запланирована задача на удаление заархивированной партиции из Oracle")
    public void testDeletionPrepared() throws IOException {
        LocalDateTime archivedDate = boundaryDH.minus(boundaryDefinition.getPartitionsInterval());
        boolean taskExists = barcDaoSteps.existsDeletionTask(
                migrationJobDefinition.getYqlEntityName(),
                archivedDate);

        assertThat(taskExists,
                describedAs("Запланировано удаление сущности %0 на дату %1",
                        is(true), migrationJobDefinition.getYqlEntityName(), archivedDate));
    }

    @Test
    @Title("Запланирована задача на репликацию заархивированной партиции в YT кластере")
    public void testReplicationPrepared() throws IOException {
        LocalDateTime archivedDate = boundaryDH.minus(boundaryDefinition.getPartitionsInterval());
        boolean taskExists = barcDaoSteps.existsReplicationTask(
                migrationJobDefinition.getYqlEntityName(),
                archivedDate, LAUNCH_DATE);

        assertThat(taskExists,
                describedAs("Запланирована репликация сущности %0 за дату %1",
                        is(true), migrationJobDefinition.getYqlEntityName(), archivedDate));
    }
}
