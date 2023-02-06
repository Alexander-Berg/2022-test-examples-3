package ru.yandex.market.pricingmgmt.util

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

class SynchronousExecutionService : ExecutorService {
    companion object {
        private val EMPTY_MUTABLE_LIST: MutableList<Runnable> = ArrayList()

        private const val SUCCESS_STATUS = 0u
    }

    private var isClosed = false

    override fun awaitTermination(timeout: Long, unit: TimeUnit) = isClosed

    override fun execute(command: Runnable) = command.run()

    override fun <T> invokeAll(tasks: MutableCollection<out Callable<T>>) =
        tasks.map(this::submit).toMutableList()

    override fun <T> invokeAll(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit) =
        invokeAll(tasks)

    override fun isShutdown() = isClosed

    override fun isTerminated() = isClosed

    override fun <T> invokeAny(tasks: MutableCollection<out Callable<T>>): T = tasks.random().call()

    override fun <T> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit) =
        invokeAny(tasks)

    override fun shutdown() {
        isClosed = true
    }

    override fun shutdownNow() = EMPTY_MUTABLE_LIST

    override fun <T> submit(task: Callable<T>) = FutureTask(task)

    override fun <T> submit(task: Runnable, result: T) = FutureTask(task, result)

    override fun submit(task: Runnable) = submit(task, SUCCESS_STATUS)

}
