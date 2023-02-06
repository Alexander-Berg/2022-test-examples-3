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
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.feature.xdoc.acceptance.finish.analytics.AppMetrica
import ru.yandex.market.sc.feature.xdoc.acceptance.finish.presenter.delete.DeleteFromInboundViewModel
import ru.yandex.market.sc.feature.xdoc.acceptance.finish.presenter.delete.data.ConfirmationDialogState
import ru.yandex.market.sc.feature.xdoc.acceptance.finish.presenter.delete.data.DeletionMode
import ru.yandex.market.sc.feature.xdoc.acceptance.finish.presenter.delete.data.UiState
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class DeleteFromInboundViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var networkInboundUseCases: NetworkInboundUseCases

    private val stringManager = TestStringManager()
    private lateinit var viewModel: DeleteFromInboundViewModel

    private lateinit var inbound: Inbound

    @Test
    fun `load inbound success`() = runTest {
        val boxes = createSortables(3)
        setupViewModel(TestFactory.getInbound(boxes = boxes))
        `when`(networkInboundUseCases.getInbound(inbound.externalId))
            .thenReturn(inbound)
        val initialDeletionMode = uiState.deletionMode
        viewModel.uiState.test {
            viewModel.loadInbound()
            val pendingState = awaitItem()
            val withSortables = awaitItem()

            assertThat(pendingState.isLoading).isTrue()

            with(withSortables) {
                assertThat(isLoading).isFalse()
                assertThat(sortables).isEqualTo(boxes)
                assertThat(deletionMode).isEqualTo(initialDeletionMode)
            }
        }
    }

    @Test
    fun `load inbound error`() = runTest {
        setupViewModel(TestFactory.getInbound())
        val deletionMode = uiState.deletionMode
        val errorText = "Error message"
        `when`(networkInboundUseCases.getInbound(inbound.externalId))
            .thenThrow(RuntimeException(errorText))
        viewModel.uiState.test {
            viewModel.loadInbound()
            val pendingState = awaitItem()
            val errorState = awaitItem()

            assertThat(pendingState.isLoading).isTrue()

            with(errorState) {
                assertThat(isLoading).isFalse()
                assertThat(deletionMode).isEqualTo(deletionMode)
                assertThat(errorMessage).isEqualTo(errorText)
            }
        }
    }

    @Test
    fun `switch deletion mode test`() = runTest {
        setupViewModel(TestFactory.getInbound())
        val initialDeletionModeOrdinal = uiState.deletionMode.ordinal
        viewModel.uiState.drop(1).test {
            for (i in 1..3) {
                viewModel.onSwitchDeletionMode()
                assertThat(awaitItem().deletionMode.ordinal)
                    .isEqualTo((i + initialDeletionModeOrdinal) % 2)
            }
        }
    }

    @Test
    fun `set deletion mode test`() = runTest {
        setupViewModel(TestFactory.getInbound())
        viewModel.uiState.drop(1).test {
            DeletionMode.values().forEachIndexed { index, mode ->
                if (index != 0 || uiState.deletionMode != mode) {
                    viewModel.setDeletionMode(mode)
                    assertThat(awaitItem().deletionMode).isEqualTo(mode)
                }
            }
        }
    }

    @Test
    fun `delete from inbound success from list`() = runTest {
        val boxes = createSortables(3)
        setupViewModelAndLoadInbound(TestFactory.getInbound(boxes = boxes))
        viewModel.setDeletionMode(DeletionMode.FromList)
        val boxToDelete = boxes.first()
        viewModel.closeScreenEvent.test {
            viewModel.uiState.drop(1).test {
                viewModel.deleteFromInbound(boxes.first())
                val pendingState = awaitItem()

                assertThat(pendingState.isLoading).isTrue()
                assertThat(pendingState.deletionMode).isEqualTo(DeletionMode.FromList)
            }
            awaitItem()
        }

        verify(
            networkInboundUseCases,
            times(1)
        ).unlinkFromInbound(
            inboundExternalId = inbound.externalId,
            barcode = boxToDelete
        )
    }

    @Test
    fun `delete from inbound error from list`() = runTest {
        val boxes = createSortables(3)
        setupViewModelAndLoadInbound(TestFactory.getInbound(boxes = boxes))
        viewModel.setDeletionMode(DeletionMode.FromList)
        val boxToDelete = boxes.first()
        val errorText = "Error message"
        `when`(networkInboundUseCases.unlinkFromInbound(inbound.externalId, boxToDelete))
            .thenThrow(RuntimeException(errorText))
        viewModel.closeScreenEvent.test {
            viewModel.uiState.drop(1).test {
                viewModel.deleteFromInbound(boxes.first())
                val pendingState = awaitItem()
                val errorState = awaitItem()
                val resetState = awaitItem()

                assertThat(pendingState.isLoading).isTrue()
                assertThat(pendingState.deletionMode).isEqualTo(DeletionMode.FromList)

                assertIsError(
                    errorState,
                    errorText = errorText,
                    deletionMode = DeletionMode.FromList
                )

                assertState(
                    resetState,
                    deletionMode = DeletionMode.FromList,
                    isLoading = false,
                    errorMessage = null,
                    sortables = boxes
                )
            }

            expectNoEvents()
        }

        verify(
            networkInboundUseCases,
            times(1)
        ).unlinkFromInbound(
            inboundExternalId = inbound.externalId,
            barcode = boxToDelete
        )
    }

    @Test
    fun `delete from inbound success from scan`() = runTest {
        val boxes = createSortables(3)
        setupViewModelAndLoadInbound(TestFactory.getInbound(boxes = boxes))
        viewModel.setDeletionMode(DeletionMode.FromScan)
        val boxToDelete = boxes.first()
        viewModel.closeScreenEvent.test {
            viewModel.uiState.drop(1).test {
                viewModel.deleteFromInbound(boxes.first())
                val pendingState = awaitItem()
                val successState = awaitItem()

                assertThat(pendingState.isLoading).isTrue()
                assertThat(pendingState.deletionMode).isEqualTo(DeletionMode.FromScan)

                assertIsSuccess(successState, deletionMode = DeletionMode.FromScan)
            }
            awaitItem()
        }

        verify(
            networkInboundUseCases,
            times(1)
        ).unlinkFromInbound(
            inboundExternalId = inbound.externalId,
            barcode = boxToDelete
        )
    }

    @Test
    fun `delete from inbound error from scan`() = runTest {
        val boxes = createSortables(3)
        setupViewModelAndLoadInbound(TestFactory.getInbound(boxes = boxes))
        viewModel.setDeletionMode(DeletionMode.FromScan)
        val boxToDelete = boxes.first()
        val errorText = "Error message"
        `when`(networkInboundUseCases.unlinkFromInbound(inbound.externalId, boxToDelete))
            .thenThrow(RuntimeException(errorText))
        viewModel.closeScreenEvent.test {
            viewModel.uiState.drop(1).test {
                viewModel.deleteFromInbound(boxes.first())
                val pendingState = awaitItem()
                val errorState = awaitItem()
                val resetState = awaitItem()

                assertThat(pendingState.isLoading).isTrue()
                assertThat(pendingState.deletionMode).isEqualTo(DeletionMode.FromScan)

                assertIsError(
                    errorState,
                    errorText = errorText,
                    deletionMode = DeletionMode.FromScan
                )

                assertState(
                    resetState,
                    deletionMode = DeletionMode.FromScan,
                    isLoading = false,
                    errorMessage = null,
                    sortables = boxes
                )
            }

            expectNoEvents()
        }

        verify(
            networkInboundUseCases,
            times(1)
        ).unlinkFromInbound(
            inboundExternalId = inbound.externalId,
            barcode = boxToDelete
        )
    }

    @Test
    fun `check deletion confirmation flow - reject`() = runTest {
        setupViewModelAndLoadInbound(TestFactory.getInbound())
        val barcode = createSortables(1).single()
        viewModel.uiState.drop(1).test {
            viewModel.onDeleteRequest(barcode)
            viewModel.onRejectDeleteRequest()

            assertThat(awaitItem().confirmationDialogState).isEqualTo(
                ConfirmationDialogState(
                    sortableName = barcode,
                    inboundName = inbound.externalId.value
                )
            )
            assertThat(awaitItem().confirmationDialogState).isNull()
        }

        verify(networkInboundUseCases, never()).unlinkFromInbound(inbound.externalId, barcode)
    }

    @Test
    fun `check deletion confirmation flow - confirm`() = runTest {
        setupViewModelAndLoadInbound(TestFactory.getInbound())
        val barcode = createSortables(1).single()
        viewModel.uiState.drop(1).test {
            viewModel.onDeleteRequest(barcode)
            viewModel.onConfirmDeleteRequest()

            assertThat(awaitItem().confirmationDialogState).isEqualTo(
                ConfirmationDialogState(
                    sortableName = barcode,
                    inboundName = inbound.externalId.value
                )
            )
            assertThat(awaitItem().confirmationDialogState).isNull()

            cancelAndIgnoreRemainingEvents()
        }

        verify(networkInboundUseCases, times(1)).unlinkFromInbound(inbound.externalId, barcode)
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

    private fun assertIsSuccess(
        uiState: UiState,
        deletionMode: DeletionMode = uiState.deletionMode,
    ) {
        assertState(
            uiState = uiState,
            isLoading = false,
            isSuccess = true,
            overlayState = OverlayState.Success,
            errorMessage = null,
            deletionMode = deletionMode,
            sortables = emptyList(),
            confirmationDialogState = null
        )
    }

    private fun assertIsError(
        uiState: UiState,
        errorText: String,
        deletionMode: DeletionMode = uiState.deletionMode,
        sortables: List<String> = uiState.sortables,
        confirmationDialogState: ConfirmationDialogState? = uiState.confirmationDialogState
    ) {
        assertState(
            uiState = uiState,
            isLoading = false,
            isSuccess = false,
            overlayState = OverlayState.Failure,
            errorMessage = errorText,
            deletionMode = deletionMode,
            sortables = sortables,
            confirmationDialogState = confirmationDialogState
        )
    }

    private fun assertState(
        uiState: UiState,
        deletionMode: DeletionMode,
        sortables: List<String> = emptyList(),
        isLoading: Boolean = false,
        confirmationDialogState: ConfirmationDialogState? = null,
        errorMessage: String? = null,
        isSuccess: Boolean = false,
        overlayState: OverlayState = OverlayState.None
    ) {
        assertThat(uiState.deletionMode).isEqualTo(deletionMode)
        assertThat(uiState.sortables).isEqualTo(sortables)
        assertThat(uiState.isLoading).isEqualTo(isLoading)
        assertThat(uiState.confirmationDialogState).isEqualTo(confirmationDialogState)
        assertThat(uiState.errorMessage).isEqualTo(errorMessage)
        assertThat(uiState.isSuccess).isEqualTo(isSuccess)
        assertThat(uiState.overlayState).isEqualTo(overlayState)
    }

    private suspend fun setupViewModelAndLoadInbound(inbound: Inbound) {
        setupViewModel(inbound = inbound)
        `when`(networkInboundUseCases.getInbound(inbound.externalId))
            .thenReturn(inbound)
        viewModel.loadInbound()
        assertThat(uiState.isLoading).isFalse()
    }

    private fun setupViewModel(inbound: Inbound) {
        this.inbound = inbound
        viewModel = DeleteFromInboundViewModel(
            appMetrica = appMetrica,
            networkInboundUseCases = networkInboundUseCases,
            stringManager = stringManager,
        )
        viewModel.setInboundExternalId(inbound.externalId)
        assertThat(uiState.isLoading).isTrue()
    }

    private val uiState: UiState get() = viewModel.uiState.value
}
