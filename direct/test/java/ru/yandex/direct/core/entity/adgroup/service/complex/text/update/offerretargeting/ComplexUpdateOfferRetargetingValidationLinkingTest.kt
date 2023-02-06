package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.offerretargeting

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.OFFER_RETARGETINGS
import ru.yandex.direct.core.entity.adgroup.service.complex.text.update.ComplexAdGroupUpdateOperationTestBase
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting.PRICE
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotLessThan
import ru.yandex.direct.currency.Money.valueOf
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import java.math.BigDecimal

/**
 * Тесты линковки ошибок офферного ретаргетинга с группой
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ComplexUpdateOfferRetargetingValidationLinkingTest : ComplexAdGroupUpdateOperationTestBase() {
    @Test
    fun update_AdGroupWithErrorInOfferRetargetingAdd_ErrorInResultIsValid() {
        val adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
        val offerRetargeting = OfferRetargeting()
            .withPrice(BigDecimal.ZERO)
            .withIsSuspended(true)
        adGroupForUpdate.withOfferRetargetings(listOf(offerRetargeting))

        val result = updateAndCheckFirstItemIsInvalid(listOf(adGroupForUpdate))
        val errPath = path(index(0), field(OFFER_RETARGETINGS.name()), index(0), field(PRICE.name()))

        val currency = campaignInfo.clientInfo.client!!.workCurrency.currency

        assertThat(result.validationResult).`is`(
            matchedBy(
                hasDefectDefinitionWith<BigDecimal>(
                    validationError(errPath, invalidValueNotLessThan(valueOf(currency.minPrice, currency.code)))
                )
            )
        )
    }

    @Test
    fun update_FirstAdGroupValidAndSecondAdGroupWithErrorInOfferRetargetingForAdd_ErrorsInResultAreValid() {
        createSecondAdGroup()
        val adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
        val adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
        val offerRetargeting = OfferRetargeting()
            .withPrice(BigDecimal.ZERO)
            .withIsSuspended(true)
        adGroupForUpdate2.withOfferRetargetings(listOf(offerRetargeting))
        val result = updateAndCheckSecondItemIsInvalid(listOf(adGroupForUpdate1, adGroupForUpdate2))
        val errPath = path(
            index(1), field(OFFER_RETARGETINGS.name()),
            index(0), field(PRICE.name())
        )
        val currency = campaignInfo.clientInfo.client!!.workCurrency.currency
        assertThat(result.validationResult).`is`(
            matchedBy(
                hasDefectDefinitionWith<BigDecimal>(
                    validationError(errPath, invalidValueNotLessThan(valueOf(currency.minPrice, currency.code)))
                )
            )
        )
    }

    @Test
    fun update_FirstAdGroupWithErrorInOfferRetargetingForAddAndSecondAdGroupWithErrorInOfferRetargetingForAdd_ErrorsInResultAreValid() {
        createSecondAdGroup()
        val offerRetargeting1 = OfferRetargeting()
            .withPrice(BigDecimal.ZERO)
            .withIsSuspended(true)
        val adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
        adGroupForUpdate1.withOfferRetargetings(listOf(offerRetargeting1))
        val adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
        val offerRetargeting2 = OfferRetargeting()
            .withPrice(BigDecimal.ZERO)
            .withIsSuspended(true)
        adGroupForUpdate2.withOfferRetargetings(listOf(offerRetargeting2))
        val result = updateAndCheckBothItemsAreInvalid(listOf(adGroupForUpdate1, adGroupForUpdate2))

        val errPath = { adGroupIndex: Int ->
            path(index(adGroupIndex), field(OFFER_RETARGETINGS.name()), index(0), field(PRICE.name()))
        }
        val currency = campaignInfo.clientInfo.client!!.workCurrency.currency

        assertThat(result.validationResult).`is`(
            matchedBy(
                hasDefectDefinitionWith<BigDecimal>(
                    validationError(errPath(0), invalidValueNotLessThan(valueOf(currency.minPrice, currency.code)))
                )
            )
        )
        assertThat(result.validationResult).`is`(
            matchedBy(
                hasDefectDefinitionWith<BigDecimal>(
                    validationError(errPath(1), invalidValueNotLessThan(valueOf(currency.minPrice, currency.code)))
                )
            )
        )
    }
}
