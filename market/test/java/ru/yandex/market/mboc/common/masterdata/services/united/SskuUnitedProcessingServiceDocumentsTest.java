package ru.yandex.market.mboc.common.masterdata.services.united;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.MdmVerdict;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class SskuUnitedProcessingServiceDocumentsTest extends UnitedProcessingServiceSetupBaseTest {
    private static final ShopSkuKey BUSINESS_KEY1 = new ShopSkuKey(1000, "sku");
    private static final ShopSkuKey SERVICE_KEY1 = new ShopSkuKey(1, "sku");
    private static final ShopSkuKey SERVICE_KEY2 = new ShopSkuKey(2, "sku");
    private static final Long MSKU_ID = 111L;
    private static final Integer CATEGORY = 12345;

    @Before
    public void before() {
        prepareSskuGroupWithMskuMappings();
    }

    @Test
    public void testOnlySilverDocumentsMergedToGold() {
        // given
        String silverRegNumber1 = "1234";
        String goldRegNumber1 = "4321";
        String goldRegNumber2 = "5678";
        String silverRegNumber2 = "8765";
        qualityDocumentRepository.insertBatch(List.of(
            generateQualityDocument(silverRegNumber1), generateQualityDocument(goldRegNumber1),
            generateQualityDocument(goldRegNumber2), generateQualityDocument(silverRegNumber2)));
        Instant ts = Instant.now();
        var editorSsku = silverSsku(SERVICE_KEY1, "cloudcat", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            List.of(silverRegNumber1));
        var supplierSsku = electrumSsku(SERVICE_KEY2, List.of(goldRegNumber1, goldRegNumber2));
        var anotherSsku = silverSsku(SERVICE_KEY2, "xdoc", MasterDataSourceType.SUPPLIER, ts,
            List.of(silverRegNumber2));
        silverSskuRepository.insertOrUpdateSsku(editorSsku);
        silverSskuRepository.insertOrUpdateSsku(anotherSsku);

        masterDataRepository.insertBatch(supplierSsku);

        // when
        processShopSkuKeys(List.of(SERVICE_KEY1));

        // then
        MasterData service1 = masterDataRepository.findById(SERVICE_KEY1);
        MasterData service2 = masterDataRepository.findById(SERVICE_KEY2);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(2);

        List<QualityDocument> expectedDocuments = List.of(silverRegNumber1, silverRegNumber2)
            .stream().map(this::generateQualityDocument).collect(Collectors.toList());

        Assertions.assertThat(service1.getShopSkuKey()).isEqualTo(SERVICE_KEY1);
        Assertions.assertThat(service1.getQualityDocuments())
            .containsExactlyInAnyOrderElementsOf(expectedDocuments);

        Assertions.assertThat(service2.getShopSkuKey()).isEqualTo(SERVICE_KEY2);
        Assertions.assertThat(service2.getQualityDocuments())
            .containsExactlyInAnyOrderElementsOf(expectedDocuments);
    }

    @Test
    public void shouldCorrectVerdictForDocumentsOnBusinessOffer() {
        String regNumber1 = "111";
        String regNumber2 = "222";

        // create documents
        List<QualityDocument> docs = qualityDocumentRepository.insertBatch(
            List.of(generateQualityDocument(regNumber1), generateQualityDocument(regNumber2))
        );

        // create relations to service sku
        qualityDocumentRepository.addDocumentRelations(List.of(
            new DocumentOfferRelation(SERVICE_KEY1.getSupplierId(), SERVICE_KEY1.getShopSku(), docs.get(0).getId(),
                LocalDateTime.now()),
            new DocumentOfferRelation(SERVICE_KEY1.getSupplierId(), SERVICE_KEY1.getShopSku(), docs.get(1).getId(),
                LocalDateTime.now())
        ));

        // add silver param for documents on business sku
        var businessSku = new SilverCommonSsku(
            new SilverSskuKey(
                BUSINESS_KEY1,
                new MasterDataSource(MasterDataSourceType.AUTO, "max")
            )
        );
        businessSku.addBaseValue(new MdmParamValue()
            .setMdmParamId(KnownMdmParams.DOCUMENT_REG_NUMBER)
            .setStrings(List.of(regNumber1, regNumber2))
        );

        // add additional silver params
        businessSku.addBaseValues(
            List.of(
                new MdmParamValue()
                    .setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY)
                    .setStrings(List.of("Россия")),
                new MdmParamValue()
                    .setMdmParamId(KnownMdmParams.WIDTH)
                    .setNumeric(BigDecimal.valueOf(15)),

                new MdmParamValue()
                    .setMdmParamId(KnownMdmParams.HEIGHT)
                    .setNumeric(BigDecimal.valueOf(15)),
                new MdmParamValue()
                    .setMdmParamId(KnownMdmParams.LENGTH)
                    .setNumeric(BigDecimal.valueOf(10)),
                new MdmParamValue()
                    .setMdmParamId(KnownMdmParams.WEIGHT_GROSS)
                    .setNumeric(BigDecimal.valueOf(1))
            )
        );

        silverSskuRepository.insertOrUpdateSsku(businessSku);
        processShopSkuKeys(List.of(SERVICE_KEY1));

        // check verdicts
        SskuVerdictResult serviceGoldenVerdict = verdictRepository.findById(SERVICE_KEY1);
        Assertions.assertThat(serviceGoldenVerdict.getSingleVerdictResults().size()).isEqualTo(1);
        Assertions.assertThat(
            serviceGoldenVerdict.getSingleVerdictResults().values().stream()
                .findFirst()
                .orElseThrow()
                .getVerdict()
        ).isEqualTo(MdmVerdict.OK);
        SskuVerdictResult businessGoldenVerdict = verdictRepository.findById(BUSINESS_KEY1);
        Assertions.assertThat(businessGoldenVerdict.getSingleVerdictResults().size()).isEqualTo(1);
        Assertions.assertThat(
            businessGoldenVerdict.getSingleVerdictResults().values().stream()
                .findFirst()
                .orElseThrow()
                .getVerdict()
        ).isEqualTo(MdmVerdict.OK);
    }

    private SilverCommonSsku silverSsku(ShopSkuKey key,
                                        String sourceId,
                                        MasterDataSourceType type,
                                        Instant updatedTs,
                                        List<String> regNumbers // уникальное объединение
    ) {
        List<SskuSilverParamValue> result = new ArrayList<>();

        if (regNumbers.size() > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.DOCUMENT_REG_NUMBER);
            value.setXslName(paramCache.get(KnownMdmParams.DOCUMENT_REG_NUMBER).getXslName());
            value.setStrings(regNumbers);
            value.setDatacampMasterDataVersion(null);
            result.add(value);
        }

        return TestDataUtils.wrapSilver(result);
    }

    private QualityDocument generateQualityDocument(String regNumber) {
        return new QualityDocument()
            .setId(Long.parseLong(regNumber))
            .setType(QualityDocument.QualityDocumentType.DECLARATION_OF_CONFORMITY)
            .setStartDate(LocalDate.now().minusYears(10))
            .setEndDate(LocalDate.now().plusYears(100))
            .setRegistrationNumber(regNumber);
    }

    private MasterData electrumSsku(ShopSkuKey key,
                                    List<String> regNumbers) { // уникальное объединение
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(key);
        if (regNumbers.size() > 0) {
            masterData.addAllQualityDocuments(
                regNumbers.stream().map(this::generateQualityDocument).collect(Collectors.toList()));
        }
        return masterData;
    }

    private void prepareSskuGroupWithMskuMappings() {
        MdmSupplier business = new MdmSupplier()
            .setId(BUSINESS_KEY1.getSupplierId())
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier service1 = new MdmSupplier().setId(SERVICE_KEY1.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessEnabled(true)
            .setBusinessId(business.getId());
        MdmSupplier service2 = new MdmSupplier().setId(SERVICE_KEY2.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessEnabled(true)
            .setBusinessId(business.getId());
        mdmSupplierRepository.insertBatch(business, service1, service2);

        mappingsCacheRepository.insertOrUpdateAll(List.of(
            new MappingCacheDao().setCategoryId(CATEGORY)
                .setShopSkuKey(BUSINESS_KEY1)
                .setMskuId(MSKU_ID)
                .setModifiedTimestamp(LocalDateTime.now()),
            new MappingCacheDao().setCategoryId(CATEGORY)
                .setShopSkuKey(SERVICE_KEY1)
                .setMskuId(MSKU_ID)
                .setModifiedTimestamp(LocalDateTime.now()),
            new MappingCacheDao().setCategoryId(CATEGORY)
                .setShopSkuKey(SERVICE_KEY2)
                .setMskuId(MSKU_ID)
                .setModifiedTimestamp(LocalDateTime.now()))
        );

        sskuExistenceRepository.markExistence(List.of(SERVICE_KEY1, SERVICE_KEY2), true);
    }

}
