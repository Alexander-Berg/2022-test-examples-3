package ru.yandex.market.sc.core.domain.order;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServicePropertySource;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
public class SortInAdvanceTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    OrderQueryService orderQueryService;
    @Autowired
    DeliveryServicePropertySource deliveryServicePropertySource;
    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    TestFactory.CourierWithDs courierWithDs;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        TestFactory.setupMockClock(clock);
        courierWithDs = testFactory.magistralCourier();
    }

    private DeliveryService setCourierSortInAdvanceDays(int days) {
        testFactory.setDeliveryServiceProperty(courierWithDs.deliveryService(),
                DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC, String.valueOf(sortingCenter.getId()));
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(days));
        deliveryServicePropertySource.refreshForce();
        return courierWithDs.deliveryService();
    }

    @Test
    void sortForToday() {
        testSortInAdvance(0);
    }

    @Test
    void sortForTomorrow() {
        testSortInAdvance(1);
    }

    @Test
    void sortForADayAfterTomorrow() {
        testSortInAdvance(2);
    }

    @Test
    void sortAWeekInAdvance() {
        testSortInAdvance(7);
    }

    @Test
    void sortForTomorrowDifferentCells() {
        testSortInAdvanceDifferentCells(1);
    }

    @Test
    void sortForADayAfterTomorrowDifferentCells() {
        testSortInAdvanceDifferentCells(2);
    }

    @Test
    void sortAWeekInAdvanceDifferentCells() {
        testSortInAdvanceDifferentCells(7);
    }

    private void testSortInAdvance(int daysInAdvance) {
        if (daysInAdvance < 0) {
            throw new IllegalArgumentException("daysInAdvance < 0: " + daysInAdvance);
        }
        var deliveryService = setCourierSortInAdvanceDays(daysInAdvance);
        var order = testFactory.create(order(sortingCenter)
                .dsType(DeliveryServiceType.TRANSIT)
                .deliveryService(deliveryService)
                .build())
                .updateShipmentDate(LocalDate.now(clock).plusDays(daysInAdvance))
                .accept()
                .get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null).getStatus())
                .isEqualTo(ApiOrderStatus.SORT_TO_COURIER);
        var actualOrder = testFactory.sortOrder(order);
        assertThat(actualOrder.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        if (daysInAdvance == 0) {
            assertThatCode(() -> testFactory.shipOrderRoute(actualOrder)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> testFactory.shipOrderRoute(actualOrder)).isInstanceOf(ScException.class);
        }
    }

    private void testSortInAdvanceDifferentCells(int daysInAdvance) {
        if (daysInAdvance <= 0) {
            throw new IllegalArgumentException("daysInAdvance < 0: " + daysInAdvance);
        }
        var deliveryService = setCourierSortInAdvanceDays(daysInAdvance);
        List<Cell> existingCells = new ArrayList<>();
        for (int i = 0; i < daysInAdvance; i++) {
            existingCells.add(
                    testFactory.storedMagistralCell(sortingCenter, "c-" + i,
                            CellSubType.IN_ADVANCE_COURIER, courierWithDs.courier().getId()));
        }
        List<Place> places = new ArrayList<>();
        for (int i = 0; i < daysInAdvance + 1; i++) {
            var place = testFactory.create(
                    order(sortingCenter).externalId("o-" + i)
                            .dsType(DeliveryServiceType.TRANSIT)
                            .deliveryService(deliveryService)
                            .build())
                    .updateShipmentDate(LocalDate.now(clock).plusDays(i))
                    .accept()
                    .getPlace();

            assertThat(orderQueryService.getOrderForApi(sortingCenter, place.getExternalId(), null).getStatus())
                    .isEqualTo(ApiOrderStatus.SORT_TO_COURIER);
            testFactory.sortPlace(place);

            place = testFactory.updated(place);
            assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
            places.add(place);
        }
        var order = testFactory.create(
                order(sortingCenter).externalId("o-" + (daysInAdvance + 1))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(deliveryService)
                        .build()
        )
                .updateShipmentDate(LocalDate.now(clock).plusDays(daysInAdvance + 1))
                .accept()
                .get();
        assertThat(orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null).getStatus())
                .isEqualTo(ApiOrderStatus.KEEP);

        for (int i = 0; i < daysInAdvance; i++) {
            assertThat(places.get(i).getCell()).isEqualTo(existingCells.get(i));
        }

        for (int i = 0; i < daysInAdvance; i++) {
            var place = places.get(i);
            if (i == 0) {
                assertThatCode(() -> testFactory.shipOrderRoute(place)).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> testFactory.shipOrderRoute(place)).isInstanceOf(ScException.class);
            }
        }
    }

}
