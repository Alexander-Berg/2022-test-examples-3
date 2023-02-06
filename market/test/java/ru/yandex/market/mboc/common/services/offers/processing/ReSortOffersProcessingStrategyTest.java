package ru.yandex.market.mboc.common.services.offers.processing;

import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.tracker.IssueMock;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mbo.tracker.utils.IssueStatus;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.ManagerRole;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.category_manager.CategoryManagerServiceImpl;
import ru.yandex.market.mboc.common.services.category_manager.ManagerCategory;
import ru.yandex.market.mboc.common.services.category_manager.repository.CategoryManagerRepository;
import ru.yandex.market.mboc.common.services.category_manager.repository.CatteamRepository;
import ru.yandex.market.mboc.common.services.converter.OffersExcelFileConverter;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingService;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.users.StaffServiceMock;
import ru.yandex.market.mboc.common.services.users.UserCachingService;
import ru.yandex.market.mboc.common.services.users.UserCachingServiceImpl;
import ru.yandex.market.mboc.common.users.UserRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.UserRef;

/**
 * @author yuramalinov
 * @created 20.03.19
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ReSortOffersProcessingStrategyTest extends BaseDbTestClass {
    private static final String MAILING_LIST = "test";
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryManagerRepository categoryManagerRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private CatteamRepository catteamRepository;
    private ReSortOffersProcessingStrategy strategy;
    private CategoryCachingServiceMock categoryCachingService;
    private CategoryManagerServiceImpl categoryManagerService;
    private TrackerServiceMock trackerServiceMock;
    private ModelStorageCachingService modelStorageCachingService;
    private CategoryKnowledgeServiceMock categoryKnowledgeServiceMock;

    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
                new SupplierConverterServiceMock(), storageKeyValueService);

        categoryCachingService = new CategoryCachingServiceMock();
        UserCachingService userCachingService = new UserCachingServiceImpl(userRepository);
        categoryManagerService = new CategoryManagerServiceImpl(categoryCachingService,
            categoryManagerRepository, userCachingService, transactionHelper, new StaffServiceMock(),
            namedParameterJdbcTemplate, categoryInfoRepository, catteamRepository);
        trackerServiceMock = Mockito.spy(new TrackerServiceMock());
        modelStorageCachingService = Mockito.mock(ModelStorageCachingService.class);

        var supplierService = new SupplierService(supplierRepository);
        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        categoryKnowledgeServiceMock = new CategoryKnowledgeServiceMock();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingService,
            offerBatchProcessor, supplierRepository);
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(
            needContentStatusService, null, offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var offersProcessingStatusService = new OffersProcessingStatusService(offerBatchProcessor,
            needContentStatusService, supplierService, categoryKnowledgeServiceMock, retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator, storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);

        strategy = new ReSortOffersProcessingStrategy(
            trackerServiceMock,
            offerRepository, masterDataHelperService,
            Mockito.mock(OffersExcelFileConverter.class),
            categoryManagerService,
            modelStorageCachingService,
            MAILING_LIST,
            offersProcessingStatusService);
    }

    @Test
    public void testNoFollowIsOK() {
        categoryCachingService.addCategory(1, "Root", 0);
        categoryCachingService.addCategory(2, "Cat1", 1);
        categoryCachingService.addCategory(3, "Cat2-no-manager", 1);

        categoryManagerRepository.storeManagerCategories(Collections.singletonList(
            newManagerCategory("user1", 2)
        ));

        Issue ticket = strategy.createTicket(Arrays.asList(
            OfferTestUtils.nextOffer().setCategoryIdForTests(3L, Offer.BindingKind.SUGGESTED),
            OfferTestUtils.nextOffer().setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
        ));

        Assertions.assertThat(ticket.getFollowers()).isEmpty();
    }

    @Test
    public void testCatmansFollow() {
        Mockito.when(modelStorageCachingService.getModelsFromPgThenMbo(Mockito.anyCollection())).thenReturn(
            ImmutableMap.of(1234L, new Model().setCategoryId(777))
        );
        categoryCachingService.addCategory(1, "Root", 0);
        categoryCachingService.addCategory(2, "Cat1", 1);
        categoryCachingService.addCategory(3, "Cat2-no-manager", 1);
        categoryCachingService.addCategory(4, "Cat1-1", 2);
        categoryCachingService.addCategory(5, "Cat3", 1);
        categoryCachingService.addCategory(777, "category777", 1);

        categoryManagerRepository.storeManagerCategories(Arrays.asList(
            newManagerCategory("user1", 2),
            newManagerCategory("user2", 2).setRole(ManagerRole.CATDIR),
            newManagerCategory("user3", 4),
            newManagerCategory("user4", 5),
            newManagerCategory("Catman777", 777)
        ));

        Issue ticket = strategy.createTicket(Arrays.asList(
            OfferTestUtils.nextOffer().setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
                .setSupplierSkuMapping(new Offer.Mapping(1234L, DateTimeUtils.dateTimeNow())),
            OfferTestUtils.nextOffer().setCategoryIdForTests(3L, Offer.BindingKind.SUGGESTED),
            OfferTestUtils.nextOffer().setCategoryIdForTests(5L, Offer.BindingKind.SUGGESTED)
        ));

        Assertions.assertThat(ticket.getFollowers())
            .extracting(UserRef::getLogin)
            .containsExactlyInAnyOrder("user1", "user2", "user4", "Catman777");
    }

    @Test
    public void shouldCallCorrectCreateTicketMethod() {
        categoryCachingService.addCategory(1, "Root", 0);

        strategy.createTicket(Collections.singletonList(OfferTestUtils.nextOffer()
                .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)));

        Mockito.verify(trackerServiceMock, Mockito.times(1))
            .createTicket(Mockito.anyString(), Mockito.anyString(), Mockito.isNull(), Mockito.anyList(),
                Mockito.eq(TicketType.RE_SORT), Mockito.anyMap(),
                Mockito.anyList(), Mockito.anyCollection(), Mockito.any());
    }

    @Test
    public void testSummonees() {
        Issue ticket = strategy.createTicket(Collections.singletonList(OfferTestUtils.nextOffer()
                .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)));

        Assertions.assertThat(trackerServiceMock.getSummonees(ticket)).containsExactlyInAnyOrder(MAILING_LIST);
    }

    @Test
    public void testAutoCloseTicketsWhenNoRelevantOffers() {
        IssueMock ticket = new IssueMock()
            .setKey("1")
            .setIssueStatus(IssueStatus.OPEN);

        offerRepository.deleteAllInTest();
        supplierRepository.deleteAll();

        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        offerRepository.insertOffers(
            OfferTestUtils.nextOffer()
                .setProcessingStatusInternal(Offer.ProcessingStatus.AUTO_PROCESSED)
                .setTrackerTicket(ticket),

            OfferTestUtils.nextOffer()
                .setProcessingStatusInternal(Offer.ProcessingStatus.AUTO_PROCESSED)
                .setTrackerTicket(ticket)
        );

        strategy.process(ticket);

        Assertions.assertThat(ticket.getIssueStatus()).isEqualTo(IssueStatus.CLOSE);
    }

    @Test
    public void testDontAutoCloseTicketsWhenHasRelevantOffers() {
        IssueMock ticket = new IssueMock()
            .setKey("1")
            .setIssueStatus(IssueStatus.OPEN);

        offerRepository.deleteAllInTest();
        supplierRepository.deleteAll();

        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        offerRepository.insertOffers(
            OfferTestUtils.nextOffer()
                .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RE_SORT)
                .setTrackerTicket(ticket),

            OfferTestUtils.nextOffer()
                .setProcessingStatusInternal(Offer.ProcessingStatus.AUTO_PROCESSED)
                .setTrackerTicket(ticket)
        );

        strategy.process(ticket);

        Assertions.assertThat(ticket.getIssueStatus()).isEqualTo(IssueStatus.OPEN);
    }

    private ManagerCategory newManagerCategory(String login, long categoryId) {
        return new ManagerCategory(login, categoryId, ManagerRole.CATMAN);
    }
}
