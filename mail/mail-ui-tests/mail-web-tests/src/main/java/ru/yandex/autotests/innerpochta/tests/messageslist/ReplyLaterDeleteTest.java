package ru.yandex.autotests.innerpochta.tests.messageslist;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.experiments.ExperimentsConstants.REPLY_LATER_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.REPLY_LATER;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на удаление напоминания о письме «Напомнить позже»")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class ReplyLaterDeleteTest extends BaseTest {

    private static final String REPLY_LATER_FOLDER_SYMBOL = "#reply_later";

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
    @Title("Удаляем напоминание до закрепа в списке писем")
    @TestCaseId("6375")
    public void shouldDeleteReminderInMsgList() {
        String folderName = getRandomName();
        user.apiFoldersSteps().createNewFolder(folderName);
        user.apiMessagesSteps().moveMessagesFromFolderToFolder(folderName, msg)
            .doReplyLaterForTomorrow(msg, 1);
        user.defaultSteps().opensDefaultUrlWithPostFix(REPLY_LATER_FOLDER_SYMBOL);
        user.messagesSteps().openReplyLaterDropdown(0);
        user.defaultSteps()
            .clicksOn(onMessagePage().replyLaterDropDown().get(onMessagePage().replyLaterDropDown().size() - 1))
            .shouldBeOnUrlWith(QuickFragments.INBOX);
        user.apiMessagesSteps().shouldGetMsgCountViaApi(REPLY_LATER, 0);
        user.apiMessagesSteps().shouldGetMsgCountViaApi(folderName, 1);
    }

    @Test
    @Title("Удаляем напоминание до закрепа из плашки в просмотре письма")
    @TestCaseId("6381")
    public void shouldDeleteReminder() {
        user.apiMessagesSteps().doReplyLaterForTomorrow(msg, 1);
        user.defaultSteps().opensDefaultUrlWithPostFix(REPLY_LATER_FOLDER_SYMBOL)
            .clicksOn(onMessagePage().displayedMessages().list().waitUntil(not(empty())).get(0))
            .clicksOn(onMessageView().deleteReminderBtn())
            .shouldBeOnUrlWith(QuickFragments.INBOX);
        user.apiMessagesSteps().shouldGetMsgCountViaApi(REPLY_LATER, 0);
    }

    @Test
    @Title("Удаляем напоминание после закрепа из плашки в просмотре письма")
    @TestCaseId("6382")
    public void shouldDeleteReminderInPin() {
        user.apiMessagesSteps().doReplyLater(msg);
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().displayedMessages().list().waitUntil(not(empty())).get(0))
            .clicksOn(onMessageView().deleteReminderBtn())
            .shouldBeOnUrlWith(QuickFragments.INBOX);
    }

    @Test
    @Title("Удаляем напоминание после закрепа в списке писем")
    @TestCaseId("6376")
    public void shouldDeleteReminderInPinMsgList() {
        user.apiMessagesSteps().moveMessagesFromFolderToFolder(
            user.apiFoldersSteps().createNewFolder(getRandomName()).getName(),
            msg
        );
        user.apiMessagesSteps().doReplyLaterForTomorrow(msg, 1);
        user.defaultSteps().opensDefaultUrlWithPostFix(REPLY_LATER_FOLDER_SYMBOL);
        user.apiFoldersSteps().deleteAllCustomFolders();
        user.messagesSteps().openReplyLaterDropdown(0);
        user.defaultSteps()
            .clicksOn(onMessagePage().replyLaterDropDown().get(onMessagePage().replyLaterDropDown().size() - 1))
            .shouldBeOnUrlWith(QuickFragments.INBOX);
        user.apiMessagesSteps().shouldGetMsgCountViaApi(REPLY_LATER, 0);
        user.apiMessagesSteps().shouldGetMsgCountViaApi(INBOX, 1);
    }
}
