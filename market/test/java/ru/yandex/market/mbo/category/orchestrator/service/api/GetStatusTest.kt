package ru.yandex.market.mbo.category.orchestrator.service.api

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import ru.yandex.market.mbo.category.orchestrator.model.MigratingModel
import ru.yandex.market.mbo.category.orchestrator.model.Migration
import ru.yandex.market.mbo.category.orchestrator.model.MigrationRequest
import ru.yandex.market.mbo.category.orchestrator.model.MigrationRequestItem
import ru.yandex.mj.generated.server.model.MoveRequestStatus
import java.time.Instant
import java.time.ZoneOffset

class GetStatusTest : CategoryOrchestratorServiceBaseTest() {

    @Test
    fun `getMoveStatuses returns status`() {
        val migrationCreatedTs = Instant.now().minusSeconds(60)
        val migration = migrationRepository.insert(
            Migration(
                camundaProcessId = "fake_id",
                createdTs = migrationCreatedTs
            )
        )

        val mboMigrationStartTs = Instant.now().minusSeconds(30)
        val mboMigrationFinishTs = Instant.now().minusSeconds(10)
        val migratingModels = migratingModelRepository.insertBatch(
            MigratingModel(
                modelId = MODEL_ID_1,
                sourceModelId = MODEL_ID_1,
                originalBroken = false,
                originalStrictChecksRequired = false,
                sourceCategoryId = CATEGORY_ID_2,
                sourceCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
                targetCategoryId = CATEGORY_ID_1,
                targetCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
                mboMigrationStartTs = mboMigrationStartTs,
                mboMigrationFinishTs = mboMigrationFinishTs,
                modelModifiedTs = 111,
                modelType = "GURU",
                migrationId = migration.id
            ),
            MigratingModel(
                modelId = MODEL_ID_2,
                sourceModelId = MODEL_ID_2,
                originalBroken = false,
                originalStrictChecksRequired = false,
                sourceCategoryId = CATEGORY_ID_1,
                sourceCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
                targetCategoryId = CATEGORY_ID_2,
                targetCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
                mboMigrationStartTs = mboMigrationStartTs,
                mboMigrationFinishTs = mboMigrationFinishTs,
                modelModifiedTs = 222,
                modelType = "GURU",
                migrationId = migration.id
            ),
        )
        val request = migrationRequestRepository.insert(
            MigrationRequest(
                source = MigrationRequest.Source.OPERATOR
            )
        )
        migrationRequestItemRepository.insertBatch(
            migratingModels.map {
                MigrationRequestItem(
                    requestId = request.id,
                    modelId = it.modelId,
                    targetCategoryId = it.targetCategoryId,
                    migratingModelId = it.id
                )
            }
        )

        val migrationRequestIds = listOf(request.id)

        val getStatusResponse = categoryOrchestratorService.getMoveStatuses(migrationRequestIds)

        getStatusResponse shouldNotBe null

        val statusByRequestId = getStatusResponse.associateBy { it.requestId }
        statusByRequestId shouldContainKey request.id

        val requestStatus = statusByRequestId[request.id]
        requestStatus shouldNotBe null
        requestStatus!!
        requestStatus.requestId shouldBe request.id
        requestStatus.requestCreatedTs shouldBe request.createdTs.atOffset(ZoneOffset.UTC)
        requestStatus.status shouldBe MoveRequestStatus.ACTIVE
        requestStatus.requestModels shouldNotBe null
        requestStatus.requestModels
            .map { it.requestModelId } shouldContainExactlyInAnyOrder listOf(MODEL_ID_1, MODEL_ID_2)
    }
}
