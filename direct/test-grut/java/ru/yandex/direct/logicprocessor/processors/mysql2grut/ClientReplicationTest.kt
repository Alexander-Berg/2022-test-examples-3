package ru.yandex.direct.logicprocessor.processors.mysql2grut

import com.google.common.truth.extensions.proto.FieldScopes
import com.google.common.truth.extensions.proto.ProtoTruth
import org.apache.commons.lang.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.additionaltargetings.model.ClientAdditionalTargeting
import ru.yandex.direct.core.entity.additionaltargetings.repository.ClientAdditionalTargetingsRepository
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.campaign.service.BillingAggregateService
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.client.model.TinType
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProduct
import ru.yandex.direct.core.entity.internalads.repository.InternalAdsProductRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateRandomIdLong
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestClients.defaultClient
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbschema.ppc.Tables.CLIENTS_OPTIONS
import ru.yandex.direct.dbschema.ppc.Tables.CLIENT_NDS
import ru.yandex.direct.dbschema.ppc.enums.ClientsOptionsStatusbalancebanned
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.utils.DateTimeUtils
import ru.yandex.grut.auxiliary.proto.YabsOperation
import ru.yandex.grut.objects.proto.Client.EProductType
import ru.yandex.grut.objects.proto.Client.TBillingAggregate
import ru.yandex.grut.objects.proto.Client.TClientFlags
import ru.yandex.grut.objects.proto.Client.TClientNds
import ru.yandex.grut.objects.proto.Client.TClientSpec
import ru.yandex.grut.objects.proto.client.Schema.TClient
import ru.yandex.grut.objects.proto.client.Schema.TClientMeta

import ru.yandex.yabs.server.proto.keywords.EKeyword

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class ClientReplicationTest {

    val TEST_TIN1 = "0123456789"
    val TEST_TIN2 = "9876543210"

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var billingAggregateService: BillingAggregateService

    private lateinit var existingInMySqlClient: ClientInfo
    private lateinit var notExistingInMySqlClient: ClientInfo

    @Autowired
    private lateinit var campaignsRepository: TestCampaignRepository

    @Autowired
    private lateinit var internalAdsProductRepository: InternalAdsProductRepository

    @Autowired
    private lateinit var clientAdditionalTargetingsRepository: ClientAdditionalTargetingsRepository

    @BeforeEach
    private fun setup() {
        existingInMySqlClient = steps.clientSteps().createDefaultClient()

        notExistingInMySqlClient = ClientInfo().withClient(Client().withClientId(generateRandomIdLong()))

        processor.withShard(existingInMySqlClient.shard)
    }

    @AfterEach
    private fun tearDown() {
        val clientIds = listOf(
            existingInMySqlClient.clientId!!.asLong(),
        )
        replicationApiService.clientGrutDao.deleteObjects(clientIds)
    }

    private fun createInGrutWithRandomSpec(clientId: ClientId) {
        //client spec in grut differs from mysql before processing
        grutSteps.createClient(clientId, RandomStringUtils.randomAlphabetic(10), generateRandomIdLong())
    }

    private fun createInGrutWithCreationTime(clientId: ClientId, creationTime: Instant) {
        //client spec in grut differs from mysql before processing
        grutSteps.createClient(
            clientId,
            RandomStringUtils.randomAlphabetic(10),
            generateRandomIdLong(),
            creationTime.toEpochMilli()
        )
    }

    @Test
    fun createSingleClientTest() {
        //arrange
        val creationDate = LocalDateTime.parse("2021-10-20T15:55:01")
        val clientInfo = steps.clientSteps().createClient(
            Client()
                .withName("randomName")
                .withCreateDate(creationDate)
                .withAllowCreateScampBySubclient(false)
                .withRole(RbacRole.CLIENT)
                .withTin(TEST_TIN1)
                .withTinType(TinType.LEGAL)
                .withCountryRegionId(1L)
        )
        assertThat(clientInfo.chiefUserInfo!!.chiefUid).isNotEqualTo(0L)
        val processorArgs = listOf(Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong()))

        //act
        processor.process(processorArgs)
        //assert
        val clientFromGrut = replicationApiService.clientGrutDao.getClient(clientInfo.clientId!!.asLong())
        val expectedClient = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(clientInfo.clientId!!.asLong())
                // число это creationDate по UTC+3 (MSK) таймзоне
                // object_api принимает дату в миллисекундах, а отдает в микросекундах  YTORM-275
                .setCreationTime(1634734501000000)
                .build()
            spec = TClientSpec.newBuilder()
                .setName("randomName")
                .setChiefUid(clientInfo.chiefUserInfo!!.chiefUid)
                .setTin(TEST_TIN1)
                .setTinType(TClientSpec.ETinType.TT_LEGAL.number)
                .build()
        }.build()
        ProtoTruth.assertThat(clientFromGrut)
            .withPartialScope(FieldScopes.fromSetFields(expectedClient))
            .isEqualTo(expectedClient)
    }

    @Test
    fun updateSingleClientTest() {
        val creationDate = LocalDateTime.parse("2021-10-20T15:55:01")
        val client = steps.clientSteps().createClient(
            Client()
                .withName("randomName")
                .withCreateDate(creationDate)
                .withAllowCreateScampBySubclient(false)
                .withRole(RbacRole.CLIENT)
                .withTin(TEST_TIN2)
                .withTinType(TinType.PHYSICAL)
                .withCountryRegionId(1L)
        )
        assertThat(client.chiefUserInfo!!.chiefUid).isNotEqualTo(0L)
        val creationTimeInstant = DateTimeUtils.moscowDateTimeToInstant(creationDate)
        //arrange
        val clientId = client.clientId!!
        //client spec in grut differs from mysql state
        createInGrutWithCreationTime(clientId, creationTimeInstant)

        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(clientId = clientId.asLong()))
        //act
        processor.process(processorArgs)
        //assert
        val clientFromGrut = replicationApiService.clientGrutDao.getClient(clientId.asLong())

        val expectedClient = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(clientId.asLong())
                // object_api принимает дату в миллисекундах, а отдает в микросекундах  YTORM-275
                // не должно было поменяться при апдейте
                .setCreationTime(1634734501000000)
                .build()
            spec = TClientSpec.newBuilder()
                .setName("randomName")
                .setChiefUid(client.chiefUserInfo!!.chiefUid)
                .setTin(TEST_TIN2)
                .setTinType(TClientSpec.ETinType.TT_PHYSICAL.number)
                .build()
        }.build()
        ProtoTruth.assertThat(clientFromGrut)
            .withPartialScope(FieldScopes.fromSetFields(expectedClient))
            .isEqualTo(expectedClient)
    }

    @Test
    fun deleteSingleClientTest() {
        //arrange
        createInGrutWithRandomSpec(clientId = notExistingInMySqlClient.clientId!!)
        val processorArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                clientId = notExistingInMySqlClient.clientId!!.asLong(),
                isDeleted = true
            )
        )
        //act
        processor.process(processorArgs)
        //assert
        val deletedClient =
            replicationApiService.clientGrutDao.getClient(notExistingInMySqlClient.clientId!!.asLong())
        assertThat(deletedClient).isNull()
    }

    @Test
    fun dontDeleteExistingClientTest() {
        //case when client was recreated before delete event came to processor
        //arrange
        createInGrutWithRandomSpec(existingInMySqlClient.clientId!!)
        val processorArgs = mutableListOf(
            Mysql2GrutReplicationObject(
                clientId = existingInMySqlClient.clientId!!.asLong(),
                isDeleted = true
            )
        )

        //act
        processor.process(processorArgs)
        //assert
        val notDeletedClient =
            replicationApiService.clientGrutDao.getClient(existingInMySqlClient.clientId!!.asLong())
        assertThat(notDeletedClient).isNotNull
    }

    @Test
    fun selectClientsTest() {
        //arrange
        val n = 3
        val clientsToUpsertInGrut = mutableListOf<ClientGrutModel>()
        val chiefUid = 123L
        for (i in 1..n) {
            val client = steps.clientSteps().createDefaultClient()
            client.client!!.chiefUid = chiefUid
            clientsToUpsertInGrut.add(ClientGrutModel(client = client.client!!, emptyList(), emptyList(), null))
        }
        replicationApiService.clientGrutDao.createOrUpdateClients(clientsToUpsertInGrut)
        //act
        val clientsReturned = replicationApiService.clientGrutDao.selectClients(
            "[/spec/chief_uid] = 123u",
            allowFullScan = true
        )
        //assert
        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(clientsReturned.size).isEqualTo(clientsToUpsertInGrut.size)
            for (cl in clientsReturned) {
                softly.assertThat(cl.spec.chiefUid).isEqualTo(chiefUid)
            }
        }
    }

    @Test
    fun bunchOfCreateUpdateDeleteTest() {
        val softly = SoftAssertions()
        //arrange
        val n = 2
        //create
        val clientsToUpsertInGrut = mutableListOf<ClientInfo>()
        for (i in 1..n) {
            val client = steps.clientSteps().createDefaultClient()
            clientsToUpsertInGrut.add(client)
        }
        //update
        for (i in 1..n) {
            val client = steps.clientSteps().createDefaultClient()
            createInGrutWithRandomSpec(clientId = client.clientId!!)
            clientsToUpsertInGrut.add(client)
        }
        //delete
        val clientsIdsToDeleteInGrut = mutableListOf<ClientId>()
        for (i in 1..n) {
            val clientId = ClientId.fromLong(generateRandomIdLong())
            createInGrutWithRandomSpec(clientId)
            clientsIdsToDeleteInGrut.add(clientId)
        }
        val processorArgs = clientsToUpsertInGrut
            .map { Mysql2GrutReplicationObject(clientId = it.clientId!!.asLong()) }
            .toList()
            .plus(clientsIdsToDeleteInGrut
                .map { Mysql2GrutReplicationObject(clientId = it.asLong(), isDeleted = true) })
        //act
        processor.process(processorArgs)
        //assert
        val clientIds = clientsToUpsertInGrut.map { it.clientId!!.asLong() }
        val grutClientsUpserted = replicationApiService.clientGrutDao.getClients(clientIds)

        val clientIdsToClient = clientsToUpsertInGrut.map { it.clientId!!.asLong() to it }.toMap()
        softly.assertThat(grutClientsUpserted.size).isEqualTo(clientsToUpsertInGrut.size)
        //assert all clients upserted to grut
        for (grutClient in grutClientsUpserted) {
            val clientId = grutClient.meta.id
            softly.assertThat(grutClient.spec.name).isEqualTo(clientIdsToClient.get(clientId)!!.client!!.name)
            softly.assertThat(grutClient.spec.chiefUid).isEqualTo(clientIdsToClient.get(clientId)!!.client!!.chiefUid)
        }

        //check all clients deleted from grut
        val grutClientsDeleted =
            replicationApiService.clientGrutDao.getClients(clientsIdsToDeleteInGrut.map { it.asLong() })
        softly.assertThat(grutClientsDeleted).isEmpty()
        softly.assertAll()
    }

    @Test
    fun ndsHistoryReplicationTest() {
        //arrange
        val client = steps.clientSteps().createDefaultClient()
        dslContextProvider.ppc(client.shard).deleteFrom(CLIENT_NDS)
            .where(CLIENT_NDS.CLIENT_ID.eq(client.clientId!!.asLong())).execute()
        dslContextProvider.ppc(client.shard)
            .insertInto(CLIENT_NDS, CLIENT_NDS.CLIENT_ID, CLIENT_NDS.DATE_FROM, CLIENT_NDS.DATE_TO, CLIENT_NDS.NDS)
            .values(
                client.clientId!!.asLong(),
                LocalDate.of(2003, 1, 1),
                LocalDate.of(2003, 12, 31),
                BigDecimal.valueOf(20)
            )
            .values(
                client.clientId!!.asLong(),
                LocalDate.of(2004, 1, 1),
                LocalDate.of(2018, 12, 31),
                BigDecimal.valueOf(18)
            )
            .values(
                client.clientId!!.asLong(),
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2038, 1, 19),
                BigDecimal.valueOf(20)
            )
            .execute()
        val processorArgs =
            mutableListOf(Mysql2GrutReplicationObject(clientId = client.clientId!!.asLong()))

        //act
        processor.process(processorArgs)
        //assert
        val clientFromGrut = replicationApiService.clientGrutDao.getClient(client.clientId!!.asLong())
        val expectedNdsHistory = listOf(
            TClientNds.newBuilder().apply {
                dateFrom = 1041368400
                dateTo = 1072818000
                nds = 20000000
            }.build(),
            TClientNds.newBuilder().apply {
                dateFrom = 1072904400
                dateTo = 1546203600
                nds = 18000000
            }.build(),
            TClientNds.newBuilder().apply {
                dateFrom = 1546290000
                dateTo = 2147461200
                nds = 20000000
            }.build()
        )
        val expectedClient = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(client.clientId!!.asLong())
                .build()
            spec = TClientSpec.newBuilder()
                .addAllClientNdsHistory(expectedNdsHistory)
                .build()
        }.build()
        ProtoTruth.assertThat(clientFromGrut)
            .withPartialScope(FieldScopes.fromSetFields(expectedClient))
            .isEqualTo(expectedClient)
    }

    @Test
    fun noNdsHistoryReplicationTest() {
        //arrange
        val client = steps.clientSteps().createDefaultClient()
        dslContextProvider.ppc(client.shard).deleteFrom(CLIENT_NDS)
            .where(CLIENT_NDS.CLIENT_ID.eq(client.clientId!!.asLong())).execute()
        val processorArgs =
            mutableListOf(Mysql2GrutReplicationObject(clientId = client.clientId!!.asLong()))

        //act
        processor.process(processorArgs)
        //assert
        val clientFromGrut = replicationApiService.clientGrutDao.getClient(client.clientId!!.asLong())
        val expectedClient = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(client.clientId!!.asLong())
                .build()
            spec = TClientSpec.getDefaultInstance()
        }.build()
        ProtoTruth.assertThat(clientFromGrut)
            .withPartialScope(
                FieldScopes.fromSetFields(expectedClient)
                    .allowingFieldDescriptors(TClientSpec.getDescriptor().findFieldByName("client_nds_history"))
            )
            .isEqualTo(expectedClient)
    }

    @Test
    fun flagsReplication_AllFlagsTrueTest() {
        //arrange
        val client = steps.clientSteps().createClient(
            defaultClient(RbacRole.CLIENT)
                .withNonResident(true)
                .withFaviconBlocked(true)
                .withHideMarketRating(true)
                .withIsBusinessUnit(true)
                .withSocialAdvertising(true)
                .withAsSoonAsPossible(true)
        )
        val processorArgs =
            mutableListOf(Mysql2GrutReplicationObject(clientId = client.clientId!!.asLong()))

        //act
        processor.process(processorArgs)
        //assert
        val clientFromGrut = replicationApiService.clientGrutDao.getClient(client.clientId!!.asLong())
        val expectedClient = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(client.clientId!!.asLong())
                .build()
            spec = TClientSpec.newBuilder()
                .setFlags(
                    TClientFlags.newBuilder()
                        .setNonResident(true)
                        .setIsFaviconBlocked(true)
                        .setHideMarketRating(true)
                        .setBusinessUnit(true)
                        .setSocialAdvertising(true)
                        .setAsSoonAsPossible(true)
                        .build()
                )
                .build()
        }.build()
        ProtoTruth.assertThat(clientFromGrut)
            .withPartialScope(FieldScopes.fromSetFields(expectedClient))
            .isEqualTo(expectedClient)
    }

    @Test
    fun flagsReplication_AllFlagsFalseTest() {
        //arrange
        val client = steps.clientSteps().createClient(
            defaultClient(RbacRole.CLIENT)
                .withNonResident(false)
                .withFaviconBlocked(false)
                .withHideMarketRating(false)
                .withIsBusinessUnit(false)
                .withSocialAdvertising(false)
                .withAsSoonAsPossible(false)
        )
        val processorArgs =
            mutableListOf(Mysql2GrutReplicationObject(clientId = client.clientId!!.asLong()))

        //act
        processor.process(processorArgs)
        //assert
        val clientFromGrut = replicationApiService.clientGrutDao.getClient(client.clientId!!.asLong())
        val expectedClient = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(client.clientId!!.asLong())
                .build()
            spec = TClientSpec.newBuilder()
                .setFlags(
                    TClientFlags.newBuilder()
                        .setNonResident(false)
                        .setIsFaviconBlocked(false)
                        .setHideMarketRating(false)
                        .setBusinessUnit(false)
                        .setSocialAdvertising(false)
                        .setAsSoonAsPossible(false)
                        .build()
                )
                .build()
        }.build()
        ProtoTruth.assertThat(clientFromGrut)
            .withPartialScope(FieldScopes.fromSetFields(expectedClient))
            .isEqualTo(expectedClient)
        assertThat(clientFromGrut).isNotNull
    }

    // проверяем только айдишники (агрегата и кошелька) для честно созданных агрегатов
    // прокидывание всех полей в сборе проверим отдельным тестом на синтетических данных
    @Test
    fun createdBillingAggregatesIdsReplicationTest() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val walletInfo = steps.campaignSteps().createWalletCampaign(clientInfo).campaign

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong()),
                Mysql2GrutReplicationObject(campaignId = walletInfo.id),
            )
        )

        val walletGrutId =
            replicationApiService.campaignGrutDao.getCampaignIdsByDirectIds(listOf(walletInfo.id))[walletInfo.id]!!

        val cpmCampaign = steps.campaignSteps().createCampaignUnderWalletByCampaignType(
            CampaignType.CPM_BANNER,
            clientInfo,
            walletInfo.id,
            BigDecimal.ZERO
        ).campaign
        // нужна только, чтобы прокинуть тип в код создания агрегатов
        val camp = CpmBannerCampaign()
            .withId(cpmCampaign.id)
            .withWalletId(cpmCampaign.walletId)
            .withType(CampaignType.CPM_BANNER)

        val createdBillingAggregateIds = billingAggregateService.createBillingAggregates(
            clientInfo.clientId!!, clientInfo.uid, clientInfo.uid,
            listOf(camp), walletInfo.id
        )
        val expectedAggregates = createdBillingAggregateIds.map {
            TBillingAggregate.newBuilder()
                .setBillingAggregateCampaignId(it)
                .setWalletCampaignId(walletGrutId)
                .build()
        }.toList()

        processor.process(listOf(Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong())))

        val clientFromGrut = replicationApiService.clientGrutDao.getClient(clientInfo.clientId!!.asLong())
        val expectedClient = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(clientInfo.clientId!!.asLong())
                .build()
            spec = TClientSpec.newBuilder()
                .addAllBillingAggregates(expectedAggregates)
                .build()
        }.build()
        ProtoTruth.assertThat(clientFromGrut)
            .ignoringRepeatedFieldOrder()
            .withPartialScope(FieldScopes.fromSetFields(expectedClient))
            .isEqualTo(expectedClient)
    }

    @Test
    fun billingAggregatesReplicationTest() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val walletInfo = steps.campaignSteps().createWalletCampaign(clientInfo).campaign

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong()),
                Mysql2GrutReplicationObject(campaignId = walletInfo.id),
            )
        )

        val walletGrutId =
            replicationApiService.campaignGrutDao.getCampaignIdsByDirectIds(listOf(walletInfo.id))[walletInfo.id]!!

        // cpm_video
        val balanceInfo1 = TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB).withWalletCid(walletInfo.id)
            .withProductId(509619)
        // cpm_banner
        val balanceInfo2 = TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB).withWalletCid(walletInfo.id)
            .withProductId(508587)
        val ba1 = steps.campaignSteps().createBillingAggregate(clientInfo, balanceInfo1)
        val ba2 = steps.campaignSteps().createBillingAggregate(clientInfo, balanceInfo2)

        processor.process(listOf(Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong())))

        val clientFromGrut = replicationApiService.clientGrutDao.getClient(clientInfo.clientId!!.asLong())
        val expectedClient = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(clientInfo.clientId!!.asLong())
                .build()
            spec = TClientSpec.newBuilder()
                .addBillingAggregates(
                    TBillingAggregate.newBuilder()
                        .setBillingAggregateCampaignId(ba1.campaignId)
                        .setWalletCampaignId(walletGrutId)
                        .setProductId(509619)
                        .setProductType(EProductType.PT_CPM_VIDEO.number)
                )
                .addBillingAggregates(
                    TBillingAggregate.newBuilder()
                        .setBillingAggregateCampaignId(ba2.campaignId)
                        .setWalletCampaignId(walletGrutId)
                        .setProductId(508587)
                        .setProductType(EProductType.PT_CPM_BANNER.number)
                )
                .build()
        }.build()
        ProtoTruth.assertThat(clientFromGrut)
            .withPartialScope(FieldScopes.fromSetFields(expectedClient))
            .isEqualTo(expectedClient)
    }

    @Test
    fun billingAggregatesRemovalReplicationTest() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val walletInfo = steps.campaignSteps().createWalletCampaign(clientInfo).campaign
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong()),
                Mysql2GrutReplicationObject(campaignId = walletInfo.id),
            )
        )

        val balanceInfo1 = TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB).withWalletCid(walletInfo.id)
            .withProductId(509619)
        val balanceInfo2 = TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB).withWalletCid(walletInfo.id)
            .withProductId(508587)
        val ba1 = steps.campaignSteps().createBillingAggregate(clientInfo, balanceInfo1)
        val ba2 = steps.campaignSteps().createBillingAggregate(clientInfo, balanceInfo2)
        processor.process(listOf(Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong())))

        val preparedClientFromGrut = replicationApiService.clientGrutDao.getClient(clientInfo.clientId!!.asLong())
        assertThat(preparedClientFromGrut!!.spec.billingAggregatesCount)
            .describedAs("число биллинговых аггрегатов в груте перед удалением")
            .isGreaterThan(0)

        campaignsRepository.deleteCampaign(1, ba1.campaignId)
        campaignsRepository.deleteCampaign(1, ba2.campaignId)
        processor.process(listOf(Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong())))

        val clientFromGrut = replicationApiService.clientGrutDao.getClient(clientInfo.clientId!!.asLong())
        val expectedClient = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(clientInfo.clientId!!.asLong())
                .build()
            spec = TClientSpec.getDefaultInstance()
        }.build()
        ProtoTruth.assertThat(clientFromGrut)
            .withPartialScope(
                FieldScopes.fromSetFields(expectedClient)
                    .allowingFieldDescriptors(TClientSpec.getDescriptor().findFieldByName("billing_aggregates"))
            )
            .isEqualTo(expectedClient)
    }

    @Test
    fun noBillingAggregatesReplicationTest() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        steps.campaignSteps().createWalletCampaign(clientInfo)

        processor.process(listOf(Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong())))

        val clientFromGrut = replicationApiService.clientGrutDao.getClient(clientInfo.clientId!!.asLong())
        val expectedClient = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(clientInfo.clientId!!.asLong())
                .build()
            spec = TClientSpec.getDefaultInstance()
        }.build()
        ProtoTruth.assertThat(clientFromGrut)
            .withPartialScope(
                FieldScopes.fromSetFields(expectedClient)
                    .allowingFieldDescriptors(TClientSpec.getDescriptor().findFieldByName("billing_aggregates"))
            )
            .isEqualTo(expectedClient)
    }

    // проверяем, что если кошелька в груте нет, то биллинговые аггрегаты не реплицируются
    @Test
    fun billingAggregatesNotReplicatedWithoutWalletTest() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val walletInfo = steps.campaignSteps().createWalletCampaign(clientInfo).campaign

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong()),
            )
        )

        steps.campaignSteps().createBillingAggregate(
            clientInfo,
            TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB).withWalletCid(walletInfo.id).withProductId(509619)
        )
        steps.campaignSteps().createBillingAggregate(
            clientInfo,
            TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB).withWalletCid(walletInfo.id).withProductId(508587)
        )

        processor.process(listOf(Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong())))

        val clientFromGrut = replicationApiService.clientGrutDao.getClient(clientInfo.clientId!!.asLong())
        val expectedClient = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(clientInfo.clientId!!.asLong())
                .build()
            spec = TClientSpec.getDefaultInstance()
        }.build()
        ProtoTruth.assertThat(clientFromGrut)
            .withPartialScope(
                FieldScopes.fromSetFields(expectedClient)
                    .allowingFieldDescriptors(TClientSpec.getDescriptor().findFieldByName("billing_aggregates"))
            )
            .isEqualTo(expectedClient)
    }

    @Test
    fun internalAdDistributionTagTest() {
        //arrange
        val clientWithTag = steps.clientSteps().createClient(defaultClient(RbacRole.CLIENT))
        val clientWithNoTag = steps.clientSteps().createClient(defaultClient(RbacRole.CLIENT))

        val clientsList = listOf(clientWithTag, clientWithNoTag)
        val clientsIdsList = clientsList.map { it.clientId!!.asLong() }

        var productName = ""
        for (i in 0..100) {
            val intProductName = "test_name_" + Random.nextInt()
            val productDescription = "Product for GRUT replication test. Should never hit any persistent database"
            try {
                internalAdsProductRepository.createProduct(
                    clientWithTag.shard,
                    InternalAdsProduct().withClientId(clientWithTag.clientId!!)
                        .withName(intProductName)
                        .withDescription(productDescription)
                        .withOptions(setOf())
                )
                productName = intProductName
                break
            } catch (e: IllegalStateException) {
            }
        }
        if (productName == "") {
            throw IllegalStateException("Failed to create a new product")
        }

        val processorArgs = clientsIdsList.map { Mysql2GrutReplicationObject(clientId = it) }

        //act
        processor.process(processorArgs)
        //assert
        val clientsFromGrut = replicationApiService.clientGrutDao.getClients(clientsIdsList).associateBy { it.meta.id }
        val clientsFromGrutWithTag = clientsFromGrut[clientWithTag.clientId!!.asLong()]
        val clientsFromGrutWithNoTag = clientsFromGrut[clientWithNoTag.clientId!!.asLong()]

        val expectedClientWithTag = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(clientWithTag.clientId!!.asLong())
                .build()
            spec = TClientSpec.newBuilder()
                .setInternalAdDistributionTag(productName)
                .build()
        }.build()

        val expectedClientWithNoTag = TClient.newBuilder().apply {
            meta = TClientMeta.newBuilder()
                .setId(clientWithNoTag.clientId!!.asLong())
                .build()
            spec = TClientSpec.newBuilder().build()
        }.build()

        ProtoTruth.assertThat(clientsFromGrutWithTag)
            .withPartialScope(FieldScopes.fromSetFields(expectedClientWithTag))
            .isEqualTo(expectedClientWithTag)

        ProtoTruth.assertThat(clientsFromGrutWithNoTag)
            .withPartialScope(FieldScopes.fromSetFields(expectedClientWithTag)) // Field scope with product_id set
            .isEqualTo(expectedClientWithNoTag)
    }

    @Test
    fun clientOverdraftReplicationTest() {
        val clientInfo = steps.clientSteps().createDefaultClient()

        dslContextProvider.ppc(clientInfo.shard)
            .update(CLIENTS_OPTIONS)
            .set(CLIENTS_OPTIONS.STATUS_BALANCE_BANNED, ClientsOptionsStatusbalancebanned.Yes)
            .set(CLIENTS_OPTIONS.DEBT, BigDecimal.valueOf(356.89))
            .set(CLIENTS_OPTIONS.AUTO_OVERDRAFT_LIM, BigDecimal.valueOf(3000.00))
            .set(CLIENTS_OPTIONS.OVERDRAFT_LIM, BigDecimal.valueOf(5679.98))
            .where(CLIENTS_OPTIONS.CLIENT_ID.eq(clientInfo.clientId!!.asLong()))
            .execute()

        processor.process(listOf(Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong())))

        val soft = SoftAssertions()
        val clientFromGrut = replicationApiService.clientGrutDao.getClient(clientInfo.clientId!!.asLong())
        soft.assertThat(clientFromGrut!!.spec.autoOverdraftLimit).isEqualTo(3000000000)
        soft.assertThat(clientFromGrut.spec.hasBalanceOptions()).isTrue
        soft.assertThat(clientFromGrut.spec.balanceOptions.overdraftLimit).isEqualTo(5679980000L)
        soft.assertThat(clientFromGrut.spec.balanceOptions.debt).isEqualTo(356890000L)
        soft.assertThat(clientFromGrut.spec.balanceOptions.isBannedInBalance).isEqualTo(true)
        soft.assertAll()
    }

    @Test
    fun clientOverdraftDefaultValuesReplicationTest() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        processor.process(listOf(Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong())))

        val soft = SoftAssertions()
        val clientFromGrut = replicationApiService.clientGrutDao.getClient(clientInfo.clientId!!.asLong())
        soft.assertThat(clientFromGrut!!.spec.autoOverdraftLimit).isEqualTo(0)
        soft.assertThat(clientFromGrut.spec.hasBalanceOptions()).isTrue
        soft.assertThat(clientFromGrut.spec.balanceOptions.overdraftLimit).isEqualTo(0)
        soft.assertThat(clientFromGrut.spec.balanceOptions.debt).isEqualTo(0)
        soft.assertThat(clientFromGrut.spec.balanceOptions.isBannedInBalance).isEqualTo(false)
        soft.assertAll()
    }

    @Test
    fun clientAdditionalTargetingReplicationTest() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        clientAdditionalTargetingsRepository.insertClientAdditionalTargetings(
            shard = clientInfo.shard,
            listOf(
                ClientAdditionalTargeting.Companion.builder()
                    .withClientId(clientInfo.clientId!!.asLong())
                    .withData(
                        """
                    {"and": [{"or": [{"value": "%prak0sep%", "keyword": 806, "operation": 12}]}]}
                """.trimIndent()
                    ),
                ClientAdditionalTargeting.Companion.builder()
                    .withClientId(clientInfo.clientId!!.asLong())
                    .withData(
                        // на этом значение падения быть не должно, оно должно быть пропущено
                        """
                    {"error_expression": 0}
                """.trimIndent()
                    )
            )
        )
        processor.process(listOf(Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong())))

        val soft = SoftAssertions()
        val clientFromGrut = replicationApiService.clientGrutDao.getClient(clientInfo.clientId!!.asLong())
        soft.assertThat(clientFromGrut!!.spec.hasAdditionalTargetingExpression()).isTrue
        val targetingExpression = clientFromGrut.spec.additionalTargetingExpression
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
    fun defaultAllowedDomainsReplicationTest() {
        val defaultAllowedDomains =
            listOf("m.znatoki.yandex.ru", "mail.yandex.com.tr", "mail.yandex.ru", "maps.yandex.ru")
        val clientInfo = steps.clientSteps().createDefaultClient()
        dslContextProvider.ppc(clientInfo.shard)
            .update(CLIENTS_OPTIONS)
            .set(
                CLIENTS_OPTIONS.DEFAULT_ALLOWED_DOMAINS,
                "[" + defaultAllowedDomains.joinToString(",") { "\"" + it + "\"" } + "]")
            .where(CLIENTS_OPTIONS.CLIENT_ID.eq(clientInfo.clientId!!.asLong()))
            .execute()
        processor.process(listOf(Mysql2GrutReplicationObject(clientId = clientInfo.clientId!!.asLong())))

        val clientFromGrut = replicationApiService.clientGrutDao.getClient(clientInfo.clientId!!.asLong())
        assertThat(clientFromGrut!!.spec.defaultAllowedDomainsList).isEqualTo(defaultAllowedDomains)
    }
}
