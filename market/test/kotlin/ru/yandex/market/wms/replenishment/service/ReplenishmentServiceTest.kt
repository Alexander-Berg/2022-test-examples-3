package ru.yandex.market.wms.replenishment.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.replenishment.dto.ReplenishmentSettings
import ru.yandex.market.wms.replenishment.dto.SkuDemand
import java.math.BigDecimal
import java.time.Instant

class ReplenishmentServiceTest : IntegrationTest() {
    @Autowired
    private val replenishmentService: ReplenishmentService? = null

    @Test
    @DatabaseSetup(
        "/service/get-demands/common.xml",
        "/service/get-demands/initial-state1.xml"
    )
    fun `qty for 11 days before and 3 days ahead`() {
        val result = replenishmentService!!.getDemands(
            ReplenishmentSettings(
                previousDays = 11,
                aheadDays = 3,
                minPickLocQtyPercent = 40,
            )
        )
        Assertions.assertThat(result).containsExactlyInAnyOrder(
            SkuDemand(
                skuId = SKU_ID_101,
                shippedQtyPerDay = BigDecimal("10.00000"),
                pickQty = BigDecimal("11.00000"),
                storageQty = BigDecimal("10.00000"),
                plannedQty = BigDecimal("7.00000"),
                currentTasksQty = BigDecimal.ZERO,
                shipDate = Instant.parse("2020-04-02T21:00:00Z"),
                replenishmentQty = BigDecimal("26"),
                minStorageLocQtyRemainder = BigDecimal("1.85000"),
            ),
            SkuDemand(
                skuId = SKU_ID_102,
                shippedQtyPerDay = BigDecimal("2.33333"),
                pickQty = BigDecimal.ZERO,
                storageQty = BigDecimal("4.00000"),
                plannedQty = BigDecimal.ZERO,
                currentTasksQty = BigDecimal.ZERO,
                shipDate = null,
                replenishmentQty = BigDecimal("7"),
                minStorageLocQtyRemainder = BigDecimal("0.35000")
            )
        )
    }

    @Test
    @DatabaseSetup(
        "/service/get-demands/common.xml",
        "/service/get-demands/initial-state2.xml"
    )
    fun `qty for 5 days before and 5 days ahead`() {
        val result = replenishmentService!!.getDemands(
            ReplenishmentSettings(
                previousDays = 5,
                aheadDays = 5,
                minPickLocQtyPercent = 40,
            )
        )
        Assertions.assertThat(result).containsExactlyInAnyOrder(
            SkuDemand(
                skuId = SKU_ID_101,
                shippedQtyPerDay = BigDecimal("5.00000"),
                pickQty = BigDecimal("11.00000"),
                storageQty = BigDecimal("10.00000"),
                plannedQty = BigDecimal("14.00000"),
                currentTasksQty = BigDecimal.ZERO,
                shipDate = Instant.parse("2020-04-02T21:00:00Z"),
                replenishmentQty = BigDecimal("28"),
                minStorageLocQtyRemainder = BigDecimal("1.95000"),
            )
        )
    }

    @Test
    @DatabaseSetup(
        "/service/get-demands/common.xml",
        "/service/get-demands/initial-state3.xml"
    )
    fun `qty for 11 days before and 1 days ahead`() {
        val result = replenishmentService!!.getDemands(
            ReplenishmentSettings(
                previousDays = 11,
                aheadDays = 1,
                minPickLocQtyPercent = 40,
            )
        )
        Assertions.assertThat(result).containsExactlyInAnyOrder(
            SkuDemand(
                skuId = SKU_ID_102,
                shippedQtyPerDay = BigDecimal("2.33333"),
                pickQty = BigDecimal.ZERO,
                storageQty = BigDecimal("4.00000"),
                plannedQty = BigDecimal.ZERO,
                currentTasksQty = BigDecimal.ZERO,
                shipDate = null,
                replenishmentQty = BigDecimal("3"),
                minStorageLocQtyRemainder = BigDecimal("0.11667"),
            )
        )
    }

    @Test
    @DatabaseSetup(
        "/service/get-demands/common.xml",
        "/service/get-demands/initial-state4.xml"
    )
    fun `qty for 0 days before and 5 days ahead`() {
        val result = replenishmentService!!.getDemands(
            ReplenishmentSettings(
                previousDays = 0,
                aheadDays = 5,
                minPickLocQtyPercent = 40,
            )
        )
        Assertions.assertThat(result).containsExactlyInAnyOrder(
            SkuDemand(
                skuId = SKU_ID_101,
                shippedQtyPerDay = BigDecimal("0.00000"),
                pickQty = BigDecimal("11.00000"),
                storageQty = BigDecimal("10.00000"),
                plannedQty = BigDecimal("14.00000"),
                currentTasksQty = BigDecimal.ZERO,
                shipDate = Instant.parse("2020-04-02T21:00:00Z"),
                replenishmentQty = BigDecimal("3"),
                minStorageLocQtyRemainder = BigDecimal("0.70000"),
            )
        )
    }

    @Test
    @DatabaseSetup("/service/delete-unassigned-tasks/before.xml")
    @ExpectedDatabase(
        value = "/service/delete-unassigned-tasks/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `delete unassigned tasks`() {
        replenishmentService!!.deleteUnassignedTasks()
    }

    companion object {
        private val SKU_ID_101 = SkuId.of("STORER1", "SKU101")
        private val SKU_ID_102 = SkuId.of("STORER2", "SKU102")
        private val SKU_ID_103 = SkuId.of("STORER2", "SKU103")
    }
}
