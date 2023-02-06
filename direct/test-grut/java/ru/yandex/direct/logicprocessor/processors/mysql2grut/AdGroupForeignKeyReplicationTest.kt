package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics
import ru.yandex.direct.core.grut.api.BiddableShowConditionGrutApi.Companion.directIdToGrutId
import ru.yandex.direct.core.grut.api.BiddableShowConditionType
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.GrutReplicationSteps
import ru.yandex.grut.objects.proto.BidModifier.EBidModifierType

/**
 * Тесты на репликацию отсутствующих в GrUT foreign keys группы.
 */
@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class AdGroupForeignKeyReplicationTest {
    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationService: GrutApiService

    @Autowired
    private lateinit var grutSteps: GrutReplicationSteps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @BeforeEach
    private fun setup() {
        ppcPropertiesSupport.set(PpcPropertyNames.ADGROUP_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        ppcPropertiesSupport.set(PpcPropertyNames.GRUT_AD_GROUPS_FILL_BIDDABLE_SHOW_CONDITIONS, "true")

        processor.withShard(1)
    }

    @AfterEach
    private fun tearDown() {
        grutSteps.cleanupClientsWithChildren()
    }

    @Test
    fun replicateAdGroupWithMissingBidModifier() {
        val adGroupInfo = grutSteps.steps.adGroupSteps().createDefaultAdGroup()

        // replicate adGroup hierarchy
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = adGroupInfo.clientId.asLong()),
                Mysql2GrutReplicationObject(campaignId = adGroupInfo.campaignId)
            ),
        )
        // create bid modifier in MySQL
        val bidModifier = grutSteps.steps.bidModifierSteps().createDefaultAdGroupBidModifierDemographics(adGroupInfo)
        val demographic = (bidModifier.bidModifier as BidModifierDemographics).demographicsAdjustments.first()

        // act - replicate adgroup with missing bid modifier in GrUT. BidModifier should be replicated as well.
        processor.process(
            listOf(Mysql2GrutReplicationObject(adGroupId = adGroupInfo.adGroupId)),
        )

        SoftAssertions.assertSoftly { softly ->
            val grutBidModifier = replicationService.bidModifierGrutApi.getBidModifier(demographic.id)
            softly.assertThat(grutBidModifier).`as`("репликация по внешнему ключу").isNotNull
            softly.assertThat(grutBidModifier!!.meta.bidModifierType).isEqualTo(EBidModifierType.MLT_DEMOGRAPHY_VALUE)

            val grutAdGroup = replicationService.adGroupGrutDao.getAdGroup(adGroupInfo.adGroupId)
            softly.assertThat(grutAdGroup).isNotNull
            softly.assertThat(grutAdGroup!!.spec.bidModifiersIdsList).isEqualTo(listOf(demographic.id))
        }
    }

    @Test
    fun replicateAdGroupWithMissingBiddableShowConditions() {

        val adGroupInfo = grutSteps.steps.adGroupSteps().createDefaultAdGroup()
        // replicate adGroup hierarchy
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = adGroupInfo.clientId.asLong()),
                Mysql2GrutReplicationObject(campaignId = adGroupInfo.campaignId)
            ),
        )

        val keyword = grutSteps.steps.keywordSteps().createKeyword(adGroupInfo)
        val dynamic = grutSteps.steps.dynamicConditionsSteps().addDefaultBidsDynamic(adGroupInfo)
        val relevanceMatch = grutSteps.steps.relevanceMatchSteps().addDefaultRelevanceMatch(adGroupInfo)

        val offerRet = grutSteps.steps.offerRetargetingSteps().addOfferRetargetingToAdGroup(
            grutSteps.steps.offerRetargetingSteps()
                .defaultOfferRetargetingForGroup(adGroupInfo), adGroupInfo
        )

        val ret = grutSteps.steps.retargetingSteps().createDefaultRetargeting(adGroupInfo)
        val performance = grutSteps.steps.performanceFilterSteps().addDefaultBidsPerformance(adGroupInfo)

        // act - replicate adgroup with missing biddable_show_condition in GrUT.
        // bsc should be replicated as well.
        processor.process(
            listOf(Mysql2GrutReplicationObject(adGroupId = adGroupInfo.adGroupId)),
        )

        val grutIds = listOf(
            directIdToGrutId(BiddableShowConditionType.KEYWORD, keyword.id),
            directIdToGrutId(BiddableShowConditionType.DYNAMIC, dynamic.dynId),
            directIdToGrutId(BiddableShowConditionType.RELEVANCE_MATCH, relevanceMatch.id),
            directIdToGrutId(BiddableShowConditionType.OFFER_RETARGETING, offerRet.id),
            directIdToGrutId(BiddableShowConditionType.RETARGETING, ret.retargeting.id),
            directIdToGrutId(BiddableShowConditionType.PERFORMANCE, performance.perfFilterId),
        )

        SoftAssertions.assertSoftly { softly ->
            val retargetingCondition =
                replicationService.retargetingConditionGrutApi.getRetargetingCondition(ret.retConditionId)
            softly.assertThat(retargetingCondition).isNotNull

            val bsc = replicationService.biddableShowConditionReplicationGrutDao
                .getBiddableShowConditions(grutIds)
            softly.assertThat(bsc.size).isEqualTo(6)

            val grutAdGroup = replicationService.adGroupGrutDao.getAdGroup(adGroupInfo.adGroupId)
            softly.assertThat(grutAdGroup).isNotNull
            softly.assertThat(grutAdGroup!!.spec.biddableShowConditionsIdsCount).isEqualTo(6)
            softly.assertThat(grutAdGroup.spec.biddableShowConditionsIdsList.sorted()).isEqualTo(grutIds.sorted())
        }
    }

    @Test
    fun fkBasicTest() {

        val adGroupInfo = grutSteps.steps.adGroupSteps().createDefaultAdGroup()
        val keyword = grutSteps.steps.keywordSteps().createKeyword(adGroupInfo)

        // replicate adGroup hierarchy
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = adGroupInfo.clientId.asLong()),
                Mysql2GrutReplicationObject(campaignId = adGroupInfo.campaignId),
                Mysql2GrutReplicationObject(adGroupId = adGroupInfo.adGroupId)
            ),
        )


        SoftAssertions.assertSoftly { softly ->
            val bsc = replicationService.biddableShowConditionReplicationGrutDao
                .getBiddableShowConditions(listOf(keyword.id))
            softly.assertThat(bsc.size).isEqualTo(1)

            val grutAdGroup = replicationService.adGroupGrutDao.getAdGroup(adGroupInfo.adGroupId)
            softly.assertThat(grutAdGroup).isNotNull
            softly.assertThat(grutAdGroup!!.spec.biddableShowConditionsIdsCount).isEqualTo(1)
            softly.assertThat(grutAdGroup.spec.biddableShowConditionsIdsList.sorted()).isEqualTo(listOf(keyword.id))
        }
    }

    @Test
    fun replicateAdGroupWithMissingMobileContent() {
        val adGroupInfo = grutSteps.createMobileContentAdGroup(mobileContentExistInGrut = false)
        val mobileContentAdGroup = adGroupInfo.adGroup as MobileContentAdGroup
        // replicate adGroup hierarchy
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = adGroupInfo.clientId.asLong()),
                Mysql2GrutReplicationObject(campaignId = adGroupInfo.campaignId)
            ),
        )

        processor.process(
            listOf(Mysql2GrutReplicationObject(adGroupId = adGroupInfo.adGroupId))
        )

        SoftAssertions.assertSoftly { softly ->
            val mobileContent =
                replicationService.mobileContentGrutDao.getMobileContent(mobileContentAdGroup.mobileContentId)
            softly.assertThat(mobileContent).isNotNull

            val grutAdGroup = replicationService.adGroupGrutDao.getAdGroup(adGroupInfo.adGroupId)
            softly.assertThat(grutAdGroup).isNotNull
            softly.assertThat(grutAdGroup!!.spec.mobileContentId).isEqualTo(mobileContentAdGroup.mobileContentId)
        }
    }

    @Test
    fun replicateAdGroupWithMissingMinusPhrase() {
        val adGroupInfo = grutSteps.createMobileContentAdGroup(mobileContentExistInGrut = false)
        val nonLibraryMinusPhrase =
            grutSteps.createRandomMinusPhraseInDirectAndGrut(adGroupInfo.clientInfo, isLibrary = false)
        val libraryMinusPhrase =
            grutSteps.createRandomMinusPhraseInDirectAndGrut(adGroupInfo.clientInfo, isLibrary = true)
        grutSteps.linkMinusPhrasesToAdGroupInDirect(
            adGroupInfo,
            listOf(libraryMinusPhrase.id!!),
            nonLibraryMinusPhrase.id
        )

        // replicate adGroup hierarchy
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = adGroupInfo.clientId.asLong()),
                Mysql2GrutReplicationObject(campaignId = adGroupInfo.campaignId)
            ),
        )

        processor.process(
            listOf(Mysql2GrutReplicationObject(adGroupId = adGroupInfo.adGroupId))
        )

        SoftAssertions.assertSoftly { softly ->
            val mwIds = listOf(
                nonLibraryMinusPhrase.id!!,
                libraryMinusPhrase.id!!,
            ).sorted()
            val minusPhrases = replicationService.minusPhrasesGrutDao.getMinusPhrases(mwIds)
            softly.assertThat(minusPhrases.size).isEqualTo(2)

            val grutAdGroup = replicationService.adGroupGrutDao.getAdGroup(adGroupInfo.adGroupId)
            softly.assertThat(grutAdGroup).isNotNull
            softly.assertThat(grutAdGroup!!.spec.minusPhrasesIdsList.sorted()).isEqualTo(mwIds)
        }
    }
}
