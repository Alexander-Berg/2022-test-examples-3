package ru.yandex.autotests.innerpochta.tests.messageslist;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SPAM_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.TRASH_FOLDER;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.experiments.ExperimentsConstants.REPLY_LATER_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.REPLY_LATER;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Общие тесты на фичу «Напомнить позже»")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.REPLY_LATER)
@RunWith(DataProviderRunner.class)
public class ReplyLaterTest extends BaseTest {

    private static final String REPLY_LATER_FOLDER_SYMBOL = "#reply_later";
    private static final String CHANGED_TIME = "12:00";
    private static final String PROMO_REPLY_LATER_PARAM = "?promo=promo-reply-later#";
    private static final String REPLY_LATER_NAME = "Напомнить позже";
    private static final String DATE = "?debugClientPromoDate=2022-05-13T20%3A00";

    Message msg;

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
    public void logIn() throws IOException {
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomString(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().addExperimentsWithYexp(REPLY_LATER_EXP);
    }

    @Test
    @Title("Нельзя включить напоминание о письмах в папкам Спам и Удалённые")
    @TestCaseId("6380")
    @DataProvider({SPAM_FOLDER, TRASH_FOLDER})
    public void shouldNotSeeReplyLaterInFolders(String fid) {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomString(), "");
        user.apiMessagesSteps().moveMessagesToSpam(user.apiMessagesSteps().getAllMessages().get(0))
            .deleteMessages(user.apiMessagesSteps().getAllMessages().get(0));
        user.defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeUrlPart(fid));
        user.messagesSteps().clicksOnMessageCheckBoxByNumber(0);
        user.defaultSteps().shouldNotSee(onMessagePage().toolbar().replyLaterBtn());
    }

    @Test
    @Title("Действие «Напомнить позже» недоступно для нескольких писем сразу")
    @TestCaseId("6402")
    public void shouldDisableReplyLaterInToolbarForFewMsgs() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomString(), "");
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageCheckBoxByNumber(0);
        user.defaultSteps().shouldSee(user.pages().MessagePage().toolbar().replyLaterBtn());
        user.messagesSteps().clicksOnMessageCheckBoxByNumber(1);
        user.defaultSteps().shouldNotSee(user.pages().MessagePage().toolbar().replyLaterBtn());
    }

    @Test
    @Title("Редактируем дату напоминания в папке «Ответить позже»")
    @TestCaseId("6397")
    public void shouldChangeReminderTime() {
        user.apiMessagesSteps().doReplyLaterForTomorrow(msg, 1);
        user.defaultSteps().opensDefaultUrlWithPostFix(REPLY_LATER_FOLDER_SYMBOL);
        user.messagesSteps().openReplyLaterDropdown(0);
        user.defaultSteps()
            .clicksOn(onMessagePage().replyLaterDropDown().get(onMessagePage().replyLaterDropDown().size() - 3))
            .shouldSeeThatElementHasText(onMessagePage().displayedMessages().list().get(0).date(), CHANGED_TIME);
    }

    @Test
    @Title("Редактируем дату напоминания в папке «Ответить позже»")
    @Description("Тест работает только на тестинге и престейбле. На проде не работает.")
    @TestCaseId("6398")
    public void shouldChangeReminderTimeInPin() {
        user.apiMessagesSteps().doReplyLater(msg);
        user.defaultSteps().refreshPage();
        user.messagesSteps().openReplyLaterDropdown(0);
        user.defaultSteps()
            .clicksOn(onMessagePage().replyLaterDropDown().get(0))
            .shouldSee(onMessagePage().emptyFolder());
        user.apiMessagesSteps().shouldGetMsgCountViaApi(INBOX, 0);
        user.apiMessagesSteps().shouldGetMsgCountViaApi(REPLY_LATER, 1);
    }

    @Test
    @Title("Должны закрыть промо «Напомнить позже»")
    @TestCaseId("6383")
    public void shouldCloseReplyLaterPromo() {
        user.defaultSteps()
            .opensDefaultUrlWithPostFix(PROMO_REPLY_LATER_PARAM + QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .shouldSee(user.pages().MessageViewPage().replyLaterPromo())
            .clicksOn(user.pages().MessageViewPage().replyLaterPromoClose())
            .shouldNotSee(user.pages().MessageViewPage().replyLaterPromo());
    }

    @Test
    @Title("Выпадушка «Напомнить позже» закрывается при выходе из просмотра письма")
    @TestCaseId("6404")
    public void shouldCloseReplyLaterDropDown() {
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .clicksOn(onMessageView().toolbar().replyLaterBtn());
        webDriverRule.getDriver().navigate().back();
        user.defaultSteps().shouldNotSee(user.pages().MessageViewPage().replyLaterPromo());
    }

    @Test
    @Title("Нельзя dnd письмо в папку «Напомнить позже»")
    @TestCaseId("6379")
    public void shouldNotDragMessageToFolderReplyLater() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomString(), "");
        user.apiMessagesSteps().doReplyLaterForTomorrow(msg, 1);
        user.defaultSteps().refreshPage()
            .dragAndDrop(
                onMessagePage().displayedMessages().list().get(0).subject(),
                onMessagePage().foldersNavigation().replyLaterFolder()
            );
        user.apiMessagesSteps().shouldGetMsgCountViaApi(REPLY_LATER, 1);
        user.apiMessagesSteps().shouldGetMsgCountViaApi(INBOX, 1);
    }

    @Test
    @Title("Нельзя dnd письмо из папки «Напомнить позже»")
    @TestCaseId("6379")
    public void shouldNotDragMessageToFolderInbox() {
        user.apiMessagesSteps().doReplyLaterForTomorrow(msg, 1);
        user.defaultSteps().opensDefaultUrlWithPostFix(REPLY_LATER_FOLDER_SYMBOL)
            .dragAndDrop(
                onMessagePage().displayedMessages().list().get(0).subject(),
                onMessagePage().foldersNavigation().replyLaterFolder()
            );
        user.apiMessagesSteps().shouldGetMsgCountViaApi(REPLY_LATER, 1);
        user.apiMessagesSteps().shouldGetMsgCountViaApi(INBOX, 0);
    }

    @Test
    @Title("Папки «Напомнить позже» нет в списке папок для перемещения")
    @TestCaseId("6379")
    public void shouldNotSeeReplyLaterFolderInList() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomString(), "");
        user.apiMessagesSteps().doReplyLaterForTomorrow(msg, 1);
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageCheckBoxByNumber(0);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().moveMessageBtn())
            .shouldNotSeeElementInList(
                user.pages().MessagePage().moveMessageDropdownMenu().customFolders(),
                REPLY_LATER_NAME
            );
    }

    @Test
    @Title("Если выделено хотя бы одно письмо с напоминанием, закрепить в тулбаре блокируется")
    @Description("Тест работает только на тестинге и престейбле. На проде не работает.")
    @TestCaseId("6395")
    public void shouldDisableActionsForPinnedLetterChecked() {
        user.apiMessagesSteps().doReplyLater(msg);
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomString(), "");
        user.defaultSteps().waitInSeconds(2)
            .refreshPage();
        user.messagesSteps().clicksOnMessageCheckBoxByNumber(1);
        user.defaultSteps().shouldSee(user.pages().MessagePage().toolbar().pinBtn());
        user.messagesSteps().clicksOnMessageCheckBoxByNumber(0);
        user.defaultSteps().shouldNotSee(user.pages().MessagePage().toolbar().pinBtn());
    }

    @Test
    @Title("Если выделено хотя бы одно письмо с напоминанием, закрепить в тулбаре блокируется")
    @TestCaseId("6392")
    public void shouldDisableActionsForDelayedLetter() {
        user.apiMessagesSteps().doReplyLaterForTomorrow(msg, 1);
        user.defaultSteps().refreshPage()
            .opensDefaultUrlWithPostFix(REPLY_LATER_FOLDER_SYMBOL);
        user.messagesSteps().clicksOnMessageCheckBoxByNumber(0);
        user.defaultSteps().shouldNotSee(
            user.pages().MessagePage().toolbar().archiveButton(),
            user.pages().MessagePage().toolbar().pinBtn(),
            user.pages().MessagePage().toolbar().moreBtn(),
            user.pages().MessagePage().toolbar().addCustomButton()
        );
    }

    @Test
    @Title("Набор пресетов зависит от времени")
    @TestCaseId("6403")
    public void shouldChangePresets() {
        user.defaultSteps().opensDefaultUrlWithPostFix(DATE);
        user.messagesSteps().clicksOnMessageCheckBoxByNumber(0);
        user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().replyLaterBtn())
            .shouldSeeElementsCount(user.pages().MessagePage().replyLaterDropDown(), 3);
    }
}
