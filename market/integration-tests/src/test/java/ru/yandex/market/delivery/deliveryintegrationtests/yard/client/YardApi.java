package ru.yandex.market.delivery.deliveryintegrationtests.yard.client;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import ru.yandex.market.delivery.deliveryintegrationtests.yard.dto.YardClientEventDto;

@Resource.Classpath("yard/yard-api.properties")
public class YardApi {

    private final Logger log = LoggerFactory.getLogger(YardApi.class);

    private static final int YARD_RETRIES = 10;
    private static final int YARD_TIMEOUT = 1;
    private static final TimeUnit YARD_TIME_UNIT = TimeUnit.MINUTES;
    private final Map<GraphTemplate, String> graphMap = new HashMap<>();

    @Property("yard-api.host")
    private String host;

    private YardApi() {
        PropertyLoader.newInstance().populate(this);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        graphMap.put(GraphTemplate.COURIER_GRAPH, FileUtil.readFile("yard/graphs/courier-graph.json"));
        graphMap.put(GraphTemplate.FF_GRAPH, FileUtil.readFile("yard/graphs/ff-graph.json"));
    }

    private static class YardApiHolder {
        private static final YardApi INSTANCE = new YardApi();
    }

    public static YardApi getInstance() {
        return YardApiHolder.INSTANCE;
    }

    private RequestSpecification baseRequestSpec() {
        return RestAssured
                .given()
                .filter(new AllureRestAssured())
                .baseUri(host);
    }

    private RequestSpecification simpleRequestSpec() {
        return RestAssured
                .given()
                .baseUri(host);
    }

    public ValidatableResponse registerClient(
            String clientId,
            Long serviceId,
            String name,
            String phone,
            ZonedDateTime arrivalPlannedDate,
            ZonedDateTime arrivalPlannedDateTo
    ) {
        log.info("Register client {}...", clientId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(new HashMap<String, Object>() {{
                                  put("clientId", clientId);
                                  put("serviceId", serviceId);
                                  put("name", name);
                                  put("phone", phone);
                                  put("arrivalPlannedDate", arrivalPlannedDate);
                                  put("arrivalPlannedDateTo", arrivalPlannedDateTo);
                              }}
                        )
                        .when()
                        .post("/client/register")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                YARD_RETRIES,
                YARD_TIMEOUT,
                YARD_TIME_UNIT
        );
    }

    public ValidatableResponse pushEvent(Long serviceId, List<YardClientEventDto> events) {

        log.info("Push events {} for service {} ...", events, serviceId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(new HashMap<String, Object>() {{
                                  put("serviceId", serviceId);
                                  put("events", events);
                              }}
                        )
                        .when()
                        .post("/event/push")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                YARD_RETRIES,
                YARD_TIMEOUT,
                YARD_TIME_UNIT
        );
    }

    public ValidatableResponse pushEventWithoutReport(Long serviceId, List<YardClientEventDto> events) {

        log.info("Push events {} for service {} ...", events, serviceId);

        return Retrier.clientRetry(() -> simpleRequestSpec()
                        .contentType(ContentType.JSON)
                        .body(new HashMap<String, Object>() {{
                                  put("serviceId", serviceId);
                                  put("events", events);
                              }}
                        )
                        .when()
                        .post("/event/push")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                YARD_RETRIES,
                YARD_TIMEOUT,
                YARD_TIME_UNIT
        );
    }

    public ValidatableResponse getClientInfo(Long serviceId, String clientId) {

        log.info("Get client info {}...", clientId);

        String reqPath = String.format("/client?clientId=%s&serviceId=%s", clientId, serviceId);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .when()
                        .get(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                YARD_RETRIES,
                YARD_TIMEOUT,
                YARD_TIME_UNIT
        );
    }

    public void createGraph(GraphTemplate graphTemplate,
                            Long graphId) {
        log.info("create graph " + graphId);
        String graph = graphMap.get(graphTemplate).replace("GRAPH_ID", graphId.toString());

        baseRequestSpec()
                .contentType(ContentType.JSON)
                .body(graph)
                .when()
                .post("/graph/create")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public void deleteGraph(Long graphId) {
        log.info("delete graph " + graphId);
        baseRequestSpec()
                .contentType(ContentType.JSON)
                .when()
                .delete("/graph/service/" + graphId)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse getService(UUID serviceUUID) {
        return Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .cookie("yandex_login", "test_login")
                        .when()
                        .get("/services/uuid/" + serviceUUID)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                Retrier.RETRIES_SMALL,
                5,
                TimeUnit.SECONDS
        );
    }

    public ValidatableResponse getServicesUUIDs() {
        ValidatableResponse validatableResponse = Retrier.clientRetry(() -> baseRequestSpec()
                        .contentType(ContentType.JSON)
                        .cookie("yandex_login", "test_login")
                        .when()
                        .get("/services_uuid/")
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                Retrier.RETRIES_SMALL,
                5,
                TimeUnit.SECONDS
        );
        log.error(String.valueOf(validatableResponse.extract().jsonPath()));

        return validatableResponse;
    }

    public ValidatableResponse driverRegisterInReqType(UUID capacityUUID, String licencePlateNumber,
                                                       String requestType) {
        return baseRequestSpec()
                .contentType(ContentType.JSON)
                .body(new HashMap<String, Object>() {{
                          put("requestType", requestType);
                          put("clientData", new HashMap<String, Object>() {{
                              put("licencePlateNumber", licencePlateNumber);
                              put("driverPhoneNumber", "+71111111111");
                              put("requiredGateType", "+WITH_RAMP");
                              put("takeAwayReturns", false);
                          }});
                      }}
                )
                .when()
                .post("/site/uuid/" + capacityUUID + "/get-in-line")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse operatorCommand(String login, UUID capacityUnitUUID, String command) {
        return baseRequestSpec()
                .contentType(ContentType.JSON)
                .cookie("yandex_login", login)
                .when()
                .post("/window/uuid/" + capacityUnitUUID + "/" + command)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse waitForClient(String login, UUID capacityUnitUUID) {

        return baseRequestSpec()
                .contentType(ContentType.JSON)
                .cookie("yandex_login", login)
                .when()
                .get("/window/uuid/" + capacityUnitUUID + "/queue")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse windowPush(UUID graphId, UUID capacityUnitUUID, Long assignedClientId, String eventType,
                                          String login) {
        return baseRequestSpec()
                .contentType(ContentType.JSON)
                .cookie("yandex_login", login)
                .body(Map.of(
                        "serviceUUID", graphId,
                        "events", List.of(Map.of(
                                        "yardClientId", assignedClientId,
                                        "eventType", eventType
                                )
                        ))
                )
                .when()
                .post("/window/uuid/" + capacityUnitUUID + "/push")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse windowSubmit(UUID graphId, UUID capacityUnitUUID, Long assignedClientId,
                                            String eventType,
                                            String login) {
        return baseRequestSpec()
                .contentType(ContentType.JSON)
                .cookie("yandex_login", login)
                .body(Map.of(
                        "serviceUUID", graphId,
                        "events", List.of(Map.of(
                                        "yardClientId", assignedClientId,
                                        "eventType", eventType,
                                        "meta", Map.of("selectedRequests", List.of())
                                )
                        ))
                )
                .when()
                .post("/window/uuid/" + capacityUnitUUID + "/submit")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }
}
