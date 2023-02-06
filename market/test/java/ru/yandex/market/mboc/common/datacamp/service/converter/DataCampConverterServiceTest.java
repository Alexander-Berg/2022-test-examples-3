package ru.yandex.market.mboc.common.datacamp.service.converter;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampCommonTypes;
import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampContentStatus.CategoryRestriction.AllowedType;
import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.DataCampValidationResult;
import Market.DataCamp.PartnerCategoryOuterClass;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.honestmark.ClassificationResult;
import ru.yandex.market.mboc.common.honestmark.EmptyCategoryRestriction;
import ru.yandex.market.mboc.common.honestmark.GroupCategoryRestriction;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.honestmark.SingleCategoryRestriction;
import ru.yandex.market.mboc.common.masterdata.model.MasterDataDto;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.vendor.models.CachedGlobalVendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

public class DataCampConverterServiceTest {

    private OfferCategoryRestrictionCalculator calculator;
    private DataCampConverterService dataCampConverterService;

    @Before
    public void setUp() {
        DataCampIdentifiersService dataCampIdentifiersService = Mockito.mock(DataCampIdentifiersService.class);
        Mockito.when(dataCampIdentifiersService.createBusinessSkuKey(any()))
            .thenAnswer(invocation -> {
                DataCampOffer.Offer offer = invocation.getArgument(0);
                return DataCampOfferUtil.extractExternalBusinessSkuKey(offer);
            });
        Mockito.when(dataCampIdentifiersService.createIdentifiers(any(), any()))
            .thenAnswer(invocation -> {
                Offer offer = invocation.getArgument(1);
                DataCampOfferIdentifiers.OfferIdentifiers.Builder identifiers =
                    DataCampOfferIdentifiers.OfferIdentifiers.newBuilder();
                identifiers.setOfferId(offer.getShopSku());
                identifiers.setBusinessId(offer.getBusinessId());
                return identifiers.build();
            });
        Mockito.when(dataCampIdentifiersService.createIdentifiers(any(), any(), any()))
            .thenAnswer(invocation -> {
                Offer offer = invocation.getArgument(1);
                DataCampOfferIdentifiers.OfferIdentifiers.Builder identifiers =
                    DataCampOfferIdentifiers.OfferIdentifiers.newBuilder();
                identifiers.setOfferId(offer.getShopSku());
                identifiers.setBusinessId(offer.getBusinessId());
                return identifiers.build();
            });
        calculator = Mockito.mock(OfferCategoryRestrictionCalculator.class);
        Mockito.when(calculator
                .calculateClassificationResult(any(UltraController.EnrichedOffer.class), any()))
            .thenReturn(ClassificationResult.UNCONFIDENT_ALLOW_GC);

        dataCampConverterService = new DataCampConverterService(
            dataCampIdentifiersService,
            calculator,
            new StorageKeyValueServiceMock(),
            true
        );
    }

    @Test
    public void testWhenCatmanCreatesCardBaseOfferHasCpaInWorkStatusServiceOfferHasCpaCardCreatingStatus() {
        Mockito.when(calculator.calculateRestriction(any())).thenReturn(Optional.empty());
        Context context = Context.builder()
            .supplier(new Supplier(1, "Test"))
            .category(new Category().setCategoryId(1))
            .build();
        Offer offer = OfferTestUtils.simpleOffer()
            .setId(1L)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS);

        SoftAssertions assertions = new SoftAssertions();

        Stream.of(
                Offer.ProcessingStatus.IN_PROCESS,
                Offer.ProcessingStatus.WAIT_CONTENT,
                Offer.ProcessingStatus.WAIT_CATALOG,
                Offer.ProcessingStatus.NO_SIZE_MEASURE_VALUE_
            )
            .forEach(testStatus -> {
                DataCampUnitedOffer.UnitedOffer dcOffer = dataCampConverterService.convertToDataCampUpdate(context,
                    offer, null);

                DataCampContentStatus.OfferContentCpaState cpaState =
                    dcOffer.getBasic().getContent().getStatus().getContentSystemStatus().getCpaState();
                assertions.assertThat(cpaState)
                    .as("checking basic with %s status", Offer.ProcessingStatus.IN_PROCESS)
                    .isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK);
                dcOffer.getServiceMap().values()
                    .forEach(so -> {
                        var serviceCpaState =
                            so.getContent().getStatus().getContentSystemStatus().getServiceOfferState();
                        assertions.assertThat(serviceCpaState)
                            .as("checking service with %s status", Offer.ProcessingStatus.IN_PROCESS)
                            .isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CARD_CREATING);
                    });
            });

        assertions.assertAll();
    }

    @Test
    public void testNoRestriction() {
        Mockito.when(calculator.calculateRestriction(any())).thenReturn(Optional.empty());
        Context context = Context.builder()
            .supplier(new Supplier(1, "Test"))
            .category(new Category().setCategoryId(1))
            .build();
        Offer offer = OfferTestUtils.simpleOffer();

        DataCampUnitedOffer.UnitedOffer dcOffer = dataCampConverterService.convertToDataCampUpdate(context, offer,
            null);
        assertOfferHasRestriction(dcOffer, DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(AllowedType.INDETERMINABLE).build());

        DataCampUnitedOffer.UnitedOffer dcOffer2 = dataCampConverterService.convertToDataCampUpdate(context,
            DataCampUnitedOffer.UnitedOffer.newBuilder().build(),
            UltraController.EnrichedOffer.newBuilder().build(),
            null);
        assertOfferHasRestriction(dcOffer2, DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(AllowedType.INDETERMINABLE).build());

        DataCampOffer.Offer dcOffer3 = dataCampConverterService.enrichWithState(context, offer,
            DataCampOffer.Offer.newBuilder().build());
        assertOfferHasRestriction(dcOffer3, DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(AllowedType.INDETERMINABLE).build());
    }

    @Test
    public void testSingleRestriction() {
        long hid = 123L;
        Mockito.when(calculator.calculateRestriction(any()))
            .thenReturn(Optional.of(new SingleCategoryRestriction(hid)));
        Mockito.when(calculator.calculateRestriction(any(UltraController.EnrichedOffer.class), any()))
            .thenReturn(Optional.of(new SingleCategoryRestriction(hid)));
        Mockito.when(calculator.calculateRestriction(any(Offer.class)))
            .thenReturn(Optional.of(new SingleCategoryRestriction(hid)));
        Context context = Context.builder()
            .supplier(new Supplier(1, "Test"))
            .category(new Category().setCategoryId(1))
            .build();
        Offer offer = OfferTestUtils.simpleOffer();

        DataCampUnitedOffer.UnitedOffer dcOffer = dataCampConverterService.convertToDataCampUpdate(context, offer,
            null);
        assertOfferHasRestriction(dcOffer, DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(AllowedType.SINGLE)
            .setAllowedCategoryId(hid)
            .build());

        DataCampUnitedOffer.UnitedOffer dcOffer2 = dataCampConverterService.convertToDataCampUpdate(context,
            DataCampUnitedOffer.UnitedOffer.newBuilder().build(),
            UltraController.EnrichedOffer.newBuilder().build(),
            null);
        assertOfferHasRestriction(dcOffer2, DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(AllowedType.SINGLE)
            .setAllowedCategoryId(hid)
            .build());

        DataCampOffer.Offer dcOffer3 = dataCampConverterService.enrichWithState(context, offer,
            DataCampOffer.Offer.newBuilder().build());
        assertOfferHasRestriction(dcOffer3, DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(AllowedType.SINGLE)
            .setAllowedCategoryId(hid)
            .build());
    }

    @Test
    public void testGroupRestriction() {
        long groupId = 123L;
        Mockito.when(calculator.calculateRestriction(any(Offer.class)))
            .thenReturn(Optional.of(new GroupCategoryRestriction(groupId, Set.of())));
        Mockito.when(calculator.calculateRestriction(any(UltraController.EnrichedOffer.class), any()))
            .thenReturn(Optional.of(new GroupCategoryRestriction(groupId, Set.of())));
        Context context = Context.builder()
            .supplier(new Supplier(1, "Test"))
            .category(new Category().setCategoryId(1))
            .build();
        Offer offer = OfferTestUtils.simpleOffer();

        DataCampUnitedOffer.UnitedOffer dcOffer = dataCampConverterService.convertToDataCampUpdate(context, offer,
            null);
        assertOfferHasRestriction(dcOffer, DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(AllowedType.GROUP)
            .setAllowedGroupId(groupId)
            .build());

        DataCampUnitedOffer.UnitedOffer dcOffer2 = dataCampConverterService.convertToDataCampUpdate(context,
            DataCampUnitedOffer.UnitedOffer.newBuilder().build(),
            UltraController.EnrichedOffer.newBuilder().build(),
            null);
        assertOfferHasRestriction(dcOffer2, DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(AllowedType.GROUP)
            .setAllowedGroupId(groupId)
            .build());

        DataCampOffer.Offer dcOffer3 = dataCampConverterService.enrichWithState(context, offer,
            DataCampOffer.Offer.newBuilder().build());
        assertOfferHasRestriction(dcOffer3, DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(AllowedType.GROUP)
            .setAllowedGroupId(groupId)
            .build());
    }

    @Test
    public void testAnyRestriction() {
        Mockito.when(calculator.calculateRestriction(any(Offer.class)))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));
        Mockito.when(calculator.calculateRestriction(any(UltraController.EnrichedOffer.class), any()))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));
        Context context = Context.builder()
            .supplier(new Supplier(1, "Test"))
            .category(new Category().setCategoryId(1))
            .build();
        Offer offer = OfferTestUtils.simpleOffer();

        DataCampUnitedOffer.UnitedOffer dcOffer = dataCampConverterService.convertToDataCampUpdate(context, offer,
            null);
        assertOfferHasRestriction(dcOffer, DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(AllowedType.ANY)
            .build());

        DataCampUnitedOffer.UnitedOffer dcOffer2 = dataCampConverterService.convertToDataCampUpdate(context,
            DataCampUnitedOffer.UnitedOffer.newBuilder().build(),
            UltraController.EnrichedOffer.newBuilder().build(),
            null);
        assertOfferHasRestriction(dcOffer2, DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(AllowedType.ANY)
            .build());

        DataCampOffer.Offer dcOffer3 = dataCampConverterService.enrichWithState(context, offer,
            DataCampOffer.Offer.newBuilder().build());
        assertOfferHasRestriction(dcOffer3, DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(AllowedType.ANY)
            .build());
    }

    @Test
    public void testAcceptedServiceOfferHaveNoVerdicts() {
        Mockito.when(calculator.calculateRestriction(any(Offer.class)))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));
        Mockito.when(calculator.calculateRestriction(any(UltraController.EnrichedOffer.class), any()))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));
        Context context = Context.builder()
            .supplier(new Supplier(1, "Test"))
            .category(new Category().setCategoryId(1))
            .build();

        Offer offer = OfferTestUtils.simpleOffer();
        offer.getServiceOffer(offer.getBusinessId())
            .ifPresent(serviceOffer -> serviceOffer.setServiceAcceptance(Offer.AcceptanceStatus.OK));

        DataCampUnitedOffer.UnitedOffer dcOffer = dataCampConverterService.convertToDataCampUpdate(context, offer,
            null);

        var serviceMap = dcOffer.getServiceMap();
        assertThat(serviceMap).isNotEmpty();

        for (var dcServiceOffer : serviceMap.values()) {
            var resolution = dcServiceOffer.getResolution();
            assertThat(resolution.getBySourceList()).isEmpty();
        }
    }

    @Test
    public void testAcceptedDSBSServiceOfferToHaveNoVerdicts() {
        testServiceOfferHidings(MbocSupplierType.DSBS, Offer.AcceptanceStatus.OK,
            offer -> {
                assertThat(offer.getResolution().getBySourceList())
                    .as("assert resolution is empty for " + MbocSupplierType.DSBS.name()
                        + " acceptance " + Offer.AcceptanceStatus.OK.name())
                    .isEmpty();

                assertThat(offer.getStatus().getDisabledList())
                    .as("assert disabled is false for " + MbocSupplierType.DSBS.name()
                        + " acceptance " + Offer.AcceptanceStatus.OK.name())
                    .hasSize(1)
                    .extracting(DataCampOfferMeta.Flag::getFlag)
                    .containsExactly(false);
            });
    }

    @Test
    public void testRejectedDSBSServiceOfferToHaveVerdicts() {
        testServiceOfferHidings(MbocSupplierType.DSBS, Offer.AcceptanceStatus.TRASH,
            offer -> {
                assertThat(offer.getResolution().getBySourceList())
                    .as("assert resolution not empty for " + MbocSupplierType.DSBS.name())
                    .isNotEmpty()
                    .flatExtracting(DataCampResolution.Verdicts::getVerdictList)
                    .flatExtracting(DataCampResolution.Verdict::getResultsList)
                    .flatExtracting(DataCampValidationResult.ValidationResult::getMessagesList)
                    .flatExtracting(DataCampExplanation.Explanation::getCode)
                    .as("assert resolution code for " + MbocSupplierType.DSBS.name())
                    .containsExactly("mboc.error.rejected-offer");

                assertThat(offer.getStatus().getDisabledList())
                    .as("assert disabled is false for " + MbocSupplierType.DSBS.name()
                        + " acceptance " + Offer.AcceptanceStatus.TRASH.name())
                    .hasSize(1)
                    .extracting(DataCampOfferMeta.Flag::getFlag)
                    .containsExactly(true);
            });
    }

    @Test
    public void testNotAcceptedDSBSServiceOfferToHaveVerdicts() {
        testServiceOfferHidings(MbocSupplierType.DSBS, Offer.AcceptanceStatus.NEW,
            offer -> {
                assertThat(offer.getResolution().getBySourceList())
                    .as("assert resolution not empty for " + MbocSupplierType.DSBS.name())
                    .isNotEmpty()
                    .flatExtracting(DataCampResolution.Verdicts::getVerdictList)
                    .flatExtracting(DataCampResolution.Verdict::getResultsList)
                    .flatExtracting(DataCampValidationResult.ValidationResult::getMessagesList)
                    .flatExtracting(DataCampExplanation.Explanation::getCode)
                    .as("assert resolution code for " + MbocSupplierType.DSBS.name())
                    .containsExactly("mboc.error.offer-not-accepted-yet");

                assertThat(offer.getStatus().getDisabledList())
                    .as("assert disabled is false for " + MbocSupplierType.DSBS.name()
                        + " acceptance " + Offer.AcceptanceStatus.TRASH.name())
                    .hasSize(1)
                    .extracting(DataCampOfferMeta.Flag::getFlag)
                    .containsExactly(true);
            });
    }

    @Test
    public void testAcceptedNotDSBSServiceOfferToHaveNoVerdictsAndNotDisabled() {
        Predicate<MbocSupplierType> notDSBS = supplierType -> supplierType != MbocSupplierType.DSBS;

        SoftAssertions softAssert = new SoftAssertions();

        getSupplierTypes(notDSBS).forEach(supplierType -> {
            testServiceOfferHidings(supplierType, Offer.AcceptanceStatus.OK,
                offer -> {
                    softAssert.assertThat(offer.getResolution().getBySourceList())
                        .as("assert resolution is empty for " + supplierType.name()
                            + " acceptance " + Offer.AcceptanceStatus.OK.name())
                        .isEmpty();

                    softAssert.assertThat(offer.getStatus().getDisabledList())
                        .as("assert disabled is false for " + supplierType.name()
                            + " acceptance " + Offer.AcceptanceStatus.OK.name())
                        .hasSize(1)
                        .extracting(DataCampOfferMeta.Flag::getFlag)
                        .containsExactly(false);
                });
        });
        softAssert.assertAll();
    }

    @Test
    public void testNotAcceptedNotDSBSServiceOfferToHaveNoVerdictsButDisabled() {
        var acceptanceStatuses = Arrays.stream(Offer.AcceptanceStatus.values());
        Predicate<MbocSupplierType> notDSBS = supplierType -> supplierType != MbocSupplierType.DSBS;

        var supplierTypeAndAcceptanceCombinations =
            acceptanceStatuses
                .filter(x -> x != Offer.AcceptanceStatus.OK)
                .flatMap(acceptanceStatus ->
                    getSupplierTypes(notDSBS).map(supplierType ->
                        Pair.of(acceptanceStatus, supplierType)))
                .collect(Collectors.toList());

        SoftAssertions softAssert = new SoftAssertions();

        supplierTypeAndAcceptanceCombinations.forEach(supplierTypeAndAcceptance -> {
            var acceptanceStatus = supplierTypeAndAcceptance.first;
            var supplierType = supplierTypeAndAcceptance.second;
            testServiceOfferHidings(supplierType, acceptanceStatus,
                offer -> {
                    softAssert.assertThat(offer.getResolution().getBySourceList())
                        .as("assert resolution is empty for " + supplierType.name()
                            + " acceptance " + acceptanceStatus.name())
                        .isEmpty();

                    softAssert.assertThat(offer.getStatus().getDisabledList())
                        .as("assert disabled is true for " + supplierType.name()
                            + " acceptance " + acceptanceStatus.name())
                        .hasSize(1)
                        .extracting(DataCampOfferMeta.Flag::getFlag)
                        .containsExactly(true);
                });
        });
        softAssert.assertAll();
    }

    private Stream<MbocSupplierType> getSupplierTypes(Predicate<MbocSupplierType> filter) {
        return Arrays.stream(MbocSupplierType.values())
            .filter(filter);
    }

    public void testServiceOfferHidings(MbocSupplierType supplierType,
                                        Offer.AcceptanceStatus acceptanceStatus,
                                        Consumer<DataCampOffer.Offer> offerAssertion) {
        Mockito.when(calculator.calculateRestriction(any(Offer.class)))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));
        Mockito.when(calculator.calculateRestriction(any(UltraController.EnrichedOffer.class), any()))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));

        Context context = Context.builder()
            .supplier(new Supplier(1, "Test"))
            .category(new Category().setCategoryId(1))
            .build();

        Offer offer = OfferTestUtils.simpleOffer();
        offer.getServiceOffer(offer.getBusinessId())
            .ifPresent(serviceOffer -> serviceOffer
                .setSupplierType(supplierType)
                .setServiceAcceptance(acceptanceStatus));

        DataCampUnitedOffer.UnitedOffer dcOffer = dataCampConverterService.convertToDataCampUpdate(context, offer,
            null);

        var serviceMap = dcOffer.getServiceMap();
        assertThat(serviceMap).isNotEmpty();

        var dcService = serviceMap.get(0);

        offerAssertion.accept(dcService);
    }

    @Test
    public void testBarcodeCheckWithoutBarcode() {
        Mockito.when(calculator.calculateRestriction(any(Offer.class)))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));
        Mockito.when(calculator.calculateRestriction(any(UltraController.EnrichedOffer.class), any()))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));

        var supplier = new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "Test").setFulfillment(true);
        Context context = Context.builder()
            .supplier(supplier)
            .category(new Category().setCategoryId(1))
            .serviceSuppliers(List.of(supplier))
            .build();

        Offer offer = OfferTestUtils.simpleOffer();
        offer.getServiceOffer(offer.getBusinessId())
            .ifPresent(serviceOffer -> serviceOffer
                .setSupplierType(MbocSupplierType.DSBS)
                .setServiceAcceptance(Offer.AcceptanceStatus.OK));

        DataCampUnitedOffer.UnitedOffer dcOffer = dataCampConverterService.convertToDataCampUpdate(context, offer,
            null);

        var verdictResults =
            dcOffer.getServiceMap().get(0).getResolution().getBySourceList().get(0).getVerdict(0).getResultsList();
        assertThat(verdictResults).hasSize(1);

        var messagesList = verdictResults.get(0).getMessagesList();
        assertThat(messagesList).hasSize(1);
        assertThat(messagesList.get(0).getCode()).isEqualTo("mboc.error.barcode-required");
    }

    @Test
    public void testBarcodeCheckWithInvalidGtin() {
        Mockito.when(calculator.calculateRestriction(any(Offer.class)))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));
        Mockito.when(calculator.calculateRestriction(any(UltraController.EnrichedOffer.class), any()))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));

        var vendor = new CachedGlobalVendor().setId(100).setRequireGtinBarcodes(true);
        var supplier = new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "Test").setFulfillment(true);
        Context context = Context.builder()
            .supplier(supplier)
            .category(new Category().setCategoryId(1))
            .serviceSuppliers(List.of(supplier))
            .vendors(Map.of(100L, Optional.of(vendor)))
            .build();

        Offer offer = OfferTestUtils.simpleOffer().setBarCode("INVALID-BARCODE").setVendorId(100);
        offer.getServiceOffer(offer.getBusinessId())
            .ifPresent(serviceOffer -> serviceOffer
                .setSupplierType(MbocSupplierType.DSBS)
                .setServiceAcceptance(Offer.AcceptanceStatus.OK));

        DataCampUnitedOffer.UnitedOffer dcOffer = dataCampConverterService.convertToDataCampUpdate(context, offer,
            null);

        var verdictResults =
            dcOffer.getServiceMap().get(0).getResolution().getBySourceList().get(0).getVerdict(0).getResultsList();
        assertThat(verdictResults).hasSize(1);

        var messagesList = verdictResults.get(0).getMessagesList();
        assertThat(messagesList).hasSize(1);
        assertThat(messagesList.get(0).getCode()).isEqualTo("mboc.error.barcode-required-gtin");
    }

    @Test
    public void testAllowCreateModelsInReopen() {
        Supplier supplier = OfferTestUtils.simpleSupplier()
            .setType(MbocSupplierType.BUSINESS)
            .setNewContentPipeline(true);
        Category category = new Category()
            .setCategoryId(1l)
            .setHasKnowledge(true)
            .setAcceptContentFromWhiteShops(true);
        Offer offer = OfferTestUtils.simpleOffer(supplier)
            .setDataCampOffer(true)
            .setCategoryIdForTests(category.getCategoryId(), Offer.BindingKind.APPROVED)
            .setMappingDestination(Offer.MappingDestination.WHITE)
            .setProcessingStatusInternal(Offer.ProcessingStatus.REOPEN);

        assertThat(dataCampConverterService
            .calculateAllowModelCreateUpdate(supplier, category, null, offer).isTrue())
            .isTrue();
    }

    @Test
    public void testEatsExportedWithoutServiceOffers() {
        Mockito.when(calculator.calculateRestriction(any(Offer.class)))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));
        Mockito.when(calculator.calculateRestriction(any(UltraController.EnrichedOffer.class), any()))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));

        var business = new Supplier(1, "eats_ogon")
            .setDatacamp(true)
            .setEats(true)
            .setType(MbocSupplierType.BUSINESS)
            .setNewContentPipeline(true);

        Context context = Context.builder()
            .supplier(business)
            .category(new Category().setCategoryId(1))
            .build();

        Offer offer = new Offer()
            .setBusinessId(business.getId())
            .setShopSku("offer-eats-ogon")
            .setTitle("title")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("category")
            .addNewServiceOfferIfNotExistsForTests(business);

        DataCampUnitedOffer.UnitedOffer dcOffer = dataCampConverterService.convertToDataCampUpdate(context, offer,
            null);

        var serviceMap = dcOffer.getServiceMap();
        assertThat(serviceMap).isEmpty();
    }

    @Test
    public void testEnrichBaseOfferWithContent() {
        var business = new Supplier(1, "eats_ogon")
            .setDatacamp(true)
            .setEats(true)
            .setType(MbocSupplierType.REAL_SUPPLIER)
            .setNewContentPipeline(true);

        OfferContent offerContent = OfferContent.builder()
            .addUrl("https://www.ya.ru")
            .description("description")
            .build();

        Offer offer = new Offer()
            .setBusinessId(business.getId())
            .setShopSku("offer-eats-ogon")
            .setTitle("title")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("category")
            .addNewServiceOfferIfNotExistsForTests(business)
            .setApprovedSkuCargoType(new Offer.SkuCargoType().setCargoType980(true))
            .setBarCode("123123")
            .setVendor("vendor")
            .setSupplierCategoryId(132L)
            .setVendorCode("vendorCode")
            .setGroupId(13)
            .setMarketModelName("market model name")

            .setAdult(true)
            .setUpdated(LocalDateTime.now())
            .storeOfferContent(offerContent);


        DataCampOfferMeta.UpdateMeta updateMeta =
            DataCampOfferUtil.createUpdateMeta(offer.getUpdated(), DataCampOfferMeta.DataSource.MARKET_MBO);


        var dcOffer = getDCOfferBuilder().build();
        var masterData = buildMasterData();

        DataCampOffer.Offer result = OfferToDataCampContentConverter.enrichWithContent(offer, dcOffer, masterData);
        var content = result.getContent();
        var partner = content.getPartner();
        var original = partner.getOriginal();
        var terms = partner.getOriginalTerms();
        Assertions.assertThat(content).isNotNull();
        Assertions.assertThat(original).isNotNull();
        Assertions.assertThat(original.getName()).isNotNull();
        Assertions.assertThat(original.getName().getValue()).isEqualTo(offer.getTitle());
        Assertions.assertThat(original.getName().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getVendor()).isNotNull();
        Assertions.assertThat(original.getVendor().getValue()).isEqualTo(offer.getVendor());
        Assertions.assertThat(original.getVendor().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getVendorCode()).isNotNull();
        Assertions.assertThat(original.getVendorCode().getValue()).isEqualTo(offer.getVendorCode());
        Assertions.assertThat(original.getVendorCode().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getDescription()).isNotNull();
        Assertions.assertThat(original.getDescription().getValue()).isEqualTo(offer.extractOfferContent().getDescription());
        Assertions.assertThat(original.getDescription().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getGroupId()).isNotNull();
        Assertions.assertThat(original.getGroupId().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getModel()).isNotNull();
        Assertions.assertThat(original.getModel().getValue()).isEqualTo(offer.getMarketModelName());
        Assertions.assertThat(original.getModel().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getCargoTypes()).isNotNull();
        Assertions.assertThat(original.getCargoTypes().getValue(0)).isEqualTo(980);
        Assertions.assertThat(original.getCargoTypes().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getCategory()).isNotNull();
        Assertions.assertThat(original.getCategory().getName()).isEqualTo(offer.getShopCategoryName());
        Assertions.assertThat(original.getCategory().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getCategory().getId()).isEqualTo(offer.getSupplierCategoryId());
        Assertions.assertThat(original.getCategory().getBusinessId()).isEqualTo(offer.getBusinessId());
        Assertions.assertThat(original.getBarcode()).isNotNull();
        Assertions.assertThat(original.getBarcode().getValue(0)).isEqualTo(offer.getAllBarCodes().get(0));
        Assertions.assertThat(original.getBarcode().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.hasAdult()).isEqualTo(true);
        Assertions.assertThat(original.getAdult().getFlag()).isEqualTo(offer.getAdult());
        Assertions.assertThat(original.getAdult().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getUrl().getValue()).isEqualTo(offer.extractOfferContent().getUrls().get(0));
        Assertions.assertThat(original.getUrl().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(result.getPrice()).isNotNull();
        Assertions.assertThat(original.getAnimalProducts()).isNotNull();
        Assertions.assertThat(original.getAnimalProducts().getFlag()).isEqualTo(masterData.getUseInMercury());
        Assertions.assertThat(original.getAnimalProducts().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getMercuryGuid()).isNotNull();
        Assertions.assertThat(original.getMercuryGuid().getValueCount()).isEqualTo(2);
        Assertions.assertThat(original.getMercuryGuid().getValue(0)).isEqualTo("vetis1");
        Assertions.assertThat(original.getMercuryGuid().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getMercuryGuid().getValue(1)).isEqualTo("vetis2");
        Assertions.assertThat(original.getTnVedCode()).isNotNull();
        Assertions.assertThat(original.getTnVedCode().getValueCount()).isEqualTo(1);
        Assertions.assertThat(original.getTnVedCode().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getTnVedCode().getValue(0)).isEqualTo("custom code");
        Assertions.assertThat(original.getDimensions()).isNotNull();
        Assertions.assertThat(original.getDimensions().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getDimensions().getHeightMkm()).isEqualTo(10L);
        Assertions.assertThat(original.getDimensions().getWidthMkm()).isEqualTo(10L);
        Assertions.assertThat(original.getDimensions().getLengthMkm()).isEqualTo(10L);
        Assertions.assertThat(terms.getBoxCount()).isNotNull();
        Assertions.assertThat(terms.getBoxCount().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(terms.getBoxCount().getValue()).isEqualTo(13);
        Assertions.assertThat(original.getCertificates()).isNotNull();
        Assertions.assertThat(original.getCertificates().getValueCount()).isEqualTo(1);
        Assertions.assertThat(original.getCertificates().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getCertificates().getValue(0)).isEqualTo("AB-15");
        Assertions.assertThat(original.getLifespan()).isNotNull();
        Assertions.assertThat(original.getLifespan().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getLifespan().getServiceLifePeriod().getDays()).isEqualTo(321);
        Assertions.assertThat(terms.getSellerWarranty()).isNotNull();
        Assertions.assertThat(terms.getSellerWarranty().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(terms.getSellerWarranty().getWarrantyPeriod()).isNotNull();
        Assertions.assertThat(terms.getSellerWarranty().getWarrantyPeriod().getDays()).isNotNull();
        Assertions.assertThat(terms.getSellerWarranty().getWarrantyPeriod().getDays()).isEqualTo(150);
        Assertions.assertThat(original.getLifespan().getServiceLifeComment()).isNotNull();
        Assertions.assertThat(original.getLifespan().getServiceLifeComment()).isEqualTo("lifetime comment");
        Assertions.assertThat(original.getWeight()).isNotNull();
        Assertions.assertThat(original.getWeight().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getWeight().getValueMg()).isEqualTo(1000L);
        Assertions.assertThat(original.getWeightNet()).isNotNull();
        Assertions.assertThat(original.getWeightNet().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getWeightNet().getValueMg()).isEqualTo(1500L);
    }

    @Test
    public void whenEnrichWithEmptyValuesThenMetaIsWritten() {
        Offer offer = new Offer()
            .setTitle("offer title")
            .setUpdated(LocalDateTime.now());

        DataCampOfferMeta.UpdateMeta updateMeta =
            DataCampOfferUtil.createUpdateMeta(offer.getUpdated(), DataCampOfferMeta.DataSource.MARKET_MBO);

        var dcOffer = getDCOfferBuilder().build();
        var masterData = new MasterDataDto();

        DataCampOffer.Offer result = OfferToDataCampContentConverter.enrichWithContent(offer, dcOffer, masterData);

        var content = result.getContent();
        var partner = content.getPartner();
        var original = partner.getOriginal();
        var terms = partner.getOriginalTerms();
        Assertions.assertThat(content).isNotNull();
        Assertions.assertThat(original).isNotNull();
        Assertions.assertThat(original.getName()).isNotNull();
        Assertions.assertThat(original.getName().getValue()).isEqualTo(offer.getTitle());
        Assertions.assertThat(original.getName().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getVendor()).isNotNull();
        Assertions.assertThat(original.getVendor().hasValue()).isFalse();
        Assertions.assertThat(original.getVendor().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getVendorCode()).isNotNull();
        Assertions.assertThat(original.getVendorCode().hasValue()).isFalse();
        Assertions.assertThat(original.getVendorCode().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getDescription()).isNotNull();
        Assertions.assertThat(original.getDescription().hasValue()).isFalse();
        Assertions.assertThat(original.getDescription().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getGroupId()).isNotNull();
        Assertions.assertThat(original.getGroupId().hasValue()).isFalse();
        Assertions.assertThat(original.getModel()).isNotNull();
        Assertions.assertThat(original.getModel().hasValue()).isFalse();
        Assertions.assertThat(original.getModel().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getCargoTypes()).isNotNull();
        Assertions.assertThat(original.getCargoTypes().getValueCount()).isEqualTo(0);
        Assertions.assertThat(original.getCargoTypes().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getCategory()).isNotNull();
        Assertions.assertThat(original.getCategory().hasName()).isFalse();
        Assertions.assertThat(original.getCategory().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getBarcode()).isNotNull();
        Assertions.assertThat(original.getBarcode().getValueCount()).isEqualTo(0);
        Assertions.assertThat(original.getBarcode().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getAdult().hasFlag()).isFalse();
        Assertions.assertThat(original.getAdult()).isNotNull();
        Assertions.assertThat(original.getAdult().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getUrl().hasValue()).isFalse();
        Assertions.assertThat(original.getUrl().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(result.getPrice().hasOriginalPriceFields()).isFalse();
        Assertions.assertThat(original.getAnimalProducts()).isNotNull();
        Assertions.assertThat(original.getAnimalProducts().hasFlag()).isFalse();
        Assertions.assertThat(original.getAnimalProducts().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getMercuryGuid()).isNotNull();
        Assertions.assertThat(original.getMercuryGuid().getValueCount()).isEqualTo(0);
        Assertions.assertThat(original.getMercuryGuid().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getTnVedCode()).isNotNull();
        Assertions.assertThat(original.getTnVedCode().getValueCount()).isEqualTo(0);
        Assertions.assertThat(original.getTnVedCode().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getTnVedCode().getValueCount()).isEqualTo(0);
        Assertions.assertThat(original.getDimensions()).isNotNull();
        Assertions.assertThat(original.getDimensions().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getDimensions().hasHeightMkm()).isFalse();
        Assertions.assertThat(original.getDimensions().hasWidthMkm()).isFalse();
        Assertions.assertThat(original.getDimensions().hasLengthMkm()).isFalse();
        Assertions.assertThat(terms.getBoxCount()).isNotNull();
        Assertions.assertThat(terms.getBoxCount().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(terms.getBoxCount().hasValue()).isFalse();
        Assertions.assertThat(original.getCertificates()).isNotNull();
        Assertions.assertThat(original.getCertificates().getValueCount()).isEqualTo(0);
        Assertions.assertThat(original.getCertificates().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(terms.getSellerWarranty()).isNotNull();
        Assertions.assertThat(terms.getSellerWarranty().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(terms.getSellerWarranty().getWarrantyPeriod()).isNotNull();
        Assertions.assertThat(terms.getSellerWarranty().hasWarrantyPeriod()).isFalse();
        Assertions.assertThat(original.getLifespan()).isNotNull();
        Assertions.assertThat(original.getLifespan().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getLifespan().hasServiceLifePeriod()).isFalse();
        Assertions.assertThat(original.getLifespan().getServiceLifeComment()).isNotNull();
        Assertions.assertThat(original.getLifespan().hasServiceLifeComment()).isFalse();
        Assertions.assertThat(original.getWeight()).isNotNull();
        Assertions.assertThat(original.getWeight().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getWeight().hasGrams()).isFalse();
        Assertions.assertThat(original.getWeightNet()).isNotNull();
        Assertions.assertThat(original.getWeightNet().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(original.getWeightNet().hasGrams()).isFalse();
    }

    @Test
    public void testEnrichServiceOfferWithContent() {
        var business = new Supplier(1, "eats_ogon")
            .setDatacamp(true)
            .setEats(true)
            .setType(MbocSupplierType.REAL_SUPPLIER)
            .setNewContentPipeline(true);

        OfferContent offerContent = OfferContent.builder()
            .addUrl("https://www.ya.ru")
            .description("description")
            .build();

        Offer offer = new Offer()
            .setBusinessId(business.getId())
            .setShopSku("offer-eats-ogon")
            .setTitle("title")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("category")
            .addNewServiceOfferIfNotExistsForTests(business)
            .setApprovedSkuCargoType(new Offer.SkuCargoType().setCargoType980(true))
            .setBarCode("123123")
            .setVendor("vendor")
            .setSupplierCategoryId(132L)
            .setVendorCode("vendorCode")
            .setGroupId(13)
            .setMarketModelName("market model name")
            .setAdult(true)
            .setUpdated(LocalDateTime.now())
            .storeOfferContent(offerContent);

        DataCampOfferMeta.UpdateMeta updateMeta =
            DataCampOfferUtil.createUpdateMeta(offer.getUpdated(), DataCampOfferMeta.DataSource.MARKET_MBO);

        var dcOfferBuilder = getDCOfferBuilder();
        var masterData = buildMasterData();

        OfferToDataCampContentConverter.enrichWithServiceMasterData(dcOfferBuilder, updateMeta, masterData);
        var dcOffer = dcOfferBuilder.build();
        var content = dcOffer.getContent();
        var partner = content.getPartner();
        var terms = partner.getOriginalTerms();
        Assertions.assertThat(terms.getSupplyQuantity()).isNotNull();
        Assertions.assertThat(terms.getSupplyQuantity().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(terms.getSupplyQuantity().getMin()).isEqualTo(masterData.getMinShipment());
        Assertions.assertThat(terms.getSupplyQuantity().getStep()).isEqualTo(masterData.getQuantumOfSupply());
        Assertions.assertThat(terms.getTransportUnitSize()).isNotNull();
        Assertions.assertThat(terms.getTransportUnitSize().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(terms.getTransportUnitSize().getValue()).isEqualTo(masterData.getTransportUnitSize());
        Assertions.assertThat(terms.getSupplyWeekdays()).isNotNull();
        Assertions.assertThat(terms.getSupplyWeekdays().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(terms.getSupplyWeekdays().getDaysList())
            .isEqualTo(List.of(DataCampCommonTypes.DayOfWeek.valueOf(DayOfWeek.MONDAY.name()),
                DataCampCommonTypes.DayOfWeek.valueOf(DayOfWeek.THURSDAY.name())));
        Assertions.assertThat(terms.getPartnerDeliveryTime()).isNotNull();
        Assertions.assertThat(terms.getPartnerDeliveryTime().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(terms.getPartnerDeliveryTime().getValue()).isEqualTo(masterData.getDeliveryTime());
        Assertions.assertThat(dcOffer.getPrice()).isNotNull();
        Assertions.assertThat(dcOffer.getPrice().getOriginalPriceFields()).isNotNull();
        Assertions.assertThat(dcOffer.getPrice().getOriginalPriceFields().getVat().getValue().getNumber()).isEqualTo(masterData.getVatId());
        Assertions.assertThat(dcOffer.getPrice().getOriginalPriceFields().getVat().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(terms.getQuantityInPack()).isNotNull();
        Assertions.assertThat(terms.getQuantityInPack().getMeta()).isEqualTo(updateMeta);
        Assertions.assertThat(terms.getQuantityInPack().getValue()).isEqualTo(masterData.getQuantityInPack());
    }

    @Test
    public void testParallelImportedOffer() {
        var business = new Supplier(1, "some-supplier")
            .setType(MbocSupplierType.REAL_SUPPLIER);
        var offer = OfferTestUtils.simpleOffer(business)
            .setParallelImported(true);
        var dcOffer = getDCOfferBuilder().build();
        var masterData = buildMasterData();

        DataCampOffer.Offer result = OfferToDataCampContentConverter.enrichWithContent(offer, dcOffer, masterData);
        var content = result.getContent();
        Assertions.assertThat(content).isNotNull();
        var partner = content.getPartner();
        Assertions.assertThat(partner).isNotNull();
        var original = partner.getOriginal();
        Assertions.assertThat(original).isNotNull();
        var terms = partner.getOriginalTerms();
        Assertions.assertThat(terms).isNotNull();
        Assertions.assertThat(terms.getParallelImported().getFlag()).isTrue();
    }

    @Test
    public void testNotParallelImportedOffer() {
        var business = new Supplier(1, "some-supplier")
            .setType(MbocSupplierType.REAL_SUPPLIER);
        var offer = OfferTestUtils.simpleOffer(business);
        var dcOffer = getDCOfferBuilder().build();
        var masterData = buildMasterData();

        DataCampOffer.Offer result = OfferToDataCampContentConverter.enrichWithContent(offer, dcOffer, masterData);
        var content = result.getContent();
        Assertions.assertThat(content).isNotNull();
        var partner = content.getPartner();
        Assertions.assertThat(partner).isNotNull();
        var original = partner.getOriginal();
        Assertions.assertThat(original).isNotNull();
        var terms = partner.getOriginalTerms();
        Assertions.assertThat(terms).isNotNull();
        Assertions.assertThat(terms.getParallelImported().getFlag()).isFalse();
    }

    private MasterDataDto buildMasterData() {
        var masterData = new MasterDataDto();
        masterData.setBoxCount(13);
        masterData.setDeliveryTime(150);
        masterData.setLifeTime(new TimeInUnits(321, TimeInUnits.TimeUnit.DAY));
        masterData.setBoxDimensionLengthInUm(10L);
        masterData.setBoxDimensionWidthInUm(10L);
        masterData.setBoxDimensionHeightInUm(10L);
        masterData.setCustomsCommodityCode("custom code");
        masterData.setDangerousGood(true);
        masterData.setGtins(List.of("gtin1", "gtin2"));
        masterData.setGuaranteePeriod(new TimeInUnits(150, TimeInUnits.TimeUnit.DAY));
        masterData.setDatacampMasterDataVersion(1L);
        masterData.setLifeTimeComment("lifetime comment");
        masterData.setHeavyGood(true);
        masterData.setUseInMercury(true);
        masterData.setGrossWeight(1000L);
        masterData.setVatId(VatRate.NO_VAT.getId());
        masterData.setNetWeight(1500L);
        masterData.setVetisGuids(List.of("vetis1", "vetis2"));
        masterData.setQualityDocumentsNumbers(List.of("AB-15"));
        masterData.setSupplySchedule(List.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY));
        masterData.setMinShipment(1);
        masterData.setQuantumOfSupply(1);
        masterData.setTransportUnitSize(1);
        masterData.setQuantityInPack(1);
        return masterData;
    }

    private DataCampOffer.Offer.Builder getDCOfferBuilder() {
        return DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId("offer1234")
                .setBusinessId(123)
                .build()
            )
            .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                .setRgb(DataCampOfferMeta.MarketColor.BLUE)
                .build()
            )
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                    .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                        .setTitle(DataCampOfferMeta.StringValue.newBuilder().setValue("TITLE").build())
                        .setVendor(DataCampOfferMeta.StringValue.newBuilder().setValue("VENDOR").build())
                        .setVendorCode(DataCampOfferMeta.StringValue.newBuilder().setValue("VENDOR_CODE").build())
                        .setBarcode(DataCampOfferMeta.StringListValue.newBuilder()
                            .addValue("BAR_CODE1")
                            .addValue("BAR_CODE2")
                            .build()
                        )
                        .setDescription(DataCampOfferMeta.StringValue.newBuilder().setValue("DESCRIPTION").build())
                        .setUrl(DataCampOfferMeta.StringValue.newBuilder().setValue("http://the.url").build())
                        .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                            .setName("CATEGORY")
                            .build())
                        .build()
                    )
                    .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                        .setGroupId(DataCampOfferMeta.Ui32Value.newBuilder()
                            .setValue(1)
                            .build())
                        .build())
                    .build()
                )
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(12)
                        .setMarketModelId(23)
                        .setMarketSkuId(34)
                        .build()
                    )
                    .build()
                )
                .build()
            );
    }

    private void assertOfferHasRestriction(DataCampUnitedOffer.UnitedOffer uOffer,
                                           DataCampContentStatus.CategoryRestriction restriction) {
        assertOfferHasRestriction(uOffer.getBasic(), restriction);
    }

    private void assertOfferHasRestriction(DataCampOffer.Offer dcOffer,
                                           DataCampContentStatus.CategoryRestriction restriction) {
        assertThat(dcOffer.getContent().getStatus().getContentSystemStatus()
            .getCategoryRestriction())
            .isEqualTo(restriction);
    }
}
