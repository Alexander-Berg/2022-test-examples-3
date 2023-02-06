package ru.yandex.market.mbo.category.orchestrator.camunda.task

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.mbo.category.orchestrator.camunda.BpmnChangeCategoryConstants.MIGRATING_MODEL_STATUS_VARIABLE_KEY
import ru.yandex.market.mbo.category.orchestrator.camunda.BpmnChangeCategoryConstants.MIGRATION_ID_VARIABLE_KEY
import ru.yandex.market.mbo.category.orchestrator.camunda.task.mboc.MbocUnlockTask
import ru.yandex.market.mbo.category.orchestrator.model.MigratingModel
import ru.yandex.market.mbo.category.orchestrator.model.Migration
import ru.yandex.market.mbo.category.orchestrator.repository.MigratingModelRepository
import ru.yandex.market.mbo.category.orchestrator.repository.MigrationRepository
import ru.yandex.market.mbo.category.orchestrator.service.mboc.MbocCategoryMigrationService
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper

class MbocUnlockTaskTest {

    private val migrationRepository: MigrationRepository = mock()
    private val migratingModelRepository: MigratingModelRepository = mock()

    private val mbocCategoryMigrationService: MbocCategoryMigrationService = mock()

    private lateinit var unlockTask: MbocUnlockTask

    @BeforeEach
    fun setUp() {
        unlockTask = MbocUnlockTask(
            migrationRepository,
            migratingModelRepository,
            mbocCategoryMigrationService,
            TransactionHelper.MOCK
        )
    }

    @Test
    fun `test mboc unlock`() {
        val testMigration = Migration(
            id = 1L,
            camundaProcessId = "camunda_process_id"
        )
        doReturn(testMigration).`when`(migrationRepository).findById(eq(1L))

        val testMigratingModel = MigratingModel(
            id = 1L,
            modelId = 100L,
            sourceModelId = 100L,
            modelType = "GURU",
            originalBroken = false,
            originalStrictChecksRequired = false,
            modelModifiedTs = 111L,
            sourceCategoryId = 1L,
            sourceCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
            targetCategoryId = 2L,
            targetCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
            status = MigratingModel.Status.FINISHING
        )
        val testMigratingModelWrongStatus = MigratingModel(
            id = 1L,
            modelId = 100L,
            sourceModelId = 100L,
            modelType = "GURU",
            originalBroken = false,
            originalStrictChecksRequired = false,
            modelModifiedTs = 111L,
            sourceCategoryId = 1L,
            sourceCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
            targetCategoryId = 2L,
            targetCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
            status = MigratingModel.Status.ACTIVE
        )
        doReturn(
            listOf(
                testMigratingModel, testMigratingModelWrongStatus
            )
        )
            .`when`(migratingModelRepository).findByMigrationId(eq(1L))

        unlockTask.execute(
            mapOf(
                MIGRATION_ID_VARIABLE_KEY to 1L,
                MIGRATING_MODEL_STATUS_VARIABLE_KEY to MigratingModel.Status.FINISHING.name
            )
        )

        val migrationCaptor = argumentCaptor<Migration>()
        val modelsCaptor = argumentCaptor<List<MigratingModel>>()
        verify(mbocCategoryMigrationService, Mockito.times(1))
            .unlock(migrationCaptor.capture(), modelsCaptor.capture())

        val requestMigration = migrationCaptor.firstValue

        requestMigration shouldNotBe null
        requestMigration shouldBe testMigration
    }
}
