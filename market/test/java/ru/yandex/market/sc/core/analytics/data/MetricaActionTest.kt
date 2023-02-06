package ru.yandex.market.sc.core.analytics.data

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class MetricaActionTest {
    @Test
    fun `test login event`() {
        val event = MetricaEvent.builder.apply {
            action = Action.Login
        }.build()
        Assert.assertEquals("LOGIN", event.toString())
    }

    @Test
    fun `test logout event`() {
        val event = MetricaEvent.builder.apply {
            action = Action.Logout
        }.build()
        Assert.assertEquals("LOGOUT", event.toString())
    }

    @Test
    fun `test BUFFERCELLS_MAINSCREEN_VIEW event`() {
        val event = MetricaEvent.builder.apply {
            flow = Flow.BufferCells
            screen = Screen.MainScreen
            action = Action.View
        }.build()
        Assert.assertEquals("BUFFERCELLS_MAINSCREEN_VIEW", event.toString())
    }

    @Test
    fun `test PRIMARYPICK_ORDERSCAN_SCAN event`() {
        val event = MetricaEvent.builder.apply {
            flow = Flow.PrimaryPick
            screen = Screen.OrderScan
            action = Action.Scan
        }
            .build()
        Assert.assertEquals("PRIMARYPICK_ORDERSCAN_SCAN", event.toString())
    }

    @Test
    fun `test SORTORDER_ORDERSCAN_MANUALENTER event`() {
        val event = MetricaEvent.builder.apply {
            flow = Flow.SortOrder
            screen = Screen.OrderScan
            action = Action.ManualEnter
        }
            .build()
        Assert.assertEquals("SORTORDER_ORDERSCAN_MANUALENTER", event.toString())
    }

    @Test
    fun `test OFFLOADDRETURN_SELECTRETURN_SELECT event`() {
        val event = MetricaEvent.builder.apply {
            flow = Flow.OffloadReturn
            screen = Screen.SelectReturn
            action = Action.Select
        }
            .build()
        Assert.assertEquals("OFFLOADRETURN_SELECTRETURN_SELECT", event.toString())
    }

    @Test
    fun `test SORTRETURN_ORDERSCAN_VIEW event`() {
        val event = MetricaEvent.builder.apply {
            flow = Flow.SortReturn
            screen = Screen.OrderScan
            action = Action.View
        }
            .build()
        Assert.assertEquals("SORTRETURN_ORDERSCAN_VIEW", event.toString())
    }

    @Test
    fun `test INVENTARISATION_STATSCREEN_VIEW event`() {
        val event = MetricaEvent.builder.apply {
            flow = Flow.Inventarisation
            screen = Screen.StatScreen
            action = Action.View
        }
            .build()
        Assert.assertEquals("INVENTARISATION_STATSCREEN_VIEW", event.toString())
    }

    @Test
    fun `test event only with flow`() {
        val event = MetricaEvent.builder.apply {
            flow = Flow.Inventarisation
        }
            .build()
        Assert.assertEquals("INVENTARISATION", event.toString())
    }

    @Test
    fun `test event only with screen`() {
        val event = MetricaEvent.builder.apply {
            screen = Screen.StatScreen
        }.build()
        Assert.assertEquals("STATSCREEN", event.toString())
    }

    @Test
    fun `test event only without flow`() {
        val event = MetricaEvent.builder.apply {
            screen = Screen.StatScreen
            action = Action.ManualEnter
        }.build()
        Assert.assertEquals("STATSCREEN_MANUALENTER", event.toString())
    }

    private enum class Flow :
        MetricaFlow { BufferCells, SortOrder, SortReturn, PrimaryPick, OffloadReturn, Inventarisation }

    private enum class Screen : MetricaScreen { MainScreen, OrderScan, SelectReturn, StatScreen }
    private enum class Action : MetricaAction { Login, Logout, View, Scan, Select, ManualEnter }
}