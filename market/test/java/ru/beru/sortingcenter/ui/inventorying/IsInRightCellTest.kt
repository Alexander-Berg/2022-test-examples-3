package ru.beru.sortingcenter.ui.inventorying

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.*
import org.junit.Assert.assertFalse
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.ui.inventorying.resorting.InventoryingResortingViewModel
import ru.yandex.market.sc.core.network.domain.NetworkLogUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSortableUseCases
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class IsInRightCellTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var networkLogUseCases: NetworkLogUseCases

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var networkSortableUseCases: NetworkSortableUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private val stringManager = TestStringManager()

    private lateinit var inventoryingResortingViewModel: InventoryingResortingViewModel

    private val possibleOutgoingDateMock = "2020-12-01"

    @Before
    fun setUp() {
        inventoryingResortingViewModel = InventoryingResortingViewModel(networkOrderUseCases,
            networkSortableUseCases,
            networkLogUseCases,
            appMetrica,
            stringManager)
    }

    @After
    fun tearDown() {
        inventoryingResortingViewModel.forceReset()
    }

    @Test
    fun `order to keep in courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val order =
            TestFactory.getOrderToKeepInCell(cell = courierCell, possibleOutgoingRouteDate = possibleOutgoingDateMock)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to keep in return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val order =
            TestFactory.getOrderToKeepInCell(cell = returnCell, possibleOutgoingRouteDate = possibleOutgoingDateMock)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order sorted in right courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = courierCell)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        Assert.assertTrue(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to courier not sorted in courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val order = TestFactory.getOrderToCourier(cellTo = courierCell)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to courier not sorted in another courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val courierCellTo = TestFactory.getCourierCell("C-2")
        val order = TestFactory.getOrderToCourier(cellTo = courierCellTo)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to courier not sorted in another buffer cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val courierCellTo = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCellTo)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to courier not sorted in another return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val courierCellTo = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCellTo)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order sorted in buffer cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val order =
            TestFactory.getOrderToKeepInCell(cell = bufferCell, possibleOutgoingRouteDate = possibleOutgoingDateMock)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        Assert.assertTrue(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to drop not sorted in buffer cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val droppedCellTo = TestFactory.getDroppedCell("D-1")
        val order = TestFactory.getOrderDropped(cellTo = droppedCellTo)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to keep not sorted in buffer cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val order = TestFactory.getOrderToKeep(possibleOutgoingRouteDate = possibleOutgoingDateMock)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to keep not sorted in another courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val order = TestFactory.getOrderToKeep(possibleOutgoingRouteDate = possibleOutgoingDateMock)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to keep not sorted in another return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val order = TestFactory.getOrderToKeep(possibleOutgoingRouteDate = possibleOutgoingDateMock)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order sorted in right return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val order = TestFactory.getOrderToCourier(cellTo = returnCell, cell = returnCell)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        Assert.assertTrue(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to return not sorted in return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val order = TestFactory.getOrderToCourier(cellTo = returnCell)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to return not sorted in another courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val returnCellTo = TestFactory.getReturnCell("R-1")
        val order = TestFactory.getOrderToCourier(cellTo = returnCellTo)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to return not sorted in another buffer cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val returnCellTo = TestFactory.getReturnCell("R-1")
        val order = TestFactory.getOrderToCourier(cellTo = returnCellTo)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to return not sorted in another return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val returnCellTo = TestFactory.getReturnCell("R-2")
        val order = TestFactory.getOrderToCourier(cellTo = returnCellTo)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to courier sorted in another courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val actualCell = TestFactory.getCourierCell("C-2")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = actualCell)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to courier sorted in another buffer cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val actualCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = actualCell)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to courier sorted in another return cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val actualCell = TestFactory.getReturnCell("R-1")
        val order = TestFactory.getOrderToCourier(cellTo = courierCell, cell = actualCell)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to keep sorted in another courier cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val actualCell = TestFactory.getCourierCell("C-1")
        val order =
            TestFactory.getOrderToKeepInCell(cell = actualCell, possibleOutgoingRouteDate = possibleOutgoingDateMock)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to keep sorted in another buffer cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val actualCell = TestFactory.getBufferCell("B-2")
        val order =
            TestFactory.getOrderToKeepInCell(cell = actualCell, possibleOutgoingRouteDate = possibleOutgoingDateMock)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to keep sorted in another return cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val actualCell = TestFactory.getReturnCell("R-1")
        val order =
            TestFactory.getOrderToKeepInCell(cell = actualCell, possibleOutgoingRouteDate = possibleOutgoingDateMock)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to return sorted in another courier cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val actualCell = TestFactory.getCourierCell("C-1")
        val order = TestFactory.getOrderToReturn(cellTo = returnCell, cell = actualCell)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to return sorted in another buffer cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val actualCell = TestFactory.getBufferCell("B-1")
        val order = TestFactory.getOrderToReturn(cellTo = returnCell, cell = actualCell)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `order to return sorted in another return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val actualCell = TestFactory.getReturnCell("R-2")
        val order = TestFactory.getOrderToReturn(cellTo = returnCell, cell = actualCell)
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
    }

    @Test
    fun `multiplace order to keep in courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        val order = TestFactory.createOrderForToday(2).sort(courierCell).keep().build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to keep in return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)

        val order = TestFactory.createOrderForToday(2).sort(returnCell).keep().build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order sorted in right courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        val order = TestFactory.createOrderForToday(2)
            .updateCellTo(courierCell)
            .sort()
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        Assert.assertTrue(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        Assert.assertTrue(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to courier not sorted in courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        val order = TestFactory.createOrderForToday(2)
            .updateCellTo(courierCell)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to courier not sorted in another courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val courierCellTo = TestFactory.getCourierCell("C-2")

        val order = TestFactory.createOrderForToday(2)
            .updateCellTo(courierCellTo)
            .sort()
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to courier not sorted in another buffer cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val courierCellTo = TestFactory.getCourierCell("C-1")

        val order = TestFactory.createOrderForToday(2)
            .updateCellTo(courierCellTo)
            .sort(cell = bufferCell)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to courier not sorted in another return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val courierCellTo = TestFactory.getCourierCell("C-1")

        val order = TestFactory.createOrderForToday(2)
            .updateCellTo(courierCellTo)
            .sort(cell = returnCell)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order sorted in buffer cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)

        val order = TestFactory.createOrderForToday(2).sort(bufferCell).keep().build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        Assert.assertTrue(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        Assert.assertTrue(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to keep not sorted in buffer cell`() {
        val bufferCell = TestFactory.getBufferCell()
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)

        val order = TestFactory.createOrderForToday(2).keep().build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to keep not sorted in another courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        val order = TestFactory.createOrderForToday(2).keep().build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to keep not sorted in another return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)

        val order = TestFactory.createOrderForToday(2).keep().build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order sorted in right return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)

        val order = TestFactory.createOrderForToday(2)
            .cancel(cellTo = returnCell)
            .sort()
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        Assert.assertTrue(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        Assert.assertTrue(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order partial sorted in right buffer cell`() {
        val droppedCell = TestFactory.getDroppedCell()
        val cellWithOrders = TestFactory.mapToCellWithOrders(droppedCell)

        val order = TestFactory.createOrderForToday(2)
            .drop(cellTo = droppedCell)
            .sort(0, cell = droppedCell)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        Assert.assertTrue(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to return not sorted in return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)

        val order = TestFactory.createOrderForToday(2)
            .cancel(cellTo = returnCell)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to return not sorted in another courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val returnCellTo = TestFactory.getReturnCell("R-1")

        val order = TestFactory.createOrderForToday(2)
            .cancel(cellTo = returnCellTo)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to return not sorted in another buffer cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val returnCellTo = TestFactory.getReturnCell("R-1")

        val order = TestFactory.createOrderForToday(2)
            .cancel(cellTo = returnCellTo)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to return not sorted in another return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val returnCellTo = TestFactory.getReturnCell("R-2")

        val order = TestFactory.createOrderForToday(2)
            .updateCellTo(returnCellTo)
            .cancel(cellTo = returnCell)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to courier sorted in another courier cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val actualCell = TestFactory.getCourierCell("C-2")

        val order = TestFactory.createOrderForToday(2)
            .updateCellTo(courierCell)
            .sort(cell = actualCell)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to courier sorted in another buffer cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val actualCell = TestFactory.getBufferCell("B-1")

        val order = TestFactory.createOrderForToday(2)
            .updateCellTo(courierCell)
            .sort(cell = actualCell)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to courier sorted in another return cell`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
        val actualCell = TestFactory.getReturnCell("R-1")

        val order = TestFactory.createOrderForToday(2)
            .updateCellTo(courierCell)
            .sort(cell = actualCell)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to keep sorted in another courier cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val actualCell = TestFactory.getCourierCell("C-1")

        val order = TestFactory.createOrderForToday(2).sort(actualCell).keep().build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to keep sorted in another buffer cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val actualCell = TestFactory.getBufferCell("B-2")

        val order = TestFactory.createOrderForToday(2).sort(actualCell).keep().build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to keep sorted in another return cell`() {
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(bufferCell)
        val actualCell = TestFactory.getReturnCell("R-1")

        val order = TestFactory.createOrderForToday(2).sort(actualCell).keep().build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to return sorted in another courier cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val actualCell = TestFactory.getCourierCell("C-1")

        val order = TestFactory.createOrderForToday(2)
            .cancel(cellTo = returnCell)
            .sort(cell = actualCell)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to return sorted in another buffer cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val actualCell = TestFactory.getBufferCell("B-1")

        val order = TestFactory.createOrderForToday(2)
            .cancel(cellTo = returnCell)
            .sort(cell = actualCell)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order to return sorted in another return cell`() {
        val returnCell = TestFactory.getReturnCell("R-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(returnCell)
        val actualCell = TestFactory.getReturnCell("R-2")

        val order = TestFactory.createOrderForToday(2)
            .cancel(cellTo = returnCell)
            .sort(cell = actualCell)
            .build()
        inventoryingResortingViewModel.init(ResortType.INVENTORYING, false, arrayOf(), cellWithOrders)

        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(order.places[1]))
    }

    @Test
    fun `multiplace order partial sorted in keep cell (free cell mode)`() {
        val courierCell = TestFactory.getCourierCell("C-1")
        val bufferCell = TestFactory.getBufferCell("B-1")
        val cellWithOrders = TestFactory.mapToCellWithOrders(courierCell)

        val order = TestFactory.createOrderForToday(2)
            .sort(bufferCell)
            .sort(0, cell = courierCell)
            .keep()
            .build()
        inventoryingResortingViewModel.init(ResortType.FREE_CELL, false, arrayOf(), cellWithOrders)

        Assert.assertTrue(inventoryingResortingViewModel.isInRightCell(place = order.places.first()))
        Assert.assertTrue(inventoryingResortingViewModel.isInRightCell(place = order.places[0]))
        assertFalse(inventoryingResortingViewModel.isInRightCell(place = order.places[1]))
    }
}
