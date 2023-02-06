package ru.yandex.market.sc.core.domain.print.generator;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.measurements.repository.Measurements;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withPrecision;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LotLabelServiceTest {

    private final Clock clock;
    private final TestFactory testFactory;
    private final LotLabelService lotLabelService;

    @Test
    @Transactional
    void buildLotLabelDto() {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L);

        var courier = testFactory.courier();

        var order1 = testFactory.createForToday(order(sortingCenter, "o1")
                .deliveryDate(LocalDate.now(clock))
                .shipmentDate(LocalDate.now(clock))
                .build()).updateCourier(courier).accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "o2")
                .deliveryDate(LocalDate.now(clock))
                .shipmentDate(LocalDate.now(clock))
                .build()).updateCourier(courier).accept().sort().get();
        var order3 = testFactory.create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .build()).updateCourier(courier)
                .acceptPlaces(List.of("p1", "p2", "p3"))
                .sortPlaces(List.of("p1", "p2", "p3"))
                .get();

        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, testFactory.orderPlace(order1).getCell());

        testFactory.sortPlaceToLot(testFactory.orderPlace(order1), lot, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(order2), lot, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(order3, "p1"), lot, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(order3, "p2"), lot, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(order3, "p3"), lot, user);

        testFactory.prepareToShipLot(lot);

        var zplLotLabelDto = lotLabelService.buildLotLabelDto(lot.getBarcode(), user);
        var weight = calculateOrdersWeight(order1, order2) +
                calculatePlacesWeight(
                        testFactory.orderPlace(order3, "p1"),
                        testFactory.orderPlace(order3, "p2"),
                        testFactory.orderPlace(order3, "p3")
                );
        var volume = calculateOrdersVolume(order1, order2) +
                calculatePlacesVolume(
                        testFactory.orderPlace(order3, "p1"),
                        testFactory.orderPlace(order3, "p2"),
                        testFactory.orderPlace(order3, "p3")
                );
        assertThat(zplLotLabelDto.itemsNumber()).isEqualTo(5);
        assertThat(zplLotLabelDto.externalId()).isEqualTo(lot.getBarcode());
        assertThat(zplLotLabelDto.sortingCenterName()).isEqualTo(sortingCenter.getScName());
        assertThat(zplLotLabelDto.volume()).isEqualTo(volume, withPrecision(0.0001d));
        assertThat(zplLotLabelDto.weight()).isEqualTo(weight, withPrecision(0.0001d));
    }

    @Test
    @Transactional
    void buildLotLabelDtoForTomorrow() {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L);

        var place1 = testFactory.createOrder(order(sortingCenter, "o1")
                .dsType(DeliveryServiceType.TRANSIT)
                .deliveryDate(LocalDate.now(clock).plusDays(1))
                .shipmentDate(LocalDate.now(clock).plusDays(1))
                .build()).accept().sort().getPlace();
        var place2 = testFactory.createOrder(order(sortingCenter, "o2")
                .dsType(DeliveryServiceType.TRANSIT)
                .deliveryDate(LocalDate.now(clock).plusDays(1))
                .shipmentDate(LocalDate.now(clock).plusDays(1))
                .build()).accept().sort().getPlace();

        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, place1.getCell());

        testFactory.sortPlaceToLot(place1, lot, user);
        testFactory.sortPlaceToLot(place2, lot, user);

        testFactory.prepareToShipLot(lot);

        var zplLotLabelDto = lotLabelService.buildLotLabelDto(lot.getBarcode(), user);

        assertThat(zplLotLabelDto.itemsNumber()).isEqualTo(2);
        assertThat(zplLotLabelDto.externalId()).isEqualTo(lot.getBarcode());
        assertThat(zplLotLabelDto.sortingCenterName()).isEqualTo(sortingCenter.getScName());
    }

    private double calculatePlacesWeight(Place... places) {
        return Arrays.stream(places)
                .map(Place::getMeasurements)
                .filter(Objects::nonNull)
                .map(Measurements::getWeightGross)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
    }

    private double calculateOrdersWeight(OrderLike... orders) {
        return Arrays.stream(orders)
                .map(OrderLike::getMeasurements)
                .filter(Objects::nonNull)
                .map(Measurements::getWeightGross)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
    }

    private double calculateOrdersVolume(OrderLike... orders) {
        return Arrays.stream(orders)
                .map(OrderLike::getMeasurements)
                .filter(Objects::nonNull)
                .mapToDouble(m -> m.getHeight() * m.getWidth() * m.getLength() / 100_00_00.0)
                .sum();
    }

    private double calculatePlacesVolume(Place... places) {
        return Arrays.stream(places)
                .map(Place::getMeasurements)
                .filter(Objects::nonNull)
                .mapToDouble(m -> m.getHeight() * m.getWidth() * m.getLength() / 100_00_00.0)
                .sum();
    }

}
