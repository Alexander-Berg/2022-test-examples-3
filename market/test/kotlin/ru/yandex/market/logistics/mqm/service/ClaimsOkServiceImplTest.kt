package ru.yandex.market.logistics.mqm.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.bolts.collection.Cf
import ru.yandex.market.logistics.mqm.configuration.properties.ClaimIssueExecutorProperties
import ru.yandex.market.logistics.mqm.entity.AdditionalInfo
import ru.yandex.market.logistics.mqm.entity.Claim
import ru.yandex.market.logistics.mqm.entity.ClaimUnit
import ru.yandex.market.logistics.mqm.entity.enums.ClaimStatus
import ru.yandex.market.logistics.mqm.entity.enums.ClaimType
import ru.yandex.market.logistics.mqm.entity.enums.ClaimUnitStatus
import ru.yandex.market.logistics.mqm.service.ok.OkService
import ru.yandex.market.logistics.mqm.service.ok.OkStatus
import ru.yandex.market.logistics.mqm.service.startrek.StartrekService
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.StatusRef
import ru.yandex.startrek.client.model.Transition

@ExtendWith(MockitoExtension::class)
@DisplayName("Тест для Обработки статусов тикетов в зависимости от типа претензий")
internal class ClaimsOkServiceImplTest {
    @Mock
    private lateinit var startrekService: StartrekService

    @Mock
    private lateinit var claimService: ClaimService

    @Mock
    private lateinit var okService: OkService

    private lateinit var claimOkService: ClaimsOkServiceImpl

    @BeforeEach
    fun setUp() {
        claimOkService = ClaimsOkServiceImpl(
            startrekService = startrekService,
            claimService = claimService,
            okService = okService,
            claimIssueExecutorProperties = ClaimIssueExecutorProperties()
        )
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Проверка перевода статуса Claim с различными состояними согласования")
    fun verifyHandleIssues(claimType: ClaimType, okStatus: OkStatus, claimStatus: ClaimStatus?) {
        val mockedIssue = mock<Issue>()
        val sentStatus = mock<StatusRef>()
        val sentTransition = mock<Transition>()

        val issueLink = "QUEUETEST-0002"
        // Если должно быть проверено, что claimStatus не изменился, то и transition у issue не нужно мокать
        if (claimStatus != null) {
            doReturn(issueLink).whenever(mockedIssue).key
            doReturn(Cf.list(sentTransition)).whenever(mockedIssue).transitions
            doReturn(sentStatus).whenever(sentTransition).to
            doReturn(claimStatus.issueStatus).whenever(sentStatus).key
        }

        val uuid = UUID.fromString("87985da1-5ba0-424c-8ab8-756690715b9d")

        val mockedClaimOpen =
            Claim(
                id = 101,
                type = claimType,
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

        if (claimType != ClaimType.PARTNER_CLAIM) {
            doReturn(okStatus).whenever(okService).getStatus(uuid)
            doReturn(listOf(mockedIssue)).whenever(startrekService).findIssues(any())
        }

        var fbsClaims = emptySet<Claim>()
        var fbyClaims = emptySet<Claim>()
        var merchClaims = emptySet<Claim>()
        when (claimType) {
            ClaimType.FBS_CLAIM -> {
                fbsClaims = setOf(mockedClaimOpen)
            }
            ClaimType.FBY_CLAIM -> {
                fbyClaims = setOf(mockedClaimOpen)
            }
            ClaimType.MERCH_CLAIM -> {
                merchClaims = setOf(mockedClaimOpen)
            }
            else -> {}
        }

        doReturn(fbsClaims)
            .whenever(claimService)
            .findByTypeWithOkAndIssue(eq(ClaimType.FBS_CLAIM))
        doReturn(fbyClaims)
            .whenever(claimService)
            .findByTypeWithOkAndIssue(eq(ClaimType.FBY_CLAIM))
        doReturn(merchClaims)
            .whenever(claimService)
            .findByTypeWithOkAndIssue(eq(ClaimType.MERCH_CLAIM))

        claimOkService.handleIssues()
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
        fun verifyHandleIssues(): Stream<Arguments?>? {
            return Stream.of(
                Arguments.of(ClaimType.FBY_CLAIM, OkStatus.IN_PROGRESS, ClaimStatus.RESPONSES_PROCESSING),
                Arguments.of(ClaimType.FBY_CLAIM, OkStatus.APPROVED, ClaimStatus.PAID),
                Arguments.of(ClaimType.FBY_CLAIM, OkStatus.SUSPENDED, null),
                Arguments.of(ClaimType.FBS_CLAIM, OkStatus.IN_PROGRESS, null),
                Arguments.of(ClaimType.FBS_CLAIM, OkStatus.APPROVED, ClaimStatus.PAID),
                Arguments.of(ClaimType.FBS_CLAIM, OkStatus.CLOSED, null),
                Arguments.of(ClaimType.MERCH_CLAIM, OkStatus.IN_PROGRESS, ClaimStatus.CREATED),
                Arguments.of(ClaimType.MERCH_CLAIM, OkStatus.APPROVED, ClaimStatus.APPROVED),
                Arguments.of(ClaimType.MERCH_CLAIM, OkStatus.REJECTED, null),
                Arguments.of(ClaimType.PARTNER_CLAIM, OkStatus.IN_PROGRESS, null),
                Arguments.of(ClaimType.PARTNER_CLAIM, OkStatus.APPROVED, null),
                Arguments.of(ClaimType.PARTNER_CLAIM, OkStatus.DECLINED, null),
            )
        }
    }
}
