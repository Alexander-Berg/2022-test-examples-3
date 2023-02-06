package ru.yandex.market.logistics.management.util;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.logistics.management.domain.entity.Address;
import ru.yandex.market.logistics.management.domain.entity.Calendar;
import ru.yandex.market.logistics.management.domain.entity.CalendarDay;
import ru.yandex.market.logistics.management.domain.entity.Contact;
import ru.yandex.market.logistics.management.domain.entity.LogisticsPoint;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamType;
import ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamValue;
import ru.yandex.market.logistics.management.domain.entity.PartnerHandlingTime;
import ru.yandex.market.logistics.management.domain.entity.PartnerRelation;
import ru.yandex.market.logistics.management.domain.entity.Phone;
import ru.yandex.market.logistics.management.domain.entity.PlatformClient;
import ru.yandex.market.logistics.management.domain.entity.PlatformClientPartner;
import ru.yandex.market.logistics.management.domain.entity.RadialLocationZone;
import ru.yandex.market.logistics.management.domain.entity.RegionEntity;
import ru.yandex.market.logistics.management.domain.entity.Schedule;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.PhoneType;
import ru.yandex.market.logistics.management.domain.entity.type.PickupPointType;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.domain.entity.type.ShipmentType;
import ru.yandex.market.logistics.management.domain.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.management.entity.type.TaxationSystem;
import ru.yandex.market.logistics.management.repository.LogisticsPointRepository;
import ru.yandex.market.logistics.management.repository.PartnerExternalParamRepository;
import ru.yandex.market.logistics.management.repository.PartnerExternalParamTypeRepository;
import ru.yandex.market.logistics.management.repository.PartnerHandlingTimeRepository;
import ru.yandex.market.logistics.management.repository.PartnerRelationRepository;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.repository.PlatformClientPartnerRepository;
import ru.yandex.market.logistics.management.repository.geoBase.GeoBaseRepository;

@TestComponent
@RequiredArgsConstructor
@Transactional
public class BusinessWarehouseFactory {
    private static final LocalDate LOCAL_DATE = LocalDate.of(2018, 10, 2);

    private static final Map<Long, PartnerType> PARTNER_TYPES = Map.of(
        0L, PartnerType.DROPSHIP,
        1L, PartnerType.DROPSHIP_BY_SELLER
    );

    private static final Map<Long, PartnerStatus> PARTNER_STATUSES = Map.of(
        0L, PartnerStatus.ACTIVE,
        1L, PartnerStatus.TESTING
    );

    private static final Map<Long, PointType> POINT_TYPE = Map.of(
        0L, PointType.WAREHOUSE,
        1L, PointType.PICKUP_POINT
    );

    private static final Map<Long, PickupPointType> PICKUP_POINT_TYPE = Map.of(
        0L, PickupPointType.PICKUP_POINT,
        1L, PickupPointType.TERMINAL,
        2L, PickupPointType.POST_OFFICE
    );

    private static final Map<Long, PhoneType> PHONE_TYPE = Map.of(
        0L, PhoneType.PRIMARY,
        1L, PhoneType.ADDITIONAL
    );

    private static final Map<Long, ShipmentType> SHIPMENT_TYPES = Map.of(
        0L, ShipmentType.IMPORT,
        1L, ShipmentType.WITHDRAW
    );

    private final PartnerRepository partnerRepository;
    private final LogisticsPointRepository logisticsPointRepository;
    private final GeoBaseRepository geoBaseRepository;
    private final PartnerExternalParamTypeRepository partnerExternalParamTypeRepository;
    private final PartnerHandlingTimeRepository partnerHandlingTimeRepository;
    private final PartnerExternalParamRepository partnerExternalParamRepository;
    private final PlatformClientPartnerRepository platformClientPartnerRepository;
    private final PartnerRelationRepository partnerRelationRepository;

    private final UuidGenerator uuidGenerator;

    private final PlatformClient client = new PlatformClient().setId(1L).setName("blue");
    private PartnerExternalParamType partnerExternalParamType;

    private Iterator<LogisticsPoint> pointIterator;
    private Iterator<Partner> partnerIterator;
    private Iterator<RegionEntity> regionEntityIterator;
    private List<RegionEntity> regionEntities;

    private long size;

    public void generateBusinessWarehouses(long size) {
        this.size = size;
        partnerExternalParamType = partnerExternalParamTypeRepository.findByKeyOrThrow("IS_COMMON");

        regionEntities = geoBaseRepository.saveAll(generate(0, 10, this::regionEntity));
        geoBaseRepository.saveAll(regionEntities);
        regionEntityIterator = regionEntities.iterator();

        pointIterator = logisticsPointRepository.saveAll(generate(0, size * 10, this::logisticsPoint)).iterator();

        List<Partner> partners = partnerRepository.saveAll(generate(1, size, this::partner));
        partnerIterator = partners.iterator();

        partnerHandlingTimeRepository.saveAll(generate(0, size, this::partnerHandlingTime)).iterator();

        partnerIterator = partners.iterator();
        platformClientPartnerRepository.saveAll(generate(0, size, this::platformClientPartner));

        partnerIterator = partners.iterator();
        partnerExternalParamRepository.saveAll(generate(0, size, this::partnerExternalParamValue));

        partnerIterator = partners.iterator();
        partnerRelationRepository.saveAll(generate(0, size, this::partnerRelation));
    }

    @Nonnull
    private Schedule schedule(long seed) {
        return new Schedule()
            .setScheduleDays(new HashSet<>(generate(seed, 15, this::scheduleDay)));
    }

    @Nonnull
    private Calendar calendar(long seed) {
        return new Calendar().addCalendarDays(generate(seed, 10, this::calendarDay));
    }

    @Nonnull
    private <T> List<T> generate(long seed, long size, Function<Long, T> creator) {
        return Stream.iterate(seed, s -> s + 1).limit(size).map(creator).collect(Collectors.toList());
    }

    @Nonnull
    private CalendarDay calendarDay(long seed) {
        return new CalendarDay().setDay(LOCAL_DATE).setIsHoliday(seed % 2 == 0);
    }

    @Nonnull
    private ScheduleDay scheduleDay(long seed) {
        return new ScheduleDay()
            .setDay((int) (seed % 7))
            .setFrom(LocalTime.MIDNIGHT)
            .setTo(LocalTime.MAX)
            .setIsMain(seed % 2 == 0);
    }

    @Nonnull
    private PartnerHandlingTime partnerHandlingTime(long seed) {
        return new PartnerHandlingTime()
            .setPartner(partnerIterator.next())
            .setLocationTo((int) (seed % 87))
            .setLocationFrom((int) (seed % 56))
            .setHandlingTime(Duration.ofDays(seed % 31));
    }

    @Nonnull
    private Address address(long seed) {
        return new Address()
            .setLocationId(225)
            .setCountry(uuidGenerator.randomUuid())
            .setSettlement(uuidGenerator.randomUuid())
            .setPostCode(uuidGenerator.randomUuid().substring(1, 4))
            .setLatitude(BigDecimal.valueOf(1))
            .setLongitude(BigDecimal.valueOf(2))
            .setStreet(uuidGenerator.randomUuid())
            .setHouse(uuidGenerator.randomUuid())
            .setHousing(uuidGenerator.randomUuid())
            .setBuilding(uuidGenerator.randomUuid())
            .setApartment(uuidGenerator.randomUuid())
            .setRegion(uuidGenerator.randomUuid())
            .setSubRegion(uuidGenerator.randomUuid())
            .setComment(uuidGenerator.randomUuid())
            .setAddressString(uuidGenerator.randomUuid())
            .setShortAddressString(uuidGenerator.randomUuid())
            .setExactLocationId(225);
    }

    @Nonnull
    private Phone phone(long seed) {
        return new Phone()
            .setComment(uuidGenerator.randomUuid())
            .setInternalNumber(uuidGenerator.randomUuid())
            .setNumber(uuidGenerator.randomUuid())
            .setType(PHONE_TYPE.get(seed % 2));
    }

    @Nonnull
    private Contact contact(long seed) {
        return new Contact()
            .setName(uuidGenerator.randomUuid())
            .setPatronymic(uuidGenerator.randomUuid())
            .setSurname(uuidGenerator.randomUuid());
    }

    @Nonnull
    private RadialLocationZone radialLocationZone(long seed) {
        if (!regionEntityIterator.hasNext()) {
            regionEntityIterator = regionEntities.iterator();
        }
        return new RadialLocationZone()
            .setIsPrivate(seed % 2 == 0)
            .setDeliveryDuration(seed % 40)
            .setName(uuidGenerator.randomUuid())
            .setRadius(seed * 30)
            .setRegionId(regionEntityIterator.next().getId());
    }

    @Nonnull
    private LogisticsPoint logisticsPoint(long seed) {
        return new LogisticsPoint()
            .setExternalId(uuidGenerator.randomUuid())
            .setAddress(generate(seed, 1, this::address).get(0))
            .setType(POINT_TYPE.get(seed % 2))
            .setBusinessId(seed + 98)
            .setFrozen(seed % 2 == 0)
            .setPickupPointType(PICKUP_POINT_TYPE.get(seed % 3))
            .setName(uuidGenerator.randomUuid())
            .setActive(seed % 10 == 0)
            .setCashAllowed(seed % 2 == 0)
            .setPrepayAllowed(seed % 2 == 0)
            .setCardAllowed(seed % 2 == 0)
            .setInstruction(uuidGenerator.randomUuid())
            .setCourierInstruction(uuidGenerator.randomUuid())
            .setReturnAllowed(seed % 2 == 0)
            .setStoragePeriod((int) (seed % 67))
            .setMaxWeight((double) (seed / 3))
            .setMaxLength((int) (seed % 23))
            .setMaxWidth((int) (seed % 44))
            .setMaxHeight((int) (seed % 76))
            .setExternalHash("ext hash " + seed)
            .setMarketBranded(seed % 2 == 0)
            .setAvailableForOnDemand(seed % 2 == 0)
            .setDeferredCourierAvailable(seed % 2 == 0)
            .setDarkStore(seed % 2 == 0)
            .setHandlingTime(Duration.ofDays(seed % 30))
            .setYandexMapPermalink(seed * 10)
            .addAllPhones(generate(0, 1, this::phone))
            .setContact(generate(seed, 1, this::contact).get(0))
            .setSchedule(generate(seed + 2, 1, this::schedule).get(0))
            .setRadialLocationZones(new HashSet<>(generate((seed + 1) * size * size, 3, this::radialLocationZone)));
    }

    @Nonnull
    private PartnerExternalParamValue partnerExternalParamValue(long seed) {
        return new PartnerExternalParamValue()
            .setPartner(partnerIterator.next())
            .setParamType(partnerExternalParamType)
            .setValue(uuidGenerator.randomUuid());
    }

    @Nonnull
    private PlatformClientPartner platformClientPartner(long seed) {
        return new PlatformClientPartner()
            .setStatus(PARTNER_STATUSES.get(seed % 2))
            .setPartner(partnerIterator.next())
            .setPlatformClient(client);
    }

    @Nonnull
    private RegionEntity regionEntity(long seed) {
        return new RegionEntity().setId((int) seed).setName(uuidGenerator.randomUuid())
            .setType(RegionType.CITY_DISTRICT);
    }

    @Nonnull
    private PartnerRelation partnerRelation(long seed) {
        Partner toPartner = partnerRepository.save(basePartner((seed + 1) * size));
        return new PartnerRelation()
            .setEnabled(seed % 2 == 0)
            .setHandlingTime(100)
            .setFromPartner(partnerIterator.next())
            .setReturnPartner(toPartner)
            .setShipmentType(SHIPMENT_TYPES.get(seed % 2))
            .setToPartner(toPartner);
    }

    @Nonnull
    private Partner basePartner(long seed) {
        return new Partner()
            .setStatus(PARTNER_STATUSES.get(seed % 2))
            .setId(seed)
            .setPartnerType(PARTNER_TYPES.get(seed % 2))
            .setName(uuidGenerator.randomUuid())
            .setReadableName(uuidGenerator.randomUuid())
            .setCodeName(uuidGenerator.randomUuid())
            .setAbbreviatedLabelName(uuidGenerator.randomUuid())
            .setMarketId(seed * 5)
            .setBusinessId(seed * 7 + 3)
            .setLocationId((int) (seed % 120))
            .setTrackingType(uuidGenerator.randomUuid() + seed)
            .setStockSyncEnabled(seed % 2 == 0)
            .setAutoSwitchStockSyncEnabled(seed % 2 == 1)
            .setStockSyncSwitchReason(StockSyncSwitchReason.NEW)
            .setLogoUrl(uuidGenerator.randomUuid() + seed)
            .setBillingClientId(seed + 40)
            .setRating((int) (seed % 100))
            .setDomain(uuidGenerator.randomUuid())
            .setDefaultOutletName(uuidGenerator.randomUuid())
            .setPassportUid(seed * 90)
            .setBillingPersonId(seed + 200)
            .setTaxationSystem(TaxationSystem.PATENT)
            .setOebsVirtualAccountNumber(uuidGenerator.randomUuid())
            .setContractSignedSince(LOCAL_DATE)
            .setRealSupplierId(uuidGenerator.randomUuid());
    }

    @Nonnull
    private Partner partner(long seed) {
        return basePartner(seed)
            .addLogisticsPoints(Stream.generate(() -> pointIterator.next()).limit(10).collect(Collectors.toSet()))
            .setCalendarId(generate(seed, 1, this::calendar).get(0).getId());
    }
}
