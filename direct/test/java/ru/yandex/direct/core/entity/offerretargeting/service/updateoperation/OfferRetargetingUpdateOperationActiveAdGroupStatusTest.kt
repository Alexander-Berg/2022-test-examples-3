package ru.yandex.direct.core.entity.offerretargeting.service.updateoperation

import org.assertj.core.api.Assertions.assertThat
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
import ru.yandex.direct.model.ModelChanges

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class OfferRetargetingUpdateOperationActiveAdGroupStatusTest : OfferRetargetingOperationBaseTest() {
    @Autowired
    private lateinit var adGroupRepository: AdGroupRepository

    @Test
    fun prepareAndApply_GroupAlreadyModerated_AdGroupStatusModerateNotReset() {
        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting.withIsSuspended(true)
        }

        val offerRetargetingUpdateOperation = getFullUpdateOperation(offerRetargetingChanges)

        offerRetargetingUpdateOperation.prepareAndApply()
        val actualAdGroupStatusModerate = adGroupRepository
            .getAdGroups(shard, listOf(defaultAdGroupId)).single()
            .statusModerate

        assertThat(actualAdGroupStatusModerate).isEqualTo(StatusModerate.YES)
    }

    @Test
    fun prepareAndApply_GroupNotModerated_AdGroupStatusModerateReset() {
        val changes = ModelChanges(defaultAdGroupId, AdGroup::class.java)
            .process(StatusModerate.SENDING, AdGroup.STATUS_MODERATE)
            .applyTo(adGroupsById[defaultAdGroupId]!!)

        adGroupRepository.updateAdGroups(shard, clientId, setOf(changes))

        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting.withIsSuspended(true)
        }

        val offerRetargetingUpdateOperation = getFullUpdateOperation(offerRetargetingChanges)
        offerRetargetingUpdateOperation.prepareAndApply()

        val actualAdGroupStatusModerate = adGroupRepository
            .getAdGroups(shard, listOf(defaultAdGroupId)).single()
            .statusModerate

        assertThat(actualAdGroupStatusModerate).isEqualTo(StatusModerate.READY)
    }

    @Test
    fun prepareAndApply_Suspend_AdGroupStatusBsSyncedNo() {
        adGroupRepository.updateStatusBsSynced(shard, listOf(defaultAdGroupId), StatusBsSynced.YES)

        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting.withIsSuspended(true)
        }

        getFullUpdateOperation(offerRetargetingChanges).prepareAndApply()

        val statusBsSynced = adGroupRepository
            .getAdGroups(shard, listOf(defaultAdGroupId)).single()
            .statusBsSynced

        assertThat(statusBsSynced).isEqualTo(StatusBsSynced.NO)
    }
}
