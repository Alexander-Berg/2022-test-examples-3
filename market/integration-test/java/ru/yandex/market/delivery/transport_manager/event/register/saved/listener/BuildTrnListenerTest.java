package ru.yandex.market.delivery.transport_manager.event.register.saved.listener;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.event.register.saved.RegisterSavedEvent;
import ru.yandex.market.delivery.transport_manager.queue.task.trn.BuildTransportationTrnProducer;
import ru.yandex.market.delivery.transport_manager.service.PropertyService;

class BuildTrnListenerTest extends AbstractContextualTest {
    @Autowired
    private BuildTrnListener buildTrnListener;

    @Autowired
    private PropertyService<TmPropertyKey> propertyService;

    @Autowired
    private BuildTransportationTrnProducer buildTransportationTrnProducer;

    @Test
    void listen() {
        buildTrnListener.listen(event(register(RegisterType.FACT), TransportationUnitType.OUTBOUND));
        Mockito.verify(buildTransportationTrnProducer, Mockito.never()).enqueue(Mockito.any());

        Mockito.when(propertyService.get(TmPropertyKey.ENABLE_TRN_BUILDING)).thenReturn(true);

        buildTrnListener.listen(event(register(RegisterType.FACT), TransportationUnitType.OUTBOUND));
        Mockito.verify(buildTransportationTrnProducer).produce(1L, 1L);

        buildTrnListener.listen(event(register(RegisterType.PLAN), TransportationUnitType.OUTBOUND));
        Mockito.verify(buildTransportationTrnProducer, Mockito.never()).enqueue(Mockito.any());

        buildTrnListener.listen(event(register(RegisterType.FACT), TransportationUnitType.INBOUND));
        Mockito.verify(buildTransportationTrnProducer, Mockito.never()).enqueue(Mockito.any());
    }

    private Register register(RegisterType registerType) {
        var register = new Register();
        register.setId(1L);
        register.setType(registerType);
        return register;
    }

    private RegisterSavedEvent event(Register register, TransportationUnitType unitType) {
        return new RegisterSavedEvent(
            this,
            new Transportation().setId(1L),
            register,
            unitType
        );
    }
}
