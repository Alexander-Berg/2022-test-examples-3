package ru.yandex.market.antifraud.orders.web.entity;

import java.io.IOException;
import java.util.HashSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.antifraud.orders.web.entity.OrderItemChange.COUNT;
import static ru.yandex.market.antifraud.orders.web.entity.OrderItemChange.FRAUD_FIXED;
import static ru.yandex.market.antifraud.orders.web.entity.OrderItemChange.PRICE;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 05.09.2019
 */
public class OrderVerdictTest {
    @Test
    public void serializationTest() throws IOException {
        ObjectMapper mapper = AntifraudJsonUtil.OBJECT_MAPPER;

        OrderItemResponseDto orderItemResponseDto1 = OrderItemResponseDto.builder()
                .id(1L)
                .feedId(1L)
                .offerId("offer1")
                .count(3)
                .changes(new HashSet<>(asList(FRAUD_FIXED, COUNT)))
                .build();

        OrderItemResponseDto orderItemResponseDto2 = OrderItemResponseDto.builder()
                .id(2L)
                .feedId(2L)
                .offerId("offer2")
                .count(5)
                .changes(new HashSet<>(asList(FRAUD_FIXED, PRICE)))
                .build();

        OrderResponseDto orderResponseDto = new OrderResponseDto(ImmutableList.of(orderItemResponseDto1,
                orderItemResponseDto2));

        OrderVerdict orderVerdict = OrderVerdict.builder()
                .checkResults(ImmutableSet.of(
                        new AntifraudCheckResult(AntifraudAction.CANCEL_ORDER, "Cancel", "Reason1"),
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "Change", "Reason2")
                ))
                .fixedOrder(orderResponseDto)
                .isDegradation(false)
                .build();
        String json = mapper.writeValueAsString(orderVerdict);
        OrderVerdict deserialized = mapper.readValue(json, OrderVerdict.class);
        assertThat(deserialized).isEqualTo(orderVerdict);
    }
}
