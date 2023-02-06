package ru.yandex.autotests.innerpochta.cal.api;

import io.restassured.builder.RequestSpecBuilder;
import ru.yandex.autotests.innerpochta.steps.beans.ApiCalWebApi;

import static ru.yandex.autotests.innerpochta.util.Utils.isCorp;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;


/**
 * @author cosmopanda
 */
class CalApiConfig {

    private CalApiConfig() {
    }

    private static String host;

    static ApiCalWebApi apiConfig() {
        if (isCorp())
            host = "calendar.qa.yandex-team.ru";
        else
            host = "calendar.qa.yandex.ru";
        return ApiCalWebApi.calwebapi(
                ApiCalWebApi.Config.calwebapiConfig().withReqSpecSupplier(
                    () -> new RequestSpecBuilder()
                        .setRelaxedHTTPSValidation()
                        .setBaseUri(apiProps().getbaseUri())
                        .setBasePath(apiProps().modelsUrl())
                        .addHeader("Host", host)
                        .addHeader("Connection", "keep-alive")
                        .addHeader("x-yandex-maya-locale", "ru")
                        .addHeader("x-yandex-maya-timezone", "Europe/Moscow")
                ));
    }

    static ApiCalWebApi apiInfoConfig() {
        return ApiCalWebApi.calwebapi(
            ApiCalWebApi.Config.calwebapiConfig().withReqSpecSupplier(
                () -> new RequestSpecBuilder()
                    .setRelaxedHTTPSValidation()
                    .setBaseUri(apiProps().getbaseUri())
            ));
    }
}