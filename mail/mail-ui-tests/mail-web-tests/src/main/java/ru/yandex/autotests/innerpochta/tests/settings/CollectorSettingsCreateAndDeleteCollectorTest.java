package ru.yandex.autotests.innerpochta.tests.settings;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.ProxyServerRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetUrlForDomainRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.proxy.LittleHostFilter.hostFilter;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_URL_WITHOUT_DOMAIN;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

@Aqua.Test
@Title("Создание сборщиков")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.COLLECTORS)
@RunWith(DataProviderRunner.class)
public class CollectorSettingsCreateAndDeleteCollectorTest extends BaseTest {

    private static final String PASSWORD = "testqA12";
    private static final String LOGIN = "yacollector@rambler.ru";
    private static final String SERVER_POP = "pop.rambler.ru";
    private static final String SERVER_IMAP = "imap.rambler.ru";
    private static final String MAILRU_LOGIN = "karteric5@mail.ru";
    private static final String MAILRU_PASSWORD = "testoviy12";

    public AccLockRule lock = AccLockRule.use().useTusAccount();
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
        user.apiCollectorSteps().removeAllUserCollectors();
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_COLLECTORS);
    }

    @DataProvider
    public static Object[][] testData() {
        return new Object[][]{
            {0, SERVER_POP},
            {1, SERVER_IMAP},
        };
    }

    @DataProvider
    public static Object[][] services() {
        return new Object[][]{
            {MAILRU_LOGIN, MAILRU_PASSWORD}
        };
    }

    @Test
    @Title("Создаём сборщик со страницы настроек c расширенной формой")
    @TestCaseId("3934")
    @UseDataProvider("testData")
    public void createNewCollectorFromSettings(int listNumber, String server) {
        user.settingsSteps().inputsTextInEmailInputBox(LOGIN)
            .inputsTextInPassInputBox(PASSWORD);
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockMain().serverSetup().selectProtocol())
            .clicksOn(onSettingsPage().selectConditionDropdown().conditionsList().get(listNumber));
        user.settingsSteps().inputsTextInServerInputBox(server);
        user.defaultSteps().inputsTextInElement(onCollectorSettingsPage().blockMain().serverSetup().login(), LOGIN)
            .clicksOn(onCollectorSettingsPage().blockMain().turnOnCollector())
            .shouldSee(onCollectorSettingsPage().blockSetup().selectFolderDropDown())
            .inputsTextInElement(onCollectorSettingsPage().blockSetup().password(), PASSWORD)
            .clicksOn(onCollectorSettingsPage().blockSetup().save());
        user.settingsSteps().shouldSeeNewCollector(LOGIN);
    }

    @Test
    @Title("Удаляем сборщик со страницы настроек")
    @TestCaseId("2504")
    public void deleteCollectorFromSettings() {
        user.apiCollectorSteps().createNewCollector(LOGIN, PASSWORD, SERVER_POP);
        user.defaultSteps().refreshPage();
        user.settingsSteps().shouldSeeNewCollector(LOGIN)
            .shouldSeeCollectorCount(1);
        user.defaultSteps().clicksOn(
            onCollectorSettingsPage().blockMain().blockConnected().collectors().get(0).collectorLink(),
            onCollectorSettingsPage().blockMain().blockConnected().collectors().get(0).deleteMailboxBtn(),
            onCollectorSettingsPage().deleteCollectorPopUp().deleteBtn()
        )
            .shouldNotSee(onCollectorSettingsPage().blockMain().blockConnected().collectors());
    }

    @Test
    @Title("Создаём сборщик с @yandex.ru со страницы настроек")
    @TestCaseId("2879")
    @UseDataProvider("services")
    public void createNewYandexCollectorFromSettings(String login, String pass) {
        user.settingsSteps().inputsTextInEmailInputBox(login)
            .inputsTextInPassInputBox(pass);
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockSetup().save())
            .shouldSee(onCollectorSettingsPage().blockSetup())
            .clicksOn(onSettingsPage().blockSettingsNav().collectorsSetupLink());
        user.settingsSteps().shouldSeeNewCollector(login);
    }

    @Test
    @Title("Меняем настройки у сборщика")
    @TestCaseId("2505")
    public void changeCollectorSettings() {
        user.apiCollectorSteps().createNewCollector(LOGIN, PASSWORD, SERVER_POP);
        user.defaultSteps().refreshPage()
            .clicksOn(
                onCollectorSettingsPage().blockMain().blockConnected().collectors().get(0).collectorLink(),
                onCollectorSettingsPage().blockMain().blockConnected().collectors().get(0).configureMailboxBtn()
            )
            .inputsTextInElement(onCollectorSettingsPage().blockSetup().password(), getRandomString());
        user.settingsSteps().clicksOnSaveChangesButton();
        user.defaultSteps().shouldSee(user.pages().CollectorSettingsPage().noResponseNotification())
            .inputsTextInElement(onCollectorSettingsPage().blockSetup().password(), PASSWORD);
        user.settingsSteps().clicksOnSaveChangesButton();
        user.defaultSteps().shouldNotSee(onCollectorSettingsPage().blockSetup());
    }

    @Test
    @Title("Подключаем oAuth сборщик")
    @TestCaseId("2887")
    public void addOauthCollectorFromSettings() {
        addOauthCollector();
    }

    @Test
    @Title("Удаляем oAuth сборщик со страницы настроек")
    @TestCaseId("2878")
    public void deleteOauthCollectorFromSettings() {
        addOauthCollector();
        user.defaultSteps().clicksOn(
            onCollectorSettingsPage().blockMain().blockConnected().collectors().get(0).collectorLink(),
            onCollectorSettingsPage().blockMain().blockConnected().collectors().get(0).deleteMailboxBtn(),
            onCollectorSettingsPage().deleteCollectorPopUp().deleteBtn()
        )
            .shouldNotSee(onCollectorSettingsPage().blockMain().blockConnected().collectors());
    }

    @Step("Подключаем oAuth сборщик")
    private void addOauthCollector() {
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockMain().mailruOauthCollectorBtn())
            .switchOnJustOpenedWindow()
            .inputsTextInElement(onHomePage().mailRuOauthPopup().username(), MAILRU_LOGIN)
            .clicksOn(onHomePage().mailRuOauthPopup().nextBtn())
            .inputsTextInElement(onHomePage().mailRuOauthPopup().password(), MAILRU_PASSWORD)
            .clicksOn(onHomePage().mailRuOauthPopup().submitBtn())
            .switchOnWindow(0)
            .shouldContainTextInUrl(MAILRU_LOGIN.replace("@", "%40"))
            .clicksOn(onSettingsPage().blockSettingsNav().collectorsSetupLink());
        user.settingsSteps().shouldSeeNewCollector(MAILRU_LOGIN);
    }
}
