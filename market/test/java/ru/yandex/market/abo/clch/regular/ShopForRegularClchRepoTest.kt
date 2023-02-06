package ru.yandex.market.abo.clch.regular

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.clch.regular.model.RegularClchReason.NEW_REQUISITES
import ru.yandex.market.abo.clch.regular.model.ShopForRegularClch
import ru.yandex.market.abo.clch.regular.service.ShopForRegularClchRepo

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 02.11.2021
 */
class ShopForRegularClchRepoTest @Autowired constructor(
    private val shopForRegularClchRepo: ShopForRegularClchRepo
): EmptyTest() {
    @BeforeEach
    fun init() {
        shopForRegularClchRepo.save(
            ShopForRegularClch(SHOP_ID, LocalDateTime.now(), arrayOf(NEW_REQUISITES), null)
        )
        flushAndClear()
    }

    @Test
    fun `update processed time test`() {
        shopForRegularClchRepo.updateProcessedTime(SHOP_ID, LocalDateTime.now())
        flushAndClear()
        assertTrue(shopForRegularClchRepo.findAllByProcessedTimeIsNull().isEmpty())
    }

    companion object {
        private const val SHOP_ID = 123L
    }
}
