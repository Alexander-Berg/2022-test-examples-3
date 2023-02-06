package ru.yandex.market.delivery.transport_manager.service.transportation_unit;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.master.NewSchemeTransportationRouter;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml"
})
class TransportationUnitUpdaterTest extends AbstractContextualTest {
    @Autowired
    private NewSchemeTransportationRouter newSchemeTransportationRouter;

    @Autowired
    private TransportationUnitUpdater transportationUnitUpdater;


    @Test
    void update() {
        transportationUnitUpdater.update(2L);

        Mockito.verify(newSchemeTransportationRouter, Mockito.times(1))
            .sendOutboundIfSupported(Mockito.any(), Mockito.any());
    }

    @Test
    @DatabaseSetup(
        value = "/repository/transportation/update/status_new_for_units.xml",
        type = DatabaseOperation.UPDATE
    )
    void ignoreUpdate() {
        transportationUnitUpdater.update(2L);
        transportationUnitUpdater.update(3L);

        Mockito.verifyNoInteractions(newSchemeTransportationRouter);

    }
}
