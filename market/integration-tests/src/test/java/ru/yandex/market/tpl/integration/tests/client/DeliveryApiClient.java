package ru.yandex.market.tpl.integration.tests.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.delivery.response.CreateOrderResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrderHistoryResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrdersStatusResponse;
import ru.yandex.market.logistic.api.model.delivery.response.UpdateItemsInstancesResponse;
import ru.yandex.market.logistic.api.model.delivery.response.UpdateOrderResponse;
import ru.yandex.market.tpl.integration.tests.configuration.TvmConfiguration;
import ru.yandex.market.tpl.integration.tests.stress.StressStatFilter;
import ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.integration.tests.configuration.TvmConfiguration.X_YA_SERVICE_TICKET;

@Component
public class DeliveryApiClient {
    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";
    private static final ObjectMapper MAPPER = createDefaultMapper();
    private final StressStatFilter stressStatFilter;
    private final TvmConfiguration.TvmTicketProvider tplTvmTicketProvider;
    private final ResponseSpecification responseSpecification = new ResponseSpecBuilder()
            .expectStatusCode(HttpStatus.OK.value())
            .expectContentType(MediaType.TEXT_XML_VALUE)
            .build();
    @Value("${api.url.delivery}")
    private String deliveryApiURL;

    public DeliveryApiClient(StressStatFilter stressStatFilter,
                             @Qualifier("tplTvmTicketProvider")
                                     TvmConfiguration.TvmTicketProvider tplTvmTicketProvider) {
        this.stressStatFilter = stressStatFilter;
        this.tplTvmTicketProvider = tplTvmTicketProvider;
    }

    private static XmlMapper createDefaultMapper() {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return xmlMapper;
    }

    RequestSpecification requestSpecification() {
        RequestSpecification requestSpecification = RestAssured
                .given()
                .urlEncodingEnabled(true)
                .filters(stressStatFilter, new AllureRestAssured())
                .baseUri(deliveryApiURL)
                .contentType(CONTENT_TYPE)
                .header("x-company-id", 1000848111L)
                .header(X_YA_SERVICE_TICKET, tplTvmTicketProvider.provideServiceTicket());
        if (!StressTestsUtil.isStressTestEnabled()) {
            requestSpecification.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        }
        return requestSpecification;
    }

    @Step("Создание заказа из Доставки POST /delivery/query-gateway/createOrder")
    public CreateOrderResponse createOrder(String createOrderRequest) {
        return readResponse(
                requestSpecification()
                        .when()
                        .body(createOrderRequest)
                        .post("/createOrder")
                        .then().spec(responseSpecification)
                        .extract().response()
                        .asString(),
                CreateOrderResponse.class);
    }

    @Step("Получение статуса заказа из Доставки POST /delivery/query-gateway/getOrdersStatus")
    public GetOrdersStatusResponse getOrdersStatus(String getOrdersStatusRequest) {
        return readResponse(
                requestSpecification()
                        .when()
                        .body(getOrdersStatusRequest)
                        .post("/getOrdersStatus")
                        .then().spec(responseSpecification)
                        .extract().response()
                        .asString(),
                GetOrdersStatusResponse.class);
    }

    @Step("Получение истории статусов заказа из Доставки POST /delivery/query-gateway/getOrderHistory")
    public GetOrderHistoryResponse getOrderHistory(String getOrderHistoryRequest) {
        return readResponse(
                requestSpecification()
                        .when()
                        .body(getOrderHistoryRequest)
                        .post("/getOrderHistory")
                        .then().spec(responseSpecification)
                        .extract().response()
                        .asString(),
                GetOrderHistoryResponse.class);
    }

    @Step("Обновление заказа из Доставки  POST /delivery/query-gateway/updateOrder")
    public UpdateOrderResponse updateOrder(String updateOrderRequest) {
        return readResponse(
                requestSpecification()
                        .when()
                        .body(updateOrderRequest)
                        .post("/updateOrder")
                        .then().spec(responseSpecification)
                        .extract().response()
                        .asString(),
                UpdateOrderResponse.class);
    }

    @Step("Обновить данные о контрольных идентификационных знаках из Доставки  POST " +
            "/delivery/query-gateway/updateItemsInstances")
    public UpdateItemsInstancesResponse updateOrderItemsInstances(String updateOrderRequest) {
        return readResponse(
                requestSpecification()
                        .when()
                        .body(updateOrderRequest)
                        .post("/updateItemsInstances")
                        .then().spec(responseSpecification)
                        .extract().response()
                        .asString(),
                UpdateItemsInstancesResponse.class);
    }

    @SneakyThrows
    private <T extends AbstractResponse> T readResponse(String input, Class<T> clazz) {
        ResponseWrapper<T> requestWrapper = MAPPER.readValue(input,
                MAPPER.getTypeFactory().constructParametricType(ResponseWrapper.class, clazz));
        assertThat(requestWrapper.getResponse()).isNotNull();
        assertThat(requestWrapper.getRequestState().hasErrors()).isFalse();
        return requestWrapper.getResponse();
    }
}
