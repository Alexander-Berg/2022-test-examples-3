package ru.yandex.market.mboc.common.services.datacamp;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.PartnerCategoryOuterClass;
import NMarketIndexer.Common.Common;
import com.google.common.collect.ImmutableSet;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.datacamp.model.DataCampUnitedOffersEvent;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.datacamp.service.DatacampImportService;
import ru.yandex.market.mboc.common.datacamp.service.converter.DataCampConverterService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationOfferState;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationStatusType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationOffer;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.ClassificationResult;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.logbroker.events.ThrottlingLogbrokerEventPublisher;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.DefaultOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.MigrationModelRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.RemovedOfferRepository;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCache;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.migration.MigrationModelService;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.services.offers.processing.RemoveOfferService;
import ru.yandex.market.mboc.common.services.proto.AddProductInfoHelperService;
import ru.yandex.market.mboc.common.services.proto.AddProductInfoListener;
import ru.yandex.market.mboc.common.services.proto.datacamp.AdditionalData;
import ru.yandex.market.mboc.common.services.proto.datacamp.DatacampContext;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.RealConverter;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingServiceMock;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappings.ProviderProductInfoResponse.ErrorKind;
import ru.yandex.misc.test.Assert;

import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandlerDbTest.offer;
import static ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandlerDbTest.offerToProcess;
import static ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandlerDbTest.offerToStore;
import static ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandlerDbTest.offerWithNoIrData;
import static ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandlerDbTest.offerWithService;
import static ru.yandex.market.mboc.common.services.proto.AddProductInfoError.errorFromInfo;
import static ru.yandex.market.mboc.common.services.proto.AddProductInfoError.errorFromMessage;
import static ru.yandex.market.mboc.common.services.proto.AddProductInfoError.errorFromMessageWithoutRecoverable;

@Slf4j
@SuppressWarnings("checkstyle:magicnumber")
public class LogbrokerDatacampOfferMessageHandlerTest {
    private static final int BERU_BUSINESS_ID = 565853;
    private static final int REAL_SUPPLIER_ID = 44;
    private static final String REAL_SUPPLIER_EXT_ID = "real";
    private static final long SEED = 1599;
    protected static final int WHITE_SHOP_ID = 4242;
    private static final int BUSINESS_ID = 234523423;

    private LogbrokerDatacampOfferMessageHandler handler;
    private AddProductInfoHelperService addProductInfoHelperService;
    private SupplierConverterServiceMock supplierConverterService;
    private EnhancedRandom random;

    private List<DataCampUnitedOffer.UnitedOffer> offersSentToLB;
    private List<BusinessSkuKey> businessSkuKeysProcessedInMboc;
    private MboMappings.ProviderProductInfoRequest request;
    private DatacampContext datacampContext;
    private LogbrokerEventPublisher<DataCampUnitedOffersEvent> logbrokerEventPublisher;
    private OfferRepository offerRepository;
    private MskuRepository mskuRepository;
    private SupplierRepository supplierRepository;
    private RemovedOfferRepository removedOfferRepository;
    private RemoveOfferService removeOfferService;
    private MigrationService migrationService;
    private DatacampImportService datacampImportService;
    private AntiMappingRepository antiMappingRepository;
    private CategoryInfoRepository categoryInfoRepository;
    private CategoryInfoCache categoryInfoCache;
    private StorageKeyValueServiceMock storageKeyValueServiceMock;
    private MigrationModelRepository migrationModelRepository;
    private MigrationModelService migrationModelService;

    private TransactionHelper transactionHelper;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(SEED);
        supplierRepository = new SupplierRepositoryMock();
        supplierRepository.insertBatch(
            OfferTestUtils.simpleSupplier(),
            new Supplier().setId(WHITE_SHOP_ID)
                .setType(MbocSupplierType.MARKET_SHOP)
                .setBusinessId(BUSINESS_ID)
                .setDatacamp(true),
            new Supplier(REAL_SUPPLIER_ID, "Test Real Supplier")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId(REAL_SUPPLIER_EXT_ID),
            new Supplier(BERU_BUSINESS_ID, "Yandex.Market Biz")
                .setType(MbocSupplierType.BUSINESS),
            new Supplier(SupplierConverterServiceMock.BERU_ID, "Yandex.Market")
                .setBusinessId(BERU_BUSINESS_ID)
                .setType(MbocSupplierType.FIRST_PARTY)
                .setDatacamp(true),
            new Supplier(BUSINESS_ID, "biz")
                .setType(MbocSupplierType.BUSINESS)
        );
        storageKeyValueServiceMock = new StorageKeyValueServiceMock();
        CategoryCachingService categoryCachingService = mock(CategoryCachingService.class);
        addProductInfoHelperService = mock(AddProductInfoHelperService.class);
        offersSentToLB = new ArrayList<>();
        businessSkuKeysProcessedInMboc = new ArrayList<>();
        when(addProductInfoHelperService.addProductInfo(
            Mockito.any(MboMappings.ProviderProductInfoRequest.class),
            Mockito.any(DatacampContext.class),
            Mockito.any(AddProductInfoListener.class)
        ))
            .then(invocation -> {
                MboMappings.ProviderProductInfoRequest argument = invocation.getArgument(0);
                request = argument;
                datacampContext = invocation.getArgument(1);
                argument.getProviderProductInfoList().forEach(providerProductInfo ->
                    businessSkuKeysProcessedInMboc.add(
                        new BusinessSkuKey(providerProductInfo.getShopId(), providerProductInfo.getShopSkuId())));
                return MboMappings.ProviderProductInfoResponse.newBuilder().build();
            });
        logbrokerEventPublisher = mock(LogbrokerEventPublisher.class);
        Mockito.doAnswer(invocation -> {
            DataCampUnitedOffersEvent event = invocation.getArgument(0);
            DataCampUnitedOffer.UnitedOffersBatch offers = event.getPayload().getUnitedOffersList().get(0);
            for (DataCampUnitedOffer.UnitedOffer offer : offers.getOfferList()) {
                offersSentToLB.add(offer);
            }
            return null;
        }).when(logbrokerEventPublisher).publishEvent(Mockito.any());

        offerRepository = new OfferRepositoryMock();
        SupplierService supplierService = new SupplierService(supplierRepository);

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
            storageKeyValueServiceMock,
            true);

        mskuRepository = Mockito.mock(MskuRepository.class);
        removedOfferRepository = Mockito.mock(RemovedOfferRepository.class);
        datacampImportService = mock(DatacampImportService.class);
        antiMappingRepository = new AntiMappingRepositoryMock();
        TestUtils.mockMskuRepositoryFindTitles(mskuRepository);

        migrationService = Mockito.mock(MigrationService.class);
        transactionHelper = Mockito.mock(TransactionHelper.class);
        when(migrationService.isInMigrationCached(Mockito.anyInt())).thenReturn(false);

        removeOfferService = Mockito.spy(new RemoveOfferService(
            removedOfferRepository,
            offerRepository,
            datacampImportService,
            migrationService,
            transactionHelper,
            new DefaultOfferDestinationCalculator()
        ));
        categoryInfoRepository = new CategoryInfoRepositoryMock(new MboUsersRepositoryMock());
        categoryInfoCache = new CategoryInfoCacheImpl(categoryInfoRepository);

        migrationModelRepository = Mockito.mock(MigrationModelRepository.class);
        migrationModelService = new MigrationModelService(
            migrationModelRepository,
            mskuRepository
        );
        when(migrationModelRepository.findByIds(anyList())).thenReturn(List.of());
        handler = new LogbrokerDatacampOfferMessageHandler(
            addProductInfoHelperService,
            new ThrottlingLogbrokerEventPublisher<>(logbrokerEventPublisher),
            categoryCachingService,
            supplierService,
            dataCampIdentifiersService,
            dataCampConverterService,
            datacampImportService,
            offerRepository,
            mskuRepository,
            migrationService,
            removeOfferService,
            new GlobalVendorsCachingServiceMock(),
            new StorageKeyValueServiceMock(),
            antiMappingRepository,
            new ContextedOfferDestinationCalculator(categoryInfoCache, storageKeyValueServiceMock),
            migrationModelService,
            SupplierConverterServiceMock.BERU_BUSINESS_ID
        );

        OfferUtils.setBusinessIdsWithNewDsbsPipeline(null);
    }

    @Test
    public void whenAddProductInfoExceptionThenRetry() {
        disableRetryDelay();
        DataCampOffer.Offer offerToStore = offerToStore(BUSINESS_ID).build();

        when(addProductInfoHelperService.addProductInfo(
            Mockito.any(MboMappings.ProviderProductInfoRequest.class),
            Mockito.any(DatacampContext.class),
            Mockito.any(AddProductInfoListener.class)
        ))
            .thenThrow(new RuntimeException("first try failed"))
            .then(invocation -> {
                MboMappings.ProviderProductInfoRequest argument = invocation.getArgument(0);
                request = argument;
                datacampContext = invocation.getArgument(1);
                argument.getProviderProductInfoList().forEach(providerProductInfo ->
                    businessSkuKeysProcessedInMboc.add(
                        new BusinessSkuKey(providerProductInfo.getShopId(), providerProductInfo.getShopSkuId())));
                return MboMappings.ProviderProductInfoResponse.newBuilder().build();
            });

        handler.process(List.of(OfferGenerationHelper.toMessage(WHITE_SHOP_ID, offerToStore)));

        Mockito.verify(logbrokerEventPublisher, Mockito.never())
            .publishEvent(Mockito.any());

        Mockito.verify(addProductInfoHelperService, Mockito.times(2))
            .addProductInfo(
                Mockito.any(MboMappings.ProviderProductInfoRequest.class),
                Mockito.any(DatacampContext.class),
                Mockito.any(AddProductInfoListener.class)
            );

        Assertions.assertThat(offersSentToLB).isEmpty();
    }

    @Ignore("In MBO-35053, fix MBO-35056")
    @Test
    public void whenAddProductInfoExceptionThenRetryPerOfferFallback() {
        disableRetryDelay();
        String toThrowSsku = "to_throw";
        DataCampOfferIdentifiers.OfferIdentifiers identifiersToThrow =
            DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId(toThrowSsku)
                .setBusinessId(BUSINESS_ID)
                .build();
        DataCampOffer.Offer toThrow = offerToStore().setIdentifiers(identifiersToThrow).build();

        DataCampOfferIdentifiers.OfferIdentifiers identifiers1 =
            DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId("offer1")
                .setBusinessId(BUSINESS_ID)
                .build();
        DataCampOffer.Offer toSave1 = offerToStore().setIdentifiers(identifiers1).build();

        DataCampOfferIdentifiers.OfferIdentifiers identifiers2 =
            DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId("offer2")
                .setBusinessId(BUSINESS_ID)
                .build();
        DataCampOffer.Offer toSave2 = offerToStore().setIdentifiers(identifiers2).build();

        when(addProductInfoHelperService.addProductInfo(
            Mockito.any(MboMappings.ProviderProductInfoRequest.class),
            Mockito.any(DatacampContext.class),
            Mockito.any(AddProductInfoListener.class)
        ))
            .thenThrow(new RuntimeException("first try failed for all"))
            .then(invocation -> {
                MboMappings.ProviderProductInfoRequest argument = invocation.getArgument(0);
                request = argument;
                datacampContext = invocation.getArgument(1);
                MboMappings.ProviderProductInfoResponse.Builder builder =
                    MboMappings.ProviderProductInfoResponse.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK);
                argument.getProviderProductInfoList()
                    .forEach(info -> {
                        if (info.getShopSkuId().equals(toThrowSsku)) {
                            throw new RuntimeException("failed");
                        }
                    });
                argument.getProviderProductInfoList().stream()
                    .peek(info ->
                        businessSkuKeysProcessedInMboc.add(new BusinessSkuKey(info.getShopId(), info.getShopSkuId())))
                    .forEach(providerProductInfo ->
                        builder.addResults(MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                            .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)));
                return builder.build();
            });

        handler.process(List.of(OfferGenerationHelper.toMessage(WHITE_SHOP_ID,
            toSave1, toThrow, toSave2)));

        Assertions.assertThat(offersSentToLB).extracting(o -> o.getBasic().getIdentifiers().getOfferId())
            .containsExactly(toThrowSsku);
        Assertions.assertThat(businessSkuKeysProcessedInMboc).extracting(BusinessSkuKey::getShopSku)
            .containsExactlyInAnyOrder("offer1", "offer2");
    }

    @Test
    public void whenAddProductInfoErrorThenSendAllInLB() {
        DataCampOffer.Offer offerToProcess = offerToProcess(BUSINESS_ID).build();
        DataCampOffer.Offer offerToStore = offerToStore(BUSINESS_ID).build();

        Mockito.doAnswer(invocation -> MboMappings.ProviderProductInfoResponse.newBuilder()
                .addResults(MboMappings.ProviderProductInfoResponse
                    .ProductResult.newBuilder()
                    .setStatus(MboMappings.ProviderProductInfoResponse.Status.ERROR)
                    .build()).build())
            .when(addProductInfoHelperService).addProductInfo(Mockito.any(), Mockito.any(), Mockito.any());

        BusinessSkuKey offerToStoreKey = DataCampOfferUtil.extractExternalBusinessSkuKey(offerToStore);

        handler.processAs(Arrays.asList(OfferGenerationHelper.toMessage(WHITE_SHOP_ID, offerToStore),
                OfferGenerationHelper.toMessage(WHITE_SHOP_ID,
                    offerToProcess)),
            new ImportMessageHandlerContext(
                Map.of(offerToStoreKey, 6),
                ImportMessageHandlerContext.DataSourceType.DEFAULT
            ));

        Mockito.verify(logbrokerEventPublisher, Mockito.times(1))
            .publishEvent(Mockito.any());

        Assertions.assertThat(offersSentToLB).hasSize(2);
        Assertions.assertThat(offersSentToLB)
            .extracting(o -> DataCampOfferUtil.extractExternalBusinessSkuKey(o.getBasic()))
            .containsExactlyInAnyOrder(
                DataCampOfferUtil.extractExternalBusinessSkuKey(offerToProcess),
                DataCampOfferUtil.extractExternalBusinessSkuKey(offerToStore));
    }

    @Test
    public void testImportOfferWithNonRecovableError() {
        DataCampOffer.Offer offerToStoreWithBadSku =
            offerToStore()
                .setIdentifiers(
                    DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId("****")
                        .setBusinessId(BUSINESS_ID)
                ).build();
        DataCampOffer.Offer offerToStore = offerToStore(BUSINESS_ID).build();


        MboMappings.ProviderProductInfoResponse.ProductResult result = MboMappings.ProviderProductInfoResponse
            .ProductResult.newBuilder()
            .setStatus(MboMappings.ProviderProductInfoResponse.Status.ERROR)
            .build();
        MboMappings.ProviderProductInfoResponse stubMessage = MboMappings.ProviderProductInfoResponse.newBuilder()
            .addResults(result)
            .addResults(result)
            .build();
        Mockito.doAnswer(invocation -> {
                AddProductInfoListener argument = invocation.getArgument(2);
                argument.onError(0,
                    errorFromMessage(ErrorKind.WRONG_SHOP_ID, "error"));
                argument.onError(1,
                    errorFromMessageWithoutRecoverable(ErrorKind.WRONG_SHOP_SKU, "error"));
                return stubMessage;
            })
            .when(addProductInfoHelperService).addProductInfo(Mockito.any(), Mockito.any(), Mockito.any());

        handler.process(Arrays.asList(
            OfferGenerationHelper.toMessage(WHITE_SHOP_ID, offerToStore),
            OfferGenerationHelper.toMessage(WHITE_SHOP_ID, offerToStoreWithBadSku)));

        Mockito.verify(logbrokerEventPublisher, Mockito.times(1))
            .publishEvent(Mockito.any());

        DataCampOfferIdentifiers.OfferIdentifiers identifiers = offerToStore.getIdentifiers();
        Map<BusinessSkuKey, String> reimportOffer = Map.of(new BusinessSkuKey(
            identifiers.getBusinessId(),
            identifiers.getOfferId()
        ), "Ошибка при сохранении оффера test-offer");
        Mockito.verify(datacampImportService, Mockito.times(1))
            .markForImport(Mockito.eq(reimportOffer));

        Assertions.assertThat(offersSentToLB).hasSize(1);
        Assertions.assertThat(offersSentToLB)
            .extracting(o -> DataCampOfferUtil.extractExternalBusinessSkuKey(o.getBasic()))
            .containsExactlyInAnyOrder(
                DataCampOfferUtil.extractExternalBusinessSkuKey(offerToStoreWithBadSku));
        Assertions.assertThat(offerRepository.findAll()).isEmpty();
    }

    @Test
    public void whenAddProductInfoErrorSkipFreshError() {
        DataCampOffer.Offer offerToStore = offerToStore(BUSINESS_ID).build();

        Mockito.doAnswer(invocation -> MboMappings.ProviderProductInfoResponse.newBuilder()
                .addResults(MboMappings.ProviderProductInfoResponse
                    .ProductResult.newBuilder()
                    .setStatus(MboMappings.ProviderProductInfoResponse.Status.ERROR)
                    .build()).build())
            .when(addProductInfoHelperService).addProductInfo(Mockito.any(), Mockito.any(), Mockito.any());

        handler.process(Arrays.asList(OfferGenerationHelper.toMessage(WHITE_SHOP_ID, offerToStore)));

        Mockito.verify(logbrokerEventPublisher, Mockito.times(0))
            .publishEvent(Mockito.any());
    }

    @Test
    public void whenAddProductInfoErrorWithInfoThenSendAllInLB() {
        DataCampOffer.Offer offerToProcess = offerToProcess(BUSINESS_ID).build();
        DataCampOffer.Offer offerToStore = offerToStore(BUSINESS_ID).build();

        Mockito.doAnswer(invocation -> {
                MboMappings.ProviderProductInfoResponse.Builder response =
                    MboMappings.ProviderProductInfoResponse.newBuilder();
                MboMappings.ProviderProductInfoRequest request = invocation.getArgument(0);
                AddProductInfoListener addProductInfoListener = invocation.getArgument(2);

                request.getProviderProductInfoList()
                    .forEach(productInfo -> {
                        response.addResults(MboMappings.ProviderProductInfoResponse
                            .ProductResult.newBuilder()
                            .setStatus(MboMappings.ProviderProductInfoResponse.Status.ERROR));

                        addProductInfoListener.onError(0, errorFromInfo(ErrorKind.NO_REQUIRED_FIELDS,
                            MbocErrors.get().barcodeRequired(productInfo.getShopSkuId())));
                    });

                return response.build();
            })
            .when(addProductInfoHelperService).addProductInfo(Mockito.any(), Mockito.any(), Mockito.any());


        BusinessSkuKey offerToProcessKey = DataCampOfferUtil.extractExternalBusinessSkuKey(offerToProcess);
        BusinessSkuKey offerToStoreKey = DataCampOfferUtil.extractExternalBusinessSkuKey(offerToStore);

        handler.processAs(Arrays.asList(OfferGenerationHelper.toMessage(WHITE_SHOP_ID, offerToStore),
                OfferGenerationHelper.toMessage(WHITE_SHOP_ID,
                    offerToProcess)),
            new ImportMessageHandlerContext(
                Map.of(offerToStoreKey, 6),
                ImportMessageHandlerContext.DataSourceType.DEFAULT
            ));

        Mockito.verify(logbrokerEventPublisher, Mockito.times(1))
            .publishEvent(Mockito.any());

        Assertions.assertThat(offersSentToLB).hasSize(2);


        Assertions.assertThat(offersSentToLB)
            .extracting(
                o -> DataCampOfferUtil.extractExternalBusinessSkuKey(o.getBasic()),
                o -> o.getBasic().getResolution().getBySourceList().stream()
                    .flatMap(resolution -> resolution.getVerdictList().stream()
                        .flatMap(verdicts -> verdicts.getResultsList().stream()))
                    .flatMap(verdictResult -> verdictResult.getMessagesList().stream())
                    .map(DataCampExplanation.Explanation::getCode)
                    .collect(Collectors.toSet())
            )
            .containsExactlyInAnyOrder(
                tuple(offerToProcessKey, Collections.emptySet()),
                tuple(offerToStoreKey,
                    Set.of(MbocErrors.get().barcodeRequired(offerToStoreKey.getShopSku()).getErrorCode()))
            );
    }

    @Test
    public void shouldProcessOffersWithEnoughData() {
        DataCampOffer.Offer withMappings = OfferGenerationHelper.offerWithShopSku(BUSINESS_ID, random)
            .setContent(OfferGenerationHelper.offerContentWithBindingAndTs(1L, 2L, 3))
            .build();
        DataCampOffer.Offer withoutMappings = OfferGenerationHelper.offerWithShopSku(BUSINESS_ID, random)
            .setContent(OfferGenerationHelper.offerContentWithBindingAndTs(null, null, null))
            .build();
        DataCampOffer.Offer offerWithoutIdentifiers = DataCampOffer.Offer.newBuilder()
            .setContent(OfferGenerationHelper.offerContentWithBindingAndTs(1L, 2L, 3))
            .build();


        handler.process(Arrays.asList(
            OfferGenerationHelper.toMessage(WHITE_SHOP_ID, withMappings),
            OfferGenerationHelper.toMessage(WHITE_SHOP_ID, withoutMappings),
            OfferGenerationHelper.toMessage(WHITE_SHOP_ID, offerWithoutIdentifiers)));
        Mockito.verify(logbrokerEventPublisher, Mockito.times(1))
            .publishEvent(Mockito.any());
        Assertions.assertThat(offersSentToLB)
            .extracting(o -> DataCampOfferUtil.extractExternalBusinessSkuKey(o.getBasic()))
            .containsExactlyInAnyOrder(
                DataCampOfferUtil.extractExternalBusinessSkuKey(withoutMappings));
    }

    @Test
    public void shouldMigrateAnyWhiteOfferWhenExists() {
        var shopSkuToStore = "ssku";
        var shopSkuNotToStore = "just offer";

        var offerToStore = offer(offerToProcess(),
            offerBuilder -> {
                offerBuilder.getIdentifiersBuilder()
                    .setBusinessId(BUSINESS_ID)
                    .setOfferId(shopSkuToStore);
                return offerBuilder;
            },
            offer -> Map.of(WHITE_SHOP_ID, offer)
        );
        var offerNotToStore = offer(offerToProcess(),
            offerBuilder -> {
                offerBuilder.getIdentifiersBuilder()
                    .setBusinessId(BUSINESS_ID)
                    .setOfferId(shopSkuNotToStore);
                return offerBuilder;
            },
            offer -> Map.of(WHITE_SHOP_ID, offer)
        );

        var offer = OfferTestUtils.simpleOffer()
            .setShopSku(shopSkuToStore)
            .setBusinessId(WHITE_SHOP_ID);
        offerRepository.insertOffers(offer);

        List<String> storedShopSkus = new ArrayList<>();

        Mockito.doAnswer(invocation -> {
                MboMappings.ProviderProductInfoRequest request = invocation.getArgument(0);

                var results = request.getProviderProductInfoList().stream()
                    .map(p -> {
                        storedShopSkus.add(p.getShopSkuId());
                        return MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                            .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK).build();
                    })
                    .collect(Collectors.toList());

                return MboMappings.ProviderProductInfoResponse.newBuilder()
                    .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                    .setMessage("Saved!")
                    .addAllResults(results)
                    .build();
            })
            .when(addProductInfoHelperService).addProductInfo(Mockito.any(), Mockito.any(), Mockito.any());

        handler.process(List.of(offerToStore, offerNotToStore));

        // first saved
        Assertions.assertThat(storedShopSkus).containsExactly(shopSkuToStore);

        // second only sent to lb
        Assertions.assertThat(offersSentToLB)
            .extracting(o -> DataCampOfferUtil.extractExternalBusinessSkuKey(o.getBasic()))
            .containsExactlyInAnyOrder(new BusinessSkuKey(BUSINESS_ID, shopSkuNotToStore));
    }

    @Test
    public void shouldProcessLatestOfferIfDuplicatesArePresent() {
        DataCampOffer.Offer whiteOfferWithMappings1 = OfferGenerationHelper.offerWithShopSku(BUSINESS_ID, random)
            .setContent(OfferGenerationHelper.offerContentWithBindingAndTs(1L, null, null))
            .build();
        DataCampOffer.Offer whiteOfferWithMappings2 = whiteOfferWithMappings1.toBuilder()
            .setContent(OfferGenerationHelper.offerContentWithBindingAndTs(null, null, null))
            .build();

        handler.process(Arrays.asList(OfferGenerationHelper.toMessage(WHITE_SHOP_ID, whiteOfferWithMappings1),
            OfferGenerationHelper.toMessage(WHITE_SHOP_ID,
                whiteOfferWithMappings2)));
        Mockito.verify(logbrokerEventPublisher, Mockito.times(1))
            .publishEvent(Mockito.any());
        // processed only 1
        Assertions.assertThat(offersSentToLB)
            .extracting(o -> DataCampOfferUtil.extractExternalBusinessSkuKey(o.getBasic()))
            .containsExactlyInAnyOrder(DataCampOfferUtil.extractExternalBusinessSkuKey(whiteOfferWithMappings1));
    }

    @Test
    public void shouldStore1POffer() {
        String shopSku = random.nextObject(String.class);
        String externalSSKU = RealConverter.generateSSKU(REAL_SUPPLIER_EXT_ID, shopSku);

        supplierConverterService.addInternalToExternalMapping(
            new ShopSkuKey(REAL_SUPPLIER_ID, shopSku),
            new ShopSkuKey(SupplierConverterServiceMock.BERU_ID, externalSSKU)
        );

        var offer1PMessage = offer(offerToStore(),
            offerBuilder -> {
                offerBuilder.getIdentifiersBuilder()
                    .setBusinessId(BERU_BUSINESS_ID)
                    .setOfferId(externalSSKU);
                return offerBuilder;
            },
            basicOffer -> Map.of(SupplierConverterServiceMock.BERU_ID, basicOffer)
        );

        handler.process(Collections.singletonList(offer1PMessage));

        Assertions.assertThat(businessSkuKeysProcessedInMboc)
            .containsExactlyInAnyOrder(
                new BusinessSkuKey(REAL_SUPPLIER_ID, shopSku));
    }

    @Test
    public void shouldStoreIfVersionIsSame() {
        var shopSkuToStore = "ssku";
        long datacampVersion = 12312321L;

        var offerToStore = offer(offerToProcess(),
            offerBuilder -> {
                offerBuilder.getIdentifiersBuilder()
                    .setBusinessId(BUSINESS_ID)
                    .setOfferId(shopSkuToStore);
                offerBuilder.getStatusBuilder().getVersionBuilder().getActualContentVersionBuilder()
                    .setCounter(datacampVersion);
                return offerBuilder;
            },
            offer -> Map.of(WHITE_SHOP_ID, offer)
        );

        var offer = OfferTestUtils.simpleOffer()
            .setShopSku(shopSkuToStore)
            .setBusinessId(BUSINESS_ID)
            .setDataCampContentVersion(datacampVersion);
        offerRepository.insertOffers(offer);

        List<String> storedShopSkus = new ArrayList<>();

        Mockito.doAnswer(invocation -> {
                MboMappings.ProviderProductInfoRequest request = invocation.getArgument(0);

                var results = request.getProviderProductInfoList().stream()
                    .map(p -> {
                        storedShopSkus.add(p.getShopSkuId());
                        return MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                            .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK).build();
                    })
                    .collect(Collectors.toList());

                return MboMappings.ProviderProductInfoResponse.newBuilder()
                    .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                    .setMessage("Saved!")
                    .addAllResults(results)
                    .build();
            })
            .when(addProductInfoHelperService).addProductInfo(Mockito.any(), Mockito.any(), Mockito.any());

        handler.process(List.of(offerToStore));

        Assertions.assertThat(storedShopSkus).containsExactly(shopSkuToStore);
    }

    @Test
    public void shouldConvertDatacampOfferToRequestWithContext() {
        long ts = System.currentTimeMillis();
        DataCampOffer.Offer offer = DataCampOffer.Offer.newBuilder()
            .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                .setCreationTs(ts)
                .setModificationTs(ts)
                .build()
            )
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId("test-offer")
                .setBusinessId(BUSINESS_ID))
            .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                    .setBinaryPrice(Common.PriceExpression.newBuilder()
                        .setPrice(102300000)
                        .build())
                    .build())
                .build()
            )
            .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                .setVersion(DataCampOfferStatus.VersionStatus.newBuilder()
                    .setActualContentVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                        .setCounter(100500L)
                        .build())
                    .build())
                .build())
            .setPictures(OfferBuilder.pictures("pic1", "pic2", "pic3"))
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(10)
                        .setMarketModelId(11L)
                        .setMarketSkuId(12L))
                    .setUcMapping(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(21)
                        .setMarketCategoryName("category-name")
                        .setMarketModelId(22)
                        .setMarketModelName("model-name")
                        .setMarketSkuId(23)
                        .setMarketSkuName("sku-name")
                        .build()))
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
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
                                .setName("PRICE")
                                .setValue("21.1")
                                .build())
                            .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                .setName("OTHER_PARAM")
                                .setValue("hello there"))
                            .build())
                        .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                            .setName("shop-category-name")
                            .build()))
                )
                .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                    .setMarketSkuPublishedOnBlueMarket(true)
                    .setMarketSkuPublishedOnMarket(true)
                    .setVendorId(25)
                    .setVendorName("UC-Vendor-name")
                    .setIrData(DataCampOfferContent.EnrichedOfferSubset.newBuilder()
                        .setEnrichType(Market.UltraControllerServiceData.UltraController.EnrichedOffer.EnrichType
                            .ET_APPROVED_MODEL)
                        .setSkutchType(Market.UltraControllerServiceData.UltraController.EnrichedOffer.SkutchType
                            .SKUTCH_BY_MODEL_ID)
                        .setClassifierCategoryId(34)
                        .setClassifierConfidentTopPercision(0.1)
                        .setMatchedId(22)
                        .build())
                    .build()))
            .build();

        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(offer)
            .putService(WHITE_SHOP_ID, offer)
            .build();

        handler.process(Collections.singletonList(OfferGenerationHelper.toMessage(
            DataCampUnitedOffer.UnitedOffersBatch.newBuilder()
                .addOffer(unitedOffer)
                .build()
        )));

        Assertions.assertThat(request)
            .isEqualTo(MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(MboMappings.ProductUpdateRequestInfo.newBuilder()
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER)
                    .setUserLogin(LogbrokerDatacampOfferMessageHandler.LOGIN)
                    .setVerifyNoApprovedMapping(false)
                    .setVerifyNoSupplierMapping(false)
                    .build())
                .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                    .setShopId(BUSINESS_ID)
                    .setShopSkuId("test-offer")
                    .setTitle("Title")
                    .setVendor("Vendor")
                    .setVendorCode("VendorCode")
                    .addAllBarcode(Arrays.asList("Barcode1", "Barcode2"))
                    .setDescription("Description")
                    .addAllUrl(Collections.singleton("ololol.com"))
                    .setShopCategoryName("shop-category-name")
                    .setMarketCategoryId(10)
                    .setMarketModelId(11L)
                    .setMarketSkuId(12L)
                    .build())
                .build());
        Assertions.assertThat(datacampContext)
            .isEqualTo(DatacampContext.builder()
                .dcOffers(Map.of(new BusinessSkuKey(BUSINESS_ID, "test-offer"), offer))
                .processDataCampData(true)
                .enrichedOffers(Map.of(new BusinessSkuKey(BUSINESS_ID, "test-offer"),
                    UltraController.EnrichedOffer.newBuilder()
                        .setMarketSkuPublishedOnBlueMarket(true)
                        .setMarketSkuPublishedOnMarket(true)
                        .setCategoryId(21)
                        .setMarketCategoryName("category-name")
                        .setModelId(22)
                        .setMarketModelName("model-name")
                        .setMarketSkuId(23)
                        .setMarketSkuName("sku-name")
                        .setVendorId(25)
                        .setMarketVendorName("UC-Vendor-name")
                        .setEnrichType(UltraController.EnrichedOffer.EnrichType.APPROVED_MODEL)
                        .setSkutchType(SkuBDApi.SkutchType.SKUTCH_BY_MODEL_ID)
                        .setClassifierCategoryId(34)
                        .setClassifierConfidentTopPrecision(0.1)
                        .setMatchedId(22)
                        .build()))
                .suppliers(supplierRepository.findByIdsAsMap(List.of(WHITE_SHOP_ID, BUSINESS_ID)))
                .offersAdditionalData(Map.of(new BusinessSkuKey(BUSINESS_ID, "test-offer"),
                    AdditionalData.builder()
                        .extraShopFields(Map.of(
                            "PRICE", "21.1",
                            "OTHER_PARAM", "hello there",
                            ExcelHeaders.PRICE.getTitle(), "10.23"
                        ))
                        .isDataCampOffer(true)
                        .dataCampContentVersion(100500L)
                        .picUrls("pic1\npic2\npic3")
                        .sourcePicUrls("sourcePic0\nsourcePic1\nsourcePic2")
                        .supplierIds(ImmutableSet.of(WHITE_SHOP_ID))
                        .build()
                ))
                .build());
    }

    @Test
    public void shouldConvertDatacampOfferToRequestWithContextWithExistingOffer() {

        offerRepository.insertOffers(
            Offer.builder().businessId(BUSINESS_ID).shopSku("test-offer").build()
        );
        Offer offer1 = offerRepository.findAll().get(0);

        antiMappingRepository.insert(
            new AntiMapping().setOfferId(offer1.getId()).setNotModelId(1L)
        );

        long ts = System.currentTimeMillis();
        DataCampOffer.Offer offer = DataCampOffer.Offer.newBuilder()
            .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                .setCreationTs(ts)
                .setModificationTs(ts)
                .build()
            )
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setOfferId("test-offer")
                .setBusinessId(BUSINESS_ID))
            .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                    .setBinaryPrice(Common.PriceExpression.newBuilder()
                        .setPrice(102300000)
                        .build())
                    .build())
                .build()
            )
            .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                .setVersion(DataCampOfferStatus.VersionStatus.newBuilder()
                    .setActualContentVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                        .setCounter(100500L)
                        .build())
                    .build())
                .build())
            .setPictures(OfferBuilder.pictures("pic1", "pic2", "pic3"))
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(10)
                        .setMarketModelId(11L)
                        .setMarketSkuId(12L))
                    .setUcMapping(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId(21)
                        .setMarketCategoryName("category-name")
                        .setMarketModelId(22)
                        .setMarketModelName("model-name")
                        .setMarketSkuId(23)
                        .setMarketSkuName("sku-name")
                        .build()))
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
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
                                .setName("PRICE")
                                .setValue("21.1")
                                .build())
                            .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                .setName("OTHER_PARAM")
                                .setValue("hello there"))
                            .build())
                        .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                            .setName("shop-category-name")
                            .build()))
                )
                .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                    .setMarketSkuPublishedOnBlueMarket(true)
                    .setMarketSkuPublishedOnMarket(true)
                    .setVendorId(25)
                    .setVendorName("UC-Vendor-name")
                    .setIrData(DataCampOfferContent.EnrichedOfferSubset.newBuilder()
                        .setEnrichType(Market.UltraControllerServiceData.UltraController.EnrichedOffer.EnrichType
                            .ET_APPROVED_MODEL)
                        .setSkutchType(Market.UltraControllerServiceData.UltraController.EnrichedOffer.SkutchType
                            .SKUTCH_BY_MODEL_ID)
                        .setClassifierCategoryId(34)
                        .setClassifierConfidentTopPercision(0.1)
                        .setMatchedId(22)
                        .build())
                    .build()))
            .build();

        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(offer)
            .putService(WHITE_SHOP_ID, offer)
            .build();

        handler.process(Collections.singletonList(OfferGenerationHelper.toMessage(
            DataCampUnitedOffer.UnitedOffersBatch.newBuilder()
                .addOffer(unitedOffer)
                .build()
        )));

        Assertions.assertThat(request)
            .isEqualTo(MboMappings.ProviderProductInfoRequest.newBuilder()
                .setRequestInfo(MboMappings.ProductUpdateRequestInfo.newBuilder()
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER)
                    .setUserLogin(LogbrokerDatacampOfferMessageHandler.LOGIN)
                    .setVerifyNoApprovedMapping(false)
                    .setVerifyNoSupplierMapping(false)
                    .build())
                .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                    .setShopId(BUSINESS_ID)
                    .setShopSkuId("test-offer")
                    .setTitle("Title")
                    .setVendor("Vendor")
                    .setVendorCode("VendorCode")
                    .addAllBarcode(Arrays.asList("Barcode1", "Barcode2"))
                    .setDescription("Description")
                    .addAllUrl(Collections.singleton("ololol.com"))
                    .setShopCategoryName("shop-category-name")
                    .setMarketCategoryId(10)
                    .setMarketModelId(11L)
                    .setMarketSkuId(12L)
                    .build())
                .build());
        Assertions.assertThat(datacampContext)
            .isEqualTo(DatacampContext.builder()
                .dcOffers(Map.of(new BusinessSkuKey(BUSINESS_ID, "test-offer"), offer))
                .processDataCampData(true)
                .enrichedOffers(Map.of(new BusinessSkuKey(BUSINESS_ID, "test-offer"),
                    UltraController.EnrichedOffer.newBuilder()
                        .setMarketSkuPublishedOnBlueMarket(true)
                        .setMarketSkuPublishedOnMarket(true)
                        .setCategoryId(21)
                        .setMarketCategoryName("category-name")
                        .setModelId(22)
                        .setMarketModelName("model-name")
                        .setMarketSkuId(23)
                        .setMarketSkuName("sku-name")
                        .setVendorId(25)
                        .setMarketVendorName("UC-Vendor-name")
                        .setEnrichType(UltraController.EnrichedOffer.EnrichType.APPROVED_MODEL)
                        .setSkutchType(SkuBDApi.SkutchType.SKUTCH_BY_MODEL_ID)
                        .setClassifierCategoryId(34)
                        .setClassifierConfidentTopPrecision(0.1)
                        .setMatchedId(22)
                        .build()))
                .existingOffers(Map.of(new BusinessSkuKey(BUSINESS_ID, "test-offer"), offerRepository.findAll().get(0)))
                .suppliers(supplierRepository.findByIdsAsMap(List.of(WHITE_SHOP_ID, BUSINESS_ID)))
                .offersAdditionalData(Map.of(new BusinessSkuKey(BUSINESS_ID, "test-offer"),
                    AdditionalData.builder()
                        .extraShopFields(Map.of(
                            "PRICE", "21.1",
                            "OTHER_PARAM", "hello there",
                            ExcelHeaders.PRICE.getTitle(), "10.23"
                        ))
                        .isDataCampOffer(true)
                        .dataCampContentVersion(100500L)
                        .picUrls("pic1\npic2\npic3")
                        .sourcePicUrls("sourcePic0\nsourcePic1\nsourcePic2")
                        .supplierIds(ImmutableSet.of(WHITE_SHOP_ID))
                        .build()
                ))
                .build());
    }


    @Test
    public void shouldAddAllRemovedServiceOffersToRemoveMapFromDcImportExecutor() {
        int markedToRemoveServiceOfferId = WHITE_SHOP_ID;
        int removedInDcServiceOfferId = OfferTestUtils.TEST_SUPPLIER_ID;
        int regularServiceOfferId = 12345;

        var businessSkuKey1 = new BusinessSkuKey(99, "test-offer");

        sendMessageToProcessServiceOffersWith(
            ImportMessageHandlerContext.DataSourceType.DATA_CAMP_IMPORT_EXECUTOR_DATA,
            businessSkuKey1,
            markedToRemoveServiceOfferId,
            removedInDcServiceOfferId,
            regularServiceOfferId);

        assertEquals(1, datacampContext.getServiceOffersToRemove().size());

        final Set<Integer> serviceOffersToRemoveIds = datacampContext.getServiceOffersToRemove().get(businessSkuKey1);
        assertEquals(2, serviceOffersToRemoveIds.size());
        assertTrue(serviceOffersToRemoveIds.contains(markedToRemoveServiceOfferId));
        assertTrue(serviceOffersToRemoveIds.contains(removedInDcServiceOfferId));
        assertFalse(serviceOffersToRemoveIds.contains(regularServiceOfferId));
    }

    @Test
    public void shouldAddOnlyMarkedToRemoveServiceOffersToRemoveMapFromDefaultDataSource() {
        int markedToRemoveServiceOfferId = WHITE_SHOP_ID;
        int removedInDcServiceOfferId = OfferTestUtils.TEST_SUPPLIER_ID;
        int regularServiceOfferId = OfferTestUtils.BLUE_SUPPLIER_ID_1;

        var businessSkuKey1 = new BusinessSkuKey(99, "test-offer");

        sendMessageToProcessServiceOffersWith(
            ImportMessageHandlerContext.DataSourceType.DEFAULT,
            businessSkuKey1,
            markedToRemoveServiceOfferId,
            removedInDcServiceOfferId,
            regularServiceOfferId);

        assertEquals(1, datacampContext.getServiceOffersToRemove().size());

        final Set<Integer> serviceOffersToRemoveIds = datacampContext.getServiceOffersToRemove().get(businessSkuKey1);

        assertEquals(1, serviceOffersToRemoveIds.size());
        assertTrue(serviceOffersToRemoveIds.contains(markedToRemoveServiceOfferId));
        assertFalse(serviceOffersToRemoveIds.contains(removedInDcServiceOfferId));
        assertFalse(serviceOffersToRemoveIds.contains(regularServiceOfferId));
    }

    @Test
    public void shouldNotAddRemovedSOToRemoveMapOnMigration() {
        int markedToRemoveServiceOfferId = WHITE_SHOP_ID;
        int removedInDcServiceOfferId = OfferTestUtils.TEST_SUPPLIER_ID;
        int regularServiceOfferId = OfferTestUtils.BLUE_SUPPLIER_ID_1;

        var businessSkuKey1 = new BusinessSkuKey(99, "test-offer");

        when(migrationService.isInMigrationCached(businessSkuKey1.getBusinessId())).thenReturn(true);

        sendMessageToProcessServiceOffersWith(
            ImportMessageHandlerContext.DataSourceType.DATA_CAMP_IMPORT_EXECUTOR_DATA,
            businessSkuKey1,
            markedToRemoveServiceOfferId,
            removedInDcServiceOfferId,
            regularServiceOfferId);

        assertEquals(1, datacampContext.getServiceOffersToRemove().size());

        final Set<Integer> serviceOffersToRemoveIds = datacampContext.getServiceOffersToRemove().get(businessSkuKey1);

        assertEquals(1, serviceOffersToRemoveIds.size());
        assertTrue(serviceOffersToRemoveIds.contains(markedToRemoveServiceOfferId));
        assertFalse(serviceOffersToRemoveIds.contains(removedInDcServiceOfferId));
        assertFalse(serviceOffersToRemoveIds.contains(regularServiceOfferId));
    }

    @Test
    public void shouldFilterOutNotFoundBusiness() {
        int businessId = 1234543;
        Assert.assertTrue(supplierRepository.findByIds(List.of(businessId)).isEmpty());

        var offerToStore = offer(
            offerToProcess(businessId),
            Function.identity()
        );
        handler.process(List.of(offerToStore));

        Mockito.verify(addProductInfoHelperService, Mockito.never())
            .addProductInfo(Mockito.any(), Mockito.any(), Mockito.any());
    }

    private void sendMessageToProcessServiceOffersWith(ImportMessageHandlerContext.DataSourceType dataSourceType,
                                                       BusinessSkuKey businessSkuKey,
                                                       int markedToRemoveServiceOfferId,
                                                       int removedInDcServiceOfferId,
                                                       int regularServiceOfferId) {
        final Supplier supplier1 = OfferTestUtils.simpleSupplier()
            .setId(businessSkuKey.getBusinessId());
        final Supplier supplier2 = OfferTestUtils.simpleSupplier()
            .setId(markedToRemoveServiceOfferId);
        final Supplier supplier3 = OfferTestUtils.simpleSupplier()
            .setId(removedInDcServiceOfferId);
        final Supplier supplier4 = OfferTestUtils.simpleSupplier()
            .setId(regularServiceOfferId);

        var offer = OfferTestUtils.simpleOffer()
            .setId(123)
            .setBusinessId(businessSkuKey.getBusinessId())
            .setShopSku(businessSkuKey.getShopSku())
            .addNewServiceOfferIfNotExistsForTests(List.of(supplier2, supplier3, supplier4));

        supplierRepository.insertBatch(List.of(supplier1, supplier2, supplier3, supplier4));
        offerRepository.insertOffers(offer);

        List<Offer> offers = offerRepository.findAll();
        Assertions.assertThat(offers).hasSize(1);
        Offer preProcessedOffer = offers.get(0);
        var serviceOffers = preProcessedOffer.getServiceOffers();
        Assertions.assertThat(serviceOffers).extracting(Offer.ServiceOffer::getSupplierId)
            .containsExactlyInAnyOrder(markedToRemoveServiceOfferId, removedInDcServiceOfferId, regularServiceOfferId);

        var removeOfferMessage = offerToStore();
        var removeStatus = removeOfferMessage.getStatusBuilder()
            .setRemoved(DataCampOfferMeta.Flag.newBuilder().setFlag(true).build());
        removeOfferMessage.setStatus(removeStatus);

        var regularOfferMessage = offerToProcess();
        var regularStatus = regularOfferMessage.getStatusBuilder()
            .setRemoved(DataCampOfferMeta.Flag.newBuilder().setFlag(false).build());
        regularOfferMessage.setStatus(regularStatus);

        var datacampMessage = offer(offerToStore(),
            Function.identity(),
            basicOffer -> Map.of(
                markedToRemoveServiceOfferId, removeOfferMessage.build(),
                regularServiceOfferId, regularOfferMessage.build()
            )
        );
        handler.processAs(
            List.of(datacampMessage),
            new ImportMessageHandlerContext(Collections.emptyMap(), dataSourceType)
        );

    }


    @Test
    public void shouldCorrectSplitOffers() {
        DataCampOffer.Offer offerToProcess = offerToProcess(BUSINESS_ID).build();
        DataCampOffer.Offer offerToStore = offerToStore(BUSINESS_ID).build();

        handler.process(Arrays.asList(OfferGenerationHelper.toMessage(WHITE_SHOP_ID, offerToStore),
            OfferGenerationHelper.toMessage(WHITE_SHOP_ID,
                offerToProcess)));

        Mockito.verify(logbrokerEventPublisher, Mockito.times(1))
            .publishEvent(Mockito.any());
        Mockito.verify(addProductInfoHelperService, Mockito.times(1))
            .addProductInfo(Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertThat(offersSentToLB)
            .extracting(o -> DataCampOfferUtil.extractExternalBusinessSkuKey(o.getBasic()))
            .containsExactlyInAnyOrder(DataCampOfferUtil.extractExternalBusinessSkuKey(offerToProcess));
        Assertions.assertThat(businessSkuKeysProcessedInMboc).containsExactlyInAnyOrder(
            DataCampOfferUtil.extractExternalBusinessSkuKey(offerToStore));
    }

    @Test
    public void offerShouldBeStored() {
        var offer = offerToProcess()
            .setContent(offerToProcess().getContentBuilder().setMarket(DataCampOfferContent.MarketContent.newBuilder()
                .setIrData(DataCampOfferContent.EnrichedOfferSubset.newBuilder().build())
                .build()))
            .build();
        var offerWithContent = offerToStore().build();
        var blueSupplier = new Supplier();
        blueSupplier.setType(MbocSupplierType.THIRD_PARTY);
        var whiteSupplier = new Supplier();
        whiteSupplier.setType(MbocSupplierType.BUSINESS);
        assertTrue(
            "Should return true for blue supplier, no mapping, no content",
            LogbrokerDatacampOfferMessageHandler.offerShouldBeStored(
                DataCampOfferUtil.extractExternalBusinessSkuKey(offer),
                offer,
                Collections.singletonList(blueSupplier)
            )
        );
        assertFalse(
            "Should return false for white supplier, no mapping, no content",
            LogbrokerDatacampOfferMessageHandler.offerShouldBeStored(
                DataCampOfferUtil.extractExternalBusinessSkuKey(offer),
                offer,
                Collections.singletonList(whiteSupplier)
            )
        );
        assertTrue(
            "Should return true for white supplier, with mapping",
            LogbrokerDatacampOfferMessageHandler.offerShouldBeStored(
                DataCampOfferUtil.extractExternalBusinessSkuKey(offer),
                offerWithContent,
                Collections.singletonList(whiteSupplier)
            )
        );
    }

    @Test
    public void testGetSuppliersToProcess() {
        var supplier1 = new Supplier().setId(1).setDatacamp(true).setBusinessId(BUSINESS_ID);
        var supplier2 = new Supplier().setId(2).setDatacamp(true).setBusinessId(BUSINESS_ID);
        var supplier4 = new Supplier().setId(4).setDatacamp(true);
        var offer12 = offerWithService(offerToStore(BUSINESS_ID),
            basicOffer -> Map.of(1, offerToStore().build(),
                2, offerToStore().build())
        );
        Assert.equals(
            Collections.emptyList(),
            handler.getSuppliersToProcess(Collections.emptyMap(), offer12)
        );

        var offer13 = offerWithService(offerToStore(BUSINESS_ID),
            basicOffer -> Map.of(1, offerToStore().build(),
                3, offerToStore().build())
        );
        Assert.equals(
            List.of(supplier1),
            handler.getSuppliersToProcess(
                Map.of(1, supplier1, 2, supplier2),
                offer13
            )
        );

        var offer4 = offerWithService(offerToStore(BUSINESS_ID),
            basicOffer -> Map.of(1, offerToStore().build(),
                4, offerToStore().build())
        );
        Assert.equals(
            List.of(supplier1),
            handler.getSuppliersToProcess(
                Map.of(1, supplier1, 4, supplier4),
                offer4
            )
        );
    }

    @Test
    public void shouldFinishMigrationOnExpiredMigrationWithNoEnrichedOffer() {
        var dcOffer = offerWithNoIrData(BUSINESS_ID).build();

        BusinessSkuKey businessSkuKey = DataCampOfferUtil.extractExternalBusinessSkuKey(dcOffer);
        var offer1 = OfferTestUtils.simpleOkOffer(new Supplier().setId(BUSINESS_ID))
            .setBusinessId(dcOffer.getIdentifiers().getBusinessId())
            .setShopSku(dcOffer.getIdentifiers().getOfferId());
        var offer2 = OfferTestUtils.simpleOkOffer(new Supplier().setId(BUSINESS_ID))
            .setBusinessId(dcOffer.getIdentifiers().getBusinessId())
            .setShopSku("Test shop sku");
        offerRepository.insertOffers(offer1, offer2);
        var migrationStatus = new MigrationStatus()
            .setId(1L)
            .setCreatedTs(Instant.now().minus(13L, ChronoUnit.HOURS))
            .setMigrationStatus(MigrationStatusType.ACTIVE)
            .setSupplierId(offer1.getBusinessId())
            .setTargetBusinessId(offer1.getBusinessId())
            .setSourceBusinessId(offer2.getBusinessId());
        var migrationOffer = new MigrationOffer()
            .setId(2L)
            .setMigrationId(1L)
            .setState(MigrationOfferState.NEW)
            .setShopSku(offer1.getShopSku());

        when(migrationService.getMigrationsForReceivingByTarget(anySet()))
            .thenReturn(Map.of(migrationStatus.getTargetBusinessId(), migrationStatus));
        when(migrationService.findMigrationOffersByState(migrationStatus, MigrationOfferState.NEW))
            .thenReturn(List.of(migrationOffer));

        handler.processAs(Arrays.asList(OfferGenerationHelper.toMessage(WHITE_SHOP_ID, dcOffer),
                OfferGenerationHelper.toMessage(WHITE_SHOP_ID,
                    dcOffer)),
            new ImportMessageHandlerContext(
                Map.of(businessSkuKey, 6),
                ImportMessageHandlerContext.DataSourceType.DEFAULT
            ));

        var resultOffer = offerRepository.findOfferByBusinessSkuKey(offer1.getBusinessSkuKey());
        var resultMos = migrationOffer.setState(MigrationOfferState.WARNING);

        assertFalse(resultOffer.getServiceOffer(migrationStatus.getSupplierId()).isPresent());
        verify(migrationService, times(1)).saveMigrationOffers(List.of(resultMos));
    }

    @SneakyThrows
    private void disableRetryDelay() {
        Field field = LogbrokerDatacampOfferMessageHandler.class.getDeclaredField("MAX_RETRY_DELAY_MS");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(LogbrokerDatacampOfferMessageHandler.class, 0);
    }
}
