package ru.yandex.market.mboc.tms.executors.orchestrator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.ContentProcessingFreezeStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationModelChangeSource;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationModelStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationModel;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.MigrationModelRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.junit.Assert.assertEquals;

public class ResendToAgUnfreezeOffersExecutorTest extends BaseDbTestClass {
    @Autowired
    private MigrationModelRepository migrationModelRepository;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private ContentProcessingQueueRepository contentProcessingQueueRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    SupplierRepository supplierRepository;

    private ResendToAgUnfreezeOffersExecutor executor;

    private Supplier supplier;

    @Before
    public void setUp() throws Exception {
        executor = new ResendToAgUnfreezeOffersExecutor(
            migrationModelRepository,
            mskuRepository,
            offerRepository,
            contentProcessingQueueRepository,
            storageKeyValueService,
            transactionHelper
        );
        supplier = supplierRepository.insert(OfferTestUtils.simpleSupplier());
    }

    @Test
    public void shouldProcessAndUpdateMigration() {
        var migrationModel = createMigrationModel(
            1L,
            ContentProcessingFreezeStatus.ALLOWED,
            MigrationModelStatus.IN_PROCESS
        );
        final var save = migrationModelRepository.save(migrationModel);

        var msku1 = createMsku(migrationModel.getModelId());
        var msku2 = createMsku(migrationModel.getModelId());
        var mskus = mskuRepository.save(msku1, msku2);

        var offers = createOffersWithSkus(mskus, 10).values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);

        executor.execute();

        var queueItems = contentProcessingQueueRepository.findAll();
        var updatedModels = migrationModelRepository.findByModelIds(List.of(migrationModel.getModelId()));
        assertEquals(offers.size(), queueItems.size());
        assertEquals(updatedModels.size(), 1);
        assertEquals(updatedModels.get(0).getContentProcessingFreezeStatus(), ContentProcessingFreezeStatus.PROCESSED);
        assertEquals(updatedModels.get(0).getStatus(), MigrationModelStatus.IN_PROCESS);
    }

    private Msku createMsku(Long modelId) {
        return new Msku()
            .setDeleted(false)
            .setVendorId(10L)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now())
            .setCategoryId(100L)
            .setParentModelId(modelId)
            .setSupplierId(1000L);
    }

    private MigrationModel createMigrationModel(Long modelId,
                                                ContentProcessingFreezeStatus freezeStatus,
                                                MigrationModelStatus status) {
        return new MigrationModel()
            .setModelId(modelId)
            .setMigrationId(modelId)
            .setChangeSource(MigrationModelChangeSource.ORCHESTRATOR)
            .setStatus(status)
            .setContentProcessingFreezeStatus(freezeStatus);
    }

    private Map<Long, List<Offer>> createOffersWithSkus(Collection<Msku> mskus, int count) {
        var random = new Random();
        return mskus.stream()
            .flatMap(msku -> Stream.iterate(1, i -> i + 1).limit(count)
                .map(i -> OfferTestUtils.simpleOffer(supplier)
                    .setId(random.nextLong())
                    .setCategoryIdInternal(1L)
                    .setShopSku("shopSku-" + i + "-" + msku.getMarketSkuId())
                    .setModelId(msku.getParentModelId())
                    .setApprovedSkuMappingConfidence(Offer.MappingConfidence.AUTO_APPROVE)
                    .setApprovedSkuMappingInternal(new Offer.Mapping(msku.getMarketSkuId(), LocalDateTime.now()))
                )
            )
            .collect(Collectors.groupingBy(Offer::getApprovedSkuId));
    }
}
