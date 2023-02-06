package ru.yandex.market.rg.asyncreport.shipment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.client.model.NamedEntity;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentDto;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatus;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatusChange;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentWarehouseDto;
import ru.yandex.market.rg.asyncreport.ReportFunctionalTest;

abstract class AbstractFirstMileShipmentReportGeneratorTest extends ReportFunctionalTest {

    private static final Date CREATION_DATE = DateUtil.asDate(LocalDateTime.of(
            2020, 11, 27, 13, 30, 15));
    private static final Date STATUS_UPDATE_TIME = DateUtil.asDate(LocalDateTime.of(
            2020, 12, 27, 15, 30, 20));
    private static final LocalDateTime SHIPMENT_DATE =
            LocalDateTime.of(2021, 1, 2, 10, 30, 15);

    @Autowired
    protected CheckouterClient checkouterClient;

    @Autowired
    protected NesuClient nesuClient;


    protected void mockNesuClient() {
        Mockito.when(nesuClient.getShipment(Mockito.eq(908765487L), Mockito.eq(101L), Mockito.eq(123456L)))
                .thenReturn(
                        PartnerShipmentDto.builder()
                                .id(123456L)
                                .number("8919")
                                .planIntervalFrom(LocalDateTime.parse("2021-01-08T10:15:00"))
                                .planIntervalTo(LocalDateTime.parse("2021-01-26T10:15:00"))
                                .shipmentType(ShipmentType.WITHDRAW)
                                .warehouseFrom(
                                        PartnerShipmentWarehouseDto.builder()
                                                .id(48123L)
                                                .name("Мой лучший склад")
                                                .address("Мой лучший адрес")
                                                .build()
                                )
                                .partner(NamedEntity.builder().id(12348L).name("Мой лучший перевозчик").build())
                                .currentStatus(PartnerShipmentStatusChange.builder()
                                        .code(PartnerShipmentStatus.OUTBOUND_CONFIRMED)
                                        .datetime(LocalDateTime.parse("2021-01-16T10:15:00").toInstant(ZoneOffset.UTC))
                                        .description("Для отгрузки найден лучший перевозчик в Галактике")
                                        .build())
                                .orderIds(List.of(56L, 7L))
                                .confirmedOrderIds(List.of(56L))
                                .build());
    }

    protected void mockCheckouterClient() {
        final OrderItem orderItem1 = new OrderItem();
        orderItem1.setId(11L);
        orderItem1.setShopSku("ssku1");
        orderItem1.setFeedOfferId(FeedOfferId.from(11L, "shop-offer-id-1"));
        orderItem1.setOfferName("Самый лучший товар");
        orderItem1.setBuyerPrice(BigDecimal.ONE);
        orderItem1.setCount(1);

        final OrderItem orderItem2 = new OrderItem();
        orderItem2.setId(22L);
        orderItem2.setShopSku("ssku2");
        orderItem2.setFeedOfferId(FeedOfferId.from(22L, "shop-offer-id-2"));
        orderItem2.setOfferName("Не самый лучший товар");
        orderItem2.setBuyerPrice(BigDecimal.TEN);
        orderItem2.setCount(1000);

        final Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(SHIPMENT_DATE);
        parcel.setBoxes(List.of(new ParcelBox(), new ParcelBox()));

        final Delivery delivery = new Delivery();
        delivery.setParcels(List.of(parcel));

        final Order order = new Order(false, false, BigDecimal.valueOf(1000), BigDecimal.ZERO,
                BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), BigDecimal.valueOf(10000));
        order.setId(56L);
        order.setShopId(101L);
        order.setStatus(OrderStatus.PENDING);
        order.setSubstatus(OrderSubstatus.PENDING_EXPIRED);
        order.setShopOrderId("myBestOrderEver12321");
        order.setCreationDate(CREATION_DATE);
        order.setStatusUpdateDate(STATUS_UPDATE_TIME);
        order.setItemsTotal(BigDecimal.valueOf(1036L));
        order.setItems(List.of(orderItem1, orderItem2));
        order.setDelivery(delivery);

        Mockito.when(checkouterClient.getOrders(Mockito.any(), Mockito.any()))
                .thenReturn(new PagedOrders(List.of(order), Pager.atPage(1, 30).setPagesCount(1)));
    }

}
