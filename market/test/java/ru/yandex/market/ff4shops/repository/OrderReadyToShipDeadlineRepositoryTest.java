package ru.yandex.market.ff4shops.repository;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.model.entity.OrderReadyToShipDeadlineEntity;

class OrderReadyToShipDeadlineRepositoryTest extends FunctionalTest {

    @Autowired
    private OrderReadyToShipDeadlineRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DbUnitDataSet(after = "OrderReadyToShipDeadlineRepositoryTest.insertOrderReadyToShipDeadline.after.csv")
    void insertOrderReadyToShipDeadline() {
        transactionTemplate.execute(ignored -> repository.saveAll(deadlines()));
    }

    @Nonnull
    private List<OrderReadyToShipDeadlineEntity> deadlines() {
        OrderReadyToShipDeadlineEntity deadline1 = new OrderReadyToShipDeadlineEntity();
        deadline1.setCheckouterOrderId(123L);
        deadline1.setDeadline(Instant.parse("2021-08-16T13:28:10.00Z"));

        OrderReadyToShipDeadlineEntity deadline2 = new OrderReadyToShipDeadlineEntity();
        deadline2.setCheckouterOrderId(333L);
        deadline2.setDeadline(Instant.parse("2021-10-10T10:10:10.00Z"));
        deadline2.setSupplierId(100L);

        return List.of(deadline1, deadline2);
    }
}
