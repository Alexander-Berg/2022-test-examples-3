package ru.yandex.direct.sql.normalizer;

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
 * Вычисляет среднее время парсинга, сортировки, сортировки с чтением результата, и нормализации блока из 11 запросов
 * (в одной операции)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx4G"})
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
public class NormalizerBenchmark {
    private Map<Integer, Map<TestQueryFileTypeEnum, String>> testFilesMap;
    private QueryNormalizer normalizer;
    private char[] inputBuffer;
    private char[] outputBuffer;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(NormalizerBenchmark.class.getSimpleName())
                .forks(1)
                .threads(1)
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        testFilesMap = TestQueriesResourcesLoader.loadTestQueries();
        normalizer = TestQueriesResourcesLoader.getNormalizer();
        inputBuffer = new char[256 * 1024];
        outputBuffer = new char[256 * 1024];
    }

    @Benchmark
    public void benchmarkQueryParsing(Blackhole bh) {
        for (Map.Entry<Integer, Map<TestQueryFileTypeEnum, String>> entry : testFilesMap.entrySet()) {
            Map<TestQueryFileTypeEnum, String> sourcesMap = entry.getValue();
            String query = sourcesMap.get(TestQueryFileTypeEnum.ORIGINAL);
            int normalFormLength = QueryParser.parseQuery(query, inputBuffer);
            bh.consume(normalFormLength);
        }
    }

    @Benchmark
    public void benchmarkQueryFieldsSorting(Blackhole bh) {
        for (Map.Entry<Integer, Map<TestQueryFileTypeEnum, String>> entry : testFilesMap.entrySet()) {
            Map<TestQueryFileTypeEnum, String> sourcesMap = entry.getValue();
            String query = sourcesMap.get(TestQueryFileTypeEnum.ORIGINAL);
            int normalFormLength = QueryParser.parseQuery(query, inputBuffer);
            QueryFieldsSorter sorter = new QueryFieldsSorter(inputBuffer, normalFormLength);
            IQueryReader reader = sorter.sortQueryFields();
            bh.consume(reader);
        }
    }

    @Benchmark
    public void benchmarkQueryFieldsSortingAndCopying(Blackhole bh) {
        for (Map.Entry<Integer, Map<TestQueryFileTypeEnum, String>> entry : testFilesMap.entrySet()) {
            Map<TestQueryFileTypeEnum, String> sourcesMap = entry.getValue();
            String query = sourcesMap.get(TestQueryFileTypeEnum.ORIGINAL);
            int normalFormLength = QueryParser.parseQuery(query, inputBuffer);
            QueryFieldsSorter sorter = new QueryFieldsSorter(inputBuffer, normalFormLength);
            IQueryReader reader = sorter.sortQueryFields();
            reader.copyToBuffer(outputBuffer);
            bh.consume(outputBuffer);
        }
    }

    @Benchmark
    public void benchmarkQueryNormalizing(Blackhole bh) {
        for (Map.Entry<Integer, Map<TestQueryFileTypeEnum, String>> entry: testFilesMap.entrySet()) {
            Map<TestQueryFileTypeEnum, String> sourcesMap = entry.getValue();
            String query = sourcesMap.get(TestQueryFileTypeEnum.ORIGINAL);
            String result = normalizer.normalizeQuery(query);
            bh.consume(result);
        }
    }

}
