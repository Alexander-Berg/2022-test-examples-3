package ru.yandex.autotests.innerpochta.conditions;

import io.restassured.RestAssured;
import io.restassured.config.DecoderConfig;
import io.restassured.config.EncoderConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.Cookies;
import io.restassured.mapper.ObjectMapperType;
import org.junit.runner.Description;
import ru.yandex.autotests.innerpochta.steps.beans.account.AccountInformation;
import ru.yandex.autotests.passport.api.core.cookie.YandexCookies;
import ru.yandex.autotests.passport.api.core.objects.UserWithProps;
import ru.yandex.autotests.passport.api.core.steps.AuthSteps;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.StartrekClientBuilder;
import ru.yandex.startrek.client.error.EntityNotFoundException;
import ru.yandex.startrek.client.model.Issue;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.accountInformationHandler;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.getAccInfo;
import static ru.yandex.autotests.innerpochta.api.DoSendJsonHandler.doSendJsonHandler;
import static ru.yandex.autotests.innerpochta.util.Utils.getYaUidCookie;
import static ru.yandex.autotests.innerpochta.util.props.TestProperties.testProperties;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;
import static ru.yandex.autotests.innerpochta.util.props.YandexServicesProperties.yandexServicesProps;
import static ru.yandex.autotests.passport.api.common.Utils.getRandomInternalUserIp;


/**
 * Класс, реализующий интерфейс {@link IgnoreCondition} и предазначенный для использования
 * в аннотации {@link ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore}.
 * Метод {@link #isSatisfied()} вернёт <code>true</code>, если у аннотируемого метода есть аннотация
 * {@link ru.yandex.qatools.allure.annotations.Issue}, тикет, указанный в ней, существует и имеет
 * любой статус кроме "закрыт".
 *
 * @see ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore
 * @see IgnoreCondition
 *
 * @author pavponn
 */
public class TicketInProgress implements IgnoreCondition {

    private static final String INFO_EMAIL_SENDER = "yndx-mariya-murm-y9kc4ti2";
    private static final String PASSWORD = "Y0Usha11N0Tpass";

    private static final String BASE_URI = "https://mail.yandex.ru";
    private static final String MODELS_URL = "/web-api/models/liza1";

    private String issue = "";
    private Description description;

    @Override
    public String getMessage() {
        return "issue " + issue + " hasn't been closed yet or doesn't exist";
    }

    @Override
    public void setFields(Description description) {
        this.description = description;
        if (hasIssueAnnotation(description)) {
            setIssue(getIssue(description));
        }
    }

    @Override
    public boolean isSatisfied() {
        if (getIssue().equals("")) {
            if (shouldNotDoMailing()) {
                return true;
            }
            setConfig();
            sendTicketNotSetMessage();
            return true;
        }
        try {
            Session session = StartrekClientBuilder.newBuilder()
                .uri("https://st-api.yandex-team.ru")
                .maxConnections(10)
                .connectionTimeout(1, TimeUnit.SECONDS)
                .socketTimeout(500, TimeUnit.MILLISECONDS)
                .build(yandexServicesProps().getStartrekToken());
            Issue issue = session.issues().get(getIssue());
            if (issue.getStatus().getKey().equals("closed")) {
                if (shouldNotDoMailing()) {
                    return false;
                }
                setConfig();
                sendTicketClosedMessage();
                return false;
            }
        } catch (EntityNotFoundException e) {
            if (shouldNotDoMailing()) {
                return true;
            }
            setConfig();
            sendNoIssueFoundMessage();
            return true;
        } catch (Exception e) {
            //Что-то случилось, письмо не отправлять тест запустить
            return false;
        }
        return true;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    private void sendTicketClosedMessage() {
        final String msgSubject = format(
            "%s: Тикет %s закрыт, следует убрать аннотацию",
            urlProps().getProject().toUpperCase(),
            getIssue()
        );
        final String msgBody = format(
            "Тикет %s закрыт, а тест %s был запущен.\n" +
                "Для начала, стоит удостовериться, что тест прошёл. Затем, чтобы сохранить чистоту и читаемость кода, " +
                "надо убрать аннотации @ConditionalIgnore и @Issue " +
                "с теста.\n" +
                "%s\n" +
                "Удачного тестирования, самурай!",
            getIssueLink(),
            description.getMethodName(),
            getTestInformationString(description)

        );
        sendInfoMessage(msgSubject, msgBody);
    }

    private void sendNoIssueFoundMessage() {
        final String msgSubject = format(
            "%s: Тикет %s не был найден в Трекере",
            urlProps().getProject().toUpperCase(),
            getIssue()
        );
        final String msgBody = format(
            "Тикет %s не был найден в Трекере, поэтому тест %s было решено не запускать.\n" +
                "Надо исправить название тикета в тестовом методе.\n" +
                "%s\n" +
                "Наверное, кто-то опечатался ¯\\_(ツ)_/¯ ",
            getIssueLink(),
            description.getMethodName(),
            getTestInformationString(description)
        );
        sendInfoMessage(msgSubject, msgBody);
    }

    private void sendTicketNotSetMessage() {
        final String msgSubject = format(
            "%s: Тикет не установлен %s.%s",
            urlProps().getProject().toUpperCase(),
            description.getTestClass().getSimpleName(),
            description.getMethodName()
        );
        final String msgBody = format(
            "Wake up, samurai!\n" +
                "Тикет не установлен, поэтому трудно понять, почему тест %s игнорируется.\n" +
                "На всякий случай я решил не запускать этот тест, но хорошо было бы разобраться с ним. " +
                "Быть может, нужно убрать аннотацию @ConditionalIgnore или добавить issue, используя аннотацию @Issue. " +
                "Возможно, этот тест нужно исправить или вовсе избавиться от него.\n" +
                "%s\n" +
                "Пока проблему не решат, я буду продолжать напоминать о ней!",
            description.getMethodName(),
            getTestInformationString(description)
        );
        sendInfoMessage(msgSubject, msgBody);
    }

    private void sendInfoMessage(String msgSubject, String msgBody) {
        Cookies cookies = getAuthCookies();
        AccountInformation accInfo = getAccountInfo(cookies);
        doSendJsonHandler()
            .withBaseUri(BASE_URI)
            .withCookies(cookies)
            .withCookie(getYaUidCookie())
            .withAccInfo(accInfo)
            .withReceiver(testProperties().getMailingAccount())
            .withSubject(msgSubject)
            .withMessageBody(msgBody)
            .withPlainTextType()
            .callDoSendJson();
    }

    private String getIssueLink() {
        return "https://st.yandex-team.ru/" + getIssue();
    }

    private void setConfig() {
        RestAssured.config = RestAssuredConfig.config()
            .encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"))
            .decoderConfig(DecoderConfig.decoderConfig().defaultContentCharset("UTF-8"))
            .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                .defaultObjectMapperType(ObjectMapperType.GSON));
    }

    private AccountInformation getAccountInfo(Cookies cookies) {
        return getAccInfo(
            accountInformationHandler(BASE_URI)
                .withCookies(cookies)
                .withCookie(getYaUidCookie())
                .callAccountInformation(MODELS_URL)
        );
    }

    private Cookies getAuthCookies() {
        return AuthSteps
            .forUser(UserWithProps.builder().login(INFO_EMAIL_SENDER).password(PASSWORD).giveUser())
            .withClientIp(getRandomInternalUserIp())
            .auth(YandexCookies::parseCookies);
    }

    private boolean shouldNotDoMailing() {
        String value = System.getenv().getOrDefault("MAILING_INFO", "TRUE");
        return value.equals("FALSE");
    }

    private static boolean hasIssueAnnotation(Description description) {
        return description.getAnnotation(ru.yandex.qatools.allure.annotations.Issue.class) != null;
    }

    private static boolean hasTitleAnnotation(Description description) {
        return description.getAnnotation(Title.class) != null;
    }

    private static String getIssue(Description description) {
        return description.getAnnotation(ru.yandex.qatools.allure.annotations.Issue.class).value();
    }


    private static String getTitle(Description description) {
        return hasTitleAnnotation(description) ? description.getAnnotation(Title.class).value() : "No title";
    }

    private static String getTestInformationString(Description description) {
        return format(
            "\nИнформация о тесте\n" +
                "Project: %s\n" +
                "Package: %s\n" +
                "Class: %s\n" +
                "Method: %s\n" +
                "Title: %s\n",
            urlProps().getProject(),
            description.getTestClass().getPackage().getName(),
            description.getTestClass().getSimpleName(),
            description.getMethodName(),
            getTitle(description)
        );
    }
}
