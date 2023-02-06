package ru.yandex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.abo.core.yt.YtCluster

open class PgEnumTest @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {
    private val javaEnumByPgEnum = hashMapOf(
        "yt_cluster" to YtCluster::class.java
    )

    @Test
    fun `all pg enums has java enum`() {
        val pgValuesByPgType = jdbcTemplate.query("""
            SELECT t.typname   AS enum_name,
                   e.enumlabel AS enum_value
            FROM pg_type t
                     JOIN pg_enum e ON t.oid = e.enumtypid
        """.trimIndent()) { rs, _ ->
            rs.getString("enum_name") to rs.getString("enum_value")
        }.groupBy({ (it.first) }, { it.second })

        pgValuesByPgType.forEach { (pgType, pgValues) ->
            val javaEnum = javaEnumByPgEnum[pgType]
            assertNotNull(javaEnum, "not specified java enum for pg enum '$pgType'")
            assertEquals(
                javaEnum!!.enumConstants.map { it.name }.sorted(),
                pgValues.sorted(),
                "$javaEnum and $pgType have different values"
            )
        }
    }
}
