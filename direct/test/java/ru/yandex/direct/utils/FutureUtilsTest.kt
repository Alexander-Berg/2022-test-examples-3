package ru.yandex.direct.utils

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.lessThan
import org.junit.Assert.assertThat
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CyclicBarrier


class FutureUtilsTest {

    @Test
    fun completeOne_Success() {
        val futures = listOf(CompletableFuture.supplyAsync { 5 })
        val result = FutureUtils.complete(futures).get()
        assertThat(result, equalTo(listOf(5)))
    }

    @Test(expected = CompletionException::class)
    fun completeOne_Failure() {
        val futures = listOf(CompletableFuture.supplyAsync { throw IllegalStateException("artificial") })
        FutureUtils.complete(futures).join()
    }

    @Test
    fun completeSeveral_Success() {
        val futures = listOf(CompletableFuture.supplyAsync { 5 },
            CompletableFuture.supplyAsync { 10 },
            CompletableFuture.supplyAsync { 1 })
        val result = FutureUtils.complete(futures).get()

        assertThat(result, equalTo(listOf(5, 10, 1)))
    }

    @Test
    fun completeSafeSeveral_Success() {
        val futures = listOf(CompletableFuture.supplyAsync { 5 },
            CompletableFuture.supplyAsync { 10 },
            CompletableFuture.supplyAsync { 1 })
        val result = FutureUtils.completeSafe(futures).get()
        result.forEach { assertThat(it.isSuccess, equalTo(true)) }

        assertThat(result.map { it.getOrNull() }, equalTo(listOf(5, 10, 1)))
    }

    @Test
    fun completeSafeSeveral_Failure() {
        val futures = listOf(CompletableFuture.supplyAsync { 5 },
            CompletableFuture.supplyAsync<Int> { throw IllegalArgumentException("artificial") },
            CompletableFuture.supplyAsync { 1 })
        val result = FutureUtils.completeSafe(futures).get()

        assertThat(result.count { it.isFailure }, equalTo(1))
        assertThat(result.map { it.getOrNull() }, equalTo(listOf(5, null, 1)))
    }

    @Test
    fun checkParallelExecution_Success() {
        val parties = 3
        val waitMillis = 200L
        val barrier = CyclicBarrier(parties)
        val futures = (1..parties).map {
            CompletableFuture.supplyAsync {
                barrier.await()
                Thread.sleep(waitMillis)
                it
            }
        }
        val start = System.currentTimeMillis()
        val result = FutureUtils.complete(futures).get()
        val timePassed = System.currentTimeMillis() - start

        assertThat(result, equalTo(listOf(1, 2, 3)))
        assertThat(timePassed, lessThan(waitMillis * parties))
    }

}
