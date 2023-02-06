package ru.yandex.market.mboc.common;

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
import ru.yandex.market.mbo.mdm.noscan.DataCampToMdmLogbrokerTestConfig;
import ru.yandex.market.mbo.mdm.noscan.MdmMbiLogbrokerTestConfig;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.MbocBaseConfiguration;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

/**
 * Класс с "правильной" общей шапкой аннотаций для интеграционных тестов,
 * чтобы создавался и переиспользовался один контекст.
 *
 * @author yuramalinov
 * @created 16.04.18
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = {MdmIntegrationTestSourcesInitializer.class, PGaaSZonkyInitializer.class})
@SpringBootTest(
    properties = "spring.profiles.active=test",
    classes = {
        MdmCommonConfiguration.class,
        MbocBaseConfiguration.class,
        MdmMasterConfig.class,
        MdmDbIntegrationTestOverridesConfiguration.class,
        MdmMbiLogbrokerTestConfig.class,
        DataCampToMdmLogbrokerTestConfig.class
    }
)
@MockBean(classes = {
    LMSClient.class,
})
@Transactional
public abstract class MdmBaseIntegrationTestWithLogbrokerClass {
    @Autowired
    private StorageKeyValueService skv;
    @Autowired
    private MdmEntityStorageServiceMock mdmEntityStorageService;

    @Before
    public final void initializeTestWideRoobilnicks() {
        skv.invalidateCache();
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
    }

    @After
    public final void removeYtStorages() {
        mdmEntityStorageService.clearAllStorages();
    }
}
