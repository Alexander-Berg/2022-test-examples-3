package ru.beru.sortingcenter.ui.palletization.scancell

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
import ru.beru.sortingcenter.ui.palletization.data.cache.PalletizationCellCache
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.network.domain.NetworkCellUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.beru.sortingcenter.ui.palletization.scancell.model.scanner.ScannerModeImpl as ScannerMode

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class PalletizationScanCellViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkCellUseCases: NetworkCellUseCases

    @Mock
    private lateinit var cellCache: PalletizationCellCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: PalletizationScanCellViewModel

    private val courierCell = TestFactory.getCourierCell()
    private val returnCell = TestFactory.getReturnCell()
    private val courierCellWithOrders = TestFactory.mapToCellWithOrders(courierCell)
    private val returnCellWithOrders = TestFactory.mapToCellWithOrders(returnCell)

    @Before
    fun setUp() {
        viewModel = PalletizationScanCellViewModel(networkCellUseCases, cellCache, appMetrica, stringManager)

    }

    @After
    fun tearDown() {
        viewModel.forceReset()
        `wait for cell`()
    }

    @Test
    fun `wait for cell`() = runTest {
        val scanner = viewModel.scanner.getOrAwaitValue()
        val label = viewModel.label.getOrAwaitValue()

        assertEquals(ScannerMode.CellQRCode, scanner.mode)
        assertEquals(OverlayState.None, scanner.overlayState)
        assertNull(scanner.overlayMessage)
        assertEquals(stringManager.getString(R.string.select_cell_to_palletization), label.text)
    }

    @Test
    fun `scan buffer cell`() = runTest {
        val bufferCell = courierCellWithOrders.copy(type = Cell.Type.BUFFER)
        val scanCellResult = ScanResultFactory.getScanResultQR(bufferCell.id)
        `when`(networkCellUseCases.getCell(bufferCell.id)).thenReturn(bufferCell)
        viewModel.processScanResult(scanCellResult)

        val scanner = viewModel.scanner.getOrAwaitValue()
        val label = viewModel.label.getOrAwaitValue()

        assertEquals(ScannerMode.CellQRCode, scanner.mode)
        assertEquals(OverlayState.Failure, scanner.overlayState)
        assertNull(scanner.overlayMessage)
        assertEquals(stringManager.getString(R.string.wrong_cell_type), label.text)
    }

    @Test
    fun `scan courier cell`() = runTest {
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        `when`(networkCellUseCases.getCell(courierCell.id)).thenReturn(courierCellWithOrders)
        viewModel.processScanResult(scanCellResult)

        assertThat(viewModel.cellScanEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `scan return cell`() = runTest {
        val scanCellResult = ScanResultFactory.getScanResultQR(returnCellWithOrders.id)
        `when`(networkCellUseCases.getCell(returnCellWithOrders.id)).thenReturn(returnCellWithOrders)
        viewModel.processScanResult(scanCellResult)

        assertThat(viewModel.cellScanEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `scan not active cell`() = runTest {
        val notActiveCell = courierCellWithOrders.copy(status = Cell.Status.NOT_ACTIVE)
        val scanCellResult = ScanResultFactory.getScanResultQR(courierCell.id)
        `when`(networkCellUseCases.getCell(courierCell.id)).thenReturn(notActiveCell)
        viewModel.processScanResult(scanCellResult)

        val scanner = viewModel.scanner.getOrAwaitValue()
        val label = viewModel.label.getOrAwaitValue()

        assertEquals(ScannerMode.CellQRCode, scanner.mode)
        assertEquals(OverlayState.Warning, scanner.overlayState)
        assertEquals(
            stringManager.getString(R.string.cell_not_active),
            stringManager.getString(scanner.overlayMessage!!)
        )
        assertEquals(stringManager.getString(R.string.error), label.text)
    }

    @Test
    fun `cell not found`() = runTest {
        val response = TestFactory.getResponseError<Int>(code = 404, error = "Что-то пошло не так")
        val scanCellResult = ScanResultFactory.getScanResultQR(-1)
        `when`(networkCellUseCases.getCell(-1)).thenThrow(HttpException(response))
        viewModel.processScanResult(scanCellResult)

        val scanner = viewModel.scanner.getOrAwaitValue()
        val label = viewModel.label.getOrAwaitValue()

        assertEquals(ScannerMode.CellQRCode, scanner.mode)
        assertEquals(OverlayState.Failure, scanner.overlayState)
        assertNull(scanner.overlayMessage)
        assertEquals(stringManager.getString(R.string.cell_not_found), label.text)
    }

    @Test
    fun `stop and resume scan`() = runTest {
        val notExistedCellId = 403L
        val scanResult = ScanResultFactory.getScanResultQR(notExistedCellId)

        viewModel.stopScan()
        viewModel.processScanResult(scanResult)

        val scanner = viewModel.scanner.getOrAwaitValue()
        val label = viewModel.label.getOrAwaitValue()

        assertEquals(ScannerMode.DoNotScan, scanner.mode)
        assertEquals(OverlayState.None, scanner.overlayState)
        assertEquals(stringManager.getString(R.string.select_cell_to_palletization), label.text)

        viewModel.resumeScan()
        `cell not found`()
    }

    @Test
    fun `on skip`() = runTest {
        viewModel.onSkip()
        assertThat(viewModel.skipEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }
}
