package ru.yandex.market.delivery.transport_manager.service.interwarehouse.movement;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.service.movement.MovementApprover;

class InterwarehouseMovementApproverTest extends AbstractContextualTest {
    @Autowired
    private TmPropertyService propertyService;

    @Autowired
    private InterwarehouseMovementApprover interwarehouseMovementApprover;

    @Autowired
    private MovementApprover movementApprover;

    @Test
    @DatabaseSetup("/repository/interwarehouse/auto_approve/tasks_and_different_transportations.xml")
    void approve() {
        clock.setFixed(Instant.parse("2021-02-20T21:00:00.00Z"), ZoneOffset.UTC);
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_AUTO_APPROVE_INTERWAREHOUSE_MOVEMENTS))
            .thenReturn(true);

        interwarehouseMovementApprover.approve();

        Mockito.verify(movementApprover).approve(
            Mockito.argThat(list -> list.containsAll(List.of(5L, 6L, 500L)))
        );
    }
}
