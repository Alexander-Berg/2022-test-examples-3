package ru.yandex.market.logistics.mqm.tms

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.io.ByteArrayInputStream
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.Cf
import ru.yandex.market.logistics.mqm.dto.ClaimOrderCsvRecord
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import ru.yandex.market.logistics.mqm.xlsx.reader.ClaimOrdersFileReader
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.SearchRequest
import ru.yandex.startrek.client.model.StatusRef
import ru.yandex.startrek.client.model.Transition
import ru.yandex.startrek.client.model.UserRef

@DisplayName("Проверка работы модификации позиций претензии")
class ClaimEditingExecutorTest : StartrekProcessorTest()  {
    @Autowired
    private lateinit var claimEditingExecutor: ClaimsForCompensationExecutor

    @Autowired
    private lateinit var claimOrdersFileReader: ClaimOrdersFileReader


    @BeforeEach
    fun setup() {
        clock.setFixed(Instant.parse("2021-05-01T22:00:00Z"), ZoneId.systemDefault())
    }

    @Test
    @DisplayName("Проверка редактирования позиций и претензий")
    @DatabaseSetup("/service/claim/claim-before-editing.xml")
    @ExpectedDatabase(value = "/service/claim/claim-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun testFilter() {
        val statusRef = Mockito.mock(StatusRef::class.java)
        whenever(statusRef.key).thenReturn("priorApproval")


        val sentTransition = mock<Transition>()
        val sentStatus = mock<StatusRef>()

        doReturn(sentStatus)
            .whenever(sentTransition)
            .to

        doReturn("approved")
            .whenever(sentStatus)
            .key
        val buildAttachment = buildAttachment(
            "2022-01-20_123.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )

        whenever(issues.find(ArgumentMatchers.any<SearchRequest>()))
            .thenReturn(
                Cf.wrap(listOf(Issue(null, null, "MQMCLAIM-123", null, 1,
                    Cf.wrap(mapOf("status" to statusRef, "transitions" to sentTransition,"statusStartTime" to org.joda.time.Instant.ofEpochMilli(1) )), startrekSession)))
                    .iterator()
            )

        doReturn(Cf.list(sentTransition))
            .whenever(transitions)
            .getAll(any<Issue>())
        doReturn(Cf.list(buildAttachment).iterator())
            .whenever(attachments)
            .getAll(any<Issue>())
        doReturn(listOf(
            ClaimOrderCsvRecord("1",  "2001-05-11T06:49:46Z", "42", "COMPENSATE_SD", "6", "7","8", "9", "10", "11"),
            ClaimOrderCsvRecord("2",  "2001-05-11T06:49:46Z", "43", "MARKET_FAULT", "6", "7","8", "9", "10", "11"),
            ClaimOrderCsvRecord("3",  "2001-05-11T06:49:46Z", "44", "DELETED", "6", "7","8", "9", "10", "11"),
            ClaimOrderCsvRecord("4",  "2001-05-11T06:49:46Z", "44", "DELETED", "6", "7","8", "9", "10", "11"),
        )).whenever(claimOrdersFileReader).read(any())
        claimEditingExecutor.run()
    }

    private fun buildAttachment(fileName: String, mimeType: String): Attachment {
        val attachmentMock = mock<Attachment>()

        whenever(attachmentMock.name).thenReturn(fileName)
        whenever(attachmentMock.mimetype).thenReturn(mimeType)
        whenever(attachmentMock.createdAt).thenReturn(org.joda.time.Instant.now())
        val userRefMock = mock<UserRef>()
        whenever(userRefMock.id).thenReturn("userId")
        whenever(attachmentMock.createdBy).thenReturn(userRefMock)

        whenever(attachmentMock.download(any()))
            .thenAnswer {
                val argument = it.getArgument(0, OutputStream::class.java)
                val inputStream = ByteArrayInputStream(File.createTempFile("temp", ".csv").readBytes())
                inputStream.use {
                    argument.write(inputStream.readAllBytes())
                }
            }

        return attachmentMock
    }
}
