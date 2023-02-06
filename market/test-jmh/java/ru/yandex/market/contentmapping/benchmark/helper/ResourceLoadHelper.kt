package ru.yandex.market.contentmapping.benchmark.helper

import com.fasterxml.jackson.core.type.TypeReference
import ru.yandex.market.contentmapping.utils.JsonUtils
import java.net.URL

/**
 * Use ru.yandex.market.contentmapping.controllers.TasksController.createJmhMockData for create new resources
 */

class ResourceLoadHelper {
    companion object {
        fun <T> loadData(url: String, typeRef: TypeReference<T>): T {
            return JsonUtils.commonObjectMapper().readValue(URL(url), typeRef)
        }
    }
}
