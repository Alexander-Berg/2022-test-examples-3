package ru.yandex.market.sc.feature.zone.check.`in`

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.zone.Flow
import ru.yandex.market.sc.core.data.zone.FlowName
import ru.yandex.market.sc.core.data.zone.Process
import ru.yandex.market.sc.core.data.zone.Zone
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkZoneUseCases
import ru.yandex.market.sc.core.network.repository.SharedPreferenceRepository
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ZoneCheckInViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var networkZoneUseCases: NetworkZoneUseCases

    @Mock
    private lateinit var sharedPreferenceRepository: SharedPreferenceRepository

    private lateinit var viewModel: ZoneCheckInViewModel
    private val stringManager = TestStringManager()
    private val zoneResponse = Zone(
        id = 1L,
        zoneName = "zone",
        flows = listOf(
            Flow(
                systemName = FlowName("MERCHANT_INITIAL_ACCEPTANCE"),
                displayName = "Первчиная приемка",
                currentProcess = Process("INITIAL_ACCEPTANCE"),
            )
        )
    )

    @Before
    fun setUp() {
        initViewModel()
    }

    @Test
    fun `success scan zone`() = runTest {
        val zoneId = 297L
        val zoneScanResult = ScanResultFactory.getScanResultQR(zoneId)
        `when`(networkZoneUseCases.zoneCheckIn(zoneId)).thenReturn(zoneResponse)

        viewModel.processScan(zoneScanResult)
        verify(sharedPreferenceRepository).zone = zoneResponse

        advanceUntilIdle()
        assertThat(viewModel.zoneName).isEqualTo(zoneResponse.zoneName)
        assertThat(viewModel.flows).isEqualTo(zoneResponse.flows)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `scan zone with invalid format`() = runTest {
        val wrongScanResult = ScanResultFactory.getScanResultQR("error qr")
        viewModel.processScan(wrongScanResult)
    }

    @Test
    fun `init with saved zone id`() = runTest {
        `when`(sharedPreferenceRepository.zone).thenReturn(zoneResponse)
        initViewModel()

        advanceUntilIdle()
        assertThat(viewModel.zoneName).isEqualTo(zoneResponse.zoneName)
        assertThat(viewModel.flows).isEqualTo(zoneResponse.flows)
    }

    private fun initViewModel() {
        viewModel = ZoneCheckInViewModel(
            networkCheckUserUseCases,
            networkZoneUseCases,
            sharedPreferenceRepository,
            stringManager,
        )
    }
}

