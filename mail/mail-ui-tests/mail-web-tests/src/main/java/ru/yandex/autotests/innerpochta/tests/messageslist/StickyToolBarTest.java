package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.KeysOwn;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.FORWARD_PREFIX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ENABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тест на залипающий тулбар")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class StickyToolBarTest extends BaseTest {

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
    public void logIn() throws IOException {
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 10);
        subject = Utils.getRandomName();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().setsWindowSize(1600, 700);
        user.messagesSteps().shouldSeeMessageWithSubjectWithWaiting(subject);
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject)
            .scrollDownPage();
    }

    @Test
    @Title("Кнопка переслать в «залипающем» тулбаре")
    @TestCaseId("1579")
    public void testForwardButtonInStickyToolBar() {
        user.defaultSteps().shouldSee(onMessagePage().stickyToolBar())
            .clicksOn(onMessagePage().stickyToolBar().forwardButton());
        user.composeSteps().clicksOnAddEmlBtn();
        user.composeSteps().shouldSeeSubject(FORWARD_PREFIX + subject)
            .shouldSeeMessageAsAttachment(0, subject);
    }

    @Test
    @Title("Кнопка удалить в «залипающем» тулбаре")
    @TestCaseId("1580")
    public void testDeleteButtonInStickyToolBar() {
        user.defaultSteps().clicksOn(onMessagePage().stickyToolBar().deleteButtonIcon());
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.leftColumnSteps().opensTrashFolder();
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Кнопка спам в «залипающем» тулбаре")
    @TestCaseId("1582")
    public void testSpamButtonInStickyToolBar() {
        user.defaultSteps().clicksOn(onMessagePage().stickyToolBar().spamButtonIcon());
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.leftColumnSteps().opensSpamFolder();
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Кнопка прочитано в «залипающем» тулбаре")
    @TestCaseId("1583")
    public void testReadButtonInStikyToolBar() {
        user.defaultSteps().clicksOn(onMessagePage().stickyToolBar().markAsReadButton());
        user.hotkeySteps().pressSimpleHotKey(KeysOwn.key(Keys.HOME));
        user.defaultSteps().clicksOn(onMessagePage().msgFiltersBlock().showUnread());
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject)
            .scrollDownPage();
        user.defaultSteps().clicksOn(onMessagePage().stickyToolBar().markAsUnreadButton());
        user.hotkeySteps().pressSimpleHotKey(KeysOwn.key(Keys.HOME));
        user.defaultSteps().clicksOn(onMessagePage().msgFiltersBlock().showUnread());
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }
}
