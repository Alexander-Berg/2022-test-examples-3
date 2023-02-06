package ru.yandex.market.logistics.mqm.tms.housekeeping

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.repository.PlanFactGroupRepository
import ru.yandex.market.logistics.mqm.repository.PlanFactRepository
import ru.yandex.market.logistics.mqm.utils.tskvGetByKey
import ru.yandex.market.logistics.mqm.utils.tskvGetEntity
import ru.yandex.market.logistics.mqm.utils.tskvGetExtra
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor

class RemoveNotActualPlanFactsFromGroupsTest: AbstractContextualTest() {

    @RegisterExtension
    @JvmField
    final val tskvLogCaptor = BackLogCaptor()

    @Autowired
    lateinit var executor: RemoveNotActualPlanFactsFromGroups

    @Autowired
    lateinit var transactionTemplate: TransactionOperations

    @Autowired
    lateinit var planFactRepository: PlanFactRepository

    @Autowired
    lateinit var groupRepository: PlanFactGroupRepository

    @Test
    @DisplayName("Удаление NOT ACTUAL и OUTDATED план-фактов из группы и обновление группы")
    @DatabaseSetup("/tms/RemoveNotActualPlanFactsFromGroups/setup.xml")
    @ExpectedDatabase(
        value = "/tms/RemoveNotActualPlanFactsFromGroups/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun removeNotActual() {
        executor.run()

        transactionTemplate.execute {
            groupRepository.getOne(1).scheduleTime shouldNotBe null
            groupRepository.getOne(2).scheduleTime shouldBe null
        }

        val actualMap = tskvLogCaptor.results.map { log ->
            mutableListOf<Pair<String, String?>>().apply {
                this.addAll(tskvGetExtra(log))
                this.addAll(tskvGetEntity(log))
                this.add(tskvGetByKey(log, RemoveNotActualPlanFactsFromGroups.CODE))
            }
        }

        assertSoftly {
            actualMap[0] shouldContain Pair("planFactsToFix", "1")
        }
    }
}
