package ru.yandex.market.abo.core.resupply.stock

import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.category.CanonicalCategoryEntity
import ru.yandex.market.abo.core.category.CanonicalCategoryRepository

open class ResupplyQualityMatrixRepositoryTest @Autowired constructor(
    private val resupplyQualityMatrixRepository: ResupplyQualityMatrixRepository,
    private val canonicalCategoryRepository: CanonicalCategoryRepository
) : EmptyTest() {

    @Test
    fun deleteByUpdatedAtBefore() {
        val now = LocalDateTime.now()
        val notDeleted = resupplyQualityMatrixRepository.save(ResupplyQualityMatrixEntity().apply {
            category = canonicalCategoryRepository.save(CanonicalCategoryEntity().apply {
                id = 10
                name = "name"
            })
            updatedAt = now
        })
        resupplyQualityMatrixRepository.save(ResupplyQualityMatrixEntity().apply {
            category = canonicalCategoryRepository.save(CanonicalCategoryEntity().apply {
                id = 20
                name = "name2"
            })
            updatedAt = now.minusDays(10)
        })
        flushAndClear()
        resupplyQualityMatrixRepository.deleteByUpdatedAtBefore(now.minusDays(1))
        flushAndClear()
        assertThat(resupplyQualityMatrixRepository.findAll())
            .extracting<Long> { it.id }
            .containsExactly(notDeleted.id)
    }
}
