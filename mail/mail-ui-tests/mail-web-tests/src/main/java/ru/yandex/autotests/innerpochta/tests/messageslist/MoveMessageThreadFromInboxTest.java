package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.ProxyServerRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.util.MailConst.INBOX_RU;
import static ru.yandex.autotests.innerpochta.util.ProxyParamsCheckFilter.proxyParamsCheckFilter;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_FOLDER;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.HANDLER_DO_MESSAGES;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_TIDS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

@Aqua.Test
@Title("Тест на перемещение тредов из папки Inbox")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class MoveMessageThreadFromInboxTest extends BaseTest {

    private static final String EMPTY_VIEW = "Письма не выбраны";
    private static final int THREAD_COUNTER = 4;
    private static final String PREFIX = "Folder ";

    private Message msg;
    private Message thread;
    private String userFolder;
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static ProxyServerRule serverProxyRule = proxyServerRule(proxyParamsCheckFilter(HANDLER_DO_MESSAGES));

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Override
    public DesiredCapabilities setCapabilities() {
        return serverProxyRule.getCapabilities();
    }

    @Before
    public void logIn() {
        userFolder = Utils.getRandomString();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-vertical",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        thread = user.apiMessagesSteps().sendThread(lock.firstAcc(), Utils.getRandomName(), THREAD_COUNTER);
        user.apiFoldersSteps().createNewFolder(userFolder);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(thread.getSubject(), msg.getSubject());
        user.leftColumnSteps().openFolders();
    }

    @Test
    @Title("Перемещаем несколько сообщений в треде из папки инбокс в кастомную папку")
    @TestCaseId("1546")
    public void testMoveMessagesInThreadFromInboxToCustomFolder() {
        user.messagesSteps().expandsMessagesThread(thread.getSubject())
            .selectMessagesInThreadCheckBoxWithNumber(0, 1);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().moveMessageDropDown())
            .onMouseHoverAndClick(onMessageView().moveMessageDropdownMenu().customFolders().get(1))
            .refreshPage();
        user.messagesSteps().expandsMessagesThread(thread.getSubject())
            .shouldSeeMessageWithSubject(thread.getSubject())
            .shouldSeeThreadCounter(thread.getSubject(), THREAD_COUNTER);
//        user.leftColumnSteps().shouldSeeCustomFolderCounterIs(2); //TODO: fix after https://st.yandex-team.ru/DARIA-71124
        user.messagesSteps().shouldSeeMessageFoldersInThread(userFolder, 0, 1);
        user.leftColumnSteps().opensCustomFolder(0);
        user.messagesSteps().shouldSeeMessageWithSubject(thread.getSubject())
            .shouldSeeThreadCounter(thread.getSubject(), THREAD_COUNTER)
            .expandsMessagesThread(thread.getSubject())
            .shouldSeeMessageFoldersInThread(INBOX_RU, 2, 3);
    }

    @Test
    @Title("Перемещаяем несколько обычных сообщений и тред из папки инбокс в кастомную папку")
    @TestCaseId("1547")
    public void testMoveThreadAndMessageFromInboxToCustomFolder() {
        user.messagesSteps().selectMessageWithSubject(msg.getSubject())
            .selectMessageWithSubject(thread.getSubject());
        user.defaultSteps().clicksOn(onMessagePage().toolbar().moveMessageDropDown())
            .onMouseHoverAndClick(onMessageView().moveMessageDropdownMenu().customFolders().get(3))
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_TIDS, msg.getTid())
            )
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_TIDS, thread.getTid())
            );
    }

    @Test
    @Title("Перемещаем последнее письмо из папки")
    @TestCaseId("2182")
    public void shouldNotSeeMessageAfterMove() {
        user.messagesSteps().movesMessageToFolder(msg.getSubject(), userFolder);
        user.leftColumnSteps().opensCustomFolder(userFolder);
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.messageViewSteps().shouldSeeMessageSubjectInCompactView(msg.getSubject());
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().moreBtn())
            .shouldSee(onMessageView().miscField())
            .clicksOn(onMessageView().miscField().folder())
            .shouldSee(onMessageView().moveMessageDropdownMenu())
            .onMouseHoverAndClick(onMessageView().moveMessageDropdownMenu().inboxFolder())
            .shouldNotSee(onMessageView().messageSubject().subject());
        user.defaultSteps().shouldHasText(onMessageView().emptyMsgView3pane(), EMPTY_VIEW);
    }

    @Test
    @Title("Письма отсортированы в хронологическом порядке после переноса в другую папку")
    @TestCaseId("5493")
    public void shouldSeeMessagesInChronologicalOrderAfterMoveFromFolder() {
        Folder folder = user.apiFoldersSteps().createNewFolder(Utils.getRandomString());
        String subject = getRandomString();
        user.apiFiltersSteps().createFilterForFolderOrLabel(
            "",
            PREFIX,
            FILTERS_ADD_PARAM_MOVE_FOLDER,
            folder.getFid(),
            FILTERS_ADD_PARAM_CLICKER_MOVE,
            false
        );
        user.apiMessagesSteps()
            .sendMailWithNoSaveWithoutCheck(lock.firstAcc().getSelfEmail(), PREFIX + getRandomString(), "");
        user.defaultSteps().waitInSeconds(1);
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, "");
        user.defaultSteps().waitInSeconds(1);
        user.apiMessagesSteps()
            .sendMailWithNoSaveWithoutCheck(lock.firstAcc().getSelfEmail(), PREFIX + getRandomString(), "");
        user.defaultSteps().opensFragment(QuickFragments.INBOX).refreshPage();
        user.messagesSteps().movesMessageToFolder(subject, folder.getName())
            .shouldNotSeeMessageWithSubject(subject);
        user.leftColumnSteps().opensCustomFolder(folder.getName());
        user.defaultSteps().shouldSeeThatElementHasText(
            onMessagePage().displayedMessages().list().get(1).subject(),
            subject
        );
    }
}
