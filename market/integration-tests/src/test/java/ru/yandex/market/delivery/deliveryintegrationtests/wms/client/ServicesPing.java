package ru.yandex.market.delivery.deliveryintegrationtests.wms.client;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

@Resource.Classpath("wms/infor.properties")
public class ServicesPing {

    @Property("infor.host")
    private String multitestingHost;

    public ServicesPing() {
        PropertyLoader.newInstance().populate(this);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private ValidatableResponse basePingRequest(String path) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssured
                .given()
                .filter(new AllureRestAssured())
                .baseUri(multitestingHost)
                .when()
                .get(path)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse apiPing() {
        return basePingRequest("/api/hc/ping");
    }

    public ValidatableResponse ytUtilPing() {
        return basePingRequest("/yt-util/hc/ping");
    }

    public ValidatableResponse authPing() {
        return basePingRequest("/auth/hc/ping");
    }

    public ValidatableResponse wcsEmulatorPing() {
        return basePingRequest("/wcs-emulator/hc/ping");
    }

    public ValidatableResponse datacreatorPing() {
        return basePingRequest("/datacreator/hc/ping");
    }

    public ValidatableResponse taskrouterPing() {
        return basePingRequest("/taskrouter/hc/ping");
    }

    public ValidatableResponse consolidationPing() {
        return basePingRequest("/consolidation/hc/ping");
    }

    public ValidatableResponse droppingPing() {
        return basePingRequest("/dropping/hc/ping");
    }

    public ValidatableResponse placementPing() {
        return basePingRequest("/placement/hc/ping");
    }

    public ValidatableResponse packingPing() {
        return basePingRequest("/packing2/hc/ping");
    }

    public ValidatableResponse reporterPing() {
        return basePingRequest("/reporter/hc/ping");
    }

    public ValidatableResponse pickingPing() {
        return basePingRequest("/picking/hc/ping");
    }

    public ValidatableResponse receivingPing() {
        return basePingRequest("/receiving/hc/ping");
    }

    public ValidatableResponse shippingPing() {
        return basePingRequest("/shipping/hc/ping");
    }

    public ValidatableResponse inventorizationPing() {
        return basePingRequest("/inventorization/hc/ping");
    }

    public ValidatableResponse autostartPing() {
        return basePingRequest("/autostart/hc/ping");
    }

    public ValidatableResponse replenishmentPing() {
        return basePingRequest("/replenishment/hc/ping");
    }

    public ValidatableResponse corePing() {
        return basePingRequest("/core/hc/ping");
    }

    public ValidatableResponse shippingsorterPing() {
        return basePingRequest("/shippingsorter/hc/ping");
    }

    public ValidatableResponse servicebusPing() {
        return basePingRequest("/servicebus/hc/ping");
    }

    public ValidatableResponse schedulerv2Ping() {
        return basePingRequest("/scheduler2/hc/ping");
    }

    public ValidatableResponse ordermanagementPing() {
        return basePingRequest("/ordermanagement/hc/ping");
    }

    public ValidatableResponse transportationPing() {
        return basePingRequest("/transportation/hc/ping");
    }
}
