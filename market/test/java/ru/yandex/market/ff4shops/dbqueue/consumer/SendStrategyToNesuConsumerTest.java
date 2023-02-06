package ru.yandex.market.ff4shops.dbqueue.consumer;

import javax.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.dbqueue.dto.SendStrategyToNesuPayload;
import ru.yandex.market.logistics.nesu.client.NesuClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class SendStrategyToNesuConsumerTest extends FunctionalTest {
    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private SendStrategyToNesuConsumer sendStrategyToNesuConsumer;

    @Test
    @DbUnitDataSet(before = "SendStrategyToNesuConsumerTest.testSuccess.before.csv")
    public void testSuccess() {
        sendStrategyToNesuConsumer.executeTask(
                createDbQueueTask(new SendStrategyToNesuPayload(1L))
        );
        verify(nesuClient).setStockSyncStrategy(eq(100L), eq(1L), eq(true));
    }

    @Test
    @DbUnitDataSet(before = "SendStrategyToNesuConsumerTest.testNoPartner.before.csv")
    public void testNoPartner() {
        assertThatThrownBy(() -> sendStrategyToNesuConsumer.executeTask(
                createDbQueueTask(new SendStrategyToNesuPayload(2L))
        )).isInstanceOf(EntityNotFoundException.class);

        verify(nesuClient, never()).setStockSyncStrategy(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    @DbUnitDataSet(before = "SendStrategyToNesuConsumerTest.testNoWarehouse.before.csv")
    public void testNoWarehouse() {
        assertThatCode(() -> sendStrategyToNesuConsumer.executeTask(
                createDbQueueTask(new SendStrategyToNesuPayload(1L))
        )).doesNotThrowAnyException();

        verify(nesuClient, never()).setStockSyncStrategy(anyLong(), anyLong(), anyBoolean());
    }
}
