package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.adv.direct.expression.keywords.KeywordEnum
import ru.yandex.adv.direct.expression.operations.OperationEnum
import ru.yandex.adv.direct.expression2.TargetingExpression
import ru.yandex.adv.direct.expression2.TargetingExpressionAtom
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.TimeAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.campaign.repository.CampaignMappings
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration


@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class AdGroupShowConditionsHandlerWithDbTest @Autowired constructor(
    private val steps: Steps,
    private val adGroupShowConditionsHandler: AdGroupShowConditionsHandler,
    private val campaignRepository: CampaignRepository
) {

    @Test
    fun `target time condition 1 adGroup`() {

        val clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct()
        val campaignInfo = steps.internalAutobudgetCampaignSteps().createDefaultCampaign(clientInfo)
        val placeId = campaignRepository.getCampaignInternalPlaces(campaignInfo.shard, listOf(campaignInfo.campaignId))[campaignInfo.campaignId]

        val internalAdGroup = activeInternalAdGroup(campaignInfo.getCampaignId())
        val adGroupInfo = steps.adGroupSteps().createAdGroup(internalAdGroup, campaignInfo)

        val builder = addTimeTargetToAdGroup(adGroupInfo, dataForTimeTargeting[0])


        val expectedResource = TargetingExpression.newBuilder()
            .addAnd(
                TargetingExpression.Disjunction.newBuilder().addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.PlaceId.number)
                        .setOperation(OperationEnum.Equal.number)
                        .setValue(placeId.toString())
                )
            )
            .addAnd(
                TargetingExpression.Disjunction.newBuilder().addOr(
                    TargetingExpressionAtom.newBuilder()
                        .setKeyword(KeywordEnum.RegId.number)
                        .setOperation(OperationEnum.Equal.number)
                        .setValue(internalAdGroup.geo[0].toString())
                )
            ).addAnd(builder)

        AdGroupHandlerAssertions.assertAdGroupsHandledCorrectly(
            adGroupShowConditionsHandler,
            listOf(internalAdGroup),
            listOf(
                ru.yandex.adv.direct.adgroup.AdGroup.newBuilder()
                    .setShowConditions(expectedResource).buildPartial()
            ),
        )
    }

    @Test
    fun `target time condition 2 adGroups`() {
        val clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct()
        val campaignInfo = steps.internalAutobudgetCampaignSteps().createDefaultCampaign(clientInfo)
        val placeId = campaignRepository.getCampaignInternalPlaces(campaignInfo.shard, listOf(campaignInfo.campaignId))[campaignInfo.campaignId]

        val internalAdGroups = listOf(activeInternalAdGroup(campaignInfo.getCampaignId()), activeInternalAdGroup(campaignInfo.getCampaignId()))
        val adGroupInfos = listOf(
            steps.adGroupSteps().createAdGroup(internalAdGroups[0], campaignInfo),
            steps.adGroupSteps().createAdGroup(internalAdGroups[1], campaignInfo),
        )

        val builders = listOf(
            addTimeTargetToAdGroup(adGroupInfos[0], dataForTimeTargeting[1]),
            addTimeTargetToAdGroup(adGroupInfos[1], dataForTimeTargeting[2])
        )

        var expectedResources = emptyList<ru.yandex.adv.direct.adgroup.AdGroup>()

        for (i in 0..1) {
            val adGroup = internalAdGroups[i]

            val expectedResource = TargetingExpression.newBuilder()
                .addAnd(
                    TargetingExpression.Disjunction.newBuilder().addOr(
                        TargetingExpressionAtom.newBuilder()
                            .setKeyword(KeywordEnum.PlaceId.number)
                            .setOperation(OperationEnum.Equal.number)
                            .setValue(placeId.toString())
                    )
                )
                .addAnd(
                    TargetingExpression.Disjunction.newBuilder().addOr(
                        TargetingExpressionAtom.newBuilder()
                            .setKeyword(KeywordEnum.RegId.number)
                            .setOperation(OperationEnum.Equal.number)
                            .setValue(adGroup.geo[0].toString())
                    )
                ).addAnd(builders[i])

            expectedResources = expectedResources.plus(
                ru.yandex.adv.direct.adgroup.AdGroup.newBuilder()
                    .setShowConditions(expectedResource).buildPartial()
            )
        }


        AdGroupHandlerAssertions.assertAdGroupsHandledCorrectly(
            adGroupShowConditionsHandler,
            internalAdGroups,
            expectedResources,
        )
    }

    private fun addTimeTargetToAdGroup(adGroupInfo: AdGroupInfo, dataForTimeTargeting: DataForTimeTargeting): TargetingExpression.Disjunction.Builder {
        val timeAdGroupAdditionalTargeting = TimeAdGroupAdditionalTargeting().withTargetingMode(dataForTimeTargeting.targetingMode).withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
        timeAdGroupAdditionalTargeting.value = listOf(CampaignMappings.timeTargetFromDb(dataForTimeTargeting.timetable))
        steps.adGroupAdditionalTargetingSteps().addValidTargetingsToAdGroup(adGroupInfo, listOf(timeAdGroupAdditionalTargeting))

        val builder = TargetingExpression.Disjunction.newBuilder()
        dataForTimeTargeting.values.forEach {
            builder.addOr(
                TargetingExpressionAtom.newBuilder()
                    .setKeyword(dataForTimeTargeting.keyword.number)
                    .setOperation(dataForTimeTargeting.operation.number)
                    .setValue(it)
            )
        }

        return builder
    }

    private class DataForTimeTargeting(
        val timetable: String,
        val keyword: KeywordEnum,
        val operation: OperationEnum,
        val values: List<String>
    ) {
        // TimeTarget работает только в режиме TARGETING
        val targetingMode = AdGroupAdditionalTargetingMode.TARGETING
    }

    private val dataForTimeTargeting = listOf(
        DataForTimeTargeting(
            "2BbCbDb3BcCcDc4BdCdDd",
            KeywordEnum.Timetable,
            OperationEnum.TimeNotLike,
            listOf("1567AEFGHIJKLMNOPQRSTUVWX"),
        ),
        DataForTimeTargeting(
            "2BCD3BCD4BCD8ABCD",
            KeywordEnum.Timetable,
            OperationEnum.TimeLike,
            listOf("234BCD", "8ABCD"),
        ),
        DataForTimeTargeting(
            "2BbCbD3BCD6A7BC9",
            KeywordEnum.TimetableSimple,
            OperationEnum.TimeNotLike,
            listOf("123456ADEFGHIJKLMNOPQRSTUVWX", "123457BCDEFGHIJKLMNOPQRSTUVWX", "14567AEFGHIJKLMNOPQRSTUVWX"),
        )
    )
}
