package ru.yandex.market.mboc.tms.service.orchestrator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationModelChangeSource;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationModelStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationModel;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.MigrationModelRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OrchestratorServiceImplTest extends BaseDbTestClass {
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private MigrationModelRepository migrationModelRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    private CategoryOrchestratorApiMock categoryOrchestratorApi;
    private ModelStorageCachingServiceMock modelStorageCachingService;

    private OrchestratorService orchestratorService;

    private Model model1;
    private Model model2;

    @Before
    public void setUp() throws Exception {
        categoryOrchestratorApi = new CategoryOrchestratorApiMock();
        modelStorageCachingService = new ModelStorageCachingServiceMock();
        storageKeyValueService.putValue(OrchestratorServiceImpl.ENABLE_CALL_ORCHESTRATOR_KEY, true);
        storageKeyValueService.invalidateCache();
        orchestratorService = new OrchestratorServiceImpl(
            categoryOrchestratorApi,
            offerRepository,
            modelStorageCachingService,
            storageKeyValueService,
            migrationModelRepository,
            transactionHelper
        );

        model1 = new Model()
            .setId(1L)
            .setCategoryId(1L)
            .setSkuParentModelId(3);
        model2 = new Model()
            .setId(2L)
            .setCategoryId(2L)
            .setSkuParentModelId(4);
        modelStorageCachingService.addModel(model1);
        modelStorageCachingService.addModel(model2);
    }

    @Test
    public void migrateOfferSkuCategoryIfInvalidOk() {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);

        var offerNeedsMigration = OfferTestUtils.simpleOffer(supplier);
        offerNeedsMigration.setCategoryIdInternal(model1.getCategoryId());
        offerNeedsMigration.setMappedCategoryId(model1.getCategoryId());
        offerNeedsMigration.setApprovedSkuMappingInternal(new Offer.Mapping(model2.getId(), LocalDateTime.now()));
        offerNeedsMigration.setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        offerNeedsMigration.setMappedModelId(model1.getCategoryId());
        offerNeedsMigration.setMappedCategoryConfidence(Offer.MappingConfidence.CONTENT);
        offerNeedsMigration.setRecheckClassificationStatus(Offer.RecheckClassificationStatus.CONFIRMED);

        var offerNoNeedMigration = OfferTestUtils.simpleOffer(supplier);
        offerNoNeedMigration.setId(2L);
        offerNoNeedMigration.setShopSku("shopSku2");
        offerNoNeedMigration.setCategoryIdInternal(model1.getCategoryId());
        offerNoNeedMigration.setMappedCategoryId(model1.getCategoryId());
        offerNoNeedMigration.setApprovedSkuMappingInternal(new Offer.Mapping(model1.getId(), LocalDateTime.now()));
        offerNoNeedMigration.setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        offerNoNeedMigration.setMappedModelId(model1.getCategoryId());
        offerNoNeedMigration.setMappedCategoryConfidence(Offer.MappingConfidence.CONTENT);
        offerRepository.insertOffers(offerNeedsMigration, offerNoNeedMigration);

        orchestratorService.migrateOfferSkuCategoryIfInvalid(
            List.of(offerNeedsMigration.getId(), offerNoNeedMigration.getId())
        );

        var requestDtos = categoryOrchestratorApi.getRequestDtos();
        assertEquals(requestDtos.size(), 1);
        assertEquals(requestDtos.get(0).getRequestItems().get(0).getModelIds().get(0),
            offerNeedsMigration.getApprovedSkuId());
        assertEquals(requestDtos.get(0).getRequestItems().get(0).getTargetCategoryId(),
            offerNeedsMigration.getMappedCategoryId());
    }

    @Test
    public void migrateOfferSkuCategoryIfInvalid_shouldSendOnlyRecheckClassifications() {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);

        var brokenOffer = OfferTestUtils.simpleOffer(supplier);
        brokenOffer.setId(1000);
        brokenOffer.setCategoryIdInternal(model1.getCategoryId());
        brokenOffer.setMappedCategoryId(model1.getCategoryId());
        brokenOffer.setApprovedSkuMappingInternal(new Offer.Mapping(model2.getId(), LocalDateTime.now()));
        brokenOffer.setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        brokenOffer.setMappedModelId(model1.getCategoryId());
        brokenOffer.setMappedCategoryConfidence(Offer.MappingConfidence.CONTENT);

        var recheckedOffer = brokenOffer.copy()
            .setId(2000)
            .setShopSku("rechecked-shopsku")
            .setMappedCategoryId(model2.getCategoryId())
            .setApprovedSkuMappingInternal(new Offer.Mapping(model1.getId(), LocalDateTime.now()))
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.CONFIRMED);

        offerRepository.insertOffers(recheckedOffer, brokenOffer);

        orchestratorService.migrateOfferSkuCategoryIfInvalid(
            List.of(recheckedOffer.getId(), brokenOffer.getId())
        );

        var requestDtos = categoryOrchestratorApi.getRequestDtos();
        assertEquals(requestDtos.size(), 1);
        assertEquals(requestDtos.get(0).getRequestItems().get(0).getModelIds().get(0),
            recheckedOffer.getApprovedSkuId());
        assertEquals(requestDtos.get(0).getRequestItems().get(0).getTargetCategoryId(),
            recheckedOffer.getMappedCategoryId());

        var frozenModels = migrationModelRepository.findAll();
        var migrationModels = frozenModels.stream()
            .collect(Collectors.toMap(MigrationModel::getSourceOfferId, Function.identity()));

        var recheckedMigrationModel = migrationModels.get(recheckedOffer.getId());
        var brokenMigrationModel = migrationModels.get(brokenOffer.getId());

        assertNotNull(recheckedMigrationModel);
        assertNotNull(brokenMigrationModel);

        assertEquals(recheckedMigrationModel.getModelId().longValue(), model1.getSkuParentModelId());
        assertEquals(recheckedMigrationModel.getChangeSource(), MigrationModelChangeSource.CLASSIFICATION);
        assertEquals(recheckedMigrationModel.getStatus(), MigrationModelStatus.IN_PROCESS);

        assertEquals(brokenMigrationModel.getModelId().longValue(), model2.getSkuParentModelId());
        assertEquals(brokenMigrationModel.getChangeSource(), MigrationModelChangeSource.UNDEFINED);
        assertEquals(brokenMigrationModel.getStatus(), MigrationModelStatus.IN_PROCESS);
    }
}
