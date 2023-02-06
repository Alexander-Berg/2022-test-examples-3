package ru.yandex.market.wms.replenishment.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.model.enums.ReplenishmentType
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.entity.replenishment.ProblemOrderDetail
import ru.yandex.market.wms.common.spring.enums.replenishment.ProblemStatus
import ru.yandex.market.wms.common.spring.enums.replenishment.ProblemType

class ProblemOrderServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var service: ProblemOrderService

    @Test
    @ExpectedDatabase(
        value = "/service/order/new-problems/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `add problems test`() {
        val details = mutableListOf(
            buildProblem("01", "01", "SKU1", "STORER1", 1, ProblemType.OUT_OF_PICKING_STOCK),
            buildProblem("01", "01", "SKU1", "STORER1", 10, ProblemType.OUT_OF_PICKING_STOCK),
            buildProblem("02", "01", "SKU1", "STORER1", 2, ProblemType.OUT_OF_PICKING_STOCK),
            buildProblem("02", "01", "SKU1", "STORER1", 3, ProblemType.OUT_OF_PICKING_STOCK),
            buildProblem("02", "01", "SKU1", "STORER1", 8, ProblemType.OUT_OF_PICKING_STOCK),
        )
        val added = service.addNewProblems(details)
        assertions.assertThat(added).isEqualTo(2)


        val details2 = listOf(
            buildProblem("03", "01", "SKU2", "STORER2", 1, ProblemType.SHORT)
        )
        val added2 = service.addNewProblems(details2)
        assertions.assertThat(added2).isEqualTo(1)
    }

    @Test
    @DatabaseSetup(value = ["/service/order/new-problems/1/after.xml"])
    fun `add same problem test`() {
        val problems = listOf(
            buildProblem("01", "01", "SKU1", "STORER1", 1, ProblemType.OUT_OF_PICKING_STOCK),
            buildProblem("02", "01", "SKU1", "STORER1", 2, ProblemType.OUT_OF_PICKING_STOCK),
            buildProblem("03", "01", "SKU2", "STORER2", 1, ProblemType.SHORT),
        )
        val added = service.addNewProblems(problems)
        assertions.assertThat(added).isEqualTo(0)
    }

    @Test
    @DatabaseSetup(value = ["/service/order/new-problems/1/after.xml"])
    fun `add same problem test another qty`() {
        val problems = ArrayList<ProblemOrderDetail>()
        problems.add(buildProblem("01", "01", "SKU1", "STORER1", 2, ProblemType.OUT_OF_PICKING_STOCK))
        problems.add(buildProblem("02", "01", "SKU1", "STORER1", 1, ProblemType.OUT_OF_PICKING_STOCK))
        problems.add(buildProblem("03", "01", "SKU2", "STORER2", 2, ProblemType.SHORT))
        val added = service.addNewProblems(problems)
        assertions.assertThat(added).isEqualTo(3)
    }

    @Test
    @DatabaseSetup(value = ["/service/order/new-problems/2/before.xml"])
    @ExpectedDatabase(
        value = "/service/order/new-problems/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `add same problem test status update`() {
        val problems = ArrayList<ProblemOrderDetail>()
        problems.add(buildProblem("01", "01", "SKU1", "STORER1", 2, ProblemType.OUT_OF_PICKING_STOCK))
        problems.add(buildProblem("02", "01", "SKU1", "STORER1", 1, ProblemType.OUT_OF_PICKING_STOCK))
        problems.add(buildProblem("03", "01", "SKU2", "STORER2", 2, ProblemType.SHORT))
        val added = service.addNewProblems(problems)
        assertions.assertThat(added).isEqualTo(3)
    }

    @Test
    @DatabaseSetup(value = ["/service/order/new-problems/3/before.xml"])
    @ExpectedDatabase(
        value = "/service/order/new-problems/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `add same problem double short`() {
        val problems = ArrayList<ProblemOrderDetail>()
        problems.add(buildProblem("01", "01", "SKU01", "STORER1", 30, ProblemType.SHORT))
        val added = service.addNewProblems(problems)
        assertions.assertThat(added).isEqualTo(1)
    }

    @Test
    @DatabaseSetup(value = ["/service/order/new-problems/4/before.xml"])
    @ExpectedDatabase(
        value = "/service/order/new-problems/4/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `add same problem several details`() {
        val problems = ArrayList<ProblemOrderDetail>()
        problems.add(buildProblem("01", "01", "SKU01", "STORER1", 10, ProblemType.SHORT))
        problems.add(buildProblem("01", "02", "SKU02", "STORER1", 10, ProblemType.OUT_OF_PICKING_STOCK))
        problems.add(buildProblem("02", "01", "SKU01", "STORER1", 20, ProblemType.OUT_OF_PICKING_STOCK))
        problems.add(buildProblem("02", "02", "SKU02", "STORER1", 20, ProblemType.OUT_OF_PICKING_STOCK))
        val added = service.addNewProblems(problems)
        assertions.assertThat(added).isEqualTo(3)
    }

    @Test
    @DatabaseSetup(value = ["/service/order/new-problems/5/before.xml"])
    @ExpectedDatabase(
        value = "/service/order/new-problems/5/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `add same problems time diff test`() {
        val problems = ArrayList<ProblemOrderDetail>()
        problems.add(buildProblem("01", "01", "SKU01", "STORER1", 10, ProblemType.SHORT))
        problems.add(buildProblem("01", "02", "SKU02", "STORER1", 10, ProblemType.OUT_OF_PICKING_STOCK))
        problems.add(buildProblem("02", "01", "SKU01", "STORER1", 20, ProblemType.OUT_OF_PICKING_STOCK))
        problems.add(buildProblem("02", "02", "SKU02", "STORER1", 20, ProblemType.OUT_OF_PICKING_STOCK))
        val added = service.addNewProblems(problems)
        assertions.assertThat(added).isEqualTo(2)
    }

    @Test
    @DatabaseSetup(value = ["/service/order/new-problems/6/before.xml"])
    @ExpectedDatabase(
        value = "/service/order/new-problems/6/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `add same problem several existing should update test`() {
        val problems = ArrayList<ProblemOrderDetail>()
        problems.add(buildProblem("01", "01", "SKU01", "STORER1", 10, ProblemType.OUT_OF_PICKING_STOCK))
        val added = service.addNewProblems(problems)
        assertions.assertThat(added).isEqualTo(1)
    }

    @Test
    @DatabaseSetup(value = ["/service/order/new-problems/7/before.xml"])
    @ExpectedDatabase(
        value = "/service/order/new-problems/7/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `add same problem several existing should not update test`() {
        val problems = ArrayList<ProblemOrderDetail>()
        problems.add(buildProblem("01", "01", "SKU01", "STORER1", 10, ProblemType.OUT_OF_PICKING_STOCK))
        val added = service.addNewProblems(problems)
        assertions.assertThat(added).isEqualTo(0)
    }

    @Test
    @DatabaseSetup("/service/order/new-problems/8/before.xml")
    fun `duplicates test`() {
        val problems = ArrayList<ProblemOrderDetail>()
        for (i in 0..99) {
            problems.add(
                buildProblem(
                    i.toString() + "01",
                    "01",
                    i.toString() + "SKU01",
                    "STORER1",
                    10,
                    ProblemType.OUT_OF_PICKING_STOCK
                )
            )
            problems.add(
                buildProblem(
                    i.toString() + "01",
                    "01",
                    "SKU01",
                    "STORER1",
                    10,
                    ProblemType.OUT_OF_PICKING_STOCK
                )
            )
            problems.add(
                buildProblem(
                    i.toString() + "01",
                    "01",
                    i.toString() + "SKU02",
                    "STORER1",
                    10,
                    ProblemType.OUT_OF_PICKING_STOCK
                )
            )
            problems.add(
                buildProblem(
                    i.toString() + "01",
                    "01",
                    "SKU02",
                    "STORER1",
                    10,
                    ProblemType.OUT_OF_PICKING_STOCK
                )
            )
        }
        val added = service.addNewProblems(problems)
        val savedProblems = service.getProblemOrderDetails(ProblemStatus.NEW, ReplenishmentType.ORDER)
        assertions.assertThat(savedProblems.size).isEqualTo(100)
        assertions.assertThat(added).isEqualTo(100)
    }

    @Test
    fun `empty test`() {
        service.addNewProblems(emptyList())
    }

    private fun buildProblem(
        order: String, line: String, sku: String, storer: String, qty: Int,
        type: ProblemType
    ): ProblemOrderDetail = ProblemOrderDetail
        .builder()
        .orderKey(order)
        .orderLineNumber(line)
        .sku(sku)
        .storerKey(storer)
        .qty(qty)
        .type(type)
        .status(ProblemStatus.NEW)
        .build()
}
