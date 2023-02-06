package ru.yandex.autotests.innerpochta.tests.autotests.grid;

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
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.cal.util.CalFragments;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.DAY;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.MONTH;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.WEEK;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Тесты на элементы в шапке")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
@UseCreds(HeaderTest.CREDS)
public class HeaderTest {

    public static final String CREDS = "HeaderTest";
    public static final String WS_CREDS = "WSCustomLogo";
    public static final String WS_FREE_CREDS = "WSFree";
    private static String SERVICE_PAID_NAME = "Админка";
    private static String SERVICE_PAID_URL = "https://connect.yandex.ru/portal/admin";
    private static String SERVICE_FREE_NAME = "Вики";
    private static String SERVICE_FREE_URL = "https://wiki.yandex.ru/";
    private static List<String> MAIN_SERVICES = Arrays.asList(
        "Главная",
        "Почта",
        "Мессенджер",
        "Диск",
        "Люди",
        "Календарь",
        "Формы"
    );

    private CalendarRulesManager rules = calendarRulesManager().withLock(AccLockRule.use().annotation());
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain();

    @DataProvider
    public static Object[][] services() {
        return new Object[][]{
            {YandexDomain.RU, "Почта", "mail.yandex.ru"},
            {YandexDomain.RU, "Диск", "disk.yandex.ru"},
            {YandexDomain.RU, "Телемост", "telemost.yandex.ru"},
            {YandexDomain.RU, "Календарь", "calendar.yandex.ru"},
            {YandexDomain.COM, "Почта", "mail.yandex.com"},
            {YandexDomain.COM, "Диск", "disk.yandex.com"},
            {YandexDomain.COM, "Телемост", "telemost.yandex.ru"},
            {YandexDomain.COM, "Календарь", "calendar.yandex.com"}
        };
    }

    @DataProvider
    public static Object[][] grids() {
        return new Object[][]{
            {DAY, 0},
            {WEEK, 1},
            {MONTH, 2}
        };
    }

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Разворачиваем левую колонку",
            new Params().withIsAsideExpanded(true)
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().clicksIfCanOn(steps.pages().cal().home().warningPopup().cancelBtn());
    }

    @Test
    @Title("Переходим в сервисы из шапки")
    @TestCaseId("2")
    @UseDataProvider("services")
    public void shouldOpenService(YandexDomain domain, String serviceName, String expUrl) {
        steps.user().loginSteps().forAcc(lock.firstAcc()).loginsToDomain(domain);
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().cal().home().calHeaderBlock().services(), serviceName)
            .shouldBeOnUrl(containsString(expUrl));
    }

    @Test
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("MAYA-2130")
    @Title("Проверяем сервисы в бургере для платного коннекта")
    @TestCaseId("668")
    @UseCreds(WS_CREDS)
    public void shouldSeeServersInBurgerForPaidWS() {
        ArrayList<String> paidServices = new ArrayList<>(MAIN_SERVICES);
        paidServices.add(SERVICE_PAID_NAME);

        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().calHeaderBlock().more())
            .shouldSeeAllElementsInList(
                steps.pages().cal().home().calHeaderBlock().moreItem(), paidServices.toArray(new String[0])
            );
    }

    @Test
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("MAYA-2130")
    @Title("Проверяем сервисы в бургере для бесплатного коннекта")
    @TestCaseId("625")
    @UseCreds(WS_FREE_CREDS)
    public void shouldSeeServersInBurgerForFreeWS() {
        ArrayList<String> freeServices = new ArrayList<>(MAIN_SERVICES);
        freeServices.add(SERVICE_FREE_NAME);

        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().calHeaderBlock().more())
            .shouldSeeAllElementsInList(
                steps.pages().cal().home().calHeaderBlock().moreItem(), freeServices.toArray(new String[0])
            );
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().cal().home().calHeaderBlock().moreItem(), SERVICE_FREE_NAME)
            .shouldBeOnUrl(containsString(SERVICE_FREE_URL));
    }

    @Test
    @Title("При переходе по кнопке «Календарь» открывается текущая дата")
    @TestCaseId("3")
    @UseDataProvider("grids")
    public void shouldSeeActualDate(CalFragments period, int selector) {
        String day = steps.pages().cal().home().leftPanel().miniCalendar().currentDay().getText();
        int outsideDaysCount = steps.pages().cal().home().leftPanel().miniCalendar().daysOutMonth().size();

        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().view())
            .clicksOn(steps.pages().cal().home().selectView().get(selector))
            .shouldBeOnUrl(containsString(period.makeUrlPart("")))
            .clicksOn(steps.pages().cal().home().leftPanel().miniCalendar().daysOutMonth().get(outsideDaysCount - 1))
            .clicksOn(steps.pages().cal().home().calHeaderBlock().calLink())
            .shouldBeOnUrl(containsString(period.makeUrlPart("")))
            .shouldSeeThatElementHasText(steps.pages().cal().home().leftPanel().miniCalendar().selectedDay(), day);
    }
}