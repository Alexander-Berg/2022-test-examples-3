package ru.yandex.market.logistics.les.cache

import java.util.Optional
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.entity.Flag
import ru.yandex.market.logistics.les.entity.enums.FlagKey
import ru.yandex.market.logistics.les.repository.FlagRepository
import ru.yandex.market.logistics.les.service.FlagService

class FlagServiceCacheTest : AbstractContextualTest() {

    @MockBean
    lateinit var flagRepository: FlagRepository

    @Autowired
    lateinit var flagService: FlagService

    @Test
    fun getAllFromCache() {
        whenever(flagRepository.findById(FlagKey.EXAMPLE_FLAG_KEY)).thenReturn(
            Optional.of(
                Flag(
                    FlagKey.EXAMPLE_FLAG_KEY,
                    "some value"
                )
            )
        )

        for (i in 0 until 10) {
            flagService.getValue(FlagKey.EXAMPLE_FLAG_KEY) shouldBe "some value"
        }

        verify(flagRepository, times(1)).findById(FlagKey.EXAMPLE_FLAG_KEY)
    }
}
