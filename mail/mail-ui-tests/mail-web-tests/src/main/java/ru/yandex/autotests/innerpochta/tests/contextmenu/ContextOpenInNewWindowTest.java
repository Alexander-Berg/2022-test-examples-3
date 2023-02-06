package ru.yandex.autotests.innerpochta.tests.contextmenu;

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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

@Aqua.Test
@Title("Проверяем пункт “Открыть в новой вкладке“")
@Features("ContextMenu")
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories("Общие тесты")
public class ContextOpenInNewWindowTest extends BaseTest {

    private static final String OPEN_IN_NEW_TAB_MENU_ITEM = "Открыть в новой вкладке";
    private static final String CUSTOM_FOLDER_NAME = "customFolder";
    private static final String CUSTOM_LABEL_NAME = "custom";
    private static final int THREAD_COUNTER = 2;
    private String subject;

    public AccLockRule lock = AccLockRule.use().useTusAccount();
    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void login() {
        subject = Utils.getRandomString();
        user.apiFoldersSteps().createNewFolder(CUSTOM_FOLDER_NAME);
        user.apiLabelsSteps().addNewLabel(CUSTOM_LABEL_NAME, LABELS_PARAM_GREEN_COLOR);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Открываем тред в новом окне")
    @TestCaseId("1256")
    public void shouldOpenThread() {
        user.apiMessagesSteps().sendThread(lock.firstAcc(), subject, THREAD_COUNTER);
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeThreadCounter(subject, 2)
            .rightClickOnMessageWithSubject(subject);
        user.defaultSteps().clicksOnElementWithText(
            onMessagePage().allMenuListInMsgList().get(0).itemListInMsgList(),
            OPEN_IN_NEW_TAB_MENU_ITEM
        )
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString("#thread"));
        user.messagesSteps().shouldSeeMsgCount(2);
    }

    @Test
    @Title("При выборе нескольких сообщений в КМ нет пункта “Открыть в новой вкладке“")
    @TestCaseId("1257")
    public void shouldNotOpenSeveralMessages() {
        String subjectToRightClick = Utils.getRandomName();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, "");
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subjectToRightClick, "");
        user.defaultSteps().refreshPage();
        user.messagesSteps().selectMessageWithSubject(subject, subjectToRightClick)
            .rightClickOnMessageWithSubject(subjectToRightClick);
        user.defaultSteps().shouldNotSeeElementInList(
            onMessagePage().allMenuListInMsgList().get(0).itemListInMsgList(),
            OPEN_IN_NEW_TAB_MENU_ITEM
        );
    }

    @Test
    @Title("Открываем папку в новой кладке")
    @TestCaseId("1258")
    public void shouldOpenCustomFolder() {
        user.leftColumnSteps().openFolders()
            .rightClickOnCustomFolder(CUSTOM_FOLDER_NAME);
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOnElementWithText(
            onMessagePage().allMenuList().get(0).itemList(),
            OPEN_IN_NEW_TAB_MENU_ITEM
        )
            .switchOnJustOpenedWindow()
            .shouldBeOnUrlWith(QuickFragments.FOLDER);
    }

    @Test
    @Title("Открываем метку в новой вкладке")
    @TestCaseId("1259")
    public void shouldOpenCustomLabel() {
        user.leftColumnSteps().rightClickOnCustomLabel(CUSTOM_LABEL_NAME);
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOnElementWithText(
            onMessagePage().allMenuList().get(0).itemList(),
            OPEN_IN_NEW_TAB_MENU_ITEM
        )
            .switchOnJustOpenedWindow()
            .shouldBeOnUrlWith(QuickFragments.LABEL);
    }

    @Test
    @Title("Открываем в новой вкладке письмо через КМ")
    @TestCaseId("639")
    public void shouldOpenMessageInNewTab() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, "");
        user.defaultSteps().refreshPage();
        user.messagesSteps().selectMessageWithSubject(subject)
            .rightClickOnMessageWithSubject(subject);
        user.defaultSteps().clicksOnElementWithText(
            onMessagePage().allMenuListInMsgList().get(0).itemListInMsgList(),
            OPEN_IN_NEW_TAB_MENU_ITEM
        );
        user.messagesSteps().shouldNotSeeContextMenu();
        user.defaultSteps().switchOnJustOpenedWindow()
            .shouldSeeThatElementTextEquals(onMessageView().messageSubjectInFullView(), subject);
    }
}
