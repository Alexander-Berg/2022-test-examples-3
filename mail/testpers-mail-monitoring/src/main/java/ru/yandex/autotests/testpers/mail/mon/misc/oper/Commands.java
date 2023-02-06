package ru.yandex.autotests.testpers.mail.mon.misc.oper;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import ru.yandex.autotests.testpers.mail.mon.beans.AccountInformation;
import ru.yandex.autotests.testpers.mail.mon.beans.Messages;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.MatcherConfig.ErrorDescriptionType.HAMCREST;
import static com.jayway.restassured.config.MatcherConfig.matcherConfig;
import static org.apache.commons.lang3.Validate.notNull;
import static org.hamcrest.Matchers.nullValue;

/**
 * User: lanwen
 * Date: 04.02.15
 * Time: 19:54
 */
public class Commands {
    static {
        RestAssured.config = RestAssured.config().matcherConfig(matcherConfig().errorDescriptionType(HAMCREST));
    }

    public static final String FIRST_HANDLER_PATH = "handlers[0].data";

    public static Messages messages(RequestSpecification reqSpec) {
        return given()
                .spec(reqSpec)
                .basePath("/neo2/handlers/handlers3.jsx")
                .param("_handlers", "messages")
                .when().post().then().assertThat().body("handlers[0].error", nullValue())
                .extract()
                .jsonPath().getObject(FIRST_HANDLER_PATH, Messages.class);
    }


    public static AccountInformation accountInformation(RequestSpecification reqSpec) {
        return given()
                .spec(reqSpec)
                .basePath("/neo2/handlers/handlers3.jsx")
                .param("_handlers", "account-information")
                .post()
                .jsonPath().getObject(FIRST_HANDLER_PATH, AccountInformation.class);
    }


    public static Response doSendJson(RequestSpecification reqSpec,
                                      AccountInformation info) {
        return given()
                .spec(reqSpec)
                .basePath("/neo2/handlers/do-send-json.jsx")
                .param("compose_check", notNull(info, "Пустой ответ acc-info!").getComposeCheck())
                .param("_ckey", info.getCkey())
                .param("ttype", "plain")
                .param("send", "Тело")
                .when().post();

    }
}
