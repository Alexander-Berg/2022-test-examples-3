package ru.yandex.market.mboc.tms.executors.offer;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.nextOffer;

public class CycledOfferWalkingExecutorTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;

    private CycledOfferWalkingExecutor executor;

    @Before
    public void setUp() {
        storageKeyValueService.invalidateCache();
        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        executor = executor(1, 1, 1000, 1, 0, (fromId, toId) -> {
            var toRemove = offerRepository.findOffers(new OffersFilter()
                .setMinIdInclusive(fromId).setMaxIdInclusive(toId));
            offerRepository.removeOffers(toRemove);
            return toRemove.size();
        });
    }

    @Test
    public void startsNewCycleIfNoCycle() {
        offerRepository.insertOffers(nextOffer(), nextOffer());
        assertThat(storageKeyValueService.getOffsetDateTime(executor.cycleStartedKey, null)).isNull();
        executor.execute();
        executor.execute();
        assertThat(offerRepository.findAll()).isEmpty();
        assertThat(storageKeyValueService.getOffsetDateTime(executor.cycleStartedKey, null)).isNotNull()
            .isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS));
    }

    @Test
    public void wontStartNewCycleBecauseOfDelay() {
        offerRepository.insertOffers(nextOffer());
        executor.execute();
        executor.execute();
        assertThat(offerRepository.findAll()).isEmpty();

        offerRepository.insertOffers(nextOffer());
        executor.execute();
        assertThat(offerRepository.findAll()).hasSize(1);

        storageKeyValueService.putOffsetDateTime(executor.cycleStartedKey, OffsetDateTime.now().minusSeconds(2000));
        executor.execute();
        assertThat(offerRepository.findAll()).isEmpty();
    }

    @Test
    public void wontProcessNewlyAddedOffersIfCycleIsFinished() {
        offerRepository.insertOffers(nextOffer());
        storageKeyValueService.putValue(executor.startingFromIdKey, Long.MAX_VALUE);
        storageKeyValueService.putValue(executor.cycleDelayKey, 4000);

        storageKeyValueService.putOffsetDateTime(executor.cycleStartedKey, OffsetDateTime.now().minusSeconds(3990));
        var spied = spy(executor);
        // Should not do anything
        spied.execute();
        assertThat(offerRepository.findAll()).hasSize(1);
        verify(spied, times(0)).processOffers(anyLong(), anyLong());

        storageKeyValueService.putOffsetDateTime(executor.cycleStartedKey, OffsetDateTime.now().minusSeconds(4000));
        // Should update
        spied.execute();
        assertThat(offerRepository.findAll()).isEmpty();
        verify(spied, times(1)).processOffers(anyLong(), anyLong());
    }

    private CycledOfferWalkingExecutor executor(int batchesPerExec, int batchSize, long cycleDelaySec, int retryCount,
                                                long retryBaseDelay, BiFunction<Long, Long, Integer> action) {
        return new CycledOfferWalkingExecutor(offerRepository, TransactionHelper.MOCK, storageKeyValueService, "test",
            batchesPerExec, batchSize, cycleDelaySec, retryCount, retryBaseDelay) {
            @Override
            protected int processOffers(long fromId, long toId) {
                return action.apply(fromId, toId);
            }
        };
    }
}
