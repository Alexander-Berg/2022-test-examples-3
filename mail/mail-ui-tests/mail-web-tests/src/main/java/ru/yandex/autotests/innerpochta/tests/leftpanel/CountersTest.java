package ru.yandex.autotests.innerpochta.tests.leftpanel;

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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_OFF;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SAVE_SENT;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тест на счетчики")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.LEFT_PANEL)
public class CountersTest extends BaseTest {

    private String subject;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.apiSettingsSteps().callWithListAndParams(SETTINGS_SAVE_SENT, of(SETTINGS_SAVE_SENT, STATUS_OFF));
    }

    @Test
    @Title("Счётчик папки «Входящие»")
    @TestCaseId("1488")
    public void testInboxCounter() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeCorrectNumberOfMessages(1);
        int inboxCounter = user.leftColumnSteps().inboxTotalCounter();
        int unreadInboxCounter = user.leftColumnSteps().unreadCounter();
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.messagesSteps().shouldSeeMessageWithSubjectWithoutRefresh(subject);
        user.leftColumnSteps().shouldSeeTotalInboxCounter(inboxCounter + 1)
            .shouldSeeUnreadInboxCounter(unreadInboxCounter + 1);
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.messageViewSteps().shouldSeeMessageSubject(subject);
        user.leftColumnSteps().shouldSeeTotalInboxCounter(inboxCounter + 1)
            .shouldSeeUnreadInboxCounter(unreadInboxCounter);
        user.defaultSteps().opensDefaultUrl();
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().deleteButton());
        user.leftColumnSteps().shouldSeeTotalInboxCounter(inboxCounter)
            .shouldSeeUnreadInboxCounter(unreadInboxCounter);
    }

    @Test
    @Title("Счётчик папки «Черновики»")
    @TestCaseId("1490")
    public void testDraftCounter() {
        subject = Utils.getRandomName();
        user.defaultSteps().opensFragment(QuickFragments.DRAFT);
        user.defaultSteps().clicksOn(onMessagePage().composeButton());
        user.composeSteps().inputsSubject(subject)
            .shouldSeeThatMessageIsSavedToDraft();
        user.defaultSteps().opensFragment(QuickFragments.DRAFT).refreshPage();
        user.leftColumnSteps().shouldSeeDraftCounter(1);
    }

    @Test
    @Title("Счётчик непрочитанных в папке «Входящие»")
    @TestCaseId("1936")
    public void testUnreadCounter() {
        Message msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
        user.leftColumnSteps().shouldSeeInboxUnreadCounter(1);
        user.defaultSteps().clicksOn(onMessagePage().foldersNavigation().inboxUnreadCounter())
            .shouldSee(onMessagePage().foldersNavigation().inboxFolder());
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }
}
