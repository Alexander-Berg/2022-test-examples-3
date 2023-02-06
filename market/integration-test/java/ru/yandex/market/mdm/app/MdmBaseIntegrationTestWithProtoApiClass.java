package ru.yandex.market.mdm.app;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.mbo.mdm.common.MdmCommonConfiguration;
import ru.yandex.market.mbo.mdm.common.config.MdmMasterConfig;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
import ru.yandex.market.mbo.mdm.common.service.bmdm.MdmEntityStorageServiceMock;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.BmdmPathUtils;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.MbocBaseConfiguration;
import ru.yandex.market.mboc.common.MdmDbIntegrationTestOverridesConfiguration;
import ru.yandex.market.mboc.common.MdmIntegrationTestSourcesInitializer;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.mdm.app.proto.MasterDataServiceImpl;

/**
 * @author dmserebr
 * @date 12/02/2020
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = {
    MdmIntegrationTestSourcesInitializer.class,
    PGaaSZonkyInitializer.class})
@SpringBootTest(
    properties = {
        "extra-properties=/app-integration-test.properties",
        "spring.profiles.active=test",
    },
    classes = {
        MdmCommonConfiguration.class,
        MbocBaseConfiguration.class,
        MdmMasterConfig.class,
        MdmDbIntegrationTestOverridesConfiguration.class,
        MasterDataServiceImpl.class
    }
)
@MockBean(classes = {
    LMSClient.class,
})
@Transactional
public abstract class MdmBaseIntegrationTestWithProtoApiClass {
    @Autowired
    private StorageKeyValueService skv;
    @Autowired
    private MdmEntityStorageServiceMock mdmEntityStorageService;

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
        mdmEntityStorageService.registerStorage(
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
        mdmEntityStorageService.registerStorage(
            KnownBmdmIds.SILVER_COMMON_SSKU_ID,
            List.of(silverCommonSskuPrimaryIndex, silverServiceIdIndex),
            true
        );
    }

    @After
    public final void removeYtStorages() {
        mdmEntityStorageService.clearAllStorages();
    }
}
