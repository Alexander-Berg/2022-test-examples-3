package ru.yandex.market.mdm.service.common_entity.service.common.metadata.mock

import ru.yandex.market.mdm.service.common_entity.model.UpdateContext
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.MetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.MetadataService.UpdateResults
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.Update

/**
 * Mock MetadataService, который хранит в себе только последний элемент и возвращает его независимо от значения в фильтре
 */
class OneElementMetadataService<T, F> : MetadataService<T, F> {

    var lastEntity: T? = null
    var lastUsedFilter: F? = null
    var lastCommitMessage: String? = null

    override fun findByFilter(filter: F): List<T> {
        lastUsedFilter = filter
        return listOfNotNull(lastEntity)
    }

    fun update(updates: List<T>, commitMessage: String?): UpdateResults<T> {
        lastEntity = updates.last()
        lastCommitMessage = commitMessage
        return UpdateResults(updates)
    }

    override fun update(updates: List<Update<T>>, context: UpdateContext): UpdateResults<T> {
        return this.update(updates.map { it.update }, context.commitMessage)
    }
}
