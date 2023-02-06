package ru.yandex.market.logistics.lom.controller.shipment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.dto.CarDto;
import ru.yandex.market.logistics.lom.model.dto.ContactDto;
import ru.yandex.market.logistics.lom.model.dto.CourierDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentApplicationDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentApplicationDto.ShipmentApplicationDtoBuilder;
import ru.yandex.market.logistics.lom.model.dto.ShipmentDto;
import ru.yandex.market.logistics.lom.model.dto.TimeIntervalDto;
import ru.yandex.market.logistics.lom.model.enums.CourierType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Валидация создания отгрузки и заявки")
class ShipmentValidationTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Кейсы валидации javax")
    @MethodSource("shipmentValidation")
    void createShipmentValidation(
        String caseName,
        Consumer<ShipmentApplicationDtoBuilder> applicationModifier,
        String message
    ) throws Exception {
        ShipmentApplicationDtoBuilder application = createShipmentApplicationDtoBuilder();
        applicationModifier.accept(application);
        ShipmentTestUtil.createShipmentByRawRequestResponse(
            mockMvc,
            objectMapper.writeValueAsString(application.build()),
            message,
            status().isBadRequest()
        );
    }

    @Nonnull
    @SuppressWarnings("checkstyle:MethodLength")
    private static Stream<Arguments> shipmentValidation() {
        return Stream.<Triple<String, Consumer<ShipmentApplicationDtoBuilder>, String>>of(
            Triple.of(
                "Проверка на заполненность car для shipment type = IMPORT",
                application -> application.courier(createCourier(createContact(), CourierType.CAR, null)),
                "{\"message\":\"Following validation errors occurred:\\nField: 'courier.car',"
                    + " message: 'car with brand and number must be not null when shipment type is IMPORT'\"}"),
            Triple.of(
                "Проверка заполненности полей car для shipment type = IMPORT",
                application -> application.courier(
                    createCourier(createContact(), CourierType.CAR, CarDto.builder().build())
                ),
                "{\"message\":\"Following validation errors occurred:\\nField: 'courier.car.brand', "
                    + "message: 'car brand must be not null when shipment type is IMPORT'\\"
                    + "nField: 'courier.car.number', "
                    + "message: 'car number must be not null when shipment type is IMPORT'\"}"),
            Triple.of(
                "Заявки без отгрузки не может быть",
                application -> application.shipment(null),
                "{\"message\":\"Following validation errors occurred:\\"
                    + "nField: 'shipment', message: 'must not be null'\"}"),
            Triple.of(
                "interval обязательно",
                application -> application.interval(null),
                "{\"message\":\"Following validation errors occurred:\\"
                    + "nField: 'interval', message: 'must not be null'\"}"),
            Triple.of(
                "korobyte обязательно",
                application -> application.korobyteDto(null),
                "{\"message\":\"Following validation errors occurred:\\"
                    + "nField: 'korobyteDto', message: 'must not be null'\"}"),
            Triple.of(
                "courier обязательно",
                application -> application.courier(null),
                "{\"message\":\"Following validation errors occurred:\\"
                    + "nField: 'courier', message: 'courier must be not null when shipment type is IMPORT'\"}"),
            Triple.of(
                "cost обязательно",
                application -> application.cost(null),
                "{\"message\":\"Following validation errors occurred:\\"
                    + "nField: 'cost', message: 'must not be null'\"}"),
            Triple.of(
                "cost >= 0",
                application -> application.cost(BigDecimal.valueOf(-1)),
                "{\"message\":\"Following validation errors occurred:\\"
                    + "nField: 'cost', message: 'must be greater than or equal to 0'\"}"),
            Triple.of(
                "requisiteId обязательно",
                application -> application.requisiteId(null),
                "{\"message\":\"Following validation errors occurred:\\"
                    + "nField: 'requisiteId', message: 'must not be blank'\"}"),
            Triple.of(
                "shipment.marketIdFrom обязательно",
                application -> application.shipment(createShipment().marketIdFrom(null).build()),
                "{\"message\":\"Following validation errors occurred:\\"
                    + "nField: 'shipment.marketIdFrom', message: 'must not be null'\"}"),
            Triple.of(
                "shipment.marketIdTo обязательно",
                application -> application.shipment(createShipment().marketIdTo(null).build()),
                "{\"message\":\"Following validation errors occurred:\\"
                    + "nField: 'shipment.marketIdTo', message: 'must not be null'\"}"),
            Triple.of(
                "shipment.shipmentType обязательно",
                application -> application.shipment(createShipment().shipmentType(null).build()),
                "{\"message\":\"Following validation errors occurred:\\"
                    + "nField: 'shipment.shipmentType', message: 'must not be null'\"}"),
            Triple.of(
                "shipment.shipmentDate обязательно",
                application -> application.shipment(createShipment().shipmentDate(null).build()),
                "{\"message\":\"Following validation errors occurred:\\"
                    + "nField: 'shipment.shipmentDate', message: 'must not be null'\"}"),
            Triple.of(
                "shipment.warehouseFrom обязательно",
                application -> application.shipment(createShipment().warehouseFrom(null).build()),
                "{\"message\":\"Following validation errors occurred:\\"
                    + "nField: 'shipment.warehouseFrom', message: 'must not be null'\"}"),
            Triple.of(
                "warehouseTo обязательно если shipment type = IMPORT",
                application -> application.shipment(createShipment().warehouseTo(null).build()),
                "{\"message\":\"Following validation errors occurred:\\nField: 'shipment', "
                    + "message: 'warehouseTo must be not null when shipment type is IMPORT'\"}"),
            Triple.of(
                "interval.to обязательно",
                application -> application.interval(TimeIntervalDto.builder().from(LocalTime.of(10, 0)).build()),
                "{\"message\":\"Following validation errors occurred:\\nField: 'interval.to', "
                    + "message: 'must not be null'\"}"),
            Triple.of(
                "interval.from обязательно",
                application -> application.interval(TimeIntervalDto.builder().to(LocalTime.of(10, 0)).build()),
                "{\"message\":\"Following validation errors occurred:\\nField: 'interval.from', "
                    + "message: 'must not be null'\"}"),
            Triple.of(
                "korobyteDto.length обязательно",
                application -> application.korobyteDto(
                    KorobyteDto.builder().height(10).width(10).weightGross(BigDecimal.valueOf(20)).build()
                ),
                "{\"message\":\"Following validation errors occurred:\\nField: 'korobyteDto.length', "
                    + "message: 'must not be null'\"}"),
            Triple.of(
                "korobyteDto.height обязательно",
                application -> application.korobyteDto(
                    KorobyteDto.builder().length(10).width(10).weightGross(BigDecimal.valueOf(20)).build()
                ),
                "{\"message\":\"Following validation errors occurred:\\nField: 'korobyteDto.height', "
                    + "message: 'must not be null'\"}"),
            Triple.of(
                "korobyteDto.width обязательно",
                application -> application.korobyteDto(
                    KorobyteDto.builder().length(10).height(10).weightGross(BigDecimal.valueOf(20)).build()
                ),
                "{\"message\":\"Following validation errors occurred:\\nField: 'korobyteDto.width', "
                    + "message: 'must not be null'\"}"),
            Triple.of(
                "korobyteDto.weightGross обязательно",
                application -> application.korobyteDto(
                    KorobyteDto.builder().length(10).height(10).width(10).build()
                ),
                "{\"message\":\"Following validation errors occurred:\\nField: 'korobyteDto.weightGross', "
                    + "message: 'must not be null'\"}"),
            Triple.of(
                "korobyteDto.length > 0",
                application -> application.korobyteDto(
                    KorobyteDto.builder().length(-1).height(10).width(10).weightGross(BigDecimal.valueOf(20)).build()
                ),
                "{\"message\":\"Following validation errors occurred:\\nField: 'korobyteDto.length', "
                    + "message: 'must be greater than 0'\"}"),
            Triple.of(
                "korobyteDto.height > 0",
                application -> application.korobyteDto(
                    KorobyteDto.builder().length(10).height(-1).width(10).weightGross(BigDecimal.valueOf(20)).build()
                ),
                "{\"message\":\"Following validation errors occurred:\\nField: 'korobyteDto.height', "
                    + "message: 'must be greater than 0'\"}"),
            Triple.of(
                "korobyteDto.width > 0",
                application -> application.korobyteDto(
                    KorobyteDto.builder().length(10).height(10).width(-1).weightGross(BigDecimal.valueOf(20)).build()
                ),
                "{\"message\":\"Following validation errors occurred:\\nField: 'korobyteDto.width', "
                    + "message: 'must be greater than 0'\"}"),
            Triple.of(
                "korobyteDto.weightGross > 0",
                application -> application.korobyteDto(
                    KorobyteDto.builder().length(10).height(10).width(10).weightGross(BigDecimal.valueOf(-1)).build()
                ),
                "{\"message\":\"Following validation errors occurred:\\nField: 'korobyteDto.weightGross', "
                    + "message: 'must be greater than 0'\"}"),
            Triple.of(
                "courier.contact.lastName обязательно, если задан контакт",
                application -> application.courier(
                    createCourier(ContactDto.builder().firstName("firstName").phone("+79998887766").build())
                ),
                "{\"message\":\"Following validation errors occurred:\\nField: 'courier.contact.lastName', "
                    + "message: 'must not be blank'\"}"),
            Triple.of(
                "courier.contact.firstName обязательно, если задан контакт",
                application -> application.courier(
                    createCourier(ContactDto.builder().lastName("lastName").phone("+79998887766").build())
                ),
                "{\"message\":\"Following validation errors occurred:\\nField: 'courier.contact.firstName', "
                    + "message: 'must not be blank'\"}"),
            Triple.of(
                "courier.contact.phone обязательно, если задан контакт",
                application -> application.courier(
                    createCourier(
                        ContactDto.builder()
                            .lastName("lastName")
                            .firstName("firstName")
                            .personalFullnameId("personal-fullname-id")
                            .personalPhoneId("personal-phone-id")
                            .build())
                ),
                "{\"message\":\"Following validation errors occurred:\\nField: 'courier.contact.phone', "
                    + "message: 'must not be blank'\"}"),
            Triple.of(
                "shipment.partnerIdTo обязательно",
                application -> application.shipment(createShipment().partnerIdTo(null).build()),
                "{\"message\":\"Following validation errors occurred:\\nField: 'shipment.partnerIdTo', "
                    + "message: 'must not be null'\"}"),
            Triple.of(
                "shipment.balanceContractId обязательно",
                application -> application.balanceContractId(null),
                "{\"message\":\"Following validation errors occurred:\\nField: 'balanceContractId', "
                    + "message: 'must not be null'\"}"),
            Triple.of(
                "shipment.balancePersonId обязательно",
                application -> application.balancePersonId(null),
                "{\"message\":\"Following validation errors occurred:\\nField: 'balancePersonId', "
                    + "message: 'must not be null'\"}")
        ).map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @Test
    @DisplayName("Неправильный формат времени")
    void nonParseableIntervalTime() throws Exception {
        ShipmentTestUtil.createShipment(mockMvc, "non_parseable_interval_time.json", status().isBadRequest());
    }

    @Nonnull
    private ShipmentApplicationDtoBuilder createShipmentApplicationDtoBuilder() {
        return ShipmentApplicationDto.builder()
            .shipment(createShipment().build())
            .balanceContractId(1001L)
            .balancePersonId(10001L)
            .comment("shipment comment")
            .cost(BigDecimal.valueOf(100))
            .courier(createCourier(createContact()))
            .externalId("ext_id")
            .interval(createTimeInterval())
            .korobyteDto(createKorobyte())
            .requisiteId("1")
            .status(ShipmentApplicationStatus.NEW);

    }

    private static CourierDto createCourier(ContactDto contact) {
        return createCourier(contact, CourierType.CAR, createCar());
    }

    private static CourierDto createCourier(ContactDto contact, CourierType courierType, CarDto car) {
        return CourierDto.builder()
            .contact(contact)
            .car(car)
            .type(courierType)
            .build();
    }

    @Nonnull
    private KorobyteDto createKorobyte() {
        return KorobyteDto.builder()
            .length(10)
            .width(20)
            .height(30)
            .weightGross(BigDecimal.valueOf(10))
            .build();
    }

    @Nonnull
    private TimeIntervalDto createTimeInterval() {
        return TimeIntervalDto.builder()
            .from(LocalTime.of(10, 0))
            .to(LocalTime.of(12, 0))
            .build();
    }

    @Nonnull
    private static ContactDto createContact() {
        return ContactDto.builder()
            .firstName("firstName")
            .lastName("lastName")
            .middleName("middleName")
            .phone("+79998887766")
            .extension("+71112223344")
            .build();
    }

    @Nonnull
    private static CarDto createCar() {
        return CarDto.builder()
            .brand("brand")
            .number("number")
            .build();
    }

    @Nonnull
    private static ShipmentDto.ShipmentDtoBuilder createShipment() {
        return ShipmentDto.builder()
            .marketIdFrom(1L)
            .marketIdTo(2L)
            .partnerIdTo(3L)
            .shipmentDate(LocalDate.of(2019, 6, 20))
            .shipmentType(ShipmentType.IMPORT)
            .warehouseFrom(3L)
            .warehouseTo(4L);
    }
}
