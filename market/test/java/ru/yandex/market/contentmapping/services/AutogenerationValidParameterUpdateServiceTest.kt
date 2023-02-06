package ru.yandex.market.contentmapping.services

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import ru.yandex.market.contentmapping.kotlin.typealiases.CategoryId

class AutogenerationValidParameterUpdateServiceTest {

    @Test
    fun `check non leaf categories empty`() {
        val allCategories = computeNonLeaf(emptyMap()) {
            emptyMap<CategoryId, CategoryId>().getOrDefault(it, 0L)
        }
        allCategories shouldHaveSize 0
    }

    /*
        0 - 1 - 2 - 5: 10, 11
                  - 6: 10, 11, 12
              - 3 - 7: 10, 13
                  - 8: 10, 14
              - 4:     10
     */
    @Test
    fun `check non leaf categories`() {
        val parents = mapOf(1L to 0L, 2L to 1L, 3L to 1L, 4L to 1L, 5L to 2L, 6L to 2L, 7L to 3L, 8L to 3L)
        val leafCategories = mapOf(
                5L to listOf(10L, 11L),
                6L to listOf(10L, 11L, 12L),
                7L to listOf(10L, 13L),
                8L to listOf(10L, 14L),
                4L to listOf(10L),
        )

        val allCategories = computeNonLeaf(leafCategories) {
            parents.getOrDefault(it, 0L)
        }
        allCategories shouldHaveSize parents.size
        parents.keys.forEach {
            allCategories shouldContainKey it
        }

        leafCategories.keys.forEach {
            allCategories[it] shouldNotBe null
            allCategories[it]?.asClue { params ->
                val paramsOriginal = leafCategories.getOrDefault(it, emptyList())
                params shouldHaveSize paramsOriginal.size
                params shouldContainExactly paramsOriginal
            }
        }

        allCategories[1] shouldNotBe null
        allCategories[1]?.asClue {
            it shouldHaveSize 1
            it shouldContainExactly setOf(10)
        }

        allCategories[2] shouldNotBe null
        allCategories[2]?.asClue {
            it shouldHaveSize 2
            it shouldContainExactly setOf(10, 11)
        }

        allCategories[3] shouldNotBe null
        allCategories[3]?.asClue {
            it shouldHaveSize 1
            it shouldContainExactly setOf(10)
        }

        allCategories[4] shouldNotBe null
        allCategories[4]?.asClue {
            it shouldHaveSize 1
            it shouldContainExactly setOf(10)
        }
    }
}
