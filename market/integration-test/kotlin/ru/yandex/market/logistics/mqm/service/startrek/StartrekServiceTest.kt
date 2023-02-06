package ru.yandex.market.logistics.mqm.service.startrek

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.logging.enums.StartrekServiceErrors
import ru.yandex.market.logistics.mqm.logging.enums.StatisticsCode
import ru.yandex.market.logistics.mqm.utils.tskvGetCode
import ru.yandex.market.logistics.mqm.utils.tskvGetExtra
import ru.yandex.market.logistics.mqm.utils.tskvGetLevel
import ru.yandex.market.logistics.mqm.utils.tskvGetPayload
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import ru.yandex.startrek.client.Issues
import ru.yandex.startrek.client.Session
import ru.yandex.startrek.client.error.StartrekInternalServerError
import ru.yandex.startrek.client.model.IssueCreate

@DisplayName("Тесты сервиса для работы со Startrek")
class StartrekServiceTest: AbstractContextualTest() {

    private val issues = Mockito.mock(Issues::class.java)

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    @Autowired
    private lateinit var startrekService: StartrekService

    @Autowired
    protected lateinit var startrekSession: Session

    @Test
    @DisplayName("Проверка обработки исключения при создании нового тикета и логирования метрик запроса")
    fun createIssueWithException() {
        val testException = "ST Error"
        whenever(startrekSession.issues()).thenReturn(issues)
        whenever(issues.create(any())).doThrow(StartrekInternalServerError(500, testException))
        whenever(clock.millis())
            .thenReturn(1)
            .thenReturn(81)
        softly.assertThatThrownBy {
            startrekService.createIssue(
                IssueCreate.builder()
                    .summary("Test ticket")
                    .build()
            )
        }
            .isInstanceOf(StartrekInternalServerError::class.java)
        assertSoftly {
            val logError = backLogCaptor.results.first { it.contains("STARTREK_INTERACTION_ERROR") }
            tskvGetLevel(logError) shouldBe "ERROR"
            tskvGetCode(logError) shouldBe StartrekServiceErrors.STARTREK_INTERACTION_ERROR.name
            tskvGetPayload(logError) shouldContain "Exception in createIssue"
            tskvGetPayload(logError) shouldContain testException
            tskvGetExtra(logError) shouldContainExactlyInAnyOrder setOf(
                "exception" to "StartrekInternalServerError",
                "action" to "createIssue",
                "issueId" to "",
            )
            val logInfo = backLogCaptor.results.first { it.contains("Statistics of startrek requests") }
            tskvGetLevel(logInfo) shouldBe "INFO"
            tskvGetCode(logInfo) shouldBe StatisticsCode.STARTREK_MONITORING.name
            tskvGetPayload(logInfo) shouldContain "Statistics of startrek requests"
            tskvGetExtra(logInfo) shouldContainExactlyInAnyOrder setOf(
                "responseTime" to "80",
                "responseCode" to "500",
                "action" to "createIssue",
            )
        }
    }
}
