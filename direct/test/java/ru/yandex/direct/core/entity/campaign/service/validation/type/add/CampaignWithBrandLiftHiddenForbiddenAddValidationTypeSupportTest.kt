package ru.yandex.direct.core.entity.campaign.service.validation.type.add

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBrandLiftHiddenForbidden
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign
import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
class CampaignWithBrandLiftHiddenForbiddenAddValidationTypeSupportTest {

    lateinit var typeSupport: CampaignWithBrandLiftHiddenForbiddenAddValidationTypeSupport

    fun testData() = listOf(
        listOf("TextCampaign", TextCampaign()),
        listOf("DynamicCampaign", DynamicCampaign()),
        listOf("SmartCampaign", SmartCampaign()),
        listOf("ContentPromotionCampaign", ContentPromotionCampaign()),
        listOf("InternalAutobudgetCampaign", InternalAutobudgetCampaign()),
        listOf("InternalDistribCampaign", InternalDistribCampaign()),
        listOf("InternalFreeCampaign", InternalFreeCampaign()),
        listOf("McBannerCampaign", McBannerCampaign()),
        listOf("MobileContentCampaign", MobileContentCampaign())
    )

    @Before
    fun setUp() {
        typeSupport = CampaignWithBrandLiftHiddenForbiddenAddValidationTypeSupport()
    }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("campaign is {0} and not copy")
    fun shouldAddDefectIfFlagIsPresent(testCase: String, campaign: CampaignWithBrandLiftHiddenForbidden) {
        val container = mock(RestrictedCampaignsAddOperationContainer::class.java)
        campaign.isBrandLiftHidden = true

        val result = typeSupport.preValidate(container, ValidationResult(listOf(campaign)))

        assertThat(result)
            .`is`(matchedBy(hasDefectWithDefinition<Any>(
                validationError(
                    path(index(0), field(CampaignWithBrandLiftHiddenForbidden.IS_BRAND_LIFT_HIDDEN)),
                    CommonDefects.isNull()
                )
            )))
    }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("campaign is {0} and not copy")
    fun shouldValidateOkIfFlagIsNotPresent(testCase: String, campaign: CampaignWithBrandLiftHiddenForbidden) {
        val container = mock(RestrictedCampaignsAddOperationContainer::class.java)

        val result = typeSupport.preValidate(container, ValidationResult(listOf(campaign)))

        assertThat(result).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("campaign is {0} and not copy")
    fun shouldValidateOkIfCopy(testCase: String, campaign: CampaignWithBrandLiftHiddenForbidden) {
        val container = mock(RestrictedCampaignsAddOperationContainer::class.java)
        `when`(container.isCopy)
            .thenReturn(true)

        val result = typeSupport.preValidate(container, ValidationResult(listOf(campaign)))

        assertThat(result).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }
}
