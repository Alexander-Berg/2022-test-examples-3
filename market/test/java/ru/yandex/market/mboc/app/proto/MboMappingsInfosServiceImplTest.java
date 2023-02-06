package ru.yandex.market.mboc.app.proto;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.mapping.RecheckMappingService;
import ru.yandex.market.mboc.app.offers.OfferProtoConverter;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.datacamp.service.converter.DataCampConverterService;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.dict.WarehouseServiceAuditRecorder;
import ru.yandex.market.mboc.common.dict.WarehouseServiceRepository;
import ru.yandex.market.mboc.common.dict.WarehouseServiceService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.Offer1PService;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferMetaRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.OfferStatService;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.antimapping.AntiMappingService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.proto.AddProductInfoHelperService;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.proto.MboMappingsHelperService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.http.MboMappings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author s-ermakov
 */
public class MboMappingsInfosServiceImplTest extends BaseMbocAppTest {

    private static final int BERU_ID = 465852;
    private OfferRepositoryMock offerRepository;
    private OfferRepositoryMock slaveOfferRepository;
    private SupplierRepositoryMock supplierRepositoryMock;
    private AddProductInfoHelperService addProductInfoHelperService;
    private MboMappingsServiceImpl service;
    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;
    private MboMappingsHelperService mboMappingsHelperService;
    private BusinessSupplierService businessSupplierService;
    private SupplierService supplierService;
    private StorageKeyValueServiceMock storageKeyValueService;
    private MigrationService migrationService;
    private AntiMappingService antiMappingService;
    private CategoryKnowledgeServiceMock categoryKnowledgeServiceMock;
    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;
    private WarehouseServiceService warehouseServiceService;
    private DataCampConverterService dataCampConverterService;
    private OfferMappingActionService offerMappingActionService;

    @Autowired
    private MigrationStatusRepository migrationStatusRepository;
    @Autowired
    protected MigrationOfferRepository migrationOfferRepository;
    @Autowired
    protected MigrationRemovedOfferRepository migrationRemovedOfferRepository;
    @Autowired
    protected MskuRepository mskuRepository;
    @Autowired
    protected AntiMappingRepository antiMappingRepository;
    @Autowired
    private OfferMetaRepository offerMetaRepository;
    @Autowired
    protected OfferUpdateSequenceService offerUpdateSequenceService;
    @Autowired
    protected OfferBatchProcessor offerBatchProcessor;
    @Autowired
    protected ContentProcessingQueueRepository contentProcessingQueue;
    @Autowired
    protected WarehouseServiceRepository warehouseServiceRepository;

    @Before
    public void setup() {
        migrationService = new MigrationService(migrationStatusRepository,
            migrationOfferRepository, migrationRemovedOfferRepository,
            supplierRepositoryMock, offerUpdateSequenceService, offerMetaRepository);
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);

        offerRepository = new OfferRepositoryMock();
        slaveOfferRepository = offerRepository;
        supplierRepositoryMock = new SupplierRepositoryMock();
        addProductInfoHelperService = Mockito.mock(AddProductInfoHelperService.class);
        businessSupplierService = new BusinessSupplierService(supplierRepositoryMock, offerRepository);
        mboMappingsHelperService = new MboMappingsHelperService(supplierRepositoryMock,
            masterDataHelperService, businessSupplierService, BERU_ID);
        supplierService = new SupplierService(supplierRepositoryMock);
        antiMappingService = new AntiMappingService(antiMappingRepository, transactionHelper);

        Mockito.when(addProductInfoHelperService.addProductInfo(
                any(MboMappings.ProviderProductInfoRequest.class),
                any(AddProductInfoHelperService.AddProductInfoContext.class))
            )
            .then(answer -> {
                MboMappings.ProviderProductInfoRequest request = answer.getArgument(0);

                List<MboMappings.ProviderProductInfoResponse.ProductResult> results =
                    request.getProviderProductInfoList()
                        .stream()
                        .map(info -> MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder().build())
                        .collect(Collectors.toList());

                return MboMappings.ProviderProductInfoResponse.newBuilder()
                    .addAllResults(results)
                    .setMessage("From helper service")
                    .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                    .build();
            });

        SupplierRepositoryMock supplierRepository = new SupplierRepositoryMock();
        OfferProtoConverter protoConverter = new OfferProtoConverter(
            new CategoryCachingServiceMock(), Mockito.mock(OfferCategoryRestrictionCalculator.class), null, BERU_ID);
        Offer1PService offer1PService = new Offer1PService(BERU_ID, supplierRepository);

        CategoryCachingServiceMock categoryCachingService = new CategoryCachingServiceMock().enableAuto();
        storageKeyValueService = new StorageKeyValueServiceMock();

        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        categoryKnowledgeServiceMock = new CategoryKnowledgeServiceMock();
        modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingServiceMock,
            offerBatchProcessor, supplierRepository);
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(
            needContentStatusService, null, offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var offersProcessingStatusService = new OffersProcessingStatusService(null, needContentStatusService,
            supplierService, categoryKnowledgeServiceMock, retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator, storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);

        warehouseServiceService = new WarehouseServiceService(
            warehouseServiceRepository,
            offerRepository,
            supplierRepository,
            mskuRepository,
            Mockito.mock(WarehouseServiceAuditRecorder.class)
        );

        dataCampConverterService = new DataCampConverterService(
            Mockito.mock(DataCampIdentifiersService.class),
            Mockito.mock(OfferCategoryRestrictionCalculator.class),
            storageKeyValueService,
            true
        );

        service = new MboMappingsServiceImpl(
            offerRepository,
            slaveOfferRepository, addProductInfoHelperService,
            protoConverter,
            supplierRepository,
            offer1PService,
            new ModelStorageCachingServiceMock(),
            Mockito.mock(ComplexMonitoring.class),
            categoryCachingService,
            null,
            BERU_ID, Mockito.mock(OfferStatService.class),
            mboMappingsHelperService,
            businessSupplierService,
            supplierService,
            storageKeyValueService,
            null,
            migrationService,
            antiMappingRepository,
            antiMappingService,
            offersProcessingStatusService,
            contentProcessingQueue,
            transactionHelper,
            warehouseServiceService,
            dataCampConverterService,
            this.offerMappingActionService,
            new RecheckMappingService(offerRepository,
                offerMappingActionService,
                offersProcessingStatusService,
                transactionHelper,
                antiMappingService));
    }

    @Test
    public void testAddProductInfo() {
        MboMappings.ProviderProductInfoRequest request = MboMappings.ProviderProductInfoRequest.newBuilder()
            .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                .setMappingType(MboMappings.MappingType.SUPPLIER))
            .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                .setMappingType(MboMappings.MappingType.REAL_SUPPLIER))
            .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                .setMappingType(MboMappings.MappingType.PRICE_COMPARISION))
            .build();
        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(request);
        Assert.assertEquals("From helper service", response.getMessage());

        verify(addProductInfoHelperService, times(1))
            .addProductInfo(
                any(MboMappings.ProviderProductInfoRequest.class),
                any(AddProductInfoHelperService.AddProductInfoContext.class)
            );
    }

    @Test
    public void testAddProductInfoIfInRequestOnlyPriceComparisionInfos() {
        MboMappings.ProviderProductInfoRequest request = MboMappings.ProviderProductInfoRequest.newBuilder()
            .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                .setMappingType(MboMappings.MappingType.PRICE_COMPARISION))
            .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                .setMappingType(MboMappings.MappingType.PRICE_COMPARISION))
            .build();
        MboMappings.ProviderProductInfoResponse response = service.addProductInfo(request);
        Assert.assertEquals("From helper service", response.getMessage());

        verify(addProductInfoHelperService, times(1))
            .addProductInfo(
                any(MboMappings.ProviderProductInfoRequest.class),
                any(AddProductInfoHelperService.AddProductInfoContext.class)
            );
    }

    /**
     * Тест проверяет, что корректно выставилось поле AcceptedStatus, если мы проксируем вызов ручки к UC.
     */
    @Ignore("Пока что мы ничего не выставляем на практике, т.е. всё идёт как APPROVED, " +
        "после включения белых мапингов надо будет пересмотреть.")
    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void testCorrectSetOfAcceptStatusIfWeProxyDataToUc() {
        MboMappings.ProviderProductInfoRequest request1 = MboMappings.ProviderProductInfoRequest.newBuilder()
            .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                .setTitle("Title1")
                .setMappingType(MboMappings.MappingType.PRICE_COMPARISION))
            .setRequestInfo(MboMappings.ProductUpdateRequestInfo.newBuilder()
                .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.CONTENT)
                .build())
            .build();

        MboMappings.ProviderProductInfoRequest request2 = MboMappings.ProviderProductInfoRequest.newBuilder()
            .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                .setTitle("Title2")
                .setMappingType(MboMappings.MappingType.PRICE_COMPARISION))
            .setRequestInfo(MboMappings.ProductUpdateRequestInfo.newBuilder()
                .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER)
                .build())
            .build();

        MboMappings.ProviderProductInfoRequest request3 = MboMappings.ProviderProductInfoRequest.newBuilder()
            .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                .setTitle("Title3")
                .setMappingType(MboMappings.MappingType.PRICE_COMPARISION))
            .setRequestInfo(MboMappings.ProductUpdateRequestInfo.newBuilder()
                .build())
            .build();

        service.addProductInfo(request1);
        service.addProductInfo(request2);
        service.addProductInfo(request3);

        ArgumentCaptor<List<MboMappings.ProviderProductInfo>> captor
            = ArgumentCaptor.forClass(List.class);

        List<List<MboMappings.ProviderProductInfo>> value = captor.getAllValues();
        MboMappings.ProviderProductInfo value1 = value.get(0).get(0);
        MboMappings.ProviderProductInfo value2 = value.get(1).get(0);
        MboMappings.ProviderProductInfo value3 = value.get(2).get(0);

        // assert
//        Assert.assertEquals(MboMappings.ApprovedStatus.APPROVED, value1.getApprovedStatus());
//        Assert.assertEquals(MboMappings.ApprovedStatus.NOT_APPROVED, value2.getApprovedStatus());
//        Assert.assertEquals(MboMappings.ApprovedStatus.NOT_APPROVED, value3.getApprovedStatus());
    }
}
