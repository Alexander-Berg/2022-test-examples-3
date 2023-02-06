package ru.yandex.market.delivery.rupostintegrationapp.service.component.getreferencepickuppoints.updater.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.PickuppointRepository;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.RussianPostPickupPoint;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatingConsumerTest extends BaseTest {

    @InjectMocks
    protected UpdatingConsumer consumer;

    @Mock
    protected PickuppointRepository repository;

    @Test
    void testConsumeNeverFlushed() {
        verify(repository, never())
            .updatePickupPoints(anyListOf(RussianPostPickupPoint.class));
        consumer.consume(new RussianPostPickupPoint());

    }

    @Test
    void testConsumeFlushedOnce() {
        int batchSize = (new TestConsumer(repository)).getBatchSize();
        when(repository.updatePickupPoints(anyListOf(RussianPostPickupPoint.class)))
            .thenReturn(null);

        for (int i = 0; i <= batchSize; i++) {
            consumer.consume(new RussianPostPickupPoint());
        }

        verify(repository, only())
            .updatePickupPoints(anyListOf(RussianPostPickupPoint.class));
    }

    @Test
    void testConsumeFlushedTwice() {
        int batchSize = (new TestConsumer(repository)).getBatchSize() * 2;
        when(repository.updatePickupPoints(anyListOf(RussianPostPickupPoint.class)))
            .thenReturn(null);

        for (int i = 0; i <= batchSize; i++) {
            consumer.consume(new RussianPostPickupPoint());
        }

        verify(repository, times(2))
            .updatePickupPoints(anyListOf(RussianPostPickupPoint.class));
    }

    @Test
    void testFlushFail() {
        consumer.flush();
        verify(repository, never())
            .updatePickupPoints(anyListOf(RussianPostPickupPoint.class));
    }

    @Test
    void testDeactivateNotUpdatedPickupPoints() {
        when(repository.deactivateAllNotUpdatedPickupPoints()).thenReturn(1);
        consumer.deactivateNotUpdatedPickupPoints();
        verify(repository, only())
            .deactivateAllNotUpdatedPickupPoints();
    }

    @Test
    void testReset() {
        int batchSize = (new TestConsumer(repository)).getBatchSize();

        for (int i = 0; i <= batchSize; i++) {
            consumer.consume(new RussianPostPickupPoint());
            consumer.reset();
        }

        verify(repository, never())
            .updatePickupPoints(anyListOf(RussianPostPickupPoint.class));
    }

    @Test
    void testGetCounter() {
        int batchSize = (new TestConsumer(repository)).getBatchSize();
        when(repository.updatePickupPoints(anyListOf(RussianPostPickupPoint.class)))
            .thenReturn(null);

        softly.assertThat(consumer.getCounter()).isEqualTo(0);
        // сначала полный батч
        for (int i = 0; i <= batchSize; i++) {
            consumer.consume(new RussianPostPickupPoint());
        }

        // неполный батч
        for (int i = 0; i <= batchSize - 10; i++) {
            consumer.consume(new RussianPostPickupPoint());
        }

        softly.assertThat(consumer.getCounter()).isEqualTo(batchSize);
    }

    protected static class TestConsumer extends UpdatingConsumer {
        TestConsumer(PickuppointRepository pickuppointRepository) {
            super(pickuppointRepository);
        }

        int getBatchSize() {
            return BATCH_SIZE;
        }
    }
}
