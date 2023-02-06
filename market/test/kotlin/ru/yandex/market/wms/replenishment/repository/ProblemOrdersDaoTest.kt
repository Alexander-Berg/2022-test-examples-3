package ru.yandex.market.wms.replenishment.repository

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.model.enums.OrderType
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.entity.replenishment.ProblemOrderDetail
import ru.yandex.market.wms.common.spring.enums.replenishment.ProblemStatus
import ru.yandex.market.wms.common.spring.enums.replenishment.ProblemType
import ru.yandex.market.wms.replenishment.dao.ProblemOrdersDao
import java.time.Clock

class ProblemOrdersDaoTest : IntegrationTest() {
    @Autowired
    private lateinit var dao: ProblemOrdersDao

    @Autowired
    private lateinit var clock: Clock

    @Test
    @DatabaseSetup("/db/dao/problemOrders/before-insert.xml")
    @ExpectedDatabase(
        value = "/db/dao/problemOrders/after-insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun insertTest() {
        val problemOrderDetails: MutableList<ProblemOrderDetail> = ArrayList()
        problemOrderDetails.add(
            ProblemOrderDetail.builder()
                .orderKey("00001")
                .orderLineNumber("001")
                .sku("SKU1")
                .storerKey("STORER1")
                .qty(3)
                .type(ProblemType.OUT_OF_PICKING_STOCK)
                .build()
        )
        problemOrderDetails.add(
            ProblemOrderDetail.builder()
                .orderKey("00002")
                .orderLineNumber("001")
                .sku("SKU2")
                .storerKey("STORER1")
                .qty(1)
                .type(ProblemType.SHORT)
                .build()
        )
        dao.insertNewProblemDetails(problemOrderDetails, "TEST", clock.instant())
    }

    @DatabaseSetup("/db/dao/problemOrders/after-insert.xml")
    @Test
    fun test() {
        var problems = dao.getProblemDetails(ProblemStatus.NEW, setOf(OrderType.STANDARD))
        assertions.assertThat(problems.size).isOne
        assertions.assertThat(problems[0].orderKey).isEqualTo("00001")
        problems = dao.getProblemDetails(ProblemStatus.NEW, OrderType.WITHDRAWALS)
        assertions.assertThat(problems.size).isOne
        assertions.assertThat(problems[0].orderKey).isEqualTo("00002")
    }

    @Test
    fun insertEmptyTest() {
        dao.insertNewProblemDetails(emptyList(), "", clock.instant())
    }
}
