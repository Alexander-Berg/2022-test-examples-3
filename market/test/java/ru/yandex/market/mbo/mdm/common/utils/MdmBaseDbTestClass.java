package ru.yandex.market.mbo.mdm.common.utils;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
import ru.yandex.market.mbo.mdm.common.service.bmdm.MdmEntityStorageServiceMock;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.BmdmPathUtils;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

/**
 * @author yuramalinov
 * @created 10.10.18
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
    initializers = PGaaSZonkyInitializer.class,
    classes = {MdmDbTestConfiguration.class}
)
@Transactional
public abstract class MdmBaseDbTestClass {
    private static final String TIMEOUT_ENV_VAR = "MDM_TEST_TIMEOUT_SEC";
    private static final long DEFAULT_TIMEOUT_SEC = 20;
    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    protected TransactionTemplate transactionTemplate;
    @Autowired
    private StorageKeyValueService skv;
    @Rule
    public Timeout globalTimeout = timeout();
    @Autowired
    private MdmEntityStorageServiceMock mdmEntityStorageServiceMock;

    @Before
    public final void initializeTestWideRoobilnicks() {
    }

    @Before
    public final void registerDefaultYtStorages() {
        MdmEntityStorageServiceMock.IndexDescription flatGoldMskuPrimaryIndex =
            new MdmEntityStorageServiceMock.IndexDescription(
                List.of(List.of(KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_MSKU_ID_ATTRIBUTE_ID)),
                true
            );
        mdmEntityStorageServiceMock.registerStorage(
            KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_TYPE_ID,
            List.of(flatGoldMskuPrimaryIndex),
            true
        );

        MdmEntityStorageServiceMock.IndexDescription silverCommonSskuPrimaryIndex =
            new MdmEntityStorageServiceMock.IndexDescription(
                List.of(
                    BmdmPathUtils.toAttributePath(KnownBmdmIds.SILVER_COMMON_SSKU_BIZ_ID_PATH),
                    BmdmPathUtils.toAttributePath(KnownBmdmIds.SILVER_COMMON_SSKU_SHOP_SKU_PATH),
                    BmdmPathUtils.toAttributePath(KnownBmdmIds.SILVER_COMMON_SSKU_SOURCE_TYPE_PATH),
                    BmdmPathUtils.toAttributePath(KnownBmdmIds.SILVER_COMMON_SSKU_SOURCE_ID_PATH)
                ),
                true
            );
        MdmEntityStorageServiceMock.IndexDescription silverServiceIdIndex =
            new MdmEntityStorageServiceMock.IndexDescription(
                List.of(
                    BmdmPathUtils.toAttributePath(KnownBmdmIds.SILVER_COMMON_SSKU_SERVICE_ID_PATH),
                    BmdmPathUtils.toAttributePath(KnownBmdmIds.SILVER_COMMON_SSKU_SHOP_SKU_PATH),
                    BmdmPathUtils.toAttributePath(KnownBmdmIds.SILVER_COMMON_SSKU_SOURCE_TYPE_PATH),
                    BmdmPathUtils.toAttributePath(KnownBmdmIds.SILVER_COMMON_SSKU_SOURCE_ID_PATH)
                ),
                false
            );
        mdmEntityStorageServiceMock.registerStorage(
            KnownBmdmIds.SILVER_COMMON_SSKU_ID,
            List.of(silverCommonSskuPrimaryIndex, silverServiceIdIndex),
            true
        );

        MdmEntityStorageServiceMock.IndexDescription goldResolutionPrimaryIndex =
            new MdmEntityStorageServiceMock.IndexDescription(
                List.of(
                    List.of(KnownBmdmIds.GOLDEN_RESOLUTION_SUPPLIER_ID_AID),
                    List.of(KnownBmdmIds.GOLDEN_RESOLUTION_SHOP_SKU_AID)
                ),
                true
            );
        mdmEntityStorageServiceMock.registerStorage(
            KnownBmdmIds.GOLDEN_RESOLUTION_ET_ID,
            List.of(goldResolutionPrimaryIndex),
            true
        );

        MdmEntityStorageServiceMock.IndexDescription partnerResolutionPrimaryIndex =
            new MdmEntityStorageServiceMock.IndexDescription(
                List.of(
                    List.of(KnownBmdmIds.SILVER_RESOLUTION_SUPPLIER_ID_AID),
                    List.of(KnownBmdmIds.SILVER_RESOLUTION_SHOP_SKU_AID)

                ),
                true
            );
        mdmEntityStorageServiceMock.registerStorage(
            KnownBmdmIds.SILVER_RESOLUTION_ET_ID,
            List.of(partnerResolutionPrimaryIndex),
            true
        );
    }

    @After
    public final void removeYtStorages() {
        mdmEntityStorageServiceMock.clearAllStorages();
    }

    private static Timeout timeout() {
        String env = System.getProperty(TIMEOUT_ENV_VAR, System.getenv(TIMEOUT_ENV_VAR));
        if (StringUtils.isBlank(env)) {
            return Timeout.seconds(DEFAULT_TIMEOUT_SEC);
        }
        try {
            long timeout = Long.parseLong(env);
            return Timeout.seconds(Math.max(DEFAULT_TIMEOUT_SEC, timeout));
        } catch (NumberFormatException e) {
            return Timeout.seconds(DEFAULT_TIMEOUT_SEC);
        }
    }
}
