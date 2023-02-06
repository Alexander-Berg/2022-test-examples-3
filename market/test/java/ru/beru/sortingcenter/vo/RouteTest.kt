package ru.beru.sortingcenter.vo

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.yandex.market.sc.test.network.mocks.TestFactory

class RouteTest {
    @Test
    fun `returns courier name`() {
        val route = TestFactory.getRoute(courier = TestFactory.getCourier("John Doe"))
        assertEquals("John Doe", route.name)
    }

    @Test
    fun `returns warehouse name`() {
        val route = TestFactory.getRoute(warehouseName = "Some warehouse")
        assertEquals("Some warehouse", route.name)
    }
}