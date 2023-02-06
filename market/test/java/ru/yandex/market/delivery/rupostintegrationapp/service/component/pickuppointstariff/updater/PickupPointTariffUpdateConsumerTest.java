package ru.yandex.market.delivery.rupostintegrationapp.service.component.pickuppointstariff.updater;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PickupPointTariffUpdateConsumerTest extends BaseTest {
    @Mock
    private PickuppointRepository repository;
    @InjectMocks
    private PickupPointTariffUpdateConsumer consumer;

    @Test
    void testConsumeNeverFlushed() {
        verify(repository, never()).updatePickupPointsTariffData(anyListOf(RussianPostPickupPoint.class));
        consumer.consume(new RussianPostPickupPoint());

    }

    @Test
    void testConsumeFlushed() {
        int batchSize = (new TestConsumer(repository)).getBatchSize();

        for (int i = 0; i <= batchSize; i++) {
            consumer.consume(new RussianPostPickupPoint());
        }

        verify(repository, times(1))
            .updatePickupPointsTariffData(anyListOf(RussianPostPickupPoint.class));
    }

    protected class TestConsumer extends PickupPointTariffUpdateConsumer {
        TestConsumer(PickuppointRepository pickuppointRepository) {
            super(pickuppointRepository);
        }

        int getBatchSize() {
            return BATCH_SIZE;
        }
    }
}
