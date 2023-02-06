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

import ru.yandex.market.sc.internal.model.lms.DeleteIdsDto;
import ru.yandex.market.tpl.integration.tests.configuration.TvmConfiguration;
import ru.yandex.market.tpl.integration.tests.service.LmsSortingCenterPropertyDetailDto;
import ru.yandex.market.tpl.integration.tests.stress.StressStatFilter;
import ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil;

import static ru.yandex.market.tpl.integration.tests.configuration.TvmConfiguration.X_YA_SERVICE_TICKET;

@Component
public class LmsScApiClient implements ApiClient {

    private final StressStatFilter stressStatFilter;
    private final ResponseSpecification responseSpecification = new ResponseSpecBuilder()
            .expectStatusCode(HttpStatus.OK.value())
            .expectContentType(ContentType.JSON)
            .build();
    private final TvmConfiguration.TvmTicketProvider scTvmTicketProvider;
    @Value("${api.url.lms-sc}")
    private String lmsScApiUrl;

    public LmsScApiClient(StressStatFilter stressStatFilter,
                          @Qualifier("scTvmTicketProvider") TvmConfiguration.TvmTicketProvider scTvmTicketProvider) {
        this.stressStatFilter = stressStatFilter;
        this.scTvmTicketProvider = scTvmTicketProvider;
    }

    RequestSpecification requestSpecification() {
        RequestSpecification requestSpecification = RestAssured
                .given()
                .urlEncodingEnabled(false)
                .filters(stressStatFilter, new AllureRestAssured())
                .baseUri(lmsScApiUrl)
                .contentType(ContentType.JSON)
                .header(X_YA_SERVICE_TICKET, scTvmTicketProvider.provideServiceTicket());
        if (!StressTestsUtil.isStressTestEnabled()) {
            requestSpecification.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        }
        return requestSpecification;
    }

    public List<LmsSortingCenterPropertyDetailDto> getPropertiesForSortingCenter(Long sortingCenterId) {
        return requestSpecification()
                .when()
                .queryParam("id", sortingCenterId)
                .get("/sortingCenters/properties")
                .then().spec(responseSpecification)
                .extract().response().jsonPath().getList("items.values", LmsSortingCenterPropertyDetailDto.class);
    }

    public List<Long> createPropertiesForSortingCenter(Long sortingCenterId,
                                                       Map<String, String> properties) {
        List<Long> result = new ArrayList<>();
        properties.forEach((key, value) -> {
            LmsSortingCenterPropertyDetailDto requestBody = new LmsSortingCenterPropertyDetailDto();
            requestBody.setKey(key);
            requestBody.setSortingCenterId(sortingCenterId.toString());
            requestBody.setValue(value);
            requestBody.setKeyComment("");

            result.add(requestSpecification()
                    .when()
                    .body(requestBody)
                    .queryParam("parentId", sortingCenterId)
                    .post("/sortingCenters/properties/")
                    .then().spec(responseSpecification)
                    .extract().response().as(Long.class));
        });

        return result;
    }

    public void deletePropertiesForSortingCenter(List<Long> ids) {
        DeleteIdsDto requestBody = new DeleteIdsDto();
        requestBody.setIds(ids);
        requestSpecification()
                .when()
                .body(requestBody)
                .post("/sortingCenters/properties/delete")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

}
