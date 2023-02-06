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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INFOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.MARKLABEL;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Общие тесты на попапы в списке писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class PopupTest {

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
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), 3);
        List<Message> messages = steps.user().apiMessagesSteps().getAllMessagesInFolder(INBOX);
        steps.user().apiMessagesSteps().deleteMessages(messages.get(0))
            .moveMessagesToSpam(messages.get(1));
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Должны очистить папки «Удаленные» и «Спам» из диалога в списке писем")
    @TestCaseId("880")
    @DataProvider({TRASH, SPAM})
    public void shouldClearFoldersFromMsgList(String folder) {
        String fid = steps.user().apiFoldersSteps().getFolderBySymbol(folder).getFid();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(fid))
            .shouldSee(steps.pages().touch().messageList().headerBlock())
            .clicksOn(steps.pages().touch().messageList().clearFolderButton())
            .clicksOn(steps.pages().touch().messageList().clearFolderPopup().confirm())
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folder, 0);
    }

    @Test
    @Title("Должен закрыться попап очистки папки при нажатии отмены в списке писем")
    @TestCaseId("879")
    @DataProvider({TRASH, SPAM})
    public void shouldCloseClearFolderPopupFromMsgList(String folder) {
        String fid = steps.user().apiFoldersSteps().getFolderBySymbol(folder).getFid();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(fid))
            .shouldSee(steps.pages().touch().messageList().headerBlock())
            .clicksOn(steps.pages().touch().messageList().clearFolderButton())
            .clicksOn(steps.pages().touch().messageList().clearFolderPopup().cancelClear())
            .shouldSee(steps.pages().touch().messageList().messageBlock());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folder, 1);
    }

    @Test
    @Title("Должен закрыться попап выбора папки")
    @TestCaseId("878")
    public void shouldCloseFolderPopup() {
        openActionForMsgesPopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), INFOLDER.btn())
            .shouldSee(steps.pages().touch().messageList().folderPopup())
            .clicksOn(steps.pages().touch().messageList().folderPopup().closePopup())
            .shouldNotSee(steps.pages().touch().messageList().folderPopup());
    }

    @Test
    @Title("Должен закрыться попап выбора метки по тапу на крестик")
    @TestCaseId("881")
    public void shouldCloseLabelPopupByCross() {
        openActionForMsgesPopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKLABEL.btn());
        closePopupByCross();
    }

    @Test
    @Title("Должен закрыться попап выбора метки при тапе на кнопку «Готово»")
    @TestCaseId("882")
    public void shouldCloseLabelPopupByBtn() {
        openActionForMsgesPopup();
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKLABEL.btn())
            .shouldSee(steps.pages().touch().messageList().popup())
            .clicksOn(steps.pages().touch().messageList().popup().done())
            .shouldNotSee(steps.pages().touch().messageList().popup());
    }

    @Test
    @Title("Должен закрыться попап «Действия с письмом» по тапу на крестик")
    @TestCaseId("883")
    public void shouldCloseActionPopupByCross() {
        openActionForMsgesPopup();
        closePopupByCross();
    }

    @Step("Закрываем попап тапом на крестик")
    private void closePopupByCross() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().popup())
            .clicksOn(steps.pages().touch().messageList().popup().closeBtn())
            .shouldNotSee(steps.pages().touch().messageList().popup());
    }

    @Step("Открываем попап действий с письмом из свайп-меню")
    private void openActionForMsgesPopup() {
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps()
            .offsetClick(steps.pages().touch().messageList().messageBlock().swipeFirstBtnDraft(), 11, 11)
            .shouldSee(steps.pages().touch().messageList().popup());
    }
}
