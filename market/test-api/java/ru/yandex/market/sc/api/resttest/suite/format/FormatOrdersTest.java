package ru.yandex.market.sc.api.resttest.suite.format;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sc.api.resttest.infra.AccessingMatcher;
import ru.yandex.market.sc.api.resttest.infra.RestTest;
import ru.yandex.market.sc.api.resttest.infra.RestTestContext;
import ru.yandex.market.sc.core.resttest.infra.RestTestFactory;
import ru.yandex.market.sc.internal.model.CreateDemoOrderDto;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author valter
 */
@RestTest
public class FormatOrdersTest {

    private final RestTestFactory restTestFactory = new RestTestFactory();

    @Test
    @DisplayName("Формат GET /api/orders (одноместный заказ)")
    void getOrders() {
        long sortingCenterId = RestTestContext.get().getSortingCenterId();
        String externalOrderId = restTestFactory.createDemoOrder(sortingCenterId);
        when()
                .get("/api/orders?externalId={externalId}", externalOrderId)
                .then()
                .statusCode(200)
                .rootPath("")
                .body(
                        "id", greaterThan(0),
                        "status", equalTo("KEEP"),
                        "cell", nullValue(),
                        "courier", nullValue(),
                        "warehouse", nullValue(),
                        "zone", nullValue(),
                        "routeTo", nullValue(),
                        "cellTo", nullValue(),
                        "places", hasSize(1),
                        "possibleOutgoingRouteDate", equalTo(LocalDate.now(RestTestContext.get().getClock()).toString())
                )
                .body(matchesJsonSchemaInClasspath("json-schema/orders.json"));
    }

    @Test
    @DisplayName("Формат PUT /api/orders/acceptReturn (многоместный заказ, приемка от курьера)")
    void getOrdersFromCourier() {
        long sortingCenterId = RestTestContext.get().getSortingCenterId();
        long courierId = 1L;
        String externalOrderId = restTestFactory.createDemoOrder(
                new CreateDemoOrderDto(sortingCenterId,
                        null, 2, courierId, LocalDate.now(RestTestContext.get().getClock()),
                        null, null, null, null, false));

        Object order = getValueFromOrder(externalOrderId, "");

        List<Object> availableCells = getValueFromObject(order, "availableCells");
        Object cellTo = availableCells.get(0);
        Integer cellToId = getValueFromObject(cellTo, "id");

        restTestFactory
                .acceptOrder(externalOrderId, externalOrderId + "-0", sortingCenterId)
                .acceptOrder(externalOrderId, externalOrderId + "-1", sortingCenterId)
                .sortOrder(externalOrderId, externalOrderId + "-0", cellToId, sortingCenterId)
                .sortOrder(externalOrderId, externalOrderId + "-1", cellToId, sortingCenterId)
                .shipOrderToCourier(externalOrderId, externalOrderId + "-0", courierId, sortingCenterId)
                .shipOrderToCourier(externalOrderId, externalOrderId + "-1", courierId, sortingCenterId);

        given()
                .body("{\"externalId\":\"" + externalOrderId + "\"}")
                .contentType(ContentType.JSON)
                .when()
                .put("/api/orders/acceptReturn")
                .then()
                .statusCode(200)
                .rootPath("")
                .body(
                        "id", greaterThan(0),
                        "status", equalTo("KEEP"),
                        "cell", nullValue(),
                        "courier", nullValue(),
                        "warehouse", nullValue(),
                        "zone", nullValue(),
                        "routeTo", nullValue(),
                        "availableCells", equalTo(List.of()),
                        "places", hasSize(2),
                        "places[0].externalId", equalTo(externalOrderId + "-0"),
                        "places[0].status", equalTo("KEEP"),
                        "places[1].externalId", equalTo(externalOrderId + "-1"),
                        "places[1].status", equalTo("KEEP"),
                        "possibleOutgoingRouteDate", equalTo(LocalDate.now(RestTestContext.get().getClock()).toString())
                )
                .body(matchesJsonSchemaInClasspath("json-schema/orders.json"));
    }

    private <T> T getValueFromObject(Object value, String field) {
        //noinspection unchecked
        return (T) (((Map<String, Object>) value).get(field));
    }

    @SuppressWarnings("SameParameterValue")
    private <T> T getValueFromOrder(String externalOrderId, String path) {
        var result = new AtomicReference<T>();
        when()
                .get("/api/orders?externalId={externalId}", externalOrderId)
                .then()
                .statusCode(200)
                .rootPath("")
                .body(path, new AccessingMatcher<T>() {
                    @Override
                    protected void accessValue(T value) {
                        result.set(value);
                    }
                });
        return result.get();
    }

}
