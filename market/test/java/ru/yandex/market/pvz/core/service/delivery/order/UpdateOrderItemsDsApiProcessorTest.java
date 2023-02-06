package ru.yandex.market.pvz.core.service.delivery.order;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.delivery.request.CreateOrderRequest;
import ru.yandex.market.logistic.api.model.delivery.request.UpdateOrderItemsRequest;
import ru.yandex.market.logistic.api.model.delivery.response.CreateOrderResponse;
import ru.yandex.market.pvz.core.domain.order.model.OrderItem;
import ru.yandex.market.pvz.core.service.delivery.DsApiBaseTest;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valeriashanti
 */
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UpdateOrderItemsDsApiProcessorTest extends DsApiBaseTest {

    private final TransactionTemplate transactionTemplate;
    private final UpdateOrderItemsDsApiProcessor updateOrderItemsDsApiProcessor;
    private final TestDeliveryServiceFactory deliveryServiceFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory testPickupPointFactory;
    private final DsOrderManager dsOrderManager;

    @ParameterizedTest(name = "updateOrderItems_{index}")
    @MethodSource("updateItemsMethodSource")
    public void updateOrderItems(String fileName, int expectedSize, int expectedCount, BigDecimal expectedSumPrice) {
        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(deliveryService)
                        .build());
        var pickupPoint = testPickupPointFactory.createPickupPoint(TestPickupPointFactory
                .CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());
        CreateOrderRequest request = readRequest("/ds/order/create_order_with_multiple_items.xml",
                CreateOrderRequest.class, Map.of(
                        "pickup_point_code", pickupPoint.getId()
                ));

        CreateOrderResponse orderResponse = dsOrderManager.createOrder(request.getOrder(), deliveryService);

        var updateOrderItemsRequest = readRequest(
                fileName,
                UpdateOrderItemsRequest.class,
                Map.of("order_id", orderResponse.getOrderId())
        );

        transactionTemplate.execute(ts -> {
            var order = dsOrderManager.getOrThrow(orderResponse.getOrderId(),
                    deliveryService.getId()
//                    , pickupPoint.getId()
            );
            var updateOrderItemsResponse =
                    updateOrderItemsDsApiProcessor.apiCall(updateOrderItemsRequest, deliveryService);

            assertThat(updateOrderItemsResponse).isNotNull();
            assertThat(order.getItems())
                    .filteredOn(Predicate.not(OrderItem::isService))
                    .hasSize(expectedSize);
            assertThat(order.getItems())
                    .filteredOn(OrderItem::isService)
                    .hasSize(1);

            assertThat(((Integer) order.getItems().stream().filter(e -> !e.isService())
                    .map(OrderItem::getCount).mapToInt(e -> e).sum())).isEqualTo(expectedCount);

            assertThat(order.getPlaces()).hasSize(2);

            assertThat(order.getOrderAdditionalInfo().getTotalPrice()).isEqualByComparingTo(expectedSumPrice);
            assertThat(order.getOrderAdditionalInfo().getIsAdult()).isFalse();

            return null;
        });
    }

    static Stream<Arguments> updateItemsMethodSource() {
        return StreamEx.of(
                Arguments.of("/ds/order/update_order_items.xml", 1, 1, BigDecimal.valueOf(1790.00)),
                Arguments.of("/ds/order/update_order_items_with_same_items_amount.xml", 2, 8,
                        BigDecimal.valueOf(12920.00)),
                Arguments.of("/ds/order/update_order_items_multi_count_null_article.xml", 2, 4,
                        BigDecimal.valueOf(6560.00))
        );
    }
}
