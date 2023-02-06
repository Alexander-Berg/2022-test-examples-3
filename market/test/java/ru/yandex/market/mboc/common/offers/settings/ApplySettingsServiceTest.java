package ru.yandex.market.mboc.common.offers.settings;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleRepository;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleService;
import ru.yandex.market.mboc.common.offers.acceptance.service.AcceptanceService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepositoryMock;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.auto_approves.CompositeAutoApproveService;
import ru.yandex.market.mboc.common.services.offers.auto_approves.SuggestAutoApproveServiceImpl;
import ru.yandex.market.mboc.common.services.offers.auto_approves.SupplierAutoApproveServiceImpl;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;

public class ApplySettingsServiceTest extends BaseDbTestClass {
    private static final long SEED = -1022017785L;
    private static final long CATEGORY_ID = 666777;
    private static final int BLUE_SUPPLIER_ID = 111;

    private EnhancedRandom random;

    private CategoryInfoRepository categoryInfoRepo;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;
    private SupplierRepository supplierRepo;
    private CategoryCachingServiceMock categoryCache;

    private ApplySettingsService service;

    @Before
    public void setup() {
        categoryInfoRepo = new CategoryInfoRepositoryMock(new MboUsersRepositoryMock());
        var categoryInfoCache = new CategoryInfoCacheImpl(categoryInfoRepo);
        var offerDestinationCalculator = new ContextedOfferDestinationCalculator(categoryInfoCache,
            storageKeyValueService);
        categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        supplierRepo = new SupplierRepositoryMock();
        var supplierService = new SupplierService(supplierRepo);
        categoryCache = new CategoryCachingServiceMock();
        NeedContentStatusService needContentStatusService = new NeedContentStatusService(categoryCache, supplierService,
            new BooksService(categoryCache, Collections.emptySet()));

        var antiMappingRepo = new AntiMappingRepositoryMock();

        var retrieveMappingSkuTypeService = Mockito.mock(RetrieveMappingSkuTypeService.class);
        Mockito.when(retrieveMappingSkuTypeService.retrieveMappingSkuType(anyCollection(), anySet(), any()))
            .then(invocation -> invocation.getArgument(0));

        var legacyOfferMappingActionService =
            new LegacyOfferMappingActionService(null, null, offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);

        var processing = new OffersProcessingStatusService(null, needContentStatusService,
            supplierService, categoryKnowledgeService, retrieveMappingSkuTypeService, offerMappingActionService,
            categoryInfoRepo, antiMappingRepo, offerDestinationCalculator, new StorageKeyValueServiceMock(),
            new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);

        var modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();
        modelStorageCachingServiceMock.setAutoModel(new Model().setId(101L).setTitle("title"));

        var suggestAutoApproveService = new SuggestAutoApproveServiceImpl(categoryInfoRepo,
            modelStorageCachingServiceMock, offerMappingActionService, antiMappingRepo);
        var supplierAutoApproveService = new SupplierAutoApproveServiceImpl(
            modelStorageCachingServiceMock, offerMappingActionService, antiMappingRepo);

        var compositeAutoApproveService = new CompositeAutoApproveService(
            antiMappingRepo, supplierAutoApproveService, suggestAutoApproveService);

        var categoryVendorRuleService = new CategoryRuleService(storageKeyValueService,
            Mockito.mock(CategoryRuleRepository.class));
        var acceptance = new AcceptanceService(categoryInfoRepo, categoryCache, supplierService, false,
            categoryVendorRuleService, true, offerDestinationCalculator);
        var fastSkuMappingsService = new FastSkuMappingsService(needContentStatusService);
        service = new ApplySettingsService(supplierService,
            acceptance, compositeAutoApproveService, processing, fastSkuMappingsService);

        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(SEED)
            .build();
    }

    @Test
    public void testAutoAcceptanceWithProcessing() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        supplierRepo.insert(supplier);
        CategoryInfo categoryInfo = customAcceptanceInfo(CATEGORY_ID, true, CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfoRepo.insertOrUpdate(categoryInfo);
        createCategory(CATEGORY_ID, true, true);

        Offer offer = createOffer(supplier).setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);

        service.applySettingsAndProcess(List.of(offer));

        assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_CONTENT);
    }

    @Test
    public void testManualAcceptanceWithOutFollowingProcessing() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        supplierRepo.insert(supplier);
        CategoryInfo categoryInfo = customAcceptanceInfo(CATEGORY_ID, true, CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfoRepo.insertOrUpdate(categoryInfo);
        createCategory(CATEGORY_ID, true, true);

        Offer offer = createOffer(supplier).setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);

        service.applySettingsAndProcess(List.of(offer));

        assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.NEW);
        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
    }

    @Test
    public void testAutoRejectedAcceptanceWithOutFollowingProcessing() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        supplierRepo.insert(supplier);
        CategoryInfo categoryInfo = customAcceptanceInfo(CATEGORY_ID, true, CategoryInfo.AcceptanceMode.AUTO_REJECT);
        categoryInfoRepo.insertOrUpdate(categoryInfo);
        createCategory(CATEGORY_ID, true, true);

        Offer offer = createOffer(supplier).setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);

        service.applySettingsAndProcess(List.of(offer));

        assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.TRASH);
        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
    }

    private Offer createOffer(Supplier supplier) {
        return random.nextObject(Offer.class, "id")
            .setBusinessId(supplier.getId())
            .addNewServiceOfferIfNotExistsForTests(supplier)
            .updateApprovedSkuMapping(null, null)
            .setRecheckSkuMapping(null)
            .setRecheckMappingSource(null)
            .setRecheckMappingStatus(null)
            .setRecheckClassificationStatus(null)
            .setRecheckCategoryId(null)
            .setRecheckClassificationSource(null)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW);
    }

    private Supplier createSupplier(int id, MbocSupplierType type) {
        return createSupplier(id, type, supplier -> {
        });
    }

    private Supplier createSupplier(int id, MbocSupplierType type, Consumer<Supplier> customizer) {
        Supplier supplier = random.nextObject(Supplier.class, "id");
        supplier.setNewContentPipeline(false);
        supplier.setType(type);
        supplier.setId(id);
        supplier.setBusinessId(id);
        supplier.setFulfillment(true);
        supplier.setCrossdock(false);
        supplier.setDropship(false);
        supplier.setDropshipBySeller(false);
        customizer.accept(supplier);
        supplierRepo.insert(supplier);
        return supplier;
    }

    private void createCategory(long id, boolean hasKnowledge, boolean goodContent) {
        categoryCache.addCategories(new Category().setCategoryId(id)
            .setHasKnowledge(hasKnowledge)
            .setAcceptGoodContent(goodContent)
            .setAcceptContentFromWhiteShops(goodContent));
        if (hasKnowledge) {
            categoryKnowledgeService.addCategory(id);
        } else {
            categoryKnowledgeService.removeCategory(id);
        }
    }


    private CategoryInfo customAcceptanceInfo(long id, boolean manual, CategoryInfo.AcceptanceMode mode) {
        var info = new CategoryInfo(id);
        info.setManualAcceptance(manual);

        info.setFbyAcceptanceMode(mode);
        info.setFbyPlusAcceptanceMode(mode);
        info.setFbsAcceptanceMode(mode);
        info.setDsbsAcceptanceMode(mode);

        return info;
    }
}
