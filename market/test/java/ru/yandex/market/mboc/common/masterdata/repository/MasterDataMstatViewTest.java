package ru.yandex.market.mboc.common.masterdata.repository;

import java.util.List;

import javax.annotation.Nullable;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

@SuppressWarnings("checkstyle:MagicNumber")
public class MasterDataMstatViewTest extends MdmBaseDbTestClass {
    private static final int BERU_ID = SupplierConverterServiceMock.BERU_ID;

    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private QualityDocumentRepository qualityDocumentRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;

    private MasterDataRepository masterDataRepository;
    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(1050402L);
        masterDataRepository = new MasterDataRepositoryImpl(jdbcTemplate, transactionHelper,
                transactionTemplate, qualityDocumentRepository);
    }

    @Test
    public void whenAllMasterDataTypesPresentShouldCreateCorrectView() {
        // 1. Подготовим поставщиков. По классике пусть будут 1Р, 3Р, REAL и бизнес.
        MdmSupplier firstPartySupplier = supplier(BERU_ID, MdmSupplierType.FIRST_PARTY, null);
        MdmSupplier thirdPartySupplier = supplier(3, MdmSupplierType.THIRD_PARTY, null);
        MdmSupplier realSupplier1 = supplier(66, MdmSupplierType.REAL_SUPPLIER, "00066");
        MdmSupplier realSupplier2 = supplier(77, MdmSupplierType.REAL_SUPPLIER, "00077");
        MdmSupplier businessSupplier = supplier(888, MdmSupplierType.BUSINESS, null);
        mdmSupplierRepository.insertBatch(firstPartySupplier, thirdPartySupplier, realSupplier1,
            realSupplier2, businessSupplier);

        // 2. Подготовим мастер-данные всех возможных видов.
        ShopSkuKey validExternalKey1 = new ShopSkuKey(BERU_ID, "00066.600007980231");
        ShopSkuKey validExternalKey2 = new ShopSkuKey(BERU_ID, "00077.0064.7592048760228");
        ShopSkuKey missingExternalKey = new ShopSkuKey(BERU_ID, "0064.600007980231");
        ShopSkuKey invalidExternalKey = new ShopSkuKey(BERU_ID, "600007980231");

        ShopSkuKey validInternalKey1 = new ShopSkuKey(realSupplier1.getId(), "600007980231");
        ShopSkuKey validInternalKey2 = new ShopSkuKey(realSupplier2.getId(), "0064.7592048760228");

        ShopSkuKey otherThirdPartyKey1 = new ShopSkuKey(thirdPartySupplier.getId(), "xxx");
        ShopSkuKey otherThirdPartyKey2 = new ShopSkuKey(thirdPartySupplier.getId(), "00066.xxx");

        ShopSkuKey otherBusinessKey = new ShopSkuKey(businessSupplier.getId(), "zzz");

        MasterData validExternalMD1 = generateAndStoreMD(validExternalKey1);
        MasterData validExternalMD2 = generateAndStoreMD(validExternalKey2);
        MasterData missingExternalMD = generateAndStoreMD(missingExternalKey);
        MasterData invalidExternalMD = generateAndStoreMD(invalidExternalKey);
        MasterData validInternalMD1 = generateAndStoreMD(validInternalKey1);
        MasterData validInternalMD2 = generateAndStoreMD(validInternalKey2);
        MasterData otherThirdPartyMD1 = generateAndStoreMD(otherThirdPartyKey1);
        MasterData otherThirdPartyMD2 = generateAndStoreMD(otherThirdPartyKey2);
        MasterData otherBusinessMD = generateAndStoreMD(otherBusinessKey);

        MasterData expected1PMD1 = new MasterData();
        expected1PMD1.copyDataFieldsFrom(validExternalMD1);
        expected1PMD1.setShopSkuKey(validInternalKey1);

        MasterData expected1PMD2 = new MasterData();
        expected1PMD2.copyDataFieldsFrom(validExternalMD2);
        expected1PMD2.setShopSkuKey(validInternalKey2);

        // 3. Проверяем вьюху
        List<MasterData> fromView = jdbcTemplate.query("select shop_sku, supplier_id, " +
                "msku_import_status, data, modified_timestamp, category_id " +
                "from mdm.v_master_data_internalized",
            Mappers.MASTER_DATA_MAPPER.toRowMapper());
        Assertions.assertThat(fromView).containsExactlyInAnyOrder(
            expected1PMD1, expected1PMD2, otherThirdPartyMD1, otherThirdPartyMD2
        );
    }

    private MdmSupplier supplier(int id, MdmSupplierType type, @Nullable String realId) {
        var supplier = new MdmSupplier();
        supplier.setId(id);
        supplier.setType(type);
        if (realId != null) {
            supplier.setRealSupplierId(realId);
        }
        return supplier;
    }

    private MasterData generateAndStoreMD(ShopSkuKey key) {
        MasterData md = TestDataUtils.generateMasterData(key, random);
        masterDataRepository.insert(md);
        // перезагружаем из базы, чтобы избавиться от транзиентных полей, мешающих сравнению.
        md = masterDataRepository.findById(key);
        return md;
    }
}
