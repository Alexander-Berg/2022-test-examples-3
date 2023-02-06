package ru.yandex.market.mbo.category.orchestrator.camunda.task

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import ru.yandex.market.mbo.category.orchestrator.camunda.BpmnChangeCategoryConstants.MIGRATION_ID_VARIABLE_KEY
import ru.yandex.market.mbo.category.orchestrator.camunda.task.catalog.PollCatalogChangeCategoryResultsTask
import ru.yandex.market.mbo.category.orchestrator.model.MigratingModel
import ru.yandex.market.mbo.category.orchestrator.repository.MigratingModelRepository
import ru.yandex.market.mbo.category.orchestrator.service.mbo.ModelId
import ru.yandex.market.mbo.category.orchestrator.service.mbo.ModelStorageService
import ru.yandex.market.mbo.http.ModelCardApi
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper
import java.util.concurrent.atomic.AtomicLong

class PollCatalogChangeCategoryResultsTaskTest {

    private companion object {
        private const val MODEL_ID_1 = 1L
        private const val MODEL_ID_2 = 2L
        private const val MODEL_ID_3 = 3L

        private const val SOURCE_CATEGORY_ID = 1L
        private const val TARGET_CATEGORY_ID = 2L

        private const val MODEL_TRANSFER_ID = 2L
    }

    private val modelStorageService: ModelStorageService = mock()
    private val migratingModelRepository: MigratingModelRepository = mock()

    private lateinit var pollCatalogChangeCategoryResultsTask: PollCatalogChangeCategoryResultsTask

    private lateinit var migratingModelIdGen: AtomicLong

    @BeforeEach
    fun setUp() {
        migratingModelIdGen = AtomicLong()
        pollCatalogChangeCategoryResultsTask = PollCatalogChangeCategoryResultsTask(
            migratingModelRepository = migratingModelRepository,
            modelStorageService = modelStorageService,
            TransactionHelper.MOCK
        )
    }

    @Test
    fun `test mbo successfully changes category`() {
        val testMigratingModel = createMigratingModel(MODEL_ID_1)
        doReturn(listOf(testMigratingModel)).`when`(migratingModelRepository).findByMigrationId(eq(1L))

        val modelsTransferStatus = ModelCardApi.ModelsTransferStatusResponse.newBuilder().apply {
            status = ModelCardApi.ModelsTransferStatusResponse.Status.SUCCESS
            addModelTransferStatuses(
                ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.newBuilder().apply {
                    this.modelTransferId = 1
                    this.status = ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.Status.SUCCESS
                    this.sourceCategoryId = SOURCE_CATEGORY_ID
                    this.targetCategoryId = TARGET_CATEGORY_ID
                    addModels(
                        ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.ModelItem.newBuilder().apply {
                            this.modelId = MODEL_ID_1
                            this.status =
                                ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.ModelItem.Status.SUCCESS
                            this.broken = false
                            this.strictChecksRequired = false
                        }.build()
                    )
                }.build()
            )
        }.build()
        doReturn(modelsTransferStatus).`when`(modelStorageService).getModelsTransferStatus(any())

        doAnswer { invocation ->
            invocation.arguments[0]
        }.`when`(migratingModelRepository).updateBatch(any<Collection<MigratingModel>>())

        pollCatalogChangeCategoryResultsTask.execute(mapOf(MIGRATION_ID_VARIABLE_KEY to 1L))

        val migratingModelsCaptor = argumentCaptor<List<MigratingModel>>()
        verify(migratingModelRepository, Mockito.atLeastOnce())
            .updateBatch(migratingModelsCaptor.capture())

        val savedMigratingModels = migratingModelsCaptor.lastValue
        savedMigratingModels shouldHaveSize 1

        val migratingModel1 = savedMigratingModels.singleOrNull { it.modelId == MODEL_ID_1 }
        migratingModel1 shouldNotBe null
        migratingModel1!!.sourceModelId shouldBe MODEL_ID_1
        migratingModel1.status shouldBe MigratingModel.Status.ACTIVE
    }

    @Test
    fun `test mbo returns error`() {
        val testMigratingModel = createMigratingModel(MODEL_ID_1)
        doReturn(listOf(testMigratingModel)).`when`(migratingModelRepository).findByMigrationId(eq(1L))

        val modelsTransferStatus = ModelCardApi.ModelsTransferStatusResponse.newBuilder().apply {
            status = ModelCardApi.ModelsTransferStatusResponse.Status.FAILURE
            addModelTransferStatuses(
                ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.newBuilder().apply {
                    this.modelTransferId = 1
                    this.status = ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.Status.FAILURE
                    this.sourceCategoryId = SOURCE_CATEGORY_ID
                    this.targetCategoryId = TARGET_CATEGORY_ID
                    addModels(
                        ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.ModelItem.newBuilder().apply {
                            this.modelId = MODEL_ID_1
                            this.status =
                                ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.ModelItem.Status.FAILURE
                            this.statusDetails = "some MBO error"
                            this.broken = false
                            this.strictChecksRequired = false
                        }.build()
                    )
                }.build()
            )
        }.build()
        doReturn(modelsTransferStatus).`when`(modelStorageService).getModelsTransferStatus(any())

        doAnswer { invocation ->
            invocation.arguments[0]
        }.`when`(migratingModelRepository).updateBatch(any<Collection<MigratingModel>>())

        val exception = assertThrows<IllegalStateException> {
            pollCatalogChangeCategoryResultsTask.execute(mapOf(MIGRATION_ID_VARIABLE_KEY to 1L))
        }
        exception.message shouldContain "Got errors while calling modelStorageService.getModelsTransferStatus"

        val migratingModelsCaptor = argumentCaptor<List<MigratingModel>>()
        verify(migratingModelRepository, Mockito.atLeastOnce())
            .updateBatch(migratingModelsCaptor.capture())

        val savedMigratingModels = migratingModelsCaptor.lastValue
        savedMigratingModels shouldHaveSize 1

        val migratingModel1 = savedMigratingModels.singleOrNull { it.modelId == MODEL_ID_1 }
        migratingModel1 shouldNotBe null
        migratingModel1!!.sourceModelId shouldBe MODEL_ID_1
        migratingModel1.status shouldBe MigratingModel.Status.ACTIVE
        migratingModel1.errorMessage shouldContain "some MBO error"
    }

    @Test
    fun `test model split`() {
        val testMigratingModel = createMigratingModel(MODEL_ID_1)
        doReturn(listOf(testMigratingModel)).`when`(migratingModelRepository).findByMigrationId(eq(1L))

        val modelsTransferStatus = ModelCardApi.ModelsTransferStatusResponse.newBuilder().apply {
            status = ModelCardApi.ModelsTransferStatusResponse.Status.SUCCESS
            addModelTransferStatuses(
                ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.newBuilder().apply {
                    this.modelTransferId = 1
                    this.status = ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.Status.SUCCESS
                    this.sourceCategoryId = SOURCE_CATEGORY_ID
                    this.targetCategoryId = TARGET_CATEGORY_ID
                    addModels(
                        ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.ModelItem.newBuilder().apply {
                            this.modelId = MODEL_ID_2
                            this.sourceModelId = MODEL_ID_1
                            this.status =
                                ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.ModelItem.Status.SUCCESS
                            this.statusDetails = "some MBO error"
                            this.broken = false
                            this.strictChecksRequired = true
                        }.build()
                    )
                    addModels(
                        ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.ModelItem.newBuilder().apply {
                            this.modelId = MODEL_ID_3
                            this.sourceModelId = MODEL_ID_1
                            this.status =
                                ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer.ModelItem.Status.SUCCESS
                            this.statusDetails = "some MBO error"
                            this.broken = false
                            this.strictChecksRequired = true
                        }.build()
                    )
                }.build()
            )
        }.build()
        doReturn(modelsTransferStatus).`when`(modelStorageService).getModelsTransferStatus(any())

        doAnswer { invocation ->
            invocation.arguments[0]
        }.`when`(migratingModelRepository).updateBatch(any<Collection<MigratingModel>>())

        pollCatalogChangeCategoryResultsTask.execute(mapOf(MIGRATION_ID_VARIABLE_KEY to 1L))

        val migratingModelsCaptor = argumentCaptor<List<MigratingModel>>()
        verify(migratingModelRepository, Mockito.atLeastOnce())
            .updateBatch(migratingModelsCaptor.capture())

        val savedMigratingModels = migratingModelsCaptor.lastValue
        savedMigratingModels shouldHaveSize 3

        val migratingModel1 = savedMigratingModels.singleOrNull { it.modelId == MODEL_ID_1 }
        migratingModel1 shouldNotBe null
        migratingModel1!!.sourceModelId shouldBe MODEL_ID_1
        migratingModel1.status shouldBe MigratingModel.Status.ACTIVE
        migratingModel1.deleted shouldBe true

        val migratingModel2 = savedMigratingModels.singleOrNull { it.modelId == MODEL_ID_2 }
        migratingModel2 shouldNotBe null
        migratingModel2!!.sourceModelId shouldBe MODEL_ID_1
        migratingModel2.status shouldBe MigratingModel.Status.ACTIVE

        val migratingModel3 = savedMigratingModels.singleOrNull { it.modelId == MODEL_ID_3 }
        migratingModel3 shouldNotBe null
        migratingModel3!!.sourceModelId shouldBe MODEL_ID_1
        migratingModel3.status shouldBe MigratingModel.Status.ACTIVE
    }

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
            status = MigratingModel.Status.ACTIVE,
            modelTransferId = MODEL_TRANSFER_ID
        )
}
