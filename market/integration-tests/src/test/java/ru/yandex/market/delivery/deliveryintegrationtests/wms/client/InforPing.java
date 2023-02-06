package ru.yandex.market.delivery.deliveryintegrationtests.wms.client;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;


@Resource.Classpath("wms/infor.properties")
public class InforPing {
    private final Logger log = LoggerFactory.getLogger(InforPing.class);

    @Property("infor.host")
    private String host;

    public InforPing() {
        PropertyLoader.newInstance().populate(this);

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private ValidatableResponse basePingRequest(String path) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssured
                .given()
                .filter(new AllureRestAssured())
                .baseUri(host)
                .when()
                .get(path)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(Matchers.startsWith("0;"));
    }

    public ValidatableResponse businessLogicPing() {
        log.info("Calling Infor business logic /ping");

        return basePingRequest("/oltp/hc/ping");
    }

    public ValidatableResponse backgroundHiLoadPing() {
        log.info("Calling Infor batch - background highload process /ping");

        return basePingRequest("/batch/hc/ping");
    }


    public ValidatableResponse socketServerPing() {
        log.info("Calling Infor socketServer /ping");

        return basePingRequest("/socketserver/hc/ping");
    }

    public ValidatableResponse uiPing() {
        log.info("Calling Infor UI, ALL Datasources /ping");

        return basePingRequest("/scprd/hc/ping");
    }
}
