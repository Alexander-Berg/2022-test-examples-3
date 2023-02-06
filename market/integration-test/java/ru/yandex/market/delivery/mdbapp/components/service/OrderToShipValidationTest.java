package ru.yandex.market.delivery.mdbapp.components.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;

import javax.validation.ConstraintViolationException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import steps.utils.TestableClock;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.service.capacity.OrderToShipService;
import ru.yandex.market.delivery.mdbclient.model.dto.DeliveryType;
import ru.yandex.market.delivery.mdbclient.model.dto.OrderToShipDto;

import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class OrderToShipValidationTest extends MockContextualTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private OrderToShipService orderToShipService;

    @Autowired
    private HealthManager healthManager;

    @Autowired
    private TestableClock clock;


    @Parameterized.Parameter
    public OrderToShipDto orderToShipDto;

    @Parameterized.Parameter(1)
    public String message;

    @Before
    public void beforeTest() {
        when(healthManager.isHealthyEnough()).thenReturn(true);
        clock.setFixed(
            LocalDateTime.of(2018, 7, 17, 15, 0, 0)
                .toInstant(OffsetDateTime.now().getOffset()),
            ZoneId.systemDefault()
        );
    }

    @Test
    public void testTryCreateWithNull() {
        thrown.expect(ConstraintViolationException.class);
        thrown.expectMessage(message);
        orderToShipService.saveCreated(orderToShipDto);
    }

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {new OrderToShipDto(null, 1L, 1L, 1L, 1L,
                DeliveryType.POST, null, LocalDate.now()), "saveCreated.dto.id: must not be null"},
            {new OrderToShipDto("ABC", null, 1L, 1L, 1L,
                DeliveryType.POST, null, LocalDate.now()), "saveCreated.dto.platformClientId: must not be null"},
            {new OrderToShipDto("ABC", 1L, null, 1L, 1L,
                DeliveryType.POST, null, LocalDate.now()), "saveCreated.dto.partnerId: must not be null"},
            {new OrderToShipDto("ABC", 1L, 1L, null, 1L,
                DeliveryType.POST, null, LocalDate.now()), "saveCreated.dto.locationFromId: must not be null"},
            {new OrderToShipDto("ABC", 1L, 1L, 1L, null,
                DeliveryType.POST, null, LocalDate.now()), "saveCreated.dto.locationToId: must not be null"},
            {new OrderToShipDto("ABC", 1L, 1L, 1L, 1L,
                null, null, LocalDate.now()), "saveCreated.dto.deliveryType: must not be null"},
            {new OrderToShipDto("ABC", 1L, 1L, 1L, 1L,
                DeliveryType.POST, null, null), "saveCreated.dto.shipmentDay: must not be null"}
        });
    }
}
