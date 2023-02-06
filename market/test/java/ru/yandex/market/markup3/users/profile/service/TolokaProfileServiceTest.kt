package ru.yandex.market.markup3.users.profile.service

import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.users.profile.TolokaProfileRow
import ru.yandex.market.markup3.users.profile.exception.ProfileNotFoundException
import ru.yandex.market.markup3.users.profile.repository.TolokaProfileRepository

class TolokaProfileServiceTest : CommonTaskTest() {

    companion object {
        const val NON_EXISTENT_WORKER_ID = "trololo"
        const val WORKER_ID = "workerId"
        const val STAFF_LOGIN = "login"
        const val UID = 111L
    }

    @Autowired
    lateinit var tolokaProfileRepository: TolokaProfileRepository

    @Autowired
    lateinit var tolokaProfileService: TolokaProfileService

    @Before
    fun before() {
        tolokaProfileRepository.insert(TolokaProfileRow(WORKER_ID, STAFF_LOGIN, UID))
    }

    @Test
    fun getTolokaProfileByWorkerId() {
        val row = tolokaProfileService.getTolokaProfileByWorkerId(WORKER_ID)
        row.workerId shouldBe WORKER_ID
        row.staffLogin shouldBe STAFF_LOGIN
        row.uid shouldBe UID
    }

    @Test(expected = ProfileNotFoundException::class)
    fun getTolokaProfileByWorkerIdThrows() {
        tolokaProfileService.getTolokaProfileByWorkerId(NON_EXISTENT_WORKER_ID)
    }
}
