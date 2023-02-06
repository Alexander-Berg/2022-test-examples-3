package ru.yandex.market.sc.feature.xdoc.fix_inbound.presenter.info

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.network.domain.NetworkInboundUseCases
import ru.yandex.market.sc.feature.xdoc.fix_inbound.analytics.AppMetrica
import ru.yandex.market.sc.feature.xdoc.fix_inbound.data.cache.FixInboundCache
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.sc.test.resources.TestStringManager
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue
import ru.yandex.market.test.utils.isNeverSet

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class XdocInboundInfoViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkInboundUseCases: NetworkInboundUseCases

    @Mock
    private lateinit var fixInboundCache: FixInboundCache

    @Mock
    private lateinit var appMetrica: AppMetrica

    private val stringManager = TestStringManager()
    private lateinit var viewModel: XdocInboundInfoViewModel
    private val inbound =
        TestFactory.getInbound(unpackedBoxes = listOf("XDOC-1"), unsortedPallets = listOf("XDOC-2"))

    @Before
    fun setUp() {
        `when`(fixInboundCache.value).thenReturn(inbound)
        viewModel = XdocInboundInfoViewModel(networkInboundUseCases, fixInboundCache, stringManager, appMetrica)
    }

    @Test
    fun `success fix inbound`() = runTest {
        `when`(networkInboundUseCases.fixInbound(inbound.externalId)).thenReturn(Unit)
        viewModel.onFixInbound()

        assertThat(viewModel.inbound).isEqualTo(inbound)
        assertThat(viewModel.successFixInboundEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }

    @Test
    fun `fix inbound with error`() = runTest {
        val message = "fix inbound with error"
        `when`(networkInboundUseCases.fixInbound(inbound.externalId)).thenThrow(
            RuntimeException(
                message
            )
        )
        viewModel.onFixInbound()

        assertThat(viewModel.inbound).isEqualTo(inbound)
        assertThat(viewModel.uiState.getOrAwaitValue().errorText).isEqualTo(message)

        advanceUntilIdle()
        assertThat(viewModel.successFixInboundEvent.isNeverSet()).isTrue()
    }
}
