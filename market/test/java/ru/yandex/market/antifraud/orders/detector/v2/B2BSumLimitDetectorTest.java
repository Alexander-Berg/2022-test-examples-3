package ru.yandex.market.antifraud.orders.detector.v2;

import java.math.BigDecimal;
import java.util.List;
import java.util.OptionalDouble;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.storage.entity.rules.v2.B2BSumLimitDetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.rules.v2.ExperimentAwareDetectorConfiguration;
import ru.yandex.market.antifraud.orders.util.concurrent.FutureValueHolder;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.CartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.crm.platform.models.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class B2BSumLimitDetectorTest {

    private static final ExperimentAwareDetectorConfiguration<B2BSumLimitDetectorConfiguration> CONFIGURATION = ExperimentAwareDetectorConfiguration.buildSimple(B2BSumLimitDetectorConfiguration.builder()
        .firstOrderDetectionThreshold(OptionalDouble.of(500_000))
        .unpaidOrdersDetectionThreshold(OptionalDouble.of(500_000))
        .build());

    private B2BSumLimitDetector detector;

    @Before
    public void setUp() {
        detector = new B2BSumLimitDetector(new B2BSumLimitDetectorImpl());
    }

    @Test
    public void detectFraud() {
        var result = runDetector(getOrderRequest(
            getCart(getOrderItemRequest(200_000)),
            getCart(getOrderItemRequest(200_000)),
            getCart(getOrderItemRequest(200_000))
        ));
        assertThat(result.isFraud()).isTrue();
        //work around &nbsp;
        assertThat(result.getAnswerText().replaceAll("(?U)\\s+", " "))
            .isEqualTo("Уменьшите сумму заказа до 499 999 ₽");
    }

    private OrderDetectorResult runDetector(MultiCartRequestDto orderRequest, Order... lastOrders) {
        return detector.detectFraud(
            OrderDataContainer.builder()
                .orderRequest(orderRequest)
                .lastOrdersFuture(new FutureValueHolder<>(List.of(lastOrders)))
                .build(),
            CONFIGURATION);
    }

    private MultiCartRequestDto getOrderRequest(CartRequestDto... carts) {
        return MultiCartRequestDto.builder()
            .buyer(OrderBuyerRequestDto.builder()
                .businessBalanceId(1L)
                .build())
            .carts(List.of(carts))
            .build();
    }

    private CartRequestDto getCart(OrderItemRequestDto... orderItems) {
        return CartRequestDto.builder()
            .items(List.of(orderItems))
            .build();
    }

    private OrderItemRequestDto getOrderItemRequest(long price) {
        return OrderItemRequestDto.builder()
            .count(1)
            .price(BigDecimal.valueOf(price))
            .build();
    }
}