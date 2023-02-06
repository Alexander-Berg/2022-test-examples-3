package ru.yandex.market.tpl.courier.presentation.feature.promo

import io.mockk.coEvery
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test
import ru.yandex.market.tpl.courier.arch.common.seconds
import ru.yandex.market.tpl.courier.arch.coroutine.TestPresentationDispatchers
import ru.yandex.market.tpl.courier.arch.fp.failure
import ru.yandex.market.tpl.courier.arch.fp.requireNotEmpty
import ru.yandex.market.tpl.courier.arch.fp.success
import ru.yandex.market.tpl.courier.arch.navigation.MainRouter
import ru.yandex.market.tpl.courier.domain.feature.promo.promoTestInstance
import ru.yandex.market.tpl.courier.extensions.advanceTimeBy

class PromoPresenterTest {
    private val dispatchers = TestPresentationDispatchers()
    private val configuration = PromoPresenter.Configuration(
        promoId = "id".requireNotEmpty(),
        showProgressDelay = 1.seconds,
        maxRetryAttempts = 3,
    )
    private val useCases: PromoUseCases = mockk {
        coEvery { getPromoById(any()) } returns success(promoTestInstance())
        coEvery { getPromoCurrentPage(any()) } returns 1
        coEvery { getPromoIsViewedFlag(any()) } returns success(false)
    }
    private val mainRouter: MainRouter = mockk()
    private val view: PromoMoxyView = mockk {
        justRun { showProgress() }
        justRun { displayPromo(any()) }
        justRun { hideSelf() }
        justRun { displayPromoProgress(any()) }
        justRun { displayPromoViewedBadge() }
    }
    private val presenter = PromoPresenter(dispatchers, configuration, useCases, mainRouter)

    @Test
    fun `Перезагружает данные при ошибках`() {
        coEvery { useCases.getPromoById(any()) } returnsMany listOf(
            failure(RuntimeException()),
            failure(RuntimeException()),
            success(promoTestInstance()),
        )

        presenter.attachView(view)
        dispatchers.advanceUntilIdle()

        verifyOrder {
            view.hideSelf()
            view.showProgress()
            view.displayPromo(any())
        }
    }

    @Test
    fun `Прячет вьюху после максимального числа попыток загрузить данные`() {
        coEvery { useCases.getPromoById(any()) } returns failure(RuntimeException())

        presenter.attachView(view)
        dispatchers.advanceUntilIdle()

        verifyOrder {
            view.hideSelf()
            view.showProgress()
            view.hideSelf()
        }
    }

    @Test
    fun `Не показывает вертухан если данные загрузились быстро`() {
        presenter.attachView(view)

        verify(exactly = 0) { view.showProgress() }
    }

    @Test
    fun `Показывает вертухан если данные грузились долго`() {
        coEvery { useCases.getPromoById(any()) } answers {
            dispatchers.delay.advanceTimeBy(configuration.showProgressDelay)
            success(promoTestInstance())
        }

        presenter.attachView(view)

        verify { view.showProgress() }
    }
}