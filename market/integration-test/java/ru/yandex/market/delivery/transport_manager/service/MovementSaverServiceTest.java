package ru.yandex.market.delivery.transport_manager.service;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.service.movement.MovementSaverService;

class MovementSaverServiceTest extends AbstractContextualTest {
    @Autowired
    MovementSaverService movementSaverService;

    @ExpectedDatabase(
        value = "/repository/movement/after/movement_new_with_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void persist() {
        movementSaverService.persist(
            new Movement()
                .setStatus(MovementStatus.IN_PROGRESS)
                .setPartnerId(1L)
                .setVolume(24)
                .setWeight(10)
        );
    }
}
