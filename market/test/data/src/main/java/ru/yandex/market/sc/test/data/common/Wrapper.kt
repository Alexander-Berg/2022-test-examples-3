package ru.yandex.market.sc.test.data.common

interface Wrapper<T> {
    fun unwrap(): T
}