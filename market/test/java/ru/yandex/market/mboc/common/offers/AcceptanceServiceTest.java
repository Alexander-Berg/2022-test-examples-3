package ru.yandex.market.mboc.common.offers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleService;
import ru.yandex.market.mboc.common.offers.acceptance.service.AcceptanceService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.report.AcceptanceReport;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 15.07.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class AcceptanceServiceTest extends BaseDbTestClass {
    private static final Logger log = LoggerFactory.getLogger(AcceptanceServiceTest.class);
    private static final long SEED = -1022017785L;
    private static final long CATEGORY_ID = 666777;
    private static final long CATEGORY_ID_2 = 666778;
    private static final long CATEGORY_ID_3 = 666779;
    private static final long CATEGORY_ID_GOOD_CONTENT = 666888;
    private static final int BLUE_SUPPLIER_ID = 111;
    private static final int WHITE_SUPPLIER_ID = 222;

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CategoryCachingService categoryCachingService;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private CategoryRuleService categoryRuleService;

    private EnhancedRandom random;

    private CategoryRuleService categoryRuleServiceSpy;
    private AcceptanceService service;

    @Value("${mboc.acceptance.config.enabled:true}")
    private boolean configurableAcceptance;

    @Before
    public void setUp() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(SEED)
            .build();
        // Init root category
        categoryRepository.insert(
            new Category()
                .setCategoryId(CategoryTree.ROOT_CATEGORY_ID)
                .setName("root")
                .setParentCategoryId(CategoryTree.NO_ROOT_ID)
                .setPublished(true)
        );
        var supplierService = new SupplierService(supplierRepository);
        categoryRuleServiceSpy = Mockito.spy(categoryRuleService);
        //modelStorageCachingServiceMock.setAutoModel(new Model().setId(101L).setTitle("title"));
        service = new AcceptanceService(categoryInfoRepository, categoryCachingService, supplierService, false,
            categoryRuleServiceSpy, configurableAcceptance, offerDestinationCalculator);
    }

    @Test
    public void approve() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        @SuppressWarnings("checkstyle:magicnumber")
        List<Offer> offers = random.objects(Offer.class, 20, "id")
            .peek(o -> o
                .setBusinessId(supplier.getId())
                .addNewServiceOfferIfNotExistsForTests(supplier)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
                .updateApprovedSkuMapping(null, null))
            .peek(o -> createCategory(o.getCategoryId(), true, true))
            .collect(Collectors.toList());


        service.tryAutoAccept(supplier, offers);

        assertThat(offers).extracting(Offer::getAcceptanceStatus).containsOnly(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void notOpenButNew() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        Offer offer = createOffer(supplier)
            .setProcessingStatusInternal(Offer.ProcessingStatus.PROCESSED)
            .setAcceptanceStatusInternal(Offer.AcceptanceStatus.NEW)
            .setId(2);

        service.tryAutoAccept(supplier, Collections.singleton(offer));

        if (configurableAcceptance) {
            assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
        } else {
            assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.NEW);
        }
    }

    @Test
    public void acceptOnlyAllowedSupplierType() {
        for (MbocSupplierType supplierType : MbocSupplierType.values()) {
            log.info("supplier type = {}", supplierType);
            Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, supplierType, sup -> sup.setEats(false));

            Offer offer = createOffer(supplier);

            createCategory(offer.getCategoryId(), true, true);
            service.tryAutoAccept(supplier, Collections.singleton(offer));

            if (supplierType.isAllowAutoAcceptance() && supplierType != MbocSupplierType.MARKET_SHOP) {
                assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
            } else {
                assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.NEW);
            }
        }
    }

    @Test
    public void acceptOnlyAllowedSupplierTypeForEats() {
        var supplierType = MbocSupplierType.BUSINESS;
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, supplierType, sup -> sup.setEats(true));

        Offer offer = createOffer(supplier);
        var serviceSupplierType = offer.getServiceOffer(supplier.getId()).get().getSupplierType();
        Assertions.assertThat(serviceSupplierType).isEqualTo(MbocSupplierType.THIRD_PARTY);

        createCategory(offer.getCategoryId(), true, true);
        service.tryAutoAccept(supplier, Collections.singleton(offer));

        if (serviceSupplierType.isAllowAutoAcceptance()) {
            assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
        } else {
            assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.NEW);
        }
    }

    @Test
    public void shouldBeNoKnowledgeForCategoryWithoutModelForm() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        Offer offerWithKnowledge = createOffer(supplier).setCategoryIdForTests(CATEGORY_ID,
            Offer.BindingKind.SUGGESTED);
        Offer offerWithoutKnowledge = createOffer(supplier).setCategoryIdForTests(CATEGORY_ID + 1,
            Offer.BindingKind.SUGGESTED);
        createCategory(CATEGORY_ID, true, true);

        service.tryAutoAccept(supplier, Arrays.asList(offerWithKnowledge, offerWithoutKnowledge));
        assertThat(offerWithKnowledge.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
        assertThat(offerWithoutKnowledge.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void shouldAutoProcessForOffersWithApprovedMappingAndOpen() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        Offer offer = createOffer(supplier)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), Offer.MappingConfidence.CONTENT)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN);

        service.tryAutoAccept(supplier, Collections.singleton(offer));

        assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void manualCategory() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        Offer offer = createOffer(supplier)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);

        CategoryInfo categoryInfo = customAcceptanceInfo(CATEGORY_ID, true, CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfoRepository.insertOrUpdate(categoryInfo);

        service.tryAutoAccept(supplier, Collections.singleton(offer));

        assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.NEW);
    }

    @Test
    public void notManualCategory() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        Offer offer = createOffer(supplier)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);

        CategoryInfo categoryInfo = autoAcceptableInfo(CATEGORY_ID);
        categoryInfoRepository.insertOrUpdate(categoryInfo);
        createCategory(CATEGORY_ID, true, true);

        service.tryAutoAccept(supplier, Collections.singleton(offer));

        assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void acceptManualCategoryBarcodeInIsbn() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        Offer offer = createOffer(supplier)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setBarCode("978-3-16-148410-0");

        CategoryInfo categoryInfo = new CategoryInfo(CATEGORY_ID);
        categoryInfo.setManualAcceptance(true);
        categoryInfoRepository.insertOrUpdate(categoryInfo);

        service.tryAutoAccept(supplier, Collections.singleton(offer));

        assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void approveForNewPipeline() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY,
            s -> s.setNewContentPipeline(true));
        Supplier whiteSupplier = createSupplier(WHITE_SUPPLIER_ID, MbocSupplierType.MARKET_SHOP,
            s -> s.setNewContentPipeline(true));

        long idGen = 0;

        Offer withSupplierMapping = createOffer(supplier)
            .setId(idGen++)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(null)
            .setSupplierSkuMapping(OfferTestUtils.mapping(2));

        Offer withSuggestMapping = createOffer(supplier)
            .setId(idGen++)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(OfferTestUtils.mapping(2))
            .setSupplierSkuMapping(null);

        Offer noMappingNoGoodContent = createOffer(supplier)
            .setId(idGen++)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);

        Offer noMappingGoodContent = createOffer(supplier)
            .setId(idGen++)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(CATEGORY_ID_GOOD_CONTENT, Offer.BindingKind.SUGGESTED);

        Offer noMappingNoModelNoGoodContent = createOffer(supplier)
            .setId(idGen++)
            .setModelId(null)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);

        Offer noMappingNoModelGoodContent = createOffer(supplier)
            .setId(idGen++)
            .setModelId(null)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(CATEGORY_ID_GOOD_CONTENT, Offer.BindingKind.SUGGESTED);

        Offer whiteSkutchedOfferGoodContent = createOffer(whiteSupplier)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(null)
            .setCategoryIdForTests(CATEGORY_ID_GOOD_CONTENT, Offer.BindingKind.SUGGESTED);

        Offer whiteMatchedOfferGoodContent = createOffer(whiteSupplier)
            .setId(idGen++)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCategoryIdForTests(CATEGORY_ID_GOOD_CONTENT, Offer.BindingKind.SUGGESTED);

        Offer noKnowledge = createOffer(supplier)
            .setId(idGen++)
            .setSupplierSkuMapping(null)
            .setCategoryIdForTests(CATEGORY_ID_2, Offer.BindingKind.SUGGESTED);

        CategoryInfo categoryInfo = autoAcceptableInfo(CATEGORY_ID);
        CategoryInfo categoryInfoGoodContent = autoAcceptableInfo(CATEGORY_ID_GOOD_CONTENT);

        categoryInfoRepository.insert(categoryInfo);
        categoryInfoRepository.insert(categoryInfoGoodContent);
        createCategory(CATEGORY_ID, true, false);
        createCategory(CATEGORY_ID_2, false, false);
        createCategory(CATEGORY_ID_GOOD_CONTENT, true, true);

        service.tryAutoAccept(supplier, Arrays.asList(
            withSupplierMapping, withSuggestMapping, noMappingNoGoodContent, noMappingGoodContent,
            noMappingNoModelNoGoodContent, noMappingNoModelGoodContent, noKnowledge
        ));
        service.tryAutoAccept(whiteSupplier, Arrays.asList(
            whiteSkutchedOfferGoodContent, whiteMatchedOfferGoodContent
        ));

        assertThat(Arrays.asList(withSupplierMapping, noMappingNoGoodContent,
            noMappingGoodContent, noKnowledge))
            .extracting(Offer::getAcceptanceStatus)
            .containsOnly(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void acceptAndApproveInFMCGPipeline() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.FMCG);
        Offer offer = createOffer(supplier);

        service.tryAutoAccept(supplier, Collections.singletonList(offer));

        assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void autoRejectByCategory() {
        if (!configurableAcceptance) {
            return;
        }

        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        Offer offer = createOffer(supplier)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);

        CategoryInfo categoryInfo = customAcceptanceInfo(CATEGORY_ID, true, CategoryInfo.AcceptanceMode.AUTO_REJECT);
        categoryInfoRepository.insertOrUpdate(categoryInfo);
        createCategory(CATEGORY_ID, true, true);

        service.tryAutoAccept(supplier, Collections.singleton(offer));

        assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.TRASH);
    }

    @Test
    public void autoAcceptsJewelryCategories() {
        Supplier supplierNonJewelry = createSupplier(OfferTestUtils.BLUE_SUPPLIER_ID_1, MbocSupplierType.THIRD_PARTY);
        Supplier supplierJewelry = supplierRepository.update(
            createSupplier(OfferTestUtils.BLUE_SUPPLIER_ID_2, MbocSupplierType.THIRD_PARTY).setSellsJewelry(true)
        );
        Offer supplier1Offer = createOffer(supplierNonJewelry)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);
        Offer supplier2Offer = createOffer(supplierJewelry)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);

        CategoryInfo categoryInfo = customAcceptanceInfo(CATEGORY_ID, true, CategoryInfo.AcceptanceMode.MANUAL)
            .addTag(CategoryInfo.CategoryTag.JEWELRY);
        categoryInfoRepository.insertOrUpdate(categoryInfo);

        createCategory(CATEGORY_ID_2, true, true);

        service.tryAutoAccept(supplierNonJewelry, List.of(supplier1Offer));
        service.tryAutoAccept(supplierJewelry, List.of(supplier2Offer));

        assertThat(supplier1Offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.NEW);
        assertThat(supplier2Offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void rulesNotAppliedWhenConfiguredAutoReject() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        Offer offerReject = createOffer(supplier)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);
        Offer offerAccept = createOffer(supplier)
            .setCategoryIdForTests(CATEGORY_ID_2, Offer.BindingKind.SUGGESTED);

        CategoryInfo rejectCategoryInfo = customAcceptanceInfo(CATEGORY_ID, true, CategoryInfo.AcceptanceMode.AUTO_REJECT);
        categoryInfoRepository.insertOrUpdate(rejectCategoryInfo);
        createCategory(CATEGORY_ID, true, true);
        CategoryInfo acceptCategoryInfo = customAcceptanceInfo(CATEGORY_ID_2, true, CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfoRepository.insertOrUpdate(acceptCategoryInfo);
        createCategory(CATEGORY_ID_2, true, true);

        service.tryAutoAccept(supplier, List.of(offerReject, offerAccept));

        assertThat(offerReject.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.TRASH);
        assertThat(offerAccept.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);

        verify(categoryRuleServiceSpy, times(1)).isAutoAcceptanceAllowed(anyLong(), anyLong(), any());
        verify(categoryRuleServiceSpy, times(1)).isAutoAcceptanceAllowed(eq(CATEGORY_ID_2), eq((long) BLUE_SUPPLIER_ID),
            eq(offerAccept.getVendorId()));
    }

    @Test
    public void rulesAreApplied() {
        Supplier supplier = createSupplier(BLUE_SUPPLIER_ID, MbocSupplierType.THIRD_PARTY);
        Offer offerWithoutRules = createOffer(supplier)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);
        Offer offerAccept = createOffer(supplier)
            .setCategoryIdForTests(CATEGORY_ID_2, Offer.BindingKind.SUGGESTED);
        Offer offerManual = createOffer(supplier)
            .setCategoryIdForTests(CATEGORY_ID_3, Offer.BindingKind.SUGGESTED);

        CategoryInfo categoryWithoutRules = customAcceptanceInfo(CATEGORY_ID, true, CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfoRepository.insertOrUpdate(categoryWithoutRules);
        createCategory(CATEGORY_ID, true, true);
        CategoryInfo categoryAccept = customAcceptanceInfo(CATEGORY_ID_2, true, CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfoRepository.insertOrUpdate(categoryAccept);
        createCategory(CATEGORY_ID_2, true, true);
        CategoryInfo categoryManual = customAcceptanceInfo(CATEGORY_ID_3, true, CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfoRepository.insertOrUpdate(categoryManual);
        createCategory(CATEGORY_ID_3, true, true);

        doReturn(Optional.empty()).when(categoryRuleServiceSpy).isAutoAcceptanceAllowed(
                eq(CATEGORY_ID), eq((long) BLUE_SUPPLIER_ID), eq(offerWithoutRules.getVendorId()));
        doReturn(Optional.of(true)).when(categoryRuleServiceSpy).isAutoAcceptanceAllowed(
                eq(CATEGORY_ID_2), eq((long) BLUE_SUPPLIER_ID), eq(offerAccept.getVendorId()));
        doReturn(Optional.of(false)).when(categoryRuleServiceSpy).isAutoAcceptanceAllowed(
                eq(CATEGORY_ID_3), eq((long) BLUE_SUPPLIER_ID), eq(offerManual.getVendorId()));

        service.tryAutoAccept(supplier, List.of(offerWithoutRules, offerAccept, offerManual));

        assertThat(offerWithoutRules.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
        assertThat(offerAccept.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
        assertThat(offerManual.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.NEW);

        verify(categoryRuleServiceSpy, times(3)).isAutoAcceptanceAllowed(anyLong(), anyLong(), any());
    }

    @Test
    public void testAcceptingReportForming() {
        var supplier = createSupplier(100500, MbocSupplierType.THIRD_PARTY);

        var offer = createOffer(supplier)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .removeServiceOfferIfExistsForTests(100500);

        CategoryInfo categoryInfo = customAcceptanceInfo(CATEGORY_ID, true, CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfoRepository.insertOrUpdate(categoryInfo);

        var reports = service.formulateReports(List.of(offer));
        var report = reports.get(0);

        assertThat(report.getOffer()).isSameAs(offer);
        var acceptanceReports = report.getReports();
        assertThat(acceptanceReports).hasSize(5);

        var tradeModels = acceptanceReports.stream().map(AcceptanceReport::getTradeModel).collect(Collectors.toSet());
        assertThat(tradeModels).hasSize(5);

        var resolutions = acceptanceReports.stream().map(AcceptanceReport::getResolution).collect(Collectors.toSet());
        assertThat(resolutions).hasSameElementsAs(List.of(AcceptanceReport.Resolution.OK));

        var warnings = acceptanceReports.stream().map(AcceptanceReport::getWarning).collect(Collectors.toList());
        assertThat(warnings).containsExactly(null, null, null, null, null);
    }

    @Test
    public void testMixedAcceptionReportForming() {
        var supplier = createSupplier(100500, MbocSupplierType.THIRD_PARTY);

        var offer = createOffer(supplier)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .removeServiceOfferIfExistsForTests(100500);

        CategoryInfo categoryInfo = customAcceptanceInfo(CATEGORY_ID, true, CategoryInfo.AcceptanceMode.AUTO_ACCEPT)
            .setFbyPlusAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_REJECT)
            .setFbsAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfoRepository.insertOrUpdate(categoryInfo);

        var reports = service.formulateReports(List.of(offer));
        var report = reports.get(0);

        assertThat(report.getOffer()).isSameAs(offer);
        var acceptanceReports = report.getReports();
        assertThat(acceptanceReports).hasSize(5);

        var tradeModels = acceptanceReports.stream().map(AcceptanceReport::getTradeModel).collect(Collectors.toSet());
        assertThat(tradeModels).hasSize(5);

        var resolutions = acceptanceReports.stream().map(AcceptanceReport::getResolution).collect(Collectors.toSet());
        assertThat(resolutions).hasSize(3);
        assertThat(resolutions).hasSameElementsAs(
            List.of(
                AcceptanceReport.Resolution.OK,
                AcceptanceReport.Resolution.MANUAL,
                AcceptanceReport.Resolution.TRASH
            )
        );

        var warnings = acceptanceReports.stream().map(AcceptanceReport::getWarning).collect(Collectors.toList());
        assertThat(warnings).containsExactly(null, null, null, null, null);
    }

    @Test
    public void testJewelryReport() {
        var supplier = createSupplier(100500, MbocSupplierType.THIRD_PARTY);

        var offer = createOffer(supplier)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .removeServiceOfferIfExistsForTests(100500);

        CategoryInfo categoryInfo = customAcceptanceInfo(CATEGORY_ID, true, CategoryInfo.AcceptanceMode.AUTO_ACCEPT)
            .setFbyPlusAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_REJECT)
            .setFbsAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL)
            .addTag(CategoryInfo.CategoryTag.JEWELRY);

        categoryInfoRepository.insertOrUpdate(categoryInfo);

        var reports = service.formulateReports(List.of(offer));
        var report = reports.get(0);

        assertThat(report.getOffer()).isSameAs(offer);
        var acceptanceReports = report.getReports();
        assertThat(acceptanceReports).hasSize(5);

        var tradeModels = acceptanceReports.stream().map(AcceptanceReport::getTradeModel).collect(Collectors.toSet());
        assertThat(tradeModels).hasSize(5);

        var resolutions = acceptanceReports.stream()
            .collect(Collectors.toMap(AcceptanceReport::getTradeModel, AcceptanceReport::getResolution));
        var resolutionsSet = acceptanceReports.stream().map(AcceptanceReport::getResolution).collect(Collectors.toSet());
        assertThat(resolutionsSet).hasSize(2);
        var orderedResolutions = Stream.of(AcceptanceReport.TradeModel.values()).map(resolutions::get)
            .collect(Collectors.toList());
        assertThat(orderedResolutions).containsExactly(
            AcceptanceReport.Resolution.BLOCKED,
            AcceptanceReport.Resolution.TRASH,
            AcceptanceReport.Resolution.BLOCKED,
            AcceptanceReport.Resolution.BLOCKED,
            AcceptanceReport.Resolution.BLOCKED
        );

        var warnings = acceptanceReports.stream()
            .collect(Collectors.toMap(AcceptanceReport::getTradeModel, one -> Optional.ofNullable(one.getWarning())));
        var orderedWarnings = Stream.of(AcceptanceReport.TradeModel.values()).map(warnings::get)
            .collect(Collectors.toList());
        assertThat(orderedWarnings).containsExactly(
            Optional.of(AcceptanceReport.Warning.JEWELRY),
            Optional.empty(),
            Optional.of(AcceptanceReport.Warning.JEWELRY),
            Optional.of(AcceptanceReport.Warning.JEWELRY),
            Optional.of(AcceptanceReport.Warning.JEWELRY)
        );
    }

    private Offer createOffer(Supplier supplier) {
        return random.nextObject(Offer.class, "id")
            .setBusinessId(supplier.getId())
            .addNewServiceOfferIfNotExistsForTests(supplier)
            .updateApprovedSkuMapping(null, null)
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
        supplier.setRealSupplierId(String.valueOf(random.nextInt(1_000_000))); //varchar 20
        customizer.accept(supplier);
        supplierRepository.insertOrUpdate(supplier);
        return supplier;
    }

    private void createCategory(long id, boolean hasKnowledge, boolean goodContent) {
        categoryRepository.insert(new Category().setCategoryId(id)
            .setHasKnowledge(hasKnowledge)
            .setAcceptGoodContent(goodContent)
            .setAcceptContentFromWhiteShops(goodContent));
    }

    private CategoryInfo autoAcceptableInfo(long id) {
        var info = new CategoryInfo(id);
        info.setManualAcceptance(false);

        var accept = CategoryInfo.AcceptanceMode.AUTO_ACCEPT;
        info.setFbyAcceptanceMode(accept);
        info.setFbyPlusAcceptanceMode(accept);
        info.setFbsAcceptanceMode(accept);
        info.setDsbsAcceptanceMode(accept);

        return info;
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
