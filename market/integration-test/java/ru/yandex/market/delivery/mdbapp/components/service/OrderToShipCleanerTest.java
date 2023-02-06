package ru.yandex.market.delivery.mdbapp.components.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import steps.utils.TestableClock;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.service.capacity.OrderToShipCleaner;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderToShip;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.OrderToShipRepository;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

@Sql(
    value = "/data/repository/orderToShip/cleanup.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    value = "/data/repository/orderToShip/order-to-ship.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public class OrderToShipCleanerTest extends MockContextualTest {

    @Autowired
    private HealthManager healthManager;

    @Autowired
    private OrderToShipCleaner orderToShipCleaner;

    @Autowired
    private OrderToShipRepository repository;

    @Autowired
    private TestableClock clock;

    @Before
    public void beforeTest() {
        Mockito.when(healthManager.isHealthyEnough()).thenReturn(true);
    }

    @Test
    @JpaQueriesCount(2)
    public void testOldOrdersCleaned() {
        setClock(3);
        orderToShipCleaner.deleteAllOutdated();

        List<OrderToShip> allOrdersToShip = repository.findAll();
        softly.assertThat(allOrdersToShip).hasSize(2);
        softly.assertThat(allOrdersToShip).extracting(OrderToShip::getLocationFromId).containsOnly(3L, 4L);
    }

    @Test
    @JpaQueriesCount(5)
    public void testAllOrdersCleaned() {
        setClock(5);
        orderToShipCleaner.deleteAllOutdated();

        List<OrderToShip> allOrdersToShip = repository.findAll();
        softly.assertThat(allOrdersToShip).isEmpty();
    }

    @Test
    @JpaQueriesCount(2)
    public void testNoOrderCleaned() {
        setClock(1);
        orderToShipCleaner.deleteAllOutdated();

        List<OrderToShip> allOrdersToShip = repository.findAll();
        softly.assertThat(allOrdersToShip).hasSize(4);
        softly.assertThat(allOrdersToShip).extracting(OrderToShip::getLocationFromId).containsOnly(1L, 2L, 3L, 4L);
    }

    @Test
    @JpaQueriesCount(2)
    public void checkLimitDelete() {
        orderToShipCleaner.setBatchSize(3);
        setClock(5);
        orderToShipCleaner.deleteAllOutdated();
        List<OrderToShip> allOrdersToShip = repository.findAll();
        softly.assertThat(allOrdersToShip).isEmpty();
    }

    private void setClock(Integer day) {
        clock.setFixed(
            LocalDateTime.of(2019, 6, day, 2, 0, 0)
                .toInstant(OffsetDateTime.now().getOffset()),
            ZoneId.systemDefault()
        );
    }
}
