package ru.yandex.market.logistics.nesu.facade;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOption;
import ru.yandex.market.logistics.delivery.calculator.client.model.TariffType;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCourierDayScheduleResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.converter.CourierScheduleConverter;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionTimeInterval;
import ru.yandex.market.logistics.nesu.service.deliveryoption.calculator.DeliveryDateCalculator;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.DeliverySchedule;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.PickupPointsData;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistics.nesu.utils.CommonsConstants.MOSCOW_GEO_ID;

@DisplayName("Расчет даты доставки")
class DeliveryDateCalculatorTest extends AbstractTest {

    private static final long DELIVERY_SERVICE_ID = 100L;
    private static final int LOCATION_TO = MOSCOW_GEO_ID;
    private static final LocalDate SHIPMENT_DATE = LocalDate.of(2019, 8, 6);

    private final Map<Long, PartnerCourierDayScheduleResponse> couriersSchedule = new HashMap<>();
    private final Map<Long, Set<LocalDate>> courierHolidays = new HashMap<>();
    private final Map<Long, Set<ScheduleDayResponse>> pickupPointSchedule = new HashMap<>();
    private final DeliveryOption.DeliveryOptionBuilder optionBuilder = DeliveryOption.builder()
        .tariffType(TariffType.COURIER)
        .deliveryServiceId(DELIVERY_SERVICE_ID);

    @Test
    @DisplayName("Доставка почтой")
    void pickupPointDelivery() {
        optionBuilder.tariffType(TariffType.POST);
        DeliverySchedule delivery = calculateFirst(22);
        softly.assertThat(delivery.getDeliveryInterval().getDeliveryMin()).isEqualTo(LocalDate.of(2019, 8, 6));
        softly.assertThat(delivery.getDeliveryInterval().getDeliveryMax()).isEqualTo(LocalDate.of(2019, 8, 28));
        softly.assertThat(delivery.getCourierSchedule()).isNull();
        softly.assertThat(delivery.getPickupPointIds()).isNull();
    }

    @Test
    @DisplayName("Нет расписаний курьеров")
    void shipmentDate() {
        Optional<DeliverySchedule> delivery = calculateFirstOptional(33);
        softly.assertThat(delivery).isEmpty();
    }

    @Test
    @DisplayName("Существующее расписание курьеров")
    void scheduleDay() {
        couriersSchedule.put(DELIVERY_SERVICE_ID, PartnerCourierDayScheduleResponse.builder()
            .locationId(LOCATION_TO)
            .partnerId(DELIVERY_SERVICE_ID)
            .schedule(List.of(scheduleDay(2)))
            .build()
        );

        DeliverySchedule delivery = calculateFirst(10);
        softly.assertThat(delivery.getDeliveryInterval().getDeliveryMin()).isEqualTo(LocalDate.of(2019, 8, 6));
        softly.assertThat(delivery.getDeliveryInterval().getDeliveryMax()).isEqualTo(LocalDate.of(2019, 8, 20));
        softly.assertThat(delivery.getCourierSchedule().getSchedule())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(scheduleDto(2000, 2, 4));
    }

    @Test
    @DisplayName("Расписание на следующий день")
    void nextScheduleDay() {
        couriersSchedule.put(DELIVERY_SERVICE_ID, PartnerCourierDayScheduleResponse.builder()
            .locationId(LOCATION_TO)
            .partnerId(DELIVERY_SERVICE_ID)
            .schedule(List.of(scheduleDay(5)))
            .build()
        );

        DeliverySchedule delivery = calculateFirst(2);
        softly.assertThat(delivery.getDeliveryInterval().getDeliveryMin()).isEqualTo(LocalDate.of(2019, 8, 9));
        softly.assertThat(delivery.getDeliveryInterval().getDeliveryMax()).isEqualTo(LocalDate.of(2019, 8, 9));
        softly.assertThat(delivery.getCourierSchedule().getSchedule())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(scheduleDto(5000, 5, 10));
    }

    @Test
    @DisplayName("Расписание на следующей неделе")
    void nextWeekScheduleDay() {
        couriersSchedule.put(DELIVERY_SERVICE_ID, PartnerCourierDayScheduleResponse.builder()
            .locationId(LOCATION_TO)
            .partnerId(DELIVERY_SERVICE_ID)
            .schedule(List.of(
                scheduleDay(2),
                scheduleDay(5)
            ))
            .build()
        );

        DeliverySchedule delivery = calculateFirst(4);
        softly.assertThat(delivery.getDeliveryInterval().getDeliveryMin()).isEqualTo(LocalDate.of(2019, 8, 6));
        softly.assertThat(delivery.getDeliveryInterval().getDeliveryMax()).isEqualTo(LocalDate.of(2019, 8, 13));
        softly.assertThat(delivery.getCourierSchedule().getSchedule())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(scheduleDto(2000, 2, 4));
    }

    @Test
    @DisplayName("Одинаковые расписания курьеров")
    void sameCourierSchedule() {
        couriersSchedule.put(DELIVERY_SERVICE_ID, PartnerCourierDayScheduleResponse.builder()
            .locationId(LOCATION_TO)
            .partnerId(DELIVERY_SERVICE_ID)
            .schedule(List.of(
                scheduleDay(2, 3, 10),
                scheduleDay(3, 11, 15),
                scheduleDay(3, 3, 10),
                scheduleDay(2, 11, 15)
            ))
            .build()
        );

        DeliverySchedule delivery = calculateFirst(5);

        softly.assertThat(delivery.getDeliveryInterval().getDeliveryMin()).isEqualTo(LocalDate.of(2019, 8, 6));
        softly.assertThat(delivery.getDeliveryInterval().getDeliveryMax()).isEqualTo(LocalDate.of(2019, 8, 13));
        softly.assertThat(delivery.getCourierSchedule().getSchedule())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(
                scheduleDto(2000, 3, 10),
                scheduleDto(2000, 11, 15)
            );
    }

    @Test
    @DisplayName("Разные расписания в ПВЗ")
    void pickupPointsSchedule() {
        pickupPointSchedule.put(100L, Set.of(scheduleDay(1)));
        pickupPointSchedule.put(101L, Set.of(scheduleDay(3)));
        pickupPointSchedule.put(102L, Set.of(scheduleDay(5)));
        pickupPointSchedule.put(103L, Set.of(scheduleDay(5)));

        DeliveryOption option = DeliveryOption.builder()
            .tariffType(TariffType.PICKUP)
            .maxDays(4)
            .pickupPoints(List.of(100L, 101L, 102L, 103L))
            .build();

        List<DeliverySchedule> results = createCalculator().fillDeliveryDate(option, SHIPMENT_DATE)
            .sorted(Comparator.comparing(DeliverySchedule::getDeliveryInterval))
            .collect(Collectors.toList());

        assertThat(results).hasSize(3);

        assertPickupPointDelivery(results.get(0), LocalDate.of(2019, 8, 7), LocalDate.of(2019, 8, 14), 101L);
        assertPickupPointDelivery(results.get(1), LocalDate.of(2019, 8, 9), LocalDate.of(2019, 8, 16), 102L, 103L);
        assertPickupPointDelivery(results.get(2), LocalDate.of(2019, 8, 12), LocalDate.of(2019, 8, 12), 100L);
    }

    @Test
    @DisplayName("Учет выходных дней курьеров")
    void courierHolidays() {
        long calendarId = 42L;

        couriersSchedule.put(DELIVERY_SERVICE_ID, PartnerCourierDayScheduleResponse.builder()
            .locationId(LOCATION_TO)
            .partnerId(DELIVERY_SERVICE_ID)
            .schedule(List.of(scheduleDay(2, 3, 10), scheduleDay(6, 3, 10), scheduleDay(7, 3, 10)))
            .calendarId(calendarId)
            .build()
        );

        courierHolidays.put(calendarId, Set.of(
            LocalDate.of(2019, 8, 9),
            LocalDate.of(2019, 8, 11)
        ));

        DeliverySchedule deliverySchedule = createCalculator()
            .fillDeliveryDate(optionBuilder.minDays(2).maxDays(5).build(), SHIPMENT_DATE)
            .findFirst()
            .orElseThrow(AssertionError::new);

        softly.assertThat(deliverySchedule.getDeliveryInterval().getDeliveryMin()).isEqualTo(LocalDate.of(2019, 8, 10));
        softly.assertThat(deliverySchedule.getDeliveryInterval().getDeliveryMax()).isEqualTo(LocalDate.of(2019, 8, 13));
    }

    private void assertPickupPointDelivery(DeliverySchedule result, LocalDate min, LocalDate max, Long... ids) {
        softly.assertThat(result.getDeliveryInterval().getDeliveryMin()).isEqualTo(min);
        softly.assertThat(result.getDeliveryInterval().getDeliveryMax()).isEqualTo(max);
        softly.assertThat(result.getPickupPointIds()).containsExactly(ids);
    }

    @Nonnull
    private DeliverySchedule calculateFirst(int daysMax) {
        return calculateFirstOptional(daysMax)
            .orElseThrow(AssertionError::new);
    }

    @Nonnull
    private Optional<DeliverySchedule> calculateFirstOptional(int daysMax) {
        return createCalculator().fillDeliveryDate(optionBuilder.maxDays(daysMax).build(), SHIPMENT_DATE)
            .findFirst();
    }

    @Nonnull
    private DeliveryDateCalculator createCalculator() {
        return new DeliveryDateCalculator(
            couriersSchedule,
            courierHolidays,
            days -> new CourierScheduleConverter().toApi(LOCATION_TO, days),
            PickupPointsData.builder()
                .pickupPointScheduleById(pickupPointSchedule)
                .availablePickupPointsIds(pickupPointSchedule.keySet())
                .build()
        );
    }

    @Nonnull
    private ScheduleDayResponse scheduleDay(int day) {
        return scheduleDay(day, day, day * 2);
    }

    @Nonnull
    private ScheduleDayResponse scheduleDay(int day, int hourFrom, int hourTo) {
        return new ScheduleDayResponse(day * 1000L, day, LocalTime.of(hourFrom, 0), LocalTime.of(hourTo, 0));
    }

    @Nonnull
    private DeliveryOptionTimeInterval scheduleDto(long id, int hourFrom, int hourTo) {
        return new DeliveryOptionTimeInterval(
            id,
            LocalTime.of(hourFrom, 0),
            LocalTime.of(hourTo, 0)
        );
    }
}
