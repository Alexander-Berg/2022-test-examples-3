package ru.yandex.autotests.innerpochta.tests.contextmenu;

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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;

@Aqua.Test
@Title("Тесты на пункт “Переложить в папку“ для писем/тредов")
@Features(FeaturesConst.CONTEXT_MENU)
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
public class ContextMoveToFolderTest extends BaseTest {

    private static final String CUSTOM_FOLDER = "customFolder";
    private static final String CUSTOM_FOLDER_2 = "customFolder2";
    private static final int THREAD_COUNTER = 2;
    private static final String[] FOLDER_LIST = {
        "Входящие",
        CUSTOM_FOLDER,
        CUSTOM_FOLDER_2,
        "Отправленные",
        "Спам",
        "Удалённые",
        "Черновики",
        "Новая папка…"
    };

    private String subject;

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
    public void login() {
        String foldersFids = user.apiFoldersSteps().getAllFids();
        user.apiSettingsSteps().callWithListAndParams(
            "Раскрываем все папки",
            of(FOLDERS_OPEN, foldersFids)
        );
        user.apiFoldersSteps().createNewFolder(CUSTOM_FOLDER);
        user.apiFoldersSteps().createNewFolder(CUSTOM_FOLDER_2);
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Структура папок в дропдауне соответствует дереву папок")
    @TestCaseId("1249")
    public void folderList() {
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).moveToFolder());
        user.messagesSteps().shouldSeeAdditionalContextMenu()
            .shouldSeeItemsInAdditionalContextMenu(FOLDER_LIST);
    }

    @Test
    @Title("Перекладываем письмо в папку")
    @TestCaseId("1248")
    public void moveMessageToFolder() {
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).moveToFolder());
        user.messagesSteps().shouldSeeAdditionalContextMenu();
        user.defaultSteps().clicksOnElementWithText(
            onMessagePage().allMenuListInMsgList().get(1).itemListInMsgList(),
            CUSTOM_FOLDER
        );
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Перекладываем тред в папку")
    @TestCaseId("1250")
    public void moveThreadToFolder() {
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subject, lock.firstAcc(), "");
        user.defaultSteps().clicksOn(onHomePage().checkMailButton());
        user.messagesSteps().shouldSeeThreadCounter(subject, THREAD_COUNTER)
            .rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).moveToFolder());
        user.messagesSteps().shouldSeeAdditionalContextMenu();
        user.defaultSteps().clicksOnElementWithText(onMessagePage().allMenuListInMsgList().get(1).itemListInMsgList(), CUSTOM_FOLDER);
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER);
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .shouldSeeThreadCounter(subject, THREAD_COUNTER);
    }

    @Test
    @Title("Перекладываем часть писем треда в другие папки")
    @TestCaseId("4313")
    public void moveFewThreadMsgToFolders() {
        String threadSbj = Utils.getRandomString();
        user.apiMessagesSteps().sendThread(lock.firstAcc(), threadSbj, 5);
        user.defaultSteps().clicksOn(onHomePage().checkMailButton());
        user.messagesSteps().shouldSeeMessageWithSubject(threadSbj)
            .shouldSeeThreadCounter(threadSbj, 5)
            .expandsMessagesThread(threadSbj)
            .selectMessagesInThreadCheckBoxWithNumber(0);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().moveMessageDropDown())
            .onMouseHoverAndClick(onMessageView().moveMessageDropdownMenu().customFolders().get(1));
        user.messagesSteps().selectMessagesInThreadCheckBoxWithNumber(2, 3);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().moveMessageDropDown())
            .onMouseHoverAndClick(onMessageView().moveMessageDropdownMenu().customFolders().get(2))
            .refreshPage();
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER);
        user.messagesSteps().shouldSeeThreadCounter(threadSbj, 5)
            .expandsMessagesThread(threadSbj)
            .selectMessagesInThreadCheckBoxWithNumber(2);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().deleteButton());
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER_2);
        user.messagesSteps().shouldSeeThreadCounter(threadSbj, 4);
    }
}
