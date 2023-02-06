package ru.yandex.market.logistics.logistics4shops.factory;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.protobuf.Timestamp;
import lombok.experimental.UtilityClass;

import ru.yandex.market.mbi.orderservice.proto.event.model.Dimensions;
import ru.yandex.market.mbi.orderservice.proto.event.model.MerchantOrderSubstatusChangedPayload;
import ru.yandex.market.mbi.orderservice.proto.event.model.NewExternalOrderCreatedPayload;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderCancelledPayload;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderItemsRemovedPayload;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderKey;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderLine;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderParcelBoxesChangedPayload;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderRecipient;
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderTrait;
import ru.yandex.market.mbi.orderservice.proto.event.model.Price;
import ru.yandex.market.mbi.orderservice.proto.event.model.RecipientAddress;
import ru.yandex.market.mbi.orderservice.proto.event.model.Status;
import ru.yandex.market.mbi.orderservice.proto.event.model.Substatus;

@UtilityClass
@ParametersAreNonnullByDefault
public class OrderServiceEventFactory {
    public static final Long EVENT_ID = 1L;
    public static final Long SHOP_ID = 2000L;
    public static final String SHOP_ORDER_ID = "300id";
    public static final Instant CREATED_AT = Instant.parse("2021-12-16T11:30:00.00Z");

    public static final Timestamp CREATED_TIMESTAMP = Timestamp.newBuilder()
        .setNanos(CREATED_AT.getNano())
        .setSeconds(CREATED_AT.getEpochSecond())
        .build();

    public static final Instant DATETIME = Instant.parse("2021-11-16T11:30:00.00Z");
    public static final Timestamp CHECKOUTER_TIMESTAMP = Timestamp.newBuilder()
        .setNanos(DATETIME.getNano())
        .setSeconds(DATETIME.getEpochSecond()).build();

    public static final String REQUEST_ID = "order-service-event-request-id";

    @Nonnull
    public OrderEvent.Builder baseEventBuilder() {
        return OrderEvent.newBuilder()
            .setId(EVENT_ID)
            .setCreatedAt(CREATED_TIMESTAMP)
            .setTraceId(REQUEST_ID);
    }

    @Nonnull
    private OrderKey orderKey(Long orderId, Long shopId) {
        return OrderKey.newBuilder()
            .setOrderId(orderId)
            .setShopId(shopId)
            .build();
    }

    @Nonnull
    public OrderEvent merchantOrderSubstatusEvent(Long orderId, Substatus substatus) {
        return baseEventBuilder().setMerchantOrderSubstatusChangedPayload(
                MerchantOrderSubstatusChangedPayload.newBuilder()
                    .setCheckouterEventSpawnTimestamp(CHECKOUTER_TIMESTAMP)
                    .setOrderKey(orderKey(orderId, SHOP_ID))
                    .setMerchantOrderId(SHOP_ORDER_ID)
                    .setStatus(Status.PROCESSING)
                    .setSubstatus(substatus)
                    .build()
            )
            .build();
    }

    @Nonnull
    public OrderEvent orderItemsRemovedEvent(long orderId) {
        return baseEventBuilder().setOrderItemsRemovedPayload(
                OrderItemsRemovedPayload.newBuilder()
                    .setCheckouterEventSpawnTimestamp(CHECKOUTER_TIMESTAMP)
                    .setOrderKey(orderKey(orderId, SHOP_ID))
                    .addAllRemovedItems(List.of(itemsRemovedEntry(12, 10), itemsRemovedEntry(13, 11)))
                    .build()
            )
            .build();
    }

    @Nonnull
    private OrderItemsRemovedPayload.ItemsRemovedEntry itemsRemovedEntry(int orderLineId, int removedCount) {
        return OrderItemsRemovedPayload.ItemsRemovedEntry.newBuilder()
            .setOrderLineId(orderLineId)
            .setRemovedCount(removedCount)
            .build();
    }

    @Nonnull
    public OrderEvent orderParcelBoxesChangedEvent(long orderId) {
        return baseEventBuilder().setOrderParcelBoxesChangedPayload(
                OrderParcelBoxesChangedPayload.newBuilder()
                    .setCheckouterEventSpawnTimestamp(CHECKOUTER_TIMESTAMP)
                    .setOrderKey(orderKey(orderId, SHOP_ID))
                    .build()
            )
            .build();
    }

    @Nonnull
    public static OrderEvent newOrderCancelledEvent() {
        return OrderEvent.newBuilder()
            .setId(1L)
            .setCreatedAt(CREATED_TIMESTAMP)
            .setOrderCancelledPayload(
                OrderCancelledPayload.newBuilder()
                    .setOrderKey(OrderKey.newBuilder().setOrderId(1L).setShopId(101L))
                    .build()
            )
            .setTraceId("trace")
            .build();
    }

    @Nonnull
    public OrderEvent newOrderCreatedEvent(Collection<OrderTrait> traits) {
        return baseEventBuilder().setNewExternalOrderCreatedPayload(
                NewExternalOrderCreatedPayload.newBuilder()
                    .setOrderKey(orderKey(1L, 101L))
                    .setRecipient(recipient())
                    .addAllLines(List.of(line()))
                    .addAllTraits(traits)
                    .setDeliveryRoute("{\"route\": {\"cost\": 0}}")
                    .setNotes("Order comment by recipient!")
                    .build()
            )
            .build();
    }

    @Nonnull
    private OrderRecipient recipient() {
        return OrderRecipient.newBuilder()
            .setFirstName("firstName")
            .setLastName("lastname")
            .setMiddleName("middleName")
            .setPhone("phone")
            .setEmail("email")
            .setPersonalFullnameId("pers-fullname")
            .setPersonalPhoneId("pers-phone")
            .setPersonalEmailId("pers-email")
            .setAddress(address())
            .build();
    }

    @Nonnull
    private RecipientAddress address() {
        return RecipientAddress.newBuilder()
            .setCountry("country")
            .setRegion("region")
            .setLocality("locality")
            .setStreet("street")
            .setHouse("house")
            .setRoom("room")
            .setZipCode("zipCode")
            .setPorch("porch")
            .setFloor("2")
            .setLatitude(10.1)
            .setLongitude(100.2)
            .setGeoId(1)
            .setIntercom("intercom")
            .setPersonalAddressId("pers-address")
            .setPersonalGpsId("pers-gps")
            .build();
    }

    @Nonnull
    private OrderLine line() {
        return OrderLine.newBuilder()
            .setTitle("title")
            .setShopId(101L)
            .setSsku("ssku")
            .setMsku("msku")
            .setCount(2L)
            .setPrice(Price.newBuilder().setValue(200).setIsoCurrencyCode("RUB").build())
            .addAllCargoTypes(List.of(300, 301))
            .setDimensions(Dimensions.newBuilder().setWeight(1).setDepth(2).setWidth(3).setHeight(4).build())
            .setVat(OrderLine.Vat.VAT_20)
            .build();
    }
}
