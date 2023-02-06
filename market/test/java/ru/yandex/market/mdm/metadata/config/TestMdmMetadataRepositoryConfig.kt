package ru.yandex.market.mdm.metadata.config

import org.mockito.Mockito
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.yandex.market.mdm.db.config.TestSqlMdmDatasourceConfig
import ru.yandex.market.mdm.lib.config.TestIdGenerationConfig
import ru.yandex.market.mdm.lib.database.MdmStorageKeyValueRepository

@Configuration
@Import(
    TestSqlMdmDatasourceConfig::class,
    TestIdGenerationConfig::class
)
open class TestMdmMetadataRepositoryConfig(
    db: TestSqlMdmDatasourceConfig,
    mdmIdGenerationConfig: TestIdGenerationConfig
) : MdmMetadataRepositoryConfig(db, mdmIdGenerationConfig) {
    override fun skv(): MdmStorageKeyValueRepository {
        return Mockito.mock(MdmStorageKeyValueRepository::class.java)
    }
}
