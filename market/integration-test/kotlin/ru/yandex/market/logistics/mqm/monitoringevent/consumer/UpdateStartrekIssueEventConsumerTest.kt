package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.only
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.logistics.mqm.logging.LomPlanFactTskvLogger
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.UpdateStartrekIssueEventConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.UpdateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import ru.yandex.market.logistics.mqm.utils.TskvLogCaptor
import ru.yandex.market.logistics.mqm.utils.extractIssueFields
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueUpdate

class UpdateStartrekIssueEventConsumerTest : StartrekProcessorTest() {

    @Autowired
    private lateinit var consumer: UpdateStartrekIssueEventConsumer

    @RegisterExtension
    @JvmField
    final val tskvLogCaptor = TskvLogCaptor(LomPlanFactTskvLogger.getLoggerName())

    @Test
    fun updateIssueTest() {
        val testPayload = UpdateStartrekIssuePayload(
            issueKey = "test_issue_key",
            comment = "test_comment",
            fields = mapOf(Pair("a", "b"), Pair("c", "d")),
        )
        doReturn(Issue(null, null, testPayload.issueKey, null, 1, EmptyMap(), null))
            .whenever(issues)
            .update(ArgumentMatchers.eq(testPayload.issueKey), ArgumentMatchers.any())

        consumer.processPayload(testPayload, null)

        val captorComment = ArgumentCaptor.forClass(IssueUpdate::class.java)
        Mockito.verify(issues, only()).update(
            ArgumentMatchers.eq(testPayload.issueKey),
            captorComment.capture()
        )
        assertSoftly {
            captorComment.value.comment.get().comment.get() shouldBe testPayload.comment
            extractIssueFields(captorComment.value.values) shouldContainExactly testPayload.fields
        }

        assertSoftly {
            tskvLogCaptor.results.toString() shouldContain
                "level=INFO\t" +
                "eventType=UPDATE_STARTREK_ISSUE\t" +
                "eventPayload=UpdateStartrekIssuePayload(issueKey=test_issue_key, comment=test_comment, fields={a=b, c=d})\t" +
                "message=Startrek issue was updated\t" +
                "extraKeys=issueId\t" +
                "extraValues=test_issue_key"
        }
    }

    @Test
    fun updateIssueWithoutCommentTest() {
        val testPayload = UpdateStartrekIssuePayload(
            issueKey = "test_issue_key",
            fields = mapOf(Pair("a", "b"), Pair("c", "d")),
        )
        doReturn(Issue(null, null, testPayload.issueKey, null, 1, EmptyMap(), null))
            .whenever(issues)
            .update(ArgumentMatchers.eq(testPayload.issueKey), ArgumentMatchers.any())

        consumer.processPayload(testPayload, null)

        val captorComment = ArgumentCaptor.forClass(IssueUpdate::class.java)
        Mockito.verify(issues, only()).update(
            ArgumentMatchers.eq(testPayload.issueKey),
            captorComment.capture()
        )
        assertSoftly {
            captorComment.value.comment.isEmpty() shouldBe true
            extractIssueFields(captorComment.value.values) shouldContainExactly testPayload.fields
        }
        assertSoftly {
            tskvLogCaptor.results.toString() shouldContain
                "level=INFO\t" +
                "eventType=UPDATE_STARTREK_ISSUE\t" +
                "eventPayload=UpdateStartrekIssuePayload(issueKey=test_issue_key, comment=null, fields={a=b, c=d})\t" +
                "message=Startrek issue was updated\t" +
                "extraKeys=issueId\t" +
                "extraValues=test_issue_key"
        }
    }

    @Test
    fun updateIssueWithoutFieldsTest() {
        val testPayload = UpdateStartrekIssuePayload(
            issueKey = "test_issue_key",
            comment = "test_comment",
        )
        doReturn(Issue(null, null, testPayload.issueKey, null, 1, EmptyMap(), null))
            .whenever(issues)
            .update(ArgumentMatchers.eq(testPayload.issueKey), ArgumentMatchers.any())

        consumer.processPayload(testPayload, null)

        val captorComment = ArgumentCaptor.forClass(IssueUpdate::class.java)
        Mockito.verify(issues, only()).update(
            ArgumentMatchers.eq(testPayload.issueKey),
            captorComment.capture()
        )
        assertSoftly {
            captorComment.value.comment.get().comment.get() shouldBe testPayload.comment
            extractIssueFields(captorComment.value.values) shouldHaveSize 0
        }
        assertSoftly {
            tskvLogCaptor.results.toString() shouldContain
                "level=INFO\t" +
                "eventType=UPDATE_STARTREK_ISSUE\t" +
                "eventPayload=UpdateStartrekIssuePayload(issueKey=test_issue_key, comment=test_comment, fields={})\t" +
                "message=Startrek issue was updated\t" +
                "extraKeys=issueId\t" +
                "extraValues=test_issue_key"
        }
    }
}
