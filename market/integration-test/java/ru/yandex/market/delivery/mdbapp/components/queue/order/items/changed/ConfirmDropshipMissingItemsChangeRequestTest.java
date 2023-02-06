package ru.yandex.market.delivery.mdbapp.components.queue.order.items.changed;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsStrategyDto;
import ru.yandex.market.checkout.checkouter.order.item.MissingItemsStrategyType;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.ChangedItemDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.changedbychangerequest.OrderChangedByPartnerChangeRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.changedbychangerequest.OrderChangedByPartnerChangeRequestQueueConsumer;
import ru.yandex.market.delivery.mdbapp.components.service.PartnerExternalParamsService;
import ru.yandex.market.delivery.mdbapp.integration.converter.OrderToLomWaybillOrderConverter;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderItemsRequest;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.change.ConfirmOrderChangedByPartnerRequest;
import ru.yandex.market.logistics.lom.model.enums.CargoType;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.ItemUnitOperationType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus.INFO_RECEIVED;
import static ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus.REJECTED;

public class ConfirmDropshipMissingItemsChangeRequestTest extends AllMockContextualTest {
    private static final Set<OptionalOrderPart> OPTIONAL_ORDER_PARTS = EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS);
    private static final long ORDER_ID = 1L;
    private static final long SHOP_ID = 1000L;
    private static final long LOM_ORDER_ID = 42L;
    private static final String BARCODE = "barcode";
    private static final BigDecimal ITEM_PRICE = BigDecimal.valueOf(123.45);
    private static final BigDecimal ITEM_ASSESSED_VALUE = BigDecimal.valueOf(235.65);
    private static final long ITEM_ID = 1234L;
    private static final long VENDOR_ID = 123L;
    private static final String ARTICLE = "12345";

    @Autowired
    private OrderChangedByPartnerChangeRequestQueueConsumer consumer;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private OrderToLomWaybillOrderConverter orderToLomWaybillOrderConverter;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private ObjectMapper commonJsonMapper;

    @Autowired
    private PartnerExternalParamsService partnerExternalParamsService;

    @AfterEach
    void tearDown() {
        verify(lomClient).getOrder(eq(LOM_ORDER_ID), eq(OPTIONAL_ORDER_PARTS), eq(false));
        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Успешное подтверждение обработки заявки на удаление товаров чекаутером")
    void confirmChangeRequest() {
        int removedItemCount = 1;
        when(lomClient.getOrder(eq(LOM_ORDER_ID), eq(OPTIONAL_ORDER_PARTS), eq(false)))
            .thenReturn(Optional.of(getLomOrder(removedItemCount)));
        doReturn(getOrder(removedItemCount)).when(checkouterAPI)
            .getOrder(eq(ORDER_ID), eq(ClientRole.SYSTEM), isNull());
        doReturn(new MissingItemsStrategyDto(MissingItemsStrategyType.NOTHING_CHANGED, null)).
            when(checkouterAPI).getMissingStrategy(anyLong(), any(MissingItemsNotification.class));
        doReturn(null).when(checkouterAPI).getOrderItemsRemovalPermissions(eq(ORDER_ID));

        TaskExecutionResult result = executeTask();

        assertEquals(TaskExecutionResult.finish(), result);

        verify(lomClient).processChangeOrderRequest(1, getRequest(removedItemCount));
    }

    @Test
    @DisplayName("Нет активной заявки на удаление товаров в LOM")
    void skipRejectedChangeRequest() {
        when(lomClient.getOrder(eq(LOM_ORDER_ID), eq(OPTIONAL_ORDER_PARTS), eq(false)))
            .thenReturn(Optional.of(getLomOrderWithRejectedChangeRequest()));

        TaskExecutionResult result = executeTask();

        assertEquals(TaskExecutionResult.finish(), result);

        verify(lomClient, never()).processChangeOrderRequest(anyLong(), any(ConfirmOrderChangedByPartnerRequest.class));
    }

    @Test
    @DisplayName("Если новое количество совпадает с чекаутером, то не запрашиваем стратегию")
    void removedItemsNotSentToCheckouter() {
        int removedItemCount = 0;
        when(lomClient.getOrder(eq(LOM_ORDER_ID), eq(OPTIONAL_ORDER_PARTS), eq(false)))
            .thenReturn(Optional.of(getLomOrder(removedItemCount)));

        doReturn(getOrder(removedItemCount)).when(checkouterAPI)
            .getOrder(eq(ORDER_ID), eq(ClientRole.SYSTEM), isNull());

        doReturn(null).when(checkouterAPI).getOrderItemsRemovalPermissions(eq(ORDER_ID));

        TaskExecutionResult result = executeTask();

        assertEquals(TaskExecutionResult.finish(), result);


        verify(checkouterAPI, never()).getMissingStrategy(anyLong(), any(MissingItemsNotification.class));

        verify(lomClient).processChangeOrderRequest(1, getRequest(removedItemCount));
    }

    @Nonnull
    private TaskExecutionResult executeTask() {
        return consumer.execute(new Task<>(
            new QueueShardId("id"),
            new OrderChangedByPartnerChangeRequestDto(1L, LOM_ORDER_ID, ORDER_ID),
            1L,
            ZonedDateTime.now(),
            null,
            null
        ));
    }

    @Nonnull
    private ConfirmOrderChangedByPartnerRequest getRequest(int removedItemCount) {
        List<ItemDto> items;
        if (removedItemCount == 0) {
            items = List.of(getItemDto(1, 1));
        } else {
            items = List.of(
                getItemDto(0, removedItemCount),
                getItemDto(1, 1)
            );
        }
        var order = getOrder(removedItemCount);
        return new ConfirmOrderChangedByPartnerRequest()
            .setUpdateOrderItemsRequest(new UpdateOrderItemsRequest(
                null,
                BARCODE,
                orderToLomWaybillOrderConverter.convertCost(
                    order,
                    orderToLomWaybillOrderConverter.calculateItemsTotalAssessedValue(items),
                    partnerExternalParamsService.orderAssessedValueTotalCheck(order)
                ),
                items
            ));
    }

    @Nonnull
    private OrderDto getLomOrder(int removedItemCount) {
        return new OrderDto()
            .setId(LOM_ORDER_ID)
            .setBarcode(BARCODE)
            .setItems(List.of(
                getItemDto(0, removedItemCount),
                getItemDto(1, 1)
            ))
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerType(PartnerType.DROPSHIP)
                    .build()
            ))
            .setChangeOrderRequests(List.of(getLomChangeRequest(INFO_RECEIVED, removedItemCount)));
    }

    @Nonnull
    private OrderDto getLomOrderWithRejectedChangeRequest() {
        return getLomOrder(1).setChangeOrderRequests(List.of(getLomChangeRequest(REJECTED, 1)));
    }

    private ChangeOrderRequestDto getLomChangeRequest(ChangeOrderRequestStatus status, int count) {
        return ChangeOrderRequestDto.builder()
            .id(1L)
            .requestType(ChangeOrderRequestType.ORDER_CHANGED_BY_PARTNER)
            .status(status)
            .payloads(Set.of(
                ChangeOrderRequestPayloadDto.builder()
                    .status(INFO_RECEIVED)
                    .payload(commonJsonMapper.valueToTree(List.of(
                        new ChangedItemDto(VENDOR_ID, ARTICLE, count, null),
                        new ChangedItemDto(VENDOR_ID + 1, ARTICLE, 1, null)
                    )))
                    .build()
            ))
            .build();
    }

    @Nonnull
    private ItemDto getItemDto(int id, int count) {
        return ItemDto.builder()
            .vendorId(VENDOR_ID + id)
            .article(ARTICLE)
            .price(buildMonetaryDto(ITEM_PRICE))
            .assessedValue(buildMonetaryDto(ITEM_ASSESSED_VALUE))
            .count(count)
            .dimensions(KorobyteDto.builder().build())
            .boxes(
                List.of(
                    OrderItemBoxDto.builder()
                        .storageUnitExternalIds(Set.of("generated-root-unit-external-id-1"))
                        .dimensions(KorobyteDto.builder().build())
                        .build()
                )
            )
            .cargoTypes(Set.of(CargoType.UNKNOWN))
            .itemUnitOperationType(ItemUnitOperationType.FULFILLMENT)
            .build();
    }

    @Nonnull
    private MonetaryDto buildMonetaryDto(BigDecimal value) {
        return MonetaryDto.builder()
            .value(value)
            .currency("RUB")
            .exchangeRate(BigDecimal.ONE)
            .build();
    }

    @Nonnull
    private Order getOrder(int removedItemCount) {
        Delivery delivery = new Delivery();
        delivery.setPrice(BigDecimal.TEN);
        delivery.setParcels(List.of(getParcel(removedItemCount)));

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setShopId(SHOP_ID);
        order.setPaymentMethod(ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CASH_ON_DELIVERY);
        order.setDelivery(delivery);
        order.setDropship(true);

        List<OrderItem> items;
        if (removedItemCount == 0) {
            items = List.of(getOrderItem(1, 1));
        } else {
            items = List.of(
                getOrderItem(0, removedItemCount),
                getOrderItem(1, 1)
            );
        }
        order.setItems(items);
        return order;
    }

    @Nonnull
    private Parcel getParcel(int removedItemCount) {
        Parcel parcel = new Parcel();
        parcel.setId(1L);
        parcel.setWeight(1L);
        parcel.setParcelItems(List.of(
            new ParcelItem(ITEM_ID, removedItemCount),
            new ParcelItem(ITEM_ID + 1, 1)
        ));
        return parcel;
    }

    @Nonnull
    private OrderItem getOrderItem(long id, int count) {
        OrderItem orderItem = new OrderItem(
            new FeedOfferId("", id), ITEM_PRICE, count
        );

        orderItem.getPrices().setBuyerPriceBeforeDiscount(ITEM_ASSESSED_VALUE);
        orderItem.setSupplierId(VENDOR_ID + id);
        orderItem.setShopSku(ARTICLE);
        orderItem.setOrderId(ORDER_ID);
        orderItem.setPrice(ITEM_PRICE);
        orderItem.setCount(count);
        orderItem.setId(ITEM_ID + id);
        return orderItem;
    }
}
