package ru.yandex.market.tpl.core.service.monitoring.listener;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.routing.events.ScRoutingResultsUpdatedEvent;
import ru.yandex.market.tpl.core.service.monitoring.dbqueue.shift.verification_with_sc.VerificationShiftWithScProducer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_ROUTING_MONITORING_SC_ENABLED;

@ExtendWith(MockitoExtension.class)
class OnFinishedShiftRoutingListenerTest {

    @Mock
    private VerificationShiftWithScProducer verificationShiftWithScProducer;
    @Mock
    private ConfigurationProviderAdapter configurationProvider;
    @InjectMocks
    private OnFinishedShiftRoutingListener routingListener;


    @Test
    void addVerificationTask_when_Enabled() {
        //given
        doReturn(true).when(configurationProvider)
                .isBooleanEnabled(eq(IS_ROUTING_MONITORING_SC_ENABLED));

        //when
        routingListener.verificationShiftWithSc(
                new ScRoutingResultsUpdatedEvent(LocalDate.now(), 1L, "processingId")
        );

        //then
        verify(verificationShiftWithScProducer, times(1)).produce(any(), any(), any());
    }

    @Test
    void addVerificationTask_when_Disabled() {
        //given
        doReturn(false).when(configurationProvider)
                .isBooleanEnabled(eq(IS_ROUTING_MONITORING_SC_ENABLED));

        //when
        routingListener.verificationShiftWithSc(
                new ScRoutingResultsUpdatedEvent(LocalDate.now(), 1L, "processingId")
        );

        //then
        verify(verificationShiftWithScProducer, never()).produce(any(), any(), any());
    }
}
