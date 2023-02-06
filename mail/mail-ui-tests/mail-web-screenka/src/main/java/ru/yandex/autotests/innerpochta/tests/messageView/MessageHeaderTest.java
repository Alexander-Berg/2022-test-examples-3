package ru.yandex.autotests.innerpochta.tests.messageView;

import io.qameta.allure.junit4.Tag;
import org.apache.commons.lang3.StringUtils;
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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_OFF;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_TRANSLATE;

/**
 * @author a-zoshchuk
 */

@Aqua.Test
@Title("Шапка письма")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.MESSAGE_HEADER)
public class MessageHeaderTest {

    private static final String VERY_LONG_THEME = StringUtils.repeat("очень длинная тема", 20);
    private static final String SHORT_THEME = "второе письмо";
    private static final String LONG_BODY = StringUtils.repeat("a \n", 20);

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
            "Выключаем переводчик, включаем открытие письма в списке писем",
            of(
                SETTINGS_PARAM_TRANSLATE, STATUS_OFF,
                SETTINGS_OPEN_MSG_LIST, STATUS_TRUE
            )
        );
        stepsTest.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), SHORT_THEME, LONG_BODY);
        stepsTest.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), VERY_LONG_THEME, LONG_BODY);
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Проверяем разворачивание залипшей длинной темы")
    @TestCaseId("2329")
    public void shouldSeeLongThemeUnwrapped() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(VERY_LONG_THEME)
                .scrollDownPage();
            st.user().defaultSteps().onMouseHover(st.pages().mail().msgView().messageSubject())
                .shouldSee(st.pages().mail().msgView().quickMessageViewSubjectFull());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем полоску важности письма")
    @TestCaseId("2692")
    public void shouldSeeImportanceString() {
        stepsProd.user().apiLabelsSteps().markImportant(
            stepsProd.user().apiMessagesSteps().getAllMessages().get(1)
        );
        Consumer<InitStepsRule> actions = st -> st.user().messagesSteps().clicksOnMessageByNumber(1);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем сворачивание письма кликом в шапку")
    @TestCaseId("2693")
    public void shouldSeeWrappedMessage() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(1);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageHead().closeMessageField())
                .clicksOn(st.pages().mail().msgView().messageHead().closeMessageField())
                .shouldNotSee(st.pages().mail().msgView().messageTextBlock().text());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем нажатие на стрелку вверх в шапке письма")
    @TestCaseId("2694")
    public void shouldSeePreviousMsg() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(1);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().messageSubject().nextThread())
                .shouldSee(st.pages().mail().msgView().messageHead().messageRead());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем нажатие на стрелку вниз в шапке письма")
    @TestCaseId("2695")
    public void shouldSeeNextMsg() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps()
                .onMouseHover(st.pages().mail().msgView().messageTextBlock())
                .clicksOn(st.pages().mail().msgView().messageSubject().prevThread())
                .shouldSeeWithHover(st.pages().mail().msgView().messageHead().messageRead());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем нажатие на замочек")
    @TestCaseId("4442")
    public void shouldSeeMedalPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(1);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageHead().medal())
                .clicksOn(st.pages().mail().msgView().messageHead().medal())
                .shouldSee(st.pages().mail().msgView().medalPopup());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

}
