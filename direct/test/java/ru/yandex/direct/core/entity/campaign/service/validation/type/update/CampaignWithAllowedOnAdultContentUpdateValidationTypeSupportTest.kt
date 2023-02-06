package ru.yandex.direct.core.entity.campaign.service.validation.type.update

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.campaign.model.CampaignWithAllowedOnAdultContent
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
class CampaignWithAllowedOnAdultContentUpdateValidationTypeSupportTest {

    @InjectMocks
    lateinit var typeSupport: CampaignWithAllowedOnAdultContentUpdateValidationTypeSupport

    @Mock
    lateinit var featureService: FeatureService

    @Before
    fun setUp() = MockitoAnnotations.initMocks(this)

    fun testData() = listOf(
        listOf(
            true,
            false,
            hasNoDefectsDefinitions<Any>()
        ),
        listOf(
            true,
            true,
            hasNoDefectsDefinitions<Any>()
        ),
        listOf(
            false,
            false,
            hasNoDefectsDefinitions<Any>()
        ),
        listOf(
            false,
            true,
            hasDefectWithDefinition<Any>(
                validationError(
                    path(index(0), field(CampaignWithAllowedOnAdultContent.IS_ALLOWED_ON_ADULT_CONTENT)),
                    Defect(DefectIds.FORBIDDEN_TO_CHANGE)
                )
            )
        )
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("When feature is {0} and campaign has changes is {1} then result is {2}")
    fun shouldValidate(featureEnabled: Boolean, campaignHasChanges: Boolean, matcher: Matcher<Any>) {
        val container = mock(RestrictedCampaignsUpdateOperationContainer::class.java)
        val campaignId = RandomNumberUtils.nextPositiveLong()
        var mc = ModelChanges(campaignId, CampaignWithAllowedOnAdultContent::class.java)
        if (campaignHasChanges) {
            mc = mc.process(true, CampaignWithAllowedOnAdultContent.IS_ALLOWED_ON_ADULT_CONTENT)
        }

        `when`(featureService.isEnabledForClientId(any(), eq(FeatureName.CAMPAIGN_ALLOWED_ON_ADULT_CONTENT)))
            .thenReturn(featureEnabled)

        val result = typeSupport.preValidate(container, ValidationResult(listOf(mc)))

        assertThat(result).`is`(matchedBy(matcher))
    }

}
