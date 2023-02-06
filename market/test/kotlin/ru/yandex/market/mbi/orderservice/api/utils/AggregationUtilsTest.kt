package ru.yandex.market.mbi.orderservice.api.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.mbi.orderservice.model.OrderStatus

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
internal class AggregationUtilsTest {
    @Test
    fun empty() {
        Assertions.assertEquals(0, aggregateFurther(mapOf(), mapOf()).size)
    }

    @Test
    fun filterAll() {
        Assertions.assertEquals(0, aggregateFurther(mapOf(OrderStatus.PROCESSING to 1), emptyMap()).size)
    }

    @Test
    fun single() {
        val orig = mapOf(
            OrderStatus.PROCESSING to 1
        )
        val mapping = mapOf(
            OrderStatus.PROCESSING to OrderStatus.PROCESSING
        )
        val actual = aggregateFurther(orig, mapping)
        Assertions.assertEquals(orig, actual)
    }

    @Test
    fun remapping() {
        val orig = mapOf(
            OrderStatus.PROCESSING to 1,
            OrderStatus.PENDING to 1,
        )
        val mapping = mapOf(
            OrderStatus.PROCESSING to OrderStatus.PROCESSING,
            OrderStatus.PENDING to OrderStatus.PROCESSING,
        )
        val actual = aggregateFurther(orig, mapping)
        val expected = mapOf(
            OrderStatus.PROCESSING to 2,
        )
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun remapAndFilter() {
        val orig = mapOf(
            OrderStatus.PROCESSING to 1,
            OrderStatus.PENDING to 1,
            OrderStatus.CANCELLED to 1,
        )
        val mapping = mapOf(
            OrderStatus.PROCESSING to OrderStatus.PROCESSING,
            OrderStatus.PENDING to OrderStatus.PROCESSING,
        )
        val actual = aggregateFurther(orig, mapping)
        val expected = mapOf(
            OrderStatus.PROCESSING to 2,
        )
        Assertions.assertEquals(expected, actual)
    }
}
