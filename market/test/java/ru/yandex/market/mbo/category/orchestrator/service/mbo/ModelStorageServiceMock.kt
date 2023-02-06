package ru.yandex.market.mbo.category.orchestrator.service.mbo

import ru.yandex.market.mbo.category.orchestrator.model.MigratingModel
import ru.yandex.market.mbo.http.ModelCardApi
import ru.yandex.market.mbo.http.ModelCardApi.ModelsTransferResponse.ModelsTransferRequest
import ru.yandex.market.mbo.http.ModelCardApi.ModelsTransferResponse.ModelsTransferRequest.ModelsTransferRequestStatus
import ru.yandex.market.mbo.http.ModelCardApi.ModelsTransferStatusResponse.ModelsTransfer
import ru.yandex.market.mbo.http.ModelStorage
import java.util.concurrent.atomic.AtomicLong

class ModelStorageServiceMock : ModelStorageService {

    private val models = mutableMapOf<Long, ModelStorage.Model>()
    private val modelTransferIdGen = AtomicLong()

    private val strictOrBrokenData =
        mutableListOf<ModelCardApi.GetStrictOrBrokenResponse.GetStrictOrBrokenResponseItem>()

    fun addModel(model: ModelStorage.Model): ModelStorageServiceMock {
        models[model.id] = model
        return this
    }

    fun removeModel(model: ModelStorage.Model): ModelStorageServiceMock {
        models.remove(model.id)
        return this
    }

    fun addStrictOrBrokenData(
        vararg items: ModelCardApi.GetStrictOrBrokenResponse.GetStrictOrBrokenResponseItem
    ) = addStrictOrBrokenData(items.asList())

    fun addStrictOrBrokenData(
        items: Collection<ModelCardApi.GetStrictOrBrokenResponse.GetStrictOrBrokenResponseItem>
    ): ModelStorageServiceMock {
        strictOrBrokenData.addAll(items)
        return this
    }

    fun resetStrictOrBrokenData(): ModelStorageServiceMock {
        strictOrBrokenData.clear()
        return this
    }

    override fun requestModelsTransfer(
        migratingModels: Collection<MigratingModel>
    ): List<Pair<ModelsTransferRequest, Collection<MigratingModel>>> {
        return migratingModels
            .groupBy {
                Triple(
                    it.sourceCategoryId,
                    it.targetCategoryId,
                    it.targetCategoryConfidence
                )
            }
            .map {
                val migratingModels = it.value
                val modelsTransferRequest = ModelsTransferRequest.newBuilder().apply {
                    modelTransferId = modelTransferIdGen.incrementAndGet()
                    requestStatus = ModelsTransferRequestStatus.SUCCESS
                }.build()
                modelsTransferRequest to migratingModels
            }
    }

    override fun getModelsTransferStatus(
        migratingModels: Collection<MigratingModel>
    ): ModelCardApi.ModelsTransferStatusResponse {
        val migratingModelByModelTransferId = migratingModels
            .filter { it.modelTransferId != null }
            .groupBy { it.modelTransferId }

        return ModelCardApi.ModelsTransferStatusResponse.newBuilder().apply {
            addAllModelTransferStatuses(
                migratingModelByModelTransferId.map { entry ->
                    val modelTransferId = entry.key
                    val migratingModelsForTransfer = entry.value
                    ModelsTransfer.newBuilder()
                        .apply {
                            this.modelTransferId = modelTransferId!!
                            this.status = ModelsTransfer.Status.SUCCESS
                            addAllModels(
                                migratingModelsForTransfer.map { migratingModel ->
                                    ModelsTransfer.ModelItem.newBuilder()
                                        .apply {
                                            this.modelId = migratingModel.modelId
                                            this.status = ModelsTransfer.ModelItem.Status.SUCCESS
                                            this.broken = false
                                            this.strictChecksRequired = false
                                        }.build()
                                }
                            )
                        }.build()
                })
        }.build()
    }

    override fun findModelsWithParents(modelIds: Collection<ModelId>): Map<ModelId, ModelWithParent> {
        return modelIds
            .distinct()
            .mapNotNull { models[it] }
            .associateBy(
                { it.id },
                { ModelWithParent(model = it) }
            )
    }

    override fun findModels(modelIds: Collection<ModelId>): Map<ModelId, ModelStorage.Model> {
        return modelIds
            .distinct()
            .mapNotNull { models[it] }
            .associateBy { it.id }
    }

    override fun getStrictOrBroken(fromTimestamp: Long, limit: Int): ModelCardApi.GetStrictOrBrokenResponse {
        val resultItems = strictOrBrokenData
            .filter { it.timestamp >= fromTimestamp }
            .sortedBy { it.timestamp }
            .take(limit)
        return ModelCardApi.GetStrictOrBrokenResponse.newBuilder().apply {
            addAllModels(resultItems)
        }.build()
    }
}
