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
import java.util.Optional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.ff.grid.reader.excel.XlsxGridReader
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.mqm.configuration.CacheConfiguration
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import ru.yandex.market.logistics.mqm.utils.getAsScalarUpdate
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate

class PartnerLrmReturnSegmentPvzAggregatedProcessorTest : StartrekProcessorTest() {
    @Captor
    lateinit var issueCreateCaptor: ArgumentCaptor<InputStream>

    @Autowired
    lateinit var lmsClient: LMSClient

    @Autowired
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2020-11-02T09:30:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        cacheManager.getCache(CacheConfiguration.LMS_INFO)?.clear()
    }

    @DisplayName("???????????????? ???????????? ?????? ???????????? ?????????????????????????? ????????-??????????")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/partner_lrm_return_segment_pvz/create_single.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/partner_lrm_return_segment_pvz/create_single.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createIssueTest() {
        val attachment = Mockito.mock(Attachment::class.java)
        whenever(issues .create(ArgumentMatchers.any()))
            .thenReturn(
                Issue(null, null, "MQMTESTRETURN-1", null, 1, EmptyMap(), null)
            )
        whenever(attachments.upload(ArgumentMatchers.anyString(), issueCreateCaptor.capture())).thenReturn(attachment)
        whenever(lmsClient.getPartner(eq(301))).thenReturn(
            Optional.of(PartnerResponse
                .newBuilder()
                .readableName("Partner from LMS")
                .build()))
        whenever(lmsClient.getPartner(eq(172))).thenReturn(
            Optional.of(PartnerResponse
                .newBuilder()
                .readableName("Partner to LMS")
                .build()))

        handleGroups()
        val captor = ArgumentCaptor.forClass(IssueCreate::class.java)
        verify(issues).create(captor.capture())
        verify(attachments).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any())
        val issueValues = captor.value.values
        val read = XlsxGridReader().read(issueCreateCaptor.value)
        assertSoftly {
            issueValues.getOrThrow("summary") shouldBe "[MQM] 01-11-2020: ?????? 172 ?????????????? ???? ???????????????? ????????????"
            issueValues.getOrThrow("description") shouldBe "???????????? ?????????????? ?? ???????????????????? (??????-???? ??????????????: 3)\nORDER1,ORDER2,ORDER3"
            issueValues.getOrThrow("queue") shouldBe "MQMTESTRETURN"
            issueValues.getOrThrow("tags") shouldBe arrayOf("Partner to LMS:172", "Partner from LMS:301")
            read!!.numberOfRows shouldBe 3
            read.numberOfColumns shouldBe 12
        }
    }

    @DisplayName("???????????????????? ???????????????????????? ?? ???????????? Startrek ?????? ???????????????????????? ????????-????????????")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/partner_lrm_return_segment_pvz/comment_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/partner_lrm_return_segment_pvz/comment_with_some_planfacts.xml",
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
                "???????????????????? ?? ???????????? ???????? ?????????????????????????? ????????????????.",
                "",
                "?????????????? ???????????????????????? ???????????? (1 ????.): ORDER2.",
                "?????????????????? ?????????? ???????????? (1 ????.): ORDER3.",
                "???????????? ?????????????? ?? ???????????????????? (2 ????.)."
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
            read.numberOfColumns shouldBe 12
        }
    }

    @DisplayName("???????????????? ???????????? Startrek ?????? ???????????????????????? ????????-????????????")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/partner_lrm_return_segment_pvz/close_all_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/partner_lrm_return_segment_pvz/close_all_planfacts.xml",
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
            commentString shouldBe "?????????? ?????????????????????????? ????????????."
            values.getAsScalarUpdate("resolution") shouldBe "can'tReproduce"
        }
    }
}

