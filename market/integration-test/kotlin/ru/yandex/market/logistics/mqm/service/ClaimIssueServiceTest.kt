package ru.yandex.market.logistics.mqm.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.bolts.collection.impl.ArrayListF
import ru.yandex.market.logistics.mqm.dto.ClaimOrderCsvRecord
import ru.yandex.market.logistics.mqm.dto.ClaimOrdersFileName.Companion.FILE_NAME_APPROVAL_REGEX
import ru.yandex.market.logistics.mqm.xlsx.reader.ClaimOrdersFileReader
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.UserRef
import java.time.Clock
import java.time.Instant

@DisplayName("Тесты для ClaimIssueService")
@ExtendWith(MockitoExtension::class)
internal class ClaimIssueServiceTest {

    private lateinit var claimIssueService: ClaimIssueService

    @Mock
    private lateinit var clock: Clock

    @BeforeEach
    fun setUp() {
        val claimOrdersParser = ClaimOrdersFileReader()
        claimIssueService = ClaimIssueService(claimOrdersParser, clock)
    }

    @Test
    @DisplayName("Тест получения только файло заказов отсотртивонных по дате создания")
    fun getOrderAttachmentsByCreatedDate() {
        val attachments = ArrayListF.valueOf(
            arrayOf(
                buildAttachment(
                    "2022-01-20_949494.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    org.joda.time.Instant.parse("2022-01-20T18:00:00.00Z")),
                buildAttachment(
                    "2022-01-15_949494.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    org.joda.time.Instant.parse("2022-01-15T18:00:00.00Z")),
                buildAttachment(
                    "2022-01-07_949494.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    org.joda.time.Instant.parse("2022-01-07T18:00:00.00Z")),
            ))

        val issue = mock<Issue>()
        whenever(issue.attachments).thenReturn(attachments.iterator())
        val orderAttachmentByDate = claimIssueService.getOrderAttachmentsByCreatedDate(issue, FILE_NAME_APPROVAL_REGEX, Instant.MIN)

        assertSoftly {
            orderAttachmentByDate.size shouldBe 3
            orderAttachmentByDate.first()?.name shouldBe "2022-01-07_949494.xlsx"
            orderAttachmentByDate.last()?.name shouldBe "2022-01-20_949494.xlsx"
            orderAttachmentByDate.last()?.mimetype shouldBe "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        }
    }

    @Test
    @DisplayName("Тест конвертации строк csv файла")
    fun convertToCsvRecords() {
        val convertedRecord = claimIssueService.convertToCsvRecords(listOf(ClaimOrderCsvRecord(
            "orderId",
            "shipmentDate",
            "100",
            "MARKETFAULT",
            "MARKET",
            "address",
            "8",
            "9",
            "10",
            "11"
        ))).get(0)


        assertSoftly {
            convertedRecord shouldNotBe null
            convertedRecord.get("orderId") shouldBe "orderId"
            convertedRecord.get("shipmentDate") shouldBe "shipmentDate"
            convertedRecord.get("cost") shouldBe "100"
            convertedRecord.get("previousStatus") shouldBe "MARKETFAULT"
            convertedRecord.get("deliveryService") shouldBe "MARKET"
        }
    }

    @Test
    @DisplayName("Тест получения только файлов от пользователей, робот яндекса не считаются")
    fun getOrderAttachmentsByUserRef() {
        val attachments = ArrayListF.valueOf(
            arrayOf(
                buildAttachment(
                    "2022-01-20_949494.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    org.joda.time.Instant.parse("2022-01-20T18:00:00.00Z"),
                    "zomb-prj-191"),
                buildAttachment(
                    "2022-01-20_949494.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    org.joda.time.Instant.parse("2022-01-20T18:00:00.00Z"),
                    "zomb-prj-12314145194"),
            ))

        val issue = mock<Issue>()
        whenever(issue.attachments).thenReturn(attachments.iterator())
        val orderAttachmentByDate = claimIssueService.getOrderAttachmentsByCreatedDate(issue, FILE_NAME_APPROVAL_REGEX, Instant.MIN)

        assertSoftly {
            orderAttachmentByDate.size shouldBe 0
        }
    }

    @Test
    @DisplayName("Тест получения только файло заказов отсотртивонных по дате создания и с файлом от робота")
    fun getOrderAttachmentsByCreatedDateWithUserIdFiltration() {
        val attachments = ArrayListF.valueOf(
            arrayOf(
                buildAttachment(
                    "2022-01-20_949494.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    org.joda.time.Instant.parse("2022-01-20T18:00:00.00Z"),
                    "someId"),
                buildAttachment(
                    "2022-01-15_949494.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    org.joda.time.Instant.parse("2022-01-15T18:00:00.00Z"),
                    "someId2"),
                buildAttachment(
                    "2022-01-07_949494.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    org.joda.time.Instant.parse("2022-01-07T18:00:00.00Z"),
                    "someId3"),
                buildAttachment(
                    "2022-01-07_949494.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    org.joda.time.Instant.parse("2022-01-07T18:00:00.00Z"),
                    "zomb-prj-191"),
            ))

        val issue = mock<Issue>()
        whenever(issue.attachments).thenReturn(attachments.iterator())
        val orderAttachmentByDate = claimIssueService.getOrderAttachmentsByCreatedDate(issue, FILE_NAME_APPROVAL_REGEX, Instant.MIN)

        assertSoftly {
            orderAttachmentByDate.size shouldBe 3
            orderAttachmentByDate.first()?.name shouldBe "2022-01-07_949494.xlsx"
            orderAttachmentByDate.last()?.name shouldBe "2022-01-20_949494.xlsx"
            orderAttachmentByDate.last()?.mimetype shouldBe "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        }
    }

    @Test
    @DisplayName("Тест генерации нового имени файла")
    fun getNextFileName() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-01-20T18:00:00.00Z"))
        val nextFileName = claimIssueService.getNextFileName(
            "2022-01-20_5698.xlsx",
            "5698",
            FILE_NAME_APPROVAL_REGEX)

        assertSoftly {
            nextFileName shouldBe "2022-01-20_5698_1.xlsx"
        }
    }

    private fun buildAttachment(
        fileName: String,
        mimeType: String,
        createdAt: org.joda.time.Instant,
        userId: String = "someUser"
    ): Attachment {
        val attachmentMock = mock<Attachment>()
        val userRefMock = mock<UserRef>()
        whenever(attachmentMock.createdAt).thenReturn(createdAt)
        whenever(attachmentMock.name).thenReturn(fileName)
        whenever(attachmentMock.mimetype).thenReturn(mimeType)
        whenever(userRefMock.id).thenReturn(userId)
        whenever(attachmentMock.createdBy).thenReturn(userRefMock)

        return attachmentMock
    }
}
