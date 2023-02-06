package ru.yandex.market.mboc.processing.assignment.strategy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.categorygroups.CategoryGroup;
import ru.yandex.market.mboc.common.categorygroups.CategoryGroupRepository;
import ru.yandex.market.mboc.common.categorygroups.CategoryGroupService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.SettingsForOfferProcessingInTolokaType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.SettingsForOfferProcessingInToloka;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest;
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentRepository;
import ru.yandex.market.mboc.processing.assignment.SettingsForOfferProcessingInTolokaRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MappingModerationInYangAssignmentStrategyTest extends BaseOfferProcessingTest {
    private static final AtomicLong OFFER_IDS = new AtomicLong();

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SolomonPushService solomonPushService;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CategoryGroupRepository categoryGroupRepository;
    @Autowired
    private OfferProcessingAssignmentRepository assignmentRepository;
    @Autowired
    private SettingsForOfferProcessingInTolokaRepository settings;
    @Autowired
    private CategoryGroupService categoryGroupService;

    private MappingModerationInYangAssignmentStrategy assignmentStrategy;

    @Before
    public void setUp() throws Exception {
        assignmentStrategy = new MappingModerationInYangAssignmentStrategy(transactionHelper, assignmentRepository,
            storageKeyValueService, solomonPushService);

        categoryGroupRepository.insertBatch(new CategoryGroup(null, null,
            List.of(OfferTestUtils.TEST_CATEGORY_INFO_ID), "", ""));
        categoryGroupService.invalidateCaches();

        var supplier = OfferTestUtils.simpleSupplier().setHideFromToloka(false);
        supplierRepository.insert(supplier);
        var category = OfferTestUtils.defaultCategory();
        categoryRepository.insert(category);
        var categoryInfo = OfferTestUtils.categoryInfoWithManualAcceptance().setHideFromToloka(false);
        categoryInfoRepository.insert(categoryInfo);

        generateOffers(this::generateOfferWithMapping);

        var all = assignmentRepository.findAll();
        var hidden = all.stream().filter(OfferProcessingAssignment::getHideFromToloka).count();
        var unHidden = all.stream().filter(Predicate.not(OfferProcessingAssignment::getHideFromToloka)).count();
        assertEquals(500L, hidden);
        assertEquals(500L, unHidden);
    }

    @Test
    public void getOfferIdsToAssign() {
        settings.updateSettings(List.of(new SettingsForOfferProcessingInToloka()
            .setCriteria(SettingsForOfferProcessingInTolokaType.TO_YANG_ONLY_HIDDEN)
            .setIsActive(false)));
        var idsToAssign = assignmentStrategy.getOfferIdsToAssign(1000, OfferTestUtils.TEST_CATEGORY_INFO_ID);
        assertEquals(1000, idsToAssign.size());

        settings.updateSettings(List.of(new SettingsForOfferProcessingInToloka()
            .setCriteria(SettingsForOfferProcessingInTolokaType.TO_YANG_ONLY_HIDDEN)
            .setIsActive(true)));
        idsToAssign = assignmentStrategy.getOfferIdsToAssign(500, OfferTestUtils.TEST_CATEGORY_INFO_ID);
        offerRepository.getOffersByIds(idsToAssign).stream().filter(Objects::nonNull)
            .forEach(offer -> assertTrue(offer.getHideFromToloka()));
    }

    @Test
    public void shouldNotAssignOffersWithEmptyTargetSku() {
        offerRepository.deleteAllInTest();
        assignmentRepository.deleteAll();
        generateOffers(this::generateOfferWithoutMapping);
        settings.updateSettings(List.of(new SettingsForOfferProcessingInToloka()
            .setCriteria(SettingsForOfferProcessingInTolokaType.TO_YANG_ONLY_HIDDEN)
            .setIsActive(false)));

        var idsToAssign = assignmentStrategy.getOfferIdsToAssign(1000, OfferTestUtils.TEST_CATEGORY_INFO_ID);
        assertEquals(0, idsToAssign.size());
    }

    @Test
    public void sendDSBStoYangSettings() {
        offerRepository.deleteAllInTest();
        assignmentRepository.deleteAll();

        assignmentRepository.insert(new OfferProcessingAssignment()
            .setOfferId(1)
            .setProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
            .setCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setTargetSkuId(123L)
            .setHideFromToloka(false)
            .setProcessingTicketId(1)
            .setPriority(-100)
        );

        settings.updateSettings(List.of(new SettingsForOfferProcessingInToloka()
            .setCriteria(SettingsForOfferProcessingInTolokaType.TO_YANG_DSBS)
            .setIsActive(false)));
        var idsToAssign = assignmentStrategy.getOfferIdsToAssign(1000, OfferTestUtils.TEST_CATEGORY_INFO_ID);
        assertEquals(0, idsToAssign.size());

        settings.updateSettings(List.of(new SettingsForOfferProcessingInToloka()
            .setCriteria(SettingsForOfferProcessingInTolokaType.TO_YANG_DSBS)
            .setIsActive(true)));
        idsToAssign = assignmentStrategy.getOfferIdsToAssign(1000, OfferTestUtils.TEST_CATEGORY_INFO_ID);
        assertEquals(1, idsToAssign.size());
    }

    @Test
    public void successfullyAssignByCategoryGroups() {
        storageKeyValueService.putValue(assignmentStrategy.getUseCategoryGroupsKey(), true);
        storageKeyValueService.invalidateCache();

        var categoryGroupId = categoryGroupRepository.findAll().stream()
            .filter(x -> x.getCategories().contains(OfferTestUtils.TEST_CATEGORY_INFO_ID))
            .findFirst()
            .get()
            .getId();

        assignmentStrategy.setUseCategoryGroupsForTest(true);

        offerRepository.deleteAllInTest();
        assignmentRepository.deleteAll();
        generateOffers(this::generateOfferWithMapping);

        var idsToAssign = assignmentStrategy.getOfferIdsToAssign(1000, categoryGroupId);
        assertTrue("idsToAssign should be > 0", idsToAssign.size() > 0);
    }

    private Offer generateOfferWithoutMapping() {
        var id = OFFER_IDS.incrementAndGet();
        return OfferTestUtils.simpleOkOffer()
            .setId(id)
            .setShopSku("uniq-shop-sku-" + id)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION);
    }

    private Offer generateOfferWithMapping() {
        var id = OFFER_IDS.incrementAndGet();
        return OfferTestUtils.simpleOkOffer()
            .setId(id)
            .setShopSku("uniq-shop-sku-" + id)
            .setSupplierSkuMapping(new Offer.Mapping(123L, LocalDateTime.now(), Offer.SkuType.PARTNER20))
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION);
    }

    private void generateOffers(Supplier<Offer> offerGenerator) {
        var clearOffers = IntStream.range(0, 500).mapToObj(__ -> offerGenerator.get()).collect(Collectors.toList());
        var hiddenOffers = IntStream.range(0, 500).mapToObj(__ -> offerGenerator.get().setHideFromToloka(true))
            .collect(Collectors.toList());

        offerRepository.insertOffers(clearOffers);
        offerRepository.insertOffers(hiddenOffers);
    }

}
