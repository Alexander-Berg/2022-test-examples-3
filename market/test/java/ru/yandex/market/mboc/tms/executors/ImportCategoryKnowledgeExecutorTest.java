package ru.yandex.market.mboc.tms.executors;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.logisticsparams.repository.SkuLogisticParamsRepository;
import ru.yandex.market.mboc.common.modelform.CachedModelForm;
import ru.yandex.market.mboc.common.modelform.ModelFormCachingService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.offers.repository.OfferStatService;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.ImportCategoryKnowledgeService;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledge;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeRepository;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceImpl;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ImportCategoryKnowledgeExecutorTest extends BaseDbTestClass {

    private static final int SUPPLIER_ID_OLD_PIPE = 1;
    private static final int SUPPLIER_ID_NEW_PIPE = 2;

    private ImportCategoryKnowledgeExecutor importCategoryKnowledgeExecutor;

    @Autowired
    private OfferRepositoryImpl offerRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    private OfferStatService offerStatService;
    private ModelFormCachingService modelFormServiceMock;
    @Autowired
    private CategoryKnowledgeRepository knowledgeRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    private SupplierService supplierService;
    private CategoryCachingServiceMock categoryCachingServiceMock;

    private Map<Long, CachedModelForm> modelFormMap;

    @Before
    public void setUp() throws Exception {
        modelFormServiceMock = categoryIds -> categoryIds.stream()
            .map(id -> modelFormMap.get(id))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(CachedModelForm::getCategoryId, Function.identity()));
        categoryCachingServiceMock = new CategoryCachingServiceMock();

        offerStatService = new OfferStatService(
            namedParameterJdbcTemplate,
            namedParameterJdbcTemplate,
            Mockito.mock(SkuLogisticParamsRepository.class),
            TransactionHelper.MOCK,
            offerRepository, storageKeyValueService);
        offerStatService.subscribe();

        supplierService = new SupplierService(supplierRepository);

        var needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));

        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);

        var modelStorageCachingService = new ModelStorageCachingServiceMock();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(
            modelStorageCachingService, offerBatchProcessor, supplierRepository);

        var categoryKnowledgeService = new CategoryKnowledgeServiceImpl(knowledgeRepository, modelFormServiceMock);

        var offersProcessingStatusService = new OffersProcessingStatusService(
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

        importCategoryKnowledgeExecutor = new ImportCategoryKnowledgeExecutor(
            new ImportCategoryKnowledgeService(offerBatchProcessor,
                offerStatService,
                modelFormServiceMock, knowledgeRepository,
                offersProcessingStatusService)
        );

        // set initial data
        supplierRepository.insertBatch(
            YamlTestUtil.readSuppliersFromResource("category-knowledge/suppliers.yml"));

        List<Offer> offers = YamlTestUtil.readOffersFromResources("category-knowledge/offers.yml");
        for (var offer : offers) {
            offer.addNewServiceOfferIfNotExistsForTests(new Supplier(2, "name"));
            offer.updateAcceptanceStatusForTests(2, Offer.AcceptanceStatus.OK);
        }
        offerRepository.insertOffers(offers);
        offerStatService.updateOfferStat();

        offers.stream().map(Offer::getCategoryId)
            .map(categoryId -> new Category().setCategoryId(categoryId))
            .forEach(categoryCachingServiceMock::addCategory);

        List<CategoryKnowledge> knowledges = YamlTestUtil.readKnowledge("category-knowledge/knowledges.yml");
        knowledgeRepository.insertBatch(knowledges);

        List<CachedModelForm> modelForms = YamlTestUtil.readModelForms("category-knowledge/model-forms.yml");
        modelFormMap = modelForms.stream().collect(Collectors.toMap(CachedModelForm::getCategoryId, Function.identity()));
    }

    @Test
    public void testImportKnowledge() {
        importCategoryKnowledgeExecutor.execute();

        List<CategoryKnowledge> knowledgeList = knowledgeRepository.findAll();
        Assertions.assertThat(knowledgeList)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new CategoryKnowledge(11).setHasKnowledge(true),
                new CategoryKnowledge(12).setHasKnowledge(false),
                new CategoryKnowledge(13).setHasKnowledge(true),
                new CategoryKnowledge(14).setHasKnowledge(false),
                new CategoryKnowledge(15).setHasKnowledge(true),
                new CategoryKnowledge(17).setHasKnowledge(true),
                new CategoryKnowledge(19).setHasKnowledge(true)
            );
    }

    @Test
    public void testOffersWillReopenedIfKnowledgeAppears() {
        Map<Long, Offer> offersMap = offerRepository.findOffers(new OffersFilter()).stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity()));

        importCategoryKnowledgeExecutor.execute();

        List<Offer> offers = offerRepository.findOffers(new OffersFilter());

        MbocAssertions.assertThat(offers)
            .containsExactlyInAnyOrder(
                // часть офферов не должна никак измениться
                offersMap.get(1L),
                offersMap.get(2L),
                offersMap.get(4L),
                offersMap.get(6L),
                offersMap.get(8L),
                // все остальные изменились
                offersMap.get(3L)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN)
                    .setContentComment("Знания появились, можно отправить на заведение")
                    .setTicketCritical(false),
                offersMap.get(5L)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN)
                    .setContentComment("Знания появились, можно отправить на заведение")
                    .setTicketCritical(false),
                offersMap.get(7L)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN)
                    .setContentComment("Знания появились, можно отправить на заведение")
                    .setTicketCritical(false),
                offersMap.get(9L)
                    .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN)
                    .setContentComment("Знания появились, можно отправить на заведение")
                    .setTicketCritical(false)
            );
    }

    @Test
    public void testOffersWillReopenedIfKnowledgeExists() {
        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(
            // old pipeline
            OfferTestUtils.simpleOffer(1)
                .setCategoryIdForTests(11L, Offer.BindingKind.SUGGESTED)
                .setBusinessId(SUPPLIER_ID_OLD_PIPE)
                .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE),
            // new pipeline category with good content
            OfferTestUtils.simpleOffer(2)
                .setCategoryIdForTests(11L, Offer.BindingKind.SUGGESTED)
                .setBusinessId(SUPPLIER_ID_NEW_PIPE)
                .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE),
            OfferTestUtils.simpleOffer(3)
                .setCategoryIdForTests(11L, Offer.BindingKind.SUGGESTED)
                .setBusinessId(SUPPLIER_ID_NEW_PIPE)
                .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE)
                .setSupplierSkuMapping(OfferTestUtils.mapping(1)),
            // new pipeline category without good content
            OfferTestUtils.simpleOffer(4)
                .setCategoryIdForTests(13L, Offer.BindingKind.SUGGESTED)
                .setBusinessId(SUPPLIER_ID_NEW_PIPE)
                .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE));

        knowledgeRepository.deleteAll();
        // Category already having knowledge
        knowledgeRepository.insert(new CategoryKnowledge(11).setHasKnowledge(true));
        knowledgeRepository.insert(new CategoryKnowledge(13).setHasKnowledge(true));
        categoryCachingServiceMock.addCategory(new Category().setCategoryId(11).setAcceptGoodContent(true).setHasKnowledge(true));

        importCategoryKnowledgeExecutor.execute();

        Assertions.assertThat(offerRepository.findOffers(
            new OffersFilter().setOfferIds(1L, 2L, 3L, 4L)
                .setOrderBy(OffersFilter.Field.ID)))
            .extracting(Offer::getProcessingStatus)
            .containsExactly(
                Offer.ProcessingStatus.REOPEN,
                Offer.ProcessingStatus.NEED_CONTENT,
                Offer.ProcessingStatus.NEED_CONTENT,
                Offer.ProcessingStatus.REOPEN);
    }
}
