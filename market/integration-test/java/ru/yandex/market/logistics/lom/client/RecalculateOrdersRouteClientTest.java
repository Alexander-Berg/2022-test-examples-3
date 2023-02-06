package ru.yandex.market.logistics.lom.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.dto.RecalculateOrdersRouteRequest;
import ru.yandex.market.logistics.lom.model.dto.RecalculatedOrdersRouteResponse;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

@DisplayName("Тест клиента на пересчет маршруты у списка заказов")
public class RecalculateOrdersRouteClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Создать заявку на пересчёт дат по сегментам заказа в Комбинаторе")
    void testRecalculateRouteDates() throws IOException {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/recalculateOrdersRoute"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/order/recalculate_orders_route.json"))
            .andRespond(
                withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/order/recalculate_orders_route.json"))
            );

        RecalculateOrdersRouteRequest requestDto = RecalculateOrdersRouteRequest.builder()
            .orderIds(List.of(101L))
            .build();

        RecalculatedOrdersRouteResponse expectedResponse = RecalculatedOrdersRouteResponse.builder()
            .recalculatedRouteDtoList(
                List.of(
                    RecalculatedOrdersRouteResponse.RecalculatedRouteDto.builder()
                        .orderId(101L)
                        .newRoute(expectedRoute())
                        .status(RecalculatedOrdersRouteResponse.RecalculationStatus.SUCCESS)
                        .build()
                )
            )
            .build();

        softly.assertThat(lomClient.recalculateOrdersRoute(requestDto))
            .as("Asserting that the response is parsed correctly")
            .isEqualTo(expectedResponse);
    }

    @Nonnull
    private CombinatorRoute expectedRoute() {
        return new CombinatorRoute()
            .setRoute(
                new CombinatorRoute.DeliveryRoute()
                    .setCost(new BigDecimal(249))
                    .setPaths(List.of(
                        new CombinatorRoute.Path().setPointFrom(0).setPointTo(1)
                    ))
            );

    }
}
