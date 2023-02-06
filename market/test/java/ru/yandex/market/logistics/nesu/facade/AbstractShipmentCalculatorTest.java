package ru.yandex.market.logistics.nesu.facade;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTimeConstants;
import org.junit.jupiter.api.BeforeEach;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.api.converter.EnumConverter;
import ru.yandex.market.logistics.nesu.api.model.Partner;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.dto.DeliverySettingShipment;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsFilterShipment;
import ru.yandex.market.logistics.nesu.dto.enums.DeliveryType;
import ru.yandex.market.logistics.nesu.model.entity.SenderDeliverySettings;
import ru.yandex.market.logistics.nesu.service.deliveryoption.calculator.ShipmentCalculator;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.DeliveryOptionShipment;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.PartnerDaysOff;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.ShipmentCalculatorData;
import ru.yandex.market.logistics.nesu.utils.ScheduleUtils;

abstract class AbstractShipmentCalculatorTest extends AbstractTest {
    private static final Instant NOW = Instant.parse("2020-02-20T12:00:00Z");

    static final long DELIVERY_SERVICE_ID = 100L;
    static final long SORTING_CENTER_ID = 101L;

    static final Partner DELIVERY_SERVICE_PARTNER = Partner.builder()
        .partnerType(PartnerType.DELIVERY)
        .id(DELIVERY_SERVICE_ID)
        .build();
    static final Partner SORTING_CENTER_PARTNER = Partner.builder()
        .partnerType(PartnerType.SORTING_CENTER)
        .id(SORTING_CENTER_ID)
        .build();

    static final Function<ru.yandex.market.logistics.management.entity.type.DeliveryType, DeliveryType>
        DELIVERY_TYPE_CONVERTER = type -> new EnumConverter().toEnum(type, DeliveryType.class);

    DeliveryOptionsFilterShipment shipment = new DeliveryOptionsFilterShipment();
    PartnerResponse sortingCenter;
    Integer sortingCenterTimeZoneOffsetSeconds;
    Map<Long, SenderDeliverySettings> deliverySettings = new HashMap<>();
    Map<Long, LogisticsPointResponse> partnerWarehouses = new HashMap<>();
    Map<Long, PartnerRelationEntityDto> partnerRelations = new HashMap<>();
    PartnerDaysOff partnerDaysOff;
    Map<Long, Set<LocalDate>> partnerHolidays = new HashMap<>();
    Map<Long, List<DeliverySettingShipment>> shipmentSettingsByPartnerId = Map.of(
        DELIVERY_SERVICE_ID,
        StreamEx.of(ShipmentType.IMPORT, ShipmentType.WITHDRAW)
            .cross(DELIVERY_SERVICE_PARTNER, SORTING_CENTER_PARTNER)
            .mapKeyValue(
                (shipmentType, partner) -> DeliverySettingShipment.builder()
                    .shipmentType(shipmentType)
                    .partner(partner)
                    .build()
            )
            .toList()
    );
    Set<Long> partnersWithShipmentApplications = null;
    Map<Pair<ShipmentType, Long>, Set<LocalDate>> shipmentDatesWithRegistriesSent = Map.of();
    LocalDate initialShipmentDate = LocalDate.of(2020, 2, 20);
    PartnerResponse deliveryService;

    TestableClock clock;

    @BeforeEach
    void setup() {
        shipment.setType(null);
        shipment.setIncludeNonDefault(true);
        sortingCenter = partner(SORTING_CENTER_ID, fullWeek(), PartnerType.SORTING_CENTER);
        sortingCenterTimeZoneOffsetSeconds = TimeZone.getDefault().getRawOffset() / DateTimeConstants.MILLIS_PER_SECOND;
        deliverySettings.put(DELIVERY_SERVICE_ID, new SenderDeliverySettings());
        deliveryService = deliveryService(fullWeek());
        partnerDaysOff = new PartnerDaysOff(List.of(), DELIVERY_TYPE_CONVERTER);

        clock = new TestableClock();
        clock.setFixed(NOW, ZoneId.systemDefault());

        softly.assertThat(initialShipmentDate.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
    }

    @Nonnull
    List<DeliveryOptionShipment> calculateShipments() {
        return new ShipmentCalculator(
            clock,
            ShipmentCalculatorData.builder()
                .filterShipment(shipment)
                .sortingCenter(sortingCenter)
                .sortingCenterTimeZoneOffsetSeconds(sortingCenterTimeZoneOffsetSeconds)
                .deliverySettings(deliverySettings)
                .partnerWarehouses(partnerWarehouses)
                .partnerHandlingTime(
                    EntryStream.of(partnerRelations)
                        .mapValues(PartnerRelationEntityDto::getHandlingTime)
                        .toMap()
                )
                .partnerDaysOff(partnerDaysOff)
                .partnerHolidays(partnerHolidays)
                .intakeSchedule(
                    Map.of(
                        deliveryService.getId(), deliveryService.getIntakeSchedule(),
                        sortingCenter.getId(), sortingCenter.getIntakeSchedule()
                    )
                )
                .partnerRelationIntakeSchedule(
                    EntryStream.of(partnerRelations)
                        .mapValues(PartnerRelationEntityDto::getIntakeSchedule)
                        .toMap()
                )
                .availableShipmentsByPartnerId(shipmentSettingsByPartnerId)
                .partnersWithTodayWithdrawShipmentApplication(partnersWithShipmentApplications)
                .shipmentDatesWithRegistriesSent(shipmentDatesWithRegistriesSent)
                .senderWarehouseWorkingDays(ScheduleUtils.EVERY_DAY)
                .virtualDeliveryServicesByHiddenReal(Map.of())
                .build()
        )
            .getShipments(deliveryService, DeliveryType.COURIER, initialShipmentDate)
            .collect(Collectors.toList());
    }

    @Nonnull
    PartnerResponse deliveryService(List<ScheduleDayResponse> intakeSchedule) {
        return partner(DELIVERY_SERVICE_ID, intakeSchedule, PartnerType.DELIVERY);
    }

    @Nonnull
    PartnerResponse partner(long id, List<ScheduleDayResponse> intakeSchedule, PartnerType type) {
        return PartnerResponse.newBuilder()
            .id(id)
            .marketId(0L)
            .partnerType(type)
            .name(String.valueOf(id))
            .readableName(String.format("%s partner %d", type, id))
            .status(PartnerStatus.ACTIVE)
            .intakeSchedule(intakeSchedule)
            .build();
    }

    @Nonnull
    LogisticsPointResponse warehouse(Set<ScheduleDayResponse> schedule) {
        Long id = 1000 + DELIVERY_SERVICE_ID;
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .partnerId(DELIVERY_SERVICE_ID)
            .type(PointType.WAREHOUSE)
            .name("Warehouse " + id)
            .active(true)
            .schedule(schedule)
            .build();
    }

    @Nonnull
    private List<ScheduleDayResponse> fullWeek() {
        return IntStream.rangeClosed(1, 7)
            .mapToObj(this::scheduleDay)
            .collect(Collectors.toList());
    }

    @Nonnull
    ScheduleDayResponse scheduleDay(int day) {
        return new ScheduleDayResponse(500L + day, day, LocalTime.of(1, 0), LocalTime.of(2, 0));
    }

}
