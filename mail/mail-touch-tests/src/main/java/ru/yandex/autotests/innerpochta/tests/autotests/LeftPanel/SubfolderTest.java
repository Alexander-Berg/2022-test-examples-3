package ru.yandex.autotests.innerpochta.tests.autotests.LeftPanel;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INFOLDER;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Переход во вложенную папку")
@Features(FeaturesConst.LEFT_PANEL)
@Stories(FeaturesConst.GENERAL)
public class SubfolderTest {

    private String userSubfolder;

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
    public void prepare() {
        userSubfolder = Utils.getRandomString();
        Folder folder = steps.user().apiFoldersSteps().createNewFolder(Utils.getRandomString());
        steps.user().apiFoldersSteps().createNewSubFolder(userSubfolder, folder);
        steps.user().apiMessagesSteps().sendMailWithNoSave(
            accLock.firstAcc().getSelfEmail(), Utils.getRandomString(), Utils.getRandomString()
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Разворачиваем подпапки, переходим в подпапку")
    @TestCaseId("12")
    public void shouldOpenSubfolder() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().subfoldertoggler().waitUntil(IsNot.not(empty())).get(0))
            .clicksOnElementWithText(steps.pages().touch().sidebar().folderBlocks(), userSubfolder)
            .shouldSeeThatElementTextEquals(steps.pages().touch().messageList().headerBlock().folderName(), userSubfolder);
    }

    @Test
    @Title("Перемещаем письмо в подпапку")
    @TestCaseId("981")
    public void shouldMoveMessageToSubfolder() {
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps()
            .offsetClick(steps.pages().touch().messageList().messageBlock().swipeFirstBtn(), 11, 11)
            .shouldSee(steps.pages().touch().messageList().popup())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), INFOLDER.btn())
            .shouldSee(steps.pages().touch().messageList().folderPopup())
            .clicksOn(steps.pages().touch().messageList().folderPopup().toggler())
            .shouldSee(steps.pages().touch().messageList().folderPopup().expandedFolders())
            .clicksOnElementWithText(steps.pages().touch().messageList().folderPopup().folders(), userSubfolder)
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(userSubfolder, 1);
    }
}