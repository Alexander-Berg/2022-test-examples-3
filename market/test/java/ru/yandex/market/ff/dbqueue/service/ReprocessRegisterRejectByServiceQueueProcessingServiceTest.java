package ru.yandex.market.ff.dbqueue.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.ReprocessRegisterRejectByServicePayload;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ReprocessRegisterRejectByServiceQueueProcessingServiceTest extends IntegrationTest {

    @Autowired
    private ReprocessRegisterRejectByServiceQueueProcessingService service;

    private static final Long VALID_ID = 1L;

    @Test
    @DatabaseSetup("classpath:db-queue/service/reprocess-register-reject-by-service-queue-processing/" +
            "successfully-process-payload.xml")
    public void successfullyProcessPayload() throws GatewayApiException {
        ReprocessRegisterRejectByServicePayload payload = new ReprocessRegisterRejectByServicePayload(VALID_ID);
        service.processPayload(payload);
        verify(fulfillmentClient, times(1)).putInboundRegistry(any(), any());
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/reprocess-register-reject-by-service-queue-processing/" +
            "process-payload-with-incorrect-status.xml")
    public void processPayloadWithIncorrectStatus() throws GatewayApiException {
        ReprocessRegisterRejectByServicePayload payload = new ReprocessRegisterRejectByServicePayload(VALID_ID);
        service.processPayload(payload);
        verify(fulfillmentClient, never()).putInboundRegistry(any(), any());
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/reprocess-register-reject-by-service-queue-processing/" +
            "process-payload-without-registry.xml")
    public void processPayloadWithoutRegistry() throws GatewayApiException {
        ReprocessRegisterRejectByServicePayload payload = new ReprocessRegisterRejectByServicePayload(VALID_ID);
        service.processPayload(payload);
        verify(fulfillmentClient, never()).putInboundRegistry(any(), any());
    }
}
