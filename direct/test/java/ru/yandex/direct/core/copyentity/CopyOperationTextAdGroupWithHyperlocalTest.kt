package ru.yandex.direct.core.copyentity

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.service.AdGroupsAddOperationTestBase
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegment
import ru.yandex.direct.core.entity.hypergeo.service.HyperGeoService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.data.defaultHyperGeo
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class CopyOperationTextAdGroupWithHyperlocalTest : AdGroupsAddOperationTestBase() {

    @Autowired
    private lateinit var factory: CopyOperationFactory

    @Autowired
    private lateinit var asserts: CopyOperationAssert

    @Autowired
    private lateinit var hyperGeoService: HyperGeoService

    private lateinit var otherClientId: ClientId
    private lateinit var otherClientInfo: ClientInfo

    private var campaignIdSameClient: Long = 0L
    private lateinit var campaignInfoSameClient: CampaignInfo

    private var campaignIdOtherClient: Long = 0L
    private lateinit var campaignInfoOtherClient: CampaignInfo

    private lateinit var hyperGeo: HyperGeo
    private lateinit var hyperGeoWithMultipleSegments: HyperGeo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo!!.clientId
        steps.featureSteps().setCurrentClient(clientId)
        steps.featureSteps().addClientFeature(clientId, FeatureName.TEXT_BANNER_INTERESTS_RET_COND_ENABLED, true)
        campaignInfo = steps.campaignSteps().createCampaign(
            TestCampaigns.activeTextCampaign(clientId, clientInfo!!.uid)
                .withEmail("test1@yandex-team.ru"), clientInfo)
        campaignId = campaignInfo!!.campaignId

        campaignInfoSameClient = steps.campaignSteps().createCampaign(
            TestCampaigns.activeTextCampaign(clientId, clientInfo.uid)
                .withEmail("test2@yandex-team.ru"), clientInfo)
        campaignIdSameClient = campaignInfoSameClient.campaignId

        otherClientInfo = steps.clientSteps().createDefaultClientAnotherShard()
        otherClientId = otherClientInfo.clientId!!

        campaignInfoOtherClient = steps.campaignSteps().createCampaign(
            TestCampaigns.activeTextCampaign(otherClientId, otherClientInfo.uid)
                .withEmail("test3@yandex-team.ru"), otherClientInfo)
        campaignIdOtherClient = campaignInfoOtherClient.campaignId

        asserts.init(clientId, clientId, clientInfo!!.uid)

        hyperGeo = createHyperGeo()
        hyperGeoWithMultipleSegments = createHyperGeo(listOf(defaultHyperGeoSegment(), defaultHyperGeoSegment()))
    }

    /**
     * Проверка копирования группы с гипергео в одной кампании
     */
    @Test
    fun adGroupWithHypergeo_CopyInSameCampaign() {
        val adGroupFrom = TestGroups.activeTextAdGroup()
            .withHyperGeoId(hyperGeo.id)
            .withHyperGeoSegmentIds(hyperGeo.hyperGeoSegments
                .map { it.id })
        val adGroupInfoFrom = steps.adGroupSteps().createAdGroup(adGroupFrom, campaignInfo)

        val copyConfig = CopyConfigBuilder(
            clientInfo.clientId!!, clientInfo.clientId!!,
            clientInfo!!.uid, AdGroup::class.java, listOf(adGroupInfoFrom.adGroupId),
        )
            .withParentIdMapping(BaseCampaign::class.java, campaignId, campaignIdSameClient)
            .build()

        val xerox = factory.build(copyConfig)
        val copyResult = xerox.copy()
        asserts.checkErrors(copyResult)

        val copiedAdGroupIds = copyResult.getEntityMapping(AdGroup::class.java).values.map { it as Long }
        val copiedAdGroup = adGroupRepository.getAdGroups(clientInfo.shard, copiedAdGroupIds)[0]

        val soft = SoftAssertions()
        soft.assertThat(copiedAdGroup.hyperGeoId)
            .`as`("гипергео группы")
            .isNotNull

        val copiedHyperGeoTo = hyperGeoService.getHyperGeoById(clientId, copiedAdGroup.hyperGeoId)

        soft.assertThat(copiedHyperGeoTo.hyperGeoSegments)
            .`as`("количество сегментов гипергео")
            .hasSize(hyperGeo.hyperGeoSegments.size)
        soft.assertThat(copiedHyperGeoTo.hyperGeoSegments[0].id)
            .`as`("сегмент гипергео")
            .isEqualTo(hyperGeo.hyperGeoSegments[0].id)
        soft.assertAll()
    }

    /**
     * Проверка копирования группы с мультисегментным гипергео в одной кампании
     */
    @Test
    fun adGroupWithMultisegmentsHypergeo_CopyInSameCampaign() {
        val adGroupFrom = TestGroups.activeTextAdGroup()
            .withHyperGeoId(hyperGeoWithMultipleSegments.id)
            .withHyperGeoSegmentIds(hyperGeoWithMultipleSegments.hyperGeoSegments
                .map { it.id })
        val adGroupInfoFrom = steps.adGroupSteps().createAdGroup(adGroupFrom, campaignInfo)

        val copyConfig = CopyConfigBuilder(
            clientInfo.clientId!!, clientInfo.clientId!!,
            clientInfo!!.uid, AdGroup::class.java, listOf(adGroupInfoFrom.adGroupId),
        )
            .withParentIdMapping(BaseCampaign::class.java, campaignId, campaignIdSameClient)
            .build()

        val xerox = factory.build(copyConfig)
        val copyResult = xerox.copy()
        asserts.checkErrors(copyResult)

        val copiedAdGroupIds = copyResult.getEntityMapping(AdGroup::class.java).values.map { it as Long }
        val copiedAdGroup = adGroupRepository.getAdGroups(clientInfo.shard, copiedAdGroupIds)[0]

        val soft = SoftAssertions()
        soft.assertThat(copiedAdGroup.hyperGeoId)
            .`as`("гипергео группы")
            .isNotNull

        val copiedHyperGeo = hyperGeoService.getHyperGeoById(clientId, copiedAdGroup.hyperGeoId)

        soft.assertThat(copiedHyperGeo.hyperGeoSegments)
            .`as`("количество сегментов гипергео")
            .hasSize(hyperGeoWithMultipleSegments.hyperGeoSegments.size)
        soft.assertThat(copiedHyperGeo.hyperGeoSegments.map { it.id }.toSet())
            .`as`("сегменты гипергео")
            .isEqualTo(hyperGeoWithMultipleSegments.hyperGeoSegments.map { it.id }.toSet())
        soft.assertAll()
    }

    /**
     * Проверка копирования группы с гипергео между кампаниями
     */
    @Test
    fun adGroupWithHypergeo_CopyToOtherCampaign() {
        val adGroupFrom = TestGroups.activeTextAdGroup()
            .withHyperGeoId(hyperGeo.id)
            .withHyperGeoSegmentIds(hyperGeo.hyperGeoSegments
                .map { it.id })
        val adGroupInfoFrom = steps.adGroupSteps().createAdGroup(adGroupFrom, campaignInfo)

        val copyConfig = CopyConfigBuilder(
            clientInfo.clientId!!, clientInfo.clientId!!,
            clientInfo!!.uid, AdGroup::class.java, listOf(adGroupInfoFrom.adGroupId),
        )
            .withParentIdMapping(BaseCampaign::class.java, campaignId, campaignIdSameClient)
            .build()

        val xerox = factory.build(copyConfig)
        val copyResult = xerox.copy()
        asserts.checkErrors(copyResult)

        val copiedAdGroupIds = copyResult.getEntityMapping(AdGroup::class.java).values.map { it as Long }
        val copiedAdGroup = adGroupRepository.getAdGroups(clientInfo.shard, copiedAdGroupIds)[0]

        val soft = SoftAssertions()
        soft.assertThat(copiedAdGroup.hyperGeoId)
            .`as`("гипергео группы")
            .isNotNull

        val copiedHyperGeoTo = hyperGeoService.getHyperGeoById(clientId, copiedAdGroup.hyperGeoId)

        soft.assertThat(copiedHyperGeoTo.hyperGeoSegments)
            .`as`("количество сегментов гипергео")
            .hasSize(hyperGeo.hyperGeoSegments.size)
        soft.assertThat(copiedHyperGeoTo.hyperGeoSegments[0].id)
            .`as`("сегмент гипергео")
            .isEqualTo(hyperGeo.hyperGeoSegments[0].id)
        soft.assertAll()
    }

    /**
     * Проверка копирования группы с мультисегменетным гипергео между кампаниями
     */
    @Test
    fun adGroupWithMultisegmentsHypergeo_CopyToOtherCampaign() {
        val adGroupFrom = TestGroups.activeTextAdGroup()
            .withHyperGeoId(hyperGeoWithMultipleSegments.id)
            .withHyperGeoSegmentIds(hyperGeoWithMultipleSegments.hyperGeoSegments
                .map { it.id })
        val adGroupInfoFrom = steps.adGroupSteps().createAdGroup(adGroupFrom, campaignInfo)

        val copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
            clientInfo, adGroupInfoFrom.adGroupId, campaignId, campaignIdSameClient, clientInfo.uid)

        val xerox = factory.build(copyConfig)
        val copyResult = xerox.copy()
        asserts.checkErrors(copyResult)

        val copiedAdGroupIds = copyResult.getEntityMapping(AdGroup::class.java).values.map { it as Long }
        val copiedAdGroup = adGroupRepository.getAdGroups(clientInfo.shard, copiedAdGroupIds)[0]

        val soft = SoftAssertions()
        soft.assertThat(copiedAdGroup.hyperGeoId)
            .`as`("гипергео группы")
            .isNotNull

        val copiedHyperGeoTo = hyperGeoService.getHyperGeoById(clientId, copiedAdGroup.hyperGeoId)

        soft.assertThat(copiedHyperGeoTo.hyperGeoSegments)
            .`as`("количество сегментов гипергео")
            .hasSize(hyperGeoWithMultipleSegments.hyperGeoSegments.size)
        soft.assertThat(copiedHyperGeoTo.hyperGeoSegments.map { it.id }.toSet())
            .`as`("сегменты гипергео")
            .isEqualTo(hyperGeoWithMultipleSegments.hyperGeoSegments.map { it.id }.toSet())
        soft.assertAll()
    }

    /**
     * Проверка копирования группы с гипергео между клиентами
     */
    @Test(expected = IllegalStateException::class)
    fun adGroupWithHypergeo_CopyToOtherClient() {
        val adGroupFrom = TestGroups.activeTextAdGroup()
            .withHyperGeoId(hyperGeo.id)
            .withHyperGeoSegmentIds(hyperGeo.hyperGeoSegments
                .map { it.id })
        steps.adGroupSteps().createAdGroup(adGroupFrom, campaignInfo)

        val xerox = factory.build(clientInfo.shard, clientInfo.client!!,
            otherClientInfo.shard, otherClientInfo.client!!,
            otherClientInfo.uid,
            BaseCampaign::class.java,
            listOf(campaignId),
            CopyCampaignFlags(isCopyNotificationSettings = true))

        xerox.copy()
    }


    private fun createHyperGeo(
        hyperGeoSegments: List<HyperGeoSegment> = listOf(defaultHyperGeoSegment()),
    ): HyperGeo {
        val hyperGeo = defaultHyperGeo(hyperGeoSegments = hyperGeoSegments)
        return steps.hyperGeoSteps().createHyperGeo(clientInfo, hyperGeo)
    }
}
