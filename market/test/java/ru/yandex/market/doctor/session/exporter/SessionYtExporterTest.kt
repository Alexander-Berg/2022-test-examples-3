package ru.yandex.market.doctor.session.exporter

import com.fasterxml.jackson.databind.JsonNode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.inside.yt.kosher.cypress.Cypress
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import ru.yandex.inside.yt.kosher.tables.YtTables
import ru.yandex.inside.yt.kosher.transactions.Transaction
import ru.yandex.inside.yt.kosher.transactions.YtTransactions
import ru.yandex.market.doctor.session.Check
import ru.yandex.market.doctor.session.Feedback
import ru.yandex.market.doctor.session.Query
import ru.yandex.market.doctor.session.Session
import ru.yandex.market.doctor.session.SessionRepository
import ru.yandex.market.doctor.session.exporter.TestSessionExporterConfig.Companion.SESSION_STATS_TABLE
import ru.yandex.market.doctor.session.request.SessionRequest
import ru.yandex.market.doctor.session.request.SessionRequestRepository
import ru.yandex.market.doctor.testutils.BaseAppTest
import java.time.LocalDateTime.now
import java.util.UUID

internal class SessionYtExporterTest : BaseAppTest() {
    @Autowired
    private lateinit var ytCypressMock: Cypress

    @Autowired
    private lateinit var ytTransactionsMock: YtTransactions

    @Autowired
    private lateinit var ytTablesMock: YtTables

    @Autowired
    private lateinit var sessionRepository: SessionRepository

    @Autowired
    private lateinit var requestRepository: SessionRequestRepository

    @Autowired
    private lateinit var sessionYtExporter: SessionYtExporter

    @Before
    fun setUp() {
        reset(ytCypressMock)
        reset(ytTransactionsMock)
        reset(ytTablesMock)
        doReturn(mock<Transaction>()).`when`(ytTransactionsMock).startAndGet(any(), any(), any())
    }

    @Test
    fun `Creates new table`() {
        sessionYtExporter.exportOldSessions()
        verify(ytCypressMock, times(1)).create(any())
        verify(ytTablesMock, times(1)).write(
            eq(YPath.simple(SESSION_STATS_TABLE)),
            eq(YTableEntryTypes.JACKSON_UTF8),
            any<Iterator<JsonNode>>()
        )
    }

    @Test
    fun `Writes and deletes batch-by-batch`() {
        val olderThan = now().minusSeconds(SessionYtExporter.OLDER_THAN_SEC)
        val session1 = Session(
            id = UUID.randomUUID(),
            login = "l1",
            started = olderThan.minusDays(1),
            finished = now(),
            query = Query(businessId = 123, shopSku = "ssku"),
            checks = listOf(Check("WARNING", "CODE", "MSG", "SOURCE")),
            feedback = Feedback(10, "MSG")
        )
        val session2 = session1.copy(
            id = UUID.randomUUID(),
            started = olderThan.minusSeconds(10)
        )
        val session3 = session1.copy(
            id = UUID.randomUUID(),
            started = olderThan.minusSeconds(100)
        )
        val sessionNotOldEnough = session1.copy(
            id = UUID.randomUUID(),
            started = olderThan.plusSeconds(10)
        )
        val sessionNotFinished = session1.copy(
            id = UUID.randomUUID(),
            started = olderThan.minusSeconds(10),
            finished = null
        )
        sessionRepository.insertBatch(session1, session2, session3, sessionNotOldEnough, sessionNotFinished)
        requestRepository.insertBatch(
            SessionRequest(
                sessionId = session1.id,
                time = now(),
                url = "url",
                statusCode = 124
            ),
            SessionRequest(
                sessionId = session1.id,
                time = now().minusSeconds(10),
                url = "url2",
                statusCode = 456
            ),
        )

        sessionYtExporter.exportOldSessions()
        verify(ytCypressMock, times(1)).create(any())

        val iteratorCaptor = argumentCaptor<Iterator<JsonNode>>()
        verify(ytTablesMock, times(1)).write(
            eq(YPath.simple(SESSION_STATS_TABLE)),
            eq(YTableEntryTypes.JACKSON_UTF8),
            iteratorCaptor.capture()
        )

        val iterator = iteratorCaptor.firstValue

        iterator.hasNext() shouldBe true
        sessionRepository.findAll() shouldHaveSize 5
        requestRepository.findAll() shouldHaveSize 2

        iterator.next()
        iterator.hasNext() shouldBe true
        sessionRepository.findAll().map { it.id } shouldContainExactlyInAnyOrder setOf(
            session2.id, session3.id, sessionNotOldEnough.id, sessionNotFinished.id
        )
        requestRepository.findAll() shouldHaveSize 0

        iterator.next()
        iterator.hasNext() shouldBe true
        sessionRepository.findAll().map { it.id } shouldContainExactlyInAnyOrder setOf(
            session2.id, sessionNotOldEnough.id, sessionNotFinished.id
        )
        iterator.next()
        iterator.hasNext() shouldBe false
        sessionRepository.findAll().map { it.id } shouldContainExactlyInAnyOrder setOf(
            sessionNotOldEnough.id, sessionNotFinished.id
        )
    }
}
