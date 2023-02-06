package ru.yandex.market.mboc.app.proto.mdm;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.app.proto.MbocCommonMessageUtils;
import ru.yandex.market.mboc.app.proto.mdm.pictures.DocumentPictureProcessingResult;
import ru.yandex.market.mboc.app.proto.mdm.pictures.DocumentProtoPictureConverter;
import ru.yandex.market.mboc.app.proto.mdm.pictures.SupplierDocumentPicturesProcessor;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.DocumentProtoConverter;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.MbocBaseProtoConverter;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.model.metadata.MdmSource;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.document.DocumentFilter;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.masterdata.services.document.AvatarImageDepotServiceMock;
import ru.yandex.market.mboc.common.masterdata.services.document.DocumentOfferFilter;
import ru.yandex.market.mboc.common.masterdata.services.document.DocumentService;
import ru.yandex.market.mboc.common.masterdata.services.document.DocumentServiceImpl;
import ru.yandex.market.mboc.common.masterdata.services.document.picture.ByteArrayMultipartFile;
import ru.yandex.market.mboc.common.masterdata.services.document.picture.QualityDocumentPictureService;
import ru.yandex.market.mboc.common.masterdata.services.document.picture.QualityDocumentPictureServiceImpl;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.MboMappingsServiceMock;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mdm.http.MdmDocument;
import ru.yandex.market.mdm.http.MdmDocument.AddSupplierDocumentsRequest.DocumentAddition;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class SupplierDocumentServiceImplTest extends MdmBaseDbTestClass {

    private static final long SEED = 1337L;
    private static final int SUPPLIER_ID = 917765;
    private static final int BUSINESS_ID = 425467;
    private static final int OTHER_SUPPLIER_ID = 567719;
    private static final String CORRECT_SCAN_FILE_URL = "http://lolkek";
    private static final String CORRECT_SCAN_FILE_NAME = "4k.jpg";
    private static final String ANY_SHOP_SKU = "any";
    @Autowired
    JdbcTemplate jdbcTemplate;
    private MboMappingsServiceMock mboMappingsService;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private QualityDocumentRepository qualityDocumentRepository;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private MdmSupplierCachingService supplierCachingService;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    private MboMappings.ApprovedMappingInfo mapping;
    private EnhancedRandom qualityDocumentsRandom;
    private EnhancedRandom defaultRandom;

    private DocumentService documentService;
    private SupplierDocumentServiceImpl supplierDocumentService;


    private SupplierDocumentPicturesProcessor documentPicturesProcessorMock;

    @Before
    public void setUpAndInsertTestDataToDB() {
        documentPicturesProcessorMock = Mockito.mock(SupplierDocumentPicturesProcessor.class);
        mboMappingsService = new MboMappingsServiceMock();

        documentService = Mockito.spy(new DocumentServiceImpl(
            mboMappingsService,
            qualityDocumentRepository,
            Mockito.mock(QualityDocumentPictureService.class),
            mdmQueuesManager,
            mdmSskuGroupManager));

        supplierDocumentService = Mockito.spy(new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService));

        defaultRandom = TestDataUtils.defaultRandom(SEED);
        qualityDocumentsRandom = TestDataUtils.qualityDocumentsRandom(defaultRandom.nextLong());
        mapping = addMappingToDB();
    }

    private MboMappings.ApprovedMappingInfo addMappingToDB() {
        return addMapping(o -> o.setSupplierId(SUPPLIER_ID));
    }

    private MboMappings.ApprovedMappingInfo addMapping(
        Consumer<MboMappings.ApprovedMappingInfo.Builder> mappingProcessor
    ) {
        MboMappings.ApprovedMappingInfo.Builder newMappingBuilder =
            TestDataUtils.generateCorrectApprovedMappingInfoBuilder(defaultRandom)
                .setSupplierId(SUPPLIER_ID);
        mappingProcessor.accept(newMappingBuilder);

        MboMappings.ApprovedMappingInfo result = newMappingBuilder.build();
        mboMappingsService.addMapping(result);
        return result;
    }

    private MasterData generateMasterData(MboMappings.ApprovedMappingInfo mapping, QualityDocument... documents) {
        return TestDataUtils.generateMasterData(
            mapping.getShopSku(),
            mapping.getSupplierId(),
            defaultRandom,
            documents
        );
    }

    private MasterData generateMasterData(QualityDocument... documents) {
        return generateMasterData(mapping, documents);
    }

    private QualityDocument generateDocument() {
        return TestDataUtils.generateDocument(qualityDocumentsRandom);
    }

    private QualityDocument generateValidDocument() {
        return TestDataUtils.generateCorrectDocument(qualityDocumentsRandom)
            .setMetadata(new QualityDocument.Metadata()
                .setSource(MdmSource.SUPPLIER)
                .setCreatedBy(SUPPLIER_ID));
    }

    private void prepareBusinessGroup() {
        MdmSupplier business = new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS);
        MdmSupplier service1 = new MdmSupplier().setId(SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true);
        MdmSupplier service2 = new MdmSupplier().setId(OTHER_SUPPLIER_ID).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true);
        mdmSupplierRepository.insertBatch(business, service1, service2);
        supplierCachingService.refresh();
        sskuExistenceRepository.markExistence(List.of(
            new ShopSkuKey(SUPPLIER_ID, ANY_SHOP_SKU),
            new ShopSkuKey(OTHER_SUPPLIER_ID, ANY_SHOP_SKU)
        ), true);
    }

    @Test
    public void whenSearchingDocumentsShouldPassCorrectParametersToInternalService() {
        QualityDocument document = generateDocument();
        MasterData offerMasterData = generateMasterData(document);
        int supplierId = offerMasterData.getSupplierId();
        qualityDocumentRepository.insertOrUpdateAll(Collections.singletonList(document));

        String searchQuery = "search";
        int limit = 1;
        String offsetKey = "13";
        ArgumentCaptor<DocumentOfferFilter> filterArgumentCaptor = ArgumentCaptor.forClass(DocumentOfferFilter.class);
        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService
            .findSupplierDocuments(MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(offerMasterData.getSupplierId())
                .setSearchQuery(searchQuery)
                .setLimit(limit)
                .setOffsetKey(offsetKey)
                .build());

        Mockito.verify(documentService, Mockito.times(1)).findBy(filterArgumentCaptor.capture());
        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);
            softly.assertThat(protoResponse.hasStatusMessage()).isFalse();
            softly.assertThat(protoResponse.hasNextOffsetKey()).isFalse();

            DocumentFilter documentFilter = filterArgumentCaptor.getValue().getDocumentFilter();
            softly.assertThat(documentFilter).isNotNull();
            softly.assertThat(documentFilter.getSupplierDocSearchCriteria().getSupplierIds())
                .isEqualTo(Set.of(supplierId));
            softly.assertThat(documentFilter.getSupplierDocSearchCriteria().getSearchQuery()).isEqualTo(searchQuery);
            softly.assertThat(documentFilter.getLimit()).isEqualTo(limit);
            softly.assertThat(documentFilter.getIdOffsetKey()).isEqualTo(Long.valueOf(offsetKey));
        });
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void whenDifferentDocumentsMatchDifferentCriteriaShouldFindAll() {
        Long mskuId = 123456L;
        MboMappings.ApprovedMappingInfo sskuMapping = addMapping(
            o -> o.setShopSku(mskuId + qualityDocumentsRandom.nextObject(String.class))
        );
        MboMappings.ApprovedMappingInfo mskuMapping = addMapping(
            o -> o.setMarketSkuId(mskuId)
        );
        MboMappings.ApprovedMappingInfo regNumberMapping = addMapping(
            o -> {
            }
        );

        QualityDocument sskuDocument = generateDocument();
        QualityDocument mskuDocument = generateDocument();
        QualityDocument regNumberDocument = generateDocument().setRegistrationNumber(mskuId + "_TEST");

        masterDataRepository.insertOrUpdateAll(Arrays.asList(
            generateMasterData(sskuMapping, sskuDocument),
            generateMasterData(mskuMapping, mskuDocument),
            generateMasterData(regNumberMapping, regNumberDocument),
            generateMasterData(addMappingToDB(), generateDocument())
        ));

        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService.findSupplierDocuments(
            MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setSearchQuery(mskuId.toString())
                .build()
        );

        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);

            List<QualityDocument> returnedDocuments = protoResponse.getDocumentList().stream()
                .map(DocumentProtoConverter::createQualityDocument)
                .collect(Collectors.toList());

            softly.assertThat(returnedDocuments)
                .containsExactlyInAnyOrder(sskuDocument.setId(0), mskuDocument.setId(0), regNumberDocument.setId(0));
        });

    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void whenMskuOfferOfOtherSupplierShouldNotFindRelatedDocument() {
        Long mskuId = 123456L;
        MboMappings.ApprovedMappingInfo mskuMapping = addMapping(
            o -> o.setMarketSkuId(mskuId)
        );
        QualityDocument mskuDocument = generateDocument();

        MboMappings.ApprovedMappingInfo otherSupplierMapping = addMapping(o -> o
            .setSupplierId(OTHER_SUPPLIER_ID)
            .setMarketSkuId(mskuId)
        );
        QualityDocument otherSupplierDocument = generateDocument();

        masterDataRepository.insertOrUpdateAll(Arrays.asList(
            generateMasterData(mskuMapping, mskuDocument),
            generateMasterData(otherSupplierMapping, otherSupplierDocument)
        ));

        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService.findSupplierDocuments(
            MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(this.SUPPLIER_ID)
                .setSearchQuery(mskuId.toString())
                .build()
        );

        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);

            List<QualityDocument> returnedDocuments = protoResponse.getDocumentList().stream()
                .map(DocumentProtoConverter::createQualityDocument)
                .collect(Collectors.toList());

            softly.assertThat(returnedDocuments)
                .containsExactlyInAnyOrder(mskuDocument.setId(0));
        });

    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void whenNonMskuQueryShouldFindBySskuAndNumber() {
        String nonMskuSearchQuery = "test_query";
        MboMappings.ApprovedMappingInfo sskuMapping = addMapping(
            o -> o.setShopSku(nonMskuSearchQuery + qualityDocumentsRandom.nextObject(String.class))
        );
        MboMappings.ApprovedMappingInfo mskuMapping = addMapping(
            o -> o.setMarketSkuId(2345678L)
        );
        MboMappings.ApprovedMappingInfo regNumberMapping = addMapping(
            o -> {
            }
        );

        QualityDocument sskuDocument = generateDocument();
        QualityDocument regNumberDocument = generateDocument().setRegistrationNumber(nonMskuSearchQuery + "_TEST");

        masterDataRepository.insertOrUpdateAll(Arrays.asList(
            generateMasterData(sskuMapping, sskuDocument),
            generateMasterData(regNumberMapping, regNumberDocument),
            generateMasterData(mskuMapping, generateDocument())
        ));

        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService.findSupplierDocuments(
            MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setSearchQuery(nonMskuSearchQuery)
                .build()
        );

        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);

            List<QualityDocument> returnedDocuments = protoResponse.getDocumentList().stream()
                .map(DocumentProtoConverter::createQualityDocument)
                .collect(Collectors.toList());

            softly.assertThat(returnedDocuments)
                .containsExactlyInAnyOrder(sskuDocument.setId(0), regNumberDocument.setId(0));
        });

    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void whenOnlyMskuMatchesShouldFindOnlyMskuRelatedSupplierDocuments() {
        Long mskuId = 123456L;
        MboMappings.ApprovedMappingInfo sskuMapping = addMapping(
            o -> o.setShopSku(qualityDocumentsRandom.nextObject(String.class))
        );
        MboMappings.ApprovedMappingInfo mskuMapping = addMapping(
            o -> o.setMarketSkuId(mskuId)
        );

        QualityDocument sskuDocument = generateDocument();
        QualityDocument mskuDocument = generateDocument();
        QualityDocument secondMskuDocument = generateDocument();

        masterDataRepository.insertOrUpdateAll(Arrays.asList(
            generateMasterData(sskuMapping, sskuDocument),
            generateMasterData(mskuMapping, mskuDocument, secondMskuDocument),
            generateMasterData(addMappingToDB(), generateDocument())
        ));

        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService.findSupplierDocuments(
            MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setSearchQuery(mskuId.toString())
                .build()
        );

        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);

            List<QualityDocument> returnedDocuments = protoResponse.getDocumentList().stream()
                .map(DocumentProtoConverter::createQualityDocument)
                .collect(Collectors.toList());

            softly.assertThat(returnedDocuments)
                .containsExactlyInAnyOrder(mskuDocument.setId(0), secondMskuDocument.setId(0));
        });

    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void whenNotFoundByNumericMskuShouldFindBySskuAndNumber() {
        String searchQuery = "12345";
        MboMappings.ApprovedMappingInfo sskuMapping = addMapping(
            o -> o.setShopSku(searchQuery + qualityDocumentsRandom.nextObject(String.class))
        );
        MboMappings.ApprovedMappingInfo otherMskuMapping = addMapping(
            o -> o.setMarketSkuId(2345678L)
        );
        MboMappings.ApprovedMappingInfo regNumberMapping = addMapping(
            o -> {
            }
        );

        QualityDocument sskuDocument = generateDocument();
        QualityDocument regNumberDocument = generateDocument().setRegistrationNumber(searchQuery + "_TEST");

        masterDataRepository.insertOrUpdateAll(Arrays.asList(
            generateMasterData(sskuMapping, sskuDocument),
            generateMasterData(regNumberMapping, regNumberDocument),
            generateMasterData(otherMskuMapping, generateDocument())
        ));

        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService.findSupplierDocuments(
            MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .setSearchQuery(searchQuery)
                .build()
        );

        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);

            List<QualityDocument> returnedDocuments = protoResponse.getDocumentList().stream()
                .map(DocumentProtoConverter::createQualityDocument)
                .collect(Collectors.toList());

            softly.assertThat(returnedDocuments)
                .containsExactlyInAnyOrder(sskuDocument.setId(0), regNumberDocument.setId(0));
        });

    }

    @Test
    public void whenSizeEqualsToLimitShouldReturnNextOffsetKey() {
        QualityDocument document1 = generateValidDocument();
        QualityDocument document2 = generateValidDocument();
        MasterData offerMasterData = generateMasterData(document1, document2);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        List<QualityDocument> inserted = qualityDocumentRepository
            .insertOrUpdateAll(Arrays.asList(document1, document2));

        int limit = 1;
        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService
            .findSupplierDocuments(MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(offerMasterData.getSupplierId())
                .setLimit(limit)
                .build());

        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);
            softly.assertThat(protoResponse.getDocumentList()).hasSize(limit);
            softly.assertThat(protoResponse.getNextOffsetKey())
                .isEqualTo(inserted.stream()
                    .map(QualityDocument::getId)
                    .min(Long::compareTo)
                    .get().toString());
        });
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void whenSizeIsLessThanLimitShouldReturnNullNextOffsetKey() {
        QualityDocument document1 = generateValidDocument();
        QualityDocument document2 = generateValidDocument();
        MasterData offerMasterData = generateMasterData(document1, document2);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        List<QualityDocument> inserted = qualityDocumentRepository
            .insertOrUpdateAll(Arrays.asList(document1, document2));

        int limit = 3;
        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService
            .findSupplierDocuments(MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(offerMasterData.getSupplierId())
                .setLimit(limit)
                .build());

        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);
            softly.assertThat(protoResponse.getDocumentList().size()).isEqualTo(inserted.size());
            softly.assertThat(protoResponse.hasNextOffsetKey()).isFalse();
        });
    }

    @Test
    public void whenCantParseOffsetKeyShouldReturnError() {
        String offsetKey = "Not a number";
        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService
            .findSupplierDocuments(MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(1)
                .setOffsetKey(offsetKey)
                .build());

        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.ERROR);
            softly.assertThat(protoResponse.getDocumentList()).hasSize(0);
            softly.assertThat(protoResponse.hasNextOffsetKey()).isFalse();

            ErrorInfo unknownError = MbocErrors.get()
                .protoUnknownError("offset_key не являетя целым числом:" + offsetKey);
            softly.assertThat(protoResponse.getStatusMessage().getMessageCode())
                .isEqualTo(unknownError.getErrorCode());
            softly.assertThat(protoResponse.getStatusMessage().getJsonDataForMustacheTemplate())
                .contains("offset_key не являетя целым числом:" + offsetKey);
        });
    }

    @Test
    public void whenDocumentServiceThrowsExceptionFindDocumentsShouldReturnError() {
        documentService = Mockito.mock(DocumentService.class);
        RuntimeException e = new RuntimeException("Failed to find documents.");
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        Mockito.when(documentService.findBy(Mockito.any()))
            .thenThrow(e);

        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService
            .findSupplierDocuments(MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(1)
                .build());

        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.ERROR);
            softly.assertThat(protoResponse.getDocumentList()).hasSize(0);
            softly.assertThat(protoResponse.hasNextOffsetKey()).isFalse();

            softly.assertThat(protoResponse.getStatusMessage()).isEqualTo(
                MbocCommonMessageUtils.errorInfoToMessage(MbocErrors.get().protoUnknownError(e.getMessage())));
        });
    }

    @Test
    public void whenDocumentServiceFindByThrowsExceptionReturnDocumentErrorResponse() throws Exception {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        Mockito.when(documentPicturesProcessorMock.updateDocumentAdditionPictures(
            Mockito.any(DocumentAddition.class), Mockito.isNull()))
            .thenReturn(new DocumentPictureProcessingResult(Collections.emptyList(), Collections.emptyList()));
        Mockito.when(documentService.findBy(Mockito.any()))
            .thenThrow(new RuntimeException("Failed"));

        MdmDocument.AddDocumentsResponse addDocumentsResponse = supplierDocumentService
            .addSupplierDocuments(MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                .setSupplierId(1)
                .addDocument(DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                    .addNewScanFile(DocumentAddition.ScanFile.newBuilder()
                        .setUrl(CORRECT_SCAN_FILE_URL)
                        .setFileName(CORRECT_SCAN_FILE_NAME))
                    .build())
                .build());

        assertSoftly(softly -> {
            softly.assertThat(addDocumentsResponse.getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
            softly.assertThat(addDocumentsResponse.getDocumentResponseList()).hasSize(1);

            MdmDocument.AddDocumentsResponse.DocumentResponse invalidFieldsError =
                addDocumentsResponse.getDocumentResponseList().get(0);
            softly.assertThat(invalidFieldsError.getStatus()).isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
            softly.assertThat(invalidFieldsError.getErrorList()).hasSize(1);

            String failedToFindQualityDocument = "DocumentService.findBy вернул ошибку:";
            MbocCommon.Message message = invalidFieldsError.getErrorList().get(0);
            softly.assertThat(message.getMessageCode())
                .isEqualTo(MbocErrors.get().qdAddSupplierDocumentsFailedToAddDocument(failedToFindQualityDocument)
                    .getErrorCode());
            softly.assertThat(message.getJsonDataForMustacheTemplate()).contains(failedToFindQualityDocument);
        });
    }

    @Test
    public void whenDocumentServiceSaveDocumentThrowsExceptionReturnDocumentErrorResponse() throws Exception {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        Mockito.when(documentPicturesProcessorMock.updateDocumentAdditionPictures(
            Mockito.any(DocumentAddition.class),
            Mockito.isNull()))
            .thenReturn(new DocumentPictureProcessingResult(Collections.emptyList(), Collections.emptyList()));
        Mockito.when(documentService.findBy(Mockito.any())).thenReturn(new ArrayList<>());
        Mockito.when(documentService.saveDocument(Mockito.any(QualityDocument.class), Mockito.any(MultipartFile.class)))
            .thenThrow(new RuntimeException("Failed"));

        MdmDocument.AddDocumentsResponse addDocumentsResponse = supplierDocumentService
            .addSupplierDocuments(MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                .setSupplierId(1)
                .addDocument(DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                    .addNewScanFile(DocumentAddition.ScanFile.newBuilder()
                        .setUrl(CORRECT_SCAN_FILE_URL)
                        .setFileName(CORRECT_SCAN_FILE_NAME))
                    .build())
                .build());

        assertSoftly(softly -> {
            softly.assertThat(addDocumentsResponse.getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
            softly.assertThat(addDocumentsResponse.getDocumentResponseList()).hasSize(1);

            MdmDocument.AddDocumentsResponse.DocumentResponse invalidFieldsError =
                addDocumentsResponse.getDocumentResponseList().get(0);
            softly.assertThat(invalidFieldsError.getStatus()).isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
            softly.assertThat(invalidFieldsError.getErrorList()).hasSize(1);

            String failedToSaveText = "DocumentService.saveDocument вернул ошибку:";
            MbocCommon.Message message = invalidFieldsError.getErrorList().get(0);
            softly.assertThat(message.getMessageCode())
                .isEqualTo(MbocErrors.get().qdAddSupplierDocumentsFailedToAddDocument(failedToSaveText)
                    .getErrorCode());
            softly.assertThat(message.getJsonDataForMustacheTemplate()).contains(failedToSaveText);
        });
    }

    @Test
    public void whenDocumentCreatedByOtherSupplierShouldReturnDocumentErrorResponse() throws Exception {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        Mockito.when(documentPicturesProcessorMock.updateDocumentAdditionPictures(
            Mockito.any(DocumentAddition.class),
            Mockito.any(QualityDocument.class)))
            .thenReturn(new DocumentPictureProcessingResult(Collections.emptyList(), Collections.emptyList()));
        QualityDocument otherSupplierDocument = generateValidDocument();
        otherSupplierDocument.getMetadata().setCreatedBy(mapping.getSupplierId() + 1);
        Mockito.when(documentService.findBy(Mockito.any())).thenReturn(
            Collections.singletonList(otherSupplierDocument));
        Mockito.when(documentService.saveDocument(Mockito.any(QualityDocument.class), Mockito.any(MultipartFile.class)))
            .thenThrow(new RuntimeException("Failed"));

        MdmDocument.AddDocumentsResponse addDocumentsResponse = supplierDocumentService
            .addSupplierDocuments(MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                .setSupplierId(1)
                .addDocument(DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                    .addNewScanFile(DocumentAddition.ScanFile.newBuilder()
                        .setUrl(CORRECT_SCAN_FILE_URL)
                        .setFileName(CORRECT_SCAN_FILE_NAME))
                    .build())
                .build());

        assertSoftly(softly -> {
            softly.assertThat(addDocumentsResponse.getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
            softly.assertThat(addDocumentsResponse.getDocumentResponseList()).hasSize(1);

            MdmDocument.AddDocumentsResponse.DocumentResponse invalidFieldsError =
                addDocumentsResponse.getDocumentResponseList().get(0);
            softly.assertThat(invalidFieldsError.getStatus()).isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
            softly.assertThat(invalidFieldsError.getErrorList()).hasSize(1);

            String otherSupplier = "Сертификат создан другим поставщиком и не может быть изменен.";
            MbocCommon.Message message = invalidFieldsError.getErrorList().get(0);
            softly.assertThat(message.getMessageCode())
                .isEqualTo(MbocErrors.get().qdAddSupplierDocumentsFailedToAddDocument(otherSupplier)
                    .getErrorCode());
            softly.assertThat(message.getJsonDataForMustacheTemplate()).contains(otherSupplier);
        });
    }

    @Test
    public void whenDocumentCanBeSuccessfullyUpdatedByOtherSupplierFromTheSameBusinessGroup() {
        prepareBusinessGroup();

        // Сохраняем документ от 1-го поставщика.
        QualityDocument validDocument = generateValidDocument();
        qualityDocumentRepository.insert(validDocument);
        Mockito.when(documentPicturesProcessorMock.updateDocumentAdditionPictures(
            Mockito.any(DocumentAddition.class),
            Mockito.any(QualityDocument.class)))
            .thenReturn(
                new DocumentPictureProcessingResult(Collections.singletonList("saved"), Collections.emptyList()));

        // Сохраняем тот же документ от 2-го поставщика.
        MdmDocument.AddDocumentsResponse updateDocumentsResponse = supplierDocumentService
            .addSupplierDocuments(MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                .setSupplierId(OTHER_SUPPLIER_ID)
                .addDocument(DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(validDocument))
                    .addNewScanFile(DocumentAddition.ScanFile.newBuilder()
                        .setUrl(CORRECT_SCAN_FILE_URL)
                        .setFileName(CORRECT_SCAN_FILE_NAME))
                    .build())
                .build());

        List<QualityDocument> repositoryDocuments = qualityDocumentRepository.findAll();
        assertSoftly(softly -> {
            softly.assertThat(repositoryDocuments).hasSize(1);
            softly.assertThat(updateDocumentsResponse.getStatus()).isEqualTo(MdmDocument.AddDocumentsResponse.Status.OK);
            softly.assertThat(updateDocumentsResponse.getDocumentResponseList()).hasSize(1);

            softly.assertThat(updateDocumentsResponse.getDocumentResponse(0).getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.OK);

            QualityDocument qualityDocument = repositoryDocuments.get(0);
            MdmDocument.Document document = updateDocumentsResponse.getDocumentResponse(0).getDocument();

            softly.assertThat(qualityDocument.getPictures()).hasSize(1);
            softly.assertThat(document.getPictureList())
                .containsExactlyInAnyOrderElementsOf(qualityDocument.getPictures());
        });
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void whenSomeDocumentsFailedShouldReturnErrorResponse() throws Exception {
        MdmDocument.AddDocumentsResponse addDocumentsResponse = supplierDocumentService
            .addSupplierDocuments(MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                .setSupplierId(1)
                .addDocument(DocumentAddition.newBuilder().build())
                .addDocument(DocumentAddition.newBuilder().build())
                .build());

        assertSoftly(softly -> {
            softly.assertThat(addDocumentsResponse.getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
            softly.assertThat(addDocumentsResponse.getError().getMessageCode())
                .isEqualTo(MbocErrors.get().qdAddSupplierDocumentsFailedDocumentsInResponse(2)
                    .getErrorCode());
            softly.assertThat(addDocumentsResponse.getDocumentResponseList()).hasSize(2);
        });
    }

    @Test
    public void whenAddingValidDocumentShouldSaveIt() throws Exception {
        Mockito.when(documentPicturesProcessorMock.updateDocumentAdditionPictures(
            Mockito.any(DocumentAddition.class),
            Mockito.isNull()))
            .thenReturn(
                new DocumentPictureProcessingResult(Collections.singletonList("saved"), Collections.emptyList()));

        MdmDocument.AddDocumentsResponse addDocumentsResponse = supplierDocumentService
            .addSupplierDocuments(MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .addDocument(DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                    .addNewScanFile(DocumentAddition.ScanFile.newBuilder()
                        .setUrl(CORRECT_SCAN_FILE_URL)
                        .setFileName(CORRECT_SCAN_FILE_NAME))
                    .build())
                .build());

        List<QualityDocument> repositoryDocuments = qualityDocumentRepository.findAll();
        assertSoftly(softly -> {
            softly.assertThat(repositoryDocuments).hasSize(1);
            softly.assertThat(addDocumentsResponse.getStatus()).isEqualTo(MdmDocument.AddDocumentsResponse.Status.OK);
            softly.assertThat(addDocumentsResponse.getDocumentResponseList()).hasSize(1);

            softly.assertThat(addDocumentsResponse.getDocumentResponse(0).getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.OK);

            QualityDocument qualityDocument = repositoryDocuments.get(0);
            MdmDocument.Document document = addDocumentsResponse.getDocumentResponse(0).getDocument();

            softly.assertThat(qualityDocument.getMetadata())
                .isEqualToIgnoringGivenFields(new QualityDocument.Metadata().setSource(MdmSource.SUPPLIER)
                    .setCreatedBy(SUPPLIER_ID), "lastUpdateDate");

            softly.assertThat(document.getRegistrationNumber()).isEqualTo(qualityDocument.getRegistrationNumber());
            softly.assertThat(document.getType())
                .isEqualTo(MdmDocument.Document.DocumentType.valueOf(qualityDocument.getType().name()));
            softly.assertThat(LocalDate.ofEpochDay(document.getStartDate()))
                .isEqualTo(qualityDocument.getStartDate());
            softly.assertThat(LocalDate.ofEpochDay(document.getEndDate()))
                .isEqualTo(qualityDocument.getEndDate());
            softly.assertThat(document.getPictureList())
                .containsExactly(qualityDocument.getPictures().toArray(new String[0]));
            softly.assertThat(document.getSerialNumber())
                .isEqualTo(qualityDocument.getSerialNumber());
            softly.assertThat(document.getRequirements())
                .isEqualTo(qualityDocument.getRequirements());
            softly.assertThat(document.getCertificationOrgRegNumber())
                .isEqualTo(qualityDocument.getCertificationOrgRegNumber());
        });
    }

    @Test
    public void whenAddingValidExemptionLetterShouldSaveItWithUpdatedRegNumber() throws Exception {
        Mockito.when(documentPicturesProcessorMock.updateDocumentAdditionPictures(
            Mockito.any(DocumentAddition.class),
            Mockito.isNull()))
            .thenReturn(
                new DocumentPictureProcessingResult(Collections.singletonList("saved"), Collections.emptyList()));

        String certificationOrgRegNumber = "0123456789";
        String registrationNumber = "reg12345";
        MdmDocument.AddDocumentsResponse addDocumentsResponse = supplierDocumentService
            .addSupplierDocuments(MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .addDocument(DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(
                        generateValidDocument()
                            .setType(QualityDocument.QualityDocumentType.EXEMPTION_LETTER)
                            .setCertificationOrgRegNumber(certificationOrgRegNumber)
                            .setRegistrationNumber(registrationNumber)))
                    .addNewScanFile(DocumentAddition.ScanFile.newBuilder()
                        .setUrl(CORRECT_SCAN_FILE_URL)
                        .setFileName(CORRECT_SCAN_FILE_NAME))
                    .build())
                .build());

        List<QualityDocument> repositoryDocuments = qualityDocumentRepository.findAll();
        assertSoftly(softly -> {
            softly.assertThat(repositoryDocuments).hasSize(1);
            softly.assertThat(addDocumentsResponse.getStatus()).isEqualTo(MdmDocument.AddDocumentsResponse.Status.OK);
            softly.assertThat(addDocumentsResponse.getDocumentResponseList()).hasSize(1);

            softly.assertThat(addDocumentsResponse.getDocumentResponse(0).getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.OK);

            QualityDocument repositoryDoc = repositoryDocuments.get(0);
            MdmDocument.Document responseDoc = addDocumentsResponse.getDocumentResponse(0).getDocument();

            softly.assertThat(repositoryDoc.getRegistrationNumber())
                .isEqualTo(certificationOrgRegNumber + "_" + registrationNumber);
            softly.assertThat(repositoryDoc.getCertificationOrgRegNumber())
                .isEqualTo(certificationOrgRegNumber);
            softly.assertThat(responseDoc.getRegistrationNumber())
                .isEqualTo(certificationOrgRegNumber + "_" + registrationNumber);
            softly.assertThat(responseDoc.getCertificationOrgRegNumber())
                .isEqualTo(certificationOrgRegNumber);
        });
    }

    @Test
    public void whenAddingValidWithExistingDocumentShouldSaveIt() throws Exception {
        QualityDocument validDocument = generateValidDocument();
        qualityDocumentRepository.insert(validDocument);
        Mockito.when(documentPicturesProcessorMock.updateDocumentAdditionPictures(
            Mockito.any(DocumentAddition.class),
            Mockito.any(QualityDocument.class)))
            .thenReturn(
                new DocumentPictureProcessingResult(Collections.singletonList("saved"), Collections.emptyList()));

        MdmDocument.AddDocumentsResponse addDocumentsResponse = supplierDocumentService
            .addSupplierDocuments(MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .addDocument(DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(validDocument))
                    .addNewScanFile(DocumentAddition.ScanFile.newBuilder()
                        .setUrl(CORRECT_SCAN_FILE_URL)
                        .setFileName(CORRECT_SCAN_FILE_NAME))
                    .build())
                .build());

        List<QualityDocument> repositoryDocuments = qualityDocumentRepository.findAll();
        assertSoftly(softly -> {
            softly.assertThat(repositoryDocuments).hasSize(1);
            softly.assertThat(addDocumentsResponse.getStatus()).isEqualTo(MdmDocument.AddDocumentsResponse.Status.OK);
            softly.assertThat(addDocumentsResponse.getDocumentResponseList()).hasSize(1);

            softly.assertThat(addDocumentsResponse.getDocumentResponse(0).getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.OK);

            QualityDocument qualityDocument = repositoryDocuments.get(0);
            MdmDocument.Document document = addDocumentsResponse.getDocumentResponse(0).getDocument();

            softly.assertThat(qualityDocument.getMetadata())
                .isEqualToIgnoringGivenFields(new QualityDocument.Metadata()
                    .setSource(MdmSource.SUPPLIER)
                    .setCreatedBy(SUPPLIER_ID), "lastUpdateDate");

            softly.assertThat(document.getRegistrationNumber()).isEqualTo(qualityDocument.getRegistrationNumber());
            softly.assertThat(document.getType())
                .isEqualTo(MdmDocument.Document.DocumentType.valueOf(qualityDocument.getType().name()));
            softly.assertThat(LocalDate.ofEpochDay(document.getStartDate()))
                .isEqualTo(qualityDocument.getStartDate());
            softly.assertThat(LocalDate.ofEpochDay(document.getEndDate()))
                .isEqualTo(qualityDocument.getEndDate());
            softly.assertThat(document.getPictureList())
                .containsExactly(qualityDocument.getPictures().toArray(new String[0]));
            softly.assertThat(document.getSerialNumber())
                .isEqualTo(qualityDocument.getSerialNumber());
            softly.assertThat(document.getRequirements())
                .isEqualTo(qualityDocument.getRequirements());
            softly.assertThat(document.getCertificationOrgRegNumber())
                .isEqualTo(qualityDocument.getCertificationOrgRegNumber());
        });
    }

    @Test
    public void whenSupplierDocumentPicturesProcessorReturnsErrorsShouldReturnErrorResponse() {
        Mockito.when(documentPicturesProcessorMock.updateDocumentAdditionPictures(
            Mockito.any(DocumentAddition.class), Mockito.isNull()))
            .thenCallRealMethod();

        MdmDocument.AddDocumentsResponse addDocumentsResponse = supplierDocumentService
            .addSupplierDocuments(MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .addDocument(DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(generateValidDocument()))
                    .addNewScanFile(DocumentAddition.ScanFile.newBuilder()
                        .setUrl("incorrect")
                        .setFileName("incorrect"))
                    .build())
                .build());

        assertSoftly(softly -> {
            softly.assertThat(addDocumentsResponse.getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
            softly.assertThat(addDocumentsResponse.getDocumentResponseList()).hasSize(1);
            softly.assertThat(addDocumentsResponse.getDocumentResponse(0).getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
        });
    }

    @Test
    public void whenRequestIsInvalidShouldReturnErrorResponse() {
        MdmDocument.AddDocumentRelationsResponse response = supplierDocumentService
            .addDocumentRelations(MdmDocument.AddDocumentRelationsRequest.newBuilder().build());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdAddDocumentRelationsEmptyRelations()));
        });
    }

    @Test
    public void whenRelationIsInvalidShouldReturnErrorResponse() {
        MdmDocument.DocumentOfferRelation newRelation = MdmDocument.DocumentOfferRelation.newBuilder()
            .setRegistrationNumber("Only this field")
            .build();
        MdmDocument.AddDocumentRelationsResponse response = supplierDocumentService
            .addDocumentRelations(MdmDocument.AddDocumentRelationsRequest.newBuilder()
                .addDocumentOfferRelation(newRelation)
                .build());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdAddRelationsFailedRelations(1)));
            softAssertions.assertThat(response.getDocumentRelationsCount()).isEqualTo(1);

            MdmDocument.AddDocumentRelationsResponse.DocumentRelationAddition relation =
                response.getDocumentRelations(0);
            softAssertions.assertThat(relation.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.ERROR);
            softAssertions.assertThat(relation.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdAddDocumentRelationsInvalidDocumentRelation(
                        "Поля registration_number, supplier_id, shop_sku обязательны.")));
            softAssertions.assertThat(relation.getOfferRelation())
                .isEqualTo(newRelation);
        });
    }

    @Test
    public void whenDocumentNotExistsShouldReturnErrorResponse() {
        String regNumber = "It's dangerous to go alone";
        MdmDocument.DocumentOfferRelation newRelation = MdmDocument.DocumentOfferRelation.newBuilder()
            .setRegistrationNumber(regNumber)
            .setSupplierId(1)
            .setShopSku("Take this!")
            .build();
        MdmDocument.AddDocumentRelationsResponse response = supplierDocumentService
            .addDocumentRelations(MdmDocument.AddDocumentRelationsRequest.newBuilder()
                .addDocumentOfferRelation(newRelation)
                .build());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdAddRelationsFailedRelations(1)));
            softAssertions.assertThat(response.getDocumentRelationsCount()).isEqualTo(1);

            MdmDocument.AddDocumentRelationsResponse.DocumentRelationAddition relation =
                response.getDocumentRelations(0);
            softAssertions.assertThat(relation.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.ERROR);
            softAssertions.assertThat(relation.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdAddRelationsFailedToAddRelation("Документ не найден: " + regNumber)));
            softAssertions.assertThat(relation.getOfferRelation())
                .isEqualTo(newRelation);
        });
    }

    @Test
    public void whenDocumentServiceThrowsExceptionShouldReturnErrorResponse() {
        RuntimeException e = new RuntimeException("Failed to add relation, sorry.");
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        Mockito.when(documentService.addDocumentRelation(Mockito.anyString(), Mockito.any(ShopSkuKey.class)))
            .thenThrow(e);

        MdmDocument.DocumentOfferRelation newRelation = MdmDocument.DocumentOfferRelation.newBuilder()
            .setRegistrationNumber("It's dangerous to go alone")
            .setSupplierId(1)
            .setShopSku("Take this!")
            .build();
        MdmDocument.AddDocumentRelationsResponse response = supplierDocumentService
            .addDocumentRelations(MdmDocument.AddDocumentRelationsRequest.newBuilder()
                .addDocumentOfferRelation(newRelation)
                .build());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdAddRelationsFailedRelations(1)));
            softAssertions.assertThat(response.getDocumentRelationsCount()).isEqualTo(1);

            MdmDocument.AddDocumentRelationsResponse.DocumentRelationAddition relation =
                response.getDocumentRelations(0);
            softAssertions.assertThat(relation.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.ERROR);
            softAssertions.assertThat(relation.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdAddRelationsFailedToAddRelation(e.getMessage())));
            softAssertions.assertThat(relation.getOfferRelation())
                .isEqualTo(newRelation);
        });
    }

    @Test
    public void whenAddingRelationShouldCallDocumentServiceWithCorrectParameters() {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);

        supplierDocumentService.addDocumentRelations(MdmDocument.AddDocumentRelationsRequest.newBuilder()
            .addDocumentOfferRelation(MdmDocument.DocumentOfferRelation.newBuilder()
                .setRegistrationNumber("Registration Number")
                .setSupplierId(1)
                .setShopSku("Shop Sku")
                .build())
            .build());

        Mockito.verify(documentService, Mockito.times(1))
            .addDocumentRelation(Mockito.eq("Registration Number"),
                Mockito.eq(new ShopSkuKey(1, "Shop Sku")));

    }

    @Test
    public void whenNewRelationSuccessfullyAddedShouldReturnOKResponse() {
        String regNumber = "RegNumber";
        String shopSku = "ShopSku";
        int supplierId = 1;
        mdmSupplierRepository.insert(new MdmSupplier().setId(supplierId).setType(MdmSupplierType.THIRD_PARTY));
        supplierCachingService.refresh();

        qualityDocumentRepository.insert(new QualityDocument()
            .setRegistrationNumber(regNumber)
            .setType(QualityDocument.QualityDocumentType.EXEMPTION_LETTER)
            .setStartDate(LocalDate.now())
            .setEndDate(LocalDate.now().plusYears(1)));
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);

        MdmDocument.DocumentOfferRelation newRelation = MdmDocument.DocumentOfferRelation.newBuilder()
            .setRegistrationNumber(regNumber)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .build();
        MdmDocument.AddDocumentRelationsResponse response = supplierDocumentService
            .addDocumentRelations(MdmDocument.AddDocumentRelationsRequest.newBuilder()
                .addDocumentOfferRelation(newRelation)
                .build());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.OK);
            softAssertions.assertThat(response.hasError())
                .isFalse();
            softAssertions.assertThat(response.getDocumentRelationsCount()).isEqualTo(1);

            MdmDocument.AddDocumentRelationsResponse.DocumentRelationAddition relation =
                response.getDocumentRelations(0);
            softAssertions.assertThat(relation.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.OK);
            softAssertions.assertThat(relation.hasError())
                .isFalse();
            softAssertions.assertThat(relation.getOfferRelation())
                .isEqualTo(newRelation);
            var queuedInfo = sskuToRefreshRepository.findAll().iterator().next();
            softAssertions.assertThat(queuedInfo.getEntityKey()).isEqualTo(new ShopSkuKey(supplierId, shopSku));
            softAssertions.assertThat(queuedInfo.getOnlyReasons())
                .containsExactly(MdmEnqueueReason.CHANGED_PARTNER_RELATED_DATA);
        });
    }

    @Test
    public void whenNewServiceRelationSuccessfullyAddedShouldEnqueueBusinessSskuKey() {
        String regNumber = "RegNumber";
        prepareBusinessGroup();

        qualityDocumentRepository.insert(new QualityDocument()
            .setRegistrationNumber(regNumber)
            .setType(QualityDocument.QualityDocumentType.EXEMPTION_LETTER)
            .setStartDate(LocalDate.now())
            .setEndDate(LocalDate.now().plusYears(1)));
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);

        MdmDocument.DocumentOfferRelation newRelation = MdmDocument.DocumentOfferRelation.newBuilder()
            .setRegistrationNumber(regNumber)
            .setSupplierId(SUPPLIER_ID)
            .setShopSku(ANY_SHOP_SKU)
            .build();
        MdmDocument.AddDocumentRelationsResponse response = supplierDocumentService
            .addDocumentRelations(MdmDocument.AddDocumentRelationsRequest.newBuilder()
                .addDocumentOfferRelation(newRelation)
                .build());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.OK);
            softAssertions.assertThat(response.hasError())
                .isFalse();
            softAssertions.assertThat(response.getDocumentRelationsCount()).isEqualTo(1);

            MdmDocument.AddDocumentRelationsResponse.DocumentRelationAddition relation =
                response.getDocumentRelations(0);
            softAssertions.assertThat(relation.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.OK);
            softAssertions.assertThat(relation.hasError())
                .isFalse();
            softAssertions.assertThat(relation.getOfferRelation())
                .isEqualTo(newRelation);
            var queuedInfo = sskuToRefreshRepository.findAll().iterator().next();
            softAssertions.assertThat(queuedInfo.getEntityKey()).isEqualTo(new ShopSkuKey(BUSINESS_ID, ANY_SHOP_SKU));
            softAssertions.assertThat(queuedInfo.getOnlyReasons())
                .containsExactly(MdmEnqueueReason.CHANGED_PARTNER_RELATED_DATA);
        });
    }

    @Test
    public void whenFindRelationsShouldPassCorrectParametersToDocumentFilter() {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);

        String regNum = "regnum";
        String idOffset = "13";
        int limit = 2;
        int supplierId = 1;
        ArgumentCaptor<DocumentFilter> filterArgumentCaptor = ArgumentCaptor.forClass(DocumentFilter.class);
        supplierDocumentService.findSupplierDocumentRelations(MdmDocument.FindSupplierDocumentRelationsRequest
            .newBuilder()
            .setSupplierId(supplierId)
            .setRegistrationNumber(regNum)
            .setOffsetKey(idOffset)
            .setLimit(limit)
            .build());

        Mockito.verify(documentService, Mockito.times(1))
            .findDocumentRelations(filterArgumentCaptor.capture());
        assertSoftly(softly -> {
            DocumentFilter documentFilter = filterArgumentCaptor.getValue();
            softly.assertThat(documentFilter).isNotNull();
            softly.assertThat(documentFilter.getRegistrationNumbers()).containsExactly(regNum);
            softly.assertThat(documentFilter.getRelationSupplierIds()).isEqualTo(Set.of(supplierId));
            softly.assertThat(documentFilter.getLimit()).isNull();
            softly.assertThat(documentFilter.getDeleted()).isFalse();
        });
    }

    @Test
    public void whenRequestIsInvalidShouldReturnError() {
        MdmDocument.FindSupplierDocumentRelationsResponse response = supplierDocumentService
            .findSupplierDocumentRelations(MdmDocument.FindSupplierDocumentRelationsRequest.newBuilder().build());
        assertSoftly(softly -> {
            softly.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.FindSupplierDocumentRelationsResponse.Status.ERROR);
            softly.assertThat(response.hasOfferRelations()).isFalse();
        });
    }

    @Test
    public void whenDocumentServiceThrowsExceptionFindRelationsShouldReturnError() {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        RuntimeException e = new RuntimeException("Failed to find relations.");
        Mockito.when(documentService.findDocumentRelations(Mockito.any(DocumentFilter.class)))
            .thenThrow(e);

        MdmDocument.FindSupplierDocumentRelationsResponse response = supplierDocumentService
            .findSupplierDocumentRelations(MdmDocument.FindSupplierDocumentRelationsRequest.newBuilder()
                .setSupplierId(1)
                .setRegistrationNumber("ololo")
                .build());
        assertSoftly(softly -> {
            softly.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.FindSupplierDocumentRelationsResponse.Status.ERROR);
            softly.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().protoUnknownError(e.getMessage())));
        });
    }

    @Test
    public void whenDocumentServiceFoundOffersShouldReturnCorrectResponse() {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        int supplierId = 1;
        String regNum = "regNum";
        String shopSku1 = "sku1";
        String shopSku2 = "sku2";
        Mockito.when(documentService.findDocumentRelations(Mockito.any()))
            .thenReturn(Arrays.asList(
                DocumentOfferRelation.from(new ShopSkuKey(supplierId, shopSku1), generateDocument().setId(1)),
                DocumentOfferRelation.from(new ShopSkuKey(supplierId, shopSku2), generateDocument().setId(1))));

        MdmDocument.FindSupplierDocumentRelationsResponse supplierDocumentRelations = supplierDocumentService
            .findSupplierDocumentRelations(MdmDocument.FindSupplierDocumentRelationsRequest.newBuilder()
                .setRegistrationNumber(regNum)
                .setSupplierId(supplierId)
                .build());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(supplierDocumentRelations.hasError()).isFalse();
            softAssertions.assertThat(supplierDocumentRelations.getStatus())
                .isEqualTo(MdmDocument.FindSupplierDocumentRelationsResponse.Status.OK);
            softAssertions.assertThat(supplierDocumentRelations.hasNextOffsetKey()).isFalse();

            MdmDocument.FindSupplierDocumentRelationsResponse.SupplierOfferRelations relations =
                supplierDocumentRelations.getOfferRelations();
            softAssertions.assertThat(relations.getSupplierId()).isEqualTo(supplierId);
            softAssertions.assertThat(relations.getRegistrationNumber()).isEqualTo(regNum);
            softAssertions.assertThat(relations.getShopSkuList()).containsExactly(shopSku1, shopSku2);
        });
    }

    @Test
    public void whenOffsetPassedShouldReturnRelationsWithOffset() {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        int supplierId = 1;
        String regNum = "regNum";
        String shopSku1 = "sku1";
        String shopSku2 = "sku2";
        String shopSku3 = "sku3";
        Mockito.when(documentService.findDocumentRelations(Mockito.any()))
            .thenReturn(Arrays.asList(
                DocumentOfferRelation.from(new ShopSkuKey(supplierId, shopSku1), generateDocument().setId(1)),
                DocumentOfferRelation.from(new ShopSkuKey(supplierId, shopSku2), generateDocument().setId(1)),
                DocumentOfferRelation.from(new ShopSkuKey(supplierId, shopSku3), generateDocument().setId(1))));

        MdmDocument.FindSupplierDocumentRelationsResponse supplierDocumentRelations = supplierDocumentService
            .findSupplierDocumentRelations(MdmDocument.FindSupplierDocumentRelationsRequest.newBuilder()
                .setRegistrationNumber(regNum)
                .setSupplierId(supplierId)
                .setOffsetKey(shopSku1)
                .build());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(supplierDocumentRelations.hasError()).isFalse();
            softAssertions.assertThat(supplierDocumentRelations.getStatus())
                .isEqualTo(MdmDocument.FindSupplierDocumentRelationsResponse.Status.OK);
            softAssertions.assertThat(supplierDocumentRelations.hasNextOffsetKey()).isFalse();

            MdmDocument.FindSupplierDocumentRelationsResponse.SupplierOfferRelations relations =
                supplierDocumentRelations.getOfferRelations();
            softAssertions.assertThat(relations.getSupplierId()).isEqualTo(supplierId);
            softAssertions.assertThat(relations.getRegistrationNumber()).isEqualTo(regNum);
            softAssertions.assertThat(relations.getShopSkuList()).containsExactly(shopSku2, shopSku3);
        });
    }

    @Test
    public void whenFoundTooManyRelationsShouldReturnNextOffsetKey() {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        int supplierId = 1;
        String regNum = "regNum";
        String shopSku1 = "sku1";
        String shopSku2 = "sku2";
        String shopSku3 = "sku3";
        int limit = 2;
        Mockito.when(documentService.findDocumentRelations(Mockito.any()))
            .thenReturn(Arrays.asList(
                DocumentOfferRelation.from(new ShopSkuKey(supplierId, shopSku1), generateDocument().setId(1)),
                DocumentOfferRelation.from(new ShopSkuKey(supplierId, shopSku2), generateDocument().setId(1)),
                DocumentOfferRelation.from(new ShopSkuKey(supplierId, shopSku3), generateDocument().setId(1))));

        MdmDocument.FindSupplierDocumentRelationsResponse supplierDocumentRelations = supplierDocumentService
            .findSupplierDocumentRelations(MdmDocument.FindSupplierDocumentRelationsRequest.newBuilder()
                .setRegistrationNumber(regNum)
                .setSupplierId(supplierId)
                .setLimit(limit)
                .build());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(supplierDocumentRelations.hasError()).isFalse();
            softAssertions.assertThat(supplierDocumentRelations.getStatus())
                .isEqualTo(MdmDocument.FindSupplierDocumentRelationsResponse.Status.OK);

            softAssertions.assertThat(supplierDocumentRelations.getNextOffsetKey()).isEqualTo(shopSku2);

            MdmDocument.FindSupplierDocumentRelationsResponse.SupplierOfferRelations relations =
                supplierDocumentRelations.getOfferRelations();
            softAssertions.assertThat(relations.getSupplierId()).isEqualTo(supplierId);
            softAssertions.assertThat(relations.getRegistrationNumber()).isEqualTo(regNum);
            softAssertions.assertThat(relations.getShopSkuList()).containsExactly(shopSku1, shopSku2);
        });
    }

    @Test
    public void whenFindRelationsUsingOffsetKeyAndLimitShouldFindCorrectRelationInEachCall() {
        QualityDocument document = generateValidDocument();
        String registrationNumber = document.getRegistrationNumber();
        int supplierId = SUPPLIER_ID;

        MboMappings.ApprovedMappingInfo offer1 = addMapping(o -> o.setSupplierId(supplierId).setShopSku("shopSku1"));
        MboMappings.ApprovedMappingInfo offer2 = addMapping(o -> o.setSupplierId(supplierId).setShopSku("shopSku2"));
        MboMappings.ApprovedMappingInfo offer3 = addMapping(o -> o.setSupplierId(supplierId).setShopSku("shopSku3"));

        masterDataRepository.insertOrUpdateAll(Arrays.asList(
            generateMasterData(offer1, document),
            generateMasterData(offer2, document),
            generateMasterData(offer3, document)
        ));
        // first call
        MdmDocument.FindSupplierDocumentRelationsRequest request =
            MdmDocument.FindSupplierDocumentRelationsRequest.newBuilder()
                .setSupplierId(supplierId)
                .setRegistrationNumber(registrationNumber)
                .setLimit(1)
                .build();
        MdmDocument.FindSupplierDocumentRelationsResponse response = supplierDocumentService
            .findSupplierDocumentRelations(request);
        assertFindRelationsResponse(registrationNumber, supplierId, Optional.of(offer1.getShopSku()),
            response, offer1.getShopSku());
        // second call
        request = request.toBuilder()
            .setOffsetKey(response.getNextOffsetKey())
            .build();
        response = supplierDocumentService.findSupplierDocumentRelations(request);
        assertFindRelationsResponse(registrationNumber, supplierId, Optional.of(offer2.getShopSku()),
            response, offer2.getShopSku());
        // third call
        request = request.toBuilder()
            .setOffsetKey(response.getNextOffsetKey())
            .build();
        response = supplierDocumentService.findSupplierDocumentRelations(request);
        assertFindRelationsResponse(registrationNumber, supplierId, Optional.of(offer3.getShopSku()),
            response, offer3.getShopSku());
        // last call
        request = request.toBuilder()
            .setOffsetKey(response.getNextOffsetKey())
            .build();
        response = supplierDocumentService.findSupplierDocumentRelations(request);
        assertFindRelationsResponse(registrationNumber, supplierId, Optional.empty(), response);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void whenFindingDocumentsWithLimitShouldIgnoreDocumentsWithMultipleRelations() {
        String regNumber = "test_";
        int supplierId = SUPPLIER_ID;
        QualityDocument document1 = generateValidDocument().setRegistrationNumber(regNumber + "doc1");
        qualityDocumentRepository.insert(document1);
        QualityDocument document2 = generateValidDocument().setRegistrationNumber(regNumber + "doc2");
        qualityDocumentRepository.insert(document2);

        MboMappings.ApprovedMappingInfo doc1offer1 = addMapping(
            o -> o.setSupplierId(supplierId).setShopSku("shopSku1")
        );
        MboMappings.ApprovedMappingInfo doc1offer2 = addMapping(
            o -> o.setSupplierId(supplierId).setShopSku("shopSku2")
        );
        MboMappings.ApprovedMappingInfo doc2offer1 = addMapping(
            o -> o.setSupplierId(supplierId).setShopSku("shopSku3")
        );

        masterDataRepository.insertOrUpdateAll(Arrays.asList(
            generateMasterData(doc1offer1, document1),
            generateMasterData(doc1offer2, document1),
            generateMasterData(doc2offer1, document2)
        ));
        // first call
        MdmDocument.FindSupplierDocumentsRequest request =
            MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(supplierId)
                .setSearchQuery(regNumber)
                .setLimit(1)
                .build();
        MdmDocument.FindDocumentsResponse response1 = supplierDocumentService.findSupplierDocuments(request);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response1.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);
            softAssertions.assertThat(response1.getDocumentList())
                .containsExactly(DocumentProtoConverter.createProtoDocument(document1));
            softAssertions.assertThat(response1.getNextOffsetKey()).isEqualTo(String.valueOf(document1.getId()));
        });
        // second call
        request = MdmDocument.FindSupplierDocumentsRequest.newBuilder()
            .setSupplierId(supplierId)
            .setSearchQuery(regNumber)
            .setLimit(2)
            .build();
        MdmDocument.FindDocumentsResponse response2 = supplierDocumentService.findSupplierDocuments(request);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response2.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);
            softAssertions.assertThat(response2.getDocumentList())
                .containsExactly(
                    DocumentProtoConverter.createProtoDocument(document1),
                    DocumentProtoConverter.createProtoDocument(document2));
            softAssertions.assertThat(response2.getNextOffsetKey()).isEqualTo(String.valueOf(document2.getId()));
        });

        // third call
        request = MdmDocument.FindSupplierDocumentsRequest.newBuilder()
            .setSupplierId(supplierId)
            .setSearchQuery(regNumber)
            .setLimit(3)
            .build();
        MdmDocument.FindDocumentsResponse response3 = supplierDocumentService.findSupplierDocuments(request);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response3.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);
            softAssertions.assertThat(response3.getDocumentList())
                .containsExactly(
                    DocumentProtoConverter.createProtoDocument(document1),
                    DocumentProtoConverter.createProtoDocument(document2));
            softAssertions.assertThat(response3.hasNextOffsetKey()).isFalse();
        });
    }

    @Test
    public void whenAddingValidDocumentWithNewPicturesShouldSaveThem() throws Exception {
        QualityDocument validDocument = generateValidDocument();
        validDocument.setPictures(null);
        qualityDocumentRepository.insert(validDocument);

        DocumentAddition.ScanFile scanFile = DocumentAddition.ScanFile.newBuilder()
            .setUrl(CORRECT_SCAN_FILE_URL)
            .setFileName(CORRECT_SCAN_FILE_NAME)
            .build();

        //  have to mock method here, url does not exist
        QualityDocumentPictureServiceImpl pictureService = Mockito
            .spy(new QualityDocumentPictureServiceImpl(new AvatarImageDepotServiceMock()));
        Mockito.doReturn(new ByteArrayMultipartFile(CORRECT_SCAN_FILE_NAME, new byte[1]))
            .when(pictureService)
            .downloadPicture(Mockito.eq(scanFile));

        SupplierDocumentPicturesProcessor picturesProcessor = new SupplierDocumentPicturesProcessor(pictureService);

        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            picturesProcessor,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);

        MdmDocument.AddDocumentsResponse addDocumentsResponse = supplierDocumentService
            .addSupplierDocuments(MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                .setSupplierId(SUPPLIER_ID)
                .addDocument(DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(validDocument))
                    .addNewScanFile(scanFile)
                    .build())
                .build());

        List<QualityDocument> repositoryDocuments = qualityDocumentRepository.findAll();
        assertSoftly(softly -> {
            softly.assertThat(repositoryDocuments).hasSize(1);
            softly.assertThat(addDocumentsResponse.getStatus()).isEqualTo(MdmDocument.AddDocumentsResponse.Status.OK);
            softly.assertThat(addDocumentsResponse.getDocumentResponseList()).hasSize(1);

            softly.assertThat(addDocumentsResponse.getDocumentResponse(0).getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.OK);

            QualityDocument qualityDocument = repositoryDocuments.get(0);
            MdmDocument.Document document = addDocumentsResponse.getDocumentResponse(0).getDocument();

            softly.assertThat(qualityDocument.getPictures()).hasSize(1);
            softly.assertThat(document.getPictureList())
                .containsExactlyInAnyOrderElementsOf(qualityDocument.getPictures());
        });
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void assertFindRelationsResponse(String regNumber, int supplierId, Optional<String> nextOffsetKey,
                                             MdmDocument.FindSupplierDocumentRelationsResponse response,
                                             String... shopSkus) {
        SoftAssertions.assertSoftly(assertSoftly -> {
            assertSoftly.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.FindSupplierDocumentRelationsResponse.Status.OK);

            MdmDocument.FindSupplierDocumentRelationsResponse.SupplierOfferRelations relations =
                response.getOfferRelations();
            assertSoftly.assertThat(relations.getSupplierId()).isEqualTo(supplierId);
            assertSoftly.assertThat(relations.getRegistrationNumber()).isEqualTo(regNumber);
            assertSoftly.assertThat(relations.getShopSkuList()).containsExactly(shopSkus);

            if (nextOffsetKey.isPresent()) {
                assertSoftly.assertThat(response.getNextOffsetKey()).isEqualTo(nextOffsetKey.get());
            } else {
                assertSoftly.assertThat(response.hasNextOffsetKey()).isFalse();
            }
        });
    }

    @Test
    public void whenSearchingDocumentsRemoveOrigSuffixFromResponse() {
        List<String> pics = qualityDocumentsRandom.objects(String.class, 2).collect(Collectors.toList());
        QualityDocument document = generateDocument()
            .setPictures(pics.stream()
                .map(s -> s + DocumentProtoPictureConverter.ORIG)
                .collect(Collectors.toList()));
        MasterData offerMasterData = generateMasterData(document);
        qualityDocumentRepository.insertOrUpdateAll(Collections.singletonList(document));

        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService
            .findSupplierDocuments(MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(offerMasterData.getSupplierId())
                .setSearchQuery(document.getRegistrationNumber())
                .build());

        Mockito.verify(supplierDocumentService, Mockito.times(1))
            .createFindDocumentsResponseWithoutSuffix(Mockito.anyList(), Mockito.eq(Optional.empty()));
        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);
            softly.assertThat(protoResponse.getDocument(0).getPictureList())
                .containsExactlyElementsOf(pics);
        });
    }

    @Test
    public void whenUpdatingDocumentsShouldAddOrigSuffixAndRemoveItInResponse() {
        Mockito.when(documentPicturesProcessorMock.updateDocumentAdditionPictures(
            Mockito.any(DocumentAddition.class), Mockito.any(QualityDocument.class)))
            .thenCallRealMethod();

        String picToDelete = qualityDocumentsRandom.nextObject(String.class);
        String picToSave = qualityDocumentsRandom.nextObject(String.class);
        List<String> picsInAddition = Arrays.asList(picToDelete, picToSave);
        QualityDocument document = generateValidDocument()
            .setPictures(picsInAddition.stream()
                .map(s -> s + DocumentProtoPictureConverter.ORIG)
                .collect(Collectors.toList()));
        MasterData offerMasterData = generateMasterData(document);
        qualityDocumentRepository.insertOrUpdateAll(Collections.singletonList(document));

        MdmDocument.AddDocumentsResponse protoResponse = supplierDocumentService
            .addSupplierDocuments(MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                .setSupplierId(offerMasterData.getSupplierId())
                .addDocument(DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(document))
                    .addDeletePictureMdmUrl(picToDelete)
                    .build())
                .build());

        Mockito.verify(supplierDocumentService, Mockito.times(1))
            .createDocumentAdditionsWithSuffix(Mockito.any(MdmDocument.AddSupplierDocumentsRequest.class));
        Mockito.verify(supplierDocumentService, Mockito.times(1))
            .createProtoDocumentWithoutSuffix(Mockito.any(QualityDocument.class));
        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.AddDocumentsResponse.Status.OK);
            softly.assertThat(protoResponse.getDocumentResponse(0).getDocument().getPictureList())
                .containsExactly(picToSave);
        });
    }

    @Test
    public void whenFoundOtherSupplierDocumentShouldAddOrigSuffixAndRemoveItInResponse() {
        Mockito.when(documentPicturesProcessorMock.updateDocumentAdditionPictures(
            Mockito.any(DocumentAddition.class), Mockito.any(QualityDocument.class)))
            .thenCallRealMethod();

        String picToDelete = qualityDocumentsRandom.nextObject(String.class);
        String picToSave = qualityDocumentsRandom.nextObject(String.class);
        List<String> picsInAddition = Arrays.asList(picToDelete, picToSave);
        QualityDocument document = generateValidDocument()
            .setPictures(picsInAddition.stream()
                .map(s -> s + DocumentProtoPictureConverter.ORIG)
                .collect(Collectors.toList()));
        MasterData offerMasterData = generateMasterData(document);
        qualityDocumentRepository.insertOrUpdateAll(Collections.singletonList(document));

        MdmDocument.AddDocumentsResponse protoResponse = supplierDocumentService
            .addSupplierDocuments(MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                .setSupplierId(offerMasterData.getSupplierId() + 1)
                .addDocument(DocumentAddition.newBuilder()
                    .setDocument(DocumentProtoConverter.createProtoDocument(document))
                    .addDeletePictureMdmUrl(picToDelete)
                    .build())
                .build());

        Mockito.verify(supplierDocumentService, Mockito.times(1))
            .createDocumentAdditionsWithSuffix(Mockito.any(MdmDocument.AddSupplierDocumentsRequest.class));
        Mockito.verify(supplierDocumentService, Mockito.times(1))
            .createProtoDocumentWithoutSuffix(Mockito.any(QualityDocument.class));
        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
            softly.assertThat(protoResponse.getDocumentResponse(0).getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
            softly.assertThat(protoResponse.getDocumentResponse(0).getDocument().getPictureList())
                .containsExactly(picToDelete, picToSave);
        });
    }

    @Test
    public void whenFindDocumentByRegistrationNumberShouldPassCorrectParametersToInternalService() {
        String registrationNumber = "registration_number";

        ArgumentCaptor<DocumentOfferFilter> filterArgumentCaptor = ArgumentCaptor.forClass(DocumentOfferFilter.class);
        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService
            .findSupplierDocumentByRegistrationNumber(MdmDocument.FindDocumentByRegistrationNumberRequest.newBuilder()
                .setRegistrationNumber(registrationNumber)
                .build());

        Mockito.verify(documentService, Mockito.times(1)).findBy(filterArgumentCaptor.capture());
        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);
            softly.assertThat(protoResponse.hasStatusMessage()).isFalse();
            softly.assertThat(protoResponse.hasNextOffsetKey()).isFalse();

            DocumentFilter documentFilter = filterArgumentCaptor.getValue().getDocumentFilter();
            softly.assertThat(documentFilter.getRegistrationNumbers()).containsExactly(registrationNumber);
            softly.assertThat(documentFilter.getSupplierDocSearchCriteria()).isNull();
        });
    }

    @Test
    public void whenFindDocumentByRegistrationNumberRequestIsInvalidShouldReturnError() {
        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService
            .findSupplierDocumentByRegistrationNumber(MdmDocument.FindDocumentByRegistrationNumberRequest.newBuilder()
                .build());

        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.ERROR);
            softly.assertThat(protoResponse.getDocumentList()).hasSize(0);
            softly.assertThat(protoResponse.hasNextOffsetKey()).isFalse();

            softly.assertThat(protoResponse.getStatusMessage()).isEqualTo(
                MbocCommonMessageUtils.errorInfoToMessage(MbocErrors.get().qdFindDocumentEmptyRegistrationNumber()));
        });
    }

    @Test
    public void whenFindDocumentByRegistrationNumberThrowsExceptionFindDocumentsShouldReturnError() {
        documentService = Mockito.mock(DocumentService.class);
        RuntimeException e = new RuntimeException("Failed to find documents.");
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        Mockito.when(documentService.findBy(Mockito.any()))
            .thenThrow(e);

        MdmDocument.FindDocumentsResponse protoResponse = supplierDocumentService
            .findSupplierDocumentByRegistrationNumber(MdmDocument.FindDocumentByRegistrationNumberRequest.newBuilder()
                .setRegistrationNumber("11")
                .build());

        assertSoftly(softly -> {
            softly.assertThat(protoResponse.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.ERROR);
            softly.assertThat(protoResponse.getDocumentList()).hasSize(0);
            softly.assertThat(protoResponse.hasNextOffsetKey()).isFalse();

            softly.assertThat(protoResponse.getStatusMessage()).isEqualTo(
                MbocCommonMessageUtils.errorInfoToMessage(MbocErrors.get().protoUnknownError(e.getMessage())));
        });
    }

    @Test
    public void whenRemoveRelationsRequestIsInvalidShouldReturnErrorResponse() {
        MdmDocument.RemoveDocumentOfferRelationsResponse response = supplierDocumentService
            .removeDocumentOfferRelations(MdmDocument.RemoveDocumentOfferRelationsRequest.newBuilder().build());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdRemoveDocumentRelationsEmptyRelations()));
        });
    }

    @Test
    public void whenRemoveRelationsAreInvalidShouldReturnErrorResponse() {
        MdmDocument.DocumentOfferRelation emptyRelation = MdmDocument.DocumentOfferRelation.newBuilder().build();
        MdmDocument.RemoveDocumentOfferRelationsResponse response = supplierDocumentService
            .removeDocumentOfferRelations(
                MdmDocument.RemoveDocumentOfferRelationsRequest.newBuilder()
                    .addRelation(emptyRelation)
                    .addRelation(emptyRelation)
                    .build());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdRemoveDocumentRelationsFailedRelations(2)));

            MdmDocument.RemoveDocumentOfferRelationsResponse.RemoveRelationResponse emptyRelationResponse =
                DocumentResponseUtil.createRemoveRelationErrorResponse(emptyRelation,
                    MbocErrors.get().qdAddDocumentRelationsInvalidDocumentRelation(
                        "Поля registration_number, supplier_id, shop_sku обязательны."));
            softAssertions.assertThat(response.getRelationResponseList())
                .containsExactly(emptyRelationResponse, emptyRelationResponse);
        });
    }

    @Test
    public void whenSearchByShopSkuKeysWithDifferentSupplierIdShouldFind() {
        QualityDocument notFoundDoc = generateDocument();
        MasterData notFoundMasterData = generateMasterData(
            addMapping(o -> o.setSupplierId(SUPPLIER_ID).setShopSku("notFoundShopSku")),
            notFoundDoc
        );

        QualityDocument foundDoc = generateDocument();
        MasterData foundMasterData = generateMasterData(
            addMapping(o -> o.setSupplierId(SUPPLIER_ID).setShopSku("foundShopSku")),
            foundDoc
        );
        QualityDocument foundDocOther = generateDocument();
        MasterData foundMasterDataOther = generateMasterData(
            addMapping(o -> o.setSupplierId(OTHER_SUPPLIER_ID).setShopSku("foundShopSkuOther")),
            foundDocOther
        );

        masterDataRepository.insertOrUpdateAll(
            Arrays.asList(notFoundMasterData, foundMasterData, foundMasterDataOther)
        );

        MdmDocument.FindDocumentsByShopSkuResponse response = supplierDocumentService.findSupplierDocumentsByShopSku(
            MdmDocument.FindDocumentsByShopSkuRequest.newBuilder()
                .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(foundMasterData.getShopSkuKey()))
                .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(foundMasterDataOther.getShopSkuKey()))
                .build()
        );

        Assertions.assertThat(response.getShopSkuDocumentsList())
            .hasSize(2);

        List<ShopSkuKey> shopSkuKeys = response.getShopSkuDocumentsList().stream()
            .map(MdmDocument.ShopSkuDocuments::getShopSkuKey)
            .map(MbocBaseProtoConverter::protoToPojo)
            .collect(Collectors.toList());
        Assertions.assertThat(shopSkuKeys)
            .containsExactlyInAnyOrder(foundMasterData.getShopSkuKey(), foundMasterDataOther.getShopSkuKey());

        Assertions.assertThat(response.getShopSkuDocumentsList()).hasSize(2);
        Assertions.assertThat(response.getShopSkuDocumentsList())
            .allSatisfy(sskuDocuments ->
                Assertions.assertThat(sskuDocuments.getDocumentList())
                    .hasSize(1));

        List<QualityDocument> docs = response.getShopSkuDocumentsList().stream()
            .flatMap(sskuDocuments -> sskuDocuments.getDocumentList().stream())
            .map(DocumentProtoConverter::createQualityDocument)
            .collect(Collectors.toList());

        Assertions.assertThat(docs).usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(foundDoc, foundDocOther);
    }

    @Test
    public void whenSearchByShopSkuKeysWithSameSupplierIdShouldFind() {
        QualityDocument notFoundDoc = generateDocument();
        MasterData notFoundMasterData = generateMasterData(
            addMapping(o -> o.setSupplierId(SUPPLIER_ID).setShopSku("notFoundShopSku")),
            notFoundDoc
        );

        QualityDocument foundDoc = generateDocument();
        MasterData foundMasterData = generateMasterData(
            addMapping(o -> o.setSupplierId(SUPPLIER_ID).setShopSku("foundShopSku")),
            foundDoc
        );

        masterDataRepository.insertOrUpdateAll(
            Arrays.asList(notFoundMasterData, foundMasterData)
        );

        MdmDocument.FindDocumentsByShopSkuResponse response = supplierDocumentService.findSupplierDocumentsByShopSku(
            MdmDocument.FindDocumentsByShopSkuRequest.newBuilder()
                .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(foundMasterData.getShopSkuKey()))
                .build()
        );

        Assertions.assertThat(response.getShopSkuDocumentsList())
            .hasSize(1);

        Assertions.assertThat(MbocBaseProtoConverter.protoToPojo(
            response.getShopSkuDocumentsList().get(0).getShopSkuKey()
        ))
            .isEqualTo(foundMasterData.getShopSkuKey());

        Assertions.assertThat(response.getShopSkuDocumentsList().get(0).getDocumentList())
            .hasSize(1);

        Assertions.assertThat(DocumentProtoConverter.createQualityDocument(
            response.getShopSkuDocumentsList().get(0).getDocumentList().get(0)
        ))
            .isEqualToIgnoringGivenFields(foundDoc, "id");
    }

    @Test
    public void whenDocumentServiceThrowsExceptionsSearchingShouldReturnError() {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService = new SupplierDocumentServiceImpl(
            documentService,
            documentPicturesProcessorMock,
            mboMappingsService,
            qualityDocumentRepository,
            mdmSskuGroupManager, supplierCachingService);
        RuntimeException exception = new RuntimeException("Failed");
        Mockito.when(documentService.findDocumentRelations(Mockito.any())).thenThrow(exception);

        MdmDocument.DocumentOfferRelation relation = MdmDocument.DocumentOfferRelation.newBuilder()
            .setRegistrationNumber("12345")
            .setSupplierId(1)
            .setShopSku("1231456")
            .build();
        MdmDocument.RemoveDocumentOfferRelationsResponse response = supplierDocumentService
            .removeDocumentOfferRelations(
                MdmDocument.RemoveDocumentOfferRelationsRequest.newBuilder()
                    .addRelation(relation)
                    .build());

        Mockito.verify(documentService, Mockito.times(1)).findDocumentRelations(Mockito.any());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdRemoveDocumentRelationsFailedRelations(1)));

            softAssertions.assertThat(response.getRelationResponseList())
                .containsExactly(DocumentResponseUtil.createRemoveRelationErrorResponse(relation,
                    MbocErrors.get().qdRemoveDocumentRelationsFailedToRemoveRelation(exception.getMessage())));
        });
    }

    @Test
    public void whenSearchByShopSkuKeysShouldAccountForBusinessIds() {
        prepareBusinessGroup();
        QualityDocument serviceDoc1 = generateDocument();
        QualityDocument serviceDoc2 = generateDocument();
        QualityDocument serviceDoc3 = generateDocument();
        QualityDocument businessDoc = generateDocument();
        MasterData serviceMD1 = generateMasterData(
            addMapping(o -> o.setSupplierId(SUPPLIER_ID).setShopSku(ANY_SHOP_SKU)),
            serviceDoc1, serviceDoc2
        );
        MasterData serviceMD2 = generateMasterData(
            addMapping(o -> o.setSupplierId(OTHER_SUPPLIER_ID).setShopSku(ANY_SHOP_SKU)),
            serviceDoc3
        );
        MasterData businessMD = generateMasterData(
            addMapping(o -> o.setSupplierId(BUSINESS_ID).setShopSku(ANY_SHOP_SKU)),
            businessDoc
        );

        masterDataRepository.insertOrUpdateAll(
            Arrays.asList(serviceMD1, serviceMD2, businessMD)
        );

        var noBusinessResponse = supplierDocumentService.findSupplierDocumentsByShopSku(
            MdmDocument.FindDocumentsByShopSkuRequest.newBuilder()
                .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(serviceMD1.getShopSkuKey()))
                .build()
        );
        var businessResponse = supplierDocumentService.findSupplierDocumentsByShopSku(
            MdmDocument.FindDocumentsByShopSkuRequest.newBuilder()
                .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(businessMD.getShopSkuKey()))
                .build()
        );

        Assertions.assertThat(noBusinessResponse.getShopSkuDocumentsList()).hasSize(1);
        Assertions.assertThat(businessResponse.getShopSkuDocumentsList()).hasSize(1);

        Assertions.assertThat(MbocBaseProtoConverter.protoToPojo(
            noBusinessResponse.getShopSkuDocumentsList().get(0).getShopSkuKey()
        )).isEqualTo(serviceMD1.getShopSkuKey());

        Assertions.assertThat(MbocBaseProtoConverter.protoToPojo(
            businessResponse.getShopSkuDocumentsList().get(0).getShopSkuKey()
        )).isEqualTo(businessMD.getShopSkuKey());

        List<QualityDocument> noBusinessResponseDocs = noBusinessResponse.getShopSkuDocumentsList()
            .stream()
            .map(MdmDocument.ShopSkuDocuments::getDocumentList)
            .flatMap(List::stream)
            .map(DocumentProtoConverter::createQualityDocument)
            .collect(Collectors.toList());

        List<QualityDocument> businessResponseDocs = businessResponse.getShopSkuDocumentsList()
            .stream()
            .map(MdmDocument.ShopSkuDocuments::getDocumentList)
            .flatMap(List::stream)
            .map(DocumentProtoConverter::createQualityDocument)
            .collect(Collectors.toList());

        Assertions.assertThat(noBusinessResponseDocs).usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(serviceDoc1, serviceDoc2);
        Assertions.assertThat(businessResponseDocs).usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(serviceDoc1, serviceDoc2, serviceDoc3, businessDoc);
    }

    @Test
    public void whenSearchRelationsWithSupplierIdShouldAccountForBusinessId() {
        prepareBusinessGroup();
        QualityDocument serviceDoc1 = generateDocument();
        QualityDocument serviceDoc2 = generateDocument();
        QualityDocument serviceDoc3 = generateDocument();
        QualityDocument businessDoc = generateDocument();
        MasterData serviceMD1 = generateMasterData(
            addMapping(o -> o.setSupplierId(SUPPLIER_ID).setShopSku(ANY_SHOP_SKU)),
            serviceDoc1, serviceDoc2
        );
        MasterData serviceMD2 = generateMasterData(
            addMapping(o -> o.setSupplierId(OTHER_SUPPLIER_ID).setShopSku(ANY_SHOP_SKU)),
            serviceDoc3
        );
        MasterData businessMD = generateMasterData(
            addMapping(o -> o.setSupplierId(BUSINESS_ID).setShopSku(ANY_SHOP_SKU)),
            businessDoc
        );

        masterDataRepository.insertOrUpdateAll(
            Arrays.asList(serviceMD1, serviceMD2, businessMD)
        );

        var response = supplierDocumentService.findSupplierDocumentRelations(
            MdmDocument.FindSupplierDocumentRelationsRequest.newBuilder()
                .setSupplierId(BUSINESS_ID)
                .setLimit(1)
                .setRegistrationNumber(serviceDoc2.getRegistrationNumber())
                .build()
        );

        Assertions.assertThat(response.getOfferRelations().getSupplierId()).isEqualTo(BUSINESS_ID);
        Assertions.assertThat(response.getOfferRelations().getRegistrationNumber())
            .isEqualTo(serviceDoc2.getRegistrationNumber());
        Assertions.assertThat(response.getOfferRelations().getShopSkuList())
            .containsExactlyInAnyOrder(serviceMD1.getShopSku());
    }

    @Test
    public void whenSearchByGenericRequestShouldAccountForBusinessId() {
        prepareBusinessGroup();
        QualityDocument serviceDoc1 = generateDocument();
        QualityDocument serviceDoc2 = generateDocument();
        QualityDocument serviceDoc3 = generateDocument();
        QualityDocument businessDoc = generateDocument();
        MasterData serviceMD1 = generateMasterData(
            addMapping(o -> o.setSupplierId(SUPPLIER_ID).setShopSku(ANY_SHOP_SKU)),
            serviceDoc1, serviceDoc2
        );
        MasterData serviceMD2 = generateMasterData(
            addMapping(o -> o.setSupplierId(OTHER_SUPPLIER_ID).setShopSku(ANY_SHOP_SKU)),
            serviceDoc3
        );
        MasterData businessMD = generateMasterData(
            addMapping(o -> o.setSupplierId(BUSINESS_ID).setShopSku(ANY_SHOP_SKU)),
            businessDoc
        );

        masterDataRepository.insertOrUpdateAll(
            Arrays.asList(serviceMD1, serviceMD2, businessMD)
        );

        var response = supplierDocumentService.findSupplierDocuments(
            MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(BUSINESS_ID)
                .build()
        );

        Assertions.assertThat(response.getDocumentList()).hasSize(4);
        List<QualityDocument> responseDocs = response.getDocumentList()
            .stream()
            .map(DocumentProtoConverter::createQualityDocument)
            .collect(Collectors.toList());

        Assertions.assertThat(responseDocs).usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(serviceDoc1, serviceDoc2, serviceDoc3, businessDoc);
    }

    @Test
    public void whenSearchByBusinessShouldOrderAllDocsByIdAndProvideValidOffset() {
        prepareBusinessGroup();
        QualityDocument serviceDoc1 = generateDocument();
        QualityDocument serviceDoc2 = generateDocument();
        QualityDocument serviceDoc3 = generateDocument();
        QualityDocument businessDoc = generateDocument();
        MasterData serviceMD1 = generateMasterData(
            addMapping(o -> o.setSupplierId(SUPPLIER_ID).setShopSku(ANY_SHOP_SKU)),
            serviceDoc1, serviceDoc2
        );
        MasterData serviceMD2 = generateMasterData(
            addMapping(o -> o.setSupplierId(OTHER_SUPPLIER_ID).setShopSku("other")),
            serviceDoc3
        );
        MasterData businessMD = generateMasterData(
            addMapping(o -> o.setSupplierId(BUSINESS_ID).setShopSku("pew pew")),
            businessDoc
        );

        masterDataRepository.insertOrUpdateAll(
            Arrays.asList(serviceMD1, serviceMD2, businessMD)
        );
        serviceDoc1 = qualityDocumentRepository.findBy(
            new DocumentFilter().addRegistrationNumber(serviceDoc1.getRegistrationNumber())).get(0);
        serviceDoc2 = qualityDocumentRepository.findBy(
            new DocumentFilter().addRegistrationNumber(serviceDoc2.getRegistrationNumber())).get(0);
        serviceDoc3 = qualityDocumentRepository.findBy(
            new DocumentFilter().addRegistrationNumber(serviceDoc3.getRegistrationNumber())).get(0);
        businessDoc = qualityDocumentRepository.findBy(
            new DocumentFilter().addRegistrationNumber(businessDoc.getRegistrationNumber())).get(0);

        List<QualityDocument> sortedDocuments = List.of(serviceDoc1, serviceDoc2, serviceDoc3, businessDoc).stream()
            .sorted(Comparator.comparing(QualityDocument::getId)).collect(Collectors.toList());

        int limit = 2;

        var response = supplierDocumentService.findSupplierDocuments(
            MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(BUSINESS_ID)
                .setLimit(limit)
                .build()
        );

        Assertions.assertThat(response.getDocumentList()).hasSize(2);
        List<QualityDocument> responseDocs = response.getDocumentList()
            .stream()
            .map(DocumentProtoConverter::createQualityDocument)
            .collect(Collectors.toList());

        Assertions.assertThat(responseDocs).usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrderElementsOf(sortedDocuments.stream().limit(limit).collect(Collectors.toList()));

        response = supplierDocumentService.findSupplierDocuments(
            MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(BUSINESS_ID)
                .setLimit(limit)
                .setOffsetKey(response.getNextOffsetKey())
                .build()
        );

        Assertions.assertThat(response.getDocumentList()).hasSize(2);
        responseDocs = response.getDocumentList()
            .stream()
            .map(DocumentProtoConverter::createQualityDocument)
            .collect(Collectors.toList());

        Assertions.assertThat(responseDocs).usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrderElementsOf(sortedDocuments.stream().skip(limit).collect(Collectors.toList()));
    }

    @Test
    public void whenDocumentServiceThrowsRemovingExceptionsShouldReturnError() {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService =
            new SupplierDocumentServiceImpl(
                documentService,
                documentPicturesProcessorMock,
                mboMappingsService,
                qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        RuntimeException exception = new RuntimeException("Failed");
        Mockito.when(documentService.findDocumentRelations(Mockito.any(DocumentFilter.class)))
            .thenReturn(Collections.singletonList(
                DocumentOfferRelation.from(new ShopSkuKey(1, "1231456"),
                    new QualityDocument().setId(1).setRegistrationNumber("12345"))));
        Mockito.doThrow(exception)
            .when(documentService).deleteOfferRelations(Mockito.anyCollection());

        MdmDocument.DocumentOfferRelation relation = MdmDocument.DocumentOfferRelation.newBuilder()
            .setRegistrationNumber("12345")
            .setSupplierId(1)
            .setShopSku("1231456")
            .build();
        MdmDocument.RemoveDocumentOfferRelationsResponse response = supplierDocumentService
            .removeDocumentOfferRelations(
                MdmDocument.RemoveDocumentOfferRelationsRequest.newBuilder()
                    .addRelation(relation)
                    .build());

        Mockito.verify(documentService, Mockito.times(1))
            .findDocumentRelations(Mockito.any(DocumentFilter.class));
        Mockito.verify(documentService, Mockito.times(1))
            .deleteOfferRelations(Mockito.anyCollection());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdRemoveDocumentRelationsFailedRelations(1)));

            softAssertions.assertThat(response.getRelationResponseList())
                .containsExactly(DocumentResponseUtil.createRemoveRelationErrorResponse(relation,
                    MbocErrors.get().qdRemoveDocumentRelationsFailedToRemoveRelation(exception.getMessage())));
        });
    }

    @Test
    public void whenNotFoundRelationToDeleteShouldReturnError() {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService =
            new SupplierDocumentServiceImpl(
                documentService,
                documentPicturesProcessorMock,
                mboMappingsService,
                qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        Mockito.when(documentService.findDocumentRelations(Mockito.any(DocumentFilter.class)))
            .thenReturn(Collections.emptyList());

        MdmDocument.DocumentOfferRelation relation = MdmDocument.DocumentOfferRelation.newBuilder()
            .setRegistrationNumber("12345")
            .setSupplierId(1)
            .setShopSku("1231456")
            .build();
        MdmDocument.RemoveDocumentOfferRelationsResponse response = supplierDocumentService
            .removeDocumentOfferRelations(
                MdmDocument.RemoveDocumentOfferRelationsRequest.newBuilder()
                    .addRelation(relation)
                    .build());

        Mockito.verify(documentService, Mockito.times(1))
            .findDocumentRelations(Mockito.any(DocumentFilter.class));
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdRemoveDocumentRelationsFailedRelations(1)));

            softAssertions.assertThat(response.getRelationResponseList())
                .containsExactly(DocumentResponseUtil.createRemoveRelationErrorResponse(relation,
                    MbocErrors.get().qdRemoveDocumentRelationsFailedToRemoveRelation("Связь не найдена.")));
        });
    }

    @Test
    public void whenRelationCanBeDeletedShoudDeleteRelation() {
        documentService = Mockito.mock(DocumentService.class);
        supplierDocumentService =
            new SupplierDocumentServiceImpl(
                documentService,
                documentPicturesProcessorMock,
                mboMappingsService,
                qualityDocumentRepository, mdmSskuGroupManager, supplierCachingService);
        Mockito.when(documentService.findDocumentRelations(Mockito.any(DocumentFilter.class)))
            .thenReturn(Arrays.asList(
                DocumentOfferRelation.from(new ShopSkuKey(1, "some_sku"),
                    new QualityDocument().setId(1).setRegistrationNumber("12345")),
                DocumentOfferRelation.from(new ShopSkuKey(1, "other_sku"),
                    new QualityDocument().setId(1).setRegistrationNumber("12345"))));

        MdmDocument.DocumentOfferRelation relation1 = MdmDocument.DocumentOfferRelation.newBuilder()
            .setRegistrationNumber("12345")
            .setSupplierId(1)
            .setShopSku("some_sku")
            .build();
        MdmDocument.DocumentOfferRelation relation2 = MdmDocument.DocumentOfferRelation.newBuilder()
            .setRegistrationNumber("12345")
            .setSupplierId(1)
            .setShopSku("other_sku")
            .build();
        MdmDocument.RemoveDocumentOfferRelationsResponse response = supplierDocumentService
            .removeDocumentOfferRelations(
                MdmDocument.RemoveDocumentOfferRelationsRequest.newBuilder()
                    .addRelation(relation1)
                    .addRelation(relation2)
                    .build());

        Mockito.verify(documentService, Mockito.times(1))
            .findDocumentRelations(Mockito.any(DocumentFilter.class));
        Mockito.verify(documentService, Mockito.times(2))
            .deleteOfferRelations(Mockito.anyCollection());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.OK);
            softAssertions.assertThat(response.hasError()).isFalse();

            softAssertions.assertThat(response.getRelationResponseList())
                .containsExactly(
                    MdmDocument.RemoveDocumentOfferRelationsResponse.RemoveRelationResponse.newBuilder()
                        .setRelation(relation1)
                        .setStatus(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.OK)
                        .build(),
                    MdmDocument.RemoveDocumentOfferRelationsResponse.RemoveRelationResponse.newBuilder()
                        .setRelation(relation2)
                        .setStatus(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.OK)
                        .build());
        });
    }
}
