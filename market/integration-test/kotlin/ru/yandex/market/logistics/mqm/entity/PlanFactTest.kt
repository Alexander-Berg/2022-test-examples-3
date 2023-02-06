package ru.yandex.market.logistics.mqm.entity

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.repository.PlanFactRepository
import java.time.Instant

internal class PlanFactTest: AbstractContextualTest() {
    @Autowired
    private lateinit var planFactRepository: PlanFactRepository

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @DisplayName("Помечание план-факта как неактуального удаляет его из группы")
    @Test
    @DatabaseSetup("/entity/PlanFact/notActualRemovesFromGroup.xml")
    fun notActualRemovesFromGroup() {
        transactionTemplate.executeWithoutResult {
            val planFact = planFactRepository.findById(1).get()
            planFact.planFactGroups.size shouldBe 1
            planFact.markNotActual(Instant.now())
            planFact.planFactGroups.size shouldBe 0
        }
        transactionTemplate.executeWithoutResult {
            val planFact = planFactRepository.findById(1).get()
            planFact.planFactGroups.size shouldBe 0
        }
    }
}
