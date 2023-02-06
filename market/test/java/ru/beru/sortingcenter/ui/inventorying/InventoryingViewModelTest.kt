package ru.beru.sortingcenter.ui.inventorying

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import ru.beru.sortingcenter.R
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.yandex.market.sc.core.data.cell.CellWithOrders
import ru.yandex.market.sc.core.data.order.OrderItem
import ru.yandex.market.sc.core.network.domain.NetworkCellUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.core.utils.domain.event.EventObserver
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.inventorying.models.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class InventoryingViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkCellUseCases: NetworkCellUseCases

    @Mock
    private lateinit var networkOrderUseCases: NetworkOrderUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: InventoryingViewModel
    private lateinit var sharedViewModelInventorying: InventoryingShareViewModel

    private val cellObserver = EventObserver { cellWithOrders: CellWithOrders ->
        sharedViewModelInventorying.prepareCommonData(cellWithOrders)
    }

    @Before
    fun setUp() {
        viewModel = InventoryingViewModel(networkCellUseCases, appMetrica)
        sharedViewModelInventorying =
            InventoryingShareViewModel(networkCellUseCases, networkOrderUseCases, appMetrica, stringManager)
    }

    @Test
    fun `wait for cell`() = runTest {
        viewModel.apply {
            assertEquals(ScannerMode.CellQRCode, this.scanMode.getOrAwaitValue())
            assertEquals(OverlayState.None, this.overlayState.getOrAwaitValue())
            assertFalse(this.isTextVisible.getOrAwaitValue())
            assertContextStringEquals(R.string.error, this.text.getOrAwaitValue())
        }
    }

    @Test
    fun `success scan cell`() = runTest {
        val cell = TestFactory.getCourierCell()
        val cellWithOrders = TestFactory.mapToCellWithOrders(cell)
        val scanResult = ScanResultFactory.getScanResultQR(cell.id)
        `when`(networkCellUseCases.getCell(cell.id)).thenReturn(cellWithOrders)

        viewModel.processScanResult(scanResult)

        viewModel.apply {
            assertThat(this.cell.getOrAwaitValue().get()).isEqualTo(cellWithOrders)
        }
    }

    @Test
    fun `success sharedViewModel data preparation`() = runTest {
        viewModel.cell.observeForever(cellObserver)
        val cell = TestFactory.getCourierCell()
        val cellWithOrders = TestFactory.mapToCellWithOrders(cell)
        val scanResult = ScanResultFactory.getScanResultQR(cell.id)
        `when`(networkCellUseCases.getCell(cell.id)).thenReturn(cellWithOrders)

        viewModel.processScanResult(scanResult)

        sharedViewModelInventorying.apply {
            assertThat(this.cell.getOrAwaitValue()).isEqualTo(cellWithOrders)
            assertEquals(listOf<OrderItem>(), this.orderItemsList.getOrAwaitValue())
            assertEquals(setOf<ExternalId>(), this.inventoriedItemsExternalIdSet)
            assertEquals(false, this.successfulInventoryingMessageShown)
        }
        viewModel.cell.removeObserver(cellObserver)
    }

    @Test
    fun `cell not found`() = runTest {
        val notExistedCellId = 404L
        val scanResult = ScanResultFactory.getScanResultQR(notExistedCellId)
        val response = TestFactory.getResponseError<Int>(code = 404)
        `when`(networkCellUseCases.getCell(notExistedCellId)).thenThrow(HttpException(response))

        viewModel.processScanResult(scanResult)

        viewModel.apply {
            assertEquals(ScannerMode.DoNotScan, this.scanMode.getOrAwaitValue())
            assertEquals(OverlayState.Failure, this.overlayState.getOrAwaitValue())
            assertTrue(this.isTextVisible.getOrAwaitValue())
            assertContextStringEquals(R.string.cell_not_found, this.text.getOrAwaitValue())
        }

        viewModel.forceReset()
        `wait for cell`()
    }

    @Test
    fun `unknown error`() = runTest {
        val notExistedCellId = 401L
        val scanResult = ScanResultFactory.getScanResultQR(notExistedCellId)
        val response = TestFactory.getResponseError<Int>(code = 401)
        `when`(networkCellUseCases.getCell(notExistedCellId)).thenThrow(HttpException(response))

        viewModel.processScanResult(scanResult)

        viewModel.apply {
            assertEquals(ScannerMode.DoNotScan, this.scanMode.getOrAwaitValue())
            assertEquals(OverlayState.Failure, this.overlayState.getOrAwaitValue())
            assertTrue(this.isTextVisible.getOrAwaitValue())
            assertContextStringEquals(R.string.unknown_error, this.text.getOrAwaitValue())
        }

        viewModel.forceReset()
        `wait for cell`()
    }

    @Test
    fun `stop and resume scan`() = runTest {
        val notExistedCellId = 403L
        val scanResult = ScanResultFactory.getScanResultQR(notExistedCellId)

        viewModel.stopScan()
        viewModel.processScanResult(scanResult)

        viewModel.apply {
            assertEquals(ScannerMode.DoNotScan, this.scanMode.getOrAwaitValue())
            assertEquals(OverlayState.None, this.overlayState.getOrAwaitValue())
            assertFalse(this.isTextVisible.getOrAwaitValue())
            assertContextStringEquals(R.string.error, this.text.getOrAwaitValue())
        }

        viewModel.resumeScan()
        `cell not found`()
    }

    private fun assertContextStringEquals(
        expected: Int,
        actual: Int,
    ) {
        assertEquals(
            stringManager.getString(expected),
            stringManager.getString(actual),
        )
    }
}
