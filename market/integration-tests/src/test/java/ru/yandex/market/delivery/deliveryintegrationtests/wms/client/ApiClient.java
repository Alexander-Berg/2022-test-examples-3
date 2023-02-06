package ru.yandex.market.delivery.deliveryintegrationtests.wms.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.SchedulerJob;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveId;

import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;

@Resource.Classpath({"wms/infor.properties"})
public class ApiClient {

    private static final int REQUEST_TIMEOUT = 15000;

    private static final RestAssuredConfig CONFIG = RestAssured.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", REQUEST_TIMEOUT)
                    .setParam("http.socket.timeout", REQUEST_TIMEOUT)
                    .setParam("http.connection-manager.timeout", REQUEST_TIMEOUT)
            );

    private final Logger log = LoggerFactory.getLogger(WrapInfor.class);

    @Property("infor.host")
    private String inforHost;

    @Property("infor.token")
    private String inforToken;

    @Property("infor.username")
    private String inforUsername;

    @Property("infor.password")
    private String inforPassword;

    public ApiClient() {
        PropertyLoader.newInstance().populate(this);
    }

    private ValidatableResponse baseRequest(String reqBody, String path) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssured
                .given()
                .config(CONFIG.encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .filter(new AllureRestAssured())
                .baseUri(inforHost)
                .contentType(ContentType.XML)
                .header("Content-Type", "text/xml")
                .body(reqBody)
                .when()
                .post(path)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("root.requestState.isError",
                        is("false"));
    }

    public ValidatableResponse getOrderStatus(Order order) {
        return getOrderStatus(order.getYandexId(), order.getFulfillmentId());
    }

    public ValidatableResponse getOrderStatus(long yandexId, String fulfillmentId) {
        log.info("Calling Api getOrdersStatus");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getOrdersStatus.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/172/order-status")
                .body("root.response.orderStatusHistories.orderStatusHistory.orderId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.orderStatusHistories.orderStatusHistory.orderId.partnerId",
                        is(fulfillmentId))
                .body("root.response.orderStatusHistories.orderStatusHistory.orderId.fulfillmentId",
                        is(fulfillmentId));
    }

    public ValidatableResponse getOrderHistory(Order order) {
        return getOrderHistory(order.getYandexId(), order.getFulfillmentId());
    }

    public ValidatableResponse getOrderHistory(long yandexId, String fulfillmentId) {
        log.info("Calling WrapInfor getOrderHistory");

        String hash = UniqueId.getStringUUID();

        String reqString = FileUtil.bodyStringFromFile("wms/wrapRequests/getOrderHistory.xml",
                hash,
                yandexId,
                fulfillmentId,
                inforToken
        );

        return baseRequest(reqString, "/api/172/order-status-history")
                .body("root.response.orderStatusHistory.orderId.yandexId",
                        is(String.valueOf(yandexId)))
                .body("root.response.orderStatusHistory.orderId.partnerId",
                        is(fulfillmentId))
                .body("root.response.orderStatusHistory.orderId.fulfillmentId",
                        is(fulfillmentId));
    }

    public ValidatableResponse getTrustworthyInfo(long partner_id ,
                                                  String partner_sku) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssured
                .given()
                .config(CONFIG.encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .filter(new AllureRestAssured())
                .baseUri(inforHost)
                .contentType(ContentType.JSON)
                .body(Map.of("manufacturerSkus", List.of(
                        Map.of(
                                "storerKey", partner_id,
                                "manufacturerSku", partner_sku))))
                .when()
                .post("/servicebus/wms/getTrustworthyInfo")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse createWave(Order order) {
        log.info("Creating wave via post-request: "+ "[\""+order.getFulfillmentId()+"\"]");
        return RestAssured
                .given()
                .header("Username",inforUsername)
                .header("Password",inforPassword)
                .config(CONFIG.encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .filter(new AllureRestAssured())
                .baseUri(inforHost)
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/JSON")
                .body("{\"orderKeys\": [\""+order.getFulfillmentId()+"\"], \"buildingId\": null}")
                .when()
                .post("/autostart/debug/startOrders")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse addSortingStationToAutostart(String sortingStation) {
        log.info("Adding sortingstation: "+ sortingStation);

        List<Map<String, String>> payload = Collections.singletonList(Map.of(
                "station", sortingStation,
                "mode", "ORDERS"
        ));
        return RestAssured
                .given()
                .header("Username",inforUsername)
                .header("Password",inforPassword)
                .config(CONFIG.encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .filter(new AllureRestAssured())
                .baseUri(inforHost)
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/JSON")
                .body(payload)
                .when()
                .post("autostart/settings/stations")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse removeSortingStationFromAutostart(String sortingStation) {
        log.info("Removing sortingstation: "+ sortingStation);

        List<Map<String, String>> payload = Collections.singletonList(Map.of(
                "station", sortingStation,
                "mode", "OFF"
        ));
        return RestAssured
                .given()
                .header("Username",inforUsername)
                .header("Password",inforPassword)
                .config(CONFIG.encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .filter(new AllureRestAssured())
                .baseUri(inforHost)
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/JSON")
                .body(payload)
                .when()
                .post("autostart/settings/stations")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse getSortingStationByWaveId(WaveId waveId) {
        log.info("Getting sortingstation by waveid: "+ waveId);
        return RestAssured
                .given()
                .header("Username",inforUsername)
                .header("Password",inforPassword)
                .config(CONFIG.encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .filter(new AllureRestAssured())
                .baseUri(inforHost)
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/json, text/plain")
                .when()
                .get("autostart/waves?filter=waveId==\""+waveId.getId()+"\"")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse refreshTouchedPallets() {
        log.info("Refreshing touched pallets");
        return RestAssured
                .given()
                .header("Username",inforUsername)
                .header("Password",inforPassword)
                .config(CONFIG.encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .filter(new AllureRestAssured())
                .baseUri(inforHost)
                .when()
                .post("inventorization/support/touched-stored-pallet/update?fromArchive=false")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse executeSchedulerJob(SchedulerJob jobType) {
        log.info("Starting Scheduler job: {}", jobType.getValue());
        return RestAssured.
                given()
                .header("Username", inforUsername)
                .header("Password", inforPassword)
                .config(CONFIG.encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .filter(new AllureRestAssured())
                .baseUri(inforHost)
                .when()
                .post("scheduler2/manage/job-group/" + jobType.getGroup() + "/jobs/" + jobType.getValue() + "/execute")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

}
