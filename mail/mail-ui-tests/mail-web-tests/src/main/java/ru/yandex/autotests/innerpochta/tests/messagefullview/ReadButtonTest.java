package ru.yandex.autotests.innerpochta.tests.messagefullview;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тест на кнопку «Прочитано»")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.TOOLBAR)
public class ReadButtonTest extends BaseTest {

    private static final int THREAD_COUNT = 2;
    private static final String THREAD = "thread";
    private static final String FOLDER_NAME = Utils.getRandomName();
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private Message msg;
    private Message threadMsg1;
    private int unreadCounter;
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        user.apiFoldersSteps().createNewFolder(FOLDER_NAME);
        threadMsg1 = user.apiMessagesSteps().sendThread(lock.firstAcc(), THREAD, THREAD_COUNT);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.defaultSteps().refreshPage();
        unreadCounter = user.leftColumnSteps().unreadCounter();
    }

    @Test
    @Title("Клик по кнопке «Не прочитано» в тулбаре при просмотре письма")
    @TestCaseId("1986")
    public void shouldMarkMessageUnread() {
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
        user.leftColumnSteps().shouldSeeInboxUnreadCounter(unreadCounter - 1);
        user.defaultSteps().clicksOn(onMessageView().toolbar().markAsUnreadButton());
        user.leftColumnSteps().shouldSeeInboxUnreadCounter(unreadCounter)
            .clicksOnUnreadMessages();
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("Остаемся в текущей папке по клику на кнопку «Не прочитано» в тулбаре при просмотре письма")
    @TestCaseId("6231")
    public void shouldStayInFolderAfterMarkMessageUnread() {
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 1)
            .moveMessagesFromFolderToFolder(
                FOLDER_NAME,
                user.apiMessagesSteps().getAllMessages().get(0)
            );
        user.leftColumnSteps().openFolders()
            .shouldSeeCustomFolderCounterIs(1)
            .opensCustomFolder(FOLDER_NAME)
            .shouldSeeCurrentCustomFolderCounterIs("1/1");
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().clicksOn(onMessageView().toolbar().markAsUnreadButton());
        user.leftColumnSteps().shouldBeInFolder(FOLDER_NAME)
            .shouldSeeCurrentCustomFolderCounterIs("1/1")
            .clicksOnUnreadMessages();
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("Клик по кнопке «Не прочитано» через выпадающее меню при просмотре письма")
    @TestCaseId("1647")
    public void shouldMarkMessageUnreadBtn() {
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.defaultSteps()
            .clicksOn(onMessageView().toolbar().markMessageDropDown())
            .shouldSee(onMessageView().labelsDropdownMenu())
            .clicksOn(onMessageView().labelsDropdownMenu().markAsUnread());
        user.messagesSteps().shouldNotSeeOpenMessageSubjectField();
        user.leftColumnSteps().shouldBeInFolder(INBOX_RU)
            .shouldSeeInboxUnreadCounter(unreadCounter);
        user.leftColumnSteps().clicksOnUnreadMessages();
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("Клик по прыщу непрочитанности при просмотре треда")
    @TestCaseId("3289")
    public void shouldMarkThreadUnread() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
        user.defaultSteps().refreshPage();
        user.defaultSteps().clicksOn(user.messagesSteps().findMessageBySubject(threadMsg1.getSubject()).subject());
        user.leftColumnSteps().shouldSeeInboxUnreadCounter(unreadCounter - 1);
        user.defaultSteps().shouldSee(onMessageView().messageSubject().threadUnread())
            .onMouseHoverAndClick(onMessageView().messageHead().messageRead())
            .shouldSee(onMessageView().messageHead().messageUnread());
        user.leftColumnSteps().shouldSeeInboxUnreadCounter(unreadCounter);
        user.defaultSteps().clicksOn(onMessageView().messageSubject().threadUnread())
            .shouldSeeWithHover(onMessageView().messageHead().messageRead());
        user.leftColumnSteps().shouldSeeInboxUnreadCounter(unreadCounter - 2);
        user.defaultSteps().refreshPage()
            .onMouseHoverAndClick(onMessageView().messageSubject().threadRead())
            .shouldSee(onMessageView().messageHead().messageUnread());
        user.leftColumnSteps().shouldSeeInboxUnreadCounter(unreadCounter);
    }
}
