package ru.yandex.market.sc.feature.xdoc.acceptance.finish

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.data.inbound.Inbound
import ru.yandex.market.sc.core.network.domain.NetworkInboundUseCases
import ru.yandex.market.sc.feature.xdoc.acceptance.finish.analytics.AppMetrica
import ru.yandex.market.sc.feature.xdoc.acceptance.finish.presenter.results.AcceptanceResultsViewModel
import ru.yandex.market.sc.feature.xdoc.acceptance.finish.presenter.results.data.NavigationEvent
import ru.yandex.market.sc.feature.xdoc.acceptance.finish.presenter.results.data.UiState
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class AcceptanceResultsViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private val stringManager = TestStringManager()

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var networkInboundUseCases: NetworkInboundUseCases

    private lateinit var viewModel: AcceptanceResultsViewModel

    private lateinit var inbound: Inbound

    @Test
    fun `load inbound info success`() = runTest {
        val oldSortables = createSortables(2)
        val newSortables = createSortables(7)
        setupViewModel(inbound = TestFactory.getInbound(boxes = oldSortables))

        `when`(networkInboundUseCases.getInbound(inbound.externalId))
            .thenReturn(
                inbound.copy(
                    info = inbound.info.copy(
                        boxes = newSortables
                    )
                )
            )

        viewModel.uiState.test {
            viewModel.loadInboundInfo()
            val loadingState = awaitItem()
            val updatedState = awaitItem()

            with(loadingState) {
                assertThat(isLoading).isTrue()
                assertThat(acceptanceResultsInfo).isNull()
            }
            with(updatedState) {
                assertThat(isLoading).isFalse()
                assertThat(acceptanceResultsInfo?.sortables).isEqualTo(newSortables)
            }
        }
    }

    @Test
    fun `load inbound info error`() = runTest {
        setupViewModel(inbound = TestFactory.getInbound())

        val errorMessage = "Error message"
        `when`(networkInboundUseCases.getInbound(inbound.externalId))
            .thenThrow(RuntimeException(errorMessage))

        viewModel.uiState.test {
            viewModel.navigationEvent.test {
                viewModel.loadInboundInfo()
                assertThat(awaitItem()).isEqualTo(NavigationEvent.NavigateUp)
            }

            val loadingState = awaitItem()
            val errorState = awaitItem()

            assertThat(loadingState.isLoading).isTrue()
            assertThat(loadingState.errorMessage).isNull()

            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.errorMessage).isEqualTo(errorMessage)
        }
    }

    @Test
    fun `available status for finish acceptance check`() = runTest {
        val availableStatuses = setOf(
            Inbound.Status.ARRIVED,
            Inbound.Status.IN_PROGRESS,
            Inbound.Status.READY_TO_RECEIVE
        )

        suspend fun checkIsFinishAcceptanceAvailableForStatus(
            status: Inbound.Status,
            isAvailable: Boolean = true
        ) {
            setupViewModelAndLoadInbound(TestFactory.getInbound(status = status))
            assertThat(uiState.isFinishAcceptanceAvailable).isEqualTo(isAvailable)
        }

        Inbound.Status.values().forEach {
            checkIsFinishAcceptanceAvailableForStatus(
                status = it,
                isAvailable = availableStatuses.contains(it)
            )
        }
    }

    @Test
    fun `finish acceptance success`() = runTest {
        val boxes = createSortables(3)
        setupViewModelAndLoadInbound(
            inbound = TestFactory.getInbound(
                status = Inbound.Status.READY_TO_RECEIVE,
                boxes = boxes
            )
        )
        `when`(networkInboundUseCases.finishAcceptance(inbound.externalId))
            .thenReturn(Unit)
        `when`(networkInboundUseCases.getInbound(inbound.externalId))
            .thenReturn(
                inbound.copy(
                    status = Inbound.Status.INITIAL_ACCEPTANCE_COMPLETED
                )
            )

        assertThat(uiState.isLoading).isFalse()
        assertThat(uiState.acceptanceResultsInfo?.sortables).isEqualTo(boxes)
        assertThat(uiState.acceptanceResultsInfo?.isFinished).isFalse()

        viewModel.uiState.drop(1).test {
            viewModel.finishAcceptance()
            val loadingState = awaitItem()
            val acceptanceFinishedState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            with(acceptanceFinishedState) {
                assertThat(isLoading).isFalse()
                assertThat(isFinishAcceptanceAvailable).isFalse()
                assertThat(isSortableDeletionAvailable).isFalse()
                assertThat(acceptanceResultsInfo?.isFinished).isTrue()
                assertThat(acceptanceResultsInfo?.sortables).isEqualTo(boxes)
            }
        }
    }

    @Test
    fun `finish acceptance error`() = runTest {
        setupViewModelAndLoadInbound(
            inbound = TestFactory.getInbound(
                status = Inbound.Status.READY_TO_RECEIVE,
            )
        )

        val errorMessage = "Error message"
        `when`(networkInboundUseCases.finishAcceptance(inbound.externalId))
            .thenReturn(Unit)
        `when`(networkInboundUseCases.getInbound(inbound.externalId))
            .thenThrow(RuntimeException(errorMessage))

        val initialState = uiState
        assertThat(uiState.isLoading).isFalse()
        assertThat(uiState.acceptanceResultsInfo?.isFinished).isFalse()

        viewModel.uiState.drop(1).test {
            viewModel.finishAcceptance()
            val loadingState = awaitItem()
            val errorState = awaitItem()
            val resetState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            with(errorState) {
                assertThat(isLoading).isFalse()
                assertThat(isFinishAcceptanceAvailable).isFalse()
                assertThat(isSortableDeletionAvailable).isFalse()
                assertThat(acceptanceResultsInfo).isNull()
            }

            assertThat(resetState).isEqualTo(initialState)
        }
    }

    @Test
    fun `check acceptance finish confirmation flow - reject`() = runTest {
        setupViewModelAndLoadInbound(inbound = TestFactory.getInbound())
        viewModel.uiState.drop(1).test {
            viewModel.onFinishAcceptanceClick()
            viewModel.onRejectFinishAcceptance()

            verify(networkInboundUseCases, never()).finishAcceptance(inbound.externalId)

            assertThat(awaitItem().confirmationDialogMessage).isNotNull()
            assertThat(awaitItem().confirmationDialogMessage).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `check acceptance finish confirmation flow - confirm`() = runTest {
        setupViewModelAndLoadInbound(
            inbound = TestFactory.getInbound(
                status = Inbound.Status.READY_TO_RECEIVE
            )
        )
        viewModel.uiState.drop(1).test {
            viewModel.onFinishAcceptanceClick()
            viewModel.onConfirmFinishAcceptance()

            verify(networkInboundUseCases, times(1)).finishAcceptance(inbound.externalId)

            assertThat(awaitItem().confirmationDialogMessage).isNotNull()
            assertThat(awaitItem().confirmationDialogMessage).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `check single initialization`() = runTest {
        val inbound = TestFactory.getInbound()
        setupViewModel(inbound)
        repeat(3) {
            viewModel.initialize(inbound.externalId)
        }
        verify(networkInboundUseCases, times(1)).getInbound(inbound.externalId)
    }

    private suspend fun setupViewModelAndLoadInbound(inbound: Inbound) {
        setupViewModel(inbound = inbound)
        `when`(networkInboundUseCases.getInbound(inbound.externalId))
            .thenReturn(inbound)
        viewModel.loadInboundInfo()
        assertThat(uiState.isLoading).isFalse()
    }

    private fun setupViewModel(inbound: Inbound) {
        this.inbound = inbound
        viewModel = AcceptanceResultsViewModel(
            appMetrica = appMetrica,
            networkInboundUseCases = networkInboundUseCases,
            stringManager = stringManager
        )
        viewModel.setInboundExternalId(inbound.externalId)
        assertThat(uiState.isLoading).isTrue()
    }

    private val uiState: UiState get() = viewModel.uiState.value
}
