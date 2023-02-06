package ru.yandex.autotests.innerpochta.tests.main;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DISABLE_PROMO;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на новую шапку")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class MailNewHeaderTest extends BaseTest {

    private static final String PASSPORT_URL = "https://passport.yandex.ru/profile";
    private static final String YANDEX_SERVICES = "https://yandex.ru/all";
    private static final String TELEMOST = "https://telemost.yandex.ru/?source=tab-mail";
    private static final String PLUS = "https://plus.yandex.ru/";
    private static final String HELP = "https://yandex.ru/support/mail/";
    private static final String PASSPORT_AUTH = "https://passport.yandex.ru/auth?from=mail";
    private static final String SEARCH_RESULT_URL = "#search?request=";

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
    public static Object[][] langs() {
        return new Object[][]{
            {"English", "Mail", "Disk", "Telemost", "Documents", "More"},
            {"Türkçe", "Mail", "Disk", "Telemost", "Belgeler", "Daha fazlası"},
            {"Беларуская", "Пошта", "Дыск", "Тэлемост", "Дакументы", "Яшчэ"}
        };
    }

    @Before
    public void login() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем компактную шапку и включаем показ промо",
            of(
                LIZA_MINIFIED_HEADER, EMPTY_STR,
                DISABLE_PROMO, FALSE
            )
        );
        user.defaultSteps().refreshPage();
    }

    @Test
    @Title("Переходим во все сервисы через кнопку «Еще»")
    @TestCaseId("5759")
    public void shouldRedirectToAllServices() {
        user.defaultSteps().shouldSee(onMessagePage().mail360HeaderBlock())
            .clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .clicksOn(onMessagePage().allServices360Popup().allServices360())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(YANDEX_SERVICES));
    }

    @Test
    @Title("Переходим в «Телемост»")
    @TestCaseId("5764")
    public void shouldRedirectToTelemost() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(2))
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(TELEMOST));
    }

    @Test
    @Title("Переходим на «Другие сервисы»")
    @TestCaseId("5782")
    public void shouldSeeOtherServices() {
        user.defaultSteps().setsWindowSize(1100, 1000)
            .clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .scrollAndClicksOn(onMessagePage().allServices360Popup().allServices360())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(YANDEX_SERVICES));
    }

    @Test
    @Title("Переход на страницу управления аккаунтом из выпадушки юзера")
    @TestCaseId("5775")
    public void shouldSeeProfilePage() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .clicksOn(onMessagePage().mail360HeaderBlock().userMenuDropdown().userProfileLink())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(PASSPORT_URL));
    }

    @Test
    @Title("Переход на страницу помощи из выпадушки юзера")
    @TestCaseId("5777")
    public void shouldSeeHelpPage() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .clicksOn(onMessagePage().mail360HeaderBlock().userMenuDropdown().userHelpLink())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(HELP));
    }

    @Test
    @Title("Логаут при нажатии на «Выход» в выпадушке юзера")
    @TestCaseId("5779")
    public void shouldLogOut() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .clicksOn(onMessagePage().mail360HeaderBlock().userMenuDropdown().userLogOutLink())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(PASSPORT_AUTH));
    }

    @Test
    @Title("Перевод шапки для разных языков")
    @TestCaseId("5784")
    @UseDataProvider("langs")
    public void shouldSeeCorrectsLang(String lang, String mail, String disk, String telemost, String documents,
                                      String more) {
        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().languageSwitch())
            .clicksOnElementWithText(onSettingsPage().languageSelect().languagesList(), lang)
            .shouldSeeThatElementHasText(onMessagePage().mail360HeaderBlock().servicesHeader().get(0), mail)
            .shouldSeeThatElementHasText(onMessagePage().mail360HeaderBlock().servicesHeader().get(1), disk)
            .shouldSeeThatElementHasText(onMessagePage().mail360HeaderBlock().servicesHeader().get(2), telemost)
            .shouldSeeThatElementHasText(onMessagePage().mail360HeaderBlock().servicesHeader().get(3), documents)
            .shouldSeeThatElementHasText(onMessagePage().mail360HeaderBlock().servicesHeader().get(4), more);
    }

    @Test
    @Title("Поиск в компактном меню")
    @TestCaseId("5780")
    public void shouldSeeCompactSearch() {
        String subject = getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, "");
        openSearch();
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), subject)
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtn())
            .shouldBeOnUrl(StringContains.containsString(SEARCH_RESULT_URL + subject));
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Поисковой запрос очищается по крестику")
    @TestCaseId("5780")
    public void shouldClearSearchInput() {
        openSearch();
        user.defaultSteps().inputsTextInElement(onMessagePage().mail360HeaderBlock().searchInput(), getRandomString())
            .clicksOn(onMessagePage().mail360HeaderBlock().closeSearch())
            .shouldHasValue(onMessagePage().mail360HeaderBlock().searchInput(), "");
    }

    @Test
    @Title("Поиск закрывается по клику на крестик")
    @TestCaseId("5780")
    public void shouldCloseSearch() {
        openSearch();
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().closeSearch())
            .shouldNotSee(onMessagePage().mail360HeaderBlock().searchInput());
    }

    @Test
    @Title("В выпадушке залогина отображается логин юзера")
    @TestCaseId("3940")
    public void shouldSeeUserMenuName() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .shouldSee(onMessagePage().mail360HeaderBlock().userMenuDropdown());
        user.defaultSteps().shouldSeeThatElementHasText(
            onMessagePage().mail360HeaderBlock().userMenuDropdown().currentUser(),
            lock.firstAcc().getLogin()
        );
    }

    @Step("Включаем компактую шапку и открываем поиск")
    private void openSearch() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем компактную шапку",
            of(LIZA_MINIFIED_HEADER, STATUS_ON)
        );
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().mail360HeaderBlock().searchBtnCompactMode());
    }
}
