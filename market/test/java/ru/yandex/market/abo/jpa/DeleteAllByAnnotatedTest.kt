package ru.yandex.market.abo.jpa

import java.lang.reflect.Method
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.yandex.market.abo.core.jpa.PgJpaRepository

open class DeleteAllByAnnotatedTest {

    @Test
    open fun `DeleteAllBy has annotations @Transactional, @Modifying, @Query, @Param`() {
        val classes = Reflections(
            "ru.yandex.market.abo",
            SubTypesScanner(false),
            MethodAnnotationsScanner()
        )

        val incorrectMethods = classes.getSubTypesOf(PgJpaRepository::class.java)
            .flatMap { it.methods.asList() }
            .filter { it.name.startsWith("deleteAllBy") || it.name.startsWith("deleteBy") }
            .filterNot {
                it.isAnnotationPresent(Query::class.java) &&
                    it.isAnnotationPresent(Modifying::class.java) &&
                    (it.isAnnotationPresent(org.springframework.transaction.annotation.Transactional::class.java) ||
                        it.isAnnotationPresent(javax.transaction.Transactional::class.java)) &&
                    it.parameters.all { it.isAnnotationPresent(Param::class.java) }
            }

        assertTrue(incorrectMethods.isEmpty(), getErrorMessage(incorrectMethods))
    }

    fun getErrorMessage(incorrectMethods: List<Method>) = """
            |incorrect methods:
            |${incorrectMethods.joinToString("\n") { "${it.declaringClass.name} ${it.name}" }}
            """.trimMargin()
}
