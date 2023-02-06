package ru.yandex.market.logistics.mqm.repository

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest

class PlanFactRepositoryTest : AbstractContextualTest() {

    @Autowired
    private lateinit var planFactRepository: PlanFactRepository

    @Test
    @DatabaseSetup("/repository/planfact/before/find_all_by_group_id.xml")
    fun findAllByPlanFactGroupId() {
        val result = planFactRepository.findAllByPlanFactGroupId(1)

        assertSoftly {
            result.size shouldBe 3
            result.map { it.id } shouldContainExactly listOf(1L, 3L, 4L)
        }
    }
}
