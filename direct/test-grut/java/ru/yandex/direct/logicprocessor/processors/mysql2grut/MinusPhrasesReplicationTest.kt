package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.apache.commons.lang.RandomStringUtils
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.mysql2grut.repository.MinusPhrase
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.GrutReplicationSteps
import ru.yandex.direct.utils.JsonUtils

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class MinusPhrasesReplicationTest {
    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationService: GrutApiService

    @Autowired
    private lateinit var grutSteps: GrutReplicationSteps
    private lateinit var existingInDirectMinusPhrase: MinusPhrase
    private lateinit var existingOnlyInGrutMinusPhrase: MinusPhrase

    @BeforeEach
    fun setup() {
        existingInDirectMinusPhrase = grutSteps.createRandomMinusPhraseInDirectWithGrutHierarchy()
        existingOnlyInGrutMinusPhrase = grutSteps.createRandomMinusPhraseInGrut()

        processor.withShard(grutSteps.DEFAULT_SHARD)
    }

    @AfterEach
    private fun tearDown() {
        grutSteps.cleanupClientsWithChildren()
    }

    @Test
    fun replicateMinusPhrase() {
        //arrange
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(minusPhraseId = existingInDirectMinusPhrase.id))
        //act
        processor.process(processorArgs)
        //assert
        val minusPhrase = replicationService.minusPhrasesGrutDao.getMinusPhrase(existingInDirectMinusPhrase.id!!)
        Assertions.assertThat(minusPhrase).isNotNull
        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(minusPhrase!!.meta.clientId).isEqualTo(existingInDirectMinusPhrase.clientId)
            softly.assertThat(minusPhrase.spec.hasName()).isFalse
            softly.assertThat(minusPhrase.spec.phrasesCount).isEqualTo(existingInDirectMinusPhrase.phrases.size)
            softly.assertThat(minusPhrase.spec.phrasesList).isEqualTo(existingInDirectMinusPhrase.phrases)
        }
    }

    @Test
    fun updateExistingMinusPhrase() {
        // arrange
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(minusPhraseId = existingInDirectMinusPhrase.id))
        processor.process(processorArgs)
        val minusPhrase = replicationService.minusPhrasesGrutDao.getMinusPhrase(existingInDirectMinusPhrase.id!!)
        Assertions.assertThat(minusPhrase).isNotNull
        // Обновляем существующую минус-фразу

        val newText = JsonUtils.toJson(listOf(RandomStringUtils.randomAlphabetic(5)))
        grutSteps.minusPhraseTestRepository.updateMinusPhrase(
            grutSteps.DEFAULT_SHARD,
            existingInDirectMinusPhrase.id!!,
            newText,
        )
        // act
        processor.process(processorArgs)
        // assert
        val updatedDirectMinusPhrase = grutSteps.minusPhraseTestRepository
            .getMinusPhrases(
                shard = grutSteps.DEFAULT_SHARD,
                listOf(existingInDirectMinusPhrase.id!!),
                existingInDirectMinusPhrase.isLibrary,
            ).first()
        val updatedMinusPhrase = replicationService.minusPhrasesGrutDao.getMinusPhrase(existingInDirectMinusPhrase.id!!)
        Assertions.assertThat(updatedMinusPhrase!!.spec.phrasesList).isEqualTo(updatedDirectMinusPhrase.phrases)
    }


    @Test
    fun deleteMinusPhrase() {
        //arrange
        val processorArgs = mutableListOf(Mysql2GrutReplicationObject(minusPhraseId = existingOnlyInGrutMinusPhrase.id, isDeleted = true))
        //act
        processor.process(processorArgs)
        //assert
        val minusPhrase = replicationService.minusPhrasesGrutDao.getMinusPhrase(existingOnlyInGrutMinusPhrase.id!!)
        Assertions.assertThat(minusPhrase).isNull()
    }

}
