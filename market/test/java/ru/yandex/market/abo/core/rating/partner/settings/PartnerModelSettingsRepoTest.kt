package ru.yandex.market.abo.core.rating.partner.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.model.PartnerModel

internal class PartnerModelSettingsRepoTest @Autowired constructor(
    private val partnerModelSettingsRepo: PartnerModelSettingsRepo
) : EmptyTest() {

    @Test
    fun `all partner models in db`() {
        assertEquals(
            PartnerModel.values().toHashSet(),
            partnerModelSettingsRepo.findAll().map { it.partnerModel }.toHashSet()
        )
    }
}
