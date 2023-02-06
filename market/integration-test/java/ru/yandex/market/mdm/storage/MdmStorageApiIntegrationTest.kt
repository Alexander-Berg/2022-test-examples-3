package ru.yandex.market.mdm.storage

import org.junit.FixMethodOrder
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer
import ru.yandex.market.mdm.lib.application.MdmBaseIntegrationDbTestClass
import ru.yandex.market.mdm.lib.config.TestIdGenerationConfig
import ru.yandex.market.mdm.storage.config.MdmStorageYtConfig
import ru.yandex.market.mdm.storage.config.TestMdmStorageYmlLoaderConfig
import ru.yandex.market.mdm.storage.config.TestMdmYtTableRpcConfig

@RunWith(SpringRunner::class)
@ContextConfiguration(initializers = [ PGaaSZonkyInitializer::class ])
@SpringBootTest(
    properties = ["spring.profiles.active=test"],
    classes = [
        TestIdGenerationConfig::class,
        TestMdmStorageYmlLoaderConfig::class,
        MdmStorageYtConfig::class,
        TestMdmYtTableRpcConfig::class,
        TestSqlDatasourceConfig::class,
        TestIdSequenceFromYtConfig::class
    ]
)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
abstract class MdmStorageApiIntegrationTest: MdmBaseIntegrationDbTestClass()
