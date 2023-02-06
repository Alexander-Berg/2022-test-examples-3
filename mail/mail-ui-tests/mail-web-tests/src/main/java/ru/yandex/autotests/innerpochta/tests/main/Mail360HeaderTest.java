package ru.yandex.autotests.innerpochta.tests.main;

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
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author eremin-ns
 */
@Aqua.Test
@Title("Почта 360 - Шапка")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class Mail360HeaderTest extends BaseTest {

    private static final String YANDEX_MESSENGER_PAGE = "https://yandex.ru/chat#/";
    private static final String YANDEX_ALL_SERVICES_PAGE = "https://yandex.ru/all";
    private static final String LANDING_360_URL = "https://360.yandex.ru/?from=mail-header-360";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @DataProvider
    public static Object[][] services() {
        return new Object[][]{
            {"https://disk.yandex.ru/", 1},
            {"https://telemost.yandex.ru/", 2},
            {"https://docs.yandex.ru/", 3}
        };
    }

    @DataProvider
    public static Object[][] servicesMore() {
        return new Object[][]{
            {"https://calendar.yandex.ru/", 2},
            {"https://mail360.yandex.ru/", 3},
            {"https://disk.yandex.ru/notes/", 4},
            {"#contacts", 5}
        };
    }

    @Before
    public void setUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Нажимаем на иконку «Я»")
    @TestCaseId("5968")
    public void shouldClickOnYandexLogo() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().yandexLogoMainPage())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(LANDING_360_URL);
    }

    @Test
    @Title("Переходим в сервисы в выпадушке «Еще»")
    @TestCaseId("5964")
    @UseDataProvider("servicesMore")
    public void shouldMoveToServicesInMore(String url, int number) {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .clicksOn(onMessagePage().allServices360Popup().serviceIcons().get(number))
            .shouldContainTextInUrl(url);
    }

    @Test
    @Title("Переходим в сервисы")
    @TestCaseId("5960")
    @UseDataProvider("services")
    public void shouldMoveToService(String url, int number) {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(number))
            .shouldContainTextInUrl(url);
    }

    @Test
    @Title("Нажимаем на иконку «Почта»")
    @TestCaseId("5961")
    public void shouldClickOnMailIcon() {
        user.defaultSteps().opensFragment(QuickFragments.DRAFT)
            .clicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(0))
            .shouldContainTextInUrl(UrlProps.urlProps().getBaseUri())
            .shouldBeOnUrl(lock.firstAcc(), QuickFragments.DRAFT);
    }

    @Test
    @Title("Переходим в мессенджер в выпадушке «Еще»")
    @TestCaseId("5964")
    public void shouldMoveToMessenger() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .clicksOn(onMessagePage().allServices360Popup().serviceIcons().get(6))
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(YANDEX_MESSENGER_PAGE);
    }

    @Test
    @Title("Переходим на «Все сервисы» в выпадушке «Еще»")
    @TestCaseId("5965")
    public void shouldMoveToAllServices() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .clicksOn(onMessagePage().allServices360Popup().allServices360())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(YANDEX_ALL_SERVICES_PAGE);
    }

    @Test
    @Title("Календарь с текущей датой")
    @TestCaseId("5971")
    public void shouldSeeRealDate() {
        LocalDate date = LocalDate.now();
        String parsedDate = date.format(DateTimeFormatter.ofPattern("d"));
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .shouldContainText(onMessagePage().allServices360Popup().serviceIcons().get(2), parsedDate);
    }

    @Test
    @Title("Должны видеть кнопку «Улучшить Почту 360»")
    @TestCaseId("5967")
    public void shouldSeeUpgradeMail360Btn() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().upgradeMail360())
            .shouldContainTextInUrl(servicesMore()[1][1].toString());
    }

}
