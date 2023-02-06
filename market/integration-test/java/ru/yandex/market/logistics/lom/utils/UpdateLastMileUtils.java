package ru.yandex.market.logistics.lom.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.UtilityClass;

import ru.yandex.market.logistic.gateway.common.model.delivery.CargoType;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.Item;
import ru.yandex.market.logistic.gateway.common.model.delivery.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.delivery.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.PaymentMethod;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.PersonalLocation;
import ru.yandex.market.logistic.gateway.common.model.delivery.PersonalRecipient;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.Recipient;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Sender;
import ru.yandex.market.logistic.gateway.common.model.delivery.Service;
import ru.yandex.market.logistic.gateway.common.model.delivery.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Supplier;
import ru.yandex.market.logistic.gateway.common.model.delivery.Tax;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaxType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Taxation;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.UnitId;
import ru.yandex.market.logistic.gateway.common.model.delivery.VatValue;
import ru.yandex.market.logistic.gateway.common.model.delivery.Warehouse;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateLastMilePayload;
import ru.yandex.market.logistics.lom.model.dto.UpdateLastMileRequestDto;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils;

@UtilityClass
public class UpdateLastMileUtils {

    private static final ObjectNode ROUTE = JsonNodeFactory.instance.objectNode();
    private static final String BARCODE = "1001";
    private static final String COMMENT = "Комментарий";
    private static final String NEW_STREET = "Новая улица";
    private static final String NEW_HOUSE = "Новый дом";
    private static final LocalDate DATE_MIN = LocalDate.of(2021, 3, 10);
    private static final LocalDate DATE_MAX = LocalDate.of(2021, 3, 11);
    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(18, 0);

    @Nonnull
    public UpdateLastMileRequestDto.UpdateLastMileRequestDtoBuilder validBuilder(
        UpdateLastMilePayload.UpdateLastMilePayloadBuilder payloadBuilder,
        @Nullable DeliveryType deliveryType
    ) {
        return UpdateLastMileRequestDto.builder()
            .barcode(BARCODE)
            .route(ROUTE)
            .deliveryType(deliveryType)
            .payload(payloadBuilder.build());
    }

    @Nonnull
    public UpdateLastMilePayload.UpdateLastMilePayloadBuilder validPayloadBuilder() {
        return UpdateLastMilePayload.builder()
            .comment(COMMENT)
            .checkouterChangeRequestId(12345L)
            .address(AddressDto.builder()
                .street(NEW_STREET)
                .house(NEW_HOUSE)
                .build()
            )
            .dateMin(DATE_MIN)
            .dateMax(DATE_MAX)
            .startTime(START_TIME)
            .endTime(END_TIME);
    }

    @Nonnull
    public Order expectedDsOrder(
        @Nullable String externalId,
        ru.yandex.market.logistic.gateway.common.model.delivery.DeliveryType deliveryType,
        Location locationTo,
        PaymentMethod paymentMethod,
        @Nullable String comment,
        boolean withPersonal
    ) {
        String pickupPointCode = null;
        ResourceId pickupPointId = null;
        if (deliveryType != ru.yandex.market.logistic.gateway.common.model.delivery.DeliveryType.COURIER) {
            pickupPointCode = "3257";
            pickupPointId = CreateLgwDeliveryEntitiesUtils.createResourceId("10001660932", "3257").build();
        }
        return new Order.OrderBuilder(
            CreateLgwDeliveryEntitiesUtils.createResourceId("1001", externalId).build(),
            locationTo,
            mkLocationFrom(),
            expectedKorobyte(),
            expectedItems(),
            "1",
            BigDecimal.valueOf(1001),
            paymentMethod,
            deliveryType,
            BigDecimal.valueOf(2000),
            expectedRecipient(),
            BigDecimal.valueOf(4000),
            expectedSender()
        )
            .setWarehouseFrom(expectedWarehouseFrom())
            .setCargoType(CargoType.UNKNOWN)
            .setCargoCost(BigDecimal.valueOf(2000))
            .setShipmentDate(
                DateTime.fromLocalDateTime(LocalDateTime.of(LocalDate.of(2021, 3, 10), LocalTime.MIDNIGHT))
            )
            .setShipmentPointCode(null)
            .setAmountPrepaid(BigDecimal.valueOf(0))
            .setPickupPointCode(pickupPointCode)
            .setPickupPointId(pickupPointId)
            .setComment(comment)
            .setWarehouse(expectedReturnWarehouse())
            .setServices(expectedServices())
            .setPlaces(List.of())
            .setTags(null)
            .setPersonalLocationTo(withPersonal
                ? expectedPersonalLocation("personal-address-id", "personal-gps-id")
                .orElse(null)
                : null
            )
            .setPersonalRecipient(expectedPersonalRecipient())
            .setDeliveryDate(
                DateTime.fromLocalDateTime(LocalDateTime.of(LocalDate.of(2021, 3, 10), LocalTime.MIDNIGHT))
            )
            .setDeliveryInterval(TimeInterval.of(LocalTime.of(9, 0), LocalTime.of(18, 0)))
            .build();
    }

    private Optional<PersonalLocation> expectedPersonalLocation(String personalAddressId, String personalGspId) {
        return Optional
            .ofNullable(personalAddressId)
            .map(id -> new PersonalLocation.LocationBuilder(id)
                .setPersonalGpsId(personalGspId)
                .build());
    }

    private PersonalRecipient expectedPersonalRecipient() {
        return new PersonalRecipient.RecipientBuilder("personal-fullname-id", null).build();
    }

    @Nonnull
    public Location pickupLocationTo() {
        return new Location.LocationBuilder("pickup-country", "pickup-locality", "Москва и Московская область")
            .setSettlement("pickup-settlement")
            .setStreet("pickup-street")
            .setHouse("pickup-house")
            .setLat(BigDecimal.valueOf(55))
            .setLng(BigDecimal.valueOf(37))
            .setLocationId(213)
            .setZipCode("pickup-zipcode")
            .build();
    }

    @Nonnull
    public Location recipientLocationTo() {
        return new Location.LocationBuilder("recipient-country", "recipient-locality", "Москва и Московская область")
            .setSettlement("recipient-settlement")
            .setStreet("recipient-street")
            .setHouse("recipient-house")
            .setLat(BigDecimal.valueOf(44))
            .setLng(BigDecimal.valueOf(32))
            .setLocationId(213)
            .setZipCode("recipient-zipcode")
            .build();
    }

    @Nonnull
    private Location mkLocationFrom() {
        return new Location.LocationBuilder("mk-country", "mk-locality", "Москва и Московская область")
            .setSettlement("mk-settlement")
            .setStreet("mk-street")
            .setHouse("mk-house")
            .setLat(BigDecimal.valueOf(33))
            .setLng(BigDecimal.valueOf(44))
            .setLocationId(213)
            .setZipCode("mk-zipcode")
            .build();
    }

    @Nonnull
    private Location expectedReturnLocation() {
        return new Location.LocationBuilder("return-country", "return-locality", "Москва и Московская область")
            .setSettlement("return-settlement")
            .setStreet("return-street")
            .setHouse("return-house")
            .setLat(BigDecimal.valueOf(1))
            .setLng(BigDecimal.valueOf(2))
            .setLocationId(1)
            .build();
    }

    @Nonnull
    private Korobyte expectedKorobyte() {
        return new Korobyte.KorobyteBuilder()
            .setLength(1)
            .setWidth(3)
            .setHeight(2)
            .setWeightGross(BigDecimal.valueOf(4))
            .build();
    }

    @Nonnull
    private List<Item> expectedItems() {
        return List.of(
            new Item.ItemBuilder("item 1", 1, BigDecimal.valueOf(10))
                .setTaxes(List.of(new Tax(TaxType.VAT, VatValue.NO_NDS)))
                .setArticle("item article 1")
                .setKorobyte(expectedKorobyte())
                .setUnitId(new UnitId.UnitIdBuilder(100L, "item article 1").build())
                .setSupplier(
                    Supplier.builder()
                        .setName("Имя поставщика")
                        .setPhone(new Phone.PhoneBuilder("+79876543210").build())
                        .setInn("1231231234")
                        .build()
                )
                .setInstances(List.of(Map.of("cis", "123abc")))
                .setCargoTypes(List.of(CargoType.TECH_AND_ELECTRONICS))
                .setCategoryName("Телефоны")
                .build()
        );
    }

    @Nonnull
    private Recipient expectedRecipient() {
        return new Recipient.RecipientBuilder(
            new Person("test-first-name", "test-last-name", "test-middle-name"),
            List.of()
        )
            .setEmail("test-email@test-domain.com")
            .build();
    }

    @Nonnull
    private Warehouse expectedWarehouseFrom() {
        return new Warehouse(
            CreateLgwDeliveryEntitiesUtils.createResourceId("10001640163", "-").build(),
            mkLocationFrom(),
            "-",
            null,
            null,
            null
        );
    }

    @Nonnull
    private Warehouse expectedReturnWarehouse() {
        return new Warehouse(
            CreateLgwDeliveryEntitiesUtils.createResourceId("1", "return-external-id").build(),
            expectedReturnLocation(),
            "-",
            null,
            null,
            null
        );
    }

    @Nonnull
    private List<Service> expectedServices() {
        return List.of(
            new Service.ServiceBuilder(false)
                .setCode(ServiceType.CASH_SERVICE)
                .setCost(1d)
                .build(),
            new Service.ServiceBuilder(false)
                .setCode(ServiceType.CHECK)
                .setCost(0d)
                .build()
        );
    }

    @Nonnull
    private Sender expectedSender() {
        return new Sender.SenderBuilder("credentials-incorporation", "credentials-ogrn")
            .setId(new ResourceId.ResourceIdBuilder().setYandexId("1").build())
            .setUrl("www.sender-url.com")
            .setInn("credentials-inn")
            .setName("sender-name")
            .setLegalForm(LegalForm.IP)
            .setTaxation(Taxation.OSN)
            .setPhones(List.of(new Phone(null, null)))
            .build();
    }
}
