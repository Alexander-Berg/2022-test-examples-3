package ru.yandex.market.mboc.common.offers.repository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.pgupdateseq.PgUpdateSeqRow;
import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferUpdateSequenceServiceTest extends BaseDbTestClass {

    private OfferUpdateSequenceService offerUpdateSequenceService;

    @Autowired
    OfferRepository offerRepository;
    @Autowired
    SupplierRepository supplierRepository;

    @Before
    public void setup() {
        offerUpdateSequenceService = new OfferUpdateSequenceService(
            jdbcTemplate, storageKeyValueService, Mockito.mock(SolomonPushService.class));
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
    }

    @Test
    public void testItCopiesData() {
        List<Offer> offers = IntStream.range(0, 1000)
            .mapToObj(i -> OfferTestUtils.nextOffer())
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);

        long stagingCount = offerUpdateSequenceService.getStagingCount();
        assertThat(stagingCount).isEqualTo(1000);

        offerUpdateSequenceService.copyOfferChangesFromStaging();

        stagingCount = offerUpdateSequenceService.getStagingCount();
        assertThat(stagingCount).isEqualTo(0);

        List<PgUpdateSeqRow<Long>> batch = offerUpdateSequenceService.getModifiedRecordsIdBatch(0,
            10000);
        assertThat(batch).hasSize(1000);
    }

    @Test
    public void testOffersModified() {
        var modifiedOfferIds = LongStream.range(0, 500).boxed().collect(Collectors.toList());
        offerUpdateSequenceService.markOffersModified(modifiedOfferIds);

        long stagingCount = offerUpdateSequenceService.getStagingCount();

        assertThat(stagingCount).isEqualTo(modifiedOfferIds.size());
    }
}
