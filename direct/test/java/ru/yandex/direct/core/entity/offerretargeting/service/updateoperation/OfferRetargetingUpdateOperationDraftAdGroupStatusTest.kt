package ru.yandex.direct.core.entity.offerretargeting.service.updateoperation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.offerretargeting.service.OfferRetargetingOperationBaseTest
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestGroups.draftTextAdgroup
import java.math.BigDecimal

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class OfferRetargetingUpdateOperationDraftAdGroupStatusTest : OfferRetargetingOperationBaseTest() {
    @Autowired
    private lateinit var adGroupRepository: AdGroupRepository

    @Before
    fun resetAdGroupStatusBsSynced() {
        adGroupRepository.updateStatusBsSynced(shard, listOf(defaultAdGroupId), StatusBsSynced.YES)
    }

    @Test
    fun prepareAndApply_AutobudgetPriorityChanged_AdGroupBsSyncStatusNotReset() {
        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting.withIsSuspended(false)
        }
        val offerRetargetingUpdateOperation = getFullUpdateOperation(offerRetargetingChanges)
        offerRetargetingUpdateOperation.prepareAndApply()
        val statusBsSynced = adGroupRepository
            .getAdGroups(shard, listOf(defaultAdGroupId)).single()
            .statusBsSynced
        assertThat(statusBsSynced).isEqualTo(StatusBsSynced.YES)
    }

    @Test
    fun prepareAndApply_PriceChanged_AdGroupBsSyncStatusNotReset() {

        campaignsByIds[activeCampaignId]!!.withAutobudget(false)
        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting
                .withIsSuspended(false)
                .withPrice(BigDecimal.TEN)
        }
        val offerRetargetingUpdateOperation = getFullUpdateOperation(offerRetargetingChanges)
        offerRetargetingUpdateOperation.prepareAndApply()
        val statusBsSynced = adGroupRepository
            .getAdGroups(shard, listOf(defaultAdGroupId)).single()
            .statusBsSynced
        assertThat(statusBsSynced).isEqualTo(StatusBsSynced.YES)
    }

    @Test
    fun prepareAndApply_SetSuspended_AdGroupBsSyncStatusNotReset() {
        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting.withIsSuspended(true)
        }
        val offerRetargetingUpdateOperation = getFullUpdateOperation(offerRetargetingChanges)
        offerRetargetingUpdateOperation.prepareAndApply()
        val statusBsSynced = adGroupRepository
            .getAdGroups(shard, listOf(defaultAdGroupId)).single()
            .statusBsSynced
        assertThat(statusBsSynced).isEqualTo(StatusBsSynced.YES)
    }

    @Test
    fun prepareAndApply__ChangeHrefParam1_AdGroupBsSyncStatusNotReset() {
        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting.withHrefParam1("asdas")
        }
        val offerRetargetingUpdateOperation = getFullUpdateOperation(offerRetargetingChanges)
        offerRetargetingUpdateOperation.prepareAndApply()
        val statusBsSynced = adGroupRepository
            .getAdGroups(shard, listOf(defaultAdGroupId)).single()
            .statusBsSynced
        assertThat(statusBsSynced).isEqualTo(StatusBsSynced.YES)
    }

    @Test
    fun prepareAndApply__ChangeHrefParam2_AdGroupBsSyncStatusNotReset() {
        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting.withHrefParam2("asdas")
        }
        val offerRetargetingUpdateOperation = getFullUpdateOperation(offerRetargetingChanges)
        offerRetargetingUpdateOperation.prepareAndApply()
        val statusBsSynced = adGroupRepository
            .getAdGroups(shard, listOf(defaultAdGroupId)).single()
            .statusBsSynced

        assertThat(statusBsSynced).isEqualTo(StatusBsSynced.YES)
    }

    @Test
    fun prepareAndApply_AdGroupStatusModerateNotReset() {
        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting.withIsSuspended(!offerRetargeting.isSuspended)
        }
        val offerRetargetingUpdateOperation = getFullUpdateOperation(offerRetargetingChanges)
        offerRetargetingUpdateOperation.prepareAndApply()
        val actualAdGroupStatusModerate = adGroupRepository
            .getAdGroups(shard, listOf(defaultAdGroupId)).single()
            .statusModerate

        assertThat(actualAdGroupStatusModerate).isEqualTo(StatusModerate.NEW)
    }

    override fun defaultAdGroup(campaignId: Long): AdGroup = draftTextAdgroup(campaignId)
}
