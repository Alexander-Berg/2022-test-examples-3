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
 * Вычисляет среднее копирования запроса из читателя отсортированного запроса для блока из 10 запросов
 * (в одной операции)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx4G"})
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
public class QueryReaderToCharArrayBenchmark {
    private QueryFieldsSorter[] sorters;
    private char[] outputBuffer;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(QueryReaderToCharArrayBenchmark.class.getSimpleName())
                .forks(1)
                .threads(1)
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        Map<Integer, Map<TestQueryFileTypeEnum, String>> testFilesMap = TestQueriesResourcesLoader.loadTestQueries();
        sorters = new QueryFieldsSorter[testFilesMap.size()];
        int sorterIndex = 0;
        outputBuffer = new char[256 * 1024];
        for (Map.Entry<Integer, Map<TestQueryFileTypeEnum, String>> entry : testFilesMap.entrySet()) {
            Map<TestQueryFileTypeEnum, String> sourcesMap = entry.getValue();
            String query = sourcesMap.get(TestQueryFileTypeEnum.PARSED);
            QueryFieldsSorter sorter = new QueryFieldsSorter(query.toCharArray(), query.length());
            IQueryReader reader = sorter.sortQueryFields();
            reader.copyToBuffer(outputBuffer);
            sorters[sorterIndex++] = sorter;
        }
    }

    @Benchmark
    public void benchmarkSortedQueryCopyingByCopyToBuffer(Blackhole bh) {
        for (QueryFieldsSorter sorter : sorters) {
            IQueryReader reader = sorter.sortQueryFields();
            reader.copyToBuffer(outputBuffer);
            bh.consume(outputBuffer);
        }
    }

    @Benchmark
    public void benchmarkSortedQueryCopyingByHasNextChar(Blackhole bh) {
        for (QueryFieldsSorter sorter : sorters) {
            IQueryReader reader = sorter.sortQueryFields();
            int pos = 0;
            while (reader.hasNextChar()) {
                outputBuffer[pos++] = reader.getNextChar();
            }
            bh.consume(outputBuffer);
        }
    }

}
