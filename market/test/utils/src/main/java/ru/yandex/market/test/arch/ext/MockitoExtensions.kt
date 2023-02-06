package ru.yandex.market.test.arch.ext

import org.mockito.Mockito

inline fun <reified T> mock(): T = Mockito.mock(T::class.java)