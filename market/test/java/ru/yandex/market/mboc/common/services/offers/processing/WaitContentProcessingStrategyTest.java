package ru.yandex.market.mboc.common.services.offers.processing;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.jooq.tools.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.tracker.IssueMock;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mbo.tracker.utils.IssueStatus;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
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
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.users.StaffServiceMock;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.UserRef;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class WaitContentProcessingStrategyTest extends BaseDbTestClass {
    private static final String DEFAULT_USER = "testDefaultUser";
    private static final long CATEGORY_ID = 777L;
    private static final long CATEGORY_ID2 = 778L;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    private CategoryCachingServiceMock categoryCachingService;
    private TrackerServiceMock trackerServiceMock;
    private WaitContentProcessingStrategy waitContentProcessingStrategy;
    private CategoryInfoRepository categoryInfoRepository;
    private MboUsersRepository mboUsersRepository;
    private StaffServiceMock staffService;
    private CategoryKnowledgeServiceMock categoryKnowledgeServiceMock;
    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;

    private Supplier supplier;

    @Before
    public void setup() {
        categoryCachingService = new CategoryCachingServiceMock();
        trackerServiceMock = Mockito.spy(new TrackerServiceMock());

        mboUsersRepository = new MboUsersRepositoryMock();
        categoryInfoRepository = new CategoryInfoRepositoryMock(mboUsersRepository);

        staffService = new StaffServiceMock();

        supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);

        var supplierService = new SupplierService(supplierRepository);
        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        categoryKnowledgeServiceMock = new CategoryKnowledgeServiceMock();
        modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();
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

        waitContentProcessingStrategy = new WaitContentProcessingStrategy(trackerServiceMock,
            offerRepository,
            supplierRepository,
            categoryInfoRepository,
            mboUsersRepository,
            categoryCachingService,
            staffService,
            offersProcessingStatusService);

        org.springframework.test.util.ReflectionTestUtils.setField(waitContentProcessingStrategy,
            "defaultUser", DEFAULT_USER);
    }


    @Test
    public void shouldCallCreateTicketMethod() {
        categoryCachingService.addCategory(CATEGORY_ID, "Root", 0);

        waitContentProcessingStrategy.createTicket(CATEGORY_ID, List.of(OfferTestUtils.nextOffer()
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)), supplier);

        Mockito.verify(trackerServiceMock, Mockito.times(1))
            .createTicket(Mockito.anyString(), Mockito.anyString(), Mockito.isNull(),
                Mockito.anyList(), Mockito.eq(TicketType.WAIT_CONTENT),
                Mockito.anyMap(), Mockito.anyList(), Mockito.anyCollection(), Mockito.any());
    }

    @Test
    public void shouldAddManagersToFollowers() {
        MboUser categoryManager = new MboUser(20L, "Name", "YaLogin", "StaffLogin");
        MboUser inputManager = new MboUser(21L, "xxx", "asd", "inputManagerLogin");
        mboUsersRepository.insert(categoryManager);
        mboUsersRepository.insert(inputManager);

        CategoryInfo categoryInfo = new CategoryInfo(777).setContentManagerUid(20L).setInputManagerUid(21L);
        categoryInfoRepository.insert(categoryInfo);

        staffService.addApiUser("yang-manager");

        Issue ticket = waitContentProcessingStrategy.createTicket(CATEGORY_ID, List.of(
            OfferTestUtils.nextOffer()
                .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
                .setCommentModifiedBy("yang-manager")
                .setProcessingStatusInternal(Offer.ProcessingStatus.WAIT_CONTENT)),
            supplier);

        Assertions.assertThat(ticket.getAssignee())
            .extracting(UserRef::getLogin)
            .containsExactlyInAnyOrder("StaffLogin");
        Assertions.assertThat(ticket.getFollowers())
            .extracting(UserRef::getLogin)
            .containsExactlyInAnyOrder("inputManagerLogin", "yang-manager");
    }

    @Test
    public void shouldAddDefaultUser() {
        Issue ticket = waitContentProcessingStrategy.createTicket(CATEGORY_ID, List.of(OfferTestUtils.nextOffer()
                .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)),
            supplier);

        Assertions.assertThat(ticket.getAssignee())
            .extracting(UserRef::getLogin)
            .containsExactlyInAnyOrder(DEFAULT_USER);
    }

    @Test
    public void shouldNotFailIfYangManagerHasNoStaffLogin() {
        Issue ticket = waitContentProcessingStrategy.createTicket(CATEGORY_ID, List.of(OfferTestUtils.nextOffer()
                .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
                .setCommentModifiedBy("yang-manager-xxx")),
            supplier);

        Assertions.assertThat(ticket.getFollowers()).isEmpty();
    }

    @Test
    public void testWaitContentTicketIsSetForOffer() {
        Offer offer = OfferTestUtils.simpleOffer().setProcessingStatusInternal(Offer.ProcessingStatus.WAIT_CONTENT);
        offerRepository.insertOffer(offer);

        waitContentProcessingStrategy.createTrackerTickets(new OffersFilter());

        List<Offer> offerList = offerRepository.findAll();
        Assertions.assertThat(offerList).hasSize(1);
        Assertions.assertThat(offerList.get(0).getAdditionalTickets()).isNotEmpty();
        Assertions.assertThat(offerList.get(0).getAdditionalTickets()).containsExactly(
            new AbstractMap.SimpleEntry<>(
                Offer.AdditionalTicketType.WAIT_CONTENT, offerList.get(0).getTrackerTicket()));
    }

    @Test
    public void testAddGoodContentTag() {
        supplierRepository.insert(new Supplier(OfferTestUtils.TEST_SUPPLIER_ID + 1, "Test Supplier")
            .setType(MbocSupplierType.REAL_SUPPLIER)
            .setRealSupplierId("real")
            .setNewContentPipeline(true));
        Offer offer = OfferTestUtils.simpleOffer()
            .setBusinessId(OfferTestUtils.TEST_SUPPLIER_ID + 1)
            .setProcessingStatusInternal(Offer.ProcessingStatus.WAIT_CONTENT);
        offerRepository.insertOffer(offer);

        List<Issue> issues = waitContentProcessingStrategy.createTrackerTickets(new OffersFilter());

        assertThat(issues.size()).isEqualTo(1);
        assertThat(issues.get(0).getTags()).contains("good_content");
    }

    @Test
    public void testWaitContentGroupingOffers() {
        int supplierId = 1234;
        int otherSupplierId = 900;
        supplierRepository.insert(new Supplier(supplierId, "Supplier"));
        supplierRepository.insert(new Supplier(otherSupplierId, "Test Supplier"));

        categoryCachingService.addCategory(CATEGORY_ID, "Category1", 0);
        categoryCachingService.addCategory(CATEGORY_ID2, "Other category", 0);

        List<Offer> offers = List.of(
            offerForWC(CATEGORY_ID, supplierId),
            offerForWC(CATEGORY_ID, supplierId),
            offerForWC(CATEGORY_ID, supplierId),
            offerForWC(CATEGORY_ID2, supplierId),
            offerForWC(CATEGORY_ID2, otherSupplierId),
            offerForWC(CATEGORY_ID2, otherSupplierId)
        );
        offerRepository.insertOffers(offers);

        List<Issue> issues = waitContentProcessingStrategy.createTrackerTickets(new OffersFilter());
        Assertions.assertThat(issues).hasSize(3);

        Map<String, List<Issue>> tickets = issues.stream().collect(Collectors.groupingBy(Issue::getSummary));
        String ticketTitle1 = "Контенту на доработку: Category1 (" + CATEGORY_ID + ")";
        Issue ticket = tickets.get(ticketTitle1).get(0);
        Assert.assertEquals(3,
            StringUtils.countMatches(ticket.getDescription().get(), "OfferTitle" + CATEGORY_ID + supplierId));

        String ticketTitle2 = "Контенту на доработку: Other category (" + CATEGORY_ID2 + ")";
        List<Issue> otherTickets = tickets.get(ticketTitle2);
        Assertions.assertThat(otherTickets).hasSize(2);
    }

    @Test
    public void testWaitContentGroupingOffersDifferentComment() {
        categoryCachingService.addCategory(CATEGORY_ID, "Category1", 0);

        List<Offer> offers = List.of(
            offerForWC(CATEGORY_ID, supplier.getId()).setContentComments(List.of(new ContentComment(
                ContentCommentType.FOR_REVISION, List.of("Comment1")
            ))),
            offerForWC(CATEGORY_ID, supplier.getId()).setContentComments(List.of(new ContentComment(
                ContentCommentType.FOR_REVISION, List.of("Comment2")
            )))
        );
        offerRepository.insertOffers(offers);

        List<Issue> issues = waitContentProcessingStrategy.createTrackerTickets(new OffersFilter());
        Assertions.assertThat(issues).hasSize(2);
    }

    @Test
    public void shouldUpdateStatusAndRemoveComment() {
        Offer offer = offerForWC(CATEGORY_ID, supplier.getId())
            .setContentComments(List.of(new ContentComment(ContentCommentType.FOR_REVISION, List.of("Comment1"))))
            .setTrackerTicket("1");

        offerRepository.insertOffer(offer);
        categoryCachingService.addCategory(CATEGORY_ID);
        categoryKnowledgeServiceMock.addCategory(CATEGORY_ID);

        IssueMock issue = new IssueMock();
        issue.setKey("1");
        issue.setIssueStatus(IssueStatus.RESOLVED);
        waitContentProcessingStrategy.process(issue);

        List<Offer> updatedOffers = offerRepository.findOffers(new OffersFilter()
            .setOfferIds(List.of(offer.getId())));

        Assertions.assertThat(updatedOffers)
            .flatExtracting(Offer::getProcessingStatus)
            .allMatch(status -> status == Offer.ProcessingStatus.IN_CLASSIFICATION);

        Assertions.assertThat(updatedOffers)
            .flatExtracting(Offer::getContentComments)
            .isEmpty();
    }

    private Offer offerForWC(long categoryId, int supplierId) {
        return OfferTestUtils.nextOffer()
            .setCategoryIdForTests(categoryId, Offer.BindingKind.SUGGESTED)
            .setBusinessId(supplierId)
            .setTitle("OfferTitle" + categoryId + supplierId)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setProcessingStatusInternal(Offer.ProcessingStatus.WAIT_CONTENT);
    }
}
