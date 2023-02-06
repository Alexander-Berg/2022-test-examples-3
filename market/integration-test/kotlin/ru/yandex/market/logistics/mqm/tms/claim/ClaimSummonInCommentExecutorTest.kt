package ru.yandex.market.logistics.mqm.tms.claim

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.configuration.properties.ClaimIssueExecutorProperties
import ru.yandex.market.logistics.mqm.service.ClaimService
import ru.yandex.market.logistics.mqm.service.startrek.StartrekService
import ru.yandex.startrek.client.model.CommentCreate
import ru.yandex.startrek.client.model.Issue

@DisplayName("Тест джобы призыв людей в тикеты, где готова выплата")
class ClaimSummonInCommentExecutorTest : AbstractContextualTest() {
    @Autowired
    lateinit var prop: ClaimIssueExecutorProperties

    @Autowired
    lateinit var claimService: ClaimService

    @Mock
    lateinit var startrekService: StartrekService

    @Mock
    lateinit var executor: ClaimSummonInCommentExecutor

    @BeforeEach
    fun beforEach() {
        executor = ClaimSummonInCommentExecutor(
            claimService,
            startrekService,
            prop
        )
    }
    @Test
    @DisplayName("Проверка создания комментария с согласованной суммой компенсацией со списком призванных людей ")
    @DatabaseSetup("/tms/claim/claimSummonInComment/before.xml")
    fun successTicketCreation() {
        clock.setFixed(Instant.parse("2021-12-20T20:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)

        var mockedIssue = mock<Issue>();

        doReturn("MQMCLAIM-123")
            .whenever(mockedIssue)
            .key

        doReturn(mockedIssue)
            .whenever(startrekService)
            .getIssue(any())

        executor.run()

        val commentCaptor = argumentCaptor<CommentCreate>()
        verify(startrekService, times(1)).createComments(any(), commentCaptor.capture())


        val comment = commentCaptor.firstValue.comment.get()
        val summons = commentCaptor.firstValue.summonees.sorted().joinToString(",")
        assertSoftly {
            comment shouldBe "По претензии сформирована выплата 42.00 рублей"
            summons shouldBe "login-user1,login1"
        }
    }
}
