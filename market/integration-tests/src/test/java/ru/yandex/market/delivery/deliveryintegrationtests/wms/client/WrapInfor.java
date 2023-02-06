package ru.yandex.market.delivery.deliveryintegrationtests.wms.client;

import java.util.List;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.FileUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Outbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.StockType;

import static org.hamcrest.Matchers.is;

@Resource.Classpath({"wms/wrapinfor.properties", "wms/infor.properties"})
public class WrapInfor {

    private static final int REQUEST_TIMEOUT = 15000;

    private static final RestAssuredConfig CONFIG = RestAssured.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", REQUEST_TIMEOUT)
                    .setParam("http.socket.timeout", REQUEST_TIMEOUT)
                    .setParam("http.connection-manager.timeout", REQUEST_TIMEOUT)
            );

    private final Logger log = LoggerFactory.getLogger(WrapInfor.class);

    @Property("wrapinfor.host")
    private String host;

    @Property("infor.host")
    private String inforHost;

    @Property("wrapinfor.token")
    private String token;

    public WrapInfor() {
        PropertyLoader.newInstance().populate(this);
    }

    private ValidatableResponse baseRequest(String reqBody) {
        return baseRequest(reqBody, "/query-gateway");
    }

    private ValidatableResponse baseRequest(String reqBody, String path) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssured
                .given()
                .config(CONFIG)
                .filter(new AllureRestAssured())
                .baseUri(host)
                .contentType(ContentType.XML)
                .header("Content-Type","text/xml")
                .body(reqBody)
                .when()
                .post(path)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("root.requestState.isError",
                        is("false"));
    }

    public ValidatableResponse ping() {
        log.info("Calling WrapInfor /ping");

        return RestAssured
                .given()
                .config(CONFIG)
                .filter(new AllureRestAssured())
                .baseUri(host)
                .when()
                .get("/ping")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse getOutboundStatus(Outbound outbound) {
        return getOutboundStatus(outbound.getYandexId(), outbound.getFulfillmentId());
    }

    public ValidatableResponse getOutboundStatus(long yandexId, String fulfillmentId) {
        log.info("Calling WrapInfor getOutboundsStatus");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getOutboundsStatus.xml",
                hash,
                yandexId,
                fulfillmentId,
                token
        );

        return baseRequest(reqString)
                .body("root.response.outboundsStatus.outboundStatus.outboundId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.outboundsStatus.outboundStatus.outboundId.partnerId",
                        is(fulfillmentId))
                .body("root.response.outboundsStatus.outboundStatus.outboundId.fulfillmentId",
                        is(fulfillmentId));
    }

    public ValidatableResponse getOutboundHistory(Outbound outbound) {
        return getOutboundHistory(outbound.getYandexId(), outbound.getFulfillmentId());
    }

    public ValidatableResponse getOutboundHistory(long yandexId, String fulfillmentId) {
        log.info("Calling WrapInfor getOutboundHistory");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getOutboundHistory.xml",
                hash,
                yandexId,
                fulfillmentId,
                token
        );

        return baseRequest(reqString)
                .body("root.response.outboundStatusHistory.outboundId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.outboundStatusHistory.outboundId.partnerId",
                        is(fulfillmentId))
                .body("root.response.outboundStatusHistory.outboundId.fulfillmentId",
                        is(fulfillmentId));
    }

}
