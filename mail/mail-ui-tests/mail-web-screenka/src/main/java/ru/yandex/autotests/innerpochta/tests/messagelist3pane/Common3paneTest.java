package ru.yandex.autotests.innerpochta.tests.messagelist3pane;

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

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Общие тесты 3пейн")
@Features(FeaturesConst.THREE_PANE)
@Tag(FeaturesConst.THREE_PANE)
@Stories(FeaturesConst.GENERAL)
@Description("Заготовлены письма и прошлый запрос subj")
public class Common3paneTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-vertical",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 3);
        stepsProd.user().apiLabelsSteps().pinLetter(stepsProd.user().apiMessagesSteps().getAllMessages().get(0));
    }

    @Test
    @Title("Должны видеть выпадушку языков")
    @TestCaseId("3162")
    public void shouldSeeLanguageSelectDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().leftPanelFooterLineBlock().languageSwitch3pane());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть плашку “Выбрано n писем“")
    @TestCaseId("3163")
    public void shouldSeeSelectedNMessagesPlank() {
        Consumer<InitStepsRule> actions = st ->
            st.user().messagesSteps().selectsAllDisplayedMessagesInFolder();

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть запиненное письмо")
    @TestCaseId("3164")
    public void shouldSeePinnedMessage() {
        Consumer<InitStepsRule> actions = st -> st.user().messagesSteps().selectMessageWithIndex(0);

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-horizontal",
            of(SETTINGS_PARAM_LAYOUT, SETTINGS_LAYOUT_3PANE_HORIZONTAL)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
