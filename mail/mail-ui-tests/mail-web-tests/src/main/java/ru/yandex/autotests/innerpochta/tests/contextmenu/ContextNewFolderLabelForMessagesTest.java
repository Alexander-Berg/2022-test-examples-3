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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;

@Aqua.Test
@Title("Создание папок и меток")
@Description("Создаём папки и метки из контекстного меню")
@Features(FeaturesConst.CONTEXT_MENU)
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
public class ContextNewFolderLabelForMessagesTest extends BaseTest {

    private static final String NEW_FOLDER = "Новая папка…";
    private static final String NEW_LABEL = "Новая метка…";
    private static final String CUSTOM_FOLDER = "CustomFolder";
    private static final String CUSTOM_LABEL = "customlabel";

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
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Создаём папку из контекстного меню и прекладываем письмо")
    @TestCaseId("1252")
    public void moveToNewFolder() {
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).moveToFolder());
        user.messagesSteps().shouldSeeAdditionalContextMenu();
        user.defaultSteps().clicksOnElementWithText(onMessagePage().allMenuListInMsgList().get(1).itemListInMsgList(), NEW_FOLDER)
            .shouldSee(onMessagePage().createFolderPopup())
            .inputsTextInElement(onMessagePage().createFolderPopup().folderName(), CUSTOM_FOLDER)
            .clicksOn(onMessagePage().createFolderPopup().create())
            .shouldNotSee(onMessagePage().createFolderPopup());
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Создаём метку из контекстного меню и помечаем письмо")
    @TestCaseId("1251")
    public void markWithNewLabel() {
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).markWithLabel());
        user.messagesSteps().shouldSeeAdditionalContextMenu();
        user.defaultSteps().clicksOnElementWithText(onMessagePage().allMenuListInMsgList().get(1).itemListInMsgList(), NEW_LABEL)
            .shouldSee(onMessagePage().createLabelPopup())
            .inputsTextInElement(onMessagePage().createLabelPopup().markNameInbox(), CUSTOM_LABEL)
            .clicksOn(onMessagePage().createLabelPopup().createMarkButton())
            .shouldNotSee(onMessagePage().createLabelPopup());
        user.messagesSteps().shouldSeeThatMessageIsLabeledWith(CUSTOM_LABEL, subject);
    }
}
