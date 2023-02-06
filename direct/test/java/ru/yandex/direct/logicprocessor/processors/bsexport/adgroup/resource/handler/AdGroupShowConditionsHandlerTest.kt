package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isA
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.adv.direct.expression.keywords.KeywordEnum
import ru.yandex.adv.direct.expression.operations.OperationEnum
import ru.yandex.adv.direct.expression2.TargetingExpression
import ru.yandex.adv.direct.expression2.TargetingExpressionAtom
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.bsexport.model.BsExportBidRetargeting
import ru.yandex.direct.core.bsexport.repository.adgroup.showconditions.BsExportAdgroupShowConditionsGeoRepository
import ru.yandex.direct.core.bsexport.repository.bids.BsExportBidsRetargetingRepository
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.TimeAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPlacementTypes
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.PlacementType
import ru.yandex.direct.core.entity.campaign.repository.CampaignMappings
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository
import ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler.AdGroupHandlerAssertions.assertAdGroupHandledCorrectly
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration
import ru.yandex.direct.regions.GeoTreeFactory

@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class AdGroupShowConditionsHandlerTest {
    private val adGroupRepository = mock<AdGroupRepository>()
    private val mobileContentRepository = mock<MobileContentRepository>()
    private val campaignRepository = mock<CampaignRepository>()
    private val campaignTypedRepository = mock<CampaignTypedRepository>()
    private val bidsRetargetingRepository = mock<BsExportBidsRetargetingRepository>()
    private val showConditionsGeoRepository = mock<BsExportAdgroupShowConditionsGeoRepository>()
    private val targetingRepository = mock<AdGroupAdditionalTargetingRepository>()
    private val geoTreeFactory = mock<GeoTreeFactory>()
    private val additionalTargetingTypeConfigGenerator = mock<AdditionalTargetingTypeConfigGenerator>()

    private val ppcPropertiesSupport = mock<PpcPropertiesSupport> {
        val rfOptionsExportByFilterEnabledProp = mock<PpcProperty<Boolean>> {
            on(mock.getOrDefault(any())) doReturn true
        }
        val filterByClientIdsProp = mock<PpcProperty<Set<Long>>> {
            on(mock.getOrDefault(any())) doReturn emptySet()
        }
        on(mock.get(
            eq(PpcPropertyNames.BS_EXPORT_RF_OPTIONS_FILTER_INTERNAL_AD_GROUPS_ENABLED), any()
        )) doReturn rfOptionsExportByFilterEnabledProp
        on(mock.get(
            eq(PpcPropertyNames.BS_EXPORT_RF_OPTIONS_FILTER_BY_CAMPAIGN_IDS), any()
        )) doReturn filterByClientIdsProp
    }

    private val handler = AdGroupShowConditionsHandler(
        adGroupRepository,
        mobileContentRepository,
        campaignRepository,
        campaignTypedRepository,
        bidsRetargetingRepository,
        showConditionsGeoRepository,
        targetingRepository,
        geoTreeFactory,
        additionalTargetingTypeConfigGenerator,
        ppcPropertiesSupport
    )

    private val shard = 1
    private val pid = 2L
    private val cid = 4L
    private val placeId = 8L
    private val retCondId = 16L

    @Test
    fun `goal context condition`() {
        whenever(bidsRetargetingRepository.getBidsByPids(eq(shard), any()))
            .thenReturn(listOf(BsExportBidRetargeting().withAdGroupId(pid).withRetCondId(retCondId)))
        assertAdGroupHandledCorrectly(
            handler,
            adGroup = AdGroup().withId(pid).withCampaignId(cid)
                .withType(AdGroupType.INTERNAL),
            expectedProto = ru.yandex.adv.direct.adgroup.AdGroup.newBuilder()
                .setShowConditions(
                    TargetingExpression.newBuilder()
                        .addAnd(
                            TargetingExpression.Disjunction.newBuilder()
                                .addOr(
                                    TargetingExpressionAtom.newBuilder()
                                        .setKeyword(KeywordEnum.GoalContextId.number)
                                        .setOperation(OperationEnum.MatchGoalContext.number)
                                        .setValue(retCondId.toString())
                                )
                        )
                ).setContextId(4831237562181579474L).buildPartial()
        )
    }

    @Test
    fun `performance condition`() = assertAdGroupHandledCorrectly(
        handler,
        adGroup = AdGroup().withId(pid).withCampaignId(cid)
            .withType(AdGroupType.PERFORMANCE),
        expectedProto = ru.yandex.adv.direct.adgroup.AdGroup.newBuilder()
            .setShowConditions(
                TargetingExpression.newBuilder()
                    .addAnd(
                        TargetingExpression.Disjunction.newBuilder()
                            .addOr(
                                TargetingExpressionAtom.newBuilder()
                                    .setKeyword(KeywordEnum.Performance.number)
                                    .setOperation(OperationEnum.Equal.number)
                                    .setValue("1")
                            )
                    )
            ).setContextId(9029891993359764238L).buildPartial()
    )

    @Test
    fun `place condition`() {
        whenever(campaignRepository.getCampaignsTypeMap(eq(shard), any()))
            .thenReturn(mapOf(cid to CampaignType.INTERNAL_FREE))
        whenever(campaignRepository.getCampaignInternalPlaces(eq(shard), any()))
            .thenReturn(mapOf(cid to placeId))
        assertAdGroupHandledCorrectly(
            handler,
            adGroup = AdGroup().withType(AdGroupType.INTERNAL).withId(pid).withCampaignId(cid),
            expectedProto = ru.yandex.adv.direct.adgroup.AdGroup.newBuilder()
                .setShowConditions(
                    TargetingExpression.newBuilder()
                        .addAnd(
                            TargetingExpression.Disjunction.newBuilder()
                                .addOr(
                                    TargetingExpressionAtom.newBuilder()
                                        .setKeyword(KeywordEnum.PlaceId.number)
                                        .setOperation(OperationEnum.Equal.number)
                                        .setValue(placeId.toString())
                                )
                        )
                ).setContextId(4646050104616429208L).buildPartial()
        )
    }

    @Test
    fun `target time condition`() {
        val timetable = "2BCD3BCD4BCD8ABCD"
        val keyword = KeywordEnum.Timetable
        val operation = OperationEnum.TimeLike
        val values = listOf("234BCD", "8ABCD")

        val timeTargetings = listOf(
            TimeAdGroupAdditionalTargeting().withAdGroupId(pid + 1).withValue(listOf(
                mock(), mock(), mock()
            )),
            TimeAdGroupAdditionalTargeting().withAdGroupId(pid - 1).withValue(listOf(
                mock()
            )),
            TimeAdGroupAdditionalTargeting().withAdGroupId(pid).withValue(listOf(
                CampaignMappings.timeTargetFromDb(timetable)
            )),
        )

        Mockito.`when`(targetingRepository.getByAdGroupIdsAndType(Mockito.anyInt(), any(), any())).thenReturn(timeTargetings)

        whenever(campaignRepository.getCampaignsTypeMap(eq(shard), any()))
            .thenReturn(mapOf(cid to CampaignType.INTERNAL_FREE))
        whenever(campaignRepository.getCampaignInternalPlaces(eq(shard), any()))
            .thenReturn(mapOf(cid to placeId))

        val expectedResource = TargetingExpression.newBuilder()
            .addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(
                        TargetingExpressionAtom.newBuilder()
                            .setKeyword(KeywordEnum.PlaceId.number)
                            .setOperation(OperationEnum.Equal.number)
                            .setValue(placeId.toString())
                    )
            )

        val builder = TargetingExpression.Disjunction.newBuilder()
        values.forEach {
            builder.addOr(
                TargetingExpressionAtom.newBuilder()
                    .setKeyword(keyword.number)
                    .setOperation(operation.number)
                    .setValue(it)

            )
        }

        expectedResource.addAnd(builder)

        assertAdGroupHandledCorrectly(
            handler,
            adGroup = AdGroup().withId(2L).withCampaignId(4L).withType(AdGroupType.INTERNAL),
            expectedProto = ru.yandex.adv.direct.adgroup.AdGroup.newBuilder()
                .setShowConditions(expectedResource).setContextId(5632521693420149514L).buildPartial()
        )
    }


    @Test
    fun `target tags condition`() {
        whenever(campaignTypedRepository.getSafely(eq(shard), isA<Collection<Long>>(), eq(CampaignWithPlacementTypes::class.java)))
            .thenReturn(listOf(DynamicCampaign().withId(cid).withPlacementTypes(setOf(PlacementType.ADV_GALLERY))))
        assertAdGroupHandledCorrectly(
            handler,
            adGroup = AdGroup().withId(pid).withCampaignId(cid)
                .withType(AdGroupType.DYNAMIC)
                .withTargetTags(listOf("tag1", "tag2")),
            expectedProto = ru.yandex.adv.direct.adgroup.AdGroup.newBuilder()
                .setShowConditions(
                    TargetingExpression.newBuilder()
                        .addAnd(
                            TargetingExpression.Disjunction.newBuilder()
                                .addOr(
                                    TargetingExpressionAtom.newBuilder()
                                        .setKeyword(KeywordEnum.TargetTags.number)
                                        .setOperation(OperationEnum.Match.number)
                                        .setValue("bko-only")
                                ).addOr(
                                    TargetingExpressionAtom.newBuilder()
                                        .setKeyword(KeywordEnum.TargetTags.number)
                                        .setOperation(OperationEnum.Match.number)
                                        .setValue("tag1")
                                ).addOr(
                                    TargetingExpressionAtom.newBuilder()
                                        .setKeyword(KeywordEnum.TargetTags.number)
                                        .setOperation(OperationEnum.Match.number)
                                        .setValue("tag2")
                                )
                        )
                ).setContextId(6307735040360524092L).buildPartial()
        )
    }

    @Test
    fun `complex condition`() {
        whenever(campaignRepository.getCampaignsTypeMap(eq(shard), any()))
            .thenReturn(mapOf(cid to CampaignType.INTERNAL_FREE))
        whenever(campaignRepository.getCampaignInternalPlaces(eq(shard), any()))
            .thenReturn(mapOf(cid to placeId))
        assertAdGroupHandledCorrectly(
            handler,
            adGroup = AdGroup().withId(pid).withCampaignId(cid)
                .withType(AdGroupType.PERFORMANCE)
                .withTargetTags(listOf("tag2", "tag1")),
            expectedProto = ru.yandex.adv.direct.adgroup.AdGroup.newBuilder()
                .setShowConditions(
                    TargetingExpression.newBuilder()
                        .addAnd(
                            TargetingExpression.Disjunction.newBuilder()
                                .addOr(
                                    TargetingExpressionAtom.newBuilder()
                                        .setKeyword(KeywordEnum.PlaceId.number)
                                        .setOperation(OperationEnum.Equal.number)
                                        .setValue(placeId.toString())
                                )
                        ).addAnd(
                            TargetingExpression.Disjunction.newBuilder()
                                .addOr(
                                    TargetingExpressionAtom.newBuilder()
                                        .setKeyword(KeywordEnum.Performance.number)
                                        .setOperation(OperationEnum.Equal.number)
                                        .setValue("1")
                                )
                        ).addAnd(
                            TargetingExpression.Disjunction.newBuilder()
                                .addOr(
                                    TargetingExpressionAtom.newBuilder()
                                        .setKeyword(KeywordEnum.TargetTags.number)
                                        .setOperation(OperationEnum.Match.number)
                                        .setValue("tag1")
                                ).addOr(
                                    TargetingExpressionAtom.newBuilder()
                                        .setKeyword(KeywordEnum.TargetTags.number)
                                        .setOperation(OperationEnum.Match.number)
                                        .setValue("tag2")
                                )
                        )

                ).setContextId(6728345693099761263L).buildPartial()
        )
    }
}

