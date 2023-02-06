package ru.yandex.market.antifraud.orders.detector;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.service.loyalty.detectors.UsedCoinsDetector;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;
import ru.yandex.market.antifraud.orders.web.entity.OrderItemChange;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class DetectorUtilsTest {

    @Test
    public void getDetectorName() {
        assertThat(DetectorUtils.getDetectorName(AntifraudBlacklistDetector.class)).isEqualTo("ORDER_AntifraudBlacklistDetector");
        assertThat(DetectorUtils.getDetectorName(UsedCoinsDetector.class)).isEqualTo("LOYALTY_UsedCoinsDetector");
    }

    @Test
    public void getUniqOrdersWithPuidKeyUid() {
        Order o1 = Order.newBuilder()
                .setId(1L)
                .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                .build();
        Order o2 = Order.newBuilder()
                .setId(1L)
                .setKeyUid(Uid.newBuilder().setType(UidType.UUID).setIntValue(1123L).build())
                .build();
        Order o3 = Order.newBuilder()
                .setId(2L)
                .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                .build();
        Order o4 = Order.newBuilder()
                .setId(2L)
                .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                .build();
        List<Order> orders = DetectorUtils.getUniqOrdersWithPuidKeyUid(List.of(o1, o2, o3, o4));
        assertThat(orders).hasSize(2);
        assertThat(orders).contains(o1, o3);
    }

    @Test
    public void applyRule_limitByCount() {
        var items = List.of(
            OrderItemRequestDto.builder()
                .id(1L)
                .count(3)
                .price(BigDecimal.valueOf(1000))
                .build(),
            OrderItemRequestDto.builder()
                .id(2L)
                .count(5)
                .price(BigDecimal.valueOf(1100))
                .build(),
            OrderItemRequestDto.builder()
                .id(3L)
                .count(4)
                .price(BigDecimal.valueOf(900))
                .build()
        );
        assertThat(DetectorUtils.applyRule(items, 6, BigDecimal.valueOf(7000)))
            .containsExactly(
                new ItemLimitRuleResult(items.get(2), 6, Set.of()),
                new ItemLimitRuleResult(items.get(0), 2, Set.of(OrderItemChange.FRAUD_FIXED, OrderItemChange.COUNT)),
                new ItemLimitRuleResult(items.get(1), 0, Set.of(OrderItemChange.FRAUD_FIXED, OrderItemChange.MISSING))
            );
    }

    @Test
    public void applyRule_limitByAmount() {
        var items = List.of(
            OrderItemRequestDto.builder()
                .id(1L)
                .count(3)
                .price(BigDecimal.valueOf(1000))
                .build(),
            OrderItemRequestDto.builder()
                .id(2L)
                .count(5)
                .price(BigDecimal.valueOf(1100))
                .build(),
            OrderItemRequestDto.builder()
                .id(3L)
                .count(4)
                .price(BigDecimal.valueOf(900))
                .build()
        );
        assertThat(DetectorUtils.applyRule(items, 7, BigDecimal.valueOf(5600)))
            .containsExactly(
                new ItemLimitRuleResult(items.get(2), 6, Set.of()),
                new ItemLimitRuleResult(items.get(0), 2, Set.of(OrderItemChange.FRAUD_FIXED, OrderItemChange.COUNT)),
                new ItemLimitRuleResult(items.get(1), 0, Set.of(OrderItemChange.FRAUD_FIXED, OrderItemChange.MISSING))
            );
    }
}
