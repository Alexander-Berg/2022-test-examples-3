package ru.beru.sortingcenter.ui.prepare

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
import ru.beru.sortingcenter.ui.prepare.data.cache.PrepareCellCache
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.network.domain.NetworkCellUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.prepare.model.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class PrepareDispatchViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkCellUseCases: NetworkCellUseCases

    @Mock
    private lateinit var cellCache: PrepareCellCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: PrepareDispatchViewModel

    private val courierCell = TestFactory.getCourierCell()
    private val returnCell = TestFactory.getReturnCell()
    private val courierCellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
    private val returnCellWithOrders = TestFactory.mapToCellWithOrders(returnCell)

    @Before
    fun setUp() {
        viewModel = PrepareDispatchViewModel(networkCellUseCases, cellCache, appMetrica)

    }

    @After
    fun tearDown() {
        viewModel.forceReset()
        `wait for cell`()
    }

    @Test
    fun `wait for cell`() = runTest {
        viewModel.apply {
            assertEquals(ScannerMode.CellQRCode, scanMode.getOrAwaitValue())
            assertEquals(OverlayState.None, overlayState.getOrAwaitValue())
            assertNull(overlayMessage.getOrAwaitValue())
            assertContextStringEquals(R.string.select_cell_to_prepare, label.getOrAwaitValue())
        }
    }

    @Test
    fun `success scan cell`() = runTest {
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        `when`(networkCellUseCases.getCell(courierCell.id)).thenReturn(courierCellWithOrders)
        viewModel.processScanResult(scanCellResult)

        assertThat(viewModel.cellScanEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `scan not courier cell`() = runTest {
        val bufferCell = courierCellWithOrders.copy(type = Cell.Type.BUFFER)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        `when`(networkCellUseCases.getCell(courierCell.id)).thenReturn(bufferCell)
        viewModel.processScanResult(scanCellResult)

        viewModel.apply {
            assertEquals(ScannerMode.CellQRCode, scanMode.getOrAwaitValue())
            assertEquals(OverlayState.Failure, overlayState.getOrAwaitValue())
            assertNull(overlayMessage.getOrAwaitValue())
            assertContextStringEquals(R.string.wrong_cell_type, label.getOrAwaitValue())
        }
    }

    @Test
    fun `scan not return cell (return)`() = runTest {
        val bufferCell = returnCellWithOrders.copy(type = Cell.Type.BUFFER)
        val scanCellResult = ScanResultFactory.getScanResultQR(returnCell.id)
        `when`(networkCellUseCases.getCell(returnCell.id)).thenReturn(bufferCell)
        viewModel.processScanResult(scanCellResult)

        viewModel.apply {
            assertEquals(ScannerMode.CellQRCode, scanMode.getOrAwaitValue())
            assertEquals(OverlayState.Failure, overlayState.getOrAwaitValue())
            assertNull(overlayMessage.getOrAwaitValue())
            assertContextStringEquals(R.string.wrong_cell_type, label.getOrAwaitValue())
        }
    }

    @Test
    fun `scan not active cell`() = runTest {
        val notActiveCell = courierCellWithOrders.copy(status = Cell.Status.NOT_ACTIVE)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        `when`(networkCellUseCases.getCell(courierCell.id)).thenReturn(notActiveCell)
        viewModel.processScanResult(scanCellResult)

        viewModel.apply {
            assertEquals(ScannerMode.CellQRCode, scanMode.getOrAwaitValue())
            assertEquals(OverlayState.Warning, overlayState.getOrAwaitValue())
            assertContextStringEquals(R.string.cell_not_active, overlayMessage.getOrAwaitValue()!!)
            assertContextStringEquals(R.string.error, label.getOrAwaitValue())
        }
    }

    @Test
    fun `cell not found`() = runTest {
        val response = TestFactory.getResponseError<Int>(code = 404, error = "Что-то пошло не так")
        val scanCellResult = ScanResultFactory.getScanResultQR(-1)
        `when`(networkCellUseCases.getCell(-1)).thenThrow(HttpException(response))
        viewModel.processScanResult(scanCellResult)

        viewModel.apply {
            assertEquals(ScannerMode.CellQRCode, scanMode.getOrAwaitValue())
            assertEquals(OverlayState.Failure, overlayState.getOrAwaitValue())
            assertNull(overlayMessage.getOrAwaitValue())
            assertContextStringEquals(R.string.cell_not_found, label.getOrAwaitValue())
        }
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
            assertContextStringEquals(R.string.select_cell_to_prepare, this.label.getOrAwaitValue())
        }

        viewModel.resumeScan()
        `cell not found`()
    }

    @Test
    fun `on skip`() = runTest {
        viewModel.onSkip()
        assertThat(viewModel.skipEvent.getOrAwaitValue().get()).isEqualTo(Unit)
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
