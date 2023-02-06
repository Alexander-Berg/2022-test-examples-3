package ru.yandex.market.replenishment.autoorder.repository;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.InterWarehouseReplenishment;
import ru.yandex.market.replenishment.autoorder.repository.postgres.InterWarehouseReplenishmentRepository;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
public class InterWarehouseReplenishmentRepositoryTest extends FunctionalTest {

    @Autowired
    private InterWarehouseReplenishmentRepository repository;

    @Test
    @DbUnitDataSet(before = "InterWarehouseReplenishmentRepository.before.csv")
    public void getReplenishments() {
        List<InterWarehouseReplenishment> replenishments = repository.findAll();

        assertThat(replenishments, hasSize(3));
        assertThat(replenishments, containsInAnyOrder(
                createExpectedInterWarehouseReplenishment(100, "2019-03-25"),
                createExpectedInterWarehouseReplenishment(200, "2019-03-25"),
                createExpectedInterWarehouseReplenishment(100, "2019-03-26")
        ));
    }

    private InterWarehouseReplenishment createExpectedInterWarehouseReplenishment(long msku, String orderDate) {
        return InterWarehouseReplenishment.builder()
                .msku(msku)
                .ssku(Long.toString(msku))
                .supplierId(1L)
                .orderDate(TestUtils.parseISOLocalDate(orderDate))
                .warehouseFrom(145)
                .warehouseTo(147)
                .deliveryTime(7L)
                .purchQty(10L)
                .supplierId(1L)
                .supplierType(1)
                .build();
    }
}
