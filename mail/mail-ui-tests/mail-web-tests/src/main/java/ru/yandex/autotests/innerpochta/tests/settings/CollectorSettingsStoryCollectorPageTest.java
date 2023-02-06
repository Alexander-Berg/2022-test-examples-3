package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.main.newcollector.BlockNotifications;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Создание сборщиков")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.COLLECTORS)
public class CollectorSettingsStoryCollectorPageTest extends BaseTest {

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
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_COLLECTORS);
    }

    @Test
    @Title("Уведомления о недобавлении сборщика")
    @TestCaseId("1727")
    public void testDoNotAddMailToCollectorFromSettings() {
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockMain().turnOnCollector())
            .shouldSee(onCollectorSettingsPage().blockMain().blockNew().notifications().emptyEmailNotification());
        user.settingsSteps().shouldSeeCorrectTextOnEmptyEmailNotification()
            .shouldSeeCorrectTextOnEmptyPasswordNotification()
            .inputsTextInEmailInputBox("123@yandex.ru");
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockMain().turnOnCollector())
            .shouldSee(onCollectorSettingsPage().blockMain().blockNew().notifications().emptyPasswordNotification());
        user.settingsSteps().shouldSeeCorrectTextOnEmptyPasswordNotification()
            .inputsTextInEmailInputBox("")
            .inputsTextInPassInputBox("123");
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockMain().turnOnCollector())
            .shouldSee(onCollectorSettingsPage().blockMain().blockNew().notifications().emptyEmailNotification());
        user.settingsSteps().shouldSeeCorrectTextOnEmptyEmailNotification();
    }

    @Test
    @Title("Создание сборщика на текущий адрес")
    @Issue("DARIA-68590")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @TestCaseId("1730")
    public void testCollectorForCurrentMailboxFromSettings() {
        createCollector(lock.firstAcc().getSelfEmail(), lock.firstAcc().getPassword());
        user.defaultSteps()
            .shouldSee(onCollectorSettingsPage().blockMain().blockNew().notifications().currentEmailNotification())
            .shouldContainText(
                onCollectorSettingsPage().blockMain().blockNew().notifications().currentEmailNotification(),
                BlockNotifications.CANT_CREATE_ON_CURRENT_TEXT
            )
            .clicksOn(onCollectorSettingsPage().blockMain().blockNew().notifications().filtersLink())
            .shouldBeOnUrl(containsString("#setup/filters"));
    }

    @Test
    @Title("Переход в Настройки сборщиков")
    @TestCaseId("3620")
    public void shouldOpenCollectorsSettings() {
        user.defaultSteps().clicksOn(onSettingsPage().blockSettingsNav().collectorsSetupLink())
            .shouldBeOnUrlWith(QuickFragments.SETTINGS_COLLECTORS);
    }

    private void createCollector(String login, String password) {
        user.settingsSteps().inputsTextInEmailInputBox(login)
            .inputsTextInPassInputBox(password);
        user.defaultSteps().clicksOn(onCollectorSettingsPage().blockMain().turnOnCollector());
    }
}
