package ru.yandex.market.logistics.management.domain.converter;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.entity.LogisticsPoint;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.Phone;
import ru.yandex.market.logistics.management.domain.entity.Schedule;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.domain.entity.ServiceCode;
import ru.yandex.market.logistics.management.domain.entity.type.PhoneType;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.domain.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointCreateRequest;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointUpdateRequest;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PickupPointType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogisticsPointConverterTest extends AbstractTest {

    private static final Address ADDRESS_DTO = Address.newBuilder()
        .locationId(12345)
        .settlement("Москва")
        .postCode("555666")
        .latitude(new BigDecimal("100"))
        .longitude(new BigDecimal("200"))
        .street("Октябрьская")
        .house("5")
        .housing("3")
        .building("2")
        .apartment("1")
        .comment("comment")
        .region("region")
        .subRegion("subRegion")
        .addressString("Строка адреса")
        .shortAddressString("Строка адреса")
        .build();

    private static final ru.yandex.market.logistics.management.domain.entity.Address ADDRESS_ENTITY =
        new ru.yandex.market.logistics.management.domain.entity.Address()
            .setLocationId(12345)
            .setSettlement("Москва")
            .setPostCode("555666")
            .setLatitude(new BigDecimal("100"))
            .setLongitude(new BigDecimal("200"))
            .setStreet("Октябрьская")
            .setHouse("5")
            .setHousing("3")
            .setBuilding("2")
            .setApartment("1")
            .setComment("comment")
            .setAddressString("Строка адреса")
            .setShortAddressString("Строка адреса")
            .setRegion("region")
            .setSubRegion("subRegion")
            .setExactLocationId(2345);

    private static final Contact CONTACT_DTO = new Contact(
        "Арсений",
        "Петров",
        "Сергеевич"
    );

    private static final ru.yandex.market.logistics.management.domain.entity.Contact CONTACT_ENTITY =
        new ru.yandex.market.logistics.management.domain.entity.Contact()
            .setName("Арсений")
            .setSurname("Петров")
            .setPatronymic("Сергеевич");

    private static final Phone PHONE_ENTITY = new Phone()
        .setType(PhoneType.PRIMARY)
        .setComment("comment")
        .setNumber("88005553535");

    private static final ru.yandex.market.logistics.management.entity.response.core.Phone PHONE_DTO =
        ru.yandex.market.logistics.management.entity.response.core.Phone.newBuilder()
            .number("+88005553535")
            .internalNumber(null)
            .comment("comment")
            .type(ru.yandex.market.logistics.management.entity.type.PhoneType.PRIMARY)
            .build();

    private static final ServiceCode SERVICE_CODE_ENTITY = new ServiceCode()
        .setCode(ServiceCodeName.CHECK)
        .setOptional(false)
        .setName("service");

    private static final ru.yandex.market.logistics.management.entity.response.point.Service SERVICE_DTO =
        new ru.yandex.market.logistics.management.entity.response.point.Service(
            ru.yandex.market.logistics.management.entity.type.ServiceCodeName.CHECK,
            false,
            "service",
            null
        );

    private static final ScheduleDay SCHEDULE_DAY_ENTITY = new ScheduleDay()
        .setDay(7);

    private static final Schedule SCHEDULE_ENTITY = new Schedule().addScheduledDay(SCHEDULE_DAY_ENTITY);

    private static final ScheduleDayResponse SCHEDULE_DAY_DTO = new ScheduleDayResponse(
        1L,
        7,
        null,
        null
    );

    private static final Partner PARTNER = new Partner().setId(0L);

    private static final LogisticsPointResponse LOGISTICS_POINT_RESPONSE = LogisticsPointResponse.newBuilder()
        .id(1L)
        .partnerId(0L)
        .externalId("CODE")
        .type(ru.yandex.market.logistics.management.entity.type.PointType.PICKUP_POINT)
        .pickupPointType(PickupPointType.PICKUP_POINT)
        .name("UPDATED_POINT")
        .address(ADDRESS_DTO)
        .phones(Collections.singleton(PHONE_DTO))
        .active(true)
        .schedule(Collections.singleton(SCHEDULE_DAY_DTO))
        .contact(CONTACT_DTO)
        .cashAllowed(true)
        .prepayAllowed(true)
        .cardAllowed(true)
        .photos(null)
        .instruction("instruction")
        .returnAllowed(true)
        .services(Collections.singleton(SERVICE_DTO))
        .storagePeriod(10)
        .maxWeight(15d)
        .maxLength(15)
        .maxWidth(15)
        .maxHeight(15)
        .maxSidesSum(15)
        .isFrozen(false)
        .locationZoneId(null)
        .marketBranded(false)
        .availableForOnDemand(false)
        .deferredCourierAvailable(false)
        .darkStore(false)
        .availableForC2C(false)
        .handlingTime(Duration.ofDays(1))
        .build();

    private static final LogisticsPoint LOGISTICS_POINT_ENTITY = new LogisticsPoint()
        .setId(1L)
        .setPartner(PARTNER)
        .setExternalId("CODE")
        .setType(ru.yandex.market.logistics.management.domain.entity.type.PointType.PICKUP_POINT)
        .setPickupPointType(ru.yandex.market.logistics.management.domain.entity.type.PickupPointType.PICKUP_POINT)
        .setName("UPDATED_POINT")
        .setAddress(ADDRESS_ENTITY)
        .addPhone(PHONE_ENTITY)
        .setActive(true)
        .setSchedule(SCHEDULE_ENTITY)
        .setContact(CONTACT_ENTITY)
        .setCashAllowed(true)
        .setPrepayAllowed(true)
        .setCardAllowed(true)
        .setInstruction("instruction")
        .setReturnAllowed(true)
        .addServiceCode(SERVICE_CODE_ENTITY)
        .setStoragePeriod(10)
        .setMaxWeight(15d)
        .setMaxLength(15)
        .setMaxWidth(15)
        .setMaxHeight(15)
        .setMaxSidesSum(15)
        .setFrozen(false)
        .setMarketBranded(true)
        .setAvailableForOnDemand(false)
        .setDeferredCourierAvailable(false)
        .setDarkStore(false)
        .setAvailableForC2C(false)
        .setHandlingTime(Duration.ofDays(1));

    private static final LogisticsPointCreateRequest LOGISTICS_POINT_CREATE_REQUEST =
        LogisticsPointCreateRequest.newBuilder()
            .externalId("CODE")
            .type(ru.yandex.market.logistics.management.entity.type.PointType.PICKUP_POINT)
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .name("UPDATED_POINT")
            .address(ADDRESS_DTO)
            .phones(Collections.singleton(PHONE_DTO))
            .active(true)
            .schedule(Collections.singleton(SCHEDULE_DAY_DTO))
            .contact(CONTACT_DTO)
            .cashAllowed(true)
            .prepayAllowed(true)
            .cardAllowed(true)
            .instruction("instruction")
            .returnAllowed(true)
            .services(Collections.singleton(SERVICE_DTO))
            .storagePeriod(10)
            .maxWeight(15d)
            .maxLength(15)
            .maxWidth(15)
            .maxHeight(15)
            .maxSidesSum(15)
            .isFrozen(false)
            .marketBranded(true)
            .handlingTime(Duration.ofDays(1))
            .build();

    private static final LogisticsPointUpdateRequest LOGISTICS_POINT_UPDATE_REQUEST =
        LogisticsPointUpdateRequest.newBuilder()
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .name("UPDATED_POINT")
            .addressComment("comment")
            .phones(Collections.singleton(PHONE_DTO))
            .active(true)
            .schedule(Collections.singleton(SCHEDULE_DAY_DTO))
            .contact(CONTACT_DTO)
            .cashAllowed(true)
            .prepayAllowed(true)
            .cardAllowed(true)
            .instruction("instruction")
            .returnAllowed(true)
            .services(Collections.singleton(SERVICE_DTO))
            .storagePeriod(10)
            .maxWeight(15d)
            .maxLength(15)
            .maxWidth(15)
            .maxHeight(15)
            .maxSidesSum(15)
            .marketBranded(true)
            .handlingTime(Duration.ofDays(1))
            .build();

    private static final LogisticsPointUpdateRequest LOGISTICS_POINT_UPDATE_REQUEST_WITH_ADDRESS =
        LogisticsPointUpdateRequest.newBuilder()
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .name("UPDATED_POINT")
            .address(ADDRESS_DTO)
            .phones(Collections.singleton(PHONE_DTO))
            .active(true)
            .schedule(Collections.singleton(SCHEDULE_DAY_DTO))
            .contact(CONTACT_DTO)
            .cashAllowed(true)
            .prepayAllowed(true)
            .cardAllowed(true)
            .instruction("instruction")
            .returnAllowed(true)
            .services(Collections.singleton(SERVICE_DTO))
            .storagePeriod(10)
            .maxWeight(15d)
            .maxLength(15)
            .maxWidth(15)
            .maxHeight(15)
            .maxSidesSum(15)
            .marketBranded(false)
            .handlingTime(Duration.ofDays(1))
            .build();

    //Внутренние конвертеры мокаются для того, чтобы их "отключить" и протестировать работу непосредственно внешнего
    //конвертера.

    @Mock
    private AddressConverter addressConverter;

    @Mock
    private ServiceConverter serviceConverter;

    @Mock
    private ContactConverter contactConverter;

    @Mock
    private PhoneConverter phoneConverter;

    @Mock
    private ScheduleDayConverter scheduleDayConverter;

    @Mock
    private LogisticsPointGateConverter logisticsPointGateConverter;

    @Mock
    private FileCollectionConverter fileCollectionConverter;

    private LogisticsPointConverter converter;

    @BeforeEach
    void init() {
        converter = new LogisticsPointConverter(
            addressConverter,
            serviceConverter,
            contactConverter,
            phoneConverter,
            scheduleDayConverter,
            fileCollectionConverter,
            logisticsPointGateConverter
        );

        LOGISTICS_POINT_ENTITY.setMarketBranded(true);
    }

    @Test
    void toDto() {
        when(addressConverter.toDto(ADDRESS_ENTITY)).thenReturn(ADDRESS_DTO);
        when(serviceConverter.toDto(SERVICE_CODE_ENTITY)).thenReturn(SERVICE_DTO);
        when(contactConverter.toDto(CONTACT_ENTITY)).thenReturn(CONTACT_DTO);
        when(phoneConverter.toDto(PHONE_ENTITY)).thenReturn(PHONE_DTO);
        when(scheduleDayConverter.toScheduleDayResponses(SCHEDULE_ENTITY))
            .thenReturn(Collections.singleton(SCHEDULE_DAY_DTO));

        softly.assertThat(converter.toDto(LOGISTICS_POINT_ENTITY.setMarketBranded(false)))
            .usingRecursiveComparison()
            .isEqualTo(LOGISTICS_POINT_RESPONSE);

        verify(addressConverter).toDto(ADDRESS_ENTITY);
        verify(serviceConverter).toDto(SERVICE_CODE_ENTITY);
        verify(contactConverter).toDto(CONTACT_ENTITY);
        verify(phoneConverter).toDto(PHONE_ENTITY);
        verify(scheduleDayConverter).toScheduleDayResponses(SCHEDULE_ENTITY);
    }

    @Test
    void toEntity() {
        when(contactConverter.toEntity(CONTACT_DTO)).thenReturn(CONTACT_ENTITY);
        when(phoneConverter.toEntity(Collections.singleton(PHONE_DTO))).thenReturn(Collections.singleton(PHONE_ENTITY));
        when(scheduleDayConverter.createSchedule(Collections.singleton(SCHEDULE_DAY_DTO))).thenReturn(SCHEDULE_ENTITY);

        softly.assertThat(converter.toEntity(
                LOGISTICS_POINT_CREATE_REQUEST,
                Collections.singleton(SERVICE_CODE_ENTITY), ADDRESS_ENTITY
            ))
            .usingRecursiveComparison()
            .isEqualTo(LOGISTICS_POINT_ENTITY.setId(null).setPartner(null));

        LOGISTICS_POINT_ENTITY.setPartner(PARTNER);
        LOGISTICS_POINT_ENTITY.setId(1L);

        verify(contactConverter).toEntity(CONTACT_DTO);
        verify(phoneConverter).toEntity(Collections.singleton(PHONE_DTO));
        verify(scheduleDayConverter).createSchedule(Collections.singleton(SCHEDULE_DAY_DTO));
    }

    @Test
    void propagateEntity() {
        when(contactConverter.propagateEntity(CONTACT_DTO, CONTACT_ENTITY)).thenReturn(CONTACT_ENTITY);
        when(phoneConverter.propagateEntity(PHONE_DTO, PHONE_ENTITY)).thenReturn(PHONE_ENTITY);
        when(scheduleDayConverter.replaceOrCreateSchedule(SCHEDULE_ENTITY, Collections.singleton(SCHEDULE_DAY_DTO)))
            .thenReturn(SCHEDULE_ENTITY);

        softly.assertThat(converter.propagateEntity(
                new LogisticsPoint()
                    .setId(1L)
                    .setExternalId("CODE")
                    .setAddress(ADDRESS_ENTITY.setComment("old comment"))
                    .setType(PointType.PICKUP_POINT)
                    .setSchedule(new Schedule().addScheduledDay(SCHEDULE_DAY_ENTITY))
                    .addPhone(PHONE_ENTITY)
                    .setContact(CONTACT_ENTITY)
                    .setPartner(null)
                    .setMarketBranded(false),
                LOGISTICS_POINT_UPDATE_REQUEST,
                null,
                Collections.singleton(SERVICE_CODE_ENTITY)
            ))
            .usingRecursiveComparison()
            .isEqualTo(LOGISTICS_POINT_ENTITY.setPartner(null));

        LOGISTICS_POINT_ENTITY.setPartner(PARTNER);

        verify(contactConverter).propagateEntity(CONTACT_DTO, CONTACT_ENTITY);
        verify(phoneConverter).propagateEntity(PHONE_DTO, PHONE_ENTITY);
        verify(scheduleDayConverter).replaceOrCreateSchedule(SCHEDULE_ENTITY, Collections.singleton(SCHEDULE_DAY_DTO));
    }

    @Test
    void propagateEntityWithAddress() {
        when(contactConverter.propagateEntity(CONTACT_DTO, CONTACT_ENTITY)).thenReturn(CONTACT_ENTITY);
        when(phoneConverter.propagateEntity(PHONE_DTO, PHONE_ENTITY)).thenReturn(PHONE_ENTITY);
        when(scheduleDayConverter.replaceOrCreateSchedule(SCHEDULE_ENTITY, Collections.singleton(SCHEDULE_DAY_DTO)))
            .thenReturn(SCHEDULE_ENTITY);

        softly.assertThat(converter.propagateEntity(
                new LogisticsPoint()
                    .setId(1L)
                    .setExternalId("CODE")
                    .setAddress(ADDRESS_ENTITY.setLocationId(654321))
                    .setType(PointType.PICKUP_POINT)
                    .setSchedule(new Schedule().addScheduledDay(SCHEDULE_DAY_ENTITY))
                    .addPhone(PHONE_ENTITY)
                    .setContact(CONTACT_ENTITY)
                    .setPartner(null)
                    .setMarketBranded(true),
                LOGISTICS_POINT_UPDATE_REQUEST_WITH_ADDRESS,
                ADDRESS_ENTITY.setLocationId(123456),
                Collections.singleton(SERVICE_CODE_ENTITY)
            ))
            .usingRecursiveComparison()
            .isEqualTo(LOGISTICS_POINT_ENTITY.setMarketBranded(false).setPartner(null));

        LOGISTICS_POINT_ENTITY.setPartner(PARTNER);

        verify(contactConverter).propagateEntity(CONTACT_DTO, CONTACT_ENTITY);
        verify(phoneConverter).propagateEntity(PHONE_DTO, PHONE_ENTITY);
        verify(scheduleDayConverter).replaceOrCreateSchedule(SCHEDULE_ENTITY, Collections.singleton(SCHEDULE_DAY_DTO));
    }

    @Test
    void propagateEntityIgnoreEmptyMarketBranded() {
        when(contactConverter.propagateEntity(CONTACT_DTO, CONTACT_ENTITY)).thenReturn(CONTACT_ENTITY);
        when(phoneConverter.propagateEntity(PHONE_DTO, PHONE_ENTITY)).thenReturn(PHONE_ENTITY);
        when(scheduleDayConverter.replaceOrCreateSchedule(SCHEDULE_ENTITY, Collections.singleton(SCHEDULE_DAY_DTO)))
            .thenReturn(SCHEDULE_ENTITY);

        softly.assertThat(converter.propagateEntity(
                new LogisticsPoint()
                    .setId(1L)
                    .setExternalId("CODE")
                    .setAddress(ADDRESS_ENTITY.setComment("old comment"))
                    .setType(PointType.PICKUP_POINT)
                    .setSchedule(new Schedule().addScheduledDay(SCHEDULE_DAY_ENTITY))
                    .addPhone(PHONE_ENTITY)
                    .setContact(CONTACT_ENTITY)
                    .setPartner(null)
                    .setMarketBranded(true),
                LOGISTICS_POINT_UPDATE_REQUEST.toBuilder().marketBranded(null).build(),
                null,
                Collections.singleton(SERVICE_CODE_ENTITY)
            ))
            .usingRecursiveComparison()
            .isEqualTo(LOGISTICS_POINT_ENTITY.setMarketBranded(true).setPartner(null));

        LOGISTICS_POINT_ENTITY.setPartner(PARTNER);

        verify(contactConverter).propagateEntity(CONTACT_DTO, CONTACT_ENTITY);
        verify(phoneConverter).propagateEntity(PHONE_DTO, PHONE_ENTITY);
        verify(scheduleDayConverter).replaceOrCreateSchedule(SCHEDULE_ENTITY, Collections.singleton(SCHEDULE_DAY_DTO));
    }
}
