package ru.yandex.market.sc.feature.login.presenter.login

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.lifecycle.MutableLiveData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.auth.domain.AuthUserUseCases
import ru.yandex.market.sc.core.data.sorting_center.ApiSortingCenter
import ru.yandex.market.sc.core.data.user.CheckUserData
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSharedPreferencesUseCases
import ru.yandex.market.sc.core.utils.data.LocaleUtils
import ru.yandex.market.sc.feature.login.analytics.AppMetrica
import ru.yandex.market.sc.test.network.mocks.errorResource
import ru.yandex.market.sc.test.network.mocks.successResource
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.yandex.market.test.utils.isNeverSet
import java.util.*

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class LoginViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var configuration: Configuration

    @Mock
    private lateinit var resources: Resources

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var authUserUseCases: AuthUserUseCases

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var networkSharedPreferencesUseCases: NetworkSharedPreferencesUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    private lateinit var viewModel: LoginViewModel
    private val sortingCenter = ApiSortingCenter(1L, "sorting center")
    private val checkUserData = CheckUserData(sortingCenter, mapOf(), mapOf(), listOf())

    @Before
    fun setUp() {
    }

    @Test
    fun `redirect to validate sc event`() = runTest {
        `when`(authUserUseCases.isLoggedIn).thenReturn(MutableLiveData(true))
        `when`(networkCheckUserUseCases.is2FaEnabled()).thenReturn(true)
        `when`(networkCheckUserUseCases.checkUserResource()).thenReturn(
            successResource(
                checkUserData
            )
        )

        viewModel = initViewModel()
        assertThat(viewModel.redirectToValidateScEvent.getOrAwaitValue().get())
            .isEqualTo(Unit)
        assertThat(viewModel.successLoginEvent.isNeverSet()).isTrue()
    }

    @Test
    fun `success login event`() = runTest {
        `when`(authUserUseCases.isLoggedIn).thenReturn(MutableLiveData(true))
        `when`(networkCheckUserUseCases.is2FaEnabled()).thenReturn(false)
        `when`(networkCheckUserUseCases.checkUserResource()).thenReturn(
            successResource(
                checkUserData
            )
        )

        viewModel = initViewModel()
        assertThat(viewModel.successLoginEvent.getOrAwaitValue().get()).isEqualTo(Unit)
        assertThat(viewModel.redirectToValidateScEvent.isNeverSet()).isTrue()
    }

    @Test
    fun `check user event event`() = runTest {
        val message = "something went wrong"
        `when`(authUserUseCases.isLoggedIn).thenReturn(MutableLiveData(true))
        `when`(networkCheckUserUseCases.checkUserResource()).thenReturn(errorResource(message))

        viewModel = initViewModel()
        assertThat(viewModel.checkUserErrorEvent.getOrAwaitValue().get())
            .isEqualTo(message)
    }

    @Test
    fun `show advertisement with russian locale`() {
        mockCurrentLocale(LocaleUtils.LOCALE_RU)
        `when`(authUserUseCases.isLoggedIn).thenReturn(MutableLiveData(true))

        viewModel = initViewModel()
        assertThat(viewModel.shouldShowAdvertisement.getOrAwaitValue()).isTrue()
    }

    @Test
    fun `do not show advertisement with foreign locale`() {
        mockCurrentLocale(LocaleUtils.LOCALE_ES)
        `when`(authUserUseCases.isLoggedIn).thenReturn(MutableLiveData(true))

        viewModel = initViewModel()
        assertThat(viewModel.shouldShowAdvertisement.getOrAwaitValue()).isFalse()
    }

    private fun initViewModel() = LoginViewModel(
        context,
        authUserUseCases,
        networkCheckUserUseCases,
        networkSharedPreferencesUseCases,
        appMetrica,
    )

    private fun mockCurrentLocale(locale: Locale) {
        `when`(context.resources).thenReturn(resources)

        @Suppress("DEPRECATION")
        configuration.locale = locale
        `when`(resources.configuration).thenReturn(configuration)
    }
}
