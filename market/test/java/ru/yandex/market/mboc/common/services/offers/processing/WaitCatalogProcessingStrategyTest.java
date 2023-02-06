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
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.services.users.StaffServiceMock;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.UserRef;

public class WaitCatalogProcessingStrategyTest extends BaseDbTestClass {
    private static final String DEFAULT_USER = "testDefaultUser";
    private static final String DEFAULT_USER_FOR_MODERATION = "Liza";
    private static final long CATEGORY_ID = 1;
    private static final long CATEGORY_ID2 = 42;
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
    private WaitCatalogProcessingStrategy waitCatalogProcessingStrategy;
    private CategoryInfoRepository categoryInfoRepository;
    private MboUsersRepository mboUsersRepository;
    private StaffServiceMock staffService;
    private Supplier supplier;
    private CategoryKnowledgeServiceMock categoryKnowledgeServiceMock;
    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;

    @Before
    public void setup() {
        categoryCachingService = new CategoryCachingServiceMock();
        trackerServiceMock = Mockito.spy(new TrackerServiceMock());

        mboUsersRepository = new MboUsersRepositoryMock();
        categoryInfoRepository = new CategoryInfoRepositoryMock(mboUsersRepository);

        staffService = new StaffServiceMock();
        supplier = OfferTestUtils.simpleSupplier().setType(MbocSupplierType.THIRD_PARTY);
        supplierRepository.insert(supplier);

        var supplierService = new SupplierService(supplierRepository);
        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        categoryKnowledgeServiceMock = new CategoryKnowledgeServiceMock();
        modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingServiceMock,
            offerBatchProcessor, supplierRepository);
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(
            needContentStatusService, null, offerDestinationCalculator, new StorageKeyValueServiceMock());
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var offersProcessingStatusService = new OffersProcessingStatusService(offerBatchProcessor,
            needContentStatusService, supplierService, categoryKnowledgeServiceMock, retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator, storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);

        waitCatalogProcessingStrategy = new WaitCatalogProcessingStrategy(trackerServiceMock,
            offerRepository,
            supplierRepository,
            categoryInfoRepository,
            mboUsersRepository,
            categoryCachingService,
            staffService,
            offersProcessingStatusService);

        org.springframework.test.util.ReflectionTestUtils.setField(waitCatalogProcessingStrategy,
            "defaultUser", DEFAULT_USER);
        org.springframework.test.util.ReflectionTestUtils.setField(waitCatalogProcessingStrategy,
            "defaultModerationUser", DEFAULT_USER_FOR_MODERATION);
    }


    @Test
    public void shouldCallCreateTicketMethod() {
        categoryCachingService.addCategory(CATEGORY_ID, "Root", 0);

        Issue ticket = waitCatalogProcessingStrategy
            .createTicket(CATEGORY_ID, List.of(OfferTestUtils.nextOffer().setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)), supplier);

        Mockito.verify(trackerServiceMock, Mockito.times(1))
            .createTicket(Mockito.anyString(), Mockito.anyString(), Mockito.isNull(),
                Mockito.anyList(), Mockito.eq(TicketType.WAIT_CATALOG),
                Mockito.anyMap(), Mockito.anyList(), Mockito.anyCollection(), Mockito.any());

        Assert.assertEquals("Менеджеру на доработку: Root (1), 3P test (42).",
            ticket.getSummary());
    }

    @Test
    public void shouldAddDefaultUser() {
        Issue ticket = waitCatalogProcessingStrategy.createTicket(CATEGORY_ID, List.of(OfferTestUtils.nextOffer()
                .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)),
            supplier);

        Assertions.assertThat(ticket.getAssignee())
            .extracting(UserRef::getLogin)
            .containsExactlyInAnyOrder(DEFAULT_USER);
    }

    @Test
    public void shouldAddDefaultUserForModeration() {
        Issue ticket = waitCatalogProcessingStrategy.createTicket(CATEGORY_ID, List.of(OfferTestUtils.nextOffer()
                .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)),
            supplier);

        Assertions.assertThat(ticket.getAssignee())
            .extracting(UserRef::getLogin)
            .doesNotContain(DEFAULT_USER_FOR_MODERATION);
        Assertions.assertThat(ticket.getAssignee())
            .extracting(UserRef::getLogin)
            .containsExactlyInAnyOrder(DEFAULT_USER);
    }

    @Test
    public void shouldAddDefaultUserForModerationWithGrouping() {
        List<Offer> offers = List.of(
            offerForWC(CATEGORY_ID, supplier.getId())
                .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION),
            offerForWC(CATEGORY_ID, supplier.getId())
        );
        offerRepository.insertOffers(offers);

        List<Issue> issues = waitCatalogProcessingStrategy.createTrackerTickets(new OffersFilter());
        Assertions.assertThat(issues).hasSize(2);
    }


    @Test
    public void shouldNotFailIfYangManagerHasNoStaffLogin() {
        Issue ticket = waitCatalogProcessingStrategy.createTicket(CATEGORY_ID, List.of(OfferTestUtils.nextOffer()
                .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
                .setCommentModifiedBy("yang-manager-xxx")),
            supplier);

        Assertions.assertThat(ticket.getFollowers()).isEmpty();
    }

    @Test
    public void testWaitCatalogTicketIsSetForOffer() {
        Offer offer = OfferTestUtils.simpleOffer().setProcessingStatusInternal(Offer.ProcessingStatus.WAIT_CATALOG);
        offerRepository.insertOffer(offer);

        waitCatalogProcessingStrategy.createTrackerTickets(new OffersFilter());

        List<Offer> offerList = offerRepository.findAll();
        Assertions.assertThat(offerList).hasSize(1);
        Assertions.assertThat(offerList.get(0).getAdditionalTickets()).isNotEmpty();
        Assertions.assertThat(offerList.get(0).getAdditionalTickets()).containsExactly(
            new AbstractMap.SimpleEntry<>(
                Offer.AdditionalTicketType.WAIT_CATALOG, offerList.get(0).getTrackerTicket()));
    }

    @Test
    public void testWaitContentGroupingOffers() {
        int otherSupplierId = 900;
        Supplier otherSupplier = new Supplier(otherSupplierId, "Test Supplier");
        supplierRepository.insert(otherSupplier);

        categoryCachingService.addCategory(CATEGORY_ID, "Category1", 0);
        categoryCachingService.addCategory(CATEGORY_ID2, "Other category", 0);

        List<Offer> offers = List.of(
            offerForWC(CATEGORY_ID, supplier.getId()),
            offerForWC(CATEGORY_ID, supplier.getId()),
            offerForWC(CATEGORY_ID, supplier.getId()),
            offerForWC(CATEGORY_ID2, supplier.getId()),
            offerForWC(CATEGORY_ID2, otherSupplierId),
            offerForWC(CATEGORY_ID2, otherSupplierId)
        );
        offerRepository.insertOffers(offers);

        List<Issue> issues = waitCatalogProcessingStrategy.createTrackerTickets(new OffersFilter());

        Map<String, List<Issue>> tickets = issues.stream().collect(Collectors.groupingBy(Issue::getSummary));
        Assertions.assertThat(tickets).hasSize(3);
        String ticketTitle1 = String.format("Менеджеру на доработку: %s (%s), %s %s (%s).",
            "Category1",
            CATEGORY_ID,
            supplier.getType().getTicketName(),
            supplier.getName(),
            supplier.getId());
        Assertions.assertThat(tickets.get(ticketTitle1)).hasSize(1);
        Issue ticket = tickets.get(ticketTitle1).get(0);
        Assert.assertEquals(3,
            StringUtils.countMatches(ticket.getDescription().get(), "OfferTitle" + CATEGORY_ID + supplier.getId()));

        String ticketTitle2 = String.format("Менеджеру на доработку: %s (%s), %s %s (%s).",
            "Other category",
            CATEGORY_ID2,
            otherSupplier.getType().getTicketName(),
            otherSupplier.getName(),
            otherSupplier.getId());
        List<Issue> otherTickets = tickets.get(ticketTitle2);
        Assertions.assertThat(otherTickets).hasSize(1);
        Issue ticket2 = otherTickets.get(0);
        Assert.assertEquals(2,
            StringUtils.countMatches(ticket2.getDescription().get(), "OfferTitle" + CATEGORY_ID2 + otherSupplierId));
    }

    @Test
    public void shouldUpdateStatusAndRemoveComment() {
        Offer offer = offerForWC(CATEGORY_ID, supplier.getId())
            .setContentComments(List.of(new ContentComment(ContentCommentType.FOR_MANAGER, List.of("Comment1"))))
            .setTrackerTicket("1");

        offerRepository.insertOffer(offer);
        categoryCachingService.addCategory(CATEGORY_ID);
        categoryKnowledgeServiceMock.addCategory(CATEGORY_ID);
        IssueMock issue = new IssueMock();
        issue.setKey("1");
        issue.setIssueStatus(IssueStatus.RESOLVED);
        waitCatalogProcessingStrategy.process(issue);

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
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setBusinessId(supplierId)
            .setTitle("OfferTitle" + categoryId + supplierId)
            .setProcessingStatusInternal(Offer.ProcessingStatus.WAIT_CATALOG);
    }
}
