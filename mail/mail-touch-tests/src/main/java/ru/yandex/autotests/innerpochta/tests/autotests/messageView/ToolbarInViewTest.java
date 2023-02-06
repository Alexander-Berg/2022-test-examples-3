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
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule.addLabelIfNeed;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.DELETE;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INARCHIVE;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INFOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INSPAM;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.MARKLABEL;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.MARKUNREAD;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.PIN;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ARCHIVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на тулбар в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.TOOLBAR)
@RunWith(DataProviderRunner.class)
public class ToolbarInViewTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    private Message msg;

    @DataProvider
    public static Object[][] buttonAndFolder() {
        return new Object[][]{
            {INSPAM.btn(), SPAM},
            {INARCHIVE.btn(), ARCHIVE},
            {DELETE.btn(), TRASH}
        };
    }

    @DataProvider
    public static Object[][] button() {
        return new Object[][]{
            {INSPAM.btn()},
            {INARCHIVE.btn()},
            {DELETE.btn()}
        };
    }

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(addLabelIfNeed(() -> steps.user()));

    @Before
    public void prep() {
        msg = steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), getRandomName(), "");
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid()));
    }

    @Test
    @Title("Помечаем непрочитанным")
    @TestCaseId("409")
    public void shouldSeeUnreadMessage() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            MSG_FRAGMENT.makeTouchUrlPart(msg.getMid())
        );
        openActionForMessagePopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKUNREAD.btn())
            .shouldSee(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0).unreadToggler());
    }

    @Test
    @Title("Ставим метку Важное")
    @TestCaseId("413")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeImportantMessage() {
        openActionForMessagePopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKLABEL.btn())
            .clicksOn(steps.pages().touch().messageList().popup().labels().get(0))
            .shouldSee(steps.pages().touch().messageList().popup().tick())
            .clicksOn(steps.pages().touch().messageList().popup().done())
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .clicksOn(steps.pages().touch().messageView().header().backToListBtn())
            .shouldSee(steps.pages().touch().messageList().messages().get(0).importantLabel());
    }

    @Test
    @Title("Ставим метку Важное")
    @TestCaseId("413")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeImportantMessageTablet() {
        openActionForMessagePopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKLABEL.btn())
            .clicksOn(steps.pages().touch().messageList().popup().labels().get(0))
            .shouldSeeElementsCount(steps.pages().touch().messageList().popup().tick(), 1)
            .clicksOn(steps.pages().touch().messageList().popup().done())
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldSee(steps.pages().touch().messageList().messages().get(0).importantLabel());
    }

    @Test
    @Title("Ставим пользовательскую метку")
    @TestCaseId("411")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeLabelMessage() {
        openActionForMessagePopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKLABEL.btn())
            .clicksOn(steps.pages().touch().messageList().popup().labels().get(1))
            .shouldSee(steps.pages().touch().messageList().popup().tick())
            .clicksOn(steps.pages().touch().messageList().popup().done())
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .clicksOn(steps.pages().touch().messageView().header().backToListBtn())
            .shouldSee(steps.pages().touch().messageList().messages().get(0).label());
        steps.user().apiMessagesSteps()
            .shouldGetMsgCountInLabelViaApi(steps.user().apiLabelsSteps().getAllUserLabels().get(0).getName(), 1);
    }

    @Test
    @Title("Ставим пользовательскую метку")
    @TestCaseId("411")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeLabelMessageTablet() {
        openActionForMessagePopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKLABEL.btn())
            .clicksOn(steps.pages().touch().messageList().popup().labels().get(1))
            .shouldSeeElementsCount(steps.pages().touch().messageList().popup().tick(), 1)
            .clicksOn(steps.pages().touch().messageList().popup().done())
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldSee(steps.pages().touch().messageList().messages().get(0).label());
        steps.user().apiMessagesSteps()
            .shouldGetMsgCountInLabelViaApi(steps.user().apiLabelsSteps().getAllUserLabels().get(0).getName(), 1);
    }

    @Test
    @Title("Пиним письмо")
    @TestCaseId("412")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeePinMessage() {
        openActionForMessagePopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), PIN.btn())
            .clicksOn(steps.pages().touch().messageView().header().backToListBtn())
            .shouldHasText(steps.pages().touch().messageList().pinnedLettersToolbar().counter(), "1")
            .clicksOn(steps.pages().touch().messageList().pinnedLettersToolbar())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 1)
            .shouldHasText(steps.pages().touch().messageList().messages().get(0).subject(), msg.getSubject());
    }

    @Test
    @Title("Пиним письмо")
    @TestCaseId("412")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeePinMessageTablet() {
        openActionForMessagePopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), PIN.btn())
            .shouldHasText(steps.pages().touch().messageList().pinnedLettersToolbar().counter(), "1")
            .clicksOn(steps.pages().touch().messageList().pinnedLettersToolbar())
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 1)
            .shouldHasText(steps.pages().touch().messageList().messages().get(0).subject(), msg.getSubject());
    }

    @Test
    @Title("Перекладываем в другую папку")
    @TestCaseId("410")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeMessageInCustomFolder() {
        Folder userFolder = steps.user().apiFoldersSteps().createNewFolder(Utils.getRandomName());
        steps.user().defaultSteps().refreshPage();
        openActionForMessagePopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), INFOLDER.btn())
            .clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(0))
            .shouldSee(steps.pages().touch().messageView().toolbar())
            .clicksOn(steps.pages().touch().messageView().header().backToListBtn())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 0);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(userFolder.getName(), 1);
    }

    @Test
    @Title("Перекладываем в другую папку")
    @TestCaseId("410")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeMessageInCustomFolderTablet() {
        Folder userFolder = steps.user().apiFoldersSteps().createNewFolder(Utils.getRandomName());
        steps.user().defaultSteps().refreshPage();
        openActionForMessagePopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), INFOLDER.btn())
            .clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(0))
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 0);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(userFolder.getName(), 1);
    }

    @Test
    @Title("Перемещаем письмо в папку {1}")
    @TestCaseId("408")
    @UseDataProvider("buttonAndFolder")
    public void shouldSeeMessageInFolder(String buttonName, String folderName) {
        openActionForMessagePopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), buttonName)
            .shouldNotSee(steps.pages().touch().messageView().moreBtn())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 0);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folderName, 1);
    }

    @Test
    @Title("Проверка тулбара в шапке письма - удалить")
    @TestCaseId("259")
    public void shouldDeleteMsgFromToolbar() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().toolbar().delete())
            .shouldBeOnUrlWith(INBOX_FOLDER)
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 0);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(TRASH, 1);
    }

    @Test
    @Title("Удалить, архивировать, отметить как спам последнее письмо в папке")
    @TestCaseId("603")
    @UseDataProvider("button")
    public void shouldSeeEmptyFolder(String btnName) {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().toolbar())
            .clicksOn(steps.pages().touch().messageView().moreBtn())
            .shouldSee(steps.pages().touch().messageView().popup())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), btnName)
            .shouldNotSee(steps.pages().touch().messageView().toolbar())
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg());
    }

    @Step("Открываем меню действия с письмом")
    private void openActionForMessagePopup() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().moreBtn())
            .shouldSee(steps.pages().touch().messageList().popup());
    }
}