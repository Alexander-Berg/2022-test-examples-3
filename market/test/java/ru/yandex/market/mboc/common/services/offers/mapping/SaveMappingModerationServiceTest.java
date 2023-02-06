package ru.yandex.market.mboc.common.services.offers.mapping;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ir.http.PskuPostProcessorService;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.OfferSkuMappingKey;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferMappingHistoryRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.services.migration.MigrationServiceTestUtils;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingService;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.antimapping.AntiMappingService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mboc.http.SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository.newFilter;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class SaveMappingModerationServiceTest extends BaseDbTestClass {
    private static final int MOCK_MARKET_SKU_ID = 12345;
    private static final long MAPPING_ID_1 = 15L;
    private static final long MAPPING_ID_2 = 20L;
    private static final long MAPPING_ID_3 = 22L;
    private static final long INVALID_MAPPING_ID_1 = 25L;
    private static final long INVALID_MAPPING_ID_2 = 30L;
    private static final long DELETED_MODEL_MAPPING_ID = 35L;
    private static final long CATEGORY_ID1 = 1L;
    private static final long CATEGORY_ID2 = 2L;

    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private TransactionTemplate masterTransactionTemplate;
    @Autowired
    private DataSource slaveDataSource;
    @Autowired
    private DataSource masterDataSource;

    @Autowired
    private OfferRepository offerRepository;
    private OfferRepository repository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    @Autowired
    private OfferMappingHistoryRepository offerMappingHistoryRepository;

    private CategoryInfoRepositoryMock categoryInfoRepository;
    private AntiMappingService antiMappingService;
    private SaveMappingModerationService service;
    private CategoryCachingServiceMock categoryCachingService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;
    private PskuHasContentMappingsService pskuHasContentMappingsServiceMock;
    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;
    private MigrationService migrationService;
    private OffersProcessingStatusService offersProcessingStatusService;
    private OfferMappingRollbackService offerMappingRollbackService;
    private PskuPostProcessorService pskuPostProcessorService;

    @Before
    public void setup() {
        storageKeyValueService.putValue(SaveMappingModerationService.ROLLBACK_ENABLED_FLAG, true);

        repository = Mockito.spy(offerRepository);

        List<Supplier> suppliers = IntStream.of(
                42, 43, 44, 45, 50, 99, 100
            )
            .mapToObj(id -> OfferTestUtils.simpleSupplier().setId(id))
            .collect(Collectors.toList());

        supplierRepository.insertBatch(suppliers);

        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/sample-offers.json");
        offers.stream().map(Offer::getServiceOffers).flatMap(Collection::stream)
            .forEach(offer -> offer.setServiceAcceptance(Offer.AcceptanceStatus.OK));
        offers.forEach(offer -> offer.updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK));

        repository.insertOffers(offers);

        modelStorageCachingServiceMock = new ModelStorageCachingServiceMock()
            .addModel(new Model()
                .setId(MOCK_MARKET_SKU_ID).setTitle("Test title")
                .setCategoryId(CATEGORY_ID1)
                .setModelType(Model.ModelType.SKU).setPublishedOnBlueMarket(true))
            .addModel(new Model()
                .setId(MAPPING_ID_1).setTitle("Published on blue sku")
                .setCategoryId(CATEGORY_ID1)
                .setModelType(Model.ModelType.SKU).setPublishedOnBlueMarket(true))
            .addModel(new Model()
                .setId(MAPPING_ID_2).setTitle("Published on blue model as sku")
                .setCategoryId(CATEGORY_ID1)
                .setModelType(Model.ModelType.GURU).setSkuModel(true).setPublishedOnBlueMarket(true))
            .addModel(new Model()
                .setId(MAPPING_ID_3).setTitle("Published on blue model as sku")
                .setCategoryId(CATEGORY_ID1)
                .setModelType(Model.ModelType.GURU).setSkuModel(true).setPublishedOnBlueMarket(true))
            .addModel(new Model()
                .setId(INVALID_MAPPING_ID_1).setTitle("Published on blue guru")
                .setCategoryId(CATEGORY_ID1)
                .setModelType(Model.ModelType.GURU).setPublishedOnBlueMarket(true))
            .addModel(new Model()
                .setId(INVALID_MAPPING_ID_2).setTitle("Not published model (with IsSku == true)")
                .setCategoryId(CATEGORY_ID1)
                .setModelType(Model.ModelType.GURU).setSkuModel(true))
            .addModel(new Model()
                .setId(DELETED_MODEL_MAPPING_ID).setTitle("Deleted, published on blue sku").setDeleted(true)
                .setCategoryId(CATEGORY_ID1)
                .setModelType(Model.ModelType.SKU).setPublishedOnBlueMarket(true));

        var supplierService = new SupplierService(supplierRepository);

        categoryCachingService = new CategoryCachingServiceMock();
        categoryCachingService.addCategory(1);
        categoryCachingService.setCategoryHasKnowledge(1, true);

        categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        categoryKnowledgeService.addCategory(1);

        var mboUsersRepository = new MboUsersRepositoryMock();
        categoryInfoRepository = new CategoryInfoRepositoryMock(mboUsersRepository);
        categoryInfoRepository.insert(new CategoryInfo().setCategoryId(1).setModerationInYang(true));

        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        pskuHasContentMappingsServiceMock = new PskuHasContentMappingsService(repository);

        migrationService = MigrationServiceTestUtils.mockMigrationService(supplierRepository);

        antiMappingService = new AntiMappingService(antiMappingRepository, TransactionHelper.MOCK);

        var retrieveMappingSkuTypeService = Mockito.mock(RetrieveMappingSkuTypeService.class);
        Mockito.when(retrieveMappingSkuTypeService.retrieveMappingSkuType(anyCollection(), anySet(), any()))
            .then(invocation -> invocation.getArgument(0));

        var offerBatchProcessor = new OfferBatchProcessor(slaveDataSource, masterDataSource,
            transactionManager, transactionManager, repository, repository, masterTransactionTemplate);

        offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor,
            needContentStatusService,
            supplierService,
            categoryKnowledgeService,
            retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository,
            antiMappingRepository,
            offerDestinationCalculator,
            storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService),
            false,
            false,
            3, categoryInfoCache);

        pskuPostProcessorService = Mockito.mock(PskuPostProcessorService.class);
        offerMappingRollbackService = new OfferMappingRollbackService(offerRepository, offerMappingHistoryRepository,
            offersProcessingStatusService);

        var recheckClassificationService = Mockito.mock(RecheckClassificationService.class);

        service = new SaveMappingModerationService(
            repository, offersProcessingStatusService, supplierRepository, offerMappingActionService,
            modelStorageCachingServiceMock, needContentStatusService,
            pskuHasContentMappingsServiceMock, migrationService,
            antiMappingService, TransactionHelper.MOCK, offerMappingRollbackService, storageKeyValueService,
            recheckClassificationService);
    }

    @Test
    public void testEmptyListWillPass() {
        SupplierOffer.OperationResult result = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder().build())
            .getResult();
        assertThat(result.getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);
        assertThat(result.getOfferStatusesList()).hasSize(0);
    }

    @Test
    public void testErrorIfNotAllFieldPassed() {
        SupplierOffer.OperationResult result = service.saveMappingsModeration(
                MboCategory.SaveMappingsModerationRequest.newBuilder()
                    .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder().build())
                    .build())
            .getResult();
        assertThat(result.getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.ERROR);
        assertThat(result.getOfferStatusesList()).hasSize(0);
    }

    @Test
    public void testNoopIfOfferNotFound() {
        SupplierOffer.OperationResult result = service.saveMappingsModeration(
                MboCategory.SaveMappingsModerationRequest.newBuilder()
                    .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                        .setOfferId("-1")
                        .setShopSkuId("not-in-repo")
                        .setSupplierId(1L)
                        .setMarketSkuId(1L)
                        .setStatus(SupplierMappingModerationResult.ACCEPTED)
                        .setStaffLogin("test")
                        .build())
                    .build())
            .getResult();
        assertThat(result.getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.NOOP);
        Mockito.verify(repository, Mockito.times(1))
            .getOffersByIds(Mockito.anyCollection());
        assertThat(result.getOfferStatusesList()).hasSize(1);
        assertThat(result.getOfferStatusesList().get(0).getOfferId()).isEqualTo("-1");
        assertThat(result.getOfferStatusesList().get(0).getStatus()).isEqualTo(SupplierOffer.OperationStatus.NOOP);
    }

    @Test
    public void testNoopIfOfferInWrongProcessingStatus() {
        long offerId = 5;
        repository.updateOffer(repository.getOfferById(offerId).setShopSku("test-sku"));
        //  check that offer will be found
        assertThat(repository.getOffersByIds(List.of(offerId))).hasSize(1);
        Mockito.reset(repository);
        SupplierOffer.OperationResult result = service.saveMappingsModeration(
            MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId(String.valueOf(offerId))
                    .setShopSkuId("test-sku")
                    .setSupplierId(44L)
                    .setMarketSkuId(1L)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("test")
                    .build())
                .build()).getResult();
        assertThat(result.getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.NOOP);
        assertThat(result.getMessage()).isEqualTo("OfferId 5: " +
            "Offer already passed IN_MODERATION state, current state: IN_PROCESS");
        Mockito.verify(repository, Mockito.times(1))
            .getOffersByIds(Mockito.anyCollection());
        assertThat(result.getOfferStatusesList()).hasSize(1);
        assertThat(result.getOfferStatusesList().get(0).getOfferId()).isEqualTo("5");
        assertThat(result.getOfferStatusesList().get(0).getStatus()).isEqualTo(SupplierOffer.OperationStatus.NOOP);
    }

    @Test
    public void testErrorIfInternalErrorInSearch() {
        // internal error in search
        OfferRepository offerRepositoryMock = Mockito.mock(OfferRepository.class);
        service = new SaveMappingModerationService(
            offerRepositoryMock,
            offersProcessingStatusService,
            supplierRepository,
            Mockito.mock(OfferMappingActionService.class),
            Mockito.mock(ModelStorageCachingService.class),
            Mockito.mock(NeedContentStatusService.class),
            pskuHasContentMappingsServiceMock,
            migrationService,
            antiMappingService,
            TransactionHelper.MOCK,
            offerMappingRollbackService,
            storageKeyValueService,
            Mockito.mock(RecheckClassificationService.class)
        );
        Mockito.when(offerRepositoryMock.getOffersByIds(Mockito.anyCollection()))
            .thenThrow(new RuntimeException("Test Exception Find"));

        SupplierOffer.OperationResult res = service.saveMappingsModeration(
                MboCategory.SaveMappingsModerationRequest.newBuilder()
                    .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                        .setOfferId("5")
                        .setShopSkuId("test-sku")
                        .setSupplierId(44L)
                        .setMarketSkuId(1L)
                        .setStatus(SupplierMappingModerationResult.ACCEPTED)
                        .setStaffLogin("test")
                        .build())
                    .build())
            .getResult();
        assertThat(res.getStatus()).isEqualTo(SupplierOffer.OperationStatus.ERROR);
        assertThat(res.getMessage()).isEqualTo("RuntimeException: Test Exception Find");

        assertThat(res.getOfferStatusesList()).hasSize(1);
        assertThat(res.getOfferStatusesList().get(0).getOfferId()).isEqualTo("5");
        assertThat(res.getOfferStatusesList().get(0).getStatus()).isEqualTo(SupplierOffer.OperationStatus.ERROR);
    }

    @Test
    public void testErrorIfInternalErrorInUpdate() {
        OfferRepository offerRepositoryMock = Mockito.mock(OfferRepository.class);
        service = new SaveMappingModerationService(
            offerRepositoryMock,
            offersProcessingStatusService,
            supplierRepository,
            Mockito.mock(OfferMappingActionService.class),
            Mockito.mock(ModelStorageCachingService.class),
            Mockito.mock(NeedContentStatusService.class),
            pskuHasContentMappingsServiceMock,
            migrationService,
            antiMappingService,
            TransactionHelper.MOCK,
            offerMappingRollbackService,
            storageKeyValueService,
            Mockito.mock(RecheckClassificationService.class)
        );

        // internal error in update
        supplierRepository.insert(OfferTestUtils.simpleSupplier().setId(250));
        Mockito.when(offerRepositoryMock.getOffersByIds(Mockito.anyCollection()))
            .thenReturn(Collections.singletonList(offerWithSupplierMapping(1)));
        Mockito.when(offerRepositoryMock.updateOffers(Mockito.anyCollection()))
            .thenThrow(new RuntimeException("Test Exception Update"));

        SupplierOffer.OperationResult res = service.saveMappingsModeration(
                MboCategory.SaveMappingsModerationRequest.newBuilder()
                    .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                        .setOfferId("1")
                        .setShopSkuId("Sku1")
                        .setSupplierId(44L)
                        .setMarketSkuId(15L)
                        .setStatus(SupplierMappingModerationResult.ACCEPTED)
                        .setStaffLogin("test")
                        .build())
                    .build())
            .getResult();
        assertThat(res.getStatus()).isEqualTo(SupplierOffer.OperationStatus.ERROR);
        assertThat(res.getMessage()).endsWith("RuntimeException: Test Exception Update");

        assertThat(res.getOfferStatusesList()).hasSize(1);
        assertThat(res.getOfferStatusesList().get(0).getOfferId()).isEqualTo("1");
        assertThat(res.getOfferStatusesList().get(0).getStatus()).isEqualTo(SupplierOffer.OperationStatus.ERROR);
    }

    @Test
    public void testCorrectModerationTaskResultRequestOldPipelineSupplier() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));
        // rejected
        repository.insertOffer(offerWithSupplierMapping(21));
        // need info
        repository.insertOffer(offerWithSupplierMapping(22));
        // need info dsbs
        repository.insertOffer(offerWithSupplierMapping(23).setOfferDestination(Offer.MappingDestination.DSBS));
        // accepted with same skuId
        repository.insertOffer(offerWithSupplierMapping(24));
        // accepted with other skuId
        repository.insertOffer(offerWithSupplierMapping(25));

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.REJECTED)
                    .setStaffLogin("rejector")
                    .build())
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("22")
                    .setShopSkuId("Sku22")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("needinfer")
                    .addContentComment(SupplierOffer.ContentComment.newBuilder()
                        .setType("NEED_PICTURES")
                        .build())
                    .build())
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("23")
                    .setShopSkuId("Sku23")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("needinfer")
                    .addContentComment(SupplierOffer.ContentComment.newBuilder()
                        .setType("NEED_PICTURES")
                        .build())
                    .build())
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("24")
                    .setShopSkuId("Sku24")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("acceptor")
                    .build())
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("25")
                    .setShopSkuId("Sku25")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_2)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("acceptor")
                    .build())
                .build());

        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer rejected = repository.getOfferById(21L);
        assertThat(rejected.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        assertThat(rejected.getSupplierSkuId()).isEqualTo(MAPPING_ID_1);
        assertThat(rejected.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.REJECTED);
        assertThat(rejected.getSupplierSkuMappingCheckLogin()).isEqualTo("rejector");
        assertThat(rejected.getContentSkuMapping()).isNull();
        assertThat(rejected.getApprovedSkuMapping()).isNull();
        assertThat(rejected.getApprovedSkuMappingConfidence()).isNull();

        assertThat(response.getResult().getOfferStatusesList()).hasSize(5);
        assertThat(response.getResult().getOfferStatusesList().get(0).getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);
        assertThat(response.getResult().getOfferStatusesList().get(1).getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);
        assertThat(response.getResult().getOfferStatusesList().get(2).getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);
        assertThat(response.getResult().getOfferStatusesList().get(3).getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);
        assertThat(response.getResult().getOfferStatusesList().get(4).getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Map<OfferSkuMappingKey, AntiMapping> skuAntiMappingsRejected =
            antiMappingRepository.findByFilterAsMap(newFilter().setOfferIds(rejected.getId()));
        assertThat(skuAntiMappingsRejected)
            .containsOnlyKeys(new OfferSkuMappingKey(rejected.getId(), MAPPING_ID_1));
        assertThat(skuAntiMappingsRejected.values())
            .allMatch(am -> am.getUploadRequestTs() != null)
            .usingElementComparatorIgnoringFields(
                "id", "createdTs", "updatedTs", "deletedTs", "version",
                "needsStampUpdate", "uploadStamp", "uploadRequestTs")
            .containsExactlyInAnyOrder(new AntiMapping()
                .setOfferId(rejected.getId())
                .setSourceType(AntiMapping.SourceType.MODERATION_REJECT)
                .setNotModelId(0L)
                .setNotSkuId(MAPPING_ID_1)
                .setUpdatedUser("rejector"));

        Offer needInfo = repository.getOfferById(22L);
        assertThat(needInfo.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_INFO);
        assertThat(needInfo.getSupplierSkuId()).isEqualTo(MAPPING_ID_1);
        assertThat(needInfo.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.NEW);
        assertThat(needInfo.getSupplierSkuMappingCheckLogin()).isNull();
        assertThat(needInfo.getContentSkuMapping()).isNull();
        assertThat(needInfo.getApprovedSkuMapping()).isNull();
        assertThat(needInfo.getApprovedSkuMappingConfidence()).isNull();
        assertThat(needInfo.getContentComments())
            .containsExactly(new ContentComment(ContentCommentType.NEED_PICTURES));

        Map<OfferSkuMappingKey, AntiMapping> skuAntiMappingsNeedInfo =
            antiMappingRepository.findByFilterAsMap(newFilter().setOfferIds(needInfo.getId()));
        assertThat(skuAntiMappingsNeedInfo).isEmpty();

        Offer needInfoDsbs = repository.getOfferById(23L);
        assertThat(needInfoDsbs.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_INFO);
        assertThat(needInfoDsbs.getSupplierSkuId()).isEqualTo(MAPPING_ID_1);
        assertThat(needInfoDsbs.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.NEW);
        assertThat(needInfoDsbs.getSupplierSkuMappingCheckLogin()).isNull();
        assertThat(needInfoDsbs.getContentSkuMapping()).isNull();
        assertThat(needInfoDsbs.getApprovedSkuMapping()).isNull();
        assertThat(needInfoDsbs.getApprovedSkuMappingConfidence()).isNull();
        assertThat(needInfoDsbs.getContentComments())
            .containsExactly(new ContentComment(ContentCommentType.NEED_PICTURES));

        Map<OfferSkuMappingKey, AntiMapping> skuAntiMappingsNeedInfoDsbs =
            antiMappingRepository.findByFilterAsMap(newFilter().setOfferIds(needInfoDsbs.getId()));
        assertThat(skuAntiMappingsNeedInfoDsbs)
            .containsOnlyKeys(new OfferSkuMappingKey(needInfoDsbs.getId(), MAPPING_ID_1));
        assertThat(skuAntiMappingsNeedInfoDsbs.values())
            .allMatch(am -> am.getUploadRequestTs() != null)
            .usingElementComparatorIgnoringFields(
                "id", "createdTs", "updatedTs", "deletedTs", "version",
                "needsStampUpdate", "uploadStamp", "uploadRequestTs")
            .containsExactlyInAnyOrder(new AntiMapping()
                .setOfferId(needInfoDsbs.getId())
                .setSourceType(AntiMapping.SourceType.MODERATION_NEED_INFO)
                .setNotModelId(0L)
                .setNotSkuId(MAPPING_ID_1)
                .setUpdatedUser("needinfer"));

        Offer accepted = repository.getOfferById(24L);
        assertThat(accepted.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(accepted.getSupplierSkuId()).isEqualTo(MAPPING_ID_1);
        assertThat(accepted.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.ACCEPTED);
        assertThat(accepted.getSupplierSkuMappingCheckLogin()).isEqualTo("acceptor");
        assertThat(accepted.getContentSkuId()).isEqualTo(MAPPING_ID_1);
        assertThat(accepted.getApprovedSkuId()).isEqualTo(MAPPING_ID_1);
        assertThat(accepted.getApprovedSkuMappingConfidence()).isEqualTo(Offer.MappingConfidence.CONTENT);

        Map<OfferSkuMappingKey, AntiMapping> skuAntiMappingsAccepted =
            antiMappingRepository.findByFilterAsMap(newFilter().setOfferIds(accepted.getId()));
        assertThat(skuAntiMappingsAccepted).isEmpty();

        Offer acceptedWithOtherSku = repository.getOfferById(25L);
        assertThat(acceptedWithOtherSku.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(acceptedWithOtherSku.getSupplierSkuId()).isEqualTo(MAPPING_ID_1);
        assertThat(acceptedWithOtherSku.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.REJECTED);
        assertThat(acceptedWithOtherSku.getSupplierSkuMappingCheckLogin()).isEqualTo("acceptor");
        assertThat(acceptedWithOtherSku.getContentSkuId()).isEqualTo(MAPPING_ID_2);
        assertThat(acceptedWithOtherSku.getApprovedSkuId()).isEqualTo(MAPPING_ID_2);
        assertThat(acceptedWithOtherSku.getApprovedSkuMappingConfidence()).isEqualTo(Offer.MappingConfidence.CONTENT);

        Map<OfferSkuMappingKey, AntiMapping> skuAntiMappingsAcceptedWithOtherSku =
            antiMappingRepository.findByFilterAsMap(newFilter().setOfferIds(acceptedWithOtherSku.getId()));
        assertThat(skuAntiMappingsAcceptedWithOtherSku).isEmpty();
    }

    @Test
    public void testExistingAntiMappingUpdated() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));
        // rejected
        repository.insertOffer(offerWithSupplierMapping(21));
        // need info
        repository.insertOffer(offerWithSupplierMapping(22).setOfferDestination(Offer.MappingDestination.DSBS));

        antiMappingRepository.insertBatch(
            new AntiMapping()
                .setOfferId(21L)
                .setSourceType(AntiMapping.SourceType.MODERATION_REJECT)
                .setNotModelId(0L)
                .setNotSkuId(MAPPING_ID_1)
                .setUpdatedUser("rejector_before"),
            new AntiMapping()
                .setOfferId(22L)
                .setSourceType(AntiMapping.SourceType.MODERATION_REJECT)
                .setNotModelId(0L)
                .setNotSkuId(MAPPING_ID_1)
                .setUpdatedUser("rejector_before_needinfer")
        );

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.REJECTED)
                    .setStaffLogin("rejector")
                    .build())
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("22")
                    .setShopSkuId("Sku22")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("needinfer")
                    .addContentComment(SupplierOffer.ContentComment.newBuilder()
                        .setType("NEED_PICTURES")
                        .build())
                    .build())
                .build());

        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        assertThat(response.getResult().getOfferStatusesList()).hasSize(2);
        assertThat(response.getResult().getOfferStatusesList().get(0).getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);
        assertThat(response.getResult().getOfferStatusesList().get(1).getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer rejected = repository.getOfferById(21L);
        assertThat(rejected.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        assertThat(rejected.getSupplierSkuId()).isEqualTo(MAPPING_ID_1);
        assertThat(rejected.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.REJECTED);
        assertThat(rejected.getSupplierSkuMappingCheckLogin()).isEqualTo("rejector");
        assertThat(rejected.getContentSkuMapping()).isNull();
        assertThat(rejected.getApprovedSkuMapping()).isNull();
        assertThat(rejected.getApprovedSkuMappingConfidence()).isNull();

        Map<OfferSkuMappingKey, AntiMapping> skuAntiMappingsRejected =
            antiMappingRepository.findByFilterAsMap(newFilter().setOfferIds(rejected.getId()));
        assertThat(skuAntiMappingsRejected)
            .containsOnlyKeys(new OfferSkuMappingKey(rejected.getId(), MAPPING_ID_1));
        assertThat(skuAntiMappingsRejected.values())
            .allMatch(am -> am.getUploadRequestTs() != null)
            .usingElementComparatorIgnoringFields(
                "id", "createdTs", "updatedTs", "deletedTs", "version",
                "needsStampUpdate", "uploadStamp", "uploadRequestTs")
            .containsExactlyInAnyOrder(new AntiMapping()
                .setOfferId(rejected.getId())
                .setSourceType(AntiMapping.SourceType.MODERATION_REJECT)
                .setNotModelId(0L)
                .setNotSkuId(MAPPING_ID_1)
                .setUpdatedUser("rejector"));

        Offer needInfo = repository.getOfferById(22L);
        assertThat(needInfo.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_INFO);
        assertThat(needInfo.getSupplierSkuId()).isEqualTo(MAPPING_ID_1);
        assertThat(needInfo.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.NEW);
        assertThat(needInfo.getSupplierSkuMappingCheckLogin()).isNull();
        assertThat(needInfo.getContentSkuMapping()).isNull();
        assertThat(needInfo.getApprovedSkuMapping()).isNull();
        assertThat(needInfo.getApprovedSkuMappingConfidence()).isNull();
        assertThat(needInfo.getContentComments())
            .containsExactly(new ContentComment(ContentCommentType.NEED_PICTURES));

        Map<OfferSkuMappingKey, AntiMapping> skuAntiMappingsNeedInfo =
            antiMappingRepository.findByFilterAsMap(newFilter().setOfferIds(needInfo.getId()));
        assertThat(skuAntiMappingsNeedInfo)
            .containsOnlyKeys(new OfferSkuMappingKey(needInfo.getId(), MAPPING_ID_1));
        assertThat(skuAntiMappingsNeedInfo.values())
            .allMatch(am -> am.getUploadRequestTs() != null)
            .usingElementComparatorIgnoringFields(
                "id", "createdTs", "updatedTs", "deletedTs", "version",
                "needsStampUpdate", "uploadStamp", "uploadRequestTs")
            .containsExactlyInAnyOrder(new AntiMapping()
                .setOfferId(needInfo.getId())
                .setSourceType(AntiMapping.SourceType.MODERATION_NEED_INFO)
                .setNotModelId(0L)
                .setNotSkuId(MAPPING_ID_1)
                .setUpdatedUser("needinfer"));
    }

    @Test
    public void testExistingAntiMappingDeletedOnAcceptance() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));
        // accepted with same skuId
        repository.insertOffer(offerWithSupplierMapping(23));

        antiMappingRepository.insertBatch(
            new AntiMapping()
                .setOfferId(23L)
                .setSourceType(AntiMapping.SourceType.MODERATION_REJECT)
                .setNotModelId(0L)
                .setNotSkuId(MAPPING_ID_1)
                .setUpdatedUser("rejector")
        );

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("23")
                    .setShopSkuId("Sku23")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("acceptor")
                    .build())
                .build());

        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        assertThat(response.getResult().getOfferStatusesList()).hasSize(1);
        assertThat(response.getResult().getOfferStatusesList().get(0).getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer accepted = repository.getOfferById(23L);
        assertThat(accepted.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(accepted.getSupplierSkuId()).isEqualTo(MAPPING_ID_1);
        assertThat(accepted.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.ACCEPTED);
        assertThat(accepted.getSupplierSkuMappingCheckLogin()).isEqualTo("acceptor");
        assertThat(accepted.getContentSkuId()).isEqualTo(MAPPING_ID_1);
        assertThat(accepted.getApprovedSkuId()).isEqualTo(MAPPING_ID_1);
        assertThat(accepted.getApprovedSkuMappingConfidence()).isEqualTo(Offer.MappingConfidence.CONTENT);

        List<AntiMapping> deletedAntimappings = antiMappingRepository.findByFilter(newFilter()
            .setOfferIds(accepted.getId()));
        assertThat(deletedAntimappings)
            .allMatch(am -> am.getUploadRequestTs() != null)
            .usingElementComparatorIgnoringFields(
                "id", "createdTs", "updatedTs", "deletedTs", "version",
                "needsStampUpdate", "uploadStamp", "uploadRequestTs")
            .allMatch(AntiMapping::isDeleted)
            .containsExactlyInAnyOrder(new AntiMapping()
                .setOfferId(accepted.getId())
                .setSourceType(AntiMapping.SourceType.MODERATION_REJECT)
                .setNotModelId(0L)
                .setNotSkuId(MAPPING_ID_1)
                .setUpdatedUser("acceptor")
                .setDeletedUser("acceptor"));
    }

    @Test
    public void testCorrectModerationTaskResultRequestOldPipeLine() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe").setNewContentPipeline(false)));

        insertDifferentOffers();

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addAllResults(createModerationTaskResults())
                .build());

        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        // all accepted should have approved mapping
        assertThat(repository.findOffers(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .setOfferIds(21, 22, 23, 24, 25)))
            .allMatch(offer -> offer.hasApprovedSkuMapping() &&
                offer.getProcessingStatus() == Offer.ProcessingStatus.PROCESSED);
        // rejected and need info without mappings goes further in pipeline
        assertThat(repository.findOffers(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .setOfferIds(31, 41)))
            .allMatch(offer -> offer.getProcessingStatus() == Offer.ProcessingStatus.IN_PROCESS
                && !offer.hasSupplierSkuMapping() && !offer.hasSuggestSkuMapping());
        // rejected and need info not changed if supplier and suggest different
        assertThat(repository.findOffers(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .setOfferIds(32, 42)))
            .allMatch(offer -> offer.getProcessingStatus() == Offer.ProcessingStatus.IN_MODERATION);
        // rejected
        assertThat(repository.getOfferById(33))
            .matches(offer -> offer.getProcessingStatus() == Offer.ProcessingStatus.IN_PROCESS
                && offer.getSupplierSkuMappingStatus() == Offer.MappingStatus.NEW);
        assertThat(repository.findOffers(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .setOfferIds(34, 35)))
            .allMatch(offer -> offer.getProcessingStatus() == Offer.ProcessingStatus.IN_PROCESS
                && offer.getSupplierSkuMappingStatus() == Offer.MappingStatus.REJECTED);
        // need info
        assertThat(repository.findOffers(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .setOfferIds(43, 44, 45)))
            .allMatch(offer -> offer.getProcessingStatus() == Offer.ProcessingStatus.NEED_INFO);
    }

    @Test
    public void testCorrectModerationTaskResultRequestNewPipeline() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "new pipe").setNewContentPipeline(true)));
        categoryCachingService.addCategory(
            new Category().setCategoryId(CATEGORY_ID1)
                .setAcceptGoodContent(true)
                .setHasKnowledge(true)
        );
        categoryKnowledgeService.addCategory(CATEGORY_ID1);

        insertDifferentOffers();

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addAllResults(createModerationTaskResults())
                .build());

        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        // all accepted should have approved mapping
        assertThat(repository.findOffers(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .setOfferIds(21, 22, 23, 24, 25)))
            .allMatch(offer -> offer.hasApprovedSkuMapping() &&
                offer.getProcessingStatus() == Offer.ProcessingStatus.PROCESSED);
        // rejected and need info without mappings goes further in pipeline
        assertThat(repository.findOffers(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .setOfferIds(31, 41)))
            .allMatch(offer -> offer.getProcessingStatus() == Offer.ProcessingStatus.NEED_CONTENT
                && !offer.hasSupplierSkuMapping() && !offer.hasSuggestSkuMapping());
        // rejected and need info not changed if supplier mapping differs
        assertThat(repository.findOffers(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .setOfferIds(32, 42)))
            .allMatch(offer -> offer.getProcessingStatus() == Offer.ProcessingStatus.IN_MODERATION);
        // rejected
        assertThat(repository.findOffers(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .setOfferIds(34, 35)))
            .allMatch(offer -> offer.getProcessingStatus() == Offer.ProcessingStatus.NEED_CONTENT &&
                offer.getSupplierSkuMappingStatus() == Offer.MappingStatus.REJECTED);
        // need info
        assertThat(repository.findOffers(new OffersFilter()
            .setOrderBy(OffersFilter.Field.ID)
            .setOfferIds(44, 45)))
            .allMatch(offer -> offer.getProcessingStatus() == Offer.ProcessingStatus.NEED_INFO);
    }

    @Test
    public void testPskuIsChangedSinceWasMapped() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "new pipe").setNewContentPipeline(true)));

        long offerId = 12345L;
        Offer offer = offerWithSuggestMapping(offerId);
        repository.insertOffer(offer);

        // case psku is changed
        long skuTimestamp = 100000L;
        long resultTimestamp = 10L;
        testPskuMappingResult(offerId, skuTimestamp, resultTimestamp, SupplierOffer.OperationStatus.REPROCESS);
        var newOffer = repository.getOffersByIdsWithOfferContent(List.of(offerId)).get(0);

        //ignore field
        offer.storeOfferContent(OfferContent.builder().id(offerId).build());

        // Should be incremented for reprocessing
        offer.incrementProcessingCounter();

        Assertions.assertThat(offer)
            .usingRecursiveComparison()
            .ignoringFields("updated", "lastVersion", "isOfferContentPresent")
            .isEqualTo(newOffer);

        // case psku is not changed
        skuTimestamp = 100000L;
        resultTimestamp = 100000L;
        testPskuMappingResult(offerId, skuTimestamp, resultTimestamp, SupplierOffer.OperationStatus.SUCCESS);
        newOffer = repository.getOffersByIds(List.of(offerId)).get(0);
        assertThat(newOffer.getApprovedSkuId()).isEqualTo(MAPPING_ID_1);
    }

    @Test
    public void testPskuIsChanged() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "new pipe").setNewContentPipeline(true)));

        long offerId = 12345L;
        Offer offer = offerWithoutMappings(offerId);
        repository.insertOffer(offer);

        // case psku is changed
        long skuTimestamp = 100000L;
        long resultTimestamp = 10L;
        testPskuMappingResult(offerId, skuTimestamp, resultTimestamp, SupplierOffer.OperationStatus.REPROCESS);
        var newOffer = repository.getOffersByIdsWithOfferContent(List.of(offerId)).get(0);

        Assert.assertNotNull(newOffer.getProcessingCounter());
        int oldCounter = offer.getProcessingCounter() == null ? 0 : offer.getProcessingCounter();
        Assert.assertTrue(oldCounter < newOffer.getProcessingCounter());
    }

    private void testPskuMappingResult(long offerId, long skuTimestamp, long resultTimestamp,
                                       SupplierOffer.OperationStatus status) {
        modelStorageCachingServiceMock.addModel(new Model()
            .setId(MAPPING_ID_1).setTitle("Test title")
            .setCategoryId(CATEGORY_ID1)
            .setModifiedTs(TimestampUtil.toInstant(skuTimestamp))
            .setModelType(Model.ModelType.PARTNER_SKU)
            .setPublishedOnBlueMarket(true));

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId(String.valueOf(offerId))
                    .setShopSkuId("Sku" + offerId)
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("login")
                    .setSkuModifiedTs(resultTimestamp)
                    .build())
                .build());

        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        SupplierOffer.OfferStatus offerStatus = response.getResult().getOfferStatusesList().get(0);
        Assert.assertEquals(status, offerStatus.getStatus());
    }

    @Test
    public void testModerationoPskuRejectByBadCard() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "new pipe").setNewContentPipeline(true)));
        categoryCachingService.addCategory(
            new Category().setCategoryId(CATEGORY_ID1)
                .setAcceptGoodContent(true)
                .setHasKnowledge(true)
        );
        categoryKnowledgeService.addCategory(CATEGORY_ID1);

        insertDifferentOffers();
        List<String> ids = Arrays.asList("33", "34", "35");
        List<SupplierOffer.MappingModerationTaskResult> moderationResults = createModerationTaskResults().stream()
            .filter(r -> ids.contains(r.getOfferId()))
            .map(r -> r.toBuilder().setBadCard(true).build())
            .collect(Collectors.toList());
        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addAllResults(moderationResults)
                .build());

        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        // all rejected
        assertThat(repository.findOffers(new OffersFilter()
            .setOfferIds(moderationResults.stream().map(r -> Long.parseLong(r.getOfferId()))
                .collect(Collectors.toSet()))))
            .allMatch(offer -> offer.getProcessingStatus() == Offer.ProcessingStatus.IN_PROCESS);
    }

    private void insertDifferentOffers() {
        // 21 - 25 accepted
        // 31 - 35 rejected
        // 41 - 45 need_info
        for (int i = 21; i <= 41; i += 10) {
            // both empty (behavior same as different)
            repository.insertOffer(offerWithoutMappings(i));
            // both different
            repository.insertOffer(offerWithoutMappings(i + 1)
                .setSupplierSkuMapping(OfferTestUtils.mapping(MAPPING_ID_2, Offer.SkuType.MARKET))
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .setSuggestSkuMapping(OfferTestUtils.mapping(MAPPING_ID_2, Offer.SkuType.MARKET)));
            // suggest same
            repository.insertOffer(offerWithoutMappings(i + 2)
                .setSupplierSkuMapping(OfferTestUtils.mapping(MAPPING_ID_2))
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .setSuggestSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET)));
            // supplier same
            repository.insertOffer(offerWithoutMappings(i + 3)
                .setSupplierSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .setSuggestSkuMapping(OfferTestUtils.mapping(MAPPING_ID_2, Offer.SkuType.MARKET)));
            // both same
            repository.insertOffer(offerWithoutMappings(i + 4)
                .setSupplierSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
                .setSuggestSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET)));
        }
    }


    @Test
    public void testModerationNeedInfoSpecialCasesOldPipeline() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));
        repository.insertOffers(Arrays.asList(
            offerWithSupplierMapping(21L),
            offerWithSupplierMapping(22L)));

        MboCategory.SaveMappingModerationResponse response = service.saveMappingsModeration(
            MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku" + 1)
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("test")
                    .addContentComment(SupplierOffer.ContentComment.newBuilder()
                        .setType("LEGAL_CONFLICT")
                        .build())
                    .build())
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("22")
                    .setShopSkuId("Sku" + 2)
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("test")
                    .addContentComment(SupplierOffer.ContentComment.newBuilder()
                        .setType("WRONG_CATEGORY")
                        .build())
                    .build())
                .build());

        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);


        Offer offer = repository.getOfferById(21L);
        assertThat(offer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.LEGAL_PROBLEM);

        offer = repository.getOfferById(22L);
        assertThat(offer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_RECLASSIFICATION);
    }

    @Test
    public void testModerationNeedInfoSpecialCasesNewPipeline() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "new pipe")
                .setNewContentPipeline(true)));
        categoryCachingService.addCategories(
            new Category().setCategoryId(CATEGORY_ID1).setAcceptGoodContent(true).setHasKnowledge(true),
            new Category().setCategoryId(CATEGORY_ID2).setAcceptGoodContent(false).setHasKnowledge(true)
        );
        categoryKnowledgeService.addCategory(CATEGORY_ID1);
        categoryKnowledgeService.addCategory(CATEGORY_ID2);

        repository.insertOffers(Arrays.asList(
            offerWithSupplierMapping(21L),
            offerWithSupplierMapping(22L),
            offerWithSupplierMapping(23L).setCategoryIdForTests(CATEGORY_ID2, Offer.BindingKind.SUGGESTED),
            offerWithSuggestMapping(24L)));


        MboCategory.SaveMappingModerationResponse response = service.saveMappingsModeration(
            MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku" + 1)
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("test")
                    .addContentComment(SupplierOffer.ContentComment.newBuilder()
                        .setType("LEGAL_CONFLICT")
                        .build())
                    .build())
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("22")
                    .setShopSkuId("Sku" + 2)
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("test")
                    .addContentComment(SupplierOffer.ContentComment.newBuilder()
                        .setType("WRONG_CATEGORY")
                        .build())
                    .build())
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("23")
                    .setShopSkuId("Sku" + 3)
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("test")
                    .addContentComment(SupplierOffer.ContentComment.newBuilder()
                        .setType("WRONG_CATEGORY")
                        .build())
                    .build())
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("24")
                    .setShopSkuId("Sku" + 4)
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("test")
                    .addContentComment(SupplierOffer.ContentComment.newBuilder()
                        .setType("WRONG_CATEGORY")
                        .build())
                    .build())
                .build());

        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);


        Offer offer = repository.getOfferById(21L);
        assertThat(offer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.LEGAL_PROBLEM);

        offer = repository.getOfferById(22L);
        assertThat(offer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.NEED_CONTENT);
        assertThat(offer.getSupplierSkuMappingStatus())
            .isEqualTo(Offer.MappingStatus.REJECTED);

        offer = repository.getOfferById(23L);
        assertThat(offer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_RECLASSIFICATION);

        offer = repository.getOfferById(24L);
        assertThat(offer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.NEED_CONTENT);
        assertThat(offer.getSupplierSkuMappingStatus())
            .isEqualTo(Offer.MappingStatus.NONE);
    }

    @Test
    public void testMappingFromTolokacheckAntiMapping() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));
        // rejected
        repository.insertOffer(offerWithSupplierMapping(21));
        // need info
        repository.insertOffer(offerWithSupplierMapping(22).setOfferDestination(Offer.MappingDestination.DSBS));


        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.REJECTED)
                    .setStaffLogin("rejector")
                    .setFromToloka(true)
                    .build())
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("22")
                    .setShopSkuId("Sku22")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("needinfer")
                    .addContentComment(SupplierOffer.ContentComment.newBuilder()
                        .setType("NEED_PICTURES")
                        .build())
                    .setFromToloka(true)
                    .build())
                .build());

        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer rejected = repository.getOfferById(21L);
        Offer needInfo = repository.getOfferById(22L);

        Map<OfferSkuMappingKey, AntiMapping> skuAntiMappingsRejected =
            antiMappingRepository.findByFilterAsMap(newFilter().setOfferIds(rejected.getId()));
        Assert.assertFalse(skuAntiMappingsRejected.isEmpty());

        Map<OfferSkuMappingKey, AntiMapping> skuAntiMappingsNeedInfo =
            antiMappingRepository.findByFilterAsMap(newFilter().setOfferIds(needInfo.getId()));
        assertThat(skuAntiMappingsNeedInfo).isEmpty();

    }

    @Test
    public void testMappingFromTolokacheckNeedInfo() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));
        // rejected
        repository.insertOffer(offerWithSupplierMapping(21));
        // need info
        repository.insertOffer(offerWithSupplierMapping(22).setOfferDestination(Offer.MappingDestination.DSBS));


        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("22")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("rejector")
                    .setFromToloka(true)
                    .build())
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.REJECTED)
                    .setStaffLogin("rejector")
                    .setFromToloka(true)
                    .build())
                .build());

        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer needInfo = repository.getOfferById(22L);
        Offer rejected = repository.getOfferById(21L);
        List<Long> toYang = new ArrayList<>();
        toYang.add(needInfo.getId());

        Assert.assertTrue(needInfo.getHideFromToloka());
        Assert.assertNull(rejected.getHideFromToloka());

    }

    @Test
    public void testMappingWillBeRejectedIfMarketSkuIsInvalid() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));
        // accepted with valid model
        repository.insertOffer(offerWithSupplierMapping(23));
        // accepted with invalid model 1
        repository.insertOffer(offerWithSupplierMapping(24)
            .setSupplierSkuMapping(OfferTestUtils.mapping(INVALID_MAPPING_ID_1, Offer.SkuType.MARKET)));
        // accepted with invalid model 2
        repository.insertOffer(offerWithSupplierMapping(25)
            .setSupplierSkuMapping(OfferTestUtils.mapping(INVALID_MAPPING_ID_2)));
        // accepted with not existing model
        repository.insertOffer(offerWithSupplierMapping(26)
            .setSupplierSkuMapping(OfferTestUtils.mapping(100, Offer.SkuType.MARKET)));
        // accepted with not existing model (when, we should not process)
        repository.insertOffer(offerWithSupplierMapping(27));
        // accepted with deleted model
        repository.insertOffer(offerWithSupplierMapping(28)
            .setSupplierSkuMapping(OfferTestUtils.mapping(DELETED_MODEL_MAPPING_ID, Offer.SkuType.MARKET)));

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("23")
                    .setShopSkuId("Sku23")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("test-user"))
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("24")
                    .setShopSkuId("Sku24")
                    .setSupplierId(250)
                    .setMarketSkuId(INVALID_MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("test-user"))
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("25")
                    .setShopSkuId("Sku25")
                    .setSupplierId(250)
                    .setMarketSkuId(INVALID_MAPPING_ID_2)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("test-user"))
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("26")
                    .setShopSkuId("Sku26")
                    .setSupplierId(250)
                    .setMarketSkuId(100)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("test-user"))
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("27")
                    .setShopSkuId("Sku27")
                    .setSupplierId(250)
                    .setMarketSkuId(100)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("test-user"))
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("28")
                    .setShopSkuId("Sku28")
                    .setSupplierId(250)
                    .setMarketSkuId(DELETED_MODEL_MAPPING_ID)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("test-user"))
                .build());

        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer accepted23 = repository.getOfferById(23L);
        MbocAssertions.assertThat(accepted23)
            .hasProcessingStatus(Offer.ProcessingStatus.PROCESSED)
            .hasSupplierMapping(MAPPING_ID_1)
            .hasContentMapping(MAPPING_ID_1);

        Offer updated99 = repository.getOfferById(99L);
        MbocAssertions.assertThat(updated99)
            .pskuHasContentMappings();

        Offer rejected24 = repository.getOfferById(24L);
        MbocAssertions.assertThat(rejected24)
            .hasSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED)
            .hasProcessingStatus(Offer.ProcessingStatus.IN_PROCESS)
            .hasComment("Can't set content mapping, because market sku (25) is invalid " +
                "(not exist, or not sku, or not published on blue, or smth else)");

        Offer rejected25 = repository.getOfferById(25L);
        MbocAssertions.assertThat(rejected25)
            .hasSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED)
            .hasProcessingStatus(Offer.ProcessingStatus.IN_PROCESS)
            .hasComment("Can't set content mapping, because market sku (30) is invalid " +
                "(not exist, or not sku, or not published on blue, or smth else)");

        Offer rejected26 = repository.getOfferById(26L);
        MbocAssertions.assertThat(rejected26)
            .hasSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED)
            .hasProcessingStatus(Offer.ProcessingStatus.IN_PROCESS)
            .hasComment("Can't set content mapping, because market sku (100) is invalid " +
                "(not exist, or not sku, or not published on blue, or smth else)");

        Offer notChanged = repository.getOfferById(27L);
        MbocAssertions.assertThat(notChanged)
            // nothing changed
            .hasSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .hasProcessingStatus(Offer.ProcessingStatus.IN_MODERATION);

        Offer rejected28 = repository.getOfferById(28L);
        MbocAssertions.assertThat(rejected28)
            .hasSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED)
            .hasProcessingStatus(Offer.ProcessingStatus.IN_PROCESS)
            .hasComment("Can't set content mapping, because market sku (35) is invalid " +
                "(not exist, or not sku, or not published on blue, or smth else)");
    }

    @Test
    public void willResetRecheckedApproveMappingOnReject() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        var offerWithApprovedMapping = offerWithApprovedMapping(21);
        offerWithApprovedMapping
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
            .setRecheckSkuMapping(offerWithApprovedMapping.getApprovedSkuMapping())
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_RECHECK);
        repository.insertOffer(offerWithApprovedMapping);

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.REJECTED)
                    .setStaffLogin("rejector")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer rejectedOffer = repository.getOfferById(offerWithApprovedMapping.getId());

        assertThat(rejectedOffer.hasApprovedSkuMapping())
            .isFalse();
        assertThat(rejectedOffer.getApprovedSkuId())
            .isEqualTo(0L);
        assertThat(rejectedOffer.hasSuggestSkuMapping())
            .isFalse();
        assertThat(rejectedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        assertThat(rejectedOffer.getRecheckMappingStatus())
            .isNull();
    }

    @Test
    public void willConfirmRecheckedApproveMappingOnApprove() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        var offerWithApprovedMapping = offerWithoutMappings(21)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
            .setSuggestSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.DEDUPLICATION);
        offerWithApprovedMapping
            .setRecheckSkuMapping(offerWithApprovedMapping.getApprovedSkuMapping())
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_RECHECK);
        offerWithApprovedMapping
            .setSupplierCategoryId(CATEGORY_ID2)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.NEW)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.ON_RECHECK)
            .setRecheckCategoryId(offerWithApprovedMapping.getCategoryId());

        repository.insertOffer(offerWithApprovedMapping);

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("acceptor")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer acceptedOffer = repository.getOfferById(offerWithApprovedMapping.getId());

        assertThat(acceptedOffer.getApprovedSkuMapping())
            .isEqualTo(offerWithApprovedMapping.getApprovedSkuMapping());
        assertThat(acceptedOffer.getApprovedSkuMappingConfidence())
            .isEqualTo(Offer.MappingConfidence.CONTENT);
        assertThat(acceptedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(acceptedOffer.getRecheckMappingStatus())
            .isEqualTo(Offer.RecheckMappingStatus.MAPPING_CONFIRMED);
        assertThat(acceptedOffer.getRecheckClassificationStatus())
            .isEqualTo(Offer.RecheckClassificationStatus.CONFIRMED);
        assertThat(acceptedOffer.getSupplierCategoryMappingStatus())
            .isEqualTo(Offer.MappingStatus.REJECTED);
    }

    @Test
    public void willConfirmRecheckedApproveMappingOnApproveWithFaultyCard() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        modelStorageCachingServiceMock
            .addModel(new Model()
                .setId(MAPPING_ID_1).setTitle("Published on blue sku")
                .setCategoryId(CATEGORY_ID1)
                .setModelType(Model.ModelType.SKU).setPublishedOnBlueMarket(false));

        var offerWithApprovedMapping = offerWithoutMappings(21)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
            .setSuggestSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.DEDUPLICATION);
        offerWithApprovedMapping
            .setRecheckSkuMapping(offerWithApprovedMapping.getApprovedSkuMapping())
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_RECHECK);

        repository.insertOffer(offerWithApprovedMapping);

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("acceptor")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer acceptedOffer = repository.getOfferById(offerWithApprovedMapping.getId());

        assertThat(acceptedOffer.getApprovedSkuMapping())
            .isEqualTo(offerWithApprovedMapping.getApprovedSkuMapping());
        assertThat(acceptedOffer.getApprovedSkuMappingConfidence())
            .isEqualTo(Offer.MappingConfidence.DEDUPLICATION);
        assertThat(acceptedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(acceptedOffer.getRecheckMappingStatus())
            .isEqualTo(Offer.RecheckMappingStatus.MAPPING_CONFIRMED);
    }

    @Test
    public void willReprocessRecheckIfMappingChanged() {
        var newMapping = OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET);
        var oldMapping = OfferTestUtils.mapping(MAPPING_ID_2, Offer.SkuType.MARKET);

        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        var offerWithApprovedMapping = offerWithoutMappings(21)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
            .setSuggestSkuMapping(newMapping)
            .updateApprovedSkuMapping(newMapping)
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_RECHECK)
            .setRecheckSkuMapping(newMapping);

        repository.insertOffer(offerWithApprovedMapping);

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(oldMapping.getMappingId())
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("acceptor")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer acceptedOffer = repository.getOfferById(offerWithApprovedMapping.getId());

        assertThat(acceptedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION);
        assertThat(acceptedOffer.getRecheckMappingStatus())
            .isEqualTo(Offer.RecheckMappingStatus.ON_RECHECK);
    }

    @Test
    public void willResetRecheckedApproveMappingOnNeedInfo() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        var offerWithApprovedMapping = offerWithApprovedMapping(21);
        offerWithApprovedMapping
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
            .setRecheckSkuMapping(offerWithApprovedMapping.getApprovedSkuMapping())
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_RECHECK);
        repository.insertOffer(offerWithApprovedMapping);

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .addContentComment(SupplierOffer.ContentComment.newBuilder()
                        .setType("NEED_PICTURES")
                        .build())
                    .setStaffLogin("rejector")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer rejectedOffer = repository.getOfferById(offerWithApprovedMapping.getId());

        assertThat(rejectedOffer.hasApprovedSkuMapping())
            .isFalse();
        assertThat(rejectedOffer.getApprovedSkuId())
            .isEqualTo(0L);
        assertThat(rejectedOffer.hasSuggestSkuMapping())
            .isFalse();
        assertThat(rejectedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        assertThat(rejectedOffer.getRecheckMappingStatus())
            .isNull();
        assertThat(rejectedOffer.getContentComments())
            .isEmpty();
    }

    @Test
    public void willResetRecheckedApproveMappingOnNeedInfoWhenNoSuggests() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        var offerWithApprovedMapping = offerWithApprovedMapping(21)
            .setSuggestSkuMapping(null)
            .setSupplierSkuMapping(null);
        offerWithApprovedMapping
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
            .setRecheckSkuMapping(offerWithApprovedMapping.getApprovedSkuMapping())
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_RECHECK);
        repository.insertOffer(offerWithApprovedMapping);

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("rejector")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer rejectedOffer = repository.getOfferById(offerWithApprovedMapping.getId());

        assertThat(rejectedOffer.hasApprovedSkuMapping())
            .isFalse();
        assertThat(rejectedOffer.getApprovedSkuId())
            .isEqualTo(0L);
        assertThat(rejectedOffer.hasSuggestSkuMapping())
            .isFalse();
        assertThat(rejectedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        assertThat(rejectedOffer.getRecheckMappingStatus())
            .isNull();
    }

    @Test
    public void willResetRollbackApproveMappingOnNeedInfo() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        var offerWithApprovedMapping = offerWithApprovedMapping(21)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_2, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        repository.insertOffer(offerWithApprovedMapping);

        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.DEDUPLICATION);
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .setApprovedSkuMappingInternal(null)
            .setApprovedSkuMappingConfidence(null)
            .setSuggestSkuMapping(null);
        offerMappingRollbackService.startRollback(List.of(offerWithApprovedMapping), Map.of());
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId());

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_2)
                    .setStatus(SupplierMappingModerationResult.NEED_INFO)
                    .setStaffLogin("rejector")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer rejectedOffer = repository.getOfferById(offerWithApprovedMapping.getId());

        assertThat(rejectedOffer.hasApprovedSkuMapping())
            .isFalse();
        assertThat(rejectedOffer.hasSuggestSkuMapping())
            .isFalse();
        assertThat(rejectedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        assertThat(rejectedOffer.getRecheckMappingStatus())
            .isNull();

        //IN_MODERATION (initial) -> IN_RECHECK_MODERATION (rollback) -> IN_PROCESS
        assertThat(offerRepository.getOfferById(rejectedOffer.getId()).getProcessingCounter()).isEqualTo(3);
    }

    @Test
    public void willConfirmRollbackApproveMappingOnApprove() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        var offerWithApprovedMapping = offerWithApprovedMapping(21)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_2, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        repository.insertOffer(offerWithApprovedMapping);

        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.DEDUPLICATION);
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId());
        offerMappingRollbackService.startRollback(List.of(offerWithApprovedMapping), Map.of());
        offerWithApprovedMapping.setApprovedSkuMappingInternal(null)
            .setApprovedSkuMappingConfidence(null);
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId());

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_2)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("acceptor")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer acceptedOffer = repository.getOfferById(offerWithApprovedMapping.getId());

        assertThat(acceptedOffer.getApprovedSkuId()).isEqualTo(MAPPING_ID_2);
        assertThat(acceptedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(acceptedOffer.getRecheckMappingStatus())
            .isEqualTo(Offer.RecheckMappingStatus.MAPPING_CONFIRMED);
    }

    @Test
    public void willRollbackFurtherApproveMappingOnReject() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        var offerWithApprovedMapping = offerWithApprovedMapping(21)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_2, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        repository.insertOffer(offerWithApprovedMapping);

        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_3, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.DEDUPLICATION);
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .setApprovedSkuMappingInternal(null)
            .setApprovedSkuMappingConfidence(null)
            .setSuggestSkuMapping(null);
        offerMappingRollbackService.startRollback(List.of(offerWithApprovedMapping), Map.of());
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId());

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.REJECTED)
                    .setStaffLogin("rejector")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer rejectedOffer = repository.getOfferById(offerWithApprovedMapping.getId());

        assertThat(rejectedOffer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION);
        assertThat(rejectedOffer.hasRecheckSkuMapping()).isTrue();
        assertThat(rejectedOffer.getRecheckSkuId()).isEqualTo(MAPPING_ID_2);

        response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_2)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("accepter")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        var acceptedOffer = repository.getOfferById(offerWithApprovedMapping.getId());
        assertThat(acceptedOffer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(acceptedOffer.hasApprovedSkuMapping()).isTrue();
        assertThat(acceptedOffer.getApprovedSkuId()).isEqualTo(MAPPING_ID_2);
    }

    @Test
    public void willRollbackFurtherWithoutDuplicates() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        var offerWithApprovedMapping = offerWithApprovedMapping(21)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_2, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        repository.insertOffer(offerWithApprovedMapping);

        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_3, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.DEDUPLICATION);
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_3, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.DEDUPLICATION);
        repository.updateOffers(offerWithApprovedMapping);

        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId());
        offerWithApprovedMapping.setRecheckMappingSource(Offer.RecheckMappingSource.PARTNER);
        offerWithApprovedMapping.setRecheckMappingStatus(Offer.RecheckMappingStatus.NEED_RECHECK);
        offerWithApprovedMapping.setRecheckSkuMapping(offerWithApprovedMapping.getApprovedSkuMapping());
        offersProcessingStatusService.processOffers(List.of(offerWithApprovedMapping));
        repository.updateOffers(offerWithApprovedMapping);

        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId());

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_3)
                    .setStatus(SupplierMappingModerationResult.REJECTED)
                    .setStaffLogin("rejector")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus()).isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId());
        assertThat(offerWithApprovedMapping.hasApprovedSkuMapping()).isFalse();
        assertThat(offerWithApprovedMapping.getRecheckSkuId()).isEqualTo(MAPPING_ID_1);

        response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.REJECTED)
                    .setStaffLogin("rejector")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        var rejectedOffer = repository.getOfferById(offerWithApprovedMapping.getId());
        assertThat(rejectedOffer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION);
        assertThat(rejectedOffer.hasRecheckSkuMapping()).isTrue();
        assertThat(rejectedOffer.getRecheckSkuId()).isEqualTo(MAPPING_ID_2);

        response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_2)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("accepter")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        var acceptedOffer = repository.getOfferById(offerWithApprovedMapping.getId());
        assertThat(acceptedOffer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(acceptedOffer.hasApprovedSkuMapping()).isTrue();
        assertThat(acceptedOffer.getApprovedSkuId()).isEqualTo(MAPPING_ID_2);
    }

    @Test
    public void willResetRecheckedAndStartRollbackOfMappingOnReject() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        var offerWithApprovedMapping = offerWithApprovedMapping(21)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_2, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        repository.insertOffer(offerWithApprovedMapping);

        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        offerWithApprovedMapping
            .setRecheckSkuMapping(offerWithApprovedMapping.getApprovedSkuMapping())
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_RECHECK);
        offersProcessingStatusService.processOffers(List.of(offerWithApprovedMapping));
        repository.updateOffers(offerWithApprovedMapping);

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setStatus(SupplierMappingModerationResult.REJECTED)
                    .setStaffLogin("rejector")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer rejectedOffer = repository.getOfferById(offerWithApprovedMapping.getId());

        assertThat(rejectedOffer.hasApprovedSkuMapping()).isFalse();
        assertThat(rejectedOffer.getApprovedSkuId()).isEqualTo(0L);
        assertThat(rejectedOffer.hasSuggestSkuMapping()).isFalse();
        assertThat(rejectedOffer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION);
        assertThat(rejectedOffer.hasRecheckSkuMapping()).isTrue();
        assertThat(rejectedOffer.getRecheckSkuId()).isEqualTo(MAPPING_ID_2);
    }

    @Test
    public void willAccentNotPublishedModelDuringRollback() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));

        var offerWithApprovedMapping = offerWithApprovedMapping(21)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(INVALID_MAPPING_ID_2, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        repository.insertOffer(offerWithApprovedMapping);

        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.DEDUPLICATION);
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId());
        offerMappingRollbackService.startRollback(List.of(offerWithApprovedMapping), Map.of());
        offerWithApprovedMapping.setApprovedSkuMappingInternal(null)
            .setApprovedSkuMappingConfidence(null);
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId());

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(INVALID_MAPPING_ID_2)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("acceptor")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer acceptedOffer = repository.getOfferById(offerWithApprovedMapping.getId());

        assertThat(acceptedOffer.getApprovedSkuId()).isEqualTo(INVALID_MAPPING_ID_2);
        assertThat(acceptedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(acceptedOffer.getRecheckMappingStatus())
            .isEqualTo(Offer.RecheckMappingStatus.MAPPING_CONFIRMED);
    }

    @Test
    public void willSEndToInProcessFailedToRestoredModelWithSuggestDuringRollback() {
        supplierRepository.insertBatch(Collections.singletonList(
            new Supplier(250, "old pipe")
                .setNewContentPipeline(false)));
        modelStorageCachingServiceMock.markModelNotRestorable(DELETED_MODEL_MAPPING_ID);

        var offerWithApprovedMapping = offerWithApprovedMapping(21)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(DELETED_MODEL_MAPPING_ID, Offer.SkuType.MARKET))
            .setSuggestSkuMapping(OfferTestUtils.mapping(DELETED_MODEL_MAPPING_ID, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        repository.insertOffer(offerWithApprovedMapping);

        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId())
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.DEDUPLICATION);
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId());
        offerMappingRollbackService.startRollback(List.of(offerWithApprovedMapping), Map.of());
        offerWithApprovedMapping.setApprovedSkuMappingInternal(null)
            .setApprovedSkuMappingConfidence(null);
        repository.updateOffers(offerWithApprovedMapping);
        offerWithApprovedMapping = repository.getOfferById(offerWithApprovedMapping.getId());

        MboCategory.SaveMappingModerationResponse response = service
            .saveMappingsModeration(MboCategory.SaveMappingsModerationRequest.newBuilder()
                .addResults(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId("21")
                    .setShopSkuId("Sku21")
                    .setSupplierId(250)
                    .setMarketSkuId(DELETED_MODEL_MAPPING_ID)
                    .setStatus(SupplierMappingModerationResult.ACCEPTED)
                    .setStaffLogin("acceptor")
                    .build())
                .build()
            );
        assertThat(response.getResult().getStatus())
            .isEqualTo(SupplierOffer.OperationStatus.SUCCESS);

        Offer acceptedOffer = repository.getOfferById(offerWithApprovedMapping.getId());

        assertThat(acceptedOffer.hasApprovedSkuMapping()).isFalse();
        assertThat(acceptedOffer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        assertThat(acceptedOffer.hasRecheckSkuMapping()).isFalse();
        assertThat(acceptedOffer.getRecheckMappingStatus()).isNull();
        assertThat(acceptedOffer.getRecheckMappingSource()).isNull();
    }

    private Offer offerWithoutMappings(long id) {
        return new Offer()
            .setId(id)
            .setShopSku("Sku" + id)
            .setCategoryIdForTests(CATEGORY_ID1, Offer.BindingKind.APPROVED)
            .setMappingDestination(Offer.MappingDestination.BLUE)
            .setTitle("Title")
            .setIsOfferContentPresent(true)
            .setShopCategoryName("Category")
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setProcessingCounter(null)
            .setBusinessId(250)
            .addNewServiceOfferIfNotExistsForTests(new Supplier(250, "new pipe"))
            .updateAcceptanceStatusForTests(250, Offer.AcceptanceStatus.OK)
            .markLoadedContent()
            .storeOfferContent(OfferContent.initEmptyContent());
    }

    private Offer offerWithSupplierMapping(long id) {
        return offerWithoutMappings(id)
            .setSupplierSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW);
    }

    private Offer offerWithApprovedMapping(long id) {
        return offerWithoutMappings(id)
            .setSuggestSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
    }

    private Offer offerWithSuggestMapping(long id) {
        return offerWithoutMappings(id)
            .setSuggestSkuMapping(OfferTestUtils.mapping(MAPPING_ID_1, Offer.SkuType.MARKET));
    }

    private List<SupplierOffer.MappingModerationTaskResult> createModerationTaskResults() {
        List<SupplierOffer.MappingModerationTaskResult> moderationTaskResults = new ArrayList<>();
        for (int i = 21; i <= 41; i += 10) {
            String login = i < 31 ? "acceptor" : i < 41 ? "rejector" : "need_infer";
            SupplierMappingModerationResult res = i < 31 ? SupplierMappingModerationResult.ACCEPTED :
                i < 41 ? SupplierMappingModerationResult.REJECTED : SupplierMappingModerationResult.NEED_INFO;
            for (int j = 0; j < 5; j++) {
                moderationTaskResults.add(SupplierOffer.MappingModerationTaskResult.newBuilder()
                    .setOfferId(String.valueOf(i + j))
                    .setShopSkuId("Sku" + (i + j))
                    .setSupplierId(250)
                    .setMarketSkuId(MAPPING_ID_1)
                    .setSkuModifiedTs(Instant.now().toEpochMilli())
                    .setStatus(res)
                    .setStaffLogin(login)
                    .build());
            }
        }
        return moderationTaskResults;
    }

}
