package ru.yandex.market.abo.core.partner.debt

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest

class PartnerDebtLoaderTest @Autowired constructor (
    val partnerDebtLoader: PartnerDebtLoader
) : EmptyTest() {

    @Test
    @Disabled
    fun `load from yt`() {
        partnerDebtLoader.load()
    }
}
