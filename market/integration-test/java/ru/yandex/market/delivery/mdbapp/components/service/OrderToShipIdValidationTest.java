package ru.yandex.market.delivery.mdbapp.components.service;

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
import ru.yandex.market.delivery.mdbclient.model.dto.CapacityServiceType;

import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class OrderToShipIdValidationTest extends MockContextualTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private OrderToShipService orderToShipService;

    @Autowired
    private HealthManager healthManager;

    @Autowired
    private TestableClock clock;

    @Parameterized.Parameter
    public String id;

    @Parameterized.Parameter(1)
    public Long platformClientId;

    @Parameterized.Parameter(2)
    public Long partnerId;

    @Parameterized.Parameter(3)
    public CapacityServiceType capacityServiceType;

    @Parameterized.Parameter(4)
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
        orderToShipService.saveCancelled(id, platformClientId, partnerId, capacityServiceType);
    }

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {null, 1L, 1L, CapacityServiceType.DELIVERY, "saveCancelled.id: must not be null"},
            {"ABC", null, 1L, CapacityServiceType.DELIVERY, "saveCancelled.platformClientId: must not be null"},
            {"ABC", 1L, null, CapacityServiceType.DELIVERY, "saveCancelled.partnerId: must not be null"},
            {"ABC", 1L, null, null, "saveCancelled.partnerId: must not be null"}
        });
    }
}
