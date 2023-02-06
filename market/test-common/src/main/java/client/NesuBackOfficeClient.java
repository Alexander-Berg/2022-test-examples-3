package client;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import api.NesuBackofficeApi;
import dto.requests.nesu.Dimensions;
import dto.requests.nesu.FilterDORequest;
import dto.requests.nesu.From;
import dto.requests.nesu.To;
import dto.responses.nesu.DeliveryOptionsItem;
import dto.responses.nesu.ShipmentLogisticPoint;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import toolkit.FileUtil;
import toolkit.Retrier;

import static toolkit.Retrofits.RETROFIT;

@Resource.Classpath("nesu/nesu.properties")
@Slf4j
public class NesuBackOfficeClient {
    private static final String QUERY_PARAMS = "?shopId={shopId}&userId={userId}";

    private final NesuBackofficeApi nesuBackofficeApi;

    @Property("nesu.host")
    private String host;

    public NesuBackOfficeClient() {
        PropertyLoader.newInstance().populate(this);
        nesuBackofficeApi = RETROFIT.getRetrofit(host).create(NesuBackofficeApi.class);
    }

    public ValidatableResponse ping() {
        log.info("Calling nesu ping...");

        return Retrier.clientRetry(() -> baseRequestSpec()
            .when()
            .get("/ping")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse createWithdrawOrders(
        long senderId,
        long shopId,
        long userId,
        LocalDate todayDay,
        LocalDate deliveryDate
    ) {
        log.info("Calling nesu orders...");

        String jsonString = FileUtil.bodyStringFromFile("nesu/requests/createWithdrawStrizhOrder.json")
            .replaceAll("TODAYDAY", DateTimeFormatter.ISO_DATE.format(todayDay))
            .replaceAll("DELIVERYDATE", DateTimeFormatter.ISO_DATE.format(deliveryDate));

        return Retrier.clientRetry(() -> baseRequestSpec()
            .when()
            .queryParam("senderId", senderId)
            .queryParam("shopId", shopId)
            .queryParam("userId", userId)
            .body(jsonString)
            .post("back-office/orders")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse createSelfExportOrders(
        long senderId,
        long shopId,
        long userId,
        LocalDate todayDay,
        LocalDate deliveryDate,
        long warehouseId
    ) {
        log.info("Calling nesu orders...");

        String jsonString = FileUtil.bodyStringFromFile("nesu/requests/createImportStrizhOrder.json")
            .replaceAll("TODAYDAY", DateTimeFormatter.ISO_DATE.format(todayDay))
            .replaceAll("DELIVERYDATE", DateTimeFormatter.ISO_DATE.format(deliveryDate))
            .replaceAll("WAREHOUSE_ID_PLACEHOLDER", "" + warehouseId);

        return Retrier.clientRetry(() -> baseRequestSpec()
            .when()
            .queryParam("senderId", senderId)
            .queryParam("shopId", shopId)
            .queryParam("userId", userId)
            .body(jsonString)
            .post("back-office/orders")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public void commitOrder(long shopId, long userId, long orderId) {
        log.info("Calling nesu orders/submit...");

        Retrier.clientRetry(() -> baseRequestSpec()
            .queryParam("shopId", shopId)
            .queryParam("userId", userId)
            .body(String.format("{\"orderIds\": [%d]}", orderId))
            .when().post("/back-office/orders/submit")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse getOrder(long shopId, long userId, long orderId) {
        log.info("Calling nesu find orders...");
        return Retrier.clientRetry(() -> baseRequestSpec()
            .pathParam("orderIds", orderId)
            .queryParam("shopId", shopId)
            .queryParam("userId", userId)
            .when().get("/back-office/orders/{orderIds}")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse createWarehouse(long shopId, long userId) {
        log.info("Calling nesu warehouse...");

        String jsonString = FileUtil.bodyStringFromFile("nesu/requests/createWarehouse.json");

        return Retrier.clientRetry(() -> baseRequestSpec()
            .when()
            .queryParam("shopId", shopId)
            .queryParam("userId", userId)
            .body(jsonString)
            .post("back-office/warehouses")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse createNewWithdrawShipment(long shopId, long userId, LocalDate date, long warehouseId) {
        log.info("Calling nesu withdraw shipment...");

        String body = FileUtil.bodyStringFromFile("nesu/requests/createWithdrawShipment.json")
            .replaceAll("DATE_PLACEHOLDER", DateTimeFormatter.ISO_DATE.format(date))
            .replaceAll("WAREHOUSE_ID_PLACEHOLDER", "" + warehouseId);

        return Retrier.clientRetry(() -> baseRequestSpec()
            .when()
            .queryParam("shopId", shopId)
            .queryParam("userId", userId)
            .body(body)
            .post("back-office/shipments")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse createNewImportCourierShipment(
        long shopId,
        long userId,
        LocalDate date,
        long warehouseId
    ) {
        log.info("Calling nesu import courier shipment...");

        String body = FileUtil.bodyStringFromFile("nesu/requests/createImportCourierShipment.json")
            .replaceAll("DATE_PLACEHOLDER", DateTimeFormatter.ISO_DATE.format(date))
            .replaceAll("WAREHOUSE_ID_PLACEHOLDER", "" + warehouseId);

        return Retrier.clientRetry(() -> baseRequestSpec()
            .when()
            .queryParam("shopId", shopId)
            .queryParam("userId", userId)
            .body(body)
            .post("back-office/shipments")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse createNewImportCourierWithCarShipment(
        long shopId,
        long userId,
        LocalDate date,
        long warehouseId
    ) {
        log.info("Calling nesu import courier with car shipment...");

        String body = FileUtil.bodyStringFromFile("nesu/requests/createImportCarShipment.json")
            .replaceAll("DATE_PLACEHOLDER", DateTimeFormatter.ISO_DATE.format(date))
            .replaceAll("WAREHOUSE_ID_PLACEHOLDER", "" + warehouseId);

        return Retrier.clientRetry(() -> baseRequestSpec()
            .when()
            .queryParam("shopId", shopId)
            .queryParam("userId", userId)
            .body(body)
            .post("back-office/shipments")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse createShop(long id) {
        log.info("Calling nesu shop...");

        Map<String, Object> body = new HashMap<>();
        body.put("balanceClientId", 100);
        body.put("balanceContractId", 100);
        body.put("balancePersonId", 100);
        body.put("id", id);
        body.put("marketId", 0);
        body.put("name", "test");
        body.put("role", "DAAS");
        body.put("taxSystem", "OSN");

        return Retrier.clientRetry(() -> baseRequestSpec()
            .when()
            .body(body)
            .post("internal/shops/register")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @SneakyThrows
    public List<DeliveryOptionsItem> getOptions(
        Long senderId,
        Long shopId,
        Long userId,
        String locationFrom,
        String locationTo,
        Long length, Long height,
        Long width,
        Long weight
    ) {
        Response<List<DeliveryOptionsItem>> execute = nesuBackofficeApi.getOptions(
            TVM.INSTANCE.getServiceTicket(TVM.NESU),
            senderId,
            shopId,
            userId,
            FilterDORequest.builder()
                .from(new From(locationFrom))
                .to(new To(locationTo))
                .dimensions(new Dimensions(length, width, height, weight))
        ).execute();

        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос поиска вариантов доставки ");
        Assertions.assertNotNull(execute.body(), "Пустое тело при поиске вариантов доставки");

        return execute.body();
    }

    public ValidatableResponse findCourierShipment(long shopId, long userId, LocalDate futureDay, long warehouseId) {
        log.info("Calling find courier shipment...");

        String nextDate = DateTimeFormatter.ISO_DATE.format(futureDay);

        Map<String, Object> body = new HashMap<>();
        body.put("fromDate", nextDate);
        body.put("toDate", nextDate);
        body.put("warehouseTo", 10000001603L);
        body.put("warehouseFrom", warehouseId);

        return Retrier.clientRetry(() -> baseRequestSpec()
            .when()
            .queryParam("shopId", shopId)
            .queryParam("userId", userId)
            .body(body)
            .put("back-office/shipments/search")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse findWithdrawShipment(long shopId, long userId, LocalDate futureDay, long warehouseId) {
        log.info("Calling find withdraw shipment...");

        String nextDate = DateTimeFormatter.ISO_DATE.format(futureDay);

        Map<String, Object> body = new HashMap<>();
        body.put("fromDate", nextDate);
        body.put("toDate", nextDate);
        body.put("warehouseFrom", warehouseId);

        return Retrier.clientRetry(() -> baseRequestSpec()
            .when()
            .queryParam("shopId", shopId)
            .queryParam("userId", userId)
            .body(body)
            .put("back-office/shipments/search")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse cancelShipmentById(long shopId, long userId, long shipmentId) {
        log.info("Calling nesu cancelled shipment...");
        return Retrier.clientRetry(() -> baseRequestSpec()
            .pathParam("shipmentId", shipmentId)
            .pathParam("shopId", shopId)
            .pathParam("userId", userId)
            .when().delete("back-office/shipments/{shipmentId}" + QUERY_PARAMS)
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse deleteWarehouseById(long shopId, long userId, long warehouseId) {
        log.info("Calling nesu deactivate warehouse...");
        return Retrier.clientRetry(() -> baseRequestSpec()
            .pathParam("warehouseId", warehouseId)
            .pathParam("shopId", shopId)
            .pathParam("userId", userId)
            .when().delete("/back-office/warehouses/{warehouseId}" + QUERY_PARAMS)
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse confirmShipmentById(long createdSelfExportShipmentId, long shopId, long userId) {
        log.info("Calling nesu confirmed shipment...");
        return Retrier.clientRetry(() -> baseRequestSpec()
            .pathParam("shipmentId", createdSelfExportShipmentId)
            .pathParam("shopId", shopId)
            .pathParam("userId", userId)
            .when().put("back-office/shipments/{shipmentId}/confirm" + QUERY_PARAMS)
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    public ValidatableResponse runYtUpdateDropoffDayoffJob() {
        log.info("Calling nesu dayoffs update from YT...");

        return runJob("nesu/requests/runYtUpdateDropoffDayoffJob.json");
    }

    public ValidatableResponse runYtUpdateDropoffSegmentsInfoJob() {
        log.info("Calling nesu dropoff segments info update from YT...");

        return runJob("nesu/requests/runYtUpdateDropoffSegmentsInfoJob.json");
    }

    private ValidatableResponse runJob(String pathToJsonJobFile) {
        String jsonString = FileUtil.bodyStringFromFile(pathToJsonJobFile);

        return Retrier.clientRetry(() -> baseRequestSpec()
            .when()
            .body(jsonString)
            .post("tms/jobs/run")
            .then()
            .statusCode(HttpStatus.SC_OK)
        );
    }

    @SneakyThrows
    public List<ShipmentLogisticPoint> getAvailableShipmentOptions(
        long partnerId,
        long shopId,
        long userId,
        @Nullable Boolean showReturnEnabled
    ) {
        log.info("Calling nesu get available shipment options...");

        Response<List<ShipmentLogisticPoint>> execute = nesuBackofficeApi.getAvailableShipmentOptions(
            TVM.INSTANCE.getServiceTicket(TVM.NESU),
            partnerId,
            shopId,
            userId,
            true,
            showReturnEnabled
        ).execute();

        Assertions.assertTrue(execute.isSuccessful(), "Неуспешный запрос получения доступных вариантов подключения");
        Assertions.assertNotNull(execute.body(), "Пустое тело при поиске доступных вариантов подключения");

        return execute.body();
    }

    private RequestSpecification baseRequestSpec() {
        return RestAssured
            .given()
            .relaxedHTTPSValidation()
            .filter(new AllureRestAssured())
            .baseUri(host)
            .header("X-Ya-Service-Ticket", TVM.INSTANCE.getServiceTicket(TVM.NESU))
            .contentType(ContentType.JSON);
    }
}
