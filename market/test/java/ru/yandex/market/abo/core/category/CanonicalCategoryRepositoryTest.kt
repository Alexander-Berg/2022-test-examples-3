package ru.yandex.market.abo.core.category

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest

open class CanonicalCategoryRepositoryTest @Autowired constructor(
    private val canonicalCategoryRepository: CanonicalCategoryRepository
) : EmptyTest() {

    @Test
    fun deleteByParentIdAndName() {
        canonicalCategoryRepository.saveAll(listOf(
            CanonicalCategoryEntity(0, "0", 0),
            CanonicalCategoryEntity(1, "1", 0),
            CanonicalCategoryEntity(2, "0", 1),
            CanonicalCategoryEntity(3, "1", 1)
        ))
        flushAndClear()
        canonicalCategoryRepository.deleteByParentIdAndName(1, "1")
        flushAndClear()
        assertThat(canonicalCategoryRepository.findAll())
            .extracting<Int> { it.id }
            .containsExactlyInAnyOrder(0, 1, 2)
    }
}
