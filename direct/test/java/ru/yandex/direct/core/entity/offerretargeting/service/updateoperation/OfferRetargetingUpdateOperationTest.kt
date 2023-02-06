package ru.yandex.direct.core.entity.offerretargeting.service.updateoperation

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.entity.offerretargeting.service.OfferRetargetingOperationBaseTest
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers
import java.math.BigDecimal
import java.time.LocalDateTime

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class OfferRetargetingUpdateOperationTest : OfferRetargetingOperationBaseTest() {
    @Test
    fun prepareAndApply_Full_OneValidItem_ResultIsFullySuccessful() {
        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { it }
        val offerRetargetingUpdateOperation = getFullUpdateOperationWithContainer(offerRetargetingChanges)
        val massResult = offerRetargetingUpdateOperation.prepareAndApply()

        assertThat(massResult).`is`(matchedBy(isSuccessfulWithMatchers(notNullValue(Long::class.java))))
    }

    @Test
    fun prepareAndApply_Full_OneValidItem_WithContainerFilledInService_ResultIsFullySuccessful() {
        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRet ->
            offerRet
                .withPrice(BigDecimal.TEN)
                .withPriceContext(BigDecimal.TEN.add(BigDecimal.ONE))
        }

        val offerRetargetingUpdateOperation = getFullUpdateOperation(offerRetargetingChanges)
        val massResult = offerRetargetingUpdateOperation.prepareAndApply()
        assertThat(massResult).`is`(matchedBy(isSuccessfulWithMatchers(notNullValue(Long::class.java))))
    }

    @Test
    fun prepareAndApply_Full_OneInValidItem_ResultHasElementError() {
        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRet ->
            offerRet.withPrice(BigDecimal(-10))
        }
        val offerRetargetingUpdateOperation = getFullUpdateOperationWithContainer(offerRetargetingChanges)
        val massResult = offerRetargetingUpdateOperation.prepareAndApply()
        assertThat(massResult).`is`(matchedBy(isSuccessful<Long>(false)))
    }

    @Test
    fun prepareAndApply_Full_OneValidItem_SavedCorrectly() {
        val offerRetargetingChanges = addDefaultOfferRetargetingToAdGroupForUpdate(0) { it }
        val offerRetargetingUpdateOperation = getFullUpdateOperationWithContainer(offerRetargetingChanges)
        val result = offerRetargetingUpdateOperation.prepareAndApply()
        val expectedOfferRetargeting = defaultOfferRetargeting
            .withStatusBsSynced(StatusBsSynced.NO)
            .withLastChangeTime(LocalDateTime.now())
            .withIsDeleted(false)
            .withAdGroupInfo(0)
        checkOfferRetargetingByIdIsEqualTo(result[0].result, expectedOfferRetargeting)
    }

    override val defaultOfferRetargeting: OfferRetargeting =
        OfferRetargeting()
            .withLastChangeTime(LocalDateTime.now())
            .withIsSuspended(false)
            .withStatusBsSynced(StatusBsSynced.NO)
}
