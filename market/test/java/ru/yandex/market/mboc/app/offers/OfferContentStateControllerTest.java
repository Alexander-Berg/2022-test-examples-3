package ru.yandex.market.mboc.app.offers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.PartnerCategoryOuterClass;
import NMarketIndexer.Common.Common;
import com.google.protobuf.Timestamp;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.ir.http.MarketParameters;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.datacamp.service.converter.DataCampConverterService;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.golden.GoldenMatrixService;
import ru.yandex.market.mboc.common.honestmark.AutoClassificationResult;
import ru.yandex.market.mboc.common.honestmark.ClassificationResult;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationCounterService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.offers.DefaultOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.OfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.MigrationModelRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCache;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.migration.MigrationModelService;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.enrichment.OffersEnrichmentService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.services.ultracontroller.UltraControllerService;
import ru.yandex.market.mboc.common.services.ultracontroller.UltraControllerServiceImpl;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static Market.DataCamp.DataCampContentStatus.PartnerMappingState.MAPPING_STATUS_ACCEPTED;
import static Market.DataCamp.DataCampContentStatus.PartnerMappingState.MAPPING_STATUS_NONE;
import static Market.DataCamp.DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU;
import static Market.DataCamp.DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mboc.app.offers.OfferContentStateControllerTest.TestDataCampOfferBuilder.dataCampOffer;

/**
 * @author dergachevfv
 * @since 8/17/20
 */
public class OfferContentStateControllerTest {

    private static final AtomicInteger generator = new AtomicInteger(0);
    private static final String SHOP_SKU_1 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_2 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_3 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_4 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_5 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_6 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_7 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_8 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_9 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_10 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_11 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_12 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_13 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_14 = "test-shop-sku-" + generator.incrementAndGet();
    private static final String SHOP_SKU_999 = "test-shop-sku-999";
    private static final String OFFER_NAME = "test offer name";
    private static final String OFFER_DESCRIPTION = "test offer description";

    private static final String VENDOR = "test vendor 1";
    private static final String VENDOR_CODE = "1001";

    private static final int SUPPLIER_ID = 11;
    private static final String SUPPLIER_NAME = "test supplier";
    private static final int SUPPLIER_GOOD_CONTENT_ID = 22;
    private static final String SUPPLIER_GOOD_CONTENT_NAME = "test supplier can send content";

    private static final int CATEGORY_ID = 200;
    private static final String CATEGORY_NAME = "test category";

    private static final int CATEGORY_NO_KNW_ID = 300;
    private static final String CATEGORY_NO_KNW_NAME = "test category with no knowledge";

    private static final int CATEGORY_KNW_GC_ID = 400;
    private static final String CATEGORY_KNW_GC_NAME = "test category with knowledge and GOOD_CONTENT enabled";

    private static final int MODEL_ID = 300001;
    private static final String MODEL_NAME = "model name";
    private static final long SKU_ID = 400000001L;
    private static final String SKU_NAME = "TEST NAME #400000001";

    private static final String BARCODE = "0123456789012";

    private static final DataCampContentStatus.CategoryRestriction INDETERMINABLE_RESTRICTION =
        DataCampContentStatus.CategoryRestriction.newBuilder()
            .setType(DataCampContentStatus.CategoryRestriction.AllowedType.INDETERMINABLE)
            .build();
    private OfferRepositoryMock offerRepository = new OfferRepositoryMock();

    private UltraControllerService ultraControllerService;
    private OffersEnrichmentService offersEnrichmentService;

    private MskuRepository mskuRepository;
    private MigrationModelRepository migrationModelRepository;
    private MigrationModelService migrationModelService;

    private OfferContentStateController offerContentStateController;

    private ModelStorageCachingServiceMock modelStorageCachingService;
    private SupplierService supplierService;
    private ApplySettingsService applySettingsService;

    private final OfferDestinationCalculator offerDestinationCalculator = new DefaultOfferDestinationCalculator();

    @Before
    public void setUp() {
        offerRepository = new OfferRepositoryMock();
        mskuRepository = Mockito.mock(MskuRepository.class);
        migrationModelRepository = Mockito.mock(MigrationModelRepository.class);
        TestUtils.mockMskuRepositoryFindForOffers(mskuRepository);

        migrationModelService = new MigrationModelService(
            migrationModelRepository,
            mskuRepository
        );
        SupplierRepositoryMock supplierRepository = new SupplierRepositoryMock();
        supplierRepository.insertBatch(
            new Supplier(SUPPLIER_ID, SUPPLIER_NAME)
                .setBusinessId(SUPPLIER_ID),
            new Supplier(SUPPLIER_GOOD_CONTENT_ID, SUPPLIER_GOOD_CONTENT_NAME)
                .setBusinessId(SUPPLIER_GOOD_CONTENT_ID)
                .setType(MbocSupplierType.BUSINESS)
                .setNewContentPipeline(true)
        );

        CategoryCachingServiceMock categoryCachingService = new CategoryCachingServiceMock();
        categoryCachingService.addCategories(
            new Category().setCategoryId(CATEGORY_ID)
                .setHasKnowledge(true)
                .setName(CATEGORY_NAME),
            new Category().setCategoryId(CATEGORY_NO_KNW_ID)
                .setHasKnowledge(false)
                .setName(CATEGORY_NO_KNW_NAME),
            new Category().setCategoryId(CATEGORY_KNW_GC_ID)
                .setName(CATEGORY_KNW_GC_NAME)
                .setHasKnowledge(true)
                .setAcceptContentFromWhiteShops(true)
                .setAcceptGoodContent(true)
        );

        var mboUsersRepo = new MboUsersRepositoryMock();
        var categoryInfoRepository = new CategoryInfoRepositoryMock(mboUsersRepo);
        modelStorageCachingService = new ModelStorageCachingServiceMock();
        CategoryInfoCache categoryInfoCache = new CategoryInfoCacheImpl(categoryInfoRepository);

        supplierService = new SupplierService(supplierRepository);
        ultraControllerService = mock(UltraControllerService.class);
        NeedContentStatusService needContentStatusService = new NeedContentStatusService(
            categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet())
        );
        LegacyOfferMappingActionService legacyOfferMappingActionService =
            new LegacyOfferMappingActionService(needContentStatusService,
                Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator,
                new StorageKeyValueServiceMock());
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        CategoryKnowledgeServiceMock categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        HonestMarkClassificationService honestMarkClassificationService = mock(HonestMarkClassificationService.class);
        Mockito.when(honestMarkClassificationService.getClassificationResult(
                any(Offer.class), anyLong(), any(Supplier.class),
                anySet(), anySet()))
            .thenReturn(new AutoClassificationResult(ClassificationResult.UNCONFIDENT_ALLOW_GC, null, true));
        var antiMappingRepositoryMock = new AntiMappingRepositoryMock();
        offersEnrichmentService = new OffersEnrichmentService(
            Mockito.mock(GoldenMatrixService.class),
            ultraControllerService,
            offerMappingActionService, supplierService, categoryKnowledgeService,
            honestMarkClassificationService,
            Mockito.mock(HonestMarkClassificationCounterService.class),
            Mockito.mock(BooksService.class), offerDestinationCalculator, categoryInfoCache);

        DataCampIdentifiersService dataCampIdentifiersService = Mockito.mock(DataCampIdentifiersService.class);
        Mockito.when(dataCampIdentifiersService.createBusinessSkuKey(any()))
            .thenAnswer(invocation -> {
                DataCampOffer.Offer offer = invocation.getArgument(0);
                return DataCampOfferUtil.extractExternalBusinessSkuKey(offer);
            });
        Mockito.when(dataCampIdentifiersService.createIdentifiers(any(), any()))
            .thenAnswer(invocation -> {
                Supplier supplier = invocation.getArgument(0);
                Offer offer = invocation.getArgument(1);
                DataCampOfferIdentifiers.OfferIdentifiers.Builder identifiers =
                    DataCampOfferIdentifiers.OfferIdentifiers.newBuilder();
                identifiers.setOfferId(offer.getShopSku());
                identifiers.setBusinessId(offer.getBusinessId());
                return identifiers.build();
            });
        OfferCategoryRestrictionCalculator calculator = mock(OfferCategoryRestrictionCalculator.class);
        when(calculator.calculateRestriction(any())).thenReturn(Optional.empty());

        DataCampConverterService dataCampConverterService = new DataCampConverterService(
            dataCampIdentifiersService,
            calculator,
            new StorageKeyValueServiceMock(),
            true
        );

        applySettingsService = Mockito.mock(ApplySettingsService.class);

        offerContentStateController = new OfferContentStateController(
            ultraControllerService,
            offerRepository,
            supplierRepository,
            mskuRepository,
            categoryCachingService,
            offersEnrichmentService,
            dataCampConverterService,
            dataCampIdentifiersService,
            offerMappingActionService,
            modelStorageCachingService,
            applySettingsService,
            antiMappingRepositoryMock,
            migrationModelService);
    }

    @Test
    public void testSetAdultAndParamFields() {
        DataCampOffer.Offer offerWithParamsAndAdult = new TestDataCampOfferBuilder(SHOP_SKU_4, SUPPLIER_ID, true)
            .build();

        assertThat(offerWithParamsAndAdult.getContent().getPartner().getOriginal().hasOfferParams()).isEqualTo(true);
        assertThat(offerWithParamsAndAdult.getContent().getPartner().getOriginal().getOfferParams()
            .getParam(0).getName()).isEqualTo("test-param");
        assertThat(offerWithParamsAndAdult.getContent().getPartner().getOriginal().getOfferParams()
            .getParam(0).getValue()).isEqualTo("test-param-value");
        Mockito.when(ultraControllerService.enrich(anyList(), anyBoolean()))
            .thenAnswer(invocation -> {
                List<Offer> offers = invocation.getArgument(0);
                assertThat(offers).hasSize(1);
                for (Offer offer : offers) {
                    offer.markLoadedContent();
                    assertThat(offer.extractOfferContent().containsExtraShopField("test-param")).isEqualTo(true);
                    assertThat(offer.getAdult()).isEqualTo(true);
                    assertThat(offer.extractOfferContent().getExtraShopField("test-param")).isEqualTo("test-param" +
                        "-value");
                    assertThat(offer.extractOfferContent().getExtraShopField("MarketParam")).isEqualTo("18");
                    assertThat(offer.extractOfferContent().getExtraShopField("MarketNumericParam")).isEqualTo("13");
                    assertThat(offer.extractOfferContent().getExtraShopField("MarketBoolParam")).isEqualTo("true");
                    assertThat(offer.getMarketParameterValues()).containsExactlyInAnyOrder(
                        DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                            .setParamId(10L)
                            .setParamName("MarketParam")
                            .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                .setOptionId(12L)
                                .setStrValue("18")
                                .build())
                            .build(),
                        DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                            .setParamId(15L)
                            .setParamName("MarketNumericParam")
                            .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.NUMERIC)
                                .setNumericValue("13")
                                .build())
                            .build(),
                        DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                            .setParamId(16L)
                            .setParamName("MarketBoolParam")
                            .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.BOOLEAN)
                                .setBoolValue(true)
                                .build())
                            .build());
                }
                return offers.stream()
                    .collect(Collectors.toMap(
                        Offer::getBusinessSkuKey,
                        __ -> UltraController.EnrichedOffer.newBuilder().build()
                    ));
            });

        offerContentStateController.calculateContentStatus(DataCampOffer.OffersBatch.newBuilder()
            .addOffer(offerWithParamsAndAdult)
            .build());
    }

    @Test
    public void testSendPriceToUC() {
        var offerWithPrice = new TestDataCampOfferBuilder(SHOP_SKU_4, SUPPLIER_ID, new BigDecimal("10.23"))
            .build();
        Mockito.when(ultraControllerService.enrich(anyList(), anyBoolean()))
            .thenAnswer(invocation -> {
                List<Offer> offers = invocation.getArgument(0);
                assertThat(offers).hasSize(1);
                for (Offer offer : offers) {
                    assertThat(offer.extractOfferContent()
                        .getExtraShopFields().get(ExcelHeaders.PRICE.getTitle())).isEqualTo("10.23");
                }
                boolean usePrice = invocation.getArgument(1);
                assertThat(usePrice).isTrue();
                return offers.stream()
                    .collect(Collectors.toMap(
                        Offer::getBusinessSkuKey,
                        __ -> UltraController.EnrichedOffer.newBuilder().build()
                    ));
            });

        var batch = offerContentStateController.calculateContentStatus(
            DataCampOffer.OffersBatch.newBuilder()
                .addOffer(offerWithPrice)
                .build()
        );
        Assert.assertEquals(1, batch.getOfferCount());
        Assert.assertEquals(102300000L,
            batch.getOffer(0).getPrice().getBasic().getBinaryPrice().getPrice());
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void testShouldSaveSupplierCategory() {

        DataCampOffer.Offer offer = new TestDataCampOfferBuilder(SHOP_SKU_1, SUPPLIER_ID, CATEGORY_ID).build();

        when(ultraControllerService.enrich(anyList(), anyBoolean()))
            .thenReturn(Map.of(
                businessSkuKey(SHOP_SKU_1, SUPPLIER_ID), UltraController.EnrichedOffer.newBuilder().build()
            ));

        DataCampOffer.OffersBatch response = offerContentStateController.calculateContentStatus(
            DataCampOffer.OffersBatch.newBuilder().addOffer(offer).build());

        assertThat(response.getOfferList().get(0).getContent().getBinding().getPartner().getMarketCategoryId())
            .isEqualTo(CATEGORY_ID);

    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void testAutoApproveCallSupplierOffers() {

        DataCampOffer.Offer offer = new TestDataCampOfferBuilder(SHOP_SKU_1, SUPPLIER_ID, CATEGORY_ID)
            .cardSource(true, MODEL_ID).build();

        when(ultraControllerService.enrich(anyList(), anyBoolean()))
            .thenReturn(Map.of(
                businessSkuKey(SHOP_SKU_1, SUPPLIER_ID), UltraController.EnrichedOffer.newBuilder().build()
            ));

        offerContentStateController.calculateContentStatus(
            DataCampOffer.OffersBatch.newBuilder().addOffer(offer).build());

        ArgumentCaptor<Collection<Offer>> captor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(applySettingsService, times(1)).applySettings(captor.capture());

        assertThat(captor.getValue().iterator().next().getDatacampSkuIdFromSearch()).isEqualTo(MODEL_ID);
    }

    @Test
    public void testNonExistingOffer() {
        List<DataCampOffer.Offer> testOffers = Stream.of(
                dataCampOffer(SHOP_SKU_1, SUPPLIER_ID).testComment("nothing will be found"),
                dataCampOffer(SHOP_SKU_2, SUPPLIER_ID).testComment("category with no knowledge will be found"),
                dataCampOffer(SHOP_SKU_3, SUPPLIER_GOOD_CONTENT_ID).testComment("valid for good content with model"),
                dataCampOffer(SHOP_SKU_4, SUPPLIER_ID).testComment("sku will be found"),
                dataCampOffer(SHOP_SKU_5, SUPPLIER_ID).testComment("category with knowledge will be found"),
                dataCampOffer(SHOP_SKU_6, SUPPLIER_GOOD_CONTENT_ID).testComment("valid for good content")
            )
            .map(TestDataCampOfferBuilder::build)
            .collect(Collectors.toList());

        when(ultraControllerService.enrich(anyList(), anyBoolean()))
            .thenReturn(Map.of(
                businessSkuKey(SHOP_SKU_1, SUPPLIER_ID), UltraController.EnrichedOffer.newBuilder().build(),
                businessSkuKey(SHOP_SKU_2, SUPPLIER_ID), UltraController.EnrichedOffer.newBuilder()
                    .setCategoryId(CATEGORY_NO_KNW_ID)
                    .setMarketCategoryName(CATEGORY_NO_KNW_NAME)
                    .build(),
                businessSkuKey(SHOP_SKU_3, SUPPLIER_GOOD_CONTENT_ID), UltraController.EnrichedOffer.newBuilder()
                    .setCategoryId(CATEGORY_KNW_GC_ID)
                    .setMarketCategoryName(CATEGORY_KNW_GC_NAME)
                    .setModelId(MODEL_ID)
                    .setMatchedId(MODEL_ID)
                    .setMarketModelName(MODEL_NAME)
                    .build(),
                businessSkuKey(SHOP_SKU_4, SUPPLIER_ID), UltraController.EnrichedOffer.newBuilder()
                    .setCategoryId(CATEGORY_ID)
                    .setMarketCategoryName(CATEGORY_NAME)
                    .setModelId(MODEL_ID)
                    .setMatchedId(MODEL_ID)
                    .setMarketModelName(MODEL_NAME)
                    .setMarketSkuId(SKU_ID)
                    .setMarketSkuName(SKU_NAME)
                    .setMarketSkuPublished(true)
                    .setMarketSkuPublishedOnMarket(true)
                    .setMarketSkuPublishedOnBlueMarket(true)
                    .build(),
                businessSkuKey(SHOP_SKU_5, SUPPLIER_ID), UltraController.EnrichedOffer.newBuilder()
                    .setCategoryId(CATEGORY_KNW_GC_ID)
                    .setMarketCategoryName(CATEGORY_KNW_GC_NAME)
                    .build(),
                businessSkuKey(SHOP_SKU_6, SUPPLIER_GOOD_CONTENT_ID), UltraController.EnrichedOffer.newBuilder()
                    .setCategoryId(CATEGORY_KNW_GC_ID)
                    .setMarketCategoryName(CATEGORY_KNW_GC_NAME)
                    .build()
            ));

        DataCampOffer.OffersBatch offersBatch = DataCampOffer.OffersBatch.newBuilder()
            .addAllOffer(testOffers)
            .build();
        DataCampOffer.OffersBatch response = offerContentStateController.calculateContentStatus(offersBatch);

        List<DataCampOffer.Offer> responseDCOffers = response.getOfferList();

        assertThat(responseDCOffers)
            .extracting(this::extractTestData)
            .containsExactly(
                dataCampOffer(SHOP_SKU_1, SUPPLIER_ID)
                    .status(
                        DataCampContentStatus.ContentSystemStatus.newBuilder()
                            .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_UNKNOWN)
                            .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                            .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                            .setAllowCategorySelection(true)
                            .setAllowModelSelection(false)
                            .setAllowModelCreateUpdate(false)
                            .setModelBarcodeRequired(true)
                            .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                            .setOfferHasServicePart(false)
                    )
                    .build(),
                dataCampOffer(SHOP_SKU_2, SUPPLIER_ID)
                    .modify(offer -> offer.contentBindingUcMapping()
                        .setMarketCategoryId(CATEGORY_NO_KNW_ID)
                        .setMarketCategoryName(CATEGORY_NO_KNW_NAME))
                    .modify(offer -> offer.contentBindingRuntimeUcMapping()
                        .setMarketCategoryId(CATEGORY_NO_KNW_ID))
                    .approvedCategory(CATEGORY_NO_KNW_ID, CATEGORY_NO_KNW_NAME)
                    .status(
                        DataCampContentStatus.ContentSystemStatus.newBuilder()
                            .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_MISSING)
                            .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                            .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                            .setAllowCategorySelection(true)
                            .setAllowModelSelection(true)
                            .setAllowModelCreateUpdate(false)
                            .setModelBarcodeRequired(true)
                            .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                            .setOfferHasServicePart(false)
                    )
                    .build(),
                dataCampOffer(SHOP_SKU_3, SUPPLIER_GOOD_CONTENT_ID)
                    .modify(offer -> offer.contentBindingUcMapping()
                        .setMarketCategoryId(CATEGORY_KNW_GC_ID)
                        .setMarketCategoryName(CATEGORY_KNW_GC_NAME)
                        .setMarketModelId(MODEL_ID)
                        .setMarketModelName(MODEL_NAME))
                    .modify(offer -> offer.contentBindingRuntimeUcMapping()
                        .setMarketCategoryId(CATEGORY_KNW_GC_ID))
                    .approvedCategory(CATEGORY_KNW_GC_ID, CATEGORY_KNW_GC_NAME)
                    .approvedModel(MODEL_ID, MODEL_NAME)
                    .status(
                        DataCampContentStatus.ContentSystemStatus.newBuilder()
                            .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
                            .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                            .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                            .setAllowCategorySelection(false)
                            .setAllowModelSelection(false)
                            .setAllowModelCreateUpdate(false)
                            .setModelBarcodeRequired(true)
                            .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                            .setOfferHasServicePart(false)
                    )
                    .build(),
                dataCampOffer(SHOP_SKU_4, SUPPLIER_ID)
                    .modify(offer -> offer.contentBindingUcMapping()
                        .setMarketCategoryId(CATEGORY_ID)
                        .setMarketCategoryName(CATEGORY_NAME)
                        .setMarketModelId(MODEL_ID)
                        .setMarketModelName(MODEL_NAME)
                        .setMarketSkuId(SKU_ID)
                        .setMarketSkuName(SKU_NAME))
                    .modify(offer -> offer.contentBindingRuntimeUcMapping()
                        .setMarketCategoryId(CATEGORY_ID)
                        .setMarketSkuId(SKU_ID))
                    .approvedCategory(CATEGORY_ID, CATEGORY_NAME)
                    .approvedModel(MODEL_ID, MODEL_NAME)
                    .status(
                        DataCampContentStatus.ContentSystemStatus.newBuilder()
                            .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
                            .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                            .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                            .setAllowCategorySelection(false)
                            .setAllowModelSelection(false)
                            .setAllowModelCreateUpdate(false)
                            .setModelBarcodeRequired(true)
                            .setSkuMappingConfidence(DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_AUTO)
                            .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                            .setOfferHasServicePart(false)
                    )
                    .build(),
                dataCampOffer(SHOP_SKU_5, SUPPLIER_ID)
                    .modify(offer -> offer.contentBindingUcMapping()
                        .setMarketCategoryId(CATEGORY_KNW_GC_ID)
                        .setMarketCategoryName(CATEGORY_KNW_GC_NAME))
                    .modify(offer -> offer.contentBindingRuntimeUcMapping()
                        .setMarketCategoryId(CATEGORY_KNW_GC_ID))
                    .approvedCategory(CATEGORY_KNW_GC_ID, CATEGORY_KNW_GC_NAME)
                    .status(
                        DataCampContentStatus.ContentSystemStatus.newBuilder()
                            .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                            .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                            .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                            .setAllowCategorySelection(true)
                            .setAllowModelSelection(true)
                            .setAllowModelCreateUpdate(false)
                            .setModelBarcodeRequired(true)
                            .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                            .setOfferHasServicePart(false)
                    )
                    .build(),
                dataCampOffer(SHOP_SKU_6, SUPPLIER_GOOD_CONTENT_ID)
                    .modify(offer -> offer.contentBindingUcMapping()
                        .setMarketCategoryId(CATEGORY_KNW_GC_ID)
                        .setMarketCategoryName(CATEGORY_KNW_GC_NAME))
                    .modify(offer -> offer.contentBindingRuntimeUcMapping()
                        .setMarketCategoryId(CATEGORY_KNW_GC_ID))
                    .approvedCategory(CATEGORY_KNW_GC_ID, CATEGORY_KNW_GC_NAME)
                    .status(
                        DataCampContentStatus.ContentSystemStatus.newBuilder()
                            .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                            .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN)
                            .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                            .setAllowCategorySelection(true)
                            .setAllowModelSelection(true)
                            .setAllowModelCreateUpdate(false)
                            .setModelBarcodeRequired(true)
                            .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                            .setOfferHasServicePart(false)
                    )
                    .build()
            );
    }

    @SuppressWarnings("checkstyle:MethodLength")
    @Test
    public void testExistingOfferCpcState() {
        ContentComment testComment = new ContentComment(ContentCommentType.ASSORTMENT, "Test comment");

        var offerWithApproveMappingOk = OfferTestUtils.simpleOffer()
            .setShopSku(SHOP_SKU_3)
            .setBusinessId(SUPPLIER_ID)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests((long) CATEGORY_ID, Offer.BindingKind.SUPPLIER)
            .setModelId((long) MODEL_ID)
            .setMarketModelName(MODEL_NAME)
            .updateApprovedSkuMapping(new Offer.Mapping(SKU_ID, DateTimeUtils.dateTimeNow()),
                Offer.MappingConfidence.CONTENT)
            .setContentComments(testComment);

        offerRepository.insertOffers(
            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_1)
                .setBusinessId(SUPPLIER_GOOD_CONTENT_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
                .setCategoryIdForTests((long) CATEGORY_KNW_GC_ID, Offer.BindingKind.SUPPLIER)
                .setModelId((long) MODEL_ID)
                .setMarketModelName(MODEL_NAME)
                .setContentComments(testComment),

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_2)
                .setBusinessId(SUPPLIER_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
                .setCategoryIdForTests((long) CATEGORY_ID, Offer.BindingKind.SUPPLIER)
                .setModelId((long) MODEL_ID)
                .setMarketModelName(MODEL_NAME)
                .setSuggestSkuMapping(new Offer.Mapping(SKU_ID, DateTimeUtils.dateTimeNow()))
                .setContentComments(testComment),

            offerWithApproveMappingOk,

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_4)
                .setBusinessId(SUPPLIER_GOOD_CONTENT_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setCategoryIdForTests((long) CATEGORY_KNW_GC_ID, Offer.BindingKind.SUGGESTED)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .setContentComments(testComment),

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_5)
                .setBusinessId(SUPPLIER_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
                .setCategoryIdForTests((long) CATEGORY_NO_KNW_ID, Offer.BindingKind.SUPPLIER)
                .setContentComments(testComment),

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_6)
                .setBusinessId(SUPPLIER_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
                .setCategoryIdForTests((long) CATEGORY_KNW_GC_ID, Offer.BindingKind.SUPPLIER)
                .setContentComments(testComment),

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_7)
                .setBusinessId(SUPPLIER_GOOD_CONTENT_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
                .setMappingDestination(Offer.MappingDestination.WHITE)
                .setCategoryIdForTests((long) CATEGORY_KNW_GC_ID, Offer.BindingKind.SUPPLIER)
                .setContentComments(testComment),

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_8)
                .setBusinessId(SUPPLIER_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
                .setContentComments(testComment)
        );

        List<DataCampOffer.Offer> testOffers = Stream.of(
                dataCampOffer(SHOP_SKU_1, SUPPLIER_GOOD_CONTENT_ID).testComment("valid for good content wiht model"),
                dataCampOffer(SHOP_SKU_2, SUPPLIER_ID).testComment("has suggest mapping"),
                dataCampOffer(SHOP_SKU_3, SUPPLIER_ID).testComment("has CONTENT approved mapping"),
                dataCampOffer(SHOP_SKU_4, SUPPLIER_GOOD_CONTENT_ID).testComment("has status CONTENT_PROCESSING"),
                dataCampOffer(SHOP_SKU_5, SUPPLIER_ID).testComment("category with no knowledge"),
                dataCampOffer(SHOP_SKU_6, SUPPLIER_ID).testComment("category with knowledge, supplier can't send " +
                    "content"),
                dataCampOffer(SHOP_SKU_7, SUPPLIER_GOOD_CONTENT_ID).testComment("valid for good content"),
                dataCampOffer(SHOP_SKU_8, SUPPLIER_ID).testComment("otherwise")
            )
            .map(TestDataCampOfferBuilder::build)
            .collect(Collectors.toList());

        when(ultraControllerService.enrich(anyList(), anyBoolean()))
            .thenAnswer(invocation -> {
                List<Offer> offers = invocation.getArgument(0);
                return offers.stream()
                    .collect(Collectors.toMap(
                        Offer::getBusinessSkuKey,
                        __ -> UltraController.EnrichedOffer.newBuilder().build()
                    ));
            });

        DataCampOffer.OffersBatch offersBatch = DataCampOffer.OffersBatch.newBuilder()
            .addAllOffer(testOffers)
            .build();
        DataCampOffer.OffersBatch response = offerContentStateController.calculateContentStatus(offersBatch);

        Map<BusinessSkuKey, DataCampOffer.Offer> responseDCOffers = response.getOfferList().stream()
            .collect(Collectors.toMap(DataCampOfferUtil::extractExternalBusinessSkuKey, this::extractTestData));
        assertThat(responseDCOffers).hasSize(8);

        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_GOOD_CONTENT_ID, SHOP_SKU_1)))
            .isEqualTo(dataCampOffer(SHOP_SKU_1, SUPPLIER_GOOD_CONTENT_ID)
                .approvedCategory(CATEGORY_KNW_GC_ID, CATEGORY_KNW_GC_NAME)
                .approvedModel(MODEL_ID, MODEL_NAME)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_CONTENT)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                        .setAllowCategorySelection(false)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(true)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_2)))
            .isEqualTo(dataCampOffer(SHOP_SKU_2, SUPPLIER_ID)
                .approvedCategory(CATEGORY_ID, CATEGORY_NAME)
                .approvedModel(MODEL_ID, MODEL_NAME)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                        .setAllowCategorySelection(false)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setSkuMappingConfidence(DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_AUTO)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_3)))
            .isEqualTo(dataCampOffer(SHOP_SKU_3, SUPPLIER_ID)
                .approvedCategory(CATEGORY_ID, CATEGORY_NAME)
                .approvedModel(MODEL_ID, MODEL_NAME)
                .approvedSku(SKU_ID, SKU_NAME, MARKET_SKU_TYPE_UNKNOWN)
                .mskuChangeTsFromMboc(offerWithApproveMappingOk.getApprovedSkuMapping().getInstant())
                .modify(dcOffer -> dcOffer.contentBindingRuntimeUcMapping()
                    .setMarketCategoryId(CATEGORY_ID)
                    .setMarketSkuId(SKU_ID))
                .modify(dcOffer -> dcOffer.contentBindingApproved()
                    // Instant.MIN as default constant in msku
                    .setMarketSkuCreationTs(DataCampOfferUtil.toTimestamp(Instant.MIN)))
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(false)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setSkuMappingConfidence(DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_GOOD_CONTENT_ID, SHOP_SKU_4)))
            .isEqualTo(dataCampOffer(SHOP_SKU_4, SUPPLIER_GOOD_CONTENT_ID)
                .approvedCategory(CATEGORY_KNW_GC_ID, CATEGORY_KNW_GC_NAME)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(true)
                        .setAllowModelCreateUpdate(true)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_5)))
            .isEqualTo(dataCampOffer(SHOP_SKU_5, SUPPLIER_ID)
                .approvedCategory(CATEGORY_NO_KNW_ID, CATEGORY_NO_KNW_NAME)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_MISSING)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(true)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_6)))
            .isEqualTo(dataCampOffer(SHOP_SKU_6, SUPPLIER_ID)
                .approvedCategory(CATEGORY_KNW_GC_ID, CATEGORY_KNW_GC_NAME)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(true)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_GOOD_CONTENT_ID, SHOP_SKU_7)))
            .isEqualTo(dataCampOffer(SHOP_SKU_7, SUPPLIER_GOOD_CONTENT_ID)
                .approvedCategory(CATEGORY_KNW_GC_ID, CATEGORY_KNW_GC_NAME)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_CONTENT)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(true)
                        .setAllowModelCreateUpdate(true)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_8)))
            .isEqualTo(dataCampOffer(SHOP_SKU_8, SUPPLIER_ID)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_UNKNOWN)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
    }

    @SuppressWarnings("checkstyle:MethodLength")
    @Test
    public void testExistingOfferCpaState() {
        ContentComment testComment = new ContentComment(ContentCommentType.ASSORTMENT, "Test comment");
        var offerWithApprovedMappingOk = OfferTestUtils.simpleOffer()
            .setShopSku(SHOP_SKU_1)
            .setBusinessId(SUPPLIER_ID)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests((long) CATEGORY_ID, Offer.BindingKind.SUPPLIER)
            .setModelId((long) MODEL_ID)
            .setMarketModelName(MODEL_NAME)
            .updateApprovedSkuMapping(new Offer.Mapping(SKU_ID, DateTimeUtils.dateTimeNow()),
                Offer.MappingConfidence.CONTENT)
            .setContentComments(testComment);

        var offerWithApprovedMappingWithErrors = OfferTestUtils.simpleOffer()
            .setShopSku(SHOP_SKU_11)
            .setBusinessId(SUPPLIER_ID)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests((long) CATEGORY_ID, Offer.BindingKind.SUPPLIER)
            .setModelId((long) MODEL_ID)
            .setMarketModelName(MODEL_NAME)
            .updateApprovedSkuMapping(new Offer.Mapping(SKU_ID, DateTimeUtils.dateTimeNow(), Offer.SkuType.PARTNER20),
                Offer.MappingConfidence.PARTNER_SELF)
            .setContentComments(testComment)
            .setContentStatusActiveError(MbocErrors.get().barcodeRequired(SHOP_SKU_11));

        offerRepository.insertOffers(
            offerWithApprovedMappingOk,

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_2)
                .setBusinessId(SUPPLIER_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH)
                .setContentComments(testComment),

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_3)
                .setBusinessId(SUPPLIER_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW)
                .setContentComments(testComment),

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_4)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setBusinessId(SUPPLIER_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO)
                .setContentComments(testComment),
            OfferTestUtils.simpleOffer()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setShopSku(SHOP_SKU_6)
                .setBusinessId(SUPPLIER_ID)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_SIZE_MEASURE)
                .setContentComments(testComment),

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_7)
                .setBusinessId(SUPPLIER_GOOD_CONTENT_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setCategoryIdForTests((long) CATEGORY_KNW_GC_ID, Offer.BindingKind.SUGGESTED)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .setContentComments(testComment),

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_8)
                .setBusinessId(SUPPLIER_GOOD_CONTENT_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setCategoryIdForTests((long) CATEGORY_KNW_GC_ID, Offer.BindingKind.SUGGESTED)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
                .setContentComments(testComment),

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_9)
                .setBusinessId(SUPPLIER_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE)
                .setContentComments(testComment),
            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_10)
                .setBusinessId(SUPPLIER_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_CATEGORY)
                .setContentComments(testComment),

            offerWithApprovedMappingWithErrors,

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_12)
                .setBusinessId(SUPPLIER_GOOD_CONTENT_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setCategoryIdForTests((long) CATEGORY_KNW_GC_ID, Offer.BindingKind.SUGGESTED)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
                .setContentComments(testComment)
                .setContentStatusActiveError(MbocErrors.get().barcodeRequired(SHOP_SKU_12)),

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_13)
                .setBusinessId(SUPPLIER_GOOD_CONTENT_ID)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setCategoryIdForTests((long) CATEGORY_KNW_GC_ID, Offer.BindingKind.SUGGESTED)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.REOPEN)
                .setContentComments(testComment)
                .setContentStatusActiveError(MbocErrors.get().barcodeRequired(SHOP_SKU_13)),

            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_999)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setBusinessId(SUPPLIER_ID)
                .setContentComments(testComment)
        );

        List<DataCampOffer.Offer> testOffers = Stream.of(
                dataCampOffer(SHOP_SKU_1, SUPPLIER_ID).testComment("has CONTENT approved mapping"),
                dataCampOffer(SHOP_SKU_2, SUPPLIER_ID).testComment("has acceptanceStatus TRASH"),
                dataCampOffer(SHOP_SKU_3, SUPPLIER_ID).testComment("has acceptanceStatus NEW"),
                dataCampOffer(SHOP_SKU_4, SUPPLIER_ID).testComment("has status NEED_INFO"),
                dataCampOffer(SHOP_SKU_6, SUPPLIER_ID).testComment("has status NEED_SIZE_MEASURE"),
                dataCampOffer(SHOP_SKU_7, SUPPLIER_GOOD_CONTENT_ID).testComment("has status NEED_CONTENT"),
                dataCampOffer(SHOP_SKU_8, SUPPLIER_GOOD_CONTENT_ID).testComment("has status CONTENT_PROCESSING"),
                dataCampOffer(SHOP_SKU_9, SUPPLIER_ID).testComment("has status NO_KNOWLEDGE"),
                dataCampOffer(SHOP_SKU_10, SUPPLIER_ID).testComment("has status NO_CATEGORY"),
                dataCampOffer(SHOP_SKU_11, SUPPLIER_ID).testComment("has PARTNER_SELF mapping with processing errors"),
                dataCampOffer(SHOP_SKU_12, SUPPLIER_GOOD_CONTENT_ID)
                    .testComment("has status NEED_CONTENT with processing errors"),
                dataCampOffer(SHOP_SKU_13, SUPPLIER_GOOD_CONTENT_ID)
                    .testComment("has status REOPEN and calculated status NEED_CONTENT with processing errors"),
                dataCampOffer(SHOP_SKU_999, SUPPLIER_ID).testComment("otherwise")
            )
            .map(TestDataCampOfferBuilder::build)
            .collect(Collectors.toList());

        when(ultraControllerService.enrich(anyList(), anyBoolean()))
            .thenAnswer(invocation -> {
                List<Offer> offers = invocation.getArgument(0);
                return offers.stream()
                    .collect(Collectors.toMap(
                        Offer::getBusinessSkuKey,
                        __ -> UltraController.EnrichedOffer.newBuilder().build()
                    ));
            });

        DataCampOffer.OffersBatch offersBatch = DataCampOffer.OffersBatch.newBuilder()
            .addAllOffer(testOffers)
            .build();
        DataCampOffer.OffersBatch response = offerContentStateController.calculateContentStatus(offersBatch);

        Map<BusinessSkuKey, DataCampOffer.Offer> responseDCOffers = response.getOfferList().stream()
            .collect(Collectors.toMap(DataCampOfferUtil::extractExternalBusinessSkuKey, this::extractTestData));
        assertThat(responseDCOffers).hasSize(13);
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_1)))
            .isEqualTo(dataCampOffer(SHOP_SKU_1, SUPPLIER_ID)
                .approvedCategory(CATEGORY_ID, CATEGORY_NAME)
                .approvedModel(MODEL_ID, MODEL_NAME)
                .approvedSku(SKU_ID, SKU_NAME, MARKET_SKU_TYPE_UNKNOWN)
                .modify(dcOffer -> dcOffer.contentBindingApproved()
                    .setMarketSkuCreationTs(DataCampOfferUtil.toTimestamp(Instant.MIN)))
                .mskuChangeTsFromMboc(offerWithApprovedMappingOk.getApprovedSkuMapping().getInstant())
                .modify(dcOffer -> dcOffer.contentBindingRuntimeUcMapping()
                    .setMarketCategoryId(CATEGORY_ID)
                    .setMarketSkuId(SKU_ID)
                )
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(false)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setSkuMappingConfidence(DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_2)))
            .isEqualTo(dataCampOffer(SHOP_SKU_2, SUPPLIER_ID)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_UNKNOWN)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REJECTED)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_TRASH)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_3)))
            .isEqualTo(dataCampOffer(SHOP_SKU_3, SUPPLIER_ID)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_UNKNOWN)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_4)))
            .isEqualTo(dataCampOffer(SHOP_SKU_4, SUPPLIER_ID)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_UNKNOWN)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_INFO)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_6)))
            .isEqualTo(dataCampOffer(SHOP_SKU_6, SUPPLIER_ID)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_UNKNOWN)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_INFO)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_GOOD_CONTENT_ID, SHOP_SKU_7)))
            .isEqualTo(dataCampOffer(SHOP_SKU_7, SUPPLIER_GOOD_CONTENT_ID)
                .approvedCategory(CATEGORY_KNW_GC_ID, CATEGORY_KNW_GC_NAME)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_CONTENT)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_CONTENT)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(true)
                        .setAllowModelCreateUpdate(true)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_GOOD_CONTENT_ID, SHOP_SKU_8)))
            .isEqualTo(dataCampOffer(SHOP_SKU_8, SUPPLIER_GOOD_CONTENT_ID)
                .approvedCategory(CATEGORY_KNW_GC_ID, CATEGORY_KNW_GC_NAME)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_PROCESSING)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CONTENT_PROCESSING)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(true)
                        .setAllowModelCreateUpdate(true)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_9)))
            .isEqualTo(dataCampOffer(SHOP_SKU_9, SUPPLIER_ID)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_UNKNOWN)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_SUSPENDED)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_10)))
            .isEqualTo(dataCampOffer(SHOP_SKU_10, SUPPLIER_ID)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_UNKNOWN)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_SUSPENDED)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(false)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_11)))
            .isEqualTo(dataCampOffer(SHOP_SKU_11, SUPPLIER_ID)
                .approvedCategory(CATEGORY_ID, CATEGORY_NAME)
                .approvedModel(MODEL_ID, MODEL_NAME)
                .approvedSku(SKU_ID, SKU_NAME, MARKET_SKU_TYPE_PSKU)
                .mskuChangeTsFromMboc(offerWithApprovedMappingWithErrors.getApprovedSkuMapping().getInstant())
                .modify(dcOffer -> dcOffer.contentBindingRuntimeUcMapping()
                    .setMarketCategoryId(CATEGORY_ID)
                    .setMarketSkuId(SKU_ID))
                .modify(dcOffer -> dcOffer.contentBindingApproved()
                    .setMarketSkuCreationTs(DataCampOfferUtil.toTimestamp(Instant.MIN)))
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_CARD_UPDATE_ERROR)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CARD_UPDATE_ERROR)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(true)
                        .setModelBarcodeRequired(true)
                        .setSkuMappingConfidence(DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_PARTNER_SELF)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_GOOD_CONTENT_ID, SHOP_SKU_12)))
            .isEqualTo(dataCampOffer(SHOP_SKU_12, SUPPLIER_GOOD_CONTENT_ID)
                .approvedCategory(CATEGORY_KNW_GC_ID, CATEGORY_KNW_GC_NAME)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_CARD_CREATE_ERROR)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CARD_CREATE_ERROR)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(true)
                        .setAllowModelCreateUpdate(true)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_GOOD_CONTENT_ID, SHOP_SKU_13)))
            .isEqualTo(dataCampOffer(SHOP_SKU_13, SUPPLIER_GOOD_CONTENT_ID)
                .approvedCategory(CATEGORY_KNW_GC_ID, CATEGORY_KNW_GC_NAME)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_CARD_CREATE_ERROR)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(true)
                        .setAllowModelCreateUpdate(true)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
        assertThat(responseDCOffers.get(new BusinessSkuKey(SUPPLIER_ID, SHOP_SKU_999)))
            .isEqualTo(dataCampOffer(SHOP_SKU_999, SUPPLIER_ID)
                .status(
                    DataCampContentStatus.ContentSystemStatus.newBuilder()
                        .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_UNKNOWN)
                        .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK)
                        .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                        .setAllowCategorySelection(true)
                        .setAllowModelSelection(false)
                        .setAllowModelCreateUpdate(false)
                        .setModelBarcodeRequired(true)
                        .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                        .setOfferHasServicePart(true)
                )
                .build());
    }

    @Test
    public void testExistingOfferSupplierChangesCategory() {
        offerRepository.insertOffers(
            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_1)
                .setBusinessId(SUPPLIER_GOOD_CONTENT_ID)
                .setCategoryIdForTests((long) CATEGORY_KNW_GC_ID, Offer.BindingKind.SUGGESTED)
        );

        when(ultraControllerService.enrich(anyList(), anyBoolean()))
            .thenReturn(Map.of(
                    businessSkuKey(SHOP_SKU_1, SUPPLIER_GOOD_CONTENT_ID), UltraController.EnrichedOffer.newBuilder()
                        .setCategoryId(CATEGORY_KNW_GC_ID)
                        .setMarketCategoryName(CATEGORY_NO_KNW_NAME)
                        .build()
                )
            );

        List<DataCampOffer.Offer> testOffers = Stream.of(
                dataCampOffer(SHOP_SKU_1, SUPPLIER_GOOD_CONTENT_ID)
                    .partnerCategory(CATEGORY_NO_KNW_ID, CATEGORY_NO_KNW_NAME)
                    .testComment("changed category to one without knowledge")
            )
            .map(TestDataCampOfferBuilder::build)
            .collect(Collectors.toList());

        DataCampOffer.OffersBatch offersBatch = DataCampOffer.OffersBatch.newBuilder()
            .addAllOffer(testOffers)
            .build();
        DataCampOffer.OffersBatch response = offerContentStateController.calculateContentStatus(offersBatch);

        List<DataCampOffer.Offer> responseDCOffers = response.getOfferList();

        assertThat(responseDCOffers)
            .extracting(this::extractTestData)
            .containsExactly(
                dataCampOffer(SHOP_SKU_1, SUPPLIER_GOOD_CONTENT_ID)
                    .modify(offer -> offer.contentBindingUcMapping()
                        .setMarketCategoryId(CATEGORY_KNW_GC_ID)
                        .setMarketCategoryName(CATEGORY_NO_KNW_NAME))
                    .modify(offer -> offer.contentBindingRuntimeUcMapping()
                        .setMarketCategoryId(CATEGORY_NO_KNW_ID))
                    .approvedCategory(CATEGORY_NO_KNW_ID, CATEGORY_NO_KNW_NAME)
                    .status(
                        DataCampContentStatus.ContentSystemStatus.newBuilder()
                            .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_MISSING)
                            .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                            .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                            .setAllowCategorySelection(true)
                            .setAllowModelSelection(true)
                            .setAllowModelCreateUpdate(false)
                            .setModelBarcodeRequired(true)
                            .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                            .setOfferHasServicePart(true)
                            .setPartnerMappingStatus(DataCampContentStatus.PartnerMappingStatus.newBuilder()
                                .setSkuMappingState(MAPPING_STATUS_NONE)
                                .setModelMappingState(MAPPING_STATUS_NONE)
                                .setCategoryMappingState(MAPPING_STATUS_ACCEPTED))
                    )
                    .build()
            );
    }

    @Test
    public void testExistingOfferEnrichmentResults() {
        offerRepository.insertOffers(
            OfferTestUtils.simpleOffer()
                .setShopSku(SHOP_SKU_1)
                .setBusinessId(SUPPLIER_GOOD_CONTENT_ID)
                .setCategoryIdForTests((long) CATEGORY_KNW_GC_ID, Offer.BindingKind.SUGGESTED)
        );

        when(ultraControllerService.enrich(anyList(), anyBoolean()))
            .thenReturn(Map.of(
                    businessSkuKey(SHOP_SKU_1, SUPPLIER_GOOD_CONTENT_ID), UltraController.EnrichedOffer.newBuilder()
                        .setCategoryId(CATEGORY_ID)
                        .setMarketCategoryName(CATEGORY_NAME)
                        .setModelId(MODEL_ID)
                        .setMatchedId(MODEL_ID)
                        .setMarketModelName(MODEL_NAME)
                        .setMarketSkuId(SKU_ID)
                        .setMarketSkuName(SKU_NAME)
                        .setMarketSkuPublished(true)
                        .setMarketSkuPublishedOnMarket(true)
                        .setMarketSkuPublishedOnBlueMarket(true)
                        .setEnrichType(UltraController.EnrichedOffer.EnrichType.MAIN)
                        .build()
                )
            );

        List<DataCampOffer.Offer> testOffers = Stream.of(
                dataCampOffer(SHOP_SKU_1, SUPPLIER_GOOD_CONTENT_ID)
                    .testComment("changed category to one without knowledge")
            )
            .map(TestDataCampOfferBuilder::build)
            .collect(Collectors.toList());

        DataCampOffer.OffersBatch offersBatch = DataCampOffer.OffersBatch.newBuilder()
            .addAllOffer(testOffers)
            .build();
        DataCampOffer.OffersBatch response = offerContentStateController.calculateContentStatus(offersBatch);

        List<DataCampOffer.Offer> responseDCOffers = response.getOfferList();

        assertThat(responseDCOffers)
            .extracting(this::extractTestData)
            .containsExactly(
                dataCampOffer(SHOP_SKU_1, SUPPLIER_GOOD_CONTENT_ID)
                    .modify(offer -> offer.contentBindingUcMapping()
                        .setMarketCategoryId(CATEGORY_ID)
                        .setMarketCategoryName(CATEGORY_NAME)
                        .setMarketModelId(MODEL_ID)
                        .setMarketModelName(MODEL_NAME)
                        .setMarketSkuId(SKU_ID)
                        .setMarketSkuName(SKU_NAME))
                    .modify(offer -> offer.contentBindingRuntimeUcMapping()
                        .setMarketCategoryId(CATEGORY_ID)
                        .setMarketSkuId(SKU_ID))
                    .approvedCategory(CATEGORY_ID, CATEGORY_NAME)
                    .approvedModel(MODEL_ID, MODEL_NAME)
                    .status(
                        DataCampContentStatus.ContentSystemStatus.newBuilder()
                            .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY)
                            .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_REVIEW)
                            .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_NEW)
                            .setAllowCategorySelection(false)
                            .setAllowModelSelection(false)
                            .setAllowModelCreateUpdate(false)
                            .setModelBarcodeRequired(true)
                            .setSkuMappingConfidence(DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_AUTO)
                            .setCategoryRestriction(INDETERMINABLE_RESTRICTION)
                            .setOfferHasServicePart(true)
                    )
                    .build()
            );
    }

    @Test
    public void testMarketParameterValueConversion() {
        DataCampOffer.Offer offerWithParamsAndAdult = new TestDataCampOfferBuilder(SHOP_SKU_4, SUPPLIER_ID, true)
            .build();

        MarketParameters.MarketParameterValue converted = UltraControllerServiceImpl.convert(
            offerWithParamsAndAdult.getContent().getPartner()
                .getMarketSpecificContent().getParameterValues().getParameterValues(0));

        assertThat(converted).isEqualTo(
            MarketParameters.MarketParameterValue.newBuilder()
                .setParamId(10L)
                .setParamName("MarketParam")
                .setValue(MarketParameters.MarketValue.newBuilder()
                    .setValueType(MarketParameters.MarketValueType.ENUM)
                    .setOptionId(12L)
                    .setStrValue("18")
                    .build())
                .build());
    }

    private DataCampOffer.Offer extractTestData(DataCampOffer.Offer dcOffer) {
        DataCampOfferIdentifiers.OfferIdentifiers identifiers = dcOffer.getIdentifiers();

        DataCampOffer.Offer.Builder builder = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(identifiers.toBuilder());

        DataCampOfferContent.OfferContent.Builder contentBuilder = builder.getContentBuilder();

        DataCampOfferContent.OriginalSpecification contentPartnerOriginal =
            dcOffer.getContent().getPartner().getOriginal();
        contentBuilder.getPartnerBuilder().setOriginal(contentPartnerOriginal);

        if (dcOffer.getContent().getBinding().hasApproved()) {
            contentBuilder.getBindingBuilder().setApproved(
                dcOffer.getContent().getBinding().getApproved().toBuilder().clearMeta()
            );
        }
        if (dcOffer.getContent().getBinding().hasUcMapping()) {
            contentBuilder.getBindingBuilder().setUcMapping(
                dcOffer.getContent().getBinding().getUcMapping().toBuilder().clearMeta()
            );
        }
        if (dcOffer.getContent().getBinding().hasRuntimeUcMapping()) {
            contentBuilder.getBindingBuilder().setRuntimeUcMapping(
                dcOffer.getContent().getBinding().getRuntimeUcMapping().toBuilder().clearMeta()
            );
        }

        DataCampContentStatus.ContentSystemStatus.Builder contentSystemStatus =
            dcOffer.getContent().getStatus().getContentSystemStatus().toBuilder();
        contentSystemStatus.clearMeta();
        if (contentSystemStatus.hasPartnerMappingStatus()) {
            contentSystemStatus.getPartnerMappingStatusBuilder().clearTimestamp();
        }
        contentBuilder.getStatusBuilder()
            .setContentSystemStatus(contentSystemStatus);

        return builder.build();
    }

    private DataCampExplanation.Explanation convertMbocComment(ContentComment comment) {
        return DataCampExplanation.Explanation.newBuilder()
            .setCode(comment.getType().getMessageCode())
            .setNamespace("mboc.ci.error")
            .setLevel(DataCampExplanation.Explanation.Level.ERROR)
            .setText(comment.getType().getDescription())
            .addParams(DataCampExplanation.Explanation.Param.newBuilder()
                .setName("itemsAsString")
                .setValue(String.join(", ", comment.getItems())))
            .build();
    }

    public static class TestDataCampOfferBuilder {
        private final DataCampOffer.Offer.Builder builder;
        private String testComment;

        public TestDataCampOfferBuilder(String shopSku, int businessId) {
            this(shopSku, businessId, null);
        }

        public TestDataCampOfferBuilder(String shopSku, int businessId, @Nullable BigDecimal price) {
            builder = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setOfferId(shopSku)
                    .setBusinessId(businessId)
                    .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                    .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                        .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                            .setBarcode(stringListValue(BARCODE))
                            .setName(stringValue(OFFER_NAME))
                            .setDescription(stringValue(OFFER_DESCRIPTION))
                            .setVendor(stringValue(VENDOR))
                            .setVendorCode(stringValue(VENDOR_CODE))
                            .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                                .setName(CATEGORY_NAME)
                            )
                        )
                    )
                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                        .setApproved(DataCampOfferMapping.Mapping.newBuilder().build())
                    )
                );
            if (price != null) {
                builder.setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                    .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                        .setBinaryPrice(Common.PriceExpression.newBuilder()
                            .setPrice(price.movePointRight(7).longValue())
                            .build())
                        .build()
                    )
                    .build()
                );
            }
        }

        public TestDataCampOfferBuilder(String shopSku, int businessId, boolean addAdultAndOfferParams) {
            if (addAdultAndOfferParams) {
                builder = DataCampOffer.Offer.newBuilder()
                    .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId(shopSku)
                        .setBusinessId(businessId)
                        .build())
                    .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                            .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                                .setBarcode(stringListValue(BARCODE))
                                .setName(stringValue(OFFER_NAME))
                                .setDescription(stringValue(OFFER_DESCRIPTION))
                                .setVendor(stringValue(VENDOR))
                                .setVendorCode(stringValue(VENDOR_CODE))
                                .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                                    .setName(CATEGORY_NAME)
                                )
                                .setAdult(DataCampOfferMeta.Flag.newBuilder().setFlag(true).build())
                                .setOfferParams(DataCampOfferContent.ProductYmlParams.newBuilder()
                                    .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                        .setName("test-param")
                                        .setValue("test-param-value")
                                        .build()).build()
                                ).build()
                            )
                            .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent.newBuilder()
                                .setParameterValues(DataCampOfferMarketContent.MarketParameterValues.newBuilder()
                                    .addParameterValues(DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                        .setParamId(10L)
                                        .setParamName("MarketParam")
                                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                            .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                            .setOptionId(12L)
                                            .setStrValue("18")
                                            .build())
                                        .build())
                                    .addParameterValues(DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                        .setParamId(15L)
                                        .setParamName("MarketNumericParam")
                                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                            .setValueType(DataCampContentMarketParameterValue.MarketValueType.NUMERIC)
                                            .setNumericValue("13")
                                            .build())
                                        .build())
                                    .addParameterValues(DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                                        .setParamId(16L)
                                        .setParamName("MarketBoolParam")
                                        .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                            .setValueType(DataCampContentMarketParameterValue.MarketValueType.BOOLEAN)
                                            .setBoolValue(true)
                                            .build())
                                        .build())
                                    .build())
                                .build())
                            .build()
                        ).build());
            } else {
                this.builder = new TestDataCampOfferBuilder(shopSku, businessId).builder;
            }
        }

        public TestDataCampOfferBuilder(String shopSku, int businessId, int marketCategoryId) {
            builder = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setOfferId(shopSku)
                    .setBusinessId(businessId)
                    .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                        .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                            .setMarketCategoryId(marketCategoryId)
                            .build())
                        .build())
                    .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                        .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                            .setBarcode(stringListValue(BARCODE))
                            .setName(stringValue(OFFER_NAME))
                            .setDescription(stringValue(OFFER_DESCRIPTION))
                            .setVendor(stringValue(VENDOR))
                            .setVendorCode(stringValue(VENDOR_CODE))
                            .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                                .setName(CATEGORY_NAME)
                            ).build()
                        ).build()
                    ).build());
        }

        public static TestDataCampOfferBuilder dataCampOffer(String shopSku, int businessId) {
            return new TestDataCampOfferBuilder(shopSku, businessId);
        }

        public TestDataCampOfferBuilder testComment(String testComment) {
            this.testComment = testComment;
            return this;
        }

        public TestDataCampOfferBuilder partnerCategory(int categoryId, String categoryName) {
            contentBindingPartner()
                .setMarketCategoryId(categoryId)
                .setMarketCategoryName(categoryName);
            return this;
        }

        public TestDataCampOfferBuilder approvedCategory(int categoryId, String categoryName) {
            contentBindingApproved()
                .setMarketCategoryId(categoryId)
                .setMarketCategoryName(categoryName);
            return this;
        }

        public TestDataCampOfferBuilder cardSource(boolean isPresent, long skuId) {
            builder.getContentBuilder().getPartnerBuilder().getOriginalBuilder()
                .setCardSource(DataCampOfferContent.CardSource.newBuilder()
                    .setCardByMskuSearch(isPresent)
                    .setMarketSkuId(skuId)
                    .build()
                );
            return this;
        }

        public TestDataCampOfferBuilder approvedModel(int modelId, String modelName) {
            contentBindingApproved()
                .setMarketModelId(modelId)
                .setMarketModelName(modelName);
            return this;
        }

        public TestDataCampOfferBuilder approvedSku(
            long skuId,
            String skuName,
            DataCampOfferMapping.Mapping.MarketSkuType skuType
        ) {
            contentBindingApproved()
                .setMarketSkuId(skuId)
                .setMarketSkuName(skuName)
                .setMarketSkuType(skuType);
            return this;
        }

        public TestDataCampOfferBuilder mskuChangeTsFromMboc(Instant approvedSkuMappingChangeTs) {
            contentBindingApproved()
                .setMskuChangeTsFromMboc(Timestamp.newBuilder()
                    .setSeconds(approvedSkuMappingChangeTs.getEpochSecond())
                    .setNanos(approvedSkuMappingChangeTs.getNano())
                );
            return this;
        }

        private DataCampOfferMapping.Mapping.Builder contentBindingApproved() {
            DataCampOfferMapping.ContentBinding.Builder bindingBuilder =
                builder.getContentBuilder().getBindingBuilder();
            return bindingBuilder.getApprovedBuilder();
        }

        private DataCampOfferMapping.Mapping.Builder contentBindingPartner() {
            DataCampOfferMapping.ContentBinding.Builder bindingBuilder =
                builder.getContentBuilder().getBindingBuilder();
            return bindingBuilder.getPartnerBuilder();
        }

        private DataCampOfferMapping.Mapping.Builder contentBindingUcMapping() {
            DataCampOfferMapping.ContentBinding.Builder bindingBuilder =
                builder.getContentBuilder().getBindingBuilder();
            return bindingBuilder.getUcMappingBuilder();
        }

        private DataCampOfferMapping.Mapping.Builder contentBindingRuntimeUcMapping() {
            DataCampOfferMapping.ContentBinding.Builder bindingBuilder =
                builder.getContentBuilder().getBindingBuilder();
            return bindingBuilder.getRuntimeUcMappingBuilder();
        }

        public DataCampOffer.Offer build() {
            return builder.build();
        }

        public TestDataCampOfferBuilder status(DataCampContentStatus.ContentSystemStatus.Builder contentSystemStatus) {
            builder.getContentBuilder()
                .getStatusBuilder()
                .setContentSystemStatus(contentSystemStatus);
            return this;
        }

        public TestDataCampOfferBuilder modify(Consumer<TestDataCampOfferBuilder> modifier) {
            modifier.accept(this);
            return this;
        }
    }

    private static DataCampOfferMeta.StringListValue stringListValue(String... values) {
        return DataCampOfferMeta.StringListValue.newBuilder()
            .addAllValue(List.of(values))
            .build();
    }

    private static DataCampOfferMeta.StringValue stringValue(String value) {
        return DataCampOfferMeta.StringValue.newBuilder()
            .setValue(value)
            .build();
    }

    private BusinessSkuKey businessSkuKey(String shopSku, int businessId) {
        return new BusinessSkuKey(businessId, shopSku);
    }
}
