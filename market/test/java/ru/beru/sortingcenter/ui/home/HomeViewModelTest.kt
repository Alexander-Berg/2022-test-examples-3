package ru.beru.sortingcenter.ui.home

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.analytics.AppMetrica
import ru.beru.sortingcenter.ui.common.UserFirebaseService
import ru.yandex.market.sc.core.auth.domain.AuthSharedPreferencesUseCases
import ru.yandex.market.sc.core.auth.domain.AuthUserUseCases
import ru.yandex.market.sc.core.data.destination.Destination
import ru.yandex.market.sc.core.network.domain.NetworkCheckUserUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSharedPreferencesUseCases
import ru.yandex.market.sc.feature.blocking.data.BlockSource
import ru.yandex.market.sc.feature.blocking.domain.BlockingSharedPreferences
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.network.mocks.successResource
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class HomeViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var authUserUseCases: AuthUserUseCases

    @Mock
    private lateinit var authSharedPreferencesUseCases: AuthSharedPreferencesUseCases

    @Mock
    private lateinit var networkSharedPreferencesUseCases: NetworkSharedPreferencesUseCases

    @Mock
    private lateinit var blockingSharedPreferences: BlockingSharedPreferences

    @Mock
    private lateinit var userFirebaseService: UserFirebaseService

    @Mock
    private lateinit var networkCheckUserUseCases: NetworkCheckUserUseCases

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var reportCollectRoute: AppMetrica.ReportCollectRoute

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        viewModel = HomeViewModel(
            authSharedPreferencesUseCases,
            networkSharedPreferencesUseCases,
            blockingSharedPreferences,
            userFirebaseService,
            networkCheckUserUseCases,
            appMetrica,
        )
    }

    @Test
    fun `navigates to destination`() {
        `when`(appMetrica.reportCollectRoute).thenReturn(reportCollectRoute)

        Destination.values().forEach { destination ->
            viewModel.navigateTo(destination, false)

            assertThat(viewModel.navigate.getOrAwaitValue().get()).isEqualTo(
                Pair(
                    destination,
                    false
                )
            )
        }
    }

    @Test
    fun `navigates if blocked on sorting`() {
        `when`(networkSharedPreferencesUseCases.savedOrder).thenReturn("demo-123")
        viewModel.navigateIfBlockOnFlow()

        assertThat(
            viewModel.navigate.getOrAwaitValue().get()
        ).isEqualTo(Pair(Destination.SortingOrders, true))
    }

    @Test
    fun `navigates if blocked on acceptance direct`() {
        `when`(blockingSharedPreferences.blockSource).thenReturn(BlockSource.AcceptanceDirect)
        viewModel.navigateIfBlockOnFlow()

        assertThat(
            viewModel.navigate.getOrAwaitValue().get()
        ).isEqualTo(Pair(Destination.InitialAcceptance, true))
    }

    @Test
    fun `navigates if blocked on acceptance return`() {
        `when`(blockingSharedPreferences.blockSource).thenReturn(BlockSource.AcceptanceReturn)
        viewModel.navigateIfBlockOnFlow()

        assertThat(viewModel.navigate.getOrAwaitValue().get()).isEqualTo(
            Pair(
                Destination.InitialAcceptanceReturn,
                true
            )
        )
    }

    @Test
    fun `refresh check user`() = runTest {
        val data = TestFactory.createCheckUserData()
        `when`(networkCheckUserUseCases.checkUserResource()).thenReturn(successResource(data))

        viewModel.refreshUserCheck()
        assertThat(viewModel.isRefreshingCheckUser.getOrAwaitValue()).isEqualTo(false)
    }
}
