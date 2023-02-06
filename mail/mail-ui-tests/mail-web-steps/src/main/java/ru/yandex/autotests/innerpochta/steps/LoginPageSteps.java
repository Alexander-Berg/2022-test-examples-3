package ru.yandex.autotests.innerpochta.steps;

import io.restassured.http.Cookies;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccountManager;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.autotests.passport.api.core.cookie.YandexCookies;
import ru.yandex.autotests.passport.api.core.objects.UserWithProps;
import ru.yandex.autotests.passport.api.core.tools.RetryFilter;
import ru.yandex.autotests.passport.api.tools.auth.AuthActions;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Step;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.of;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.apache.log4j.LogManager.getLogger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.api.ApiAuthPasswordSubmit.addUsersToCookies;
import static ru.yandex.autotests.innerpochta.api.ApiAuthPasswordSubmit.fillingCookiesWithBallastUsers;
import static ru.yandex.autotests.innerpochta.data.Languages.RU;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_BASE_URL;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASSPORT_URL;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.UNDEFINED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_STORED_COMPOSE_STATES;
import static ru.yandex.autotests.innerpochta.util.passport.PassportApisV2.apiBundle;
import static ru.yandex.autotests.innerpochta.util.props.ExperimentsProperties.experimentsProperties;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;
import static ru.yandex.autotests.passport.api.common.Utils.getRandomInternalUserIp;
import static ru.yandex.autotests.passport.api.core.api.CommonApiSettings.filteredBy;
import static ru.yandex.autotests.passport.api.core.api.CommonApiSettings.matchesSpec;
import static ru.yandex.autotests.passport.api.core.api.CommonApiSettings.validatedWith;
import static ru.yandex.autotests.passport.api.core.api.JSONResponses.shouldBe200Ok;
import static ru.yandex.autotests.passport.api.core.api.JSONResponses.shouldBePythonApiError;
import static ru.yandex.autotests.passport.api.core.api.captcha.CaptchaRetryFilter.withRetryIfCaptchaShowed;
import static ru.yandex.autotests.passport.api.core.cookie.YandexCookies.YANDEXUID;
import static ru.yandex.autotests.passport.matchers.WDCookieMatcher.hasCookie;
import static ru.yandex.autotests.passport.matchers.WDCookieMatcher.withName;

@SuppressWarnings("UnusedReturnValue")
public class LoginPageSteps {

    private AllureStepStorage user;
    private WebDriverRule driverRule;
    private Account acc;

    public LoginPageSteps(WebDriverRule driverRule, AllureStepStorage user) {
        this.driverRule = driverRule;
        this.user = user;
    }

    //--------------------------------------------------------
    // LOGIN_WELCOME_PAGE_OFF PAGE STEPS
    //-------------------------------------------------------

    @Step("Логинимся юзером {0}")
    public LoginPageSteps forAcc(Account acc) {
        this.acc = acc;
        return this;
    }

    @Step("Выполняем служебные действия и открываем mail.yandex.ru")
    public LoginPageSteps logins() {
        loginsToDomain(YandexDomain.RU);
        return this;
    }

    @Step("Логинимся в почту на домене {0}")
    public LoginPageSteps loginsToDomain(YandexDomain domain) {
        String url = substringBeforeLast(driverRule.getBaseUrl(), ".") + "." + domain.getDomain();
        setUpBeforeLoginToDomain(domain);
        user.defaultSteps().switchLanguage(RU.lang());
        if (!driverRule.getDriver().getCurrentUrl().contains(url)) {
            user.defaultSteps().opensDefaultUrlWithDomain(domain.getDomain());
        }
        setExperiments();
        prepareSettings();
        shouldBeAtFullyLoginedUrl();
        return this;
    }

    @Step("Логинимся на домене {0} пользователем коннекта без сервисов")
    public LoginPageSteps loginsToDomainByConnectUser(YandexDomain domain) {
        setUpBeforeLoginToDomain(domain);
        user.defaultSteps().opensDefaultUrlWithDomain(domain.getDomain())
            .shouldBeOnUrl(containsString(domain.getDomain()));
        prepareSettings();
        return this;
    }

    @Step("Авторизуемся в почте на домене {0} без перехода в инбокс")
    @Description("Степ для тестирования сценариев авторизации с последующими редиректами на вспомогательные страницы")
    public LoginPageSteps tryLoginToDomain(YandexDomain domain) {
        setUpBeforeLoginToDomain(domain);
        user.defaultSteps().opensDefaultUrlWithDomain(domain.getDomain());
        return this;
    }

    @Step("Логинимся в корпоративную почту")
    public LoginPageSteps loginsToCorp() {
        user.defaultSteps().opensDefaultUrl();
        //Passport magic made by @gladnik
        Cookies cookies = AccountManager.getInstance().getCorpAuthCookies(acc.getLogin(), acc.getPassword());
        Set<Cookie> allCookiesExceptPassport;
        if (driverRule.getDriver().getCurrentUrl().contains("passport")) {
            allCookiesExceptPassport = convertRestAssuredCookiesToSeleniumCookies(cookies);
        } else {
            allCookiesExceptPassport = filterOutCookiesForPassportDomain(
                    convertRestAssuredCookiesToSeleniumCookies(cookies)
            );
        }
        addCookies(allCookiesExceptPassport);
        user.defaultSteps().setsWindowSize(1920, 1080)
            .opensDefaultUrl();
        shouldBeAtFullyLoginedUrl();
        return this;
    }

    @Step("Мультиавторизация: дологиниваем пользователей на домене ru")
    public LoginPageSteps multiLoginWith(Account... accs) {
        //Берем куки залогиненного пользователя(ей) и делаем из них RestAssured cookies для паспорта
        Set<Cookie> selCookies = driverRule.getDriver().manage().getCookies();
        Cookies raCookies = new Cookies(
            selCookies.stream()
                //иначе не сможем сбилдить RestAssured куку
                .filter(cookie -> cookie.getExpiry() != null)
                .map(cookie -> new io.restassured.http.Cookie.Builder(cookie.getName(), cookie.getValue())
                    .setDomain(cookie.getDomain())
                    .setExpiryDate(cookie.getExpiry())
                    .setHttpOnly(cookie.isHttpOnly())
                    .setPath(cookie.getPath())
                    .build())
                .collect(Collectors.toList()));
        //Дологиниваем еще пользователей, получаем их RestAssured куки
        Cookies multiRaCookies = getMultiRaCookies(YandexDomain.RU, raCookies, accs);
        //Делаем из них селениумные куки и добавляем в браузер
        Set<Cookie> multiSelCookieWithoutPassport = filterOutCookiesForPassportDomain(
            convertRestAssuredCookiesToSeleniumCookies(multiRaCookies));
        addCookies(multiSelCookieWithoutPassport);
        user.defaultSteps().opensDefaultUrl();
        setExperiments();
        user.defaultSteps().switchLanguage(RU.lang());
        return this;
    }

    @Step("Мультиавторизация: дологиниваем пользователей на домене {0}")
    public LoginPageSteps multiLoginWith(YandexDomain domain, Account... accs) {
        Set<Cookie> selCookies = driverRule.getDriver().manage().getCookies();
        Cookies raCookies = new Cookies(
            selCookies.stream()
                .filter(cookie -> cookie.getExpiry() != null)
                .map(cookie -> new io.restassured.http.Cookie.Builder(cookie.getName(), cookie.getValue())
                    .setDomain(cookie.getDomain())
                    .setExpiryDate(cookie.getExpiry())
                    .setHttpOnly(cookie.isHttpOnly())
                    .setPath(cookie.getPath())
                    .build())
                .collect(Collectors.toList()));
        Cookies multiRaCookies = getMultiRaCookies(domain, raCookies, accs);
        Set<Cookie> multiSelCookieWithoutPassport = filterOutCookiesForPassportDomain(
            convertRestAssuredCookiesToSeleniumCookies(multiRaCookies));
        addCookies(multiSelCookieWithoutPassport);
        user.defaultSteps().opensDefaultUrlWithDomain(domain.getDomain());
        return this;
    }

    @Step("Получаем мультикуки для пользователей")
    private Cookies getMultiRaCookies(YandexDomain domain, Cookies cookies, Account... accs) {
        for (Account acc : accs) {
            cookies = apiBundle()
                .auth().password().submit()
                .withDefaults()
                .withLogin(acc.getLogin())
                .withPassword(acc.getPassword())
                .withYaClientHostHeader("passport.yandex." + domain.getDomain())
                .withYaClientCookieHeader(YandexCookies.asHeader(cookies))
                .withYaConsumerClientIpHeader(getRandomInternalUserIp())
                .withPolicy("long")
                .withReq(filteredBy(withRetryIfCaptchaShowed()))
                .withReq(filteredBy(RetryFilter.retry()
                    .ifResponse(matchesSpec(shouldBePythonApiError("account.not_found")))
                    .withMaxRetries(3).withDelay(500, TimeUnit.MILLISECONDS)))
                .post(validatedWith(shouldBe200Ok()).andThen(YandexCookies::parseCookies));
        }
        return cookies;
    }

    @Step("Получаем для пользователя куку {0} на домене {1}")
    public LoginPageSteps getsCookie(YandexCookies cookie, YandexDomain domain) {
        user.defaultSteps().opensDefaultUrlWithDomain(domain.getDomain());
        Cookie seleniumCookie = convertRestAssuredCookieToSeleniumCookie(
            AuthActions.authUserWithCaptchaPassingOnDomain(acc.getLogin(), acc.getPassword(), domain)
                .get(cookie.cookieName())
        );
        driverRule.getDriver().manage().addCookie(seleniumCookie);
        assertThat(driverRule.getDriver(), hasCookie(withName(seleniumCookie.getName())));
        user.defaultSteps()
            .setsWindowSize(1920, 1080)
            .opensDefaultUrlWithDomain(domain.getDomain());
        return this;
    }

    private Set<Cookie> convertRestAssuredCookiesToSeleniumCookies(Cookies raCookies) {
        Set<Cookie> selCookies = new HashSet<>();
        for (io.restassured.http.Cookie raCookie : raCookies) {
            selCookies.add(convertRestAssuredCookieToSeleniumCookie(raCookie));
        }
        return selCookies;
    }

    private Cookie convertRestAssuredCookieToSeleniumCookie(io.restassured.http.Cookie raCookie) {
        return new Cookie(raCookie.getName(), raCookie.getValue(), raCookie.getDomain(),
            raCookie.getPath(), raCookie.getExpiryDate(), raCookie.isSecured()
        );
    }

    private Set<Cookie> filterOutCookiesForPassportDomain(Set<Cookie> cookies) {
        return cookies.stream()
            .filter(cookie -> !cookie.getDomain().contains("passport"))
            .collect(Collectors.toSet());
    }

    private LoginPageSteps addCookies(Set<Cookie> cookies) {
        driverRule.getDriver().manage().deleteAllCookies();
        for (Cookie cookie : cookies) {
            driverRule.getDriver().manage().addCookie(cookie);
        }
        return this;
    }

    public LoginPageSteps logins(QuickFragments fragment) {
        logins();
        user.defaultSteps().opensFragment(fragment);
        return this;
    }

    @Step("Получаем куку для UIDов «{0}»")
    private Cookies getCookieFor(Account lastInCookieAcc, int ballastUserAmount) {
        getLogger(this.getClass()).info(
            "DOMAIN from passport ->" + ru.yandex.autotests.passport.api.common.Properties.props().getDomain()
        );
        Cookies cookie = addUsersToCookies(
            fillingCookiesWithBallastUsers(new Cookies(), ballastUserAmount),
            UserWithProps.builder()
                .login(lastInCookieAcc.getLogin())
                .password(lastInCookieAcc.getPassword())
                .giveUser()
        );
        getLogger(this.getClass()).info("cookie val sessin_id ->" + cookie.getValue("Session_id"));
        return cookie;
    }

    @Step("Логинимся несколькими рандомными юзерами ({1} шт.) и {0}")
    public LoginPageSteps multiLoginWithRandomAccountsNumber(Account lastInCookieAcc, int ballastUserAmount) {
        user.defaultSteps().opensDefaultUrl();
        Cookies cs = getCookieFor(lastInCookieAcc, ballastUserAmount);
        Cookie c = new Cookie("Session_id", cs.getValue("Session_id"));
        driverRule.getDriver().manage().deleteAllCookies();
        driverRule.getDriver().manage().addCookie(c);
        user.defaultSteps().opensDefaultUrl();
        return this;
    }

    @Step("Кликаем по ссылке «Почта» в домике на морде")
    public LoginPageSteps clicksOnInboxLinkOnDomik() {
        user.defaultSteps().clicksOn(user.pages().HomePage().domikBlock().inbox());
        return this;
    }

    @Step("Кликаем по ссылке «Написать» в домике на морде")
    public LoginPageSteps clicksOnComposeLinkOnDomik() {
        user.defaultSteps().clicksOn(user.pages().HomePage().domikBlock().compose());
        return this;
    }

    @Step("Меняем юзера через шапку на «{0}»")
    public LoginPageSteps changeUserFromShapka(String name) {
        user.defaultSteps().clicksOn(user.pages().HomePage().mail360HeaderBlock().userMenu())
            .shouldSee(user.pages().HomePage().userMenuDropdown())
            .clicksOnElementWithText(user.pages().HomePage().userMenuDropdown().userList(), name)
            .shouldContainText(user.pages().HomePage().mail360HeaderBlock().userMenu(), name);
        return this;
    }

    @Step("Включаем промку мультиавторизации")
    public LoginPageSteps enableMultiAuthPromo() {
        user.defaultSteps().executesJavaScript("ns.Model.get('settings').setSettingOff('user_dropdown_promo')");
        return this;
    }

    @Step("Делаем захардкоженную yandexuid куку")
    Cookie getYandexuidCookie() {
        final int MAX_AGE_IN_SECONDS = 315360000;
        final String VALID_YANDEXUID = "6113057721516026714";

        return new Cookie.Builder(YANDEXUID.cookieName(), VALID_YANDEXUID)
            .domain(".yandex.ru")
            .expiresOn(Date.from(Instant.now().plusSeconds(MAX_AGE_IN_SECONDS)))
            .build();
    }

    //TODO: Убрать гет-параметр new=1 когда пасспорт закончит экспериментировать
    @Step("Авторизуемся через интерфейс паспорта")
    public LoginPageSteps logInFromPassport(String login, String password) {
        String urlForNewAuthForm = fromUri(driverRule.getDriver().getCurrentUrl())
            .queryParam("new", 1).build().toString();
        user.defaultSteps().shouldBeOnUrl(containsString(PASSPORT_URL))
            .opensUrl(urlForNewAuthForm)
            .inputsTextInElement(user.pages().PassportPage().login(), login)
            .clicksOn(user.pages().PassportPage().submit())
            .inputsTextInElement(user.pages().PassportPage().psswd(), password)
            .clicksOn(user.pages().PassportPage().submit())
            .clicksIfCanOn(user.pages().PassportPage().notNowBtn())
            .clicksIfCanOn(user.pages().PassportPage().notNowEmailBtn())
            .clicksIfCanOn(user.pages().PassportPage().skipAvatarBtn())
            .shouldBeOnUrl(containsString("mail.yandex."))
            .shouldBeOnUrlWith(INBOX);
        setExperiments();
        return this;
    }

    //TODO: Придумать, как обойтись без clicksIfCanOn, тратим много времени
    @Step("Авторизуемся через интерфейс паспорта с морды")
    public LoginPageSteps logInFromPassportFromMorda(String login, String password) {
        String urlForNewAuthForm = fromUri(driverRule.getDriver().getCurrentUrl())
            .queryParam("new", 1).build().toString();
        user.defaultSteps().shouldBeOnUrl(containsString(PASSPORT_URL))
            .opensUrl(urlForNewAuthForm)
            .inputsTextInElement(user.pages().PassportPage().login(), login)
            .clicksOn(user.pages().PassportPage().submit())
            .inputsTextInElement(user.pages().PassportPage().psswd(), password)
            .clicksOn(user.pages().PassportPage().submit())
            .clicksIfCanOn(user.pages().PassportPage().notNowBtn())
            .clicksIfCanOn(user.pages().PassportPage().notNowEmailBtn())
            .clicksIfCanOn(user.pages().PassportPage().skipAvatarBtn())
            .shouldBeOnUrl(containsString(driverRule.getBaseUrl() + "/?uid"));
        setExperiments();
        return this;
    }

    private LoginPageSteps shouldBeAtFullyLoginedUrl() {
        FluentWait wait = new FluentWait(driverRule.getDriver())
            .withTimeout(Duration.ofSeconds(30))
            .withMessage(
                "Не смогли средиректить на залогиненный урл, текущий урл: " + driverRule.getDriver().getCurrentUrl()
            );
        wait.until(ExpectedConditions.urlMatches(".*uid=.*|.*/touch.*"));
        return this;
    }

    private LoginPageSteps setUpBeforeLoginToDomain(YandexDomain domain) {
        user.defaultSteps().opensDefaultUrlWithDomain(domain.getDomain());
        Cookies allCookies = AccountManager.getInstance().getAuthCookies(
            acc.getLogin(),
            acc.getPassword(),
            domain
        );
        Set<Cookie> allCookiesExceptPassport;
        if (driverRule.getDriver().getCurrentUrl().contains("passport")) {
            allCookiesExceptPassport = convertRestAssuredCookiesToSeleniumCookies(allCookies);
        } else {
            allCookiesExceptPassport = filterOutCookiesForPassportDomain(
                    convertRestAssuredCookiesToSeleniumCookies(allCookies)
            );
        }
        addCookies(allCookiesExceptPassport);
        driverRule.getDriver().manage().addCookie(user.loginSteps().getYandexuidCookie());
        user.defaultSteps().setsWindowSize(1920, 1080);
        return this;
    }

    private LoginPageSteps setExperiments() {
        if (urlProps().getExperiments().size() != 0) {
            user.defaultSteps().addExperimentsWithJson(urlProps().getExperiments());
        } else {
            List<String> experiments = experimentsProperties().getExperiments();
            user.defaultSteps().addExperimentsWithYexp(experiments.toArray(new String[0]));
        }
        return this;
    }

    private LoginPageSteps prepareSettings() {
        if (urlProps().getProject().equals("liza") && user.isAuthPresent()) {
            user.apiSettingsSteps()
                .callWithListAndParams(SETTINGS_STORED_COMPOSE_STATES, of(SETTINGS_STORED_COMPOSE_STATES, UNDEFINED));
        }
        return this;
    }
}
