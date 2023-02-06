package ru.yandex.market.contentmapping.benchmark.mock

import ru.yandex.market.contentmapping.dto.data.category.CategoryInfo
import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterInfo
import ru.yandex.market.contentmapping.kotlin.typealiases.CategoryId
import ru.yandex.market.contentmapping.kotlin.typealiases.ParamId
import ru.yandex.market.contentmapping.services.ComplexCategoryDataService

class ComplexCategoryDataServiceMock(
        private val data: Map<CategoryId, Map<ParamId, CategoryParameterInfo>>
): ComplexCategoryDataService {

    override fun getCategoryParameterInfos(
        categoryId: CategoryId,
        joinChildParams: Boolean
    ): Map<ParamId, CategoryParameterInfo> {
        return data.get(categoryId) ?: emptyMap()
    }

    override fun getCategoryInfo(id: CategoryId): CategoryInfo? {
        throw NotImplementedError()
    }
}
