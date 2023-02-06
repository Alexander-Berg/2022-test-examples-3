package ru.yandex.autotests.innerpochta.tests.autotests.messageslist;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SENT_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INARCHIVE;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INFOLDER;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.FREEZE_DONE_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ARCHIVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TEMPLATE;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Общие тесты на список писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class GeneralMsgListTest {

    private String subj;

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
    public void login() {
        subj = getRandomName();
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), getRandomName(), "");
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), subj, "");
        steps.user().apiFoldersSteps().createArchiveFolder();
        steps.user().apiFoldersSteps().createNewFolder(getRandomName());
        steps.user().apiMessagesSteps().createTemplateMessage(accLock.firstAcc());
        steps.user().apiMessagesSteps().createDraftMessage();
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("У черновиков и шаблонов не должно быть прыщика непрочитанности")
    @TestCaseId("405")
    @DataProvider({DRAFT, TEMPLATE})
    public void shouldNotSeeToggleForDraftsAndTemplates(String folderName) {
        String fid = steps.user().apiFoldersSteps().getFolderBySymbol(folderName).getFid();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(fid))
            .shouldSee(steps.pages().touch().messageList().headerBlock())
            .shouldSee(steps.pages().touch().messageList().messages().get(0))
            .shouldNotSee(steps.pages().touch().messageList().messages().get(0).toggler());
    }

    @Test
    @Title("Переходим в папку при тапе на статуслайн о переносе письма")
    @TestCaseId("152")
    public void shouldOpenFolderByTapInStatusline() {
        makeActionFromSwipeMenu(INFOLDER.btn());
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(0))
            .clicksOn(steps.pages().touch().messageList().statusLineInfo())
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .shouldBeOnUrl(
                containsString(
                    FOLDER_ID.makeTouchUrlPart(steps.user().apiFoldersSteps().getAllUserFolders().get(0).getFid())
                )
            )
            .shouldSeeThatElementTextEquals(steps.pages().touch().messageList().messageBlock().subject(), subj);
    }

    @Test
    @Title("Переходим в архив при тапе на статуслайн о переносе письма")
    @TestCaseId("152")
    public void shouldOpenArchiveByTapInStatusline() {
        makeActionFromSwipeMenu(INARCHIVE.btn());
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().statusLineInfo())
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .shouldBeOnUrl(
                containsString(
                    FOLDER_ID.makeTouchUrlPart(steps.user().apiFoldersSteps().getFolderBySymbol(ARCHIVE).getFid())
                )
            )
            .shouldSeeThatElementTextEquals(steps.pages().touch().messageList().messageBlock().subject(), subj);
    }

    @Test
    @Title("Должны перейти в композ из шапки в списке писем")
    @TestCaseId("1053")
    public void shouldOpenComposeFromMsgList() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().headerBlock().compose());
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().inputBody())
            .shouldBeOnUrlWith(COMPOSE);
    }

    @Test
    @Title("Должны выделить одиночное письмо и тред лонгтапом")
    @TestCaseId("1058")
    @DataProvider({"0", "1"})
    public void shouldCheckMsgWithLongtap(int num) {
        steps.user().touchSteps()
            .longTap(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(num));
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().checkedMessageBlock());
        steps.user().touchSteps().longTap(steps.pages().touch().messageList().messages().get(num));
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messageBlock())
            .shouldNotSee(steps.pages().touch().messageList().checkedMessageBlock());
    }

    @Test
    @Title("Тред становится прочитанным во всех папках")
    @TestCaseId("1182")
    public void shouldSeeThreadReadInAllFolders() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(SENT_FOLDER))
            .shouldSee(steps.pages().touch().messageList().messages().get(1).unreadToggler())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().inboxFolder())
            .clicksOn(steps.pages().touch().messageList().messages().get(1).unreadToggler())
            .shouldNotSee(steps.pages().touch().messageList().messages().get(1).unreadToggler())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().sentFolder())
            .shouldNotSee(steps.pages().touch().messageList().messages().get(1).unreadToggler());
    }

    @Test
    @Title("Должны открыть письмо из статуслайна о новом письме")
    @TestCaseId("150")
    public void shouldOpenMsgFromNewMsgStatusline() {
        String subj = Utils.getRandomName();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(SENT_FOLDER));
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().statusLineNewMsg())
            .shouldSeeThatElementHasText(steps.pages().touch().messageView().threadHeader(), subj);
    }

    @Test
    @Title("Должны закрыть статуслайн о новом письме")
    @TestCaseId("150")
    public void shouldCloseNewMsgStatusline() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(SENT_FOLDER));
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), Utils.getRandomName(), "");
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().statusLineNewMsg())
            .executesJavaScript(FREEZE_DONE_SCRIPT)
            .clicksOn(steps.pages().touch().messageList().statuslineClose())
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .shouldNotSee(steps.pages().touch().messageList().statusLineNewMsg());
    }

    @Step("Открываем меню действий с письмом, нажимаем кнопку")
    private void makeActionFromSwipeMenu(String btnName) {
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageList().bootPage());
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messageBlock().swipeFirstBtn())
            .shouldSee(steps.pages().touch().messageList().popup())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), btnName);
    }
}
