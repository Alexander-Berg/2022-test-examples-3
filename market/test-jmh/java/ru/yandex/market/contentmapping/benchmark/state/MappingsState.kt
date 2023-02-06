package ru.yandex.market.contentmapping.benchmark.state

import com.fasterxml.jackson.core.type.TypeReference
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import ru.yandex.market.contentmapping.benchmark.helper.ResourceLoadHelper
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingWithRules

@State(Scope.Benchmark)
open class MappingsState {
    var mappings = listOf<ParamMappingWithRules>()

    @Setup
    open fun setup() {
        println("loading mappings")
        mappings = ResourceLoadHelper.loadData(resourceUrl, typeReference)
    }

    @TearDown
    open fun shutdown() {
        mappings = listOf()
    }

    companion object {
        const val resourceUrl = "https://proxy.sandbox.yandex-team.ru/2019053470"
        val typeReference = object: TypeReference<List<ParamMappingWithRules>>(){}
    }
}
