package ru.yandex.market.contentmapping.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import ru.yandex.market.contentmapping.benchmark.state.MappingsState;
import ru.yandex.market.contentmapping.benchmark.state.ModelsState;
import ru.yandex.market.contentmapping.benchmark.state.RuleEngineServiceState;

@Warmup(iterations = 5, time = 10)
@Measurement(iterations = 10, time = 10)
@Fork(value = 1)
public class RuleEngineBenchmark {

    @Benchmark
    public void applyAllRulesBenchmark(
        RuleEngineServiceState ruleEngineServiceState,
        ModelsState modelsState,
        MappingsState mappingsState,
        Blackhole blackhole
    ) {
        RuleEngineRunner.applyAllRulesBenchmark(ruleEngineServiceState, modelsState, mappingsState, blackhole);
    }
}
