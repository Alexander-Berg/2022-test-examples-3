package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.vcard.repository.VcardMappings.phoneToDb
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository
import ru.yandex.direct.core.entity.vcard.repository.internal.AddressesRepository
import ru.yandex.direct.core.entity.vcard.repository.internal.MapsRepository
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.api.utils.bigDecimalToGrut
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestVcards.fullVcard
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS
import ru.yandex.direct.dbschema.ppc.Tables.VCARDS
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.mysql2grut.enummappers.VCardEnumMappers.Companion.precisionTypeToGrut
import ru.yandex.qatools.allure.annotations.Description

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class VCardReplicationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var vcardRepository: VcardRepository

    @Autowired
    private lateinit var addressesRepository: AddressesRepository

    @Autowired
    private lateinit var mapsRepository: MapsRepository

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

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
    fun createVCardTest() {
        val vcardInfo = steps.vcardSteps().createVcard(fullVcard(), clientInfo)
        val campaignInfo = vcardInfo.campaignInfo

        val shard = campaignInfo.shard
        processor.withShard(shard)
        processor.process(listOf(Mysql2GrutReplicationObject(vcardId = vcardInfo.vcardId)))

        val grutVcard = replicationApiService.vcardGrutDao.getVCards(listOf(vcardInfo.vcardId)).firstOrNull()
        val mysqlVcard = vcardRepository.getVcards(shard, listOf(vcardInfo.vcardId))[0]
        val mysqlAddress = addressesRepository.getAddresses(shard, listOf(mysqlVcard.addressId))[mysqlVcard.addressId]
        val mysqlMapPoint = mapsRepository.getPoints(shard, listOf(mysqlAddress!!.mapId))[mysqlAddress.mapId]
        val mysqlMapPointAuto = mapsRepository.getPoints(shard, listOf(mysqlAddress.mapIdAuto))[mysqlAddress.mapIdAuto]

        SoftAssertions().apply {
            assertThat(grutVcard).isNotNull
            assertThat(grutVcard!!.spec.geoId).isEqualTo(mysqlVcard.geoId)
            assertThat(grutVcard.spec.phone).isEqualTo(phoneToDb(mysqlVcard.phone))
            assertThat(grutVcard.spec.name).isEqualTo(mysqlVcard.companyName)
            assertThat(grutVcard.spec.city).isEqualTo(mysqlVcard.city)
            assertThat(grutVcard.spec.contactPerson).isEqualTo(mysqlVcard.contactPerson)
            assertThat(grutVcard.spec.worktime).isEqualTo(mysqlVcard.workTime)
            assertThat(grutVcard.spec.country).isEqualTo(mysqlVcard.country)
            assertThat(grutVcard.spec.street).isEqualTo(mysqlVcard.street)
            assertThat(grutVcard.spec.house).isEqualTo(mysqlVcard.house)
            assertThat(grutVcard.spec.build).isEqualTo(mysqlVcard.build)
            assertThat(grutVcard.spec.apart).isEqualTo(mysqlVcard.apart)
            assertThat(grutVcard.spec.metroId).isEqualTo(mysqlVcard.metroId)
            assertThat(grutVcard.spec.extraMessage).isEqualTo(mysqlVcard.extraMessage)
            assertThat(grutVcard.spec.contactEmail).isEqualTo(mysqlVcard.email)
            assertThat(grutVcard.spec.imClient).isEqualTo(mysqlVcard.instantMessenger.type)
            assertThat(grutVcard.spec.imLogin).isEqualTo(mysqlVcard.instantMessenger.login)
            assertThat(grutVcard.spec.ogrn).isEqualTo(mysqlVcard.ogrn)
            if (mysqlVcard.permalink != null) {
                assertThat(grutVcard.spec.permalinkId).isEqualTo(mysqlVcard.permalink)
            } else {
                assertThat(grutVcard.spec.hasPermalinkId()).isFalse
            }

            assertThat(grutVcard.spec.address.address).isEqualTo(mysqlAddress.address)
            assertThat(grutVcard.spec.address.metroId).isEqualTo(mysqlAddress.metroId)
            assertThat(grutVcard.spec.address.precisionType).isEqualTo(precisionTypeToGrut(mysqlAddress.precision).number)

            // Преобразования соответствуют коду
            // https://a.yandex-team.ru/arc_vcs/direct/libs/geosearch-client/src/main/java/ru/yandex/direct/geosearch/model/GeoObject.java?rev=r6539534#L135
            assertThat(grutVcard.spec.address.mapPoint.lon).isEqualTo(bigDecimalToGrut(mysqlMapPoint!!.x))
            assertThat(grutVcard.spec.address.mapPoint.lat).isEqualTo(bigDecimalToGrut(mysqlMapPoint.y))
            assertThat(grutVcard.spec.address.mapPoint.lowerCornerLon).isEqualTo(bigDecimalToGrut(mysqlMapPoint.x1))
            assertThat(grutVcard.spec.address.mapPoint.lowerCornerLat).isEqualTo(bigDecimalToGrut(mysqlMapPoint.y1))
            assertThat(grutVcard.spec.address.mapPoint.upperCornerLon).isEqualTo(bigDecimalToGrut(mysqlMapPoint.x2))
            assertThat(grutVcard.spec.address.mapPoint.upperCornerLat).isEqualTo(bigDecimalToGrut(mysqlMapPoint.y2))

            assertThat(grutVcard.spec.address.mapPointAuto.lon).isEqualTo(bigDecimalToGrut(mysqlMapPointAuto!!.x))
            assertThat(grutVcard.spec.address.mapPointAuto.lat).isEqualTo(bigDecimalToGrut(mysqlMapPointAuto.y))
            assertThat(grutVcard.spec.address.mapPointAuto.lowerCornerLon)
                .isEqualTo(bigDecimalToGrut(mysqlMapPointAuto.x1))
            assertThat(grutVcard.spec.address.mapPointAuto.lowerCornerLat)
                .isEqualTo(bigDecimalToGrut(mysqlMapPointAuto.y1))
            assertThat(grutVcard.spec.address.mapPointAuto.upperCornerLon)
                .isEqualTo(bigDecimalToGrut(mysqlMapPointAuto.x2))
            assertThat(grutVcard.spec.address.mapPointAuto.upperCornerLat)
                .isEqualTo(bigDecimalToGrut(mysqlMapPointAuto.y2))
        }.assertAll()
    }

    @Test
    fun createAndDeleteVCardTest() {
        val vcardInfo = steps.vcardSteps().createVcard(fullVcard(), clientInfo)
        val campaignInfo = vcardInfo.campaignInfo

        val vcardId = vcardInfo.vcardId

        processor.withShard(campaignInfo.shard)
        processor.process(listOf(Mysql2GrutReplicationObject(vcardId = vcardId)))

        val grutVcardBefore = replicationApiService.vcardGrutDao.getVCards(listOf(vcardId)).firstOrNull()

        vcardRepository.deleteUnusedVcards(campaignInfo.shard, listOf(vcardId))
        processor.process(listOf(Mysql2GrutReplicationObject(vcardId = vcardId, isDeleted = true)))

        val grutVcardAfter = replicationApiService.vcardGrutDao.getVCards(listOf(vcardId)).firstOrNull()

        SoftAssertions().apply {
            assertThat(grutVcardBefore).isNotNull
            assertThat(grutVcardAfter).isNull()
        }.assertAll()
    }

    @Test
    fun updateVCardTest() {
        val vcardInfo = steps.vcardSteps().createVcard(fullVcard(), clientInfo)
        val campaignInfo = vcardInfo.campaignInfo

        val vcardId = vcardInfo.vcardId

        val shard = campaignInfo.shard
        processor.withShard(shard)
        processor.process(listOf(Mysql2GrutReplicationObject(vcardId = vcardId)))

        val grutVcardBefore = replicationApiService.vcardGrutDao.getVCards(listOf(vcardId)).first()
        val mysqlVcard = vcardRepository.getVcards(shard, listOf(vcardId)).first()

        // update vcard
        dslContextProvider.ppc(shard)
            .update(VCARDS)
            .set(VCARDS.NAME, "updated name")
            .set(VCARDS.CONTACTPERSON, "updated contact person")
            .where(VCARDS.VCARD_ID.eq(vcardId))
            .execute()

        processor.process(listOf(Mysql2GrutReplicationObject(vcardId = vcardId)))

        val grutVcardAfter = replicationApiService.vcardGrutDao.getVCards(listOf(vcardId)).first()

        SoftAssertions().apply {
            assertThat(grutVcardBefore.spec.name).isEqualTo(mysqlVcard.companyName)
            assertThat(grutVcardBefore.spec.contactPerson).isEqualTo(mysqlVcard.contactPerson)

            assertThat(grutVcardAfter.spec.name).isEqualTo("updated name")
            assertThat(grutVcardAfter.spec.contactPerson).isEqualTo("updated contact person")
        }.assertAll()
    }

    @Test
    @Description("Если кампания к моменту репликции визитки удалена, то визитку нужно скипнуть")
    fun createVCardWithDeletedCampaignTest() {
        val vcardInfo = steps.vcardSteps().createVcard(fullVcard(), clientInfo)
        val campaignInfo = vcardInfo.campaignInfo

        val vcardId = vcardInfo.vcardId

        dslContextProvider.ppc(campaignInfo.shard)
            .delete(CAMPAIGNS)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(listOf(Mysql2GrutReplicationObject(vcardId = vcardId)))

        val grutVcard = replicationApiService.vcardGrutDao.getVCards(listOf(vcardId)).firstOrNull()

        SoftAssertions().apply {
            assertThat(grutVcard).isNull()
        }.assertAll()
    }
}
