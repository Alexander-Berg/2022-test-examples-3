package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames.GRUT_SKIP_WRONG_PARENT_KEY_ERRORS
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.api.RetargetingConditionGrut
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultABSegmentRules
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.GrutReplicationSteps
import ru.yandex.grut.client.YtGenericException
import java.time.LocalDateTime

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class ReplicationPropertiesTest {
    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationService: GrutApiService

    @Autowired
    private lateinit var grutSteps: GrutReplicationSteps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @BeforeEach
    fun setup() {
        processor.withShard(grutSteps.DEFAULT_SHARD)
    }

    @AfterEach
    private fun tearDown() {
        ppcPropertiesSupport.set(GRUT_SKIP_WRONG_PARENT_KEY_ERRORS, "false")
        grutSteps.cleanupClientsWithChildren()
    }

    // Тест на то, что мы умеем скипать записи, у которых возникает row conflict по parent key в Грут.
    // Актуально для тестинга, где бывают дубли по айди для разных шардов.
    // На проде будем держать фичу всегда выключеной.
    /**
     * C флагом мы пропускаем исключение, теряя часть апдейтов
     */
    @Test
    fun parentRowConflict_SKIP_Test() {
        ppcPropertiesSupport.set(GRUT_SKIP_WRONG_PARENT_KEY_ERRORS, "true")
        assertDoesNotThrow { parentKeyConflictBase() }
    }

    /**
     * Без флага исключение должно прокидываться дальше
     */
    @Test
    fun parentRowConflict_THROW_Test() {
        ppcPropertiesSupport.set(GRUT_SKIP_WRONG_PARENT_KEY_ERRORS, "false")
        assertThrows<YtGenericException> { parentKeyConflictBase() }
    }

    /**
     * Создает ситуацию, когда возникает ошибка конфликта родительских идентификаторов в груте
     */
    fun parentKeyConflictBase() {
        val retCondInfo = grutSteps.steps.retConditionSteps().createDefaultRetCondition()
        val retCondId = retCondInfo.retConditionId

        // создаем пару ret_cond - client с отличающимся от директа clientId
        // чтобы получить конфликт
        replicationService.clientGrutDao.createOrUpdateClient(
            ClientGrutModel(
                Client()
                    .withName("some_client")
                    .withId(256)
                    .withCreateDate(LocalDateTime.of(2022, 7, 7, 12, 0))
                    .withDefaultAllowedDomains(emptyList()), emptyList()
            )
        )
        val createdClient = replicationService.clientGrutDao.getClient(256)
        Assertions.assertNotNull(createdClient)

        val retCond = RetargetingCondition()
            .withRules(defaultABSegmentRules())
            .withClientId(256L)
            .withId(retCondId)
            .withDeleted(false)
            .withType(retCondInfo.retCondition.type)
            .withName("ret_cond") as RetargetingCondition
        replicationService.retargetingConditionGrutApi.createOrUpdateRetargetingConditions(
            listOf(
                RetargetingConditionGrut(
                    retCond, emptyMap()
                )
            )
        )
        val createdRetCond = replicationService.retargetingConditionGrutApi.getRetargetingCondition(retCondId)
        Assertions.assertNotNull(createdRetCond)

        // реплицируем существующий в директе ret c cond - должны получить конфликт
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = retCondInfo.clientId.asLong()),
                Mysql2GrutReplicationObject(retargetingConditionId = retCondInfo.retConditionId)
            )
        )
    }
}
