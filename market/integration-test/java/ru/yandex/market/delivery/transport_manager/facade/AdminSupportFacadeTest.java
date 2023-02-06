package ru.yandex.market.delivery.transport_manager.facade;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationSubstatus;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.cancellation.TransportationCancellationProducer;
import ru.yandex.market.delivery.transport_manager.service.TransportationService;

public class AdminSupportFacadeTest extends AbstractContextualTest {
    @Autowired
    private AdminSupportFacade adminSupportFacade;

    @Autowired
    private TransportationCancellationProducer cancellationProducer;

    @Autowired
    private TransportationService transportationService;

    @Test
    @DatabaseSetup("/repository/transportation/cancelled_transportation.xml")
    void cancellationFailed() {
        softly.assertThatThrownBy(
            () -> adminSupportFacade.cancel(2L)
        ).hasMessage("Невозможно отменить перемещение 2");
    }

    @Test
    @DatabaseSetup("/repository/transportation/for_cancellation.xml")
    void cancellationSucceeded() {
        adminSupportFacade.cancel(1L);
        Mockito.verify(cancellationProducer)
            .enqueue(
                1L,
                TransportationSubstatus.MANUAL_CANCELLATION
            );
    }
}
