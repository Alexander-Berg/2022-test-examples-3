package ru.yandex.market.delivery.deliveryintegrationtests.wms.client;


import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.wms.common.spring.tvm.TvmClientUtils;
import ru.yandex.market.wms.trace.request.RequestIdHolder;
import ru.yandex.market.wms.trace.request.RequestUtils;

import static ru.yandex.market.request.trace.RequestTraceUtil.USER_TICKET_HEADER;

@Resource.Classpath("wms/infor.properties")
public class InboundManagementClient {


    private static final int REQUEST_TIMEOUT = 120000;

    private static final RestAssuredConfig CONFIG = RestAssured.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", REQUEST_TIMEOUT)
                    .setParam("http.socket.timeout", REQUEST_TIMEOUT)
                    .setParam("http.connection-manager.timeout", REQUEST_TIMEOUT)
            );

    private final Logger log = LoggerFactory.getLogger(InboundManagementClient.class);

    public InboundManagementClient() {
        PropertyLoader.newInstance().populate(this);
        inboundManagementHost = host + inboundManagement;
    }

    @Property("infor.host")
    private String host;

    @Property("infor.inboundmanagement")
    private String inboundManagement;

    @Property("infor.token")
    private String token;

    private final String inboundManagementHost;

    private static final String PRIORITIES_CALC_PATH = "/priorities/calculate";

    private ValidatableResponse basePutRequest(String path) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssured
                .given()
                .config(CONFIG)
                .filter(new AllureRestAssured())
                .baseUri(inboundManagementHost)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .header(TvmClientUtils.JWT_TOKEN_HEADER, token)
                .header(USER_TICKET_HEADER, token)
                .header(RequestUtils.REQUEST_ID_HEADER, RequestIdHolder.get().next())
                .when()
                .put(path)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse calculatePriorities() {
        log.info("Calling Inbound Management calculate priorities");
        return basePutRequest(PRIORITIES_CALC_PATH);
    }
}
