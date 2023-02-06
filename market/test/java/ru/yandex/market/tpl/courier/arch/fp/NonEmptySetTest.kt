package ru.yandex.market.tpl.courier.arch.fp

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import ru.yandex.market.tpl.courier.domain.feature.task.deliveryTaskIdTestInstance

class NonEmptySetTest {

    @Test
    fun `Кидает исключение если в коллекции есть одинаковые элементы и allowDuplicates == false`() {
        shouldThrow<IllegalArgumentException> {
            listOf(
                deliveryTaskIdTestInstance(1L),
                deliveryTaskIdTestInstance(2L),
                deliveryTaskIdTestInstance(3L),
                deliveryTaskIdTestInstance(2L),
                deliveryTaskIdTestInstance(2L),
                deliveryTaskIdTestInstance(4L),
            )
                .verifyNonEmptySet(allowDuplicates = false)
                .orThrow()
        }
    }

    @Test
    fun `Не кидает исключение если в коллекции нет одинаковых элементов и allowDuplicates == false`() {
        shouldNotThrowAny {
            listOf(
                deliveryTaskIdTestInstance(1L),
                deliveryTaskIdTestInstance(2L),
                deliveryTaskIdTestInstance(3L),
                deliveryTaskIdTestInstance(4L),
            )
                .verifyNonEmptySet(allowDuplicates = false)
                .orThrow()
        }
    }

    @Test
    fun `Не кидает исключение если в коллекции есть одинаковые элементы и allowDuplicates == true`() {
        shouldNotThrowAny {
            listOf(
                deliveryTaskIdTestInstance(1L),
                deliveryTaskIdTestInstance(2L),
                deliveryTaskIdTestInstance(3L),
                deliveryTaskIdTestInstance(2L),
                deliveryTaskIdTestInstance(2L),
                deliveryTaskIdTestInstance(4L),
            )
                .verifyNonEmptySet(allowDuplicates = true)
                .orThrow()
        }
    }
}