package ru.yandex.market.mboc.tms.executors;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author yuramalinov
 * @created 28.11.18
 */
public class UpdateYtStampExecutorTest extends BaseDbTestClass {
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    private UpdateYtStampExecutor updateYtStampExecutor;

    @Before
    public void setUp() {
        updateYtStampExecutor = new UpdateYtStampExecutor(offerRepository, transactionHelper);
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
    }

    @Test
    public void testEmptyRunIsFine() {
        updateYtStampExecutor.execute();
    }

    @Test
    public void testSimpleMarkBlue() {
        Offer offerApprovedMapping = OfferTestUtils.simpleOffer(1L)
            .setCategoryIdInternal(111L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1), Offer.MappingConfidence.CONTENT);
        Offer offerMappedCategory = OfferTestUtils.simpleOffer(2L)
            .setCategoryIdInternal(111L)
            .setMappedCategoryId(111L);
        offerRepository.insertOffers(offerApprovedMapping, offerMappedCategory);

        updateYtStampExecutor.execute();

        List<Offer> updated =
            offerRepository.getOffersByIds(offerApprovedMapping.getId(), offerMappedCategory.getId());

        Assertions.assertThat(updated).allMatch(o -> o.getUploadToYtStamp() != null);
    }

    @Test
    public void testSimpleMarkWhite() {
        Offer offerApprovedMapping = OfferTestUtils.simpleOffer(1L)
            .setCategoryIdInternal(111L)
            .setMappingDestination(Offer.MappingDestination.WHITE)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1), Offer.MappingConfidence.CONTENT);
        Offer offerMappedModel = OfferTestUtils.simpleOffer(2L)
            .setCategoryIdInternal(111L)
            .setMappingDestination(Offer.MappingDestination.WHITE)
            .setMappedModelId(11L);
        Offer offerMappedCategory = OfferTestUtils.simpleOffer(3L)
            .setCategoryIdInternal(111L)
            .setMappingDestination(Offer.MappingDestination.WHITE)
            .setMappedCategoryId(111L);
        offerRepository.insertOffers(offerApprovedMapping, offerMappedModel, offerMappedCategory);

        updateYtStampExecutor.execute();

        List<Offer> updated =
            offerRepository.getOffersByIds(
                offerApprovedMapping.getId(),
                offerMappedModel.getId(),
                offerMappedCategory.getId());

        Assertions.assertThat(updated).allMatch(o -> o.getUploadToYtStamp() != null);
    }

    @Test
    public void testNoOverwrite() {
        Offer offer = OfferTestUtils.simpleOffer().setMappingDestination(Offer.MappingDestination.WHITE);
        offerRepository.insertOffer(offer);

        updateYtStampExecutor.execute();

        Offer updated = offerRepository.getOfferById(offer.getId());
        Long stamp = updated.getUploadToYtStamp();

        updateYtStampExecutor.execute();
        updated = offerRepository.getOfferById(offer.getId());

        Assertions.assertThat(updated.getUploadToYtStamp()).isEqualTo(stamp);
    }
}
