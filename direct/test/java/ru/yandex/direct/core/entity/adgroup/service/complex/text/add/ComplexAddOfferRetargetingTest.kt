package ru.yandex.direct.core.entity.adgroup.service.complex.text.add

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.OFFER_RETARGETINGS
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.defaultOfferRetargeting
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyAdGroupWithModelForAdd
import ru.yandex.direct.core.entity.bids.validation.BidsDefects.CurrencyAmountDefects
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting.PRICE
import ru.yandex.direct.core.entity.offerretargeting.validation.MAX_OFFER_RETARGETINGS_IN_GROUP
import ru.yandex.direct.core.entity.offerretargeting.validation.OfferRetargetingDefects.tooManyOfferRetargetingsInAdGroup
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.validation.defects.params.CurrencyAmountDefectParams
import ru.yandex.direct.currency.CurrencyCode.RUB
import ru.yandex.direct.currency.Money.valueOf
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult
import java.math.BigDecimal

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ComplexAddOfferRetargetingTest : ComplexTextAddTestBase() {
    @Test
    fun oneAdGroupWithOfferRetargeting() {
        val adGroup = adGroupWithOfferRetargetings(defaultOfferRetargeting())
        addAndCheckComplexAdGroups(listOf(adGroup))
    }

    @Test
    fun oneAdGroupWithoutOfferRetargetingsAndOneWith() {
        val emptyComplexAdGroup = emptyTextAdGroup()
        val adGroupWithOfferRetargetings = adGroupWithOfferRetargetings(defaultOfferRetargeting())
        addAndCheckComplexAdGroups(listOf(emptyComplexAdGroup, adGroupWithOfferRetargetings))
    }

    @Test
    fun adGroupWithMaxOfferRetargetings() {
        val adGroup = adGroupWithOfferRetargetings(List(MAX_OFFER_RETARGETINGS_IN_GROUP) { defaultOfferRetargeting() })
        addAndCheckComplexAdGroups(listOf(adGroup))
    }

    @Test
    fun adGroupWithTooManyOfferRetargetings() {
        val adGroup =
            adGroupWithOfferRetargetings(List(MAX_OFFER_RETARGETINGS_IN_GROUP + 1) { defaultOfferRetargeting() })
        val addOperation = createOperation(listOf(adGroup))
        val result = addOperation.prepareAndApply()
        assertThat(result.validationResult)
            .withFailMessage { "превышено максимальное количество офферных ретаргетингов в группе" }
            .`is`(
                matchedBy(
                    allOf<ValidationResult<Long, Defect<Any>>>(
                        hasDefectDefinitionWith(
                            validationError(
                                path(index(0), field(OFFER_RETARGETINGS.name()), index(0)),
                                tooManyOfferRetargetingsInAdGroup()
                            )
                        ),
                        hasDefectDefinitionWith(
                            validationError(
                                path(index(0), field(OFFER_RETARGETINGS.name()), index(1)),
                                tooManyOfferRetargetingsInAdGroup()
                            )
                        )
                    )
                )
            )
    }

    @Test
    fun oneAdGroupWithValidAndOneWithInvalidOfferRetargeting() {
        val adGroupValid = adGroupWithOfferRetargetings(defaultOfferRetargeting())
        val adGroupInvalid = adGroupWithOfferRetargetings(defaultOfferRetargeting().withPrice(BigDecimal(0.2)))
        val addOperation = createOperation(listOf(adGroupValid, adGroupInvalid))
        val result = addOperation.prepareAndApply()
        assertThat(result.validationResult).`is`(
            matchedBy(
                hasDefectDefinitionWith<Long>(
                    validationError(
                        path(index(1), field(OFFER_RETARGETINGS.name()), index(0), field(PRICE.name())),
                        Defect(
                            CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN,
                            CurrencyAmountDefectParams(valueOf(MIN_AUTOBUDGET_BID, RUB))
                        )
                    )
                )
            )
        )
    }

    private fun adGroupWithOfferRetargetings(offerRetargetings: List<OfferRetargeting>): ComplexTextAdGroup {
        return emptyAdGroupWithModelForAdd(campaignId, offerRetargetings, OFFER_RETARGETINGS)
    }

    private fun adGroupWithOfferRetargetings(vararg offerRetargetings: OfferRetargeting): ComplexTextAdGroup {
        return emptyAdGroupWithModelForAdd(campaignId, offerRetargetings.toList(), OFFER_RETARGETINGS)
    }

    companion object {
        private val MIN_AUTOBUDGET_BID = BigDecimal("0.3")
    }
}
