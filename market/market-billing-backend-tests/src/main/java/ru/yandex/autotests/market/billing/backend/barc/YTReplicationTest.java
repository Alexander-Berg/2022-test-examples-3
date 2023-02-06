package ru.yandex.autotests.market.billing.backend.barc;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsoleFactory;
import ru.yandex.autotests.market.billing.backend.core.dao.barc.TaskQueueDao;
import ru.yandex.autotests.market.billing.backend.core.dao.barc.beans.ReplicateEntityDefinition;
import ru.yandex.autotests.market.billing.backend.core.dao.yql.configuration.MbiDataArchiverYQLConfiguration;
import ru.yandex.autotests.market.billing.backend.core.dao.yt.configuration.MbiDataArchiverYTConfiguration;
import ru.yandex.autotests.market.billing.backend.steps.BarcDaoSteps;
import ru.yandex.autotests.market.billing.backend.steps.archiver.ArchivingSteps;
import ru.yandex.autotests.market.billing.backend.steps.archiver.ReplicationTestExecutionListener;
import ru.yandex.autotests.market.billing.util.ReactiveJUnitErrorRule;
import ru.yandex.autotests.market.common.spring.CommonSpringTest;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;
import static ru.yandex.autotests.market.billing.backend.steps.archiver.ReplicationTestExecutionListener.REPLICATE_EXECUTOR;

/**
 * @author Dmitriy Poluyanov <a href="mailto:neiwick@yandex-team.ru">Dmitriy Poluyanov</a>
 * @since 04.04.17
 */
@Aqua.Test(title = "Тест на работу джобы репликации replicateExecutor")
@Feature("barc")
@Issue("AUTOTESTMARKET-5889")
@CommonSpringTest(classes = {
        ArchivingSteps.class,
        BarcDaoSteps.class,
        MarketBillingConsoleFactory.class,
        MbiDataArchiverYTConfiguration.class,
        MbiDataArchiverYQLConfiguration.class,
        TaskQueueDao.class}
)
@ContextConfiguration({
        "classpath*:ru/yandex/autotests/market/billing/backend/barc/YTReplicationTestSupport.xml",
        "classpath:billingDatabaseConfiguration.xml"})
@TestExecutionListeners(listeners = ReplicationTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@RunWith(Parameterized.class)
public class YTReplicationTest {

    @ClassRule
    public static final ReactiveJUnitErrorRule ERROR_RULE = new ReactiveJUnitErrorRule();

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    @Parameterized.Parameter
    public String entityName;

    @Autowired
    private ArchivingSteps archivingSteps;

    private String tableName;

    @Parameterized.Parameters(name = "{index} - Тест репликации {0}")
    public static Collection<Object[]> data() {
        // load entity names for test parametrization
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                YTReplicationTest.class.getName()
                        .replace(".", "/") + "Support.xml")) {

            Map<String, ReplicateEntityDefinition> beans = context.getBeansOfType(ReplicateEntityDefinition.class);

            return beans.values()
                    .stream().map(e -> new Object[]{e.getEntityName()})
                    .collect(Collectors.toList());
        }
    }

    @Before
    public void setUp() {
        tableName = ReplicationTestExecutionListener.getCopiedTables().get(entityName);
    }

    @Test
    @Title("Джоба replicateExecutor отработала без ошибок")
    public void successReplicationJobTest() throws IOException {
        archivingSteps.checkArchiverJob(REPLICATE_EXECUTOR);
    }

    @Test
    @Title("Таблица для сущности {0} среплицировалась на кластер")
    public void tableReplicationTest() {
        archivingSteps.checkReplicatedTable(entityName, tableName);
    }

    @Test
    @Title("Аттрибуты таблицы для сущности {0} одинаковые (реплика<->мастер)")
    public void tableAttributesTest() {
        archivingSteps.checkAttributesReplication(entityName, tableName);
    }

    @Test
    @Title("Для сущности {0} есть данные на реплике")
    public void testDataExists() {
        long count = archivingSteps.getReplicaRowsCount(entityName, tableName);

        assertThat(count,
                describedAs("В таблице на реплике есть записи",
                        greaterThan(0L)));
    }

    @Test
    @Title("Для сущности {0} есть данные на реплике")
    public void testDataEquivalency() throws IOException {
        archivingSteps.compareReplicatedTable(entityName, tableName);
    }
}
