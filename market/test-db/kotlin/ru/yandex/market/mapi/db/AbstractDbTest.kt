package ru.yandex.market.mapi.db

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.test.context.ActiveProfiles
import ru.yandex.market.mapi.AbstractMapiBaseTest
import ru.yandex.market.mapi.db.mock.MapiEmbeddedDbConfig
import javax.sql.DataSource

@Import(
    value = [MapiEmbeddedDbConfig::class]
)
@ActiveProfiles(value = ["junit", "junit-db"])
abstract class AbstractDbTest : AbstractMapiBaseTest() {

    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    @Qualifier("pgDataSource")
    protected lateinit var pgDataSource: DataSource

    @BeforeEach
    fun cleanDb() {
        pgDataSource.applySqlScript("truncate.sql")
    }

    private fun DataSource.applySqlScript(sqlFilePath: String) {
        val scriptLauncher = ResourceDatabasePopulator()
        scriptLauncher.addScript(ClassPathResource(sqlFilePath))
        scriptLauncher.execute(this)
    }
}
