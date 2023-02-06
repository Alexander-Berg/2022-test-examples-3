package ru.yandex.market.pricelabs.search.matcher;

import java.io.IOException;
import java.util.Random;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.processing.ProcessingUtils;
import ru.yandex.misc.random.Random2;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 3, time = 5)
public class PatternMatchingJMH {

    /*

    На MacOS mid-2017

    1 самый сложный фильтр: ~ 384 500 офферов в секунду
    10 самых сложных фильтров: ~ 95 000 офферов в секунду

    1 некорректный фильтр, состоящий из ID: ~ 17 500 офферов в секунду
    1 некорректный фильтр, состоящий из ID после детектирования как Set: ~ 14 000 000 офферов в секунду

    10 некорректных фильтров, до преобразования: ~ 1 900 офферов в секунду
    10 некорректных фильтров, после преобразования: ~ 1 500 000 офферов в секунду

Benchmark                                                               Mode  Cnt       Score        Error   Units
PatternMatchingJMH.testSimpleApproach_10_vs_1                           thrpt    3   95022.463 ± 233165.227   ops/s
PatternMatchingJMH.testSimpleApproach_10_vs_1:·gc.alloc.rate            thrpt    3      ≈ 10⁻⁴               MB/sec
PatternMatchingJMH.testSimpleApproach_10_vs_1:·gc.alloc.rate.norm       thrpt    3       0.001 ±      0.003    B/op
PatternMatchingJMH.testSimpleApproach_10_vs_1:·gc.count                 thrpt    3         ≈ 0               counts
PatternMatchingJMH.testSimpleApproach_10_vs_1000                        thrpt    3     102.776 ±    186.361   ops/s
PatternMatchingJMH.testSimpleApproach_10_vs_1000:·gc.alloc.rate         thrpt    3      ≈ 10⁻⁴               MB/sec
PatternMatchingJMH.testSimpleApproach_10_vs_1000:·gc.alloc.rate.norm    thrpt    3       0.798 ±      1.500    B/op
PatternMatchingJMH.testSimpleApproach_10_vs_1000:·gc.count              thrpt    3         ≈ 0               counts
PatternMatchingJMH.testSimpleApproach_10_vs_100000                      thrpt    3       1.513 ±      1.078   ops/s
PatternMatchingJMH.testSimpleApproach_10_vs_100000:·gc.alloc.rate       thrpt    3      ≈ 10⁻⁴               MB/sec
PatternMatchingJMH.testSimpleApproach_10_vs_100000:·gc.alloc.rate.norm  thrpt    3      51.000 ±      0.001    B/op
PatternMatchingJMH.testSimpleApproach_10_vs_100000:·gc.count            thrpt    3         ≈ 0               counts
PatternMatchingJMH.testSimpleApproach_1_vs_1                            thrpt    3  384387.682 ± 446498.575   ops/s
PatternMatchingJMH.testSimpleApproach_1_vs_1:·gc.alloc.rate             thrpt    3      ≈ 10⁻⁴               MB/sec
PatternMatchingJMH.testSimpleApproach_1_vs_1:·gc.alloc.rate.norm        thrpt    3      ≈ 10⁻⁴                 B/op
PatternMatchingJMH.testSimpleApproach_1_vs_1:·gc.count                  thrpt    3         ≈ 0               counts
PatternMatchingJMH.testSimpleApproach_1_vs_1000                         thrpt    3     386.475 ±     72.330   ops/s
PatternMatchingJMH.testSimpleApproach_1_vs_1000:·gc.alloc.rate          thrpt    3      ≈ 10⁻⁴               MB/sec
PatternMatchingJMH.testSimpleApproach_1_vs_1000:·gc.alloc.rate.norm     thrpt    3       0.222 ±      0.183    B/op
PatternMatchingJMH.testSimpleApproach_1_vs_1000:·gc.count               thrpt    3         ≈ 0               counts
PatternMatchingJMH.testSimpleApproach_1_vs_100000                       thrpt    3       4.453 ±      0.972   ops/s
PatternMatchingJMH.testSimpleApproach_1_vs_100000:·gc.alloc.rate        thrpt    3      ≈ 10⁻⁴               MB/sec
PatternMatchingJMH.testSimpleApproach_1_vs_100000:·gc.alloc.rate.norm   thrpt    3      17.739 ±      0.001    B/op
PatternMatchingJMH.testSimpleApproach_1_vs_100000:·gc.count             thrpt    3         ≈ 0               counts


BEFORE
Benchmark                                                              Mode  Cnt      Score      Error   Units
PatternMatchingJMH.testSimpleApproach_bad_1_vs_1                      thrpt    3  17453.910 ± 1077.172   ops/s
PatternMatchingJMH.testSimpleApproach_bad_1_vs_1:·gc.alloc.rate       thrpt    3     ≈ 10⁻⁴             MB/sec
PatternMatchingJMH.testSimpleApproach_bad_1_vs_1:·gc.alloc.rate.norm  thrpt    3      0.005 ±    0.001    B/op
PatternMatchingJMH.testSimpleApproach_bad_1_vs_1:·gc.count            thrpt    3        ≈ 0             counts

Benchmark                                                               Mode  Cnt     Score      Error   Units
PatternMatchingJMH.testSimpleApproach_bad_10_vs_1                      thrpt    3  1718.288 ± 3995.740   ops/s
PatternMatchingJMH.testSimpleApproach_bad_10_vs_1:·gc.alloc.rate       thrpt    3    ≈ 10⁻⁴             MB/sec
PatternMatchingJMH.testSimpleApproach_bad_10_vs_1:·gc.alloc.rate.norm  thrpt    3     0.049 ±    0.112    B/op
PatternMatchingJMH.testSimpleApproach_bad_10_vs_1:·gc.count            thrpt    3       ≈ 0             counts

AFTER
Benchmark                                                              Mode  Cnt         Score         Error   Units
PatternMatchingJMH.testSimpleApproach_bad_1_vs_1                      thrpt    3  13922559.123 ± 3217367.050   ops/s
PatternMatchingJMH.testSimpleApproach_bad_1_vs_1:·gc.alloc.rate       thrpt    3        ≈ 10⁻⁴                MB/sec
PatternMatchingJMH.testSimpleApproach_bad_1_vs_1:·gc.alloc.rate.norm  thrpt    3        ≈ 10⁻⁵                  B/op
PatternMatchingJMH.testSimpleApproach_bad_1_vs_1:·gc.count            thrpt    3           ≈ 0                counts

Benchmark                                                               Mode  Cnt        Score        Error   Units
PatternMatchingJMH.testSimpleApproach_bad_10_vs_1                      thrpt    3  1497664.698 ± 469555.129   ops/s
PatternMatchingJMH.testSimpleApproach_bad_10_vs_1:·gc.alloc.rate       thrpt    3       ≈ 10⁻⁴               MB/sec
PatternMatchingJMH.testSimpleApproach_bad_10_vs_1:·gc.alloc.rate.norm  thrpt    3       ≈ 10⁻⁴                 B/op
PatternMatchingJMH.testSimpleApproach_bad_10_vs_1:·gc.count            thrpt    3          ≈ 0               counts

     */

    static final int MAX_FILTERS = 10;
    static final int MAX_OFFERS = 100_000;

    //CHECKSTYLE:OFF
    @Benchmark
    public void testSimpleApproach_bad_1_vs_1(InitFiltersBad init, Samples samples, Blackhole bh) {
        var random = init.random;

        var f = init.filters[0];
        var i = random.nextInt(MAX_OFFERS);

        var m = f.getFilterState().getQueryMatcher();
        var o = samples.offers[i];

        bh.consume(m.isMatched(o.getName_index()) || m.isMatched(o.getOffer_id_index()));
    }

    @Benchmark
    public void testSimpleApproach_bad_10_vs_1(InitFiltersBad init, Samples samples, Blackhole bh) {
        var random = init.random;

        for (Filter f : init.filters) {
            var m = f.getFilterState().getQueryMatcher();
            var o = samples.offers[random.nextInt(MAX_OFFERS)];
            bh.consume(m.isMatched(o.getName_index()) || m.isMatched(o.getOffer_id_index()));
        }
    }

    @Benchmark
    public void testSimpleApproach_1_vs_1(InitFiltersGood init, Samples samples, Blackhole bh) {
        var random = init.random;

        var f = init.filters[0];
        var i = random.nextInt(MAX_OFFERS);

        var m = f.getFilterState().getQueryMatcher();
        var o = samples.offers[i];

        bh.consume(m.isMatched(o.getName_index()) || m.isMatched(o.getOffer_id_index()));
    }

    @Benchmark
    public void testSimpleApproach_1_vs_1000(InitFiltersGood init, Samples samples, Blackhole bh) {
        var random = init.random;

        var f = init.filters[0];
        var m = f.getFilterState().getQueryMatcher();

        for (int i = 0; i < 1000; i++) {
            var o = samples.offers[random.nextInt(MAX_OFFERS)];
            bh.consume(m.isMatched(o.getName_index()) || m.isMatched(o.getOffer_id_index()));
        }
    }

    @Benchmark
    public void testSimpleApproach_10_vs_1(InitFiltersGood init, Samples samples, Blackhole bh) {
        var random = init.random;

        for (Filter f : init.filters) {
            var m = f.getFilterState().getQueryMatcher();
            var o = samples.offers[random.nextInt(MAX_OFFERS)];
            bh.consume(m.isMatched(o.getName_index()) || m.isMatched(o.getOffer_id_index()));
        }
    }

    @Benchmark
    public void testSimpleApproach_10_vs_1000(InitFiltersGood init, Samples samples, Blackhole bh) {
        var random = init.random;

        for (Filter f : init.filters) {
            var m = f.getFilterState().getQueryMatcher();

            for (int i = 0; i < 1000; i++) {
                var o = samples.offers[random.nextInt(MAX_OFFERS)];
                bh.consume(m.isMatched(o.getName_index()) || m.isMatched(o.getOffer_id_index()));
            }
        }
    }

    @Benchmark
    public void testSimpleApproach_1_vs_100000(InitFiltersGood init, Samples samples, Blackhole bh) {
        var random = init.random;

        var f = init.filters[0];
        var m = f.getFilterState().getQueryMatcher();

        for (var o : samples.offers) {
            bh.consume(m.isMatched(o.getName_index()) || m.isMatched(o.getOffer_id_index()));
        }
    }

    @Benchmark
    public void testSimpleApproach_10_vs_100000(InitFiltersGood init, Samples samples, Blackhole bh) {
        for (Filter f : init.filters) {
            var m = f.getFilterState().getQueryMatcher();

            for (var o : samples.offers) {
                bh.consume(m.isMatched(o.getName_index()) || m.isMatched(o.getOffer_id_index()));
            }
        }
    }
    //CHECKSTYLE:ON

    private static Filter[] loadFilters(String filename) {
        Filter[] filters;
        try (var stream = Utils.getResourceStream(filename)) {
            filters = Utils.getJsonMapper()
                    .readerFor(Filter.class)
                    .<Filter>readValues(stream)
                    .readAll()
                    .toArray(new Filter[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (filters.length != MAX_FILTERS) {
            throw new RuntimeException("Expect filters of " + MAX_FILTERS + " size, got " + filters.length);
        }
        for (Filter filter : filters) {
            ProcessingUtils.initFilter(filter);
            filter.getFilterState();
        }
        return filters;
    }


    @State(Scope.Thread)
    public static class InitFiltersGood {
        private final Filter[] filters;
        private final Random random;

        public InitFiltersGood() {
            filters = loadFilters("good-filters.json");
            random = new Random();
        }
    }

    @State(Scope.Thread)
    public static class InitFiltersBad {
        private final Filter[] filters;
        private final Random random;

        public InitFiltersBad() {
            filters = loadFilters("bad-filters.json");
            random = new Random();
        }
    }

    @State(Scope.Benchmark)
    public static class Samples {
        private final Offer[] offers;

        public Samples() {
            StringBuilder alphabet = new StringBuilder();
            for (int i = 'a'; i < 'z'; i++) {
                alphabet.append((char) i);
                alphabet.append(Character.toUpperCase((char) i));
            }
            for (int i = 'а'; i < 'я'; i++) {
                alphabet.append((char) i);
                alphabet.append(Character.toUpperCase((char) i));
            }
            for (int i = '0'; i < '9'; i++) {
                alphabet.append((char) i);
                alphabet.append(' ');
            }

            var r = Random2.threadLocal();

            this.offers = new Offer[MAX_OFFERS];
            for (int i = 0; i < offers.length; i++) {
                var offer = new Offer();
                offer.setShop_id(1);
                offer.setFeed_id(2);
                offer.setOffer_id(String.valueOf(i));
                offer.setName(r.nextString(30, alphabet));
                offer.normalizeFields();
                offers[i] = offer;
            }
        }
    }

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(PatternMatchingJMH.class.getSimpleName() + ".testSimpleApproach_bad_10_vs_1")
                .addProfiler(GCProfiler.class)
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
