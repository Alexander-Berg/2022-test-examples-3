package ru.yandex.autotests.innerpochta.tests.autotests;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SENT_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.DELETE;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INARCHIVE;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INFOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INSPAM;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.REPLY;
import static ru.yandex.autotests.innerpochta.util.MailConst.INBOX_RU;
import static ru.yandex.autotests.innerpochta.util.MailConst.SENT_RU;
import static ru.yandex.autotests.innerpochta.util.MailConst.XIVA_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на ксиву")
@Features(FeaturesConst.XIVA)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class XivaTest {

    private static final int NEW_MSGS_COUNT = 3;
    private static final String SUBJ_MSG = "Тема письма";
    private static final String SUBJ_THREAD = "Тема треда";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @DataProvider
    public static Object[][] button() {
        return new Object[][]{
            {INSPAM.btn()},
            {DELETE.btn()}
        };
    }

    @DataProvider
    public static Object[][] answerMsgParams() {
        return new Object[][]{
            {1, SUBJ_MSG, "3", 0},
            {2, SUBJ_THREAD, "4", 2}
        };
    }

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), SUBJ_THREAD, "");
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), SUBJ_MSG, "");
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), getRandomString(), "");
        steps.user().apiFoldersSteps().createNewFolder(getRandomString());
        steps.user().defaultSteps().waitInSeconds(3); //ждём, чтобы все письма пришли и счётчики не скакали после логина
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 3)
            .shouldSeeThatElementHasText(
                steps.pages().touch().messageList().headerBlock().unreadCounter(),
                Integer.toString(NEW_MSGS_COUNT)
            );
    }

    @Test
    @Title("Должен создаться тред из одиночного письма")
    @TestCaseId("272")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCreateThreadFromMsg() {
        steps.user().apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(SUBJ_MSG, accLock.firstAcc(), "");
        assertThat(
            "Тред не переместился наверх в списке писем",
            steps.user().touchPages().messageList().messageBlock().threadCounter(),
            withWaitFor(isDisplayed(), XIVA_TIMEOUT)
        );
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().touch().messageList().messageBlock().subject(),
            SUBJ_MSG
        );
    }

    @Test
    @Title("Должны обновиться счётчики непрочитанных при удалении или отметке спамом непрочитанного письма")
    @TestCaseId("282")
    @UseDataProvider("button")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldUpdateUnreadCounterWhenDeleteOrSpamMsg(String btn) {
        actionWithMsg(0, btn);
        checkCounters();
    }

    @Test
    @Title("Должны обновиться счётчики непрочитанных при архивировании непрочитанного письма")
    @TestCaseId("283")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldUpdateUnreadCounterWhenArchiveMsg() {
        actionWithMsg(0, INARCHIVE.btn());
        checkCounters();
        steps.user().defaultSteps()
            .shouldSeeThatElementHasText(steps.pages().touch().sidebar().folderBlocks().get(6).counter(), "1");
    }

    @Test
    @Title("Должны обновиться счётчики непрочитанных при перемещении непрочитанного письма")
    @TestCaseId("283")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldUpdateUnreadCounterWhenMoveMsg() {
        actionWithMsg(0, INFOLDER.btn());
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(0));
        checkCounters();
        steps.user().defaultSteps()
            .shouldSeeThatElementHasText(steps.pages().touch().sidebar().folderBlocks().get(1).counter(), "1");
    }

    @Test
    @Title("Новое письмо должно приклеиться к треду")
    @TestCaseId("274")
    @DoTestOnlyForEnvironment("Phone")
    //TODO: из-за бага тред не поднимается в списке писем без рефреша
    public void shouldUpdateThreadWithNewMsg() {
        steps.user().apiMessagesSteps()
            .sendMessageToThreadWithSubjectWithNoSave(SUBJ_THREAD, accLock.firstAcc(), "");
        checkUnreadCounterInHeader(1, 20);
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().touch().messageList().messages().get(2).threadCounter(),
            "3"
        )
            .shouldSeeThatElementHasText(
                steps.pages().touch().messageList().messages().get(2).subject(),
                SUBJ_THREAD
            );
    }

    @Test
    @Title("При ответе на письмо/тред ответ подклеивается к письму")
    @TestCaseId("276")
    @UseDataProvider("answerMsgParams")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCreateThreadWhenAnswerMsg(int msgNum, String subj, String msgsInThread, int msgNumAfterReply) {
        actionWithMsg(msgNum, REPLY.btn());
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().sendBtn());
        checkUnreadCounterInHeader(1, 20);
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().touch().messageList().messages().get(msgNumAfterReply).subject(),
            subj
        )
            .shouldSeeThatElementHasText(
                steps.pages().touch().messageList().messages().get(msgNumAfterReply).threadCounter(),
                msgsInThread
            );
    }

    @Test
    @Title("При ответе на письмо/тред ответ подклеивается к письму")
    @TestCaseId("276")
    @Issue("QUINN-5688")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @DoTestOnlyForEnvironment("Phone")
    //TODO: если починили Issue, то нужно поправить shouldCreateThreadWhenAnswerMsg: msgNumAfterReply должен быть 0
    public void reminderTest() {
    }


    @Test
    @Title("Ответ на тред подклеивается к треду")
    @TestCaseId("275")
    @Issue("QUINN-6681")
    @ConditionalIgnore(condition = TicketInProgress.class)
    public void shouldUpdateThreadAfterAnswer() {
        openSentAndGoToInbox();
        actionWithMsg(1, REPLY.btn());
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .waitInSeconds(3)
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOnElementWithText(steps.pages().touch().sidebar().folderBlocks(), SENT_RU)
            .shouldSeeThatElementHasText(
                steps.pages().touch().messageList().messageBlock().subject(),
                SUBJ_MSG
            );
    }

    @Test
    @Title("Письмо появляется в инбоксе без рефреша")
    @TestCaseId("273")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeNewMsgWithoutRefresh() {
        String subj = Utils.getRandomName();
        steps.user().defaultSteps()
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), NEW_MSGS_COUNT);
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().touch().messageList().messageBlock().subject(),
            subj
        );
        steps.user().defaultSteps()
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), NEW_MSGS_COUNT + 1);
    }

    @Test
    @Title("Отправленное в треде письмо корректно отображается в папке «Отправленные»")
    @TestCaseId("1185")
    @Issue("QUINN-6681")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeSentThreadInSent() {
        String text = Utils.getRandomName();
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), getRandomString(), "");
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(SENT_FOLDER))
            .clicksOn(steps.pages().touch().messageList().messages().get(1))
            .clicksAndInputsText(steps.pages().touch().messageView().quickReply().input(), text)
            .clicksOn(steps.pages().touch().messageView().quickReply().send())
            .shouldNotSee(steps.pages().touch().messageList().headerBlock().unreadCounter())
            .shouldSeeThatElementHasText(steps.pages().touch().messageList().messageBlock().firstline(), text)
            .shouldSeeThatElementHasText(
                steps.pages().touch().messageList().messageBlock().threadCounter(),
                Integer.toString(NEW_MSGS_COUNT + 1)
            );
    }

    @Step("Выполняем действие с письмом ")
    private void actionWithMsg(int msgNum, String btn) {
        steps.user().touchSteps().rightSwipe(steps.pages().touch().messageList().messages().get(msgNum));
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messages().get(msgNum).swipeFirstBtn())
            .shouldSee(steps.pages().touch().messageList().popup())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), btn);
    }

    @Step("Проверяем счётчики непрочитанных писем")
    private void checkCounters() {
        checkUnreadCounterInHeader(-1, 10);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldSeeThatElementHasText(steps.pages().touch().sidebar().folderBlocks().get(0).counter(), "2");
    }

    @Step("Проверяем счётчики непрочитанных писем в шапке списка писем")
    private void checkUnreadCounterInHeader(int diff, int timeout) {
        steps.pages().touch().messageList().headerBlock().unreadCounter()
            .waitUntil(
                "Счетчик непрочитанных писем отличен от ожидаемого",
                hasText(Integer.toString(NEW_MSGS_COUNT + diff)),
                timeout
            );
    }

    @Step("Открываем папку отправленные и переходим в инбокс")
    private void openSentAndGoToInbox() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(SENT_FOLDER))
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 1)
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOnElementWithText(steps.pages().touch().sidebar().folderBlocks(), INBOX_RU);
    }
}
