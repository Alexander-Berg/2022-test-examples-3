package ru.yandex.market.ff4shops.dbqueue.consumer;

import javax.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.dbqueue.dto.SendStocksByPiToMbiPayload;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.BooleanParamValueDTO;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class SendStocksByPiToMbiConsumerTest extends FunctionalTest {
    @Autowired
    private MbiOpenApiClient mbiOpenApiClient;

    @Autowired
    private SendStocksByPiToMbiConsumer sendStocksByPiToMbiConsumer;

    @BeforeEach
    public void init() {
        clearInvocations(mbiOpenApiClient);
    }

    @Test
    @DbUnitDataSet(before = "SendStrategyToNesuConsumerTest.testSuccess.before.csv")
    public void testSuccess() {
        sendStocksByPiToMbiConsumer.executeTask(
                createDbQueueTask(new SendStocksByPiToMbiPayload(1L, 100L))
        );

        BooleanParamValueDTO expected = new BooleanParamValueDTO();
        expected.setPartnerId(1L);
        expected.setValue(true);
        expected.setParamType("STOCKS_BY_PARTNER_INTERFACE");
        verify(mbiOpenApiClient).setBoolParam(eq(100L), eq(expected));
        verifyNoMoreInteractions(mbiOpenApiClient);
    }

    @Test
    @DbUnitDataSet(before = "SendStrategyToNesuConsumerTest.testNoPartner.before.csv")
    public void testNoPartner() {
        assertThatThrownBy(() -> sendStocksByPiToMbiConsumer.executeTask(
                createDbQueueTask(new SendStocksByPiToMbiPayload(2L, 100L))
        )).isInstanceOf(EntityNotFoundException.class);

        verify(mbiOpenApiClient, never()).setBoolParam(anyLong(), any());
    }

}
