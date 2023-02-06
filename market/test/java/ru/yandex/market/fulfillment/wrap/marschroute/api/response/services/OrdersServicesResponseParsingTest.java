package ru.yandex.market.fulfillment.wrap.marschroute.api.response.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.client.MarschrouteClientConfiguration;

import static com.google.common.io.Resources.getResource;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MarschrouteClientConfiguration.class)
class OrdersServicesResponseParsingTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Сценарий, в котором проверяется корректность парсинга ответа от Маршрута,
     * в котором были возвращены две услуги.
     */
    @Test
    void parsingSuccessfulResponse() throws Exception {
        OrdersServicesResponse response = parseResponse("orders_service/full_response.json");

        assertSoftly(assertions -> {
            assertions.assertThat(response.isSuccess())
                .as("Asserting success value")
                .isTrue();

            assertions.assertThat(response.getData())
                .as("Asserting data size")
                .hasSize(2);

            OrderService service = response.getData().get(0);

            assertions.assertThat(service.getDate())
                .as("Asserting date value")
                .isEqualTo(LocalDateTime.of(2017, 11, 26, 2, 1, 31, 5105000));

            assertions.assertThat(service.getNameService())
                .as("Asserting nameService value")
                .isEqualTo("Комплектация заказов в стрейч-пленку");

            assertions.assertThat(service.getUnit())
                .as("Asserting unit value")
                .isEqualTo("шт");

            assertions.assertThat(service.getOrderId())
                .as("Asserting orderId value")
                .isEqualTo("EXT51551902");

            assertions.assertThat(service.getQty())
                .as("Asserting qty value")
                .isEqualTo(BigDecimal.valueOf(1));

            assertions.assertThat(service.getRate())
                .as("Asserting rate value")
                .isEqualTo("129.0000");

            assertions.assertThat(service.getSumNds())
                .as("Asserting sumNds value")
                .isEqualByComparingTo(new BigDecimal("129.00"));
        });
    }


    /**
     * Сценарий, в котором проверяется корректность парсинга ответа от Маршрута,
     * в котором отсутствовали услуги за запрошенный период времени.
     */
    @Test
    void parsingEmptyResponse() throws Exception {
        OrdersServicesResponse response = parseResponse("orders_service/empty_response.json");

        assertSoftly(assertions -> {
            assertions.assertThat(response.getData())
                .as("Asserting response data size")
                .isEmpty();

            assertions.assertThat(response.isSuccess())
                .as("Asserting success value")
                .isTrue();
        });
    }

    /**
     * Сценарий, в котором проверяется корректность парсинга ответа от Маршрута,
     * в котором в результате запроса произошла ошибка
     */
    @Test
    void parsingResponseWithError() throws Exception {
        OrdersServicesResponse response = parseResponse("orders_service/error_response.json");

        assertSoftly(assertions -> {
            assertions.assertThat(response.isSuccess())
                .as("Asserting success value")
                .isFalse();

            assertions.assertThat(response.getCode())
                .as("Asserting code value")
                .isEqualTo(104);

            assertions.assertThat(response.getComment())
                .as("Asserting comment value")
                .isEqualTo("Некорректный формат параметров запроса");
        });
    }

    private OrdersServicesResponse parseResponse(String filePath) throws IOException {
        String json = Resources.toString(getResource(filePath), Charsets.UTF_8);

        return objectMapper.readValue(json, OrdersServicesResponse.class);
    }
}
