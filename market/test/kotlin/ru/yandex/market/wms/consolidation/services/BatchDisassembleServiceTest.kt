package ru.yandex.market.wms.consolidation.services

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.PickDetailDao
import ru.yandex.market.wms.consolidation.modules.preconsolidation.service.BatchDisassembleService

class BatchDisassembleServiceTest(
    private @Autowired val service: BatchDisassembleService,
    private @Autowired val pickDetailsDao: PickDetailDao,
    private @Autowired val orderDetailDao: OrderDetailDao
) : IntegrationTest() {

    @Test
    @DatabaseSetup("/batch-disassemble/before/single-nonsort-order.xml")
    @ExpectedDatabase("/batch-disassemble/after/single-nonsort-order.xml", assertionMode = NON_STRICT_UNORDERED)
    fun oneOrderWithNonSortItem() {
        assertThat(service.tryDisassembleBatchForNonSort("CART001")).isTrue;
    }

    @Test
    @DatabaseSetup("/batch-disassemble/before/single-simple-order.xml")
    @ExpectedDatabase("/batch-disassemble/before/single-simple-order.xml", assertionMode = NON_STRICT_UNORDERED)
    fun oneOrderWithSimpleItem() {
        assertThat(service.tryDisassembleBatchForNonSort("CART001")).isFalse;
    }

    @Test
    @DatabaseSetup("/batch-disassemble/before/several-orders.xml")
    @ExpectedDatabase("/batch-disassemble/after/several-orders.xml", assertionMode = NON_STRICT_UNORDERED)
    fun severalOrders() {
        assertThat(service.tryDisassembleBatchForNonSort("CART001")).isFalse;
        assertThat(service.tryDisassembleBatchForNonSort("CART002")).isFalse;
        assertThat(service.tryDisassembleBatchForNonSort("CART003")).isTrue;
        assertThat(service.tryDisassembleBatchForNonSort("CART004")).isTrue;
    }

    @Test
    @DatabaseSetup("/batch-disassemble/after/several-orders.xml")
    @ExpectedDatabase("/batch-disassemble/after/several-orders.xml", assertionMode = NON_STRICT_UNORDERED)
    fun doubleBatchDisassembleCheck() {
        assertThat(service.tryDisassembleBatchForNonSort("CART003")).isFalse;
        assertThat(service.tryDisassembleBatchForNonSort("CART004")).isFalse;
    }

    @Test
    @DatabaseSetup("/batch-disassemble/before/several-orders-nonsort-only.xml")
    @ExpectedDatabase("/batch-disassemble/after/several-orders-nonsort-only.xml", assertionMode = NON_STRICT_UNORDERED)
    fun severalOrdersNonSortOnly() {
        assertThat(service.tryDisassembleBatchForNonSort("CART001")).isTrue;
        assertThat(service.tryDisassembleBatchForNonSort("CART002")).isTrue;
        assertThat(service.tryDisassembleBatchForNonSort("CART003")).isTrue;
        assertThat(service.tryDisassembleBatchForNonSort("CART004")).isTrue;
        assertThat(service.tryDisassembleBatchForNonSort("CART005")).isTrue;

        val picks = pickDetailsDao.getByWaveKeyOnly("WAVE001")
        assertThat(picks.map { it.id }).isSubsetOf("CART001", "CART002", "CART003", "CART004", "CART005")

        val orderDetails = orderDetailDao.findOrderDetailsByOrderDetailKeys(picks.map { it.orderDetailKey }.toSet())
        assertThat(picks.size).isEqualTo(orderDetails.sumOf { it.openQty.intValueExact() })

        for (detail in orderDetails) {
            val pickForDetail = picks.filter { it.orderDetailKey == detail.getOrderDetailKey() }
            assertThat(pickForDetail.size).isEqualTo(detail.openQty.intValueExact())
            assertThat(pickForDetail).allMatch { it.skuId == detail.skuId() }
        }
    }
}
