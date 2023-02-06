package ru.yandex.autotests.innerpochta.tests.autotests;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.MORDA_URL;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASSPORT_AUTH_URL;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Разнообразные залогины")
@Features(FeaturesConst.AUTH)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class LoginTest {

    private static final String PASSPORT_URL_UPGRADE = "passport.yandex.ru/profile/upgrade?";
    private static final String EMAIL_FOR_FACEBOOK = "testovichok2017@gmail.com";
    private static final String PASS_FOR_FACEBOOK = "testoviy1";
    private static final String EMAIL_FOR_PDD = "testix001@админкапдд.рф";
    private static final String PASS_FOR_PDD = "Y0Usha11N0Tpass";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lockRule = AccLockRule.use().useTusAccount();

    private TouchRulesManager rules = touchRulesManager().withLock(null);
    private InitStepsRule steps = rules.getSteps();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Должно средиректить на дорегистрацию при соцавторизации юзера без почты")
    @TestCaseId("645")
    public void shouldRedirectOnPassport() {
        steps.user().defaultSteps().opensDefaultUrl()
            .shouldSee(steps.pages().homer().logoYandex())
            .clicksOn(steps.pages().homer().logInBtnHeadBanner())
            .shouldBeOnUrl(containsString(PASSPORT_AUTH_URL))
            .clicksOn(steps.pages().touch().passport().fbBtn())
            .switchOnJustOpenedWindow()
            .inputsTextInElement(steps.pages().touch().passport().inputLoginOnFb(), EMAIL_FOR_FACEBOOK)
            .inputsTextInElement(steps.pages().touch().passport().inputPassOnFb(), PASS_FOR_FACEBOOK)
            .clicksOn(steps.pages().touch().passport().logInOnFb())
            .switchOnWindow(0)
            .waitInSeconds(1)
            .shouldBeOnUrl(containsString(PASSPORT_URL_UPGRADE));
    }

    @Test
    @Title("Логинимся кириллическим доменом")
    @TestCaseId("243")
    public void shouldLoginWithPddRf() {
        steps.user().defaultSteps().opensDefaultUrl()
            .shouldSee(steps.pages().homer().logoYandex())
            .clicksOn(steps.pages().homer().logInBtnHeadBanner())
            .inputsTextInElement(steps.pages().touch().passport().inputLogin(), EMAIL_FOR_PDD)
            .clicksOn(steps.pages().touch().passport().logInBtn())
            .inputsTextInElement(steps.pages().touch().passport().inputPass(), PASS_FOR_PDD)
            .clicksOn(steps.pages().touch().passport().logInBtn())
            .shouldBeOnUrl(containsString(INBOX_FOLDER.makeTouchUrlPart()));
    }

    @Test
    @Title("Логинимся с морды")
    @TestCaseId("658")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldLoginFromMorda() {
        steps.user().defaultSteps().opensUrl(MORDA_URL)
            .clicksOn(steps.pages().touch().mordaPage().mailBlockPhone())
            .clicksOn(steps.pages().homer().logInBtnHeadBanner());
        logInFromPassportFromMorda();
    }

    @Test
    @Title("Логинимся с морды")
    @TestCaseId("658")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldLoginFromMordaTablet() {
        steps.user().defaultSteps().opensUrl(MORDA_URL)
            .clicksOn(steps.pages().touch().mordaPage().mailBlockTablet())
            .switchOnJustOpenedWindow()
            .inputsTextInElement(steps.pages().touch().mordaPage().inputLogin(), lockRule.firstAcc().getLogin())
            .clicksOn(steps.pages().touch().mordaPage().logInBtn())
            .inputsTextInElement(steps.pages().touch().mordaPage().inputPass(), lockRule.firstAcc().getPassword())
            .clicksOn(steps.pages().touch().mordaPage().logInBtn())
            .shouldBeOnUrl(containsString(INBOX_FOLDER.makeTouchUrlPart()));
    }

    @Step("Авторизуемся через интерфейс паспорта с морды")
    private void logInFromPassportFromMorda() {
        steps.user().defaultSteps()
            .inputsTextInElement(steps.pages().touch().passport().inputLogin(), lockRule.firstAcc().getLogin())
            .clicksOn(steps.pages().touch().passport().logInBtn())
            .inputsTextInElement(steps.pages().touch().passport().inputPass(), lockRule.firstAcc().getPassword())
            .clicksOn(steps.pages().touch().passport().logInBtn())
            .clicksIfCanOn(steps.pages().passport().notNowBtn())
            .clicksIfCanOn(steps.pages().passport().notNowEmailBtn())
            .clicksIfCanOn(steps.pages().passport().skipAvatarBtn())
            .shouldBeOnUrl(containsString(INBOX_FOLDER.makeTouchUrlPart()));
    }
}
