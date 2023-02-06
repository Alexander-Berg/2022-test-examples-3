package ru.yandex.market.mdm.lib.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import ru.yandex.market.mdm.db.config.TestSqlMdmDatasourceConfig

@Configuration
@Import(
    TestSqlMdmDatasourceConfig::class,
    TestIdGenerationConfig::class
)
@PropertySource("classpath:test.properties")
open class TestConfig
