package ru.yandex.direct.mysql.slowlog.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Вычисляет среднее время парсинга блока из 15 записей slow query лога mySQL 5.7 с перконовскими расширениями
 * (в одной операции)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx4G"})
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
public class SlowLogRawRecordParserBenchmark {
    private static String[][] testData;
    private static Map<String, String> paramsMap;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SlowLogRawRecordParserBenchmark.class.getSimpleName())
                .forks(1)
                .threads(1)
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        testData = TestDataResourcesLoader.loadTestData();
        paramsMap = new HashMap<>(40);
    }

    @Benchmark
    public void benchmarkQueryParsing(Blackhole bh) {
        for (int i = 0; i < testData[0].length; i++) {
            ParsedSlowLogRawRecord record = SlowLogRawRecordParser.parseRawRecordText(testData[0][i], paramsMap);
            bh.consume(record);
        }
    }

}
