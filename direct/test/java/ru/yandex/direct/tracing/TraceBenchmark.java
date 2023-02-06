package ru.yandex.direct.tracing;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import ru.yandex.direct.tracing.real.RealTrace;

/**
 * Created by snaury on 25/04/16.
 */
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 6)
@BenchmarkMode(Mode.SampleTime)
@Fork(2)
public class TraceBenchmark {
    private final Trace trace = RealTrace.builder().build();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    @Benchmark
    public void benchmarkSingle() {
        trace.profile("myfunc", "mytags", 0).close();
    }

    @Benchmark
    public void benchmarkDummy() {
        DummyTrace.instance().profile("myfunc", "mytags", 0).close();
    }

    @Benchmark
    public void benchmarkNested() {
        try (TraceProfile profile = trace.profile("myfunc1", "mytags1", 0)) {
            trace.profile("myfunc2", "mytags2", 0).close();
        }
    }

    //@Benchmark
    public void benchmarkCpuTimes(Blackhole bh) {
        bh.consume(threadMXBean.getCurrentThreadCpuTime());
        bh.consume(threadMXBean.getCurrentThreadUserTime());
    }

    //@Benchmark
    public void benchmarkUserTime(Blackhole bh) {
        bh.consume(threadMXBean.getCurrentThreadUserTime());
    }

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(TraceBenchmark.class.getSimpleName())
                .threads(2)
                .build();
        new Runner(opts).run();
    }
}
