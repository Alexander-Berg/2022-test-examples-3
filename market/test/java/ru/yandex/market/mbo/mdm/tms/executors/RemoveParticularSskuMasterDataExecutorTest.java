package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;

@SuppressWarnings("checkstyle:MagicNumber")
public class RemoveParticularSskuMasterDataExecutorTest extends MdmBaseDbTestClass {
    private static final int BERU_ID = SupplierConverterServiceMock.BERU_ID; // FIRST_PARTY
    private static final int REAL_SID = 2456;
    private static final int THIRD_PARTY_SID = 7582;

    @Autowired
    private QualityDocumentRepository qualityDocumentRepository;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private MdmSupplierRepository supplierRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;

    private MasterDataRepository masterDataRepository;
    private SupplierConverterServiceMock supplierConverterService;
    private RemoveParticularSskuMasterDataExecutor executor;
    private EnhancedRandom random;

    @Before
    public void setup() {
        supplierConverterService = new SupplierConverterServiceMock();
        random = TestDataUtils.defaultRandom(505670243L);
        masterDataRepository = new MasterDataRepositoryImpl(
            jdbcTemplate,
            transactionHelper,
            transactionTemplate,
            qualityDocumentRepository
        );
        executor = new RemoveParticularSskuMasterDataExecutor(
            jdbcTemplate,
            transactionHelper,
            transactionTemplate,
            qualityDocumentRepository,
            supplierConverterService,
            new StorageKeyValueServiceMock()
        );
    }

    @Test
    public void whenNoSpecialCasesShouldDeleteInternal1PsAsExpected() {
        prepareSupplier(BERU_ID, "Беру", MdmSupplierType.FIRST_PARTY, null);
        prepareSupplier(REAL_SID, "ООО 'Реалити'", MdmSupplierType.REAL_SUPPLIER, "0064");
        prepareSupplier(THIRD_PARTY_SID, "Торговый дом 'ТРОЯ'", MdmSupplierType.THIRD_PARTY, null);

        ShopSkuKey externalKey = new ShopSkuKey(BERU_ID, "0064.наконечник-копьё-нерж278");
        ShopSkuKey internalKey = new ShopSkuKey(REAL_SID, "наконечник-копьё-нерж278");
        ShopSkuKey random3PKey = new ShopSkuKey(THIRD_PARTY_SID, "набор-4-юный-инквизитор");
        supplierConverterService.addInternalToExternalMapping(internalKey, externalKey);

        MasterData externalMD = TestDataUtils.generateMasterData(externalKey, random);
        MasterData internalMD = TestDataUtils.generateMasterData(internalKey, random);
        MasterData random3PMD = TestDataUtils.generateMasterData(random3PKey, random);

        masterDataRepository.insertBatch(externalMD, internalMD, random3PMD);

        executor.execute();
        Assertions.assertThat(storedKeys()).containsExactlyInAnyOrder(externalKey, random3PKey);
    }

    @Test
    public void whenExternalCounterpartIsMissingShouldThrow() {
        prepareSupplier(BERU_ID, "Беру", MdmSupplierType.FIRST_PARTY, null);
        prepareSupplier(REAL_SID, "ПАО 'Реалити'", MdmSupplierType.REAL_SUPPLIER, "0064");
        prepareSupplier(THIRD_PARTY_SID, "Торговый дом 'ТРОЯ'", MdmSupplierType.THIRD_PARTY, null);

        ShopSkuKey externalKey = new ShopSkuKey(BERU_ID, "0064.наконечник-копьё-нерж278");
        ShopSkuKey internalKey = new ShopSkuKey(REAL_SID, "наконечник-копьё-нерж278");
        ShopSkuKey random3PKey = new ShopSkuKey(THIRD_PARTY_SID, "набор-4-юный-инквизитор");
        supplierConverterService.addInternalToExternalMapping(internalKey, externalKey);

        // not inserting external MD here
        MasterData internalMD = TestDataUtils.generateMasterData(internalKey, random);
        MasterData random3PMD = TestDataUtils.generateMasterData(random3PKey, random);

        masterDataRepository.insertBatch(internalMD, random3PMD);

        Assertions.assertThatThrownBy(() -> executor.execute())
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Can't remove master data that does not have external copy, aborting.");
        Assertions.assertThat(storedKeys()).containsExactlyInAnyOrder(internalKey, random3PKey);
    }

    @Test
    public void whenKeyIsInFact3PShouldThrow() {
        prepareSupplier(BERU_ID, "Беру", MdmSupplierType.FIRST_PARTY, null);
        prepareSupplier(REAL_SID, "ПАО 'Реалити'", MdmSupplierType.REAL_SUPPLIER, "0064");
        prepareSupplier(THIRD_PARTY_SID, "Торговый дом 'ТРОЯ'", MdmSupplierType.THIRD_PARTY, null);

        ShopSkuKey externalKey = new ShopSkuKey(BERU_ID, "0064.наконечник-копьё-нерж278");
        ShopSkuKey internalKey = new ShopSkuKey(REAL_SID, "наконечник-копьё-нерж278");
        ShopSkuKey random3PKey = new ShopSkuKey(THIRD_PARTY_SID, "набор-4-юный-инквизитор");
        supplierConverterService.addInternalToExternalMapping(externalKey, externalKey); // force int-ext equality

        MasterData externalMD = TestDataUtils.generateMasterData(externalKey, random);
        MasterData internalMD = TestDataUtils.generateMasterData(internalKey, random);
        MasterData random3PMD = TestDataUtils.generateMasterData(random3PKey, random);

        masterDataRepository.insertBatch(externalMD, internalMD, random3PMD);

        Assertions.assertThatThrownBy(() -> executor.execute())
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Key inconsistency found, aborting.");
        Assertions.assertThat(storedKeys()).containsExactlyInAnyOrder(externalKey, internalKey, random3PKey);
    }

    @Test
    public void whenExternalKeyHasNoBeruIdShouldThrow() {
        prepareSupplier(BERU_ID, "Беру", MdmSupplierType.FIRST_PARTY, null);
        prepareSupplier(REAL_SID, "ПАО 'Реалити'", MdmSupplierType.REAL_SUPPLIER, "0064");
        prepareSupplier(THIRD_PARTY_SID, "Торговый дом 'ТРОЯ'", MdmSupplierType.THIRD_PARTY, null);

        ShopSkuKey externalKey = new ShopSkuKey(BERU_ID, "0064.наконечник-копьё-нерж278");
        ShopSkuKey internalKey = new ShopSkuKey(REAL_SID, "наконечник-копьё-нерж278");
        ShopSkuKey random3PKey = new ShopSkuKey(THIRD_PARTY_SID, "набор-4-юный-инквизитор");
        supplierConverterService.addInternalToExternalMapping(internalKey, random3PKey); // force ext to look like 3P

        MasterData externalMD = TestDataUtils.generateMasterData(externalKey, random);
        MasterData internalMD = TestDataUtils.generateMasterData(internalKey, random);
        MasterData random3PMD = TestDataUtils.generateMasterData(random3PKey, random);

        masterDataRepository.insertBatch(externalMD, internalMD, random3PMD);

        Assertions.assertThatThrownBy(() -> executor.execute())
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Key inconsistency found, aborting.");
        Assertions.assertThat(storedKeys()).containsExactlyInAnyOrder(externalKey, internalKey, random3PKey);
    }

    private void prepareSupplier(int id, String name, MdmSupplierType type, @Nullable String realId) {
        MdmSupplier supplier = new MdmSupplier();
        supplier.setId(id);
        supplier.setName(name);
        supplier.setRealSupplierId(realId);
        supplier.setType(type);
        supplierRepository.insert(supplier);
    }

    private List<ShopSkuKey> storedKeys() {
        return masterDataRepository.findAll().stream().map(MasterData::getShopSkuKey).collect(Collectors.toList());
    }
}
