package ru.yandex.direct.core.entity.offerretargeting.service.updateoperation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.entity.offerretargeting.service.OfferRetargetingOperationBaseTest
import ru.yandex.direct.core.testing.configuration.CoreTest
import java.math.BigDecimal

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class OfferRetargetingUpdateBsSyncStatusTest : OfferRetargetingOperationBaseTest() {
    @Test
    fun prepareAndApply_ChangePrice_StatusBsSyncReset() {
        val campaign = campaignsByIds[activeCampaignId]!!
        campaignsByIds[activeCampaignId]!!.autobudget = false
        campaignsByIds[activeCampaignId]!!.strategy.autobudget = CampaignsAutobudget.NO
        campaignsByIds[activeCampaignId]!!.strategy.platform = CampaignsPlatform.BOTH

        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting.withPrice(BigDecimal.TEN)
        }

        val offerRetargetingUpdateOperation = getFullUpdateOperation(offerRetargetingChanges)

        val savedOfferRetargetingId: Long = offerRetargetingUpdateOperation.prepareAndApply()[0].result

        val statusBsSynced = offerRetargetingRepository.getOfferRetargetingsByIds(
            shard, clientId, listOf(savedOfferRetargetingId)
        ).values.single().statusBsSynced

        assertThat(statusBsSynced).isEqualTo(StatusBsSynced.NO)
    }

    @Test
    fun prepareAndApply_ChangeHrefParam_StatusBsSyncNotReset() {
        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting.withHrefParam1("123")
        }

        val offerRetargetingUpdateOperation = getFullUpdateOperation(offerRetargetingChanges)

        val savedOfferRetargetingId = offerRetargetingUpdateOperation.prepareAndApply().result.single().result

        val statusBsSynced = offerRetargetingRepository.getOfferRetargetingsByIds(
            shard, clientId, listOf(savedOfferRetargetingId)
        )[savedOfferRetargetingId]!!.statusBsSynced

        assertThat(statusBsSynced).isEqualTo(StatusBsSynced.YES)
    }

    override val defaultOfferRetargeting: OfferRetargeting
        get() = super.defaultOfferRetargeting
            .withStatusBsSynced(StatusBsSynced.YES)
}
