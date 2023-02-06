package ru.yandex.market.contentmapping.benchmark

import org.openjdk.jmh.infra.Blackhole
import ru.yandex.market.contentmapping.benchmark.state.MappingsState
import ru.yandex.market.contentmapping.benchmark.state.ModelsState
import ru.yandex.market.contentmapping.benchmark.state.RuleEngineServiceState

class RuleEngineRunner {
    companion object {
        @JvmStatic
        fun applyAllRulesBenchmark(
                ruleEngineServiceState: RuleEngineServiceState,
                modelsState: ModelsState,
                mappingsState: MappingsState,
                blackhole: Blackhole
        ) {
            val ruleEngineService = ruleEngineServiceState.ruleEngineService
            val modelsSequence = modelsState.shopModels.asSequence()
            val mappings = mappingsState.mappings
            ruleEngineService!!.applyAllRules(modelsSequence, mappings).forEach { blackhole.consume(it) }
        }
    }
}
