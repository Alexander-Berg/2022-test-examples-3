package ru.yandex.market.logistics.logistrator.queue.processor

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.logistrator.exception.RequestInvalidStatusException
import ru.yandex.market.logistics.logistrator.queue.payload.RequestIdPayload
import ru.yandex.market.logistics.logistrator.queue.processor.sc_or_ff_creation.ScOrFfPartnerCreationProcessor
import ru.yandex.market.logistics.logistrator.utils.REQUEST_ID_PAYLOAD
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult

@DisplayName("Валидация запроса в состоянии драфта")
internal class RequestValidationProcessorTest : AbstractContextualTest() {

    @Autowired
    private lateinit var processor: RequestValidationProcessor

    @AfterEach
    private fun tearDown() {
        verifyNoMoreInteractions(dbQueueService)
    }

    @Test
    @DatabaseSetup("/db/request/before/sent_to_validation_valid.xml")
    @ExpectedDatabase("/db/request/after/commited.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Успешное выполнение валидации")
    fun testExecuteSuccess() {
        assertSoftly {
            processor.execute(REQUEST_ID_PAYLOAD) shouldBe TaskExecutionResult.finish()
        }

        verify(dbQueueService)
            .produceTask(eq(ScOrFfPartnerCreationProcessor::class.java), eq(PAYLOAD))
    }

    @Test
    @DatabaseSetup("/db/request/before/sent_to_validation_invalid.xml")
    @ExpectedDatabase(
        "/db/request/after/validation_errors.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное выполнение валидации с обнаружением ошибок валидации")
    fun testExecuteSuccessWithValidationErrors() {
        assertSoftly {
            processor.execute(REQUEST_ID_PAYLOAD) shouldBe TaskExecutionResult.finish()
        }
    }

    @Test
    @DatabaseSetup("/db/request/before/created.xml")
    @ExpectedDatabase("/db/request/after/created.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Невозможно выполнить валидацию - запрос не в состоянии драфта")
    fun testExecuteNotDraftFailed() {
        softly.assertThatCode { processor.execute(PAYLOAD) }
            .`as`("Asserting that a valid exception is thrown")
            .isExactlyInstanceOf(RequestInvalidStatusException::class.java)
            .hasMessage("Request 101 status is CREATED instead of DRAFT")
    }

    @Test
    @DatabaseSetup("/db/request/before/draft.xml")
    @ExpectedDatabase("/db/request/after/draft.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Невозможно выполнить валидацию - драфт не отправлен на валидацию")
    fun testExecuteNotSentToValidationFailed() {
        softly.assertThatCode { processor.execute(PAYLOAD) }
            .`as`("Asserting that a valid exception is thrown")
            .isExactlyInstanceOf(RuntimeException::class.java)
            .hasMessage("Request 101 is not sent to validation")
    }

    private companion object {
        private val PAYLOAD = RequestIdPayload(101)
    }
}
