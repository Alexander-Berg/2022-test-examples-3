package ru.yandex.market.mboc.common.services.datacamp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPictures;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.PartnerCategoryOuterClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.logbroker.LogbrokerEventPublisherMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.proto.AddProductInfoHelperServiceTestBase;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.datacamp.model.DataCampUnitedOffersEvent;
import ru.yandex.market.mboc.common.datacamp.repository.TempImportChangeDeltaRepository;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.datacamp.service.DatacampImportService;
import ru.yandex.market.mboc.common.datacamp.service.converter.DataCampConverterService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationRemovedOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.RemovedOffer;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.honestmark.ClassificationResult;
import ru.yandex.market.mboc.common.honestmark.HonestMarkDepartmentService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.logbroker.events.ThrottlingLogbrokerEventPublisher;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.MigrationModelRepository;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.migration.MigrationModelService;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.processing.RemoveOfferService;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.RealConverter;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.DEFAULT_SHOP_SKU;

@SuppressWarnings("checkstyle:magicnumber")
@Slf4j
public class LogbrokerDatacampOfferMessageHandlerDbTest extends AddProductInfoHelperServiceTestBase {
    private static final int BERU_BUSINESS_ID = 565853;
    private static final int REAL_SUPPLIER_ID = 44;
    private static final String REAL_SUPPLIER_EXT_ID = "real";
    private static final int CATEGORY_WITH_KNOWLEDGE = 999;
    public static final int NEW_CONTENT_SUPPLIER = 99;
    private static final int MODEL_ID_999 = 109;
    private static final int UNITED_SIZE_ID = 25911110;
    private LogbrokerDatacampOfferMessageHandler handler;
    private LogbrokerEventPublisherMock<DataCampUnitedOffersEvent> logbrokerEventPublisherMock;
    private MskuRepository mskuRepository;
    private RemoveOfferService removeOfferService;
    private MigrationModelRepository migrationModelRepository;
    private MigrationModelService migrationModelService;

    private SupplierConverterServiceMock supplierConverterService;

    @Autowired
    TransactionHelper transactionHelper;
    OfferCategoryRestrictionCalculator offerCategoryRestrictionCalculator;
    @Autowired
    DatacampImportService datacampImportService;
    @Autowired
    TempImportChangeDeltaRepository tempImportChangeDeltaRepository;

    ContentProcessingQueueRepository datacampOfferRepository;

    @Before
    public void setUpLb() {
        datacampOfferRepository = mock(ContentProcessingQueueRepository.class);
        Category categoryWithKnowledge = new Category();
        categoryWithKnowledge.setCategoryId(CATEGORY_WITH_KNOWLEDGE);
        categoryWithKnowledge.setHasKnowledge(true);
        categoryWithKnowledge.setAcceptContentFromWhiteShops(true);
        categoryWithKnowledge.setAcceptGoodContent(true);
        categoryWithKnowledge.setAllowPskuWithoutBarcode(true);
        offerCategoryRestrictionCalculator =
            new OfferCategoryRestrictionCalculator(Mockito.mock(HonestMarkDepartmentService.class), categoryInfoCache);
        categoryCachingService.addCategory(categoryWithKnowledge);
        modelServiceMock.addModel(new Model()
            .setId(MODEL_ID_999)
            .setCategoryId(999)
            .setTitle("TestTitle9999")
            .setPublishedOnBlueMarket(true)
            .setSkuModel(true)
            .setModelType(Model.ModelType.GURU));
        modelServiceMock.addModel(new Model()
            .setId(MODEL_ID_999)
            .setCategoryId(999)
            .setTitle("TestTitle9999")
            .setPublishedOnBlueMarket(true)
            .setSkuModel(true)
            .setModelType(Model.ModelType.GURU));
        supplierRepository.insertBatch(
            supplierRepository.findById(WHITE_SHOP_ID)
                .setId(NEW_CONTENT_SUPPLIER)
                .setType(MbocSupplierType.BUSINESS)
                .setNewContentPipeline(true)
                .setDatacamp(true),
            new Supplier(REAL_SUPPLIER_ID, "Test Real Supplier")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId(REAL_SUPPLIER_EXT_ID),
            new Supplier(BERU_BUSINESS_ID, "Yandex.Market Biz")
                .setType(MbocSupplierType.BUSINESS),
            new Supplier(SupplierConverterServiceMock.BERU_ID, "Yandex.Market")
                .setBusinessId(BERU_BUSINESS_ID)
                .setType(MbocSupplierType.FIRST_PARTY)
                .setDatacamp(true)
        );
        supplierRepository.updateBatch(
            supplierRepository.findById(WHITE_SHOP_ID)
                .setBusinessId(NEW_CONTENT_SUPPLIER)
                .setDatacamp(true),
            supplierRepository.findById(BLUE_SHOP_ID)
                .setBusinessId(NEW_CONTENT_SUPPLIER)
                .setDatacamp(true));

        logbrokerEventPublisherMock = new LogbrokerEventPublisherMock<>();

        supplierConverterService = new SupplierConverterServiceMock();
        DataCampIdentifiersService dataCampIdentifiersService = new DataCampIdentifiersService(
            SupplierConverterServiceMock.BERU_ID, BERU_BUSINESS_ID, supplierConverterService);

        var offerCategoryRestrictionCalculator = Mockito.mock(OfferCategoryRestrictionCalculator.class);
        Mockito.when(offerCategoryRestrictionCalculator
                .calculateClassificationResult(any(UltraController.EnrichedOffer.class), any()))
            .thenReturn(ClassificationResult.UNCONFIDENT_ALLOW_GC);

        DataCampConverterService dataCampConverterService = new DataCampConverterService(
            dataCampIdentifiersService,
            offerCategoryRestrictionCalculator,
            storageKeyValueService,
            true);

        mskuRepository = Mockito.mock(MskuRepository.class);
        TestUtils.mockMskuRepositoryFindTitles(mskuRepository);
        removeOfferService = new RemoveOfferService(
            removedOfferRepository,
            offerRepository,
            datacampImportService,
            migrationService,
            transactionHelper,
            offerDestinationCalculator
        );
        migrationModelRepository = Mockito.mock(MigrationModelRepository.class);
        migrationModelService = new MigrationModelService(
            migrationModelRepository,
            mskuRepository
        );

        handler = new LogbrokerDatacampOfferMessageHandler(
            service,
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
            offerDestinationCalculator,
            migrationModelService,
            SupplierConverterServiceMock.BERU_BUSINESS_ID
        );
        doAnswer(i -> {
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(22L, Offer.BindingKind.SUGGESTED)
                .setAutomaticClassification(true));
            return null;
        }).when(offersEnrichmentService).enrichOffers(ArgumentMatchers.anyList(), Mockito.anyBoolean(),
            Mockito.anyMap());

        migrationService.checkAndUpdateCache();
    }

    public static DatacampMessageOuterClass.DatacampMessage offer() {
        return offer(offerToStore(), Function.identity());
    }

    public static DataCampOffer.Offer.Builder offerToStore() {
        return offerToStore(NEW_CONTENT_SUPPLIER, 21, false);
    }

    public static DataCampOffer.Offer.Builder offerToStore(int businessId) {
        return offerToStore(businessId, 21, false);
    }

    public static DataCampOffer.Offer.Builder offerFistPictureNotDownloadedToStore() {
        return offerToStore(NEW_CONTENT_SUPPLIER, 21, true);
    }

    public static DataCampOffer.Offer.Builder offerToStore(int businessId, int categoryId,
                                                           boolean firstPictureNotDownloaded) {
        var pictures = OfferBuilder.pictures(List.of(
            Pair.of("pic1", firstPictureNotDownloaded ? DataCampOfferPictures.MarketPicture.Status.FAILED :
                DataCampOfferPictures.MarketPicture.Status.AVAILABLE),
            Pair.of("pic2", DataCampOfferPictures.MarketPicture.Status.AVAILABLE),
            Pair.of("pic3", DataCampOfferPictures.MarketPicture.Status.AVAILABLE)
        ));
        return DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId("test-offer")
                .setBusinessId(businessId))
            .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                .setVersion(DataCampOfferStatus.VersionStatus.newBuilder()
                    .setActualContentVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                        .setCounter(100500L)
                        .build())
                    .build())
                .build())
            .setPictures(pictures)
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(1)
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                            .setTimestamp(DataCampOfferUtil.toTimestamp(DateTimeUtils.instantNow()))
                            .build())
                        .setMarketSkuId(MODEL_ID))
                    .setUcMapping(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(categoryId)
                        .setMarketCategoryName("category-name")
                        .setMarketModelId(22)
                        .setMarketModelName("model-name")
                        .setMarketSkuId(23)
                        .setMarketSkuName("sku-name")
                        .build()))
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                    .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                        .setGroupId(DataCampOfferMeta.Ui32Value.newBuilder().setValue(1).build())
                        .build())
                    .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                        .setTitle(OfferGenerationHelper.stringValue("Title"))
                        .setVendor(OfferGenerationHelper.stringValue("Vendor"))
                        .setVendorCode(OfferGenerationHelper.stringValue("VendorCode"))
                        .setBarcode(DataCampOfferMeta.StringListValue.newBuilder()
                            .addValue("Barcode1")
                            .addValue("Barcode2").build())
                        .setDescription(OfferGenerationHelper.stringValue("Description"))
                        .setUrl(OfferGenerationHelper.stringValue("ololol.com"))
                        .setOfferParams(DataCampOfferContent.ProductYmlParams.newBuilder()
                            .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                .setName("PARAM")
                                .setValue("21.1")
                                .build())
                            .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                .setName("OTHER_PARAM")
                                .setValue("hello there"))
                            .build())
                        .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                            .setName("shop-category-name")
                            .build()))
                    .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent.newBuilder()
                        .setParameterValues(DataCampOfferMarketContent.MarketParameterValues.newBuilder()
                            .addParameterValues(0, DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                .setParamId(UNITED_SIZE_ID)
                                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                    .setStrValue("40/182")
                                    .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                    .build())
                                .setValueSource(DataCampContentMarketParameterValue.MarketValueSource.CONTENT_EXCEL)
                                .build())
                            .build())
                        .build())
                )
                .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                    .setMarketSkuPublishedOnBlueMarket(true)
                    .setMarketSkuPublishedOnMarket(true)
                    .setVendorId(25)
                    .setVendorName("UC-Vendor-name")
                    .setIrData(DataCampOfferContent.EnrichedOfferSubset.newBuilder()
                        .setEnrichType(Market.UltraControllerServiceData.UltraController
                            .EnrichedOffer.EnrichType.ET_APPROVED_MODEL)
                        .setSkutchType(Market.UltraControllerServiceData.UltraController
                            .EnrichedOffer.SkutchType.SKUTCH_BY_MODEL_ID)
                        .setClassifierCategoryId(34)
                        .setClassifierConfidentTopPercision(0.1)
                        .setMatchedId(22)
                        .build())
                    .build()));
    }

    public static DataCampOffer.Offer.Builder offerWithNoIrData(int businessId) {
        var pictures = OfferBuilder.pictures(List.of(
            Pair.of("pic1", DataCampOfferPictures.MarketPicture.Status.AVAILABLE),
            Pair.of("pic2", DataCampOfferPictures.MarketPicture.Status.AVAILABLE),
            Pair.of("pic3", DataCampOfferPictures.MarketPicture.Status.AVAILABLE)
        ));
        return DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId("test-offer")
                .setBusinessId(businessId))
            .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                .setVersion(DataCampOfferStatus.VersionStatus.newBuilder()
                    .setActualContentVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                        .setCounter(100500L)
                        .build())
                    .build())
                .build())
            .setPictures(pictures)
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(1)
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                            .setTimestamp(DataCampOfferUtil.toTimestamp(DateTimeUtils.instantNow()))
                            .build())
                        .setMarketSkuId(MODEL_ID))
                    .setUcMapping(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(21)
                        .setMarketCategoryName("category-name")
                        .setMarketModelId(22)
                        .setMarketModelName("model-name")
                        .setMarketSkuId(23)
                        .setMarketSkuName("sku-name")
                        .build()))
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                    .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                        .setGroupId(DataCampOfferMeta.Ui32Value.newBuilder().setValue(1).build())
                        .build())
                    .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                        .setTitle(OfferGenerationHelper.stringValue("Title"))
                        .setVendor(OfferGenerationHelper.stringValue("Vendor"))
                        .setVendorCode(OfferGenerationHelper.stringValue("VendorCode"))
                        .setBarcode(DataCampOfferMeta.StringListValue.newBuilder()
                            .addValue("Barcode1")
                            .addValue("Barcode2").build())
                        .setDescription(OfferGenerationHelper.stringValue("Description"))
                        .setUrl(OfferGenerationHelper.stringValue("ololol.com"))
                        .setOfferParams(DataCampOfferContent.ProductYmlParams.newBuilder()
                            .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                .setName("PARAM")
                                .setValue("21.1")
                                .build())
                            .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                .setName("OTHER_PARAM")
                                .setValue("hello there"))
                            .build())
                        .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                            .setName("shop-category-name")
                            .build()))
                    .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent.newBuilder()
                        .setParameterValues(DataCampOfferMarketContent.MarketParameterValues.newBuilder()
                            .addParameterValues(0, DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                .setParamId(UNITED_SIZE_ID)
                                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                    .setStrValue("40/182")
                                    .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                    .build())
                                .setValueSource(DataCampContentMarketParameterValue.MarketValueSource.CONTENT_EXCEL)
                                .build())
                            .build())
                        .build())
                )
                .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                    .setMarketSkuPublishedOnBlueMarket(true)
                    .setMarketSkuPublishedOnMarket(true)
                    .setVendorId(25)
                    .setVendorName("UC-Vendor-name")
                    .build()));
    }

    public static DataCampOffer.Offer.Builder offerToProcess() {
        return offerToProcess(NEW_CONTENT_SUPPLIER);
    }

    public static DataCampOffer.Offer.Builder offerToProcess(int businessId) {
        return DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId("test-offer-2")
                .setBusinessId(businessId))
            .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                .setVersion(DataCampOfferStatus.VersionStatus.newBuilder()
                    .setActualContentVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                        .setCounter(100500L)
                        .build())
                    .build())
                .build())
            .setPictures(OfferBuilder.pictures("pic1", "pic2", "pic3"))
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                    .setIrData(DataCampOfferContent.EnrichedOfferSubset.newBuilder().build())
                    .build())
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                    .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                        .setTitle(OfferGenerationHelper.stringValue("Title"))
                        .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                            .setName("shop-category-name")
                            .build())
                        .build())
                    .build())
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setUcMapping(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(1)
                        .build())
                    .build())
                .build());
    }

    public static DatacampMessageOuterClass.DatacampMessage offer(
        DataCampOffer.Offer.Builder offerBuilder,
        Function<DataCampOffer.Offer.Builder, DataCampOffer.Offer.Builder> modifier
    ) {
        return offer(offerBuilder, modifier, offer -> Map.of(WHITE_SHOP_ID, offer));
    }

    public static DataCampUnitedOffer.UnitedOffer offerWithService(
        DataCampOffer.Offer.Builder offerBuilder,
        Function<DataCampOffer.Offer, Map<Integer, DataCampOffer.Offer>> serviceModifier
    ) {
        return DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(offerBuilder)
            .putAllService(serviceModifier.apply(offerBuilder.build()))
            .build();
    }

    public static DatacampMessageOuterClass.DatacampMessage offer(
        DataCampOffer.Offer.Builder offerBuilder,
        Function<DataCampOffer.Offer.Builder, DataCampOffer.Offer.Builder> modifier,
        Function<DataCampOffer.Offer, Map<Integer, DataCampOffer.Offer>> serviceModifier
    ) {
        var offer = modifier.apply(offerBuilder);
        return DatacampMessageOuterClass.DatacampMessage.newBuilder()
            .addUnitedOffers(DataCampUnitedOffer.UnitedOffersBatch.newBuilder()
                .addOffer(offerWithService(offer, serviceModifier))
                .build())
            .build();
    }

    @Test
    public void shouldInsertSkuMappingCorrectly() {
        handler.process(Collections.singletonList(offer()));

        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertOfferInDB(offer);

        Assertions.assertThat(offer.getSupplierSkuMapping().getMappingId()).isEqualTo(MODEL_ID);
    }

    @Test
    public void testRemoveBaseOffer() {
        handler.process(Collections.singletonList(offer()));

        var removeOfferMessage = offerToStore();
        var status = removeOfferMessage.getStatusBuilder()
            .setRemoved(DataCampOfferMeta.Flag.newBuilder().setFlag(true).build());
        removeOfferMessage.setStatus(status);
        handler.process(List.of(offer(removeOfferMessage, Function.identity())));

        // not removed yet
        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertOfferInDB(offer);

        // but marked to remove
        var removedOffers = removedOfferRepository.findAll();
        Assertions.assertThat(removedOffers).hasSize(1);
        RemovedOffer removedOffer = removedOffers.get(0);
        Assert.assertEquals(offer.getId(), removedOffer.getId().longValue());
        Assert.assertFalse(removedOffer.getIsRemoved());

        handler.process(List.of(offer()));

        offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        offer = offers.get(0);
        assertOfferInDB(offer);
        // removing is cancelled
        removedOffers = removedOfferRepository.findAll();
        Assertions.assertThat(removedOffers).hasSize(0);
    }

    @Test
    public void testRemoveServiceOffer() {
        handler.process(Collections.singletonList(offer()));

        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        var serviceOffers = offer.getServiceOffers();
        Assertions.assertThat(serviceOffers).isNotEmpty();

        var removeOfferMessage = offerToStore();
        var status = removeOfferMessage.getStatusBuilder()
            .setRemoved(DataCampOfferMeta.Flag.newBuilder().setFlag(true).build());
        removeOfferMessage.setStatus(status);
        var message = offer(offerToStore(),
            Function.identity(),
            basicOffer -> Map.of(WHITE_SHOP_ID, removeOfferMessage.build())
        );
        handler.process(List.of(message));

        // not removed yet
        offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        offer = offers.get(0);
        serviceOffers = offer.getServiceOffers();
        Assertions.assertThat(serviceOffers).isEmpty();
        Assertions.assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.INVALID);
    }

    @Test
    public void testAddServiceOfferAfterRemoveAll() {
        handler.process(Collections.singletonList(offer()));

        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0).updateProcessingStatusIfValid(Offer.ProcessingStatus.INVALID)
            .setServiceOffers(List.of());
        offerRepository.updateOffer(offer);

        offers = offerRepository.findAll();
        offer = offers.get(0);
        var serviceOffers = offer.getServiceOffers();
        Assertions.assertThat(serviceOffers).isEmpty();

        var message = offer(offerToStore(),
            Function.identity()
        );
        handler.process(List.of(message));

        offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        offer = offers.get(0);
        serviceOffers = offer.getServiceOffers();
        Assertions.assertThat(serviceOffers).isNotEmpty();
        Assertions.assertThat(offer.getProcessingStatus()).isNotEqualTo(Offer.ProcessingStatus.INVALID);

    }

    @Test
    public void testRemoveOneOfServiceOffers() {
        var message = offer(offerToStore(),
            Function.identity(),
            basicOffer -> Map.of(WHITE_SHOP_ID, offerToStore().build(),
                BLUE_SHOP_ID, offerToStore().build())
        );
        handler.process(List.of(message));

        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        var serviceOffers = offer.getServiceOffers();
        Assertions.assertThat(serviceOffers).extracting(Offer.ServiceOffer::getSupplierId)
            .containsExactlyInAnyOrder(WHITE_SHOP_ID, BLUE_SHOP_ID);

        var removeOfferMessage = offerToStore();
        var status = removeOfferMessage.getStatusBuilder()
            .setRemoved(DataCampOfferMeta.Flag.newBuilder().setFlag(true).build());
        removeOfferMessage.setStatus(status);
        var removeMessage = offer(offerToStore(),
            Function.identity(),
            basicOffer -> Map.of(WHITE_SHOP_ID, removeOfferMessage.build())
        );
        handler.process(List.of(removeMessage));

        offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        offer = offers.get(0);
        serviceOffers = offer.getServiceOffers();
        Assertions.assertThat(serviceOffers).extracting(Offer.ServiceOffer::getSupplierId)
            .containsExactlyInAnyOrder(BLUE_SHOP_ID);
    }

    @Test
    public void shouldInsertCategoryMappingCorrectly() {
        DatacampMessageOuterClass.DatacampMessage message = offer(offerToStore(), offer -> {
            offer.getContentBuilder().getBindingBuilder().getPartnerBuilder()
                .setMarketCategoryId((int) CATEGORY_ID)
                .setMarketCategoryName("category")
                .clearMarketModelId()
                .clearMarketModelName()
                .clearMarketSkuId()
                .clearMarketSkuName();
            return offer;
        });
        handler.process(Collections.singletonList(message));

        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertOfferInDB(offer);

        Assertions.assertThat(offer.getMappedCategoryId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    public void shouldCorrectSplitOffers() {
        DataCampOffer.Offer offerToProcess = offerToProcess().build();
        DatacampMessageOuterClass.DatacampMessage message = offer(offerToStore(), offer -> {
            offer.getContentBuilder().getBindingBuilder().getPartnerBuilder()
                .setMarketCategoryId((int) CATEGORY_ID)
                .setMarketCategoryName("category")
                .clearMarketModelId()
                .clearMarketModelName()
                .clearMarketSkuId()
                .clearMarketSkuName();
            return offer;
        });
        handler.process(Arrays.asList(message, OfferGenerationHelper.toMessage(WHITE_SHOP_ID, offerToProcess)));
        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertOfferInDB(offer);
        var sendEvents = logbrokerEventPublisherMock.getSendEvents();
        Assertions.assertThat(sendEvents).hasSize(1);
        Assertions.assertThat(offer.getMappedCategoryId()).isEqualTo(CATEGORY_ID);
    }

    private void assertOfferInDB(Offer offer) {
        Assertions.assertThat(offer.getBusinessId()).isEqualTo(NEW_CONTENT_SUPPLIER);
        Assertions.assertThat(offer.getServiceOffers()).hasSize(1).containsExactly(
            new Offer.ServiceOffer(WHITE_SHOP_ID,
                MbocSupplierType.MARKET_SHOP, Offer.AcceptanceStatus.OK)
        );
        Assertions.assertThat(offer.getShopSku()).isEqualTo("test-offer");
        Assertions.assertThat(offer.getTitle()).isEqualTo("Title");
        Assertions.assertThat(offer.getVendor()).isEqualTo("Vendor");
        Assertions.assertThat(offer.getVendorCode()).isEqualTo("VendorCode");
        Assertions.assertThat(offer.extractOfferContent().getDescription()).isEqualTo("Description");
        Assertions.assertThat(offer.getShopCategoryName()).isEqualTo("shop-category-name");
        Assertions.assertThat(offer.getVendor()).isEqualTo("Vendor");
        Assertions.assertThat(offer.getVendorCode()).isEqualTo("VendorCode");
        Assertions.assertThat(offer.getBarCode()).isEqualTo("Barcode1,Barcode2");
        Assertions.assertThat(offer.isDataCampOffer()).isTrue();
        Assertions.assertThat(offer.extractOfferContent().getExtraShopFields()).isEqualTo(Map.of("PARAM", "21.1",
            "OTHER_PARAM", "hello there"));
        Assertions.assertThat(offer.extractOfferContent().getPicUrls()).isEqualTo("pic1\npic2\npic3");
        Assertions.assertThat(offer.extractOfferContent().getUrls()).isEqualTo(Collections.singletonList("ololol.com"));
        Assertions.assertThat(offer.extractOfferContent().getUnitedSize()).isEqualTo("40/182");
        Assertions.assertThat(offer.getSupplierCategoryId()).isEqualTo(1L);
        Assertions.assertThat(offer.getSupplierCategoryMappingStatus()).isEqualTo(Offer.MappingStatus.ACCEPTED);
        Assertions.assertThat(offer.getSupplierMappingTimestamp()).isNotNull();
    }

    @Test
    public void shouldUpdateCorrectly() {
        handler.process(Collections.singletonList(offer()));

        DataCampOffer.Offer offer = offer().getUnitedOffers(0).getOffer(0).getBasic()
            .toBuilder()
            .setPictures(OfferBuilder.pictures("pic4", "pic5", "pic6"))
            .build();

        handler.process(Collections.singletonList(OfferGenerationHelper.toMessage(WHITE_SHOP_ID, offer)));

        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer res = offers.get(0);
        assertEquals("pic4\npic5\npic6", res.extractOfferContent().getPicUrls());
    }

    @Test
    public void shouldUpdate1PCorrectly() {
        Offer offer = offerRepository.insertAndGetOffer(
            OfferTestUtils.simpleOffer()
                .setBusinessId(REAL_SUPPLIER_ID)
                .storeOfferContent(OfferContent.builder().picUrls("pic4\npic5\npic6").build()));

        String externalSSKU = RealConverter.generateSSKU(REAL_SUPPLIER_EXT_ID, offer.getShopSku());

        supplierConverterService.addInternalToExternalMapping(
            new ShopSkuKey(REAL_SUPPLIER_ID, offer.getShopSku()),
            new ShopSkuKey(SupplierConverterServiceMock.BERU_ID, externalSSKU)
        );

        var offerMessage = offer(offerToStore(),
            offerBuilder -> {
                offerBuilder.getIdentifiersBuilder()
                    .setBusinessId(BERU_BUSINESS_ID)
                    .setOfferId(externalSSKU);
                return offerBuilder;
            },
            basicOffer -> Map.of(SupplierConverterServiceMock.BERU_ID, basicOffer)
        );

        handler.process(Collections.singletonList(offerMessage));

        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer res = offers.get(0);
        assertEquals("pic1\npic2\npic3", res.extractOfferContent().getPicUrls());
    }

    @Test
    public void testWhiteMappingMergedToDSBSBusinessOffer() {
        int supplierId = 1000001;
        Supplier supplierMs = new Supplier(supplierId, "biz child")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setDatacamp(true)
            .setBusinessId(NEW_CONTENT_SUPPLIER)
            .setMbiBusinessId(NEW_CONTENT_SUPPLIER);
        int dsbsSupplierId = 1000002;
        Supplier dsbsSupplier = new Supplier(dsbsSupplierId, "biz child dsbs")
            .setType(MbocSupplierType.DSBS)
            .setDatacamp(true)
            .setBusinessId(NEW_CONTENT_SUPPLIER)
            .setMbiBusinessId(NEW_CONTENT_SUPPLIER);
        supplierRepository.insertBatch(supplierMs, dsbsSupplier);

        Offer existingSupplierOffer = commonWhiteOffer()
            .setShopSku("test-offer")
            .setBusinessId(supplierId)
            .setServiceOffers(List.of())
            .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, CONTENT)
            .setMappedModelId(OfferTestUtils.TEST_MODEL_ID, CONTENT)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(OfferTestUtils.TEST_SKU_ID), CONTENT)
            .addNewServiceOfferIfNotExistsForTests(supplierMs);
        offerRepository.insertOffers(existingSupplierOffer);

        DatacampMessageOuterClass.DatacampMessage message = offer(offerToStore(), Function.identity(),
            offer -> Map.of(supplierId, offer, dsbsSupplierId, offer));
        handler.process(Collections.singletonList(message));

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        // mapping is set to business offer
        assertEquals(NEW_CONTENT_SUPPLIER, offer.getBusinessId());
        assertEquals(OfferTestUtils.TEST_CATEGORY_INFO_ID, offer.getMappedCategoryId().longValue());
        assertEquals(OfferTestUtils.TEST_MODEL_ID, offer.getMappedModelId().longValue());
        assertEquals(OfferTestUtils.TEST_SKU_ID, offer.getApprovedSkuMapping().getMappingId());
        Assert.assertTrue(offer.hasServiceOffer(so -> so.getSupplierType() == MbocSupplierType.DSBS));
        Assert.assertTrue(offer.hasServiceOffer(so -> so.getSupplierType() == MbocSupplierType.MARKET_SHOP));
        assertEquals(Offer.ProcessingStatus.IN_RE_SORT, offer.getProcessingStatus());
        assertEquals(Offer.AcceptanceStatus.OK, offer.getAcceptanceStatus());

        List<MigrationRemovedOffer> removedOffers = migrationRemovedOfferRepository.findAll();
        assertThat(removedOffers).hasSize(1);
        var removedOffer = removedOffers.get(0);
        Assert.assertEquals(Integer.valueOf(NEW_CONTENT_SUPPLIER), removedOffer.getAutoRemovedForBusiness());
        assertThat(removedOffer.getRemovedOffer()).isNotNull();
        assertThat(removedOffer.getModifiedAt()).isNotNull();
    }

    @Test
    public void testWhiteMappingMergedToExistingDsbs() {
        doAnswer(i -> null).when(offersEnrichmentService)
            .enrichOffers(ArgumentMatchers.anyList(), Mockito.anyBoolean(), Mockito.anyMap());

        int supplierId = 1000001;
        Supplier supplierMs = new Supplier(supplierId, "biz child")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setDatacamp(true)
            .setBusinessId(NEW_CONTENT_SUPPLIER)
            .setMbiBusinessId(NEW_CONTENT_SUPPLIER);
        int dsbsSupplierId = 1000002;
        Supplier dsbsSupplier = new Supplier(dsbsSupplierId, "biz child dsbs")
            .setType(MbocSupplierType.DSBS)
            .setDatacamp(true)
            .setBusinessId(NEW_CONTENT_SUPPLIER)
            .setMbiBusinessId(NEW_CONTENT_SUPPLIER);
        supplierRepository.insertBatch(supplierMs, dsbsSupplier);

        Offer existingSupplierOffer = commonWhiteOffer()
            .setShopSku("test-offer")
            .setBusinessId(supplierId)
            .setServiceOffers(List.of())
            .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, CONTENT)
            .setCategoryIdForTests((long) CATEGORY_WITH_KNOWLEDGE, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(0), CONTENT)
            .addNewServiceOfferIfNotExistsForTests(supplierMs);

        Offer existingDsbsOffer = commonWhiteOffer()
            .setShopSku("test-offer")
            .setBusinessId(NEW_CONTENT_SUPPLIER)
            .setServiceOffers(List.of())
            .setCategoryIdForTests(null, Offer.BindingKind.SUPPLIER)
            .addNewServiceOfferIfNotExistsForTests(dsbsSupplier);
        offerRepository.insertOffers(existingSupplierOffer, existingDsbsOffer);

        DatacampMessageOuterClass.DatacampMessage message = offer(offerToStore(),
            offer -> {
                DataCampOfferContent.OfferContent.Builder content = offer.getContent().toBuilder();

                DataCampOfferMapping.ContentBinding.Builder binding = content.getBinding().toBuilder();
                DataCampOfferMapping.Mapping.Builder ucMapping = binding.getUcMapping().toBuilder();
                ucMapping.clear().setMarketCategoryId(0);
                binding.setUcMapping(ucMapping);

                binding.clearPartner();
                content.setBinding(binding);

                offer.setContent(content);
                return offer;
            },
            offer -> Map.of(supplierId, offer, dsbsSupplierId, offer));
        handler.process(Collections.singletonList(message));

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);

        assertEquals(NEW_CONTENT_SUPPLIER, offer.getBusinessId());
        assertEquals(OfferTestUtils.TEST_CATEGORY_INFO_ID, offer.getMappedCategoryId().longValue());
        assertEquals(CATEGORY_WITH_KNOWLEDGE, offer.getCategoryId().longValue());
        Assert.assertTrue(offer.hasServiceOffer(so -> so.getSupplierType() == MbocSupplierType.DSBS));
        Assert.assertTrue(offer.hasServiceOffer(so -> so.getSupplierType() == MbocSupplierType.MARKET_SHOP));

        List<MigrationRemovedOffer> removedOffers = migrationRemovedOfferRepository.findAll();
        assertThat(removedOffers).hasSize(1);
        var removedOffer = removedOffers.get(0);
        Assert.assertEquals(Integer.valueOf(NEW_CONTENT_SUPPLIER), removedOffer.getAutoRemovedForBusiness());
        assertThat(removedOffer.getRemovedOffer()).isNotNull();
        assertThat(removedOffer.getModifiedAt()).isNotNull();
    }

    @Test
    public void testWhiteMappingMergedToDSBSBusinessOfferDoNotRewrite() {
        int supplierId = 1000001;
        Supplier supplierMs = new Supplier(supplierId, "biz child")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setDatacamp(true)
            .setBusinessId(NEW_CONTENT_SUPPLIER)
            .setMbiBusinessId(NEW_CONTENT_SUPPLIER);
        int dsbsSupplierId = 1000002;
        Supplier dsbsSupplier = new Supplier(dsbsSupplierId, "biz child dsbs")
            .setType(MbocSupplierType.DSBS)
            .setDatacamp(true)
            .setBusinessId(NEW_CONTENT_SUPPLIER)
            .setMbiBusinessId(NEW_CONTENT_SUPPLIER);
        supplierRepository.insertBatch(supplierMs, dsbsSupplier);

        Offer existingSupplierOffer = commonWhiteOffer()
            .setShopSku("test-offer")
            .setBusinessId(supplierId)
            .setServiceOffers(List.of())
            .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, CONTENT)
            .setMappedModelId(OfferTestUtils.TEST_MODEL_ID, CONTENT)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(OfferTestUtils.TEST_SKU_ID), CONTENT)
            .addNewServiceOfferIfNotExistsForTests(supplierMs);

        long notChangedSkuMapping = 321L;
        Offer dsbsExistingSupplierOffer = commonWhiteOffer()
            .setShopSku("test-offer")
            .setBusinessId(NEW_CONTENT_SUPPLIER)
            .setServiceOffers(List.of())
            .updateApprovedSkuMapping(OfferTestUtils.mapping(notChangedSkuMapping), CONTENT)
            .addNewServiceOfferIfNotExistsForTests(dsbsSupplier);
        offerRepository.insertOffers(existingSupplierOffer, dsbsExistingSupplierOffer);

        DatacampMessageOuterClass.DatacampMessage message = offer(offerToStore(), Function.identity(),
            offer -> Map.of(supplierId, offer, dsbsSupplierId, offer));
        handler.process(Collections.singletonList(message));

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        // mapping is set to business offer
        assertEquals(NEW_CONTENT_SUPPLIER, offer.getBusinessId());
        assertEquals(notChangedSkuMapping, offer.getApprovedSkuMapping().getMappingId());
        Assert.assertTrue(offer.hasServiceOffer(so -> so.getSupplierType() == MbocSupplierType.DSBS));
        Assert.assertTrue(offer.hasServiceOffer(so -> so.getSupplierType() == MbocSupplierType.MARKET_SHOP));

        List<MigrationRemovedOffer> removedOffers = migrationRemovedOfferRepository.findAll();
        assertThat(removedOffers).hasSize(1);
        var removedOffer = removedOffers.get(0);
        Assert.assertEquals(Integer.valueOf(NEW_CONTENT_SUPPLIER), removedOffer.getAutoRemovedForBusiness());
        assertThat(removedOffer.getRemovedOffer()).isNotNull();
        assertThat(removedOffer.getModifiedAt()).isNotNull();
    }

    @Test
    public void saveOfferWithoutTitleValidationError() {
        var dcOffer = offerToStore()
            .setContent(offerToStore().getContentBuilder()
                // set empty title
                .setPartner(offerToProcess().getContent().getPartner().toBuilder().setActual(
                    offerToProcess().getContent().getPartner().getActual().toBuilder().clearTitle()))
            );

        handler.process(Collections.singletonList(offer(dcOffer, Function.identity())));

        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        Assertions.assertThat(offer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.INVALID);
        Assertions.assertThat(offer.getOfferErrors())
            .isNotNull()
            .extracting(ErrorInfo::getErrorCode)
            .containsExactlyInAnyOrder("mboc.error.excel-value-is-required");

        List<DataCampUnitedOffersEvent> events = logbrokerEventPublisherMock.getSendEvents();
        // offer has been saved, no events are sent immediately
        Assertions.assertThat(events).hasSize(0);
    }

    @Test
    public void saveOfferAfterValidationErrorFixed() {
        var dcOffer = offerToStore();

        BusinessSkuKey businessSkuKey = DataCampOfferUtil.extractExternalBusinessSkuKey(dcOffer);

        Offer offer = offerRepository.insertAndGetOffer(
            OfferTestUtils.simpleOffer()
                .setBusinessId(businessSkuKey.getBusinessId())
                .setShopSku(businessSkuKey.getShopSku())
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.INVALID)
                .setOfferErrors(List.of(MbocErrors.get().internalErrorSavingOffer(DEFAULT_SHOP_SKU))));
        categoryKnowledgeService.addCategory(1L);
        handler.process(Collections.singletonList(offer(dcOffer, Function.identity())));

        List<Offer> offers = offerRepository.findOffersByBusinessSkuKeysWithOfferContent(offer.getBusinessSkuKey());
        Assertions.assertThat(offers)
            .hasSize(1)
            .allMatch(o -> o.getProcessingStatus() == Offer.ProcessingStatus.IN_CLASSIFICATION)
            .allMatch(o -> CollectionUtils.isEmpty(o.getOfferErrors()));
    }

    @Test
    public void contentActiveErrorIsNotRemovedWhenHashIsNotChanged() {
        var dcOffer = offerToStore();

        BusinessSkuKey businessSkuKey = DataCampOfferUtil.extractExternalBusinessSkuKey(dcOffer);

        Long sameHash = hashCalculator.marketSpecificContentHash(
                dcOffer.build(),
                Offer.builder().build(),
                offerDestinationCalculator)
            .get();

        Offer offer = offerRepository.insertAndGetOffer(
            OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
                .setBusinessId(businessSkuKey.getBusinessId())
                .setShopSku(businessSkuKey.getShopSku())
                .setMarketSpecificContentHash(sameHash)
                .setMarketSpecificContentHashSent(sameHash)
                .setContentStatusActiveError(MbocErrors.get().barcodeRequired(DEFAULT_SHOP_SKU)));

        handler.process(Collections.singletonList(
            offer(dcOffer, Function.identity(), so -> Map.of(OfferTestUtils.blueSupplierUnderBiz1().getId(), so))));

        List<Offer> offers = offerRepository.findOffersByBusinessSkuKeysWithOfferContent(offer.getBusinessSkuKey());
        Assertions.assertThat(offers)
            .extracting(Offer::getContentStatusActiveError)
            .allMatch(not(Objects::isNull));
    }

    @Test
    public void removeContentActiveErrorOnHashChange() {
        var dcOffer = offerToStore();

        BusinessSkuKey businessSkuKey = DataCampOfferUtil.extractExternalBusinessSkuKey(dcOffer);

        Long anotherHash = hashCalculator.marketSpecificContentHash(dcOffer.build(),
                Offer.builder().build(),
                offerDestinationCalculator)
            .map(hash -> hash + 1)
            .get();

        Offer offer = offerRepository.insertAndGetOffer(
            OfferTestUtils.simpleOffer()
                .setBusinessId(businessSkuKey.getBusinessId())
                .setShopSku(businessSkuKey.getShopSku())
                .setMarketSpecificContentHash(anotherHash)
                .setMarketSpecificContentHashSent(anotherHash)
                .setContentStatusActiveError(MbocErrors.get().barcodeRequired(DEFAULT_SHOP_SKU)));

        handler.process(Collections.singletonList(offer(dcOffer, Function.identity())));

        List<Offer> offers = offerRepository.findOffersByBusinessSkuKeysWithOfferContent(offer.getBusinessSkuKey());
        Assertions.assertThat(offers)
            .extracting(Offer::getContentStatusActiveError)
            .allMatch(Objects::isNull);
    }

    @Test
    public void saveOfferWithoutTitleNoAutoapprove() {
        DataCampOfferContent.EnrichedOfferSubset irData =
            offerToStore().getContent().getMarket().getIrData().toBuilder()
                .setSkutchType(Market.UltraControllerServiceData.UltraController.EnrichedOffer.SkutchType.BARCODE_SKUTCH)
                .build();
        DataCampOfferContent.MarketContent.Builder market =
            offerToStore().getContent().getMarket().toBuilder().setIrData(irData);

        var dcOffer = offerToStore()
            .setContent(offerToStore().getContentBuilder()
                // set empty title
                .setPartner(offerToProcess().getContent().getPartner().toBuilder().setActual(
                    offerToProcess().getContent().getPartner().getActual().toBuilder().clearTitle()))
                .setMarket(market)
            );

        handler.process(Collections.singletonList(offer(dcOffer, Function.identity())));

        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        Assertions.assertThat(offer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.INVALID);

        Assertions.assertThat(offer.getApprovedSkuMapping()).isNull();

        List<DataCampUnitedOffersEvent> events = logbrokerEventPublisherMock.getSendEvents();
        // offer has been saved, no events are sent immediately
        Assertions.assertThat(events).hasSize(0);
    }

    @Test
    public void saveAutoApproveOfferWithCardSource() {
        supplierRepository.update(supplierRepository.findById(BLUE_SHOP_ID).setType(MbocSupplierType.THIRD_PARTY));

        var dcOffer = offerToStore()
            .setContent(offerToStore().getContentBuilder()
                .setPartner(offerToProcess().getContent().getPartner().toBuilder().setOriginal(
                    offerToProcess().getContent().getPartner().getOriginal().toBuilder()
                        .setCardSource(
                            DataCampOfferContent.CardSource.newBuilder()
                                .setCardByMskuSearch(true).setMarketSkuId(MODEL_ID).build()
                        )))
            );

        var message = offer(dcOffer,
            Function.identity(),
            basicOffer -> Map.of(BLUE_SHOP_ID, dcOffer.build())
        );

        handler.process(Arrays.asList(message));

        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        Assertions.assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
        Assertions.assertThat(offer.isAutoApprovedMapping()).isTrue();
        Assertions.assertThat(offer.getSupplierSkuMappingCheckLogin()).isEqualTo("auto-accepted");

        Assertions.assertThat(offer.getApprovedSkuMapping().getMappingId()).isEqualTo(MODEL_ID);

        List<DataCampUnitedOffersEvent> events = logbrokerEventPublisherMock.getSendEvents();
        // offer has been saved, no events are sent immediately
        Assertions.assertThat(events).hasSize(0);
    }

    @Test
    public void saveFailAutoApproveOfferWithotCardSource() {
        supplierRepository.update(supplierRepository.findById(BLUE_SHOP_ID).setType(MbocSupplierType.THIRD_PARTY));

        var message = offer(offerToStore(),
            Function.identity(),
            basicOffer -> Map.of(BLUE_SHOP_ID, offerToStore().build())
        );

        handler.process(Arrays.asList(message));

        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        Assertions.assertThat(offer.getProcessingStatus()).isNotEqualTo(Offer.ProcessingStatus.PROCESSED);
        Assertions.assertThat(offer.isAutoApprovedMapping()).isFalse();

        Assertions.assertThat(offer.getApprovedSkuMapping()).isNull();

        List<DataCampUnitedOffersEvent> events = logbrokerEventPublisherMock.getSendEvents();
        // offer has been saved, no events are sent immediately
        Assertions.assertThat(events).hasSize(0);
    }

    @Test
    public void shouldSendToRecheckOnRequest() {
        storageKeyValueService.putValue("RecheckMapping.enabled", true);
        supplierRepository.update(supplierRepository.findById(BLUE_SHOP_ID).setType(MbocSupplierType.THIRD_PARTY));

        var dcOffer = offerToStore()
            .setContent(offerToStore().getContentBuilder()
                .setPartner(offerToProcess().getContent().getPartner().toBuilder().setOriginal(
                    offerToProcess().getContent().getPartner().getOriginal().toBuilder()
                        .setCardSource(
                            DataCampOfferContent.CardSource.newBuilder()
                                .setCardByMskuSearch(true).setMarketSkuId(MODEL_ID).build()
                        )))
            );

        var message = offer(dcOffer,
            Function.identity(),
            basicOffer -> Map.of(BLUE_SHOP_ID, dcOffer.build())
        );

        handler.process(Arrays.asList(message));

        dcOffer.getContentBuilder().getBindingBuilder()
            .setPartnerMappingModeration(
                DataCampOfferMapping.PartnerMappingModeration.newBuilder()
                    .setPartnerDecision(DataCampOfferMapping.PartnerDecision.newBuilder()
                        .setMarketSkuId(MODEL_ID)
                        .setValue(DataCampOfferMapping.PartnerDecision.Decision.DENY)
                        .build())
                    .build()
            );

        handler.process(Collections.singletonList(
            offer(dcOffer,
                Function.identity(),
                basicOffer -> Map.of(BLUE_SHOP_ID, dcOffer.build())
            )
        ));

        List<Offer> offers = offerRepository.findAll();
        assertEquals(1, offers.size());

        Offer resultOffer = offers.get(0);

        assertEquals(Offer.ProcessingStatus.IN_RECHECK_MODERATION, resultOffer.getProcessingStatus());
        assertEquals(Offer.RecheckMappingStatus.ON_RECHECK, resultOffer.getRecheckMappingStatus());
        assertEquals(Offer.RecheckMappingSource.PARTNER, resultOffer.getRecheckMappingSource());
    }

    @Test
    public void shouldNotSendToRecheckOnMappingChanged() {
        var newModelId = MODEL_ID;
        var oldModelId = MODEL_ID_2;

        supplierRepository.update(supplierRepository.findById(BLUE_SHOP_ID).setType(MbocSupplierType.THIRD_PARTY));

        var dcOffer = offerToStore()
            .setContent(offerToStore().getContentBuilder()
                .setPartner(offerToProcess().getContent().getPartner().toBuilder().setOriginal(
                    offerToProcess().getContent().getPartner().getOriginal().toBuilder()
                        .setCardSource(
                            DataCampOfferContent.CardSource.newBuilder()
                                .setCardByMskuSearch(true).setMarketSkuId(newModelId).build()
                        )))
            );

        var message = offer(dcOffer,
            Function.identity(),
            basicOffer -> Map.of(BLUE_SHOP_ID, dcOffer.build())
        );

        handler.process(Arrays.asList(message));

        dcOffer.getContentBuilder().getBindingBuilder()
            .setPartnerMappingModeration(
                DataCampOfferMapping.PartnerMappingModeration.newBuilder()
                    .setPartnerDecision(DataCampOfferMapping.PartnerDecision.newBuilder()
                        .setMarketSkuId(oldModelId)
                        .setValue(DataCampOfferMapping.PartnerDecision.Decision.DENY)
                        .build())
                    .build()
            );

        handler.process(Collections.singletonList(
            offer(dcOffer,
                Function.identity(),
                basicOffer -> Map.of(BLUE_SHOP_ID, dcOffer.build())
            )
        ));

        List<Offer> offers = offerRepository.findAll();
        assertEquals(1, offers.size());

        Offer resultOffer = offers.get(0);

        assertNotEquals(Offer.ProcessingStatus.IN_RECHECK_MODERATION, resultOffer.getProcessingStatus());
        assertNull(resultOffer.getRecheckMappingStatus());
        assertNull(resultOffer.getRecheckMappingSource());
    }

    @Test
    public void processResaleOffer() {
        DatacampMessageOuterClass.DatacampMessage message = offer(offerToStore(), offer -> {
            offer.getContentBuilder().getPartnerBuilder().getActualBuilder().getIsResaleBuilder()
                .setFlag(true);
            return offer;
        });
        handler.process(Collections.singletonList(message));
        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertOfferInDB(offer);
        Assertions.assertThat(offer.isResale()).isEqualTo(true);
    }

    @Test
    public void processNotResaleOffer() {
        handler.process(Collections.singletonList(offer()));
        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer offer = offers.get(0);
        assertOfferInDB(offer);
        Assertions.assertThat(offer.isResale()).isEqualTo(false);
    }
}
