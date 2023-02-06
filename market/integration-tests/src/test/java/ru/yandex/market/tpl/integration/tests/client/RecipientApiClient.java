package ru.yandex.market.tpl.integration.tests.client;

import java.util.List;
import java.util.Map;

import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.tracking.TrackingCancelOrderDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingRescheduleDto;
import ru.yandex.market.tpl.api.model.tracking.UserLocationDto;
import ru.yandex.market.tpl.integration.tests.configuration.TvmConfiguration;
import ru.yandex.market.tpl.integration.tests.stress.StressStatFilter;
import ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil;

import static ru.yandex.market.tpl.common.util.StringFormatter.sf;
import static ru.yandex.market.tpl.integration.tests.configuration.TvmConfiguration.X_YA_SERVICE_TICKET;

@Component
public class RecipientApiClient implements ApiClient {

    private final StressStatFilter stressStatFilter;
    private final TvmConfiguration.TvmTicketProvider tplTvmTicketProvider;
    private final ResponseSpecification responseSpecification = new ResponseSpecBuilder()
            .expectStatusCode(HttpStatus.OK.value())
            .expectContentType(ContentType.JSON)
            .build();
    @Value("${api.url.recipient}")
    private String recipientApiUrl;

    public RecipientApiClient(StressStatFilter stressStatFilter,
                              @Qualifier("tplTvmTicketProvider")
                                      TvmConfiguration.TvmTicketProvider tplTvmTicketProvider) {
        this.stressStatFilter = stressStatFilter;
        this.tplTvmTicketProvider = tplTvmTicketProvider;
    }

    RequestSpecification requestSpecification() {
        RequestSpecification requestSpecification = RestAssured
                .given()
                .urlEncodingEnabled(false)
                .filters(stressStatFilter, new AllureRestAssured())
                .baseUri(recipientApiUrl)
                .contentType(ContentType.JSON)
                .header("x-company-id", 1000848111L)
                .header(X_YA_SERVICE_TICKET, tplTvmTicketProvider.provideServiceTicket());
        if (!StressTestsUtil.isStressTestEnabled()) {
            requestSpecification.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        }
        return requestSpecification;
    }

    @Step("Вызов GET /internal/tracking")
    public String getTrackingLinkByOrder(String externalOrderId) {
        return requestSpecification()
                .when()
                .queryParam("externalOrderId", externalOrderId)
                .get("/internal/tracking")
                .then().spec(responseSpecification)
                .extract().jsonPath().get("id");
    }

    @Step("Вызов GET /internal/tracking/{id}")
    public TrackingDto getTrackingInfo(String trackingId) {
        return requestSpecification()
                .when()
                .get(sf("/internal/tracking/{}", trackingId))
                .then().spec(responseSpecification)
                .extract().as(TrackingDto.class);
    }

    @Step("Вызов GET /internal/tracking/{id}/courier-location")
    public UserLocationDto getCourierLocation(String trackingId) {
        return requestSpecification()
                .when()
                .get(sf("/internal/tracking/{}/courier-location", trackingId))
                .then().spec(responseSpecification)
                .extract().as(UserLocationDto.class);
    }

    @Step("Вызов POST /internal/tracking/{id}/cancel")
    public TrackingDto cancelTrackingOrder(String trackingId, TrackingCancelOrderDto cancelOrderDto) {
        return requestSpecification()
                .when()
                .body(cancelOrderDto)
                .post(sf("/internal/tracking/{}/cancel", trackingId))
                .then().spec(responseSpecification)
                .extract().as(TrackingDto.class);
    }

    @Step("Вызов GET /internal/tracking/{id}/rescheduleDates")
    public Map<String, List<String>> getRescheduleDatesForOrder(String trackingId) {
        return requestSpecification()
                .when()
                .get(sf("/internal/tracking/{}/rescheduleDates", trackingId))
                .then().spec(responseSpecification)
                .extract().jsonPath().getMap("");
    }

    @Step("Вызов POST /internal/tracking/{id}/reschedule")
    public TrackingDto reschedule(String trackingId, TrackingRescheduleDto trackingRescheduleDto) {
        return requestSpecification()
                .when()
                .body(trackingRescheduleDto)
                .post(sf("/internal/tracking/{}/reschedule", trackingId))
                .then().spec(responseSpecification)
                .extract().as(TrackingDto.class);
    }
}
