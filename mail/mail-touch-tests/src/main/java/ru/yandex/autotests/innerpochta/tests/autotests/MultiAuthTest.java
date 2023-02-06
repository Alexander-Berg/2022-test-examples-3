package ru.yandex.autotests.innerpochta.tests.autotests;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.PASSPORT;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_DEFAULT_EMAIL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TOUCH_ONBOARDING;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.MAX_NUMBER_OF_USERS_IN_SESSION_ID;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Мультиавторизация")
@Features(FeaturesConst.MULTI_AUTH)
@Stories(FeaturesConst.GENERAL)
public class MultiAuthTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private AccLockRule lock2 = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock2);
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(lock2)
        .around(auth2);

    @Before
    public void login() {
        steps.user().apiSettingsSteps().withAuth(auth2).callWithListAndParams(
            "Сбрасываем показ онбординга табов",
            of(TOUCH_ONBOARDING, STATUS_ON)
        );
    }

    @Test
    @Title("По клику в боковом меню переключаемся на другого пользователя")
    @TestCaseId("598")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSwitchMultiAuthUsers() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins()
            .multiLoginWith(lock2.firstAcc());
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldContainText(
                steps.pages().touch().sidebar().userEmail(),
                steps.user().apiSettingsSteps().withAuth(auth2).getUserSettings(SETTINGS_PARAM_DEFAULT_EMAIL)
            )
            .clicksOn(steps.pages().touch().sidebar().inactiveMAAccount().get(0))
            .shouldContainText(steps.pages().touch().sidebar().userEmail(), accLock.firstAcc().getSelfEmail());
    }

    @Test
    @Title("По клику в боковом меню переключаемся на другого пользователя")
    @TestCaseId("598")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSwitchMultiAuthUsersTablet() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins()
            .multiLoginWith(lock2.firstAcc());
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldContainText(
                steps.pages().touch().sidebar().userEmail(),
                steps.user().apiSettingsSteps().withAuth(auth2).getUserSettings(SETTINGS_PARAM_DEFAULT_EMAIL)
            )
            .clicksOn(steps.pages().touch().sidebar().inactiveMAAccount().get(0))
            .shouldSee(steps.pages().touch().messageList().headerBlock())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldHasText(steps.pages().touch().sidebar().userEmail(), accLock.firstAcc().getSelfEmail());
    }

    @Test
    @Title("Из бокового меню по клику по «+» попадаем в паспорт")
    @TestCaseId("6")
    public void shouldGoToPassportWithPlus() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins()
            .multiLoginWith(lock2.firstAcc());
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().plusButton())
            .shouldBeOnUrl(containsString(PASSPORT.fragment()));
    }

    @Test
    @Title("Залогиниваем 15 юзеров и проверяем, что в карусели нет плюса для добавления ещё юзеров")
    @TestCaseId("244")
    public void shouldLogin15Users() {
        steps.user().loginSteps().multiLoginWithRandomAccountsNumber(
            lock2.firstAcc(),
            MAX_NUMBER_OF_USERS_IN_SESSION_ID - 1
        );
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldNotSee(steps.pages().touch().sidebar().plusButton());
    }
}
