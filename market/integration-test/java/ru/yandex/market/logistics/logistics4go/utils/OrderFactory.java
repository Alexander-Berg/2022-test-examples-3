package ru.yandex.market.logistics.logistics4go.utils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.logistics4go.client.model.ContactDto;
import ru.yandex.market.logistics.logistics4go.client.model.Cost;
import ru.yandex.market.logistics.logistics4go.client.model.CreateOrderRequest;
import ru.yandex.market.logistics.logistics4go.client.model.DeliveryOption;
import ru.yandex.market.logistics.logistics4go.client.model.DeliveryType;
import ru.yandex.market.logistics.logistics4go.client.model.Dimensions;
import ru.yandex.market.logistics.logistics4go.client.model.Handing;
import ru.yandex.market.logistics.logistics4go.client.model.Inbound;
import ru.yandex.market.logistics.logistics4go.client.model.Interval;
import ru.yandex.market.logistics.logistics4go.client.model.Item;
import ru.yandex.market.logistics.logistics4go.client.model.ItemInstanceDto;
import ru.yandex.market.logistics.logistics4go.client.model.ItemSupplier;
import ru.yandex.market.logistics.logistics4go.client.model.OrderAddressDto;
import ru.yandex.market.logistics.logistics4go.client.model.OrderTag;
import ru.yandex.market.logistics.logistics4go.client.model.PaymentMethod;
import ru.yandex.market.logistics.logistics4go.client.model.PersonName;
import ru.yandex.market.logistics.logistics4go.client.model.Phone;
import ru.yandex.market.logistics.logistics4go.client.model.PhysicalPersonSender;
import ru.yandex.market.logistics.logistics4go.client.model.Place;
import ru.yandex.market.logistics.logistics4go.client.model.ShipmentType;
import ru.yandex.market.logistics.logistics4go.client.model.VatRate;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class OrderFactory {

    public static final long SENDER_ID = 12345L;
    public static final long SHOP_WAREHOUSE_ID = 10000051904L;
    public static final long SC_SEGMENT_ID = 55067L;
    public static final long PICKUP_POINT_ID = 10000051906L;
    public static final long SUPPLIER_ID = 10336698L;
    public static final String CIS_FULL = "010942102361011221oPJf79NVw;8so\u001d91TWFxiUG;06\u001d92SWthRXp6NXEnMGtEM3"
        + "FyMWpVVTYleTtiamluPmdFM3hGJVMieiVIMSpXO3JXY2NUVVdxN0U3Tk9VZGswakFUYzI=";
    public static final String CIS = "010942102361011221oPJf79NVw;8so";
    public static final String YANDEX_INN = "7736207543";

    @Nonnull
    public static CreateOrderRequest baseCreateOrderRequest(boolean isOnlyRequired, boolean isCourier) {
        return new CreateOrderRequest()
            .senderId(SENDER_ID)
            .externalId("externalId")
            .comment(isOnlyRequired ? null : "comment")
            .cost(
                new Cost()
                    .assessedValue(new BigDecimal("999.90"))
                    .deliveryForCustomer(new BigDecimal("249"))
                    .paymentMethod(PaymentMethod.PREPAID)
            )
            .recipient(recipient(isOnlyRequired))
            .items(List.of(item(isOnlyRequired)))
            .places(isOnlyRequired ? null : List.of(place()))
            .deliveryOption(
                new DeliveryOption()
                    .inbound(
                        new Inbound()
                            .type(ShipmentType.IMPORT)
                            .fromLogisticsPointId(isOnlyRequired ? null : SHOP_WAREHOUSE_ID)
                            .toSegmentId(SC_SEGMENT_ID)
                            .dateTime(Instant.parse("2022-02-22T15:00:00Z"))
                    )
                    .handing(
                        new Handing()
                            .deliveryType(isCourier ? DeliveryType.COURIER : DeliveryType.PICKUP)
                            .pickupPointId(isCourier ? null : PICKUP_POINT_ID)
                            .address(isCourier ? address(isOnlyRequired) : null)
                            .interval(
                                isCourier
                                    ? new Interval()
                                        .start(LocalTime.of(10, 0, 0))
                                        .end(LocalTime.of(14, 0, 0))
                                    : null
                            )
                            .dateMin(LocalDate.of(2022, 2, 23))
                            .dateMax(LocalDate.of(2022, 2, 25))
                    )
            );
    }

    @Nonnull
    public static ContactDto recipient(boolean isOnlyRequired) {
        return new ContactDto()
            .name(recipientPersonName(isOnlyRequired))
            .email(isOnlyRequired ? null : "recipient@email.com")
            .phone(recipientPhone(isOnlyRequired));
    }

    @Nonnull
    public static Place place() {
        return new Place()
            .externalId("place[0].externalId")
            .dimensions(
                new Dimensions()
                    .height(30)
                    .length(40)
                    .width(50)
                    .weight(new BigDecimal("1.234"))
            );
    }

    @Nonnull
    public static Item item(boolean isOnlyRequired) {
        return new Item()
            .externalId("item[0].externalId")
            .name("item[0].name")
            .count(1)
            .price(new BigDecimal("999.90"))
            .assessedValue(isOnlyRequired ? null : new BigDecimal("99.90"))
            .tax(VatRate.VAT_20)
            .supplier(new ItemSupplier().inn(YANDEX_INN))
            .dimensions(
                new Dimensions()
                    .height(30)
                    .length(40)
                    .width(50)
                    .weight(new BigDecimal("1.234"))
            )
            .instances(isOnlyRequired ? null : List.of(new ItemInstanceDto().cis(CIS_FULL)))
            .cargoTypes(isOnlyRequired ? null : List.of(300, 301, 302))
            .placesExternalIds(isOnlyRequired ? null : List.of("place[0].externalId"));
    }

    @Nonnull
    public static OrderAddressDto address(boolean isOnlyRequired) {
        return new OrderAddressDto()
            .geoId(213)
            .latitude(isOnlyRequired ? null : new BigDecimal("55.733974"))
            .longitude(isOnlyRequired ? null : new BigDecimal("37.587093"))
            .country("Россия")
            .region("Москва")
            .subRegion(isOnlyRequired ? null : "Москва")
            .locality("Москва")
            .street(isOnlyRequired ? null : "Льва Толстого")
            .house("16")
            .housing(isOnlyRequired ? null : "1")
            .building(isOnlyRequired ? null : "1")
            .apartment(isOnlyRequired ? null : "1")
            .postalCode(isOnlyRequired ? null : "119021");
    }

    @Nonnull
    public static PersonName recipientPersonName(boolean isOnlyRequired) {
        return new PersonName()
            .lastName("recipient.lastName")
            .firstName("recipient.firstName")
            .middleName(isOnlyRequired ? null : "recipient.middleName");
    }

    @Nonnull
    public static PersonName senderPersonName(boolean isOnlyRequired) {
        return new PersonName()
            .lastName("sender.lastName")
            .firstName("sender.firstName")
            .middleName(isOnlyRequired ? null : "sender.middleName");
    }

    @Nonnull
    public static Phone recipientPhone(boolean isOnlyRequired) {
        return new Phone()
            .number("+7 999 888 7766")
            .extension(isOnlyRequired ? null : "12345");
    }

    @Nonnull
    public static Phone senderPhone(boolean isOnlyRequired) {
        return new Phone()
            .number("+7 987 456 7890")
            .extension(isOnlyRequired ? null : "54321");
    }

    @Nonnull
    public static <F, T> UnaryOperator<F> modifier(
        Function<F, T> getter,
        UnaryOperator<T> modifier,
        Class<F> modifiedClass
    ) {
        return request -> {
            modifier.apply(getter.apply(request));
            return request;
        };
    }

    @Nonnull
    public static UnaryOperator<CreateOrderRequest> c2cModifier() {
        return request -> modifier(
            r -> r.getItems().get(0),
            r -> r.supplier(null),
            CreateOrderRequest.class
        )
            .apply(request)
            .physicalPersonSender(
                new PhysicalPersonSender()
                    .name(senderPersonName(false))
                    .phone(senderPhone(false))
            )
            .tags(List.of(OrderTag.C2C));
    }
}
