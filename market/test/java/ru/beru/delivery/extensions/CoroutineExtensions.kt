package ru.yandex.market.tpl.courier.extensions

import kotlinx.coroutines.test.DelayController
import kotlinx.coroutines.test.TestCoroutineDispatcher
import ru.yandex.market.tpl.courier.arch.common.Duration

fun DelayController.advanceTimeBy(delay: Duration): Long = advanceTimeBy(delay.longMillis)

fun TestCoroutineDispatcher.advanceTimeBy(delay: Duration): Long = advanceTimeBy(delay.longMillis)