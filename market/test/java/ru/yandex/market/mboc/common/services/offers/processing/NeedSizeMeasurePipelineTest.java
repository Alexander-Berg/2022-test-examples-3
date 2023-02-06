package ru.yandex.market.mboc.common.services.offers.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendor;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendorBuilder;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendorRepositoryImpl;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.queue.OfferQueueService;
import ru.yandex.market.mboc.common.services.offers.ticket.forming.NeedSizeMeasureTicketDataFormer;
import ru.yandex.market.mboc.common.services.proto.SizeMeasureHelper;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.CategorySizeMeasureServiceStub;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingServiceMock;
import ru.yandex.market.mboc.common.vendor.models.CachedGlobalVendor;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

@SuppressWarnings("checkstyle:magicnumber")
public class NeedSizeMeasurePipelineTest extends BaseDbTestClass {

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    @Qualifier("createSizeMeasureTickets")
    private OfferQueueService queueService;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private AntiMappingRepository antiMappingRepository;

    private TrackerServiceMock trackerServiceMock;
    private GlobalVendorsCachingServiceMock globalVendorsCachingService;

    private NeedSizeMeasureTicketDataFormer needSizeMeasureTicketDataFormer;
    private CategorySupplierVendorRepositoryImpl categorySupplierVendorRepository;

    private CategorySizeMeasureServiceStub categorySizeMeasureService;
    private SizeMeasureHelper sizeMeasureHelper;
    private NeedSizeMeasurePipeline needSizeMeasurePipeline;
    private final AtomicLong idGenerator = new AtomicLong(0);
    private NeedSizeMeasureFilter needSizeMeasureFilter;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private CategoryKnowledgeServiceMock categoryKnowledgeServiceMock;
    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;

    @Before
    public void init() {
        trackerServiceMock = Mockito.spy(new TrackerServiceMock());
        globalVendorsCachingService = new GlobalVendorsCachingServiceMock();
        categoryCachingServiceMock = new CategoryCachingServiceMock();

        supplierRepository.insert(new Supplier(1, "first"));
        supplierRepository.insert(new Supplier(2, "second"));
        supplierRepository.insert(new Supplier(42, "42"));

        needSizeMeasureTicketDataFormer = new NeedSizeMeasureTicketDataFormer(supplierRepository,
            globalVendorsCachingService, categoryCachingServiceMock);

        categorySizeMeasureService = new CategorySizeMeasureServiceStub();

        sizeMeasureHelper = new SizeMeasureHelper(categorySizeMeasureService, categorySizeMeasureService);
        needSizeMeasureFilter = new NeedSizeMeasureFilter(sizeMeasureHelper, offerRepository);
        categorySupplierVendorRepository = Mockito.mock(CategorySupplierVendorRepositoryImpl.class);

        var supplierService = new SupplierService(supplierRepository);
        var needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        categoryKnowledgeServiceMock = new CategoryKnowledgeServiceMock();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingServiceMock,
            offerBatchProcessor, supplierRepository);
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(
            needContentStatusService, null, offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var offersProcessingStatusService = new OffersProcessingStatusService(offerBatchProcessor,
            needContentStatusService, supplierService, categoryKnowledgeServiceMock, retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator, storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);

        needSizeMeasurePipeline = new NeedSizeMeasurePipeline(offerRepository, offerBatchProcessor, trackerServiceMock,
            categorySupplierVendorRepository, needSizeMeasureTicketDataFormer, sizeMeasureHelper,
            needSizeMeasureFilter, offersProcessingStatusService);

        ReflectionTestUtils.setField(needSizeMeasureTicketDataFormer, "defaultFollower", "defaultFollower");
        ReflectionTestUtils.setField(needSizeMeasureTicketDataFormer, "mbocUrl", "mbocUrl");
        ReflectionTestUtils.setField(needSizeMeasureTicketDataFormer, "mboUrl", "mboUrl");
        ReflectionTestUtils.setField(needSizeMeasurePipeline, "queue", "queue");
    }

    private void initProtoApiBasedOnOffersCategories() {
        Map<Long, List<MboSizeMeasures.SizeMeasure>> sizeMeasuresByCategoryId = offerRepository.getOffersCategories()
            .stream()
            .distinct()
            .sorted()
            .collect(Collectors.toMap(el -> el, el -> {
                long id = idGenerator.incrementAndGet();
                return Collections.singletonList(MboSizeMeasures.SizeMeasure.newBuilder()
                    .setId(id)
                    .setName("name " + id)
                    .setValueParamId(id)
                    .setUnitParamId(id)
                    .setNumericParamId(id)
                    .build());
            }));
        categorySizeMeasureService.initializeSizeMeasures(sizeMeasuresByCategoryId);
    }

    @Test
    public void shouldCreateTicketWithTwoOffers() {
        List<Offer> allOffers = YamlTestUtil.readOffersFromResources(
            "offers/need-size-measure/tracker-offers-for-need-size-measure.yml");
        offerRepository.insertOffers(allOffers);
        initProtoApiBasedOnOffersCategories();

        queueService.handleQueueBatch(needSizeMeasurePipeline::createOrUpdateTickets);

        Mockito.verify(trackerServiceMock, times(1))
            .createTicket(Mockito.anyString(), Mockito.anyString(), Mockito.isNull(),
                Mockito.anyList(), Mockito.eq(TicketType.NEED_SIZE_MEASURE), Mockito.anyMap(),
                Mockito.anyList(), Mockito.anyCollection(), any());
        Mockito.verify(trackerServiceMock, times(1))
            .commentTicket(any(), Mockito.anyString());
        Mockito.verify(trackerServiceMock, times(1))
            .updateTicketsDescription(any(), Mockito.anyString());
    }

    @Test
    public void shouldUpdateOfferCommentIfAbsent() {
        List<Offer> allOffers = YamlTestUtil.readOffersFromResources(
            "offers/need-size-measure/tracker-offers-for-need-size-measure.yml");
        Assertions.assertThat(allOffers).hasSize(3);
        ContentComment customComment = new ContentComment(ContentCommentType.NO_SIZE_MEASURE, "Куку!");
        allOffers.get(0).setContentComments(customComment);

        globalVendorsCachingService.addVendor(new CachedGlobalVendor(1, "Укулеле"));
        offerRepository.insertOffers(allOffers);
        initProtoApiBasedOnOffersCategories();

        queueService.handleQueueBatch(needSizeMeasurePipeline::createOrUpdateTickets);

        Map<Long, Offer> currentOffers = offerRepository.getOffersByIds(
            allOffers.stream().map(Offer::getId).collect(Collectors.toList()))
            .stream()
            .collect(Collectors.toMap(o -> o.getId(), o -> o));

        ContentComment offer1Comment = new ContentComment(ContentCommentType.NO_SIZE_MEASURE,
            "У производителя Укулеле для категории Все товары");

        Assertions.assertThat(currentOffers.get(allOffers.get(0).getId()).getContentComments())
            .containsExactly(customComment);
        Assertions.assertThat(currentOffers.get(allOffers.get(1).getId()).getContentComments())
            .containsExactly(offer1Comment);
    }

    @Test
    public void whenTicketWithOfferAlreadyExistShouldUpdateOldTicketAndCreateTicketForOtherOffer() {
        Offer offerThatWillBeUpdated = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setVendorId(1)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_SIZE_MEASURE)
            .setBusinessId(1);

        offerRepository.insertOffer(offerThatWillBeUpdated);
        initProtoApiBasedOnOffersCategories();

        //create ticket that will be updated
        queueService.handleQueueBatch(needSizeMeasurePipeline::createOrUpdateTickets);

        List<Offer> newOffers = Arrays.asList(
            OfferTestUtils.nextOffer()
                .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
                .setVendorId(1)
                .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_SIZE_MEASURE)
                .setBusinessId(1),
            OfferTestUtils.nextOffer()
                .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
                .setVendorId(2)
                .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_SIZE_MEASURE)
                .setBusinessId(2)
        );
        offerRepository.insertOffers(newOffers);

        queueService.handleQueueBatch(needSizeMeasurePipeline::createOrUpdateTickets);

        Mockito.verify(trackerServiceMock, times(2))
            .createTicket(Mockito.anyString(), Mockito.anyString(), Mockito.isNull(),
                Mockito.anyList(), Mockito.eq(TicketType.NEED_SIZE_MEASURE), Mockito.anyMap(),
                Mockito.anyList(), Mockito.anyCollection(), any());
        Mockito.verify(trackerServiceMock, times(3))
            .commentTicket(any(), Mockito.anyString());
        Mockito.verify(trackerServiceMock, times(2))
            .updateTicketsDescription(any(), Mockito.anyString());
    }

    @Test
    public void whenCategorySupplierVendorAndSizeMeasureExistShouldMoveToStateAlive() {
        long firstCategoryId = 1L;
        int firstVendorId = 1;
        int firstSupplierId = 2;
        long secondCategoryId = 2L;
        int secondVendorId = 1;
        int secondSupplierId = 2;
        Mockito.when(categorySupplierVendorRepository.findSuspendedCategoryVendors())
            .thenReturn(Arrays.asList(
                CategorySupplierVendorBuilder.newBuilder()
                    .categoryId(firstCategoryId)
                    .supplierId(firstSupplierId)
                    .vendorId(firstVendorId)
                    .state(CategorySupplierVendor.State.SUSPENDED)
                    .build(),
                CategorySupplierVendorBuilder.newBuilder()
                    .categoryId(secondCategoryId)
                    .supplierId(secondSupplierId)
                    .vendorId(secondVendorId)
                    .state(CategorySupplierVendor.State.SUSPENDED)
                    .build()));

        Map<Long, List<MboSizeMeasures.ScaleInfo>> scalesByCategoryId = ImmutableMap.of(
            firstCategoryId, Collections.singletonList(MboSizeMeasures.ScaleInfo.newBuilder()
                .setScaleId(1L)
                .setVendorId(firstVendorId)
                .build()),
            secondCategoryId, Collections.singletonList(MboSizeMeasures.ScaleInfo.newBuilder()
                .setScaleId(1L)
                .setVendorId(secondVendorId)
                .build())
        );

        categorySizeMeasureService.initializeScaleInfos(scalesByCategoryId);

        needSizeMeasurePipeline.returnToWorkOffersAndLocksWithSizeMeasures();

        ArgumentCaptor<CategorySupplierVendor> captor = ArgumentCaptor.forClass(CategorySupplierVendor.class);

        Mockito.verify(categorySupplierVendorRepository,
            times(2)).update(captor.capture());

        Assertions.assertThat(captor.getAllValues())
            .extracting(CategorySupplierVendor::getState)
            .allMatch(state -> state == CategorySupplierVendor.State.ALIVE);
    }

    @Test
    public void shouldUpdateOfferStatusAndCloseTicket() {
        Mockito.when(categorySupplierVendorRepository.findSuspendedCategoryVendors())
            .thenReturn(Collections.emptyList());

        Issue issueWhereOffersWillBeClosed = trackerServiceMock
            .createTicket("", "", "", needSizeMeasurePipeline.getType(), null);
        Issue issueWhereOfferWithoutMeasure = trackerServiceMock
            .createTicket("", "", "", needSizeMeasurePipeline.getType(), null);
        Issue issueWithOfferStillWaitingSizeMeasure = trackerServiceMock
            .createTicket("", "", "", needSizeMeasurePipeline.getType(), null);
        List<Offer> allOffers = YamlTestUtil.readOffersFromResources(
            "offers/need-size-measure/move-need-size-measure-to-be-in-process.yml");
        categoryKnowledgeServiceMock.addCategory(1L);
        categoryKnowledgeServiceMock.addCategory(2L);

        categoryCachingServiceMock.addCategory(new Category().setCategoryId(1L).setHasKnowledge(true));
        categoryCachingServiceMock.addCategory(new Category().setCategoryId(2L).setHasKnowledge(true));

        allOffers.forEach(offer -> {
            offer.setIsOfferContentPresent(true);
            switch ((int) offer.getId()) {
                case 2:
                    offer.setTrackerTicket(issueWhereOfferWithoutMeasure.getKey());
                case 4:
                    offer.setTrackerTicket(issueWithOfferStillWaitingSizeMeasure.getKey());
                default:
                    offer.setTrackerTicket(issueWhereOffersWillBeClosed.getKey());
            }
        });


        offerRepository.insertOffers(allOffers);
        Map<Long, List<MboSizeMeasures.ScaleInfo>> scalesByCategoryId = ImmutableMap.of(
            1L, Collections.singletonList(MboSizeMeasures.ScaleInfo.newBuilder()
                .setScaleId(1L)
                .setVendorId(1)
                .build())
        );
        categorySizeMeasureService.initializeScaleInfos(scalesByCategoryId);

        needSizeMeasurePipeline.returnToWorkOffersAndLocksWithSizeMeasures();

        //закрываем 2 потому что в одном были заведены все размерные сетки, а в другом категория была не размерной
        Mockito.verify(trackerServiceMock, times(2))
            .closeAndCommentTicket(any(), anyString());

        List<Offer> offers = offerRepository.findAll();

        Assertions.assertThat(offers)
            .extracting(Offer::getProcessingStatus)
            .containsExactlyInAnyOrder(Offer.ProcessingStatus.IN_CLASSIFICATION, Offer.ProcessingStatus.IN_CLASSIFICATION,
                Offer.ProcessingStatus.IN_CLASSIFICATION, Offer.ProcessingStatus.NEED_SIZE_MEASURE);
    }

    @Test
    public void whenMeasureCategoryWithoutSizeMeasureShouldSetOfferStatusToBeNeedSizeMeasure() {
        offerRepository.findAll();
        long withoutMeasureCategoryId = 1L;
        long withoutSizeMeasureCategoryId = 2L;
        long withSizeMeasureCategoryId = 3L;
        int vendorIdWithoutSizeMeasure = 1;
        int vendorWithSizeMeasure1 = 2;
        int vendorWithSizeMeasure2 = 3;
        List<Offer> offers = Arrays.asList(OfferTestUtils.simpleOffer()
                .setCategoryIdForTests(withoutMeasureCategoryId, Offer.BindingKind.SUGGESTED)
                .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS),
            OfferTestUtils.nextOffer()
                .setVendorId(5)
                .setCategoryIdForTests(withoutSizeMeasureCategoryId, Offer.BindingKind.SUGGESTED)
                .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS),
            OfferTestUtils.nextOffer()
                .setVendorId(4)
                .setCategoryIdForTests(withoutSizeMeasureCategoryId, Offer.BindingKind.SUGGESTED)
                .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS),
            OfferTestUtils.nextOffer()
                .setCategoryIdForTests(withSizeMeasureCategoryId, Offer.BindingKind.SUGGESTED)
                .setVendorId(vendorWithSizeMeasure1)
                .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS),
            OfferTestUtils.nextOffer()
                .setCategoryIdForTests(withSizeMeasureCategoryId, Offer.BindingKind.SUGGESTED)
                .setVendorId(vendorWithSizeMeasure2)
                .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS));

        offerRepository.insertOffers(offers);

        Map<Long, List<MboSizeMeasures.ScaleInfo>> scalesByCategoryId = ImmutableMap.of(
            withoutSizeMeasureCategoryId, Collections.singletonList(MboSizeMeasures.ScaleInfo.newBuilder()
                .setScaleId(1L)
                .setVendorId(vendorIdWithoutSizeMeasure)
                .build()),
            withSizeMeasureCategoryId, Arrays.asList(MboSizeMeasures.ScaleInfo.newBuilder()
                    .setScaleId(2L)
                    .setVendorId(vendorWithSizeMeasure1)
                    .build(),
                MboSizeMeasures.ScaleInfo.newBuilder()
                    .setScaleId(3L)
                    .setVendorId(vendorWithSizeMeasure2)
                    .build()));

        categorySizeMeasureService.initializeScaleInfos(scalesByCategoryId);

        needSizeMeasurePipeline.filterInProcessOffers();

        List<Offer> processedOffers = offerRepository.findAll();

        Assertions.assertThat(processedOffers).extracting(Offer::getProcessingStatus)
            .filteredOn(status -> status == Offer.ProcessingStatus.IN_PROCESS)
            .size()
            .isEqualTo(3);
        Assertions.assertThat(processedOffers).extracting(Offer::getProcessingStatus)
            .filteredOn(status -> status == Offer.ProcessingStatus.NEED_SIZE_MEASURE)
            .size()
            .isEqualTo(2);
    }
}
