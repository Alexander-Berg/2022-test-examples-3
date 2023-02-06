package ru.yandex.market.mboc.tms.executors.offer;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.nextOffer;

public class OfferWalkingExecutorTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;

    private OfferWalkingExecutor executor;

    @Before
    public void setUp() {
        storageKeyValueService.invalidateCache();
        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        executor = executor(1, 1, 1, 0, (fromId, toId) -> {
            var toRemove = offerRepository.findOffers(new OffersFilter()
                .setMinIdInclusive(fromId).setMaxIdInclusive(toId));
            offerRepository.removeOffers(toRemove);
            return toRemove.size();
        });
    }

    @Test
    public void testBatchesPerExecution() {
        offerRepository.insertOffers(nextOffer(), nextOffer(), nextOffer());
        executor.execute();
        assertThat(offerRepository.findAll()).hasSize(2);

        storageKeyValueService.invalidateCache();

        storageKeyValueService.putValue(executor.batchesPerExecutionKey, 100);
        executor.execute();
        assertThat(offerRepository.findAll()).isEmpty();
    }

    @Test
    public void testBatchSize() {
        offerRepository.insertOffers(nextOffer(), nextOffer(), nextOffer());
        executor.execute();
        executor.execute();
        assertThat(offerRepository.findAll()).hasSize(1);

        storageKeyValueService.invalidateCache();

        offerRepository.insertOffers(nextOffer(), nextOffer());
        storageKeyValueService.putValue(executor.batchSizeKey, 2);
        executor.execute();
        assertThat(offerRepository.findAll()).hasSize(1);
        executor.execute();
        assertThat(offerRepository.findAll()).isEmpty();
    }

    @Test
    public void testStopsWhenMaxIdReached() {
        offerRepository.insertOffers(nextOffer(), nextOffer(), nextOffer(), nextOffer(), nextOffer());
        var range = offerRepository.findOfferIdRange();
        var spied = spy(executor(100, 1, 1, 0, (fromId, toId) -> {
            assertThat(fromId).isGreaterThanOrEqualTo(range.getMin());
            assertThat(toId).isLessThanOrEqualTo(range.getMax() + 1);
            return 0;
        }));
        spied.execute();
        verify(spied, times(5)).processOffers(anyLong(), anyLong());
    }

    @Test
    public void testRetryCount() {
        offerRepository.insertOffers(nextOffer());

        var noRetryExecutor = spy(executorWithRetryCount(1, (fromId, toId) -> 0));
        doThrow(new RuntimeException()).when(noRetryExecutor).processOffers(anyLong(), anyLong());
        assertThatThrownBy(noRetryExecutor::execute).isInstanceOf(RuntimeException.class);
        verify(noRetryExecutor, times(1)).processOffers(anyLong(), anyLong());

        var retryExecutor = spy(executorWithRetryCount(99, (fromId, toId) -> 0));
        doThrow(new RuntimeException()).when(retryExecutor).processOffers(anyLong(), anyLong());
        assertThatThrownBy(retryExecutor::execute).isInstanceOf(RuntimeException.class);
        verify(retryExecutor, times(99)).processOffers(anyLong(), anyLong());
    }

    private OfferWalkingExecutor executorWithRetryCount(int retryCount, BiFunction<Long, Long, Integer> action) {
        return executor(1, 1, retryCount, 0, action);
    }

    private OfferWalkingExecutor executor(int batchesPerExec, int batchSize, int retryCount, long retryBaseDelay,
                                          BiFunction<Long, Long, Integer> action) {
        return new OfferWalkingExecutor(offerRepository, TransactionHelper.MOCK, storageKeyValueService, "test",
            batchesPerExec, batchSize, retryCount, retryBaseDelay) {
            @Override
            protected int processOffers(long fromId, long toId) {
                return action.apply(fromId, toId);
            }
        };
    }
}
