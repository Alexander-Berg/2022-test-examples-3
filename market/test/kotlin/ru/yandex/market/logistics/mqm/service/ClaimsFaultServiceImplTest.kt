package ru.yandex.market.logistics.mqm.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.bolts.collection.Cf
import ru.yandex.market.logistics.mqm.configuration.properties.ClaimIssueExecutorProperties
import ru.yandex.market.logistics.mqm.dto.ClaimOrderCsvRecord
import ru.yandex.market.logistics.mqm.entity.AdditionalInfo
import ru.yandex.market.logistics.mqm.entity.Claim
import ru.yandex.market.logistics.mqm.entity.ClaimUnit
import ru.yandex.market.logistics.mqm.entity.enums.ClaimStatus
import ru.yandex.market.logistics.mqm.entity.enums.ClaimType
import ru.yandex.market.logistics.mqm.entity.enums.ClaimUnitStatus
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssueForClaimPayload
import ru.yandex.market.logistics.mqm.service.monitoringevent.MonitoringEventService
import ru.yandex.market.logistics.mqm.service.ok.OkService
import ru.yandex.market.logistics.mqm.service.ok.OkStatus
import ru.yandex.market.logistics.mqm.service.startrek.StartrekService
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.CommentCreate
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.StatusRef
import ru.yandex.startrek.client.model.Transition

@ExtendWith(MockitoExtension::class)
@DisplayName("Тест для Создания тикетов в маркет операции")
internal class ClaimsFaultServiceImplTest {

    @Mock
    private lateinit var startrekService: StartrekService

    @Mock
    private lateinit var monitoringEventService: MonitoringEventService<CreateStartrekIssueForClaimPayload>

    private lateinit var claimsFaultService:  ClaimsEditingServiceImpl

    private lateinit var claimIssueService: ClaimIssueService
    private lateinit var claimPdfService: ClaimPdfGeneratorService

    @Mock
    private lateinit var claimService: ClaimService

    @Mock
    private lateinit var okService: OkService

    @BeforeEach
    fun setUp() {
        claimIssueService = Mockito.mock(ClaimIssueService::class.java)
        claimPdfService = Mockito.mock(ClaimPdfGeneratorService::class.java)

        claimsFaultService = ClaimsEditingServiceImpl(
            claimIssueService = claimIssueService,
            startrekService = startrekService,
            claimService = claimService,
            approveClaimProperties = ClaimIssueExecutorProperties(
                enableManuallyEditingValidation = true
            ),
            monitoringEventService = monitoringEventService,
            pdfService = claimPdfService,
            okService = okService,
        )
    }

    @Test
    @DisplayName("Проверка корректной обработки тикета с заказами в статусе MARKET_FAULT")
    fun handleIssues() {
        val mockedIssue = mock<Issue>()

        doReturn("MQMCLAIMTEST-1")
            .whenever(mockedIssue)
            .key
        val mockedIssue2 = mock<Issue>()

        doReturn("MQMCLAIMTEST-2")
            .whenever(mockedIssue2)
            .key

        val sentTransition = mock<Transition>()
        val sentStatus = mock<StatusRef>()
        doReturn(Cf.list(sentTransition))
            .whenever(mockedIssue)
            .transitions

        doReturn(Cf.list(sentTransition))
            .whenever(mockedIssue2)
            .transitions

        doReturn(sentStatus)
            .whenever(sentTransition)
            .to

        doReturn("indemnificationWait")
            .whenever(sentStatus)
            .key
        val buildAttachment = buildAttachment(
            "2022-01-20_949494.csv",
            "text/csv",
            org.joda.time.Instant.parse("2022-01-20T18:00:00.00Z")
        )

        whenever(claimIssueService.getOrderAttachmentsByCreatedDate(any(), any(), any())).thenReturn(listOf(buildAttachment))
        whenever(claimIssueService.getClaimOrders(any()))
            .thenReturn(listOf(ClaimOrderCsvRecord("1", "3", "4", "MARKET_FAULT", "6", "address", "7", "8", "9","11")))

        doReturn(listOf(mockedIssue, mockedIssue2))
            .whenever(startrekService)
            .findIssues(any())


        claimsFaultService.handleIssues()

        val createCaptor = argumentCaptor<CreateStartrekIssueForClaimPayload>()
        val updateCaptor = argumentCaptor<IssueUpdate>()
        val commentCaptor = argumentCaptor<CommentCreate>()
        verify(monitoringEventService, times(4)).pushEvent(any(), createCaptor.capture())
        verify(startrekService, times(8)).updateIssue(any(), updateCaptor.capture())
        verify(startrekService, times(8)).createComments(any(), commentCaptor.capture())

        val createIssuePayload = createCaptor.firstValue
        val issueUpdate = updateCaptor.firstValue
        val commentCreate = commentCaptor.firstValue
        val agreedSum = createIssuePayload.fields?.get(CreateStartrekIssueForClaimPayload.Fields.AGREED_COMPENSATION_SUM.key)
            ?: ""
        val tags = createIssuePayload.tags.first()
        assertSoftly {
            agreedSum shouldBe 8.0
            tags shouldContain CreateStartrekIssueForClaimPayload.Tags.NOT_RETURNED_CLAIM.name
            issueUpdate.values.keys() shouldContain  "amountOfPayment"
            commentCreate.comment.get() shouldBe "Все позиции претензии были удалены. Сумма компенсации установлена равной 0."
        }
        assertSoftly {
            createIssuePayload.issueLinks shouldBe listOf("MQMCLAIMTEST-1", "MQMCLAIMTEST-2")
        }
    }

    @Test
    @DisplayName("Проверка корректной обработки тикета с заказами в статусе Deleted")
    fun handleIssuesDeleted() {
        val mockedIssue = mock<Issue>()

        doReturn("MQMCLAIMTEST-1")
            .whenever(mockedIssue)
            .key

        val mockedIssue2 = mock<Issue>()

        doReturn("MQMCLAIMTEST-2")
            .whenever(mockedIssue2)
            .key

        val sentTransition = mock<Transition>()
        val sentStatus = mock<StatusRef>()
        doReturn(Cf.list(sentTransition))
            .whenever(mockedIssue)
            .transitions

        doReturn(Cf.list(sentTransition))
            .whenever(mockedIssue2)
            .transitions

        doReturn(sentStatus)
            .whenever(sentTransition)
            .to

        doReturn("indemnificationWait")
            .whenever(sentStatus)
            .key
        val buildAttachment = buildAttachment(
            "2022-01-20_949494.csv",
            "text/csv",
            org.joda.time.Instant.parse("2022-01-20T18:00:00.00Z")
        )

        whenever(claimIssueService.getOrderAttachmentsByCreatedDate(any(), any(), any())).thenReturn(listOf(buildAttachment))
        whenever(claimIssueService.getClaimOrders(any()))
            .thenReturn(listOf(ClaimOrderCsvRecord("1", "3", "4", "DELETED", "6", "address", "7", "8", "9","11")))

        doReturn(listOf(mockedIssue, mockedIssue2))
            .whenever(startrekService)
            .findIssues(any())


        claimsFaultService.handleIssues()

        val createCaptor = argumentCaptor<CreateStartrekIssueForClaimPayload>()
        val updateCaptor = argumentCaptor<IssueUpdate>()
        verify(monitoringEventService, never()).pushEvent(any(), createCaptor.capture())
        verify(startrekService, times(8)).updateIssue(any(), updateCaptor.capture())

        val issueUpdate = updateCaptor.firstValue
        assertSoftly {
            issueUpdate.values.keys() shouldContain  "amountOfPayment"
        }
    }

    @Test
    @DisplayName("Проверка перехода в статус и начала процесса согласования")
    fun handleIssuesAndStartApprovement() {
        val mockedIssue = mock<Issue>()

        doReturn("MQMCLAIMTEST-1")
            .whenever(mockedIssue)
            .key

        doReturn(UUID.fromString("c6152fcf-94fc-4046-b80c-9fa3f59d1cd1"))
            .whenever(okService)
            .createApprovement(any())

        val mockedClaim = Claim(
            id = 100,
            status = ClaimStatus.CREATED,
            type = ClaimType.PARTNER_CLAIM,
            claimUnits = listOf(ClaimUnit(
                id = 200,
                status = ClaimUnitStatus.MARKET_FAULT,
                additionalInfo = listOf(AdditionalInfo(
                    orderId = "1",
                    vendorCode = "vendor1",
                ))
            ))
        )

        doReturn(mockedClaim)
            .whenever(claimService)
            .findClaimByIssue(mockedIssue)

        val sentTransition = mock<Transition>()
        val sentStatus = mock<StatusRef>()
        doReturn(Cf.list(sentTransition))
            .whenever(mockedIssue)
            .transitions

        doReturn(sentStatus)
            .whenever(sentTransition)
            .to

        doReturn("approved")
            .whenever(sentStatus)
            .key
        val buildAttachment = buildAttachment(
            "2022-01-20_949494.csv",
            "text/csv",
            org.joda.time.Instant.parse("2022-01-20T18:00:00.00Z")
        )

        whenever(claimIssueService.getOrderAttachmentsByCreatedDate(any(), any(), any())).thenReturn(listOf(buildAttachment))
        whenever(claimIssueService.getClaimOrders(any()))
            .thenReturn(listOf(ClaimOrderCsvRecord("1", "3", "4", "MARKET_FAULT", "6", "address", "7", "8", "9","11")))

        doReturn(listOf(mockedIssue))
            .whenever(startrekService)
            .findIssues(any())

        claimsFaultService.handleIssues()

        val createCaptor = argumentCaptor<CreateStartrekIssueForClaimPayload>()
        verify(monitoringEventService, times(4)).pushEvent(any(), createCaptor.capture())
        verify(sentTransition, times(1)).execute(mockedIssue)
        verify(okService, times(1)).createApprovement(any())
        verify(claimService, times(1)).updateOkId(
            "MQMCLAIMTEST-1",
            UUID.fromString("c6152fcf-94fc-4046-b80c-9fa3f59d1cd1"))

        assertSoftly {
            createCaptor.firstValue.issueLinks shouldBe listOf("MQMCLAIMTEST-1")
        }
    }

    @Test
    @DisplayName("Проверка перехода в статус и начала процесса согласования с уже существующим согласованием")
    fun handleIssuesAndStartApprovementWithExistingOkId() {
        val mockedIssue = mock<Issue>()
        doReturn("MQMCLAIMTEST-1")
            .whenever(mockedIssue)
            .key

        val sentStatus = mock<StatusRef>()
        val sentTransition = mock<Transition>()
        doReturn(Cf.list(sentTransition))
            .whenever(mockedIssue)
            .transitions

        doReturn(sentStatus)
            .whenever(sentTransition)
            .to

        doReturn("approved")
            .whenever(sentStatus)
            .key
        val buildAttachment = buildAttachment(
            "2022-01-20_949494.csv",
            "text/csv",
            org.joda.time.Instant.parse("2022-01-20T18:00:00.00Z")
        )

        val exisitingOkUuid = UUID.fromString("c6000aaa-94fc-4046-b80c-9fa3f59d1cd1")
        val mockedClaim = Claim(
            id = 100,
            status = ClaimStatus.CREATED,
            type = ClaimType.PARTNER_CLAIM,
            ok_uuid = exisitingOkUuid,
            claimUnits = listOf(ClaimUnit(
                id = 200,
                status = ClaimUnitStatus.MARKET_FAULT,
                additionalInfo = listOf(AdditionalInfo(
                    orderId = "1",
                    vendorCode = "vendor1",
                ))
            ))
        )

        doReturn(UUID.fromString("c6152fcf-94fc-4046-b80c-9fa3f59d1cd1"))
            .whenever(okService)
            .createApprovement(any())

        doReturn(mockedClaim)
            .whenever(claimService)
            .findClaimByIssue(mockedIssue)

        whenever(claimIssueService.getOrderAttachmentsByCreatedDate(any(), any(), any())).thenReturn(listOf(buildAttachment))
        whenever(claimIssueService.getClaimOrders(any()))
            .thenReturn(listOf(ClaimOrderCsvRecord("1", "3", "4", "MARKET_FAULT", "6", "address", "7", "8", "9","11")))

        doReturn(listOf(mockedIssue))
            .whenever(startrekService)
            .findIssues(any())
        claimsFaultService.handleIssues()

        val createCaptor = argumentCaptor<CreateStartrekIssueForClaimPayload>()
        verify(monitoringEventService, times(4)).pushEvent(any(), createCaptor.capture())
        verify(sentTransition, times(1)).execute(mockedIssue)
        verify(okService, times(1)).createApprovement(any())
        verify(okService, times(1)).closeApprovement(exisitingOkUuid)
        verify(claimService, times(1)).updateOkId(any(), any())

        val createIssuePayload = createCaptor.firstValue
        assertSoftly {
            createIssuePayload.issueLinks shouldBe listOf("MQMCLAIMTEST-1")
        }
    }

    @Test
    @DisplayName("Проверка поврежденного файла в тикете и остановка дальнейшего процесса")
    fun detectManuallyEditingCommentTable() {
        val mockedIssue = mock<Issue>()
        doReturn("MQMCLAIMTEST-1")
            .whenever(mockedIssue)
            .key

        val sentStatus = mock<StatusRef>()
        val sentTransition = mock<Transition>()
        doReturn(Cf.list(sentTransition))
            .whenever(mockedIssue)
            .transitions

        doReturn(sentStatus)
            .whenever(sentTransition)
            .to

        doReturn("approved")
            .whenever(sentStatus)
            .key
        val buildAttachment = buildAttachment(
            "2022-01-20_949494.csv",
            "text/csv",
            org.joda.time.Instant.parse("2022-01-20T18:00:00.00Z")
        )

        val exisitingOkUuid = UUID.fromString("c6000aaa-94fc-4046-b80c-9fa3f59d1cd1")
        val mockedClaim = Claim(
            id = 100,
            status = ClaimStatus.CREATED,
            type = ClaimType.PARTNER_CLAIM,
            ok_uuid = exisitingOkUuid,
            claimUnits = listOf(ClaimUnit(
                id = 200,
                status = ClaimUnitStatus.MARKET_FAULT,
                additionalInfo = listOf(AdditionalInfo(
                    orderId = "300",
                    vendorCode = "vendor1",
                ))
            ),
                ClaimUnit(
                    id = 201,
                    status = ClaimUnitStatus.DELETED,
                    additionalInfo = listOf(AdditionalInfo(
                        orderId = "301",
                        vendorCode = "vendor1",
                    ))
                ),
                ClaimUnit(
                    id = 202,
                    status = ClaimUnitStatus.DELETED,
                    additionalInfo = listOf(AdditionalInfo(
                        orderId = "302",
                        vendorCode = "vendor1",
                    ))
                ),
                ClaimUnit(
                    id = 203,
                    status = ClaimUnitStatus.MARKET_FAULT,
                    additionalInfo = listOf(AdditionalInfo(
                        orderId = "303",
                        vendorCode = "vendor1",
                    ))
                ))
        )

        doReturn(mockedClaim)
            .whenever(claimService)
            .findClaimByIssue(mockedIssue)

        whenever(claimIssueService.getOrderAttachmentsByCreatedDate(any(), any(), any())).thenReturn(listOf(
            buildAttachment))
        whenever(claimIssueService.getClaimOrders(any()))
            .thenReturn(listOf(
                ClaimOrderCsvRecord("300", "3", "4", "DELETED", "6", "address", "7", "8", "9", "11"),
                ClaimOrderCsvRecord("301", "3", "4", "MARKET_FAULT", "6", "address", "7", "8", "9", "11"),
                ClaimOrderCsvRecord("303", "3", "4", "MARKET_FAULT", "6", "address", "7", "8", "9", "11"),
                ClaimOrderCsvRecord("307", "3", "4", "MARKET_FAULT", "6", "address", "7", "8", "9", "11"),
            ))

        doReturn(listOf(mockedIssue))
            .whenever(startrekService)
            .findIssues(any())

        claimsFaultService.handleIssues()

        val commentCaptor = argumentCaptor<CommentCreate>()
        verify(startrekService, times(4)).createComments(any(), commentCaptor.capture())
        verify(sentTransition, times(0)).execute(mockedIssue)
        verify(okService, times(0)).createApprovement(any())
        verify(okService, times(0)).closeApprovement(exisitingOkUuid)
        verify(claimService, times(0)).updateOkId(any(), any())

        val comment = commentCaptor.firstValue.comment.toString()
        assertSoftly {
            comment shouldContain "В прикрепляемом документе не найдены следующие позиции: 302"
            comment shouldContain "Следующие позиции были перемещены в статус DELETED: 300"
            comment shouldContain "В прикрепляемом документе добавлены не заявленные позиции: 307"
        }
    }


    @ParameterizedTest
    @MethodSource
    @DisplayName("Проверка перевода статуса Claim с различными состояними согласования")
    fun updateClaimStatusDependsOnOkStatus(transitionKey: String, okStatus: OkStatus, claimStatus: ClaimStatus?) {
        val mockedIssue = mock<Issue>()
        val sentStatus = mock<StatusRef>()
        val sentTransition = mock<Transition>()

        // Если должно быть проверено, что claimStatus не изменился, то и transition у issue не нужно мокать
        if (claimStatus != null) {
            doReturn(Cf.list(sentTransition)).whenever(mockedIssue).transitions
            doReturn(sentStatus).whenever(sentTransition).to
            doReturn(transitionKey).whenever(sentStatus).key
        }

        val uuid = UUID.fromString("87985da1-5ba0-424c-8ab8-756690715b9d")
        val issueLink = "QUEUETEST-0002"
        val mockedClaimOpen =
            Claim(
                id = 101,
                status = claimStatus,
                type = ClaimType.PARTNER_CLAIM,
                ok_uuid = uuid,
                claimUnits = listOf(ClaimUnit(
                    id = 201,
                    status = ClaimUnitStatus.MARKET_FAULT,
                    additionalInfo = listOf(AdditionalInfo(
                        orderId = "1",
                        vendorCode = "vendor1",
                    ))
                )),
                issueLink = issueLink
            )
        doReturn(okStatus).whenever(okService).getStatus(uuid)
        doReturn(mockedIssue).whenever(startrekService).getIssue(issueLink)

        doReturn(setOf(mockedClaimOpen))
            .whenever(claimService)
            .findByOkAndIssueIsNotNull()

        claimsFaultService.handleIssues()
        if (claimStatus != null) {
            verify(sentTransition, times(1)).execute(mockedIssue)
            verify(claimService, times(1)).updateClaimStatus(any(), eq(claimStatus))
        } else {
            verify(sentTransition, times(0)).execute(mockedIssue)
            verify(claimService, times(0)).updateClaimStatus(any(), any())
        }
    }

    companion object {
        @JvmStatic
        fun updateClaimStatusDependsOnOkStatus(): Stream<Arguments?>? {
            return Stream.of(
                Arguments.of("open", OkStatus.IN_PROGRESS, ClaimStatus.CREATED),
                Arguments.of("approved", OkStatus.APPROVED, ClaimStatus.APPROVED),
                Arguments.of("sent", OkStatus.CLOSED, null),
                Arguments.of("sent", OkStatus.REJECTED, null),
                Arguments.of("sent", OkStatus.SUSPENDED, null),
                Arguments.of("sent", OkStatus.DECLINED, null),
            )
        }
    }

    private fun buildAttachment(fileName: String, mimeType: String, createdAt: org.joda.time.Instant): Attachment {
        val attachmentMock = mock<Attachment>()

        return attachmentMock
    }
}

