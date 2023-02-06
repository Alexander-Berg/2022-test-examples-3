package ru.yandex.market.logistics.mqm.logging

import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import java.lang.reflect.Proxy

class LoggingInvocationQualityRuleHandlerTest: AbstractContextualTest() {

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    val lmsClient = mock(LMSClient::class.java)

    private val proxy = Proxy.newProxyInstance(
        this::class.java.classLoader,
        arrayOf(LMSClient::class.java),
        LoggingInvocationHandler(lmsClient, "LmsClient")
    ) as LMSClient

    @Test
    @DisplayName("Проверка корректного логирования вызовов методов")
    fun testCorrectMethodCallLogging() {
        proxy.getPartner(1)
        proxy.getPartnerExternalParam(2, PartnerExternalParamType.IS_DROPOFF)
        val capturedLogs = backLogCaptor.results
        val logRegexes = listOf(
            "tskv\t" +
                "ts=[\\S]+\t" +
                "level=INFO\t" +
                "format=plain\t" +
                "code=METHOD_CALL_TIMING_METRICS\t" +
                "payload=Called method getPartner\t" +
                "request_id=1000000000000\\/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=LmsClient\t" +
                "extra_keys=delegateClass,args,method,executionTimeMs\t" +
                "extra_values=[^,]+,'1',getPartner,\\d+\\s",
            "tskv\t" +
                "ts=[\\S]+\t" +
                "level=INFO\t" +
                "format=plain\t" +
                "code=METHOD_CALL_TIMING_METRICS\t" +
                "payload=Called method getPartnerExternalParam\t" +
                "request_id=1000000000000\\/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=LmsClient\t" +
                "extra_keys=delegateClass,args,method,executionTimeMs\t" +
                "extra_values=[^,]+,'2; IS_DROPOFF',getPartnerExternalParam,\\d+\\s"
        )

        assertSoftly {
            logRegexes.map { Regex(it) }
                .zip(capturedLogs)
                .forEach { (regex, log) -> log shouldMatch regex }
        }
    }

    @Test
    @DisplayName("Проверка корректности логирования при бросании исключения")
    fun testCorrectMethodCallLoggingWithException() {
        whenever(lmsClient.getPartnerCapacities(any())).thenThrow(ArrayIndexOutOfBoundsException())
        var exceptionWasThrown = false
        try {
            proxy.getPartnerCapacities(3)
        } catch (exception: ArrayIndexOutOfBoundsException) {
            exceptionWasThrown = true;
        }
        val capturedLog = backLogCaptor.results.first()
        val logRegex = "tskv\t" +
            "ts=[\\S]+\t" +
            "level=ERROR\t" +
            "format=json-exception\t" +
            "code=METHOD_CALL_TIMING_METRICS\t" +
            "payload=[^\\t]*\t" +
            "request_id=1000000000000\\/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
            "tags=LmsClient\t" +
            "extra_keys=delegateClass,args,exception,method,executionTimeMs\t" +
            "extra_values=[^,]+,'3',ArrayIndexOutOfBoundsException,getPartnerCapacities,\\d+\\s"

        assertSoftly {
            exceptionWasThrown shouldBe true
            capturedLog shouldMatch logRegex
        }
    }
}
