package ru.yandex.autotests.innerpochta.tests.autotests.multiauth;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.hamcrest.CoreMatchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.cal.util.CalFragments;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.annotations.Issue;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.EVENT;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.MONTH;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.SETTINGS_SIDEBAR;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на мультиавторизацию")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.MULTI_AUTH)
@RunWith(DataProviderRunner.class)
public class MultiAuthTest {

    public static final String CREDS = "MultiAuthLoginSeveralUsers";
    private static final String CREDS_PDD_RF = "AuthorizationTestPddRFAdminkapddrf";
    private static final String CREDS_PDD = "MultiAuthLoginSeveralUsersWithPddTest";
    private static final String CREDS_WS = "WSCustomLogo";


    private CalendarRulesManager rules = calendarRulesManager()
        .withLock(AccLockRule.use().names(CREDS, CREDS_PDD_RF, CREDS_PDD, CREDS_WS));
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain();

    @DataProvider
    public static Object[][] grids() {
        return new Object[][]{
            {EVENT, CREDS_PDD_RF},
            {SETTINGS_SIDEBAR, CREDS_PDD}
        };
    }

    @DataProvider
    public static Object[][] testDomains() {
        return new Object[][]{
            {YandexDomain.COM},
            {YandexDomain.RU},
            {YandexDomain.UZ},
            {YandexDomain.KZ},
            {YandexDomain.BY}
        };
    }

    @Test
    @Title("Логин несколькими юзерами на различных доменах")
    @TestCaseId("1")
    @UseDataProvider("testDomains")
    public void shouldMultiLogin(YandexDomain domain) {
        steps.user().loginSteps().forAcc(lock.acc(CREDS)).loginsToDomain(domain);
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().calHeaderBlock().userAvatar(),
            steps.pages().cal().home().adduser()
        );
        steps.user().loginSteps().multiLoginWith(
            domain,
            lock.acc(CREDS_PDD), lock.acc(CREDS_PDD_RF), lock.acc(CREDS_WS)
        );
        steps.user().defaultSteps().shouldBeOnUrl(containsString(domain.getDomain()))
            .clicksOn(steps.pages().cal().home().calHeaderBlock().userAvatar())
            .shouldSeeAllElementsInList(
                steps.pages().cal().home().userMenuDropdown().userList(),
                lock.acc(CREDS).getLogin(),
                lock.acc(CREDS_PDD_RF).getLogin(),
                lock.acc(CREDS_PDD).getLogin()
            );
        steps.user().defaultSteps().clicksOnElementWithText(
            steps.pages().cal().home().userMenuDropdown().userList(),
            lock.acc(CREDS).getLogin()
        )
            .shouldContainText(
                steps.pages().cal().home().calHeaderBlock().userName(),
                lock.acc(CREDS).getLogin()
            );
    }

    @Test
    @Title("Переключение между пользователями")
    @TestCaseId("130")
    @UseDataProvider("grids")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("MAYA-2334")
    public void multiLoginShouldSwitchUsers(CalFragments grid, String account) {
        steps.user().loginSteps().forAcc(lock.acc(account)).logins();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().calHeaderBlock().userAvatar(),
            steps.pages().cal().home().adduser()
        );
        steps.user().loginSteps().multiLoginWith(YandexDomain.RU, lock.acc(CREDS));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid.makeUrlPart(""))
            .clicksOn(steps.pages().cal().home().calHeaderBlock().userAvatar())
            .clicksOnElementWithText(
                steps.pages().cal().home().userList(),
                lock.acc(account).getLogin()
            )
            .shouldContainText(
                steps.pages().cal().home().calHeaderBlock().userName(),
                lock.acc(account).getLogin()
            )
            .shouldBeOnUrl(CoreMatchers.containsString(WEEK_GRID));
    }

    @Test
    @Title("Переключение между пользователями один из которых WS")
    @TestCaseId("130")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("MAYA-2334")
    public void multiLoginShouldSwitchWSUsers() {
        steps.user().loginSteps().forAcc(lock.acc(CREDS_WS)).logins();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().oldCalHeaderBlock().oldUserAvatar(),
            steps.pages().cal().home().oldAddUser()
        );
        steps.user().loginSteps().multiLoginWith(YandexDomain.RU, lock.acc(CREDS));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MONTH.makeUrlPart(""))
            .clicksOn(steps.pages().cal().home().calHeaderBlock().userAvatar())
            .clicksOnElementWithText(
                steps.pages().cal().home().userList(),
                lock.acc(CREDS_WS).getLogin()
            )
            .shouldContainText(
                steps.pages().cal().home().oldCalHeaderBlock().oldUserName(),
                lock.acc(CREDS_WS).getLogin()
            )
            .shouldBeOnUrl(CoreMatchers.containsString(WEEK_GRID));
    }
}
