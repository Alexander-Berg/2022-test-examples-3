package ru.yandex.market.mboc.common.masterdata.services.document;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.common.imageservice.UploadImageException;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.document.DocumentFilter;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.masterdata.services.document.picture.ByteArrayMultipartFile;
import ru.yandex.market.mboc.common.masterdata.services.document.picture.QualityDocumentPictureService;
import ru.yandex.market.mboc.common.masterdata.services.document.picture.QualityDocumentPictureServiceImpl;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.MboMappingsServiceMock;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.http.MboMappings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

/**
 * @author jkt on 20.07.18.
 */
public class DocumentServiceImplTest extends MdmBaseIntegrationTestClass {

    private static final long MSKU_ID = 1234567L;
    private static final long NOT_EXISTING_MSKU_ID = 10293847L;

    private static final long SEED = 27L;
    private static final String DOCUMENT_SCAN_PDF = "masterdata/document-scan.pdf";
    private static final int SUPPLIER_ID = 999777;
    MboMappingsServiceMock mboMappingsService;
    @Autowired
    MasterDataRepository masterDataRepository;
    @Autowired
    QualityDocumentRepository qualityDocumentRepository;
    @Autowired
    DocumentServiceImpl documentService;
    @Autowired
    MdmQueuesManager mdmQueuesManager;
    @Autowired
    MdmSskuGroupManager mdmSskuGroupManager;
    private AvatarImageDepotServiceMock imageServiceMock;
    private MboMappings.ApprovedMappingInfo mapping;

    private EnhancedRandom random;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(SEED);
        imageServiceMock = Mockito.spy(new AvatarImageDepotServiceMock());
        mboMappingsService = new MboMappingsServiceMock();

        QualityDocumentPictureService docPictureService = new QualityDocumentPictureServiceImpl(imageServiceMock);
        documentService = new DocumentServiceImpl(
            mboMappingsService,
            qualityDocumentRepository,
            docPictureService,
            mdmQueuesManager,
            mdmSskuGroupManager);

        insertTestData();
    }

    public void insertTestData() {
        mapping = addOfferToDB();
    }

    @Test
    public void whenDocumentExistsShouldFindDocumentByMarketSkuId() {
        MboMappings.ApprovedMappingInfo mskuOffer = addOfferToDB(MSKU_ID);
        QualityDocument mskuOfferDocument = generateDocument();
        MasterData mskuOfferMasterData = TestDataUtils.generateMasterData(
            new ShopSkuKey(mskuOffer.getSupplierId(), mskuOffer.getShopSku()),
            random,
            mskuOfferDocument
        );

        MboMappings.ApprovedMappingInfo anotherOffer = addOfferToDB();
        QualityDocument anotherOfferDocument = generateDocument();
        MasterData anotherOfferMasterData = TestDataUtils.generateMasterData(
            new ShopSkuKey(anotherOffer.getSupplierId(), anotherOffer.getShopSku()),
            random, anotherOfferDocument
        );

        masterDataRepository.insertOrUpdateAll(Arrays.asList(mskuOfferMasterData, anotherOfferMasterData));

        List<QualityDocument> documentsFromDb = documentService.findBy(
            new DocumentOfferFilter().setApprovedMarketSku(MSKU_ID)
        );

        assertSoftly(softly -> {
            softly.assertThat(documentsFromDb).containsExactlyInAnyOrder(mskuOfferDocument);
            softly.assertThat(documentsFromDb).doesNotContain(anotherOfferDocument);
        });
    }

    @Test
    public void whenDocumentForMarketSkuDoesNotExistShouldNotFindDocumentByMarketSkuId() {
        QualityDocument document = generateDocument();
        MasterData offerMasterData = generateMasterData(document);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        List<QualityDocument> documentsFromDb = documentService.findBy(
            new DocumentOfferFilter().setApprovedMarketSku(NOT_EXISTING_MSKU_ID)
        );

        assertSoftly(softly -> {
            softly.assertThat(documentsFromDb).isEmpty();
        });
    }

    @Test
    public void whenDocumentOfferAndDocumentFiltersSpecifiedShouldUseBoth() {
        MboMappings.ApprovedMappingInfo mskuOffer = addOfferToDB(MSKU_ID);
        QualityDocument document = generateDocument();
        QualityDocument anotherDocument = generateDocument();
        MasterData mskuOfferMasterData = TestDataUtils.generateMasterData(
            new ShopSkuKey(mskuOffer.getSupplierId(), mskuOffer.getShopSku()),
            random, document, anotherDocument
        );

        masterDataRepository.insertOrUpdateAll(Arrays.asList(mskuOfferMasterData));

        List<QualityDocument> documentsFromDb = documentService.findBy(
            new DocumentOfferFilter()
                .setApprovedMarketSku(MSKU_ID)
                .setDocumentFilter(new DocumentFilter().addRegistrationNumber(document.getRegistrationNumber()))
        );

        assertSoftly(softly -> {
            softly.assertThat(documentsFromDb).containsExactlyInAnyOrder(document);
            softly.assertThat(documentsFromDb).doesNotContain(anotherDocument);
        });
    }

    @Test
    public void whenDocumentExistsShouldCountDocumentsProperly() {
        QualityDocument document = generateDocument();
        QualityDocument anotherDocument = generateDocument();
        QualityDocument[] documents = new QualityDocument[]{document, anotherDocument};

        MasterData offerMasterData = generateMasterData(documents);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        long count = documentService.getCount(
            new DocumentOfferFilter().setDocumentFilter(
                new DocumentFilter().addShopSkuKey(offerMasterData.getSupplierId(), offerMasterData.getShopSku())
            )
        );

        assertThat(count).isEqualTo(documents.length);

    }

    @Test
    public void whenCountingDocumentsShouldNotUseFilterLimitAndOffset() {
        MasterData offerMasterData = generateMasterData(generateDocument(), generateDocument(), generateDocument());
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        long count = documentService.getCount(
            new DocumentOfferFilter().setDocumentFilter(
                new DocumentFilter()
                    .addShopSkuKey(offerMasterData.getSupplierId(), offerMasterData.getShopSku())
                    .setLimit(1)
                    .setOffset(1)
            )
        );

        assertThat(count).isEqualTo(offerMasterData.getQualityDocuments().size());

    }

    @Test
    public void whenAddingRelationsShouldSaveAllFieldsCorrectly() {
        QualityDocument document = qualityDocumentRepository.insert(generateDocument());

        DocumentOfferRelation relation = DocumentOfferRelation.from(
            new ShopSkuKey(mapping.getSupplierId(), mapping.getShopSku()), document
        );

        List<DocumentOfferRelation> returnedRelations = qualityDocumentRepository.addDocumentRelations(
            Collections.singletonList(relation)
        );

        DocumentOfferRelation relationFromDb = findSingleRelation(
            new DocumentFilter().addShopSkuKey(
                new ShopSkuKey(mapping.getSupplierId(), mapping.getShopSku())
            )
        );

        assertSoftly(softly -> {
            softly.assertThat(relationFromDb).isEqualTo(relation);
            softly.assertThat(returnedRelations).containsExactly(relationFromDb);
        });

    }

    @Test
    public void whenAddingRelationsShouldSetModifyTimestampNow() {
        QualityDocument document = qualityDocumentRepository.insert(generateDocument());

        DocumentOfferRelation relation = DocumentOfferRelation.from(
            new ShopSkuKey(mapping.getSupplierId(), mapping.getShopSku()),
            document
        );
        relation.setModifiedTimestamp(null);

        LocalDateTime timeBeforeSaving = DateTimeUtils.dateTimeNow();

        qualityDocumentRepository.addDocumentRelations(Collections.singletonList(relation));

        DocumentOfferRelation relationFromDb = findSingleRelation(
            new DocumentFilter().addShopSkuKey(
                new ShopSkuKey(mapping.getSupplierId(), mapping.getShopSku())
            )
        );

        assertSoftly(softly -> {
            softly.assertThat(relationFromDb.getModifiedTimestamp()).isAfterOrEqualTo(timeBeforeSaving);
        });

    }

    @Test
    public void whenImageFileIsNotProvidedShouldNotSavePicture() {
        QualityDocument document = generateDocument();

        QualityDocument savedDocument = documentService.saveDocument(document, FileActionType.UPDATE,
            new ByteArrayMultipartFile(null, new byte[1]));

        assertSoftly(softly -> {
            softly.assertThat(savedDocument.getPictures()).isEmpty();
            softly.assertThat(savedDocument).isEqualToIgnoringGivenFields(document, "id", "picture");
        });

        Mockito.verify(imageServiceMock, Mockito.never()).addImage(any(byte[].class), anyString());
    }

    @Test
    @SuppressWarnings("checkstyle:linelength")
    public void whenSavingDocumentWithScanAsImageShouldSaveImageToImageStore() {
        QualityDocument document = generateDocument();

        QualityDocument savedDocument = documentService.saveDocument(document, FileActionType.UPDATE,
            new ByteArrayMultipartFile("picture.jpg", new byte[1]));

        assertSoftly(softly -> {
            softly.assertThat(savedDocument.getPictures())
                .containsExactlyElementsOf(imageServiceMock.getImages().values().stream()
                    .map(s -> QualityDocumentPictureServiceImpl.PIC_PREFIX + s)
                    .collect(Collectors.toList()));
            softly.assertThat(savedDocument).isEqualToIgnoringGivenFields(document, "id", "picture");
        });

        Mockito.verify(imageServiceMock, Mockito.times(1)).addImage(any(byte[].class),
            anyString());
    }

    @Test
    @SuppressWarnings("checkstyle:linelength")
    public void whenSavingDocumentWithScanAsPdfShouldStoreEachPageAsImage() {
        QualityDocument document = generateDocument();

        byte[] scanFileBytes = loadPdfBytes();
        QualityDocument savedDocument = documentService.saveDocument(document, FileActionType.UPDATE,
            new ByteArrayMultipartFile("scan.pdf", scanFileBytes));

        assertSoftly(softly -> {
            softly.assertThat(savedDocument.getPictures())
                .containsExactlyInAnyOrderElementsOf(imageServiceMock.getImages().values().stream()
                    .map(s -> QualityDocumentPictureServiceImpl.PIC_PREFIX + s)
                    .collect(Collectors.toList()));
            softly.assertThat(savedDocument).isEqualToIgnoringGivenFields(document, "id", "picture");
        });

        Mockito.verify(imageServiceMock, Mockito.times(2)).addImage(any(byte[].class), anyString());
    }

    @Test
    @SuppressWarnings("checkstyle:linelength")
    public void whenSavingDocumentScanAsPdfWithErrorsShouldThrowException() {
        QualityDocument document = generateDocument();

        byte[] incorrectScanBytes = new byte[1];

        assertThatThrownBy(() -> documentService.saveDocument(document, FileActionType.UPDATE,
            new ByteArrayMultipartFile("scan.pdf", incorrectScanBytes)))
            .isInstanceOf(UploadImageException.class);

        Mockito.verify(imageServiceMock, Mockito.never()).addImage(any(byte[].class), anyString());
    }

    @Test
    public void whenSavingDocumentShouldTrimSpacesInRegNumber() {
        QualityDocument document = generateDocument();
        String badRegNumber = " \t" + document.getRegistrationNumber()  +"  ";
        String certificationOrgRegNumber = " \t" + document.getCertificationOrgRegNumber()  +"  ";
        document.setRegistrationNumber(badRegNumber);
        document.setCertificationOrgRegNumber(certificationOrgRegNumber);

        byte[] scanFileBytes = loadPdfBytes();
        QualityDocument savedDocument = documentService.saveDocument(document, FileActionType.UPDATE,
            new ByteArrayMultipartFile("scan.pdf", scanFileBytes));
        assertThat(savedDocument.getRegistrationNumber()).isEqualTo(badRegNumber.trim());
        assertThat(savedDocument.getCertificationOrgRegNumber()).isEqualTo(certificationOrgRegNumber.trim());
    }

    @Test
    @SuppressWarnings("checkstyle:linelength")
    public void whenSavingDocumentScanShouldCheckExtensionAsLowercase() {
        QualityDocument document = generateDocument();

        byte[] scanBytes = new byte[1];

        QualityDocument savedDocument = documentService.saveDocument(document, FileActionType.UPDATE,
            new ByteArrayMultipartFile("scan.PNG", scanBytes));

        assertSoftly(softly -> {
            softly.assertThat(savedDocument.getPictures())
                .containsExactlyInAnyOrderElementsOf(imageServiceMock.getImages().values().stream()
                    .map(s -> QualityDocumentPictureServiceImpl.PIC_PREFIX + s)
                    .collect(Collectors.toList()));
            softly.assertThat(savedDocument).isEqualToIgnoringGivenFields(document, "id", "picture");
        });

        Mockito.verify(imageServiceMock, Mockito.times(1)).addImage(any(byte[].class), anyString());
    }

    @Test
    public void whenSearchingOffersOfDocumentShouldReturnOnlyRelated() {
        MboMappings.ApprovedMappingInfo firstOffer = addOfferToDB(MSKU_ID);
        QualityDocument firstDocument = qualityDocumentRepository.insert(generateDocument());
        DocumentOfferRelation firstOfferRelation = DocumentOfferRelation.from(
            new ShopSkuKey(firstOffer.getSupplierId(), firstOffer.getShopSku()), firstDocument
        );

        MboMappings.ApprovedMappingInfo anotherOffer = addOfferToDB(MSKU_ID);
        QualityDocument anotherDocument = qualityDocumentRepository.insert(generateDocument());
        DocumentOfferRelation anotherOfferRelation = DocumentOfferRelation.from(
            new ShopSkuKey(anotherOffer.getSupplierId(), anotherOffer.getShopSku()), anotherDocument
        );

        qualityDocumentRepository.insertOrUpdateRelations(Arrays.asList(firstOfferRelation, anotherOfferRelation));

        List<ShopSkuKey> firstDocumentOffersFromDb = documentService.findOfferShopSkuKeys(firstDocument);

        assertSoftly(softly -> {
            softly.assertThat(firstDocumentOffersFromDb).containsExactlyInAnyOrder(
                new ShopSkuKey(firstOffer.getSupplierId(), firstOffer.getShopSku())
            );
            softly.assertThat(firstDocumentOffersFromDb).doesNotContain(
                new ShopSkuKey(anotherOffer.getSupplierId(), anotherOffer.getShopSku())
            );
        });

    }

    @Test
    public void whenDocumentDoesNotExistShouldReturnNoneOffers() {
        MboMappings.ApprovedMappingInfo anotherOffer = addOfferToDB();
        QualityDocument anotherDocument = qualityDocumentRepository.insert(generateDocument());
        DocumentOfferRelation anotherOfferRelation = DocumentOfferRelation.from(
            new ShopSkuKey(anotherOffer.getSupplierId(), anotherOffer.getShopSku()),
            anotherDocument
        );

        qualityDocumentRepository.insertOrUpdateRelations(Collections.singleton(anotherOfferRelation));

        QualityDocument nonExistingDocument = generateDocument();
        List<ShopSkuKey> offersFromDb = documentService.findOfferShopSkuKeys(nonExistingDocument);

        Assertions.assertThat(offersFromDb).isEmpty();
    }

    @Test
    public void addNewPics() {
        QualityDocument document = generateDocument();

        QualityDocument firstSavedDoc = documentService.saveDocument(document, FileActionType.UPDATE,
            new ByteArrayMultipartFile("1.png", new byte[1]));
        assertThat(firstSavedDoc.getPictures().size()).isEqualTo(1);

        QualityDocument secondSavedDoc = documentService.saveDocument(firstSavedDoc, FileActionType.ADD,
            new ByteArrayMultipartFile("2.png", new byte[1]));
        Mockito.verify(imageServiceMock, Mockito.times(2)).addImage(any(byte[].class), anyString());
        assertThat(secondSavedDoc.getPictures().size()).isEqualTo(2);
    }

    @Test
    public void deletePics() {
        QualityDocument document = generateDocument();

        document = documentService.saveDocument(document, FileActionType.UPDATE,
            new ByteArrayMultipartFile("1.png", new byte[1]));
        assertThat(document.getPictures().size()).isEqualTo(1);

        String removedPic = document.getPictures().get(0).replace(QualityDocumentPictureServiceImpl.PIC_PREFIX, "");
        assertThat(imageServiceMock.isImageExist(removedPic)).isEqualTo(true);

        document.setPictures(Lists.emptyList());
        document = documentService.saveDocument(document, FileActionType.UPDATE,
            new ByteArrayMultipartFile(null, new byte[1]));

        assertThat(document.getPictures()).hasSize(0);
        assertThat(imageServiceMock.isImageExist(removedPic)).isEqualTo(false);
    }

    @Test
    public void whenDeleteDocumentShouldFindById() {
        QualityDocument document = generateDocument();
        MasterData masterData = generateMasterData(document);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(masterData));

        QualityDocument byIdBeforeDelete = qualityDocumentRepository.findById(document.getId());

        document.getMetadata().setDeleted(true);
        documentService.saveDocument(document, null);

        QualityDocument byIdAfterDelete = qualityDocumentRepository.findById(document.getId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(byIdBeforeDelete).isEqualToIgnoringGivenFields(document, "metadata");
            s.assertThat(byIdBeforeDelete.getMetadata())
                .isEqualToIgnoringGivenFields(document.getMetadata(),
                    "deleted", "deleteDate", "lastUpdateDate");
            s.assertThat(byIdAfterDelete).isEqualTo(document);
        });
    }

    @Test
    public void whenDeleteDocumentIsNullShouldList() {
        QualityDocument document = generateDocument();
        document.getMetadata().setDeleted(null);
        MasterData masterData = generateMasterData(document);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(masterData));

        DocumentOfferFilter filter = new DocumentOfferFilter()
            .setShopSku(masterData.getShopSkuKey())
            .setDocumentFilter(new DocumentFilter()
                .setRegistrationNumberSearch(document.getRegistrationNumber())
            );

        QualityDocument byIdBeforeDelete = qualityDocumentRepository.findById(document.getId());
        List<QualityDocument> byShopSkuBeforeDelete = documentService.findBy(filter);
        long documentCountBeforeDelete = documentService.getCount(filter);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(byIdBeforeDelete).isEqualToIgnoringGivenFields(document, "metadata");
            s.assertThat(byIdBeforeDelete.getMetadata())
                .isEqualToIgnoringGivenFields(document.getMetadata(),
                    "deleted", "deleteDate", "lastUpdateDate");
            s.assertThat(byShopSkuBeforeDelete).containsExactly(byIdBeforeDelete);
            s.assertThat(documentCountBeforeDelete).isEqualTo(1);
        });
    }

    @Test
    public void whenDeleteDocumentShouldNotList() {
        QualityDocument document = generateDocument();
        MasterData masterData = generateMasterData(document);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(masterData));

        DocumentOfferFilter filter = new DocumentOfferFilter().setShopSku(masterData.getShopSkuKey());
        filter.getDocumentFilter().setRegistrationNumberSearch(document.getRegistrationNumber());

        QualityDocument byIdBeforeDelete = qualityDocumentRepository.findById(document.getId());
        List<QualityDocument> byShopSkuBeforeDelete = documentService.findBy(filter);
        long documentCountBeforeDelete = documentService.getCount(filter);

        document.getMetadata().setDeleted(true);
        documentService.saveDocument(document, null);

        List<QualityDocument> byShopSkuAfterDelete = documentService.findBy(filter);
        long documentCountAfterDelete = documentService.getCount(filter);


        SoftAssertions.assertSoftly(s -> {
            s.assertThat(byIdBeforeDelete).isEqualToIgnoringGivenFields(document, "metadata");
            s.assertThat(byIdBeforeDelete.getMetadata())
                .isEqualToIgnoringGivenFields(document.getMetadata(),
                    "deleted", "deleteDate", "lastUpdateDate");
            s.assertThat(byShopSkuBeforeDelete).containsExactly(byIdBeforeDelete);
            s.assertThat(documentCountBeforeDelete).isEqualTo(1);

            s.assertThat(byShopSkuAfterDelete).isEmpty();
            s.assertThat(documentCountAfterDelete).isEqualTo(0);
        });
    }

    @Test
    public void whenNotFoundDocumentShouldReturnOptionalEmpty() {
        Optional<DocumentOfferRelation> relationOptional = documentService
            .addDocumentRelation("some_number", new ShopSkuKey(1, "sup/b/"));
        Assertions.assertThat(relationOptional).isNotPresent();
    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void whenAddingRelationShouldReturnCorrectRelation() {
        QualityDocument qualityDocument = generateDocument();
        qualityDocumentRepository.insert(qualityDocument);
        Optional<DocumentOfferRelation> relationOptional = documentService
            .addDocumentRelation(qualityDocument.getRegistrationNumber(),
                new ShopSkuKey(SUPPLIER_ID, mapping.getShopSku()));
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(relationOptional).isPresent();
            DocumentOfferRelation relation = relationOptional.get();
            softAssertions.assertThat(relation.getDocumentId()).isEqualTo(qualityDocument.getId());
            softAssertions.assertThat(relation.getSupplierId()).isEqualTo(SUPPLIER_ID);
            softAssertions.assertThat(relation.getShopSku()).isEqualTo(mapping.getShopSku());
        });
    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void whenAddingNewRelationShouldInsertIt() {
        QualityDocument qualityDocument = generateDocument();
        qualityDocumentRepository.insert(qualityDocument);

        Optional<DocumentOfferRelation> relationOptional = documentService
            .addDocumentRelation(qualityDocument.getRegistrationNumber(),
                new ShopSkuKey(SUPPLIER_ID, mapping.getShopSku()));
        List<DocumentOfferRelation> relations = qualityDocumentRepository.findRelations(new DocumentFilter()
            .setRegistrationNumberSearch(qualityDocument.getRegistrationNumber()));

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(relationOptional).isPresent();
            softAssertions.assertThat(relations).hasSize(1);
            DocumentOfferRelation relation = relations.get(0);
            softAssertions.assertThat(relation).isEqualTo(relationOptional.get());

            softAssertions.assertThat(relation.getDocumentId()).isEqualTo(qualityDocument.getId());
            softAssertions.assertThat(relation.getSupplierId()).isEqualTo(SUPPLIER_ID);
            softAssertions.assertThat(relation.getShopSku()).isEqualTo(mapping.getShopSku());
        });
    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void whenRelationExistsShouldNotUpdateIt() {
        QualityDocument qualityDocument = generateDocument();
        MasterData masterData = generateMasterData(qualityDocument);
        masterDataRepository.insert(masterData);
        List<DocumentOfferRelation> relationsBefore = qualityDocumentRepository.findRelations(new DocumentFilter()
            .setRegistrationNumberSearch(qualityDocument.getRegistrationNumber()));

        Optional<DocumentOfferRelation> relationOptional = documentService
            .addDocumentRelation(qualityDocument.getRegistrationNumber(),
                new ShopSkuKey(SUPPLIER_ID, mapping.getShopSku()));

        List<DocumentOfferRelation> relationsAfter = qualityDocumentRepository.findRelations(new DocumentFilter()
            .setRegistrationNumberSearch(qualityDocument.getRegistrationNumber()));

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(relationOptional).isPresent();
            softAssertions.assertThat(relationsAfter).hasSize(1);
            softAssertions.assertThat(relationsBefore).hasSize(1);

            DocumentOfferRelation relationBefore = relationsBefore.get(0);
            DocumentOfferRelation relationAfter = relationsAfter.get(0);
            DocumentOfferRelation returnedRelation = relationOptional.get();

            // релейшены могу создаваться и удаляться. Обновлять дату создания каждый раз не надо.
            // Потому что данные то теже самые. Это специально заложено в коде upsert-а.
            softAssertions.assertThat(relationAfter).isEqualTo(relationBefore);

            softAssertions.assertThat(returnedRelation.getDocumentId()).isEqualTo(qualityDocument.getId());
            softAssertions.assertThat(returnedRelation.getSupplierId()).isEqualTo(SUPPLIER_ID);
            softAssertions.assertThat(returnedRelation.getShopSku()).isEqualTo(mapping.getShopSku());
        });
    }

    @Test
    public void whenFindDocumentRelationsShouldCallQualityDocumentRepository() {
        qualityDocumentRepository = Mockito.mock(QualityDocumentRepository.class);
        documentService = new DocumentServiceImpl(
            mboMappingsService,
            qualityDocumentRepository,
            Mockito.mock(QualityDocumentPictureService.class),
            mdmQueuesManager, mdmSskuGroupManager);

        DocumentFilter filter = new DocumentFilter()
            .setRelationSupplierId(SUPPLIER_ID)
            .addRegistrationNumber("1")
            .addRegistrationNumber("2");

        documentService.findDocumentRelations(filter);

        ArgumentCaptor<DocumentFilter> requestCaptor = ArgumentCaptor.forClass(DocumentFilter.class);
        Mockito.verify(qualityDocumentRepository, times(1))
            .findRelations(requestCaptor.capture());
        Assertions.assertThat(requestCaptor.getValue().getRelationSupplierIds())
            .isEqualTo(Set.of(SUPPLIER_ID));
        Assertions.assertThat(requestCaptor.getValue().getRegistrationNumbers())
            .containsExactly("1", "2");
    }

    private DocumentOfferRelation findSingleRelation(DocumentFilter documentFilter) {
        List<DocumentOfferRelation> relations = documentService.findDocumentRelations(documentFilter);
        if (relations.size() != 1) {
            throw new IllegalStateException("Only one relation should have been found, but got: " + relations);
        }
        return relations.get(0);
    }

    private byte[] loadPdfBytes() {
        try {
            return IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(DOCUMENT_SCAN_PDF));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed loadig bytes of resource: " + DOCUMENT_SCAN_PDF, ex);
        }
    }

    private MboMappings.ApprovedMappingInfo addOfferToDB() {
        return addOfferToDB(random.nextLong());
    }

    private MboMappings.ApprovedMappingInfo addOfferToDB(long marketSkuId) {
        MboMappings.ApprovedMappingInfo result = TestDataUtils
            .generateCorrectApprovedMappingInfoBuilder(random)
            .setSupplierId(SUPPLIER_ID)
            .setMarketSkuId(marketSkuId)
            .build();

        mboMappingsService.addMapping(result);
        return result;
    }

    private MasterData generateMasterData(QualityDocument... documents) {
        return TestDataUtils.generateMasterData(
            mapping.getShopSku(),
            mapping.getSupplierId(),
            random,
            documents
        );
    }

    private QualityDocument generateDocument() {
        return TestDataUtils.generateCorrectDocument(random);
    }
}
