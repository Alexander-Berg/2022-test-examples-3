package ru.yandex.market.abo.jpa

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import java.lang.reflect.Method
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Enumerated
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import ru.yandex.EmptyTest

/**
 * @author komarovns
 */
open class HibernateEnumTest : EmptyTest() {

    @Test
    open fun `enumerated annotation test`() {
        val types = entityManager.metamodel.managedTypes
        val attributes = types.asSequence()
            .flatMap { it.attributes }
            .map { it.javaMember }
            .toList()
        if (attributes.isEmpty()) {
            throw IllegalStateException("cannot find hibernate attributes")
        }

        attributes.forEach { attr ->
            when (attr) {
                is Field -> validateField(attr)
                is Method -> validateMethod(attr)
            }
        }
    }

    private fun validateField(field: Field) {
        if (field.type.isEnum && hasEnumeratedAnnotationProblem(field)) {
            fail<Any>("field $field is enum and doesn't have annotations @Enumerated")
        }
    }

    private fun validateMethod(method: Method) {
        if (method.returnType.isEnum && hasEnumeratedAnnotationProblem(method)) {
            fail<Any>("method $method returns enum and doesn't have annotations @Enumerated")
        }
    }

    private fun hasEnumeratedAnnotationProblem(element: AnnotatedElement) =
        element.isAnnotationPresent(Column::class.java)
            && !(element.isAnnotationPresent(Enumerated::class.java) || element.isAnnotationPresent(Convert::class.java))
}
