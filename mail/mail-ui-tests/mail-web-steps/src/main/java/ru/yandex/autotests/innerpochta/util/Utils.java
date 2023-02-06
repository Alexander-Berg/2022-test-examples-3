package ru.yandex.autotests.innerpochta.util;


import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.Matcher;
import org.kohsuke.randname.RandomNameGenerator;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.autotests.passport.api.beans.SessionIdBBAnswer;
import ru.yandex.autotests.passport.api.beans.User;
import ru.yandex.autotests.passport.api.core.objects.UserWithProps;
import ru.yandex.autotests.passport.api.core.tools.RetryFilter;
import ru.yandex.qatools.htmlelements.matchers.decorators.MatcherDecoratorsBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.passport.api.common.Utils.log;
import static ru.yandex.autotests.passport.api.core.api.CommonApiSettings.filteredBy;
import static ru.yandex.autotests.passport.api.core.api.CommonApiSettings.validatedWith;
import static ru.yandex.autotests.innerpochta.util.passport.PassportApisV2.apiBlackbox;
import static ru.yandex.autotests.passport.api.core.cookie.YandexCookies.YANDEXUID;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.STR_ERROR;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.STR_ERRORS;
import static ru.yandex.autotests.passport.api.tools.registration.RegUser.registration;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.decorators.MatcherDecoratorsBuilder.should;
import static ru.yandex.qatools.htmlelements.matchers.decorators.TimeoutWaiter.timeoutHasExpired;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 19.06.12
 * <p> Time: 14:22
 */
public class Utils {

    private static RandomNameGenerator rnd = new RandomNameGenerator();
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    /**
     * Проверяет, существует ли на странице такой элемент
     *
     * @param element - уже существующий элемент
     * @param by      - элемент для поиска
     * @return возвращает true, если элемент НАЙДЕН
     */
    public static Boolean isSubElementAvailable(WebElement element, By by) {
        try {
            return element.findElement(by).isDisplayed();
        } catch (NoSuchElementException e) {
            // Returns false because the element is not present in DOM. The
            // try block checks if the element is present but is invisible.
            return false;
        } catch (StaleElementReferenceException e) {
            // Returns false because stale element reference implies that element
            // is no longer visible.
            return false;
        }
    }

    public static Matcher isPresent() {
        return allOf(exists(), isDisplayed());
    }

    /**
     * Возвращает случайную строку
     *
     * @return случайная строка длиной 13 символов
     */
    public static String getRandomString() {
        return Long.toString(Math.abs(new Random().nextLong()), 36);
    }

    public static String getRandomName() {
        return rnd.next();
    }

    /**
     * Генерация случайного целого числа в пределах от min до max
     *
     * @param max
     * @param min
     * @return
     */
    public static int getRandomNumber(int max, int min) {
        Random rand = new Random();
        return rand.nextInt(max - min + 1) + min;
    }

    public static void waitForElementVisible(final WrapsElement element) {
        assertThat("Не дождались видимости элемента " + element + "!", element, withWaitFor(isPresent()));
    }

    public static <T> MatcherDecoratorsBuilder<T> withWaitFor(Matcher<? super T> matcher) {
        return (MatcherDecoratorsBuilder<T>) should(matcher)
            .whileWaitingUntil(timeoutHasExpired(SECONDS.toMillis(15)));
    }

    public static <T> MatcherDecoratorsBuilder<T> withWaitFor(Matcher<? super T> matcher, long timeout) {
        return (MatcherDecoratorsBuilder<T>) should(matcher)
            .whileWaitingUntil(timeoutHasExpired(timeout));
    }

    public static <T> MatcherDecoratorsBuilder<T> withWaitFor(Matcher<? super T> matcher, long timeout, long interval) {
        return (MatcherDecoratorsBuilder<T>) should(matcher)
            .whileWaitingUntil(timeoutHasExpired(timeout).withPollingInterval(interval));
    }

    /**
     * Возвращает uid пользователя с логином login
     *
     * @param login логин пользователя (без @yandex.ru)
     * @return String uid пользователя
     */

    private static ResponseSpecification accountsLoginUidRespSpec = new ResponseSpecBuilder()
            .expectStatusCode(HttpStatus.SC_OK)
            .expectContentType(ContentType.JSON)
            .expectBody(STR_ERRORS, nullValue())
            .expectBody(STR_ERROR, nullValue())
            .expectBody("users", hasSize(1))
            .setDefaultParser(Parser.JSON)
            .build();

    public static String getUserUid(String login) {
        return trimToEmpty(
                apiBlackbox()
                        .userinfo()
                        .withDefaults()
                        .withLogin(login)
                        .withReq(filteredBy(RetryFilter.retry()
                                        .ifResponse(response -> checkUidValueIsEmpty(response, login))
                                        .withMaxRetries(2)
                                )
                        )
                        .fetch(validatedWith(accountsLoginUidRespSpec))
                        .as(SessionIdBBAnswer.class).getUsers().get(0).getUid().getValue());
    }

    private static boolean checkUidValueIsEmpty(Response response, String loginOrUid) {
        List<User> users = response.as(SessionIdBBAnswer.class).getUsers();
        if (users.isEmpty()) {
            log(String.format("Empty users list from blackbox response for %s", loginOrUid));
            return true;
        }
        String uid = users.get(0).getUid().getValue();
        if (StringUtils.isEmpty(uid)) {
            log(String.format("Empty uid value from blackbox response for %s", loginOrUid));
            return true;
        }
        return false;
    }

    public static String get(String requestText) throws IOException {

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(requestText);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();

        try {
            return httpClient.execute(httpGet, responseHandler);
        } catch (ClientProtocolException e) {
            throw new RuntimeException("QA debug: Не удалось выполнить запрос: ClientProtocolException", e);
        }
        //FIX ME! Add "wait for 5 seconds before fail"
    }

    public static int randomPort() {
        final SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < 20; i++) {
            // The +1 on the random int is because
            // Math.abs(Integer.MIN_VALUE) == Integer.MIN_VALUE -- caught
            // by FindBugs.
            final int randomPort = 1024 + (Math.abs(secureRandom.nextInt() + 1) % 60000);
            ServerSocket sock = null;
            try {
                sock = new ServerSocket();
                sock.bind(new InetSocketAddress("127.0.0.1", randomPort));
                final int port = sock.getLocalPort();
                return port;
            } catch (final IOException e) {
            } finally {
                if (sock != null) {
                    try {
                        sock.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        // If we can't grab one of our securely chosen random ports, use
        // whatever port the OS assigns.
        ServerSocket sock = null;
        try {
            sock = new ServerSocket();
            sock.bind(null);
            final int port = sock.getLocalPort();
            return port;
        } catch (final IOException e) {
            return 1024 + (Math.abs(secureRandom.nextInt() + 1) % 60000);
        } finally {
            if (sock != null) {
                try {
                    sock.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static BasicNameValuePair param(String key, String val) {
        return new BasicNameValuePair(key, val);
    }


    public static String getTomorrowDate(String dateFormat) {
        DateTimeFormatter dataFormat = DateTimeFormatter.ofPattern(dateFormat).withLocale(new Locale("ru"));
        Date dt = new Date();
        return LocalDateTime.from(dt.toInstant().atZone(ZoneId.of("UTC+3"))).plusDays(1).format(dataFormat);
    }

    public static String convertDate(String fromPattern, String dateString, String toPattern) throws ParseException {
        DateTimeFormatter dataFormat = DateTimeFormatter.ofPattern(toPattern).withLocale(new Locale("ru"));
        return new SimpleDateFormat(fromPattern).parse(dateString).toInstant()
            .atZone(ZoneId.of("UTC+3")).format(dataFormat);
    }

    public static UserWithProps createNewUser() {
        int tryCount = 2;
        int attempt = 0;

        Throwable whatHappens = null;

        while (attempt < tryCount) {
            try {
                attempt++;
                UserWithProps user = registration().withoutValidation().complete();
                Thread.sleep(5000);
                return user;
            } catch (Throwable t) {
                whatHappens = t;
            }
        }
        throw new RuntimeException(format("Не смогли создать пользователя за %s попыток(ки)", tryCount), whatHappens);
    }

    public static int getCurrentTimeout(int defaultTimeout, WebDriverRule webDriverRule) {
        int countedTimeout = (webDriverRule.getCurrentRetryNum() + 1) * defaultTimeout;
        return countedTimeout <= 30 ? countedTimeout : 30;
    }

    public static FluentWait standardProgressiveWait(WebDriverRule driverRule, int timeout) {
        return new FluentWait(driverRule.getDriver())
            .withTimeout(Duration.ofSeconds(Utils.getCurrentTimeout(timeout, driverRule)))
            .ignoring(NoSuchElementException.class)
            .ignoring(ElementNotVisibleException.class)
            .ignoring(StaleElementReferenceException.class);
    }

    public static FluentWait standardWait(WebDriverRule driverRule, int timeout) {
        return new FluentWait(driverRule.getDriver())
            .withTimeout(Duration.ofSeconds(timeout))
            .ignoring(NoSuchElementException.class)
            .ignoring(ElementNotVisibleException.class)
            .ignoring(StaleElementReferenceException.class);
    }

    public static Boolean isCorp() {
        return UrlProps.urlProps().getBaseUri().contains("yandex-team");
    }

    //yandexuid используется для генерации и валидации ckey, а значит должен быть всегда одинаковый,
    // если не передавать его, то каждый сервис будет проставлять его рандомно и валидация ckey не пройдет
    public static Cookie getYaUidCookie() {
        final int MAX_AGE_IN_SECONDS = 315360000;
        final String VALID_YANDEXUID = "6113057721516026714";

        return new Cookie.Builder(YANDEXUID.cookieName(), VALID_YANDEXUID)
            .setPath("/")
            .setDomain(".yandex.ru")
            .setMaxAge(MAX_AGE_IN_SECONDS)
            .setExpiryDate(Date.from(Instant.now().plusSeconds(MAX_AGE_IN_SECONDS)))
            .build();
    }
}
