package com.yandex.xplat.common

import okhttp3.internal.Util
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias Executor<V> = XPromise<V>.((V) -> Unit, (YSError) -> Unit) -> Unit

fun buildFailure(message: Throwable) = YSError("Failure from Throwable: $message\nUnderlying stack trace: $message")

fun Throwable.toFailure(): YSError = buildFailure(this)

fun createSingleThreadExecutor(name: String): ExecutorService = Executors.newSingleThreadExecutor(Util.threadFactory(name, true))
