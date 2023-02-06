package ru.yandex.direct.grid.processing.service.dynamiccondition

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.thymeleaf.util.StringUtils
import ru.yandex.direct.grid.model.GdEntityStats
import ru.yandex.direct.grid.model.GdGoalStats
import ru.yandex.direct.grid.model.GdOrderByParams
import ru.yandex.direct.grid.model.Order
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTarget
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetOrderBy
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetOrderByField
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetPrimaryStatus
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetStatus
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetTab
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicWebpageAdTarget
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionAutobudgetPriority
import ru.yandex.direct.grid.processing.service.dynamiccondition.DynamicAdTargetUtils.getComparator
import ru.yandex.direct.test.utils.randomPositiveLong
import java.math.BigDecimal

class DynamicAdTargetUtilsTest {

    @Test
    fun comparator_nameAndPrice() {
        val first = dynamicAdTextTarget().apply {
            name = "a"
            price = 100.toBigDecimal()
        }
        val second = dynamicAdTextTarget().apply {
            name = "b"
            price = 200.toBigDecimal()
        }
        val third = dynamicAdTextTarget().apply {
            name = "b"
            price = 300.toBigDecimal()
        }

        val comparator = getComparator(
                orderBy(GdDynamicAdTargetOrderByField.NAME, Order.ASC),
                orderBy(GdDynamicAdTargetOrderByField.PRICE, Order.DESC))
        val sorted = listOf(first, second, third).sortedWith(comparator)

        assertThat(sorted).containsExactly(first, third, second)
    }

    @Test
    fun comparator_byPrice() {
        val first = dynamicAdTextTarget().apply { price = 100.toBigDecimal() }
        val second = dynamicAdTextTarget().apply { price = 200.toBigDecimal() }
        val third = dynamicAdTextTarget().apply { price = null }

        val comparator = getComparator(orderBy(GdDynamicAdTargetOrderByField.PRICE, Order.ASC))
        val sorted = listOf(third, second, first).sortedWith(comparator)

        assertThat(sorted).containsExactly(first, second, third)
    }

    @Test
    fun comparator_byName() {
        val first = dynamicAdTextTarget().apply { name = "a" }
        val second = dynamicAdTextTarget().apply { name = "b" }

        val comparator = getComparator(orderBy(GdDynamicAdTargetOrderByField.NAME, Order.ASC))
        val sorted = listOf(first, second).sortedWith(comparator)

        assertThat(sorted).containsExactly(first, second)
    }

    @Test
    fun comparator_byName_reverse() {
        val first = dynamicAdTextTarget().apply { name = "a" }
        val second = dynamicAdTextTarget().apply { name = "b" }

        val comparator = getComparator(orderBy(GdDynamicAdTargetOrderByField.NAME, Order.DESC))
        val sorted = listOf(first, second).sortedWith(comparator)

        assertThat(sorted).containsExactly(second, first)
    }

    @Test
    fun comparator_byStatClicks() {
        val first = dynamicAdTextTarget().apply { stats.clicks = 100 }
        val second = dynamicAdTextTarget().apply { stats.clicks = 200 }

        val comparator = getComparator(orderBy(GdDynamicAdTargetOrderByField.STAT_CLICKS, Order.ASC))
        val sorted = listOf(second, first).sortedWith(comparator)

        assertThat(sorted).containsExactly(first, second)
    }

    @Test
    fun comparator_byGoalStat() {
        val first = dynamicAdTextTarget().apply {
            goalStats = listOf(GdGoalStats().apply {
                goalId = 1
                costPerAction = 100.toBigDecimal()
            }, GdGoalStats().apply {
                goalId = 2
                costPerAction = 200.toBigDecimal()
            })
        }
        val second = dynamicAdTextTarget().apply {
            goalStats = listOf(GdGoalStats().apply {
                goalId = 1
                costPerAction = 200.toBigDecimal()
            }, GdGoalStats().apply {
                goalId = 2
                costPerAction = 100.toBigDecimal()
            })
        }

        val comparator = getComparator(orderBy(
                GdDynamicAdTargetOrderByField.STAT_COST_PER_ACTION,
                Order.ASC,
                GdOrderByParams().apply { goalId = 2 }
        ))
        val sorted = listOf(first, second).sortedWith(comparator)

        assertThat(sorted).containsExactly(second, first)
    }

    private fun orderBy(field: GdDynamicAdTargetOrderByField, order: Order, params: GdOrderByParams? = null)
            : GdDynamicAdTargetOrderBy {
        return GdDynamicAdTargetOrderBy().also {
            it.field = field
            it.order = order
            it.params = params
        }
    }

    private fun dynamicAdTextTarget(): GdDynamicAdTarget {
        return GdDynamicWebpageAdTarget().apply {
            id = randomPositiveLong()
            dynamicConditionId = randomPositiveLong()
            adGroupId = randomPositiveLong()
            campaignId = randomPositiveLong()
            name = StringUtils.randomAlphanumeric(10)
            price = BigDecimal.valueOf(100L)
            priceContext = BigDecimal.valueOf(100L)
            autobudgetPriority = GdShowConditionAutobudgetPriority.MEDIUM
            isSuspended = false
            status = GdDynamicAdTargetStatus().apply {
                readOnly = false
                suspended = false
                primaryStatus = GdDynamicAdTargetPrimaryStatus.ACTIVE
            }
            tab = GdDynamicAdTargetTab.CONDITION
            stats = GdEntityStats().apply {
                goals = randomPositiveLong()
            }
            goalStats = listOf()
        }
    }

    private fun getComparator(vararg orderByItems: GdDynamicAdTargetOrderBy): Comparator<GdDynamicAdTarget> =
            getComparator(orderByItems.asList())

}
