package ru.yandex.direct.core.entity.campaign.service.validation.type.add

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.campaign.model.CampaignWithNewIosVersion
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.feature.FeatureName
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
class CampaignWithNewIosVersionAddValidationTypeSupportTest {

    @InjectMocks
    lateinit var typeSupport: CampaignWithNewIosVersionAddValidationTypeSupport

    @Mock
    lateinit var featureService: FeatureService

    fun testData() = listOf(
            listOf(
                    true,
                    true,
                    hasNoDefectsDefinitions<Any>()
            ),
            listOf(
                    true,
                    false,
                    hasNoDefectsDefinitions<Any>()
            ),
            listOf(
                    false,
                    true,
                    hasNoDefectsDefinitions<Any>()
            ),
            listOf(
                    false,
                    false,
                    hasDefectWithDefinition<Any>(
                            validationError(
                                    path(index(0), field(CampaignWithNewIosVersion.IS_NEW_IOS_VERSION_ENABLED)),
                                    CommonDefects.isNull()
                            )
                    )
            )
    )

    @Before
    fun setUp() = MockitoAnnotations.initMocks(this)

    @Test
    @Parameters(method = "testData")
    @TestCaseName("When feature is {0} and copy is {1}, result = {2}")
    fun shouldValidate(featureEnabled: Boolean, isCopy: Boolean, matcher: Matcher<Any>) {
        val container = mock(RestrictedCampaignsAddOperationContainer::class.java)
        val campaign = MobileContentCampaign().withIsNewIosVersionEnabled(true)

        `when`(featureService.isEnabledForClientId(any(), eq(FeatureName.SHOW_RMP_ON_NEW_IOS_VERSION_ENABLED)))
                .thenReturn(featureEnabled)
        `when`(container.isCopy)
                .thenReturn(isCopy)

        val result = typeSupport.preValidate(container, ValidationResult(listOf(campaign)))

        assertThat(result).`is`(matchedBy(matcher))
    }
}