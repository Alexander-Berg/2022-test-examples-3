package ru.yandex.autotests.innerpochta.tests.autotests.messageslist;

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
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MESSAGES;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INFOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.MARKLABEL;
import static ru.yandex.autotests.innerpochta.util.MailConst.XIVA_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Все про треды")
@Features(FeaturesConst.THREAD)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class ThreadsTest {

    private static final int ARBITRARY_THREAD_SIZE = 3;
    private static final String UNREAD_MSG_URL_PART = "all/only_new";
    private String subject;
    private Folder folder;
    private Label label;

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
        subject = Utils.getRandomString();
        steps.user().apiMessagesSteps().sendThread(accLock.firstAcc(), subject, ARBITRARY_THREAD_SIZE);
        folder = steps.user().apiFoldersSteps().createNewFolder(Utils.getRandomName());
        label = steps.user().apiLabelsSteps().addNewLabel(Utils.getRandomName(), LABELS_PARAM_GREEN_COLOR);
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[][] threadsAndMessages() {
        return new Object[][]{
            {"Включаем тредный режим", true, 1},
            {"Отключаем тредный режим", false, ARBITRARY_THREAD_SIZE}
        };
    }

    @Test
    @Title("Включаем у пользователя тредный/нетредный режим в зависимости от настройки в БП")
    @TestCaseId("353")
    @UseDataProvider("threadsAndMessages")
    public void shouldSwitchBetweenThreads(String message, boolean isThreadModeOn, int messagesInInbox) {
        steps.user().apiSettingsSteps().callWithListAndParams(
            message,
            of(SETTINGS_FOLDER_THREAD_VIEW, isThreadModeOn)
        );
        steps.user().defaultSteps()
            .refreshPage()
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), messagesInInbox);
    }

    @Test
    @Title("Проверяем, что счетчик писем в треде меняется, если удалить одно письмо")
    @TestCaseId("401")
    public void shouldUpdateMessageInThreadCounter() {
        steps.user().apiMessagesSteps().deleteMessages(
            steps.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0)
        );
        steps.user().defaultSteps()
            .refreshPage()
            .shouldSee(steps.pages().touch().messageList().headerBlock())
            .shouldSeeThatElementTextEquals(
                steps.pages().touch().messageList().messages().get(0).threadCounter(),
                String.valueOf(ARBITRARY_THREAD_SIZE - 1)
            );
    }

    @Test
    @Title("Черновик должен прикрепиться к одиночному письму и сложить тред")
    @TestCaseId("311")
    public void shouldGetThreadFromMsgAndDraft() {
        steps.user().apiFoldersSteps().purgeFolder(steps.user().apiFoldersSteps().getFolderBySymbol(INBOX))
            .purgeFolder(steps.user().apiFoldersSteps().getFolderBySymbol(SENT));
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subject, "");
        steps.user().defaultSteps().refreshPage();
        steps.user().apiMessagesSteps().prepareDraftToThread("", subject, "");
        assertThat(
            "Тред не переместился наверх в списке писем",
            steps.pages().touch().messageList().messageBlock().threadCounter(),
            withWaitFor(isDisplayed(), XIVA_TIMEOUT)
        );
    }

    @Test
    @Title("Черновик в треде должен увеличить счётчик")
    @TestCaseId("312")
    public void shouldIncreaseCounterWhenAddDraft() {
        steps.user().apiMessagesSteps().prepareDraftToThread("", subject, "");
        steps.user().defaultSteps().shouldSeeThatElementTextEquals(
            steps.pages().touch().messageList().messageBlock().threadCounter(),
            String.valueOf(ARBITRARY_THREAD_SIZE + 1)
        );
    }

    @Test
    @Title("По тапу на прыщ прочитываются все письма треда")
    @TestCaseId("252")
    public void shouldMarkThreadRead() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messageBlock().toggler())
            .opensDefaultUrlWithPostFix(MESSAGES.makeTouchUrlPart(UNREAD_MSG_URL_PART))
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg());
    }

    @Test
    @Title("По тапу на прыщ помечаются непрочитанными все письма треда")
    @TestCaseId("256")
    public void shouldMarkThreadUnread() {
        steps.user().apiMessagesSteps().markAllMsgRead();
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().touch().messageList().messageBlock().toggler())
            .shouldSee(steps.pages().touch().messageList().messageBlock().unreadToggler())
            .opensDefaultUrlWithPostFix(MESSAGES.makeTouchUrlPart(UNREAD_MSG_URL_PART))
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), ARBITRARY_THREAD_SIZE);
    }

    @Test
    @Title("Должны поставить метку на все письма треда")
    @TestCaseId("250")
    public void shouldMarkAllMsgInThreadWithLabel() {
        steps.user().touchSteps().openActionsForMessages(0);
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKLABEL.btn())
            .clicksOn(steps.pages().touch().messageList().popup().labels().get(1))
            .shouldSeeElementsCount(steps.pages().touch().messageList().popup().tick(), 1)
            .clicksOn(steps.pages().touch().messageList().popup().done())
            .shouldSee(steps.pages().touch().messageList().statusLineInfo());
        steps.user().apiMessagesSteps().shouldGetMsgCountInLabelViaApi(label.getName(), ARBITRARY_THREAD_SIZE);
    }

    @Test
    @Title("Тред должен быть виден во всех папках, где есть его письма этого треда")
    @TestCaseId("248")
    public void shouldSeeThreadInDiffFolders() {
        steps.user().apiMessagesSteps().moveMessagesToSpam(
            steps.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0)
        )
            .moveAllMessagesFromFolderToFolder(SPAM, folder.getName());
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(folder.getFid()))
            .shouldSeeThatElementTextEquals(
                steps.pages().touch().messageList().messageBlock().threadCounter(),
                String.valueOf(ARBITRARY_THREAD_SIZE)
            );
    }

    @Test
    @Title("Должны переместить все письма треда в папку")
    @TestCaseId("249")
    public void shouldMoveAllMsgInThread() {
        Folder folder = steps.user().apiFoldersSteps().getAllUserFolders().get(0);
        steps.user().apiMessagesSteps().sendMessageToThreadWithSubject(subject, accLock.firstAcc(), "");
        steps.user().defaultSteps().refreshPage();
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messageBlock().swipeFirstBtn())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), INFOLDER.btn())
            .clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(0))
            .shouldSee(steps.pages().touch().messageList().statusLineInfo());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folder.getName(), 4);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(SENT, 1);
    }

    @Test
    @Title("Метка на треде появляется и исчезает при отметке и снятии метки с одного письма в треде")
    @TestCaseId("1069")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeLabelOnTreadWithOneLabeledMsg() {
        labelMsgViaApi(0);
        checkLabelInMsgListAndView();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().unmarkLabelBtn())
            .shouldNotSee(steps.pages().touch().messageView().unmarkLabelBtn())
            .clicksOn(steps.pages().touch().messageView().header().backToListBtn())
            .shouldNotSee(steps.pages().touch().messageList().messageBlock().label());
    }

    @Test
    @Title("Метка на треде появляется и исчезает при отметке и снятии метки с одного письма в треде")
    @TestCaseId("1069")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeLabelOnTreadWithOneLabeledMsgTablet() {
        labelMsgViaApi(0);
        checkLabelInMsgListAndView();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().unmarkLabelBtn())
            .shouldNotSee(
                steps.pages().touch().messageView().unmarkLabelBtn(),
                steps.pages().touch().messageList().messageBlock().label()
            );
    }

    @Test
    @Title("Снятие метки с одного письма в треде")
    @TestCaseId("1068")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldUnlabelOneMsgInThread() {
        labelMsgViaApi(0, 1, 2);
        checkLabelInMsgListAndView();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().moreBtn());
        unlabelMsg();
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageView().unmarkLabelBtn())
            .clicksOn(steps.pages().touch().messageView().header().backToListBtn())
            .shouldSee(steps.pages().touch().messageList().messageBlock().label());
        steps.user().apiMessagesSteps().shouldGetMsgCountInLabelViaApi(label.getName(), 2);
    }

    @Test
    @Title("Снятие метки с одного письма в треде")
    @TestCaseId("1068")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldUnlabelOneMsgInThreadTablet() {
        labelMsgViaApi(0, 1, 2);
        checkLabelInMsgListAndView();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().moreBtn());
        unlabelMsg();
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageView().unmarkLabelBtn())
            .shouldSee(steps.pages().touch().messageList().messageBlock().label());
        steps.user().apiMessagesSteps().shouldGetMsgCountInLabelViaApi(label.getName(), 2);
    }

    @Step("Поставить на письмо метку через апи")
    private void labelMsgViaApi(int... num) {
        List<Message> msgs = steps.user().apiMessagesSteps().getAllMessagesInFolder(INBOX);
        for (int i : num) {
            steps.user().apiLabelsSteps().markWithLabel(msgs.get(i), label);
        }
    }

    @Step("Проверяем, что метка есть в списке писем и просмотре письма")
    private void checkLabelInMsgListAndView() {
        steps.user().defaultSteps().refreshPage()
            .shouldSee(steps.pages().touch().messageList().messageBlock().label())
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageView().toolbar())
            .shouldSee(steps.pages().touch().messageView().msgDetails())
            .shouldSee(steps.pages().touch().messageView().unmarkLabelBtn());
    }

    @Step("Снимаем метку через меню действий с письмом")
    private void unlabelMsg() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().popup())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKLABEL.btn())
            .clicksOn(steps.pages().touch().messageList().popup().labels().get(1))
            .shouldNotSee(steps.pages().touch().messageList().popup().tick())
            .clicksOn(steps.pages().touch().messageList().popup().activeDone());
    }
}
