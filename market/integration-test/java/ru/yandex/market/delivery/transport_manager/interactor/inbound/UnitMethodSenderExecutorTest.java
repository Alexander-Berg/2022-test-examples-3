package ru.yandex.market.delivery.transport_manager.interactor.inbound;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.facade.FfWfInboundFacade;
import ru.yandex.market.delivery.transport_manager.interactor.UnitMethodSenderExecutor;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.external.lgw.LgwClientExecutor;

import static org.mockito.ArgumentMatchers.any;

@DatabaseSetup({
    "/repository/facade/transportation_with_deps.xml",
    "/repository/facade/courier.xml",
    "/repository/facade/interactor/metadata.xml",
    "/repository/facade/interactor/method.xml",
})
class UnitMethodSenderExecutorTest extends AbstractContextualTest {

    @Autowired
    private TransportationMapper transportationMapper;

    @Autowired
    private UnitMethodSenderExecutor executor;

    @Autowired
    private FfWfInboundFacade inboundFacade;

    @Autowired
    private LgwClientExecutor lgwClientExecutor;

    @Test
    @DisplayName("null стратегия направляет запрос в FFWF")
    void putUsingFfWfStrategy() {
        Mockito.doNothing().when(inboundFacade).putInbound(Mockito.argThat(t -> t.getId() == 1L), Mockito.eq(false));

        executor.put(transportationMapper.getById(1L), TransportationUnitType.INBOUND);
        Mockito.verify(inboundFacade).putInbound(
            Mockito.argThat(t -> t.getId() == 1L),
            Mockito.eq(false)
        );
    }

    @Test
    @DatabaseSetup("/repository/facade/transportations_for_cancellation.xml")
    void cancelViaFfwf() {
        Mockito.doNothing().when(inboundFacade).cancelUnit(3L);

        var transportation = transportationMapper.getById(2L);
        executor.cancel(transportation.getInboundUnit());
        Mockito.verify(inboundFacade).cancelUnit(3L);
    }

    @Test
    @DatabaseSetup("/repository/facade/transportations_for_cancellation.xml")
    void cancelViaLgw() {
        var transportation = transportationMapper.getById(3L);

        executor.cancel(transportation.getOutboundUnit());
        Mockito.verify(lgwClientExecutor).cancelOutbound(any(), any());
    }
}
