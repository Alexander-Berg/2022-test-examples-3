package ru.yandex.market.tpl.integration.tests.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import ru.yandex.market.tpl.api.model.lms.IdsDto;
import ru.yandex.market.tpl.api.model.user.TplUserPropertyDto;
import ru.yandex.market.tpl.integration.tests.configuration.TvmConfiguration;
import ru.yandex.market.tpl.integration.tests.service.LmsUserPropertyGridView;
import ru.yandex.market.tpl.integration.tests.stress.StressStatFilter;
import ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil;

import static ru.yandex.market.tpl.integration.tests.configuration.TvmConfiguration.X_YA_SERVICE_TICKET;

@Component
public class LmsTplApiClient implements ApiClient {

    private final StressStatFilter stressStatFilter;
    private final ResponseSpecification responseSpecification = new ResponseSpecBuilder()
            .expectStatusCode(HttpStatus.OK.value())
            .expectContentType(ContentType.JSON)
            .build();

    private final TvmConfiguration.TvmTicketProvider tplTvmTicketProvider;
    @Value("${api.url.lms-tpl}")
    private String lmsTplApiUrl;

    public LmsTplApiClient(StressStatFilter stressStatFilter,
                           @Qualifier("tplTvmTicketProvider") TvmConfiguration.TvmTicketProvider tplTvmTicketProvider) {
        this.stressStatFilter = stressStatFilter;
        this.tplTvmTicketProvider = tplTvmTicketProvider;
    }

    RequestSpecification requestSpecification() {
        RequestSpecification requestSpecification = RestAssured
                .given()
                .urlEncodingEnabled(false)
                .filters(stressStatFilter, new AllureRestAssured())
                .baseUri(lmsTplApiUrl)
                .contentType(ContentType.JSON)
                .header(X_YA_SERVICE_TICKET, tplTvmTicketProvider.provideServiceTicket());
        if (!StressTestsUtil.isStressTestEnabled()) {
            requestSpecification.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        }
        return requestSpecification;
    }

    public List<LmsUserPropertyGridView> getPropertiesForCourier(Long userId) {
        return requestSpecification()
                .when()
                .queryParam("size", 100)
                .queryParam("userId", userId)
                .get("/users/properties")
                .then().spec(responseSpecification)
                .extract().response().jsonPath().getList("items.values", LmsUserPropertyGridView.class);
    }

    public List<Long> createPropertiesForCourier(Long courierId, Map<String, String> properties) {
        List<Long> result = new ArrayList<>();
        properties.forEach((key, value) -> {
            TplUserPropertyDto requestBody = new TplUserPropertyDto();
            requestBody.setName(key);
            requestBody.setValue(value);

            result.add(requestSpecification()
                    .when()
                    .body(requestBody)
                    .queryParam("parentId", courierId)
                    .post("/users/properties/")
                    .then().spec(responseSpecification)
                    .extract().response().as(Long.class));
        });

        return result;
    }

    public void deletePropertiesForCourier(List<Long> ids) {
        IdsDto requestBody = new IdsDto(ids);
        requestSpecification()
                .when()
                .body(requestBody)
                .post("/users/properties/delete")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

}
