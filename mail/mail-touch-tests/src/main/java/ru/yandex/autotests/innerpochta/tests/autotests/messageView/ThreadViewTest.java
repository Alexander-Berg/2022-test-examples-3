package ru.yandex.autotests.innerpochta.tests.autotests.messageView;

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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.Scrips.SCRIPT_FOR_SCROLLDOWN_THREAD;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.DELETE;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INARCHIVE;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INFOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INSPAM;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на просмотр письма и треда")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class ThreadViewTest {

    private static final int MESSAGE_NUMBER = 3,
        MSG_PORTION_IN_THREAD = 12,
        BIG_MESSAGE_NUMBER = 24; //BIG_MESSAGE_NUMBER должно быть >=24

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[] buttons() {
        return new Object[][]{
            {INSPAM.btn()},
            {DELETE.btn()}
        };
    }

    @Test
    @Title("Должны подгрузить порцию писем в длинном треде")
    @TestCaseId("297")
    public void shouldLoadPreviousMsgs() {
        sendAndOpenThread(BIG_MESSAGE_NUMBER);
        steps.user().defaultSteps()
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), MSG_PORTION_IN_THREAD)
            .executesJavaScript(SCRIPT_FOR_SCROLLDOWN_THREAD)
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), BIG_MESSAGE_NUMBER)
        ;
    }

    @Test
    @Title("Счётчик писем в треде уменьшается при удалении/пометке как спам письма")
    @TestCaseId("296")
    @UseDataProvider("buttons")
    public void shouldDecreaseThreadCounter(String btnName) {
        sendAndOpenThread(MESSAGE_NUMBER);
        makeActionInMsgView(btnName);
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldContainText(steps.pages().touch().messageView().threadCounter(), String.valueOf(MESSAGE_NUMBER - 1));
    }

    @Test
    @Title("Счётчик писем в треде не меняется при архивировании письма")
    @TestCaseId("605")
    public void shouldNotChangeThreadCounterWhenArchive() {
        sendAndOpenThread(MESSAGE_NUMBER);
        makeActionInMsgView(INARCHIVE.btn());
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldContainText(steps.pages().touch().messageView().threadCounter(), String.valueOf(MESSAGE_NUMBER));
    }

    @Test
    @Title("Счётчик писем в треде не меняется при перемещении письма")
    @TestCaseId("607")
    public void shouldNotChangeThreadCounterWhenChangeFolder() {
        sendAndOpenThread(MESSAGE_NUMBER);
        makeActionInMsgView(INFOLDER.btn());
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(0))
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .waitInSeconds(10)
            .shouldContainText(steps.pages().touch().messageView().threadCounter(), String.valueOf(MESSAGE_NUMBER));
    }

    @Test
    @Title("Счётчик писем в треде увеличивается при приходе нового письма в тред")
    @TestCaseId("604")
    public void shouldIncreaseThreadCounter() {
        String subject = sendAndOpenThread(MESSAGE_NUMBER);
        steps.user().apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subject, accLock.firstAcc(), "");
        steps.pages().touch().messageView().threadCounter().waitUntil(
            "Счетчик писем треда отличен от ожидаемого",
            hasText(String.valueOf(MESSAGE_NUMBER + 1) + " письма"),
            10
        );
    }

    @Test
    @Title("Развернуть письмо в треде и увидеть его прочитанным")
    @TestCaseId("280")
    public void shouldMakeMailAsReadInThreadWhenOpen() {
        sendAndOpenThread(2);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().msgInThread().get(1))
            .shouldNotSee(steps.pages().touch().messageView().recentToggler());
    }

    @Test
    @Title("Тред должен открыться после удаления тредообразующего письма")
    @TestCaseId("1050")
    public void shouldOpenThreadAfterDeleteThreadMsg() {
        String subj = getRandomName();
        Message msg = steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        steps.user().apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subj, accLock.firstAcc(), "");
        steps.user().apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subj, accLock.firstAcc(), "");
        steps.user().apiMessagesSteps().deleteMessages(msg);
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 2);
    }

    @Test
    @Title("Тред должен исчезнуть из списка писем, но остаться в просмотре при удалении последнего письма из папки")
    @TestCaseId("310")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldDeleteAllMsgsInThreadInFolder() {
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), getRandomName(), "");
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 2);
        String url = steps.getDriver().getCurrentUrl();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().toolbar().delete())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 1)
            .shouldBeOnUrlNotDiffWith(url)
            .clicksOn(steps.pages().touch().messageView().header().backToListBtn())
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(SENT, 1);

    }

    @Test
    @Title("Тред должен исчезнуть из списка писем, но остаться в просмотре при удалении последнего письма из папки")
    @TestCaseId("310")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldDeleteAllMsgsInThreadTablet() {
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), getRandomName(), "");
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 2);
        String url = steps.getDriver().getCurrentUrl();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().toolbar().delete())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 1)
            .shouldBeOnUrlNotDiffWith(url)
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(SENT, 1);
    }

    @Test
    @Title("Должны вернуться к списку писем после удаления всех сообщений в треде")
    @TestCaseId("1281")
    public void shouldDeleteAllMsgsInThread() {
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), getRandomName(), "");
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 2)
            .clicksOn(steps.pages().touch().messageView().toolbar().delete())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 1)
            .clicksOn(steps.pages().touch().messageView().toolbar().delete())
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg())
            .shouldNotSee(steps.pages().touch().messageView().msgBody());
    }

    @Test
    @Title("Тред должен остаться в списке писем при удалении одного письма в нём")
    @TestCaseId("310")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeThreadInFolderAfterDeletingMsg() {
        steps.user().apiMessagesSteps().sendThread(accLock.firstAcc(), getRandomName(), 3);
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 3)
            .clicksOn(steps.pages().touch().messageView().toolbar().delete())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 2)
            .clicksOn(steps.pages().touch().messageView().header().backToListBtn())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 1);
    }

    @Test
    @Title("Тред должен остаться в списке писем при удалении одного письма в нём")
    @TestCaseId("310")
    @DoTestOnlyForEnvironment("Tablet")
    @Issue("QUINN-7463")
    @ConditionalIgnore(condition = TicketInProgress.class)
    public void shouldSeeThreadInFolderAfterDeletingMsgTablet() {
        steps.user().apiMessagesSteps().sendThread(accLock.firstAcc(), getRandomName(), 3);
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 3)
            .clicksOn(steps.pages().touch().messageView().toolbar().delete())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 2)
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 1);
    }

    @Step("Присылаем и открываем тред")
    private String sendAndOpenThread(int numMsgInThread) {
        String subject = getRandomName();
        steps.user().apiMessagesSteps().sendThread(accLock.firstAcc(), subject, numMsgInThread);
        steps.user().defaultSteps().opensDefaultUrl()
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .clicksOnElementWithText(steps.pages().touch().messageList().subjectList(), subject)
            .shouldContainText(steps.pages().touch().messageView().threadCounter(), String.valueOf(numMsgInThread));
        return subject;
    }

    @Step("Открываем меню действия с письмом, нажимаем на действие")
    private void makeActionInMsgView(String btnName) {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().moreBtn())
            .shouldSee(steps.pages().touch().messageList().popup())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), btnName);
    }
}
