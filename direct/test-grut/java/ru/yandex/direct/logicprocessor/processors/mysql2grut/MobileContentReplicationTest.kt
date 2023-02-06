package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent
import ru.yandex.direct.core.grut.api.MobileContentGrutApi
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.MobileContentInfo
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.GrutReplicationSteps
import ru.yandex.direct.model.ModelChanges

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class MobileContentReplicationTest {
    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationService: GrutApiService

    @Autowired
    private lateinit var grutSteps: GrutReplicationSteps

    private lateinit var existingInDirectMobileContent: MobileContentInfo
    private var existingOnlyInGrutMobileContentId: Long = 0

    @BeforeEach
    fun setup() {
        existingInDirectMobileContent = grutSteps.createMobileContentInMySql()
        existingOnlyInGrutMobileContentId = grutSteps.createMobileContentInGrut()

        processor.withShard(grutSteps.DEFAULT_SHARD)
    }

    @AfterEach
    private fun tearDown() {
        grutSteps.cleanupClientsWithChildren()
    }

    @Test
    fun replicateMobileContent() {
        //arrange
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(mobileContentId = existingInDirectMobileContent.mobileContentId))
        //act
        processor.process(processorArgs)
        //assert
        val mobileContent = replicationService.mobileContentGrutDao.getMobileContent(existingInDirectMobileContent.mobileContentId!!)
        val entity = existingInDirectMobileContent.mobileContent
        Assertions.assertThat(mobileContent).isNotNull

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(mobileContent!!.meta.clientId).isEqualTo(existingInDirectMobileContent.clientId.asLong())
            softly.assertThat(mobileContent.spec.name).isEqualTo(entity.name)
            softly.assertThat(mobileContent.spec.platform).isEqualTo(MobileContentGrutApi.convertOsType(entity.osType).number)
        }
    }

    @Test
    fun updateExistingMobileContent() {
        // arrange
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(mobileContentId = existingInDirectMobileContent.mobileContentId))
        processor.process(processorArgs)
        val mobileContent = replicationService.mobileContentGrutDao.getMobileContent(existingInDirectMobileContent.mobileContentId!!)
        Assertions.assertThat(mobileContent).isNotNull
        // update existing mobile content
        val newName = "newMobileContentName"
        val changes = ModelChanges(existingInDirectMobileContent.mobileContentId, MobileContent::class.java)
            .process(newName, MobileContent.NAME)
            .applyTo(existingInDirectMobileContent.mobileContent)
        grutSteps.mobileContentRepository.updateMobileContent(existingInDirectMobileContent.shard,
            listOf(changes))
        // act
        processor.process(processorArgs)
        // assert
        val mobileContentUpdated = replicationService.mobileContentGrutDao.getMobileContent(existingInDirectMobileContent.mobileContentId!!)
        Assertions.assertThat(mobileContentUpdated!!.spec.name).isEqualTo(newName)
    }

    @Test
    fun deleteMobileContent() {
        //arrange
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(
            mobileContentId = existingOnlyInGrutMobileContentId,
            isDeleted = true,
        ))
        //act
        processor.process(processorArgs)
        //assert
        val mobileContent = replicationService.mobileContentGrutDao.getMobileContent(existingOnlyInGrutMobileContentId)
        Assertions.assertThat(mobileContent).isNull()
    }

}
