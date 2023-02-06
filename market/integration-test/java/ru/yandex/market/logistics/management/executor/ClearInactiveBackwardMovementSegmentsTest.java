package ru.yandex.market.logistics.management.executor;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.facade.LogisticSegmentFacade;

@DisplayName("Очистка еджей от/к BACKWARD_MOVEMENT сегментам для уже неактивных MOVEMENT сегментов")
@DatabaseSetup("/data/executor/clear-inactive-backward-movements/before/setup.xml")
class ClearInactiveBackwardMovementSegmentsTest extends AbstractContextualAspectValidationTest {
    @Autowired
    private LogisticSegmentFacade logisticSegmentFacade;

    @Test
    @DisplayName("Еджи и сегменты удаляются")
    @ExpectedDatabase(
        value = "/data/executor/clear-inactive-backward-movements/after/success_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successClear() {
        logisticSegmentFacade.clearInactiveBackwardMovementSegments();
    }

    @Test
    @DisplayName("MOVEMENT активен, еджи не удаляются")
    @DatabaseSetup(
        value = "/data/executor/clear-inactive-backward-movements/before/active_service.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/clear-inactive-backward-movements/after/success_active_movement.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successActiveMovement() {
        logisticSegmentFacade.clearInactiveBackwardMovementSegments();
    }

    @Test
    @DisplayName(
        "MOVEMENT активен, но нет ни одного еджа, идущего к этому MOVEMENT сегменту, еджи BACKWARD_MOVEMENT удаляются"
    )
    @DatabaseSetup(
        value = "/data/executor/clear-inactive-backward-movements/before/active_service.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/executor/clear-inactive-backward-movements/before/deleted_from_edges.xml",
        type = DatabaseOperation.DELETE
    )
    @ExpectedDatabase(
        value = "/data/executor/clear-inactive-backward-movements/after/success_delete_active_movement_no_edges.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successActiveMovementNoEdgesFrom() {
        logisticSegmentFacade.clearInactiveBackwardMovementSegments();
    }
}
