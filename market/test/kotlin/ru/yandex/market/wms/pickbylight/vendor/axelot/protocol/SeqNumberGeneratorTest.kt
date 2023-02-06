package ru.yandex.market.wms.pickbylight.vendor.axelot.protocol

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class SeqNumberGeneratorTest {

    @Test
    fun nextSeqNumber() {
        val gen = SeqNumberGenerator()

        for (i in 0..999) {
            assertThat(gen.nextSeqNumber()).isEqualTo(i)
        }

        assertThat(gen.nextSeqNumber()).isEqualTo(0)
    }

    @Test
    fun nextSeqNumberAsync() {
        val gen = SeqNumberGenerator()
        val workersCount = 125
        val repetitionsCount = 93295

        val tasks: List<Callable<Unit>> = (1..workersCount).map {
            Callable {
                for (j in 1..repetitionsCount) {
                    gen.nextSeqNumber()
                }
            }
        }
        Executors.newFixedThreadPool(20).apply {
            invokeAll(tasks, 10, TimeUnit.SECONDS)
            shutdownNow()
        }
        assertThat(gen.nextSeqNumber()).isEqualTo((workersCount * repetitionsCount) % 1000)
    }
}
