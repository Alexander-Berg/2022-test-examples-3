package ru.yandex.market.sc.feature.blocking.presenter

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSharedPreferencesUseCases
import ru.yandex.market.sc.core.network.domain.NetworkUserPasswordUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.feature.blocking.R
import ru.yandex.market.sc.feature.blocking.analytics.AppMetrica
import ru.yandex.market.sc.feature.blocking.data.BlockSource
import ru.yandex.market.sc.feature.blocking.data.OrderInformation
import ru.yandex.market.sc.feature.blocking.domain.BlockingSharedPreferences
import ru.yandex.market.sc.feature.blocking.presenter.state.BlockingScanMode
import ru.yandex.market.sc.test.network.mocks.IdManager
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.yandex.market.test.utils.isNeverSet

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class BlockingViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var networkSharedPreferencesUseCases: NetworkSharedPreferencesUseCases

    @Mock
    private lateinit var networkPasswordUseCases: NetworkUserPasswordUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var blockingSharedPreferences: BlockingSharedPreferences

    private lateinit var viewModel: BlockingViewModel
    private val successPassword = ScanResultFactory.getScanResultQR("success")
    private val wrongPassword = ScanResultFactory.getScanResultQR("wrong")
    private val orderInformation = OrderInformation(
        orderExternalId = IdManager.getExternalId(),
        placeExternalId = IdManager.getIndexedExternalId()
    )

    @Before
    fun setUp() {
        viewModel = BlockingViewModel(
            networkSharedPreferencesUseCases,
            networkPasswordUseCases,
            networkCheckUserUseCases,
            blockingSharedPreferences,
            appMetrica,
            stringManager,
        )

        `when`(blockingSharedPreferences.blockSource).thenReturn(BlockSource.Sorting)
    }

    @Test
    fun `success scan`() = runTest {
        `when`(
            networkPasswordUseCases.validatePassword(
                successPassword.value,
                orderInformation.orderExternalId,
                orderInformation.placeExternalId,
                BlockSource.Sorting.toString(),
            )
        ).thenReturn(Unit)

        viewModel.init(BlockSource.Sorting, orderInformation)
        viewModel.onScan(successPassword)

        assertThat(viewModel.forceSkipEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `wrong password scan`() = runTest {
        val errorMessage = "Неверный пароль"

        `when`(
            networkPasswordUseCases.validatePassword(
                wrongPassword.value,
                orderInformation.orderExternalId,
                orderInformation.placeExternalId,
                BlockSource.Sorting.toString(),
            )
        ).thenThrow(RuntimeException(errorMessage))

        viewModel.init(BlockSource.Sorting, orderInformation)
        viewModel.onScan(wrongPassword)

        assertThat(viewModel.forceSkipEvent.isNeverSet()).isTrue()
        `assert scanner fragment`(
            overlayState = OverlayState.Failure,
            message = errorMessage,
        )
    }

    @Test
    fun `close dialog after if blocking is turned off`() = runTest {
        val errorMessage = "Неверный пароль"

        `when`(
            networkPasswordUseCases.validatePassword(
                wrongPassword.value,
                orderInformation.orderExternalId,
                orderInformation.placeExternalId,
                BlockSource.Sorting.toString(),
            )
        ).thenThrow(RuntimeException(errorMessage))

        viewModel.init(BlockSource.Sorting, orderInformation)
        viewModel.onScan(wrongPassword)

        assertThat(viewModel.forceSkipEvent.isNeverSet()).isTrue()
    }

    private fun `assert scanner fragment`(
        mode: BlockingScanMode = BlockingScanMode.PasswordQRCode,
        overlayState: OverlayState = OverlayState.None,
        message: String = stringManager.getString(R.string.must_finish_sorting),
    ) {
        val uiState = viewModel.uiState.getOrAwaitValue()
        assertEquals(mode, uiState.mode)
        assertEquals(overlayState, uiState.overlayState)
        assertEquals(message, uiState.message)
    }
}
