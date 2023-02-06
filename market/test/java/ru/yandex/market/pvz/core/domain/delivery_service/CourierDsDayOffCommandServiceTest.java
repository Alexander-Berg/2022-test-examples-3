package ru.yandex.market.pvz.core.domain.delivery_service;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CourierDsDayOffCommandServiceTest {

    public static final long COURIER_DELIVERY_SERVICE_ID = 1L;
    public static final LocalDate DAY_OFF = LocalDate.of(2021, 2, 8);

    private final CourierDsDayOffRepository dayOffRepository;

    private final CourierDsDayOffCommandService courierDsDayOffCommandService;

    @Test
    void createNewDayOff() {
        CourierDsDayOff actual = courierDsDayOffCommandService.create(COURIER_DELIVERY_SERVICE_ID, DAY_OFF);

        assertThat(actual.getCourierDeliveryServiceId()).isEqualTo(COURIER_DELIVERY_SERVICE_ID);
        assertThat(actual.getDayOff()).isEqualTo(DAY_OFF);

        assertThat(dayOffRepository.findAll()).hasSize(1);
    }

    @Test
    void createNewDayOffForAnotherDate() {
        courierDsDayOffCommandService.create(COURIER_DELIVERY_SERVICE_ID, DAY_OFF);
        courierDsDayOffCommandService.create(COURIER_DELIVERY_SERVICE_ID, DAY_OFF.plusDays(1));

        assertThat(dayOffRepository.findAll()).hasSize(2);
    }

    @Test
    void createNewDayOffForAnotherDs() {
        courierDsDayOffCommandService.create(COURIER_DELIVERY_SERVICE_ID, DAY_OFF);
        courierDsDayOffCommandService.create(COURIER_DELIVERY_SERVICE_ID + 1, DAY_OFF);

        assertThat(dayOffRepository.findAll()).hasSize(2);
    }

    @Test
    void doNotCreateTheSameDayOff() {
        courierDsDayOffCommandService.create(COURIER_DELIVERY_SERVICE_ID, DAY_OFF);
        CourierDsDayOff actual = courierDsDayOffCommandService.create(COURIER_DELIVERY_SERVICE_ID, DAY_OFF);

        assertThat(actual.getCourierDeliveryServiceId()).isEqualTo(COURIER_DELIVERY_SERVICE_ID);
        assertThat(actual.getDayOff()).isEqualTo(DAY_OFF);

        assertThat(dayOffRepository.findAll()).hasSize(1);
    }

    @Test
    void deleteDayOff() {
        courierDsDayOffCommandService.create(COURIER_DELIVERY_SERVICE_ID, DAY_OFF);

        List<CourierDsDayOff> actual = courierDsDayOffCommandService.delete(COURIER_DELIVERY_SERVICE_ID, DAY_OFF);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getCourierDeliveryServiceId()).isEqualTo(COURIER_DELIVERY_SERVICE_ID);
        assertThat(actual.get(0).getDayOff()).isEqualTo(DAY_OFF);
    }

    @Test
    void noDayOffToDelete() {
        courierDsDayOffCommandService.create(COURIER_DELIVERY_SERVICE_ID, DAY_OFF);

        List<CourierDsDayOff> actual = courierDsDayOffCommandService.delete(COURIER_DELIVERY_SERVICE_ID + 1, DAY_OFF);

        assertThat(actual).isEmpty();
    }
}
