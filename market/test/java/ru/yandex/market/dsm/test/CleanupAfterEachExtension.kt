package ru.yandex.market.dsm.test

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.test.context.junit.jupiter.SpringExtension
import javax.sql.DataSource

const val TRUNCATE_SQL_SCRIPT = "truncate.sql"

class CleanupAfterEachExtension : AfterEachCallback {
    override fun afterEach(context: ExtensionContext) {
        val dataSource: DataSource = getBeanFromExtensionTestContext(context, DataSource::class.java)
        truncateTables(dataSource)
    }

    private fun truncateTables(dataSource: DataSource) {
        val scriptLauncher = ResourceDatabasePopulator()
        scriptLauncher.addScript(ClassPathResource(TRUNCATE_SQL_SCRIPT))
        scriptLauncher.execute(dataSource)
    }

    private fun <T> getBeanFromExtensionTestContext(context: ExtensionContext, beanClass: Class<T>): T {
        return SpringExtension.getApplicationContext(context).getBean(beanClass)
    }
}
