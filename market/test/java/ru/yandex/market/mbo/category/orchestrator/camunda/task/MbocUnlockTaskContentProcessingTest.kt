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
import ru.yandex.market.mbo.category.orchestrator.camunda.BpmnChangeCategoryConstants.MIGRATION_ID_VARIABLE_KEY
import ru.yandex.market.mbo.category.orchestrator.camunda.task.mboc.MbocUnlockContentProcessingTask
import ru.yandex.market.mbo.category.orchestrator.model.MigratingModel
import ru.yandex.market.mbo.category.orchestrator.model.Migration
import ru.yandex.market.mbo.category.orchestrator.repository.MigratingModelRepository
import ru.yandex.market.mbo.category.orchestrator.repository.MigrationRepository
import ru.yandex.market.mbo.category.orchestrator.service.mboc.MbocCategoryMigrationService

class MbocUnlockTaskContentProcessingTest {

    private val migrationRepository: MigrationRepository = mock()
    private val migratingModelRepository: MigratingModelRepository = mock()

    private val mbocCategoryMigrationService: MbocCategoryMigrationService = mock()

    private lateinit var unlockContentProcessingTask: MbocUnlockContentProcessingTask

    @BeforeEach
    fun setUp() {
        unlockContentProcessingTask = MbocUnlockContentProcessingTask(
            migrationRepository,
            migratingModelRepository,
            mbocCategoryMigrationService
        )
    }

    @Test
    fun `test mboc is invoked`() {
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
            status = MigratingModel.Status.ACTIVE
        )
        doReturn(listOf(testMigratingModel)).`when`(migratingModelRepository).findByMigrationId(eq(1L))

        unlockContentProcessingTask.execute(mapOf(MIGRATION_ID_VARIABLE_KEY to 1L))

        val migrationCaptor = argumentCaptor<Migration>()
        val modelsCaptor = argumentCaptor<List<MigratingModel>>()
        verify(mbocCategoryMigrationService, Mockito.times(1))
            .unlockContentProcessing(migrationCaptor.capture(), modelsCaptor.capture())

        val requestMigration = migrationCaptor.firstValue

        requestMigration shouldNotBe null
        requestMigration shouldBe testMigration
    }

    @Test
    fun `test mboc is invoked for deleted`() {
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
            status = MigratingModel.Status.ACTIVE,
            deleted = true
        )
        doReturn(listOf(testMigratingModel)).`when`(migratingModelRepository).findByMigrationId(eq(1L))

        unlockContentProcessingTask.execute(mapOf(MIGRATION_ID_VARIABLE_KEY to 1L))

        val migrationCaptor = argumentCaptor<Migration>()
        val modelsCaptor = argumentCaptor<List<MigratingModel>>()
        verify(mbocCategoryMigrationService, Mockito.times(1))
            .unlockContentProcessing(migrationCaptor.capture(), modelsCaptor.capture())

        val requestMigration = migrationCaptor.firstValue

        requestMigration shouldNotBe null
        requestMigration shouldBe testMigration
    }
}
