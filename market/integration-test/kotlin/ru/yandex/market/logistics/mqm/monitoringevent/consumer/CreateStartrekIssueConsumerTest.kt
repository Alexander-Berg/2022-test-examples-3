package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.Option
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.logistics.mqm.entity.enums.IssueLinkEntityType
import ru.yandex.market.logistics.mqm.logging.MonitoringEventTskvLogger
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.CreateStartrekIssueEventConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.BaseCreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssuePayload.Fields.UNIQUE
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import ru.yandex.market.logistics.mqm.utils.TskvLogCaptor
import ru.yandex.startrek.client.error.ConflictException
import ru.yandex.startrek.client.error.ErrorCollection
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import java.io.ByteArrayInputStream

class CreateStartrekIssueConsumerTest: StartrekProcessorTest() {

    @Autowired
    private lateinit var consumer: CreateStartrekIssueEventConsumer

    @RegisterExtension
    @JvmField
    final val tskvLogCaptor = TskvLogCaptor(MonitoringEventTskvLogger.getLoggerName())

    @Test
    fun createEmptyIssueTest() {
        doReturn(Issue(null, null, "$QUEUE-1", null, 1, EmptyMap(), null))
            .whenever(issues)
            .create(ArgumentMatchers.any())

        consumer.processPayload(CreateStartrekIssuePayload(QUEUE, SUMMARY), null)

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        Mockito.verify(issues).create(captor.capture())

        val values = captor.value.values

        assertSoftly {
            values.getOrThrow("queue") shouldBe QUEUE
            values.getOrThrow("summary") shouldBe SUMMARY
        }

        assertThrows<NoSuchElementException> {
            values.getOrThrow("description")
        }

        assertSoftly {
            tskvLogCaptor.results.toString() shouldContain
                "level=INFO\t" +
                "eventType=CREATE_STARTREK_ISSUE\t" +
                "eventPayload=CreateStartrekIssuePayload(" +
                "queue=MONITORINGEVENT, " +
                "summary=Something happened today, " +
                "description=null, " +
                "fields=null, " +
                "csvAttachments=[], " +
                "entities=null"
            ")\t" +
                "message=Startrek issue was created\t" +
                "extraKeys=issueId,queue\t" +
                "extraValues=MONITORINGEVENT-1,MONITORINGEVENT"
        }
    }

    @Test
    fun createIssueWithLinks() {
        doReturn(Issue(null, null, "$QUEUE-1", null, 1, EmptyMap(), null))
            .whenever(issues)
            .create(ArgumentMatchers.any())
        val mock = Mockito.mock(Attachment::class.java)
        doReturn("123").whenever(mock).id
        doReturn(mock).whenever(attachments).add(eq("$QUEUE-1"), any(), any<ByteArrayInputStream>())
        consumer.processPayload(
            CreateStartrekIssuePayload(
                queue = QUEUE,
                summary = SUMMARY,
                issueLinks = listOf("TEST-1", "TEST-1")
            ),
            null
        )

        val captor = argumentCaptor<IssueCreate>()
        verify(issues).create(captor.capture())

        val issueCreate = captor.firstValue
        val values = issueCreate.values

        assertSoftly {
            values.getOrThrow("queue") shouldBe QUEUE
            values.getOrThrow("summary") shouldBe SUMMARY
            issueCreate.links.size shouldBe LINKSIZE
        }
    }

    @Test
    @ExpectedDatabase(
        value = "/monitoringevent/consumer/after/all_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueWithAllFields() {
        doReturn(Issue(null, null, "$QUEUE-1", null, 1, EmptyMap(), null))
            .whenever(issues)
            .create(ArgumentMatchers.any())

        consumer.processPayload(
            CreateStartrekIssuePayload(
                queue = QUEUE,
                summary = SUMMARY,
                description = DESCRIPTION,
                fields = mapOf(
                    "tags" to listOf("TAG1", "TAG2"),
                    "components" to listOf(123, 534),
                    "customerOrderNumber" to "777"
                ),
                entities = setOf(BaseCreateStartrekIssuePayload.Entity("2051", IssueLinkEntityType.ORDER))
            ),
            null
        )

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())

        val issueCreate = captor.value
        val values = issueCreate.values

        assertSoftly {
            values.getOrThrow("queue") shouldBe QUEUE
            values.getOrThrow("summary") shouldBe SUMMARY
            values.getOrThrow("description") shouldBe DESCRIPTION
            values.getOrThrow("tags") shouldBe listOf("TAG1", "TAG2")
            values.getOrThrow("components") shouldBe listOf(123, 534)
            values.getOrThrow("customerOrderNumber") shouldBe "777"
        }

    }

    @Test
    @ExpectedDatabase(
        value = "/monitoringevent/consumer/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueNotFailIfConflict() {
        val testUnique = "test_unique"
        val testException = ConflictException(ErrorCollection(null, null, 0, Option.empty()))
        whenever(issues.create(ArgumentMatchers.any())).thenThrow(testException)

        consumer.processPayload(
            CreateStartrekIssuePayload(
                queue = QUEUE,
                summary = SUMMARY,
                fields = mapOf(
                    UNIQUE.key to testUnique,
                ),
            ),
            null
        )

        val issueCreateCaptor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(issueCreateCaptor.capture())
        val values = issueCreateCaptor.value.values
        val log = tskvLogCaptor.results[0]

        assertSoftly {
            values.getOrThrow("queue") shouldBe QUEUE
            values.getOrThrow("summary") shouldBe SUMMARY
            values.getOrThrow(UNIQUE.key) shouldBe testUnique

            log shouldContain "${UNIQUE.key}=$testUnique"
            log shouldContain "There is issue with same unique value. Issue was not created."
        }
    }

    companion object {
        private const val QUEUE = "MONITORINGEVENT"
        private const val SUMMARY = "Something happened today"
        private const val LINKSIZE = 2
        private const val DESCRIPTION = "Happened because of reasons"
    }
}
