package ru.yandex.market.delivery.transport_manager.service.interwarehouse.algorithm;

import java.util.Collection;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportMetadata;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BaselineCarSelectionServiceTest {

    private TransportationMapper mapper;
    private BaselineCarSelectionService service;

    @BeforeEach
    public void init() {
        mapper = Mockito.mock(TransportationMapper.class);
        service = new BaselineCarSelectionService(mapper);
    }

    @Test
    void testEmptyCarCollection() {
        var cars = service.solve(List.of(transportation(1L, 1L, 2L, 3)), List.of());
        Assertions.assertThat(cars.getAll()).isEmpty();
        Mockito.verifyZeroInteractions(mapper);
    }

    @Test
    void testNoRelevantCar() {
        var cars = service.solve(
            List.of(transportation(1L, 1L, 2L, 3)),
            List.of(car(1L, 1L, 3L, 5))
        );
        Assertions.assertThat(cars.getAll()).isEmpty();
    }

    @Test
    void testAllInOneMinCar() {
        var cars = service.solve(
            List.of(
                transportation(1L, 1L, 2L, 2),
                transportation(2L, 1L, 2L, 1),
                transportation(3L, 1L, 2L, 3)
            ),
            List.of(
                car(1L, 1L, 2L, 5),
                car(2L, 1L, 2L, 6),
                car(3L, 1L, 2L, 7)
            )
        );
        Assertions.assertThat(cars.size()).isEqualTo(1);
        TransportMetadata car = cars.getAll().iterator().next().getTransportMetadata();
        Assertions.assertThat(car).extracting(TransportMetadata::getPalletCount).isEqualTo(6);

        Assertions.assertThat(cars.get(0).getTransportations())
            .extracting(Transportation::getId)
            .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void testDifferentCarsOneRoute() {
        var cars = service.solve(
            List.of(
                transportation(1L, 1L, 2L, 1),
                transportation(2L, 1L, 2L, 2),
                transportation(3L, 1L, 2L, 3),
                transportation(4L, 1L, 2L, 4),
                transportation(5L, 1L, 2L, 5)
            ),
            List.of(
                car(1L, 1L, 2L, 8, 990),
                car(2L, 1L, 2L, 10, 1000)
            )
        );

        Assertions.assertThat(cars.getAll())
            .extracting(TransportationsInCar::getTransportMetadata)
            .extracting(TransportMetadata::getExternalId)
            .containsExactly(2L, 1L);

        Assertions.assertThat(cars.get(0).getTransportations())
            .extracting(Transportation::getId)
            .containsExactlyInAnyOrder(1L, 4L, 5L);
        Assertions.assertThat(cars.get(1).getTransportations())
            .extracting(Transportation::getId)
            .containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    void testDifferentRoutes() {
        var car1to2 = car(1L, 1L, 2L, 1);
        var car3to4 = car(2L, 3L, 4L, 6);
        var cars = service.solve(
            List.of(
                transportation(1L, 1L, 2L, 1),
                transportation(2L, 1L, 2L, 2),
                transportation(3L, 3L, 4L, 3),
                transportation(4L, 3L, 4L, 3),
                transportation(5L, 1L, 4L, 1)
            ),
            List.of(car1to2, car3to4)
        );

        Assertions.assertThat(cars.size()).isEqualTo(2);
        Assertions.assertThat(
            cars.getAll().stream()
                .map(TransportationsInCar::getTransportations)
                .mapToLong(Collection::size)
                .sum()
        ).isEqualTo(3);

        Assertions.assertThat(cars.get(0).getTransportations())
            .extracting(Transportation::getId)
            .containsExactlyInAnyOrder(1L);
        Assertions.assertThat(cars.get(1).getTransportations())
            .extracting(Transportation::getId)
            .containsExactlyInAnyOrder(3L, 4L);
    }

    @Test
    void testSelectCheapestOneCar() {
        var cars = service.solve(
            List.of(
                transportation(1, 100, 101, 2),
                transportation(2, 100, 101, 4)
            ),
            List.of(
                car(1L, 100, 101, 5, 1),
                car(2L, 100, 101, 7, 200),
                car(3L, 100, 101, 33, 205)
            )
        );
        Assertions.assertThat(cars.size()).isEqualTo(1);
        TransportMetadata car = cars.get(0).getTransportMetadata();
        Assertions.assertThat(car.getExternalId()).isEqualTo(2L);
        Assertions.assertThat(cars.get(0).getTransportations())
            .extracting(Transportation::getId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void testSelectOneCarSeveralTimes() {
        var cars = service.solve(
            List.of(
                transportation(1, 100, 101, 2),
                transportation(2, 100, 101, 4),
                transportation(3, 100, 101, 5)
            ),
            List.of(
                car(1L, 100, 101, 6, 1)
            )
        );
        Assertions.assertThat(cars.size()).isEqualTo(2);
        Assertions.assertThat(cars.get(0).getTransportations())
            .extracting(Transportation::getId).containsExactlyInAnyOrder(3L);
        Assertions.assertThat(cars.get(1).getTransportations())
            .extracting(Transportation::getId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void testManyTransportationsOneCar() {
        var cars = service.solve(
            List.of(
                transportation(1, 100, 101, 2),
                transportation(2, 100, 101, 2),
                transportation(3, 100, 101, 2),
                transportation(4, 100, 101, 2),
                transportation(5, 100, 101, 2),
                transportation(6, 100, 101, 3),
                transportation(7, 100, 101, 3),
                transportation(8, 100, 101, 4),
                transportation(9, 100, 101, 4),
                transportation(10, 100, 101, 5),
                transportation(11, 100, 101, 5),
                transportation(12, 100, 101, 6),
                transportation(13, 100, 101, 6)
            ),
            List.of(
                car(1L, 100, 101, 33, 1000)
            )
        );
        Assertions.assertThat(cars.size()).isEqualTo(2);
        Assertions.assertThat(
            cars.getAll().stream().map(TransportationsInCar::getTransportations).mapToInt(List::size).sum()
        ).isEqualTo(13);
    }

    @Test
    void testSelectProfitableCarWithEnoughVolume() {
        var cars = service.solve(
            List.of(
                transportation(1, 100, 101, 2),
                transportation(2, 100, 101, 2)
            ),
            List.of(
                car(1L, 100, 101, 1, 100),
                car(2L, 100, 101, 3, 1000)
            )
        );
        Assertions.assertThat(cars.size()).isEqualTo(2);
        Assertions.assertThat(cars.getAll())
            .extracting(TransportationsInCar::getTransportMetadata)
            .extracting(TransportMetadata::getExternalId)
            .containsOnly(2L);
    }

    private Transportation transportation(long id, long from, long to, Integer palletCount) {
        Transportation transportation = new Transportation()
            .setId(id)
            .setOutboundUnit(
                new TransportationUnit()
                    .setLogisticPointId(from)
            )
            .setMovement(new Movement())
            .setInboundUnit(
                new TransportationUnit()
                    .setLogisticPointId(to)
            );
        when(mapper.getOutboundRegisterPalletCount(id)).thenReturn(palletCount);
        return transportation;
    }

    private TransportMetadata car(long id, long from, long to, int maxPallet) {
        return car(id, from, to, maxPallet, maxPallet);
    }

    private TransportMetadata car(long id, long from, long to, int maxPallet, long price) {
        return new TransportMetadata()
            .setExternalId(id)
            .setLogisticPointFromId(from)
            .setLogisticPointToId(to)
            .setPalletCount(maxPallet)
            .setPrice(price);
    }
}
