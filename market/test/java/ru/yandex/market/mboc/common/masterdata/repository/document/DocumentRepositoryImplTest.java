package ru.yandex.market.mboc.common.masterdata.repository.document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.model.metadata.MdmSource;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepositoryImpl;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author jkt on 20.07.18.
 */
@SuppressWarnings({"checkstyle:magicnumber"})
public class DocumentRepositoryImplTest extends MdmBaseDbTestClass {

    private static final int SEARCH_LIMIT = 1;
    private static final int OFFSET = 1;
    private static final int SUPPLIER_ID = 222;
    private static final int OTHER_SUPPLIER_ID = 333;

    private static final String NOT_EXISTING = "NOT_EXISTING";
    private static final String SEARCH_QUERY = "PREFIX_РУС.ёЙ";
    private static final long SEED = 242L;
    private static final String MODIFIED_TIMESTAMP = "modifiedTimestamp";

    private static final Comparator<DocumentOfferRelation> DOCUMENT_OFFER_RELATION_COMPARATOR =
        (r1, r2) -> equalsWithoutModifiedTimestamp(r1, r2) ? 0 : 1;

    @Autowired
    MasterDataRepositoryImpl masterDataRepository;

    @Autowired
    QualityDocumentRepositoryImpl qualityDocumentRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private ShopSkuKey offer;

    private EnhancedRandom random;

    private static boolean equalsWithoutModifiedTimestamp(DocumentOfferRelation r1, DocumentOfferRelation r2) {
        return EqualsBuilder.reflectionEquals(r1, r2, "modifiedTimestamp");
    }

    @Before
    public void insertOffersAndSuppliers() {
        random = TestDataUtils.defaultRandom(SEED);
        offer = addOfferToDB();
    }

    @Test
    public void whenDocumentExistsShouldFindDocumentByShopSku() {
        QualityDocument document = generateDocument();
        MasterData offerMasterData = generateMasterData(document);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        List<QualityDocument> documentsFromDb = qualityDocumentRepository.findBy(new DocumentFilter()
            .addShopSkuKey(offerMasterData.getShopSkuKey())
        );

        assertSoftly(softly -> {
            softly.assertThat(documentsFromDb).containsExactlyInAnyOrder(document);
        });
    }

    @Test
    public void whenFilteringForDocumentsShouldFindOnlyMatchingCriteria() {
        QualityDocument document = generateDocument();
        QualityDocument anotherDocument = generateDocument();
        MasterData offerMasterData = generateMasterData(document, anotherDocument);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        List<QualityDocument> documentsFromDb = qualityDocumentRepository.findBy(new DocumentFilter()
            .addRegistrationNumber(document.getRegistrationNumber())
        );

        assertSoftly(softly -> {
            softly.assertThat(documentsFromDb).containsExactlyInAnyOrder(document);
            softly.assertThat(documentsFromDb).doesNotContain(anotherDocument);
        });
    }

    @Test
    public void whenSearchingRelationsShouldFindOnlyMatchingSpecifiedDocument() {
        QualityDocument document = generateDocument();
        QualityDocument anotherDocument = generateDocument();
        MasterData offerMasterData = generateMasterData(document);
        MasterData anotherMasterData = generateMasterData(anotherDocument);
        masterDataRepository.insertOrUpdateAll(Arrays.asList(offerMasterData, anotherMasterData));

        List<DocumentOfferRelation> documentRelations = qualityDocumentRepository.findRelations(new DocumentFilter()
            .addRegistrationNumber(document.getRegistrationNumber())
        );

        assertSoftly(softly -> {
            softly.assertThat(documentRelations)
                .usingElementComparator(DOCUMENT_OFFER_RELATION_COMPARATOR)
                .containsExactlyInAnyOrderElementsOf(DocumentOfferRelation.fromMasterData(offerMasterData));
            softly.assertThat(documentRelations)
                .doesNotContainAnyElementsOf(DocumentOfferRelation.fromMasterData(anotherMasterData));
        });
    }

    @Test
    public void whenMultipleRelationsExistShouldFindAll() {
        QualityDocument document = qualityDocumentRepository.insert(generateDocument());

        List<DocumentOfferRelation> relations = Arrays.asList(generateRelation(document), generateRelation(document));
        qualityDocumentRepository.insertOrUpdateRelations(relations);

        List<DocumentOfferRelation> documentRelations = qualityDocumentRepository.findRelations(new DocumentFilter()
            .addRegistrationNumber(document.getRegistrationNumber())
        );

        assertThat(documentRelations).containsExactlyInAnyOrderElementsOf(relations);
    }

    @Test
    public void whenFilteringByShopSkuShouldFindOnlyMatchingThisShopSku() {
        QualityDocument document = qualityDocumentRepository.insert(generateDocument());

        DocumentOfferRelation offerRelation = generateRelation(document);
        DocumentOfferRelation anotherOfferRelation = generateRelation(document);
        qualityDocumentRepository.insertOrUpdateRelations(Arrays.asList(offerRelation, anotherOfferRelation));

        List<DocumentOfferRelation> documentRelations = qualityDocumentRepository.findRelations(new DocumentFilter()
            .addShopSkuKey(offerRelation.getShopSkuKey())
        );

        assertSoftly(softly -> {
            softly.assertThat(documentRelations).containsExactly(offerRelation);
            softly.assertThat(documentRelations).doesNotContain(anotherOfferRelation);
        });
    }

    @Test
    public void whenRelationDoesNotExistShouldFindNone() {
        QualityDocument document = qualityDocumentRepository.insert(generateDocument());

        DocumentOfferRelation offerRelation = generateRelation(document);
        qualityDocumentRepository.insertOrUpdateRelations(Collections.singleton(offerRelation));

        List<DocumentOfferRelation> documentRelations = qualityDocumentRepository.findRelations(new DocumentFilter()
            .addRegistrationNumber(NOT_EXISTING)
        );

        assertThat(documentRelations).isEmpty();
    }

    @Test
    public void whenSearchingBySubstringShouldReturnOnlyMatching() {
        QualityDocument document = qualityDocumentRepository.insert(generateDocument(SEARCH_QUERY));
        QualityDocument anotherDocument = qualityDocumentRepository.insert(generateDocument());

        List<QualityDocument> documentsFromDb = qualityDocumentRepository.findBy(new DocumentFilter()
            .setRegistrationNumberSearch(SEARCH_QUERY)
        );

        assertSoftly(softly -> {
            softly.assertThat(documentsFromDb).containsExactly(document);
            softly.assertThat(documentsFromDb).doesNotContain(anotherDocument);
        });
    }

    @Test
    public void whenSearchingBySubstringShouldReturnMultipleDocumentsIfMatching() {
        QualityDocument document = qualityDocumentRepository.insert(generateDocument(SEARCH_QUERY));
        QualityDocument anotherDocument = qualityDocumentRepository.insert(generateDocument(SEARCH_QUERY));

        List<QualityDocument> documentsFromDb = qualityDocumentRepository.findBy(new DocumentFilter()
            .setRegistrationNumberSearch(SEARCH_QUERY)
        );

        assertThat(documentsFromDb).containsExactlyInAnyOrder(document, anotherDocument);

    }

    @Test
    public void whenSearchingBySubstringShouldReturnNoneIfEmpty() {
        qualityDocumentRepository.insert(generateDocument(SEARCH_QUERY));

        List<QualityDocument> documentsFromDb = qualityDocumentRepository.findBy(new DocumentFilter()
            .setRegistrationNumberSearch("another_search_query")
        );

        assertThat(documentsFromDb).isEmpty();

    }

    @Test
    public void whenLimitSpecifiedShouldSearchExactlyThatAmount() {
        QualityDocument[] documents = {generateDocument(), generateDocument()};
        MasterData offerMasterData = generateMasterData(documents);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        List<QualityDocument> documentsFromDb = qualityDocumentRepository.findBy(new DocumentFilter()
            .addShopSkuKey(new ShopSkuKey(offerMasterData.getSupplierId(), offerMasterData.getShopSku()))
            .setLimit(SEARCH_LIMIT)
        );

        assertSoftly(softly -> {
            softly.assertThat(documentsFromDb).hasSize(SEARCH_LIMIT);
            softly.assertThat(asRegistrationNumbers(documentsFromDb.stream()))
                .isSubsetOf(asRegistrationNumbers(Stream.of(documents)));
        });
    }

    @Test
    public void whenOffsetSpecifiedShouldSkipOffset() {
        QualityDocument[] documents = {generateDocument(), generateDocument(), generateDocument()};

        MasterData offerMasterData = generateMasterData(documents);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        List<QualityDocument> documentsFromDb = qualityDocumentRepository.findBy(new DocumentFilter()
            .addShopSkuKey(new ShopSkuKey(offerMasterData.getSupplierId(), offerMasterData.getShopSku()))
            .setOffset(OFFSET)
        );

        assertSoftly(softly -> {
            softly.assertThat(documentsFromDb).hasSize(documents.length - OFFSET);
            softly.assertThat(asRegistrationNumbers(documentsFromDb.stream()))
                .isSubsetOf(asRegistrationNumbers(Stream.of(documents)));
        });
    }

    @Test
    public void whenQueringForCountShouldREturnCorrectCount() {
        QualityDocument document = generateDocument();
        QualityDocument anotherDocument = generateDocument();

        QualityDocument[] qualityDocuments = {document, anotherDocument};

        MasterData offerMasterData = generateMasterData(qualityDocuments);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        long count = qualityDocumentRepository.getCount(new DocumentFilter()
            .addShopSkuKey(new ShopSkuKey(offerMasterData.getSupplierId(), offerMasterData.getShopSku()))
        );

        assertThat(count).isEqualTo(qualityDocuments.length);
    }

    @Test
    public void whenChangingExistingDocumentToAnotherDocumentRegistrationNumberShouldFail() {
        List<QualityDocument> qualityDocuments = Arrays.asList(generateDocument(), generateDocument());
        List<QualityDocument> documentsFromDb = qualityDocumentRepository.insertOrUpdateAll(qualityDocuments);

        QualityDocument documentFromDb = documentsFromDb.get(0);
        QualityDocument anotherDocumentFromDb = documentsFromDb.get(1);

        assertThatThrownBy(() -> {
            documentFromDb.setRegistrationNumber(anotherDocumentFromDb.getRegistrationNumber());
            qualityDocumentRepository.insertOrUpdateAll(Collections.singletonList(documentFromDb));
        }).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void whenChangingExistingDocumentShouldFindByRegistrationNumberAndUpdate() {
        QualityDocument existingDocument = qualityDocumentRepository.insert(generateDocument());

        QualityDocument newDocumentSameRegNumber = generateDocument();
        newDocumentSameRegNumber.setRegistrationNumber(existingDocument.getRegistrationNumber());

        qualityDocumentRepository.insertOrUpdateAll(Collections.singletonList(newDocumentSameRegNumber));
        QualityDocument documentFromDb = qualityDocumentRepository.findById(existingDocument.getId());

        assertSoftly(softly -> {
            softly.assertThat(documentFromDb).isEqualToIgnoringGivenFields(newDocumentSameRegNumber, "id");
            softly.assertThat(documentFromDb.getId()).isEqualTo(existingDocument.getId());
        });
    }

    @Test
    public void whenUpdatingDeletedDocumentShouldFindItAndUpdate() {
        QualityDocument generatedDocument = generateDocument();
        generatedDocument.getMetadata().setDeleted(true);
        QualityDocument deletedDocument = qualityDocumentRepository.insert(generatedDocument);
        assertThat(deletedDocument.getMetadata().isDeleted()).isEqualTo(true);

        QualityDocument newDocumentSameRegNumber = generateDocument();
        newDocumentSameRegNumber.setRegistrationNumber(deletedDocument.getRegistrationNumber());

        qualityDocumentRepository.insertOrUpdateAll(Collections.singletonList(newDocumentSameRegNumber));
        QualityDocument documentFromDb = qualityDocumentRepository.findById(deletedDocument.getId());

        assertSoftly(softly -> {
            softly.assertThat(documentFromDb).isEqualToIgnoringGivenFields(newDocumentSameRegNumber, "id");
            softly.assertThat(documentFromDb.getId()).isEqualTo(deletedDocument.getId());
        });
    }

    @Test
    public void whenSavingDocumentShouldSaveAllTypes() {
        List<QualityDocument> documents = Stream.of(QualityDocument.QualityDocumentType.values())
            .map(this::generateDocument)
            .collect(Collectors.toList());

        List<QualityDocument.QualityDocumentType> typesFromDb = new ArrayList<>();

        assertSoftly(softly -> {
            softly.assertThatCode(() -> {
                    qualityDocumentRepository.insertOrUpdateAll(documents);
                }
            ).doesNotThrowAnyException();

            List<QualityDocument> documentsFromDb = qualityDocumentRepository.findAll();
            documentsFromDb.forEach(document -> typesFromDb.add(document.getType()));

            softly.assertThat(typesFromDb).containsExactlyInAnyOrder(QualityDocument.QualityDocumentType.values());
        });
    }

    @Test
    public void whenSavingDocumentWithMultiplePicturesShouldSaveAll() {
        QualityDocument document = qualityDocumentRepository.insert(
            generateDocumentWithPictures(generatePicture(), generatePicture(), generatePicture())
        );

        QualityDocument documentFromDb = qualityDocumentRepository.findById(document.getId());

        assertThat(documentFromDb.getPictures()).containsExactlyInAnyOrderElementsOf(document.getPictures());
    }

    @Test
    public void whenSavingDocumentWithoutPicturesShouldSaveEmptyArray() {
        QualityDocument document = qualityDocumentRepository.insert(
            generateDocumentWithPictures()
        );

        QualityDocument documentFromDb = qualityDocumentRepository.findById(document.getId());

        assertThat(documentFromDb.getPictures()).isEmpty();
    }

    @Test
    public void whenSavingDocumentWithoutSourceShouldNotSaveSource() {
        QualityDocument document = generateDocument();
        document.getMetadata().setSource(null);

        qualityDocumentRepository.insert(document);

        QualityDocument documentFromDb = qualityDocumentRepository.findById(document.getId());

        assertThat(documentFromDb.getMetadata().getSource()).isNull();
    }

    @Test
    public void whenDocumentExistsShouldFindDocumentByCreatedBySupplierId() {
        int supplierId = random.nextInt();
        int otherSupplierId = supplierId + 1;

        QualityDocument noMetadataDocument = generateDocument().setMetadata(null);
        QualityDocument wrongSourceMetadataDocument = generateDocument().setMetadata(
            new QualityDocument.Metadata().setSource(MdmSource.WAREHOUSE).setCreatedBy(supplierId));
        QualityDocument wrongCreatedByMetadataDocument = generateDocument().setMetadata(
            new QualityDocument.Metadata().setSource(MdmSource.SUPPLIER).setCreatedBy(otherSupplierId));
        QualityDocument documentWithMetadata = generateDocument().setMetadata(
            new QualityDocument.Metadata().setSource(MdmSource.SUPPLIER).setCreatedBy(supplierId));

        qualityDocumentRepository.insertOrUpdateAll(Arrays.asList(
            noMetadataDocument, wrongSourceMetadataDocument, wrongCreatedByMetadataDocument,
            documentWithMetadata));

        List<QualityDocument> documentsFromDb = qualityDocumentRepository.findBy(
            new DocumentFilter().setSupplierDocSearchCriteria(supplierId, null));

        assertSoftly(softly -> softly.assertThat(documentsFromDb).containsExactlyInAnyOrder(documentWithMetadata));
    }

    @Test
    public void whenDocumentExistsShouldFindDocumentBySupplierId() {
        int createdBySupplierId = SUPPLIER_ID + 1;
        QualityDocument documentWithSupplierId = generateDocument().setMetadata(
            new QualityDocument.Metadata().setSource(MdmSource.SUPPLIER).setCreatedBy(createdBySupplierId));
        MasterData offerMasterData = generateMasterData(documentWithSupplierId);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        List<QualityDocument> documentsFromDb = qualityDocumentRepository.findBy(
            new DocumentFilter().setSupplierDocSearchCriteria(SUPPLIER_ID, null));

        assertSoftly(softly -> softly.assertThat(documentsFromDb)
            .containsExactlyInAnyOrder(documentWithSupplierId));
    }

    @Test
    public void whenIdOffsetKeyProvidedShouldFindOnlyGreaterIdsThanIdOffsetKey() {
        QualityDocument document1 = generateDocument();
        QualityDocument document2 = generateDocument();

        MasterData offerMasterData = generateMasterData(document1, document2);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        List<QualityDocument> inserted = qualityDocumentRepository
            .insertOrUpdateAll(Arrays.asList(document1, document2));

        long lesserId = inserted.stream().map(QualityDocument::getId).min(Long::compareTo).get();
        List<QualityDocument> documentWithGreaterId = inserted.stream()
            .filter(qualityDocument -> qualityDocument.getId() != lesserId)
            .collect(Collectors.toList());
        List<QualityDocument> documentsFromDb = qualityDocumentRepository.findBy(new DocumentFilter()
            .setSupplierDocSearchCriteria(offerMasterData.getSupplierId(), null)
            .setIdOffsetKey(lesserId)
        );
        assertSoftly(softly -> {
            softly.assertThat(documentsFromDb).hasSize(1);
            softly.assertThat(documentsFromDb).containsAll(documentWithGreaterId);
        });
    }

    @Test
    public void whenDocumentExistsShouldFindDocumentByRelationSupplierId() {
        QualityDocument document = generateDocument();
        MasterData offerMasterData = generateMasterData(document);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));
        QualityDocument documentWithoutRelation = generateDocument();
        qualityDocumentRepository.insert(documentWithoutRelation);

        List<QualityDocument> documentsFromDb = qualityDocumentRepository.findBy(
            new DocumentFilter().setRelationSupplierId(SUPPLIER_ID));

        assertSoftly(softly -> softly.assertThat(documentsFromDb)
            .containsExactlyInAnyOrder(document));
    }

    @Test
    public void testLimitAndOffsetUsage() {
        String registrationNumber = "test_";
        QualityDocument document1 = generateDocument().setRegistrationNumber(registrationNumber + "doc1");
        qualityDocumentRepository.insert(document1);
        QualityDocument document2 = generateDocument().setRegistrationNumber(registrationNumber + "doc2");
        qualityDocumentRepository.insert(document2);
        QualityDocument document3 = generateDocument().setRegistrationNumber(registrationNumber + "doc3");
        qualityDocumentRepository.insert(document3);

        ShopSkuKey shopSkuKey1 = addOfferToDB();
        ShopSkuKey shopSkuKey2 = addOfferToDB();
        ShopSkuKey shopSkuKey3 = addOfferToDB();

        masterDataRepository.insertOrUpdateAll(Arrays.asList(
            generateMasterData(shopSkuKey1, document1, document2, document3),
            generateMasterData(shopSkuKey2, document2, document3),
            generateMasterData(shopSkuKey3, document3)
        ));

        SoftAssertions.assertSoftly(softAssertions -> {
            // no limit
            softAssertions.assertThat(qualityDocumentRepository.findBy(
                new DocumentFilter().setSupplierDocSearchCriteria(SUPPLIER_ID, registrationNumber)))
                .containsExactlyInAnyOrder(document1, document2, document3);
            // limit 2
            softAssertions.assertThat(qualityDocumentRepository.findBy(
                new DocumentFilter()
                    .setSupplierDocSearchCriteria(SUPPLIER_ID, registrationNumber)
                    .setLimit(2)))
                .containsExactly(document1, document2);
            // limit 2 offset 2
            softAssertions.assertThat(qualityDocumentRepository.findBy(
                new DocumentFilter()
                    .setSupplierDocSearchCriteria(SUPPLIER_ID, registrationNumber)
                    .setLimit(2)
                    .setOffset(2)))
                .containsExactly(document3);
            softAssertions.assertThat(qualityDocumentRepository.getCount(
                new DocumentFilter()
                    .setSupplierDocSearchCriteria(SUPPLIER_ID, registrationNumber)))
                .isEqualTo(3);

            // relations no limit
            softAssertions.assertThat(qualityDocumentRepository.findRelations(
                new DocumentFilter().setSupplierDocSearchCriteria(SUPPLIER_ID, registrationNumber)))
                .usingElementComparatorIgnoringFields("modifiedTimestamp")
                .containsExactlyInAnyOrder(
                    DocumentOfferRelation.from(shopSkuKey1, document1),
                    DocumentOfferRelation.from(shopSkuKey1, document2),
                    DocumentOfferRelation.from(shopSkuKey2, document2),
                    DocumentOfferRelation.from(shopSkuKey1, document3),
                    DocumentOfferRelation.from(shopSkuKey2, document3),
                    DocumentOfferRelation.from(shopSkuKey3, document3));
            // relations limit 1
            softAssertions.assertThat(qualityDocumentRepository.findRelations(
                new DocumentFilter()
                    .setSupplierDocSearchCriteria(SUPPLIER_ID, registrationNumber)
                    .setLimit(1)))
                .usingElementComparatorIgnoringFields("modifiedTimestamp")
                .containsExactly(DocumentOfferRelation.from(shopSkuKey1, document1));
            // relations limit 2, offset 1
            softAssertions.assertThat(qualityDocumentRepository.findRelations(
                new DocumentFilter()
                    .setSupplierDocSearchCriteria(SUPPLIER_ID, registrationNumber)
                    .setLimit(2)
                    .setOffset(1)))
                .usingElementComparatorIgnoringFields("modifiedTimestamp")
                .containsExactlyInAnyOrder(
                    DocumentOfferRelation.from(shopSkuKey1, document2),
                    DocumentOfferRelation.from(shopSkuKey2, document2));
        });
    }

    @Test
    public void whenDocumentExistsShouldFindItsRelationsBySupplierId() {
        QualityDocument document = generateDocument();
        MasterData offerMasterData = generateMasterData(document);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));
        QualityDocument documentWithoutRelation = generateDocument();
        qualityDocumentRepository.insert(documentWithoutRelation);

        List<DocumentOfferRelation> relations = qualityDocumentRepository.findRelations(
            new DocumentFilter().setRelationSupplierId(SUPPLIER_ID));

        assertSoftly(softly -> {
            DocumentOfferRelation expectedRelation = DocumentOfferRelation
                .from(new ShopSkuKey(SUPPLIER_ID, offer.getShopSku()), document);

            relations.forEach(relation -> {
                softly.assertThat(relation)
                    .isEqualToIgnoringGivenFields(expectedRelation, MODIFIED_TIMESTAMP);
                softly.assertThat(relation.getModifiedTimestamp())
                    .isAfterOrEqualTo(DateTimeUtils.dateTimeNow().minusSeconds(5));
            });
        });
    }

    @Test
    public void whenDocumentExistsShouldNotRelationsForOtherSupplierId() {
        QualityDocument document = generateDocument();
        MasterData offerMasterData = generateMasterData(document);

        ShopSkuKey otherSupplierShopSku = addOfferToDB(OTHER_SUPPLIER_ID);
        QualityDocument otherSupplierDoc = generateDocument();
        MasterData otherSupplierMasterData = generateMasterData(otherSupplierShopSku, otherSupplierDoc);

        masterDataRepository.insertOrUpdateAll(Arrays.asList(offerMasterData, otherSupplierMasterData));

        List<DocumentOfferRelation> supplierRelations = qualityDocumentRepository.findRelations(
            new DocumentFilter()
                .setRelationSupplierId(SUPPLIER_ID));
        List<DocumentOfferRelation> otherSupplierRelations = qualityDocumentRepository.findRelations(
            new DocumentFilter()
                .setRelationSupplierId(OTHER_SUPPLIER_ID));

        assertSoftly(softly -> {
            softly.assertThat(supplierRelations).hasSize(1);
            softly.assertThat(otherSupplierRelations).hasSize(1);

            softly.assertThat(supplierRelations.get(0)).isEqualToIgnoringGivenFields(
                DocumentOfferRelation.from(new ShopSkuKey(SUPPLIER_ID, offer.getShopSku()), document),
                MODIFIED_TIMESTAMP
            );

            softly.assertThat(otherSupplierRelations.get(0)).isEqualToIgnoringGivenFields(
                DocumentOfferRelation
                    .from(new ShopSkuKey(OTHER_SUPPLIER_ID, otherSupplierShopSku.getShopSku()), otherSupplierDoc),
                MODIFIED_TIMESTAMP);
        });
    }

    @Test
    public void whenFindsRelationsByIdUsesDocumentFilterDocumentId() {
        QualityDocument qualityDocument0 = qualityDocumentRepository.insert(generateDocument());
        QualityDocument qualityDocument1 = qualityDocumentRepository.insert(generateDocument());

        DocumentOfferRelation expectedRelation0 = generateRelation(qualityDocument0);
        DocumentOfferRelation expectedRelation1 = generateRelation(qualityDocument1);
        List<DocumentOfferRelation> expectedRelations = Arrays.asList(expectedRelation0, expectedRelation1);
        qualityDocumentRepository.insertOrUpdateRelations(expectedRelations);

        List<DocumentOfferRelation> actualRelations0 = qualityDocumentRepository.findRelations(
            new DocumentFilter().addAllDocumentIds(Collections.singleton(qualityDocument0.getId()))
        );
        List<DocumentOfferRelation> actualRelations1 = qualityDocumentRepository.findRelations(
            new DocumentFilter().addAllDocumentIds(Collections.singleton(qualityDocument1.getId()))
        );
        List<DocumentOfferRelation> actualRelations = qualityDocumentRepository.findRelations(new DocumentFilter());

        assertThat(actualRelations)
            .containsExactlyInAnyOrderElementsOf(expectedRelations);
        assertThat(actualRelations0)
            .isEqualTo(Collections.singletonList(expectedRelation0));
        assertThat(actualRelations1)
            .isEqualTo(Collections.singletonList(expectedRelation1));
    }

    @Test
    public void testAddDocumentRelationsReturnActualTimestamp() {
        QualityDocument qualityDocument0 = qualityDocumentRepository.insert(generateDocument());
        QualityDocument qualityDocument1 = qualityDocumentRepository.insert(generateDocument());

        DocumentOfferRelation expectedRelation0 = generateRelation(qualityDocument0);
        DocumentOfferRelation expectedRelation1 = generateRelation(qualityDocument1);
        DocumentOfferRelation expectedRelation2 = generateRelation(qualityDocument0);
        DocumentOfferRelation expectedRelation3 = generateRelation(qualityDocument1);

        List<DocumentOfferRelation> relations = Arrays.asList(expectedRelation0, expectedRelation1);
        List<DocumentOfferRelation> additionalRelations = Arrays.asList(expectedRelation2, expectedRelation3);

        // Insert relations
        qualityDocumentRepository.addDocumentRelations(relations);
        qualityDocumentRepository.addDocumentRelations(additionalRelations);

        // Update relations with new timestamp
        List<DocumentOfferRelation> returnedRelations = qualityDocumentRepository
            .addDocumentRelations(relations);

        List<DocumentOfferRelation> actualRelations = qualityDocumentRepository
            .findRelations(new DocumentFilter().addAllShopSkuKeys(Arrays.asList(
                new ShopSkuKey(expectedRelation0.getSupplierId(), expectedRelation0.getShopSku()),
                new ShopSkuKey(expectedRelation1.getSupplierId(), expectedRelation1.getShopSku())
            )));

        assertThat(returnedRelations).containsExactlyInAnyOrderElementsOf(actualRelations);
    }

    private DocumentOfferRelation generateRelation(QualityDocument document) {
        ShopSkuKey shopSkuKey = addOfferToDB();
        DocumentOfferRelation relation = DocumentOfferRelation.from(shopSkuKey, document);
        relation.setModifiedTimestamp(random.nextObject(LocalDateTime.class));
        return relation;
    }

    private List<String> asRegistrationNumbers(Stream<QualityDocument> documents) {
        return documents.map(QualityDocument::getRegistrationNumber).collect(Collectors.toList());
    }

    private ShopSkuKey addOfferToDB(int supplierId) {
        return new ShopSkuKey(supplierId, String.valueOf(random.nextInt(Integer.MAX_VALUE)));
    }

    private ShopSkuKey addOfferToDB() {
        return addOfferToDB(SUPPLIER_ID);
    }

    private MasterData generateMasterData(QualityDocument... documents) {
        return generateMasterData(offer, documents);
    }

    private MasterData generateMasterData(ShopSkuKey shopSkuKey, QualityDocument... documents) {
        return TestDataUtils.generateMasterData(shopSkuKey, random, documents);
    }

    private QualityDocument generateDocument() {
        QualityDocument document = TestDataUtils.generateDocument(random);
        return document;
    }

    private QualityDocument generateDocumentWithPictures(String... pictures) {
        QualityDocument qualityDocument = generateDocument();
        qualityDocument.setPictures(Arrays.asList(pictures));
        return qualityDocument;
    }

    private String generatePicture() {
        return TestDataUtils.generate(String.class, random);
    }

    private QualityDocument generateDocument(String registrationNumberSearchQuery) {
        QualityDocument document = TestDataUtils.generateDocument(random);
        document.setRegistrationNumber(
            TestDataUtils.generate(String.class, random) + registrationNumberSearchQuery +
                TestDataUtils.generate(String.class, random)
        );
        return document;
    }

    private QualityDocument generateDocument(QualityDocument.QualityDocumentType type) {
        QualityDocument qualityDocument = generateDocument();
        qualityDocument.setType(type);
        return qualityDocument;
    }
}
