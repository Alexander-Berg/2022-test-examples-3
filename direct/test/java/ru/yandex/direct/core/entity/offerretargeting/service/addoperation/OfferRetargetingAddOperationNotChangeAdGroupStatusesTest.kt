package ru.yandex.direct.core.entity.offerretargeting.service.addoperation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.offerretargeting.service.OfferRetargetingOperationBaseTest
import ru.yandex.direct.core.testing.configuration.CoreTest

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class OfferRetargetingAddOperationNotChangeAdGroupStatusesTest : OfferRetargetingOperationBaseTest() {
    @Autowired
    private lateinit var adGroupRepository: AdGroupRepository

    @Test
    fun prepareAndApply_AdGroupAlreadyModerated_GroupStatusModerateNotReset() {
        val offerRetargeting = defaultOfferRetargeting.withAdGroupInfo(0)
        val fullAddOperation = getFullAddOperation(offerRetargeting)
        fullAddOperation.prepareAndApply()
        val actualAdGroupStatusModerate = adGroupRepository
            .getAdGroups(shard, listOf(defaultAdGroupId)).single()
            .statusModerate
        assertThat(actualAdGroupStatusModerate).isEqualTo(StatusModerate.YES)
    }
}
