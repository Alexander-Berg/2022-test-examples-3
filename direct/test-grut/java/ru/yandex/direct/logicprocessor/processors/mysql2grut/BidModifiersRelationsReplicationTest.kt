package ru.yandex.direct.logicprocessor.processors.mysql2grut

import com.google.common.truth.Truth.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTraffic
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.ess.logicobjects.mysql2grut.BidModifierTableType
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.GrutReplicationSteps
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.repository.BidModifierTestRepository
import ru.yandex.grut.objects.proto.client.Schema

/**
 * В этом классе находятся тесты на привязку/отвязку корректировок к группам.
 */
@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class BidModifiersRelationsReplicationTest {

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationService: GrutApiService

    @Autowired
    private lateinit var grutSteps: GrutReplicationSteps

    @Autowired
    private lateinit var bidModifierService: BidModifierService

    @Autowired
    private lateinit var bidModifierTestRepository: BidModifierTestRepository

    private lateinit var mySqlAdGroup: AdGroupInfo
    private var existingOrderId: Long = 0
    private lateinit var grutCampaign: Schema.TCampaignV2

    @BeforeEach
    private fun setup() {
        val (adGroup, orderId) = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        mySqlAdGroup = adGroup
        existingOrderId = orderId

        grutCampaign = replicationService.campaignGrutDao.getCampaign(orderId)!!
        processor.withShard(mySqlAdGroup.shard)
    }

    /**
     * Кейс, когда группы в груте нет, а корректировка есть. Группа должна реплицироваться с корректировкой.
     */
    @Test
    fun replicateNewAdGroupBidModifierExistInGrut() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val retargetingCondition = grutSteps.steps.retConditionSteps().createDefaultRetCondition()
        // создаем корректировку retargeting
        val retargetingBidModifierInfo = grutSteps.steps.bidModifierSteps().createDefaultAdGroupBidModifierRetargeting(
            adGroup.adGroup,
            listOf(retargetingCondition.retCondition.id)
        )
        val adjustmentId = (retargetingBidModifierInfo.bidModifier as BidModifierRetargeting)
            .retargetingAdjustments.first().id
        // создаем корректировку в грут
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    clientId = retargetingCondition.clientId.asLong()
                ),
                Mysql2GrutReplicationObject(
                    retargetingConditionId = retargetingCondition.retConditionId
                ),
            )
        )
        Assertions.assertNotNull(
            replicationService.retargetingConditionGrutApi.getRetargetingCondition(
                retargetingCondition.retConditionId
            )
        )

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.RETARGETING,
                    bidModifierId = adjustmentId
                ),
            )
        )
        Assertions.assertNotNull(replicationService.bidModifierGrutApi.getBidModifier(adjustmentId))
        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    adGroupId = adGroup.adGroup.adGroupId,
                )
            )
        )
        val adgGroup = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroup.adGroupId)
        assertThat(adgGroup!!.spec.bidModifiersIdsList).isEqualTo(listOf(adjustmentId))
    }

    /**
     * Тест проверяет, что если корректировка стала disabled, она пропадает из связи
     */
    @Test
    fun bidModifierEnableFieldChanged() {
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierInfo = grutSteps.steps.bidModifierSteps()
            .createDefaultAdGroupIosBidModifierMobile(adGroup.adGroup)
        val adjustment = (bidModifierInfo.bidModifier as BidModifierMobile).mobileAdjustment
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.MOBILE,
                    bidModifierId = adjustment.id
                ),
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = bidModifierInfo.bidModifierId
                ),
                Mysql2GrutReplicationObject(
                    adGroupId = adGroup.adGroup.adGroupId
                ),
            )
        )
        val adgGroup = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroup.adGroupId)
        assertThat(adgGroup!!.spec.bidModifiersIdsList).isEqualTo(listOf(adjustment.id))

        // выставляем корректировке enabled = false
        bidModifierTestRepository.updateHierarchicalMultiplierEnabled(
            grutSteps.DEFAULT_SHARD,
            bidModifierInfo.bidModifierId,
            false
        )
        //act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.MOBILE,
                    bidModifierId = adjustment.id
                ),
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierIdForRelation = bidModifierInfo.bidModifierId
                ),
            )
        )
        val adgGroupUpdated = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroup.adGroupId)
        assertThat(adgGroupUpdated!!.spec.bidModifiersIdsCount).isEqualTo(0)
    }

    /**
     * Тест проверяет, что после удаления корректировки из Грута, она удалится и из связи в группе
     */
    @Test
    fun bidModifierDeletedTest() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val bidModifierInfo = grutSteps.steps.bidModifierSteps()
            .createDefaultAdGroupIosBidModifierMobile(adGroup.adGroup)
        val adjustment = (bidModifierInfo.bidModifier as BidModifierMobile).mobileAdjustment
        // подготавливаем корректировку и группу в Груте
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.MOBILE,
                    bidModifierId = adjustment.id
                ),
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierIdForRelation = bidModifierInfo.bidModifierId
                ),
            )
        )
        SoftAssertions.assertSoftly { softly ->
            val bidModifier = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)
            softly.assertThat(bidModifier).isNotNull
            val adGroup = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroup.adGroupId)
            softly.assertThat(adGroup).isNotNull
            softly.assertThat(adGroup!!.spec.bidModifiersIdsList).isEqualTo(listOf(adjustment.id))
        }
        // act
        // удаляем корректирову из директа и грута
        bidModifierService.deleteAdjustments(
            grutSteps.DEFAULT_SHARD,
            adGroup.adGroup.clientId,
            1L,
            listOf(bidModifierInfo.bidModifier)
        )
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.MOBILE,
                    bidModifierId = adjustment.id,
                    isDeleted = true,
                ),
            )
        )
        // assert
        val adGroupGrut = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroup.adGroupId)
        val bidModifierGrut = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(bidModifierGrut).isNull()
            softly.assertThat(adGroupGrut).isNotNull
            softly.assertThat(adGroupGrut!!.spec.bidModifiersIdsList).isEmpty()
        }
    }

    /**
     * Не реплицировать группу если у нее есть корректировка, которой еще нет в Груте.
     */
    @Test
    fun dontReplicateAdGroupWithMissingBidModifier() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val retargetingCondition = grutSteps.steps.retConditionSteps().createDefaultRetCondition()
        // создаем корректировку retargeting в Директе
        val retargetingBidModifierInfo = grutSteps.steps.bidModifierSteps().createDefaultAdGroupBidModifierRetargeting(
            adGroup.adGroup,
            listOf(retargetingCondition.retCondition.id)
        )
        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    adGroupId = adGroup.adGroup.adGroupId,
                )
            )
        )
        val adGroupGrut = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroup.adGroupId)
        assertThat(adGroupGrut).isNull()
    }

    /**
     * Проверяем, что если добавилась новая корректировка в группу в директе, то она же появится в группе
     * (без отдельного события об изменении в группе)
     */
    @Test
    fun bidModifierAddedToGroup() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        // создаем корректировку retargeting
        val bidModifierInfo = grutSteps.steps.bidModifierSteps()
            .createDefaultAdGroupBidModifierDemographics(adGroup.adGroup)
        val adjustment = (bidModifierInfo.bidModifier as BidModifierDemographics)
            .demographicsAdjustments.first()
        // создаем корректировку в грут
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.DEMOGRAPHY,
                    bidModifierId = adjustment.id
                ),
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierIdForRelation = bidModifierInfo.bidModifierId
                ),
            )
        )
        SoftAssertions.assertSoftly { softly ->
            val bidModifier = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)
            softly.assertThat(bidModifier).isNotNull

            val adgGroup = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroup.adGroupId)
            softly.assertThat(adgGroup!!.spec.bidModifiersIdsList)
                .`as`("Группа должна содержать новую корректировку")
                .isEqualTo(listOf(adjustment.id))
        }
    }

    /**
     * Проверяем, что если добавилась новая корректировка в группу в директе, но ее тип не поддерживается в GrUT
     * и событий изменения групп нет, репликации связи не будет, при этом не будет и ошибки. (DIRECT-171337)
     */
    @Test
    fun unsupportedBidModifierRelationWillNotBeSet() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        // создаем корректировку traffic - не поддерживается в GrUT
        val bidModifierInfo = grutSteps.steps.bidModifierSteps()
            .createDefaultAdGroupBidModifierTraffic(adGroup.adGroup)
        val adjustment = (bidModifierInfo.bidModifier as BidModifierTraffic).expressionAdjustments.first()
        // создаем корректировку в грут
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = bidModifierInfo.bidModifierId,
                ),
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierIdForRelation = bidModifierInfo.bidModifierId
                ),
                Mysql2GrutReplicationObject(adGroupId = adGroup.adGroup.adGroupId)
            )
        )
        SoftAssertions.assertSoftly { softly ->
            val bidModifier = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)
            softly.assertThat(bidModifier).isNull()

            val adgGroup = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroup.adGroupId)
            softly.assertThat(adGroup).isNotNull
            softly.assertThat(adgGroup!!.spec.bidModifiersIdsCount).isZero
        }
    }

    /**
     * DIRECT-174508 - теперь для GrUT 45_ - валидный тип demography-корректировки, она должна реплицироваться
     * Пока ждем релиза GrUT age не заполняем
     */
    @Test
    fun replicateAdGroupWithUnsupportedDemographyBidModifier() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()

        // создаем demography-корректировку с неподдерживаемым возрастом 45-
        val bidModifierInfo = grutSteps.steps.bidModifierSteps()
            .createAdGroupBidModifierDemographicsWithUnsupportedAgeType(adGroup.adGroup)
        val adjustment = (bidModifierInfo.bidModifier as BidModifierDemographics).demographicsAdjustments.first()
        // создаем корректировку в грут
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = bidModifierInfo.bidModifierId,
                ),
                Mysql2GrutReplicationObject(adGroupId = adGroup.adGroup.adGroupId)
            )
        )
        SoftAssertions.assertSoftly { softly ->
            val bidModifier = replicationService.bidModifierGrutApi.getBidModifier(adjustment.id)
            softly.assertThat(bidModifier).isNotNull
            softly.assertThat(bidModifier!!.spec.demography.ageGroup).isZero

            val adgGroup = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroup.adGroupId)
            softly.assertThat(adGroup).isNotNull
            softly.assertThat(adgGroup!!.spec.bidModifiersIdsCount).isEqualTo(1)
        }
    }


    /**
     * Проверяет кейс, когда мы реплицируем группу, у которой есть корректировки с уточнениями.
     * В таком случае у корректировок идентификаторы в Грут не равны hierarchical_multipliers в MySql,
     * а совпадают с идентификаторами *-values объектов.
     */
    @Test
    fun bidModifierWithAdjustmentsTest() {
        // arrange
        val adGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        // создаем корректировку retargeting
        val demographyInfo =
            grutSteps.steps.bidModifierSteps()
                .createAdGroupBidModifierDemographicsWithMulipleAdjustments(adGroup.adGroup)
        val tabletInfo = grutSteps.steps.bidModifierSteps()
            .createDefaultAdGroupBidModifierMobile(adGroup.adGroup)
        val demography = demographyInfo.bidModifier as BidModifierDemographics

        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = demography.id,
                ),
                Mysql2GrutReplicationObject(
                    bidModifierTableType = BidModifierTableType.PARENT,
                    bidModifierId = tabletInfo.bidModifierId
                ),
                Mysql2GrutReplicationObject(
                    adGroupId = adGroup.adGroup.adGroupId
                ),
            )
        )
        // проверяем что корректировки доехали до Грута

        SoftAssertions.assertSoftly { softly ->
            val expectedIds = listOf(
                demography.demographicsAdjustments[0].id,
                demography.demographicsAdjustments[1].id,
                tabletInfo.bidModifierId,
            );
            val grutBidModifiers = replicationService.bidModifierGrutApi
                .getBidModifiers(expectedIds)
            softly.assertThat(grutBidModifiers.size).isEqualTo(3)

            val adgGroup = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroup.adGroupId)
            softly.assertThat(HashSet(adgGroup!!.spec.bidModifiersIdsList))
                .`as`("Группа должна содержать 3 корректировки")
                .isEqualTo(expectedIds.toSet())
        }
    }
}
