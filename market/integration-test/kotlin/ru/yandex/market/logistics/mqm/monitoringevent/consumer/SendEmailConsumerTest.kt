package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.joda.time.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.impl.DefaultIteratorF
import ru.yandex.bolts.collection.impl.DefaultListF
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.logistics.mqm.logging.MonitoringEventTskvLogger
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.SendEmailStartrekEventConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.SendEmailStartrekIssuePayload
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import ru.yandex.market.logistics.mqm.startrek.CommentWithEmail
import ru.yandex.market.logistics.mqm.utils.TskvLogCaptor
import ru.yandex.startrek.client.Session
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueRef
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.Transition

class SendEmailConsumerTest : StartrekProcessorTest() {

        @Autowired
        private lateinit var consumer: SendEmailStartrekEventConsumer


        @RegisterExtension
        @JvmField
        final val tskvLogCaptor = TskvLogCaptor(MonitoringEventTskvLogger.getLoggerName())

        @Test
        fun createEmptyIssueTest() {
            val session = Mockito.mock(
                Session::class.java
            )
            val forClass = ArgumentCaptor.forClass(String::class.java)
            doReturn(Issue("62022da26e9da2476aa78522", null, "$QUEUE-1", null, 1, EmptyMap(), session))
                .whenever(issues)
                .get(forClass.capture())
            doReturn(attachments).whenever(session).attachments()
            doReturn(transitions).whenever(session).transitions()

            val iteratorF = DefaultIteratorF.wrap(
                listOf(
                    createPdfAttachment("NOT_DESIRED_NAME_1",createdAt = Instant.ofEpochMilli(1)),
                    createPdfAttachment("DESIRED_NAME_1",createdAt = Instant.ofEpochMilli(100)),
                    createCsvAttachment("NOT_DESIRED_NAME_1",createdAt = Instant.ofEpochMilli(1)),
                    createCsvAttachment("DESIRED_NAME_1",createdAt = Instant.ofEpochMilli(100)),
                ).listIterator()
            )
            doReturn(iteratorF)
                .whenever(attachments)
                .getAll(ArgumentMatchers.any<IssueRef>())
            val transition = mock<Transition>()
            doReturn("sent")
                .whenever(transition)
                .id

            doReturn(DefaultListF(listOf(transition)))
                .whenever(transitions)
                .getAll(ArgumentMatchers.any<IssueRef>())
            whenever(transitions["MONITORINGEVENT-1", "sent"]).thenReturn(transition)

            doReturn(Issue("62022da26e9da2476aa78522", null, "$QUEUE-1", null, 1, EmptyMap(), session))
                .whenever(issues)
                .update(ArgumentMatchers.any<Issue>(), any())

            consumer.processPayload(
                SendEmailStartrekIssuePayload(
                    QUEUE,
                    ID,
                    SENDER,
                    TARGET,
                    "SomeDescr",
                ), null
            )

            val captor = ArgumentCaptor.forClass(IssueUpdate::class.java)
            val stringCaptor = ArgumentCaptor.forClass(Issue::class.java)
            Mockito.verify(startrekSession.issues()).update(stringCaptor.capture(), captor.capture())


            val value = captor.value;

            assertSoftly {
                (value.comment.get() as CommentWithEmail).email.get().emailSend.to shouldBe listOf(TARGET)
                (value.comment.get() as CommentWithEmail).email.get().emailSend.from shouldBe listOf(SENDER)
                (value.comment.get() as CommentWithEmail).email.get().subject shouldBe listOf("Претензия №1")
                (value.comment.get().attachments.get(0) shouldBe "DESIRED_NAME_1")
                (value.comment.get().attachments.get(1) shouldBe "DESIRED_NAME_1")
                (value.comment.get().attachments.size shouldBe 2)
            }

        }

    private fun createPdfAttachment(attachmentName: String = ATTACHMENT_NAME, createdAt: Instant = Instant.ofEpochMilli(0)): Attachment? {
            val mock = Mockito.mock(Attachment::class.java)
            doReturn(attachmentName).whenever(mock).id
            doReturn("$attachmentName.pdf").whenever(mock).name
            doReturn(createdAt).whenever(mock).createdAt
            return mock
        }

        private fun createCsvAttachment(attachmentName: String = ATTACHMENT_NAME, createdAt: Instant = Instant.ofEpochMilli(0)): Attachment? {
            val mock = Mockito.mock(Attachment::class.java)
            doReturn(attachmentName).whenever(mock).id
            doReturn("$attachmentName.xlsx").whenever(mock).name
            doReturn(createdAt).whenever(mock).createdAt
            return mock
        }
        companion object {
            private const val QUEUE = "MONITORINGEVENT"
            private const val ID = "62022da26e9da2476aa78522"
            private const val ATTACHMENT_NAME = "2020-02-03-1.pdf"
            private const val SENDER = "sender@ya.ru"
            private const val TARGET = "target@ya.ru"
        }
}
