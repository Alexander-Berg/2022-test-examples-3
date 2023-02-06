package ru.yandex.market.contentmapping.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import ru.yandex.market.contentmapping.config.utils.TestServiceWrapper
import javax.sql.DataSource

/**
 * @author yuramalinov
 * @created 27.08.2019
 */
@Configuration
open class TestSqlDatasourceConfig(testServiceWrapper: TestServiceWrapper) : SqlDatasourceConfig(testServiceWrapper) {
    @Autowired
    private lateinit var environment: Environment

    override fun slaveSqlDataSource(): DataSource {
        // We don't have separate DB-s in tests, tests run in singe transaction,
        // so separate datasource doesn't have changes to work properly. So just link it to single bean.
        return dataSource()
    }

    override fun getContexts(): String? {
        return environment.activeProfiles.joinToString()
    }
}
