package ru.yandex.autotests.innerpochta.rules.acclock;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.DecoderConfig;
import io.restassured.config.EncoderConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.util.AllureLogger;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.autotests.passport.api.core.cookie.YandexCookies;
import ru.yandex.autotests.passport.api.core.tools.RetryFilter;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.util.passport.PassportApisV2.apiBundle;
import static ru.yandex.autotests.innerpochta.util.passport.PassportApisV2.apiBundleCorp;
import static ru.yandex.autotests.passport.api.common.Utils.getRandomInternalUserIp;
import static ru.yandex.autotests.passport.api.core.api.CommonApiSettings.filteredBy;
import static ru.yandex.autotests.passport.api.core.api.CommonApiSettings.matchesSpec;
import static ru.yandex.autotests.passport.api.core.api.CommonApiSettings.validatedWith;
import static ru.yandex.autotests.passport.api.core.api.JSONResponses.shouldBe200Ok;
import static ru.yandex.autotests.passport.api.core.api.JSONResponses.shouldBePythonApiError;
import static ru.yandex.autotests.passport.api.core.api.captcha.CaptchaRetryFilter.withRetryIfCaptchaShowed;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.EMPTY_COOKIES_HEADER;

/*
Класс для хранения авторизационной информации об аккаунтах.
Необходим чтобы не ходить за этой информацией в блэкбокс несколько раз.
 */
public class AccountManager {
    private static AccountManager INSTANCE;

    private final HashMap<String, HashMap<String, Cookies>> accounts = new HashMap<>();
    private final HashMap<String, Cookies> accountsCorp = new HashMap<>();

    private AccountManager() {

    }

    public static synchronized AccountManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AccountManager();
        }
        return INSTANCE;

    }

    @Step("Получаем куки для аккаунта «{0}» на домене «{2}»")
    public Cookies getAuthCookies(String login, String password, YandexDomain yandexDomain) {
        String domain = yandexDomain.getDomain();
        if (accounts.containsKey(login)) {
            if (accounts.get(login).containsKey(domain)) {
                AllureLogger.logToAllure(String.format(
                    "Нашли куки для логина «%s» и домена «%s». Используем их. Список кук: «%s»",
                    login,
                    domain,
                    accounts.get(login).get(domain)
                ));
                return accounts.get(login).get(domain);
            }
        }
        Cookies cookies;
        HashMap<String, Cookies> domainCookie = new HashMap<>();
        System.out.printf("Начинаем получение кук в блэкбоксе %s%n", new Date());
        RestAssured.config = RestAssuredConfig.config()
            .encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"))
            .decoderConfig(DecoderConfig.decoderConfig().defaultContentCharset("UTF-8"))
            .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                .defaultObjectMapperType(ObjectMapperType.GSON));
        cookies = apiBundle()
            .auth().password().submit()
            .withDefaults()
            .withLogin(login)
            .withPassword(password)
            .withYaClientHostHeader("passport.yandex." + domain)
            .withYaClientCookieHeader(EMPTY_COOKIES_HEADER)
            .withYaConsumerClientIpHeader(getRandomInternalUserIp())
            .withPolicy("long")
            .withReq(filteredBy(withRetryIfCaptchaShowed()))
            .withReq(filteredBy(RetryFilter.retry()
                .ifResponse(matchesSpec(shouldBePythonApiError("account.not_found")))
                .withMaxRetries(3).withDelay(500, TimeUnit.MILLISECONDS)))
            .post(validatedWith(shouldBe200Ok()).andThen(YandexCookies::parseCookies));
        System.out.printf("Закончили получение кук в блэкбоксе %s%n", new Date());
        if (cookies.size() < 3) {
            AllureLogger.logToAllure(String.format(
                "Получили очень мало кук, не записываем. Список кук: «%s»",
                cookies
            ));
            return cookies;
        }
        domainCookie.put(domain, cookies);
        if (!accounts.containsKey(login)) {
            accounts.put(login, domainCookie);
        } else {
            accounts.get(login).putAll(domainCookie);
        }
        return cookies;
    }

    @Step("Получаем куки для корп аккаунта «{0}»")
    public Cookies getCorpAuthCookies(String login, String password) {
        if (accountsCorp.containsKey(login)) {
            AllureLogger.logToAllure(String.format(
                "Нашли куки для логина «%s». Используем их.",
                login
            ));
            return accountsCorp.get(login);
        }
        System.out.printf("Начинаем получение кук в блэкбоксе %s%n", new Date());
        Cookies authCookies;
        RestAssured.config = RestAssuredConfig.config()
            .encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"))
            .decoderConfig(DecoderConfig.decoderConfig().defaultContentCharset("UTF-8"))
            .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                .defaultObjectMapperType(ObjectMapperType.GSON));
        authCookies = apiBundleCorp()
            .auth().password().submit()
            .withDefaults()
            .withYaClientHostHeader("passport.yandex-team.ru")
            .withYaClientCookieHeader(EMPTY_COOKIES_HEADER)
            .withYaConsumerClientIpHeader(getRandomInternalUserIp())
            .withPolicy("long")
            .withLogin(login)
            .withPassword(password)
            .withReq(filteredBy(withRetryIfCaptchaShowed()))
            .post(validatedWith(shouldBe200Ok()).andThen(YandexCookies::parseCookies));
        System.out.printf("Закончили получение кук в блэкбоксе %s%n", new Date());
        Response response = given()
            .spec(
                new RequestSpecBuilder()
                    .addCookies(authCookies)
                    .setBaseUri("https://passport.yandex-team.ru/auth/guard")
                    .addQueryParam("retpath", "https://mail.yandex-team.ru/")
                    .build()
            )
            .redirects().follow(false)
            .filter(log())
            .get();
        String sessguard_container = response.htmlPath().getString("html.body.form.input.@value");
        response = given()
            .spec(
                new RequestSpecBuilder()
                    .addCookies(authCookies)
                    .setBaseUri("https://guard.mail.yandex-team.ru/set")
                    .addQueryParam("container", sessguard_container)
                    .addParam("container", sessguard_container)
                    .build()
            )
            .redirects().follow(false)
            .filter(log())
            .post();
        Cookie sessguard = response.getDetailedCookies().get("sessguard");
        List<Cookie> authList =
            authCookies.asList().stream().filter(x -> !Objects.equals(x.getName(), "sessguard"))
                .collect(Collectors.toList());
        authList.add(sessguard);
        authCookies = new Cookies(authList);
        accountsCorp.put(login, authCookies);
        return authCookies;
    }
}
