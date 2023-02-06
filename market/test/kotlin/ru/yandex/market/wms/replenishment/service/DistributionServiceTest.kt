package ru.yandex.market.wms.replenishment.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.wms.common.model.enums.RotationType
import ru.yandex.market.wms.common.spring.dao.entity.LotLocIdKey
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.common.spring.dao.entity.Storer
import ru.yandex.market.wms.replenishment.dto.LotLocIdSupply
import ru.yandex.market.wms.replenishment.dto.ReplenishmentSettings
import ru.yandex.market.wms.replenishment.dto.SkuDemand
import ru.yandex.market.wms.replenishment.dto.SupplyTask
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal class DistributionServiceTest {
    private val distributionService = DistributionService()

    @Test
    fun `distribute supplies empty`() {
        val tasks = distributionService.distributeSupplies(emptyList(), emptyList(), ReplenishmentSettings.EMPTY)
        Assertions.assertThat(tasks).isEmpty()
    }

    /**
     * Только товары с СГ.
     * Создаются задания на те товары, которые раньше всего протухнут, вне зависимости от ячейки.
     */
    @Test
    fun `distribute supplies with shelf life`() {
        val demand = listOf(
            demand(SKU_ID_11, 10),  // хватает ровно
            demand(SKU_ID_12, 10),  // не хватает
            demand(SKU_ID_21, 10),  // хватает с избытком
            demand(SKU_ID_22, 1) // на складе отсутствует
        )
        val supply = listOf(
            supply(SKU_ID_11, "LOC1", "ID10", 2, "2030-05-20"),
            supply(SKU_ID_11, "LOC1", "ID10", 5, "2030-05-10"),
            supply(SKU_ID_11, "LOC2", "ID20", 3, "2030-05-15"),
            supply(SKU_ID_12, "LOC2", "ID21", 3, "2030-06-11"),
            supply(SKU_ID_12, "LOC3", "ID30", 4, "2030-05-11"),
            supply(SKU_ID_12, "LOC1", "ID11", 1, "2030-04-11"),
            supply(SKU_ID_21, "LOC1", "ID10", 8, "2030-04-10"),
            supply(SKU_ID_21, "LOC2", "ID20", 8, "2030-04-09"),
            supply(SKU_ID_21, "LOC2", "ID20", 8, "2030-05-10"),
            supply(SKU_ID_23, "LOC1", "ID10", 5, "2030-05-10") // такое вообще не надо
        )
        val expectedTasks = listOf(
            task(SKU_ID_11, "LOC1", "ID10", 2, "2030-05-20"),
            task(SKU_ID_11, "LOC1", "ID10", 5, "2030-05-10"),
            task(SKU_ID_11, "LOC2", "ID20", 3, "2030-05-15"),
            task(SKU_ID_12, "LOC2", "ID21", 3, "2030-06-11"),
            task(SKU_ID_12, "LOC3", "ID30", 4, "2030-05-11"),
            task(SKU_ID_12, "LOC1", "ID11", 1, "2030-04-11"),
            task(SKU_ID_21, "LOC1", "ID10", 2, "2030-04-10"),
            task(SKU_ID_21, "LOC2", "ID20", 8, "2030-04-09")
        )
        val tasks = distributionService.distributeSupplies(demand, supply, ReplenishmentSettings.EMPTY)
        Assertions.assertThat(tasks).containsExactlyInAnyOrderElementsOf(expectedTasks)
    }

    /**
     * Создается задание для товара с СГ.
     * Создаются задания для товаров без СГ, которые лежат в той же ячейке.
     */
    @Test
    fun `distribute supplies with shelf life and without from the same locs`() {
        val demand = listOf(
            demand(SKU_ID_11, 10),  // с СГ
            demand(SKU_ID_21, 10),  // без СГ
            demand(SKU_ID_22, 10) // без СГ
        )
        val supply = listOf(
            supply(SKU_ID_11, "LOC1", "ID10", 8, "2030-06-11"),
            supply(SKU_ID_11, "LOC2", "ID20", 8, "2030-06-12"),
            supply(SKU_ID_21, "LOC3", "ID31", 20),
            supply(SKU_ID_21, "LOC1", "ID11", 20),
            supply(SKU_ID_22, "LOC2", "ID22", 100),
            supply(SKU_ID_22, "LOC1", "ID12", 3),
            supply(SKU_ID_22, "LOC3", "ID32", 100)
        )
        val expectedTasks = listOf(
            task(SKU_ID_11, "LOC1", "ID10", 8, "2030-06-11"),
            task(SKU_ID_11, "LOC2", "ID20", 2, "2030-06-12"),
            task(SKU_ID_21, "LOC1", "ID11", 10),
            task(SKU_ID_22, "LOC2", "ID22", 7),
            task(SKU_ID_22, "LOC1", "ID12", 3)
        )
        val tasks = distributionService.distributeSupplies(demand, supply, ReplenishmentSettings.EMPTY)
        Assertions.assertThat(tasks).containsExactlyInAnyOrderElementsOf(expectedTasks)
    }

    /**
     * Только товары без СГ
     */
    @Test
    fun `distribute supplies without shelf life`() {
        val demand = listOf(
            demand(SKU_ID_11, 5),
            demand(SKU_ID_12, 6),
            demand(SKU_ID_21, 7),
            demand(SKU_ID_22, 8)
        )
        val supply = listOf(
            supply(SKU_ID_11, "LOC1", "ID1", 5),  // хватает как раз
            supply(SKU_ID_12, "LOC2", "ID2", 5),  // не хватает
            supply(SKU_ID_21, "LOC1", "ID1", 8),  // хватает с избытком
            supply(SKU_ID_23, "LOC1", "ID1", 5) // такое вообще не надо
        )
        val expectedTasks = listOf(
            task(SKU_ID_11, "LOC1", "ID1", 5),
            task(SKU_ID_12, "LOC2", "ID2", 5),
            task(SKU_ID_21, "LOC1", "ID1", 7)
        )
        val tasks = distributionService.distributeSupplies(demand, supply, ReplenishmentSettings.EMPTY)
        Assertions.assertThat(tasks).containsExactlyInAnyOrderElementsOf(expectedTasks)
    }

    @Test
    fun `distribute supplies from loc with many skus`() {
        val demand = listOf(
            demand(SKU_ID_11, 10),
            demand(SKU_ID_12, 10),
            demand(SKU_ID_21, 10),
            demand(SKU_ID_22, 10)
        )
        val supply = listOf(
            supply(SKU_ID_11, "LOC1", "ID1", 100),
            supply(SKU_ID_12, "LOC2", "ID2", 100),
            supply(SKU_ID_21, "LOC3", "ID3", 100),
            supply(SKU_ID_22, "LOC4", "ID4", 100),
            supply(SKU_ID_11, "LOC100", "ID101", 10),
            supply(SKU_ID_12, "LOC100", "ID101", 5),
            supply(SKU_ID_12, "LOC100", "ID102", 5),
            supply(SKU_ID_21, "LOC100", "ID103", 9)
        )
        val expectedTasks = listOf(
            task(SKU_ID_11, "LOC100", "ID101", 10),
            task(SKU_ID_12, "LOC100", "ID101", 5),
            task(SKU_ID_12, "LOC100", "ID102", 5),
            task(SKU_ID_21, "LOC100", "ID103", 9),
            task(SKU_ID_21, "LOC3", "ID3", 1),
            task(SKU_ID_22, "LOC4", "ID4", 10)
        )
        val tasks = distributionService.distributeSupplies(demand, supply, ReplenishmentSettings.EMPTY)
        Assertions.assertThat(tasks).containsExactlyInAnyOrderElementsOf(expectedTasks)
    }

    /**
     * Проверка формирования заданий на малые остатки товара
     */
    @Test
    fun `distribute supplies with remainders`() {
        val demand = listOf(
            demand(SKU_ID_11, 10, 10),
            demand(SKU_ID_12, 10, 10),
            demand(SKU_ID_21, 15)
        )
        val supply = listOf( // SKU 11 и SKU 12 берутся из одной ячейки
            supply(SKU_ID_11, "LOC1", "ID1", 10),
            supply(SKU_ID_12, "LOC1", "ID2", 12),  // из этой ячейки будет взят остаток SKU 11, поскольку он меньше
            // указанного и паллета уже спускается для SKU 21
            supply(SKU_ID_11, "LOC2", "ID3", 7),
            supply(
                SKU_ID_21,
                "LOC2",
                "ID4",
                15
            ),  // остаток из этой ячейки не будет взят, поскольку паллета не спускается
            supply(SKU_ID_11, "LOC3", "ID5", 7)
        )
        val expectedTasks = listOf(
            task(SKU_ID_11, "LOC1", "ID1", 10),
            task(SKU_ID_12, "LOC1", "ID2", 12),
            task(SKU_ID_11, "LOC2", "ID3", 7),
            task(SKU_ID_21, "LOC2", "ID4", 15)
        )
        val tasks = distributionService.distributeSupplies(demand, supply, ReplenishmentSettings.EMPTY)
        Assertions.assertThat(expectedTasks).containsExactlyInAnyOrderElementsOf(tasks)
    }

    @Test
    fun `distribute supplies for items by exp date`() {
        val demand = emptyList<SkuDemand>()
        val supply = listOf( // no, since item has too long lifetime left and no demand
            supply(
                SKU_Y_1,
                lot(SKU_Y_1),
                "LOC1",
                "ID1",
                31,
                expDateIn(1000)
            ),  // yes, since item has 40 days left to expiry
            supply(
                SKU_Y_2,
                lot(SKU_Y_2),
                "LOC2",
                "ID2",
                32,
                expDateIn(40)
            ),  // no, since item has too short lifetime left (exp in 15 days, block in 5 days, min days - 10)
            supply(
                SKU_Y_3,
                lot(SKU_Y_3),
                "LOC3",
                "ID3",
                33,
                expDateIn(15)
            ),  // no, since item has appropriate exp date, but too small qty
            supply(
                SKU_Y_4,
                lot(SKU_Y_4),
                "LOC4",
                "ID4",
                4,
                expDateIn(40)
            ),  // no, since item has appropriate exp date and qty, but not from Yandex storer key
            supply(
                SKU_ID_11,
                lot(SKU_ID_11),
                "LOC55",
                "ID5",
                35,
                expDateIn(40)
            ),  // no, since item has appropriate exp date and qty and from Yandex storer key, but wrong rotation type
            supply(SKU_ID_11, lot(SKU_ID_11), "LOC55", "ID5", 36, expDateIn(40), RotationType.BY_LOT)
        )
        val expectedTasks = listOf(
            task(SKU_Y_2, "LOC2", "ID2", 32, true)
        )
        val tasks = distributionService.distributeSupplies(demand, supply, testReplenishmentSettings())
        Assertions.assertThat(expectedTasks).containsExactlyInAnyOrderElementsOf(tasks)
    }

    private fun testReplenishmentSettings(): ReplenishmentSettings {
        return ReplenishmentSettings(
            generateByExpDate = true,
            minDaysToExpiry = 10,
            useDaysToExpiry = 60,
            minQtyToPickByExpiry = BigDecimal.TEN
        )
    }

    private fun expDateIn(days: Int): String {
        return DATE_TIME_FORMATTER.format(Instant.now().plus(days.toLong(), ChronoUnit.DAYS))
    }

    companion object {
        val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
        private const val AREA_KEY = "AREA_51"
        private val SKU_ID_11 = SkuId.of("STORER1", "SKU11")
        private val SKU_ID_12 = SkuId.of("STORER1", "SKU12")
        private val SKU_ID_21 = SkuId.of("STORER2", "SKU21")
        private val SKU_ID_22 = SkuId.of("STORER2", "SKU22")
        private val SKU_ID_23 = SkuId.of("STORER2", "SKU23")
        private val SKU_Y_1 = SkuId.of(Storer.YANDEX_STORERKEY, "SKU_Y1")
        private val SKU_Y_2 = SkuId.of(Storer.YANDEX_STORERKEY, "SKU_Y2")
        private val SKU_Y_3 = SkuId.of(Storer.YANDEX_STORERKEY, "SKU_Y3")
        private val SKU_Y_4 = SkuId.of(Storer.YANDEX_STORERKEY, "SKU_Y4")
        private fun supply(skuId: SkuId, loc: String, id: String, qty: Int): LotLocIdSupply {
            return supply(skuId, lot(skuId), loc, id, qty, null)
        }

        private fun task(skuId: SkuId, loc: String, id: String, qty: Int): SupplyTask {
            return task(skuId, lot(skuId), loc, id, qty)
        }

        private fun task(
            skuId: SkuId, loc: String, id: String, qty: Int, isReplenishmentByExpiryDate: Boolean
        ): SupplyTask = task(skuId, lot(skuId), loc, id, qty, isReplenishmentByExpiryDate)

        private fun supply(skuId: SkuId, loc: String, id: String, qty: Int, expirationDate: String): LotLocIdSupply {
            return supply(skuId, lot(skuId, expirationDate), loc, id, qty, expirationDate)
        }

        private fun task(skuId: SkuId, loc: String, id: String, qty: Int, expirationDate: String): SupplyTask {
            return task(skuId, lot(skuId, expirationDate), loc, id, qty)
        }

        private fun supply(
            skuId: SkuId, lot: String, loc: String, id: String, qty: Int,
            expirationDate: String?, rotationType: RotationType = RotationType.BY_EXPIRATION_DATE
        ): LotLocIdSupply {
            return LotLocIdSupply(
                skuId = skuId,
                lotLocIdKey = LotLocIdKey(lot, loc, id),
                areaKey = AREA_KEY,
                qty = BigDecimal.valueOf(qty.toLong()),
                hasShelfLife = expirationDate != null,
                expirationDate = expirationDate?.let { Instant.parse(expirationDate + "T11:00:00Z") },
                daysBeforeBlockByExpiry = 10,
                rotationType = rotationType,
            )
        }

        private fun demand(skuId: SkuId, replenishmentQty: Int): SkuDemand {
            return SkuDemand(
                skuId = skuId,
                replenishmentQty = BigDecimal.valueOf(replenishmentQty.toLong()),
                minStorageLocQtyRemainder = BigDecimal.ZERO,
                currentTasksQty = BigDecimal.ZERO,
                pickQty = BigDecimal.ZERO,
                plannedQty = BigDecimal.ZERO,
                shippedQtyPerDay = BigDecimal.ZERO,
                storageQty = BigDecimal.ZERO,
            )
        }

        private fun demand(skuId: SkuId, replenishmentQty: Int, remainder: Int): SkuDemand {
            return SkuDemand(
                skuId = skuId,
                replenishmentQty = BigDecimal.valueOf(replenishmentQty.toLong()),
                minStorageLocQtyRemainder = BigDecimal.valueOf(remainder.toLong()),
                storageQty = BigDecimal.ZERO,
                shippedQtyPerDay = BigDecimal.ZERO,
                plannedQty = BigDecimal.ZERO,
                pickQty = BigDecimal.ZERO,
                currentTasksQty = BigDecimal.ZERO,
            )
        }

        private fun task(
            skuId: SkuId, lot: String, loc: String, id: String, qty: Int,
            isReplenishmentByExpiryDate: Boolean = false
        ): SupplyTask {
            return SupplyTask(
                skuId, LotLocIdKey(lot, loc, id), AREA_KEY, BigDecimal.valueOf(qty.toLong()), false,
                isReplenishmentByExpiryDate
            )
        }

        private fun lot(skuId: SkuId): String {
            return "LOT_" + skuId.sku
        }

        private fun lot(skuId: SkuId, expirationDate: String): String {
            return "LOT_" + skuId.sku + "_" + expirationDate
        }
    }
}
