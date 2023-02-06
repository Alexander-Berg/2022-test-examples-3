package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition
import ru.yandex.direct.core.entity.retargeting.model.Rule
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository
import ru.yandex.direct.core.grut.api.RetargetingConditionGrut
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.mysql2grut.repository.RetargetingGoal
import ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRule
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.GrutReplicationSteps
import ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.repository.RetargetingGoalTestRepository
import ru.yandex.direct.mysql2grut.enummappers.RetargetingConditionEnumMappers.Mappers.toGrut
import ru.yandex.direct.mysql2grut.enummappers.RetargetingConditionEnumMappers.Mappers.toGrutGoalSource
import ru.yandex.grut.auxiliary.proto.RetargetingRule.TRetargetingRule.TGoalCombination
import ru.yandex.grut.objects.proto.RetargetingCondition.TRetargetingConditionMetaBase.ERetargetingConditionType

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class RetargetingConditionReplicationTest {
    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationService: GrutApiService

    @Autowired
    private lateinit var retargetingGoalRepository: RetargetingGoalTestRepository

    @Autowired
    private lateinit var retargetingConditionRepository: RetargetingConditionRepository

    @Autowired
    private lateinit var grutSteps: GrutReplicationSteps

    @BeforeEach
    fun setup() {
        processor.withShard(grutSteps.DEFAULT_SHARD)
    }

    @AfterEach
    private fun tearDown() {
        grutSteps.cleanupClientsWithChildren()
    }

    @Test
    fun replicateRetCond() {
        val retCondInfo = grutSteps.steps.retConditionSteps().createDefaultRetCondition()
        val retGoalsByGoalId = retargetingGoalRepository
            .getRetargetingGoals(grutSteps.DEFAULT_SHARD, listOf(retCondInfo.retConditionId))
            .associateBy { it.goalId }
        // act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = retCondInfo.clientId.asLong()),
                Mysql2GrutReplicationObject(retargetingConditionId = retCondInfo.retConditionId)
            )
        )
        // assert
        val retCondCreated = replicationService.retargetingConditionGrutApi
            .getRetargetingCondition(retCondInfo.retConditionId)
        Assertions.assertNotNull(retCondCreated)

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(retCondCreated!!.meta.id).isEqualTo(retCondInfo.retConditionId)
            softly.assertThat(retCondCreated.meta.clientId).isEqualTo(retCondInfo.clientId.asLong())
            softly.assertThat(retCondCreated.meta.retargetingConditionType)
                .isEqualTo(ERetargetingConditionType.RC_METRIKA_GOALS.number)

            softly.assertThat(retCondCreated.spec.name).isEqualTo(retCondInfo.retCondition.name)
            softly.assertThat(retCondCreated.spec.description).isEqualTo(retCondInfo.retCondition.description)
            softly.assertThat(retCondCreated.spec.isDeleted).isEqualTo(retCondInfo.retCondition.deleted)

            softly.assertThat(retCondCreated.spec.rule.goalCombinations.itemsCount)
                .isEqualTo(1)

            val combination = retCondCreated.spec.rule.goalCombinations.itemsList.first()
            val directRule = retCondInfo.retCondition.rules.first()
            softly.assertThat(combination.operator).isEqualTo(toGrut(directRule.type).number)
            softly.assertThat(combination.goalsCount).isEqualTo(6)
            validateGoals(softly, combination, directRule, retGoalsByGoalId)
        }
    }

    private fun validateGoals(
        softly: SoftAssertions,
        combination: TGoalCombination,
        directRule: Rule,
        extraGoalInfo: Map<Long, RetargetingGoal>
    ) {
        combination.goalsList.forEachIndexed { i, grutGoal ->
            val directGoal = directRule.goals.get(i)
            softly.assertThat(grutGoal.goalId).isEqualTo(directGoal.id)
            softly.assertThat(grutGoal.goalType).isEqualTo(toGrut(directGoal.type).number)
            softly.assertThat(grutGoal.goalSource).isEqualTo(toGrutGoalSource(directGoal.type).number)
            softly.assertThat(grutGoal.validDays.toInt()).isEqualTo(directGoal.time)
            softly.assertThat(grutGoal.isAccessible).isEqualTo(extraGoalInfo[grutGoal.goalId]!!.isAccessible)
        }
    }

    @Test
    fun replicateABSegmentRetCond() {
        val clientInfo = grutSteps.steps.clientSteps().createDefaultClient()
        val retCondInfo = grutSteps.steps.retConditionSteps().createDefaultABSegmentRetCondition(clientInfo)
        val retGoalsByGoalId = retargetingGoalRepository
            .getRetargetingGoals(grutSteps.DEFAULT_SHARD, listOf(retCondInfo.retConditionId))
            .associateBy { it.goalId }

        //act
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = retCondInfo.clientId.asLong()),
                Mysql2GrutReplicationObject(retargetingConditionId = retCondInfo.retConditionId)
            )
        )
        val ab = replicationService.retargetingConditionGrutApi.getRetargetingCondition(retCondInfo.retConditionId)
        // assert
        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(ab!!.meta.id).isEqualTo(retCondInfo.retConditionId)
            softly.assertThat(ab.meta.clientId).isEqualTo(retCondInfo.clientId.asLong())
            softly.assertThat(ab.meta.retargetingConditionType)
                .isEqualTo(ERetargetingConditionType.RC_AB_SEGMENTS.number)
            softly.assertThat(ab.spec.name).isEqualTo(retCondInfo.retCondition.name)
            softly.assertThat(ab.spec.rule.abExperiments).isNotNull
            softly.assertThat(ab.spec.rule.bidModifierAbCondition).isNotNull
        }
    }

    /**
     * Проверяем, что изменение флага isAccessible в таблице retargeting_goals реплицируется
     */
    @Test
    fun changeIsAccessibleField() {
        val retCondInfo = grutSteps.steps.retConditionSteps().createDefaultRetCondition()
        val firstGoalId = retCondInfo.retCondition.rules[0].goals[0].id
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = retCondInfo.clientId.asLong()),
                Mysql2GrutReplicationObject(retargetingConditionId = retCondInfo.retConditionId)
            )
        )
        val retCondBefore = replicationService.retargetingConditionGrutApi
            .getRetargetingCondition(retCondInfo.retConditionId)

        Assertions.assertNotNull(retCondBefore)
        Assertions.assertEquals(1, retCondBefore!!.spec.rule.goalCombinations.itemsCount)
        Assertions.assertEquals(6, retCondBefore.spec.rule.goalCombinations.itemsList.first().goalsCount)
        Assertions.assertTrue(retCondBefore.spec.rule.goalCombinations.itemsList.first().goalsList.first().isAccessible)

        // act
        retargetingGoalRepository.updateIsAccessible(
            grutSteps.DEFAULT_SHARD,
            retCondInfo.retConditionId,
            firstGoalId,
            isAccessible = false
        )
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(retargetingConditionId = retCondInfo.retConditionId)
            )
        )
        // assert
        val retCondAfter = replicationService.retargetingConditionGrutApi
            .getRetargetingCondition(retCondInfo.retConditionId)
        Assertions.assertNotNull(retCondAfter)
        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(retCondAfter!!.spec.rule.goalCombinations.itemsCount).isEqualTo(1)
            softly.assertThat(retCondAfter!!.spec.rule.goalCombinations.itemsList.first().goalsCount).isEqualTo(6)
            val firstGoal = retCondAfter.spec.rule.goalCombinations.itemsList.first().goalsList.first()
            softly.assertThat(firstGoal.goalId).isEqualTo(firstGoalId)
            softly.assertThat(firstGoal.isAccessible).isFalse
        }
    }

    @Test
    fun replicateRetCondWithEmptyCondition() {
        val client = grutSteps.steps.clientSteps().createDefaultClient()
        val retCondInfo = grutSteps.steps.retConditionSteps().createDefaultRetCondition(emptyList(), client)

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = retCondInfo.clientId.asLong()),
                Mysql2GrutReplicationObject(retargetingConditionId = retCondInfo.retConditionId)
            )
        )
        val retCondGrut = replicationService.retargetingConditionGrutApi
            .getRetargetingCondition(retCondInfo.retConditionId)
        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(retCondInfo.retCondition.collectGoalsSafe()).isEmpty()
            softly.assertThat(retCondGrut!!.meta.retargetingConditionType)
                .isEqualTo(ERetargetingConditionType.RC_METRIKA_GOALS.number)

            softly.assertThat(retCondGrut.spec.rule.serializedSize).`as`("/spec/rule не заполняется")
                .isEqualTo(0)

            softly.assertThat(retCondGrut.spec.rule.goalCombinations.itemsCount).isEqualTo(0)
            softly.assertThat(retCondGrut.spec.rule.abExperiments.itemsCount).isEqualTo(0)
        }
    }

    /**
     * Тест проверяет, что изменение поля isDeleted реплицируется
     */
    @Test
    fun changeIsDeletedField() {
        val retCondInfo = grutSteps.steps.retConditionSteps().createDefaultRetCondition()
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(clientId = retCondInfo.clientId.asLong()),
                Mysql2GrutReplicationObject(retargetingConditionId = retCondInfo.retConditionId)
            )
        )
        val retCondBefore = replicationService.retargetingConditionGrutApi
            .getRetargetingCondition(retCondInfo.retConditionId)
        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(retCondBefore!!.spec.isDeleted).isEqualTo(false)
        }
        //act
        // Помечаем retargeting_condition как удаленный
        retargetingConditionRepository
            .delete(grutSteps.DEFAULT_SHARD, retCondInfo.clientId, listOf(retCondInfo.retConditionId))
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(retargetingConditionId = retCondInfo.retConditionId)
            )
        )
        // assert
        val retCondAfter = replicationService.retargetingConditionGrutApi
            .getRetargetingCondition(retCondInfo.retConditionId)
        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(retCondAfter!!.spec.isDeleted).isEqualTo(true)
        }
    }

    /**
     * В проде удалений retargeting_conditions не бывает, но логика репликации, если такие запросы будут, есть.
     */
    @Test
    fun deleteRetCond() {
        // arrange
        // Cоздаем клиента в GrUT и MySQL.
        val clientInfo = grutSteps.steps.clientSteps().createDefaultClient()
        val clientId = clientInfo.clientId!!.asLong()
        val retCondId = 325L;
        val goal = Goal().withId(Goal.HOST_LOWER_BOUND) as Goal
        processor.process(
            listOf(Mysql2GrutReplicationObject(clientId = clientId))
        )

        // Создаем объект в GrUT, которого нет в MySQL.
        replicationService.retargetingConditionGrutApi.createOrUpdateRetargetingConditions(
            listOf(
                RetargetingConditionGrut(
                    RetargetingCondition()
                        .withId(retCondId)
                        .withClientId(clientId)
                        .withRules(listOf(defaultRule(listOf(goal))))
                        .withType(ConditionType.interests)
                        .withDeleted(false)
                        .withName("testName") as RetargetingCondition,
                    emptyMap()
                )
            )
        )

        // act - Реплицируем удаление в GrUT.
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    retargetingConditionId = retCondId,
                    isDeleted = true
                )
            )
        )
        // assert
        val deleted = replicationService.retargetingConditionGrutApi
            .getRetargetingCondition(retCondId)
        Assertions.assertNull(deleted)
    }
}
