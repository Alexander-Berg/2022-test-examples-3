package ru.yandex.direct.logicprocessor.processors.mysql2grut

import com.google.common.truth.extensions.proto.FieldScopes
import com.google.common.truth.extensions.proto.ProtoTruth
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.adgroup.model.AdGroupPriceSales
import ru.yandex.direct.core.entity.adgroup.model.AdGroupWithPageBlocks
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup
import ru.yandex.direct.core.entity.adgroup.model.PageGroupTagEnum
import ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory
import ru.yandex.direct.core.grut.api.InternalAdGroupOptions
import ru.yandex.direct.core.grut.api.utils.AdGroupAdditionalTargetingConverter.Companion.transformPageBlock
import ru.yandex.direct.core.grut.api.utils.moscowDateTimeToGrut
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.MobileContentInfo
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.GrutReplicationSteps
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.utils.TimeConvertUtils
import ru.yandex.grut.objects.proto.AdGroupV2
import ru.yandex.grut.objects.proto.AdGroupV2.TAdGroupV2Spec
import ru.yandex.grut.objects.proto.AdGroupV2.TAdGroupV2Spec.TMobileContentDetails
import ru.yandex.grut.objects.proto.RelevanceMatchCategory.ERelevanceMatchCategory
import ru.yandex.grut.objects.proto.client.Schema
import ru.yandex.grut.objects.proto.client.Schema.TAdGroupV2
import java.time.LocalDateTime

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class AdGroupReplicationTest {

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationService: GrutApiService

    @Autowired
    private lateinit var grutSteps: GrutReplicationSteps

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

    @AfterEach
    private fun tearDown() {
        grutSteps.cleanupClientsWithChildren()
    }

    @Test
    fun createSingleAdGroupTest() {
        //arrange
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(adGroupId = mySqlAdGroup.adGroupId))
        //act
        processor.process(processorArgs)
        //assert
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(mySqlAdGroup.adGroupId)
        assertThat(adGroup).isNotNull

        assertSoftly { softly -> assertBasicFields(softly, adGroup!!, mySqlAdGroup, existingOrderId) }
    }

    @Test
    fun filterAdGroupWithMissingCampaignInGrut() {
        //arrange
        val adGroup = grutSteps.createAdGroupInMySql()
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(adGroupId = adGroup.adGroupId))
        //act
        processor.process(processorArgs)
        //assert
        val adGroupFromGrut = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroupId)
        assertThat(adGroupFromGrut).isNull()
    }

    @Test
    fun deleteSingleAdGroupTest() {
        //arrange
        val createdAGroupId = grutSteps.createAdGroupInGrut(existingOrderId)

        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(adGroupId = createdAGroupId, isDeleted = true))
        //act
        processor.process(processorArgs)
        //assert
        val deletedAdGroup = replicationService.adGroupGrutDao.getAdGroup(createdAGroupId)
        assertThat(deletedAdGroup).isNull()
    }

    @Test
    fun checkAdGroupsExist_noAdGroupsExist() {
        val emptyAdGroupsList = replicationService.adGroupGrutDao.getExistingObjects(listOf(randomPositiveLong(), randomPositiveLong()))
        assertThat(emptyAdGroupsList).isEmpty()
    }

    @Test
    fun replicateMobileContentAdGroup() {
        //arrange
        val adGroup = grutSteps.createMobileContentAdGroup()
        val mobileContentAdGroup = adGroup.adGroup as MobileContentAdGroup
        //act
        processor.process(listOf(Mysql2GrutReplicationObject(adGroupId = adGroup.adGroupId)))
        //assert
        val grutAdGroup = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroupId)
        val expectedGroup = TAdGroupV2.newBuilder()
            .setMeta(Schema.TAdGroupV2Meta.newBuilder()
                .setId(adGroup.adGroupId)
                .setDirectType(AdGroupV2.EAdGroupType.AGT_MOBILE_CONTENT.number)
                .build()
            )
            .setSpec(TAdGroupV2Spec.newBuilder()
                .setMobileContentId(mobileContentAdGroup.mobileContentId)
                .setMobileContentDetails(TMobileContentDetails.newBuilder()
                    .setStoreUrl(mobileContentAdGroup.storeUrl)
                    .addDeviceTypes(TMobileContentDetails.EDeviceType.DV_PHONE.number)
                    .addNetworkTypes(TMobileContentDetails.ENetworkType.NT_CELL.number)
                    .build()
                )
                .build()
            )
            .build()
        ProtoTruth.assertThat(grutAdGroup)
            .withPartialScope(FieldScopes.fromSetFields(expectedGroup))
            .isEqualTo(expectedGroup)
    }

    @Test
    fun replicateOneMobileContentAndOneBaseAdGroup() {
        //arrange
        val mobileAdGroupInfo = grutSteps.createMobileContentAdGroup()
        val textAdGroup = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        //act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(adGroupId = mobileAdGroupInfo.adGroupId),
                Mysql2GrutReplicationObject(adGroupId = textAdGroup.adGroup.adGroupId),
            )
        )
        //assert
        val grutAdGroups = replicationService.adGroupGrutDao
            .getAdGroups(listOf(mobileAdGroupInfo.adGroupId, textAdGroup.adGroup.adGroupId))
        Assertions.assertEquals(2, grutAdGroups.size)
    }

    @Test
    fun replicateTwoMobileContentAdGroups() {
        val campaign = grutSteps.steps.campaignSteps()
            .createActiveCampaignByCampaignType(CampaignType.MOBILE_CONTENT, mySqlAdGroup.clientInfo)
        val adGroup1 = grutSteps.steps.adGroupSteps().createActiveMobileContentAdGroup(campaign)
        val mobileContent = grutSteps.mobileContentRepository.getMobileContent(
            adGroup1.shard,
            (adGroup1.adGroup as MobileContentAdGroup).mobileContentId
        )
        val adGroup2 = grutSteps.steps.adGroupSteps().createActiveMobileContentAdGroup(
            campaign,
            MobileContentInfo().withClientInfo(campaign.clientInfo).withMobileContent(mobileContent)
        )
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(campaignId = campaign.campaignId),
                Mysql2GrutReplicationObject(mobileContentId = mobileContent.id),
            )
        )

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(adGroupId = adGroup1.adGroupId),
                Mysql2GrutReplicationObject(adGroupId = adGroup2.adGroupId),
            )
        )

        val grutAdGroups = replicationService.adGroupGrutDao.getAdGroups(listOf(adGroup1.adGroupId, adGroup2.adGroupId))
        val expectedGroup1 = TAdGroupV2.newBuilder()
            .setMeta(Schema.TAdGroupV2Meta.newBuilder()
                .setId(adGroup1.adGroupId)
                .setDirectType(AdGroupV2.EAdGroupType.AGT_MOBILE_CONTENT.number)
                .build()
            )
            .setSpec(TAdGroupV2Spec.newBuilder()
                .setMobileContentId(mobileContent.id)
                .build()
            )
            .build()
        val expectedGroup2 = TAdGroupV2.newBuilder()
            .setMeta(Schema.TAdGroupV2Meta.newBuilder()
                .setId(adGroup2.adGroupId)
                .setDirectType(AdGroupV2.EAdGroupType.AGT_MOBILE_CONTENT.number)
                .build()
            )
            .setSpec(TAdGroupV2Spec.newBuilder()
                .setMobileContentId(mobileContent.id)
                .build()
            )
            .build()
        ProtoTruth.assertThat(grutAdGroups)
            .withPartialScope(FieldScopes.fromSetFields(expectedGroup1))
            .ignoringRepeatedFieldOrder()
            .containsExactly(expectedGroup1, expectedGroup2)
    }

    @Test
    fun dontReplicateMobileContentAdGroupWithMissingMobileContentInGrut() {
        //arrange
        val adGroup = grutSteps.createMobileContentAdGroup(mobileContentExistInGrut = false)
        //act
        processor.process(listOf(Mysql2GrutReplicationObject(adGroupId = adGroup.adGroupId)))
        //assert
        val grutAdGroup = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroupId)
        assertThat(grutAdGroup).isNull()
    }

    @Test
    fun replicateAdGroupWithPageBlocks() {
        //arrange
        val adGroup = grutSteps.createCpmAdGroup()
        val adGroupWithPageBlocks = adGroup.adGroup as AdGroupWithPageBlocks
        //act
        processor.process(listOf(Mysql2GrutReplicationObject(adGroupId = adGroup.adGroupId)))
        //assert
        val grutAdGroup = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroupId)
        assertThat(grutAdGroup).isNotNull
        assertSoftly { softly ->
            softly.assertThat(grutAdGroup!!.meta.directType).isEqualTo(AdGroupV2.EAdGroupType.AGT_CPM_INDOOR.number)
            softly.assertThat(grutAdGroup.spec.pageBlocksCount).isGreaterThan(0)
            softly.assertThat(grutAdGroup.spec.pageBlocksList).isEqualTo(adGroupWithPageBlocks.pageBlocks.map { transformPageBlock(it) }.toList())
        }
    }

    @Test
    fun checkAdGroupsExist_onlyOneAdGroupExist() {
        //arrange
        var createdAGroupId = grutSteps.createAdGroupInGrut(existingOrderId)
        //act
        val addGroupIdsList = replicationService.adGroupGrutDao.getExistingObjects(
            listOf(randomPositiveLong(), randomPositiveLong(), createdAGroupId))
        //assert
        assertSoftly { softly ->
            softly.assertThat(addGroupIdsList.size).isEqualTo(1)
            softly.assertThat(addGroupIdsList.first()).isEqualTo(createdAGroupId)
        }
    }

    @Test
    fun replicatePerformanceAdGroupTest() {
        val (directAdGroup, orderId) = grutSteps.createPerformanceAdGroupWithGrutHierarchy()
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(adGroupId = directAdGroup.adGroupId))
        //act
        processor.process(processorArgs)
        //assert - smart ad group is replicated
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(directAdGroup.adGroupId)


        assertThat(adGroup).isNotNull
        assertSoftly { softly ->
            assertBasicFields(softly, adGroup!!, directAdGroup, orderId, AdGroupV2.EAdGroupType.AGT_SMART)
            softly.assertThat(adGroup.spec.trackingParams).isEqualTo(directAdGroup.adGroup.trackingParams)
        }
    }

    @Test
    fun replicateAdGroupWithPriorityTest() {
        val campaign = grutSteps.steps.campaignSteps().createDefaultCampaign()
        val adGroup = grutSteps.steps.adGroupSteps().createActiveCpmYndxFrontpageAdGroup(campaign, 10)
        val processorArgs = mutableListOf(
            Mysql2GrutReplicationObject(clientId = campaign.clientId.asLong()),
            Mysql2GrutReplicationObject(campaignId = campaign.campaignId),
            Mysql2GrutReplicationObject(adGroupId = adGroup.adGroupId),
        )
        //act
        processor.process(processorArgs)
        //assert
        val adGroupGrut = replicationService.adGroupGrutDao.getAdGroup(adGroup.adGroupId)
        val priority = (adGroup.adGroup as? AdGroupPriceSales)?.let { it.priority }

        assertThat(adGroup).isNotNull
        assertSoftly { softly ->
            softly.assertThat(priority).isNotNull
            softly.assertThat(adGroupGrut!!.spec.matchPriority.toLong()).isEqualTo(priority)
        }
    }

    @Test
    fun replicateDynamicGroupTest() {
        //arrange - create dynamic ad group in Direct
        val (directAdGroup, orderId) = grutSteps.createDynamicAdGroupInMySqlWithGrutHierarchy(setOf(
            RelevanceMatchCategory.exact_mark,
            RelevanceMatchCategory.alternative_mark,
            RelevanceMatchCategory.competitor_mark,
            RelevanceMatchCategory.broader_mark,
            RelevanceMatchCategory.accessory_mark
        ))
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(adGroupId = directAdGroup.adGroupId))
        //act
        processor.process(processorArgs)
        //assert - dynamic ad group is replicated
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(directAdGroup.adGroupId)
        assertThat(adGroup).isNotNull

        assertSoftly { softly ->
            assertBasicFields(softly, adGroup!!, directAdGroup, orderId, AdGroupV2.EAdGroupType.AGT_DYNAMIC)
            softly.assertThat(adGroup.spec.relevanceMatch.categoriesCount).isEqualTo(5)
            softly.assertThat(adGroup.spec.relevanceMatch.categoriesList.contains(ERelevanceMatchCategory.RMC_EXACT_MARK.number))
            softly.assertThat(adGroup.spec.relevanceMatch.categoriesList.contains(ERelevanceMatchCategory.RMC_ALTERNATIVE_MARK.number))
            softly.assertThat(adGroup.spec.relevanceMatch.categoriesList.contains(ERelevanceMatchCategory.RMC_COMPETITOR_MARK.number))
            softly.assertThat(adGroup.spec.relevanceMatch.categoriesList.contains(ERelevanceMatchCategory.RMC_BROADER_MARK.number))
            softly.assertThat(adGroup.spec.relevanceMatch.categoriesList.contains(ERelevanceMatchCategory.RMC_ACCESSORY_MARK.number))
        }
    }

    @Test
    fun replicateDynamicGroup_updateCategoriesTwiceTest() {
        //arrange - do replication #1
        val (directAdGroup, orderId) = grutSteps.createDynamicAdGroupInMySqlWithGrutHierarchy(setOf(
            RelevanceMatchCategory.exact_mark,
            RelevanceMatchCategory.alternative_mark,
        ))
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(adGroupId = directAdGroup.adGroupId))
        processor.process(processorArgs)
        //act - do replication again
        processor.process(processorArgs)
        //assert
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(directAdGroup.adGroupId)
        assertThat(adGroup).isNotNull
        assertSoftly { softly ->
            assertBasicFields(softly, adGroup!!, directAdGroup, orderId, AdGroupV2.EAdGroupType.AGT_DYNAMIC)
            softly.assertThat(adGroup.spec.relevanceMatch.categoriesCount).isEqualTo(2).`as`("relevance match category shouldn't be doubled after second replication")
            softly.assertThat(adGroup.spec.relevanceMatch.categoriesList.contains(ERelevanceMatchCategory.RMC_EXACT_MARK.number))
            softly.assertThat(adGroup.spec.relevanceMatch.categoriesList.contains(ERelevanceMatchCategory.RMC_ALTERNATIVE_MARK.number))
        }
    }

    @Test
    fun replicateAdGroupWithPageGroupTags() {
        //arrange
        val pageGroupTag = PageGroupTagEnum.APP_METRO_TAG
        val (directAdGroup, orderId) = grutSteps.createAdGroupInMySqlWithGrutHierarchy(pageGroupTags = listOf(pageGroupTag))
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(adGroupId = directAdGroup.adGroupId))
        //act
        processor.process(processorArgs)
        //assert
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(directAdGroup.adGroupId)
        assertThat(adGroup).isNotNull

        assertSoftly { softly ->
            assertBasicFields(softly, adGroup!!, directAdGroup, orderId)
            softly.assertThat(adGroup.spec.pageGroupTagsCount).isEqualTo(1)
            softly.assertThat(adGroup.spec.pageGroupTagsList.first()).isEqualTo(pageGroupTag.typedValue)
            softly.assertThat(adGroup.spec.targetTagsList).isEmpty()
        }
    }

    @Test
    fun replicateSamePageGroupTagsTwice() {
        //arrange
        val firstPageGroupTag = PageGroupTagEnum.APP_METRO_TAG
        val secondPageGroupTag = PageGroupTagEnum.GEO_PIN
        val (directAdGroup, orderId) = grutSteps.createAdGroupInMySqlWithGrutHierarchy(pageGroupTags = listOf(firstPageGroupTag, secondPageGroupTag))
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(adGroupId = directAdGroup.adGroupId))
        //act: do process twice
        processor.process(processorArgs)
        processor.process(processorArgs)
        //assert:
        //assert
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(directAdGroup.adGroupId)
        assertThat(adGroup).isNotNull

        assertSoftly { softly ->
            assertBasicFields(softly, adGroup!!, directAdGroup, orderId)
            softly.assertThat(adGroup.spec.pageGroupTagsCount).`as`("target tags shouldn't be doubled").isEqualTo(2)
            softly.assertThat(adGroup.spec.pageGroupTagsList).isEqualTo(listOf(firstPageGroupTag.typedValue, secondPageGroupTag.typedValue))
            softly.assertThat(adGroup.spec.targetTagsList).isEmpty()
        }
    }

    @Test
    fun replicatePageGroupTagsAndTargetTags() {
        //arrange
        val pageGroupTag = PageGroupTagEnum.APP_METRO_TAG
        val targetTag = TargetTagEnum.GEO_PIN
        val (directAdGroup, orderId) = grutSteps.createAdGroupInMySqlWithGrutHierarchy(pageGroupTags = listOf(pageGroupTag), targetTags = listOf(targetTag))
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(adGroupId = directAdGroup.adGroupId))

        //act: do process twice
        processor.process(processorArgs)
        processor.process(processorArgs)
        //assert
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(directAdGroup.adGroupId)
        assertThat(adGroup).isNotNull

        assertSoftly { softly ->
            assertBasicFields(softly, adGroup!!, directAdGroup, orderId)
            softly.assertThat(adGroup.spec.pageGroupTagsList).isEqualTo(listOf(pageGroupTag.typedValue)).`as`("pageGroupTag list is not doubled")
            softly.assertThat(adGroup.spec.targetTagsList).isEqualTo(listOf(targetTag.typedValue)).`as`("targetTag list is not doubled")
        }
    }

    @Test
    fun replicateAdGroupWithMinusPhrases() {
        //arrange
        val (directAdGroup, orderId) = grutSteps.createAdGroupInMySqlWithGrutHierarchy()

        val nonLibraryMinusPhrase = grutSteps.createRandomMinusPhraseInDirectAndGrut(directAdGroup.clientInfo, isLibrary = false)
        val libraryMinusPhrase = grutSteps.createRandomMinusPhraseInDirectAndGrut(directAdGroup.clientInfo, isLibrary = true)
        grutSteps.linkMinusPhrasesToAdGroupInDirect(directAdGroup, listOf(libraryMinusPhrase.id!!), nonLibraryMinusPhrase.id)
        val processorArgs = listOf(Mysql2GrutReplicationObject(adGroupId = directAdGroup.adGroupId))
        //act
        processor.process(processorArgs)
        //assert
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(directAdGroup.adGroupId)
        assertThat(adGroup).isNotNull
        assertSoftly { softly ->
            softly.assertThat(adGroup!!.spec.minusPhrasesIdsCount).isEqualTo(2) //library + nonLibrary
            softly.assertThat(adGroup.spec.minusPhrasesIdsList.toSet()).isEqualTo(setOf(nonLibraryMinusPhrase.id, libraryMinusPhrase.id))
        }
    }

    @Test
    fun dontReplicateAdGroupWithMinusPhrasesMissingInGrut() {
        //arrange
        val (directAdGroup, orderId) = grutSteps.createAdGroupInMySqlWithGrutHierarchy()
        val nonLibraryMinusPhrase = grutSteps.createRandomMinusPhraseInDirectWithGrutHierarchy(directAdGroup.clientInfo, isLibrary = false)
        val libraryMinusPhrase = grutSteps.createRandomMinusPhraseInDirectWithGrutHierarchy(directAdGroup.clientInfo, isLibrary = true)

        grutSteps.linkMinusPhrasesToAdGroupInDirect(directAdGroup, listOf(libraryMinusPhrase.id!!), nonLibraryMinusPhrase.id)

        val processorArgs = listOf(Mysql2GrutReplicationObject(adGroupId = directAdGroup.adGroupId))
        //act
        processor.process(processorArgs)
        //assert
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(directAdGroup.adGroupId)
        assertThat(adGroup).`as`("We skip adGroups with missing minus phrases").isNull()
    }

    @Test
    fun replicateInternalFields() {
        //arrange
        val (directAdGroup, orderId) = grutSteps.createInternalAdGroupInMySqlWithGrutHierarchy(1,
            InternalAdGroupOptions(
                3,
                4,
                5,
                6,
                7,
                8,
                LocalDateTime.parse("2020-02-10T00:00:00"),
                null,
            )
        )
        val internalAdGroup = directAdGroup.adGroup as InternalAdGroup

        val processorArgs = listOf(Mysql2GrutReplicationObject(adGroupId = directAdGroup.adGroupId))
        //act
        processor.process(processorArgs)
        //assert
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(directAdGroup.adGroupId)
        assertThat(adGroup).isNotNull

        assertSoftly { softly ->
            assertBasicFields(softly, adGroup!!, directAdGroup, orderId, AdGroupV2.EAdGroupType.AGT_INTERNAL)
            softly.assertThat(adGroup.spec.internalLevel).isEqualTo(internalAdGroup.level)
            softly.assertThat(adGroup.spec.rfOptions).isNotNull
            softly.assertThat(adGroup.spec.rfOptions.shows.count).isEqualTo(internalAdGroup.rf)
            softly.assertThat(adGroup.spec.rfOptions.shows.period).isEqualTo(TimeConvertUtils.daysToSecond(internalAdGroup.rfReset))
            softly.assertThat(adGroup.spec.rfOptions.clicks.count).isEqualTo(internalAdGroup.maxClicksCount)
            softly.assertThat(adGroup.spec.rfOptions.clicks.period).isEqualTo(internalAdGroup.maxClicksPeriod)
            softly.assertThat(adGroup.spec.rfOptions.closes.count).isEqualTo(internalAdGroup.maxStopsCount)
            softly.assertThat(adGroup.spec.rfOptions.closes.period).isEqualTo(internalAdGroup.maxStopsPeriod)
        }
    }

    @Test
    fun replicateInternalFieldsWithZeroRf() {
        //arrange
        val (directAdGroup, orderId) = grutSteps.createInternalAdGroupInMySqlWithGrutHierarchy(1,
            InternalAdGroupOptions(
                0,
                4,
                5,
                6,
                7,
                8,
                LocalDateTime.parse("2020-02-10T00:00:00"),
                LocalDateTime.parse("2020-02-20T00:00:00"),
            )
        )
        val internalAdGroup = directAdGroup.adGroup as InternalAdGroup
        val processorArgs = listOf(Mysql2GrutReplicationObject(adGroupId = directAdGroup.adGroupId))
        //act
        processor.process(processorArgs)
        //assert
        val adGroup = replicationService.adGroupGrutDao.getAdGroup(directAdGroup.adGroupId)
        assertThat(adGroup).isNotNull

        assertSoftly { softly ->
            assertBasicFields(softly, adGroup!!, directAdGroup, orderId, AdGroupV2.EAdGroupType.AGT_INTERNAL)
            softly.assertThat(adGroup.spec.internalLevel).isEqualTo(internalAdGroup.level)
            softly.assertThat(adGroup.spec.hasRfOptions()).isTrue
            softly.assertThat(adGroup.spec.rfOptions.shows.count).isEqualTo(internalAdGroup.rf)
            softly.assertThat(adGroup.spec.rfOptions.shows.period).isEqualTo(TimeConvertUtils.daysToSecond(internalAdGroup.rfReset))
            softly.assertThat(internalAdGroup.startTime).isNotNull
            softly.assertThat(internalAdGroup.finishTime).isNotNull
            softly.assertThat(adGroup.spec.startTime).isEqualTo(moscowDateTimeToGrut(internalAdGroup.startTime))
            softly.assertThat(adGroup.spec.finishTime).isEqualTo(moscowDateTimeToGrut(internalAdGroup.finishTime))
        }
    }

    private fun assertBasicFields(softly: SoftAssertions,
                                  adGroup: Schema.TAdGroupV2,
                                  directAdGroup: AdGroupInfo,
                                  orderId: Long,
                                  type: AdGroupV2.EAdGroupType = AdGroupV2.EAdGroupType.AGT_BASE
    ) {
        softly.assertThat(adGroup.meta.campaignId).isEqualTo(orderId)
        softly.assertThat(adGroup.meta.directType).isEqualTo(type.number)
        softly.assertThat(adGroup.spec).isNotNull
        softly.assertThat(adGroup.spec.name).isEqualTo(directAdGroup.adGroup.name)
        softly.assertThat(adGroup.spec.regionsIdsCount).isEqualTo(directAdGroup.adGroup.geo.size)
        softly.assertThat(adGroup.spec.regionsIdsList).isEqualTo(directAdGroup.adGroup.geo)
    }

}
