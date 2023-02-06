package ru.yandex.market.logistics.mqm.utils

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.slf4j.LoggerFactory
import ru.yandex.market.logistics.logging.backlog.BackLogWrapper
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor

internal class LoggingContextTest: AbstractTest() {
    @RegisterExtension
    @JvmField
    val backLogCaptor = BackLogCaptor()

    private val log = LoggerFactory.getLogger(this::class.java)

    @Test
    @DisplayName("Успешное логирование в контексте")
    fun withLoggingContextTest() {
        withLoggingContext(mapOf("key1" to "val1")) {
            BackLogWrapper.of(log)
                .withExtra("key2", "val2")
                .withContextData()
                .info("some info")
        }
        assertSoftly {
            LoggingContext.getAll() shouldHaveSize 0
            backLogCaptor.results.first() shouldContain
                "level=INFO\t" +
                "format=plain\t" +
                "payload=some info\t" +
                "extra_keys=key1,key2\t" +
                "extra_values=val1,val2\n"
        }
    }

    @Test
    @DisplayName("При выходе из контекста стирать только заданные в нем поля")
    fun cleanOnlyCorrespondingContextValuesTest() {
        withLoggingContext(mapOf("key1" to "val1")) {
            BackLogWrapper.of(log)
                .withExtra("key2", "val2")
                .withContextData()
                .info("some info1")
            withLoggingContext(mapOf("key3" to "val3")) {
                BackLogWrapper.of(log)
                    .withExtra("key4", "val4")
                    .withContextData()
                    .info("some info2")
            }
            BackLogWrapper.of(log)
                .withExtra("key5", "val5")
                .withContextData()
                .info("some info3")
        }
        assertSoftly {
            LoggingContext.getAll() shouldHaveSize 0
            tskvGetExtra(backLogCaptor.results[0]) shouldContainExactlyInAnyOrder listOf(
                "key1",
                "key2"
            ).zip(listOf("val1", "val2"))
            tskvGetExtra(backLogCaptor.results[1]) shouldContainExactlyInAnyOrder listOf("key1", "key3", "key4").zip(
                listOf("val1", "val3", "val4")
            )
            tskvGetExtra(backLogCaptor.results[2]) shouldContainExactlyInAnyOrder listOf(
                "key1",
                "key5"
            ).zip(listOf("val1", "val5"))
        }
    }
}
