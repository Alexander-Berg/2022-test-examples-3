package ru.yandex.market.abo.cpa.order.limit

import java.time.LocalDate
import java.util.Date
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.limit.count.CpaOrderLimitCount
import ru.yandex.market.abo.cpa.order.limit.count.CpaOrderLimitCountRepo
import ru.yandex.market.abo.cpa.order.limit.cutoff.CpaOrderLimitActiveCutoff
import ru.yandex.market.abo.cpa.order.limit.cutoff.CpaOrderLimitActiveCutoffRepo
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimit
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason
import ru.yandex.market.abo.cpa.order.model.PartnerModel

/**
 * @author komarovns
 */
open class BaseCpaOrderLimitTest(
    protected val cpaOrderLimitRepo: CpaOrderLimitRepo,
    protected val cpaOrderLimitCountRepo: CpaOrderLimitCountRepo,
    protected val cpaOrderLimitActiveCutoffRepo: CpaOrderLimitActiveCutoffRepo
) : EmptyTest() {

    protected fun initLimit(partnerId: Long, partnerModel: PartnerModel, ordersLimit: Int, deleted: Boolean = false) {
        val limit = CpaOrderLimit(partnerId, partnerModel, CpaOrderLimitReason.MANUAL, ordersLimit, null, 1000).apply {
            this.deleted = deleted
            this.creationTime = Date()
            this.createdUserId = 0
        }
        cpaOrderLimitRepo.save(limit)
    }

    protected fun initCount(partnerId: Long, partnerModel: PartnerModel, ordersCount: Long) {
        val count = CpaOrderLimitCount(partnerId, partnerModel.id, TODAY, ordersCount)
        cpaOrderLimitCountRepo.save(count)
    }

    protected fun initCutoff(partnerId: Long, partnerModel: PartnerModel) {
        val cutoff = CpaOrderLimitActiveCutoff(CpaOrderLimitActiveCutoff.Key(partnerId, partnerModel))
        cpaOrderLimitActiveCutoffRepo.save(cutoff)
    }

    protected companion object {
        val TODAY: LocalDate = LocalDate.now()
    }
}
