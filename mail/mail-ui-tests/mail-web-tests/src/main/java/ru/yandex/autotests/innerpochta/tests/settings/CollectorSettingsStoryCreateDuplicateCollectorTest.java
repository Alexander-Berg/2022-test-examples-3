package ru.yandex.autotests.innerpochta.tests.settings;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

@Aqua.Test
@Title("Создание дубликатов сборщиков")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.COLLECTORS)
@RunWith(DataProviderRunner.class)
public class CollectorSettingsStoryCreateDuplicateCollectorTest extends BaseTest {

    private static final String PASSWORD = "testqa";

    @DataProvider
    public static Object[][] testData() {
        return new Object[][]{
            {"mailrucollector@mail.ru", PASSWORD, "imap.mail.ru"},
        };
    }

    private static final String[] collectors = {
        "collectorgmaizl2@gmail.com",
        "mailrucollector@mail.ru",
        "ramblercollector@rambler.ru",
        "testoviy-test109@yandex.ru"
    };

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().className();

    @Before
    public void setUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_COLLECTORS);
    }

    @Test
    @Title("Создание дубликатов сборщиков")
    @TestCaseId("1733")
    @UseDataProvider("testData")
    public void testAttemptToCreateDuplicateCollectorFromSettings(String login, String pass, String server) {
        user.settingsSteps().inputsTextInEmailInputBox(login)
            .inputsTextInPassInputBox(pass);
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockMain().turnOnCollector())
            .shouldSee(
                onCollectorSettingsPage().blockMain().blockNew().notifications().alreadyExistsNotification()
            );
        user.settingsSteps().shouldSeeCorrectTextOnNotificationAboutDuplicate(server, login);
    }

    @Test
    @Title("Загрузка списка сборщиков на странице настроек")
    @TestCaseId("2503")
    public void shouldSeeCollectorsList() {
        user.defaultSteps().shouldSeeAllElementsInList(
            user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors(),
            collectors
        );
    }
}
