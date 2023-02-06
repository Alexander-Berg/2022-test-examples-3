package ru.yandex.autotests.innerpochta.tests.messagelist3pane;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.apache.commons.lang3.StringUtils;
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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableSortedMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.MailConst.FORWARD_PREFIX;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_OFF;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_TRANSLATE;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Тулбар в  3pane")
@Features(FeaturesConst.THREE_PANE)
@Tag(FeaturesConst.THREE_PANE)
@Stories(FeaturesConst.TOOLBAR)
@RunWith(DataProviderRunner.class)
public class Toolbar3paneTest {

    private static final String LONG_BODY = StringUtils.repeat("f \n", 50);
    private static final String SCRIPT_FOR_SCROLLDOWN = "$('.js-layout-second-pane').scrollTop(1000)";

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
    public RuleChain chain = rules.createRuleChain()
        .around(removeAllMessages(() -> stepsProd.user(), INBOX, TRASH));

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем вертикальный 3-пейн", of(
                SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL
            )
        );
        stepsProd.user().apiMessagesSteps().markLetterRead(
            stepsProd.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomString(), LONG_BODY)
        );
        stepsProd.user().apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc(), Utils.getRandomString(), Utils.getRandomString());
    }

    @Test
    @Title("Нажимаем на «Написать»")
    @TestCaseId("3119")
    public void shouldOpenBlankCompose() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на «Переслать»")
    @TestCaseId("3120")
    public void shouldOpenComposeWithForwardMessage() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageCheckBox();
            st.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().forwardMessageButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .shouldContainValue(st.pages().mail().composePopup().expandedPopup().sbjInput(), FORWARD_PREFIX);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем дропдаун настройки ПК")
    @TestCaseId("3121")
    public void shouldOpenToolbarSettings() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().home().toolbar())
                .onMouseHoverAndClick(st.pages().mail().home().toolbar().configureCustomButtons())
                .shouldNotSee(st.pages().mail().home().toolbar().configureCustomButtons());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем выпадушку «Вид»")
    @TestCaseId("3123")
    public void shouldOpenViewChangeDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().layoutSwitchBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть залипший тулбар письма")
    @TestCaseId("3160")
    public void shouldSeePinnedToolbar() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем бажный переводчик",
            of(SETTINGS_PARAM_TRANSLATE, STATUS_OFF)
        );
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(1);
            scrollMessageDown(st);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Отскролливаем письмо вниз")
    private void scrollMessageDown(InitStepsRule steps) {
        steps.user().defaultSteps().executesJavaScript(SCRIPT_FOR_SCROLLDOWN);
    }
}
