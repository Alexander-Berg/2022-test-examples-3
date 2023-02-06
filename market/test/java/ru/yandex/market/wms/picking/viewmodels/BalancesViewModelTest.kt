package ru.yandex.market.wms.picking.viewmodels

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.qameta.allure.kotlin.junit4.AllureRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.market.analytics.AppMetrics
import ru.yandex.market.auth.AuthDelegate
import ru.yandex.market.data.InputResult
import ru.yandex.market.delegats.ErrorDelegate
import ru.yandex.market.delegats.LoadingDelegate
import ru.yandex.market.exceptions.mappers.StatusCodeExplainer
import ru.yandex.market.generators.SeedAutoIncrementer
import ru.yandex.market.generators.generateInt
import ru.yandex.market.media.MediaPlayer
import ru.yandex.market.providers.DataBus
import ru.yandex.market.resources.ResourceManager
import ru.yandex.market.validation.EmptyStringValidator
import ru.yandex.market.wms.balances.data.dto.SearchModeDTO
import ru.yandex.market.wms.balances.data.entity.Balances
import ru.yandex.market.wms.balances.ui.BalancesViewModel
import ru.yandex.market.wms.balances.usecases.BalancesUseCases
import ru.yandex.market.wms.logger.Logger
import ru.yandex.market.wms.picking.common.CoroutineTestRule

@ExperimentalCoroutinesApi
@RunWith(AllureRunner::class)
class BalancesViewModelTest {

    @RelaxedMockK
    private lateinit var authDelegate: AuthDelegate

    @RelaxedMockK
    private lateinit var loadingDelegate: LoadingDelegate

    @MockK
    private lateinit var balancesUseCases: BalancesUseCases

    @MockK
    private lateinit var errorBuss: DataBus<Throwable>

    @MockK
    private lateinit var logger: Logger

    @MockK
    private lateinit var statusCodeExplainer: StatusCodeExplainer

    @MockK
    private lateinit var mediaPlayer: MediaPlayer

    @MockK
    private lateinit var resourceManager: ResourceManager

    @MockK
    private lateinit var appMetrics: AppMetrics

    private lateinit var errorDelegate: ErrorDelegate
    private lateinit var emptyStringValidator: EmptyStringValidator
    private lateinit var balancesViewModel: BalancesViewModel

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    private val seedGenerator = SeedAutoIncrementer()

    @Before
    fun setUpViewModel() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        emptyStringValidator = EmptyStringValidator(resourceManager)

        errorDelegate = spyk(
            ErrorDelegate(
                errorBuss = errorBuss,
                logger = logger,
                statusCodeExplainer = statusCodeExplainer,
                mediaPlayer = mediaPlayer,
                resourceManager = resourceManager,
                appMetrics = appMetrics,
            )
        )

        balancesViewModel = BalancesViewModel(
            balancesUseCases = balancesUseCases,
            authDelegate = authDelegate,
            loadingDelegate = loadingDelegate,
            errorDelegate = errorDelegate,
            dispatchers = coroutineTestRule.testDispatcherProvider,
            validator = emptyStringValidator,
        )
    }

    @Test
    fun `Test searching random value by each search mode`() = runTest {
        val searchModeSlot = slot<SearchModeDTO>()
        coEvery { balancesUseCases.getBalances(capture(searchModeSlot), any()) } returns Balances()

        balancesViewModel.searchModes.forEach { searchModePair ->
            balancesViewModel.onSearchModeSelected(searchModePair.second)
            val searchValue = InputResult("search: " + generateInt(seedGenerator.nextState()), 100L)
            balancesViewModel.onSearch(searchValue)

            coVerify { balancesUseCases.getBalances(searchModePair.second, searchValue.text) }
            coVerify {
                loadingDelegate.setLoading(true)
                loadingDelegate.setLoading(false)
            }

            assertEquals(searchModePair.second, searchModeSlot.captured)
            assertEquals(Balances(), balancesViewModel.balances.first())
        }
    }

    @Test
    fun `Test enter empty string`() = runTest {
        coEvery { resourceManager.getString(any()) } returns ""

        balancesViewModel.onSearch(InputResult("", 100L))

        verify { errorDelegate.validate(InputResult("", 100L), emptyStringValidator, any()) }
        assertTrue(errorDelegate.error.first() != null)
    }

    @Test
    fun `Test clicking on error`() = runTest {
        balancesViewModel.onErrorClicked()
        coVerify { errorDelegate.hideError() }
    }
}
