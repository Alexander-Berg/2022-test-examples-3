package ru.yandex.autotests.innerpochta.tests.messagelist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TIMELINE_COLLAPSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TIMELINE_ENABLE;

/**
 * @author yaroslavna
 */
@Aqua.Test
@Title("Тесты на таймлайн")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TIMELINE)
@RunWith(Parameterized.class)
public class TimelineTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @Parameterized.Parameter
    public String urlPath;

    @Parameterized.Parameters(name = "url: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
            {"?theme=lamp"},
            {"?theme=colorful"},
        });
    }

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams(
                "Включаем и разворачиваем таймлайн",
                of(
                    TIMELINE_ENABLE, TRUE,
                    TIMELINE_COLLAPSE, TRUE
                )
            );
    }

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Переходим по урлам c темной и светлой темой")
    @TestCaseId("3937")
    public void shouldSeeTimeline() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().home().timelineBlock());

        parallelRun.withActions(actions).withUrlPath(urlPath).withAcc(lock.firstAcc()).run();
    }

}
