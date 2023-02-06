package ru.yandex.direct.tracing.data;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

import ru.yandex.direct.utils.Checked;


@ParametersAreNonnullByDefault
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.SampleTime)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class DirectTraceInfoBenchmark {
    private final String hugeFixture;
    private final String averageFixture;

    public DirectTraceInfoBenchmark() {
        hugeFixture = Checked.get(() -> {
            try (InputStream stream = DirectTraceInfoBenchmark.class
                    .getResourceAsStream("/DirectTraceInfoBenchmarkHugeFixture.txt")) {
                return IOUtils.toString(stream, Charsets.UTF_8);
            }
        });
        if (StringUtils.isEmpty(hugeFixture)) {
            throw new IllegalStateException("Empty huge fixture");
        }
        averageFixture = Checked.get(() -> {
            try (InputStream stream = DirectTraceInfoBenchmark.class
                    .getResourceAsStream("/DirectTraceInfoBenchmarkAverageFixture.txt")) {
                return IOUtils.toString(stream, Charsets.UTF_8);
            }
        });
        if (StringUtils.isEmpty(averageFixture)) {
            throw new IllegalStateException("Empty median fixture");
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(DirectTraceInfoBenchmark.class.getSimpleName())
                .threads(2)
                .build();
        new Runner(opts).run();
    }

    @Benchmark
    public void benchmarkExtractHugeFixture() {
        DirectTraceInfo.extract(hugeFixture);
    }

    @Benchmark
    public void benchmarkExtractAverageFixture() {
        DirectTraceInfo.extract(averageFixture);
    }
}
