package ru.yandex.market.delivery.transport_manager.service.movement;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.service.core.TmEventPublisher;

class MovementApproverTest extends AbstractContextualTest {
    @Autowired
    private MovementApprover movementApprover;

    @Autowired
    private TmEventPublisher eventPublisher;

    @Test
    @DatabaseSetup("/repository/interwarehouse/auto_approve/tasks_and_different_transportations.xml")
    void approve() {
        movementApprover.approve(List.of(5L, 50L));

        Mockito.verify(eventPublisher, Mockito.times(1)).publishEvent(Mockito.any());
    }

    @Test
    @DatabaseSetup("/repository/interwarehouse/auto_approve/tasks_and_different_transportations.xml")
    void onlyOneEventPublished() {
        movementApprover.approve(List.of(5L, 6L));

        Mockito.verify(eventPublisher, Mockito.times(1)).publishEvent(Mockito.any());
    }
}
