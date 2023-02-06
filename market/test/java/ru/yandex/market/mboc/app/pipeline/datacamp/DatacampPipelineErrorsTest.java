package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.util.List;
import java.util.Set;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import Market.UltraControllerServiceData.UltraController;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.pipeline.GenericScenario;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.logbroker.events.ThrottlingLogbrokerEventPublisher;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.OfferValidator;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandler;
import ru.yandex.market.mboc.common.services.proto.AddProductInfoListener;
import ru.yandex.market.mboc.common.services.proto.datacamp.DatacampContext;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappings;

import static Market.DataCamp.DataCampExplanation.Explanation.Level.ERROR;
import static org.mockito.ArgumentMatchers.any;

public class DatacampPipelineErrorsTest extends BaseDatacampPipelineTest {

    @Before
    public void setUpDc() throws Exception {
        super.setUpDc();
        categoryCachingService
            .setAcceptContentFromWhiteShops(OfferTestUtils.TEST_CATEGORY_INFO_ID, false);

        addProductInfoHelperService = Mockito.spy(addProductInfoHelperService);

        Mockito.doThrow(new RuntimeException("test"))
            .when(addProductInfoHelperService)
            .addProductInfo(
                any(MboMappings.ProviderProductInfoRequest.class),
                any(DatacampContext.class),
                any(AddProductInfoListener.class));

        logbrokerDatacampOfferMessageHandler = new LogbrokerDatacampOfferMessageHandler(
            addProductInfoHelperService,
            new ThrottlingLogbrokerEventPublisher<>(logbrokerEventPublisherMock),
            categoryCachingService,
            supplierService,
            dataCampIdentifiersService,
            dataCampConverterService,
            datacampImportService,
            offerRepository,
            mskuRepository,
            migrationService,
            removeOfferService,
            globalVendorsCachingService,
            storageKeyValueService,
            antiMappingRepository,
            new ContextedOfferDestinationCalculator(categoryInfoCache, storageKeyValueService),
            migrationModelService,
            SupplierConverterServiceMock.BERU_BUSINESS_ID
        );
    }

    @Test
    public void processWhiteWithContentSavedWithError() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withDefaultMarketSpecificContent()
            .get().build();

        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import white offer auto-accepted")
            .action(importOfferFromDCWithExhaustedRetry(toMessage(OfferTestUtils.WHITE_SUPPLIER_ID, dcOffer)))
            .ignoreDefaultCheck()
            .addChecks(List.of(
                assertNoOfferInRepo(),
                assertLastDCContentSystemStatus(),
                verifyContentSystemStatusNoDiff(dcOffer),
                assertLastDCServiceOffers(),
                assertLastDCBasicOffer()
            ))
            .expectedState(DCPipelineState.onlyOffer(testDCWhiteOffer())
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(true)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(false)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .addBasicVerdict(
                    MbocErrors.get().contentProcessingInternalError(SHOP_SKU_DCP, "stub").getErrorCode(),
                    ERROR)
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING)
                    .withDisabledFlag(false).build()))
            .endStep()

            .execute();
    }

    @Test
    public void processWhiteBlueSavedWithError() {
        Supplier blueSupplier = supplierService.getCachedById(OfferTestUtils.BLUE_SUPPLIER_ID_1);
        DataCampOffer.Offer dcOffer = OfferBuilder.create(initialOffer())
            .withDefaultMarketContent(market ->
                market.getIrDataBuilder()
                    .setClassifierCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setClassifierConfidentTopPercision(0.99)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN))
            .get().build();

        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import white-blue offer with failure produces NEED_INFO")
            .action(importOfferFromDCWithExhaustedRetry(toMessage(Set.of(
                OfferTestUtils.WHITE_SUPPLIER_ID,
                OfferTestUtils.BLUE_SUPPLIER_ID_1),
                dcOffer)))
            .ignoreDefaultCheck()
            .addChecks(List.of(
                assertNoOfferInRepo(),
                assertLastDCContentSystemStatus(),
                assertLastDCServiceOffers(),
                assertLastDCBasicOffer()
            ))
            .addCheck(verifyContentSystemStatusHandle(dcOffer, st -> st
                .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(blueSupplier))
                .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setAllowCategorySelection(false)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(false)
                    .setModelBarcodeRequired(true)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_INFO)
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                .addBasicVerdict(
                    MbocErrors.get().contentProcessingInternalError(SHOP_SKU_DCP, "stub").getErrorCode(),
                    ERROR)
                .modifyDatacampServiceOffer(OfferTestUtils.WHITE_SUPPLIER_ID, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.WHITE_SUPPLIER_ID, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_MAPPING)
                    .withDisabledFlag(false).build())
                .modifyDatacampServiceOffer(OfferTestUtils.BLUE_SUPPLIER_ID_1, nullValue -> OfferBuilder.create()
                    .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1, SHOP_SKU_DCP)
                    .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_INFO)
                    .withDisabledFlag(false).build()))
            .endStep()

            .execute();
    }

    @Test
    public void processBlueUpdatedWithError() {
        Mockito.doCallRealMethod()
            .when(addProductInfoHelperService)
            .addProductInfo(
                any(MboMappings.ProviderProductInfoRequest.class),
                any(DatacampContext.class),
                any(AddProductInfoListener.class));

        Supplier supplier = OfferTestUtils.blueSupplierUnderBiz1();
        var dcOffer = OfferBuilder.create(initialOffer())
            .withDefaultMarketContent(market ->
                market.getIrDataBuilder()
                    .setClassifierCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setClassifierConfidentTopPercision(0.99)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN))
            .get().build();
        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import blue service offer")
            .action(importOfferFromDCWithExhaustedRetry(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(supplier)
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
                .setClassifierConfidenceInternal(0.99)
                .setAutomaticClassification(true)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)))
            .endStep()

            .step("Send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(
                scenario.previousValidState()
                    .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setAllowCategorySelection(false)
                        .setAllowModelSelection(true)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                        .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                    .modifyDatacampServiceOffer(supplier.getId(), nullValue -> OfferBuilder.create()
                        .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1,
                            SHOP_SKU_DCP)
                        .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                        .withDisabledFlag(true).build()))
            .endStep()

            .step("mock addProductInfo failure")
            .action(run(offer -> Mockito.doThrow(new RuntimeException("test"))
                .when(addProductInfoHelperService)
                .addProductInfo(
                    any(MboMappings.ProviderProductInfoRequest.class),
                    any(DatacampContext.class),
                    any(AddProductInfoListener.class))))
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("Import blue service offer with failure and nothing changes")
            .action(importOfferFromDCWithExhaustedRetry(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1,
                OfferBuilder.create(dcOffer)
                .withPartnerMapping(
                    OfferBuilder.categoryMapping(OfferTestUtils.TEST_CATEGORY_INFO_ID + 1))
                .get().build())))
            .expectedState(scenario.previousValidState()
                .addBasicVerdict(
                    MbocErrors.get().contentProcessingInternalError(SHOP_SKU_DCP, "stub").getErrorCode(),
                    ERROR))
            .endStep()

            .step("Send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusNoDiff(dcOffer))
            .expectedState(scenario.previousValidState())
            .endStep()

            .execute();
    }

    @Test
    public void processBlueSavedWithErrorAsInvalidThenCorrectUpdateComes() {
        var info = categoryInfoRepository.findById(OfferTestUtils.TEST_CATEGORY_INFO_ID);
        info.setManualAcceptance(false);
        info.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        info.setFbyPlusAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        info.setFbsAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        info.setDsbsAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfoRepository.update(info);

        Mockito.doCallRealMethod()
            .when(addProductInfoHelperService)
            .addProductInfo(
                any(MboMappings.ProviderProductInfoRequest.class),
                any(DatacampContext.class),
                any(AddProductInfoListener.class));

        Supplier supplier = OfferTestUtils.blueSupplierUnderBiz1();
        var dcOfferBuilder = OfferBuilder.create(initialOffer())
            .withDefaultMarketContent(market ->
                market.getIrDataBuilder()
                    .setClassifierCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                    .setClassifierConfidentTopPercision(0.99)
                    .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN))
            .get();
        DataCampOffer.Offer correctDcOffer = dcOfferBuilder.build();
        dcOfferBuilder.getContentBuilder()
            .getPartnerBuilder()
            .getActualBuilder()
            .clearTitle()
            .clearCategory();
        var dcOffer = dcOfferBuilder.build();

        ultraControllerServiceMock.createMockResponse(List.of(dcOffer));

        GenericScenario<DCPipelineState> scenario = createScenario();
        scenario
            .step("Import blue service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(DCPipelineState.onlyOffer(testDCOffer(supplier)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.INVALID)
                .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.NEW)
                .setTitle("-")
                .setShopCategoryName("-")
                .setOfferErrors(List.of(MbocErrors.get().excelValueIsRequired(OfferValidator.TITLE)))
                .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
                .setClassifierConfidenceInternal(0.99)
                .setAutomaticClassification(false)
                .setMarketVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME)
                .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
                .setSuggestCategoryMappingId(OfferTestUtils.TEST_CATEGORY_INFO_ID)))
            .endStep()

            .step("Send state to DataCamp 1")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusHandle(dcOffer, st -> st
                .setAllowCategorySelection(false)))
            .expectedState(
                scenario.previousValidState()
                    .modifyDatacampStatus(nullStatus -> DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(true)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_INFO)
                        .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder().setCounter(0)))
                    .addBasicVerdict(
                        MbocErrors.get().excelValueIsRequired(OfferValidator.TITLE).getErrorCode(),
                        ERROR)
                    .modifyDatacampServiceOffer(supplier.getId(), nullValue -> OfferBuilder.create()
                        .withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.BLUE_SUPPLIER_ID_1,
                            SHOP_SKU_DCP)
                        .withServiceOfferState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_INFO)
                        .withDisabledFlag(true).build()))
            .endStep()

            .step("Import blue service offer again, offer is stays invalid")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, dcOffer)))
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("Send state to DataCamp 2")
            .action(tmsSendDataCampOfferStates())
            .addCheck(verifyContentSystemStatusHandle(dcOffer, st -> st
                .setAllowCategorySelection(false)))
            .expectedState(scenario.previousValidState())
            .endStep()

            .step("Import correct blue service offer")
            .action(importOfferFromDC(toMessage(OfferTestUtils.BLUE_SUPPLIER_ID_1, correctDcOffer)))
            .expectedState(scenario.previousValidState()
                .modifyOffer(offer -> offer
                    .resolveInvalidProcessingStatus(Offer.ProcessingStatus.IN_PROCESS)
                    .incrementProcessingCounter()
                    .setTitle("title")
                    .updateAcceptanceStatusForTests(OfferTestUtils.BLUE_SUPPLIER_ID_1, Offer.AcceptanceStatus.OK)
                    .setShopCategoryName("shop-category-name")
                    .setBindingKind(Offer.BindingKind.APPROVED)
                    .setAutomaticClassification(true)
                    .setOfferErrors(null)))
            .endStep()

            .execute();
    }
}
