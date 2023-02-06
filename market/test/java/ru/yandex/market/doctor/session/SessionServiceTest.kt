package ru.yandex.market.doctor.session

import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.doctor.testutils.BaseAppTest
import ru.yandex.market.mbo.lightmapper.exceptions.ItemNotFoundException
import java.time.LocalDateTime
import java.util.UUID

internal class SessionServiceTest : BaseAppTest() {

    @Autowired
    private lateinit var sessionService: SessionService

    @Autowired
    private lateinit var sessionRepository: SessionRepository

    @Test
    fun `Create session`() {
        val query = Query(
            123,
            "456",
            null,
            111,
            222,
            null,
            "3456"
        )

        sessionRepository.findAll() shouldBe beEmpty<Session>()
        sessionService.createSession("12345", query.copy())
        sessionRepository.findAll() shouldHaveSize 1
        sessionRepository.findAll().first()!! should {
            it.id != UUID.fromString("00000000-0000-0000-0000-000000000000")
                && it.login == "12345"
                && it.query == query
        }
    }

    @Test
    fun `Update existing session`() {
        val original = sessionRepository.insert(Session(
            id = UUID.randomUUID(),
            login = "storm spirit",
            started = LocalDateTime.parse("2021-12-17T07:31:24"),
            query = Query(
                businessId = 999,
                shopSku = "ssku",
                shopId = 123,
                feedId = 444
            )
        ))
        val expected = original.copy(
            finished = LocalDateTime.parse("2021-12-18T07:31:24"),
            checks = listOf(
                Check("ERROR", "111", "Error message", "MARKET_IDX"),
                Check("INFO", "222", "Info message", "MARKET_IDX"),
            ),
            feedback = Feedback(
                rating = 10,
                message = "so good",
            )
        )
        val fromUpdate = sessionService.updateSession(original.id) {
            it.copy(
                finished = expected.finished,
                checks = expected.checks,
                feedback = expected.feedback
            )
        }
        val fromDb = sessionRepository.findById(original.id)

        fromUpdate shouldBe expected
        fromDb shouldBe expected
    }

    @Test(expected = ItemNotFoundException::class)
    fun `Fails on update non-existing session`() {
        sessionService.updateSession(UUID.randomUUID()) { it }
    }
}
