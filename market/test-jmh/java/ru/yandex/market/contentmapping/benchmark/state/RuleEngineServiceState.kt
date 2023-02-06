package ru.yandex.market.contentmapping.benchmark.state

import com.fasterxml.jackson.core.type.TypeReference
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import ru.yandex.market.contentmapping.benchmark.helper.ResourceLoadHelper
import ru.yandex.market.contentmapping.benchmark.service.RuleEngineServiceBuilder
import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterInfo
import ru.yandex.market.contentmapping.services.rules.v2.RuleEngineService
import java.util.HashMap

@State(Scope.Benchmark)
open class RuleEngineServiceState {
    var ruleEngineService: RuleEngineService? = null

    @Setup
    open fun setup() {
        println("building RuleEngineService")
        ruleEngineService = RuleEngineServiceBuilder.buildRuleEngineService(
                ResourceLoadHelper.loadData(resourceUrl, typeReference)
        )
        println("building RuleEngineService complete")
    }

    @TearDown
    open fun shutdown() {
        ruleEngineService = null
    }

    companion object {
        const val resourceUrl = "https://proxy.sandbox.yandex-team.ru/2019055346"
        val typeReference = object: TypeReference<HashMap<Long, HashMap<Long, CategoryParameterInfo>>>(){}
    }
}
