package ru.yandex.market.mboc.app.offers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.lightmapper.exceptions.SqlConcurrentModificationException;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.app.offers.ImportFileService.ImportResult;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationCounterService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.MasterDataAsJsonDTO;
import ru.yandex.market.mboc.common.masterdata.parsing.FieldParseUtil;
import ru.yandex.market.mboc.common.masterdata.parsing.ImportedOfferToMasterDataConverter;
import ru.yandex.market.mboc.common.masterdata.parsing.ImportedOfferToMasterDataConverter.MasterDataConvertResult;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataParsingConfig;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataParsingConfigProvider;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.TimeInUnitsConverter;
import ru.yandex.market.mboc.common.masterdata.services.category.MboTimeUnitAliasesService;
import ru.yandex.market.mboc.common.offers.DefaultOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.ExcelS3ServiceMock;
import ru.yandex.market.mboc.common.offers.OfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.MasterDataRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCache;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.converter.ErrorAtLine;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.enrichment.OffersEnrichmentService;
import ru.yandex.market.mboc.common.services.offers.enrichment.TransformImportOffersService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.Line;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.mboc.common.vendor.models.CachedGlobalVendor;
import ru.yandex.market.mdm.http.MdmCommon;
import ru.yandex.misc.test.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.market.mboc.app.offers.ImportFileService.ImportSettings;
import static ru.yandex.market.mboc.app.offers.ImportFileService.RequestSource.INTERNAL;
import static ru.yandex.market.mboc.app.offers.ImportFileService.SavePolicy.ALL_OR_NOTHING;
import static ru.yandex.market.mboc.app.offers.ImportFileService.SavePolicy.DRY_RUN;
import static ru.yandex.market.mboc.app.offers.ImportFileService.SavePolicy.PARTIAL;
import static ru.yandex.market.mboc.app.offers.ImportFileService.SavePolicy.PARTIAL_BATCH;
import static ru.yandex.market.mboc.common.masterdata.parsing.MasterDataParsingConfig.CUSTOMS_COMMODITY_CODE_REQUIRED_KEY;

/**
 * @author yuramalinov
 * @created 06.09.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ImportExcelServiceImportTest {
    private static final Consumer<String> NULL_CONSUMER = s -> {
    };
    private static final int SEED = 1987365;
    private static final int SUPPLIER_ID_1P = 3;
    private static final int SUPPLIER_ID_DATACAMP = 4;
    private static final int SUPPLIER_ID_NO_MDM = 13451;
    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();
    private final OfferDestinationCalculator offerDestinationCalculator = new DefaultOfferDestinationCalculator();
    private OfferRepositoryMock offerRepository;
    private MasterDataRepositoryMock masterDataRepositoryMock;
    private OffersEnrichmentService offersEnrichmentService;
    private ImportedOfferToMasterDataConverter offerToMasterDataConverter;
    private ImportExcelService importExcelService;
    private ImportOffersProcessService importOffersProcessService;
    private ModelStorageCachingServiceMock modelStorageCachingService;
    private CategoryCachingServiceMock categoryCachingService;
    private CategoryInfoCache categoryInfoCache;
    private MboTimeUnitAliasesService mboTimeUnitAliasesService;
    private SupplierRepositoryMock supplierRepository;
    private NeedContentStatusService needContentStatusService;
    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;
    @Mock
    private ApplySettingsService applySettingsService;
    private EnhancedRandom defaultRandom;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;
    private MboUsersRepositoryMock mboUsersRepository;
    private CategoryInfoRepositoryMock categoryInfoRepositoryMock;
    private GlobalVendorsCachingService globalVendorsCachingService;
    private SupplierService supplierService;
    private StorageKeyValueService storageKeyValueService;

    @Before
    public void setUp() {
        defaultRandom = TestDataUtils.defaultRandom(SEED);
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        storageKeyValueService = new StorageKeyValueServiceMock();
        storageKeyValueService.putValue("is1pDatacampProcessingEnabled", false);
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);

        offerRepository = Mockito.spy(new OfferRepositoryMock());
        masterDataRepositoryMock = new MasterDataRepositoryMock();
        supplierRepository = new SupplierRepositoryMock();
        categoryCachingService = new CategoryCachingServiceMock().enableAuto();
        Category category = new Category().setCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setHasKnowledge(true)
            .setAcceptGoodContent(true)
            .setAcceptPartnerSkus(true);
        categoryInfoCache = new CategoryInfoCacheImpl(categoryInfoRepositoryMock);
        categoryCachingService.addCategory(category);
        OffersToExcelFileConverterConfig config =
            new OffersToExcelFileConverterConfig(categoryCachingService);

        mboTimeUnitAliasesService = Mockito.mock(MboTimeUnitAliasesService.class);
        MasterDataParsingConfigProvider masterDataParsingConfig = new MasterDataParsingConfig(
            mboTimeUnitAliasesService, storageKeyValueService);
        offerToMasterDataConverter = Mockito.spy(
            new ImportedOfferToMasterDataConverter(masterDataParsingConfig, masterDataHelperService)
        );

        supplierService = new SupplierService(supplierRepository);
        needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        mboUsersRepository = new MboUsersRepositoryMock();
        categoryInfoRepositoryMock = new CategoryInfoRepositoryMock(mboUsersRepository);
        categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        LegacyOfferMappingActionService legacyOfferMappingActionService = new LegacyOfferMappingActionService(
            needContentStatusService, Mockito.mock(OfferCategoryRestrictionCalculator.class),
            offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        modelStorageCachingService = new ModelStorageCachingServiceMock();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingService, null,
            supplierRepository);
        var antiMappingRepositoryMock = new AntiMappingRepositoryMock();
        var offersProcessingStatusService = new OffersProcessingStatusService(null, needContentStatusService,
            supplierService, categoryKnowledgeService, retrieveMappingSkuTypeService, offerMappingActionService,
            categoryInfoRepositoryMock, antiMappingRepositoryMock, offerDestinationCalculator,
            new StorageKeyValueServiceMock(), new FastSkuMappingsService(needContentStatusService), false, false, 3,
            new CategoryInfoCacheImpl(categoryInfoRepositoryMock));
        offersEnrichmentService = Mockito.spy(new OffersEnrichmentService(null, null,
            offerMappingActionService, supplierService,
            categoryKnowledgeService, Mockito.mock(HonestMarkClassificationService.class),
            Mockito.mock(HonestMarkClassificationCounterService.class),
            Mockito.mock(BooksService.class), offerDestinationCalculator, categoryInfoCache));

        TransformImportOffersService transformImportOffersService =
            new TransformImportOffersService(offerMappingActionService, offersProcessingStatusService,
                offerDestinationCalculator);

        globalVendorsCachingService = Mockito.mock(GlobalVendorsCachingService.class);
        CachedGlobalVendor globalVendor = new CachedGlobalVendor();
        globalVendor.setRequireGtinBarcodes(true);
        Mockito.when(globalVendorsCachingService.getVendor(Mockito.anyLong())).thenReturn(Optional.of(globalVendor));

        importOffersProcessService = new ImportOffersProcessService(
            offerRepository,
            masterDataRepositoryMock,
            masterDataHelperService,
            offersEnrichmentService,
            TransactionHelper.MOCK,
            offerToMasterDataConverter,
            supplierRepository,
            applySettingsService,
            offersProcessingStatusService,
            transformImportOffersService,
            globalVendorsCachingService,
            true,
            storageKeyValueService);

        importExcelService = new ImportExcelService(
            config.importedExcelFileConverter(
                modelStorageCachingService,
                mboTimeUnitAliasesService,
                supplierRepository,
                storageKeyValueService
            ),
            new ExcelS3ServiceMock(),
            importOffersProcessService
        );

        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        Supplier thirdPartySupplier = OfferTestUtils.simpleSupplier();
        thirdPartySupplier.setId(SUPPLIER_ID_1P);
        thirdPartySupplier.setType(MbocSupplierType.FIRST_PARTY);
        supplierRepository.insert(thirdPartySupplier);
        supplierRepository.insert(OfferTestUtils.fmcgSupplier());
        supplierRepository.insert(OfferTestUtils.simpleSupplier().setId(SUPPLIER_ID_NO_MDM).setDisableMdm(true));
        Supplier datacampSupplier = OfferTestUtils.simpleSupplier();
        datacampSupplier.setDatacamp(true);
        datacampSupplier.setId(SUPPLIER_ID_DATACAMP);
        supplierRepository.insert(datacampSupplier);
        doAnswer(i -> null).when(offersEnrichmentService).enrichOffers(anyList(), any());

        storageKeyValueService.putValue(CUSTOMS_COMMODITY_CODE_REQUIRED_KEY, true);
    }

    @Test
    public void testBasicImportValidation() {
        Offer offer = OfferTestUtils.simpleOffer();
        offerRepository.insertOffer(offer);

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства", "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "https://some.site.ru/123")
            .addLine("1234567890", "invalid+sku_ !!", "title2", "category2", "Китай", "http://other.site.com/tovar/vl2mv")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getOffers()).hasSize(2);

        ImportFileService.OfferResult result0 = result.getOffers().get(0);
        assertEquals(0, result0.getLineIndex());
        assertEquals("changed-title", result0.getOffer().getTitle());
        assertFalse("First result must not be new", result0.isNew());

        ImportFileService.OfferResult result1 = result.getOffers().get(1);
        assertEquals(1, result1.getLineIndex());
        assertEquals("title2", result1.getOffer().getTitle());
        assertTrue(result1.isNew());

        // No new offers added
        assertEquals(1, offerRepository.getCount());
        // Title isn't changed: ALL_OR_NOTHING
        assertEquals("title", offerRepository.getOfferById(offer.getId()).getTitle());
    }

    @Test
    public void testSkuLengthImportValidation() {
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства", "Ссылка на страницу модели")
            .addLine("1234567890", "short-sku",
                "title", "category", "Китай", "https://some.site.ru/234")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), SUPPLIER_ID_1P, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
        assertThat(result.getOffers()).hasSize(1);

        excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства", "Ссылка на страницу модели")
            .addLine("1234567890", "long-1p-sku-123456789012345678901234567890123456789012345678901234567890",
                "title", "category", "Китай", "https://some.site.ru/234")
            .build();

        result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), SUPPLIER_ID_1P, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getOffers()).hasSize(1);

        excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства", "Ссылка на " +
                "страницу модели")
            .addLine("1234567890", "short-3p-sku-123456789012345678901234567890123456789012345678901234567890",
                "title", "category", "Китай", "https://some.site.ru/234")
            .build();

        result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
        assertThat(result.getOffers()).hasSize(1);

        excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства", "Ссылка на " +
                "страницу модели")
            .addLine("1234567890", "long-3p-sku-123456789012345678901234567890123456789012345678901234567890",
                "title", "category", "Китай", "https://some.site.ru/234")
            .build();

        result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
        assertThat(result.getOffers()).hasSize(1);
    }

    @Test
    public void testCustomCommodityCodeIsRequiredValidation() {
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства", "Ссылка на " +
                "страницу модели")
            .addLine("1234567890", "short-sku", "title", "category", "Китай", "https://some.site.ru/234")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), SUPPLIER_ID_1P, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
        assertThat(result.getOffers()).hasSize(1);

        excelFile = ExcelFile.Builder
            .withHeaders("sku", "название товара", "категория", "Страна производства", "Ссылка на страницу модели")
            .addLine("short-sku", "title", "category", "Китай", "https://some.site.ru/234")
            .build();

        result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), SUPPLIER_ID_1P, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getOffers()).hasSize(1);

        excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства", "Ссылка на страницу модели")
            .addLine("1234567890", "short-sku", "title", "category", "Китай", "https://some.site.ru/234")
            .build();

        result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
        assertThat(result.getOffers()).hasSize(1);
    }

    @Test
    public void testAllOrNothingWithErrorsDoesntInsertNorUpdate() {
        Offer offer = OfferTestUtils.simpleOffer();
        offerRepository.insertOffer(offer);

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория")
            .addLine("1234567890", "shop-sku", "changed-title", "category")
            .addLine("1234567890", "invalid+sku_ !!", "title2", "category2")
            .build();

        importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        // Second isn't inserted
        assertEquals(1, offerRepository.getCount());
        // Title isn't changed: new ImportSettings(ALL_OR_NOTHING)
        assertEquals("title", offerRepository.getOfferById(offer.getId()).getTitle());
    }

    @Test
    public void testAllOrNothingSavesWhenNoErrors() {
        Offer offer = OfferTestUtils.simpleOffer();
        offerRepository.insertOffer(offer);

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства", "Ссылка на " +
                "страницу модели")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "https://some.site.ru/123")
            .addLine("1234567890", "good-new-sku", "title2", "category2", "Китай", "http://other.site.com/tovar/vl2mv")
            .build();

        importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        // Second is inserted
        assertEquals(2, offerRepository.getCount());
        // Title is changed
        var changedOffer = offerRepository.getOfferById(offer.getId());
        assertEquals("changed-title", changedOffer.getTitle());
        assertThat(changedOffer.isDataCampOffer()).isFalse();
        assertThat(offerRepository.findOffers(new OffersFilter().setOrderBy(OffersFilter.Field.ID)))
            .extracting(Offer::getTitle)
            .containsExactly("changed-title", "title2");
        assertThat(masterDataRepositoryMock.findAll()).hasSize(2);
        assertThat(storageKeyValueService.getBool("is1pDatacampProcessingEnabled", false)).isFalse();
    }

    @Test
    public void testThatWhenDatacampProcessingFlagEnabledThenFlagIsSet() {
        storageKeyValueService.putValue("is1pDatacampProcessingEnabled", true);
        assertThat(storageKeyValueService.getBool("is1pDatacampProcessingEnabled", false)).isTrue();
        Offer offer = OfferTestUtils.firstPartyOffer();
        Supplier realSupplier = OfferTestUtils.realSupplier();
        supplierRepository.insert(realSupplier);

        offerRepository.insertOffer(offer);

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства", "Ссылка на " +
                "страницу модели")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "https://some.site.ru/123")
            .build();

        importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.REAL_SUPPLIER_ID, "test", NULL_CONSUMER);

        // Second is inserted
        assertEquals(1, offerRepository.getCount());
        // Title is changed
        var changedOffer = offerRepository.getOfferById(offer.getId());
        assertThat(changedOffer.isDataCampOffer()).isTrue();

        assertThat(masterDataRepositoryMock.findAll()).hasSize(1);
    }

    @Test
    public void testThatWhenDatacampProcessingFlagEnabledThenFlagIsNotSet() {
        storageKeyValueService.putValue("is1pDatacampProcessingEnabled", false);
        assertThat(storageKeyValueService.getBool("is1pDatacampProcessingEnabled", false)).isFalse();
        Offer offer = OfferTestUtils.firstPartyOffer();
        Supplier realSupplier = OfferTestUtils.realSupplier();
        supplierRepository.insert(realSupplier);

        offerRepository.insertOffer(offer);

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства", "Ссылка на " +
                "страницу модели")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "https://some.site.ru/123")
            .build();

        importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.REAL_SUPPLIER_ID, "test", NULL_CONSUMER);

        // Second is inserted
        assertEquals(1, offerRepository.getCount());
        // Title is changed
        var changedOffer = offerRepository.getOfferById(offer.getId());
        assertThat(changedOffer.isDataCampOffer()).isFalse();

        assertThat(masterDataRepositoryMock.findAll()).hasSize(1);
    }

    @Test
    public void testNoneDoesntSaveEvenInSuccess() {
        Offer offer = OfferTestUtils.simpleOffer();
        offerRepository.insertOffer(offer);

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства", "страница модели")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "https://some.site.ru/123")
            .addLine("1234567890", "good-new-sku", "title2", "category2", "Китай", "https://some.site.ru/123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(DRY_RUN), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isEmpty();

        // Second isn't inserted
        assertEquals(1, offerRepository.getCount());
        // Title isn't changed: DRY_RUN
        assertEquals("title", offerRepository.getOfferById(offer.getId()).getTitle());
        assertThat(masterDataRepositoryMock.findAll()).isEmpty();
    }

    @Test
    public void testPartialDoesTheJobForThoseSuccessful() {
        Offer offer1 = OfferTestUtils.simpleOffer();
        Offer offer2 = OfferTestUtils.simpleOffer().setShopSku("shop-sku-2");
        offerRepository.insertOffers(Arrays.asList(offer1, offer2));

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства", "Страница модели")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "https://some.site.ru/123")
            .addLine("1234567890", "shop-sku-2", "", "changed-category", "Китай", "https://some.site.ru/123") // NOTE empty title
            .addLine("1234567890", "invalid+sku_ !!", "title2", "category2", "Китай", "https://some.site.ru/123")
            .addLine("1234567890", "valid-new-sku", "title3", "category3", "Китай", "https://some.site.ru/123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors()).extracting(ErrorAtLine::getLineIndex).containsExactly(1, 2);

        assertThat(result.getOffers()).extracting(ImportFileService.OfferResult::getLineIndex)
            .containsExactly(0, 1, 2, 3);

        assertThat(result.getOffers()).extracting(ImportFileService.OfferResult::isNew)
            .containsExactly(false, false, true, true);

        // Invalid isn't inserted
        assertThat(offerRepository.findAll()).extracting(Offer::getShopSku)
            .containsExactlyInAnyOrder("shop-sku", "shop-sku-2", "valid-new-sku");
        assertThat(offerRepository.getOfferById(offer1.getId()).getTitle()).isEqualTo("changed-title");
        assertThat(offerRepository.getOfferById(offer2.getId()).getTitle()).isEqualTo(OfferTestUtils.DEFAULT_TITLE);
        assertThat(offerRepository.getOfferById(offer2.getId()).getShopCategoryName())
            .isEqualTo(OfferTestUtils.DEFAULT_SHOP_CATEGORY_NAME);
        assertThat(offerRepository.findAll()).extracting(Offer::getShopSku)
            .containsExactlyInAnyOrder("shop-sku", "shop-sku-2", "valid-new-sku");
        assertThat(masterDataRepositoryMock.findAll()).hasSize(2);

    }

    @Test
    public void testMskusAreCheckedAndFilledIn() {
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "sku маркета", "Страна производства",
                "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku-1", "title1", "category", "just-wrong-msku", "Китай", "https://some.site.ru/123")
            // not published
            .addLine("1234567890", "shop-sku-2", "title2", "changed-category", "2", "Китай", "https://some.site.ru/123")
            // not sku
            .addLine("1234567890", "shop-sku-3", "title3", "category2", "3", "Китай", "https://some.site.ru/123")
            .addLine("1234567890", "shop-sku-4", "title4", "category3", "4", "Китай", "https://some.site.ru/123")
            .build();

        modelStorageCachingService
            .addModel(new Model().setId(2).setCategoryId(2).setSkuModel(true).setPublishedOnBlueMarket(false))
            .addModel(new Model().setId(3)
                .setCategoryId(3)
                .setModelType(Model.ModelType.GURU)
                .setPublishedOnBlueMarket(true))
            .addModel(new Model().setId(4)
                .setCategoryId(42)
                .setTitle("The title")
                .setModelType(Model.ModelType.SKU)
                .setSkuParentModelId(142)
                .setPublishedOnBlueMarket(true));

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).extracting(ErrorAtLine::toString)
            .containsExactly(
                "Ошибка на строке 1: Market SKU 'just-wrong-msku' должно быть числом",
                "Ошибка на строке 2: Модель (sku id: 2) должна быть опубликована на синем маркете.",
                "Ошибка на строке 3: Модель (sku id: 3) должна быть SKU или иметь проставленный признак " +
                    "'Модель является SKU'.");

        List<ImportFileService.OfferResult> offers = result.getOffers();
        assertThat(offers).hasSize(4);
        assertThat(offers.get(0).getOffer().hasSupplierSkuMapping()).isFalse();
        assertThat(offers.get(1).getOffer().hasSupplierSkuMapping()).isFalse();
        assertThat(offers.get(2).getOffer().hasSupplierSkuMapping()).isFalse();
        assertThat(offers.get(3).getOffer().hasSupplierSkuMapping()).isTrue();

        Offer offer = offers.get(3).getOffer();
        assertThat(offer.getSupplierSkuMapping().getMappingId()).isEqualTo(4);
    }

    @Test
    public void testZeroMskuIsOK() {
        Offer offer1 = OfferTestUtils.simpleOffer().setShopSku("shop-sku-1");
        Offer offer2 = OfferTestUtils.simpleOffer().setShopSku("shop-sku-2");
        offerRepository.insertOffers(List.of(offer1, offer2));

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "sku маркета", "Страна производства",
                "Страница модели")
            .addLine("1234567890", "shop-sku-1", "title1", "category", "0", "Китай", "https://some.site.ru/123")
            .addLine("1234567890", "shop-sku-2", "title2", "changed-category", "0", "Китай", "https://some.site.ru/123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL, INTERNAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isEmpty();

        List<ImportFileService.OfferResult> offers = result.getOffers();
        assertThat(offers).hasSize(2);
        Map<String, Offer> offersByShopSku = offers.stream()
            .map(ImportFileService.OfferResult::getOffer)
            .collect(Collectors.toMap(Offer::getShopSku, Function.identity()));
        assertThat(offersByShopSku).containsOnlyKeys("shop-sku-1", "shop-sku-2");
        assertThat(offersByShopSku.get("shop-sku-1").hasSupplierSkuMapping()).isFalse();
        assertThat(offersByShopSku.get("shop-sku-1").getApprovedSkuMapping().getMappingId()).isZero();
        assertThat(offersByShopSku.get("shop-sku-2").hasSupplierSkuMapping()).isFalse();
        assertThat(offersByShopSku.get("shop-sku-2").getApprovedSkuMapping().getMappingId()).isZero();
    }

    @Test
    public void testZeroMskuIsNotSavedForNewOffer() {
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "sku маркета", "Страна производства",
                "Страница модели")
            .addLine("1234567890", "shop-sku-1", "title1", "category", "0", "Китай", "https://some.site.ru/123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isEmpty();

        List<ImportFileService.OfferResult> offers = result.getOffers();
        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).getOffer().hasSupplierSkuMapping()).isFalse();
        assertThat(offers.get(0).getOffer().getApprovedSkuMapping()).isNull();
    }

    @Test
    public void testZeroMskuIsErasingApprovedMappingForInner() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setSupplierSkuMapping(new Offer.Mapping(10L, LocalDateTime.now()))
            .updateApprovedSkuMapping(new Offer.Mapping(123L, LocalDateTime.now()),
                Offer.MappingConfidence.CONTENT)
            .setShopSku("shop-sku-1");
        offerRepository.insertOffers(List.of(offer1));

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "sku маркета", "Страна производства",
                "Страница модели")
            .addLine("1234567890", "shop-sku-1", "title1", "category", "0", "Китай", "https://some.site.ru/123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL, INTERNAL),
            OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isEmpty();

        List<ImportFileService.OfferResult> offers = result.getOffers();
        assertThat(offers).hasSize(1);
        Offer offerAfterUpdate = offers.get(0).getOffer();
        assertFalse(offerAfterUpdate.hasApprovedSkuMapping());
        assertThat(offerAfterUpdate.getSupplierSkuId()).isZero();
    }

    @Test
    public void testZeroMskuIsNOTErasingApprovedMappingForOuter() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setSupplierSkuMapping(new Offer.Mapping(10L, LocalDateTime.now()))
            .setContentSkuMapping(new Offer.Mapping(123L, LocalDateTime.now()))
            .updateApprovedSkuMapping(new Offer.Mapping(123L, LocalDateTime.now()),
                Offer.MappingConfidence.CONTENT)
            .setShopSku("shop-sku-1");
        offerRepository.insertOffers(List.of(offer1));

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "sku маркета", "Страна производства",
                "Страница модели")
            .addLine("1234567890", "shop-sku-1", "title1", "category", "0", "Китай", "https://some.site.ru/123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL),
            OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isEmpty();

        List<ImportFileService.OfferResult> offers = result.getOffers();
        assertThat(offers).hasSize(1);
        Offer offerAfterUpdate = offers.get(0).getOffer();
        assertTrue(offerAfterUpdate.hasApprovedSkuMapping());
        assertThat(offerAfterUpdate.getSupplierSkuId()).isZero();
    }

    @Test
    public void testPartialFailsInCaseOfGlobalErrors() {
        Offer offer1 = OfferTestUtils.simpleOffer();
        offerRepository.insertOffers(Collections.singletonList(offer1));

        // shop_title is absent
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "категория", "Страна производства", "Страница модели")
            .addLine("1234567890", "shop-sku", "category-changed", "Китай", "https://some.site.ru/123")
            .addLine("1234567890", "shop-sku-2", "changed-category", "Китай", "https://some.site.ru/123") // NOTE empty title
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        Mockito.verifyZeroInteractions(offersEnrichmentService);

        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors()).extracting(ErrorAtLine::getLineIndex).containsExactly(-1, -1);
        assertThat(result.getErrors()).extracting(ErrorAtLine::toString)
            .anyMatch(e -> e.contains("Файл не содержит обязательного заголовка 'Shop_title'"));

        // No offers are returned in case of global errors. At least for now.
        assertThat(result.getOffers()).isEmpty();

        // Nothing is changed as there's global error
        assertEquals(1, offerRepository.getCount());
        assertThat(offerRepository.findAll()).extracting(Offer::getShopSku)
            .containsExactlyInAnyOrder("shop-sku");
        assertThat(offerRepository.getOfferById(offer1.getId()).getShopCategoryName())
            .isEqualTo(OfferTestUtils.DEFAULT_SHOP_CATEGORY_NAME);
    }

    @Test
    public void testMasterDataIsNotSavedForOffersWithErrors() {
        // shop_title is absent
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Гарантийный срок", "Страна производства",
                "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "", "cat", "180", "Китай", "https://some.site.ru/123") // empty title, shouldn't save
            .addLine("1234567890", "shop-sku-2", "ok-title", "changed-category", "180", "Китай", "http://other.site.com/tovar/vl2mv")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors()).extracting(ErrorAtLine::getLineIndex).containsExactly(0);
        assertThat(result.getErrors()).extracting(ErrorAtLine::toString)
            .anyMatch(e -> e.contains("Ошибка на строке 1: Отсутствует значение для колонки 'название товара'"));

        assertEquals(1, offerRepository.getCount());
        assertThat(offerRepository.findAll()).extracting(Offer::getShopSku)
            .containsExactlyInAnyOrder("shop-sku-2");

        // No extra data is saved
        assertThat(masterDataServiceMock.getSskuMasterData().keySet().stream()
            .map(MdmCommon.ShopSkuKey::getShopSku)
        )
            .containsExactly("shop-sku-2");
        assertThat(masterDataRepositoryMock.findAll()).hasSize(1);
        assertThat(masterDataRepositoryMock.findAll().stream().findFirst()
            .map(MasterDataAsJsonDTO::getShopSku).orElse(null))
            .isEqualTo("shop-sku-2");
    }

    @Test
    public void testMasterDataIsNotSendForOffersWithDataCampFlag() {
        Offer offer1 = OfferTestUtils.simpleOffer();
        offer1.setDataCampOffer(true);
        offerRepository.insertOffers(Collections.singletonList(offer1));
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Гарантийный срок", "Страна производства",
                "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "title", "cat", "180", "Китай", "https://some.site.ru/123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isEmpty();

        assertThat(masterDataRepositoryMock.findAll()).hasSize(1); //saved
        assertThat(masterDataServiceMock.getSskuMasterData()).isEmpty(); //but not sent
    }

    @Test
    public void testOfferNotSavedWhenMasterDataValidationFailsAllOrNothingModeParsing() {
        // manufacturer country is absent
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Гарантийный срок", "Страна производства",
                "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "ok-title", "cat", "180", "", "https://some.site.ru/123") // empty
            // country, not save
            .addLine("1234567890", "shop-sku-2", "ok-title", "changed-category", "180", "Китай", "http://other.site" +
                ".com/tovar/vl2mv")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertSoftly(softly -> {
            softly.assertThat(result.getErrors()).isNotEmpty();
            softly.assertThat(offerRepository.getCount()).isEqualTo(0);
            softly.assertThat(masterDataServiceMock.getSskuMasterData()).isEmpty();
            softly.assertThat(masterDataRepositoryMock.findAll()).isEmpty();
        });
    }

    @Test
    public void testOfferNotSavedWhenMasterDataValidationFailsPartialModeParsing() {
        // manufacturer country is absent
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Гарантийный срок", "Страна производства",
                "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "ok-title", "cat", "180", "", "https://some.site.ru/123") // empty
            // country, not save
            .addLine("1234567890", "shop-sku-2", "ok-title", "changed-category", "180", "Китай", "http://other.site" +
                ".com/tovar/vl2mv")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertSoftly(softly -> {
            softly.assertThat(result.getErrors()).isNotEmpty();
            softly.assertThat(offerRepository.findAll()).extracting(Offer::getShopSku)
                .containsExactlyInAnyOrder("shop-sku-2");
            softly.assertThat(masterDataServiceMock.getSskuMasterData().keySet().stream()
                    .map(MdmCommon.ShopSkuKey::getShopSku)
                )
                .containsExactly("shop-sku-2");
            softly.assertThat(masterDataRepositoryMock.findAll()).hasSize(1);
            softly.assertThat(masterDataRepositoryMock.findAll().stream().findFirst().orElseThrow().getShopSku())
                .isEqualTo("shop-sku-2");
        });
    }

    @Test
    public void testMasterDataNotSavedNorValidatedForFmcgOffers() {
        // fmcg offers
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Гарантийный срок", "Страна производства",
                "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "ok-title", "cat", "180", "", "https://some.site.ru/123") // empty
            // country
            .addLine("1234567890", "shop-sku-2", "ok-title", "changed-category", "180", "Китай", "http://other.site" +
                ".com/tovar/vl2mv")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.FMCG_SUPPLIER_ID, "fmcg", NULL_CONSUMER);

        assertSoftly(softly -> {
            softly.assertThat(result.getErrors()).isEmpty();
            softly.assertThat(offerRepository.findAll()).extracting(Offer::getShopSku)
                .containsExactlyInAnyOrder("shop-sku", "shop-sku-2");
            softly.assertThat(masterDataServiceMock.getSskuMasterData()).isEmpty();
            softly.assertThat(masterDataRepositoryMock.findAll()).isEmpty();
        });
    }

    @Test
    public void testMasterDataNotSavedNorValidatedIfDisabled() {
        // fmcg offers
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Гарантийный срок", "Страна производства",
                "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "ok-title", "cat", "180", "", "https://some.site.ru/123") // empty
            // country
            .addLine("1234567890", "shop-sku-2", "ok-title", "changed-category", "180", "Китай", "http://other.site" +
                ".com/tovar/vl2mv")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), SUPPLIER_ID_NO_MDM, "fmcg", NULL_CONSUMER);

        assertSoftly(softly -> {
            softly.assertThat(result.getErrors()).isEmpty();
            softly.assertThat(offerRepository.findAll()).extracting(Offer::getShopSku)
                .containsExactlyInAnyOrder("shop-sku", "shop-sku-2");
            softly.assertThat(masterDataServiceMock.getSskuMasterData()).isEmpty();
            softly.assertThat(masterDataRepositoryMock.findAll()).isEmpty();
        });
    }

    @Test
    public void testSavedCorrectMasterDataInPartialMode() {
        MasterData masterData = TestDataUtils.generateMasterData(
            "shop-sku", OfferTestUtils.TEST_SUPPLIER_ID, defaultRandom
        );
        ExcelFile excelFile = getExcelFileForMasterData(masterData);

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        MasterData masterDataFromDb = masterDataHelperService.findSskuMasterData(
            Collections.singletonList(masterData.getShopSkuKey())
        ).get(0);

        assertSoftly(softly -> {
            softly.assertThat(result.getErrors()).isEmpty();
            softly.assertThat(masterDataFromDb)
                .usingComparatorForFields(Comparator.comparing(FieldParseUtil::toStringLowerFirstChar),
                    "shelfLifeComment", "lifeTimeComment", "guaranteePeriodComment")
                .isEqualToIgnoringGivenFields(masterData,
                    "vat", "boxCount", "supplySchedule", "qualityDocuments", "dangerousGood", "heavyGood",
                    "shelfLifeRequired", "isUploadedToYtHahn", "isUploadedToYtArnold", "modifiedTimestamp",
                    "itemShippingUnit", "useInMercury", "vetisGuids", "preciousGood", "goldenItemShippingUnit",
                    "goldenRsl", "gtins", "surplusHandleMode", "cisHandleMode", "heavyGood20", "regNumbers",
                    "datacampMasterDataVersion", "traceable", "measurementState");
            softly.assertThat(masterDataRepositoryMock.findAll()).hasSize(1);
            softly.assertThat(masterDataRepositoryMock.findAll().stream().findFirst().orElseThrow()
                    .getMasterData().convertTo())
                .usingComparatorForFields(Comparator.comparing(FieldParseUtil::toStringLowerFirstChar),
                    "shelfLifeComment", "lifeTimeComment", "guaranteePeriodComment")
                .isEqualToIgnoringGivenFields(masterData,
                    "vat", "boxCount", "supplySchedule", "qualityDocuments", "dangerousGood", "heavyGood",
                    "shelfLifeRequired", "isUploadedToYtHahn", "isUploadedToYtArnold", "modifiedTimestamp",
                    "itemShippingUnit", "useInMercury", "vetisGuids", "preciousGood", "goldenItemShippingUnit",
                    "goldenRsl", "gtins", "surplusHandleMode", "cisHandleMode", "heavyGood20", "regNumbers",
                    "datacampMasterDataVersion", "traceable", "measurementState");
        });
    }

    @Test
    public void testSavedCorrectMasterDataInAllOrNothingMode() {
        MasterData masterData = TestDataUtils.generateMasterData(
            "shop-sku", OfferTestUtils.TEST_SUPPLIER_ID, defaultRandom
        );
        ExcelFile excelFile = getExcelFileForMasterData(masterData);

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        MasterData masterDataFromDb = masterDataHelperService.findSskuMasterData(
            Collections.singletonList(masterData.getShopSkuKey())
        ).get(0);

        assertSoftly(softly -> {
            softly.assertThat(result.getErrors()).isEmpty();
            softly.assertThat(masterDataFromDb)
                .usingComparatorForFields(Comparator.comparing(FieldParseUtil::toStringLowerFirstChar),
                    "shelfLifeComment", "lifeTimeComment", "guaranteePeriodComment")
                .isEqualToIgnoringGivenFields(masterData,
                    "vat", "boxCount", "supplySchedule", "qualityDocuments", "dangerousGood", "shelfLifeRequired",
                    "isUploadedToYtHahn", "isUploadedToYtArnold", "modifiedTimestamp", "boxCount", "heavyGood",
                    "itemShippingUnit", "useInMercury", "vetisGuids", "preciousGood", "goldenItemShippingUnit",
                    "goldenRsl", "gtins", "surplusHandleMode", "cisHandleMode", "heavyGood20", "regNumbers",
                    "datacampMasterDataVersion", "traceable", "measurementState");
            softly.assertThat(masterDataRepositoryMock.findAll()).hasSize(1);
            softly.assertThat(masterDataRepositoryMock.findAll().stream().findFirst().orElseThrow()
                    .getMasterData().convertTo())
                .usingComparatorForFields(Comparator.comparing(FieldParseUtil::toStringLowerFirstChar),
                    "shelfLifeComment", "lifeTimeComment", "guaranteePeriodComment")
                .isEqualToIgnoringGivenFields(masterData,
                    "vat", "boxCount", "supplySchedule", "qualityDocuments", "dangerousGood", "heavyGood",
                    "shelfLifeRequired", "isUploadedToYtHahn", "isUploadedToYtArnold", "modifiedTimestamp",
                    "itemShippingUnit", "useInMercury", "vetisGuids", "preciousGood", "goldenItemShippingUnit",
                    "goldenRsl", "gtins", "surplusHandleMode", "cisHandleMode", "heavyGood20", "regNumbers",
                    "datacampMasterDataVersion", "traceable", "measurementState");
        });

    }

    @Test
    public void testEraseBoxCount() {
        MasterData repositoryMasterData =
            TestDataUtils.generateMasterData("shop-sku", OfferTestUtils.TEST_SUPPLIER_ID, defaultRandom);
        assertThat(repositoryMasterData.hasBoxCount()).isTrue();
        masterDataHelperService.saveSskuMasterDataAndDocuments(Collections.singletonList(repositoryMasterData));

        MasterData excelMasterData =
            TestDataUtils.generateMasterData("shop-sku", OfferTestUtils.TEST_SUPPLIER_ID, defaultRandom);
        excelMasterData.setBoxCount(null);
        ExcelFile excelFile = getExcelFileForMasterData(excelMasterData);

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isEmpty();

        MasterData masterDataFromDb = masterDataHelperService.findSskuMasterData(
            Collections.singletonList(repositoryMasterData.getShopSkuKey())
        ).get(0);

        assertThat(masterDataRepositoryMock.findAll()).hasSize(1);
        MasterData masterDataInRepo = masterDataRepositoryMock.findAll().stream().findFirst().orElseThrow()
            .getMasterData().convertTo();
        assertThat(masterDataInRepo.hasBoxCount()).isFalse(); // has been erased
    }

    @Test
    public void testReturnErrorsProducedByMasterDataValidation() {
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Гарантийный срок", "Страна производства",
                "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "ok-title", "changed-category", "180", "Китай", "http://other.site" +
                ".com/tovar/vl2mv")
            .build();

        ErrorAtLine masterDataError = new ErrorAtLine(1,
            new ErrorInfo("error-code",
                "message-template",
                ErrorInfo.Level.ERROR,
                ImmutableMap.of("param", "value"))
        );

        Mockito.doReturn(Collections.singletonList(
            new MasterDataConvertResult(0, new MasterData(), Collections.singletonList(masterDataError)))
        ).when(offerToMasterDataConverter).convert(anyList());

        ImportResult partialResult = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        ImportResult allOrNothingResult = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertSoftly(softly -> {
            softly.assertThat(partialResult.getErrors()).containsExactly(masterDataError);
            softly.assertThat(allOrNothingResult.getErrors()).containsExactly(masterDataError);
        });
    }

    @Test
    public void testUrlIsRequiredForImportingFile() {
        // with no url header
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getOffers()).isEmpty();
        assertEquals(0, offerRepository.getCount());

        // with url header, but empty value
        excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай")
            .build();

        result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getOffers()).hasSize(1);
        assertEquals(0, offerRepository.getCount());

        // everything is ok
        excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "http://other.site.com/tovar/vl2mv")
            .build();

        result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getOffers()).hasSize(1);
        assertEquals(1, offerRepository.getCount());
    }

    @Test
    @Ignore
    public void testInvalidTimeMdmParamsNotImported() {
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства", "Ссылка на страницу модели",
                "Срок годности", "Гарантийный срок", "Срок службы")
            .addLine("1234567890", "shop-sku", "dmserebr-title", "category", "Ватикан", "http://yandex.ru",
                "30000 дней", "12345 месяцев", "180000 дней")
            .build();

        ImportResult importResult = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        ErrorAtLine shelfLifeError = new ErrorAtLine(0,
            MbocErrors.get().excelValueMustBeInRange(ExcelHeaders.SHELF_LIFE.getTitle(), "30000", "3", "3650"));
        ErrorAtLine lifeTimeError = new ErrorAtLine(0,
            MbocErrors.get().excelValueMustBeInRange(ExcelHeaders.LIFE_TIME.getTitle(), "180000", "1", "18250"));
        ErrorAtLine guaranteePeriodError = new ErrorAtLine(0,
            MbocErrors.get().excelValueMustBeInRange(ExcelHeaders.GUARANTEE_PERIOD.getTitle(), "370350", "30", "18250")
        );

        assertSoftly(softly -> {
            softly.assertThat(importResult.getErrors()).containsExactlyInAnyOrder(
                shelfLifeError, lifeTimeError, guaranteePeriodError
            );
        });
    }

    @Test
    public void testValidTimeMdmParamsAreImported() {
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства", "Ссылка на страницу модели",
                "Срок годности", "Гарантийный срок", "Срок службы")
            .addLine("1234567890", "shop-sku", "dmserebr-title", "category", "Ватикан", "http://yandex.ru",
                "30 дней", "6 месяцев", "180 дней")
            .build();

        ImportResult importResult = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(importResult.hasErrors()).isFalse();
    }

    @Test
    public void testParallelImportedOffers() {
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders(
                "Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели", "Параллельный импорт")
            .addLine("1234567890", "shop-sku-0", "title1", "category1", "Китай", "https://some.site.ru/123", "")
            .addLine("1234567890", "shop-sku-1", "title2", "category2", "Китай", "https://some.site.ru/123", "нет")
            .addLine("1234567890", "shop-sku-2", "title3", "category1", "Китай", "https://some.site.ru/123", "да")
            .addLine("1234567890", "shop-sku-3", "title4", "category2", "Китай", "https://some.site.ru/123", "Нет")
            .addLine("1234567890", "shop-sku-4", "title5", "category1", "Китай", "https://some.site.ru/123", "Да")
            .addLine("1234567890", "shop-sku-5", "title6", "category2", "Китай", "https://some.site.ru/123", "no")
            .addLine("1234567890", "shop-sku-6", "title7", "category1", "Китай", "https://some.site.ru/123", "yes")
            .addLine("1234567890", "shop-sku-7", "title8", "category2", "Китай", "https://some.site.ru/123", "NO")
            .addLine("1234567890", "shop-sku-8", "title9", "category1", "Китай", "https://some.site.ru/123", "YES")
            .addLine("1234567890", "shop-sku-9", "title10", "category2", "Китай", "https://some.site.ru/123", "abracadabra")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
        assertThat(result.getOffers()).hasSize(10);
        assertEquals(10, offerRepository.getCount());

        var offers = offerRepository.findAll();
        var imported = offers.stream().filter(Offer::isParallelImported).count();
        var notImported = offers.stream().filter(Predicate.not(Offer::isParallelImported)).count();
        assertEquals(4, imported);
        assertEquals(6, notImported);
    }

    @Test
    public void testParallelImport1pSupplierWithoutGuaranteePeriod(){
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders(
                "Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели", "Параллельный импорт")
            .addLine("1234567890", "shop-sku-0", "title1", "category1", "Китай", "https://some.site.ru/123", "")
            .addLine("1234567890", "shop-sku-2", "title3", "category1", "Китай", "https://some.site.ru/123", "да")
            .addLine("1234567890", "shop-sku-4", "title5", "category1", "Китай", "https://some.site.ru/123", "Да")
            .addLine("1234567890", "shop-sku-6", "title7", "category1", "Китай", "https://some.site.ru/123", "yes")
            .addLine("1234567890", "shop-sku-8", "title9", "category1", "Китай", "https://some.site.ru/123", "YES")
            .addLine("1234567890", "shop-sku-9", "title10", "category2", "Китай", "https://some.site.ru/123", "abracadabra")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), SUPPLIER_ID_1P, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(4);
        assertThat(result.getOffers()).hasSize(6);

        var offers = offerRepository.findAll();
        var imported = offers.stream().filter(Offer::isParallelImported).count();
        var notImported = offers.stream().filter(Predicate.not(Offer::isParallelImported)).count();
        assertEquals(0, imported);
        assertEquals(2, notImported);
    }

    @Test
    public void testParallelImport1pSupplierWithGuaranteePeriod(){
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders(
                "Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели", "Параллельный импорт", ExcelHeaders.GUARANTEE_PERIOD.getTitle())
            .addLine("1234567890", "shop-sku-0", "title1", "category1", "Китай", "https://some.site.ru/123", "", 180)
            .addLine("1234567890", "shop-sku-2", "title3", "category1", "Китай", "https://some.site.ru/123", "да", 180)
            .addLine("1234567890", "shop-sku-4", "title5", "category1", "Китай", "https://some.site.ru/123", "Да", 180)
            .addLine("1234567890", "shop-sku-6", "title7", "category1", "Китай", "https://some.site.ru/123", "yes", 180)
            .addLine("1234567890", "shop-sku-8", "title9", "category1", "Китай", "https://some.site.ru/123", "YES", 180)
            .addLine("1234567890", "shop-sku-9", "title10", "category2", "Китай", "https://some.site.ru/123", "abracadabra", 180)
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), SUPPLIER_ID_1P, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
        assertThat(result.getOffers()).hasSize(6);

        var offers = offerRepository.findAll();
        var imported = offers.stream().filter(Offer::isParallelImported).count();
        var notImported = offers.stream().filter(Predicate.not(Offer::isParallelImported)).count();
        assertEquals(4, imported);
        assertEquals(2, notImported);
    }

    @Test
    public void testParallelImport1pSupplier(){
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders(
                "Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели", "Параллельный импорт", ExcelHeaders.GUARANTEE_PERIOD.getTitle())
            .addLine("1234567890", "shop-sku-0", "title1", "category1", "Китай", "https://some.site.ru/123", "", 180)
            .addLine("1234567890", "shop-sku-2", "title3", "category1", "Китай", "https://some.site.ru/123", "да", null)
            .addLine("1234567890", "shop-sku-4", "title5", "category1", "Китай", "https://some.site.ru/123", "Да", 180)
            .addLine("1234567890", "shop-sku-6", "title7", "category1", "Китай", "https://some.site.ru/123", "yes", "")
            .addLine("1234567890", "shop-sku-8", "title9", "category1", "Китай", "https://some.site.ru/123", "YES", 180)
            .addLine("1234567890", "shop-sku-9", "title10", "category2", "Китай", "https://some.site.ru/123", "abracadabra", 180)
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), SUPPLIER_ID_1P, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors().get(0).getErrorInfo())
            .isEqualTo(MbocErrors.get().excelValueIsRequired(ExcelHeaders.GUARANTEE_PERIOD.getTitle()));
        assertThat(result.getOffers()).hasSize(6);

        var offers = offerRepository.findAll();
        var imported = offers.stream().filter(Offer::isParallelImported).count();
        var notImported = offers.stream().filter(Predicate.not(Offer::isParallelImported)).count();
        assertEquals(2, imported);
        assertEquals(2, notImported);
    }


    private ExcelFile getExcelFileForMasterData(MasterData masterData) {
        HashMap<String, String> data = new HashMap<>();
        data.put(ExcelHeaders.SKU.getTitle(), masterData.getShopSku());
        data.put(ExcelHeaders.TITLE.getTitle(), "ok-title");
        data.put(ExcelHeaders.CATEGORY.getTitle(), "cat");
        data.put(ExcelHeaders.URL.getTitle(), "https://some.site.ru/123");
        data.put(ExcelHeaders.MANUFACTURER_COUNTRY.getTitle(),
            masterData.getManufacturerCountries().stream().collect(Collectors.joining())
        );
        data.put(ExcelHeaders.MANUFACTURER.getTitle(), masterData.getManufacturer());

        data.put(ExcelHeaders.SHELF_LIFE.getTitle(),
            TimeInUnitsConverter.convertToStringRussian(masterData.getShelfLife()));
        data.put(ExcelHeaders.LIFE_TIME.getTitle(),
            TimeInUnitsConverter.convertToStringRussian(masterData.getLifeTime()));
        data.put(ExcelHeaders.GUARANTEE_PERIOD.getTitle(),
            TimeInUnitsConverter.convertToStringRussian(masterData.getGuaranteePeriod()));
        data.put(ExcelHeaders.SHELF_LIFE_COMMENT.getTitle(), masterData.getShelfLifeComment());
        data.put(ExcelHeaders.LIFE_TIME_COMMENT.getTitle(), masterData.getLifeTimeComment());
        data.put(ExcelHeaders.GUARANTEE_PERIOD_COMMENT.getTitle(), masterData.getGuaranteePeriodComment());
        data.put(ExcelHeaders.MIN_SHIPMENT.getTitle(), masterData.getMinShipment() + "");
        data.put(ExcelHeaders.TRANSPORT_UNIT_SIZE.getTitle(), masterData.getTransportUnit().toString());
        data.put(ExcelHeaders.DELIVERY_TIME.getTitle(), masterData.getDeliveryTime() + "");
        data.put(ExcelHeaders.QUANTUM_OF_SUPPLY.getTitle(), masterData.getQuantumOfSupply() + "");
        data.put(ExcelHeaders.CUSTOMS_COMMODITY_CODE.getTitle(), masterData.getCustomsCommodityCode());
        data.put(ExcelHeaders.BOX_COUNT.getTitle(), masterData.hasBoxCount() ? masterData.getBoxCount().toString() : "");
        return ExcelFile.Builder
            .withHeaders(data.keySet().stream().toArray(String[]::new))
            .addLine(data.values().stream().toArray(String[]::new))
            .build();
    }

    @Test
    public void testZeroMskuIsOKChangesStatusInNewPipeline() {
        categoryKnowledgeService.addCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        testZeroMskuIsOKChangesStatusInNewPipelineBody("shop-sku-1", "shop-sku-2",
            Offer.ProcessingStatus.OPEN, Offer.ProcessingStatus.PROCESSED);
        categoryCachingService.setCategoryAcceptGoodContent(OfferTestUtils.TEST_CATEGORY_INFO_ID, true);
        testZeroMskuIsOKChangesStatusInNewPipelineBody("shop-sku-3", "shop-sku-4",
            Offer.ProcessingStatus.IN_MODERATION, Offer.ProcessingStatus.PROCESSED);
        testZeroMskuIsOKChangesStatusInNewPipelineBody("shop-sku-5", "shop-sku-6",
            Offer.ProcessingStatus.IN_MODERATION, Offer.ProcessingStatus.PROCESSED);
        categoryCachingService.setCategoryAcceptGoodContent(OfferTestUtils.TEST_CATEGORY_INFO_ID, false);
        testZeroMskuIsOKChangesStatusInNewPipelineBody("shop-sku-7", "shop-sku-8",
            Offer.ProcessingStatus.IN_MODERATION, Offer.ProcessingStatus.PROCESSED);
        testZeroMskuIsOKChangesStatusInNewPipelineBody("shop-sku-9", "shop-sku-10",
            Offer.ProcessingStatus.IN_MODERATION, Offer.ProcessingStatus.PROCESSED);
    }

    private void testZeroMskuIsOKChangesStatusInNewPipelineBody(String shopSku1,
                                                                String shopSku2,
                                                                Offer.ProcessingStatus inStatus,
                                                                Offer.ProcessingStatus resultStatus) {
        offerRepository.insertOffer(OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(inStatus)
            .setShopSku(shopSku1));
        offerRepository.insertOffer(OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(inStatus)
            .setShopSku(shopSku2));
        supplierRepository.insert(new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "test")
            .setNewContentPipeline(true));
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "sku маркета", "Страна производства",
                "Страница модели")
            .addLine("1234567890", shopSku1, "title1", "category", "0", "Китай", "https://some.site.ru/123")
            .addLine("1234567890", shopSku2, "title2", "changed-category", "0", "Китай", "https://some.site.ru/123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL, INTERNAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isEmpty();

        List<ImportFileService.OfferResult> offers = result.getOffers();
        List<Offer> offersInRepo = offerRepository.findOffersByBusinessSkuKeys(
            Arrays.asList(new BusinessSkuKey(OfferTestUtils.TEST_SUPPLIER_ID, shopSku1),
                new BusinessSkuKey(OfferTestUtils.TEST_SUPPLIER_ID, shopSku2)));
        assertThat(offers).hasSize(2);
        assertThat(offersInRepo).hasSize(2);
        offersInRepo.forEach(offer -> {
            assertThat(offer.hasSupplierSkuMapping()).isFalse();
            assertThat(offer.getApprovedSkuMapping().getMappingId()).isZero();
            assertThat(offer.getProcessingStatus()).isEqualTo(resultStatus);
        });
        offers.forEach(offerResult -> {
            assertThat(offerResult.getOffer().hasSupplierSkuMapping()).isFalse();
            assertThat(offerResult.getOffer().getApprovedSkuMapping().getMappingId()).isZero();
            assertThat(offerResult.getOffer().getProcessingStatus()).isEqualTo(resultStatus);
        });
    }

    @Test
    public void testNoInMskuChangesStatusInNewPipeline() {
        categoryKnowledgeService.addCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        offerRepository.insertOffer(OfferTestUtils.simpleOkOffer()
            .setShopSku("shop-sku-1")
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO));
        offerRepository.insertOffer(OfferTestUtils.simpleOkOffer().
            setShopSku("shop-sku-2")
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setLastPrimaryProcessingStatus(Offer.ProcessingStatus.IN_MODERATION)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO));
        supplierRepository.insert(new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "test")
            .setNewContentPipeline(true));
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "sku маркета", "Страна производства",
                "Страница модели")
            .addLine("1234567890", "shop-sku-1", "title1", "category", "Нет", "Китай", "https://some.site.ru/123")
            .addLine("1234567890", "shop-sku-2", "title1", "category", "nO", "Китай", "https://some.site.ru/123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isEmpty();

        List<ImportFileService.OfferResult> offers = result.getOffers();
        List<Offer> offersInRepo = offerRepository.findOffersByBusinessSkuKeys(
            Arrays.asList(new BusinessSkuKey(OfferTestUtils.TEST_SUPPLIER_ID, "shop-sku-1"),
                new BusinessSkuKey(OfferTestUtils.TEST_SUPPLIER_ID, "shop-sku-2")));
        assertThat(offers).hasSize(2);
        assertThat(offersInRepo).hasSize(2);
        offersInRepo.forEach(offer -> {
            assertThat(offer.hasSupplierSkuMapping()).isFalse();
            assertThat(offer.hasSupplierSkuMapping()).isFalse();
            assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_CONTENT);
        });
        offers.forEach(offerResult -> {
            assertThat(offerResult.getOffer().hasSupplierSkuMapping()).isFalse();
            assertThat(offerResult.getOffer().hasApprovedSkuMapping()).isFalse();
            assertThat(offerResult.getOffer().getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_CONTENT);
        });
    }

    @Test
    public void testDuplicateOffer() {
        Offer offer = OfferTestUtils.simpleOffer();
        offerRepository.insertOffer(offer);

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства", "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "https://some.site.ru/123")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "https://some.site.ru/123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getOffers()).hasSize(2);

        ImportFileService.OfferResult result0 = result.getOffers().get(0);
        assertEquals(0, result0.getLineIndex());
        assertEquals("changed-title", result0.getOffer().getTitle());
        assertFalse("First result must not be new", result0.isNew());

        ImportFileService.OfferResult result1 = result.getOffers().get(1);
        assertEquals(1, result1.getLineIndex());
        assertEquals("changed-title", result1.getOffer().getTitle());
        assertFalse("First result must not be new", result1.isNew());

        ErrorAtLine duplicateShopSkuError = new ErrorAtLine(1,
            MbocErrors.get().excelDuplicatedShopSku("shop-sku", new Line(1), new Line(2)));

        assertEquals(duplicateShopSkuError, result.getErrors().get(0));

        // No new offers added
        assertEquals(1, offerRepository.getCount());
        // Title isn't changed: new ImportSettings(ALL_OR_NOTHING)
        assertEquals("title", offerRepository.getOfferById(offer.getId()).getTitle());
    }

    @Test
    public void testDuplicateOfferPARTIAL() {
        Offer offer = OfferTestUtils.simpleOffer();
        offerRepository.insertOffer(offer);

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства", "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "changed-title1", "category", "Китай", "https://some.site.ru/123")
            .addLine("1234567890", "shop-sku", "changed-title2", "category", "Китай", "https://some.site.ru/123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getOffers()).hasSize(2);

        ImportFileService.OfferResult result0 = result.getOffers().get(0);
        assertEquals(0, result0.getLineIndex());
        assertEquals("changed-title1", result0.getOffer().getTitle());
        assertFalse("First result must not be new", result0.isNew());

        ImportFileService.OfferResult result1 = result.getOffers().get(1);
        assertEquals(1, result1.getLineIndex());
        assertEquals("changed-title2", result1.getOffer().getTitle());
        assertFalse("First result must not be new", result1.isNew());

        ErrorAtLine duplicateShopSkuError = new ErrorAtLine(1,
            MbocErrors.get().excelDuplicatedShopSku("shop-sku", new Line(1), new Line(2)));

        assertEquals(duplicateShopSkuError, result.getErrors().get(0));

        // No new offers added
        assertEquals(1, offerRepository.getCount());
        // Title is changed to title from not error line: PARTIAL
        assertEquals("changed-title1", offerRepository.getOfferById(offer.getId()).getTitle());
    }

    @Test
    public void testPartialBatchHandlesErrors() {
        ExcelFile.Builder builder = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства", "Ссылка на страницу модели");
        for (int i = 0; i < 1100; i++) {
            builder.addLine("1234567890", "shop-sku" + i, "changed-title" + i, "category", "Китай", "https://some.site.ru/123");
        }
        ExcelFile excelFile = builder.build();

        int[] call = new int[1];
        doAnswer(i -> {
            if (call[0]++ == 0) {
                throw new SqlConcurrentModificationException("!!", Collections.emptyList());
            } else {
                return i.callRealMethod();
            }
        }).when(offerRepository).insertOffers(anyList());

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL_BATCH), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertFalse(result.hasErrors());
        assertEquals(1100, offerRepository.getCount());
    }

    @Test
    public void testPartialBatchSkipsFailingBatch() {
        ExcelFile.Builder builder = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства", "Ссылка на страницу модели");
        for (int i = 0; i < 1100; i++) {
            builder.addLine("1234567890", "shop-sku" + i, "changed-title" + i, "category", "Китай", "https://some" +
                ".site.ru/123");
        }
        ExcelFile excelFile = builder.build();

        doAnswer(i -> {
            if (i.<List<Offer>>getArgument(0).stream().anyMatch(o -> o.getShopSku().equals("shop-sku666"))) {
                throw new IllegalStateException("No luck for this batch");
            } else {
                return i.callRealMethod();
            }
        }).when(offerRepository).insertOffers(anyList());

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL_BATCH), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertTrue(result.hasErrors());
        assertEquals(600, offerRepository.getCount());
        assertThat(result.getOffers()).hasSize(600);
        assertThat(result.getMasterDataConvertResultList()).hasSize(600);
        assertThat(result.getErrors()).hasSize(500);

        ErrorAtLine error = result.getErrors().get(0);
        assertThat(error.getLineIndex()).isEqualTo(500);
        assertThat(error.getErrorInfo().toString()).contains("Произошла ошибка при разборе файла");
    }

    @Test
    public void testMappingsApprovedForFmcgSupplier() {
        supplierRepository.insert(new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "test")
            .setType(MbocSupplierType.FMCG)
            .setNewContentPipeline(true));
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "sku маркета", "Страна производства",
                "Страница модели")
            .addLine("1234567890", "shop-sku-1", "title1", "category", "1", "Россия", "https://yandex.ru/123")
            .addLine("1234567890", "shop-sku-2", "title1", "category", "2", "Китай", "https://yandex.ru/12345")
            .build();

        modelStorageCachingService
            .addModel(new Model().setId(1)
                .setCategoryId(1)
                .setModelType(Model.ModelType.SKU)
                .setPublishedOnBlueMarket(true))
            .addModel(new Model().setId(2)
                .setCategoryId(2)
                .setModelType(Model.ModelType.SKU)
                .setPublishedOnBlueMarket(true));

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isEmpty();

        List<Offer> offers = offerRepository.findOffersByBusinessSkuKeys(Arrays.asList(
            new BusinessSkuKey(OfferTestUtils.TEST_SUPPLIER_ID, "shop-sku-1"),
            new BusinessSkuKey(OfferTestUtils.TEST_SUPPLIER_ID, "shop-sku-2")));

        assertThat(offers).hasSize(2);
        offers.forEach(offer -> {
            assertThat(offer.hasSupplierSkuMapping()).isTrue();
            assertThat(offer.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.ACCEPTED);
            assertThat(offer.hasApprovedSkuMapping()).isTrue();
            assertThat(offer.getBindingKind()).isEqualTo(Offer.BindingKind.SUGGESTED);
            assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
            assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
        });
    }

    @Test
    public void testSupplierMappingsForPsku10() {
        Offer offer = OfferTestUtils.simpleOffer();
        offerRepository.insertOffer(offer);

        supplierRepository.insert(new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "test")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setNewContentPipeline(true));
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "sku маркета", "Страна производства",
                "Страница модели")
            .addLine("1234567890", OfferTestUtils.DEFAULT_SHOP_SKU, "title1", "category", "2", "Россия", "https://yandex.ru/123")
            .build();

        modelStorageCachingService
            .addModel(new Model()
                .setId(1)
                .setCategoryId(1)
                .setSupplierId((long) OfferTestUtils.TEST_SUPPLIER_ID)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER)
                .setPublishedOnBlueMarket(true))
            .addModel(new Model()
                .setId(2)
                .setCategoryId(2)
                .setSupplierId((long) OfferTestUtils.TEST_SUPPLIER_ID)
                .setModelType(Model.ModelType.PARTNER_SKU)
                .setModelQuality(Model.ModelQuality.PARTNER)
                .setPublishedOnBlueMarket(true));

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isEmpty();

        List<Offer> offers = offerRepository.findOffersByBusinessSkuKeys(
            List.of(new BusinessSkuKey(OfferTestUtils.TEST_SUPPLIER_ID, OfferTestUtils.DEFAULT_SHOP_SKU)));

        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).hasSupplierSkuMapping()).isFalse();
    }

    @Test
    public void testSupplierMappingsForFastSku() {
        Offer offer = OfferTestUtils.simpleOffer();
        offerRepository.insertOffer(offer);

        supplierRepository.insert(new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "test")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setNewContentPipeline(true));
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "sku маркета", "Страна производства",
                "Страница модели")
            .addLine("1234567890", OfferTestUtils.DEFAULT_SHOP_SKU, "title1", "category", "1", "Россия", "https://yandex.ru/123")
            .build();

        modelStorageCachingService
            .addModel(new Model()
                .setId(1)
                .setCategoryId(1)
                .setSupplierId((long) OfferTestUtils.TEST_SUPPLIER_ID)
                .setModelType(Model.ModelType.FAST_SKU)
                .setPublishedOnBlueMarket(true));

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).isEmpty();

        List<Offer> offers = offerRepository.findOffersByBusinessSkuKeys(
            List.of(new BusinessSkuKey(OfferTestUtils.TEST_SUPPLIER_ID, OfferTestUtils.DEFAULT_SHOP_SKU)));

        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).hasSupplierSkuMapping()).isFalse();
    }

    @Test
    public void testSupplierMappingsForPskuForNewOfferAllowPsku2() {
        supplierRepository.insert(new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "test")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setNewContentPipeline(true));
        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "sku маркета", "Страна производства",
                "Страница модели")
            .addLine("1234567890", OfferTestUtils.DEFAULT_SHOP_SKU, "title1", "category", "2", "Россия", "https://yandex.ru/123")
            .build();

        modelStorageCachingService
            .addModel(new Model()
                .setId(1)
                .setCategoryId(1)
                .setSupplierId((long) OfferTestUtils.TEST_SUPPLIER_ID)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER)
                .setPublishedOnBlueMarket(true))
            .addModel(new Model()
                .setId(2)
                .setCategoryId(2)
                .setSupplierId((long) OfferTestUtils.TEST_SUPPLIER_ID)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER)
                .setPublishedOnBlueMarket(true));

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(PARTIAL), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);

        assertThat(result.getErrors()).hasSize(0);

        List<Offer> offers = offerRepository.findOffersByBusinessSkuKeys(
            List.of(new BusinessSkuKey(OfferTestUtils.TEST_SUPPLIER_ID, OfferTestUtils.DEFAULT_SHOP_SKU)));

        assertThat(offers).hasSize(1);
        offers.forEach(offer -> {
            assertThat(offer.hasSupplierSkuMapping()).isTrue();
            assertThat(offer.getSupplierSkuMappingStatus()).isEqualTo(Offer.MappingStatus.NEW);
        });
    }

    @Test
    public void testNotAllowDatacampSuppliers() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setId(SUPPLIER_ID_DATACAMP).setDatacamp(true);
        Offer offer = OfferTestUtils.simpleOffer(supplier);
        offerRepository.insertOffer(offer);

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "sku", "название товара", "категория", "Страна производства", "Ссылка на страницу модели")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "https://some.site.ru/123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), SUPPLIER_ID_DATACAMP, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(1);
        Assert.equals("mboc.error.excel-unknown-error", result.getErrors().get(0).getErrorInfo().getErrorCode());
    }

    @Test
    public void testDoesNotRequiredAtLeastOneGtinBarcodeDropship() {
        initForBarcodeValidation(OfferTestUtils.dropshipSupplier());

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели", "Штрихкод")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "http://other.site.com/tovar/vl2mv", "123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
    }

    @Test
    public void testDoesNotRequiredAtLeastOneBarcodeDropship() {
        initForBarcodeValidation(OfferTestUtils.dropshipSupplier());

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели", "Штрихкод")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "http://other.site.com/tovar/vl2mv", "")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
    }

    @Test
    public void testDoesNotRequiredAtLeastOneGtinBarcodeDropshipBySeller() {
        initForBarcodeValidation(OfferTestUtils.dropshipBySellerSupplier());

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели", "Штрихкод")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "http://other.site.com/tovar/vl2mv", "123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
    }

    @Test
    public void testDoesNotRequiredAtLeastOneBarcodeDropshipBySeller() {
        initForBarcodeValidation(OfferTestUtils.dropshipBySellerSupplier());

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели", "Штрихкод")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "http://other.site.com/tovar/vl2mv", "")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
    }

    @Test
    public void testDoesNotRequiredAtLeastOneGtinBarcodeClickAndCollect() {
        initForBarcodeValidation(OfferTestUtils.clickAndCollectSupplier());

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели", "Штрихкод")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "http://other.site.com/tovar/vl2mv", "123")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
    }

    @Test
    public void testDoesNotRequiredAtLeastOneBarcodeClickAndCollect() {
        initForBarcodeValidation(OfferTestUtils.clickAndCollectSupplier());

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели", "Штрихкод")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "http://other.site.com/tovar/vl2mv", "")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
    }

    @Test
    public void testRequiredAtLeastOneGtinBarcodeFulfillmentWithoutErrors() {
        initForBarcodeValidation(OfferTestUtils.fulfillmentSupplier());

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели", "Штрихкод")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "http://other.site.com/tovar/vl2mv",
                "899121530013513485")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
    }

    @Test
    public void testRequiredAtLeastOneGtinBarcodeCrossdockWithoutErrors() {
        initForBarcodeValidation(OfferTestUtils.crossdockSupplier());

        ExcelFile excelFile = ExcelFile.Builder
            .withHeaders("Код ТН ВЭД", "shop_sku", "название товара", "категория", "Страна производства",
                "Ссылка на страницу модели", "Штрихкод")
            .addLine("1234567890", "shop-sku", "changed-title", "category", "Китай", "http://other.site.com/tovar/vl2mv",
                "899121530013513485")
            .build();

        ImportResult result = importExcelService.parseExcel(
            excelFile, new ImportSettings(ALL_OR_NOTHING), OfferTestUtils.TEST_SUPPLIER_ID, "test", NULL_CONSUMER);
        assertThat(result.getErrors()).hasSize(0);
    }

    private void initForBarcodeValidation(Supplier supplier) {
        initForBarcodeValidation(supplier, 1);
    }

    private void initForBarcodeValidation(Supplier supplier, Integer vendorId) {
        Offer offer = OfferTestUtils.simpleOffer();
        offer.setVendorId(vendorId);
        offerRepository.insertOffer(offer);
        supplierRepository.delete(supplier);
        supplierRepository.insert(supplier);
    }
}
