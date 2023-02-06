package ru.yandex.market.mboc.common.offers.repository.queue;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferManualProcessingQueueRepositoryTest extends BaseDbTestClass {

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private OfferManualProcessingQueueRepository queue;

    @Autowired
    private NamedParameterJdbcTemplate template;

    @Before
    public void setup() {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);
        var category = OfferTestUtils.defaultCategory();
        var offers = IntStream.range(0, 100)
            .mapToObj(i -> generateOfferWithId(supplier, category, i))
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);
    }

    private Offer generateOfferWithId(Supplier supplier, Category category, int id) {
        return OfferTestUtils.simpleOffer(supplier)
            .setShopSku("ssku-" + id)
            .setCategoryIdInternal(category.getCategoryId());
    }

    @Test
    public void enqueue() {
        var ids = offerRepository.findOfferIds(new OffersFilter());
        queue.enqueue(ids);

        template.query("select count(*) as total, count(distinct offer_id) as offers " +
            "from mbo_category.offer_queue_processing " +
            "where processed_ts is null", rs -> {
            var total = rs.getInt("total");
            var offers = rs.getInt("offers");
            assertThat(total).isEqualTo(offers);
            assertThat(offers).isEqualTo(ids.size());
        });

        queue.enqueue(ids);

        template.query("select count(*) as total, count(distinct offer_id) as offers " +
            "from mbo_category.offer_queue_processing " +
            "where processed_ts is null", rs -> {
            var total = rs.getInt("total");
            var offers = rs.getInt("offers");
            assertThat(total).isEqualTo(offers);
            assertThat(offers).isEqualTo(ids.size());
        });
    }

    @Test
    public void reEnqueue() {
        var ids = offerRepository.findOfferIds(new OffersFilter());
        queue.enqueue(ids);

        template.query("select count(*) as total, count(distinct offer_id) as offers " +
            "from mbo_category.offer_queue_processing " +
            "where processed_ts is null", rs -> {
            var total = rs.getInt("total");
            var offers = rs.getInt("offers");
            assertThat(total).isEqualTo(offers);
            assertThat(offers).isEqualTo(ids.size());
        });

        queue.enqueue(ids);

        var toReEnqueue = ids.stream().limit(5).collect(Collectors.toList());
        queue.dequeue(toReEnqueue);

        template.query("select count(*) as total, count(distinct offer_id) as offers " +
            "from mbo_category.offer_queue_processing " +
            "where processed_ts is null", rs -> {
            var total = rs.getInt("total");
            var offers = rs.getInt("offers");
            assertThat(total).isEqualTo(offers);
            assertThat(offers).isEqualTo(ids.size() - toReEnqueue.size());
        });

        queue.enqueue(toReEnqueue);
        template.query("select count(*) as total, count(distinct offer_id) as offers " +
            "from mbo_category.offer_queue_processing " +
            "where processed_ts is null", rs -> {
            var total = rs.getInt("total");
            var offers = rs.getInt("offers");
            assertThat(total).isEqualTo(offers);
            assertThat(offers).isEqualTo(ids.size());
        });

        template.query("select count(*) as total from mbo_category.offer_queue_processing", rs -> {
            var total = rs.getInt("total");
            assertThat(total).isEqualTo(ids.size() + toReEnqueue.size());
        });
    }

    @Test
    public void find() {
        var ids = offerRepository.findOfferIds(new OffersFilter());
        queue.enqueue(ids);

        var foundIds = queue.find(1000);
        assertThat(foundIds).hasSameElementsAs(ids);

        var toDequeue = ids.stream().limit(10).collect(Collectors.toList());
        queue.dequeue(toDequeue);

        var foundIdsAfterDequeue = queue.find(1000);
        assertThat(foundIdsAfterDequeue).hasSize(ids.size() - toDequeue.size());
        assertThat(foundIdsAfterDequeue).doesNotContainAnyElementsOf(toDequeue);
    }

    @Test
    public void hasOffersToProcess() {
        var ids = offerRepository.findOfferIds(new OffersFilter());
        queue.enqueue(ids);

        assertThat(queue.hasOffersToProcess()).isTrue();
        var foundIds = queue.find(1000);

        assertThat(foundIds).hasSameElementsAs(ids);
        queue.dequeue(ids);
        assertThat(queue.hasOffersToProcess()).isFalse();
    }

    @Test
    public void removeOldCompleted() {
        var ids = offerRepository.findOfferIds(new OffersFilter());
        queue.enqueue(ids);
        queue.dequeue(ids);

        int updated = template.update("update mbo_category.offer_queue_processing " +
            "set processed_ts = now() - interval '2 month' where processed_ts is not null", Map.of());

        assertThat(updated).isEqualTo(ids.size());
        assertThat(queue.hasOffersToProcess()).isFalse();

        queue.removeOldCompleted();

        template.query("select count(*) as total from mbo_category.offer_queue_processing", rs -> {
            var total = rs.getInt("total");
            assertThat(total).isEqualTo(0);
        });
    }
}
