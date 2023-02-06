package ru.yandex.market.mboc.common.masterdata.repository.document;


import java.util.Arrays;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.model.metadata.MdmSource;
import ru.yandex.market.mboc.common.masterdata.parsing.QualityDocumentValidation;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static org.assertj.core.api.Assertions.assertThatCode;

public class SupplierDocumentSearchCriteriaTest extends MdmBaseDbTestClass {

    private static final long SEED = 6719324L;
    private static final long ID_OFFSET_KEY = 1L;
    private static final int SUPPIER_ID = 1;
    private static final int OTHER_SUPPIER_ID = 2;
    private static final int REGNUM_SEARCH_PREFIX_LEN = QualityDocumentValidation.REGISTRATION_NUMBER_MIN_LENGTH - 1;
    private static final int SHOP_SKU_SEARCH_PREFIX_LEN = SupplierDocumentSearchCriteria.REG_NUMBER_LIKE_THRESHOLD - 1;

    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private QualityDocumentRepository qualityDocumentRepository;

    private EnhancedRandom qualityDocumentsRandom;
    private EnhancedRandom defaultRandom;
    private ShopSkuKey offer;
    private ShopSkuKey otherOffer;

    private QualityDocument supplierMetadataDoc;
    private QualityDocument supplierShopSkuDoc;
    private QualityDocument supplierRegNumberDoc;
    private QualityDocument supplierShopSkuAndRegNumberDoc;
    private QualityDocument otherSupplierDoc;

    @Before
    public void setUp() throws Exception {
        defaultRandom = TestDataUtils.defaultRandom(SEED);
        qualityDocumentsRandom = TestDataUtils.qualityDocumentsRandom(defaultRandom.nextLong());
        insertOffers();
        insertDocuments();
    }

    private void insertOffers() {
        EnhancedRandom shopSkuRandom = TestDataUtils.defaultRandomBuilder(defaultRandom.nextLong())
            .stringLengthRange(SHOP_SKU_SEARCH_PREFIX_LEN + 1, TestDataUtils.MAX_STRING_LENGTH)
            .build();
        offer = TestDataUtils.generate(ShopSkuKey.class, shopSkuRandom);
        otherOffer = TestDataUtils.generate(ShopSkuKey.class, shopSkuRandom);
    }

    private void insertDocuments() {
        supplierMetadataDoc = qualityDocumentRepository.insert(generateDocument(SUPPIER_ID));
        otherSupplierDoc = qualityDocumentRepository.insert(generateDocument(OTHER_SUPPIER_ID));

        supplierRegNumberDoc = generateDocument(SUPPIER_ID);
        masterDataRepository.insert(generateMasterData(otherOffer, supplierRegNumberDoc));

        supplierShopSkuDoc = generateDocument(SUPPIER_ID);
        supplierShopSkuAndRegNumberDoc = generateDocument(SUPPIER_ID)
            .setRegistrationNumber(supplierRegNumberDoc.getRegistrationNumber() + "_hello_there");
        masterDataRepository.insert(generateMasterData(offer, supplierShopSkuDoc, supplierShopSkuAndRegNumberDoc));
    }

    private MasterData generateMasterData(ShopSkuKey shopSkuKey, QualityDocument... documents) {
        return TestDataUtils.generateMasterData(shopSkuKey, defaultRandom, documents);
    }

    private QualityDocument generateDocument(int supplierId) {
        return TestDataUtils.generateDocument(qualityDocumentsRandom)
            .setRegistrationNumber(qualityDocumentsRandom.nextObject(String.class))
            .setMetadata(new QualityDocument.Metadata()
                .setSource(MdmSource.SUPPLIER)
                .setCreatedBy(supplierId));
    }

    @Test
    public void whenSearchQueryIsNullShouldFindBySupplierId() {
        List<QualityDocument> found = qualityDocumentRepository.findBy(new DocumentFilter()
            .setSupplierDocSearchCriteria(SUPPIER_ID, null));
        Assertions.assertThat(found).containsExactlyInAnyOrder(supplierMetadataDoc,
            supplierShopSkuDoc, supplierRegNumberDoc, supplierShopSkuAndRegNumberDoc);
    }

    @Test
    public void whenSearchQueryIsEmptyShouldFindBySupplierId() {
        List<QualityDocument> found = qualityDocumentRepository.findBy(new DocumentFilter()
            .setSupplierDocSearchCriteria(SUPPIER_ID, ""));
        Assertions.assertThat(found).containsExactlyInAnyOrder(supplierMetadataDoc,
            supplierShopSkuDoc, supplierRegNumberDoc, supplierShopSkuAndRegNumberDoc);
    }

    @Test
    public void whenSearchQuerySizeIsLessThanRegNumLikeThresholdShouldFindByShopSkuLike() {
        String shopSkuQuery = offer.getShopSku()
            .substring(0, SHOP_SKU_SEARCH_PREFIX_LEN);
        List<QualityDocument> found = qualityDocumentRepository.findBy(new DocumentFilter()
            .setSupplierDocSearchCriteria(SUPPIER_ID, shopSkuQuery));
        Assertions.assertThat(found).containsExactlyInAnyOrder(supplierShopSkuDoc, supplierShopSkuAndRegNumberDoc);
    }

    @Test
    public void whenSearchQueryIsLikeShopSkuShouldCheckSupplierId() {
        String shopSkuQuery = offer.getShopSku()
            .substring(0, SHOP_SKU_SEARCH_PREFIX_LEN);
        List<QualityDocument> found = qualityDocumentRepository.findBy(new DocumentFilter()
            .setSupplierDocSearchCriteria(OTHER_SUPPIER_ID, shopSkuQuery));
        Assertions.assertThat(found).isEmpty();
    }

    @Test
    public void whenSearchQuerySizeIsLessThanRegNumEqualsThresholdShouldFindRegNumberLike() {
        String regNumberQuery = supplierRegNumberDoc.getRegistrationNumber()
            .substring(0, REGNUM_SEARCH_PREFIX_LEN);
        List<QualityDocument> found = qualityDocumentRepository.findBy(new DocumentFilter()
            .setSupplierDocSearchCriteria(SUPPIER_ID, regNumberQuery));
        Assertions.assertThat(found).containsExactlyInAnyOrder(supplierRegNumberDoc, supplierShopSkuAndRegNumberDoc);
    }

    @Test
    public void whenSearchQueryIsLikeRegNumberShouldCheckSupplierId() {
        String regNumberQuery = supplierRegNumberDoc.getRegistrationNumber()
            .substring(0, REGNUM_SEARCH_PREFIX_LEN);
        List<QualityDocument> found = qualityDocumentRepository.findBy(new DocumentFilter()
            .setSupplierDocSearchCriteria(OTHER_SUPPIER_ID, regNumberQuery));
        Assertions.assertThat(found).isEmpty();
    }

    @Test
    public void whenSearchQueryEqualsRegNumberShouldReturnDocumentsForOtherSupplier() {
        String otherRegNumber = otherSupplierDoc.getRegistrationNumber();
        List<QualityDocument> found = qualityDocumentRepository.findBy(new DocumentFilter()
            .setSupplierDocSearchCriteria(SUPPIER_ID, otherRegNumber));
        Assertions.assertThat(found).containsExactlyInAnyOrder(otherSupplierDoc);
    }

    @Test
    public void whenSearchDocumentShouldIgnoreShopSkuCase() {
        String shopSkuQuery = offer.getShopSku()
            .substring(0, SHOP_SKU_SEARCH_PREFIX_LEN).toUpperCase();
        List<QualityDocument> found = qualityDocumentRepository.findBy(new DocumentFilter()
            .setSupplierDocSearchCriteria(SUPPIER_ID, shopSkuQuery));
        Assertions.assertThat(found).containsExactlyInAnyOrder(supplierShopSkuDoc, supplierShopSkuAndRegNumberDoc);
    }

    @Test
    public void whenSearchDocumentShouldIgnoreRegNumberCase() {
        String regNumberQuery = supplierRegNumberDoc.getRegistrationNumber()
            .substring(0, REGNUM_SEARCH_PREFIX_LEN).toUpperCase();
        List<QualityDocument> found = qualityDocumentRepository.findBy(new DocumentFilter()
            .setSupplierDocSearchCriteria(SUPPIER_ID, regNumberQuery));
        Assertions.assertThat(found).containsExactlyInAnyOrder(supplierRegNumberDoc, supplierShopSkuAndRegNumberDoc);
    }

    @Test
    public void whenCombinedWithOtherCriteriaThroughOrShouldWorkLikeOrClause() {
        // check that offer criteria added with AND operator
        List<QualityDocument> foundByRegNumbers = qualityDocumentRepository.findBy(new DocumentFilter()
            .addAllRegistrationNumbers(Arrays.asList(
                supplierMetadataDoc.getRegistrationNumber(),
                supplierRegNumberDoc.getRegistrationNumber(),
                supplierShopSkuDoc.getRegistrationNumber()))
            .setSupplierDocSearchCriteria(SUPPIER_ID, "totally_invalid_search", "or"));

        Assertions.assertThat(foundByRegNumbers)
            .containsExactlyInAnyOrder(supplierMetadataDoc, supplierRegNumberDoc, supplierShopSkuDoc);
    }

    @Test
    public void whenCombinedWithOtherCriteriaShouldByDefaultShouldUseAndClause() {
        // check that offer criteria added with AND operator
        List<QualityDocument> foundByRegNumbers = qualityDocumentRepository.findBy(new DocumentFilter()
            .addAllRegistrationNumbers(Arrays.asList(
                supplierMetadataDoc.getRegistrationNumber(),
                supplierRegNumberDoc.getRegistrationNumber(),
                supplierShopSkuDoc.getRegistrationNumber()))
            .setSupplierDocSearchCriteria(SUPPIER_ID, "totally_invalid_search"));

        Assertions.assertThat(foundByRegNumbers).isEmpty();
    }

    @Test
    public void whenCombinedWithAddOnlyCriteriaShouldNotThrowException() {
        // check that offer criteria added with AND operator
        assertThatCode(() -> qualityDocumentRepository.findBy(new DocumentFilter()
            .setDeleted(true)
            .setIdOffsetKey(ID_OFFSET_KEY)
            .setModifiedAfter(DateTimeUtils.dateTimeNow())
            .setSupplierDocSearchCriteria(SUPPIER_ID, "totally_invalid_search"))
        ).doesNotThrowAnyException();


    }

    @Test
    public void whenCriteriaDoesNotMatchShouldReturnEmptyList() {
        // check that offer criteria added with AND operator
        List<QualityDocument> foundByRegNumbers = qualityDocumentRepository.findBy(new DocumentFilter()
            .addAllRegistrationNumbers(Arrays.asList(
                supplierMetadataDoc.getRegistrationNumber(),
                supplierRegNumberDoc.getRegistrationNumber(),
                supplierShopSkuDoc.getRegistrationNumber())));
        Assertions.assertThat(foundByRegNumbers)
            .containsExactlyInAnyOrder(supplierMetadataDoc, supplierRegNumberDoc, supplierShopSkuDoc);

        List<QualityDocument> found = qualityDocumentRepository.findBy(new DocumentFilter()
            .addAllRegistrationNumbers(Arrays.asList("invalid_reg_number_1", "invalid_reg_number_2"))
            .setSupplierDocSearchCriteria(SUPPIER_ID, "1234567"));
        Assertions.assertThat(found).isEmpty();
    }

    @Test
    public void whenSeveralCriteriaMatchShouldReturnOneDocument() {
        //  edge case when shop_sku==reg_number==searchQuery
        qualityDocumentRepository.deleteAll();
        masterDataRepository.deleteAll();
        QualityDocument qd = generateDocument(SUPPIER_ID)
            .setRegistrationNumber(offer.getShopSku());
        masterDataRepository.insert(generateMasterData(offer, qd));
        List<QualityDocument> found = qualityDocumentRepository.findBy(new DocumentFilter()
            .setSupplierDocSearchCriteria(SUPPIER_ID, qd.getRegistrationNumber()));
        Assertions.assertThat(found).containsExactly(qd);
    }
}
