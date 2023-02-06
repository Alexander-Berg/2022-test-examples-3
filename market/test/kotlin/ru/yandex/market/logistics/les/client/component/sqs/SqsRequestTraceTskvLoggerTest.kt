package ru.yandex.market.logistics.les.client.component.sqs

import org.assertj.core.api.Assertions
import org.assertj.core.api.Condition
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.Logger
import ru.yandex.market.logistics.les.client.configuration.properties.TraceProperties
import ru.yandex.market.request.trace.Module.LOGISTICS_MQM
import ru.yandex.market.request.trace.RequestContextHolder
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Stream


@ExtendWith(MockitoExtension::class)
class SqsRequestTraceTskvLoggerTest {

    private var clock = mock<Clock>()

    @BeforeEach
    @AfterEach
    fun before() {
        RequestContextHolder.clearContext();
        whenever(clock.instant()).thenReturn(INSTANT)
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("argumentsForIn")
    @DisplayName("Логируем входящие запросы")
    fun paramTestIn(
        name: String,
        props: TraceProperties,
        requestId: String?,
        timestamp: Long?,
        assertionCall: () -> Unit
    ) {
        val tskvLogger = SqsRequestTraceTskvLogger(props, clock, logger)
        tskvLogger.logSqsRequestIn(requestId, timestamp)
        assertionCall.invoke()
        logger.debug(name) // for warning suppress
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("argumentsForOut")
    @DisplayName("Логируем исходящие запросы")
    fun paramTestOut(name: String, prereq: () -> Unit, assertionCall: () -> Unit) {
        val props = TraceProperties(true)
        val tskvLogger = SqsRequestTraceTskvLogger(props, clock, logger)
        prereq.invoke()

        tskvLogger.logSqsRequestOut(LOGISTICS_MQM)

        assertionCall.invoke()
        logger.debug(name) // for warning suppress
    }

    companion object {
        @Spy
        var logger: Logger = Mockito.spy(Logger::class.java)

        private val INSTANT = Instant.parse("2022-04-01T14:00:00.00Z")
        private val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        @JvmStatic
        fun argumentsForIn(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "Логирование выключено",
                    TraceProperties(),
                    "test",
                    123235L,
                    {
                        Mockito.verify(logger, Mockito.never()).trace(Mockito.anyString())
                    }
                ),
                Arguments.of(
                    "Не передан таймстемп, логируем с текущем временем",
                    TraceProperties(true),
                    "123123",
                    null,
                    {
                        checkTrace(
                            { s -> s.contains("request_id=123123") && s.contains("date=${formatInstant(INSTANT)}") },
                            listOf(
                                "type=IN",
                                "source_host=sqs",
                                "target_module=les_client",
                                "request_method=sqs"
                            )
                        )
                    }
                ),
                Arguments.of(
                    "Не передан трейс, но всё ок, создаём новый трейс",
                    TraceProperties(true),
                    null,
                    1648813400L,
                    {
                        checkTrace(
                            { s: String ->
                                s.matches(".*?\trequest_id=\\d{10,14}/\\w{32}\t.*?".toRegex()) && s.contains(
                                    formatInstant(Instant.ofEpochMilli(1648813400L))
                                )
                            },
                            listOf(
                                "type=IN",
                                "source_host=sqs",
                                "target_module=les_client",
                                "request_method=sqs"
                            )
                        )
                    }
                ),
                Arguments.of(
                    "Передан трейс, но всё ок, используем его",
                    TraceProperties(true),
                    "test",
                    123235L,
                    {
                        checkTrace(
                            { s: String -> s.contains("request_id=test") },
                            listOf(
                                "type=IN",
                                "source_host=sqs",
                                "target_module=les_client",
                                "request_method=sqs"
                            )
                        )
                    }
                ),
            )
        }

        @JvmStatic
        fun argumentsForOut(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "Контекст пуст и генерим новый запрос",
                    {},
                    {
                        checkTrace(
                            { s: String -> s.matches(".*?\trequest_id=\\d{10,14}/\\w{32}\t.*?".toRegex()) },
                            listOf(
                                "type=OUT",
                                "source_host=$LOGISTICS_MQM",
                                "target_module=sqs",
                                "request_method=sqs"
                            )
                        )
                    }
                ),
                Arguments.of(
                    "Контекст не пуст и генерим новый подзапрос",
                    { RequestContextHolder.createNewContext() },
                    {
                        checkTrace(
                            { s: String -> s.matches(".*?\trequest_id=\\d{10,14}/\\w{32}/\\d+\t.*?".toRegex()) },
                            listOf(
                                "type=OUT",
                                "source_host=$LOGISTICS_MQM",
                                "target_module=sqs",
                                "request_method=sqs"
                            )
                        )
                    }
                ),
            )
        }

        private fun checkTrace(
            cond: (s: String) -> Boolean,
            constainsSeqs: Iterable<CharSequence>
        ) {
            val captor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)
            Mockito.verify(logger, Mockito.atLeast(1)).trace(
                captor.capture()
            )
            Assertions
                .assertThat(captor.value)
                .`is`(
                    Condition(
                        cond, "RequestId is not in condition"
                    )
                )
                .contains(constainsSeqs)
        }

        private fun formatInstant(instant: Instant) = DATE_TIME_FORMAT.format(
            ZonedDateTime.ofInstant(instant, Clock.systemDefaultZone().zone)
        )
    }
}
