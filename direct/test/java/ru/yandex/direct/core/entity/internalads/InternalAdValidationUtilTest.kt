package ru.yandex.direct.core.entity.internalads

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.banner.model.InternalBanner
import ru.yandex.direct.core.entity.banner.model.TemplateVariable
import ru.yandex.direct.core.entity.internalads.Constants.CLOSE_BY_AD_GROUP_COUNTER_VALUE
import ru.yandex.direct.core.entity.internalads.Constants.CLOSE_BY_CAMPAIGN_COUNTER_VALUE
import ru.yandex.direct.core.entity.internalads.model.InternalTemplateInfo
import ru.yandex.direct.core.entity.internalads.model.ResourceInfo
import ru.yandex.direct.core.entity.internalads.model.ResourceType
import ru.yandex.direct.test.utils.randomPositiveLong

private const val TEXT_TEMPLATE_RESOURCE_ID = 1L
private const val CLOSE_COUNTER_TEMPLATE_RESOURCE_ID = 2L
private const val NON_EXISTENT_RESOURCE_ID = Long.MAX_VALUE;

@RunWith(JUnitParamsRunner::class)
class InternalAdValidationUtilTest {

    private val templateInfo = InternalTemplateInfo()
            .withTemplateId(randomPositiveLong())
            .withResources(listOf(
                    ResourceInfo()
                            .withId(TEXT_TEMPLATE_RESOURCE_ID)
                            .withType(ResourceType.TEXT)
                            .withLabel("this is text")
                            .withValueRestrictions(listOf()),
                    ResourceInfo()
                            .withId(CLOSE_COUNTER_TEMPLATE_RESOURCE_ID)
                            .withType(ResourceType.CLOSE_COUNTER)
                            .withLabel("this is close counter")
                            .withValueRestrictions(emptyList())
            ))
            .withResourceRestrictions(emptyList())

    private lateinit var banner: InternalBanner
    private lateinit var closeCounterVariable: TemplateVariable


    @Before
    fun initTestData() {
        closeCounterVariable = TemplateVariable()
                .withTemplateResourceId(CLOSE_COUNTER_TEMPLATE_RESOURCE_ID)
                .withInternalValue(null)
        banner = InternalBanner()
                .withId(randomPositiveLong())
                .withTemplateId(templateInfo.templateId)
                .withTemplateVariables(listOf(
                        TemplateVariable()
                                .withTemplateResourceId(TEXT_TEMPLATE_RESOURCE_ID)
                                .withInternalValue("some text"),
                        //для проверки фильтрации отсутствующих ресурсов
                        TemplateVariable()
                                .withTemplateResourceId(NON_EXISTENT_RESOURCE_ID)
                                .withInternalValue("123"),
                        closeCounterVariable
                ))
    }

    fun parametrizedTestData(): List<List<Any?>> = listOf(
            listOf("without closeCounter", null, CLOSE_BY_AD_GROUP_COUNTER_VALUE, false),
            listOf("with campaignsCloseCounter but expected for adGroup", CLOSE_BY_CAMPAIGN_COUNTER_VALUE,
                    CLOSE_BY_AD_GROUP_COUNTER_VALUE, false),
            listOf("with adGroupsCloseCounter but expected for campaign", CLOSE_BY_AD_GROUP_COUNTER_VALUE,
                    CLOSE_BY_CAMPAIGN_COUNTER_VALUE, false),

            listOf("with campaignsCloseCounter and expected for campaign", CLOSE_BY_CAMPAIGN_COUNTER_VALUE,
                    CLOSE_BY_CAMPAIGN_COUNTER_VALUE, true),
            listOf("with adGroupsCloseCounter and expected for adGroup", CLOSE_BY_AD_GROUP_COUNTER_VALUE,
                    CLOSE_BY_AD_GROUP_COUNTER_VALUE, true)
    )

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("checkGetBannerWithCloseCounterVarIds: {0}")
    fun checkGetBannerWithCloseCounterVarIds(@Suppress("UNUSED_PARAMETER") description: String,
                                             closeCounterValue: String?,
                                             expectedCloseCounterValue: String,
                                             hasResultBannerIds: Boolean) {
        closeCounterVariable.internalValue = closeCounterValue
        val bannerWithCloseCounterVarIds = InternalAdValidationUtil
                .getBannerWithCloseCounterVarIds(listOf(banner), listOf(templateInfo), expectedCloseCounterValue)

        val expectedResult = if (hasResultBannerIds) listOf(banner.id) else emptyList()
        assertThat(bannerWithCloseCounterVarIds)
                .isEqualTo(expectedResult)
    }

}
