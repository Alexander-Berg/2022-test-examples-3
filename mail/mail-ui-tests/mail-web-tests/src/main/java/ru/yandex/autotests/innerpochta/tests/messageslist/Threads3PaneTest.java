package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Просмотр тредов в 3Pane")
@Features(FeaturesConst.THREE_PANE)
@Tag(FeaturesConst.THREE_PANE)
@Stories(FeaturesConst.THREAD)
public class Threads3PaneTest extends BaseTest {

    private static final int THREAD_SIZE = 5;
    private static final String LONG_TEXT = "t\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt\nt";

    private String threadSubject;
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        threadSubject = getRandomName();
        user.apiMessagesSteps().sendThread(lock.firstAcc(), threadSubject, THREAD_SIZE);
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем 3Pane и тредный режим",
            of(
                SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL,
                SETTINGS_FOLDER_THREAD_VIEW, TRUE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().list().get(0).messageUnread());
    }

    @Test
    @Title("Проверяем спрятанные письма в просмотре треда")
    @TestCaseId("348")
    public void loadMoreButtonInThread() {
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(threadSubject, lock.firstAcc(), LONG_TEXT);
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().displayedMessages().list().get(0).messageUnread());
        user.messagesSteps().clicksOnMessageWithSubject(threadSubject);
        user.defaultSteps().shouldSee(onMessageView().msgInThread().get(0), onMessageView().loadMore());
    }

    @Test
    @Title("Открываем тред с непрочитанными письмами")
    @TestCaseId("348")
    public void openThreadWithUnreadMessages() {
        user.messagesSteps().expandsMessagesThread(threadSubject)
            .selectMessagesInThreadCheckBoxWithNumber(1, THREAD_SIZE - 1);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().markAsUnreadButton());
        user.messagesSteps().clicksOnMessageWithSubject(threadSubject);
        user.defaultSteps().shouldSeeAllElementsInList(
            onMessageView().expandMsgInThread(),
            onMessagePage().displayedMessages().list().get(2).firstLine().getText(),
            onMessagePage().displayedMessages().list().get(THREAD_SIZE).firstLine().getText()
        );
    }

    @Test
    @Title("Открываем тред с черновиком")
    @TestCaseId("348")
    public void openThreadWithDraft() {
        user.apiMessagesSteps().prepareDraftToThread(lock.firstAcc().getSelfEmail(), threadSubject, getRandomString());
        user.defaultSteps().refreshPage();
        user.messagesSteps().expandsMessagesThread(threadSubject)
            .clicksOnMessageWithSubject(threadSubject);
        user.defaultSteps().shouldSeeElementsCount(onMessageView().expandMsgInThread(), 1)
            .shouldHasText(
                onMessageView().messageTextBlock().text(),
                onMessagePage().displayedMessages().list().get(2).firstLine().getText()
            );
    }

    @Test
    @Title("Разворачиваем несколько писем треда")
    @TestCaseId("1068")
    public void expandMessagesInThread() {
        user.messagesSteps().clicksOnMessageWithSubject(threadSubject);
        user.defaultSteps().clicksOn(onMessageView().msgInThread().get(0))
            .shouldSeeElementsCount(onMessageView().expandMsgInThread(), 2);
    }

    @Test
    @Title("Открываем список писем треда")
    @TestCaseId("1068")
    public void loadMoreMessagesInThread() {
        user.messagesSteps().clicksOnMessageWithSubject(threadSubject);
        user.defaultSteps().clicksOn(onMessageView().loadMore())
            .shouldSeeElementsCount(onMessageView().msgInThread(), THREAD_SIZE - 1);
    }
}
