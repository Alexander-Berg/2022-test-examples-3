package ru.yandex.market.mboc.common.services.offers.processing;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.market.mbo.tracker.IssueMock;
import ru.yandex.market.mbo.tracker.TrackerService;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.honestmark.AutoClassificationResult;
import ru.yandex.market.mboc.common.honestmark.ClassificationResult;
import ru.yandex.market.mboc.common.honestmark.GcClassificationResult;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationCounterService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationService;
import ru.yandex.market.mboc.common.offers.ClassifierOffer;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeService;
import ru.yandex.market.mboc.common.services.converter.OffersExcelFileConverter;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.startrek.client.model.Issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

public class ClassificationOffersProcessingStrategyTest {
    ClassificationOffersProcessingStrategy strategy;
    private NeedContentStatusService needContentStatusService;
    private TrackerService trackerService;
    private SupplierRepository supplierRepository;
    private CategoryKnowledgeService categoryKnowledgeService;
    private HonestMarkClassificationService honestMarkClassificationService;

    @Before
    public void before() {
        needContentStatusService = Mockito.mock(NeedContentStatusService.class);
        trackerService = Mockito.mock(TrackerService.class);
        OfferRepository offerRepository = Mockito.mock(OfferRepository.class);
        supplierRepository = Mockito.mock(SupplierRepository.class);
        MasterDataHelperService masterDataHelperService = Mockito.mock(MasterDataHelperService.class);
        //noinspection unchecked
        OffersExcelFileConverter<ClassifierOffer> offersExcelFileConverter =
            Mockito.mock(OffersExcelFileConverter.class);
        categoryKnowledgeService = Mockito.mock(CategoryKnowledgeService.class);
        ClassificationOffersProcessingService classificationOffersProcessingService =
            Mockito.mock(ClassificationOffersProcessingService.class);
        ProcessingTicketHelper processingTicketHelper = Mockito.mock(ProcessingTicketHelper.class);
        HonestMarkClassificationCounterService honestMarkClassificationCounterService =
            Mockito.mock(HonestMarkClassificationCounterService.class);
        honestMarkClassificationService = Mockito.mock(HonestMarkClassificationService.class);
        OffersProcessingStatusService offersProcessingStatusService = Mockito.mock(OffersProcessingStatusService.class);

        var applySettingsService = Mockito.mock(ApplySettingsService.class);
        strategy = new ClassificationOffersProcessingStrategy(trackerService, offerRepository, supplierRepository,
            masterDataHelperService, offersExcelFileConverter, categoryKnowledgeService,
            classificationOffersProcessingService, processingTicketHelper, honestMarkClassificationCounterService,
            honestMarkClassificationService, needContentStatusService,
            applySettingsService, offersProcessingStatusService,
            false);
    }

    @Test
    public void testSplitOffers() {
        var offer1 = createOffer(1L);
        var offer2 = createOffer(2L);
        var offer3 = createOffer(3L);
        var offer4 = createOffer(4L);
        offer4.setReprocessRequested(true);

        Mockito.when(needContentStatusService.isGoodContentOffer(offer1)).thenReturn(false);
        Mockito.when(needContentStatusService.isGoodContentOffer(offer2)).thenReturn(true);
        Mockito.when(needContentStatusService.isGoodContentOffer(offer3)).thenReturn(true);
        Mockito.when(needContentStatusService.isGoodContentOffer(offer4)).thenReturn(true);

        assertEquals(
            Set.of(Collections.singletonList(offer1), Arrays.asList(offer2, offer3), Collections.singletonList(offer4)),
            strategy.splitOffers(Arrays.asList(
                offer1, offer2, offer3, offer4
            )).collect(Collectors.toSet())
        );

        assertEquals(
            Collections.emptySet(),
            strategy.splitOffers(Collections.emptyList()).collect(Collectors.toSet())
        );
    }

    @Test
    public void testCreateTicketTags() {
        var offer1 = createOffer(1L);
        var offer2 = createOffer(2L);
        offer2.setReprocessRequested(true);
        Mockito.when(needContentStatusService.isGoodContentOffer(offer1)).thenReturn(false);
        Mockito.when(needContentStatusService.isGoodContentOffer(offer2)).thenReturn(true);
        var supplier = new Supplier(99, "test" + 99).setNewContentPipeline(true);
        Mockito.when(supplierRepository.findById(supplier.getId())).thenReturn(supplier);
        Mockito.when(categoryKnowledgeService.filterCategoriesWithKnowledge(Set.of(1L))).thenReturn(Set.of(1L));
        Mockito.when(honestMarkClassificationService.getClassificationResult(
            eq(offer1), anyLong(), eq(supplier), eq(Set.of(1L)), Mockito.anySet())
        )
            .thenReturn(new AutoClassificationResult(
                ClassificationResult.CONFIDENT,
                GcClassificationResult.CONFIDENT_FOR_CLASSIFICATION,
                true));
        Mockito.when(honestMarkClassificationService.getClassificationResult(
            eq(offer2), anyLong(), eq(supplier), eq(Set.of(1L)), Mockito.anySet())
        )
            .thenReturn(new AutoClassificationResult(
                ClassificationResult.CONFIDENT,
                GcClassificationResult.CONFIDENT_FOR_CLASSIFICATION,
                true));
        Mockito.when(trackerService.createTicket(Mockito.anyString(), Mockito.anyString(), Mockito.isNull(),
            Mockito.anyList(), Mockito.any(), Mockito.anyMap(), Mockito.anySet(), Mockito.any())
        )
            .thenAnswer((Answer<Issue>) invocation -> {
                //noinspection unchecked
                var tags = (Collection<String>) invocation.getArguments()[6];
                assertEquals(Set.of("good_content", "честный_знак_гутгин", "честный_знак"), tags);
                return new IssueMock();
            });
        var issue = strategy.createTicket(99, List.of(offer1, offer2), null);
        assertNotNull(issue);
    }

    private Offer createOffer(long id) {
        var supplierId = 99;
        return new Offer()
            .setId(id)
            .setBusinessId(supplierId)
            .setShopSku("Sku-" + id)
            .addNewServiceOfferIfNotExistsForTests(new Supplier(supplierId, "test" + supplierId))
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setIsOfferContentPresent(true)
            .setShopCategoryName("shop_category_name")
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN)
            .setTitle("Offer_" + id);
    }
}
