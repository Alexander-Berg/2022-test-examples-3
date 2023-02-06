package ru.yandex.market.wms.replenishment.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.model.enums.RotationType
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.entity.LotLocIdKey
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.replenishment.dao.PromoReplenishmentDao
import ru.yandex.market.wms.replenishment.dto.LotLocIdSupply
import ru.yandex.market.wms.replenishment.dto.SkuDemand
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.Instant

internal class PromoReplenishmentServiceTest : IntegrationTest() {

    @Autowired
    private lateinit var promoReplenishmentDao: PromoReplenishmentDao

    @Autowired
    private lateinit var promoReplenishmentService: PromoReplenishmentService

    private val skuId101 = SkuId.of("STORER1", "SKU101")
    private val skuId102 = SkuId.of("STORER1", "SKU102")
    private val skuId103 = SkuId.of("STORER1", "SKU103")
    private val skuId104 = SkuId.of("STORER1", "SKU104")

    /**
     * есть три заказа в -1 статусе, которые проходят по минимальным требованиям на спуск палеты,
     * и три разных палеты с соответствующими товарами, на которые будет создано 3 задания на спуск,
     * и есть 1 заказ который не проходит
     */
    @Test
    @DatabaseSetup("/service/promo/3-tasks-3-containers/before.xml")
    @ExpectedDatabase(
        value = "/service/promo/3-tasks-3-containers/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create 3 tasks for 3 container`() {
        val demands = promoReplenishmentDao.getPromoDemands()
        assertThat(demands).containsExactlyInAnyOrder(
            demand(skuId101, 7),
            demand(skuId103, 2),
            demand(skuId104, 5),
        )

        val supplies = promoReplenishmentDao.getPromoSupplies(demands.map { it.skuId })
        assertThat(supplies).containsExactlyInAnyOrder(
            supplyWithoutShelfLife(skuId101, "HRAN1", "PLT1", 12),
            supplyWithoutShelfLife(skuId103, "HRAN3", "PLT3", 14),
            supplyWithoutShelfLife(skuId104, "HRAN4", "PLT4", 15),
        )

        promoReplenishmentService.createPromoReplenishmentTasks()
    }

    /**
     * есть 2 заказа в -1 статусе, которые проходят по минимальным требованиям на спуск палеты,
     * и одна палета с двумя НЗН с соответствующими товарами,
     * в итоге будет создано одно задание на один из НЗН (не два задания на разные НЗН)
     */
    @Test
    @DatabaseSetup("/service/promo/1-task-2-containers/before.xml")
    @ExpectedDatabase(
        value = "/service/promo/1-task-2-containers/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create 1 tasks for 2 containers in the same loc`() {
        val demands = promoReplenishmentDao.getPromoDemands()
        assertThat(demands).containsExactlyInAnyOrder(
            demand(skuId101, 7),
            demand(skuId103, 2),
        )

        val supplies = promoReplenishmentDao.getPromoSupplies(demands.map { it.skuId })
        assertThat(supplies).containsExactlyInAnyOrder(
            supplyWithoutShelfLife(skuId101, "HRAN1", "PLT1", 12),
            supplyWithoutShelfLife(skuId103, "HRAN1", "PLT3", 14),
        )

        promoReplenishmentService.createPromoReplenishmentTasks()
    }

    /**
     * есть несколько заказов в -1 статусе, с одним и тем же товаром с СГ
     * и несколько разных палеты с соответствующими товарами.
     * Должна быть выбрана палета с минимальным СГ, но так как на ней товаров не хватит - будет несколько заданий
     */
    @Test
    @DatabaseSetup("/service/promo/N-tasks-N-containers-shelflife/before.xml")
    @ExpectedDatabase(
        value = "/service/promo/N-tasks-N-containers-shelflife/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create N tasks for N container with shelflife`() {
        val demands = promoReplenishmentDao.getPromoDemands()
        assertThat(demands).containsExactlyInAnyOrder(
            demand(skuId101, 21),
        )

        val supplies = promoReplenishmentDao.getPromoSupplies(demands.map { it.skuId })
        assertThat(supplies).containsExactlyInAnyOrder(
            supplyWithShelfLife(skuId101, "LOT1","HRAN1", "PLT1", 10, "2022-01-01T00:00:00Z"),
            supplyWithShelfLife(skuId101, "LOT2","HRAN2", "PLT2", 10, "2021-01-01T00:00:00Z"),
            supplyWithShelfLife(skuId101, "LOT3","HRAN3", "PLT3", 10, "2021-10-01T00:00:00Z"),
            supplyWithShelfLife(skuId101, "LOT4","HRAN4", "PLT4", 10, "2022-10-01T00:00:00Z"),
            supplyWithShelfLife(skuId101, "LOT5","HRAN5", "PLT5", 10, "2021-05-01T00:00:00Z"),
        )

        promoReplenishmentService.createPromoReplenishmentTasks()
    }

    /**
     * есть несколько заказов в -1 статусе, с одним и тем же товаром без СГ
     * и несколько разных палет с соответствующими товарами.
     * Сначала должны утилизироваться палеты с наименьшим количеством товаров.
     */
    @Test
    @DatabaseSetup("/service/promo/N-tasks-N-containers-no-shelflife/before.xml")
    @ExpectedDatabase(
        value = "/service/promo/N-tasks-N-containers-no-shelflife/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create N tasks for N container without shelflife`() {
        val demands = promoReplenishmentDao.getPromoDemands()
        assertThat(demands).containsExactlyInAnyOrder(
            demand(skuId101, 21),
        )

        val supplies = promoReplenishmentDao.getPromoSupplies(demands.map { it.skuId })
        assertThat(supplies).containsExactlyInAnyOrder(
            supplyWithoutShelfLife(skuId101, "HRAN1", "PLT1", 13),
            supplyWithoutShelfLife(skuId101, "HRAN2", "PLT2", 100),
            supplyWithoutShelfLife(skuId101, "HRAN3", "PLT3", 1),
            supplyWithoutShelfLife(skuId101, "HRAN4", "PLT4", 8),
            supplyWithoutShelfLife(skuId101, "HRAN5", "PLT5", 6),
        )

        promoReplenishmentService.createPromoReplenishmentTasks()
    }

    private fun demand(skuId: SkuId, qty: Long) =
        SkuDemand(
            skuId = skuId,
            shippedQtyPerDay = ZERO,
            pickQty = ZERO,
            storageQty = ZERO,
            plannedQty = ZERO,
            currentTasksQty = ZERO,
            replenishmentQty = BigDecimal("$qty.00000")
        )

    private fun supplyWithoutShelfLife(skuId: SkuId, loc: String, id: String, qty: Int) =
        LotLocIdSupply(
            skuId = skuId,
            lotLocIdKey = LotLocIdKey("", loc, id),
            areaKey = "AHRAN",
            hasShelfLife = false,
            rotationType = RotationType.BY_LOT,
            qty = BigDecimal("$qty.00000"),
        )

    private fun supplyWithShelfLife(skuId: SkuId, lot: String, loc: String, id: String, qty: Int, expDate: String) =
        LotLocIdSupply(
            skuId = skuId,
            lotLocIdKey = LotLocIdKey(lot, loc, id),
            areaKey = "AHRAN",
            hasShelfLife = true,
            rotationType = RotationType.BY_EXPIRATION_DATE,
            qty = BigDecimal("$qty.00000"),
            expirationDate = Instant.parse(expDate),
        )
}
