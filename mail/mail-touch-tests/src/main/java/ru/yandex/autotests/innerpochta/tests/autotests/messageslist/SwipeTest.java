package ru.yandex.autotests.innerpochta.tests.autotests.messageslist;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.interactions.Actions;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.aspectj.runtime.internal.Conversions.intValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.DRAFT_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ScriptNumbers.MIN_LONGSWIPE_MOVE;
import static ru.yandex.autotests.innerpochta.touch.data.ScriptNumbers.MIN_RIGHT_SWIPE_MOVE;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Общие тесты на список писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class SwipeTest {

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
        steps.user().apiMessagesSteps().createDraftMessage();
        steps.user().apiMessagesSteps().sendThread(accLock.firstAcc(), Utils.getRandomName(), 2);
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[][] swipeInInboxAndDraft() {
        return new Object[][]{
            {INBOX_FOLDER.makeTouchUrlPart(), INBOX},
            {FOLDER_ID.makeTouchUrlPart(DRAFT_FOLDER), DRAFT}
        };
    }

    @DataProvider
    public static Object[][] swipeInInboxAndDraftWithSwipeMenuSize() {
        return new Object[][]{
            {INBOX_FOLDER.makeTouchUrlPart(), INBOX, 140},
            {FOLDER_ID.makeTouchUrlPart(DRAFT_FOLDER), DRAFT, 70}
        };
    }

    @Test
    @Title("Свайп-меню у письма")
    @TestCaseId("351")
    public void useSwipeMenuOnMail() {
        steps.user().touchSteps().openActionsForMessages(0);
    }

    @Test
    @Title("Удаление из свайп меню")
    @TestCaseId("712")
    @UseDataProvider("swipeInInboxAndDraft")
    public void useSwipeMenuDelete(String folderFrom, String folderTo) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(folderFrom);
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messageBlock().swipeDelBtn())
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folderTo, 0);
    }

    @Test
    @Title("Должно закрыться свайп-меню по тапу")
    @TestCaseId("718")
    public void closeSwipeByTap() {
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageList().messages().get(0).avatar())
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .shouldSeeInViewport(steps.pages().touch().messageList().messages().get(0).avatar());
    }

    @Test
    @Title("Режим групповых операций неактивен при развернутом свайп-меню")
    @TestCaseId("720")
    public void inactiveGroupOperationWhenSwipe() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем тредный режим",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
        steps.user().defaultSteps().refreshPage();
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageList().messages().get(0).avatar())
            .clicksOn(steps.pages().touch().messageList().messages().get(1).avatar())
            .shouldSee(steps.pages().touch().messageList().messages().get(0).avatar())
            .shouldNotSee(steps.pages().touch().messageList().checkedMessageBlock());
    }

    @Test
    @Title("Нельзя развернуть свайп-меню, если активен режим групповых операций")
    @TestCaseId("750")
    public void inactiveSwipeWhenGroupOperation() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем тредный режим",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().touch().messageList().messages().get(1).avatar())
            .shouldSee(steps.pages().touch().messageList().checkedMessageBlock());
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().shouldSeeInViewport(steps.pages().touch().messageList().messages().get(0).avatar());
    }

    @Test
    @Title("Помечаем письмо прочитанным с помощью свайпа")
    @TestCaseId("708")
    public void shouldMarkMsgReadWithSwipe() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messages().get(0).unreadToggler());
        steps.user().touchSteps()
            .leftSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageList().messages().get(0).unreadToggler());
        steps.user().touchSteps()
            .leftSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messages().get(0).unreadToggler());
    }

    @Test
    @Title("Свайп сворачивается при переходе в композ")
    @TestCaseId("456")
    public void shouldCollapseSwipeAfterCompose() {
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messageBlock().swipeFirstBtn())
            .clicksOn(steps.pages().touch().messageList().headerBlock().compose());
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().closeBtn())
            .shouldSeeInViewport(steps.pages().touch().messageList().messageBlock().avatar());
    }

    @Test
    @Title("Свайп сворачивается при переходе в поиск")
    @TestCaseId("456")
    public void shouldCollapseSwipeAfterSearch() {
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messageBlock().swipeFirstBtn())
            .clicksOn(steps.pages().touch().messageList().headerBlock().search())
            .clicksOn(steps.pages().touch().search().header().back())
            .shouldSeeInViewport(steps.pages().touch().messageList().messageBlock().avatar());
    }

    @Test
    @Title("Свайп сворачивается при открытии списка папок")
    @TestCaseId("1071")
    public void shouldCollapseSwipeAfterSidebar() {
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messageBlock().swipeFirstBtn())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().folderBlocks().get(0))
            .shouldSeeInViewport(steps.pages().touch().messageList().messageBlock().avatar());
    }

    @Test
    @Title("Нельзя развернуть свайп-меню в подгруженных письмах, если активен режим групповых операций")
    @TestCaseId("1138")
    public void inactiveSwipeForOldMsgsWhenGroupOperation() {
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), 12);
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем тредный режим",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().touch().messageList().messages().get(1).avatar())
            .shouldSee(steps.pages().touch().messageList().checkedMessageBlock());
        int msgNum = steps.user().touchSteps().scrollMsgListDown();
        steps.user().touchSteps().rightSwipe(steps.pages().touch().messageList().messages().get(msgNum));
        steps.user().defaultSteps()
            .shouldSeeInViewport(steps.pages().touch().messageList().messages().get(msgNum).avatar());
    }

    @Test
    @Title("Удаление лонгсвайпом")
    @TestCaseId("707")
    @UseDataProvider("swipeInInboxAndDraft")
    public void deleteWithLongswipe(String folderFrom, String folderTo) {
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), Utils.getRandomString(), "");
        steps.user().apiMessagesSteps().createDraftWithSubject(Utils.getRandomString());
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(folderFrom);
        steps.user().touchSteps()
            .longSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(1));
        steps.user().defaultSteps().shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 1);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folderTo, 1);
    }

    @Test
    @Title("Свайп-меню раздвигается в лонгсвайп")
    @TestCaseId("716")
    @UseDataProvider("swipeInInboxAndDraftWithSwipeMenuSize")
    public void shouldLongswipeAfterSwipeInbox(String folderFrom, String folderTo, int menuSize) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(folderFrom);
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messages().get(0).swipeDelBtn());
        new Actions(steps.getDriver()).moveToElement(
            steps.pages().touch().messageList().messages().get(0),
            steps.pages().touch().messageList().messages().get(0).getSize().getWidth() - menuSize,
            0
        )
            .clickAndHold()
            .moveByOffset(
                intValue(
                    steps.pages().touch().messageList().messages().get(0).getSize().getWidth() * MIN_LONGSWIPE_MOVE
                ) - 1 + menuSize,
                0
            )
            .release()
            .perform();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().emptyFolderImg());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folderTo, 0);
    }

    @Test
    @Title("Свайп сворачивается при ptr")
    @TestCaseId("719")
    @DoTestOnlyForEnvironment("iOS")
    @Issue("QUINN-5544")
    @ConditionalIgnore(condition = TicketInProgress.class)
    public void shouldHideSwipeAfterPtr() {
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageList().messageBlock().avatar());
        steps.user().touchSteps().ptr();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messageBlock().avatar());
    }

    @Test
    @Title("Свайпнуть письмо за свободную область аттачей")
    @TestCaseId("765")
    public void shouldSwipeMsgWithAttachment() {
        steps.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            accLock.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString(),
            IMAGE_ATTACHMENT
        );
        steps.user().defaultSteps().refreshPage()
            .shouldSeeElementsCount(
                steps.pages().touch().messageList().messageBlock().attachmentsInMessageList(),
                1
            );
        swipeMsgWithAttach();
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageList().messageBlock().avatar());
    }

    @Test
    @Title("Письмо не свайпается за аттачи")
    @TestCaseId("764")
    public void shouldNotSwipeLeftMsgWithAttachment() {
        steps.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            accLock.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString(),
            IMAGE_ATTACHMENT,
            PDF_ATTACHMENT,
            IMAGE_ATTACHMENT,
            PDF_ATTACHMENT
        );
        steps.user().defaultSteps().refreshPage()
            .shouldSeeElementsCount(
                steps.pages().touch().messageList().messageBlock().attachmentsInMessageList(),
                4
            );
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messageBlock().attachmentsInMessageList().get(1));
        steps.user().defaultSteps().shouldSee(
            steps.pages().touch().messageList().messageBlock().avatar(),
            steps.pages().touch().messageList().messageBlock().arrorBack()
        );
        steps.user().touchSteps().rightSwipe(steps.pages().touch().messageList().messageBlock().arrorNext());
        steps.user().defaultSteps().shouldSee(
            steps.pages().touch().messageList().messageBlock().avatar(),
            steps.pages().touch().messageList().messageBlock().arrorBack()
        );
    }

    @Test
    @Title("Свайпнуть письмо за аттачи вправо")
    @TestCaseId("234")
    public void shouldNotSwipeRightMsgWithAttachment() {
        steps.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            accLock.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString(),
            IMAGE_ATTACHMENT,
            IMAGE_ATTACHMENT,
            IMAGE_ATTACHMENT,
            IMAGE_ATTACHMENT
        );
        steps.user().defaultSteps().refreshPage()
            .shouldSeeElementsCount(
                steps.pages().touch().messageList().messageBlock().attachmentsInMessageList(),
                4
            );
        steps.user().touchSteps()
            .leftSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messageBlock().unreadToggler());
    }

    @Test
    @Title("Свайп работает после открытия вьюера")
    @TestCaseId("1254")
    public void shouldSwipeMsgAfterOpenAttachment() {
        steps.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            accLock.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString(),
            IMAGE_ATTACHMENT
        );
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().touch().messageList().messageBlock().attachmentsInMessageList().get(0))
            .clicksOn(steps.pages().touch().messageView().viewer().viewerClose());
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0).subject());
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageList().messageBlock().avatar());
    }

    @Step("Свайпаем письмо за область относительно аттачей")
    private void swipeMsgWithAttach() {
        MailElement element = steps.pages().touch().messageList().messageBlock().attachmentsInMessageList().get(0);
        new Actions(steps.getDriver())
            .moveToElement(
                element,
                element.getSize().getWidth() + 20,
                element.getSize().getHeight() / 2
            )
            .clickAndHold()
            .moveByOffset(MIN_RIGHT_SWIPE_MOVE, 0)
            .release()
            .perform();
    }
}
