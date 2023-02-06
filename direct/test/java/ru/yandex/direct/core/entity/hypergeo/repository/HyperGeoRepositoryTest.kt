package ru.yandex.direct.core.entity.hypergeo.repository

import org.hamcrest.Matchers.equalTo
import org.jooq.Configuration
import org.jooq.DSLContext
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.test.utils.randomPositiveLong

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class HyperGeoRepositoryTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var adGroupRepository: AdGroupRepository

    @Autowired
    private lateinit var hyperGeoRepository: HyperGeoRepository

    private lateinit var adGroupInfo: AdGroupInfo
    private lateinit var anotherAdGroupInfo: AdGroupInfo
    private lateinit var clientInfo: ClientInfo
    private lateinit var dslContext: DSLContext
    private lateinit var dslConfig: Configuration

    private var adGroupId = 0L
    private var anotherAdGroupId = 0L
    private var shard = 0
    private lateinit var clientId: ClientId

    private lateinit var hyperGeo: HyperGeo

    @Before
    fun before() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup()
        clientInfo = adGroupInfo.clientInfo
        dslContext = dslContextProvider.ppc(adGroupInfo.shard)
        dslConfig = dslContext.configuration()
        anotherAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo)

        adGroupId = adGroupInfo.adGroupId
        anotherAdGroupId = anotherAdGroupInfo.adGroupId
        shard = adGroupInfo.shard
        clientId = clientInfo.clientId!!

        hyperGeo = steps.hyperGeoSteps().createHyperGeo(clientInfo)
    }

    @Test
    fun linkHyperGeosToAdGroupsTest() {
        adGroupInfo.adGroup.hyperGeoId
            .checkEquals(null)

        val hyperGeoId = randomPositiveLong(Int.MAX_VALUE.toLong())
        hyperGeoRepository.linkHyperGeosToAdGroups(dslConfig, mapOf(adGroupId to hyperGeoId))

        adGroupRepository.getAdGroups(shard, listOf(adGroupId))[0].hyperGeoId
            .checkEquals(hyperGeoId)
    }

    @Test
    fun unlinkHyperGeosFromAdGroupsTest() {
        val hyperGeoId = randomPositiveLong(Int.MAX_VALUE.toLong())

        hyperGeoRepository.linkHyperGeosToAdGroups(dslConfig, mapOf(adGroupId to hyperGeoId))
        adGroupRepository.getAdGroups(adGroupInfo.shard, listOf(adGroupInfo.adGroupId))[0].hyperGeoId
            .checkEquals(hyperGeoId)

        hyperGeoRepository.unlinkHyperGeosFromAdGroups(dslConfig, listOf(adGroupId))
        adGroupRepository.getAdGroups(shard, listOf(adGroupId))[0].hyperGeoId
            .checkEquals(null)
    }

    @Test
    fun getHyperGeoByAdGroupIdTest() {
        hyperGeoRepository.linkHyperGeosToAdGroups(dslConfig, mapOf(adGroupId to hyperGeo.id))

        hyperGeoRepository.getHyperGeoByAdGroupId(shard, clientId, listOf(adGroupId))
            .checkEquals(mapOf(adGroupId to hyperGeo))
    }

    @Test
    fun getHyperGeoByIdTest() {
        hyperGeoRepository.getHyperGeoById(shard, clientId, listOf(hyperGeo.id))
            .checkEquals(mapOf(hyperGeo.id to hyperGeo))
    }

    @Test
    fun getAdGroupIdsByHyperGeoTest() {
        adGroupInfo.adGroup.hyperGeoId
            .checkEquals(null)

        val hyperGeoId = randomPositiveLong(Int.MAX_VALUE.toLong())
        hyperGeoRepository.linkHyperGeosToAdGroups(dslConfig, mapOf(adGroupId to hyperGeoId))

        hyperGeoRepository.getAdGroupIdsByHyperGeoId(shard, listOf(hyperGeoId))
            .checkEquals(mapOf(hyperGeoId to listOf(adGroupId)))
    }

    @Test
    fun getAdGroupIdsByHyperGeo_MultipleTest() {
        adGroupInfo.adGroup.hyperGeoId
            .checkEquals(null)
        anotherAdGroupInfo.adGroup.hyperGeoId
            .checkEquals(null)

        val hyperGeoId = randomPositiveLong(Int.MAX_VALUE.toLong())
        hyperGeoRepository
            .linkHyperGeosToAdGroups(dslConfig, mapOf(adGroupId to hyperGeoId, anotherAdGroupId to hyperGeoId))

        hyperGeoRepository.getAdGroupIdsByHyperGeoId(shard, listOf(hyperGeoId))
            .checkEquals(mapOf(hyperGeoId to listOf(adGroupId, anotherAdGroupId)))
    }

    @Test
    fun getUnusedHyperGeoIds_UnlinkedHyperGeoTest() {
        adGroupInfo.adGroup.hyperGeoId
            .checkEquals(null)

        hyperGeoRepository.getUnusedHyperGeoIds(dslContext, clientId, listOf(hyperGeo.id))
            .checkEquals(setOf(hyperGeo.id))
        hyperGeoRepository.getUnusedHyperGeoIds(dslContext, null, listOf(hyperGeo.id))
            .checkEquals(setOf(hyperGeo.id))
        hyperGeoRepository
            .getUnusedHyperGeoIds(dslContext, ClientId.fromLong(clientId.asLong() + 1), listOf(hyperGeo.id))
            .checkEquals(setOf())
    }

    @Test
    fun getUnusedHyperGeoIds_LinkedHyperGeoTest() {
        adGroupInfo.adGroup.hyperGeoId
            .checkEquals(null)

        hyperGeoRepository.getUnusedHyperGeoIds(dslContext, clientId, listOf(hyperGeo.id))
            .checkEquals(setOf(hyperGeo.id))

        hyperGeoRepository.linkHyperGeosToAdGroups(dslConfig, mapOf(adGroupId to hyperGeo.id))

        hyperGeoRepository.getUnusedHyperGeoIds(dslContext, clientId, listOf(hyperGeo.id))
            .checkEquals(setOf())
    }
}

private fun <T> T.checkEquals(expected: T?) = assertThat(this, equalTo(expected))
