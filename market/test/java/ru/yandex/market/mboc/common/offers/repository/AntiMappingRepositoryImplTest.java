package ru.yandex.market.mboc.common.offers.repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.lightmapper.exceptions.SqlConcurrentModificationException;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository.newFilter;

public class AntiMappingRepositoryImplTest extends BaseDbTestClass {

    private static final long OFFER_ID = 12345L;

    @Autowired
    private AntiMappingRepositoryImpl antiMappingRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    @Before
    public void setUp() {
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        Offer offer = OfferTestUtils.nextOffer().setId(OFFER_ID);
        offerRepository.insertOffer(offer);
    }

    @Test
    public void testVersion() {
        AntiMapping antiMapping = new AntiMapping()
            .setOfferId(OFFER_ID)
            .setNotModelId(OfferTestUtils.TEST_MODEL_ID)
            .setNotSkuId(OfferTestUtils.TEST_SKU_ID)
            .setSourceType(AntiMapping.SourceType.MODERATION_REJECT)
            .setCreatedTs(Instant.now().minus(3, ChronoUnit.DAYS))
            .setUpdatedTs(Instant.now().minus(2, ChronoUnit.DAYS))
            .setUpdatedUser("test user updated")
            .setDeletedUser("test user deleted")
            .setDeletedTs(Instant.now().minus(1, ChronoUnit.DAYS))
            .markNeedsUpload();

        antiMappingRepository.insert(antiMapping);

        List<AntiMapping> inRepo = antiMappingRepository.findByFilter(newFilter()
            .setSkuMappingKeys(antiMapping.getOfferSkuMappingKey()));

        assertThat(inRepo).hasSize(1);
        AntiMapping antiMappingInRepoV1 = inRepo.get(0);

        // version is incremented
        AntiMapping antiMappingInRepoV2 = antiMappingRepository.update(antiMappingInRepoV1);

        assertThat(antiMappingInRepoV2.getVersion())
            .isGreaterThan(antiMappingInRepoV1.getVersion());

        Assertions.assertThatThrownBy(() -> antiMappingRepository.update(antiMappingInRepoV1))
            .isInstanceOf(SqlConcurrentModificationException.class);
    }

    @Test
    public void testInsertAndSelectResultsAreEqual() {
        AntiMapping antiMapping = new AntiMapping()
            .setOfferId(OFFER_ID)
            .setNotModelId(OfferTestUtils.TEST_MODEL_ID)
            .setNotSkuId(OfferTestUtils.TEST_SKU_ID)
            .setSourceType(AntiMapping.SourceType.MODERATION_REJECT)
            .setCreatedTs(Instant.now().minus(3, ChronoUnit.DAYS))
            .setUpdatedTs(Instant.now().minus(2, ChronoUnit.DAYS))
            .setUpdatedUser("test user updated")
            .setDeletedUser("test user deleted")
            .setDeletedTs(Instant.now().minus(1, ChronoUnit.DAYS))
            .markNeedsUpload();

        antiMapping = antiMappingRepository.insert(antiMapping);

        assertThat(antiMapping.getId())
            .isNotNull()
            .isPositive();

        AntiMapping foundById = antiMappingRepository.findById(antiMapping.getId());
        assertThat(foundById)
            .isEqualToIgnoringGivenFields(antiMapping, "needsUpload");

        AntiMappingRepository.Filter filter = newFilter()
            .setOfferIds(OFFER_ID);

        List<AntiMapping> foundByFilter = antiMappingRepository.findByFilter(filter);
        assertThat(foundByFilter)
            .usingElementComparatorIgnoringFields("needsUpload")
            .containsExactly(antiMapping);

        List<AntiMapping> foundByFilterIter = new ArrayList<>();
        antiMappingRepository.findByFilterIter(filter,
            i -> i.forEachRemaining(foundByFilterIter::add));
        assertThat(foundByFilterIter)
            .usingElementComparatorIgnoringFields("needsUpload")
            .containsExactly(antiMapping);
    }
}
