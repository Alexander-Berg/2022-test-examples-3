package ru.yandex.market.doctor.session

import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.doctor.testutils.BaseAppTest
import java.time.LocalDateTime

internal class SessionWatchdogTest : BaseAppTest() {
    @Autowired
    private lateinit var sessionRepository: SessionRepository

    @Autowired
    private lateinit var watchdog: SessionWatchdog

    @Test
    fun `Watchdog closes all stale sessions`() {
        val initial = listOf(
            // Already closed
            Session(
                login = "a",
                started = LocalDateTime.now(),
                finished = LocalDateTime.now().plusMinutes(5),
                query = Query()
            ),
            // Not old enough to close
            Session(
                login = "b",
                started = LocalDateTime.now().minusSeconds(SessionWatchdog.OLDER_THAN_SEC - 10),
                query = Query()
            ),
            // Barely old enough to close
            Session(
                login = "c",
                started = LocalDateTime.now().minusSeconds(SessionWatchdog.OLDER_THAN_SEC + 1),
                query = Query()
            ),
            // Very old
            Session(
                login = "d",
                started = LocalDateTime.now().minusMonths(1),
                query = Query()
            )
        )
        sessionRepository.insertBatch(initial)
        watchdog.finishOldUnfinishedSessions()
        sessionRepository.findAll().sortedBy { it.login } should { sessions ->
            sessions[0] shouldBe initial[0]
            sessions[2].finished shouldNotBe null
            sessions[2].finished!! shouldBeLessThanOrEqualTo LocalDateTime.now()
            sessions[3].finished shouldNotBe null
            sessions[3].finished!! shouldBeLessThanOrEqualTo LocalDateTime.now()
        }
    }
}
