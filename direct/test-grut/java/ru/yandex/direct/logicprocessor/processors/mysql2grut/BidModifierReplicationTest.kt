package ru.yandex.direct.logicprocessor.processors.mysql2grut

import com.google.common.truth.extensions.proto.FieldScopes
import com.google.common.truth.extensions.proto.ProtoTruth
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegment
import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerType
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopOnly
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgo
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPrismaIncomeGrade
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingFilter
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTV
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPosition
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTraffic
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideo
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeather
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.ess.logicobjects.mysql2grut.BidModifierTableType
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.GrutReplicationSteps
import ru.yandex.grut.auxiliary.proto.YabsOperation
import ru.yandex.grut.objects.proto.AgeGroup.EAgeGroup
import ru.yandex.grut.objects.proto.BidModifier.EBidModifierType
import ru.yandex.grut.objects.proto.BidModifier.TBidModifierSpec
import ru.yandex.grut.objects.proto.BidModifier.TBidModifierSpec.EWeatherParameter
import ru.yandex.grut.objects.proto.BidModifier.TBidModifierSpec.TDemography
import ru.yandex.grut.objects.proto.BidModifier.TBidModifierSpec.TExpressionAtom
import ru.yandex.grut.objects.proto.Gender.EGender
import ru.yandex.grut.objects.proto.InventoryType.EInventoryType
import ru.yandex.grut.objects.proto.MobilePlatform.EMobilePlatform
import ru.yandex.grut.objects.proto.client.Schema
import ru.yandex.grut.objects.proto.client.Schema.TBidModifier

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class BidModifierReplicationTest {
    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationService: GrutApiService

    @Autowired
    private lateinit var grutSteps: GrutReplicationSteps

    @Autowired
    private lateinit var bidModifierService: BidModifierService

    @AfterEach
    fun tearDown() {
        grutSteps.cleanupClientsWithChildren()
    }

    @BeforeEach
    fun setup() {
        processor.withShard(grutSteps.DEFAULT_SHARD)
    }

    /**
     * Проверяет репликацию корректировок, у которых нет записей в *-value таблицах
     */
    @Test
    fun replicateAllSingleValuedBidModifiers() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()

        val desktopOnlyDirect = bidModifierSteps.createDefaultAdGroupBidModifierDesktopOnly(adGroup.adGroup)
        val desktopDirect = bidModifierSteps.createDefaultAdGroupBidModifierDesktop(adGroup.adGroup)
        val performanceTgoDirect = bidModifierSteps.createDefaultAdGroupBidModifierPerformanceTgo(adGroup.adGroup)
        val smartTVDirect = bidModifierSteps.createDefaultAdGroupBidModifierSmartTV(adGroup.adGroup)
        val videoDirect = bidModifierSteps.createDefaultAdGroupBidModifierVideo(adGroup.adGroup)

        val directModifiers = listOf(desktopOnlyDirect, desktopDirect, performanceTgoDirect, smartTVDirect, videoDirect)

        // act
        processor.process(
            directModifiers.map {
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = it.bidModifierId,
                )
            }
        )

        // assert
        val grutBidModifiers = replicationService.bidModifierGrutApi.getBidModifiers(
            directModifiers.map { it.bidModifierId }
        ).toList()
        val desktopOnly = grutBidModifiers[0]
        val desktop = grutBidModifiers[1]
        val performanceTgo = grutBidModifiers[2]
        val smartTV = grutBidModifiers[3]
        val video = grutBidModifiers[4]
        SoftAssertions.assertSoftly { softly ->
            for (grutModifier in grutBidModifiers) {
                softly.assertThat(desktopOnly.meta.clientId).isEqualTo(adGroup.adGroup.clientId.asLong())
            }
            // desktop only
            val desktopOnlyPercent = (desktopOnlyDirect.bidModifier as BidModifierDesktopOnly)
                .desktopOnlyAdjustment.percent.toLong()
            softly.assertThat(desktopOnly.meta.bidModifierType).isEqualTo(EBidModifierType.MLT_DESKTOP_ONLY.number)
            softly.assertThat(desktopOnly.spec.priceCoefficientPercent).isEqualTo(desktopOnlyPercent)
            // desktop
            val desktopPercent = (desktopDirect.bidModifier as BidModifierDesktop)
                .desktopAdjustment.percent.toLong()
            softly.assertThat(desktop.meta.bidModifierType).isEqualTo(EBidModifierType.MLT_DESKTOP.number)
            softly.assertThat(desktop.spec.priceCoefficientPercent).isEqualTo(desktopPercent)
            // performanceTgo
            val performanceTgoPecent = (performanceTgoDirect.bidModifier as BidModifierPerformanceTgo)
                .performanceTgoAdjustment.percent.toLong()
            softly.assertThat(performanceTgo.meta.bidModifierType)
                .isEqualTo(EBidModifierType.MLT_PERFORMANCE_TGO.number)
            softly.assertThat(performanceTgo.spec.priceCoefficientPercent).isEqualTo(performanceTgoPecent)
            //smartTV
            val smartTVPercent = (smartTVDirect.bidModifier as BidModifierSmartTV)
                .smartTVAdjustment.percent.toLong()
            softly.assertThat(smartTV.meta.bidModifierType).isEqualTo(EBidModifierType.MLT_SMARTTV.number)
            softly.assertThat(smartTV.spec.priceCoefficientPercent).isEqualTo(smartTVPercent)
            //video
            val videoPercent = (videoDirect.bidModifier as BidModifierVideo)
                .videoAdjustment.percent.toLong()
            softly.assertThat(video.meta.bidModifierType).isEqualTo(EBidModifierType.MLT_VIDEO.number)
            softly.assertThat(video.spec.priceCoefficientPercent).isEqualTo(videoPercent)
        }
    }

    @Test
    fun replicateExistingBidModifier() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val desktopOnlyDirect = bidModifierSteps.createDefaultAdGroupBidModifierDesktopOnly(adGroup.adGroup)
        val adjustment = (desktopOnlyDirect.bidModifier as BidModifierDesktopOnly).desktopOnlyAdjustment

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = desktopOnlyDirect.bidModifierId,
                )
            )
        )

        val desktopOnly = replicationService.bidModifierGrutApi.getBidModifier(desktopOnlyDirect.bidModifierId)
        assertThat(desktopOnly!!.spec.priceCoefficientPercent).isEqualTo(adjustment.percent.toLong())

        //update existing bid modifier in direct
        val newPercent = 25L;
        grutSteps.bidModifierTestRepository.updateHierarchicalMultiplierPercent(
            grutSteps.DEFAULT_SHARD,
            desktopOnlyDirect.bidModifierId,
            newPercent
        );

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = desktopOnlyDirect.bidModifierId,
                )
            )
        )

        val desktopOnlyAfterUpdate =
            replicationService.bidModifierGrutApi.getBidModifier(desktopOnlyDirect.bidModifierId)
        assertThat(desktopOnlyAfterUpdate!!.spec.priceCoefficientPercent).isEqualTo(newPercent)
    }

    @Test
    fun deleteBidModifierByHierarchicalMultiplierId() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val willBeDeleted = bidModifierSteps.createDefaultAdGroupBidModifierDesktopOnly(adGroup.adGroup)

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = willBeDeleted.bidModifierId,
                )
            )
        )
        assertThat(replicationService.bidModifierGrutApi.getBidModifier(willBeDeleted.bidModifierId)).isNotNull
        //delete bidModifier in MySql
        bidModifierService.deleteAdjustments(
            grutSteps.DEFAULT_SHARD,
            adGroup.adGroup.clientId,
            1L,
            listOf(willBeDeleted.bidModifier)
        )
        //act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = willBeDeleted.bidModifierId,
                    isDeleted = true,
                )
            )
        )
        assertThat(replicationService.bidModifierGrutApi.getBidModifier(willBeDeleted.bidModifierId)).isNull()
    }

    @Test
    fun deleteBidModifierByValueId() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val bidModifier = bidModifierSteps.createDefaultAdGroupBidModifierGeo(adGroup.adGroup)
        val adjustment = (bidModifier.bidModifier as BidModifierGeo).regionalAdjustments.first()

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.GEO,
                    bidModifierId = adjustment.id,
                )
            )
        )
        assertThat(replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)).isNotNull
        //delete bidModifier in MySql
        bidModifierService.deleteAdjustments(
            grutSteps.DEFAULT_SHARD,
            adGroup.adGroup.clientId,
            1L,
            listOf(bidModifier.bidModifier)
        )
        //act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.GEO,
                    bidModifierId = adjustment.id,
                    isDeleted = true,
                )
            )
        )
        assertThat(replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)).isNull()
    }

    /**
     * Проверяет репликацию корректировок с уточнениями - при изменении в hierarchical_multiplier
     */
    @Test
    fun replicateBidModifiersWithAdjustments_ParentFieldChanged() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val demographyInfo =
            bidModifierSteps.createAdGroupBidModifierDemographicsWithMulipleAdjustments(adGroup.adGroup)
        var demography = demographyInfo.bidModifier as BidModifierDemographics

        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = demographyInfo.bidModifierId,
                )
            )
        )
        // assert
        val grutBidModifiers = replicationService.bidModifierGrutApi.getBidModifiers(
            listOf(
                demographyInfo.bidModifierId as Long,
                demography.demographicsAdjustments[0].id,
                demography.demographicsAdjustments[1].id,
            )
        )
        assertThat(grutBidModifiers.size)
            .`as`("we don't save hierarchical_multiplier record for multi-valued bid modifiers")
            .isEqualTo(demography.demographicsAdjustments.size)

        val expectedDemographyFirst = TDemography.newBuilder()
            .setAgeGroup(EAgeGroup.AG_25_34.number)
            .setGender(EGender.G_MALE.number)
            .build()!!

        val expectedDemographySecond = TDemography.newBuilder()
            .setAgeGroup(EAgeGroup.AG_18_24_VALUE)
            .setGender(EGender.G_FEMALE.number)
            .build()!!

        val idToExpectedDemography = mapOf(
            demography.demographicsAdjustments[0].id to expectedDemographyFirst,
            demography.demographicsAdjustments[1].id to expectedDemographySecond,
        )

        SoftAssertions.assertSoftly { softly ->
            grutBidModifiers.forEachIndexed { i, adjustmentGrut ->
                val adjustment = demography.demographicsAdjustments[i]
                softly.assertThat(adjustmentGrut.meta.id).isEqualTo(adjustment.id.toLong())
                softly.assertThat(adjustmentGrut.meta.bidModifierType).isEqualTo(EBidModifierType.MLT_DEMOGRAPHY.number)
                softly.assertThat(adjustmentGrut.meta.clientId).isEqualTo(demographyInfo.clientId.asLong())
                softly.assertThat(adjustmentGrut.spec.priceCoefficientPercent).isEqualTo(adjustment.percent.toLong())

                softly.assertThat(adjustmentGrut.spec.demography)
                    .isEqualTo(idToExpectedDemography[adjustmentGrut.meta.id])
            }
        }
    }

    @Test
    fun replicateMobileBidModifierOsNotSpecified() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val mobileInfo = bidModifierSteps.createDefaultAdGroupBidModifierMobile(adGroup.adGroup)
        val mobileAdjustment = (mobileInfo.bidModifier as BidModifierMobile).mobileAdjustment

        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = mobileInfo.bidModifierId,
                )
            )
        )
        // assert

        // when mobile os isn't specified we replicate hierarchical_multiplier data
        val bidModifierGrut = replicationService.bidModifierGrutApi.getBidModifier(mobileInfo.bidModifierId)
        Assertions.assertNotNull(bidModifierGrut)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(bidModifierGrut!!.meta.id).isEqualTo(mobileInfo.bidModifierId.toLong())
            softly.assertThat(bidModifierGrut.meta.bidModifierType).isEqualTo(EBidModifierType.MLT_MOBILE.number)
            softly.assertThat(bidModifierGrut.meta.clientId).isEqualTo(mobileInfo.clientId.asLong())
            softly.assertThat(bidModifierGrut.spec.priceCoefficientPercent).isEqualTo(mobileAdjustment.percent.toLong())

            softly.assertThat(bidModifierGrut.spec.number).isEqualTo(EMobilePlatform.MP_UNKNOWN.number.toLong())
        }
    }

    @Test
    fun replicateMobileBidModifierOsSpecified() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val mobileInfo = bidModifierSteps.createDefaultAdGroupIosBidModifierMobile(adGroup.adGroup)
        val mobileAdjustment = (mobileInfo.bidModifier as BidModifierMobile).mobileAdjustment

        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = mobileInfo.bidModifierId,
                )
            )
        )

        // assert
        val bidModifierGrut = replicationService.bidModifierGrutApi.getBidModifier(mobileAdjustment.id)
        Assertions.assertNotNull(bidModifierGrut)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(bidModifierGrut!!.meta.id).isEqualTo(mobileAdjustment.id)
            softly.assertThat(bidModifierGrut.meta.bidModifierType).isEqualTo(EBidModifierType.MLT_MOBILE.number)
            softly.assertThat(bidModifierGrut.meta.clientId).isEqualTo(mobileInfo.clientId.asLong())
            softly.assertThat(bidModifierGrut.spec.priceCoefficientPercent).isEqualTo(mobileAdjustment.percent.toLong())

            softly.assertThat(bidModifierGrut.spec.number).isEqualTo(EMobilePlatform.MP_IOS.number.toLong())
        }
    }

    @Test
    fun retargetingBidModifierTest() {
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()

        val retargetingCondition = grutSteps.steps.retConditionSteps().createDefaultRetCondition()
        // создаем корректировку retargeting с двумя уточнениями
        val anotherRetargetingCondition = grutSteps.steps.retConditionSteps().createDefaultRetCondition()
        val retargetingBidModifierInfo = grutSteps.steps.bidModifierSteps().createDefaultAdGroupBidModifierRetargeting(
            adGroup.adGroup,
            listOf(retargetingCondition.retCondition.id, anotherRetargetingCondition.retConditionId)
        )

        // создаем корректировку retargeting_filter с одним уточнением
        val retargetingFilterCondition = grutSteps.steps.retConditionSteps().createDefaultRetCondition()
        val retargetingFilterBidModifierInfo = grutSteps.steps.bidModifierSteps()
            .createAdGroupBidModifierRetargetingFilterWithRetCondIds(
                adGroup.adGroup,
                listOf(retargetingFilterCondition.retConditionId)
            )

        val retargetingFilterAdjustment = (retargetingFilterBidModifierInfo.bidModifier as BidModifierRetargetingFilter)
            .retargetingAdjustments.first()
        val retargetingAdjustments =
            (retargetingBidModifierInfo.bidModifier as BidModifierRetargeting).retargetingAdjustments
        // реплицируем retargeting_conditions чтобы корректно отработали проверки на внешние ключи

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    clientId = retargetingCondition.clientId.asLong()
                ),
                Mysql2GrutReplicationObject(
                    clientId = anotherRetargetingCondition.clientId.asLong()
                ),
                Mysql2GrutReplicationObject(
                    clientId = retargetingFilterCondition.clientId.asLong()
                ),
                Mysql2GrutReplicationObject(
                    retargetingConditionId = retargetingAdjustments[0].retargetingConditionId
                ),
                Mysql2GrutReplicationObject(
                    retargetingConditionId = retargetingAdjustments[1].retargetingConditionId
                ),
                Mysql2GrutReplicationObject(
                    retargetingConditionId = retargetingFilterAdjustment.retargetingConditionId
                ),
            )
        )
        val retCondCreated = replicationService.retargetingConditionGrutApi.getExistingObjects(
            listOf(
                retargetingAdjustments[0].retargetingConditionId,
                retargetingAdjustments[1].retargetingConditionId,
                retargetingFilterAdjustment.retargetingConditionId,
            )
        )
        Assertions.assertEquals(retCondCreated.size, 3)

        // реплицируем retargetingAdjustments по изменению в таблице с уточнением
        // реплицируем retargetingFilterAdjustment по изменению в родительской таблице
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.RETARGETING,
                    bidModifierId = retargetingAdjustments[0].id
                ),
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.RETARGETING,
                    bidModifierId = retargetingAdjustments[1].id
                ),
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = retargetingFilterBidModifierInfo.bidModifierId
                ),
            )
        )

        val bidModifiersGrut = replicationService.bidModifierGrutApi.getBidModifiers(
            retargetingAdjustments.map { it.id } + retargetingFilterAdjustment.id
        )
        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(bidModifiersGrut.size)
                .`as`("Должна быть 1 запись для retargeting_filter и 2 записи adjumstemnt для retargeting")
                .isEqualTo(3)

            val firstRetargetingAdjustment = bidModifiersGrut.first { it.meta.id == retargetingAdjustments[0].id }
            val secondRetargetingAdjustment = bidModifiersGrut.first { it.meta.id == retargetingAdjustments[1].id }
            val retargetingFilterAdjustmentGrut =
                bidModifiersGrut.first { it.meta.id == retargetingFilterAdjustment.id }

            softly.assertThat(firstRetargetingAdjustment.meta.bidModifierType)
                .isEqualTo(EBidModifierType.MLT_RETARGETING.number)
            softly.assertThat(firstRetargetingAdjustment.spec.priceCoefficientPercent)
                .isEqualTo(retargetingAdjustments[0].percent.toLong())

            softly.assertThat(secondRetargetingAdjustment.meta.bidModifierType)
                .isEqualTo(EBidModifierType.MLT_RETARGETING.number)
            softly.assertThat(secondRetargetingAdjustment.spec.priceCoefficientPercent)
                .isEqualTo(retargetingAdjustments[1].percent.toLong())

            softly.assertThat(retargetingFilterAdjustmentGrut.meta.bidModifierType)
                .isEqualTo(EBidModifierType.MLT_RETARGETING_FILTER.number)
            softly.assertThat(retargetingFilterAdjustmentGrut.spec.priceCoefficientPercent)
                .isEqualTo(retargetingFilterAdjustment.percent.toLong())

            softly.assertThat(firstRetargetingAdjustment.spec.retargetingConditionId)
                .isEqualTo(retargetingAdjustments[0].retargetingConditionId)
            softly.assertThat(secondRetargetingAdjustment.spec.retargetingConditionId)
                .isEqualTo(retargetingAdjustments[1].retargetingConditionId)
            softly.assertThat(retargetingFilterAdjustmentGrut.spec.retargetingConditionId)
                .isEqualTo(retargetingFilterAdjustment.retargetingConditionId)
        }
    }

    @Test
    fun replicateGeoBidModifier() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val geoBidModifier = bidModifierSteps.createDefaultAdGroupBidModifierGeo(adGroup.adGroup)
        val adjustments = (geoBidModifier.bidModifier as BidModifierGeo).regionalAdjustments

        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.GEO,
                    bidModifierId = geoBidModifier.bidModifierId,
                )
            )
        )

        // assert
        val bidModifierGrut = replicationService.bidModifierGrutApi.getBidModifier(adjustments.first().id)

        val expectedBidModifier = TBidModifier.newBuilder().apply {
            meta = Schema.TBidModifierMeta.newBuilder()
                .setId(adjustments.first().id)
                .setBidModifierType(EBidModifierType.MLT_GEO.number)
                .setClientId(adGroup.adGroup.clientId.asLong())
                .build()
            spec = TBidModifierSpec.newBuilder().apply {
                priceCoefficientPercent = adjustments[0].percent.toLong()
                geo = TBidModifierSpec.TGeo.newBuilder()
                    .setRegionId(adjustments[0].regionId)
                    .setIsHidden(adjustments[0].hidden)
                    .build()
            }.build()
        }.build()

        ProtoTruth.assertThat(bidModifierGrut)
            .withPartialScope(FieldScopes.fromSetFields(expectedBidModifier))
            .isEqualTo(expectedBidModifier)
    }

    @Test

    fun replicateABSegmentBidModifier() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val abSegmentRetCondition =
            grutSteps.steps.retConditionSteps().createDefaultABSegmentRetCondition(adGroup.adGroup.clientInfo)
        val bidModifier = bidModifierSteps.createDefaultABSegmentBidModifier(adGroup.adGroup, abSegmentRetCondition)
        val adjustment = (bidModifier.bidModifier as BidModifierABSegment).abSegmentAdjustments.first()

        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    retargetingConditionId = adjustment.abSegmentRetargetingConditionId,
                ),
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.AB_SEGMENT,
                    bidModifierId = adjustment.id,
                )
            )
        )

        //assert
        val bidModifierGrut = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)
        val expectedBidModifier = TBidModifier.newBuilder().apply {
            meta = Schema.TBidModifierMeta.newBuilder()
                .setId(adjustment.id)
                .setBidModifierType(EBidModifierType.MLT_AB_SEGMENT.number)
                .setClientId(adGroup.adGroup.clientId.asLong())
                .build()
            spec = TBidModifierSpec.newBuilder().apply {
                priceCoefficientPercent = adjustment.percent.toLong()
                retargetingConditionId = adjustment.abSegmentRetargetingConditionId
            }.build()
        }.build()

        ProtoTruth.assertThat(bidModifierGrut)
            .withPartialScope(FieldScopes.fromSetFields(expectedBidModifier))
            .isEqualTo(expectedBidModifier)
    }

    @Test
    fun replicateBannerTypeAdjustment() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val bidModifier = bidModifierSteps.createDefaultBannerTypeBidModifier(adGroup.adGroup)
        val adjustment = (bidModifier.bidModifier as BidModifierBannerType).bannerTypeAdjustments.first()

        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.BANNER_TYPE,
                    bidModifierId = adjustment.id,
                )
            )
        )

        //assert
        val bidModifierGrut = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)
        val expectedBidModifier = TBidModifier.newBuilder().apply {
            meta = Schema.TBidModifierMeta.newBuilder()
                .setId(adjustment.id)
                .setBidModifierType(EBidModifierType.MLT_INVENTORY.number)
                .setClientId(adGroup.adGroup.clientId.asLong())
                .build()
            spec = TBidModifierSpec.newBuilder().apply {
                priceCoefficientPercent = adjustment.percent.toLong()
                number = EInventoryType.IT_ALL_BANNERS.number.toLong()
            }.build()
        }.build()

        ProtoTruth.assertThat(bidModifierGrut)
            .withPartialScope(FieldScopes.fromSetFields(expectedBidModifier))
            .isEqualTo(expectedBidModifier)
    }

    @Test
    fun replicateInventoryBidModifier() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val bidModifier = bidModifierSteps.createDefaultInventoryBidModifier(adGroup.adGroup)
        val adjustments = (bidModifier.bidModifier as BidModifierInventory).inventoryAdjustments

        // act
        processor.process(
            adjustments.map {
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.INVENTORY,
                    bidModifierId = it.id
                )
            }
        )

        //assert
        val bidModifiersGrut = replicationService.bidModifierGrutApi.getBidModifiers(adjustments.map { it.id }).toList()

        val firstInventoryGrut = TBidModifier.newBuilder().apply {
            meta = Schema.TBidModifierMeta.newBuilder()
                .setId(adjustments[0].id)
                .setBidModifierType(EBidModifierType.MLT_INVENTORY.number)
                .setClientId(adGroup.adGroup.clientId.asLong())
                .build()
            spec = TBidModifierSpec.newBuilder().apply {
                priceCoefficientPercent = adjustments[0].percent.toLong()
                number = EInventoryType.IT_INAPP.number.toLong()
            }.build()
        }.build()

        val secondInventoryGrut = TBidModifier.newBuilder().apply {
            meta = Schema.TBidModifierMeta.newBuilder()
                .setId(adjustments[1].id)
                .setBidModifierType(EBidModifierType.MLT_INVENTORY.number)
                .setClientId(adGroup.adGroup.clientId.asLong())
                .build()
            spec = TBidModifierSpec.newBuilder().apply {
                priceCoefficientPercent = adjustments[1].percent.toLong()
                number = EInventoryType.IT_INBANNER.number.toLong()
            }.build()
        }.build()

        ProtoTruth.assertThat(bidModifiersGrut[0])
            .withPartialScope(FieldScopes.fromSetFields(firstInventoryGrut))
            .isEqualTo(firstInventoryGrut)

        ProtoTruth.assertThat(bidModifiersGrut[1])
            .withPartialScope(FieldScopes.fromSetFields(secondInventoryGrut))
            .isEqualTo(secondInventoryGrut)
    }

    @Test
    fun replicateWeatherBidModifier() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val bidModifier = bidModifierSteps.createDefaultAdGroupBidModifierWeather(adGroup.adGroup)
        val adjustment = (bidModifier.bidModifier as BidModifierWeather).weatherAdjustments.first()

        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.WEATHER,
                    bidModifierId = adjustment.id
                )
            )
        )

        // assert
        val expectedBidModifier = TBidModifier.newBuilder().apply {
            meta = Schema.TBidModifierMeta.newBuilder()
                .setId(adjustment.id)
                .setBidModifierType(EBidModifierType.MLT_WEATHER.number)
                .setClientId(adGroup.adGroup.clientId.asLong())
                .build()
            spec = TBidModifierSpec.newBuilder().apply {
                priceCoefficientPercent = adjustment.percent.toLong()

                val expression = adjustment.expression.first().first()

                conjunction = TBidModifierSpec.TConjunction.newBuilder()
                    .addDisjunctions(
                        TBidModifierSpec.TDisjunction.newBuilder()
                            .addExpressions(
                                TExpressionAtom.newBuilder()
                                    .setOperator(YabsOperation.EYabsOperation.YO_EQUAL.number)
                                    .setWeather(
                                        TExpressionAtom.TWeather.newBuilder()
                                            .setValue(expression.value)
                                            .setWeatherParameter(EWeatherParameter.WT_CLOUDNESS.number)
                                    ).build()
                            ).build()
                    )
                    .build()
            }.build()
        }.build()

        val bidModifierGrut = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)


        ProtoTruth.assertThat(bidModifierGrut)
            .withPartialScope(FieldScopes.fromSetFields(expectedBidModifier))
            .isEqualTo(expectedBidModifier)
    }

    @Test
    fun replicatePrismaIncome() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val bidModifier = bidModifierSteps.createDefaultAdGroupBidModifierIncomeGrade(adGroup.adGroup)
        val adjustment = (bidModifier.bidModifier as BidModifierPrismaIncomeGrade).expressionAdjustments.first()
        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.EXPRESSION,
                    bidModifierId = adjustment.id
                ),
            )
        )
        //assert
        val expectedBidModifier = TBidModifier.newBuilder().apply {
            meta = Schema.TBidModifierMeta.newBuilder()
                .setId(adjustment.id)
                .setBidModifierType(EBidModifierType.MLT_PRISMA_INCOME_GRADE.number)
                .setClientId(adGroup.adGroup.clientId.asLong())
                .build()
            spec = TBidModifierSpec.newBuilder().apply {
                priceCoefficientPercent = adjustment.percent.toLong()

                conjunction = TBidModifierSpec.TConjunction.newBuilder()
                    .addDisjunctions(
                        TBidModifierSpec.TDisjunction.newBuilder()
                            .addExpressions(
                                TExpressionAtom.newBuilder()
                                    .setOperator(YabsOperation.EYabsOperation.YO_EQUAL.number)
                                    .setNumber(1)
                            )
                            .build()
                    )
                    .build()
            }.build()
        }.build()

        val bidModifierGrut = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)
        ProtoTruth.assertThat(bidModifierGrut)
            .withPartialScope(FieldScopes.fromSetFields(expectedBidModifier))
            .isEqualTo(expectedBidModifier)
    }

    /**
     * Попытка реплицировать существующую в MySql запись - удаления в груте не должно произойти
     */
    @Test
    fun tryDeleteExistingRetargetingFilterAdjustment() {
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val retargetingFilterCondition = grutSteps.steps.retConditionSteps().createDefaultRetCondition()
        val retargetingFilterBidModifierInfo = grutSteps.steps.bidModifierSteps()
            .createAdGroupBidModifierRetargetingFilterWithRetCondIds(
                adGroup.adGroup,
                listOf(retargetingFilterCondition.retConditionId)
            )
        val adjustment = (retargetingFilterBidModifierInfo.bidModifier as BidModifierRetargetingFilter)
            .retargetingAdjustments.first()

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    clientId = retargetingFilterCondition.clientId.asLong()
                ),
                Mysql2GrutReplicationObject(
                    retargetingConditionId = adjustment.retargetingConditionId
                ),
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.RETARGETING,
                    bidModifierId = adjustment.id
                )
            )
        )
        val bidModifierGrut = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)
        Assertions.assertNotNull(bidModifierGrut)
        //act - пробуем удалить существующую корректировку
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.RETARGETING,
                    bidModifierId = adjustment.id,
                    isDeleted = true
                ),
            )
        )

        val bidModifierGrutAfterDeletionAttempt = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)
        Assertions.assertNotNull(bidModifierGrutAfterDeletionAttempt)
    }

    @Test
    fun tryReplicateTrafficJamBidModifier() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifier = grutSteps.steps.bidModifierSteps().createDefaultAdGroupBidModifierTraffic(adGroup.adGroup)
        val adjustment = (bidModifier.bidModifier as BidModifierTraffic).expressionAdjustments.first()
        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = bidModifier.bidModifierId
                )
            )
        )
        // assert
        val bidModifierGrut = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)
        assertThat(bidModifierGrut)
            .`as`("Корректировка не поддерживается в репликации - должна быть пропущена").isNull()
    }

    @Test
    fun replicateTrafaretPosition() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val bidModifier = bidModifierSteps.createDefaultAdGroupBidModifierTrafaretPosition(adGroup.adGroup)
        val adjustment = (bidModifier.bidModifier as BidModifierTrafaretPosition).trafaretPositionAdjustments.first()
        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.TRAFARET_POSITION,
                    bidModifierId = adjustment.id
                )
            )
        )
        // assert
        val expectedBidModifier = TBidModifier.newBuilder().apply {
            meta = Schema.TBidModifierMeta.newBuilder()
                .setId(adjustment.id)
                .setBidModifierType(EBidModifierType.MLT_TRAFARET_POSITION.number)
                .setClientId(adGroup.adGroup.clientId.asLong())
                .build()
            spec = TBidModifierSpec.newBuilder().apply {
                priceCoefficientPercent = adjustment.percent.toLong()
                number = TBidModifierSpec.EPosition.POS_ALONE.number.toLong()
            }.build()
        }.build()

        val bidModifierGrut = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)
        ProtoTruth.assertThat(bidModifierGrut)
            .withPartialScope(FieldScopes.fromSetFields(expectedBidModifier))
            .isEqualTo(expectedBidModifier)
    }

    @Test
    fun dontReplicateUnsupportedDemographyBidModifier() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierSteps = grutSteps.steps.bidModifierSteps()
        val demographyInfo =
            bidModifierSteps.createAdGroupBidModifierDemographicsWithUnsupportedAgeType(adGroup.adGroup)
        val demography = demographyInfo.bidModifier as BidModifierDemographics
        val adjustmentId = demography.demographicsAdjustments.first().id

        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = demographyInfo.bidModifierId,
                )
            )
        )
        // assert
        val got = replicationService.bidModifierGrutApi.getBidModifier(adjustmentId)
        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(got).isNotNull
            softly.assertThat(got!!.spec.demography.ageGroup).`as`("DIRECT-174508 демографическая корректировка c age:45_" +
                " не доступна из UI, но все еще едет транспортом, поэтому мы поддержали ее в GrUT. " +
                "Пока не обновился GrUT, не заполняем поле age для 45_, но корректировку реплицируем.").isZero
        }
    }
}
