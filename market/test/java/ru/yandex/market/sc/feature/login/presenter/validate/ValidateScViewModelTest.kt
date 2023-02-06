package ru.yandex.market.sc.feature.login.presenter.validate

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.auth.domain.AuthSharedPreferencesUseCases
import ru.yandex.market.sc.core.data.sorting_center.ApiSortingCenter
import ru.yandex.market.sc.core.data.user.LogoutType
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSharedPreferencesUseCases
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.sc.test.scanner.ScanResultFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ValidateScViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var authSharedPreferencesUseCases: AuthSharedPreferencesUseCases

    @Mock
    private lateinit var networkSharedPreferencesUseCases: NetworkSharedPreferencesUseCases


    private val stringManager = TestStringManager()

    private lateinit var viewModel: ValidateScViewModel
    private val sortingCenter = ApiSortingCenter(1L, "sorting center")

    @Before
    fun setUp() {
        `when`(networkSharedPreferencesUseCases.sortingCenter).thenReturn(sortingCenter)

        viewModel = ValidateScViewModel(
            networkCheckUserUseCases,
            authSharedPreferencesUseCases,
            networkSharedPreferencesUseCases,
            stringManager,
        )
        verify(authSharedPreferencesUseCases).requireValidateSc = true
    }

    @Test
    fun `success scan qr`() = runTest {
        val password = "secure-password"
        val scanResult = ScanResultFactory.getScanResultQR(password)

        `when`(networkCheckUserUseCases.loginUser(password)).thenReturn(Unit)
        viewModel.onScan(scanResult)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.overlayStatus).isEqualTo(OverlayState.Success)
        assertThat(uiState.successText).isNotEmpty()

        advanceUntilIdle()
        verify(authSharedPreferencesUseCases).requireValidateSc = false
    }

    @Test(expected = RuntimeException::class)
    fun `scan wrong sc`() = runTest {
        val password = "secure-password"
        val errorMessage = "Wrong password"
        val scanResult = ScanResultFactory.getScanResultQR(password)

        `when`(networkCheckUserUseCases.loginUser(password)).thenThrow(RuntimeException(errorMessage))
        viewModel.onScan(scanResult)

        advanceUntilIdle()
        verifyNoInteractions(authSharedPreferencesUseCases)
    }

    @Test
    fun `on back pressed logout`() = runTest {
        viewModel.onBack()

        verify(networkCheckUserUseCases).logoutUser(LogoutType.AUTOMATICALLY_LOGOUT)
        assertThat(viewModel.logoutEvent.getOrAwaitValue().get()).isEqualTo(Unit)

        advanceUntilIdle()
        verify(authSharedPreferencesUseCases).requireValidateSc = false
    }
}
