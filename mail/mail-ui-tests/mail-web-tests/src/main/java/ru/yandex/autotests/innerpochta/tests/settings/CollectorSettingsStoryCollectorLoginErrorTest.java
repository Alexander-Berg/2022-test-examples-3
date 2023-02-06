package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.ProxyServerRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.RetryRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.concurrent.TimeUnit;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.util.ProxyCollectorsCheckFilter.proxyCollectorsCheckFilter;

/**
 * Created by mabelpines on 19.05.15.
 */
@Aqua.Test
@Title("Попытка создать сборщик с неверным логином/паролем")
@Description("Тесты на страницу сборщиков")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.GENERAL)
public class CollectorSettingsStoryCollectorLoginErrorTest extends BaseTest {

    private static final String LOGIN_FOR_COLLECTOR = "mailforcollector";
    private static final String JSON_RESOURCE_PATH = "collectors/collectors-login-error.json";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @Override
    public DesiredCapabilities setCapabilities() {
        return serverProxyRule.getCapabilities();
    }

    @ClassRule
    public static ProxyServerRule serverProxyRule = proxyServerRule(proxyCollectorsCheckFilter(JSON_RESOURCE_PATH));

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Rule
    public RetryRule retry = RetryRule.retry().ifException(Exception.class).every(3, TimeUnit.SECONDS).times(3);

    @Before
    public void logIn() throws InterruptedException {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_COLLECTORS);
    }

    @Test
    @Title("Уведомление о неверном логине/пароле при создании")
    @TestCaseId("1725")
    public void testWrongPasswordForCollectorFromSettings() throws Exception {
        createCollector(LOGIN_FOR_COLLECTOR + "@yandex.ru", "12345");
        restartTestIfConditionFailed(
            user.settingsSteps().shouldSeeNoResponseFromServerNotification(),
            "Уведомление о неверном логине/пароле не появилось"
        );
        user.settingsSteps().shouldSeeCorrectTextOnNoResponseFromServerNotification();
    }

    @Test
    @Title("Уведомление «Сервер не отвечает, либо введен неверный логин или пароль»")
    @TestCaseId("1726")
    public void testUnknownLoginForCollectorFromSettings() throws Exception {
        createCollector(LOGIN_FOR_COLLECTOR + "@abcd.ru", "12345");
        restartTestIfConditionFailed(
            user.settingsSteps().shouldSeeServerSettingsConfiguration(),
            "Настройки сервера не появились"
        );
        user.settingsSteps().inputsTextInLoginInputBox(LOGIN_FOR_COLLECTOR + "@abcd.ru")
            .inputsTextInServerInputBox("pop.gmail.com");
        user.settingsSteps().selectsSslCheckBox();
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockMain().turnOnCollector());
        restartTestIfConditionFailed(
            user.settingsSteps().shouldSeeNoResponseFromServerNotification(),
            "Уведомление о неверном логине/пароле не появилось"
        );
    }

    private void createCollector(String login, String password) {
        user.settingsSteps().inputsTextInPassInputBox(password)
            .inputsTextInEmailInputBox(login);
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockMain().turnOnCollector());
    }

    private void restartTestIfConditionFailed(boolean condition, String reason) throws Exception {
        if (!condition) {
            throw new Exception(reason);
        }
    }
}
