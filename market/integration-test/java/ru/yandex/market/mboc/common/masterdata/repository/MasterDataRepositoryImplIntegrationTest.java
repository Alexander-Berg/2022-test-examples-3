package ru.yandex.market.mboc.common.masterdata.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockserver.model.Delay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.pglogid.PgLogIdService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.ReplicaCluster;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.repository.document.DocumentFilter;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.models.HasId;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.misc.thread.ThreadUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author jkt on 20.07.18.
 */
@SuppressWarnings("checkstyle:magicnumber")
public class MasterDataRepositoryImplIntegrationTest extends MdmBaseIntegrationTestClass {

    private static final String COUNTRY_VALUE = "NEW_COUNTRY_VALUE";
    private static final String ANOTHER_COUNTRY_VALUE = "ANOTHER_COUNTRY_VALUE";
    private static final String PICTURE = "PICTURE";
    private static final int SHELF_LIFE = 123;
    private static final int SUPPLIER_ID = 1;
    private static final String[] IGNORED_FIELDS = {
        "modifiedTimestamp", "itemShippingUnit", "goldenItemShippingUnit", "goldenRsl",
        "surplusHandleMode", "cisHandleMode", "regNumbers", "measurementState"
    };

    private static final long SEED = 242135L;

    private static final Comparator<Object> NO_OP_COMPARATOR = (x, y) -> 0;
    @Autowired
    QualityDocumentRepository qualityDocumentRepository;
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    TransactionHelper transactionHelper;
    @Autowired
    StorageKeyValueService storageKeyValueService;
    @Autowired
    PgLogIdService<ShopSkuKey> pgLogIdService;

    private MasterDataRepositoryImpl masterDataRepository;
    private MboMappings.ApprovedMappingInfo mapping;

    private EnhancedRandom random;

    private ShopSkuKey shopSkuKey;

    @Before
    public void insertOffersAndSuppliers() {
        random = TestDataUtils.defaultRandom(SEED);
        shopSkuKey = new ShopSkuKey(SUPPLIER_ID, getShopSku(SUPPLIER_ID));
        mapping = addOfferToDB();
        masterDataRepository = new MasterDataRepositoryImpl(
            jdbcTemplate,
            transactionHelper,
            transactionTemplate,
            qualityDocumentRepository
        );
    }

    @Test
    public void whenInsertingOrUpdatingShouldWriteCorrectValues() {
        MasterData offerMasterData = generateMasterData(generateDocument());

        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            softly.assertThat(masterDataFromDB)
                .usingComparatorForFields(NO_OP_COMPARATOR, "modifiedTimestamp")
                .isEqualToIgnoringGivenFields(offerMasterData, IGNORED_FIELDS);
            softly.assertThat(masterDataFromDB).hasNoNullFieldsOrPropertiesExcept("categoryId",
                "itemShippingUnit", "goldenItemShippingUnit", "goldenRsl", "surplusHandleMode", "cisHandleMode",
                "regNumbers", "measurementState");
        });
    }

    @Test
    public void whenInsertingNullModiShouldWriteNewModifiedTimestamp() {
        MasterData offerMasterData = generateMasterData(generateDocument());
        offerMasterData.setModifiedTimestamp(null);

        LocalDateTime now = DateTimeUtils.dateTimeNow();
        Delay.milliseconds(1).applyDelay();

        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            softly.assertThat(masterDataFromDB.getModifiedTimestamp()).isAfter(now);
            softly.assertThat(masterDataFromDB).hasNoNullFieldsOrPropertiesExcept("categoryId",
                "itemShippingUnit", "goldenItemShippingUnit", "goldenRsl", "surplusHandleMode", "cisHandleMode",
                "regNumbers", "measurementState");
        });
    }

    @Test
    public void whenInsertingShouldWriteCorrectValues() {
        MasterData offerMasterData = generateMasterData(generateDocument());

        masterDataRepository.insertBatch(Collections.singletonList(offerMasterData));
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertThat(masterDataFromDB)
            .usingComparatorForFields(NO_OP_COMPARATOR, IGNORED_FIELDS)
            .isEqualToComparingFieldByFieldRecursively(offerMasterData);
    }

    @Test
    public void whenInsertingDataWithoutDocumentsShouldWriteCorrectValues() {
        MasterData offerMasterData = generateMasterData(generateDocument());
        offerMasterData.getQualityDocuments().clear();

        masterDataRepository.insertBatch(Collections.singletonList(offerMasterData));
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertThat(masterDataFromDB)
            .usingComparatorForFields(NO_OP_COMPARATOR, IGNORED_FIELDS)
            .isEqualToComparingFieldByFieldRecursively(offerMasterData);
    }

    @Test
    public void whenDeletingShouldRemoveOnlyCorrectRows() {
        MasterData offerMasterData = generateMasterData(generateDocument());
        MasterData otherOfferMasterData = generateMasterData(addOfferToDB(), generateDocument());

        masterDataRepository.insertOrUpdateAll(Arrays.asList(offerMasterData, otherOfferMasterData));

        masterDataRepository.delete(offerMasterData);

        List<MasterData> masterDataFromDB = findMasterDataInDB(Arrays.asList(offerMasterData, otherOfferMasterData));

        assertThat(masterDataFromDB).hasSize(1);
        assertThat(masterDataFromDB.get(0))
            .isEqualToIgnoringGivenFields(otherOfferMasterData, IGNORED_FIELDS);
    }

    @Test
    public void whenUpdatingShouldWriteCorrectValues() {
        MasterData offerMasterData = generateMasterData(generateDocument());
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        offerMasterData.setManufacturerCountries(Collections.singletonList(COUNTRY_VALUE));
        masterDataRepository.update(offerMasterData);
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            softly.assertThat(masterDataFromDB.getManufacturerCountries()).containsExactly(COUNTRY_VALUE);
            softly.assertThat(masterDataFromDB).isEqualToIgnoringGivenFields(offerMasterData, IGNORED_FIELDS);
        });
    }

    @Test
    public void whenSavingMultipleCountriesShouldSaveCorrectly() {
        MasterData offerMasterData = generateMasterData(generateDocument());
        List<String> countries = Arrays.asList(COUNTRY_VALUE, ANOTHER_COUNTRY_VALUE);
        offerMasterData.setManufacturerCountries(countries);

        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            softly.assertThat(masterDataFromDB.getManufacturerCountries())
                .containsExactlyInAnyOrderElementsOf(countries);
            softly.assertThat(masterDataFromDB)
                .usingComparatorForFields(NO_OP_COMPARATOR, IGNORED_FIELDS)
                .isEqualToComparingFieldByFieldRecursively(offerMasterData);
        });
    }

    @Test
    public void whenInsertingOrUpdatingNewDocumentShouldGenerateId() {
        QualityDocument newDocument = generateDocument();
        newDocument.setId(HasId.EMPTY_ID);
        MasterData offerMasterData = generateMasterData(newDocument);

        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            masterDataFromDB.getQualityDocuments().forEach(document ->
                softly.assertThat(document.getId()).isPositive()
            );
            softly.assertThat(masterDataFromDB)
                .usingComparatorForFields(NO_OP_COMPARATOR, IGNORED_FIELDS)
                .isEqualToComparingFieldByFieldRecursively(offerMasterData);
        });
    }

    @Test
    public void whenInsertingOrUpdatingDataWithoutDocumentsShouldWriteCorrectValues() {
        MasterData offerMasterData = generateMasterData(generateDocument());
        offerMasterData.getQualityDocuments().clear();

        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            softly.assertThat(masterDataFromDB)
                .usingComparatorForFields(NO_OP_COMPARATOR, IGNORED_FIELDS)
                .isEqualToComparingFieldByFieldRecursively(offerMasterData);
            softly.assertThat(masterDataFromDB).hasNoNullFieldsOrPropertiesExcept("categoryId",
                "itemShippingUnit", "goldenItemShippingUnit", "goldenRsl", "surplusHandleMode", "cisHandleMode",
                "measurementState");
        });
    }

    @Test
    public void whenInsertingNewDocumentShouldGenerateId() {
        QualityDocument newDocument = generateDocument();
        newDocument.setId(HasId.EMPTY_ID);
        MasterData offerMasterData = generateMasterData(newDocument);

        masterDataRepository.insertBatch(Collections.singletonList(offerMasterData));
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            masterDataFromDB.getQualityDocuments().forEach(document ->
                softly.assertThat(document.getId()).isPositive()
            );
            softly.assertThat(masterDataFromDB)
                .usingComparatorForFields(NO_OP_COMPARATOR, IGNORED_FIELDS)
                .isEqualToComparingFieldByFieldRecursively(offerMasterData);
        });
    }

    @Test
    public void whenInsertingExistingMasterDataShouldUpdate() {
        MasterData offerMasterData = generateMasterData(generateDocument());
        List<MasterData> masterData = Collections.singletonList(offerMasterData);

        masterDataRepository.insertOrUpdateAll(masterData);

        offerMasterData.setManufacturerCountries(Collections.singletonList(COUNTRY_VALUE));
        masterDataRepository.insertOrUpdateAll(masterData);

        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            softly.assertThat(masterDataFromDB.getManufacturerCountries()).containsExactly(COUNTRY_VALUE);
            softly.assertThat(masterDataFromDB)
                .usingComparatorForFields(NO_OP_COMPARATOR, IGNORED_FIELDS)
                .isEqualToComparingFieldByFieldRecursively(offerMasterData);
        });
    }

    @Test
    public void whenInsertingExistingAndNewMasterDataShouldInsertAndUpdate() {
        MasterData offerMasterData = generateMasterData(generateDocument());
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        MasterData otherOfferMasterData = generateMasterData(addOfferToDB(), generateDocument());

        List<MasterData> masterData = Arrays.asList(offerMasterData, otherOfferMasterData);
        masterDataRepository.insertOrUpdateAll(masterData);

        List<MasterData> masterDataFromDB = findMasterDataInDB(masterData);

        assertThat(masterDataFromDB).usingElementComparatorIgnoringFields(IGNORED_FIELDS)
            .containsExactlyInAnyOrder(otherOfferMasterData, offerMasterData);
    }

    @Test
    public void whenInsertingDataWithMultipleDocumentsShouldInsertAll() {
        MasterData offerMasterData = generateMasterData(generateDocument(), generateDocument());

        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            softly.assertThat(masterDataFromDB)
                .usingComparatorForFields(NO_OP_COMPARATOR, IGNORED_FIELDS)
                .isEqualToIgnoringGivenFields(offerMasterData, "qualityDocuments");
            softly.assertThat(masterDataFromDB.getQualityDocuments()).containsExactlyInAnyOrder(
                offerMasterData.getQualityDocuments().stream().toArray(QualityDocument[]::new)
            );
        });
    }

    @Test
    public void whenInsertingDataWithExistingDocumentShouldUpdate() {
        QualityDocument initialDocument = generateDocument();
        qualityDocumentRepository.insertOrUpdateAll(Collections.singletonList(initialDocument));

        QualityDocument modifiedDocument = generateIdenticalDocument(initialDocument);
        modifiedDocument.setPictures(Collections.singletonList(PICTURE));
        MasterData offerMasterData = generateMasterData(modifiedDocument);

        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            masterDataFromDB.getQualityDocuments().forEach(document -> {
                softly.assertThat(document.getPictures()).containsExactly(PICTURE);
                softly.assertThat(document.getId()).isEqualTo(initialDocument.getId());
            });
            softly.assertThat(masterDataFromDB)
                .usingComparatorForFields(NO_OP_COMPARATOR, IGNORED_FIELDS)
                .isEqualToComparingFieldByFieldRecursively(offerMasterData);
        });
    }

    @Test
    public void whenInsertingDataWithExistingAndNewDocumentsShouldInsertAndUpdate() {
        QualityDocument existingDocument = generateDocument();
        qualityDocumentRepository.insertOrUpdateAll(Collections.singletonList(existingDocument));

        existingDocument.setPictures(Collections.singletonList(PICTURE));
        QualityDocument newDocument = generateDocument();
        MasterData offerMasterData = generateMasterData(existingDocument, newDocument);

        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            softly.assertThat(masterDataFromDB.getQualityDocuments())
                .flatExtracting(QualityDocument::getPictures)
                .containsExactly(PICTURE);
            softly.assertThat(masterDataFromDB)
                .usingComparatorForFields(NO_OP_COMPARATOR, IGNORED_FIELDS)
                .isEqualToIgnoringGivenFields(offerMasterData, "qualityDocuments");
            softly.assertThat(masterDataFromDB.getQualityDocuments()).containsExactlyInAnyOrder(
                offerMasterData.getQualityDocuments().stream().toArray(QualityDocument[]::new)
            );
        });

    }

    @Test
    public void whenInsertingEmptyOptionalFieldsShouldReturnNulls() {
        MasterData offerMasterData = generateMasterData(generateDocument());
        offerMasterData.setShelfLife(null);
        offerMasterData.setLifeTime(null);
        offerMasterData.setGuaranteePeriod(null);

        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            softly.assertThat(masterDataFromDB.getShelfLife()).isNull();
            softly.assertThat(masterDataFromDB.getLifeTime()).isNull();
            softly.assertThat(masterDataFromDB.getGuaranteePeriod()).isNull();
            softly.assertThat(masterDataFromDB)
                .usingComparatorForFields(NO_OP_COMPARATOR, IGNORED_FIELDS)
                .isEqualToComparingFieldByFieldRecursively(offerMasterData);
        });
    }

    @Test
    public void whenModifyingEmptyOptionalFieldsShouldUpdate() {
        MasterData offerMasterData = generateMasterData(generateDocument());
        offerMasterData.setShelfLife(null);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));

        offerMasterData.setShelfLife(SHELF_LIFE, TimeInUnits.TimeUnit.DAY);
        masterDataRepository.insertOrUpdateAll(Collections.singletonList(offerMasterData));
        MasterData masterDataFromDB = findSingleResultInDB(offerMasterData);

        assertSoftly(softly -> {
            softly.assertThat(masterDataFromDB.getShelfLife()).isEqualTo(
                new TimeInUnits(SHELF_LIFE, TimeInUnits.TimeUnit.DAY));
            softly.assertThat(masterDataFromDB)
                .usingComparatorForFields(NO_OP_COMPARATOR, IGNORED_FIELDS)
                .isEqualToComparingFieldByFieldRecursively(offerMasterData);
        });
    }

    @Test
    public void whenFindingByOfferShouldReturnOnlyMatchingOfferRow() {
        MasterData offerMasterData = generateMasterData(generateDocument());
        MasterData otherOfferMasterData = generateMasterData(addOfferToDB(), generateDocument());

        masterDataRepository.insertOrUpdateAll(Arrays.asList(offerMasterData, otherOfferMasterData));

        List<MasterData> foundData = masterDataRepository
            .findBy(new MasterDataFilter()
                .addAllShopSkuKeys(Collections.singletonList(offerMasterData.getShopSkuKey())));

        assertThat(foundData)
            .usingComparatorForElementFieldsWithNames(NO_OP_COMPARATOR, IGNORED_FIELDS)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(offerMasterData);
    }

    @Test
    public void whenFindingByOfferShouldReturnOnlyMatchingOfferRowDifferentSupplierId() {
        MasterData offerMasterData = generateMasterData(generateDocument());
        MasterData offerMasterData2 = generateMasterData(generateDocument());
        offerMasterData2.setSupplierId(SUPPLIER_ID + 1);
        MasterData otherOfferMasterData = generateMasterData(addOfferToDB(), generateDocument());

        masterDataRepository.insertOrUpdateAll(Arrays.asList(offerMasterData, otherOfferMasterData, offerMasterData2));

        List<MasterData> foundData = masterDataRepository
            .findBy(new MasterDataFilter()
                .addAllShopSkuKeys(Arrays.asList(offerMasterData.getShopSkuKey(), offerMasterData2.getShopSkuKey())));

        assertThat(foundData).usingElementComparatorIgnoringFields(IGNORED_FIELDS)
            .containsExactlyInAnyOrder(offerMasterData, offerMasterData2);
    }

    @Test
    public void whenSavingSameDocumentForMultipleOffersShouldNotThrowException() {
        QualityDocument qualityDocument = generateDocument();
        MasterData offerMasterData = generateMasterData(qualityDocument);
        MasterData otherOfferMasterData = generateMasterData(addOfferToDB(), qualityDocument);

        assertThatCode(() ->
            masterDataRepository.insertOrUpdateAll(Arrays.asList(offerMasterData, otherOfferMasterData))
        ).doesNotThrowAnyException();
        assertThat(qualityDocument.getId()).isPositive();
    }

    @Test
    public void whenSavingIdenticalDocumentsForMultipleOffersShouldNotThrowException() {
        QualityDocument qualityDocument = generateDocument();
        QualityDocument identicalDocument = generateIdenticalDocument(qualityDocument);
        MasterData offerMasterData = generateMasterData(qualityDocument);
        MasterData otherOfferMasterData = generateMasterData(addOfferToDB(), identicalDocument);

        assertSoftly(softly -> {
            softly.assertThatCode(() ->
                masterDataRepository.insertOrUpdateAll(Arrays.asList(offerMasterData, otherOfferMasterData))
            ).doesNotThrowAnyException();
            softly.assertThat(qualityDocument.getId()).isPositive();
            softly.assertThat(identicalDocument.getId()).isEqualTo(qualityDocument.getId());
        });
    }

    @Test
    public void whenSavingDifferentDocumentsWithSameRegistrationNumberShouldThrowException() {
        QualityDocument qualityDocument = generateDocument();
        QualityDocument otherQualityDocument = generateDocument();
        otherQualityDocument.setRegistrationNumber(qualityDocument.getRegistrationNumber());

        MasterData offerMasterData = generateMasterData(qualityDocument);
        MasterData otherOfferMasterData = generateMasterData(addOfferToDB(), otherQualityDocument);

        assertThatCode(() ->
            masterDataRepository.insertOrUpdateAll(Arrays.asList(offerMasterData, otherOfferMasterData))
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(qualityDocument.getRegistrationNumber());
    }

    @Test
    public void whenSavingDocumentOfferRelationsShouldRelateDocuments() {
        QualityDocument document = generateDocument();
        qualityDocumentRepository.insert(document);

        MasterData offerMasterData = generateMasterData(document);
        List<DocumentOfferRelation> relations = DocumentOfferRelation.fromMasterData(offerMasterData);

        qualityDocumentRepository.insertOrUpdateRelations(relations);

        List<QualityDocument> relatedDocumentsFromDb = qualityDocumentRepository.findBy(
            new DocumentFilter().addShopSkuKey(offerMasterData.getShopSkuKey())
        );

        assertThat(relatedDocumentsFromDb).containsExactlyInAnyOrder(document);
    }

    @Test
    public void whenUsingLimitShouldOrderMasterDataBySupplierIdAndShopSku() {
        MasterData masterData1 = generateMasterData(addOfferToDB("1"), generateDocument());
        MasterData masterData2 = generateMasterData(addOfferToDB("2"), generateDocument());

        List<MasterData> masterDataList = masterDataRepository
            .insertOrUpdateAll(Arrays.asList(masterData1, masterData2));
        masterDataList.sort(MasterDataRepositoryMock.ORDER_BY_KEY);

        MasterDataFilter filter = new MasterDataFilter();
        List<MasterData> foundWithLimit = masterDataRepository.findBy(filter.batch(null, 1));
        Assertions.assertThat(foundWithLimit).hasSize(1);
        Assertions.assertThat(foundWithLimit.get(0))
            .isEqualToIgnoringGivenFields(masterDataList.get(0), IGNORED_FIELDS);
    }

    @Test
    public void whenUsingOffsetShouldOrderMasterDataBySupplierIdAndShopSku() {
        MasterData masterData1 = generateMasterData(addOfferToDB("1"), generateDocument());
        MasterData masterData2 = generateMasterData(addOfferToDB("2"), generateDocument());

        List<MasterData> masterDataList = masterDataRepository
            .insertOrUpdateAll(Arrays.asList(masterData1, masterData2));
        masterDataList.sort(MasterDataRepositoryMock.ORDER_BY_KEY);

        MasterDataFilter filter = new MasterDataFilter()
            .setLimit(2)
            .setOffset(1)
            .setOrderByFields(Arrays.asList("supplier_id", "shop_sku"));
        List<MasterData> foundWithOffset = masterDataRepository.findBy(filter);
        Assertions.assertThat(foundWithOffset).hasSize(1);
        Assertions.assertThat(foundWithOffset.get(0))
            .isEqualToIgnoringGivenFields(masterDataList.get(1), IGNORED_FIELDS);
    }

    @Test
    public void whenUsingBigOffsetShouldReturnEmptyList() {
        MasterData masterData1 = generateMasterData(addOfferToDB("1"), generateDocument());
        MasterData masterData2 = generateMasterData(addOfferToDB("2"), generateDocument());

        masterDataRepository.insertOrUpdateAll(Arrays.asList(masterData1, masterData2));

        MasterDataFilter filter = new MasterDataFilter()
            .setLimit(2)
            .setOffset(2)
            .setOrderByFields(Arrays.asList("supplier_id", "shop_sku"));
        List<MasterData> foundWithLimitOffset = masterDataRepository.findBy(filter);
        Assertions.assertThat(foundWithLimitOffset).hasSize(0);
    }

    @Test
    public void whenUsingBatchFilterShouldReturnSorted() {
        MasterData masterData1 = generateMasterData(addOfferToDB("3"), generateDocument());
        MasterData masterData2 = generateMasterData(addOfferToDB("2"), generateDocument());
        MasterData masterData3 = generateMasterData(addOfferToDB("1"), generateDocument());
        MasterData masterData4 = generateMasterData(addOfferToDB("4"), generateDocument());
        masterDataRepository.insertBatch(Arrays.asList(masterData1, masterData2, masterData3, masterData4));

        MasterDataFilter filter = new MasterDataFilter();
        List<MasterData> found = masterDataRepository.findBy(filter.batch(
            new ShopSkuKey(SUPPLIER_ID, "1"), 3
        ));
        assertThat(found).hasSize(3);
        assertThat(found.get(0))
            .isEqualToIgnoringGivenFields(masterData2, IGNORED_FIELDS);
        assertThat(found.get(1))
            .isEqualToIgnoringGivenFields(masterData1, IGNORED_FIELDS);
        assertThat(found.get(2))
            .isEqualToIgnoringGivenFields(masterData4, IGNORED_FIELDS);
    }

    @Test
    public void whenUsingOffsetAndLimitShouldApplyBoth() {
        MasterData masterData1 = generateMasterData(addOfferToDB("1"), generateDocument());
        MasterData masterData2 = generateMasterData(addOfferToDB("2"), generateDocument());
        MasterData masterData3 = generateMasterData(addOfferToDB("3"), generateDocument());
        MasterData masterData4 = generateMasterData(addOfferToDB("4"), generateDocument());

        List<MasterData> masterDataList = masterDataRepository
            .insertOrUpdateAll(Arrays.asList(masterData1, masterData2, masterData3, masterData4));
        masterDataList.sort(MasterDataRepositoryMock.ORDER_BY_KEY);

        MasterDataFilter filter = new MasterDataFilter()
            .setLimit(1)
            .setOffset(2)
            .setOrderByFields(Arrays.asList("supplier_id", "shop_sku"));

        List<MasterData> foundWithLimitOffset = masterDataRepository.findBy(filter);
        Assertions.assertThat(foundWithLimitOffset).hasSize(1);
        Assertions.assertThat(foundWithLimitOffset.get(0))
            .isEqualToIgnoringGivenFields(masterDataList.get(2), IGNORED_FIELDS);
    }

    @Test
    public void whenUsingKeyCriteriaShouldStartOnSpecifiedKeys() {
        MasterData masterData1 = generateMasterData(addOfferToDB("1"), generateDocument());
        MasterData masterData2 = generateMasterData(addOfferToDB("2"), generateDocument());
        MasterData masterData3 = generateMasterData(addOfferToDB("3"), generateDocument());
        MasterData masterData4 = generateMasterData(addOfferToDB("4"), generateDocument());
        masterDataRepository.insertOrUpdateAll(Arrays.asList(masterData1, masterData2, masterData3, masterData4));

        List<MasterData> foundWithoutLimit = masterDataRepository.findBy(new MasterDataFilter()
            .batch(new ShopSkuKey(SUPPLIER_ID, "2"), 100));
        assertThat(foundWithoutLimit).hasSize(2);
        assertThat(foundWithoutLimit.get(0))
            .isEqualToIgnoringGivenFields(masterData3, IGNORED_FIELDS);
        assertThat(foundWithoutLimit.get(1))
            .isEqualToIgnoringGivenFields(masterData4, IGNORED_FIELDS);

        List<MasterData> foundWithLimit = masterDataRepository.findBy(new MasterDataFilter()
            .batch(new ShopSkuKey(SUPPLIER_ID, "2"), 1));
        assertThat(foundWithLimit).hasSize(1);
        assertThat(foundWithLimit.get(0))
            .isEqualToIgnoringGivenFields(masterData3, IGNORED_FIELDS);
    }

    @Test
    public void whenUsingKeyCriteriaShouldAccountForSupplierIdsOrder() {
        int secondSupplierId = SUPPLIER_ID + 1;

        MasterData masterData1 = generateMasterData(addOfferToDB("1"), generateDocument());
        MasterData masterData2 = generateMasterData(addOfferToDB("2"), generateDocument());
        MasterData masterData3 = generateMasterData(addOfferToDB("3"), generateDocument());
        masterData3.setSupplierId(secondSupplierId);
        MasterData masterData4 = generateMasterData(addOfferToDB("4"), generateDocument());
        masterData4.setSupplierId(secondSupplierId);

        masterDataRepository.insertOrUpdateAll(Arrays.asList(masterData1, masterData2, masterData3, masterData4));

        List<MasterData> found = masterDataRepository.findBy(new MasterDataFilter()
            .addCriterion(new MDKeyGreaterCriteria(SUPPLIER_ID, "4")));
        assertThat(found).hasSize(2);
        assertThat(found.get(0))
            .isEqualToIgnoringGivenFields(masterData3, IGNORED_FIELDS);
        assertThat(found.get(1))
            .isEqualToIgnoringGivenFields(masterData4, IGNORED_FIELDS);
    }

    @Test
    public void whenFindingByCheckLifeTimeModifiedTsReturnOnlyMatchingOfferRow() {
        MasterData oldMasterData = generateMasterData(addOfferToDB("1"), generateDocument());
        masterDataRepository.insertOrUpdateAll(Arrays.asList(oldMasterData));
        ThreadUtils.sleep(100);

        LocalDateTime checkTime = DateTimeUtils.dateTimeNow();
        ThreadUtils.sleep(100);

        MasterData offerMasterData = generateMasterData(addOfferToDB("2"), generateDocument());
        offerMasterData.setLifeTime(null);

        MasterData offerMasterWithLifeTimeData = generateMasterData(addOfferToDB("3"), generateDocument());
        offerMasterWithLifeTimeData.setLifeTime(1, TimeInUnits.TimeUnit.DAY);

        masterDataRepository.insertOrUpdateAll(Arrays.asList(offerMasterData, offerMasterWithLifeTimeData));

        List<MasterData> foundData = masterDataRepository.findBy(new MasterDataFilter()
            .setCheckLifeTimeModifiedTs(checkTime));

        assertThat(foundData).hasSize(1);
        assertThat(foundData.get(0))
            .isEqualToIgnoringGivenFields(offerMasterData, IGNORED_FIELDS);
    }

    @Test
    public void testLargeShopSkuList() {
        NamedParameterJdbcTemplate jdbcSpy = Mockito.spy(jdbcTemplate);
        masterDataRepository = new MasterDataRepositoryImpl(
            jdbcSpy, transactionHelper, transactionTemplate,
            qualityDocumentRepository
        );
        QualityDocument document = generateDocument();

        List<MboMappings.ApprovedMappingInfo> mappings = IntStream.range(1, 2003)
            .mapToObj(n -> addOfferToDB("shop-sku-" + n))
            .collect(Collectors.toList());

        List<MasterData> masterDataList = mappings.stream()
            .map(ofr -> generateMasterData(ofr, document))
            .collect(Collectors.toList());
        masterDataRepository.insertBatch(masterDataList);

        List<ShopSkuKey> shopSkus = mappings.stream()
            .map(m -> new ShopSkuKey(m.getSupplierId(), m.getShopSku()))
            .collect(Collectors.toList());

        Mockito.reset(jdbcSpy);
        List<MasterData> result = masterDataRepository.findByIds(shopSkus);

        Assertions.assertThat(result).hasSize(2002);
        // Searching for 1000 + 1000 + 2 shop_sku_key pairs, verify query count
        Mockito.verify(jdbcSpy, Mockito.times(3))
            .query(Mockito.anyString(), Mockito.anyMap(),
                Mockito.any(MasterDataRowCallbackHandler.class));
    }

    @Test
    public void whenFindingByShopSkuKeysAndUsingOrdersShouldThrowException() {
        Assertions.assertThatThrownBy(() ->
            masterDataRepository.findBy(
                new MasterDataFilter()
                    .setShopSkuKeys(Collections.singleton(shopSkuKey))
                    .setLimit(1)))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() ->
            masterDataRepository.findBy(
                new MasterDataFilter()
                    .setShopSkuKeys(Collections.singleton(shopSkuKey))
                    .setOffset(1)))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() ->
            masterDataRepository.findBy(
                new MasterDataFilter()
                    .setShopSkuKeys(Collections.singleton(shopSkuKey))
                    .setOrderByFields(Arrays.asList("supplier_id", "shop_sku"))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void whenShopSkuKeysNotPassedShouldQueryOnce() {
        NamedParameterJdbcTemplate jdbcSpy = Mockito.spy(jdbcTemplate);
        masterDataRepository = new MasterDataRepositoryImpl(
            jdbcSpy, transactionHelper, transactionTemplate, qualityDocumentRepository
        );

        LocalDateTime now = DateTimeUtils.dateTimeNow();
        QualityDocument document = generateDocument();
        List<MboMappings.ApprovedMappingInfo> offers = IntStream.range(1, 1002)
            .mapToObj(n -> addOfferToDB("shop-sku-" + n))
            .collect(Collectors.toList());

        List<MasterData> masterDataList = offers.stream()
            .map(ofr -> generateMasterData(ofr, document))
            .collect(Collectors.toList());
        masterDataRepository.insertBatch(masterDataList);

        Mockito.reset(jdbcSpy);
        List<MasterData> result = masterDataRepository.findBy(new MasterDataFilter().setModifiedAfter(now));

        Assertions.assertThat(result).hasSize(1001);
        Mockito.verify(jdbcSpy, Mockito.times(1))
            .query(Mockito.anyString(), Mockito.anyMap(),
                Mockito.any(MasterDataRowCallbackHandler.class));
    }

    @Test
    public void whenMarksUploadedToYtShouldNotChangeModifiedTimestamp() {
        MasterData masterData = addMasterDataToDB();
        for (ReplicaCluster replicaCluster : ReplicaCluster.getAllMdmClusters()) {
            LocalDateTime beforeTs = masterDataRepository.findById(shopSkuKey).getModifiedTimestamp();
            Assertions.assertThat(countMasterDataWhereColumnIsTrue("msku_import_status"))
                .isEqualTo(0);

            Assertions.assertThat(countMasterDataWhereColumnIsTrue("msku_import_status"))
                .isEqualTo(0);
            LocalDateTime afterTs = masterDataRepository.findById(shopSkuKey).getModifiedTimestamp();
            Assertions.assertThat(afterTs)
                .isEqualTo(beforeTs);
        }
    }

    private List<MasterData> generateTestMasterData(int count) {
        List<MasterData> result = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            MasterData masterData = addMasterDataToDB(i);
            result.add(masterData);
        }
        return result;
    }

    private ShopSkuKey getShopSkuKey(int shopSkuSuffix) {
        return new ShopSkuKey(SUPPLIER_ID, getShopSku(shopSkuSuffix));
    }

    private String getShopSku(int suffix) {
        return "SSKU " + suffix;
    }

    private DocumentOfferRelation makeRelation(QualityDocument document, int supplierId,
                                               MboMappings.ApprovedMappingInfo mapping) {
        DocumentOfferRelation relation = new DocumentOfferRelation();
        relation.setDocumentId(document.getId());
        relation.setShopSku(mapping.getShopSku());
        relation.setSupplierId(supplierId);
        return relation;
    }

    @Test
    public void whenInsertsOrUpdatesRelationsShouldRemoveThemFromDeletedTable() {
        QualityDocument document = generateDocument();
        qualityDocumentRepository.insert(document);

        MboMappings.ApprovedMappingInfo offer0 = mapping;
        MboMappings.ApprovedMappingInfo offer1 = addOfferToDB();

        DocumentOfferRelation relation0 = makeRelation(document, SUPPLIER_ID, offer0);
        DocumentOfferRelation relation1 = makeRelation(document, SUPPLIER_ID, offer1);

        qualityDocumentRepository.insertOrUpdateRelations(
            Arrays.asList(relation0, relation1)
        );

        Set<DocumentOfferRelation> toDeleteAndToRestoreRelations = Collections.singleton(relation0);

        assertThat(countDeletedRelations())
            .isEqualTo(0);
        assertThat(qualityDocumentRepository.findRelations(new DocumentFilter()))
            .hasSize(2);

        qualityDocumentRepository.deleteOfferRelations(toDeleteAndToRestoreRelations);

        assertThat(countDeletedRelations())
            .isEqualTo(1);
        assertThat(qualityDocumentRepository.findRelations(new DocumentFilter()))
            .hasSize(1);

        qualityDocumentRepository.insertOrUpdateRelations(toDeleteAndToRestoreRelations);

        assertThat(countDeletedRelations())
            .isEqualTo(0);
        assertThat(qualityDocumentRepository.findRelations(new DocumentFilter()))
            .hasSize(2);
    }

    @Test
    public void whenInsertsMasterDataShouldSaveIrisItems() {
        MasterData masterData = TestDataUtils.generateMasterData("12", 34, random);
        Assertions.assertThat(masterData.getItemShippingUnit())
            .isNotNull();
        masterDataRepository.insert(masterData);
        MasterData savedMasterData = masterDataRepository.findById(masterData.getShopSkuKey());
        Assertions.assertThat(savedMasterData)
            .isEqualToIgnoringGivenFields(masterData, IGNORED_FIELDS);
    }

    private Integer countMasterDataWhereColumnIsTrue(String columnName) {
        return jdbcTemplate.getJdbcTemplate()
            .queryForObject("select count(*) from mdm.master_data where " + columnName + " = ?", Integer.class, true);
    }

    private int countDeletedRelations() {
        // language=PostgreSQL
        return jdbcTemplate.queryForObject("" +
                "select count(*)\n" +
                "from mdm.deleted_offer_document",
            new HashMap<>(), Integer.class);
    }

    private MboMappings.ApprovedMappingInfo addOfferToDB() {
        return addOfferToDB(TestDataUtils.generate(String.class, random));
    }

    private MboMappings.ApprovedMappingInfo addOfferToDB(String shopSku) {
        MboMappings.ApprovedMappingInfo result = TestDataUtils
            .generateCorrectApprovedMappingInfoBuilder(random)
            .setSupplierId(SUPPLIER_ID)
            .setShopSku(shopSku)
            .build();
        return result;
    }

    private MasterData addMasterDataToDB() {
        return addMasterDataToDB(SUPPLIER_ID);
    }

    private MasterData addMasterDataToDB(int shopSkuKeySuffix) {
        MboMappings.ApprovedMappingInfo testMapping = addOfferToDB(getShopSku(shopSkuKeySuffix));
        MasterData masterData = generateMasterData(testMapping);
        masterData.setShopSkuKey(new ShopSkuKey(testMapping.getSupplierId(), testMapping.getShopSku()));
        return masterDataRepository.insert(masterData);
    }

    private MasterData findSingleResultInDB(MasterData masterData) {
        List<MasterData> masterDataFromDB = findMasterDataInDB(Arrays.asList(masterData));
        if (masterDataFromDB.size() != 1) {
            throw new IllegalStateException("Single result is expected. But was: " + masterDataFromDB.size());
        }
        return masterDataFromDB.get(0);
    }

    private List<MasterData> findMasterDataInDB(Collection<MasterData> masterData) {
        return masterDataRepository.findByIds(masterData.stream()
            .map(MasterData::getShopSkuKey)
            .collect(Collectors.toList())
        );
    }

    private MasterData generateMasterData(MboMappings.ApprovedMappingInfo mapping, QualityDocument... documents) {
        MasterData masterData = TestDataUtils.generateMasterData(new ShopSkuKey(mapping.getSupplierId(),
            mapping.getShopSku()), random, documents);
        masterData.setModifiedTimestamp(DateTimeUtils.dateTimeNow().minusMinutes(1));
        return masterData;
    }

    private MasterData generateMasterData(QualityDocument... documents) {
        return generateMasterData(mapping, documents);
    }

    private QualityDocument generateIdenticalDocument(QualityDocument qualityDocument) {
        return TestDataUtils.generateIdenticalDocument(qualityDocument);
    }

    private QualityDocument generateDocument() {
        return TestDataUtils.generateDocument(random);
    }
}
