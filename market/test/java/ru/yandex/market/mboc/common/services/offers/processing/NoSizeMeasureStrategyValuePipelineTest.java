package ru.yandex.market.mboc.common.services.offers.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.mbo.tracker.IssueMock;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mbo.tracker.utils.IssueStatus;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.queue.OfferQueueService;
import ru.yandex.market.mboc.common.services.proto.SizeMeasureHelper;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.CategorySizeMeasureServiceStub;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingServiceMock;
import ru.yandex.startrek.client.model.Issue;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class NoSizeMeasureStrategyValuePipelineTest extends BaseDbTestClass {

    private static final int NEW_PIPELINE_SUPPLIER_ID = 3;
    private static final long CATEGORY_ID = 12;

    @Autowired
    private OfferRepository offerRepositoryAutowired;
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    @Qualifier("createNoSizeMeasureValueTickets")
    private OfferQueueService offerQueueService;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private AntiMappingRepository antiMappingRepository;

    private TrackerServiceMock trackerServiceMock;
    private NoSizeMeasureValueStrategy noSizeMeasureValueStrategy;
    private CategorySizeMeasureServiceStub sizeMeasureService;
    private GlobalVendorsCachingService globalVendorsCachingService;
    private SizeMeasureHelper sizeMeasureHelper;
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Before
    public void init() {
        offerRepository = Mockito.spy(offerRepositoryAutowired);
        trackerServiceMock = Mockito.spy(new TrackerServiceMock());
        sizeMeasureService = new CategorySizeMeasureServiceStub();
        globalVendorsCachingService = new GlobalVendorsCachingServiceMock();
        sizeMeasureHelper = new SizeMeasureHelper(sizeMeasureService, sizeMeasureService);
        var supplierService = new SupplierService(supplierRepository);
        var categoryCachingService = new CategoryCachingServiceMock();
        categoryCachingService.addCategory(CATEGORY_ID);
        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        var categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        categoryKnowledgeService.addCategory(CATEGORY_ID);
        var modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();
        modelStorageCachingServiceMock.setAutoModel(new Model());
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingServiceMock,
            offerBatchProcessor, supplierRepository);
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService, null,
            offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
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
            false, false, 3, categoryInfoCache);

        noSizeMeasureValueStrategy = new NoSizeMeasureValueStrategy(offerRepository, trackerServiceMock,
            supplierRepository, globalVendorsCachingService, sizeMeasureHelper, offersProcessingStatusService);

        supplierRepository.insertBatch(
            new Supplier(1, "Old pipeline 1"),
            new Supplier(2, "Old pipeline 2"),
            new Supplier(NEW_PIPELINE_SUPPLIER_ID, "New pipeline")
                .setNewContentPipeline(true),
            OfferTestUtils.simpleSupplier()
        );

        List<Offer> offers =
            YamlTestUtil.readOffersFromResources(
                "offers/no-size-measure/tracker-offers-for-no-size-measure.yml");
        offerRepository.insertOffers(offers);

        ReflectionTestUtils.setField(noSizeMeasureValueStrategy, "mailList", "mailList");
        ReflectionTestUtils.setField(noSizeMeasureValueStrategy, "defaultFollower", "defaultFollower");
        ReflectionTestUtils.setField(noSizeMeasureValueStrategy, "queue", "queue");
        ReflectionTestUtils.setField(noSizeMeasureValueStrategy, "mbocUrl", "mbocUrl");
        ReflectionTestUtils.setField(noSizeMeasureValueStrategy, "mboUrl", "mboUrl");
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
        sizeMeasureService.initializeSizeMeasures(sizeMeasuresByCategoryId);
    }

    @Test
    public void shouldCreateFourTickets() {
        initProtoApiBasedOnOffersCategories();

        offerQueueService.handleQueueBatch(noSizeMeasureValueStrategy::createTrackerTicket);

        Mockito.verify(trackerServiceMock, Mockito.times(4))
            .createTicket(Mockito.anyString(), Mockito.anyString(), Mockito.isNull(),
                Mockito.anyList(), Mockito.eq(TicketType.NO_SIZE_MEASURE_VALUE), Mockito.anyMap(),
                Mockito.anyList(), Mockito.anyCollection(), Mockito.any());

        Mockito.verify(trackerServiceMock, Mockito.times(4))
            .updateTicketsDescription(Mockito.any(Issue.class), Mockito.anyString());
    }

    @Test
    public void whenCategoryAbsentShouldCreateFourTicketsOnUnknownSizeMeasure() {
        List<Issue> tickets =
            noSizeMeasureValueStrategy.createOrUpdateTrackerTickets(new OffersFilter());

        Mockito.verify(trackerServiceMock, Mockito.times(3))
            .createTicket(Mockito.anyString(), Mockito.anyString(), Mockito.isNull(),
                Mockito.anyList(), Mockito.eq(TicketType.NO_SIZE_MEASURE_VALUE), Mockito.anyMap(),
                Mockito.anyList(), Mockito.anyCollection(), Mockito.any());

        Mockito.verify(trackerServiceMock, Mockito.times(3))
            .updateTicketsDescription(Mockito.any(Issue.class), Mockito.anyString());

        // все недостающие size measure в категории заменяются на дефолтную "отсутствующую" меру
        Assertions.assertThat(tickets).flatExtracting("tags").contains("size-measure_0");
    }

    @Test
    public void whenSizeMeasuresIsEmptyAbsentShouldCreateFourTicketsOnUnknownSizeMeasure() {
        Map<Long, List<MboSizeMeasures.SizeMeasure>> sizeMeasuresByCategoryId = offerRepository.getOffersCategories()
            .stream()
            .distinct()
            .collect(Collectors.toMap(el -> el, categoryId -> {
                long id = idGenerator.incrementAndGet();
                return categoryId == 1 ? ImmutableList.of() :
                    Collections.singletonList(
                        MboSizeMeasures.SizeMeasure.newBuilder()
                            .setId(id)
                            .setName("name " + id)
                            .setValueParamId(id)
                            .setUnitParamId(id)
                            .setNumericParamId(id)
                            .build());
            }));
        sizeMeasureService.initializeSizeMeasures(sizeMeasuresByCategoryId);
        List<Issue> tickets = noSizeMeasureValueStrategy.createOrUpdateTrackerTickets(new OffersFilter());

        Mockito.verify(trackerServiceMock, Mockito.times(4))
            .createTicket(Mockito.anyString(), Mockito.anyString(), Mockito.isNull(),
                Mockito.anyList(), Mockito.eq(TicketType.NO_SIZE_MEASURE_VALUE), Mockito.anyMap(),
                Mockito.anyList(), Mockito.anyCollection(), Mockito.any());

        Mockito.verify(trackerServiceMock, Mockito.times(4))
            .updateTicketsDescription(Mockito.any(Issue.class), Mockito.anyString());

        // все пустые size measures в категории заменяются на дефолтную "отсутствующую" меру
        Assertions.assertThat(tickets).flatExtracting("tags").containsOnlyOnce("size-measure_0");
    }

    @Test
    public void shouldCreateSixAndUpdateOneTickets() {
        initProtoApiBasedOnOffersCategories();

        List<Issue> newTickets =
            noSizeMeasureValueStrategy.createOrUpdateTrackerTickets(new OffersFilter());
        Issue ticketThatWillBeUpdated = newTickets.stream()
            .filter(issue -> issue.getTags().containsAllTs(Arrays.asList("size-measure_2", "vendor_1", "supplier_1")))
            .collect(Collectors.toList()).get(0);
        List<Offer> offers =
            YamlTestUtil.readOffersFromResources(
                "offers/no-size-measure/tracker-offers-for-no-size-measure-update.yml");
        offers.stream()
            .filter(offer -> Objects.equals(offer.getVendorId(), 1) &&
                offer.getBusinessId() == 1 &&
                Objects.equals(offer.getCategoryId(), 12L))
            .forEach(offer -> offer.setTrackerTicket(ticketThatWillBeUpdated.getKey()));

        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(offers);

        offerQueueService.handleQueueBatch(noSizeMeasureValueStrategy::createTrackerTicket);

        // 4 тикета создается при первой обработке тикетов и еще 1 после второй загрузки
        Mockito.verify(trackerServiceMock, Mockito.times(5))
            .createTicket(Mockito.anyString(), Mockito.anyString(), Mockito.isNull(),
                Mockito.anyList(), Mockito.eq(TicketType.NO_SIZE_MEASURE_VALUE), Mockito.anyMap(),
                Mockito.anyList(), Mockito.anyCollection(), Mockito.any());

        Mockito.verify(trackerServiceMock, Mockito.times(5))
            .updateTicketsDescription(Mockito.any(Issue.class), Mockito.anyString());

        Mockito.verify(trackerServiceMock, Mockito.times(5))
            .summonMaillistInTicket(Mockito.any(Issue.class), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void shouldUpdateOfferStatus() {
        List<Offer> offers =
            YamlTestUtil.readOffersFromResources(
                "offers/no-size-measure/tracker-offers-for-completion-no-size-measure.yml");
        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(offers);

        IssueMock issue = new IssueMock();
        issue.setKey("1");
        issue.setIssueStatus(IssueStatus.RESOLVED);

        List<Offer> offersBefore = offerRepository.findOffers(new OffersFilter()
            .setTrackerTickets(issue.getKey()));
        Assertions.assertThat(offersBefore)
            .flatExtracting(Offer::getProcessingStatus)
            .allMatch(status -> status == Offer.ProcessingStatus.NO_SIZE_MEASURE_VALUE_);
        List<Long> ids = offersBefore.stream().map(Offer::getId).collect(Collectors.toList());

        noSizeMeasureValueStrategy.process(issue);

        List<Offer> offersAfter = offerRepository.findOffers(new OffersFilter().setOfferIds(ids));

        Assertions.assertThat(offersAfter)
            .flatExtracting(Offer::getProcessingStatus)
            .allMatch(status -> status == Offer.ProcessingStatus.IN_CLASSIFICATION);
    }

    @Test
    public void shouldAddGoodContentTag() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setBusinessId(NEW_PIPELINE_SUPPLIER_ID)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_SIZE_MEASURE_VALUE_);
        offerRepository.insertOffers(offer);

        List<Issue> allIssues = noSizeMeasureValueStrategy
            .createOrUpdateTrackerTickets(new OffersFilter());

        List<Issue> newPipelineSupplierIssues = allIssues.stream()
            .filter(issue -> issue.getSummary().contains("поставщик 3 New pipeline"))
            .collect(Collectors.toList());

        assertThat(newPipelineSupplierIssues.size()).isEqualTo(1);
        assertThat(newPipelineSupplierIssues.get(0).getTags()).contains("good_content");
    }

    @Test
    public void shouldAutoCloseTicketsWhenNoRelevantOffers() {
        IssueMock ticket = new IssueMock()
            .setKey("1")
            .setIssueStatus(IssueStatus.OPEN);

        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(
            OfferTestUtils.nextOffer()
                .setProcessingStatusInternal(Offer.ProcessingStatus.AUTO_PROCESSED)
                .setTrackerTicket(ticket)
                .setContentLabState(Offer.ContentLabState.CL_WAIT_TICKET)
                .setTrackerTicketFromClab(ticket.getKey()),

            OfferTestUtils.nextOffer()
                .setProcessingStatusInternal(Offer.ProcessingStatus.AUTO_PROCESSED)
                .setTrackerTicket(ticket)
                .setContentLabState(Offer.ContentLabState.CL_WAIT_TICKET)
                .setTrackerTicketFromClab(ticket.getKey())
        );

        noSizeMeasureValueStrategy.process(ticket);

        Assertions.assertThat(ticket.getIssueStatus()).isEqualTo(IssueStatus.CLOSE);
    }

    @Test
    public void shouldDontAutoCloseTicketsWhenHasRelevantOffers() {
        IssueMock ticket = new IssueMock()
            .setKey("1")
            .setIssueStatus(IssueStatus.OPEN);

        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(
            OfferTestUtils.nextOffer()
                .setProcessingStatusInternal(Offer.ProcessingStatus.NO_SIZE_MEASURE_VALUE_)
                .setTrackerTicket(ticket)
                .setContentLabState(Offer.ContentLabState.CL_WAIT_TICKET)
                .setTrackerTicketFromClab(ticket.getKey()),

            OfferTestUtils.nextOffer()
                .setProcessingStatusInternal(Offer.ProcessingStatus.AUTO_PROCESSED)
                .setTrackerTicket(ticket)
                .setContentLabState(Offer.ContentLabState.CL_WAIT_TICKET)
                .setTrackerTicketFromClab(ticket.getKey())
        );

        noSizeMeasureValueStrategy.process(ticket);

        Assertions.assertThat(ticket.getIssueStatus()).isEqualTo(IssueStatus.OPEN);
    }
}
