package ru.yandex.market.sc.feature.accept_returns.sorting.returns.scan_courier

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.network.domain.NetworkCourierUseCases
import ru.yandex.market.sc.feature.accept_returns.alanytics.AppMetrica
import ru.yandex.market.sc.feature.accept_returns.presenter.scan_courier.ScanCourierViewModel
import ru.yandex.market.sc.feature.accept_returns.presenter.scan_courier.data.CourierScannerMode
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.network.mocks.successResource
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ScanCourierViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkCourierUseCases: NetworkCourierUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private val stringManager = TestStringManager()
    private lateinit var viewModel: ScanCourierViewModel
    private val cipherCourierId = "cipherCourierId"

    @Before
    fun setUp() {
        viewModel = ScanCourierViewModel(
            appMetrica,
            networkCourierUseCases,
            stringManager,
        )

        Truth.assertThat(viewModel.scanMode.getOrAwaitValue())
            .isEqualTo(CourierScannerMode.CourierQRCode)
    }

    @Test
    fun `success scan courier`() = runTest {
        val courier = TestFactory.getCourier("Test Courier")
        val scanCourierResult = ScanResultFactory.getScanResultQR(cipherCourierId)

        `when`(networkCourierUseCases.getCourier(cipherCourierId)).thenReturn(
            successResource(courier)
        )
        viewModel.onScan(scanCourierResult)
        Truth.assertThat(viewModel.courier.getOrAwaitValue()).isEqualTo(courier)
    }
}
