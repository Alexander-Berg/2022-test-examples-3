package ru.yandex.market.sc.feature.master_password.presenter.qr

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.password.Password
import ru.yandex.market.sc.core.network.domain.NetworkUserPasswordUseCases
import ru.yandex.market.sc.feature.master_password.analytics.AppMetrica
import ru.yandex.market.sc.test.network.mocks.errorResource
import ru.yandex.market.sc.test.network.mocks.successResource
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class PasswordQrViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkUserPasswordUseCases: NetworkUserPasswordUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private val stringManager = TestStringManager()
    private lateinit var viewModel: PasswordQrViewModel

    @Test
    fun `generate password with error`() {
        val message = "Что-то пошло не так"
        `when`(networkUserPasswordUseCases.generatePassword()).thenReturn(errorResource(message))
        viewModel = PasswordQrViewModel(networkUserPasswordUseCases, appMetrica, stringManager)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.errorText).isEqualTo(message)
        assertThat(uiState.password).isNull()
    }

    @Test
    fun `generate password success`() {
        val password = "password_1"
        `when`(networkUserPasswordUseCases.generatePassword()).thenReturn(
            successResource(
                Password(
                    password
                )
            )
        )
        viewModel = PasswordQrViewModel(networkUserPasswordUseCases, appMetrica, stringManager)

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.password).isEqualTo(password)
        assertThat(uiState.errorText).isEmpty()
    }

    @Test
    fun `update password`() {
        val password1 = "password_1"
        `when`(networkUserPasswordUseCases.generatePassword()).thenReturn(
            successResource(
                Password(
                    password1
                )
            )
        )
        viewModel = PasswordQrViewModel(networkUserPasswordUseCases, appMetrica, stringManager)

        val password2 = "password_2"
        `when`(networkUserPasswordUseCases.generatePassword()).thenReturn(
            successResource(
                Password(
                    password2
                )
            )
        )
        viewModel.onUpdatePassword()

        val uiState = viewModel.uiState.getOrAwaitValue()
        assertThat(uiState.password).isEqualTo(password2)
        assertThat(uiState.errorText).isEmpty()
    }
}
