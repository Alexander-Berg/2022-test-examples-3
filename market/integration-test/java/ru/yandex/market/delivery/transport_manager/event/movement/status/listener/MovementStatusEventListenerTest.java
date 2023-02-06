package ru.yandex.market.delivery.transport_manager.event.movement.status.listener;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ClassUtils;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.service.StatusHistoryService;
import ru.yandex.market.delivery.transport_manager.service.movement.MovementStatusService;

class MovementStatusEventListenersTest extends AbstractContextualTest {
    @Autowired
    private List<MovementStatusEventListener> listeners;

    @Autowired
    private StatusHistoryService statusHistoryService;

    @Test
    void testAllImplementationsHasStatusServiceMapping() {
        MockStatusService mockStatusService = new MockStatusService(statusHistoryService);

        listeners.forEach(
            movementStatusEventListener -> softly
                .assertThat(
                    mockStatusService.getMapping()
                        .containsKey(ClassUtils.getUserClass(movementStatusEventListener))
                )
                .as(
                    ClassUtils.getUserClass(movementStatusEventListener).getSimpleName() +
                        " do not have mapping in MovementStatusService"
                )
                .isTrue()
        );
    }

    private static final class MockStatusService extends MovementStatusService {
        MockStatusService(StatusHistoryService statusHistoryService) {
            super(statusHistoryService);
        }

        public Map<Class<? extends MovementStatusEventListener>, Collection<MovementStatus>> getMapping() {
            return SUBSCRIBED_LISTENERS;
        }
    }
}
