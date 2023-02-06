package ru.yandex.market.antifraud.orders.detector;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.storage.entity.antifraud.AccountState;
import ru.yandex.market.antifraud.orders.test.utils.AntifraudTestUtils;
import ru.yandex.market.antifraud.orders.util.concurrent.FutureValueHolder;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.models.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author dzvyagin
 */
public class DisableYongAccPostpaidDetectorTest {

    private final DisableYongAccPostpaidDetector detector = new DisableYongAccPostpaidDetector();

    @Test
    public void testHaveOrders() {
        OrderDataContainer container = OrderDataContainer.builder()
            .lastOrdersFuture(new FutureValueHolder<>(List.of(
                Order.newBuilder()
                    .setCreationDate(Instant.now().minusSeconds(50000).toEpochMilli())
                    .setStatus("DELIVERED")
                    .setRgb(RGBType.BLUE)
                    .setId(123L)
                    .build()
            )))
            .build();
        assertThat(detector.detectFraud(container)).isEqualTo(OrderDetectorResult.empty(detector.getUniqName()));
    }

    @Test
    public void testYoungCookie() {
        OrderDataContainer container = OrderDataContainer.builder()
                .orderRequest(MultiCartRequestDto.builder()
                    .buyer(OrderBuyerRequestDto.builder()
                        .yandexuid("cookie-" + (Instant.now().toEpochMilli() / 1000))
                        .build())
                    .build())
                .lastOrdersFuture(new FutureValueHolder<>(List.of(
                        Order.newBuilder()
                                .setCreationDate(Instant.now().minusSeconds(50000).toEpochMilli())
                                .setStatus("DELIVERY")
                                .setRgb(RGBType.BLUE)
                                .setId(123L)
                                .build()
                )))
                .build();
        assertThat(detector.detectFraud(container))
                .isEqualTo(OrderDetectorResult.builder()
                        .ruleName(detector.getUniqName())
                        .actions(Set.of(AntifraudAction.PREPAID_ONLY))
                        .reason("Покупатель без доставленных заказов и с молодой кукой.")
                        .answerText("Постоплатные заказы с молодых аккаунтов временно ограничены")
                        .build());
    }

    @Test
    public void testOldCookieOldAccount() {
        OrderDataContainer container = OrderDataContainer.builder()
                .orderRequest(MultiCartRequestDto.builder()
                    .buyer(OrderBuyerRequestDto.builder()
                        .yandexuid("cookie-" + (Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli() / 1000))
                        .build())
                    .build())
                .lastOrdersFuture(new FutureValueHolder<>(List.of(
                        Order.newBuilder()
                                .setCreationDate(Instant.now().minusSeconds(50000).toEpochMilli())
                                .setStatus("DELIVERY")
                                .setRgb(RGBType.BLUE)
                                .setId(123L)
                                .build()
                )))
                .passportFeaturesFuture(new FutureValueHolder<>(Optional.empty()))
                .accountStateFuture(new FutureValueHolder<>(Optional.of(mock(AccountState.class))))
                .build();
        OrderDetectorResult expected = AntifraudTestUtils.okResult(
                detector.getUniqName(),
                "Покупатель без доставленных заказов, но со старыми аккаунтом и кукой");
        assertThat(detector.detectFraud(container)).isEqualTo(expected);
    }

    @Test
    public void testOldCookieNewAccount() {
        OrderDataContainer container = OrderDataContainer.builder()
                .orderRequest(MultiCartRequestDto.builder()
                    .buyer(OrderBuyerRequestDto.builder()
                        .yandexuid("cookie-" + (Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli() / 1000))
                        .build())
                    .build())
                .lastOrdersFuture(new FutureValueHolder<>(List.of(
                        Order.newBuilder()
                                .setCreationDate(Instant.now().minusSeconds(50000).toEpochMilli())
                                .setStatus("DELIVERY")
                                .setRgb(RGBType.BLUE)
                                .setId(123L)
                                .build()
                )))
                .passportFeaturesFuture(new FutureValueHolder<>(Optional.empty()))
                .accountStateFuture(new FutureValueHolder<>(Optional.empty()))
                .build();
        assertThat(detector.detectFraud(container))
                .isEqualTo(OrderDetectorResult.builder()
                        .ruleName(detector.getUniqName())
                        .actions(Set.of(AntifraudAction.PREPAID_ONLY))
                        .reason("Покупатель без доставленных заказов и со свежим аккаунтом.")
                        .answerText("Постоплатные заказы с молодых аккаунтов временно ограничены")
                        .build());
    }

}
