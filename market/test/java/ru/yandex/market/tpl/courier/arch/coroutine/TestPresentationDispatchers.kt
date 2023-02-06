package ru.yandex.market.tpl.courier.arch.coroutine

import kotlinx.coroutines.test.TestCoroutineDispatcher

class TestPresentationDispatchers(
    override val main: TestCoroutineDispatcher = TestCoroutineDispatcher(),
    override val worker: TestCoroutineDispatcher = TestCoroutineDispatcher(),
    override val delay: TestCoroutineDispatcher = TestCoroutineDispatcher(),
) : PresentationDispatchers {

    fun advanceUntilIdle() {
        main.advanceUntilIdle()
        worker.advanceUntilIdle()
        delay.advanceUntilIdle()
    }
}