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

import static ru.yandex.qatools.htmlelements.matchers.common.HasTextMatcher.hasText;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.util.MailConst.XIVA_TIMEOUT;
import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;


@Aqua.Test
@Title("Тест на изменение счетчика новых писем")
@Features(FeaturesConst.XIVA)
@Tag(FeaturesConst.XIVA)
@Stories(FeaturesConst.COUNTERS)
public class XivaStoryCountersUpdateTest extends BaseTest {

    private static final String CUSTOM_FOLDER = "test";

    private int oldCounter;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
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
        sendYourselfAnEmail();
        user.apiFoldersSteps().createNewFolder(CUSTOM_FOLDER);
        user.apiSettingsSteps().callWithListAndParams(
                "Раскрываем все папки",
                of(FOLDERS_OPEN, user.apiFoldersSteps().getAllFids())
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER);
        windowsHandler = user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        oldCounter = user.leftColumnSteps().inboxTotalCounter();
    }

    @Test
    @Title("Проверка обновления счетчика новых писем для двух окон находясь в одной папке")
    @TestCaseId("1890")
    public void shouldUpdateForTwoWindowsWhenUserInSameFolder() {
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER);
        sendYourselfAnEmail();
        shouldSeeTotalInboxUnreadCounterWithWaiting(oldCounter + 1);
        user.defaultSteps().switchesOnMainWindow(windowsHandler);
        shouldSeeTotalInboxUnreadCounterWithWaiting(oldCounter + 1);
    }

    @Test
    @Title("Проверка обновления счетчика новых писем для двух окон находясь в разных папках")
    @TestCaseId("1891")
    public void shouldUpdateForTwoWindowsWhenUserInDifFolder() {
        user.leftColumnSteps().opensSpamFolder();
        sendYourselfAnEmail();
        shouldSeeTotalInboxUnreadCounterWithWaiting(oldCounter + 1);
        user.defaultSteps().switchesOnMainWindow(windowsHandler);
        shouldSeeTotalInboxUnreadCounterWithWaiting(oldCounter + 1);
    }

    @Step("Отправляем себе новое сообщение")
    private void sendYourselfAnEmail() {
        user.apiMessagesSteps().sendMailWithNoSave(
            lock.firstAcc(),
            Utils.getRandomString(),
            Utils.getRandomString()
        );
    }

    @Step("Счётчик непрочитанных папки «Входящие» должен быть равен «{0}», ждём с задержкой")
    public void shouldSeeTotalInboxUnreadCounterWithWaiting(int expectedCounter) {
        user.defaultSteps().shouldSee(onMessagePage().foldersNavigation().inboxUnreadCounter());
        onMessagePage().foldersNavigation().inboxUnreadCounter().waitUntil(not(empty()))
            .waitUntil(
                "Счетчик непрочитанных писем отличен от ожидаемого",
                hasText(Integer.toString(expectedCounter)),
                XIVA_TIMEOUT
            );
    }
}
