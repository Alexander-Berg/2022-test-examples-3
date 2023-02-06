package ru.yandex.market.replenishment.autoorder.repository;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.WarehouseType;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.SpecialOrder;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Supplier;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.User;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Warehouse;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SpecialOrderRepository;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
public class SpecialOrderRepositoryTest extends FunctionalTest {
    @Autowired
    private SpecialOrderRepository repository;

    private Supplier getSupplier() {
        Supplier supplier = new Supplier();
        supplier.setId(20);
        supplier.setName("Test supplier");
        supplier.setRsId("10");
        return supplier;
    }

    private User getUser() {
        User user = new User();
        user.setId(1L);
        user.setLogin("root");
        return user;
    }

    private SpecialOrder getSpecialOrderWithId(Long id) {
        SpecialOrder order = new SpecialOrder();
        order.setId(id);
        order.setWarehouse(
            new Warehouse(10L, "Test warehouse", WarehouseType.FULFILLMENT, null, null)
        );
        order.setSupplier(getSupplier());
        order.setSsku("20.10");
        order.setType(SpecialOrder.OrderType.LOT);
        order.setPrice(13.37);
        order.setShipmentQuantum(2);
        order.setUser(getUser());

        return order;
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderRepository.before-add.csv", after = "SpecialOrderRepository.after-add.csv")
    public void saveSpecialOrder() {
        repository.saveSpecialOrder(getSpecialOrderWithId(1L));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderRepository.before-add.csv")
    public void getNextIdSequenceValue() {
        Long nextId = repository.getNextIdSequenceValue();
        assertThat(nextId, is(1L));
    }
}
