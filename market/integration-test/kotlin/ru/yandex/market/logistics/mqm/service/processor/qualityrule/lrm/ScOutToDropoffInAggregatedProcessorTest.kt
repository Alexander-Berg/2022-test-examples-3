package ru.yandex.market.logistics.mqm.service.processor.qualityrule.lrm

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.io.InputStream
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mockito
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.ff.grid.reader.excel.XlsxGridReader
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import ru.yandex.market.logistics.mqm.utils.getAsScalarUpdate
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate

class ScOutToDropoffInAggregatedProcessorTest : StartrekProcessorTest() {
    @Captor
    lateinit var issueCreateCaptor: ArgumentCaptor<InputStream>

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2020-11-02T09:30:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Создание тикета для одного просроченного план-факта")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/sc_out_to_do_in/create_single.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/sc_out_to_do_in/create_single.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        val attachment = Mockito.mock(Attachment::class.java)
        whenever(issues.create(ArgumentMatchers.any()))
            .thenReturn(
                Issue(null, null, "MQMTESTRETURN-1", null, 1, EmptyMap(), null)
            )
        whenever(attachments.upload(ArgumentMatchers.anyString(), issueCreateCaptor.capture())).thenReturn(attachment)
        handleGroups()
        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())
        verify(attachments).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())
        val issueValues = captor.value.values
        val read = XlsxGridReader().read(issueCreateCaptor.value)
        assertSoftly {
            issueValues.getOrThrow("summary") shouldBe "[MQM] 01-11-2020: Партнёр СЦ (NO_SC_NAME_FOUND, 100), но не принято на ДО (NO_DO_NAME_FOUND, 172)"
            issueValues.getOrThrow("description") shouldBe "Список заказов в приложении (кол-во заказов: 2)\nORDER1,ORDER2"
            issueValues.getOrThrow("queue") shouldBe "MQMTESTRETURN"
            read!!.numberOfRows shouldBe 2
            read.numberOfColumns shouldBe 10
        }
    }

    @DisplayName("Добавление комментариев в тикета Startrek для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/sc_out_to_do_in/comment_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/sc_out_to_do_in/comment_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun commentAggregatedIssueTest() {
        val attachment = Mockito.mock(
            Attachment::class.java
        )
        Mockito.`when`(attachments.upload(ArgumentMatchers.anyString(), issueCreateCaptor.capture())).thenReturn(attachment)
        handleGroups()
        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        Mockito.verify(issues).update(ArgumentMatchers.eq("MQMTESTRETURN-1"), captor.capture())
        val issueUpdate = captor.value
        val commentString = issueUpdate.comment.get().comment.get()
        val values = issueUpdate.values
        softly.assertThat(commentString).isEqualTo(
            listOf(
                "Информация в тикете была автоматически изменена.",
                "",
                "Удалены неактуальные заказы (1 шт.): ORDER2.",
                "Добавлены новые заказы (1 шт.): ORDER3.",
                "Список заказов в приложении (2 шт.)."
            ).joinToString(separator = System.lineSeparator())
        )
        softly.assertThat((values.getOrThrow("customerOrderNumber") as ScalarUpdate<*>).set.get())
            .isEqualTo("ORDER1, ORDER2, ORDER3")
        softly.assertThat((values.getOrThrow("defectOrders") as ScalarUpdate<*>).set.get())
            .isEqualTo(3)
        Mockito.verify(attachments).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())
        val read = XlsxGridReader().read(issueCreateCaptor.value)
        assertSoftly {
            read!!.numberOfRows shouldBe 2
            read.numberOfColumns shouldBe 10
        }
    }

    @DisplayName("Закрытие тикета Startrek для просроченных план-фактов")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/sc_out_to_do_in/close_all_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/sc_out_to_do_in/close_all_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun closeIssueAfterLastAggregated() {
        handleGroups()
        val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
        verify(transitions).execute(
            eq("MONITORINGSNDBX-1"),
            ArgumentMatchers.any(String::class.java),
            captor.capture()
        )
        val issueUpdate = captor.value
        val commentString = issueUpdate.comment.get().comment.get()
        val values = issueUpdate.values

        assertSoftly {
            commentString shouldBe "Тикет автоматически закрыт."
            values.getAsScalarUpdate("resolution") shouldBe "can'tReproduce"
        }
    }
}
