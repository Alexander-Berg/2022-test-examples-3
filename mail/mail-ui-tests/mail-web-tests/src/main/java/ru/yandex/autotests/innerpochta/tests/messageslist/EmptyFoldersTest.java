package ru.yandex.autotests.innerpochta.tests.messageslist;

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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.MailConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тесты на навигацию по пустым папкам")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class EmptyFoldersTest extends BaseTest {

    private static final String EMPTY_INBOX = "В папке «Входящие» нет писем";
    private static final String EMPTY_FOLDER = "В папке «folder» нет писем\n" + "Перейти во Входящие";
    private static final String USER_FOLDER = "folder";
    private static final String THREAD_SUBJ = "thread";
    private static final int THREAD_COUNT = 3;

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
    public void logIn() {
        user.apiFoldersSteps().createNewFolder(USER_FOLDER);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Пустая папка инбокс")
    @TestCaseId("1500")
    public void testEmptyInboxFolder() {
        user.defaultSteps().shouldSeeThatElementTextEquals(onMessagePage().emptyFolder(), EMPTY_INBOX);
    }

    @Test
    @Title("Проверяем отсутствие группировки писем в папках Удаленные и Спам")
    @TestCaseId("1501")
    public void testNoThreadModeInTrashSpamAndDraft() {
        user.apiMessagesSteps().sendThread(lock.firstAcc(), THREAD_SUBJ, THREAD_COUNT);
        user.defaultSteps().refreshPage();
        user.messagesSteps().deleteAllMessage();
        user.defaultSteps().opensFragment(QuickFragments.TRASH);
        user.messagesSteps().shouldSeeMessageWithSubject(THREAD_SUBJ)
            .shouldSeeCorrectNumberOfMessages(THREAD_COUNT)
            .labelsAllMessagesAsSpam();
        user.defaultSteps().opensFragment(QuickFragments.SPAM);
        user.messagesSteps().shouldSeeMessageWithSubject(THREAD_SUBJ)
            .shouldSeeCorrectNumberOfMessages(THREAD_COUNT);
    }

    @Test
    @Title("Пустая пользовательской папка")
    @TestCaseId("1502")
    public void testEmptyCustomFolder() {
        user.leftColumnSteps().openFolders()
            .opensCustomFolder(0)
            .shouldBeInFolder(USER_FOLDER);
        user.defaultSteps().shouldSeeThatElementTextEquals(onMessagePage().emptyFolder(), EMPTY_FOLDER)
            .clicksOn(onMessagePage().emptyFolder().inboxLink());
        user.leftColumnSteps().shouldBeInFolder(MailConst.INBOX_RU);
    }
}
