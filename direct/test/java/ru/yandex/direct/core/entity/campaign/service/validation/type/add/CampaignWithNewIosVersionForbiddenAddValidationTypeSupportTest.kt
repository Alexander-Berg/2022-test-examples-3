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
import ru.yandex.direct.core.entity.campaign.model.CampaignWithNewIosVersionForbidden
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign
import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign
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
class CampaignWithNewIosVersionForbiddenAddValidationTypeSupportTest {

    lateinit var typeSupport: CampaignWithNewIosVersionForbiddenAddValidationTypeSupport

    fun testData() = listOf(
            listOf("TextCampaign", TextCampaign()),
            listOf("DynamicCampaign", DynamicCampaign()),
            listOf("SmartCampaign", SmartCampaign()),
            listOf("ContentPromotionCampaign", ContentPromotionCampaign()),
            listOf("CpmBannerCampaign", CpmBannerCampaign()),
            listOf("CpmPriceCampaign", CpmPriceCampaign()),
            listOf("CpmYndxFrontpageCampaign", CpmYndxFrontpageCampaign()),
            listOf("InternalAutobudgetCampaign", InternalAutobudgetCampaign()),
            listOf("InternalDistribCampaign", InternalDistribCampaign()),
            listOf("InternalFreeCampaign", InternalFreeCampaign()),
            listOf("McBannerCampaign", McBannerCampaign())
    )

    @Before
    fun setUp() {
        typeSupport = CampaignWithNewIosVersionForbiddenAddValidationTypeSupport()
    }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("campaign is {0} and not copy")
    fun shouldAddDefectIfFlagIsPresent(testCase: String, campaign: CampaignWithNewIosVersionForbidden) {
        val container = mock(RestrictedCampaignsAddOperationContainer::class.java)
        campaign.isNewIosVersionEnabled = true

        val result = typeSupport.preValidate(container, ValidationResult(listOf(campaign)))

        assertThat(result)
                .`is`(matchedBy(hasDefectWithDefinition<Any>(
                        validationError(
                                path(index(0), field(CampaignWithNewIosVersionForbidden.IS_NEW_IOS_VERSION_ENABLED)),
                                CommonDefects.isNull()
                        )
                )))
    }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("campaign is {0} and not copy")
    fun shouldValidateOkIfFlagIsNotPresent(testCase: String, campaign: CampaignWithNewIosVersionForbidden) {
        val container = mock(RestrictedCampaignsAddOperationContainer::class.java)

        val result = typeSupport.preValidate(container, ValidationResult(listOf(campaign)))

        assertThat(result).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("campaign is {0} and not copy")
    fun shouldValidateOkIfCopy(testCase: String, campaign: CampaignWithNewIosVersionForbidden) {
        val container = mock(RestrictedCampaignsAddOperationContainer::class.java)
        `when`(container.isCopy)
                .thenReturn(true)

        val result = typeSupport.preValidate(container, ValidationResult(listOf(campaign)))

        assertThat(result).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

}