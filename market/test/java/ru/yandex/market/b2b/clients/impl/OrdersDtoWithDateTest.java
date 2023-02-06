package ru.yandex.market.b2b.clients.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2b.clients.Randoms;
import ru.yandex.market.b2b.clients.common.InternalException;
import ru.yandex.mj.generated.server.model.OrderCustomerDto;
import ru.yandex.mj.generated.server.model.OrderDeliveryDto;
import ru.yandex.mj.generated.server.model.OrderDto;
import ru.yandex.mj.generated.server.model.OrderItemDto;
import ru.yandex.mj.generated.server.model.Vat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OrdersDtoWithDateTest {

    private static final BigDecimal INVOICE_NUMBER = BigDecimal.valueOf(111);

    @Test
    public void getCustomer_findsFirst() {
        List<OrderDto> orders = makeTwoOrder();

        OrdersDtoWithDate ordersWithDate = new OrdersDtoWithDate(orders, INVOICE_NUMBER);

        assertEquals(orders.get(0).getCustomer(), ordersWithDate.getCustomer());
    }

    private List<OrderDto> makeTwoOrder() {
        OrderDto order1 = new OrderDto()
                .id(Randoms.bigDecimal())
                .customer(customer())
                .items(IntStream.range(1, 7).mapToObj((i) -> item("Товар из заказа №1 ")).collect(Collectors.toList()))
                .delivery(new OrderDeliveryDto().total(BigDecimal.ONE).vat(Vat.VAT_20));

        OrderDto order2 = new OrderDto()
                .id(Randoms.bigDecimal())
                .customer(customer())
                .items(IntStream.range(1, 7).mapToObj((i) -> item("Товар из заказа №2 ")).collect(Collectors.toList()))
                .delivery(new OrderDeliveryDto().total(BigDecimal.TEN).vat(Vat.VAT_20));
        return List.of(order1, order2);
    }

    private static OrderItemDto item(String productName) {
        BigDecimal price = Randoms.bigDecimal(1_000_000, 2);
        BigDecimal count = Randoms.bigDecimal(100, 0);
        return new OrderItemDto()
                .id(Randoms.bigDecimal())
                .count(count.intValue() + 1)
                .name(productName + Randoms.bigDecimal())
                .price(price)
                .total(price.multiply(new BigDecimal(count.longValue() + 1)))
                .vat(Vat.VAT_20);
    }

    private static OrderCustomerDto customer() {
        return new OrderCustomerDto()
                .id(2L)
                .buyer("Иванов Иван Ииванович ибн Хотаб")
                .phone("+79221131234 доб. 567");
    }

    @Test
    public void getCustomer_ifAllNullThenException() {
        List<OrderDto> orders = makeTwoOrder();
        orders.forEach(order -> order.setCustomer(null));

        assertThrows(InternalException.class, () -> new OrdersDtoWithDate(orders, INVOICE_NUMBER));
    }

    @Test
    public void getItems() {
        List<OrderDto> orders = makeTwoOrder();

        OrdersDtoWithDate ordersWithDate = new OrdersDtoWithDate(orders, INVOICE_NUMBER);

        Assertions.assertThat(ordersWithDate.getItems())
                .containsAll(orders.get(0).getItems());
        Assertions.assertThat(ordersWithDate.getItems())
                .containsAll(orders.get(1).getItems());
    }

    @Test
    public void getDeliveries() {
        List<OrderDto> orders = makeTwoOrder();

        OrdersDtoWithDate ordersWithDate = new OrdersDtoWithDate(orders, INVOICE_NUMBER);

        Assertions.assertThat(ordersWithDate.getDeliveries())
                .contains(orders.get(0).getDelivery(), orders.get(1).getDelivery());
    }

    @Test
    public void getCurrentDate() {
        List<OrderDto> orders = makeTwoOrder();

        OrdersDtoWithDate ordersWithDate = new OrdersDtoWithDate(orders, INVOICE_NUMBER);

        assertNotNull(ordersWithDate.getCurrentDate());
    }

    @Test
    public void getPaymentInvoiceNumber() {
        List<OrderDto> orders = makeTwoOrder();

        OrdersDtoWithDate ordersWithDate = new OrdersDtoWithDate(orders, INVOICE_NUMBER);

        assertEquals(INVOICE_NUMBER.toString(), ordersWithDate.getPaymentInvoiceNumber());
    }

    @Test
    public void getOrders() {
        List<OrderDto> orders = makeTwoOrder();

        OrdersDtoWithDate ordersWithDate = new OrdersDtoWithDate(orders, INVOICE_NUMBER);

        Assertions.assertThat(ordersWithDate.getOrders())
                .containsAll(orders);
    }

    @Test
    public void getOrderIds() {
        List<OrderDto> orders = makeTwoOrder();

        OrdersDtoWithDate ordersWithDate = new OrdersDtoWithDate(orders, INVOICE_NUMBER);

        Assertions.assertThat(ordersWithDate.getOrderIds())
                .contains(
                        orders.get(0).getId().toString(),
                        orders.get(1).getId().toString()
                );
    }
}
