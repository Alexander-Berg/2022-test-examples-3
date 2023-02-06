package ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.transfer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.PutInboundRegisterProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.transfer.ffwf.TransferRegisterFfwfConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.transfer.ffwf.TransferRegisterFfwfDto;
import ru.yandex.market.delivery.transport_manager.service.TransportationService;
import ru.yandex.market.delivery.transport_manager.service.checker.PartnerMethodsCheckService;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;
import ru.yandex.market.delivery.transport_manager.service.transportation_unit.TransportationUnitService;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// todo: TMDEV-308
class TransferRegisterFfwfConsumerMockTest {
    private static final String SHARD_ID = "SHARD ID";
    private static final String PARTNER_ID = "7";
    private static final Long OUTBOUND_FACT_REGISTER_ID = 7L;
    private static final Long INBOUND_PARTNER_ID = 8L;

    @Mock
    private PutInboundRegisterProducer putInboundRegisterProducer;

    @Mock
    private PartnerMethodsCheckService partnerMethodsCheckService;

    @Mock
    private RegisterService registerService;

    @Mock
    private TransportationUnitService transportationUnitService;

    @Mock
    private TransportationService transportationService;

    @InjectMocks
    private TransferRegisterFfwfConsumer consumer;

    @Test
    void execute_succeedsAndCallsFFWFPutInboundRegistryWithConvertedRegistry() {
        when(partnerMethodsCheckService.supportsPutInboundRegistryMethod(any(), any())).thenReturn(true);
        when(transportationUnitService.getInboundTransportationUnitByOutboundRegisterId(OUTBOUND_FACT_REGISTER_ID))
            .thenReturn(getInboundTransportationUnit());
        when(transportationService.getTransportationIdByInboundUnitId(any())).thenReturn(1L);

        assertThat(consumer.execute(createTask())).isEqualTo(TaskExecutionResult.finish());

        verify(putInboundRegisterProducer).produce(1L);
    }

    private Task<TransferRegisterFfwfDto> createTask() {
        return Task.<TransferRegisterFfwfDto>builder(new QueueShardId(SHARD_ID))
            .withPayload(new TransferRegisterFfwfDto(OUTBOUND_FACT_REGISTER_ID))
            .build();

    }

    private TransportationUnit getInboundTransportationUnit() {
        TransportationUnit transportationUnit = new TransportationUnit();
        transportationUnit.setId(1L);
        transportationUnit.setRequestId(1L);
        transportationUnit.setPartnerId(INBOUND_PARTNER_ID);
        return transportationUnit;
    }
}
