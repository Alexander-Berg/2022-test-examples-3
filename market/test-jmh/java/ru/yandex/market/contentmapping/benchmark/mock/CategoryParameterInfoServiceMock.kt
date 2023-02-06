package ru.yandex.market.contentmapping.benchmark.mock

import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterInfo
import ru.yandex.market.contentmapping.kotlin.typealiases.CategoryId
import ru.yandex.market.contentmapping.kotlin.typealiases.ParamId
import ru.yandex.market.contentmapping.services.category.info.CategoryParameterInfoService

class CategoryParameterInfoServiceMock(
        private val data: Map<Long, Map<Long, CategoryParameterInfo>>
): CategoryParameterInfoService {

    override fun getCategoryParameters(categoryId: Long): Map<Long, CategoryParameterInfo> {
        return data.get(categoryId) ?: emptyMap()
    }

    override fun getCategoryParametersWithSubCategories(categoryId: CategoryId): Map<ParamId, CategoryParameterInfo> {
        return data[categoryId]
            ?.asSequence()
            ?.map { it.key to it.value }
            ?.toMap() ?: emptyMap()
    }

    override fun getOriginalCategoryParameters(categoryId: Long): Map<ParamId, CategoryParameterInfo> {
        throw NotImplementedError()
    }

    override fun getAllCategoryParameters(categoryIds: Collection<Long>): Map<Long, Map<Long, CategoryParameterInfo>> {
        throw NotImplementedError()
    }

    override fun getAllCategoryOriginalParameters(categoryIds: Collection<Long>): Map<CategoryId, Map<ParamId, CategoryParameterInfo>> {
        throw NotImplementedError()
    }

    override fun isMultivalue(parameterId: Long): Boolean {
        throw NotImplementedError()
    }

    override fun clear() {
        throw NotImplementedError()
    }

    override fun refreshCache(categoryId: Long) {
        throw NotImplementedError()
    }
}
