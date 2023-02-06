package ru.yandex.direct.core.entity.offerretargeting.service.addoperation

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.notNullValue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.entity.offerretargeting.service.OfferRetargetingOperationBaseTest
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestGroups.draftTextAdgroup
import ru.yandex.direct.operation.AddedModelId
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers
import java.math.BigDecimal

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class OfferRetargetingAddOperationTest : OfferRetargetingOperationBaseTest() {
    @Test
    fun prepareAndApply_Full_OneValidItem_ResultIsFullySuccessful() {
        val offerRetargeting = defaultOfferRetargeting.withAdGroupInfo(0)
        val fullAddOperation = getFullAddOperation(offerRetargeting)
        val massResult = fullAddOperation.prepareAndApply()
        assertThat(massResult).`is`(matchedBy(isSuccessfulWithMatchers(notNullValue(AddedModelId::class.java))))
    }

    @Test
    fun prepareAndApply_Full_OneValidItem_WithContainerFilledInService_ResultIsFullySuccessful() {
        val offerRetargeting = OfferRetargeting()
            .withPrice(BigDecimal.TEN)
            .withPriceContext(BigDecimal.TEN.add(BigDecimal.ONE))
            .withIsSuspended(true)
            .withAdGroupInfo(0)

        val fullAddOperation = offerRetargetingService.createFullAddOperation(
            listOf(offerRetargeting),
            clientId,
            operatorUid,
            currency
        )
        val massResult = fullAddOperation.prepareAndApply()
        assertThat(massResult).`is`(matchedBy(isSuccessfulWithMatchers(notNullValue(AddedModelId::class.java))))
    }

    @Test
    fun prepareAndApply_Full_OneInvalidItem_ResultHasElementError() {
        val invalidOfferRetargeting = OfferRetargeting()
            .withPrice(BigDecimal.valueOf(1.12))
            .withPriceContext(BigDecimal.valueOf(-1.01))
            .withAdGroupInfo(0)

        val fullAddOperation = getFullAddOperation(invalidOfferRetargeting)
        val massResult = fullAddOperation.prepareAndApply()
        assertThat(massResult).`is`(matchedBy(isSuccessful<AddedModelId>(false)))
    }

    @Test
    fun prepareAndApply_Full_OneValidItem_SavedCorrectly() {
        val offerRetargeting = defaultOfferRetargeting.withAdGroupInfo(0)
        val fullAddOperation = getFullAddOperation(offerRetargeting)
        val result = fullAddOperation.prepareAndApply()

        val expectedOfferRetargeting = defaultOfferRetargeting.withAdGroupInfo(0)
        checkOfferRetargetingByIdIsEqualTo(result[0].result.id, expectedOfferRetargeting)
    }

    override fun defaultAdGroup(campaignId: Long): AdGroup = draftTextAdgroup(campaignId)
}
