package ru.yandex.market.mbo.category.orchestrator.service.migration

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.mbo.category.orchestrator.model.MigratingModel
import ru.yandex.market.mbo.category.orchestrator.repository.MigratingModelRepository
import ru.yandex.market.mbo.category.orchestrator.repository.MigrationRepository
import ru.yandex.market.mbo.category.orchestrator.service.event.MigratingModelEventService
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper

class MigrationServiceTest {

    private companion object {
        private const val MODEL_ID_1 = 100L
        private const val MODEL_ID_2 = 200L
        private const val MODEL_ID_3 = 300L

        private const val MIGRATION_ID = 10L

        private const val SOURCE_CATEGORY_ID = 1L
        private const val TARGET_CATEGORY_ID = 2L
    }

    private val migrationRepository: MigrationRepository = mock()
    private val migratingModelRepository: MigratingModelRepository = mock()
    private val migratingModelEventService: MigratingModelEventService = mock()

    private lateinit var migrationService: MigrationService

    @BeforeEach
    fun setUp() {
        migrationService = MigrationService(
            migrationRepository,
            migratingModelRepository,
            migratingModelEventService,
            TransactionHelper.MOCK
        )
    }

    @Test
    fun `test mark split MigratingModel finishing`() {
        val badSourceMigratingModel = MigratingModel(
            id = 1L,
            modelId = MODEL_ID_1,
            sourceModelId = MODEL_ID_1,
            modelType = "GURU",
            originalBroken = false,
            originalStrictChecksRequired = false,
            currentBroken = true,
            currentStrictChecksRequired = true,
            modelModifiedTs = 111L,
            sourceCategoryId = SOURCE_CATEGORY_ID,
            sourceCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
            targetCategoryId = TARGET_CATEGORY_ID,
            targetCategoryConfidence = MigratingModel.CategoryConfidence.CONTENT,
            migrationId = MIGRATION_ID,
            status = MigratingModel.Status.ACTIVE,
            deleted = true
        )

        val badTargetMigratingModel1 = badSourceMigratingModel.copy(
            id = 2,
            modelId = MODEL_ID_2,
            deleted = false
        )
        val badTargetMigratingModel2 = badSourceMigratingModel.copy(
            id = 3,
            modelId = MODEL_ID_3,
            deleted = false
        )

        reset(migratingModelRepository)
        doReturn(
            listOf(
                badSourceMigratingModel,
                badTargetMigratingModel1,
                badTargetMigratingModel2
            )
        )
            .`when`(migratingModelRepository).findByMigrationId(eq(MIGRATION_ID))

        migrationService.markFixedMigratingModelsFinishing(MIGRATION_ID)
        verify(migratingModelRepository, Mockito.never())
            .updateBatch(any<Collection<MigratingModel>>())

        val firstFixResult = listOf(
            badSourceMigratingModel,
            badTargetMigratingModel1.copy(
                currentBroken = false,
                currentStrictChecksRequired = false
            ),
            badTargetMigratingModel2
        )
        reset(migratingModelRepository)
        doReturn(firstFixResult)
            .`when`(migratingModelRepository).findByMigrationId(eq(MIGRATION_ID))
        doReturn(firstFixResult)
            .`when`(migratingModelRepository)
            .findByFilter(argThat { filter -> filter.sourceModelIds != null })

        migrationService.markFixedMigratingModelsFinishing(MIGRATION_ID)
        val migratingModelsCaptor = argumentCaptor<List<MigratingModel>>()
        verify(migratingModelRepository, Mockito.times(1))
            .updateBatch(migratingModelsCaptor.capture())
        val firstlyFixedModels = migratingModelsCaptor.lastValue
        firstlyFixedModels shouldHaveSize 1
        firstlyFixedModels.map { it.modelId } shouldContainExactlyInAnyOrder
            listOf(badTargetMigratingModel1.modelId)

        val secondFixResult = listOf(
            badSourceMigratingModel,
            badTargetMigratingModel1.copy(
                currentBroken = false,
                currentStrictChecksRequired = false,
                status = MigratingModel.Status.FINISHING
            ),
            badTargetMigratingModel2.copy(
                currentBroken = false,
                currentStrictChecksRequired = false
            )
        )
        reset(migratingModelRepository)
        doReturn(secondFixResult)
            .`when`(migratingModelRepository).findByMigrationId(eq(MIGRATION_ID))
        doReturn(firstFixResult)
            .`when`(migratingModelRepository)
            .findByFilter(argThat { filter -> filter.sourceModelIds != null })

        migrationService.markFixedMigratingModelsFinishing(MIGRATION_ID)
        verify(migratingModelRepository, Mockito.times(1))
            .updateBatch(migratingModelsCaptor.capture())
        val secondlyFixedModels = migratingModelsCaptor.lastValue
        secondlyFixedModels shouldHaveSize 2
        secondlyFixedModels.map { it.modelId } shouldContainExactlyInAnyOrder
            listOf(badTargetMigratingModel2.modelId, badSourceMigratingModel.modelId)
    }
}
