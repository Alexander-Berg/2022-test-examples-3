package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RetryRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.concurrent.TimeUnit;

;

@Aqua.Test
@Title("Тест на добавление дубликата сборщика mail.ru по imap")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.COLLECTORS)
public class CollectorSettingsPopMailRuDuplicateTest extends BaseTest {

    private static final String SERVER = "imap.mail.ru";
    private static final String LOGIN_FOR_COLLECTOR = "mailrucollector@mail.ru";
    private static final String PASSWORD = "testqa";

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().className();

    @Rule
    public RetryRule retry = RetryRule.retry().ifException(Exception.class).every(3, TimeUnit.SECONDS).times(3);

    @Before
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_COLLECTORS);
    }

    @Test
    @Title("Добавление дубликата сборщика mail.ru")
    @TestCaseId("1733")
    public void testCreateDuplicateForPopMailRuCollector() {
        user.settingsSteps().inputsTextInPassInputBox(PASSWORD)
            .inputsTextInEmailInputBox(LOGIN_FOR_COLLECTOR);
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockMain().turnOnCollector())
        .shouldSee(onCollectorSettingsPage().blockMain().blockNew().notifications().alreadyExistsNotification());
        user.settingsSteps().shouldSeeCorrectTextOnNotificationAboutDuplicate(SERVER, LOGIN_FOR_COLLECTOR);
    }
}
