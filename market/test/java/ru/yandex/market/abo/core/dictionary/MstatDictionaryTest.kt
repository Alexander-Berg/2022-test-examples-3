package ru.yandex.market.abo.core.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 06.09.2021
 */
abstract class MstatDictionaryTest : EmptyTest() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    protected abstract fun getDictionaryConfig(): MstatDictionaryConfig

    protected open fun initDictionarySourceData() {
        /*
            by default:
            do not validate dictionary result
            validate columns and grants only
        */
    }

    protected open fun validateResult() {
        /*
            by default:
            do not validate dictionary result
            validate columns and grants only
        */
    }

    @BeforeEach
    fun init() {
        initDictionarySourceData()
        flushAndClear()
    }

    @Test
    fun `mstat dictionary test`() {
        validateGrants()
        validateColumns()
        validateResult()
    }

    private fun validateGrants() {
        val dictionaryConfig = getDictionaryConfig()
        val grants = HashSet(jdbcTemplate.queryForList("""
            SELECT privilege_type
            FROM information_schema.table_privileges
            WHERE table_name = ?
              AND table_schema = 'public'
              AND grantee = 'aboreader'
        """.trimIndent(), String::class.java, dictionaryConfig.dbEntityName))

        assertEquals(
            1, grants.size,
            "More then one grants was found for aboreader user! aboreader must have SELECT grant only!"
        )
        assertEquals("SELECT", grants.first(), "aboreader has not SELECT grant!")
    }

    private fun validateColumns() {
        val dictionaryConfig = getDictionaryConfig()
        val dbColumns = HashSet(jdbcTemplate.queryForList("""
            SELECT column_name
            FROM information_schema.columns
            WHERE table_name = ?
              AND table_schema = 'public'
        """.trimIndent(), String::class.java, dictionaryConfig.dbEntityName))
        val columnsDiff = dictionaryConfig.columns.subtract(dbColumns)

        assertTrue(
            columnsDiff.isEmpty(),
            """
                Db entity columns set was changed! 
                Not all columns from config represent in db entity!
                Not represented columns: $columnsDiff
                """.trimIndent()
        )
    }
}
