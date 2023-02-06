package ru.yandex.direct.core.entity.offerretargeting.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.bids.validation.BidsDefects.Ids.BID_FOR_SEARCH_WONT_BE_ACCEPTED_IN_CASE_OF_AUTOBUDGET_STRATEGY
import ru.yandex.direct.core.entity.bids.validation.BidsDefects.Ids.STRATEGY_IS_NOT_SET
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignTypeNotSupported
import ru.yandex.direct.core.entity.offerretargeting.container.OfferRetargetingAddContainerWithExistentAdGroups
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.entity.offerretargeting.service.OfferRetargetingOperationBaseTest
import ru.yandex.direct.core.entity.offerretargeting.validation.OfferRetargetingDefects.Number.TOO_MANY_OFFER_RETARGETINGS_IN_AD_GROUP
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.validation.defects.RightsDefects.noRights
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult
import java.math.BigDecimal

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class OfferRetargetingAddValidationTest : OfferRetargetingOperationBaseTest() {
    @Autowired
    private lateinit var offerRetargetingValidationService: OfferRetargetingValidationService

    @Test
    fun validate_Success() {
        val offerRetargeting = OfferRetargeting()
            .withCampaignId(activeCampaignId)
            .withAdGroupId(defaultAdGroupId)
            .withIsSuspended(true)
        val actual = validate(listOf(offerRetargeting))
        assertThat(actual).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun validate_OfferRetargetingWithPriceAndAutobudgetPriority_Success() {
        val offerRetargeting = OfferRetargeting()
            .withCampaignId(activeCampaignId)
            .withAdGroupId(defaultAdGroupId)
            .withPrice(BigDecimal.TEN)
            .withIsSuspended(true)
        val actual = validate(listOf(offerRetargeting))
        assertThat(actual).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun validate_AutobudgetStrategyWithPrice_Warning() {
        val offerRetargeting = OfferRetargeting()
            .withCampaignId(activeCampaignId)
            .withAdGroupId(defaultAdGroupId)
            .withPrice(BigDecimal.TEN)
            .withIsSuspended(true)
        val offerRetargetingAddOperationContainer = defaultOfferRetargetingAddOperationContainer()
        offerRetargetingAddOperationContainer.getCampaignByAdGroupId(defaultAdGroupId)!!
            .strategy
            .withPlatform(CampaignsPlatform.SEARCH)
        val actual = validate(listOf(offerRetargeting), offerRetargetingAddOperationContainer)
        assertThat(actual, hasNoErrors())
        assertThat(actual).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(
                        path(index(0), field("price")),
                        BID_FOR_SEARCH_WONT_BE_ACCEPTED_IN_CASE_OF_AUTOBUDGET_STRATEGY
                    )
                )
            )
        )
    }

    @Test
    fun validate_NullAdGroupId_error() {
        val offerRetargeting = OfferRetargeting()
            .withCampaignId(activeCampaignId)
            .withAdGroupId(null)
            .withIsSuspended(true)
        val actual = validate(listOf(offerRetargeting))
        assertThat(actual).`is`(
            matchedBy(
                hasDefectWithDefinition<Any>(
                    validationError(path(index(0), field("adGroupId")), notNull())
                )
            )
        )
    }

    @Test
    fun validate_TwoOfferRetargeting_Error() {
        val offerRetargeting1 = OfferRetargeting()
            .withCampaignId(activeCampaignId)
            .withAdGroupId(defaultAdGroupId)
            .withIsSuspended(true)
        val offerRetargeting2 = OfferRetargeting()
            .withCampaignId(activeCampaignId)
            .withAdGroupId(defaultAdGroupId)
            .withIsSuspended(true)
        val actual = validate(listOf(offerRetargeting1, offerRetargeting2))
        assertThat(actual).`is`(
            matchedBy(
                hasDefectWithDefinition<Any>(
                    validationError(path(index(0)), TOO_MANY_OFFER_RETARGETINGS_IN_AD_GROUP)
                )
            )
        )
    }

    @Test
    fun validate_NotVisibleCampaignAccessChecker_ObjectNotFound() {
        defaultUser = steps.clientSteps().createDefaultClient().chiefUserInfo!!
        val offerRetargeting = OfferRetargeting()
            .withCampaignId(activeCampaignId)
            .withAdGroupId(defaultAdGroupId)
            .withIsSuspended(true)
        val actual = validate(listOf(offerRetargeting))
        assertThat(actual).`is`(
            matchedBy(
                hasDefectWithDefinition<Any>(
                    validationError(path(index(0), field("adGroupId")), objectNotFound())
                )
            )
        )
    }

    @Test
    fun validate_NotWritableCampaignAccessChecker_NoRights() {
        defaultUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER).chiefUserInfo!!
        val offerRetargeting = OfferRetargeting()
            .withCampaignId(activeCampaignId)
            .withAdGroupId(defaultAdGroupId)
            .withIsSuspended(true)
        val actual = validate(listOf(offerRetargeting))
        assertThat(actual).`is`(
            matchedBy(
                hasDefectWithDefinition<Any>(
                    validationError(path(index(0), field("adGroupId")), noRights())
                )
            )
        )
    }

    @Test
    fun validate_CampaignTypeNotSupported() {
        val offerRetargeting = OfferRetargeting()
            .withCampaignId(activeCampaignId)
            .withAdGroupId(defaultAdGroupId)
            .withIsSuspended(true)
        val offerRetargetingAddOperationContainer = defaultOfferRetargetingAddOperationContainer()
        offerRetargetingAddOperationContainer.getCampaignByAdGroupId(defaultAdGroupId)!!
            .withType(CampaignType.DYNAMIC)
        val actual = validate(listOf(offerRetargeting), offerRetargetingAddOperationContainer)
        assertThat(actual).`is`(
            matchedBy(
                hasDefectWithDefinition<Any>(
                    validationError(path(index(0)), campaignTypeNotSupported())
                )
            )
        )
    }

    @Test
    fun validate_OfferRetargetingCategories_Success() {
        val offerRetargeting = OfferRetargeting()
            .withCampaignId(activeCampaignId)
            .withAdGroupId(defaultAdGroupId)
            .withIsSuspended(true)
        val actual = validate(listOf(offerRetargeting))
        assertThat(actual).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun validate_OfferRetargetingCategories_Empty_Success() {
        val offerRetargeting = OfferRetargeting()
            .withCampaignId(activeCampaignId)
            .withAdGroupId(defaultAdGroupId)
            .withIsSuspended(true)
        val actual = validate(listOf(offerRetargeting))
        assertThat(actual).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun validate_OfferRetargetingCategories_Empty_CampaignTypeNotSupported_Success() {
        val offerRetargeting = OfferRetargeting()
            .withCampaignId(activeCampaignId)
            .withAdGroupId(defaultAdGroupId)
            .withIsSuspended(true)
        val offerRetargetingAddOperationContainer = defaultOfferRetargetingAddOperationContainer()
        offerRetargetingAddOperationContainer.getCampaignByAdGroupId(defaultAdGroupId)!!
            .withType(CampaignType.MOBILE_CONTENT)
        val actual = validate(listOf(offerRetargeting), offerRetargetingAddOperationContainer)
        assertThat(actual).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun validate_ValidateStrategyIsNotSet() {
        val offerRetargeting = defaultOfferRetargeting
            .withCampaignId(activeCampaignId)
            .withAdGroupId(defaultAdGroupId)
            .withIsSuspended(true)
        campaignsByIds[offerRetargeting.campaignId]!!
            .withStrategy(null)
        val actual = validate(listOf(offerRetargeting), defaultOfferRetargetingAddOperationContainer())
        assertThat(actual).`is`(
            matchedBy(
                hasDefectWithDefinition<Any>(
                    validationError(path(index(0)), Defect(STRATEGY_IS_NOT_SET))
                )
            )
        )
    }

    private fun validate(
        offerRetargetings: List<OfferRetargeting>,
        offerRetargetingAddContainer: OfferRetargetingAddContainerWithExistentAdGroups = defaultOfferRetargetingAddOperationContainer()
    ): ValidationResult<List<OfferRetargeting>, Defect<*>> {
        val preValidationResult = ValidationResult<List<OfferRetargeting>, Defect<*>>(offerRetargetings)
        return offerRetargetingValidationService
            .validateAddOfferRetargetings(preValidationResult, offerRetargetingAddContainer)
    }

    private fun defaultOfferRetargetingAddOperationContainer(): OfferRetargetingAddContainerWithExistentAdGroups {
        return OfferRetargetingAddContainerWithExistentAdGroups(
            operatorUid,
            clientId,
            currency,
            campaignsByIds,
            campaignIdsByAdGroupIds
        )
    }
}
