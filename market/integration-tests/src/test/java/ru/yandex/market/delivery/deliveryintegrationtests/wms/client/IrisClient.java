package ru.yandex.market.delivery.deliveryintegrationtests.wms.client;

import java.util.List;
import java.util.Map;

import client.TVM;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;

@Resource.Classpath("wms/iris.properties")
public class IrisClient {

    private static final int REQUEST_TIMEOUT = 15000;

    private static final RestAssuredConfig CONFIG = RestAssured.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", REQUEST_TIMEOUT)
                    .setParam("http.socket.timeout", REQUEST_TIMEOUT)
                    .setParam("http.connection-manager.timeout", REQUEST_TIMEOUT)
            );

    private final Logger log = LoggerFactory.getLogger(WrapInfor.class);

    @Property("iris.host")
    private String irisHost;


    public IrisClient() {
        PropertyLoader.newInstance().populate(this);
    }

    private RequestSpecification baseRequestSpec() {
        return RestAssured
                .given()
                .urlEncodingEnabled(false)
                .filter(new AllureRestAssured())
                .baseUri(irisHost)
                .header(TVM.HEADER, TVM.INSTANCE.getServiceTicket(TVM.IRIS));

    }


    public ValidatableResponse getTrustworthyInfo(long partner_id ,
                                                 String partner_sku) {
        return Retrier.clientRetry(() -> baseRequestSpec()
               .contentType(ContentType.JSON)
                        .body(Map.of("items", List.of(
                        Map.of(
                        "partner_id", partner_id,
                        "partner_sku", partner_sku))))
                .when()
                .post("/servicebus/wms/getTrustworthyInfo")
                .then()
                .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse measurementAudit(long partner_id ,
                                                  String partner_sku) {
        return Retrier.clientRetry(() -> baseRequestSpec()
                .queryParam("partner_id", partner_id)
                .queryParam("partner_sku", partner_sku)
                        .when()
                        .get("/measurement-audit")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
        );
    }
}
