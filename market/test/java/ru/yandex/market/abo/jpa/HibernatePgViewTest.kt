package ru.yandex.market.abo.jpa

import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable
import javax.persistence.Entity
import javax.persistence.Table
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.jpa.PgJpaRepository
import ru.yandex.market.abo.util.db.datasource.WithDataSource

open class HibernatePgViewTest @Autowired constructor(
    jdbcTemplate: JdbcTemplate
) : EmptyTest() {
    private val pgViews = jdbcTemplate.queryForList("""
        SELECT viewname FROM pg_views
        UNION ALL
        SELECT matviewname FROM pg_matviews
    """.trimIndent(), String::class.java).toHashSet()

    /**
     * Проверяем, что у репозиториев ссылающихся на вьюху/матвьюху/ни на что (используется как роу-маппер)
     * указана аннотация [WithDataSource]
     * Почти всегда в таких случаях стоит ходить в реплику
     */
    @Test
    fun `@WithDataSource on repositories with view`() {
        Reflections("ru.yandex.market.abo").getSubTypesOf(PgJpaRepository::class.java).forEach { repoInterface ->
            val baseInterface = repoInterface.genericInterfaces[0] as ParameterizedType
            val genericType = baseInterface.actualTypeArguments[0]
            if (genericType is TypeVariable<*>) {
                return@forEach
            }
            val domainClass = genericType as Class<*>
            val tableName = extractTableName(domainClass)
            if (!pgViews.contains(tableName)) {
                return@forEach
            }
            assertTrue(
                repoInterface.isAnnotationPresent(WithDataSource::class.java),
                "repo ${repoInterface.name} for entity ${domainClass.simpleName} is not annotated with @WithDataSource"
            )
        }
    }

    private fun extractTableName(domainClass: Class<*>): String? {
        return (domainClass.getAnnotation(Table::class.java)?.name
            ?: domainClass.getAnnotation(Entity::class.java)?.name)
            .takeUnless { it.isNullOrBlank() }
    }
}
