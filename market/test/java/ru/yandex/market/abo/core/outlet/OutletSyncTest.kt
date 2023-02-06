package ru.yandex.market.abo.core.outlet

import java.util.Date
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.common.util.date.DateUtil
import ru.yandex.market.abo.core.outlet.model.OutletCheck
import ru.yandex.market.abo.core.outlet.model.ext.OutletForModeration
import ru.yandex.market.abo.core.outlet.model.ext.OutletForModerationRepo
import ru.yandex.market.abo.core.outlet.model.status.OutletCheckStatus.CANCELED
import ru.yandex.market.abo.core.outlet.model.status.OutletCheckStatus.NEW
import ru.yandex.market.abo.core.outlet.repo.OutletCheckRepo
import ru.yandex.market.core.campaign.model.CampaignType

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 25.07.2022
 */
class OutletSyncTest @Autowired constructor(
    private val outletCheckRepo: OutletCheckRepo,
    private val outletForModerationRepo: OutletForModerationRepo
) : EmptyTest() {

    /**
     * +-------------+-----+-----+----------+----------------+
     * | mbiOutletId | abo | mbi | актуален | завершён в abo |
     * +-------------+-----+-----+----------+----------------+
     * |           0 | +   | +   | +        | -              |
     * |           1 | +   | +   | -        | -              |
     * |           2 | +   | +   | +        | +              |
     * |           3 | +   | +   | -        | +              |
     * |           4 | +   | -   |          |                |
     * |           5 | -   | +   |          |                |
     * +-------------+-----+-----+----------+----------------+
     */
    @Test
    fun `test outlets for check creation and cancellation`() {
        val actualUpdateTime = Date()
        val oldUpdateTime = DateUtil.addDay(actualUpdateTime, -1)
        val aboChecks = listOf(
            createOpenCheck(0, actualUpdateTime),
            createOpenCheck(1, oldUpdateTime),
            createOpenCheck(4, oldUpdateTime)
        )
        val closedAboChecks = listOf(
            createClosedCheck(2, actualUpdateTime),
            createClosedCheck(3, oldUpdateTime)
        )
        val mbiChecks = listOf(
            createMbiCheck(0, actualUpdateTime),
            createMbiCheck(1, actualUpdateTime),
            createMbiCheck(2, actualUpdateTime),
            createMbiCheck(3, actualUpdateTime),
            createMbiCheck(5, actualUpdateTime)
        )
        outletCheckRepo.saveAll(aboChecks)
        outletCheckRepo.saveAll(closedAboChecks)
        outletForModerationRepo.saveAll(mbiChecks)
        flushAndClear()

        val checksForCancellation = outletCheckRepo.findChecksForCancellation(CANCELED.id)
            .map { it.mbiOutletId }.toHashSet()
        val outletsForTaskCreation = outletForModerationRepo.findOutletsForCheckCreation(
            CANCELED.id, CampaignType.SHOP.name
        ).map { it.id }.toHashSet()
        assertEquals(mutableSetOf(1L, 4L), checksForCancellation)
        assertEquals(mutableSetOf(1L, 3L, 5L), outletsForTaskCreation)
    }

    private fun createOpenCheck(mbiOutletId: Long, updateTime: Date) = createAboCheck(mbiOutletId, updateTime).apply {
        status = NEW
    }

    private fun createClosedCheck(mbiOutletId: Long, updateTime: Date): OutletCheck {
        val check = createAboCheck(mbiOutletId, updateTime)
        check.status = CANCELED
        check.markAsSent()
        return check
    }

    private fun createAboCheck(mbiOutletId: Long, updateTime: Date): OutletCheck {
        return OutletCheck.fromMbiCheck(createMbiCheck(mbiOutletId, updateTime))
    }

    private fun createMbiCheck(mbiOutletId: Long, updateTime: Date): OutletForModeration {
        val check = OutletForModeration()
        check.id = mbiOutletId
        check.partnerTypeId = CampaignType.SHOP.id
        check.updateTime = updateTime
        check.moderationModificationTime = updateTime
        return check
    }
}
