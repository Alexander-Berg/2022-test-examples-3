package ru.yandex.autotests.innerpochta.tests.messageView;

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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.FORWARD_PREFIX;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Просмотр тредных писем")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.THREAD)
public class ThreadsTest {

    private static final int THREAD_SIZE = 4;
    private static final String SUBJECT = Utils.getRandomString();

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
            "Включаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_TRUE)
        );
        stepsProd.user().apiMessagesSteps().sendThread(lock.firstAcc(), SUBJECT, THREAD_SIZE);
        stepsProd.user().apiMessagesSteps()
            .markAllMsgRead()
            .prepareDraftToThread("", SUBJECT, Utils.getRandomName());
    }

    @Test
    @Title("Проверяем просмотр треда в списке писем")
    @TestCaseId("2680")
    public void shouldSeeThreadInMessageList() {
        Consumer<InitStepsRule> actions = this::openFirstMessageInMessageList;

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем открытие общего тулбара треда")
    @TestCaseId("2731")
    public void shouldSeeThreadToolbar() {
        Consumer<InitStepsRule> actions = st -> {
            openFirstMessageInMessageList(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().messageSubject().threadToolbarButton());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем пересылку треда через общий тулбар треда")
    @TestCaseId("3287")
    public void shouldSeeThreadForwardInCompose() {
        Consumer<InitStepsRule> actions = st -> {
            openFirstMessageInMessageList(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().messageSubject().threadToolbarButton())
                .clicksOn(st.pages().mail().msgView().commonToolbar().forwardButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .shouldContainValue(st.pages().mail().composePopup().expandedPopup().sbjInput(), FORWARD_PREFIX);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем черновик в треде на просмотр")
    @TestCaseId("2733")
    public void shouldSeeDraftInMessageView() {
        Consumer<InitStepsRule> actions = st -> {
            openFirstMessageWithWrapping(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().msgInThread().get(0))
                .shouldSee(st.pages().mail().msgView().messageTextBlock().text());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверям переход в композ после нажатия на кнопку “Дописать“")
    @TestCaseId("2734")
    public void shouldSeeDraftInCompose() {
        Consumer<InitStepsRule> actions = st -> {
            openFirstMessageWithWrapping(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().msgInThread().get(0))
                .clicksOn(st.pages().mail().msgView().contentToolbarBlock().finishDraft())
                .shouldSee(st.pages().mail().composePopup().expandedPopup());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем наличие кнопок разворачивания треда и непрочитанных")
    @TestCaseId("2735")
    public void shouldSeeLinksInThread() {
        stepsProd.user().apiMessagesSteps().markAllMsgUnRead();
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldSee(
                st.pages().mail().msgView().loadMore(),
                st.pages().mail().msgView().expandUnreadBtn()
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверям кнопку «Открыть список писем в треде»")
    @TestCaseId("2140")
    public void shouldSeeAllMessagesInThread() {
        Consumer<InitStepsRule> actions = st -> {
            openFirstMessageInMessageList(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().loadMore())
                .shouldNotSee(st.pages().mail().msgView().loadMore());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Разворачиваем первое подгруженное письмо в полном списке писем треда")
    @TestCaseId("2736")
    public void shouldSeeExpandedMessageUnwrapped() {
        Consumer<InitStepsRule> actions = st -> {
            openFirstMessageWithWrapping(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().loadMore());
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().msgInThread().get(THREAD_SIZE))
                .shouldSee(st.pages().mail().msgView().messageHead().messageRead());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Открываем первое сообщение в списке писем")
    private void openFirstMessageInMessageList(InitStepsRule st) {
        st.user().messagesSteps().clicksOnMessageByNumber(0);
        st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageTextBlock().text());
    }

    @Step("Открываем первое сообщение в списке писем и сворачиваем открывшееся письмо")
    private void openFirstMessageWithWrapping(InitStepsRule st) {
        openFirstMessageInMessageList(st);
        st.user().defaultSteps().clicksOn(st.pages().mail().msgView().messageHead().closeMessageField());
    }

}
