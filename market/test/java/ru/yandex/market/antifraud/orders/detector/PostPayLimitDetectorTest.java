package ru.yandex.market.antifraud.orders.detector;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.util.concurrent.FutureValueHolder;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.CartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.OrderItem;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author dzvyagin
 */
public class PostPayLimitDetectorTest {

    private final PostPayLimitDetector detector = new PostPayLimitDetector();


    @Test
    public void noTriggering() {
        OrderDataContainer cntxt = OrderDataContainer.builder()
            .orderRequest(MultiCartRequestDto.builder()
                .buyer(OrderBuyerRequestDto.builder()
                    .uid(123L)
                    .uuid("123")
                    .build())
                .carts(List.of(
                    CartRequestDto.builder()
                        .items(List.of(
                            OrderItemRequestDto.builder()
                                .count(2)
                                .price(BigDecimal.valueOf(12000))
                                .build()
                        ))
                        .build()))
                .build())
            .lastOrdersFuture(new FutureValueHolder<>(
                List.of(
                    Order.newBuilder()
                        .setKeyUid(Uid.newBuilder()
                            .setType(UidType.PUID)
                            .setStringValue("123")
                            .build())
                        .addItems(OrderItem.newBuilder()
                            .setId(1)
                            .setCount(1)
                            .setPrice(12300)
                            .build())
                        .setStatus("DELIVERY")
                        .build(),
                    Order.newBuilder()
                        .setKeyUid(Uid.newBuilder()
                            .setType(UidType.PUID)
                            .setStringValue("123")
                            .build())
                        .addItems(OrderItem.newBuilder()
                            .setId(1)
                            .setCount(1)
                            .setPrice(123000)
                            .build())
                        .setStatus("DELIVERED")
                        .build(),
                    Order.newBuilder()
                        .setKeyUid(Uid.newBuilder()
                            .setType(UidType.PUID)
                            .setStringValue("123")
                            .build())
                        .addItems(OrderItem.newBuilder()
                            .setId(1)
                            .setCount(1)
                            .setPrice(123000)
                            .build())
                        .setStatus("DELIVERY")
                        .build(),
                    Order.newBuilder()
                        .setKeyUid(Uid.newBuilder()
                            .setType(UidType.PUID)
                            .setStringValue("123")
                            .build())
                        .addItems(OrderItem.newBuilder()
                            .setId(1)
                            .setCount(1)
                            .setPrice(123000)
                            .build())
                        .setStatus("DELIVERY")
                        .build()
                )
            ))
            .gluedIdsFuture(new FutureValueHolder<>(
                Set.of()
            ))
            .build();
        OrderDetectorResult odr = detector.detectFraud(cntxt);
        assertThat(odr.isFraud()).isFalse();
    }


    @Test
    public void userLimit() {
        OrderDataContainer cntxt = OrderDataContainer.builder()
            .orderRequest(MultiCartRequestDto.builder()
                .buyer(OrderBuyerRequestDto.builder()
                    .uid(123L)
                    .uuid("123")
                    .build())
                .carts(List.of(
                    CartRequestDto.builder()
                        .items(List.of(
                            OrderItemRequestDto.builder()
                                .count(5)
                                .price(BigDecimal.valueOf(12000))
                                .build()
                        ))
                        .build()))
                .build())
                .lastOrdersFuture(new FutureValueHolder<>(
                        List.of(
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("123")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(1)
                                                .setPrice(12300)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setId(123L)
                                        .setStatus("DELIVERY")
                                        .build(),
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("123")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(1)
                                                .setPrice(12300)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setId(124L)
                                        .setStatus("DELIVERY")
                                        .build(),
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("123")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(1)
                                                .setPrice(12300)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setId(125L)
                                        .setStatus("DELIVERY")
                                        .build()
                                , Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("123")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(1)
                                                .setPrice(12300)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setId(126L)
                                        .setStatus("DELIVERY")
                                        .build()
                        )
                ))
                .gluedIdsFuture(new FutureValueHolder<>(
                        Set.of()
                ))
                .build();
        OrderDetectorResult odr = detector.detectFraud(cntxt);
        assertThat(odr.isFraud()).isTrue();
        assertThat(odr.getActions()).contains(AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void glueLimit() {
        OrderDataContainer cntxt = OrderDataContainer.builder()
                .orderRequest(MultiCartRequestDto.builder()
                    .buyer(OrderBuyerRequestDto.builder()
                        .uid(123L)
                        .uuid("123")
                        .build())
                    .carts(List.of(
                        CartRequestDto.builder()
                        .items(List.of(
                                OrderItemRequestDto.builder()
                                        .count(1)
                                        .price(BigDecimal.valueOf(12000))
                                        .build()
                        ))
                        .build()))
                    .build())
                .lastOrdersFuture(new FutureValueHolder<>(
                        List.of(
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("124")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(4)
                                                .setPrice(10000)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setStatus("DELIVERY")
                                        .setId(123L)
                                        .build(),
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("125")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(4)
                                                .setPrice(10000)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setStatus("DELIVERY")
                                        .setId(124L)
                                        .build(),
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("126")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(4)
                                                .setPrice(10000)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setStatus("DELIVERY")
                                        .setId(125L)
                                        .build(),
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("127")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(4)
                                                .setPrice(10000)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setStatus("DELIVERY")
                                        .setId(126L)
                                        .build(),
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("128")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(4)
                                                .setPrice(10000)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setStatus("DELIVERY")
                                        .setId(127L)
                                        .build()
                        )
                ))
                .gluedIdsFuture(new FutureValueHolder<>(
                        Set.of(
                                MarketUserId.fromUid(124L),
                                MarketUserId.fromUid(123L)
                        )
                ))
                .build();
        OrderDetectorResult odr = detector.detectFraud(cntxt);
        assertThat(odr.isFraud()).isTrue();
        assertThat(odr.getActions()).contains(AntifraudAction.PREPAID_ONLY);
    }

    @Test
    public void checkMultiorder() {
        OrderDataContainer cntxt = OrderDataContainer.builder()
                .orderRequest(MultiCartRequestDto.builder()
                    .buyer(OrderBuyerRequestDto.builder()
                        .uid(123L)
                        .uuid("123")
                        .build())
                    .carts(List.of(
                        CartRequestDto.builder()
                        .items(List
                            .of(
                                OrderItemRequestDto.builder()
                                        .count(1)
                                        .price(BigDecimal.valueOf(12000))
                                        .build()
                        ))
                        .build()))
                    .build())
                .lastOrdersFuture(new FutureValueHolder<>(
                        List.of(
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("124")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(4)
                                                .setPrice(10000)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setStatus("DELIVERY")
                                        .setId(123L)
                                        .build(),
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("125")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(4)
                                                .setPrice(10000)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setStatus("DELIVERY")
                                        .setMultiOrderId("1")
                                        .setId(124L)
                                        .build(),
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("126")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(4)
                                                .setPrice(10000)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setStatus("DELIVERY")
                                        .setMultiOrderId("1")
                                        .setId(125L)
                                        .build(),
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("127")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(4)
                                                .setPrice(10000)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setStatus("DELIVERY")
                                        .setMultiOrderId("1")
                                        .setId(126L)
                                        .build(),
                                Order.newBuilder()
                                        .setKeyUid(Uid.newBuilder()
                                                .setType(UidType.PUID)
                                                .setStringValue("128")
                                                .build())
                                        .addItems(OrderItem.newBuilder()
                                                .setId(1)
                                                .setCount(4)
                                                .setPrice(10000)
                                                .build())
                                        .setRgb(RGBType.BLUE)
                                        .setStatus("DELIVERY")
                                        .setMultiOrderId("1")
                                        .setId(127L)
                                        .build()
                        )
                ))
                .gluedIdsFuture(new FutureValueHolder<>(
                        Set.of(
                                MarketUserId.fromUid(124L),
                                MarketUserId.fromUid(123L)
                        )
                ))
                .build();
        OrderDetectorResult odr = detector.detectFraud(cntxt);
        assertThat(odr.isFraud()).isFalse();
        assertThat(odr.getActions()).contains(AntifraudAction.NO_ACTION);
    }
}
