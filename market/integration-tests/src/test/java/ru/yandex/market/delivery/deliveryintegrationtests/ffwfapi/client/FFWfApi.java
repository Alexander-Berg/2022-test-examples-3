package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.client;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.FileUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;

import static org.hamcrest.Matchers.is;

@Resource.Classpath("ffwfapi/ffwfapi.properties")
public class FFWfApi {
    private final Logger log = LoggerFactory.getLogger(FFWfApi.class);

    private static final int FFWF_RETRIES = 10;
    private static final int FFWF_TIMEOUT = 1;
    private static final TimeUnit FFWF_TIME_UNIT = TimeUnit.MINUTES;

    @Property("ffwfapi.host")
    private String host;

    public FFWfApi() {
        PropertyLoader.newInstance().populate(this);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private RequestSpecification baseRequestSpec() {
        return RestAssured
                .given()
                .filter(new AllureRestAssured())
                .baseUri(host);
    }

    public ValidatableResponse uploadSupply(String supplyDate) {
        return uploadSupply(supplyDate, "ffwfapi/requests/inbound.json");
    }

    public ValidatableResponse uploadSupply(String supplyDate, String requestFilePath) {
        log.info("Uploading supply request...");

        String reqString = FileUtil.bodyStringFromFile(requestFilePath, supplyDate);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(reqString)
                        .when()
                        .post("/upload-request/supply")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse uploadSupply(String supplyDate, String requestFilePath, String calendaringMode) {
        log.info("Uploading supply request...");

        String reqString = FileUtil.bodyStringFromFile(requestFilePath, supplyDate, calendaringMode);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(reqString)
                        .when()
                        .post("/upload-request/supply")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse uploadShadowSupply(String supplyDate, String requestFilePath, String calendaringMode) {
        log.info("Uploading shadow supply request...");

        String reqString = FileUtil.bodyStringFromFile(requestFilePath, supplyDate, calendaringMode);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(reqString)
                        .when()
                        .post("/upload-request/shadow-supply")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse uploadXdocSupply(long warehouseId, long xDocPartnerId, String supplyDate) {
        log.info("Uploading x-doc supply request...");

        String reqString = FileUtil.bodyStringFromFile("ffwfapi/requests/xdocInbound.json", warehouseId, xDocPartnerId, supplyDate);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(reqString)
                        .when()
                        .post("/upload-request/supply")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse uploadCrossdockSupply(String supplyDate) {
        log.info("Uploading supply request...");

        String reqString = FileUtil.bodyStringFromFile("ffwfapi/requests/crossdockinbound.json", supplyDate);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(reqString)
                        .when()
                        .post("/upload-request/supply")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse uploadWithdraw(String withdrawDate) {
        log.info("Uploading withdraw request...");

        File file = FileUtil.getFile("ffwfapi/requests/outbound.xlsx");

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .multiPart("supplierId", 10264169)
                        .multiPart("file", file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .multiPart("date", withdrawDate)
                        .multiPart("serviceId", 171)
                        .multiPart("consignee", "Иван Петров")
                        .multiPart("contactPersonName", "Иван")
                        .multiPart("contactPersonSurname", "Петров")
                        .multiPart("phoneNumber", "9998887766")
                        .multiPart("stock", 0)
                        .when()
                        .post("/upload-request-file/withdraw")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse uploadShadowWithdraw(String withdrawDate) {
        log.info("Uploading shadow withdraw request...");

        File file = FileUtil.getFile("ffwfapi/requests/outbound.xlsx");

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .multiPart("calendaringMode", "REQUIRED")
                        .multiPart("file", file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .multiPart("date", withdrawDate)
                        .multiPart("serviceId", 171)
                        .multiPart("supplierId", 10264169)
                        .multiPart("stock", 0)
                        .when()
                        .post("/upload-request-file/shadow-withdraw")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse uploadShadowSupply() {
        log.info("Uploading shadow supply request...");

        File file = FileUtil.getFile("ffwfapi/requests/inbound.xlsx");

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .multiPart("supplierId", 10427354)
                        .multiPart("file", file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .when()
                        .post("/upload-request-file/shadow-supply")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse itemsUpdate(Long requestId, HashMap<String, Object> updateRequestBody) {
        log.info("Uploading update items {}", requestId);

        String reqPath = String.format("/requests/%s/items-update", requestId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(updateRequestBody)
                        .when()
                        .put(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse createTransfer(
            long inboundId,
            String article,
            int count,
            long serviceId,
            int stockTypeFrom,
            int stockTypeTo,
            long supplierId
    ) {
        log.info("Creating transfer...");

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(new HashMap<String, Object>() {{
                                  put("externalOperationId", UniqueId.getString());
                                  put("inboundId", inboundId);
                                  put("items", Collections.singletonList(new HashMap<String, Object>() {{
                                      put("article", article);
                                      put("count", count);
                                  }}));
                                  put("serviceId", serviceId);
                                  put("stockTypeFrom", stockTypeFrom);
                                  put("stockTypeTo", stockTypeTo);
                                  put("supplierId", supplierId);
                              }}
                        )
                        .when()
                        .post("/transfer/create")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse cancelRequest(Long requestId) {
        log.info("Cancelling request {}", requestId);

        String reqPath = String.format("/requests/%s/cancel", requestId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .when()
                        .put(reqPath)
                        .then()
                        .statusCode(is(HttpStatus.SC_OK)),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse getRequest(Long requestId) {
        log.info("Getting data for request {}", requestId);

        String reqPath = String.format("/requests/%s", requestId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .when()
                        .get(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse confirmRequest(Long requestId) {
        log.info("Confirming request", requestId);

        String reqPath = String.format("/requests/%s/confirm", requestId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .when()
                        .put(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse getTransfer(Long transferId) {
        log.info("Getting data for transfer {}", transferId);

        String reqPath = String.format("/transfer/%s", transferId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .when()
                        .get(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse getRequestDocuments(Long requestId) {
        log.info("Getting data for request {}", requestId);

        String reqPath = String.format("/requests/%s/documents", requestId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .when()
                        .get(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse getCrossdockItemsByOrder(Long orderId) {
        log.info("Getting data for orderId {}", orderId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .when()
                        .queryParam("orderId", orderId)
                        .get("/crossdock/items")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse getCrossdockItemsByDate(long shopId, String draftDate) {
        log.info("Getting data for shipmentDate {}", draftDate);

        String draftFinalizationDateFrom = draftDate + "T00:00";
        String draftFinalizationDateTo = draftDate + "T23:59";

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .when()
                        .queryParam("supplierId", shopId)
                        .queryParam("draftFinalizationDateFrom", draftFinalizationDateFrom)
                        .queryParam("draftFinalizationDateTo", draftFinalizationDateTo)
                        .get("/crossdock/items")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse getFreeTimeSlots(Long requestId) {
        log.info("Getting free time slots for request {}", requestId);

        String reqPath = String.format("/requests/%s/getFreeTimeSlots", requestId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .when()
                        .get(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse getFreeTimeSlotsByService(Long requestId) {
        log.info("Getting free time slots by service for request {}", requestId);

        String reqPath = String.format("/requests/%s/free-time-slots-by-service", requestId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .when()
                        .get(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse selectSlot(
            Long requestId,
            String date,
            String from,
            String to
    ) {
        log.info("select slot...");

        String reqPath = String.format("/requests/%s/selectSlot", requestId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(new HashMap<String, Object>() {{
                                  put("date", date);
                                  put("from", from);
                                  put("to", to);
                              }}
                        )
                        .when()
                        .post(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse commitShadowSupply(
            Long requestId,
            String date,
            String from,
            String to,
            String serviceId
    ) {
        log.info("commit shadow supply...");

        String reqPath = String.format("/requests/%s/commit-shadow-supply", requestId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(new HashMap<String, Object>() {{
                                  put("requestedDate", date);
                                  put("from", from);
                                  put("to", to);
                                  put("serviceId", serviceId);
                              }}
                        )
                        .when()
                        .post(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse createSupplyBbasedOnShadow(Long requestId) {
        log.info("creating supply based on shadow {}", requestId);

        String reqPath = String.format("/requests/%s/create-supply-based-on-shadow-request", requestId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .when()
                        .post(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }

    public ValidatableResponse createWithdrawBasedOnShadow(
            Long requestId,
            String date,
            String from,
            String to
    ) {
        log.info("creating withdraw based on shadow {}", requestId);

        String reqPath = String.format("/requests/%s/commit-shadow-withdraw", requestId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(new HashMap<String, Object>() {{
                            put("consignee", "Иван Петров");
                            put("requestedDate", date);
                            put("from", from);
                            put("to", to);
                        }}
                        )
                        .when()
                        .post(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                FFWF_RETRIES,
                FFWF_TIMEOUT,
                FFWF_TIME_UNIT
        );
    }
}
