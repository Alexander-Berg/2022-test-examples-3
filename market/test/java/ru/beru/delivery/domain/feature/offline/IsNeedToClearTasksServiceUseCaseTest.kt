package ru.yandex.market.tpl.courier.domain.feature.offline

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import ru.yandex.market.tpl.courier.arch.fp.failure
import ru.yandex.market.tpl.courier.arch.fp.success
import ru.yandex.market.tpl.courier.domain.feature.user.GetCurrentUserUseCase
import ru.yandex.market.tpl.courier.domain.feature.user.userPropertiesTestInstance
import ru.yandex.market.tpl.courier.domain.feature.user.userTestInstance
import ru.yandex.market.tpl.courier.extensions.failureWith
import java.io.IOException

class IsNeedToClearTasksServiceUseCaseTest {

    private val userUseCase: GetCurrentUserUseCase = mockk {
        coEvery { getCurrentUser() } returns success(userTestInstance())
    }
    private val useCase = IsNeedToClearTasksServiceUseCase(userUseCase)

    @Test
    fun `Возвращает false когда у пользователя есть пропертя со значением false`() = runBlockingTest {
        coEvery { userUseCase.getCurrentUser() } returns success(
            userTestInstance(
                properties = userPropertiesTestInstance(needClearOfflineScheduler = false)
            )
        )

        val result = useCase.isNeedToClearOfflineTasks()

        result shouldBe success(false)
    }

    @Test
    fun `Возвращает false когда у пользователя нет проперти`() = runBlockingTest {
        coEvery { userUseCase.getCurrentUser() } returns success(
            userTestInstance(
                properties = userPropertiesTestInstance(
                    needClearOfflineScheduler = null,
                )
            )
        )

        val result = useCase.isNeedToClearOfflineTasks()

        result shouldBe success(false)
    }

    @Test
    fun `Возвращает true когда у пользователя есть пропертя со значением true`() = runBlockingTest {
        coEvery { userUseCase.getCurrentUser() } returns success(
            userTestInstance(
                properties = userPropertiesTestInstance(
                    needClearOfflineScheduler = true,
                )
            )
        )

        val result = useCase.isNeedToClearOfflineTasks()

        result shouldBe success(true)
    }

    @Test
    fun `Фейлится если зафейлился репозиторий`() = runBlockingTest {
        coEvery { userUseCase.getCurrentUser() } returns failure(IOException())

        val result = useCase.isNeedToClearOfflineTasks()

        result shouldBe failureWith<Boolean, Throwable>(beInstanceOf<IOException>())
    }
}