package ru.yandex.market.mdm.integration.test.logbroker;

import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerService;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestWithLogbrokerClass;
import ru.yandex.market.mdm.integration.test.logbroker.utils.DataCampMdmIntTestHelper;

@DirtiesContext
public class DataCampToMdmLogbrokerIntegrationTest extends MdmBaseIntegrationTestWithLogbrokerClass {
    private static final int LB_TIMEOUT_SEC = 600;
    // Checking updates only for biz_ids
    // All business logic with services has been already checked in DataCampToMdmLogbrokerDataProcessorImplTest
    private static final int BUSINESS = 1;
    private static final long MSKU = 123456L;
    private static final int CATEGORY = 777;
    private static final int UPDATED_CATEGORY = 888;
    private static final String BUSINES_SHOP_SKU = "xxx";

    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    @Qualifier("testingDataCampToMdmProducer")
    private MdmLogbrokerService datacampToMdmLogbrokerService;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    StorageKeyValueService keyValueService;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;

    @Before
    public void setup() {
        mappingsCacheRepository.deleteAll();
        mdmSupplierRepository.deleteAll();
        MdmSupplier existingBusiness = new MdmSupplier()
            .setId(BUSINESS)
            .setType(MdmSupplierType.BUSINESS)
            .setBusinessEnabled(true)
            .setDeleted(false);
        mdmSupplierRepository.insertBatch(existingBusiness);
        commit();
    }

    @Test
    @Ignore
    public void testNewMappingsReceived() {
        commit();
        datacampToMdmLogbrokerService.publishEvent(DataCampMdmIntTestHelper.message(
            DataCampMdmIntTestHelper.unitedOffer(CATEGORY, BUSINESS, BUSINES_SHOP_SKU, MSKU)));
        MappingCacheDao expectedBiz =
            DataCampMdmIntTestHelper.mappingCacheDao(BUSINESS, BUSINES_SHOP_SKU, CATEGORY, MSKU);
        Awaitility.await().atMost(LB_TIMEOUT_SEC, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertThat(mappingsCacheRepository.findAll())
                .usingElementComparatorIgnoringFields("updateStamp", "modifiedTimestamp", "versionTimestamp",
                    "mappingSource")
                .containsExactlyInAnyOrder(
                    expectedBiz
                );
        });
    }

    @Test
    @Ignore
    public void testOldMappingsUpdated() {
        commit();
        MappingCacheDao insertedBiz =
            DataCampMdmIntTestHelper.mappingCacheDao(BUSINESS, BUSINES_SHOP_SKU, CATEGORY, MSKU);
        mappingsCacheRepository.insertBatch(insertedBiz);
        commit();
        datacampToMdmLogbrokerService.publishEvent(DataCampMdmIntTestHelper.message(
            DataCampMdmIntTestHelper.unitedOffer(UPDATED_CATEGORY, BUSINESS, BUSINES_SHOP_SKU, MSKU)));
        MappingCacheDao expectedBiz =
            DataCampMdmIntTestHelper.mappingCacheDao(BUSINESS, BUSINES_SHOP_SKU, UPDATED_CATEGORY, MSKU);
        Awaitility.await().atMost(LB_TIMEOUT_SEC, TimeUnit.SECONDS).untilAsserted(() -> {
            Assertions.assertThat(mappingsCacheRepository.findAll())
                .usingElementComparatorIgnoringFields("updateStamp", "modifiedTimestamp", "versionTimestamp",
                    "mappingSource")
                .containsExactlyInAnyOrder(
                    expectedBiz
                );
        });
    }

    @After
    public void cleanup() {
        mappingsCacheRepository.deleteAll();
        mdmSupplierRepository.deleteAll();
        commit();
    }

    private void commit() {
        jdbcTemplate.getJdbcTemplate().execute("commit");
    }
}
