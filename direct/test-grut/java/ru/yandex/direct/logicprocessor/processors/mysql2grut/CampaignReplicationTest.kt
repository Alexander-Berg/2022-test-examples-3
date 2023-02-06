package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.common.db.PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN
import ru.yandex.direct.core.entity.additionaltargetings.model.CampAdditionalTargeting
import ru.yandex.direct.core.entity.additionaltargetings.repository.CampAdditionalTargetingsRepository
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator
import ru.yandex.direct.core.entity.campaign.converter.CampaignConverter.meaningfulGoalsToDb
import ru.yandex.direct.core.entity.campaign.model.CampaignMetatype
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.mobileapp.model.SkAdNetworkSlot
import ru.yandex.direct.core.entity.mobileapp.repository.IosSkAdNetworkSlotRepository
import ru.yandex.direct.core.grut.api.CampaignGrutModel
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmPriceCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.activeInternalDistribCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.activeInternalFreeCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.activeMobileContentCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_CPM_PRICE
import ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_INTERNAL
import ru.yandex.direct.dbschema.ppc.Tables.CAMP_CALLTRACKING_SETTINGS
import ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS
import ru.yandex.direct.dbschema.ppc.Tables.WALLET_CAMPAIGNS
import ru.yandex.direct.dbschema.ppc.Tables.WIDGET_PARTNER_CAMPAIGNS
import ru.yandex.direct.dbschema.ppc.enums.CampaignsAttributionModel
import ru.yandex.direct.dbschema.ppc.enums.CampaignsAutobudget
import ru.yandex.direct.dbschema.ppc.enums.CampaignsInternalRestrictionType
import ru.yandex.direct.dbschema.ppc.enums.CampaignsInternalRfCloseByClick
import ru.yandex.direct.dbschema.ppc.enums.CampaignsPaidByCertificate
import ru.yandex.direct.dbschema.ppc.enums.CampaignsPlatform
import ru.yandex.direct.dbschema.ppc.enums.CampaignsSource
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusempty
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusopenstat
import ru.yandex.direct.dbschema.ppc.enums.WalletCampaignsIsSumAggregated
import ru.yandex.direct.dbschema.ppc.tables.Campaigns.CAMPAIGNS
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.utils.DateTimeUtils.moscowDateTimeToEpochSecond
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.utils.JsonUtils.toJson
import ru.yandex.grut.auxiliary.proto.YabsOperation
import ru.yandex.grut.objects.proto.CampaignPlatform
import ru.yandex.grut.objects.proto.CampaignV2

import ru.yandex.yabs.server.proto.keywords.EKeyword

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class CampaignReplicationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var shardHelper: ShardHelper

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var bsOrderIdCalculator: BsOrderIdCalculator

    @Autowired
    private lateinit var campAdditionalTargetingsRepository: CampAdditionalTargetingsRepository

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var iosSkAdNetworkSlotRepository: IosSkAdNetworkSlotRepository

    private lateinit var clientInfo: ClientInfo

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
    fun createCampaignTest() {
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull

        val mysqlCampaign =
            campaignTypedRepository.getTyped(clientInfo.shard, listOf(campaignInfo.campaignId))[0] as CommonCampaign
        val softly = SoftAssertions()

        softly.assertThat(createdCampaign!!.meta.directId).isEqualTo(campaignInfo.campaign.id)
        softly.assertThat(createdCampaign.meta.id).isEqualTo(campaignInfo.campaign.orderId)
        softly.assertThat(createdCampaign.meta.creationTime / 1_000_000)
            .isEqualTo(moscowDateTimeToEpochSecond(mysqlCampaign.createTime))
        softly.assertThat(createdCampaign.meta.clientId).isEqualTo(campaignInfo.clientId.asLong())
        softly.assertThat(createdCampaign.meta.directType).isEqualTo(CampaignV2.ECampaignType.CT_TEXT.number)
        softly.assertThat(createdCampaign.spec.name).isEqualTo(campaignInfo.campaign.name)
        softly.assertThat(createdCampaign.spec.status).isEqualTo(CampaignV2.ECampaignStatus.CST_ACTIVE.number)
        softly.assertThat(createdCampaign.spec.currencyIsoCode).isEqualTo(643)
        softly.assertThat(createdCampaign.meta.metaType).isEqualTo(CampaignV2.ECampaignMetaType.CM_DEFAULT.number)

        softly.assertAll()
    }

    @Test
    fun createCampaignWithoutClientTest() {
        ppcPropertiesSupport.set(CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "true")
        val notExistenceClientInGrut = steps.clientSteps().createDefaultClient()
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, notExistenceClientInGrut)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    orderId = campaignInfo.campaign.orderId
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNull()
    }

    @Test
    fun createCampaignWithoutWalletTest() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "true")
        val wallet = activeWalletCampaign(null, null)
            .withOrderId(0L)
        val walletInfo = steps.campaignSteps().createCampaign(wallet, clientInfo)
        val campaign = activeTextCampaign(null, null)
            .withWalletId(walletInfo.campaignId)
            .withOrderId(0L)
        campaign.balanceInfo.walletCid = walletInfo.campaignId
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNull()
    }

    @Test
    fun createCampaignWithWalletTest() {
        val wallet = activeWalletCampaign(null, null)
            .withOrderId(0L)
        val walletInfo = steps.campaignSteps().createCampaign(wallet, clientInfo)
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)
            .withWalletId(walletInfo.campaignId)
        campaign.balanceInfo.walletCid = walletInfo.campaignId
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = walletInfo.campaignId,
                    clientId = walletInfo.clientId.asLong()
                ),
            )
        )

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                ),
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        val createdWallet = replicationApiService.campaignGrutDao.getCampaignByDirectId(walletInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdWallet).isNotNull
        assertThat(createdCampaign!!.spec.walletCampaignId).isEqualTo(walletInfo.campaignId + 100_000_000)
    }

    /*
        Тест проверяет, что если пришло изменение по кампании, а кошелек для нее не создан, то он создастся
        такое может случиться, если сначала пришло, например, событие по изменении имени кампании, к моменту обработки
        уже создался и прилинковался кошелек, но это событие еще не доехало
     */
    @Test
    fun createCampaignWithWallet_WalletNotReplicatedYetTest() {
        ppcPropertiesSupport.set(CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val wallet = activeWalletCampaign(null, null)
            .withOrderId(0L)
        val walletInfo = steps.campaignSteps().createCampaign(wallet, clientInfo)
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)
            .withWalletId(walletInfo.campaignId)
        campaign.balanceInfo.walletCid = walletInfo.campaignId
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                ),
            )
        )

        ppcPropertiesSupport.set(CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "true")
        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        val createdWallet = replicationApiService.campaignGrutDao.getCampaignByDirectId(walletInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdWallet).isNotNull
        assertThat(createdCampaign!!.spec.walletCampaignId).isEqualTo(walletInfo.campaignId + 100_000_000)
    }

    @Test
    fun createCampaignWithWalletInSameChunkTest() {
        val wallet = activeWalletCampaign(null, null)
            .withOrderId(0L)
        val walletInfo = steps.campaignSteps().createCampaign(wallet, clientInfo)
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)
            .withWalletId(walletInfo.campaignId)
        campaign.balanceInfo.walletCid = walletInfo.campaignId
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = walletInfo.campaignId,
                    clientId = walletInfo.clientId.asLong()
                ),
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                ),
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        val createdWallet = replicationApiService.campaignGrutDao.getCampaignByDirectId(walletInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdWallet).isNotNull
        assertThat(createdCampaign!!.spec.walletCampaignId).isEqualTo(walletInfo.campaignId + 100_000_000)
    }

    @Test
    fun createCampaignWithoutAgencyTest() {
        ppcPropertiesSupport.set(CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "true")
        val notExistenceAgencyInGrut = steps.clientSteps().createDefaultClient()

        val campaign = activeTextCampaign(null, null)
            .withAgencyId(notExistenceAgencyInGrut.clientId!!.asLong())
            .withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNull()
    }

    @Test
    fun createCampaignWithAgencyTest() {
        val agency = steps.clientSteps().createDefaultClient()
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)
            .withAgencyId(agency.clientId!!.asLong())
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = agency.clientId!!.asLong()),
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.meta.agencyClientId).isEqualTo(agency.clientId!!.asLong())
    }

    @Test
    fun createCampaignWithoutOrderIdTest() {
        val campaignInfo = steps.campaignSteps().createDefaultCampaignByCampaignType(CampaignType.TEXT)
        processor.withShard(campaignInfo.shard)
        val expectedOrderId = campaignInfo.campaignId + 100_000_000
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(expectedOrderId)
        assertThat(createdCampaign).isNotNull

        val mysqlCampaign =
            campaignTypedRepository.getTyped(clientInfo.shard, listOf(campaignInfo.campaignId))[0] as CommonCampaign
        val softly = SoftAssertions()

        softly.assertThat(createdCampaign!!.meta.directId).isEqualTo(campaignInfo.campaign.id)
        softly.assertThat(createdCampaign.meta.id).isEqualTo(expectedOrderId)
        softly.assertThat(createdCampaign.meta.creationTime / 1_000_000)
            .isEqualTo(moscowDateTimeToEpochSecond(mysqlCampaign.createTime))
        softly.assertThat(createdCampaign.meta.clientId).isEqualTo(campaignInfo.clientId.asLong())
        softly.assertThat(createdCampaign.meta.directType).isEqualTo(CampaignV2.ECampaignType.CT_TEXT.number)
        softly.assertThat(createdCampaign.spec.name).isEqualTo(campaignInfo.campaign.name)
        softly.assertThat(createdCampaign.spec.status).isEqualTo(CampaignV2.ECampaignStatus.CST_ACTIVE.number)
        softly.assertThat(createdCampaign.spec.currencyIsoCode).isEqualTo(643)

        softly.assertAll()
    }

    @Test
    fun replicateInternalCampaignTest() {
        val campaignInfo = steps.campaignSteps().createActiveInternalAutobudgetCampaign()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull

        val softly = SoftAssertions()

        softly.assertThat(createdCampaign!!.spec.placeId).isNotNull
        softly.assertThat(createdCampaign.meta.directType)
            .isEqualTo(CampaignV2.ECampaignType.CT_INTERNAL_AUTOBUDGET.number)

        softly.assertAll()
    }

    @Test
    fun updateCampaignTest() {
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(clientInfo)
        processor.withShard(campaignInfo.shard)
        val mysqlCampaign =
            campaignTypedRepository.getTyped(clientInfo.shard, listOf(campaignInfo.campaignId))[0] as CommonCampaign
        replicationApiService.campaignGrutDao.createOrUpdateCampaign(CampaignGrutModel(mysqlCampaign, 1))
        val newName = "${mysqlCampaign.name} New"
        updateCampaignName(clientInfo.shard, campaignInfo.campaignId, newName)

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val updatedCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(updatedCampaign).isNotNull

        val softly = SoftAssertions()

        softly.assertThat(updatedCampaign!!.meta.directId).isEqualTo(campaignInfo.campaign.id)
        softly.assertThat(updatedCampaign.meta.id).isEqualTo(campaignInfo.campaign.orderId)
        softly.assertThat(updatedCampaign.meta.creationTime / 1_000_000)
            .isEqualTo(moscowDateTimeToEpochSecond(mysqlCampaign.createTime))
        softly.assertThat(updatedCampaign.meta.clientId).isEqualTo(campaignInfo.clientId.asLong())
        softly.assertThat(updatedCampaign.meta.directType).isEqualTo(CampaignV2.ECampaignType.CT_TEXT.number)
        softly.assertThat(updatedCampaign.spec.name).isEqualTo(newName)
        softly.assertThat(updatedCampaign.spec.status).isEqualTo(CampaignV2.ECampaignStatus.CST_ACTIVE.number)
        softly.assertThat(updatedCampaign.spec.currencyIsoCode).isEqualTo(643)

        softly.assertAll()
    }

    /**
     * В репликации разрешается менять поле source
     * это может происходить, когда коммандер сначала создает камапнию с source = 'direct', а после меняет на 'dc'
     */
    @Test
    fun updateCampaignSourceTest() {
        val campaign = activeTextCampaign(null, null)
            .withSource(CampaignSource.DIRECT)
            .withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        processor.withShard(campaignInfo.shard)
        val mysqlCampaign =
            campaignTypedRepository.getTyped(clientInfo.shard, listOf(campaignInfo.campaignId))[0] as CommonCampaign
        replicationApiService.campaignGrutDao.createOrUpdateCampaign(CampaignGrutModel(mysqlCampaign, 1))
        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.SOURCE, CampaignsSource.dc)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val updatedCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaignId)
        assertThat(updatedCampaign).isNotNull
        assertThat(updatedCampaign!!.meta.source).isEqualTo(CampaignV2.ECampaignSource.CSR_DC.number)
    }

    /**
     * В директе в процессе создания кампании может меняться agency_client_id
     */
    @Test
    fun updateCampaignAgencyIdTest() {
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(clientInfo)
        val agency = steps.clientSteps().createDefaultClient()
        replicationApiService.clientGrutDao.createOrUpdateClients(
            listOf(
                ClientGrutModel(
                    agency.client!!,
                    listOf()
                )
            )
        )

        val agencyClientId = agency.clientId!!.asLong()

        processor.withShard(campaignInfo.shard)
        val mysqlCampaign =
            campaignTypedRepository.getTyped(clientInfo.shard, listOf(campaignInfo.campaignId))[0] as CommonCampaign
        replicationApiService.campaignGrutDao.createOrUpdateCampaign(CampaignGrutModel(mysqlCampaign, 1))

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.AGENCY_ID, agencyClientId)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val updatedCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaignId)
        assertThat(updatedCampaign).isNotNull
        assertThat(updatedCampaign!!.meta.agencyClientId).isEqualTo(agencyClientId)
    }

    @Test
    fun deleteExistenceCampaignTest() {
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(clientInfo)
        processor.withShard(clientInfo.shard)
        val mysqlCampaign =
            campaignTypedRepository.getTyped(clientInfo.shard, listOf(campaignInfo.campaignId))[0] as CommonCampaign

        replicationApiService.campaignGrutDao.createOrUpdateCampaign(CampaignGrutModel(mysqlCampaign, orderType = 1))

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = clientInfo.clientId!!.asLong(),
                    isDeleted = true
                )
            )
        )

        val deletedCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(deletedCampaign).isNotNull
    }

    @Test
    fun deleteNotExistenceCampaignTest() {
        val notExistenceId = shardHelper.generateCampaignIds(clientInfo.clientId!!.asLong(), 1)[0]
        processor.withShard(clientInfo.shard)
        val notExistenceCampaign = TextCampaign()
            .withId(notExistenceId)
            .withClientId(clientInfo.clientId!!.asLong())
            .withProductId(0)
            .withOrderId(notExistenceId + 100_000_000)
            .withName("name")
            .withType(CampaignType.TEXT)
            .withCurrency(CurrencyCode.RUB)
            .withStrategy(defaultStrategy())
            .withSource(CampaignSource.UAC)
            .withCreateTime(LocalDateTime.of(2021, 12, 31, 1, 1, 1))
            .withMetatype(CampaignMetatype.DEFAULT_)
            .withSum(BigDecimal.valueOf(0))
            .withSumSpent(BigDecimal.valueOf(0))
        replicationApiService.campaignGrutDao.createOrUpdateCampaign(
            CampaignGrutModel(
                notExistenceCampaign,
                orderType = 1
            )
        )

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = notExistenceId,
                    clientId = clientInfo.clientId!!.asLong(),
                    isDeleted = true
                )
            )
        )

        val deletedCampaign = replicationApiService.campaignGrutDao.getCampaign(notExistenceId + 100_000_000)
        assertThat(deletedCampaign).isNull()
    }

    @Test
    fun tryReplicateCampaignWithDeprecatedType() {
        // arrange
        val campaignInfo = steps.campaignSteps()
            .createCampaign(TestCampaigns.newGeoCampaign(clientInfo.clientId, clientInfo.uid), clientInfo)
        processor.withShard(campaignInfo.shard)
        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )
        // assert
        val orderId = bsOrderIdCalculator.calculateOrderId(campaignInfo.campaignId)
        val notExistingCampaigns = replicationApiService.campaignGrutDao.getExistingObjects(listOf(orderId))
        assertThat(notExistingCampaigns).isEmpty()
    }

    @Test
    fun campaignTimeTargetReplicationTest() {
        val timeTarget = "1JKLMNOPQ2JKLMNOPQ3JKLMNOPQ4JKLMNOPQ5JKLMNOPQ67;p:w"
        val campaign = activeTextCampaign(null, null)
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.TIME_TARGET, timeTarget)
            .where(CAMPAIGNS.CID.eq(campaign.id))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.timeTargetStr).isEqualTo(timeTarget)
    }

    @Test
    fun campaignManagerUidReplicationTest() {
        val campaign = activeTextCampaign(null, null)
            .withManagerUid(12345L)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.managerUid).isEqualTo(campaign.managerUid)
    }

    @Test
    fun campaignWithNullTimeTargetReplicationTest() {
        val campaign = activeTextCampaign(null, null)
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .setNull(CAMPAIGNS.TIME_TARGET)
            .where(CAMPAIGNS.CID.eq(campaign.id))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.hasTimeTargetStr()).isFalse()
    }

    @Test
    fun campaignFlagsReplicationTest() {
        val campaign = activeTextCampaign(null, null)
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)
        val campaignOpts =
            "is_virtual,is_alone_trafaret_allowed,require_filtration_by_dont_show_domains,no_title_substitute," +
                "hide_permalink_info,no_extended_geotargeting,enable_cpc_hold,is_allowed_on_adult_content,has_turbo_app,is_skadnetwork_enabled"
        val statusOpenStat = CampaignsStatusopenstat.Yes
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.OPTS, campaignOpts)
            .set(CAMPAIGNS.STATUS_OPEN_STAT, statusOpenStat)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull
        val expectedFlags = CampaignV2.TCampaignV2Spec.TFlags.newBuilder().apply {
            isVirtual = true
            isAloneTrafaretAllowed = true
            requireFiltrationByDontShowDomains = true
            noTitleSubstitute = true
            hidePermalinkInfo = true
            noExtendedGeotargeting = true
            enableCpcHold = true
            isAllowedOnAdultContent = true
            addMetrikaTagToUrl = false
            siteMonitoringEnabled = false
            hasTurboSmarts = false
            openStatEnabled = true
            hasTurboApp = true
            isNewIosVersionEnabled = false
            isSkadNetworkEnabled = true
            isWorldwide = false
        }.build()
        assertThat(createdCampaign!!.spec.flags).isEqualTo(expectedFlags)
    }

    // has_turbo_smarts есть только в dynamic и smart кампаниях, поэтому для него отдельный тест
    @Test
    fun campaignHasTurboSmartsFlagReplicationTest() {
        val campaign = activeDynamicCampaign(null, null)
            .withOrderId(0)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        val campaignOpts = "has_turbo_smarts"
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.OPTS, campaignOpts)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.flags.hasTurboSmarts).isTrue
    }

    // is_ww_managed_order есть только в smart кампаниях, поэтому для него отдельный тест
    @Test
    fun campaignIsWorldWideFlagReplicationTest() {
        val campaign = activePerformanceCampaign(null, null)
            .withOrderId(0)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        val campaignOpts = "is_ww_managed_order"
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.OPTS, campaignOpts)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.flags.isWorldwide).isTrue
    }

    // is_new_ios_version_enabled есть только в mobile_content кампаниях, поэтому для него отдельный тест
    @Test
    fun campaignIsNewOsVersionEnabledFlagReplicationTest() {
        val campaign = activeMobileContentCampaign(null, null)
            .withOrderId(0)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        val campaignOpts = "is_new_ios_version_enabled"
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.OPTS, campaignOpts)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.flags.isNewIosVersionEnabled).isTrue
    }

    @Test
    fun campaignMoneyParamsReplicationTest() {
        val campaign = activeWalletCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        dslContextProvider.ppc(campaignInfo.shard)
            .insertInto(WALLET_CAMPAIGNS, WALLET_CAMPAIGNS.WALLET_CID, WALLET_CAMPAIGNS.IS_SUM_AGGREGATED)
            .values(campaignInfo.campaignId, WalletCampaignsIsSumAggregated.Yes)
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.hasMoneyParams())
        assertThat(createdCampaign.spec.moneyParams.flags.isWalletSumAggregated).isTrue
        assertThat(createdCampaign.spec.moneyParams.flags.isPaidByCertificate).isFalse
    }

    @Test
    fun campaignMoneyParams_PaidByCertificateReplicationTest() {
        val campaign = activeWalletCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.PAID_BY_CERTIFICATE, CampaignsPaidByCertificate.Yes)
            .where(
                CAMPAIGNS.CID.eq(campaignInfo.campaignId)
            )
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.hasMoneyParams())
        assertThat(createdCampaign.spec.moneyParams.flags.isWalletSumAggregated).isFalse
        assertThat(createdCampaign.spec.moneyParams.flags.isPaidByCertificate).isTrue
    }

    @Test
    fun campaignSumsReplicationTest() {
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.SUM, BigDecimal.valueOf(35763.98))
            .set(CAMPAIGNS.SUM_SPENT, BigDecimal.valueOf(8764.13))
            .set(CAMPAIGNS.DAY_BUDGET, BigDecimal.valueOf(787.66))
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        val soft = SoftAssertions()
        soft.assertThat(createdCampaign!!.spec.hasMoneyParams())
        soft.assertThat(createdCampaign.spec.moneyParams.flags.hasIsWalletSumAggregated()).isFalse
        soft.assertThat(createdCampaign.spec.moneyParams.sum).isEqualTo(35763980000)
        soft.assertThat(createdCampaign.spec.moneyParams.sumSpent).isEqualTo(8764130000)
        soft.assertThat(createdCampaign.spec.moneyParams.dayBudget).isEqualTo(787660000)
        soft.assertAll()
    }

    @Test
    fun campaignStrategyReplicationTest() {
        val campaign = activeTextCampaign(null, null)
        val strategyName = StrategyName.PERIOD_FIX_BID
        val strategyData = StrategyData()
            .withAutoProlongation(1L)
            .withAvgBid(BigDecimal.valueOf(1234567, 2))
            .withAvgCpa(BigDecimal.valueOf(7654321, 3))
            .withAvgCpi(BigDecimal.valueOf(987654321, 3))
            .withAvgCpm(BigDecimal.valueOf(123456789, 1))
            .withAvgCpv(BigDecimal.valueOf(1234567, 0))
            .withBid(BigDecimal.valueOf(123456789, 6))
            .withBudget(BigDecimal.valueOf(900000000, 6))
            .withCrr(15)
            .withDailyChangeCount(145)
            .withFilterAvgBid(BigDecimal.valueOf(1000001, 5))
            .withFilterAvgCpa(BigDecimal.valueOf(2000003, 5))
            .withGoalId(8974626L)
            .withPayForConversion(false)
            .withLastBidderRestartTime(LocalDateTime.of(2022, 1, 3, 3, 3))
            .withLastUpdateTime(LocalDateTime.of(2022, 1, 7, 1, 19, 10))
            .withName("default")
            .withStart(LocalDate.of(2022, 1, 2))
            .withFinish(LocalDate.of(2022, 2, 18))
            .withSum(BigDecimal.valueOf(3000003, 2))
            .withProfitability(BigDecimal.valueOf(1345, 2))
            .withRoiCoef(BigDecimal.valueOf(75123, 3))
            .withReserveReturn(50)

        val platform = CampaignsPlatform.search
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.STRATEGY_NAME, StrategyName.toSource(strategyName))
            .set(CAMPAIGNS.STRATEGY_DATA, toJson(strategyData))
            .set(CAMPAIGNS.PLATFORM, platform)
            .set(CAMPAIGNS.AUTOBUDGET, CampaignsAutobudget.Yes)
            .where(CAMPAIGNS.CID.eq(campaign.id))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull
        val softly = SoftAssertions()
        val gotStrategy = createdCampaign!!.spec.strategy
        softly.assertThat(gotStrategy.type)
            .isEqualTo(CampaignV2.TCampaignV2Spec.TStrategy.EStrategyType.ST_PERIOD_FIX_BID.number)
        softly.assertThat(gotStrategy.autoProlongation).isEqualTo(true)
        softly.assertThat(gotStrategy.avgBid).isEqualTo(12345670000L)
        softly.assertThat(gotStrategy.avgCpa).isEqualTo(7654321000L)
        softly.assertThat(gotStrategy.avgCpi).isEqualTo(987654321000L)
        softly.assertThat(gotStrategy.avgCpm).isEqualTo(12345678900000L)
        softly.assertThat(gotStrategy.avgCpv).isEqualTo(1234567000000L)
        softly.assertThat(gotStrategy.bid).isEqualTo(123456789L)
        softly.assertThat(gotStrategy.budget).isEqualTo(900000000L)
        softly.assertThat(gotStrategy.crr).isEqualTo(15)
        softly.assertThat(gotStrategy.dailyChangeCount).isEqualTo(145)
        softly.assertThat(gotStrategy.filterAvgBid).isEqualTo(10000010L)
        softly.assertThat(gotStrategy.filterAvgCpa).isEqualTo(20000030L)
        softly.assertThat(gotStrategy.goalId).isEqualTo(8974626L)
        softly.assertThat(gotStrategy.payForConversion).isEqualTo(false)
        softly.assertThat(gotStrategy.bidderRestartTime).isEqualTo(1641168180L)
        softly.assertThat(gotStrategy.updateTime).isEqualTo(1641507550L)
        softly.assertThat(gotStrategy.name).isEqualTo("default")
        softly.assertThat(gotStrategy.startDate).isEqualTo(1641070800L)
        softly.assertThat(gotStrategy.finishDate).isEqualTo(1645131600L)
        softly.assertThat(gotStrategy.budgetLimit).isEqualTo(30000030000L)
        softly.assertThat(gotStrategy.profitability).isEqualTo(13450000L)
        softly.assertThat(gotStrategy.roiCoef).isEqualTo(75123000L)
        softly.assertThat(gotStrategy.reserveReturn).isEqualTo(50)
        softly.assertThat(gotStrategy.autobudgetEnabled).isTrue
        softly.assertThat(createdCampaign.spec.platform).isEqualTo(CampaignPlatform.ECampaignPlatform.CP_SEARCH.number)
        softly.assertAll()
    }

    @Test
    fun campaignFlags_AllFalseReplicationTest() {
        val campaign = activeTextCampaign(null, null)
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)
        val campaignOpts = ""
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.OPTS, campaignOpts)
            .where(CAMPAIGNS.CID.eq(campaign.id))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull
        val expectedFlags = CampaignV2.TCampaignV2Spec.TFlags.newBuilder().apply {
            isVirtual = false
            isAloneTrafaretAllowed = false
            requireFiltrationByDontShowDomains = false
            noTitleSubstitute = false
            hidePermalinkInfo = false
            noExtendedGeotargeting = false
            enableCpcHold = false
            isAllowedOnAdultContent = false
            siteMonitoringEnabled = false
            addMetrikaTagToUrl = false
            openStatEnabled = false
            hasTurboSmarts = false
            hasTurboApp = false
            isNewIosVersionEnabled = false
            isSkadNetworkEnabled = false
            isWorldwide = false
        }.build()
        assertThat(createdCampaign!!.spec.flags).isEqualTo(expectedFlags)
    }

    /**
     * Тест проверяет, что кампании со statusEmpty = Yes не удаляются из GrUT
     */
    @Test
    fun tryReplicateEmptyCampaign() {
        // сначала кампания со statusEmpty = No
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(clientInfo)
        campaignInfo.campaign.withOrderId(null)
        processor.withShard(clientInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )
        val notEmptyCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(notEmptyCampaign).isNotNull

        // теперь кампания пустая
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.STATUS_EMPTY, CampaignsStatusempty.Yes)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )
        val emptyCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        // пустая кампания теперь не должна удаляться
        assertThat(emptyCampaign).isNotNull
        assertThat(emptyCampaign!!.spec.status).isEqualTo(CampaignV2.ECampaignStatus.CST_CREATING.number)
    }

    @Test
    fun campaignAttributionModelReplicationTest() {
        val campaign = activeTextCampaign(null, null)

        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.ATTRIBUTION_MODEL, CampaignsAttributionModel.first_click_cross_device)
            .where(CAMPAIGNS.CID.eq(campaign.id))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.attribution).isEqualTo(CampaignV2.ECampaignAttribution.CA_FIRST_CLICK_CROSS_DEVICE.number)
    }

    @Test
    fun campaignSourceReplicationTest() {
        val campaign = activeTextCampaign(null, null)
            .withSource(CampaignSource.EDA)

        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.meta.source).isEqualTo(CampaignV2.ECampaignSource.CSR_EDA.number)
    }

    @Test
    fun campaignMetrikaCountersReplicationTest() {
        val metrikaCounters = listOf(143522L, 41534523L, 342252L)
        val campaign = activeTextCampaign(null, null)
            .withMetrikaCounters(metrikaCounters)

        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.metrikaCountersIdsList).containsAnyElementsOf(metrikaCounters)
    }

    @Test
    fun campaignStartEndDateReplicationTest() {
        val campaign = activeTextCampaign(null, null)
            .withStartTime(LocalDate.of(2022, 2, 1))
            .withFinishTime(LocalDate.of(2022, 3, 2))

        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.startDate).isEqualTo(1643662800L)
        assertThat(createdCampaign.spec.endDate).isEqualTo(1646168400L)
    }

    @Test
    fun campaignStartEndDate_NullEndDateReplicationTest() {
        val campaign = activeTextCampaign(null, null)
            .withStartTime(LocalDate.of(2022, 2, 1))
            .withFinishTime(null)

        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.ATTRIBUTION_MODEL, CampaignsAttributionModel.first_click_cross_device)
            .where(CAMPAIGNS.CID.eq(campaign.id))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.startDate).isEqualTo(1643662800L)
        assertThat(createdCampaign.spec.hasEndDate()).isFalse
    }

    @Test
    fun calltrackingSettingsIdReplicationTest() {
        val campaign = activeTextCampaign(null, null)
        val settingsId = 934242L
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .insertInto(
                CAMP_CALLTRACKING_SETTINGS,
                CAMP_CALLTRACKING_SETTINGS.CID,
                CAMP_CALLTRACKING_SETTINGS.CALLTRACKING_SETTINGS_ID
            )
            .values(campaign.id, settingsId)
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.calltrackingSettingsId).isEqualTo(settingsId)
    }

    @Test
    fun meaningfulGoalsReplicationTest() {
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        val meaningfulGoals = listOf(
            MeaningfulGoal().withGoalId(1234L).withConversionValue(BigDecimal.valueOf(196.34))
                .withIsMetrikaSourceOfValue(true),
            MeaningfulGoal().withGoalId(9876L).withConversionValue(BigDecimal.valueOf(5000))
                .withIsMetrikaSourceOfValue(false),
            MeaningfulGoal().withGoalId(6743L).withConversionValue(BigDecimal.valueOf(0.78)),
        )
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMP_OPTIONS)
            .set(CAMP_OPTIONS.MEANINGFUL_GOALS, meaningfulGoalsToDb(meaningfulGoals, true))
            .where(CAMP_OPTIONS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        val expectedMeaningfulGoals = listOf(
            CampaignV2.TCampaignV2Spec.TMeaningfulGoal.newBuilder()
                .apply {
                    goalId = 1234L
                    conversionValue = 196_340_000
                    isMetrikaSourceOfValue = true
                }.build(),
            CampaignV2.TCampaignV2Spec.TMeaningfulGoal.newBuilder()
                .apply {
                    goalId = 9876L
                    conversionValue = 5_000_000_000
                    isMetrikaSourceOfValue = false
                }.build(),
            CampaignV2.TCampaignV2Spec.TMeaningfulGoal.newBuilder()
                .apply {
                    goalId = 6743L
                    conversionValue = 780_000
                    isMetrikaSourceOfValue = false
                }.build(),
        )
        assertThat(createdCampaign!!.spec.meaningfulGoalsList).containsExactlyInAnyOrder(*expectedMeaningfulGoals.toTypedArray())
        val mysqlCampaign =
            campaignTypedRepository.getTyped(clientInfo.shard, listOf(campaignInfo.campaignId))[0] as CommonCampaign
        val expectedMeaningfulGoalsHash = replicationApiService.campaignGrutDao.getMeaningfulGoalsHash(mysqlCampaign)
        assertThat(createdCampaign!!.spec.meaningfulGoalsHash).isEqualTo(expectedMeaningfulGoalsHash)
    }

    @Test
    fun placementTypesReplicationTest() {
        val campaign = activeDynamicCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMP_OPTIONS)
            .set(CAMP_OPTIONS.PLACEMENT_TYPES, "adv_gallery")
            .where(CAMP_OPTIONS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        val expectedPlacementTypes = CampaignV2.TCampaignV2Spec.TPlacementTypes.newBuilder()
            .apply {
                advGallery = true
                searchPage = false
            }
            .build()
        assertThat(createdCampaign!!.spec.placementTypes).isEqualTo(expectedPlacementTypes)
    }

    @Test
    fun restrinctionTypeAndValueReplicationTest() {
        val campaign = activeInternalFreeCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .insertInto(
                CAMPAIGNS_INTERNAL,
                CAMPAIGNS_INTERNAL.CID,
                CAMPAIGNS_INTERNAL.RESTRICTION_TYPE,
                CAMPAIGNS_INTERNAL.RESTRICTION_VALUE
            )
            .values(campaignInfo.campaignId, CampaignsInternalRestrictionType.clicks, 88)
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.internalAdOptions.restrictionType).isEqualTo(CampaignV2.ERestrictionType.RT_CLICKS.number)
        assertThat(createdCampaign.spec.internalAdOptions.restrictionValue).isEqualTo(88)
    }

    @Test
    fun rfCloseByClickReplicationTest() {
        val campaign = activeInternalFreeCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .insertInto(CAMPAIGNS_INTERNAL, CAMPAIGNS_INTERNAL.CID, CAMPAIGNS_INTERNAL.RF_CLOSE_BY_CLICK)
            .values(campaignInfo.campaignId, CampaignsInternalRfCloseByClick.adgroup)
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.internalAdOptions.rfCloseByClick).isEqualTo(CampaignV2.ERFCloseByClick.RFCC_ADGROUP.number)
    }

    @Test
    fun internalAdIsMobileReplicationTest() {
        val campaign = activeInternalFreeCampaign(null, null)
            .withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .insertInto(CAMPAIGNS_INTERNAL, CAMPAIGNS_INTERNAL.CID, CAMPAIGNS_INTERNAL.IS_MOBILE)
            .values(campaignInfo.campaignId, 1L)
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.internalAdOptions.isMobile).isTrue
    }

    @Test
    fun rotationGoalIdReplicationTest() {
        val campaign = activeInternalDistribCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .insertInto(CAMPAIGNS_INTERNAL, CAMPAIGNS_INTERNAL.CID, CAMPAIGNS_INTERNAL.ROTATION_GOAL_ID)
            .values(campaignInfo.campaignId, 123456L)
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.internalAdOptions.rotationGoalId).isEqualTo(123456L)
    }

    @Test
    fun rfOptionsReplicationTest() {
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.RF, 3)
            .set(CAMPAIGNS.RF_RESET, 7)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.hasRfOptions()).isTrue
        assertThat(createdCampaign.spec.rfOptions.shows.count).isEqualTo(3)
        assertThat(createdCampaign.spec.rfOptions.shows.period).isEqualTo(604800)
    }

    @Test
    fun rfOptions_MaxRfPeriod_ReplicationTest() {
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.RF, 3)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.hasRfOptions()).isTrue
        assertThat(createdCampaign.spec.rfOptions.shows.count).isEqualTo(3)
        val maxRfSeconds = 90 * 24 * 60 * 60
        assertThat(createdCampaign.spec.rfOptions.shows.period).isEqualTo(maxRfSeconds)
    }

    @Test
    fun campaignAdditionalTargetingReplicationTest() {
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        campAdditionalTargetingsRepository.insertCampAdditionalTargetings(
            shard = campaignInfo.shard,
            listOf(
                CampAdditionalTargeting.Companion.builder()
                    .withCid(campaignInfo!!.campaignId)
                    .withData(
                        """
                    {"and": [{"or": [{"value": "%prak0sep%", "keyword": 806, "operation": 12}]}]}
                """.trimIndent()
                    ),
                CampAdditionalTargeting.Companion.builder()
                    .withCid(campaignInfo.campaignId)
                    .withData(
                        // на этом значение падения быть не должно, оно должно быть пропущено
                        """
                    {"error_expression": 0}
                """.trimIndent()
                    )
            )
        )
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                )
            )
        )

        val soft = SoftAssertions()
        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        soft.assertThat(createdCampaign!!.spec.hasAdditionalTargetingExpression()).isTrue
        val targetingExpression = createdCampaign.spec.additionalTargetingExpression
        soft.assertThat(targetingExpression.andCount).isEqualTo(1)
        val targetingExpressionAnd = targetingExpression.getAnd(0)
        soft.assertThat(targetingExpressionAnd.orCount).isEqualTo(1)
        soft.assertThat(targetingExpressionAnd.getOr(0).keyword)
            .isEqualTo(EKeyword.KW_INTERNAL_TEST_IDS.number)
        soft.assertThat(targetingExpressionAnd.getOr(0).operation)
            .isEqualTo(YabsOperation.EYabsOperation.YO_NOT_LIKE.number)
        soft.assertThat(targetingExpressionAnd.getOr(0).value).isEqualTo("%prak0sep%")
        soft.assertAll()
    }

    @Test
    fun allowedDomainsReplicationTest() {
        val allowedDomains = listOf("games.mail.ru", "onlajnigry.net", "onlineigry.net", "store.my.games")
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMP_OPTIONS)
            .set(CAMP_OPTIONS.ALLOWED_DOMAINS, "[" + allowedDomains.map { "\"" + it + "\"" }.joinToString(",") + "]")
            .where(CAMP_OPTIONS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.allowedDomainsList).isEqualTo(allowedDomains)
    }

    @Test
    fun widgetPartnerIdReplicationTest() {
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)
        val widgetPartnerId = 8217L
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .insertInto(
                WIDGET_PARTNER_CAMPAIGNS,
                WIDGET_PARTNER_CAMPAIGNS.CID,
                WIDGET_PARTNER_CAMPAIGNS.WIDGET_PARTNER_ID
            )
            .values(campaignInfo.campaignId, widgetPartnerId)
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.widgetPartnerId).isEqualTo(widgetPartnerId)
    }

    @Suppress("unused")
    private fun impressionStandardTimeValues() = arrayOf(
        arrayOf(2000L, CampaignV2.EImpressionStandardType.IST_YANDEX),
        arrayOf(1000L, CampaignV2.EImpressionStandardType.IST_MRC),
        arrayOf(null, CampaignV2.EImpressionStandardType.IST_MRC),
        arrayOf(123L, CampaignV2.EImpressionStandardType.IST_UNKNOWN),
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("impressionStandardTimeValues")
    fun impressionStandardTypeReplicationTest(
        impressionStandardTime: Long,
        grutImpressionStandardType: CampaignV2.EImpressionStandardType
    ) {
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMP_OPTIONS)
            .set(CAMP_OPTIONS.IMPRESSION_STANDARD_TIME, impressionStandardTime)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.widgetPartnerId).isEqualTo(grutImpressionStandardType)
    }

    @Test
    fun eshowsVideoTypeReplicationTest() {
        val campaign = activeCpmBannerCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMP_OPTIONS)
            .set(CAMP_OPTIONS.ESHOWS_VIDEO_TYPE, "completes")
            .where(CAMP_OPTIONS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.eshowVideoType).isEqualTo(CampaignV2.EShowVideoType.EST_COMPLETES.number)
    }

    @Test
    fun disabledDomainsAndDisabledSSPReplicationTest() {
        val disabledDomains = setOf("domain.ru", "zzz.com")
        val disabledSSPs = listOf("MoPub", "MobFox", "Smaato")

        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)
            .withDisabledDomains(disabledDomains)
            .withDisabledSsp(disabledSSPs)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )
        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        val spec = createdCampaign!!.spec
        assertThat(spec.forbiddenDomainsList).containsOnly(*disabledDomains.toTypedArray())
        assertThat(spec.forbiddenSspList).containsOnly(*disabledSSPs.toTypedArray())
    }

    @Test
    fun skadNetworkReplicationTest() {
        val campaign = activeMobileContentCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        val bundleId = "test_bundle"
        val slot = 4
        iosSkAdNetworkSlotRepository.addSlot(
            dslContext = dslContextProvider.ppcdict(),
            slot = SkAdNetworkSlot(bundleId, campaignInfo.campaignId, slot)
        )

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.skadNetwork.bundleId).isEqualTo(bundleId)
        assertThat(createdCampaign.spec.skadNetwork.slot).isEqualTo(slot)
    }

    @Suppress("unused")
    private fun phraseQualityClusteringValues() = arrayOf(
        arrayOf(true, CampaignV2.EPhraseQualityClustering.PQC_BY_ORDER),
        arrayOf(false, CampaignV2.EPhraseQualityClustering.PQC_UNKNOWN),
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("phraseQualityClusteringValues")
    fun phraseQualityClusteringReplicationTest(
        isOrderPhraseLengthPrecedenceEnabled: Boolean,
        phraseQualityClustering: CampaignV2.EPhraseQualityClustering
    ) {
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)

        val opts = if (isOrderPhraseLengthPrecedenceEnabled) "is_order_phrase_length_precedence_enabled" else ""
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.OPTS, opts)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.phraseQualityClustering).isEqualTo(phraseQualityClustering)
    }

    @Test
    fun auctionPriorityReplicationTest() {
        val campaign = activeCpmPriceCampaign(null, null)
            .withOrderId(0L)

        val auctionPriority = 20L
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .insertInto(
                CAMPAIGNS_CPM_PRICE,
                CAMPAIGNS_CPM_PRICE.CID,
                CAMPAIGNS_CPM_PRICE.PACKAGE_ID,
                CAMPAIGNS_CPM_PRICE.TARGETINGS_SNAPSHOT,
                CAMPAIGNS_CPM_PRICE.ORDER_VOLUME,
                CAMPAIGNS_CPM_PRICE.AUCTION_PRIORITY,
            )
            .values(
                campaignInfo.campaignId,
                0L, // чтобы не создавать дополнительно объект пакета
                "{\"geoType\": 3, \"behaviors\": [], \"viewTypes\": [\"DESKTOP\"], \"geoExpanded\": [225], \"allowExpandedDesktopCreative\": false}",
                1,
                auctionPriority
            )
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.auctionPriority).isEqualTo(auctionPriority)
    }

    @Test
    fun pageIdsReplicationTest() {
        val campaign = activeCpmBannerCampaign(null, null)
            .withOrderId(0L)
            .withAllowedPageIds(listOf(42L, 4004L))
            .withDisAllowedPageIds(listOf(123L, 654L))

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.allowedPageIdsList).isEqualTo(listOf(42L, 4004L))
        assertThat(createdCampaign.spec.forbiddenPageIdsList).isEqualTo(listOf(123L, 654L))
    }

    @Test
    fun trackingParamsReplicationTest() {
        val campaign = activeTextCampaign(null, null)
            .withOrderId(0L)

        val hrefParams = "param1=value1&param2=value2";
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMP_OPTIONS)
            .set(CAMP_OPTIONS.HREF_PARAMS, hrefParams)
            .where(CAMP_OPTIONS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.trackingParams).isEqualTo(hrefParams)
    }

    @Test
    fun forbiddenVideoPlacementsReplicationTest() {
        val forbiddenVideoPlacements = listOf("vk.com", "music.yandex.ru")
        val campaign = activeCpmBannerCampaign(null, null)
            .withOrderId(0L)
            .withDisabledVideoPlacements(forbiddenVideoPlacements)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.forbiddenVideoPlacementsList).isEqualTo(forbiddenVideoPlacements)
    }

    @Test
    fun forbiddenIpsReplicationTest() {
        val forbiddenIps = listOf("1.2.3.4", "5.6.7.8")
        val campaign = activeCpmBannerCampaign(null, null)
            .withOrderId(0L)

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.DISABLED_IPS, forbiddenIps.joinToString(","))
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.forbiddenIpsList).isEqualTo(forbiddenIps)
    }

    //TODO: написать тест в котором мы пытаемся обновить кампанию у которая уже есть в груте но с другим ордер айди

    private fun updateCampaignName(shard: Int, campaignId: Long, name: String) {
        dslContextProvider.ppc(shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.NAME, name)
            .where(CAMPAIGNS.CID.eq(campaignId))
            .execute()
    }
}
