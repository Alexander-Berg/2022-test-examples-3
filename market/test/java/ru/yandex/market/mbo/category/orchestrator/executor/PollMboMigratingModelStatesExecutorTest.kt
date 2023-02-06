package ru.yandex.market.mbo.category.orchestrator.executor

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbo.category.orchestrator.AbstractFunctionalTest
import ru.yandex.market.mbo.category.orchestrator.model.MigratingModel
import ru.yandex.market.mbo.category.orchestrator.model.Migration
import ru.yandex.market.mbo.category.orchestrator.repository.MigratingModelRepository
import ru.yandex.market.mbo.category.orchestrator.repository.MigrationRepository
import ru.yandex.market.mbo.category.orchestrator.service.mbo.ModelStorageServiceMock
import ru.yandex.market.mbo.http.ModelCardApi
import ru.yandex.market.mbo.storage.StorageKeyValueService
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper

class PollMboMigratingModelStatesExecutorTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var storageKeyValueService: StorageKeyValueService

    @Autowired
    private lateinit var migrationRepository: MigrationRepository

    @Autowired
    private lateinit var migratingModelRepository: MigratingModelRepository

    @Autowired
    protected lateinit var modelStorageServiceMock: ModelStorageServiceMock

    private lateinit var executor: PollMboMigratingModelStatesExecutor

    @BeforeEach
    fun setUp() {
        modelStorageServiceMock.resetStrictOrBrokenData()
        executor =
            PollMboMigratingModelStatesExecutor(
                storageKeyValueService = storageKeyValueService,
                modelStorageService = modelStorageServiceMock,
                migratingModelRepository = migratingModelRepository,
                transactionHelper = TransactionHelper.MOCK
            )
    }

    @Test
    fun `when no mbo updates does nothing`() {
        val migration = migrationRepository.insert(
            Migration(
                camundaProcessId = "camunda_process_id"
            )
        )
        val migratingModel = migratingModelRepository.insert(
            MigratingModel(
                id = 1L,
                modelId = 100L,
                sourceModelId = 100L,
                migrationId = migration.id,
                status = MigratingModel.Status.ACTIVE,
                modelType = "GURU",
                originalBroken = true,
                originalStrictChecksRequired = true,
                modelModifiedTs = 111L,
                sourceCategoryId = 1L,
                sourceCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
                targetCategoryId = 2L,
                targetCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT
            )
        )

        executor.doRealJob(null)

        val migratingModelInDb = migratingModelRepository.findById(migratingModel.id)

        migratingModelInDb.currentBroken shouldBe null
        migratingModelInDb.currentStrictChecksRequired shouldBe null
        migratingModelInDb.status shouldBe MigratingModel.Status.ACTIVE
    }

    @Test
    fun `it updates flags`() {
        val migration = migrationRepository.insert(
            Migration(
                camundaProcessId = "camunda_process_id"
            )
        )
        val migratingModel = migratingModelRepository.insert(
            MigratingModel(
                id = 1L,
                modelId = 100L,
                sourceModelId = 100L,
                migrationId = migration.id,
                status = MigratingModel.Status.ACTIVE,
                modelType = "GURU",
                originalBroken = true,
                originalStrictChecksRequired = false,
                modelModifiedTs = 111L,
                sourceCategoryId = 1L,
                sourceCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
                targetCategoryId = 2L,
                targetCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT
            )
        )

        modelStorageServiceMock.addStrictOrBrokenData(
            ModelCardApi.GetStrictOrBrokenResponse.GetStrictOrBrokenResponseItem.newBuilder().apply {
                modelId = 100L
                broken = false
                strictChecksRequired = true
                timestamp = 1
            }.build()
        )

        executor.doRealJob(null)

        val migratingModelInDb = migratingModelRepository.findById(migratingModel.id)

        migratingModelInDb.currentBroken shouldBe false
        migratingModelInDb.currentStrictChecksRequired shouldBe true
        migratingModelInDb.status shouldBe MigratingModel.Status.ACTIVE
    }
}
