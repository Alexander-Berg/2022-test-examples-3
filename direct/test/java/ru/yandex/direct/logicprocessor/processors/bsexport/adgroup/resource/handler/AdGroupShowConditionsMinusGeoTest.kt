package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.adv.direct.adgroup.AdGroup
import ru.yandex.adv.direct.expression.keywords.KeywordEnum
import ru.yandex.adv.direct.expression.operations.OperationEnum
import ru.yandex.adv.direct.expression2.TargetingExpressionAtom
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration

@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class AdGroupShowConditionsMinusGeoTest @Autowired constructor(
    private val steps: Steps,
    private val adGroupShowConditionsHandler: AdGroupShowConditionsHandler,
    private val ppcPropertiesSupport: PpcPropertiesSupport
) {
    private lateinit var adGroupInfo: AdGroupInfo
    private lateinit var builder: AdGroup.Builder
    private lateinit var adGroupWithBuilder: AdGroupWithBuilder

    private fun initWithGeo(adGroupGeo: List<Long>, bannerMinusGeo: List<Long>) {
        adGroupInfo = createCampaignAndAdGroup(adGroupGeo, bannerMinusGeo)
        builder = AdGroup.newBuilder().setAdGroupId(adGroupInfo.adGroupId)
        adGroupWithBuilder = AdGroupWithBuilder(adGroupInfo.adGroup, builder)
    }

    @Test
    fun testDontIgnoreUserGeo() {
        initWithGeo(listOf(225, -102444), listOf(235L))

        ppcPropertiesSupport.set(PpcPropertyNames.IGNORE_MINUS_GEO_IN_ADGROUP_CONDITIONS_CID_PERCENT, "100")
        ppcPropertiesSupport.remove(PpcPropertyNames.IGNORE_MINUS_GEO_IN_ADGROUP_CONDITIONS_CIDS)

        adGroupShowConditionsHandler.handle(adGroupInfo.shard, mapOf(adGroupInfo.adGroupId to adGroupWithBuilder))

        assertTrue(builder.showConditions.andList.any { it.orList[0].hasIncludedRegion(225) })
        assertTrue(builder.showConditions.andList.any { it.orList[0].hasExcludedRegion(102444) })
        assertFalse(builder.showConditions.andList.any { it.orList[0].hasRegion(235) })
    }

    @Test
    fun testIgnoreDisabled() {
        initWithGeo(listOf(225), listOf(235L))

        ppcPropertiesSupport.remove(PpcPropertyNames.IGNORE_MINUS_GEO_IN_ADGROUP_CONDITIONS_CID_PERCENT)
        ppcPropertiesSupport.remove(PpcPropertyNames.IGNORE_MINUS_GEO_IN_ADGROUP_CONDITIONS_CIDS)

        adGroupShowConditionsHandler.handle(adGroupInfo.shard, mapOf(adGroupInfo.adGroupId to adGroupWithBuilder))

        assertTrue(builder.showConditions.andList.any { it.orList[0].hasIncludedRegion(225) })
        assertTrue(builder.showConditions.andList.any { it.orList[0].hasExcludedRegion(235) })
    }

    @Test
    fun testIgnoreEnabledByPercent() {
        initWithGeo(listOf(225), listOf(235L))

        ppcPropertiesSupport.set(PpcPropertyNames.IGNORE_MINUS_GEO_IN_ADGROUP_CONDITIONS_CID_PERCENT, "100")
        ppcPropertiesSupport.remove(PpcPropertyNames.IGNORE_MINUS_GEO_IN_ADGROUP_CONDITIONS_CIDS)

        adGroupShowConditionsHandler.handle(adGroupInfo.shard, mapOf(adGroupInfo.adGroupId to adGroupWithBuilder))

        assertTrue(builder.showConditions.andList.any { it.orList[0].hasIncludedRegion(225) })
        assertFalse(builder.showConditions.andList.any { it.orList[0].hasRegion(235) })
    }

    @Test
    fun testIgnoreEnabledByCid() {
        initWithGeo(listOf(225), listOf(235L))

        ppcPropertiesSupport.set(PpcPropertyNames.IGNORE_MINUS_GEO_IN_ADGROUP_CONDITIONS_CID_PERCENT, "0")
        ppcPropertiesSupport.set(
            PpcPropertyNames.IGNORE_MINUS_GEO_IN_ADGROUP_CONDITIONS_CIDS,
            adGroupInfo.campaignId.toString()
        )

        adGroupShowConditionsHandler.handle(adGroupInfo.shard, mapOf(adGroupInfo.adGroupId to adGroupWithBuilder))

        assertTrue(builder.showConditions.andList.any { it.orList[0].hasIncludedRegion(225) })
        assertFalse(builder.showConditions.andList.any { it.orList[0].hasRegion(235) })
    }

    private fun createCampaignAndAdGroup(adGroupGeo: List<Long>, bannerMinusGeo: List<Long>): AdGroupInfo {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val campaign = TestCampaigns.defaultTextCampaignWithSystemFields()
        val campaignInfo = steps.textCampaignSteps().createCampaign(clientInfo, campaign)
        val adGroup = steps.adGroupSteps().createAdGroup(
            AdGroupInfo()
                .withAdGroup(
                    TestGroups.activeTextAdGroup(null)
                        .withGeo(adGroupGeo)
                )
                .withCampaignInfo(campaignInfo)
        )
        steps.bannerSteps().createDefaultTextBannerWithMinusGeo(adGroup, bannerMinusGeo)
        return adGroup
    }

    private fun TargetingExpressionAtom.hasRegion(regionId: Long): Boolean {
        return this.value == regionId.toString()
            && this.keyword == KeywordEnum.RegId.number
    }

    private fun TargetingExpressionAtom.hasIncludedRegion(regionId: Long): Boolean {
        return this.value == regionId.toString()
            && this.operation == OperationEnum.Equal.number
            && this.keyword == KeywordEnum.RegId.number
    }

    private fun TargetingExpressionAtom.hasExcludedRegion(regionId: Long): Boolean {
        return this.value == regionId.toString()
            && this.operation == OperationEnum.NotEqual.number
            && this.keyword == KeywordEnum.RegId.number
    }
}
