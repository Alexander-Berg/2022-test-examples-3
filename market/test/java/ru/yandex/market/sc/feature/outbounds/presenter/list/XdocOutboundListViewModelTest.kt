package ru.yandex.market.sc.feature.outbounds.presenter.list

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.network.domain.NetworkOutboundUseCases
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class XdocOutboundListViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkOutboundUseCases: NetworkOutboundUseCases

    private val stringManager = TestStringManager()
    private lateinit var viewModel: XdocOutboundListViewModel
    private val outbounds = MutableList(3) { TestFactory.getOutbound() }

    @Before
    fun setUp() = runBlocking {
        `when`(networkOutboundUseCases.getOutbounds(null)).thenReturn(listOf())
        viewModel = XdocOutboundListViewModel(networkOutboundUseCases, stringManager)
    }

    @Test
    fun `fetch outbound list with error`() = runTest {
        val message = "Что-то пошло не так"
        `when`(networkOutboundUseCases.getOutbounds(null)).thenThrow(RuntimeException(message))

        viewModel.onUpdateOutboundList()
        val uiState = viewModel.uiState.getOrAwaitValue()

        assertThat(uiState.errorText).isEqualTo(message)
    }

    @Test
    fun `success fetch outbound list`() = runTest {
        `when`(networkOutboundUseCases.getOutbounds(null)).thenReturn(outbounds)

        viewModel.onUpdateOutboundList()
        val uiState = viewModel.uiState.getOrAwaitValue()

        assertThat(uiState.errorText).isEmpty()
        assertThat(uiState.outbounds).isEqualTo(outbounds)
    }

    @Test
    fun `select outbound from list`() = runTest {
        val outbound = outbounds.first()
        viewModel.onSelectOutbound(outbound)

        assertThat(
            viewModel.selectOutboundEvent.getOrAwaitValue().get()
        ).isEqualTo(outbound.externalId)
    }
}
