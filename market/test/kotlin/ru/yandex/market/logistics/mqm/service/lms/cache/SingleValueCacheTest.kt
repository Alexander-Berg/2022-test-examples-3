package ru.yandex.market.logistics.mqm.service.lms.cache


import com.github.benmanes.caffeine.cache.Ticker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.longs.shouldBeExactly
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
class SingleValueCacheTest {

    private val ticker = object: Ticker {
        private var secondsPassed = 0L
        override fun read(): Long {
            secondsPassed += 10
            return TimeUnit.SECONDS.toNanos(secondsPassed)
        }
    }


    private fun mockCache(refreshTime: Long, init: (Int) -> Long): SingleValueCache<Long> {
        return object: SingleValueCache<Long>(refreshTime, ticker) {
            var numberOfInvocations = 0
            override fun loadValue() = init(++numberOfInvocations)
        }
    }

    @Test
    @DisplayName("Вызвать loadValue только один раз, если прошедшее время не превышает refreshTime")
    fun loadValueOnlyOnce() {
        val cache = mockCache(1000) { it.toLong() }
        cache.getValue() shouldBeExactly 1
        cache.getValue() shouldBeExactly 1
        cache.getValue() shouldBeExactly 1
    }


      @Test
      @DisplayName("Если нет старого значения и бросается эксепшн во время получения нового, то бросать его дальше")
      fun throwExceptionIfEmptyCacheAndExceptionDuringLoading() {
          val cache = mockCache(5) { throw IllegalStateException() }
          shouldThrow<IllegalStateException> { cache.getValue() }
          shouldThrow<IllegalStateException> { cache.getValue() }
          shouldThrow<IllegalStateException> { cache.getValue() }
      }

        @Test
        @DisplayName("Если есть старое значение и бросается эксепшн во время получения нового, то вернуть старое значение")
        fun returnOldValueIfCacheIsNotEmptyAndExceptionDuringLoading() {
            val cache = mockCache(1) { if (it == 1) 1234 else throw IllegalStateException() }
            cache.getValue() shouldBeExactly 1234
            cache.getValue() shouldBeExactly 1234
            cache.getValue() shouldBeExactly 1234
        }
}
