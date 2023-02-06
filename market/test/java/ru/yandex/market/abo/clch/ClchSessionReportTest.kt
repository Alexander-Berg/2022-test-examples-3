package ru.yandex.market.abo.clch

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.clch.repo.ClchSessionSearchRepo


open class ClchSessionReportTest @Autowired constructor(
    val clchSessionSearchRepo: ClchSessionSearchRepo
) : EmptyTest() {

    @Test
    open fun findByShop() {
        clchSessionSearchRepo.findAllByShop(1L, null)
    }
}
