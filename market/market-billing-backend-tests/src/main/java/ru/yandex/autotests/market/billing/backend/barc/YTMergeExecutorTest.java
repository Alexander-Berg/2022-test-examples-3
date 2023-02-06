package ru.yandex.autotests.market.billing.backend.barc;

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
import ru.yandex.autotests.market.billing.backend.core.dao.barc.beans.MergeEntityDefinition;
import ru.yandex.autotests.market.billing.backend.core.dao.yql.configuration.MbiDataArchiverYQLConfiguration;
import ru.yandex.autotests.market.billing.backend.core.dao.yt.YtClient;
import ru.yandex.autotests.market.billing.backend.core.dao.yt.configuration.MbiDataArchiverYTConfiguration;
import ru.yandex.autotests.market.billing.backend.steps.BarcDaoSteps;
import ru.yandex.autotests.market.billing.backend.steps.archiver.ArchivingSteps;
import ru.yandex.autotests.market.billing.backend.steps.archiver.MergeTestExecutionListener;
import ru.yandex.autotests.market.billing.util.ReactiveJUnitErrorRule;
import ru.yandex.autotests.market.common.spring.CommonSpringTest;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;
import static ru.yandex.autotests.market.billing.backend.steps.archiver.MergeTestExecutionListener.MERGE_EXECUTOR;

/**
 * @author Dmitriy Poluyanov <a href="mailto:neiwick@yandex-team.ru">Dmitriy Poluyanov</a>
 * @since 04.05.17
 */
@Aqua.Test(title = "Тест на работу джобы мержа mergeExecutor")
@Feature("barc")
@Issue("AUTOTESTMARKET-6106")
@CommonSpringTest(classes = {
        ArchivingSteps.class,
        BarcDaoSteps.class,
        MarketBillingConsoleFactory.class,
        MbiDataArchiverYTConfiguration.class,
        MbiDataArchiverYQLConfiguration.class,
        TaskQueueDao.class
})
@ContextConfiguration({
        "classpath:billingDatabaseConfiguration.xml",
        "classpath*:ru/yandex/autotests/market/billing/backend/barc/YTMergeExecutorTestSupport.xml"})
@TestExecutionListeners(listeners = MergeTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@RunWith(Parameterized.class)
public class YTMergeExecutorTest {

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
    @Autowired
    private YtClient ytClient;

    @Parameterized.Parameters(name = "{index} - Тест репликации {0}")
    public static Collection<Object[]> data() {
        // load entity names for test parametrization
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                YTMergeExecutorTest.class.getName()
                        .replace(".", "/") + "Support.xml")) {

            Map<String, MergeEntityDefinition> beans = context.getBeansOfType(MergeEntityDefinition.class);

            return beans.values()
                    .stream().map(e -> new Object[]{e.getEntityName()})
                    .collect(Collectors.toList());
        }
    }

    @Test
    @Title("Джоба mergeExecutor отработала без ошибок")
    public void successReplicationJobTest() throws IOException {
        archivingSteps.checkArchiverJob(MERGE_EXECUTOR);
    }

    @Test
    @Title("Джоба удалила партиции сущности {0} которые были смержены")
    public void testDeleteMergedPaths() {
        List<String> inputs = MergeTestExecutionListener.getMergeInfos().get(entityName).getT1();

        for (String input : inputs) {
            assertThat(ytClient.exists(entityName, input))
                    .describedAs("В YT нет таблицы для сущности [" + entityName + "]" +
                            " на дату [" + input + "]")
                    .isFalse();
        }
    }
}
