package ru.yandex.autotests.innerpochta.tests.messageView;

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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_OFF;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_ENABLE_QUOTING;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_ENABLE_RICHEDIT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_AVATARS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_TRANSLATE;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Общие тесты на просмотр письма")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@RunWith(DataProviderRunner.class)
@Stories(FeaturesConst.GENERAL)
public class GeneralTest {

    private static final int TREAD_SIZE = 3;
    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    @Rule
    public RuleChain chain = rules.createRuleChain();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);
    private AccLockRule lock = rules.getLock().useTusAccount();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams(
                "Выключаем цитирование и оформление письма, включаем открытие письма в списке писем",
                of(
                    SETTINGS_ENABLE_QUOTING, STATUS_OFF,
                    SETTINGS_ENABLE_RICHEDIT, STATUS_FALSE,
                    SETTINGS_PARAM_TRANSLATE, STATUS_OFF,
                    SETTINGS_OPEN_MSG_LIST, STATUS_TRUE
                )
            );
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(
            lock.firstAcc(),
            getRandomString(),
            getRandomString()
        );
        stepsProd.user().apiMessagesSteps().sendThread(lock.firstAcc(), getRandomString(), TREAD_SIZE);
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomString(), getRandomString());
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Должны видеть QR")
    @TestCaseId("2699")
    public void shouldSeeQuickReply() {
        String text = getRandomString();
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(1);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().quickReplyPlaceholder())
                .inputsTextInElement(st.pages().mail().msgView().quickReply().replyText(), text);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны перейти из QR в композ")
    @TestCaseId("2700")
    public void shouldSeeCorrectTextInCompose() {
        String text = getRandomString();
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().quickReplyPlaceholder())
                .inputsTextInElement(st.pages().mail().msgView().quickReply().replyText(), text)
                .clicksOn(st.pages().mail().msgView().quickReply().openCompose())
                .shouldSee(st.pages().mail().composePopup().expandedPopup());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Помечаем письмо непрочитанным через прыщ")
    @TestCaseId("2701")
    public void shouldSeeUnreadMessage() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().refreshPage();
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps()
                .onMouseHoverAndClick(st.pages().mail().msgView().messageHead().messageRead())
                .shouldSee(st.pages().mail().msgView().messageHead().messageUnread());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Просмотр письма и развернутого треда в списке писем")
    @Description("Разворчиваем тред в списке писем и открываем следующее за ним письмо, DARIA-65161")
    @TestCaseId("5251")
    public void shouldViewMessageInList() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем аватарки",
            of(
                SETTINGS_PARAM_MESSAGE_AVATARS, FALSE
            )
        );
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().displayedMessages().list().get(1).expandThread());
            st.user().messagesSteps().clicksOnMessageByNumber(TREAD_SIZE + 1);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageHead());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
