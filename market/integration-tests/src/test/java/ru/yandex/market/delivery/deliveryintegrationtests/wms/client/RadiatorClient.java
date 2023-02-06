package ru.yandex.market.delivery.deliveryintegrationtests.wms.client;

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

import static org.hamcrest.Matchers.is;

@Resource.Classpath({"wms/radiator.properties", "wms/wrapinfor.properties"})
public class RadiatorClient {

    private static final int REQUEST_TIMEOUT = 15000;

    private static final RestAssuredConfig CONFIG = RestAssured.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", REQUEST_TIMEOUT)
                    .setParam("http.socket.timeout", REQUEST_TIMEOUT)
                    .setParam("http.connection-manager.timeout", REQUEST_TIMEOUT)
            );

    private final Logger log = LoggerFactory.getLogger(RadiatorClient.class);

    @Property("wms-radiator.host")
    private String host;

    @Property("wrapinfor.token")
    private String token;

    public RadiatorClient() {
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
                .header("Content-Type", "text/xml")
                .body(reqBody)
                .when()
                .post(path)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.SC_OK)
                .body("root.requestState.isError", is("false"));
    }

    public ValidatableResponse getReferenceItems(long vendorId, String article) {
        log.info("Calling Wms-Radiator getReferenceItems");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getReferenceItems.xml",
                hash,
                vendorId,
                article,
                token
        );

        final String prefix = "root.response.itemReferences.itemReference.unitId.";
        return baseRequest(reqString)
                .body(prefix + "vendorId", is(String.valueOf(vendorId)))
                .body(prefix + "article", is(article));
    }

    public ValidatableResponse getExpirationItems(long vendorId, String article) {
        log.info("Calling Wms-Radiator getStocks");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getExpirationItems.xml",
                hash,
                vendorId,
                article,
                token
        );

        return baseRequest(reqString)
                .body("root.response.itemExpirationList.itemExpiration.unitId.vendorId",
                        is(String.valueOf(vendorId)))
                .body("root.response.itemExpirationList.itemExpiration.unitId.article",
                        is(String.valueOf(article)));
    }

    public ValidatableResponse getStocks(long vendorId, String article) {
        log.info("Calling Wms-Radiator getStocks");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getStocks.xml",
                hash,
                vendorId,
                article,
                token
        );

        return baseRequest(reqString)
                .body("root.response.itemStocksList.itemStocks.unitId.vendorId",
                        is(String.valueOf(vendorId)))
                .body("root.response.itemStocksList.itemStocks.unitId.article",
                        is(String.valueOf(article)));
    }

    public void dropCache() {
        RestAssured
                .given()
                .config(CONFIG)
                .filter(new AllureRestAssured())
                .baseUri(host)
                .when()
                .post("/stocks/147/refresh")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.SC_OK);

        RestAssured
                .given()
                .config(CONFIG)
                .filter(new AllureRestAssured())
                .baseUri(host)
                .when()
                .post("/stocks/172/refresh")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.SC_OK);
    }

    public void refreshReferenceCache() {
        RestAssured
                .given()
                .config(CONFIG)
                .filter(new AllureRestAssured())
                .baseUri(host)
                .when()
                .header("token", token)
                .post("/reference-items/refresh")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.SC_OK);
    }

}
