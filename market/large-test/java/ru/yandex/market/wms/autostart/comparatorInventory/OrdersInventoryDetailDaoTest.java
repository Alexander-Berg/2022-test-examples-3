package ru.yandex.market.wms.autostart.comparatorInventory;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.autostart.AutostartTestData1;
import ru.yandex.market.wms.autostart.autostartlogic.OrderInventoryDetailTestData;
import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;
import ru.yandex.market.wms.common.spring.dao.entity.OrderInventoryDetail;
import ru.yandex.market.wms.common.spring.dao.implementation.OrdersInventoryDetailDao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;


class OrdersInventoryDetailDaoTest extends TestcontainersConfiguration {

    @Autowired
    protected OrdersInventoryDetailDao ordersInventoryDetailDao;

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void findInventoryByOrderKeys() {
        List<OrderInventoryDetail> expected = OrderInventoryDetailTestData.sampleInventory();
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleOrderKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void findInventoryForWithdrawals() {
        List<OrderInventoryDetail> expected = List.of(
                OrderInventoryDetailTestData.invROV0000000000000000010C5300001(1),
                OrderInventoryDetailTestData.invROV0000000000000000010C5200001(1)
        );
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleWithdrawalOrdersKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/config_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/multiple_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void multipleExclusiveHoldsAreIgnored() {
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleWithdrawalOrdersKeys(), true, false);

        assertThat(actual, empty());
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/config_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void exclusiveHoldCoversNonExclusiveHolds() {
        List<OrderInventoryDetail> expected = List.of(
                OrderInventoryDetailTestData.invROV0000000000000000010C5300001(1),
                OrderInventoryDetailTestData.invROV0000000000000000010C5300002(1)
        );
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleWithdrawalOrdersKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/config_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/" +
                    "cell_has_exclusive_holds_items_in_this_cell_have_nonexclusive_hold.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void cellHasExclusiveHoldAndItemsHaveInThisCellHaveNonexclusiveHold() {
        List<OrderInventoryDetail> expected = List.of(
                OrderInventoryDetailTestData.invROV0000000000000000010C5300001(1),
                OrderInventoryDetailTestData.invROV0000000000000000010C5300002(1)
        );
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleWithdrawalOrdersKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/config_locked_uits.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void findInventoryByOrderKeysWithLockedUits() {
        List<OrderInventoryDetail> expected = OrderInventoryDetailTestData.sampleInventory();
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleOrderKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/config_locked_uits.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void findInventoryForWithdrawalsWithLockedUits() {
        List<OrderInventoryDetail> expected = List.of(
                OrderInventoryDetailTestData.invROV0000000000000000010C5300001(1),
                OrderInventoryDetailTestData.invROV0000000000000000010C5200001(1)
        );
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleWithdrawalOrdersKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/config_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void findInventoryByOrderKeysWithExclusiveHolds() {
        List<OrderInventoryDetail> expected = OrderInventoryDetailTestData.sampleInventory();
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleOrderKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/config_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void findInventoryForWithdrawalsWithExclusiveHolds() {
        List<OrderInventoryDetail> expected = List.of(
                OrderInventoryDetailTestData.invROV0000000000000000010C5300001(1),
                OrderInventoryDetailTestData.invROV0000000000000000010C5200001(1)
        );
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleWithdrawalOrdersKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/" +
                    "config_locked_uits_with_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void findInventoryByOrderKeysWithLockedUitsWithExclusiveHolds() {
        List<OrderInventoryDetail> expected = OrderInventoryDetailTestData.sampleInventory();
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleOrderKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/" +
                    "config_locked_uits_with_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void findInventoryForWithdrawalsWithLockedUitsWithExclusiveHolds() {
        List<OrderInventoryDetail> expected = List.of(
                OrderInventoryDetailTestData.invROV0000000000000000010C5300001(1),
                OrderInventoryDetailTestData.invROV0000000000000000010C5200001(1)
        );
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleWithdrawalOrdersKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/" +
                    "config_locked_uits_with_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/multiple_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void multipleExclusiveHoldsAreIgnoredWithLockedUits() {
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleWithdrawalOrdersKeys(), true, false);

        assertThat(actual, empty());
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/" +
                    "config_locked_uits_with_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void exclusiveHoldCoversNonExclusiveHoldsWithLockedUits() {
        List<OrderInventoryDetail> expected = List.of(
                OrderInventoryDetailTestData.invROV0000000000000000010C5300001(1),
                OrderInventoryDetailTestData.invROV0000000000000000010C5300002(1)
        );
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleWithdrawalOrdersKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/" +
                    "config_locked_uits_with_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/" +
                    "cell_has_exclusive_holds_items_in_this_cell_have_nonexclusive_hold.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void cellHasExclusiveHoldAndItemsInThisCellHaveNonexclusiveHoldWithLockedUits() {
        List<OrderInventoryDetail> expected = List.of(
                OrderInventoryDetailTestData.invROV0000000000000000010C5300001(1),
                OrderInventoryDetailTestData.invROV0000000000000000010C5300002(1)
        );
        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleWithdrawalOrdersKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/config_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/disabled_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void disabledExclusiveHoldIsIgnored() {
        List<OrderInventoryDetail> expected = List.of(
                OrderInventoryDetailTestData.invROV0000000000000000010C5300001(1),
                OrderInventoryDetailTestData.invROV0000000000000000010C5300002(1)
        );

        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleWithdrawalOrdersKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/" +
                    "config_locked_uits_with_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/order_flow_types.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/pick_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/disabled_exclusive_holds.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/skus.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/sku_locations.xml"),
            @DatabaseSetup(value = "/testcontainers/ordersInventoryDetailDaoTest/preset/orders_to_batch.xml"),
    })
    @Test
    void disabledExclusiveHoldIsIgnoredWithLockedUits() {
        List<OrderInventoryDetail> expected = List.of(
                OrderInventoryDetailTestData.invROV0000000000000000010C5300001(1),
                OrderInventoryDetailTestData.invROV0000000000000000010C5300002(1)
        );

        List<OrderInventoryDetail> actual = ordersInventoryDetailDao.findInventoryByOrderKeys(
                AutostartTestData1.exampleWithdrawalOrdersKeys(), true, false);

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }
}
