package ru.yandex.market.sc.feature.xdoc.fix_inbound.presenter.success

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class XdocSuccessFixViewModelTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    private lateinit var viewModel: XdocSuccessFixViewModel

    @Before
    fun setUp() {
        viewModel = XdocSuccessFixViewModel()
    }

    @Test
    fun `success navigate after delay`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.finishFixInboundEvent.getOrAwaitValue().get()).isEqualTo(Unit)
    }
}
