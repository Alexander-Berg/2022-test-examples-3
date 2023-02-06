package ru.yandex.market.wms.shippingsorter.sorting.sorterorderfsm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.core.base.dto.LocationType;
import ru.yandex.market.wms.core.base.response.MoveBalanceResponse.Location;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.domain.SorterOrderStatus;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.LocationId;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.PackStationId;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.entity.SorterOrderEntity;
import ru.yandex.market.wms.shippingsorter.sorting.entity.id.SorterExitId;
import ru.yandex.market.wms.shippingsorter.sorting.service.SorterOrderStateManager;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class SorterOrderStateManagerTest extends IntegrationTest {
    @Autowired
    private SorterOrderStateManager orderStateManager;

    @Test
    public void assignedToInProgressTest() {
        SorterOrderEntity sorterOrderEntity = getOrder("INIT-01", "INIT-01", "FINAL-01",
                SorterOrderStatus.ASSIGNED);

        Location currentLocation = new Location("SRT-A", LocationType.SHIPSORT);

        SorterOrderEntity updatedOrder = orderStateManager.updateState(sorterOrderEntity, currentLocation);

        Assertions.assertAll(
                () -> Assertions.assertEquals(SorterOrderStatus.IN_PROGRESS, updatedOrder.getStatus()),
                () -> Assertions.assertEquals(currentLocation.getLoc(), updatedOrder.getActualLocationId().getId())
        );
    }

    @Test
    public void assignedToFailedTest() {
        SorterOrderEntity sorterOrderEntity = getOrder("INIT-01", "INIT-01", "FINAL-01",
                SorterOrderStatus.ASSIGNED);

        Location currentLocation = new Location("OTHER-A", LocationType.OTHER);

        SorterOrderEntity updatedOrder = orderStateManager.updateState(sorterOrderEntity, currentLocation);

        Assertions.assertAll(
                () -> Assertions.assertEquals(SorterOrderStatus.FAILED, updatedOrder.getStatus()),
                () -> Assertions.assertEquals(currentLocation.getLoc(), updatedOrder.getActualLocationId().getId())
        );
    }

    @Test
    public void assignedWithNoChangesTest() {
        SorterOrderEntity sorterOrderEntity = getOrder("INIT-01", "INIT-01", "FINAL-01",
                SorterOrderStatus.ASSIGNED);

        Location currentLocation = new Location("INIT-01", LocationType.OTHER);

        SorterOrderEntity updatedOrder = orderStateManager.updateState(sorterOrderEntity, currentLocation);

        Assertions.assertAll(
                () -> Assertions.assertEquals(SorterOrderStatus.ASSIGNED, updatedOrder.getStatus()),
                () -> Assertions.assertEquals(currentLocation.getLoc(), updatedOrder.getActualLocationId().getId())
        );
    }

    @Test
    public void inProgressToFinishedTest() {
        SorterOrderEntity sorterOrderEntity = getOrder("SRT-A", "INIT-01", "FINAL-01",
                SorterOrderStatus.IN_PROGRESS);

        Location currentLocation = new Location("FINAL-01", LocationType.OTHER);

        SorterOrderEntity updatedOrder = orderStateManager.updateState(sorterOrderEntity, currentLocation);

        Assertions.assertAll(
                () -> Assertions.assertEquals(SorterOrderStatus.FINISHED, updatedOrder.getStatus()),
                () -> Assertions.assertEquals(currentLocation.getLoc(), updatedOrder.getActualLocationId().getId()),
                () -> Assertions.assertEquals(currentLocation.getLoc(), updatedOrder.getSorterExitId().getId())
        );
    }

    @Test
    public void inProgressToFailedWhenLocationIsNotEqualToTargetSorterExitTest() {
        SorterOrderEntity sorterOrderEntity = getOrder("SRT-A", "INIT-01", "FINAL-01",
                SorterOrderStatus.IN_PROGRESS);

        Location currentLocation = new Location("ERR-01", LocationType.OTHER);

        SorterOrderEntity updatedOrder = orderStateManager.updateState(sorterOrderEntity, currentLocation);

        Assertions.assertAll(
                () -> Assertions.assertEquals(SorterOrderStatus.FAILED, updatedOrder.getStatus()),
                () -> Assertions.assertEquals(currentLocation.getLoc(), updatedOrder.getActualLocationId().getId())
        );
    }

    @Test
    public void inProgressToFailedWhenLocationAndActualLocationAreTheSameTest() {
        SorterOrderEntity sorterOrderEntity = getOrder("SRT-A", "INIT-01", "FINAL-01",
                SorterOrderStatus.IN_PROGRESS);

        Location currentLocation = new Location("SRT-A", LocationType.SHIPSORT);

        SorterOrderEntity updatedOrder = orderStateManager.updateState(sorterOrderEntity, currentLocation);

        Assertions.assertAll(
                () -> Assertions.assertEquals(SorterOrderStatus.FAILED, updatedOrder.getStatus()),
                () -> Assertions.assertEquals(currentLocation.getLoc(), updatedOrder.getActualLocationId().getId())
        );
    }

    @Test
    public void inProgressToInProgressTest() {
        SorterOrderEntity sorterOrderEntity = getOrder("SRT-A", "INIT-01", "FINAL-01",
                SorterOrderStatus.IN_PROGRESS);

        Location currentLocation = new Location("SRT-B", LocationType.SHIPSORT);

        SorterOrderEntity updatedOrder = orderStateManager.updateState(sorterOrderEntity, currentLocation);

        Assertions.assertAll(
                () -> Assertions.assertEquals(SorterOrderStatus.IN_PROGRESS, updatedOrder.getStatus()),
                () -> Assertions.assertEquals(currentLocation.getLoc(), updatedOrder.getActualLocationId().getId())
        );
    }

    @Test
    public void assignedToCancelledTest() {
        SorterOrderEntity sorterOrderEntity = getOrder("INIT-01", "INIT-01", "FINAL-01",
                SorterOrderStatus.ASSIGNED);

        SorterOrderEntity updatedOrder = orderStateManager.cancelOrder(sorterOrderEntity);

        Assertions.assertEquals(SorterOrderStatus.CANCELED, updatedOrder.getStatus());
}

    private SorterOrderEntity getOrder(String actualLocationId, String source, String destination,
                                       SorterOrderStatus status) {
        return SorterOrderEntity.builder()
                .actualLocationId(LocationId.of(actualLocationId))
                .packStationId(PackStationId.of(source))
                .sorterExitId(SorterExitId.of(destination))
                .alternateSorterExitId(SorterExitId.of(destination))
                .errorSorterExitId(SorterExitId.of(destination))
                .assignee("sorter")
                .status(status)
                .build();
    }
}
