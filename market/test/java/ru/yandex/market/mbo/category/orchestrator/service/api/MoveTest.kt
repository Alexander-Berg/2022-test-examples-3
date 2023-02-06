package ru.yandex.market.mbo.category.orchestrator.service.api

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import ru.yandex.market.mbo.category.orchestrator.model.MigratingModel
import ru.yandex.mj.generated.server.model.ModelStatus
import ru.yandex.mj.generated.server.model.MoveRequestDto
import ru.yandex.mj.generated.server.model.MoveRequestItemDto
import ru.yandex.mj.generated.server.model.MoveResponseDto
import ru.yandex.mj.generated.server.model.MoveResponseItemDto
import ru.yandex.mj.generated.server.model.RequestSource

class MoveTest : CategoryOrchestratorServiceBaseTest() {

    @Test
    fun `test move single model request successfully processed`() {
        val moveRequest = MoveRequestDto().apply {
            source = RequestSource.OPERATOR
            requestItems = listOf(
                MoveRequestItemDto().apply {
                    targetCategoryId = CATEGORY_ID_2
                    modelIds = listOf(MODEL_ID_1)
                }
            )
        }
        val moveResponse = categoryOrchestratorService.move(moveRequest)

        moveResponse.status shouldBe MoveResponseDto.StatusEnum.OK
        moveResponse.requestId shouldBeGreaterThan 0

        val savedRequest = migrationRequestRepository.findById(moveResponse.requestId)
        savedRequest shouldNotBe null

        val requestItems = migrationRequestItemRepository.findByRequestIds(savedRequest.id)
        requestItems shouldHaveSize 1

        val requestItemModelLinks = migrationRequestItemModelLinkRepository.findByRequestItemId(requestItems[0].id)
        requestItemModelLinks shouldHaveSize 1

        val migratingModel = migratingModelRepository.findById(requestItemModelLinks[0].migratingModelId)
        migratingModel shouldNotBe null
        migratingModel.modelId shouldBe MODEL_ID_1
        migratingModel.sourceCategoryId shouldBe CATEGORY_ID_1
        migratingModel.targetCategoryId shouldBe CATEGORY_ID_2
        migratingModel.modelModifiedTs shouldBe MODEL_MODIFIED_TS_1
        migratingModel.status shouldBe MigratingModel.Status.NEW
    }

    @Test
    fun `test move multi model request successfully processed`() {
        val moveRequest = MoveRequestDto().apply {
            source = RequestSource.OPERATOR
            requestItems = listOf(
                MoveRequestItemDto().apply {
                    targetCategoryId = CATEGORY_ID_2
                    modelIds = listOf(MODEL_ID_1, MODEL_ID_2)
                }
            )
        }
        val moveResponse = categoryOrchestratorService.move(moveRequest)

        moveResponse.status shouldBe MoveResponseDto.StatusEnum.OK
        moveResponse.requestId shouldBeGreaterThan 0

        val savedRequest = migrationRequestRepository.findById(moveResponse.requestId)
        savedRequest shouldNotBe null

        val requestItems = migrationRequestItemRepository.findByRequestIds(savedRequest.id)
        requestItems shouldHaveSize 2
        requestItems.map { it.modelId } shouldContainExactlyInAnyOrder listOf(MODEL_ID_1, MODEL_ID_2)

        val requestItemModelLinks = migrationRequestItemModelLinkRepository.findAll()
        requestItemModelLinks shouldHaveSize 2

        val migratingModels = migratingModelRepository.findAll()
        migratingModels.map { it.modelId } shouldContainExactlyInAnyOrder listOf(MODEL_ID_1, MODEL_ID_2)
    }

    @Test
    fun `test move model not found error`() {
        val moveRequest = MoveRequestDto().apply {
            source = RequestSource.OPERATOR
            requestItems = listOf(
                MoveRequestItemDto().apply {
                    targetCategoryId = CATEGORY_ID_2
                    modelIds = listOf(NOT_EXISTING_MODEL_ID)
                }
            )
        }
        val moveResponse = categoryOrchestratorService.move(moveRequest)

        moveResponse shouldBe
            MoveResponseDto().apply {
                status = MoveResponseDto.StatusEnum.ERROR
                results = listOf(
                    MoveResponseItemDto().apply {
                        targetCategoryId = CATEGORY_ID_2
                        modelResults = listOf(
                            ModelStatus().apply {
                                modelId = NOT_EXISTING_MODEL_ID
                                status = ModelStatus.StatusEnum.ERROR
                                statusMessage = "request model not found 999"
                            }
                        )
                    }
                )
            }
    }
}
