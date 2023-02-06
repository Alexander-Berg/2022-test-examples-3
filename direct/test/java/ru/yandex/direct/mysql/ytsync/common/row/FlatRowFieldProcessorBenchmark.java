package ru.yandex.direct.mysql.ytsync.common.row;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(2)
public class FlatRowFieldProcessorBenchmark {
    Object[] vals = Arrays.stream(FlatRowFieldProcessorTest.params())
            .map(it -> it[0])
            .toArray();

    @Benchmark
    public Object[] benchmarkOldVersion() {
        Object[] ret = new Object[vals.length];
        for (int i = 0; i < vals.length; i++) {
            ret[i] = FlatRowFieldProcessorTest.kosherBuildNode(vals[i]);
        }
        return ret;
    }

    @Benchmark
    public Object[] benchmarkNewVersion() {
        Object[] ret = new Object[vals.length];
        for (int i = 0; i < vals.length; i++) {
            ret[i] = FlatRowFieldProcessor.buildNode(vals[i]);
        }
        return ret;
    }

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(FlatRowFieldProcessorBenchmark.class.getSimpleName())
                .threads(2)
                .build();
        new Runner(opts).run();
    }
}
