package ru.yandex.direct.core.copyentity

import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.service.AdGroupsAddOperationTestBase
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegment
import ru.yandex.direct.core.entity.hypergeo.service.HyperGeoService
import ru.yandex.direct.core.testing.data.defaultHyperGeo
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName

@Ignore("Базовый класс. Используется только для дочерних")
open class CopyOperationAdGroupWithHyperlocalBaseTest : AdGroupsAddOperationTestBase() {

    @Autowired
    lateinit var factory: CopyOperationFactory

    @Autowired
    lateinit var asserts: CopyOperationAssert

    @Autowired
    lateinit var hyperGeoService: HyperGeoService

    lateinit var otherClientId: ClientId
    lateinit var otherClientInfo: ClientInfo

    var campaignIdSameClient: Long = 0L
    lateinit var campaignInfoSameClient: CampaignInfo

    var campaignIdOtherClient: Long = 0L
    lateinit var campaignInfoOtherClient: CampaignInfo

    lateinit var hyperGeo: HyperGeo
    lateinit var hyperGeoWithMultipleSegments: HyperGeo
    lateinit var adGroupInfoWithOneSegment: AdGroupInfo
    lateinit var adGroupInfoWithMultipleSegments: AdGroupInfo

    fun init() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo!!.clientId
        steps.featureSteps().setCurrentClient(clientId)
        steps.featureSteps().addClientFeature(clientId, FeatureName.HYPERLOCAL_GEO_IN_SMART_AND_DO_FOR_DNA, true)

        otherClientInfo = steps.clientSteps().createDefaultClientAnotherShard()
        otherClientId = otherClientInfo.clientId!!

        asserts.init(clientId, clientId, clientInfo!!.uid)

        hyperGeo = createHyperGeo()
        hyperGeoWithMultipleSegments = createHyperGeo(listOf(defaultHyperGeoSegment(), defaultHyperGeoSegment()))
    }

    /**
     * Проверка копирования группы с гипергео в одной кампании
     */
    @Test
    fun adGroupWithHypergeo_CopyInSameCampaign() {
        val copiedHyperGeo = runAndCheckCopy(campaignId, adGroupInfoWithOneSegment)

        val soft = SoftAssertions()
        soft.assertThat(copiedHyperGeo.hyperGeoSegments)
            .`as`("количество сегментов гипергео")
            .hasSize(hyperGeo.hyperGeoSegments.size)
        soft.assertThat(copiedHyperGeo.hyperGeoSegments[0].id)
            .`as`("сегмент гипергео")
            .isEqualTo(hyperGeo.hyperGeoSegments[0].id)
        soft.assertAll()
    }

    /**
     * Проверка копирования группы с мультисегментным гипергео в одной кампании
     */
    @Test
    fun adGroupWithMultisegmentsHypergeo_CopyInSameCampaign() {
        val copiedHyperGeo = runAndCheckCopy(campaignId, adGroupInfoWithMultipleSegments)

        val soft = SoftAssertions()
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
        val copiedHyperGeo = runAndCheckCopy(campaignIdSameClient, adGroupInfoWithOneSegment)

        val soft = SoftAssertions()
        soft.assertThat(copiedHyperGeo.hyperGeoSegments)
            .`as`("количество сегментов гипергео")
            .hasSize(hyperGeo.hyperGeoSegments.size)
        soft.assertThat(copiedHyperGeo.hyperGeoSegments[0].id)
            .`as`("сегмент гипергео")
            .isEqualTo(hyperGeo.hyperGeoSegments[0].id)
        soft.assertAll()
    }

    /**
     * Проверка копирования группы с мультисегменетным гипергео между кампаниями
     */
    @Test
    fun adGroupWithMultisegmentsHypergeo_CopyToOtherCampaign() {
        val copiedHyperGeo = runAndCheckCopy(campaignIdSameClient, adGroupInfoWithMultipleSegments)

        val soft = SoftAssertions()
        soft.assertThat(copiedHyperGeo.hyperGeoSegments)
            .`as`("количество сегментов гипергео")
            .hasSize(hyperGeoWithMultipleSegments.hyperGeoSegments.size)
        soft.assertThat(copiedHyperGeo.hyperGeoSegments.map { it.id }.toSet())
            .`as`("сегменты гипергео")
            .isEqualTo(hyperGeoWithMultipleSegments.hyperGeoSegments.map { it.id }.toSet())
        soft.assertAll()
    }

    fun runAndCheckCopy(
        toCampaignId: Long,
        adGroupInfoFrom: AdGroupInfo,
    ): HyperGeo {
        val copyConfig = CopyConfigBuilder(
            clientInfo.clientId!!, clientInfo.clientId!!,
            clientInfo!!.uid, AdGroup::class.java, listOf(adGroupInfoFrom.adGroupId),
        )
            .withParentIdMapping(BaseCampaign::class.java, campaignId, toCampaignId)
            .build()

        val xerox = factory.build(copyConfig)
        val copyResult = xerox.copy()
        asserts.checkErrors(copyResult)

        val copiedAdGroupIds = copyResult.getEntityMapping(AdGroup::class.java).values.map { it as Long }
        val copiedAdGroup = adGroupRepository.getAdGroups(clientInfo.shard, copiedAdGroupIds)[0]

        Assertions.assertThat(copiedAdGroup.hyperGeoId)
            .`as`("гипергео группы")
            .isNotNull

        return hyperGeoService.getHyperGeoById(clientId, copiedAdGroup.hyperGeoId)
    }

    private fun createHyperGeo(
        hyperGeoSegments: List<HyperGeoSegment> = listOf(defaultHyperGeoSegment()),
    ): HyperGeo {
        val hyperGeo = defaultHyperGeo(hyperGeoSegments = hyperGeoSegments)
        return steps.hyperGeoSteps().createHyperGeo(clientInfo, hyperGeo)
    }
}

