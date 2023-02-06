package ru.yandex.direct.logicprocessor.processors.mysql2grut.steps

import org.assertj.core.api.Assertions.assertThat
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
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.api.PricePackageGrut
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_CPM_PRICE
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.logicprocessor.processors.mysql2grut.Mysql2GrutReplicationProcessor
import ru.yandex.grut.objects.proto.AdGroupV2

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class CampaignPricePackageReplicationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    private lateinit var clientInfo: ClientInfo

    companion object {
        const val targetingsSnapshot = """
            {"geoType": 3, "behaviors": [], "viewTypes": ["MOBILE"], "geoExpanded": [225], "allowExpandedDesktopCreative": false}
        """
    }

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        replicationApiService.clientGrutDao.createOrUpdateClients(
            listOf(
                ClientGrutModel(
                    clientInfo.client!!,
                    listOf()
                )
            )
        )
    }

    @AfterEach
    fun after() {
        replicationApiService.clientGrutDao.deleteObjects(listOf(clientInfo.clientId!!.asLong()))
    }

    @Test
    fun replicateCpmPriceCampaign_NoPackageInGrut() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaign = TestCampaigns.activeCpmPriceCampaign(null, null)
            .withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        val allowedDomains = listOf("yandex.ru", "ggg.ru")
        val auctionPriority = 12L
        val pricePackage = steps.pricePackageSteps().createPricePackage(
            defaultPricePackage()
                .withAuctionPriority(auctionPriority)
                .withAllowedDomains(allowedDomains)
                .withAvailableAdGroupTypes(setOf(AdGroupType.CPM_YNDX_FRONTPAGE, AdGroupType.CPM_BANNER))
        )

        insertCampaignsCpmPrice(campaignInfo.campaignId, pricePackage.pricePackageId)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        val createdPricePackage = replicationApiService.pricePackageGrutApi.getPricePackage(pricePackage.pricePackageId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdPricePackage).isNotNull
        val assertions = SoftAssertions()
        assertions.assertThat(createdCampaign!!.spec.packageId).isEqualTo(pricePackage.pricePackageId)
        assertions.assertThat(createdPricePackage!!.spec.allowedDomainsList).isEqualTo(allowedDomains)
        assertions.assertThat(createdPricePackage.spec.auctionPriority).isEqualTo(auctionPriority)
        assertions.assertThat(createdPricePackage.spec.availableAdGroupTypesList)
            .containsExactlyInAnyOrder(
                AdGroupV2.EAdGroupType.AGT_CPM_YNDX_FRONTPAGE.number,
                AdGroupV2.EAdGroupType.AGT_CPM.number
            )
        assertions.assertAll()
    }

    @Test
    fun replicateCpmPriceCampaign_PackageExistsInGrut() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaign = TestCampaigns.activeCpmPriceCampaign(null, null)
            .withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        val allowedDomains = listOf("yandex.ru", "ggg.ru")
        val auctionPriority = 12L
        val pricePackage = steps.pricePackageSteps().createPricePackage(
            defaultPricePackage()
                .withAuctionPriority(auctionPriority)
                .withAllowedDomains(allowedDomains)
                .withAvailableAdGroupTypes(setOf(AdGroupType.CPM_YNDX_FRONTPAGE, AdGroupType.CPM_BANNER))
        )
        replicationApiService.pricePackageGrutApi.createOrUpdatePackages(
            listOf(
                PricePackageGrut(
                    pricePackage.pricePackageId,
                    pricePackage.pricePackage.auctionPriority,
                    pricePackage.pricePackage.allowedDomains,
                    pricePackage.pricePackage.availableAdGroupTypes
                )
            )
        )

        insertCampaignsCpmPrice(campaignInfo.campaignId, pricePackage.pricePackageId)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        val createdPricePackage = replicationApiService.pricePackageGrutApi.getPricePackage(pricePackage.pricePackageId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdPricePackage).isNotNull
        val assertions = SoftAssertions()
        assertions.assertThat(createdCampaign!!.spec.packageId).isEqualTo(pricePackage.pricePackageId)
        assertions.assertThat(createdPricePackage!!.spec.allowedDomainsList).isEqualTo(allowedDomains)
        assertions.assertThat(createdPricePackage.spec.auctionPriority).isEqualTo(auctionPriority)
        assertions.assertThat(createdPricePackage.spec.availableAdGroupTypesList)
            .containsExactlyInAnyOrder(
                AdGroupV2.EAdGroupType.AGT_CPM_YNDX_FRONTPAGE.number,
                AdGroupV2.EAdGroupType.AGT_CPM.number
            )
        assertions.assertAll()
    }

    @Test
    fun replicateCpmPriceCampaign_NoAllowedDomainsTest() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaign = TestCampaigns.activeCpmPriceCampaign(null, null)
            .withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        val auctionPriority = 12L
        val pricePackage = steps.pricePackageSteps().createPricePackage(
            defaultPricePackage().withAuctionPriority(auctionPriority).withAllowedDomains(null)
        )

        insertCampaignsCpmPrice(campaignInfo.campaignId, pricePackage.pricePackageId)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        val createdPricePackage = replicationApiService.pricePackageGrutApi.getPricePackage(pricePackage.pricePackageId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdPricePackage).isNotNull
        val assertions = SoftAssertions()

        assertions.assertThat(createdCampaign!!.spec.packageId).isEqualTo(pricePackage.pricePackageId)
        assertions.assertThat(createdPricePackage!!.spec.allowedDomainsList).isEmpty()
        assertions.assertThat(createdPricePackage.spec.auctionPriority).isEqualTo(auctionPriority)
        assertions.assertAll()
    }

    private fun insertCampaignsCpmPrice(campaignId: Long, packageId: Long) {
        dslContextProvider.ppc(clientInfo.shard)
            .insertInto(
                CAMPAIGNS_CPM_PRICE,
                CAMPAIGNS_CPM_PRICE.CID,
                CAMPAIGNS_CPM_PRICE.PACKAGE_ID,
                CAMPAIGNS_CPM_PRICE.TARGETINGS_SNAPSHOT,
                CAMPAIGNS_CPM_PRICE.ORDER_VOLUME
            )
            .values(campaignId, packageId, targetingsSnapshot, 10000000)
            .execute()
    }
}
