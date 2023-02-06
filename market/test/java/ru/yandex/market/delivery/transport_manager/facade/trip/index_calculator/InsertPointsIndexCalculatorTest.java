package ru.yandex.market.delivery.transport_manager.facade.trip.index_calculator;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.dto.trip.IndexPair;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip.TripPointFlatInfo;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip_included_outbounds.DirectionAndType;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitCargoType;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;

class InsertPointsIndexCalculatorTest {
    private final InsertPointsIndexCalculator calculator = new InsertPointsIndexCalculator();

    @DisplayName("Добавляется лог точка, в которой не было никаких операций")
    @Test
    void missingPoint() {
        Assertions.assertThatThrownBy(() -> calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 5, 1L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 1, 2L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 4, 2L, 3L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(5L, 2, 3L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(6L, 3, 3L, 2L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 100L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Can't get length of segment 1 -> 100");
    }

    /**
     * P1      P2      P3      P4
     * 0 ---------------------> 1
     * <p>
     * Insert P1 --- > P4
     * <p>
     * P1      P2      P3      P4
     * 0 ---------------------> 3
     * 1 -new transportation.-> 2
     */
    @DisplayName("Уже есть перемещения в этом направлении: простой вариант")
    @Test
    void calculateExistingDirectionSimple() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 1, 1L, 4L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 4L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(1, 2));
    }


    /**
     * P1      P2      P3      P4
     * 0 ---------------------> 3
     * 1 ---------------------> 2
     * <p>
     * Insert P1 --- > P4
     * <p>
     * P1      P2      P3      P4
     * 0 ---------------------> 5
     * 1 ---------------------> 4
     * 2 -new transportation.-> 3
     */
    @DisplayName("Уже есть 2 перемещения в этом направлении")
    @Test
    void calculateExistingDirectionTwoTransportations() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 1, 2L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(3L, 2, 2L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(4L, 3, 1L, 4L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 4L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(2, 3));
    }


    /**
     * P1      P2      P3      P4
     * 0 ---------------------> 5
     * 1 ------------> 4
     * 2 ----> 3
     * <p>
     * Insert P1 --- > P3
     * <p>
     * P1      P2      P3      P4
     * 0 ---------------------> 7
     * 1 ------------> 6
     * 2 -new transp-> 5
     * 3 ----> 4
     */
    @DisplayName("Уже есть отгрузки в данной точке на разные направления включая текущее")
    @Test
    void calculateExistingDirectionDifferentLength() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 1, 2L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 2, 3L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(3L, 3, 3L, 2L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 4, 2L, 3L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(4L, 5, 1L, 4L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 3L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(2, 5));
    }

    /**
     * P1      P2      P3      P4
     * 0 ---------------------> 100
     * <p>
     * Insert P1 --- > P4
     * <p>
     * P1      P2      P3      P4
     * 0 ---------------------> 102
     * 1 -new transportation.-> 101
     *
     * Тут для обобщения алгоритма будем раздвигать индексы несмотря на то, что
     * с 0 до 100 всё и так свободно.
     */
    @DisplayName("Уже есть перемещения в этом направлении: индексы не подряд")
    @Test
    void calculateExistingDirectionIndexesBigDiff() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 100, 1L, 4L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 4L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(1, 101));
    }

    /**
     * P1      P2      P3      P4
     * 0 ---------------------> 3
     *         1 -------------> 2
     * <p>
     * Insert P1 --- > P2
     * <p>
     * P1      P2      P3      P4
     * 0 ---------------------> 5
     * 1 new-> 2
     *         3 -------------> 4
     */
    @DisplayName("Нет перемещений по этому направлению: типичный случай 1")
    @Test
    void calculateNewDirection1() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 3, 1L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 1, 2L, 2L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 2, 2L, 4L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 2L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(1, 2));
    }

    /**
     * P1      P2      P3      P4
     * 0 ---------------------> 5
     *         1 -------------> 4
     *         2 -------------> 3
     * <p>
     * Insert P1 --- > P2
     * <p>
     * P1      P2      P3      P4
     * 0 ---------------------> 7
     * 1 new-> 2
     *         3 -------------> 6
     *         4 -------------> 5
     */
    @DisplayName("Нет перемещений по этому направлению, две отгрузки с точки 2")
    @Test
    void calculateNewDirection1TwoOutbounds() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 1, 2L, 2L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(3L, 2, 3L, 2L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 3, 3L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(5L, 4, 2L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(6L, 5, 1L, 4L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 2L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(1, 2));
    }

    /**
     * P1      P2      P3      P4
     * 0 ---------------------> 3
     * 1 -------------> 2
     * <p>
     * Insert P3 --- > P4
     * <p>
     * P1      P2      P3      P4
     * 0 ---------------------> 5
     * 1 -------------> 2
     *                  3 new-> 4
     */
    @DisplayName("Нет перемещений по этому направлению: типичный случай 2")
    @Test
    void calculateNewDirection2() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 3, 1L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 1, 2L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 2, 2L, 3L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(3L, 4L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(3, 4));
    }

    /**
     * P1      P2      P3      P4
     * 0 ---------------------> 5
     * 1 -------------> 4
     * 2 -------------> 3
     * <p>
     * Insert P3 --- > P4
     * <p>
     * P1      P2      P3      P4
     * 0 ---------------------> 7
     * 1 -------------> 4
     * 2 -------------> 3
     *                  5 new-> 6
     */
    @DisplayName("Нет перемещений по этому направлению: две приёмки на точку 2")
    @Test
    void calculateNewDirection2TwoOutbounds() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 5, 1L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 1, 2L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 4, 2L, 3L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 2, 3L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 3, 3L, 3L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(3L, 4L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(5, 6));
    }


    /**
     * P1      P2      P3      P4
     * 0 ------------> 1
     *                 2 ----> 3
     * <p>
     * Insert P1 --- > P4
     * <p>
     * P1      P2      P3      P4
     * 0 -new transportation--> 5
     * 1 ------------> 2
     *                 3 -----> 4
     */
    @DisplayName("Нет перемещений по этому направлению: типичный случай 3")
    @Test
    void calculateNewDirection3() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 1, 1L, 3L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 2, 2L, 4L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 3, 2L, 4L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 4L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(0, 5));
    }


    /**
     * P1      P2      P3      P4
     * 0 ---------------------> 5
     * 1 ------------> 4
     * 2 ----> 3
     * <p>
     * Insert P1 --- > P3
     * <p>
     * P1      P2      P3      P4
     * 0 ---------------------> 7
     * 1 ------------> 6
     * 2 -new transp-> 5
     * 3 ----> 4
     */
    @DisplayName("Уже есть перемещения в этом направлении")
    @Test
    void calculateExistingDirection() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 5, 1L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 1, 2L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 4, 2L, 3L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(5L, 2, 3L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(6L, 3, 3L, 2L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 3L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(2, 5));
    }

    /**
     *      Insert last
     *
     *      P1      P2      P3      P4
     *      0 ------------> 4
     *      1 ----> 2
     *              3 ------------> 5
     *
     *      Insert P1 --- > P4
     *

     *      P1      P2      P3      P4
     *      0 -new transportation-> 7
     *      1 ------------> 5
     *      2 ----> 3
     *              4 ------------> 6
     *
     */
    @DisplayName("Нет перемещений по этому направлению, но есть более короткие отгрузки с первой точки")
    @Test
    void calculateInsertFirst() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 4, 1L, 3L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 1, 2L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 2, 2L, 2L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(5L, 3, 3L, 2L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(6L, 5, 3L, 4L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 4L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(0, 7));
    }

    /**
     *      Insert last
     *
     *      P1      P2      P3      P4
     *      0 ---------------------> 3
     *             1 -----> 2
     *
     *      Insert P1 --- > P3
     *
     *
     *      P1      P2      P3      P4
     *      0 ---------------------> 5
     *      1 -new transp.-> 4
     *             2 ------> 3
     *
     */
    @DisplayName("Нет перемещений по этому направлению, но есть более длинные отгрузки с первой точки")
    @Test
    void calculateInsertLast() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 3, 1L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 1, 2L, 2L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 2, 2L, 3L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 3L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(1, 4));
    }

    /**
     *      Insert last
     *
     *      P1      P2      P3      P4
     *      0 ------------> 2
     *              1 ------------> 5
     *                      3 ----> 4
     *
     *      Insert P1 --- > P4
     *

     *      P1      P2      P3      P4
     *      0 -new transportation-> 7
     *      1 ------------> 3
     *              2 ------------> 6
     *                      4 ----> 5
     *
     */
    @DisplayName("Нет перемещений по этому направлению, но есть более короткие приёмки на последней точке")
    @Test
    void calculateInsertFirstInb() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 2, 1L, 3L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 1, 2L, 2L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 5, 2L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(5L, 3, 3L, 3L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(6L, 4, 3L, 4L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 4L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(0, 7));
    }

    /**
     *      Insert last
     *
     *      P1      P2      P3      P4
     *      0 ---------------------> 5
     *             1 --------------> 4
     *                       2 ----> 3
     *
     *      Insert P1 --- > P2
     *
     *
     *      P1      P2      P3      P4
     *      0 ---------------------> 7
     *      1 -new-> 2
     *               3 ------------> 6
     *                       4 ----> 5
     *
     */
    @DisplayName("Нет других приёмок в данной точке")
    @Test
    void calculateInsertFirstInboundToPoint() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 5, 1L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 1, 2L, 2L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 4, 2L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(5L, 2, 3L, 3L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(6L, 3, 3L, 4L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(1L, 2L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(1, 2));
    }

    /**
     *      Insert last
     *
     *      P1      P2      P3      P4
     *      0 ---------------------> 5
     *      1 --------------> 4
     *      2 ----> 3
     *
     *      Insert P3 --- > P4
     *
     *
     *      P1      P2      P3        P4
     *      0 -----------------------> 7
     *      1 --------------> 4
     *                        5 -new-> 6
     *      2 ----> 3
     *
     */
    @DisplayName("Нет других отгрузок в данной точке")
    @Test
    void calculateInsertFirstOutboundToPoint() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 5, 1L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 1, 2L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 4, 2L, 3L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(5L, 2, 3L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(6L, 3, 3L, 2L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(3L, 4L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(5, 6));
    }

    /**
     *      Insert last
     *
     *      P1      P2      P3      P4
     *      0 ---------------------> 5
     *      1 --------------> 4
     *      2 ----> 3
     *
     *      Insert P2 --- > P4
     *
     *
     *      P1      P2      P3        P4
     *      0 ----------------------> 7
     *      1 --------------> 5
     *      2 ----> 3
     *              4 -new transp---> 6
     *
     */
    @DisplayName("Потребуется выгрузить и положить назад")
    @Test
    void calculateArrowsCross() {
        IndexPair indexPair = calculator.calculate(
            1L,
            List.of(
                new TripPointFlatInfo(1L, 0, 1L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 5, 1L, 4L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(3L, 1, 2L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(4L, 4, 2L, 3L, TransportationUnitType.INBOUND),
                new TripPointFlatInfo(5L, 2, 3L, 1L, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(6L, 3, 3L, 2L, TransportationUnitType.INBOUND)
            ),
            new DirectionAndType(2L, 4L, DistributionCenterUnitCargoType.INTERWAREHOUSE_FIT)
        );

        Assertions.assertThat(indexPair).isEqualTo(new IndexPair(4, 6));
    }
}
