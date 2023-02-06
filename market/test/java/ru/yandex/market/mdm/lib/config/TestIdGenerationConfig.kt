package ru.yandex.market.mdm.lib.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.yandex.market.mdm.db.config.TestSqlMdmDatasourceConfig

@Configuration
@Import(
    TestSqlMdmDatasourceConfig::class
)
open class TestIdGenerationConfig(db: TestSqlMdmDatasourceConfig) : IdGenerationConfig(db)
