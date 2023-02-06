package ru.yandex.market.delivery.mdbapp.components.queue.order.items.update;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.changerequest.exception.ChangeRequestException;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.PartnerExternalParamsService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.integration.converter.KorobyteConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.OrderToLomWaybillOrderConverter;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderItemsRequest;
import ru.yandex.market.logistics.lom.model.dto.change.ConfirmOrderChangedByPartnerRequest;
import ru.yandex.market.logistics.lom.model.enums.CargoType;
import ru.yandex.market.logistics.lom.model.enums.ItemUnitOperationType;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus.INFO_RECEIVED;
import static ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType.ORDER_CHANGED_BY_PARTNER;
import static ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart.CHANGE_REQUESTS;

public class UpdateLomOrderItemsTest extends AllMockContextualTest {

    public static final long UPDATE_REQUEST_ID = 155L;
    private static final Long ORDER_ID = 1L;
    private static final Long SHOP_ID = 1000L;
    private static final long LOM_ORDER_ID = 42L;
    private static final String LOM_EXTERNAL_ID = "extId";
    private static final String BARCODE = "barcode";
    private static final String TRACK = "track";
    private static final Long PARTNER_ID = 51L;
    private static final BigDecimal ITEM_PRICE = BigDecimal.valueOf(123.45);
    private static final BigDecimal ITEM_ASSESSED_VALUE = BigDecimal.valueOf(235.65);
    private static final BigDecimal DELIVERY_COST = BigDecimal.TEN;
    private static final Long ITEM_ID = 1234L;
    @Autowired
    private ChangeRequestUpdateOrderItemsConsumer consumer;

    @Autowired
    private KorobyteConverter korobyteConverter;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private OrderToLomWaybillOrderConverter orderToLomWaybillOrderConverter;

    @Autowired
    private PartnerExternalParamsService partnerExternalParamsService;

    @MockBean
    private CheckouterOrderService checkouterOrderService;

    /**
     * Успешный сценарий.
     */
    @Test
    public void success() {
        mockSearchOrders(List.of(getLomOrder().setChangeOrderRequests(List.of(ChangeOrderRequestDto.builder()
            .id(1L)
            .requestType(ORDER_CHANGED_BY_PARTNER)
            .status(INFO_RECEIVED)
            .build()
        ))));
        doReturn(getOrder()).when(checkouterOrderService).getOrder(eq(ORDER_ID));

        TaskExecutionResult result = exec(getChangeRequestBuilder());

        assertEquals(TaskExecutionResult.finish(), result);

        var order = getOrder();
        ConfirmOrderChangedByPartnerRequest request = new ConfirmOrderChangedByPartnerRequest()
            .setUpdateOrderItemsRequest(new UpdateOrderItemsRequest(
                null,
                BARCODE,
                orderToLomWaybillOrderConverter.convertCost(
                    order,
                    ITEM_ASSESSED_VALUE,
                    partnerExternalParamsService.orderAssessedValueTotalCheck(order)
                ),
                List.of(getItemDto())
            ));

        verify(lomClient).processChangeOrderRequest(1, request);
    }

    /**
     * Успешный сценарий: имя товара скрыто для службы доставки Экспресс.
     */
    @Test
    public void successHideItemNameForExpressDelivery() {
        mockSearchOrders(List.of(getLomOrder().setChangeOrderRequests(List.of(ChangeOrderRequestDto.builder()
            .id(1L)
            .requestType(ORDER_CHANGED_BY_PARTNER)
            .status(INFO_RECEIVED)
            .build()
        ))));
        doReturn(getOrder(1006360L)).when(checkouterOrderService).getOrder(eq(ORDER_ID));

        TaskExecutionResult result = exec(getChangeRequestBuilder());

        assertEquals(TaskExecutionResult.finish(), result);

        var order = getOrder(1006360L);

        ConfirmOrderChangedByPartnerRequest request = new ConfirmOrderChangedByPartnerRequest()
            .setUpdateOrderItemsRequest(new UpdateOrderItemsRequest(
                null,
                BARCODE,
                orderToLomWaybillOrderConverter.convertCost(
                    order,
                    ITEM_ASSESSED_VALUE,
                    partnerExternalParamsService.orderAssessedValueTotalCheck(order)
                ),
                List.of(getItemDto("Аксессуар, null"))
            ));

        verify(lomClient).processChangeOrderRequest(1, request);

    }


    /**
     * Успешный сценарий.
     */
    @Test
    public void unableToProcessChangeRequest() {
        mockSearchOrders(List.of(getLomOrder()));
        doReturn(getOrder()).when(checkouterOrderService).getOrder(eq(ORDER_ID));
        doThrow(new ChangeRequestException("error"))
            .when(checkouterOrderService)
            .processChangeRequest(eq(ORDER_ID), eq(UPDATE_REQUEST_ID));

        TaskExecutionResult result = exec(getChangeRequestBuilder());

        assertEquals(TaskExecutionResult.finish(), result);

        verify(lomClient, never()).updateOrderItemsIfAllowed(any(UpdateOrderItemsRequest.class));
    }

    /**
     * Ошибка если для заказа чекаутера не найдено ни одного заказа в LOM.
     */
    @Test
    public void failOrderNotFound() {
        mockSearchOrders(List.of());

        TaskExecutionResult result = exec(getChangeRequestBuilder());

        assertEquals(TaskExecutionResult.fail(), result);
        verify(lomClient, never()).processChangeOrderRequest(anyLong(), any(ConfirmOrderChangedByPartnerRequest.class));
    }

    /**
     * Ошибка если список посылок пуст.
     */
    @Test
    public void failEmptyParcel() {
        mockSearchOrders(List.of());

        TaskExecutionResult result = exec(getChangeRequestBuilder().updatedParcels(ImmutableList.of()));

        assertEquals(TaskExecutionResult.fail(), result);
        verify(lomClient, never()).processChangeOrderRequest(anyLong(), any(ConfirmOrderChangedByPartnerRequest.class));
    }

    @Nonnull
    public TaskExecutionResult exec(
        ChangeRequestUpdateOrderItemsDto.ChangeRequestUpdateOrderItemsDtoBuilder changeRequestBuilder
    ) {
        return consumer.execute(new Task<>(
            new QueueShardId("id"),
            changeRequestBuilder.build(),
            1L,
            ZonedDateTime.now(),
            null,
            null
        ));
    }

    @Nonnull
    private ChangeRequestUpdateOrderItemsDto.ChangeRequestUpdateOrderItemsDtoBuilder getChangeRequestBuilder() {
        return ChangeRequestUpdateOrderItemsDto.builder()
            .orderId(ORDER_ID)
            .trackCode(TRACK)
            .updateRequestId(UPDATE_REQUEST_ID)
            .partnerId(PARTNER_ID)
            .updatedItems(ImmutableSet.of(getOrderItem()))
            .updatedParcels(ImmutableSet.of(getParcel()));
    }

    @Nonnull
    public OrderItem getOrderItem() {
        OrderItem orderItem = new OrderItem(
            null,
            null,
            null,
            null,
            ITEM_ASSESSED_VALUE,
            null,
            null,
            null
        );
        orderItem.setOrderId(ORDER_ID);
        orderItem.setPrice(ITEM_PRICE);
        orderItem.setCount(1);
        orderItem.setId(ITEM_ID);
        orderItem.setOfferName("name");
        return orderItem;
    }

    @Nonnull
    private ItemDto getItemDto(String name) {
        return ItemDto.builder()
            .price(buildMonetaryDto(ITEM_PRICE))
            .assessedValue(buildMonetaryDto(ITEM_ASSESSED_VALUE))
            .count(1)
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
            .name(name)
            .build();
    }

    @Nonnull
    public ItemDto getItemDto() {
        return getItemDto("name");
    }

    @Nonnull
    public Parcel getParcel() {
        ParcelItem parcelItem = new ParcelItem();
        parcelItem.setItemId(ITEM_ID);
        parcelItem.setCount(1);
        Parcel parcel = new Parcel();
        parcel.setId(1L);
        parcel.setWeight(1L);
        parcel.addParcelItem(parcelItem);
        return parcel;
    }

    public void mockSearchOrders(List<OrderDto> result) {
        mockSearchLomOrders(result);
    }

    public void mockSearchLomOrders(List<OrderDto> result) {
        when(lomClient.searchOrders(
            eq(
                OrderSearchFilter.builder()
                    .externalIds(Set.of(String.valueOf(ORDER_ID)))
                    .senderIds(Set.of(SHOP_ID))
                    .build()
            ),
            eq(Set.of(CHANGE_REQUESTS)),
            any(),
            eq(false)
        ))
            .thenReturn(PageResult.of(result, 1, 0, 10));
    }

    @Nonnull
    private Order getOrder(long deliveryId) {
        Delivery delivery = new Delivery();
        delivery.setPrice(BigDecimal.TEN);
        delivery.setDeliveryServiceId(deliveryId);
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setShopId(SHOP_ID);
        order.setPaymentMethod(ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CASH_ON_DELIVERY);
        order.setDelivery(delivery);
        return order;
    }

    @Nonnull
    public Order getOrder() {
        return getOrder(1L);
    }

    @Nonnull
    public OrderDto getLomOrder() {
        return new OrderDto()
            .setId(LOM_ORDER_ID)
            .setBarcode(BARCODE)
            .setCost(CostDto.builder()
                .assessedValue(ITEM_ASSESSED_VALUE)
                .deliveryForCustomer(DELIVERY_COST)
                .build())
            .setUnits(ImmutableList.of(
                getStorageUnit(LOM_EXTERNAL_ID, getParcel())
            ));
    }

    @Nonnull
    public StorageUnitDto getStorageUnit(String externalId, Parcel parcel) {
        return StorageUnitDto.builder()
            .externalId(externalId)
            .type(StorageUnitType.ROOT)
            .dimensions(korobyteConverter.convertKorobyte(parcel))
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
}
