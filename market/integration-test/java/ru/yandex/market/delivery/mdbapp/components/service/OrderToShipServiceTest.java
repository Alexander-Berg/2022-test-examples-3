package ru.yandex.market.delivery.mdbapp.components.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import steps.utils.TestableClock;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.service.capacity.OrderToShipService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderToShip;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderToShipId;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderToShipValue;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.CapacityCountingType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.OrderToShipStatus;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.OrderToShipRepository;
import ru.yandex.market.delivery.mdbapp.exception.InvalidEntityException;
import ru.yandex.market.delivery.mdbclient.model.dto.DeliveryType;
import ru.yandex.market.delivery.mdbclient.model.dto.OrderToShipDto;

import static org.mockito.Mockito.when;

@Sql(
    value = "/data/repository/orderToShip/cleanup.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    value = "/data/repository/orderToShip/order-to-ship-updater.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public class OrderToShipServiceTest extends MockContextualTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private OrderToShipService orderToShipService;

    @Autowired
    private OrderToShipRepository orderToShipRepository;

    @Autowired
    private HealthManager healthManager;

    @Autowired
    private TestableClock clock;

    @Before
    public void beforeTest() {
        when(healthManager.isHealthyEnough()).thenReturn(true);
        clock.setFixed(
            LocalDateTime.of(2018, 7, 17, 15, 0, 0)
                .toInstant(OffsetDateTime.now().getOffset()),
            ZoneId.systemDefault()
        );
    }

    @After
    public void clear() {
        clock.clearFixed();
    }

    @Test
    public void testCreate() {
        OrderToShipDto dto = newDto();
        OrderToShipDto orderToShipDto = orderToShipService.saveCreated(dto);

        softly.assertThat(orderToShipDto).isEqualTo(dto);

        softly.assertThat(orderToShipRepository.findAll())
            .extracting(OrderToShip::getId)
            .extracting(OrderToShipId::getId)
            .containsExactlyInAnyOrder("A1", "A1", "B2", "C3");
    }

    @Test
    public void testTryCreateExisting() {
        thrown.expect(InvalidEntityException.class);
        thrown.expectMessage(
            "Entity OrderToShipId{id='A1', platformClientId=1, " +
                "partnerId=1, serviceType=UNKNOWN, status=CREATED} is already existed.");
        orderToShipService.saveCreated(dtoExistingId());
    }

    @Test
    @Transactional
    public void testDelete() {
        orderToShipService.saveCancelled("A1", 1L, 1L, null);

        List<OrderToShip> orderToShips = orderToShipRepository.findAll();

        softly.assertThat(orderToShips)
            .extracting(OrderToShip::getId)
            .extracting(OrderToShipId::getId)
            .containsExactlyInAnyOrder("A1", "B2", "C3", "A1");

        softly.assertThat(orderToShips)
            .extracting(OrderToShip::getId)
            .extracting(OrderToShipId::getStatus)
            .containsOnly(OrderToShipStatus.CREATED, OrderToShipStatus.CANCELLED);

        softly.assertThat(orderToShips)
            .flatExtracting(OrderToShip::getOrderToShipValues)
            .extracting(OrderToShipValue::getCountingType)
            .containsOnly(CapacityCountingType.ORDER, CapacityCountingType.ITEM);

        softly.assertThat(orderToShips)
            .flatExtracting(OrderToShip::getOrderToShipValues)
            .extracting(OrderToShipValue::getValue)
            .containsOnly(1L, 3L, 1L, 17L, 1L, 21L, 1L, 21L);
    }

    @Test
    public void testTryDeleteNotExisting() {
        orderToShipService.saveCancelled("B2", 3L, 2L, null);
        softly.assertThat(orderToShipRepository.findAll()).hasSize(3);
    }

    private OrderToShipDto newDto() {
        return new OrderToShipDto("A1", 1L, 2L, 1L, 1L,
            DeliveryType.DELIVERY, null, LocalDate.of(2019, 6, 27));
    }

    private OrderToShipDto dtoExistingId() {
        return new OrderToShipDto("A1", 1L, 1L, 225L, 225L,
            DeliveryType.POST, null, LocalDate.of(2019, 6, 28));
    }
}
