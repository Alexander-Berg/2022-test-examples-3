package ru.yandex.direct.core.entity.offerretargeting.service.modifyoperation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.offerretargeting.repository.OfferRetargetingMapping.createOfferRetargetingModification
import ru.yandex.direct.core.entity.offerretargeting.service.OfferRetargetingOperationBaseTest
import ru.yandex.direct.core.testing.configuration.CoreTest
import java.math.BigDecimal

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class OfferRetargetingModifyOperationTest : OfferRetargetingOperationBaseTest() {
    @Test
    fun addOfferRetargeting_Prepare_Success() {
        val offerRetargetingToAdd = defaultOfferRetargeting.withAdGroupInfo(0)
        val offerRetargetingModification =
            createOfferRetargetingModification(offerRetargetingsToAdd = listOf(offerRetargetingToAdd))

        val offerRetargetingModifyOperation = getFullModifyOperation(offerRetargetingModification)
        val prepareResult = offerRetargetingModifyOperation.prepare()

        assertThat(prepareResult).isNull()
    }

    @Test
    fun updateOfferRetargeting_Prepare_Success() {
        val offerRetargetingToUpdate = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting
                .withPrice(BigDecimal("10.1"))
                .withPriceContext(BigDecimal("11.1"))
                .withIsSuspended(true)
        }
        val offerRetargetingModification =
            createOfferRetargetingModification(offerRetargetingsToUpdate = listOf(offerRetargetingToUpdate))

        val offerRetargetingModifyOperation = getFullModifyOperation(offerRetargetingModification)
        val prepareResult = offerRetargetingModifyOperation.prepare()

        assertThat(prepareResult).isNull()
    }

    @Test
    fun deleteOfferRetargeting_Prepare_Success() {
        val offerRetargetingToDelete = addOfferRetargetingToAdGroup(0)
        val offerRetargetingModification =
            createOfferRetargetingModification(offerRetargetingIdsToDelete = listOf(offerRetargetingToDelete.id))

        val offerRetargetingModifyOperation = getFullModifyOperation(offerRetargetingModification)
        val prepareResult = offerRetargetingModifyOperation.prepare()

        assertThat(prepareResult).isNull()
    }

    @Test
    fun addOfferRetargeting_Apply_Success() {
        val offerRetargetingToAdd = defaultOfferRetargeting.withAdGroupInfo(0)
        val offerRetargetingModification =
            createOfferRetargetingModification(offerRetargetingsToAdd = listOf(offerRetargetingToAdd))

        val offerRetargetingModifyOperation = getFullModifyOperation(offerRetargetingModification)
        val result = offerRetargetingModifyOperation.prepareAndApply()
        assertThat(result.isSuccessful).isTrue

        val addedOfferRetargetingId = result.result.offerRetargetingAddResult.single()
        checkOfferRetargetingByIdIsEqualTo(addedOfferRetargetingId, offerRetargetingToAdd)
    }

    @Test
    fun updateOfferRetargeting_Apply_Success() {
        val offerRetargetingToUpdate = addDefaultOfferRetargetingToAdGroupForUpdate(0) { offerRetargeting ->
            offerRetargeting
                .withPrice(BigDecimal("10.10"))
                .withPriceContext(BigDecimal("11.10"))
                .withIsSuspended(true)
        }
        val offerRetargetingModification =
            createOfferRetargetingModification(offerRetargetingsToUpdate = listOf(offerRetargetingToUpdate))

        val offerRetargetingModifyOperation = getFullModifyOperation(offerRetargetingModification)
        val result = offerRetargetingModifyOperation.prepareAndApply()

        assertThat(result.isSuccessful).isTrue

        val expectedUpdatedOfferRetargeting = defaultOfferRetargeting.withAdGroupInfo(0)
            .withPrice(BigDecimal("10.10"))
            .withPriceContext(BigDecimal("11.10"))
            .withIsSuspended(true)
        val updatedOfferRetargetingId = result.result.offerRetargetingUpdateResult.single()
        checkOfferRetargetingByIdIsEqualTo(updatedOfferRetargetingId, expectedUpdatedOfferRetargeting)
    }

    @Test
    fun deleteOfferRetargeting_Apply_Success() {
        val offerRetargetingToDelete = addOfferRetargetingToAdGroup(0)
        val offerRetargetingModification =
            createOfferRetargetingModification(offerRetargetingIdsToDelete = listOf(offerRetargetingToDelete.id))

        val offerRetargetingModifyOperation = getFullModifyOperation(offerRetargetingModification)
        val result = offerRetargetingModifyOperation.prepareAndApply()

        assertThat(result.isSuccessful).isTrue

        val deletedOfferRetargetingId = result.result.offerRetargetingDeleteResult.single()
        checkOfferRetargetingByIdIsDeleted(deletedOfferRetargetingId)
    }

    @Test
    fun fullModifyOfferRetargeting_Apply_Success() {
        // создаем дополнительные кампании и группы
        addCampaign()
        addCampaign()
            .withAutobudget(false)
            .withStrategy(DbStrategy())

        val offerRetargetingToAdd = defaultOfferRetargeting.withAdGroupInfo(0)
        val offerRetargetingToDelete = addOfferRetargetingToAdGroup(1)
        val offerRetargetingToUpdate = addDefaultOfferRetargetingToAdGroupForUpdate(2) { offerRetargeting ->
            offerRetargeting
                .withPrice(BigDecimal("10.10"))
                .withPriceContext(BigDecimal("11.10"))
                .withIsSuspended(true)
        }

        val offerRetargetingModification = createOfferRetargetingModification(
            offerRetargetingsToAdd = listOf(offerRetargetingToAdd),
            offerRetargetingsToUpdate = listOf(offerRetargetingToUpdate),
            offerRetargetingIdsToDelete = listOf(offerRetargetingToDelete.id)
        )

        val offerRetargetingModifyOperation = getFullModifyOperation(offerRetargetingModification)
        val result = offerRetargetingModifyOperation.prepareAndApply()

        // проверяем, что операция успешно завершилась
        assertThat(result.isSuccessful).isTrue

        val expectedOfferRetargetingToUpdate = defaultOfferRetargeting
            .withPrice(BigDecimal("10.10"))
            .withPriceContext(BigDecimal("11.10"))
            .withIsSuspended(true)
            .withAdGroupInfo(2)

        val addedOfferRetargetingId = result.result.offerRetargetingAddResult.single()
        val updatedOfferRetargetingId = result.result.offerRetargetingUpdateResult.single()
        val deletedOfferRetargetingId = result.result.offerRetargetingDeleteResult.single()

        checkOfferRetargetingByIdIsEqualTo(addedOfferRetargetingId, offerRetargetingToAdd)
        checkOfferRetargetingByIdIsEqualTo(updatedOfferRetargetingId, expectedOfferRetargetingToUpdate)
        checkOfferRetargetingByIdIsDeleted(deletedOfferRetargetingId)
    }

    @Test
    fun addOfferRetargetingWithInvalidPrice_Apply_Error() {
        val offerRetargetingWithInvalidPrice = defaultOfferRetargeting
            .withPrice(BigDecimal.ZERO)
            .withAdGroupInfo(0)
        val offerRetargetingModification =
            createOfferRetargetingModification(offerRetargetingsToAdd = listOf(offerRetargetingWithInvalidPrice))

        val offerRetargetingModifyOperation = getFullModifyOperation(offerRetargetingModification)
        val result = offerRetargetingModifyOperation.prepareAndApply()

        assertThat(result.errors).isNotEmpty
    }

    @Test
    fun addOfferRetargetingToOtherClientsGroup_Apply_Error() {
        val otherUser = userSteps.createDefaultUser()
        val otherClientAdGroup = adGroupSteps.createActiveTextAdGroup(otherUser.clientInfo)

        val offerRetargeting = defaultOfferRetargeting
            .withAdGroupId(otherClientAdGroup.adGroupId)
            .withPrice(BigDecimal.ZERO)
        val offerRetargetingModification =
            createOfferRetargetingModification(offerRetargetingsToAdd = listOf(offerRetargeting))

        val offerRetargetingModifyOperation = getFullModifyOperation(offerRetargetingModification)
        val result = offerRetargetingModifyOperation.prepareAndApply()

        assertThat(result.errors).isNotEmpty
    }
}
