package ru.yandex.market.antifraud.orders.service;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.test.providers.OrderBuyerRequestProvider;
import ru.yandex.market.antifraud.orders.test.providers.OrderItemRequestProvider;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.CartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderDeliveryRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderDeliveryType;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.antifraud.orders.entity.AntifraudAction.CANCEL_ORDER;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 20.05.19
 */
public class AntifraudLogFormatterTest {

    private final AntifraudLogFormatter antifraudLogFormatter = new AntifraudLogFormatter();

    @Test
    public void simpleOrderLogTest() {
        String expected = "tskv\tdatetime=[datetime]\t" +
                "timestamp={ts}\t" +
                "antifraudActions=[CANCEL_ORDER]\t" +
                "detectorName=Blacklist\torderId=null\t" +
                "buyer=\tdelivery=\titems=\treason=Reason";
        String actual = antifraudLogFormatter.format(
                MultiCartRequestDto.builder().build(),
                "Blacklist",
                new TreeSet<>(asList(CANCEL_ORDER)),
                "Reason"
        )
                .replaceFirst("\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2} \\+\\d{4}\\]", "[datetime]")
                .replaceFirst("timestamp=\\d{10}", "timestamp={ts}");
        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void complexOrderLogTest() {
        OrderItemRequestDto orderItem1 = OrderItemRequestProvider.getPreparedBuilder()
                .id(7013020L)
                .offerId("232666301")
                .count(2)
                .feedId(546058L)
                .msku(1111111111L)
                .build();
        OrderItemRequestDto orderItem2 = OrderItemRequestProvider.getPreparedBuilder()
                .id(7013021L)
                .offerId("232666302")
                .count(3)
                .feedId(546059L)
                .msku(22222222222L)
                .shopSku("test-order-2")
                .build();

        OrderDeliveryRequestDto delivery = new OrderDeliveryRequestDto(100500L, "oc1337",
                OrderDeliveryType.PICKUP, null);
        List<OrderItemRequestDto> items = Arrays.asList(orderItem1, orderItem2);
        var order = MultiCartRequestDto.builder()
            .carts(List.of(
                CartRequestDto.builder()
                    .items(items)
                    .delivery(delivery)
                    .fulfilment(true)
                    .build()
            ))
            .buyer(OrderBuyerRequestProvider.getOrderBuyerRequest())
            .build();

        String expected1 = "tskv\tdatetime=[datetime]\ttimestamp={ts}\tantifraudActions=[CANCEL_ORDER]\t"
                + "detectorName=Blacklist\torderId=null\tbuyer=[uid=359953025, uuid=100500, "
                + "email=a@b.com, phone=71234567891]\tdelivery=[[outletId=100500, outletCode=oc1337]]\t"
                + "items=[[item_id=7013020, feed_id=546058, offer_id=232666301, ssku=shop_sku_test, msku=1111111111, "
                + "price=250]x2 [item_id=7013021, feed_id=546059, offer_id=232666302, ssku=test-order-2, "
                + "msku=22222222222, price=250]x3 ]\treason=Reason";

        String actual = antifraudLogFormatter.format(
                order,
                "Blacklist",
                singleton(CANCEL_ORDER),
                "Reason"
        )
                .replaceFirst("\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2} \\+\\d{4}\\]", "[datetime]")
                .replaceFirst("timestamp=\\d{10}", "timestamp={ts}");
        //Костыль нужен, так как в order не гарантируется порядок orderItems
        assertThat(actual).isEqualTo(expected1);
    }
}
