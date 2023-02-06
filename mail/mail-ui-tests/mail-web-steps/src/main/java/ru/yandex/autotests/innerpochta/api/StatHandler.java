package ru.yandex.autotests.innerpochta.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSender;

import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.util.props.YandexServicesProperties.yandexServicesProps;

public class StatHandler {

    private final RequestSpecBuilder reqSpecBuilder;

    private StatHandler() {
        reqSpecBuilder = new RequestSpecBuilder()
            .addHeader("Authorization", "OAuth " + yandexServicesProps().getStatToken())
            .addQueryParam("scale", "s")
            .addParam("_append_mode", 1);
    }

    public static StatHandler statHandler() {
        return new StatHandler();
    }

    public static Response retryRequestIfNot200(RequestSender spec) {
        int retries = 0;
        Response response = spec.put();
        while (response.statusCode() != 200 && retries < 5) {
            System.out.println(response.body().prettyPrint());
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(2, 10));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retries++;
            response = spec.put();
        }
        return response;
    }

    public StatHandler withData(String data) {
        reqSpecBuilder.setBody(data);
        return this;
    }

    public void callStatHandler() {
        String STAT_HOST = "https://upload.stat.yandex-team.ru";
        String UPLOAD_PATH = "/_api/report/simple_upload";
        String REPORT_PATH = "/Mail/Others/AutotestsStat";
        retryRequestIfNot200(
            given().spec(reqSpecBuilder.build())
                .baseUri(STAT_HOST)
                .basePath(UPLOAD_PATH + REPORT_PATH)
                .filter(log())
                .when()
        );
    }

}
