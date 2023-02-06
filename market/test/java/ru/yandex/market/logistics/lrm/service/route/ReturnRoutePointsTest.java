package ru.yandex.market.logistics.lrm.service.route;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.service.route.model.Route;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

class ReturnRoutePointsTest extends LrmTest {

    private static final List<Route.Point> POINTS = List.of(
        point(1, LogisticSegmentType.WAREHOUSE, PartnerType.SORTING_CENTER),
        point(2, LogisticSegmentType.BACKWARD_MOVEMENT, PartnerType.DELIVERY),
        point(3, LogisticSegmentType.WAREHOUSE, PartnerType.SORTING_CENTER)
    );

    @Test
    @DisplayName("Пустой маршрут")
    void emptyRoute() {
        softly.assertThatThrownBy(() -> ReturnRoutePoints.of(List.of()))
            .hasMessage("Route must have at least 3 points");
    }

    @Test
    @DisplayName("Инициализируется на первой точке маршрута")
    void initializedOnFirstPoint() {
        ReturnRoutePoints returnRoutePoints = ReturnRoutePoints.of(POINTS);

        softly.assertThat(returnRoutePoints.get().getSegmentId())
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("Инициализируется на первом складе маршрута")
    void mustBeInitializedOnWarehouse() {
        List<Route.Point> pointsStartingFromMovement = List.of(
            point(1, LogisticSegmentType.BACKWARD_MOVEMENT, PartnerType.DELIVERY),
            point(2, LogisticSegmentType.WAREHOUSE, PartnerType.SORTING_CENTER),
            point(3, LogisticSegmentType.BACKWARD_MOVEMENT, PartnerType.DELIVERY),
            point(4, LogisticSegmentType.WAREHOUSE, PartnerType.SORTING_CENTER)
        );

        softly.assertThat(ReturnRoutePoints.of(pointsStartingFromMovement).get().getSegmentId())
            .isEqualTo(2);
    }

    @Test
    @DisplayName("Проверка наличия предыдущего склада: первый склад")
    void hasPrevWarehouseForFirstPoint() {
        ReturnRoutePoints returnRoutePoints = ReturnRoutePoints.of(POINTS);

        softly.assertThat(returnRoutePoints.hasPrevWarehouse())
            .isEqualTo(false);
    }

    @Test
    @DisplayName("Проверка наличия предыдущего склада: не первый склад")
    void hasPrevWarehouseForNotFirstPoint() {
        ReturnRoutePoints returnRoutePoints = ReturnRoutePoints.of(POINTS).nextWarehouse();

        softly.assertThat(returnRoutePoints.hasPrevWarehouse())
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Получение предыдущего склада: первый склад")
    void prevWarehouseForFirstPoint() {
        ReturnRoutePoints returnRoutePoints = ReturnRoutePoints.of(POINTS);

        softly.assertThatThrownBy(returnRoutePoints::prevWarehouse)
            .hasMessage("Cannot get previous warehouse of the first warehouse");
    }

    @Test
    @DisplayName("Получение предыдущего склада: не первый склад")
    void prevWarehouseForNotFirstPoint() {
        ReturnRoutePoints returnRoutePoints = ReturnRoutePoints.of(POINTS).nextWarehouse();

        softly.assertThat(returnRoutePoints.prevWarehouse().get().getSegmentId())
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("Проверка наличия следующего склада: последний склад")
    void hasNextWarehouseForLastPoint() {
        ReturnRoutePoints returnRoutePoints = ReturnRoutePoints.of(POINTS).lastWarehouse();

        softly.assertThat(returnRoutePoints.hasNextWarehouse())
            .isEqualTo(false);
    }

    @Test
    @DisplayName("Проверка наличия следующего склада: не последний склад")
    void hasNextWarehouseForNotLastPoint() {
        ReturnRoutePoints returnRoutePoints = ReturnRoutePoints.of(POINTS);

        softly.assertThat(returnRoutePoints.hasNextWarehouse())
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Получение следующего склада: последний склад")
    void nextWarehouseForLastPoint() {
        ReturnRoutePoints returnRoutePoints = ReturnRoutePoints.of(POINTS).lastWarehouse();

        softly.assertThatThrownBy(returnRoutePoints::nextWarehouse)
            .hasMessage("Cannot get next warehouse of the last warehouse");
    }

    @Test
    @DisplayName("Получение следующего склада: не последний склад")
    void nextWarehouseForNotLastPoint() {
        ReturnRoutePoints returnRoutePoints = ReturnRoutePoints.of(POINTS);

        softly.assertThat(returnRoutePoints.nextWarehouse().get().getSegmentId())
            .isEqualTo(3L);
    }

    @Test
    @DisplayName("Получение последнего склада")
    void lastWarehouse() {
        ReturnRoutePoints returnRoutePoints = ReturnRoutePoints.of(POINTS);

        softly.assertThat(returnRoutePoints.lastWarehouse().get().getSegmentId())
            .isEqualTo(3L);
    }

    @Test
    @DisplayName("Маршрут иммутабельный")
    void pointsAreUnmodifiable() {
        List<Route.Point> modifiablePoints = new java.util.ArrayList<>(POINTS);
        ReturnRoutePoints returnRoutePoints = ReturnRoutePoints.of(modifiablePoints);
        modifiablePoints.set(0, point(10, LogisticSegmentType.WAREHOUSE, PartnerType.SORTING_CENTER));

        softly.assertThat(returnRoutePoints.get().getSegmentId())
            .isEqualTo(1L);
    }

    private static Route.Point point(int segmentId, LogisticSegmentType segmentType, PartnerType partnerType) {
        return Route.Point.builder()
            .segmentId(segmentId)
            .segmentType(segmentType)
            .partnerType(partnerType)
            .build();
    }
}
