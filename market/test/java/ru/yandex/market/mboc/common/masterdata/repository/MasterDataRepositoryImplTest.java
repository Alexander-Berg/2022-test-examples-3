package ru.yandex.market.mboc.common.masterdata.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.MasterDataLogIdService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.pglogid.PgLogIdRow;
import ru.yandex.market.mbo.pglogid.PgLogIdService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.http.MboMappings;

public class MasterDataRepositoryImplTest extends MdmBaseDbTestClass {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    TransactionHelper transactionHelper;
    @Autowired
    TransactionHelper newTransactionHelper;
    @Autowired
    QualityDocumentRepository qualityDocumentRepository;
    @Autowired
    StorageKeyValueService storageKeyValueService;
    @Autowired
    MasterDataLogIdService pgLogIdService;

    private MasterDataRepositoryImpl masterDataRepository;
    private EnhancedRandom random;
    private MboMappings.ApprovedMappingInfo mapping;

    private static final long SEED = 242135L;
    private static final int SUPPLIER_ID = 1;

    @Before
    public void setUp() throws Exception {
        masterDataRepository = new MasterDataRepositoryImpl(jdbcTemplate, transactionHelper,
                transactionTemplate, qualityDocumentRepository);
        random = TestDataUtils.defaultRandom(SEED);
        mapping = addOfferToDB();
    }

    @Test
    public void testFindUpdatedKeysBatchBySeqId() {
        long initialSeqId = PgLogIdService.INITIAL_MODIFIED_SEQUENCE_ID;
        String processorName = "erpShippingUnitExporterService";
        MasterData masterData = generateMasterData(generateDocument());
        masterData.setShopSku("1234");
        masterDataRepository.insert(masterData);
        List<Map<String, Object>> rs1 = jdbcTemplate.getJdbcOperations()
            .queryForList("select * from mdm_audit.log_id_master_data");
        Assertions.assertThat(rs1).hasSize(1);
        Assertions.assertThat(rs1.get(0).get("modified_seq_id")).isNull();
        int batchSize = 10;
        jdbcTemplate.getJdbcTemplate().queryForList("select mdm_audit.log_id_master_data_update_seq(?)", batchSize);
        List<PgLogIdRow<ShopSkuKey>> batch1 = findUpdatedKeysBatchBySeqId(initialSeqId, batchSize);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(batch1).hasSize(1);
            s.assertThat(batch1).extracting("key").isEqualTo(List.of(masterData.getShopSkuKey()));
            s.assertThat(batch1).extracting("modifiedSeqId").isNotNull();
        });
        long lastSeqId = batch1.get(0).getModifiedSeqId();
        masterData.setCategoryId(12398L);
        masterDataRepository.insertOrUpdate(masterData);
        List<PgLogIdRow<ShopSkuKey>> batch2 = findUpdatedKeysBatchBySeqId(lastSeqId, batchSize);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(batch1).hasSize(1);
            s.assertThat(batch1).extracting("key").isEqualTo(List.of(masterData.getShopSkuKey()));
            s.assertThat(batch1).extracting("modifiedSeqId").isNotNull();
        });
        List<Map<String, Object>> rs3 = jdbcTemplate.getJdbcOperations()
            .queryForList("select * from mdm_audit.log_id_master_data");
        Assertions.assertThat(rs3).hasSize(1);
        Assertions.assertThat(rs3.get(0).get("modified_seq_id")).isNull();
    }

    @Test
    public void testCount() {
        MasterData masterData1 = generateMasterData(generateDocument());
        masterData1 = masterDataRepository.insert(masterData1);

        LocalDateTime localDateTime = masterData1.getModifiedTimestamp().plusMinutes(2);
        MasterDataFilter masterDataFilter = new MasterDataFilter().setModifiedAfter(localDateTime);
        Assertions.assertThat(masterDataRepository.count(masterDataFilter)).isEqualTo(0L);

        localDateTime = masterData1.getModifiedTimestamp().minusMinutes(2);
        masterDataFilter.setModifiedAfter(localDateTime);
        Assertions.assertThat(masterDataRepository.count(masterDataFilter)).isEqualTo(1L);

        masterDataFilter.setSupplierId(masterData1.getSupplierId());
        Assertions.assertThat(masterDataRepository.count(masterDataFilter)).isEqualTo(1L);

        masterDataFilter.setSupplierId(masterData1.getSupplierId() + 1);
        Assertions.assertThat(masterDataRepository.count(masterDataFilter)).isEqualTo(0L);
    }

    @Test
    public void testEmptyListOfSskuForFindByShopSkuKeysWithoutNulls() {
        MasterData masterData = generateMasterData(generateDocument());
        masterDataRepository.insertOrUpdateAll(List.of(masterData));

        var masterDatas = masterDataRepository.findByShopSkuKeysWithoutNulls(List.of(), false);
        Assertions.assertThat(masterDatas).isEmpty();
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

    private QualityDocument generateDocument() {
        return TestDataUtils.generateDocument(random);
    }

    private List<PgLogIdRow<ShopSkuKey>> findUpdatedKeysBatchBySeqId(long fromModifiedSequenceId, int batchSize) {
        return pgLogIdService.getModifiedRecordsIdBatch(fromModifiedSequenceId, batchSize);
    }
}
