package ru.yandex.market.b2b.clients.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.market.b2b.clients.PaymentInvoiceReportType;
import ru.yandex.market.b2b.clients.Randoms;
import ru.yandex.market.b2b.clients.jreport.ReportService;
import ru.yandex.mj.generated.server.model.OrderCustomerDto;
import ru.yandex.mj.generated.server.model.OrderDeliveryDto;
import ru.yandex.mj.generated.server.model.OrderDto;
import ru.yandex.mj.generated.server.model.OrderItemDto;
import ru.yandex.mj.generated.server.model.Vat;

public class PaymentInvoiceReportTest extends AbstractFunctionalTest {

    @Autowired
    ReportService reportService;

    @Test
    @Disabled
    public void generateOneOrderInvoice() throws Exception {
        OrderDto order = new OrderDto()
                .id(Randoms.bigDecimal())
                .customer(
                        new OrderCustomerDto()
                                .id(2L)
                                //.name("Индивидуальный предприниматель Замарин Евгений Анатольевич")
                                .buyer("Иванов Иван Ииванович ибн Хотаб")
                                .phone("+79221131234 доб. 567")
                )
                .items(IntStream.range(1, 7).mapToObj((i) -> item("Айфончик ")).collect(Collectors.toList()));

        OrderDtoWithDate orderWithDate = new OrderDtoWithDate(order);

        byte[] generate = reportService.generate(PaymentInvoiceReportType.INSTANCE, orderWithDate);

        File file = new File("/tmp/my.pdf");
        try (FileOutputStream os = new FileOutputStream(file)) {
            os.write(generate);
        }
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

    @Test
    @Disabled
    public void generateMultiOrderInvoice() throws Exception {
        BigDecimal order1Id = BigDecimal.valueOf(1);
        OrderDto order1 = new OrderDto()
                .id(order1Id)
                .customer(
                        new OrderCustomerDto()
                                .id(2L)
                                .buyer("Иванов Иван Ииванович ибн Хотаб")
                                .phone("+79221131234 доб. 567")
                )
                .items(IntStream.range(1, 7).mapToObj((i) -> item("Товар из заказа №" + order1Id + " ")).collect(Collectors.toList()))
                .delivery(new OrderDeliveryDto().total(Randoms.bigDecimal(100L, 0)).vat(Vat.VAT_20));

        BigDecimal order2Id = BigDecimal.valueOf(2);
        OrderDto order2 = new OrderDto()
                .id(order2Id)
                .customer(
                        new OrderCustomerDto()
                                .id(2L)
                                .buyer("Иванов Иван Ииванович ибн Хотаб")
                                .phone("+79221131234 доб. 567")
                )
                .items(IntStream.range(1, 7).mapToObj((i) -> item("Товар из заказа №" + order2Id + " ")).collect(Collectors.toList()))
                .delivery(new OrderDeliveryDto().total(Randoms.bigDecimal(100L, 0)).vat(Vat.VAT_20));

        List<OrderDto> multiOrder = List.of(order1, order2);

        OrdersDtoWithDate orders = new OrdersDtoWithDate(multiOrder, BigDecimal.valueOf(1234));

        byte[] generate = reportService.generate(PaymentInvoiceReportType.INSTANCE_MULTI, orders);

        File file = new File("/tmp/multi-order.pdf");
        try (FileOutputStream os = new FileOutputStream(file)) {
            os.write(generate);
        }
    }
}
