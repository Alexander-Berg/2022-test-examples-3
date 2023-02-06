package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.client;

import java.util.concurrent.TimeUnit;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;

@Resource.Classpath("cs/cs-api.properties")
public class CalendaringServiceApi {

    private static final Logger log = LoggerFactory.getLogger(CalendaringServiceApi.class);

    private static final int CS_RETRIES = 10;
    private static final int CS_TIMEOUT = 1;
    private static final TimeUnit CS_TIME_UNIT = TimeUnit.MINUTES;

    @Property("cs-api.host")
    private String host;

    public CalendaringServiceApi() {
        PropertyLoader.newInstance().populate(this);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    public ValidatableResponse getCalendaringRequest(String warehouseId, String supplyDate) {
        log.info("Getting calendaring data for date {} and warehouse {}", supplyDate, warehouseId);

        String reqPath = String.format("/calendaring/%s/%s/INBOUND", warehouseId, supplyDate);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .when()
                        .get(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                CS_RETRIES,
                CS_TIMEOUT,
                CS_TIME_UNIT
        );
    }

    public ValidatableResponse getCalendaringWithdrawRequest(String warehouseId, String supplyDate) {
        log.info("Getting calendaring withdraw data for date {} and warehouse {}", supplyDate, warehouseId);

        String reqPath = String.format("/calendaring/%s/%s/OUTBOUND", warehouseId, supplyDate);

        return Retrier.clientRetry(() -> baseRequestSpec()
                        .when()
                        .get(reqPath)
                        .then()
                        .statusCode(HttpStatus.SC_OK),
                CS_RETRIES,
                CS_TIMEOUT,
                CS_TIME_UNIT
        );
    }

    private RequestSpecification baseRequestSpec() {
        return RestAssured
                .given()
                .filter(new AllureRestAssured())
                .baseUri(host)
                .header("User-Login", "mslyusarenko");
    }
}
