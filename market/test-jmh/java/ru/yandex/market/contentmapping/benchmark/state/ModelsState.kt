package ru.yandex.market.contentmapping.benchmark.state

import com.fasterxml.jackson.core.type.TypeReference
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import ru.yandex.market.contentmapping.benchmark.helper.ResourceLoadHelper
import ru.yandex.market.contentmapping.dto.model.ShopModel

@State(Scope.Benchmark)
open class ModelsState {
    var shopModels = listOf<ShopModel>()

    @Setup
    open fun setup() {
        println("loading models")
        shopModels = ResourceLoadHelper.loadData(resourceUrl, typeReference)
        println("loading models complete")
    }

    @TearDown
    open fun shutdown() {
        shopModels = listOf()
    }

    companion object {
        const val resourceUrl = "https://proxy.sandbox.yandex-team.ru/2019053933"
        val typeReference = object: TypeReference<List<ShopModel>>(){}
    }
}
