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
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.PIN;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.UNPIN;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Закрепленные письма")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class PinnedLettersTest {

    private static final int TREAD_SIZE = 3;
    private static final int PINNED_COUNT = 1;

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
    public void fillInbox() {
        steps.user().apiMessagesSteps().sendThread(accLock.firstAcc(), Utils.getRandomName(), TREAD_SIZE);
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Запиниваем/распиниваем письма")
    @Description("Запиниваем письмо из треда через апи. В списке писем видим одно запиненное, не весь тред. \n" +
        "Распиниваем письмо через апи. Блок запиненных исчезает")
    @TestCaseId("356")
    public void shouldPinAndUnpinLetter() {
        Message pinUnpinMessage = steps.user().apiMessagesSteps().getAllMessages().get(0);
        steps.user().apiLabelsSteps().pinLetter(pinUnpinMessage);
        steps.user().defaultSteps()
            .refreshPage()
            .shouldSee(steps.pages().touch().messageList().pinnedLettersToolbar())
            .shouldHasText(
                steps.pages().touch().messageList().pinnedLettersToolbar().counter(),
                String.valueOf(PINNED_COUNT)
            );
        steps.user().apiLabelsSteps().unPinLetter(pinUnpinMessage);
        steps.user().defaultSteps()
            .refreshPage()
            .shouldNotSee(steps.pages().touch().messageList().pinnedLettersToolbar());
    }

    @Test
    @Title("Должны вернуться из Поиска по закрепленным обратно во Входящие")
    @TestCaseId("869")
    public void shouldReturnFromPinned() {
        steps.user().apiLabelsSteps().pinLetter(steps.user().apiMessagesSteps().getAllMessages().get(0));
        steps.user().defaultSteps().refreshPage()
            .clicksOn(
                steps.pages().touch().messageList().pinnedLettersToolbar(),
                steps.pages().touch().messageList().headerBlock().search()
            )
            .clicksAndInputsText(steps.pages().touch().search().header().input(), Utils.getRandomName())
            .clicksOn(steps.pages().touch().search().header().find())
            .shouldSee(steps.pages().touch().search().emptySearchResultImg())
            .clicksOn(
                steps.pages().touch().search().header().back(),
                steps.pages().touch().messageList().headerBlock().sidebar()
            )
            .shouldBeOnUrlWith(QuickFragments.INBOX_FOLDER);
    }

    @Test
    @Title("Проверяем, что одиночное письмо нельзя запинить из всех системных папок кроме входящих")
    @TestCaseId("25")
    @DataProvider({SPAM, SENT, TRASH})
    public void shouldNotPinMsgFromNotInbox(String folder) {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем треды у пользователя",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
        steps.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, folder);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            FOLDER_ID.makeTouchUrlPart(
                steps.user().apiFoldersSteps().getFolderBySymbol(folder).getFid()
            )
        );
        steps.user().touchSteps().openActionsForMessages(0);
        steps.user().defaultSteps()
            .shouldNotSeeElementInList(steps.pages().touch().messageView().btnsList(), PIN.btn());
    }

    @Test
    @Title("Должен обновиться список закреплённых при закреплении/откреплении письма")
    @TestCaseId("649")
    public void shouldUpdatePinnedLettersList() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем треды у пользователя",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
        steps.user().apiLabelsSteps().pinLetter(steps.user().apiMessagesSteps().getAllMessages().get(1));
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().touch().messageList().pinnedLettersToolbar())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 1)
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar());
        steps.user().touchSteps().openActionsForMessages(0);
        steps.user().defaultSteps().clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), PIN.btn())
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .clicksOn(steps.pages().touch().messageList().pinnedLettersToolbar())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 2);
    }

    @Test
    @Title("Должен открепить все письма увидеть пустой список закреплённых")
    @TestCaseId("446")
    public void shouldUnpinAllMsgs() {
        steps.user().apiLabelsSteps().pinLetter(steps.user().apiMessagesSteps().getAllMessages().get(1));
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().touch().messageList().pinnedLettersToolbar())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 1);
        steps.user().touchSteps().openActionsForMessages(0);
        steps.user().defaultSteps().clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), UNPIN.btn())
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg());
    }
}
