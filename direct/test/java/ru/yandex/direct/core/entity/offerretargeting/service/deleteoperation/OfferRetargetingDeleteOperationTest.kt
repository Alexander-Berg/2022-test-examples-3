package ru.yandex.direct.core.entity.offerretargeting.service.deleteoperation

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.offerretargeting.service.OfferRetargetingOperationBaseTest
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class OfferRetargetingDeleteOperationTest : OfferRetargetingOperationBaseTest() {
    @Autowired
    private lateinit var adGroupRepository: AdGroupRepository

    @Test
    fun prepareAndApply_Full_OneValidItem_ResultIsFullySuccessful() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        val offerRetargetingDeleteOperation = getFullDeleteOperation(listOf(savedOfferRetargeting.id))
        val massResult = offerRetargetingDeleteOperation.prepareAndApply()
        assertThat(massResult).`is`(matchedBy(isSuccessfulWithMatchers(notNullValue(Long::class.java))))
    }

    @Test
    fun prepareAndApply_Full_OneInValidItem_ResultHasElementError() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        getFullDeleteOperation(listOf(savedOfferRetargeting.id)).prepareAndApply()
        val offerRetargetingDeleteOperation = getFullDeleteOperation(listOf(savedOfferRetargeting.id))
        val massResult = offerRetargetingDeleteOperation.prepareAndApply()
        assertThat(massResult).`is`(matchedBy(isSuccessful<Long>(false)))
    }

    @Test
    fun prepareAndApply_Full_OneValidItem_DeleteCorrectly() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        val offerRetargetingDeleteOperation = getFullDeleteOperation(listOf(savedOfferRetargeting.id))
        val result = offerRetargetingDeleteOperation.prepareAndApply()
        checkOfferRetargetingByIdIsDeleted(result[0].result)
    }

    @Test
    fun prepareAndApply_DeleteOfferRetargeting_AdGroupStatusBsSyncedNo() {
        adGroupRepository.updateStatusBsSynced(shard, listOf(defaultAdGroupId), StatusBsSynced.YES)
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        val offerRetargetingDeleteOperation = getFullDeleteOperation(listOf(savedOfferRetargeting.id))
        offerRetargetingDeleteOperation.prepareAndApply()
        val adGroup = adGroupRepository.getAdGroups(shard, listOf(defaultAdGroupId))[0]

        assertThat(adGroup.statusBsSynced).isEqualTo(StatusBsSynced.NO)
    }
}
