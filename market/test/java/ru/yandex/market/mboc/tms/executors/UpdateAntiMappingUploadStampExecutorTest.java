package ru.yandex.market.mboc.tms.executors;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateAntiMappingUploadStampExecutorTest extends BaseDbTestClass {

    private static final long OFFER_ID = 12345L;

    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    private UpdateAntiMappingUploadStampExecutor updateAntiMappingUploadStampExecutor;

    @Before
    public void setUp() {
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        Offer offer = OfferTestUtils.nextOffer().setId(OFFER_ID);
        offerRepository.insertOffer(offer);

        updateAntiMappingUploadStampExecutor = new UpdateAntiMappingUploadStampExecutor(
            antiMappingRepository,
            transactionHelper
        );
    }

    @Test
    public void testStampIsUpdated() {
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

        long antiMappingId = antiMappingRepository.insert(antiMapping).getId();

        updateAntiMappingUploadStampExecutor.execute();

        antiMapping = antiMappingRepository.findById(antiMappingId);
        assertThat(antiMapping.getUploadStamp())
            .isNotNull()
            .isPositive();

        Long origUploadStamp = antiMapping.getUploadStamp();
        antiMapping.markNeedsUpload();
        antiMappingRepository.update(antiMapping);

        updateAntiMappingUploadStampExecutor.execute();

        antiMapping = antiMappingRepository.findById(antiMappingId);
        assertThat(antiMapping.getUploadStamp())
            .isNotNull()
            .isPositive()
            .isGreaterThan(origUploadStamp);
    }
}
