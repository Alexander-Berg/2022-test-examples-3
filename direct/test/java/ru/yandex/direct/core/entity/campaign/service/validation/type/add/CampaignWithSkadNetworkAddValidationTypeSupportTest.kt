package ru.yandex.direct.core.entity.campaign.service.validation.type.add

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
class CampaignWithSkadNetworkAddValidationTypeSupportTest {

    @InjectMocks
    lateinit var typeSupport: CampaignWithSkadNetworkAddValidationTypeSupport

    @Mock
    lateinit var featureService: FeatureService

    @Before
    fun setUp() = MockitoAnnotations.initMocks(this)

    fun testData() = listOf(
        listOf(
            true,
            Matchers.hasNoDefectsDefinitions<Any>()
        ),
        listOf(
            false,
            Matchers.hasDefectWithDefinition<Any>(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0)),
                    Defect(DefectIds.INVALID_VALUE)
                )
            )
        )
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("When feature is {0} then result is {2}")
    fun shouldValidate(featureEnabled: Boolean, matcher: Matcher<Any>) {
        val container = Mockito.mock(RestrictedCampaignsAddOperationContainer::class.java)
        val campaign = MobileContentCampaign().withIsSkadNetworkEnabled(true)

        Mockito.`when`(featureService.isEnabledForClientId(Mockito.any(), ArgumentMatchers.eq(FeatureName.SHOW_SKADNETWORK_ON_NEW_IOS_ENABLED)))
            .thenReturn(featureEnabled)

        val result = typeSupport.preValidate(container, ValidationResult(listOf(campaign)))

        Assertions.assertThat(result).`is`(Conditions.matchedBy(matcher))
    }
}
