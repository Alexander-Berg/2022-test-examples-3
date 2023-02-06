package ru.yandex.autotests.innerpochta.tests.screentests.grid;

import com.google.common.collect.Sets;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.util.Set;
import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.cal.rules.AddLayerIfNeedRule.addLayerIfNeed;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.LAYER_SETTING;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Настройки календаря")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.SETTINGS_PAGE)
public class SettingsTest {

    private static final Set<Coords> IGNORED = Sets.newHashSet(
        new Coords(0, 60, 170, 200), // скачет мини-календарь немного
        new Coords(170, 60, 58, 800), // полоса времени, текущее время постоянно меняется
        new Coords(271, 234, 340, 188) // разные ссылки для теста и прода
    );

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED).withClosePromo();

    private String layerID;

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(addLayerIfNeed(() -> stepsTest.user()));

    @Before
    public void prepare() {
        layerID = stepsTest.user().apiCalSettingsSteps().getUserLayers().get(0).getId().toString();
    }

    @Test
    @Title("Открываем экспорт")
    @TestCaseId("411")
    public void shouldOpenExportTab() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().settings().tabExport())
            .shouldSee(st.pages().cal().home().settings());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(LAYER_SETTING.makeUrlPart(layerID)).run();
    }

    @Test
    @Title("Открываем саджест контактов")
    @Description("Юзеру добавлено много контактов")
    @TestCaseId("528")
    public void shouldSeeContactsSuggest() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().settings().tabAccess())
            .clicksOn(st.pages().cal().home().settings().inputContact())
            .inputsTextInElement(st.pages().cal().home().settings().inputContact(), "test")
            .shouldSee(st.pages().cal().home().suggest())
            .onMouseHover(st.pages().cal().home().suggestItem().get(1));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(LAYER_SETTING.makeUrlPart(layerID)).run();
    }

    @Test
    @Title("Добавляем доступ для контактов")
    @Description("Юзеру добавлено много контактов")
    @TestCaseId("528")
    public void shouldSeeContactsWithAccess() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().settings().tabAccess())
            .clicksOn(st.pages().cal().home().settings().inputContact())
            .inputsTextInElement(st.pages().cal().home().settings().inputContact(), "test")
            .clicksOn(st.pages().cal().home().suggestItem().get(0))
            .inputsTextInElement(st.pages().cal().home().settings().inputContact(), "test")
            .clicksOn(st.pages().cal().home().suggestItem().get(1));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(LAYER_SETTING.makeUrlPart(layerID)).run();
    }
}
