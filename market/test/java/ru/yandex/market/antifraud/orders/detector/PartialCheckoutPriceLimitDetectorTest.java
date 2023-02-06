package ru.yandex.market.antifraud.orders.detector;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.storage.entity.rules.DetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.rules.PartialCheckoutPriceLimitDetectorConfiguration;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.CartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderPaymentFullInfoDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderPaymentType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class PartialCheckoutPriceLimitDetectorTest {

    private static final PartialCheckoutPriceLimitDetectorConfiguration MOCK_CONFIG = new PartialCheckoutPriceLimitDetectorConfiguration(100_000);

    private PartialCheckoutPriceLimitDetector detector;

    @Before
    public void setUp() {
        detector = new PartialCheckoutPriceLimitDetector();
    }

    @Test
    public void noTriggeringForNotPartial() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                OrderPaymentType.POSTPAID,
                getCart(
                    false,
                    getOrderItemRequest(150_000)
                )));
        assertThat(odr.isFraud()).isFalse();
    }

    @Test
    public void noTriggeringForSmallPrice() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                OrderPaymentType.POSTPAID,
                getCart(
                    true,
                    getOrderItemRequest(15_000)
                )));
        assertThat(odr.isFraud()).isFalse();
    }

    @Test
    public void noTriggeringForPrepaid() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                OrderPaymentType.PREPAID,
                getCart(
                    true,
                    getOrderItemRequest(150_000)
                )));
        assertThat(odr.isFraud()).isFalse();
    }

    @Test
    public void triggeringForPartial() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                OrderPaymentType.POSTPAID,
                getCart(
                    true,
                    getOrderItemRequest(150_000)
                )));
        assertThat(odr)
            .extracting("isFraud", "actions", "reason")
            .containsExactly(true,
                Set.of(AntifraudAction.PREPAID_ONLY),
                "Заказ с примеркой. Стоимость заказа превышает лимит в 100000: 150000.00");
    }

    @Test
    public void triggeringForPartialWithPreviousOrders() {
        OrderDetectorResult odr = runDetector(
            getOrderRequest(
                OrderPaymentType.POSTPAID,
                getCart(
                    true,
                    getOrderItemRequest(60_000)
                ),
                getCart(
                    false,
                    getOrderItemRequest(60_000)
                ),
                getCart(
                    true,
                    getOrderItemRequest(60_000)
                )));
        assertThat(odr)
            .isEqualToIgnoringNullFields(OrderDetectorResult.builder()
                .actions(Set.of(AntifraudAction.PREPAID_ONLY))
                .reason("Заказ с примеркой. Стоимость заказа превышает лимит в 100000: 120000.00")
                .build());
    }

    @Test
    public void configDeserialize() {
        var json = AntifraudJsonUtil.toJson(MOCK_CONFIG);
        var configuration = AntifraudJsonUtil.fromJson(json, DetectorConfiguration.class);
        assertThat(configuration).isEqualTo(MOCK_CONFIG);
    }

    @Test
    public void configDeserialize2() {
        var json = "{\"_class\":\"ru.yandex.market.antifraud.orders.storage.entity.rules.PartialCheckoutPriceLimitDetectorConfiguration\",\"enabled\":true," +
            "\"experiment\":false,\"priceLimit\":100000," +
            "\"createdAt\":\"2021-10-12T13:18:55.553192Z\"}";
        assertThatCode(() -> AntifraudJsonUtil.fromJson(json, DetectorConfiguration.class))
            .doesNotThrowAnyException();
    }

    private OrderDetectorResult runDetector(MultiCartRequestDto orderRequest) {
        return detector.detectFraud(
            OrderDataContainer.builder()
                .orderRequest(orderRequest)
                .build(),
            MOCK_CONFIG);
    }

    private MultiCartRequestDto getOrderRequest(OrderPaymentType paymentType,
                                            CartRequestDto... carts) {
        return MultiCartRequestDto.builder()
            .checkout(true)
            .paymentFullInfo(OrderPaymentFullInfoDto.builder().orderPaymentType(paymentType).build())
            .carts(List.of(carts))
            .build();
    }

    private CartRequestDto getCart(boolean isPartial,
                                   OrderItemRequestDto... orderItems) {
        return CartRequestDto.builder()
            .partialCheckoutAvailable(isPartial)
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