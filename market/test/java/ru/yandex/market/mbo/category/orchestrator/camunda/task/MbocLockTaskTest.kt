package ru.yandex.market.mbo.category.orchestrator.camunda.task

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.mbo.category.orchestrator.camunda.BpmnChangeCategoryConstants.MIGRATION_ID_VARIABLE_KEY
import ru.yandex.market.mbo.category.orchestrator.camunda.task.mboc.MbocLockTask
import ru.yandex.market.mbo.category.orchestrator.model.MigratingModel
import ru.yandex.market.mbo.category.orchestrator.model.Migration
import ru.yandex.market.mbo.category.orchestrator.repository.MigratingModelRepository
import ru.yandex.market.mbo.category.orchestrator.repository.MigrationRepository
import ru.yandex.market.mbo.category.orchestrator.service.mbo.ModelId
import ru.yandex.market.mbo.category.orchestrator.service.mboc.MbocCategoryMigrationService
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class MbocLockTaskTest {

    private companion object {
        private const val MODEL_ID_1 = 1L
        private const val MODEL_ID_2 = 2L

        private const val SOURCE_CATEGORY_ID = 1L
        private const val TARGET_CATEGORY_ID = 2L
    }

    private val migrationRepository: MigrationRepository = mock()
    private val migratingModelRepository: MigratingModelRepository = mock()

    private val mbocCategoryMigrationService: MbocCategoryMigrationService = mock()

    private lateinit var migratingModelIdGen: AtomicLong

    private lateinit var lockTask: MbocLockTask

    @BeforeEach
    fun setUp() {
        migratingModelIdGen = AtomicLong()
        lockTask = MbocLockTask(
            migrationRepository,
            migratingModelRepository,
            mbocCategoryMigrationService,
            TransactionHelper.MOCK
        )
    }

    @Test
    fun `test mboc is invoked`() {
        val migration = createMigration()
        doReturn(migration).`when`(migrationRepository).findById(eq(1L))

        val migratingModel = createMigratingModel(modelId = MODEL_ID_1)
        doReturn(listOf(migratingModel)).`when`(migratingModelRepository).findByMigrationId(eq(1L))

        lockTask.execute(mapOf(MIGRATION_ID_VARIABLE_KEY to 1L))

        val migrationCaptor = argumentCaptor<Migration>()
        val modelsCaptor = argumentCaptor<List<MigratingModel>>()
        verify(mbocCategoryMigrationService, Mockito.times(1))
            .lock(migrationCaptor.capture(), modelsCaptor.capture())

        val requestMigration = migrationCaptor.firstValue

        requestMigration shouldNotBe null
        requestMigration shouldBe migration

        val requestModels = modelsCaptor.firstValue
        requestModels shouldHaveSize 1
    }

    @Test
    fun `test lock only not locked models`() {
        val migration = createMigration()
        doReturn(migration).`when`(migrationRepository).findById(eq(1L))

        val alreadyLockedModel = createMigratingModel(modelId = MODEL_ID_1)
            .copy(mbocLockTs = Instant.now())
        val notLockedModel = createMigratingModel(modelId = MODEL_ID_2)
            .copy(mbocLockTs = null)
        doReturn(listOf(alreadyLockedModel, notLockedModel))
            .`when`(migratingModelRepository).findByMigrationId(eq(1L))

        lockTask.execute(
            mapOf(
                MIGRATION_ID_VARIABLE_KEY to 1L
            )
        )

        val migrationCaptor = argumentCaptor<Migration>()
        val modelsCaptor = argumentCaptor<List<MigratingModel>>()
        verify(mbocCategoryMigrationService, Mockito.times(1))
            .lock(migrationCaptor.capture(), modelsCaptor.capture())

        val requestMigration = migrationCaptor.firstValue

        requestMigration shouldNotBe null
        requestMigration shouldBe migration

        val requestModels = modelsCaptor.firstValue
        requestModels shouldHaveSize 1
        requestModels[0].modelId shouldBe MODEL_ID_2
    }

    private fun createMigration() =
        Migration(
            id = 1L,
            camundaProcessId = "camunda_process_id"
        )

    private fun createMigratingModel(modelId: ModelId) =
        MigratingModel(
            id = migratingModelIdGen.incrementAndGet(),
            modelId = modelId,
            sourceModelId = modelId,
            modelType = "GURU",
            originalBroken = false,
            originalStrictChecksRequired = false,
            modelModifiedTs = 111L,
            sourceCategoryId = SOURCE_CATEGORY_ID,
            sourceCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
            targetCategoryId = TARGET_CATEGORY_ID,
            targetCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
            status = MigratingModel.Status.ACTIVE
        )
}
