package ru.yandex.autotests.innerpochta.rules.acclock;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSender;
import io.restassured.specification.RequestSpecification;
import org.apache.log4j.Logger;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.util.AllureLogger;
import ru.yandex.autotests.innerpochta.util.AllureStepLogger;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;
import static ru.yandex.autotests.innerpochta.util.props.YandexServicesProperties.yandexServicesProps;

public class TestUserService {

    private final Logger logger = Logger.getLogger(AllureStepLogger.class);
    private final String TUS_URL = "https://tus.yandex-team.ru";
    private String consumer = "mailweb_test";
    private final String ENV = "prod";
    private final int LOCK_DURATION = 350;
    private final String FALSE = "false";
    private final String DEFAULT_TAG = "default";
    private final String NEW_USER_TAG = "new_user";
    private final String[] TAGS_LIST = {"default", "default1", "default2", "default3", "default4", "default5",
        "default6", "default7"};
    private String testClassName = "";

    public void setTestClassName(String testClassName) {
        this.testClassName = testClassName;
    }

    private void getConsumer() {
        if (urlProps().getProject().equals("cal")) {
            consumer = "calweb_test";
        }
    }

    private String getRandomTag() {
        if (urlProps().getProject().equals("cal")) {
            return DEFAULT_TAG;
        }
        int rnd = new Random().nextInt(TAGS_LIST.length);
        return TAGS_LIST[rnd];
    }

    public Account getAcc(String... tags) {
        Response resp = null;
        Map account;
        String login;
        String password;
        String tags_list;
        Set<String> set = new HashSet<>(Arrays.asList(tags));
        tags_list = tags.length > 0 ? String.join(",", set) : getRandomTag();

        getConsumer();
        try {
            RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(URI.create(TUS_URL))
                .addParam("env", ENV)
                .addParam("tus_consumer", consumer)
                .addParam("ignore_locks", FALSE)
                .addParam("tags", tags_list)
                .addParam("lock_duration", LOCK_DURATION)
                .addParam("running_test_class", testClassName)
                .setRelaxedHTTPSValidation();

            RequestSpecification requestSpec = builder.build();

            resp = retryRequestIfNot200(
                given().spec(requestSpec)
                    .filter(log())
                    .basePath("1/get_account/")
                    .header("Authorization", "OAuth " + yandexServicesProps().getTusToken())
                    .when()
            );

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        account = resp.getBody().jsonPath().getMap("account");
        login = account.get("login").toString();
        password = account.get("password").toString();
        logger.info(String.format("Locked acc: %s with password %s", login, password));
        logger.info(String.format("Tags %s", tags_list));

        return new Account(login, password);
    }


    private static Response retryRequestIfNot200(RequestSender spec) {
        int retries = 0;
        Response response = spec.get();
        while (response.statusCode() != 200 && retries < 3) {
            AllureLogger.logToAllure(response.body().prettyPrint());
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(2, 10));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retries++;
            response = spec.get();
        }
        return response;
    }

    public void createAcc() {
        getConsumer();

        try {
            RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(URI.create(TUS_URL))
                .addParam("env", ENV)
                .addParam("tus_consumer", consumer)
                .addParam("tags", DEFAULT_TAG)
                .setRelaxedHTTPSValidation();

            RequestSpecification requestSpec = builder.build();

            given().spec(requestSpec)
                .filter(log())
                .basePath("1/create_account/portal/")
                .header("Authorization", "OAuth " + yandexServicesProps().getTusToken())
                .when()
                .post();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Account createAndLockAcc() {
        getConsumer();
        Response resp;
        Map account;
        String login;
        String password;

        RequestSpecBuilder builder = new RequestSpecBuilder()
            .setBaseUri(URI.create(TUS_URL))
            .addParam("env", ENV)
            .addParam("tus_consumer", consumer)
            .addParam("tags", NEW_USER_TAG)
            .setRelaxedHTTPSValidation();

        RequestSpecification requestSpec = builder.build();

        resp = given().spec(requestSpec)
            .filter(log())
            .basePath("1/create_account/portal/")
            .header("Authorization", "OAuth " + yandexServicesProps().getTusToken())
            .when()
            .post();

        account = resp.getBody().jsonPath().getMap("account");
        login = account.get("login").toString();

        RequestSpecBuilder accLockBuilder = new RequestSpecBuilder()
            .setBaseUri(URI.create(TUS_URL))
            .addParam("env", ENV)
            .addParam("tus_consumer", consumer)
            .addParam("ignore_locks", FALSE)
            .addParam("login", login)
            .addParam("lock_duration", LOCK_DURATION)
            .addParam("running_test_class", testClassName)
            .setRelaxedHTTPSValidation();

        RequestSpecification accLockRequestSpec = accLockBuilder.build();

        retryRequestIfNot200(
            given().spec(accLockRequestSpec)
                .filter(log())
                .basePath("1/get_account/")
                .header("Authorization", "OAuth " + yandexServicesProps().getTusToken())
                .when()
        );

        account = resp.getBody().jsonPath().getMap("account");
        login = account.get("login").toString();
        password = account.get("password").toString();
        logger.info(String.format("Created acc: %s with password %s", login, password));

        return new Account(login, password);
    }

    public void createAcc(String... tags) {

        Response resp = null;
        Map account;
        String login;
        String password;
        String tags_list;
        Set<String> set = new HashSet<>(Arrays.asList(tags));
        tags_list = String.join(",", set);

        getConsumer();

        try {
            RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(URI.create(TUS_URL))
                .addParam("env", ENV)
                .addParam("tags", tags_list)
                .addParam("tus_consumer", consumer)
                .setRelaxedHTTPSValidation();

            RequestSpecification requestSpec = builder.build();

            resp = given().spec(requestSpec)
                .filter(log())
                .basePath("1/create_account/portal/")
                .header("Authorization", "OAuth " + yandexServicesProps().getTusToken())
                .when()
                .post();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        account = resp.getBody().jsonPath().getMap("account");
        login = account.get("login").toString();
        password = account.get("password").toString();

        logger.info(String.format("Created acc: %s with password %s", login, password));
    }

    public void unlockAcc(String uid) {

        getConsumer();
        try {
            RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(URI.create(TUS_URL))
                .addParam("env", ENV)
                .addParam("tus_consumer", consumer)
                .addParam("uid", uid)
                .addParam("running_test_class", testClassName)
                .setRelaxedHTTPSValidation();

            RequestSpecification requestSpec = builder.build();

            Response resp = given().spec(requestSpec)
                .filter(log())
                .basePath("1/unlock_account/")
                .header("Authorization", "OAuth " + yandexServicesProps().getTusToken())
                .when()
                .post();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void saveToTus(String login, String password, String... tags) {

        getConsumer();
        String tags_list;
        Set<String> set = new HashSet<>(Arrays.asList(tags));
        tags_list = String.join(",", set);

        try {
            RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(URI.create(TUS_URL))
                .addParam("env", ENV)
                .addParam("tus_consumer", consumer)
                .addParam("login", login)
                .addParam("password", password)
                .addParam("tags", tags_list)
                .setRelaxedHTTPSValidation();

            RequestSpecification requestSpec = builder.build();

            given().spec(requestSpec)
                .filter(log())
                .basePath("1/save_account/")
                .header("Authorization", "OAuth " + yandexServicesProps().getTusToken())
                .when()
                .post();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
