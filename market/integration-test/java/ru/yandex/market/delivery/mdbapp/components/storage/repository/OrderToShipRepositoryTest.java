package ru.yandex.market.delivery.mdbapp.components.storage.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import steps.utils.TestableClock;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderToShip;

@Sql(
    value = "/data/repository/orderToShip/cleanup.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    value = "/data/repository/orderToShip/order-to-ship.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@RunWith(Parameterized.class)
public class OrderToShipRepositoryTest extends MockContextualTest {

    @Autowired
    private HealthManager healthManager;

    @Autowired
    private TestableClock clock;

    @Autowired
    private OrderToShipRepository orderToShipRepository;

    @Parameterized.Parameter
    public Collection<String> ids;

    @Parameterized.Parameter(1)
    public Integer day;

    @Parameterized.Parameter(2)
    public Integer size;

    @Parameterized.Parameter(3)
    public String testName;

    @Before
    public void beforeTest() {
        Mockito.when(healthManager.isHealthyEnough()).thenReturn(true);
    }

    @Test
    public void testFindAllByParcelsIds() {
        clock.setFixed(
            LocalDateTime.of(2019, 6, day, 15, 0, 0)
                .toInstant(OffsetDateTime.now().getOffset()),
            ZoneId.systemDefault()
        );

        List<OrderToShip> orderToShipList = orderToShipRepository
            .findByIdIdInAndShipmentDayGreaterThanEqual(ids, LocalDate.now(clock));
        Assert.assertThat(orderToShipList, Matchers.hasSize(size));
    }

    @Parameterized.Parameters(name = "{index}: {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {
                Collections.emptyList(),
                1,
                0,
                "Empty list"
            },
            {
                Collections.singletonList("2"),
                1,
                1,
                "One order"
            },
            {
                Arrays.asList("1", "2"),
                1,
                3,
                "Part of orders"
            },
            {
                Arrays.asList("1", "2", "3"),
                1,
                4,
                "All orders"
            },
            {
                Arrays.asList("1", "2", "3"),
                3,
                2,
                "Ignore some order to ship in the past"
            },
            {
                Collections.singletonList("1"),
                3,
                1,
                "Ignore some order to ship in the past"
            },
            {
                Collections.singletonList("2"),
                3,
                0,
                "Ignore all order to ship in the past"
            },
        });
    }
}
