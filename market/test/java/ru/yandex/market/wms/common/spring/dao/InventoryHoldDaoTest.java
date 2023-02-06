package ru.yandex.market.wms.common.spring.dao;

import java.util.Arrays;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.model.enums.InventoryHoldType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.InventoryHold;
import ru.yandex.market.wms.common.spring.dao.implementation.InventoryHoldDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class InventoryHoldDaoTest extends IntegrationTest {

    @Autowired
    private InventoryHoldDao inventoryHoldDao;

    @Test
    @DatabaseSetup("/db/empty-db.xml")
    @ExpectedDatabase(value = "/db/dao/inventory-hold/after-insert.xml", assertionMode = NON_STRICT)
    public void insert() {
        InventoryHold inventoryHold1 = InventoryHold.builder()
                .inventoryHoldKey("0003413320")
                .lot("0000012345")
                .loc("STAGE1")
                .id("CART1")
                .hold(InventoryHoldType.HOLD)
                .status(InventoryHoldStatus.DAMAGE_RESELL)
                .addWho("TEST1")
                .editWho("TEST2")
                .build();

        InventoryHold inventoryHold2 = InventoryHold.builder()
                .inventoryHoldKey("0003413321")
                .lot("0000012346")
                .loc("STAGE2")
                .id("CART2")
                .hold(InventoryHoldType.OK)
                .status(InventoryHoldStatus.DAMAGE_DISPOSAL)
                .addWho("TEST1")
                .editWho("TEST2")
                .build();

        inventoryHoldDao.insert(Arrays.asList(inventoryHold1, inventoryHold2));
    }

    @Test
    @DatabaseSetup("/db/dao/inventory-hold/before-get-statuses.xml")
    @ExpectedDatabase(value = "/db/dao/inventory-hold/before-get-statuses.xml", assertionMode = NON_STRICT)
    public void getStatusesForNotExistingLot() {
        Set<InventoryHoldStatus> statuses = inventoryHoldDao.getStatusesByLot("0000012346");
        assertions.assertThat(statuses).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/inventory-hold/before-get-statuses.xml")
    @ExpectedDatabase(value = "/db/dao/inventory-hold/before-get-statuses.xml", assertionMode = NON_STRICT)
    public void getStatusesForExistingLot() {
        Set<InventoryHoldStatus> statuses = inventoryHoldDao.getStatusesByLot("0000012345");
        assertions.assertThat(statuses).containsExactlyInAnyOrder(InventoryHoldStatus.EXPIRED,
                InventoryHoldStatus.DAMAGE);
    }
}
