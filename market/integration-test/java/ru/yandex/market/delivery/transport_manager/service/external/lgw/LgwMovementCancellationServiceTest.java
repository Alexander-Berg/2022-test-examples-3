package ru.yandex.market.delivery.transport_manager.service.external.lgw;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.converter.lgw.LgwCommonResourceIdConverter;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;

public class LgwMovementCancellationServiceTest extends AbstractContextualTest {

    @Autowired
    private LgwMovementCancellationService lgwMovementCancellationService;

    @Autowired
    private LgwClientExecutor lgwClientExecutor;

    @Autowired
    private LgwCommonResourceIdConverter resourceIdConverter;

    @Test
    @DatabaseSetup("/repository/movement/movement_test.xml")
    @ExpectedDatabase(
        value = "/repository/service/movement/after/single_transportation_movement_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void singleTransportation() {
        Mockito.doNothing().when(lgwClientExecutor).cancelMovement(Mockito.any(), Mockito.any());

        lgwMovementCancellationService.cancelMovement(1L);
        Mockito.verify(lgwClientExecutor).cancelMovement(
            resourceIdConverter.movementId(1L, "movement1"),
            new Partner(156L)
        );
    }

    @Test
    @DatabaseSetup("/repository/service/movement/with_several_transportations.xml")
    void severalTransportationsActive() {
        lgwMovementCancellationService.cancelMovement(1L);
        Mockito.verify(lgwClientExecutor, Mockito.times(0)).cancelMovement(
            resourceIdConverter.movementId(4L, null),
            new Partner(4L)
        );
    }

}
