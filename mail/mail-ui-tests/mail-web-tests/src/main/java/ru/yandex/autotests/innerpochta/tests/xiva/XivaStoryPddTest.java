package ru.yandex.autotests.innerpochta.tests.xiva;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.MultipleWindowsHandler;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.matchers.MessageExistsMatcher.msgNotExists;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDD_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.MailConst.XIVA_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;


@Aqua.Test
@Title("Тест на Xiva в  ПДД почте")
@Features(FeaturesConst.XIVA)
@Tag(FeaturesConst.XIVA)
@Stories(FeaturesConst.PDD_GENERAL)
public class XivaStoryPddTest extends BaseTest {

    private String expectedSubject;

    private AccLockRule lock = AccLockRule.use().useTusAccount(PDD_USER_TAG);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private MultipleWindowsHandler windowsHandler;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        expectedSubject = Utils.getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(
            lock.firstAcc(),
            expectedSubject,
            Utils.getRandomString()
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        windowsHandler = user.defaultSteps().opensNewWindowAndSwitchesOnIt();
    }

    @Test
    @Title("Загрузка нового сообщения без рефреша")
    @TestCaseId("1894")
    public void testLoadNewMessageWithoutRefreshPdd() {
        expectedSubject = Utils.getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(
            lock.firstAcc(),
            expectedSubject,
            Utils.getRandomString()
        );
        user.messagesSteps().shouldSeeMessageWithSubjectWithWaiting(expectedSubject);
        user.defaultSteps().switchesOnMainWindow(windowsHandler);
        user.messagesSteps().shouldSeeMessageWithSubjectWithWaiting(expectedSubject);
    }

    @Test
    @Title("Удаление сообщение при открытых двух табах")
    @TestCaseId("1895")
    public void testDeleteMessageTwoTabsSyncPdd() {
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(expectedSubject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().deleteButton());
        user.messagesSteps().shouldNotSeeMessageWithSubject(expectedSubject);
        user.defaultSteps().switchesOnMainWindow(windowsHandler);
        shouldNotSeeMessageWithSubjectWithWaiting(expectedSubject);
    }

    @Test
    @Title("Помечаем сообщение важным при открытых двух табах")
    @TestCaseId("1896")
    public void testLabelMessageImportantTwoTabsSyncPdd() {
        user.messagesSteps().labelsMessageImportant(expectedSubject);
        user.messagesSteps().shouldSeeThatMessageIsImportant(expectedSubject);
        user.defaultSteps().switchesOnMainWindow(windowsHandler);
        shouldSeeThatMessageIsImportantWithWaiting(expectedSubject);
    }

    @Step("Сообщения с темой «{0}» должны быть помечены меткой «Важные», ждём с задержкой")
    public void shouldSeeThatMessageIsImportantWithWaiting(String... expectedSubjects) {
        for (String expectedSubject : expectedSubjects) {
            assertThat(
                "Письмо не отметилось важным",
                user.messagesSteps().findMessageBySubject(expectedSubject).isImportance(),
                withWaitFor(isPresent(), XIVA_TIMEOUT)
            );
        }
    }

    @Step("Не должны видеть письмо с темой: «{0}», ждём с задержкой")
    public void shouldNotSeeMessageWithSubjectWithWaiting(String... expectedSubjects) {
        assertThat(
            "Письмо присутствует на странице",
            onMessagePage(),
            withWaitFor(msgNotExists(expectedSubjects), XIVA_TIMEOUT)
        );
    }
}
