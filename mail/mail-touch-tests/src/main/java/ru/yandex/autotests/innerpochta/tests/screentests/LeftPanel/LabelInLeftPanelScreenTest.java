package ru.yandex.autotests.innerpochta.tests.screentests.LeftPanel;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.util.Set;
import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на метки")
@Features(FeaturesConst.LEFT_PANEL)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class LabelInLeftPanelScreenTest {

    private static final String LONG_NAME = "1234567890123456789012345678901234567890";

    private static final Set<Coords> IGNORED_AREA = Sets.newHashSet(
        new Coords(500, 1950, 460, 60)
    );

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED_AREA);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Метка с длинным именем помещается в список папок")
    @TestCaseId("986")
    public void shouldSeeLongNameLabel() {
        Consumer<InitStepsRule> act = st -> st.user().defaultSteps()
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .shouldSee(st.pages().touch().sidebar().folderBlocks())
            .scrollTo(st.pages().touch().sidebar().sidebarPromo())
            .shouldSee(st.pages().touch().sidebar().labelsBlockSidebar());

        stepsProd.user().apiLabelsSteps().addNewLabel(LONG_NAME, LABELS_PARAM_GREEN_COLOR);
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Текущая метка выделяется в сайдбаре")
    @TestCaseId("1124")
    public void shouldSeeHighlightedLabel() {
        Consumer<InitStepsRule> act = st -> st.user().defaultSteps()
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .scrollTo(st.pages().touch().sidebar().sidebarPromo())
            .clicksOn(st.pages().touch().sidebar().labelsBlockSidebar().labels().get(0))
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .shouldSee(st.pages().touch().sidebar().labelsBlockSidebar());

        stepsProd.user().apiLabelsSteps().addNewLabel(Utils.getRandomName(), LABELS_PARAM_GREEN_COLOR);
        stepsProd.user().apiLabelsSteps().addNewLabel(Utils.getRandomName(), LABELS_PARAM_GREEN_COLOR);
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Пользователские метки не гаснут при выборе системной")
    @TestCaseId("1126")
    public void shouldNotDeactivateCustomLabels() {
        Consumer<InitStepsRule> act = st -> st.user().defaultSteps()
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .scrollTo(st.pages().touch().sidebar().sidebarPromo())
            .clicksOn(st.pages().touch().sidebar().systemLabelsBlock().get(0))
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .shouldSee(st.pages().touch().sidebar().labelsBlockSidebar());

        stepsProd.user().apiLabelsSteps().addNewLabel(Utils.getRandomName(), LABELS_PARAM_GREEN_COLOR);
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }
}
