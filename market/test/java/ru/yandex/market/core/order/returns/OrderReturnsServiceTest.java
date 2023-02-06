package ru.yandex.market.core.order.returns;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.ReturnSubreason;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.order.returns.model.OrderReturn;
import ru.yandex.market.core.order.returns.model.OrderReturnExtendedStatus;
import ru.yandex.market.core.order.returns.model.OrderReturnItem;
import ru.yandex.market.core.order.returns.model.OrderReturnStatus;
import ru.yandex.market.core.order.returns.model.OrderReturnsFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для {@link OrderReturnsService}
 * <p>
 * дату в csv h2 конвертирует из utc в ZoneId.systemDefault() [Asia/Vladivostok]
 */
class OrderReturnsServiceTest extends FunctionalTest {

    @Autowired
    OrderReturnsService orderReturnsService;

    @Test
    @DbUnitDataSet(before = "OrderReturnsServiceTest.getReturnsByPartnerOrder.before.csv")
    void getReturnsPartnerOrder() {
        var expected = List.of(new OrderReturn.OrderReturnBuilder()
                .setId(18802L)
                .setOrderId(6512371L)
                .setStatus(OrderReturnStatus.REFUND_IN_PROGRESS)
                .setCreatedAt(Instant.parse("2019-05-16T09:32:56.888Z"))
                .setUpdatedAt(Instant.parse("2019-05-28T14:14:23.939Z"))
                .setStatusUpdatedAt(Instant.parse("2019-05-28T14:14:23.939Z"))
                .setApplicationUrl("http://application.url")
                .setProcessingDetails("Return 18802expired")
                .setFastReturn(false)
                .setPartnerId(12345L)
                .setExtendedStatus(OrderReturnExtendedStatus.COMPLETED)
                .setItems(List.of(new OrderReturnItem.OrderReturnItemBuilder()
                        .setId(28640)
                        .setReturnId(18802)
                        .setOrderId(6512371)
                        .setOrderItemId(9568212)
                        .setCount(10)
                        .setSupplierCompensation(BigDecimal.valueOf(777.0))
                        .setReturnReason("нет возможности быстро печатать")
                        .setReturnReasonType(ReturnReasonType.DO_NOT_FIT)
                        .setReturnSubreason(ReturnSubreason.NOT_WORKING)
                        .setPicturesUrls(List.of("single url"))
                        .build()))
                .build()
        );

        var filter = OrderReturnsFilter.builder()
                .setSupplierId(12345L)
                .setOrderIds(List.of(6512371L))
                .setStatuses(OrderReturnStatus.APPROVED_STATUSES)
                .build();
        assertEquals(expected, orderReturnsService.getReturnsByFilter(filter));
    }

    @Test
    @DbUnitDataSet(before = "OrderReturnsServiceTest.getReturnIdsByPartnerOrderIds.before.csv")
    void getReturnIdsByOderIds() {
        var expected = List.of(
                new OrderReturn.OrderReturnBuilder()
                        .setId(18803)
                        .setOrderId(6512371)
                        .setStatus(OrderReturnStatus.REFUND_IN_PROGRESS)
                        .setCreatedAt(Instant.parse("2019-05-16T09:32:56.888Z"))
                        .setUpdatedAt(Instant.parse("2019-05-28T14:14:23.939Z"))
                        .setStatusUpdatedAt(Instant.parse("2019-05-28T14:14:23.939Z"))
                        .setApplicationUrl(null)
                        .setProcessingDetails("Return 18802expired")
                        .setFastReturn(false)
                        .setPartnerId(12345L)
                        .setExtendedStatus(OrderReturnExtendedStatus.RETURNED_TO_BAD_STOCK)
                        .setItems(List.of(new OrderReturnItem.OrderReturnItemBuilder()
                                .setId(28641)
                                .setReturnId(18803)
                                .setOrderId(6512371)
                                .setOrderItemId(9568212)
                                .setCount(10)
                                .setSupplierCompensation(BigDecimal.valueOf(777.0))
                                .setReturnReason("нет возможности быстро печатать")
                                .setReturnReasonType(ReturnReasonType.DO_NOT_FIT)
                                .setReturnSubreason(ReturnSubreason.NOT_WORKING)
                                .setPicturesUrls(List.of())
                                .build()))
                        .build(),
                new OrderReturn.OrderReturnBuilder()
                        .setId(18802L)
                        .setOrderId(6512371L)
                        .setStatus(OrderReturnStatus.REFUND_IN_PROGRESS)
                        .setCreatedAt(Instant.parse("2019-05-16T09:32:56.888Z"))
                        .setUpdatedAt(Instant.parse("2019-05-28T14:14:23.939Z"))
                        .setStatusUpdatedAt(Instant.parse("2019-05-28T14:14:23.939Z"))
                        .setApplicationUrl("http://application.url")
                        .setProcessingDetails("Return 18802expired")
                        .setFastReturn(false)
                        .setPartnerId(12345L)
                        .setExtendedStatus(OrderReturnExtendedStatus.COMPLETED)
                        .setItems(List.of(new OrderReturnItem.OrderReturnItemBuilder()
                                .setId(28640)
                                .setReturnId(18802)
                                .setOrderId(6512371)
                                .setOrderItemId(9568212)
                                .setCount(10)
                                .setSupplierCompensation(BigDecimal.valueOf(777.0))
                                .setReturnReason("нет возможности быстро печатать")
                                .setReturnReasonType(ReturnReasonType.DO_NOT_FIT)
                                .setReturnSubreason(ReturnSubreason.NOT_WORKING)
                                .setPicturesUrls(List.of("single url"))
                                .build()))
                        .build(),
                new OrderReturn.OrderReturnBuilder()
                        .setId(13926L)
                        .setOrderId(4954899L)
                        .setStatus(OrderReturnStatus.FAILED)
                        .setCreatedAt(Instant.parse("2019-03-27T12:01:03.814Z"))
                        .setUpdatedAt(Instant.parse("2019-04-12T09:29:48.303Z"))
                        .setStatusUpdatedAt(Instant.parse("2019-04-12T09:29:48.303Z"))
                        .setApplicationUrl("https://market-checkouter-prod.s3.mds.yandex.net/return-application-13926" +
                                ".pdf")
                        .setProcessingDetails("Return 13926expired")
                        .setFastReturn(false)
                        .setPartnerId(12345L)
                        .setExtendedStatus(OrderReturnExtendedStatus.COMPLAIN_PERIOD_EXPIRED)
                        .setItems(List.of(new OrderReturnItem.OrderReturnItemBuilder()
                                .setId(19356)
                                .setReturnId(13926)
                                .setOrderId(4954899)
                                .setOrderItemId(6296828)
                                .setCount(2)
                                .setSupplierCompensation(BigDecimal.valueOf(1111.0))
                                .setReturnReason("Разбитый")
                                .setReturnReasonType(ReturnReasonType.BAD_QUALITY)
                                .setReturnSubreason(ReturnSubreason.UNKNOWN)
                                .setPicturesUrls(List.of())
                                .build()))
                        .build()
        );

        var filter = OrderReturnsFilter.builder()
                .setSupplierId(12345L)
                .setOrderIds(List.of(1L, 4954899L, 6512371L))
                .setStatuses(OrderReturnStatus.APPROVED_STATUSES)
                .build();
        assertEquals(expected, orderReturnsService.getReturnsByFilter(filter));
    }

    @Test
    @DbUnitDataSet(before = "OrderReturnsServiceTest.getReturnIdsByPartnerOrderIds.before.csv")
    void getReturnIdsByOderIdsWithExtendedStatuses() {
        var expected = List.of(
                new OrderReturn.OrderReturnBuilder()
                        .setId(18803)
                        .setOrderId(6512371)
                        .setStatus(OrderReturnStatus.REFUND_IN_PROGRESS)
                        .setCreatedAt(Instant.parse("2019-05-16T09:32:56.888Z"))
                        .setUpdatedAt(Instant.parse("2019-05-28T14:14:23.939Z"))
                        .setStatusUpdatedAt(Instant.parse("2019-05-28T14:14:23.939Z"))
                        .setApplicationUrl(null)
                        .setProcessingDetails("Return 18802expired")
                        .setFastReturn(false)
                        .setPartnerId(12345L)
                        .setExtendedStatus(OrderReturnExtendedStatus.RETURNED_TO_BAD_STOCK)
                        .setItems(List.of(new OrderReturnItem.OrderReturnItemBuilder()
                                .setId(28641)
                                .setReturnId(18803)
                                .setOrderId(6512371)
                                .setOrderItemId(9568212)
                                .setCount(10)
                                .setSupplierCompensation(BigDecimal.valueOf(777.0))
                                .setReturnReason("нет возможности быстро печатать")
                                .setReturnReasonType(ReturnReasonType.DO_NOT_FIT)
                                .setReturnSubreason(ReturnSubreason.NOT_WORKING)
                                .setPicturesUrls(List.of())
                                .build()))
                        .build()
        );

        var filter = OrderReturnsFilter.builder()
                .setSupplierId(12345L)
                .setOrderIds(List.of(1L, 4954899L, 6512371L))
                .setStatuses(OrderReturnStatus.APPROVED_STATUSES)
                .setExtendedStatuses(List.of(OrderReturnExtendedStatus.RETURNED_TO_BAD_STOCK))
                .build();
        assertEquals(expected, orderReturnsService.getReturnsByFilter(filter));
    }

}
