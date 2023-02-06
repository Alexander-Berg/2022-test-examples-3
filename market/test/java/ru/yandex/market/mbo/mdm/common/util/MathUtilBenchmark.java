package ru.yandex.market.mbo.mdm.common.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@Fork(3)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class MathUtilBenchmark {
    private static final long SEED = 987651234;
    private static final double MIN = 0.000_001;
    private static final double MAX = 1_000_000;
    @Param({"10", "50", "100", "1000"})
    private int valuesNumber;

    private List<BigDecimal> values;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(MathUtilBenchmark.class.getSimpleName())
            .build();

        new Runner(opt).run();
    }

    @Setup(Level.Trial)
    public void prepare() {
        Random random = new Random(SEED);
        this.values = random.doubles(valuesNumber, MIN, MAX)
            .mapToObj(BigDecimal::new)
            .collect(Collectors.toList());
    }

    @Benchmark
    public Optional<BigDecimal> checkGeometricMeanComputation() {
        return MathUtil.computeGeometricMeanUsingLogarithms(values);
    }

    //to compare
    @Benchmark
    public Optional<BigDecimal> justSumAllBigDecimalValues() {
        return values.stream().reduce(BigDecimal::add);
    }
}
