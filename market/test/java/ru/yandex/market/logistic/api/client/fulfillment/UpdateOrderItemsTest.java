package ru.yandex.market.logistic.api.client.fulfillment;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.request.UpdateOrderItemsRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.UpdateOrderItemsResponse;
import ru.yandex.market.logistic.api.utils.ResponseValidationUtils;
import ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getDecimalMinZeroErrorMessage;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getNotEmptyErrorMessage;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getNotNullErrorMessage;

class UpdateOrderItemsTest extends CommonServiceClientTest {
    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Test
    void testUpdateOrderItemsSucceeded() throws Exception {
        prepareMockServiceNormalized(
            "ff_update_order_items",
            PARTNER_URL
        );
        UpdateOrderItemsRequest request = validArguments().build();
        UpdateOrderItemsResponse response =
            fulfillmentClient.updateOrderItems(
                request.getOrderId(),
                request.getTotal(),
                request.getAssessedCost(),
                request.getDeliveryCost(),
                request.getKorobyte(),
                request.getItems(),
                getPartnerProperties()
            );
        assertEquals(
            DtoFactory.createResourceId("12345", "FF-12345"),
            response.getOrderId(),
            "Проверяем соответствие orderId"
        );
    }

    @Test
    void testUpdateOrderItemsFailed() throws Exception {
        prepareMockServiceNormalized(
            "ff_update_order_items",
            "ff_update_order_items_with_error",
            PARTNER_URL
        );
        UpdateOrderItemsRequest request = validArguments().build();
        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.updateOrderItems(
                request.getOrderId(),
                request.getTotal(),
                request.getAssessedCost(),
                request.getDeliveryCost(),
                request.getKorobyte(),
                request.getItems(),
                getPartnerProperties()
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidRequests")
    void testUpdateOrderItemValidationFailed(
        UpdateOrderItemsRequest.UpdateOrderItemsRequestBuilder requestBuilder,
        String errorMessage,
        String displayName
    ) {
        UpdateOrderItemsRequest request = requestBuilder.build();
        assertions.assertThatThrownBy(
            () -> fulfillmentClient.updateOrderItems(
                request.getOrderId(),
                request.getTotal(),
                request.getAssessedCost(),
                request.getDeliveryCost(),
                request.getKorobyte(),
                request.getItems(),
                getPartnerProperties()
            )
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(errorMessage);
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
            Arguments.of(
                validArguments().setOrderId(null),
                getNotNullErrorMessage("orderId"),
                "Идентификатор заказа не указан"
            ),
            Arguments.of(
                validArguments().setTotal(null),
                getNotNullErrorMessage("total"),
                "Стоимость заказа не указана"
            ),
            Arguments.of(
                validArguments().setTotal(BigDecimal.valueOf(-1)),
                getDecimalMinZeroErrorMessage("total"),
                "Стоимость заказа отрицательна"
            ),
            Arguments.of(
                validArguments().setAssessedCost(null),
                getNotNullErrorMessage("assessedCost"),
                "Оценочная стоимость заказа не указана"
            ),
            Arguments.of(
                validArguments().setAssessedCost(BigDecimal.valueOf(-1)),
                getDecimalMinZeroErrorMessage("assessedCost"),
                "Оценочная стоимость заказа отрицательна"
            ),
            Arguments.of(
                validArguments().setDeliveryCost(null),
                getNotNullErrorMessage("deliveryCost"),
                "Стоимость доставки не указана"
            ),
            Arguments.of(
                validArguments().setDeliveryCost(BigDecimal.valueOf(-1)),
                getDecimalMinZeroErrorMessage("deliveryCost"),
                "Стоимость доставки заказа отрицательна"
            ),
            Arguments.of(
                validArguments().setKorobyte(null),
                getNotNullErrorMessage("korobyte"),
                "Весогабариты заказа не указаны"
            ),
            Arguments.of(
                validArguments().setItems(ImmutableList.of()),
                getNotEmptyErrorMessage("items"),
                "Товары заказа не указаны"
            ),
            Arguments.of(
                validArguments().setItems(Collections.singletonList(null)),
                getNotNullErrorMessage("items[0].<list element>"),
                "Товары заказа не указаны"
            )
        );
    }

    @Test
    void testUpdateOrderItemsResponseFailed() throws Exception {
        prepareMockServiceNormalized(
            "ff_update_order_items",
            "ff_update_order_items_with_validation_error",
            PARTNER_URL
        );
        UpdateOrderItemsRequest request = validArguments().build();
        assertions.assertThatThrownBy(
            () -> fulfillmentClient.updateOrderItems(
                request.getOrderId(),
                request.getTotal(),
                request.getAssessedCost(),
                request.getDeliveryCost(),
                request.getKorobyte(),
                request.getItems(),
                getPartnerProperties()
            )
        )
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining(ResponseValidationUtils.getNotNullErrorMessage(
                "fulfillment",
                "UpdateOrderItemsResponse",
                "orderId"
            ));
    }

    private static UpdateOrderItemsRequest.UpdateOrderItemsRequestBuilder validArguments() {
        return new UpdateOrderItemsRequest.UpdateOrderItemsRequestBuilder()
            .setOrderId(DtoFactory.createResourceId("12345", "FF-12345"))
            .setTotal(BigDecimal.valueOf(1100))
            .setAssessedCost(BigDecimal.valueOf(1000))
            .setDeliveryCost(BigDecimal.valueOf(100))
            .setKorobyte(DtoFactory.createKorobyte())
            .setItems(DtoFactory.createItems());
    }
}
