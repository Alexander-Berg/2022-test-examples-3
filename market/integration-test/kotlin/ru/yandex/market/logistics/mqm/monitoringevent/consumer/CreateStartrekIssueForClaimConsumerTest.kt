package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.Option
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.enums.IssueLinkEntityType
import ru.yandex.market.logistics.mqm.logging.MonitoringEventTskvLogger
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.CreateStartrekIssueForClaimEventConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.BaseCreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssueForClaimPayload
import ru.yandex.market.logistics.mqm.service.ClaimService
import ru.yandex.market.logistics.mqm.service.FbyClaimService
import ru.yandex.market.logistics.mqm.service.enums.ClaimPartnerType
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import ru.yandex.market.logistics.mqm.service.yt.dto.ClaimInfo
import ru.yandex.market.logistics.mqm.tms.claim.AbstractCreateClaimExecutor
import ru.yandex.market.logistics.mqm.utils.TskvLogCaptor
import ru.yandex.startrek.client.error.ConflictException
import ru.yandex.startrek.client.error.ErrorCollection
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate

class CreateStartrekIssueForClaimConsumerTest: StartrekProcessorTest() {

    @Autowired
    private lateinit var consumer: CreateStartrekIssueForClaimEventConsumer

    @Autowired
    private lateinit var claimService: ClaimService

    @Autowired
    private lateinit var fbyClaimService: FbyClaimService

    @RegisterExtension
    @JvmField
    final val tskvLogCaptor = TskvLogCaptor(MonitoringEventTskvLogger.getLoggerName())

    @Test
    fun createEmptyIssueTest() {
        doReturn(Issue(null, null, "$QUEUE-1", null, 1, EmptyMap(), null))
            .whenever(issues)
            .create(ArgumentMatchers.any())

        consumer.processPayload(CreateStartrekIssueForClaimPayload(QUEUE, SUMMARY), null)

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
                "eventType=CREATE_STARTREK_ISSUE_FOR_CLAIM\t" +
                "eventPayload=CreateStartrekIssueForClaimPayload(" +
                "queue=MONITORINGEVENT, " +
                "summary=Something happened today, " +
                "description=null, " +
                "fields=null, " +
                "csvAttachments=[], " +
                "entities=[]"
            ")\t" +
                "message=Startrek issue was created\t" +
                "extraKeys=issueId,queue\t" +
                "extraValues=MONITORINGEVENT-1,MONITORINGEVENT"
        }
    }

    @Test
    fun createIssueWithAttachments() {
        doReturn(Issue(null, null, "$QUEUE-1", null, 1, EmptyMap(), null))
            .whenever(issues)
            .create(ArgumentMatchers.any())
        val mock = Mockito.mock(Attachment::class.java)
        doReturn("123").whenever(mock).id
        doReturn(mock).whenever(attachments).add(eq("$QUEUE-1"), any(), any<ByteArrayInputStream>())
        val partnerInfo = AbstractCreateClaimExecutor.PartnerInfo(
            302,
            "Какая-то ПВЗ из YT 1",
            "ООО Какая-то ПВЗ из LMS 1",
            "Legal адрес ПВЗ из LMS 1"
        )
        consumer.processPayload(
            CreateStartrekIssueForClaimPayload(
                queue = QUEUE,
                summary = SUMMARY,
                claimData = claimService.collectClaimDataForYtOrders(
                    partnerInfo,
                    listOf(
                        ClaimInfo(
                            1, 1001, 302,
                            ClaimPartnerType.PICKUP_POINT,
                            "Подтип",
                            "Какая-то ПВЗ из YT 1",
                            "Адрес ПВЗ из YT 1",
                            "email@email.com",
                            BigDecimal("101.01"),
                            LocalDateTime.parse("2021-12-20T20:00:00.00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                .atZone(DateTimeUtils.MOSCOW_ZONE).toInstant(),
                            LocalDateTime.parse("2021-12-20T20:00:00.00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                .atZone(DateTimeUtils.MOSCOW_ZONE).toInstant(),
                            "Подтип партнера",
                            "Юридическое имя"
                        )
                    ),
                ),
                partnerInfo = AbstractCreateClaimExecutor.PartnerInfo(
                    302,
                    "Какая-то ПВЗ из YT 1",
                    "ООО Какая-то ПВЗ из LMS 1",
                    "Legal адрес ПВЗ из LMS 1"
                )

            ),
            null
        )

        val create = argumentCaptor<IssueCreate>()
        val update = argumentCaptor<IssueUpdate>()
        verify(issues).create(create.capture())
        verify(issues).update(eq("$QUEUE-1"), update.capture())

        verify(attachments, times(1)).upload(any(), any<ByteArrayInputStream>())

        val issueCreate = create.firstValue
        val issueUpdate = update.firstValue
        val values = issueCreate.values

        assertSoftly {
            values.getOrThrow("queue") shouldBe QUEUE
            values.getOrThrow("summary") shouldBe SUMMARY
            issueUpdate.attachments shouldBe emptyList()
            issueUpdate.comment.get().attachments shouldContain  ATTACHMENT_ID
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
        val partnerInfo = AbstractCreateClaimExecutor.PartnerInfo(
            302,
            "Какая-то ПВЗ из YT 1",
            "ООО Какая-то ПВЗ из LMS 1",
            "Legal адрес ПВЗ из LMS 1"
        )
        consumer.processPayload(
            CreateStartrekIssueForClaimPayload(
                queue = QUEUE,
                summary = SUMMARY,
                claimData = claimService.collectClaimDataForYtOrders(
                    partnerInfo,
                    listOf(
                        ClaimInfo(
                            1, 1001, 302,
                            ClaimPartnerType.PICKUP_POINT,
                            "Подтип",
                            "Какая-то ПВЗ из YT 1",
                            "Адрес ПВЗ из YT 1",
                            "email@email.com",
                            BigDecimal("101.01"),
                            LocalDateTime.parse("2021-12-20T20:00:00.00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                .atZone(DateTimeUtils.MOSCOW_ZONE).toInstant(),
                            LocalDateTime.parse("2021-12-20T20:00:00.00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                .atZone(DateTimeUtils.MOSCOW_ZONE).toInstant(),
                            "Подтип партнера",
                            "Юридическое имя"
                        )
                    ),
                ), partnerInfo = partnerInfo,
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
    @DatabaseSetup("/monitoringevent/consumer/before/claim/all_fields.xml")
    @ExpectedDatabase(
        value = "/monitoringevent/consumer/after/claim/all_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueWithAllFields() {
        doReturn(Issue(null, null, "$QUEUE-1", null, 1, EmptyMap(), null))
            .whenever(issues)
            .create(ArgumentMatchers.any())

        consumer.processPayload(
            CreateStartrekIssueForClaimPayload(
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
            CreateStartrekIssueForClaimPayload(
                queue = QUEUE,
                summary = SUMMARY,
                fields = mapOf(
                    CreateStartrekIssueForClaimPayload.Fields.UNIQUE.key to testUnique,
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
            values.getOrThrow(CreateStartrekIssueForClaimPayload.Fields.UNIQUE.key) shouldBe testUnique

            log shouldContain "${CreateStartrekIssueForClaimPayload.Fields.UNIQUE.key}=$testUnique"
            log shouldContain "There is issue with same unique value. Issue was not created."
        }
    }

    @Test
    @DatabaseSetup("/monitoringevent/consumer/before/claim/before_merch_claim.xml")
    fun createIssueWithAllFieldsForMerchClaim() {
        var QUEUE = "MQMMERCHANTCLAIM"
        var DESCRIPTION = "Претензия включает 4 позиции товаров на сумму : 10.00"
        var AMMOUNT_CLAIMED = "10.00 RUB"
        var TAGS = setOf(CreateStartrekIssueForClaimPayload.Tags.MERCH_CLAIM.name)

        doReturn(Issue(null, null, "MQMMERCHANTCLAIM-1", null, 1, EmptyMap(), null))
            .whenever(issues)
            .create(ArgumentMatchers.any())

        val claim = claimService.findById(11)
        val payload = fbyClaimService.createStartrekIssueForClaimPayload(claim!!, QUEUE, TAGS)

        consumer.processPayload(payload, null)

        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())

        val issueCreate = captor.value
        val values = issueCreate.values

        assertSoftly {
            values.getOrThrow("queue") shouldBe QUEUE
            values.getOrThrow("amountClaimed") shouldBe AMMOUNT_CLAIMED
            values.getOrThrow("description") shouldBe DESCRIPTION
            values.getOrThrow("tags") shouldBe TAGS
        }

    }

    companion object {
        private const val QUEUE = "MONITORINGEVENT"
        private const val SUMMARY = "Something happened today"
        private const val LINKSIZE = 2
        private const val DESCRIPTION = "Happened because of reasons"
    }
}

