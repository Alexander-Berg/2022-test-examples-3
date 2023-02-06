package ru.yandex.market.mboc.tms.executors;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.golden.GoldenMatrixService;
import ru.yandex.market.mboc.common.honestmark.ClassificationResult;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationCounterService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.DefaultOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.OfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleRepository;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleService;
import ru.yandex.market.mboc.common.offers.acceptance.service.AcceptanceService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.auto_approves.CompositeAutoApproveService;
import ru.yandex.market.mboc.common.services.offers.auto_approves.SuggestAutoApproveServiceImpl;
import ru.yandex.market.mboc.common.services.offers.auto_approves.SupplierAutoApproveServiceImpl;
import ru.yandex.market.mboc.common.services.offers.enrichment.OffersEnrichmentService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.services.ultracontroller.UltraControllerServiceImpl;
import ru.yandex.market.mboc.common.test.YamlTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author yuramalinov
 * @created 14.06.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class EnrichOffersExecutorTest {

    private OfferRepositoryMock repositoryMock;
    private OffersEnrichmentService offersEnrichmentService;
    private OfferMappingActionService offerMappingActionService;
    private UltraControllerService ultraControllerServiceRemote;
    private EnrichOffersExecutor enrichOffersExecutor;
    private SupplierRepositoryMock supplierRepository;
    private NeedContentStatusService needContentStatusService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;
    private StorageKeyValueService storageKeyValueService;
    private SupplierService supplierService;
    private AntiMappingRepositoryMock antiMappingRepositoryMock;
    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;

    private final OfferDestinationCalculator offerDestinationCalculator = new DefaultOfferDestinationCalculator();

    @Before
    public void setup() {
        repositoryMock = Mockito.spy(new OfferRepositoryMock());
        supplierRepository = new SupplierRepositoryMock();
        ultraControllerServiceRemote = Mockito.mock(UltraControllerService.class);
        UltraControllerServiceImpl ultraControllerService = new UltraControllerServiceImpl(
            ultraControllerServiceRemote,
            UltraControllerServiceImpl.DEFAULT_RETRY_COUNT,
            UltraControllerServiceImpl.DEFAULT_RETRY_SLEEP_MS);
        var categoryCachingServiceMock = new CategoryCachingServiceMock();
        categoryCachingServiceMock.addCategory(123L);

        supplierService = new SupplierService(supplierRepository);
        needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        var offerCategoryRestrictionCalculator = Mockito.mock(OfferCategoryRestrictionCalculator.class);
        Mockito.when(offerCategoryRestrictionCalculator
            .calculateClassificationResult(any(UltraController.EnrichedOffer.class), any(Offer.class)))
            .thenReturn(ClassificationResult.UNCONFIDENT_ALLOW_GC);
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            offerCategoryRestrictionCalculator, offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        HonestMarkClassificationService honestMarkClassificationService = new HonestMarkClassificationService(
            Collections.emptySet(),
            categoryCachingServiceMock,
            needContentStatusService, offerCategoryRestrictionCalculator);

        modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();
        var retrieveMappingSkuTypeService = Mockito.mock(RetrieveMappingSkuTypeService.class);
        antiMappingRepositoryMock = new AntiMappingRepositoryMock();
        var categoryInfoRepositoryMock = new CategoryInfoRepositoryMock(new MboUsersRepositoryMock());
        var categoryInfoCache = new CategoryInfoCacheImpl(categoryInfoRepositoryMock);
        var offersProcessingStatusService = new OffersProcessingStatusService(
            null,
            needContentStatusService,
            supplierService,
            categoryKnowledgeService,
            retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepositoryMock,
            antiMappingRepositoryMock,
            offerDestinationCalculator,
            storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService),
            false, false, 3, categoryInfoCache);


        offersEnrichmentService = new OffersEnrichmentService(
            Mockito.mock(GoldenMatrixService.class), ultraControllerService,
            offerMappingActionService, supplierService, categoryKnowledgeService,
            honestMarkClassificationService, Mockito.mock(HonestMarkClassificationCounterService.class),
            Mockito.mock(BooksService.class), offerDestinationCalculator, categoryInfoCache);
        storageKeyValueService = Mockito.spy(new StorageKeyValueServiceMock());

        var categoryVendorRuleService = new CategoryRuleService(
            storageKeyValueService, Mockito.mock(CategoryRuleRepository.class)
        );
        var supplierAutoApproveService = new SupplierAutoApproveServiceImpl(
            modelStorageCachingServiceMock, offerMappingActionService, antiMappingRepositoryMock
        );
        var suggestAutoApproveService = new SuggestAutoApproveServiceImpl(
            categoryInfoRepositoryMock,
            modelStorageCachingServiceMock, offerMappingActionService, antiMappingRepositoryMock
        );
        var compositeAutoApproveService = new CompositeAutoApproveService(
            antiMappingRepositoryMock, supplierAutoApproveService, suggestAutoApproveService
        );
        var acceptanceService = new AcceptanceService(categoryInfoRepositoryMock, categoryCachingServiceMock,
            supplierService,
            false, categoryVendorRuleService, false, offerDestinationCalculator);
        var fastSkuMappingsService = new FastSkuMappingsService(needContentStatusService);
        var applySettingsService = new ApplySettingsService(supplierService,
            acceptanceService, compositeAutoApproveService, offersProcessingStatusService, fastSkuMappingsService);

        enrichOffersExecutor = new EnrichOffersExecutor(offersEnrichmentService,
            repositoryMock,
            storageKeyValueService,
            applySettingsService);
    }

    @Test
    public void testItDoesNotEnrichmentApprovedOffers() {
        Offer offer = YamlTestUtil.readFromResources("tms-offers/minimal-offer.yml", Offer.class);
        repositoryMock.insertOffer(offer);
        offer = repositoryMock.getOfferById(offer.getId()); // refresh

        Mockito.when(ultraControllerServiceRemote.enrich(any())).thenReturn(
            UltraController.DataResponse.newBuilder()
                .addOffers(UltraController.EnrichedOffer.newBuilder()
                    .setCategoryId(123)
                    .setModelId(123)
                    .setVendorId(123)
                    .setMarketSkuId(42)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.APPROVED_SKU)
                    .setMarketCategoryName("")
                    .setMarketModelName("")
                    .setMarketVendorName("")
                    .build())
                .build());

        enrichOffersExecutor.execute();

        Offer updated = repositoryMock.getOfferById(offer.getId());
        assertThat(updated.getLastVersion()).isEqualTo(offer.getLastVersion());
        assertThat(updated.getApprovedSkuMapping()).isNull();
    }

    @Test
    public void testItDoesEnrichmentSuggestedOffers() {
        supplierRepository.insert(new Supplier().setId(43));
        Offer offer = YamlTestUtil.readFromResources("tms-offers/minimal-offer.yml", Offer.class);
        repositoryMock.insertOffer(offer);
        offer = repositoryMock.getOfferById(offer.getId()); // refresh

        Mockito.when(ultraControllerServiceRemote.enrich(any())).thenReturn(
            UltraController.DataResponse.newBuilder()
                .addOffers(UltraController.EnrichedOffer.newBuilder()
                    .setCategoryId(123)
                    .setModelId(123)
                    .setVendorId(123)
                    .setMarketSkuId(42)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.MAIN)
                    .setMarketCategoryName("")
                    .setMarketModelName("")
                    .setMarketVendorName("")
                    .setMarketSkuPublishedOnBlueMarket(true)
                    .setMarketSkuPublishedOnMarket(true)
                    .build())
                .build());

        enrichOffersExecutor.execute();

        Offer updated = repositoryMock.getOfferById(offer.getId());
        assertThat(updated.getLastVersion()).isGreaterThan(offer.getLastVersion());
        assertThat(updated.getSuggestSkuMapping()).isNotNull();
    }

    @Test
    public void testItDoesNotProcessOffersWithApprovedMapping() {
        Offer offer = YamlTestUtil.readFromResources("tms-offers/filled-offer.yml", Offer.class);
        repositoryMock.insertOffer(offer);
        offer = repositoryMock.getOfferById(offer.getId()); // refresh

        enrichOffersExecutor.execute();

        Mockito.verify(storageKeyValueService, Mockito.times(1000)).putValue(
            Mockito.eq(EnrichOffersExecutor.getStorageKeyLastSuccessTime()), any(LocalDateTime.class)
        );

        Offer updated = repositoryMock.getOfferById(offer.getId());
        assertThat(updated.getLastVersion()).isEqualTo(offer.getLastVersion());
    }

    @Test
    public void testBatches() {
        duplicateOffers(202);
        setupEchoUC();

        enrichOffersExecutor.execute();

        ArgumentCaptor<OffersFilter> args = ArgumentCaptor.forClass(OffersFilter.class);
        Mockito.verify(repositoryMock, Mockito.times(1000)).findOffers(args.capture());

        assertThat(args.getAllValues().get(0).getMinIdInclusive()).isEqualTo(0);
        assertThat(args.getAllValues().get(1).getMinIdInclusive()).isEqualTo(110);
        assertThat(args.getAllValues().get(2).getMinIdInclusive()).isEqualTo(210);
        assertThat(args.getAllValues().get(3).getMinIdInclusive()).isEqualTo(212);
        assertThat(args.getAllValues().get(4).getMinIdInclusive()).isEqualTo(0);
    }

    @Test
    public void testBatchesExact() {
        duplicateOffers(200);
        setupEchoUC();

        enrichOffersExecutor.execute();

        ArgumentCaptor<OffersFilter> args = ArgumentCaptor.forClass(OffersFilter.class);
        Mockito.verify(repositoryMock, Mockito.times(1000)).findOffers(args.capture());

        assertThat(args.getAllValues().get(0).getMinIdInclusive()).isEqualTo(0);
        assertThat(args.getAllValues().get(1).getMinIdInclusive()).isEqualTo(110);
        assertThat(args.getAllValues().get(2).getMinIdInclusive()).isEqualTo(210);
        assertThat(args.getAllValues().get(3).getMinIdInclusive()).isEqualTo(0);
    }

    private void duplicateOffers(int i2) {
        Offer offer = YamlTestUtil.readFromResources("tms-offers/minimal-offer.yml", Offer.class);
        for (int i = 0; i < i2; i++) {
            offer.setId(10 + i);
            offer.setShopSku("sku_" + i);
            offer.setTitle("title_" + i);
            offer.setMappingDestination(Offer.MappingDestination.BLUE);
            repositoryMock.insertOffer(offer);
        }
    }

    private void setupEchoUC() {
        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(call -> {
            UltraController.DataRequest request = call.getArgument(0);

            UltraController.DataResponse.Builder builder = UltraController.DataResponse.newBuilder();
            for (UltraController.Offer o : request.getOffersList()) {
                builder.addOffers(UltraController.EnrichedOffer.newBuilder()
                    .setCategoryId(123)
                    .setModelId(123)
                    .setVendorId(123)
                    .setMarketSkuId(o.getShopId())
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.APPROVED_SKU)
                    .setMarketCategoryName("test")
                    .setMarketModelName("test")
                    .setMarketVendorName("test")
                    .build());
            }
            return builder.build();
        });
    }
}
