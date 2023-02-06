package ru.yandex.market.abo.core.rating.partner.stat

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.pilot.PilotType
import ru.yandex.market.abo.cpa.order.model.PartnerModel
import java.util.Date

/**
 * @author zilzilok
 */
class DropshipOrderRepoTest @Autowired constructor(
    val dropshipOrderRepo: DropshipOrderRepo
) : EmptyTest() {

    @Test
    fun `test repo`() {
        dropshipOrderRepo.findBadOrders(Date(), Date(), Date(), Date(), PartnerModel.DSBB, PilotType.DSBB_NEW_RATING)
    }
}
