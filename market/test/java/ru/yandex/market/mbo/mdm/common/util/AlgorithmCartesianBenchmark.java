package ru.yandex.market.mbo.mdm.common.util;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import ru.yandex.market.mdm.lib.util.AlgorithmKt;

@Fork(2)
@Warmup(iterations = 3)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class AlgorithmCartesianBenchmark {
    // Симулируем сохранение в силвер сторадж
    private static final List<List<Integer>> TABLE = List.of(
        // Аптека = 500 сервисов
        IntStream.range(Integer.MAX_VALUE - 1000, Integer.MAX_VALUE - 500)
            .boxed()
            .collect(Collectors.toList()),
        List.of(1),
        List.of(1),
        List.of(1)
    );

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(AlgorithmCartesianBenchmark.class.getSimpleName())
            .build();

        new Runner(opt).run();
    }

    @Benchmark
    public List<List<Integer>> cartesian() {
        return AlgorithmKt.cartesian(TABLE);
    }
}
