package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.creative.model.Creative
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.PERF_CREATIVES
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.model.ModelChanges
import java.math.BigDecimal

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class CreativeReplicationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var creativeRepository: CreativeRepository

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
    fun createHtml5CreativeTest() {
        val creativeId = steps.creativeSteps().nextCreativeId
        steps.creativeSteps().addDefaultHtml5Creative(clientInfo, creativeId)

        val shard = clientInfo.shard
        processor.withShard(shard)
        processor.process(listOf(Mysql2GrutReplicationObject(creativeId = creativeId)))

        val mysqlCreative = creativeRepository.getCreatives(shard, listOf(creativeId)).first()
        val grutCreative = replicationApiService.creativeGrutDao.getCreative(creativeId)

        SoftAssertions().apply {
            assertThat(grutCreative).isNotNull

            assertThat(grutCreative!!.spec.name).isEqualTo(mysqlCreative.name)
            assertThat(grutCreative.spec.width).isEqualTo(mysqlCreative.width.toInt())
            assertThat(grutCreative.spec.height).isEqualTo(mysqlCreative.height.toInt())
            assertThat(grutCreative.spec.isAdaptive).isEqualTo(mysqlCreative.isAdaptive)
            assertThat(grutCreative.spec.previewUrl).isEqualTo(mysqlCreative.previewUrl)
            assertThat(grutCreative.spec.isBrandLift).isEqualTo(mysqlCreative.isBrandLift)
            assertThat(grutCreative.spec.hasSourceMediaType()).isFalse
            assertThat(grutCreative.spec.layoutId).isEqualTo(mysqlCreative.layoutId?.toInt() ?: 0)
            assertThat(grutCreative.spec.livePreviewUrl).isEqualTo(mysqlCreative.livePreviewUrl)
            assertThat(grutCreative.spec.hasDuration()).isFalse

            assertThat(grutCreative.spec.hasHtml5Data()).isTrue
            assertThat(grutCreative.spec.hasVideoData()).isFalse
            assertThat(grutCreative.spec.html5Data.isGenerated).isEqualTo(mysqlCreative.isGenerated)
            assertThat(grutCreative.spec.html5Data.zipArchiveUrl).isEqualTo(mysqlCreative.archiveUrl)
            assertThat(grutCreative.spec.html5Data.yabsDataBasePath).isEqualTo(mysqlCreative.yabsData.basePath)
            assertThat(grutCreative.spec.html5Data.hasExpandedPreviewUrl()).isFalse
        }.assertAll()
    }

    @Test
    fun createVideoCreativeTest() {
        val creativeId = steps.creativeSteps().nextCreativeId
        steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId)

        val shard = clientInfo.shard
        processor.withShard(shard)
        processor.process(listOf(Mysql2GrutReplicationObject(creativeId = creativeId)))

        val mysqlCreative = creativeRepository.getCreatives(shard, listOf(creativeId)).first()
        val grutCreative = replicationApiService.creativeGrutDao.getCreative(creativeId)

        SoftAssertions().apply {
            assertThat(grutCreative).isNotNull

            assertThat(grutCreative!!.spec.name).isEqualTo(mysqlCreative.name)
            assertThat(grutCreative.spec.width).isEqualTo(mysqlCreative.width?.toInt() ?: 0)
            assertThat(grutCreative.spec.height).isEqualTo(mysqlCreative.height?.toInt() ?: 0)
            assertThat(grutCreative.spec.isAdaptive).isEqualTo(mysqlCreative.isAdaptive)
            assertThat(grutCreative.spec.previewUrl).isEqualTo(mysqlCreative.previewUrl)
            assertThat(grutCreative.spec.isBrandLift).isEqualTo(mysqlCreative.isBrandLift)
            assertThat(grutCreative.spec.hasSourceMediaType()).isFalse
            assertThat(grutCreative.spec.layoutId).isEqualTo(mysqlCreative.layoutId?.toInt() ?: 0)
            assertThat(grutCreative.spec.livePreviewUrl).isEqualTo(mysqlCreative.livePreviewUrl)
            assertThat(grutCreative.spec.duration).isEqualTo(mysqlCreative.duration.toInt())

            assertThat(grutCreative.spec.hasHtml5Data()).isFalse
            assertThat(grutCreative.spec.hasVideoData()).isTrue
            assertThat(grutCreative.spec.videoData.stockCreativeId).isEqualTo(mysqlCreative.stockCreativeId)
            assertThat(grutCreative.spec.videoData.duration)
                .isEqualTo(mysqlCreative.additionalData.duration.multiply(BigDecimal(1000_000)).toLong())
            assertThat(grutCreative.spec.videoData.hasPackshot).isEqualTo(mysqlCreative.hasPackshot)
            assertThat(grutCreative.spec.videoData.videoFormatsList).hasSize(mysqlCreative.additionalData.formats.size)
        }.assertAll()
    }

    @Test
    fun updateCreativeTest() {
        val creativeId = steps.creativeSteps().nextCreativeId
        val creative = steps.creativeSteps().addDefaultHtml5Creative(clientInfo, creativeId)

        val shard = clientInfo.shard
        processor.withShard(shard)
        processor.process(listOf(Mysql2GrutReplicationObject(creativeId = creativeId)))

        val mysqlCreative = creativeRepository.getCreatives(shard, listOf(creativeId)).first()
        val grutCreativeBefore = replicationApiService.creativeGrutDao.getCreative(creativeId)

        val oldName = mysqlCreative.name
        val oldLayoutId = mysqlCreative.layoutId

        val modelChanges = ModelChanges(creative.creativeId, Creative::class.java)
        modelChanges.process("updated name", Creative.NAME)
        modelChanges.process(8888, Creative.LAYOUT_ID)
        val appliedChanges = modelChanges.applyTo(mysqlCreative)

        creativeRepository.update(shard, listOf(appliedChanges))

        processor.process(listOf(Mysql2GrutReplicationObject(creativeId = creativeId)))

        val grutCreativeAfter = replicationApiService.creativeGrutDao.getCreative(creativeId)

        SoftAssertions().apply {
            assertThat(grutCreativeBefore!!.spec.name).isEqualTo(oldName)
            assertThat(grutCreativeBefore.spec.layoutId).isEqualTo(oldLayoutId ?: 0)

            assertThat(grutCreativeAfter!!.spec.name).isEqualTo("updated name")
            assertThat(grutCreativeAfter.spec.layoutId).isEqualTo(8888)
        }.assertAll()
    }

    @Test
    fun deleteCreativeTest() {
        val creativeId = steps.creativeSteps().nextCreativeId
        steps.creativeSteps().addDefaultHtml5Creative(clientInfo, creativeId)

        val shard = clientInfo.shard
        processor.withShard(shard)
        processor.process(listOf(Mysql2GrutReplicationObject(creativeId = creativeId)))

        val grutCreativeBefore = replicationApiService.creativeGrutDao.getCreative(creativeId)

        dslContextProvider.ppc(shard)
            .delete(PERF_CREATIVES)
            .where(PERF_CREATIVES.CREATIVE_ID.eq(creativeId))
            .execute();

        processor.process(listOf(Mysql2GrutReplicationObject(creativeId = creativeId, isDeleted = true)))

        val grutCreativeAfter = replicationApiService.creativeGrutDao.getCreative(creativeId)

        SoftAssertions().apply {
            assertThat(grutCreativeBefore).isNotNull
            assertThat(grutCreativeAfter).isNull()
        }.assertAll()
    }
}
