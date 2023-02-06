package ru.yandex.market.logistics.nesu.facade;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTimeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.response.capacity.PartnerCapacityDayOffSearchResult;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.DeliveryOptionShipment;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.PartnerDaysOff;

@DisplayName("Расчет даты ближайшей отгрузки варианта доставки")
class ShipmentCalculatorDatesTest extends AbstractShipmentCalculatorTest {

    @BeforeEach
    void setupPartner() {
        shipment.setPartnerId(DELIVERY_SERVICE_ID);
    }

    @Test
    @DisplayName("Забор, нет расписаний")
    void noSchedule() {
        sortingCenter = partner(SORTING_CENTER_ID, List.of(), PartnerType.SORTING_CENTER);
        deliveryService = deliveryService(List.of());

        softly.assertThat(calculateShipments()).isEmpty();
    }

    @Test
    @DisplayName("Забор, есть расписание на сегодня и есть заявка на отгрузку")
    void exactDaySameDate() {
        deliveryService = deliveryService(List.of(
            scheduleDay(4)
        ));

        partnersWithShipmentApplications = Set.of(DELIVERY_SERVICE_ID);
        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate);
    }

    @Test
    @DisplayName("Забор, есть расписание на сегодня и завтра и нет заявки на отгрузку")
    void exactDaySameDateNoShipmentApplication() {
        deliveryService = deliveryService(List.of(
            scheduleDay(4),
            scheduleDay(5)
        ));

        partnersWithShipmentApplications = Set.of();
        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(1));
    }

    @Test
    @DisplayName("Забор, есть расписание на сегодня и завтра и нет заявки на отгрузку, собственная СД")
    void exactDaySameDateNoShipmentApplicationOwnDelivery() {
        deliveryService = partner(
            DELIVERY_SERVICE_ID,
            List.of(scheduleDay(4), scheduleDay(5)),
            PartnerType.OWN_DELIVERY
        );

        partnersWithShipmentApplications = Set.of();
        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate);
    }

    @Test
    @DisplayName("Забор, нет заявки на отгрузку, коллекция заявок на отгрузку null (не указан склад отгрузки)")
    void exactDaySameDateNoShipmentApplicationAndShopWarehouse() {
        deliveryService = deliveryService(List.of(
            scheduleDay(4),
            scheduleDay(5)
        ));

        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate);
    }

    @Test
    @DisplayName("Забор, есть расписание на текущую неделю")
    void sameWeek() {
        deliveryService = deliveryService(List.of(
            scheduleDay(7),
            scheduleDay(6)
        ));

        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.with(DayOfWeek.SATURDAY));
    }

    @Test
    @DisplayName("Забор, есть расписание на следующую неделю")
    void nextWeek() {
        deliveryService = deliveryService(List.of(
            scheduleDay(3),
            scheduleDay(2)
        ));

        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.with(DayOfWeek.TUESDAY).plusWeeks(1));
    }

    @Test
    @DisplayName("Самопривоз по расписанию склада")
    void warehouseImport() {
        shipment.setType(ShipmentType.IMPORT);

        deliveryService = deliveryService(List.of(
            scheduleDay(4)
        ));
        partnerWarehouses.put(DELIVERY_SERVICE_ID, warehouse(Set.of(
            scheduleDay(5),
            scheduleDay(3)
        )));

        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.with(DayOfWeek.FRIDAY));
    }

    @Test
    @DisplayName("В фильтре указана дата")
    void filterShipmentDate() {
        initialShipmentDate = initialShipmentDate.with(DayOfWeek.SUNDAY);

        deliveryService = deliveryService(List.of(
            scheduleDay(6),
            scheduleDay(5)
        ));

        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.with(DayOfWeek.FRIDAY).plusWeeks(1));
    }

    @Test
    @DisplayName("Учет времени сортировки в СЦ")
    void sortingCenterHandlingTime() {
        shipment.setPartnerId(SORTING_CENTER_ID);

        int handlingTime = 2;
        partnerRelations.put(DELIVERY_SERVICE_ID, partnerRelationDto(handlingTime));

        partnersWithShipmentApplications = Set.of(SORTING_CENTER_ID);
        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getDeliveryServiceShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(handlingTime));
    }

    @Test
    @DisplayName("Учет времени отгрузки")
    void sortingCenterShipmentTime() {
        shipment.setPartnerId(SORTING_CENTER_ID);

        partnerRelations.put(DELIVERY_SERVICE_ID, partnerRelationDto(LocalTime.now(clock).minusMinutes(30)));

        partnersWithShipmentApplications = Set.of(SORTING_CENTER_ID);
        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getDeliveryServiceShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(1));
    }

    @Test
    @DisplayName("Учет времени отгрузки и таймзоны склада СЦ")
    void sortingCenterShipmentTimeTimezoneOffset() {
        shipment.setPartnerId(SORTING_CENTER_ID);
        sortingCenterTimeZoneOffsetSeconds = sortingCenterTimeZoneOffsetSeconds + DateTimeConstants.SECONDS_PER_HOUR;

        partnerRelations.put(DELIVERY_SERVICE_ID, partnerRelationDto(LocalTime.now(clock).plusMinutes(30)));

        partnersWithShipmentApplications = Set.of(SORTING_CENTER_ID);
        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getDeliveryServiceShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(1));
    }

    @Test
    @DisplayName("Учет времени отгрузки, интервал отгрузки еще не наступил")
    void sortingCenterBeforeShipmentTime() {
        shipment.setPartnerId(SORTING_CENTER_ID);

        partnerRelations.put(DELIVERY_SERVICE_ID, partnerRelationDto(LocalTime.now(clock).plusMinutes(30)));

        partnersWithShipmentApplications = Set.of(SORTING_CENTER_ID);
        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getDeliveryServiceShipmentDate)
            .containsExactly(initialShipmentDate);
    }

    @Test
    @DisplayName("Учет времени отгрузки: если дата отгрузки не сегодня, то она не сдвигается")
    void sortingCenterShipmentDateAsNotToday() {
        initialShipmentDate = LocalDate.now(clock).plusDays(1);
        shipment.setPartnerId(SORTING_CENTER_ID);

        partnerRelations.put(DELIVERY_SERVICE_ID, partnerRelationDto(LocalTime.now(clock).minusMinutes(30)));

        partnersWithShipmentApplications = Set.of(SORTING_CENTER_ID);
        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getDeliveryServiceShipmentDate)
            .containsExactly(initialShipmentDate);
    }

    @Test
    @DisplayName("Учет времени отгрузки: если ночная отгрузка, то дата не сдвигается")
    void sortingCenterNightShipment() {
        initialShipmentDate = LocalDate.now(clock).plusDays(1);
        shipment.setPartnerId(SORTING_CENTER_ID);

        partnerRelations.put(DELIVERY_SERVICE_ID, partnerRelationDto(LocalTime.of(0, 0)));

        partnersWithShipmentApplications = Set.of(SORTING_CENTER_ID);
        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getDeliveryServiceShipmentDate)
            .containsExactly(initialShipmentDate);
    }

    @Test
    @DisplayName("Учет дэй-оффов для отгрузки из магазина в СД")
    void dayOffDeliveryServiceShipment() {
        partnerDaysOff = new PartnerDaysOff(
            List.of(
                PartnerCapacityDayOffSearchResult.builder()
                    .partnerId(DELIVERY_SERVICE_ID)
                    .deliveryType(DeliveryType.COURIER)
                    .days(List.of(initialShipmentDate))
                    .build()
            ),
            DELIVERY_TYPE_CONVERTER
        );

        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(1));
    }

    @Test
    @DisplayName("Учет нескольких дэй-оффов подряд")
    void multipleDaysOff() {
        partnerDaysOff = new PartnerDaysOff(
            List.of(
                PartnerCapacityDayOffSearchResult.builder()
                    .partnerId(DELIVERY_SERVICE_ID)
                    .deliveryType(DeliveryType.COURIER)
                    .days(List.of(initialShipmentDate, initialShipmentDate.plusDays(1)))
                    .build()
            ),
            DELIVERY_TYPE_CONVERTER
        );

        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(2));
    }

    @Test
    @DisplayName("Учет дэй-оффов, если не задан способ доставки")
    void dayOffNoDeliveryType() {
        partnerDaysOff = new PartnerDaysOff(
            List.of(
                PartnerCapacityDayOffSearchResult.builder()
                    .partnerId(DELIVERY_SERVICE_ID)
                    .deliveryType(null)
                    .days(List.of(initialShipmentDate))
                    .build()
            ),
            DELIVERY_TYPE_CONVERTER
        );

        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(1));
    }

    @Test
    @DisplayName("Учет дэй-оффов для отгрузки из магазина в СЦ")
    void dayOffSortingCenterShipment() {
        shipment.setPartnerId(SORTING_CENTER_ID);

        partnerDaysOff = new PartnerDaysOff(
            List.of(
                PartnerCapacityDayOffSearchResult.builder()
                    .partnerId(SORTING_CENTER_ID)
                    .deliveryType(DeliveryType.COURIER)
                    .days(List.of(initialShipmentDate))
                    .build()
            ),
            DELIVERY_TYPE_CONVERTER
        );

        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(1));
    }

    @Test
    @DisplayName("Учет дэй-оффов для отгрузки из СД в СЦ")
    void dayOffSortingCenterToDeliveryServiceShipment() {
        shipment.setPartnerId(SORTING_CENTER_ID);

        partnerDaysOff = new PartnerDaysOff(
            List.of(
                PartnerCapacityDayOffSearchResult.builder()
                    .partnerId(SORTING_CENTER_ID)
                    .deliveryType(DeliveryType.COURIER)
                    .days(List.of(initialShipmentDate))
                    .build(),
                PartnerCapacityDayOffSearchResult.builder()
                    .partnerId(DELIVERY_SERVICE_ID)
                    .deliveryType(DeliveryType.COURIER)
                    .days(List.of(initialShipmentDate.plusDays(1)))
                    .build()
            ),
            DELIVERY_TYPE_CONVERTER
        );

        List<DeliveryOptionShipment> shipments = calculateShipments();

        softly.assertThat(shipments)
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(1));
        softly.assertThat(shipments)
            .extracting(DeliveryOptionShipment::getDeliveryServiceShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(2));
    }

    @Test
    @DisplayName("Учет выходных для отгрузки из магазина в СЦ")
    void holidaySortingCenterShipment() {
        shipment.setPartnerId(SORTING_CENTER_ID);

        partnerHolidays.put(SORTING_CENTER_ID, Set.of(initialShipmentDate));

        softly.assertThat(calculateShipments())
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(1));
    }

    @Test
    @DisplayName("Учет выходных для отгрузки из СД в СЦ")
    void holidaySortingCenterToDeliveryServiceShipment() {
        shipment.setPartnerId(SORTING_CENTER_ID);

        partnerHolidays.put(SORTING_CENTER_ID, Set.of(initialShipmentDate));
        partnerHolidays.put(DELIVERY_SERVICE_ID, Set.of(initialShipmentDate.plusDays(1)));

        List<DeliveryOptionShipment> shipments = calculateShipments();

        softly.assertThat(shipments)
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(1));
        softly.assertThat(shipments)
            .extracting(DeliveryOptionShipment::getDeliveryServiceShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(2));
    }

    @Test
    @DisplayName("Учет отгрузок с отправленными реестрами для отгрузок ИМ - СД")
    void shipmentDatesWithRegistriesSentFilteredForShipmentFromShopToDeliveryService() {
        shipment.setPartnerId(DELIVERY_SERVICE_ID);

        shipmentDatesWithRegistriesSent = Map.of(
            Pair.of(ShipmentType.WITHDRAW, DELIVERY_SERVICE_ID),
            Set.of(initialShipmentDate, initialShipmentDate.plusDays(1))
        );

        List<DeliveryOptionShipment> shipments = calculateShipments();

        softly.assertThat(shipments)
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(2));
        softly.assertThat(shipments)
            .extracting(DeliveryOptionShipment::getDeliveryServiceShipmentDate)
            .containsExactly(initialShipmentDate.plusDays(2));
    }

    @Test
    @DisplayName("Для отгрузок ИМ - СЦ не учитываются отправленные в СД реестры")
    void shipmentDatesWithRegistriesSentNotFilteredForShipmentFromShopToSortingCenter() {
        shipment.setPartnerId(SORTING_CENTER_ID);

        shipmentDatesWithRegistriesSent = Map.of(
            Pair.of(ShipmentType.WITHDRAW, DELIVERY_SERVICE_ID),
            Set.of(initialShipmentDate, initialShipmentDate.plusDays(1))
        );

        List<DeliveryOptionShipment> shipments = calculateShipments();

        softly.assertThat(shipments)
            .extracting(DeliveryOptionShipment::getInitialShipmentDate)
            .containsExactly(initialShipmentDate);
        softly.assertThat(shipments)
            .extracting(DeliveryOptionShipment::getDeliveryServiceShipmentDate)
            .containsExactly(initialShipmentDate);
    }

    @Nonnull
    private PartnerRelationEntityDto partnerRelationDto(int handlingTime) {
        return PartnerRelationEntityDto.newBuilder()
            .handlingTime(handlingTime)
            .intakeSchedule(Set.of())
            .build();
    }

    @Nonnull
    private PartnerRelationEntityDto partnerRelationDto(LocalTime intakeScheduleTimeFrom) {
        return PartnerRelationEntityDto.newBuilder()
            .handlingTime(0)
            .intakeSchedule(Set.of(
                new ScheduleDayResponse(1L, 4, intakeScheduleTimeFrom, intakeScheduleTimeFrom.plusHours(1))
            ))
            .build();
    }
}
