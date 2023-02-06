package ru.yandex.market.ff.dbqueue.producer.service;

import javax.annotation.Nonnull;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.dbqueue.producer.BaseQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.ValidateCommonRequestQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.ValidateShadowSupplyRequestQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.ValidateShadowWithdrawRequestQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.ValidateSupplyRequestQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.ValidateUpdatingRequestQueueProducer;
import ru.yandex.market.ff.model.TypeSubtype;
import ru.yandex.market.ff.model.dbqueue.ValidateRequestPayload;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.service.RequestSubTypeService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SendingToCorrectValidationProducerServiceTest {

    private static final long REQUEST_ID = 123;
    private final RequestSubTypeEntity requestSubTypeEntity = mock(RequestSubTypeEntity.class);

    private RequestSubTypeService requestSubTypeService;
    private ValidateSupplyRequestQueueProducer supplyProducer;
    private ValidateCommonRequestQueueProducer commonProducer;
    private ValidateShadowSupplyRequestQueueProducer shadowSupplyProducer;
    private ValidateShadowWithdrawRequestQueueProducer shadowWithdrawProducer;
    private ValidateUpdatingRequestQueueProducer updatingProducer;
    private SendingToCorrectValidationProducerService sendingToCorrectValidationProducerService;
    private SoftAssertions assertions;

    @BeforeEach
    public void init() {
        requestSubTypeService = mock(RequestSubTypeService.class);
        supplyProducer = mock(ValidateSupplyRequestQueueProducer.class);
        commonProducer = mock(ValidateCommonRequestQueueProducer.class);
        shadowSupplyProducer = mock(ValidateShadowSupplyRequestQueueProducer.class);
        shadowWithdrawProducer = mock(ValidateShadowWithdrawRequestQueueProducer.class);
        updatingProducer = mock(ValidateUpdatingRequestQueueProducer.class);
        sendingToCorrectValidationProducerService = new SendingToCorrectValidationProducerService(
            requestSubTypeService,
                supplyProducer,
                commonProducer,
                shadowSupplyProducer,
                shadowWithdrawProducer,
                updatingProducer
        );
        assertions = new SoftAssertions();
        when(requestSubTypeService.getEntityByRequestTypeAndSubtype(any())).thenReturn(requestSubTypeEntity);

    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    @Test
    public void shouldNotSendWhenRequestTypeShouldNotBeValidated() {
        when(requestSubTypeEntity.isToBeValidated()).thenReturn(false);
        boolean sent = sendingToCorrectValidationProducerService
            .sendIfAllowed(REQUEST_ID, new TypeSubtype(RequestType.TRANSFER, "DEFAULT"));
        assertions.assertThat(sent).isFalse();
        verify(supplyProducer, never()).produceSingle(any());
        verify(shadowSupplyProducer, never()).produceSingle(any());
        verify(commonProducer, never()).produceSingle(any());
    }

    @Test
    public void sendSupply() {
        when(requestSubTypeEntity.isToBeValidated()).thenReturn(true);
        assertSentToCorrectProducer(RequestType.SUPPLY, supplyProducer);
        verify(shadowSupplyProducer, never()).produceSingle(any());
        verify(shadowWithdrawProducer, never()).produceSingle(any());
        verify(updatingProducer, never()).produceSingle(any());
        verify(commonProducer, never()).produceSingle(any());
    }

    @Test
    public void sendShadowSupply() {
        when(requestSubTypeEntity.isToBeValidated()).thenReturn(true);
        assertSentToCorrectProducer(RequestType.SHADOW_SUPPLY, shadowSupplyProducer);
        verify(supplyProducer, never()).produceSingle(any());
        verify(shadowWithdrawProducer, never()).produceSingle(any());
        verify(updatingProducer, never()).produceSingle(any());
        verify(commonProducer, never()).produceSingle(any());
    }

    @Test
    public void sendWithdraw() {
        when(requestSubTypeEntity.isToBeValidated()).thenReturn(true);
        assertSentToCorrectProducer(RequestType.WITHDRAW, commonProducer);
        verify(supplyProducer, never()).produceSingle(any());
        verify(shadowSupplyProducer, never()).produceSingle(any());
        verify(shadowWithdrawProducer, never()).produceSingle(any());
        verify(updatingProducer, never()).produceSingle(any());
    }

    @Test
    public void sendCustomerReturnSupply() {
        when(requestSubTypeEntity.isToBeValidated()).thenReturn(true);
        assertSentToCorrectProducer(RequestType.CUSTOMER_RETURN_SUPPLY, commonProducer);
        verify(supplyProducer, never()).produceSingle(any());
        verify(shadowSupplyProducer, never()).produceSingle(any());
        verify(shadowWithdrawProducer, never()).produceSingle(any());
        verify(updatingProducer, never()).produceSingle(any());
    }

    @Test
    public void sendTransfer() {
        when(requestSubTypeEntity.isToBeValidated()).thenReturn(true);
        assertSentToCorrectProducer(RequestType.TRANSFER, commonProducer);
        verify(supplyProducer, never()).produceSingle(any());
        verify(shadowSupplyProducer, never()).produceSingle(any());
        verify(shadowWithdrawProducer, never()).produceSingle(any());
        verify(updatingProducer, never()).produceSingle(any());
    }

    @Test
    public void sendOrderSupply() {
        when(requestSubTypeEntity.isToBeValidated()).thenReturn(true);
        assertSentToCorrectProducer(RequestType.ORDERS_SUPPLY, commonProducer);
        verify(supplyProducer, never()).produceSingle(any());
        verify(shadowSupplyProducer, never()).produceSingle(any());
        verify(shadowWithdrawProducer, never()).produceSingle(any());
        verify(updatingProducer, never()).produceSingle(any());
    }

    @Test
    public void sendUpdatingRequest() {
        when(requestSubTypeEntity.isToBeValidated()).thenReturn(true);
        assertSentToCorrectProducer(RequestType.UPDATING_REQUEST, updatingProducer);
        verify(supplyProducer, never()).produceSingle(any());
        verify(shadowSupplyProducer, never()).produceSingle(any());
        verify(shadowWithdrawProducer, never()).produceSingle(any());
        verify(commonProducer, never()).produceSingle(any());
    }

    @Test
    public void sendShadowWithdrawRequest() {
        when(requestSubTypeEntity.isToBeValidated()).thenReturn(true);
        assertSentToCorrectProducer(RequestType.SHADOW_WITHDRAW, shadowWithdrawProducer);
        verify(supplyProducer, never()).produceSingle(any());
        verify(shadowSupplyProducer, never()).produceSingle(any());
        verify(updatingProducer, never()).produceSingle(any());
        verify(commonProducer, never()).produceSingle(any());
    }

    private void assertSentToCorrectProducer(
            @Nonnull RequestType requestType,
            @Nonnull BaseQueueProducer<ValidateRequestPayload> producer
    ) {
        boolean sent = sendingToCorrectValidationProducerService
            .sendIfAllowed(REQUEST_ID, new TypeSubtype(requestType, "DEFAULT"));
        assertions.assertThat(sent).isTrue();
        ArgumentCaptor<ValidateRequestPayload> captor = ArgumentCaptor.forClass(ValidateRequestPayload.class);
        verify(producer).produceSingle(captor.capture());
        ValidateRequestPayload payload = captor.getValue();
        assertions.assertThat(payload.getEntityId()).isEqualTo(REQUEST_ID);
    }
}
