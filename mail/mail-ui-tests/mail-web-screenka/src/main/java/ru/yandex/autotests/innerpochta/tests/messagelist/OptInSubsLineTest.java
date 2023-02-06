package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.webcommon.rules.AccountsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME_COLORFUL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAMP_THEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Вертска полоски опт-ина в списке писем")
@Features({FeaturesConst.OPTIN, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.MESSAGE_LIST)
@RunWith(DataProviderRunner.class)
@Description("У юзера есть наразобранные новые рассылки")
public class OptInSubsLineTest {

    private ScreenRulesManager rules = screenRulesManager();
    private AccLockRule lock = rules.getLock().className();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static AccountsRule account = new AccountsRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams(
                "Меняем лэйаут",
                of(
                    SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                    COLOR_SCHEME, COLOR_SCHEME_COLORFUL
                )
            );
    }

    @Test
    @Title("Верстка полоски опт-ина")
    @TestCaseId("6324")
    @DataProvider({LAYOUT_2PANE, LAYOUT_3PANE_VERTICAL})
    public void shouldSeeOptInLine(String layout) {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().home().optInLine());

        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams("Меняем лэйаут", of(SETTINGS_PARAM_LAYOUT, layout));
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка полоски опт-ина в тёмной теме")
    @TestCaseId("6328")
    public void shouldSeeOptInLineDark() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().home().optInLine());

        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams("Меняем тему", of(COLOR_SCHEME, LAMP_THEME));
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на кнопку «Разобрать» в полоске опт-ина")
    @TestCaseId("6327")
    public void shouldHoverOnButtonSort() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().home().optInLine().sortBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на кнопку «Позже» в полоске опт-ина")
    @TestCaseId("6327")
    public void shouldHoverOnButtonLater() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().home().optInLine().laterBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на крестик в полоске опт-ина")
    @TestCaseId("6327")
    public void shouldHoverOnButtonClose() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().home().optInLine().closeBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
