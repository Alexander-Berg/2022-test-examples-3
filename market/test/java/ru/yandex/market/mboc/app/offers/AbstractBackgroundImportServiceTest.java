package ru.yandex.market.mboc.app.offers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.common.io.ByteStreams;
import org.apache.commons.collections.ListUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.mboc.app.importexcel.BackgroundImportService;
import ru.yandex.market.mboc.common.TransactionTemplateMock;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionRepositoryMock;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionServiceImpl;
import ru.yandex.market.mboc.common.dict.stockstorage.MbocStockRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.parsing.ImportedOfferToMasterDataConverter;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataParsingConfig;
import ru.yandex.market.mboc.common.masterdata.services.category.MboTimeUnitAliasesService;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.ExcelS3ServiceMock;
import ru.yandex.market.mboc.common.offers.ImportedOffer;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.MasterDataRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.converter.YmlFileToImportedOfferConverter;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
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
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.mboc.common.masterdata.parsing.MasterDataParsingConfig.CUSTOMS_COMMODITY_CODE_REQUIRED_KEY;

@SuppressWarnings("checkstyle:magicnumber")
public abstract class AbstractBackgroundImportServiceTest {
    protected static final int SUPPLIER_ID = OfferTestUtils.TEST_SUPPLIER_ID;
    protected static final int SEED = 146;
    protected static final String COUNTRY_FROM_CORRECT_FILE = "Россия";
    protected static final String TEST_SSKU = "test_ssku";
    protected static final String SSKU_FROM_FILE = "12222";
    protected static final String SSKU_FROM_FILE_2 = "12223";
    protected static final String DESCRIPTION = "some text";
    protected static final String LOGIN = "test";
    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();
    protected BackgroundActionServiceImpl backgroundActionService;
    protected BackgroundImportService backgroundImportService;
    protected OffersEnrichmentService enrichmentService;
    protected BackgroundActionRepositoryMock actionRepository;
    protected OfferRepositoryMock offerRepository;
    protected MasterDataRepositoryMock masterDataRepositoryMock;
    protected SupplierRepositoryMock supplierRepository;
    protected BusinessSupplierService businessSupplierService;
    protected TransformImportOffersService transformImportOffersService;

    protected Random random = new Random(SEED);
    protected MasterDataParsingConfig masterDataParsingConfig;
    protected MboTimeUnitAliasesService mboTimeUnitAliasesService;
    protected ImportedOfferToMasterDataConverter importedOfferToMasterDataConverter;
    protected MasterDataHelperService masterDataHelperService;
    @Mock
    protected ApplySettingsService applySettingsService;
    ImportExcelService importExcelService;
    ImportOffersProcessService importOffersProcessService;
    ImportFileService importFileService;
    MbocStockRepository mbocStockRepository;
    private ImportYmlService importYmlService;

    @Before
    public void setup() {
        actionRepository = new BackgroundActionRepositoryMock();
        backgroundActionService = new BackgroundActionServiceImpl(
            actionRepository, new TransactionTemplateMock(), 1);
        backgroundActionService.init();
        enrichmentService = mock(OffersEnrichmentService.class);
        masterDataHelperService = mock(MasterDataHelperService.class);
        offerRepository = new OfferRepositoryMock();
        masterDataRepositoryMock = new MasterDataRepositoryMock();

        supplierRepository = new SupplierRepositoryMock();
        mbocStockRepository = Mockito.mock(MbocStockRepository.class);

        Mockito.doAnswer(call -> {
                Collection<MasterData> masterData = call.getArgument(0);
                Map<ShopSkuKey, List<ErrorInfo>> result = masterData.stream()
                    .collect(Collectors.toMap(md -> md.getShopSkuKey(), md -> new ArrayList(), ListUtils::union));
                return result;
            })
            .when(masterDataHelperService).saveSskuMasterDataAndDocuments(Mockito.any());

        Mockito.doAnswer(call -> {
                Collection<MasterData> masterData = call.getArgument(0);
                Map<ShopSkuKey, List<ErrorInfo>> result = masterData.stream()
                    .collect(Collectors.toMap(MasterData::getShopSkuKey, md -> new ArrayList(), ListUtils::union));
                return result;
            })
            .when(masterDataHelperService).validateMasterData(any());

        var categoryCachingServiceMock = new CategoryCachingServiceMock();
        OffersToExcelFileConverterConfig config =
            new OffersToExcelFileConverterConfig(categoryCachingServiceMock);
        mboTimeUnitAliasesService = Mockito.mock(MboTimeUnitAliasesService.class);
        var storageKeyValueServiceMock = new StorageKeyValueServiceMock();
        masterDataParsingConfig = new MasterDataParsingConfig(mboTimeUnitAliasesService, storageKeyValueServiceMock);
        importedOfferToMasterDataConverter = new ImportedOfferToMasterDataConverter(masterDataParsingConfig,
            masterDataHelperService);

        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        businessSupplierService =
            new BusinessSupplierService(supplierRepository, offerRepository);

        var supplierService = new SupplierService(supplierRepository);
        var needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        var categoryKnowledgeServiceMock = new CategoryKnowledgeServiceMock();
        var modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingServiceMock, null,
            supplierRepository);
        var categoryInfoRepositoryMock = new CategoryInfoRepositoryMock(new MboUsersRepositoryMock());
        var categoryInfoCache = new CategoryInfoCacheImpl(categoryInfoRepositoryMock);
        var keyValueService = new StorageKeyValueServiceMock();
        var offerDestinationCalculator = new ContextedOfferDestinationCalculator(categoryInfoCache,
            keyValueService);
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService, null,
            offerDestinationCalculator, new StorageKeyValueServiceMock());
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var antiMappingRepositoryMock = new AntiMappingRepositoryMock();
        var offersProcessingStatusService = new OffersProcessingStatusService(null, needContentStatusService,
            supplierService, categoryKnowledgeServiceMock, retrieveMappingSkuTypeService, offerMappingActionService,
            categoryInfoRepositoryMock, antiMappingRepositoryMock, offerDestinationCalculator,
            keyValueService, new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);

        transformImportOffersService = new TransformImportOffersService(offerMappingActionService,
            offersProcessingStatusService, offerDestinationCalculator);

        importOffersProcessService = new ImportOffersProcessService(
            offerRepository,
            masterDataRepositoryMock,
            masterDataHelperService,
            enrichmentService,
            TransactionHelper.MOCK,
            importedOfferToMasterDataConverter,
            supplierRepository,
            applySettingsService,
            Mockito.mock(OffersProcessingStatusService.class),
            transformImportOffersService,
            Mockito.mock(GlobalVendorsCachingService.class),
            false,
            keyValueService);

        importExcelService = new ImportExcelService(
            config.importedExcelFileConverter(modelStorageCachingServiceMock, mboTimeUnitAliasesService,
                supplierRepository, storageKeyValueServiceMock),
            new ExcelS3ServiceMock(),
            importOffersProcessService
        );

        importYmlService = new ImportYmlService(new YmlFileToImportedOfferConverter(), importOffersProcessService);

        importFileService = new ImportFileService(importExcelService, importYmlService);

        backgroundImportService = new BackgroundImportService(backgroundActionService);

        storageKeyValueServiceMock.putValue(CUSTOMS_COMMODITY_CODE_REQUIRED_KEY, true);
    }

    @After
    public void tearDown() throws InterruptedException {
        backgroundActionService.stop();
    }

    byte[] readResource(String fileName) throws IOException {
        return ByteStreams.toByteArray(
            getClass().getClassLoader().getResourceAsStream(fileName));
    }

    MasterData generateMasterData(ImportedOffer importedOffer) {
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(importedOffer.getShopSkuKey());
        return masterData;
    }

    ImportedOffer generateOffer() {
        LocalDate startDate = LocalDate.now().minusDays(50);
        LocalDate endDate = LocalDate.now().plusDays(50);

        ImportedOffer offer = new ImportedOffer();
        offer.setSupplierId(SUPPLIER_ID);
        offer.setShopSkuId(TEST_SSKU);
        masterDataParsingConfig.getAllHeaders().forEach(header -> {
            offer.setMasterData(header, String.valueOf(random.nextInt()));
        });

        offer.setMasterData(ExcelHeaders.SUPPLY_SCHEDULE, "пн,вт");
        offer.setMasterData(ExcelHeaders.NDS, "18%");
        offer.setMasterData(ExcelHeaders.DOCUMENT_TYPE, "Сертификат соответствия");
        offer.setMasterData(ExcelHeaders.DOCUMENT_START_DATE, startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        offer.setMasterData(ExcelHeaders.DOCUMENT_END_DATE, endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        offer.setMasterData(ExcelHeaders.DOCUMENT_PICTURE, "http://test.url.com/some_picture");

        return offer;
    }

    protected Offer nextOffer(ShopSkuKey shopSkuKey) {
        return new Offer()
            .setBusinessId(shopSkuKey.getSupplierId()).setShopSku(shopSkuKey.getShopSku())
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.simpleSupplier());
    }

    protected Offer nextOffer(int businessId, String shopSku) {
        return new Offer()
            .setBusinessId(businessId).setShopSku(shopSku)
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.simpleSupplier());
    }
}
