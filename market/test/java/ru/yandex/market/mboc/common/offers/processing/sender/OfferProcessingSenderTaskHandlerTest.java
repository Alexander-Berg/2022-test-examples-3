package ru.yandex.market.mboc.common.offers.processing.sender;

import java.util.Set;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.taskqueue.TaskRecord;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.offers.repository.queue.OfferManualProcessingQueueRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static java.util.Set.of;
import static org.assertj.core.api.Assertions.assertThat;

public class OfferProcessingSenderTaskHandlerTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private OfferRepositoryImpl offerRepository;
    @Autowired
    private OfferManualProcessingQueueRepository processingQueue;

    private OfferProcessingSenderTaskHandler taskHandler;

    @Before
    public void setUp() {
        taskHandler = new OfferProcessingSenderTaskHandler(jdbcTemplate, offerRepository);
    }

    @Test
    public void sendsToOfferProcessingQueue() {
        var business1 = supplierRepository.insert(OfferTestUtils.businessSupplier().setId(1));
        var business2 = supplierRepository.insert(OfferTestUtils.businessSupplier().setId(2));
        var offer111 = nextOffer(1, 1, business1);
        var offer112 = nextOffer(1, 1, business2);
        var offer121 = nextOffer(1, 2, business1);
        var offer122 = nextOffer(1, 2, business2);
        var offer211 = nextOffer(2, 1, business1);
        var offer212 = nextOffer(2, 1, business2);
        var offer221 = nextOffer(2, 2, business1);
        var offer222 = nextOffer(2, 2, business2);

        assertThat(processingQueue.find(100)).isEmpty();

        // Test category filter
        testCase(of(1L), of(), of(), of(offer111.getId(), offer112.getId(), offer121.getId(), offer122.getId()));
        // Test vendor filter
        testCase(of(), of(1L), of(), of(offer111.getId(), offer112.getId(), offer211.getId(), offer212.getId()));
        // Test business filter
        testCase(of(), of(), of(1L), of(offer111.getId(), offer121.getId(), offer211.getId(), offer221.getId()));
        // Test category + vendor
        testCase(of(1L), of(2L), of(), of(offer121.getId(), offer122.getId()));
        // Test vendor + business
        testCase(of(), of(1L), of(2L), of(offer112.getId(), offer212.getId()));
        // Test all
        testCase(of(2L), of(2L), of(2L), of(offer222.getId()));
    }

    private Offer nextOffer(long categoryId, long vendorId, Supplier business) {
        return offerRepository.insertAndGetOffer(OfferTestUtils.nextOffer(business)
            .setBusinessId(business.getId())
            .setCategoryIdInternal(categoryId)
            .setVendorId((int) vendorId));
    }

    @SneakyThrows
    private void testCase(Set<Long> categories, Set<Long> vendors, Set<Long> businesses, Set<Long> expected) {
        taskHandler.handle(new OfferProcessingSenderTask(new OfferProcessingSenderFilter(
            categories, vendors, businesses
        )), Mockito.mock(TaskRecord.class));
        var ids = processingQueue.find(Integer.MAX_VALUE);
        assertThat(ids).containsExactlyInAnyOrderElementsOf(expected);
        processingQueue.dequeue(ids);
    }
}
