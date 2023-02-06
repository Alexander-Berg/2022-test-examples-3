package ru.yandex.market.pricelabs.misc;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.Assertions;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.yt.utils.Template;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 3, time = 5)
public class UtilsStringReplaceJMH {

    /*

contrib/java/org/antlr/ST4

Benchmark                                                      Mode  Cnt        Score         Error   Units
UtilsJMH.testReplaceStObjects                                 thrpt    3      139.860 ±     315.680   ops/s
UtilsJMH.testReplaceStObjects:·gc.alloc.rate                  thrpt    3      343.970 ±     358.887  MB/sec
UtilsJMH.testReplaceStObjects:·gc.alloc.rate.norm             thrpt    3  2852873.908 ± 3463111.461    B/op
...


Benchmark                                                            Mode  Cnt       Score        Error   Units
UtilsStringReplaceJMH.testReplaceEach                               thrpt    3  886590.622 ± 441341.606   ops/s
UtilsStringReplaceJMH.testReplaceEach:·gc.alloc.rate                thrpt    3     737.160 ±    366.720  MB/sec
UtilsStringReplaceJMH.testReplaceEach:·gc.alloc.rate.norm           thrpt    3     960.000 ±      0.001    B/op
UtilsStringReplaceJMH.testReplaceEach:·gc.churn.G1_Eden_Space       thrpt    3     742.105 ±    351.999  MB/sec
UtilsStringReplaceJMH.testReplaceEach:·gc.churn.G1_Eden_Space.norm  thrpt    3     966.645 ±    413.676    B/op
UtilsStringReplaceJMH.testReplaceEach:·gc.churn.G1_Old_Gen          thrpt    3       0.002 ±      0.008  MB/sec
UtilsStringReplaceJMH.testReplaceEach:·gc.churn.G1_Old_Gen.norm     thrpt    3       0.002 ±      0.009    B/op
UtilsStringReplaceJMH.testReplaceEach:·gc.count                     thrpt    3      67.000               counts
UtilsStringReplaceJMH.testReplaceEach:·gc.time                      thrpt    3      36.000                   ms
UtilsStringReplaceJMH.testSubstr                                    thrpt    3  443868.950 ± 256426.566   ops/s
UtilsStringReplaceJMH.testSubstr:·gc.alloc.rate                     thrpt    3    1162.755 ±    677.766  MB/sec
UtilsStringReplaceJMH.testSubstr:·gc.alloc.rate.norm                thrpt    3    3024.000 ±      0.001    B/op
UtilsStringReplaceJMH.testSubstr:·gc.churn.G1_Eden_Space            thrpt    3    1171.047 ±   1013.767  MB/sec
UtilsStringReplaceJMH.testSubstr:·gc.churn.G1_Eden_Space.norm       thrpt    3    3044.943 ±   1429.414    B/op
UtilsStringReplaceJMH.testSubstr:·gc.churn.G1_Old_Gen               thrpt    3       0.004 ±      0.032  MB/sec
UtilsStringReplaceJMH.testSubstr:·gc.churn.G1_Old_Gen.norm          thrpt    3       0.010 ±      0.083    B/op
UtilsStringReplaceJMH.testSubstr:·gc.count                          thrpt    3      73.000               counts
UtilsStringReplaceJMH.testSubstr:·gc.time                           thrpt    3      54.000                   ms
UtilsStringReplaceJMH.testTemplate                                  thrpt    3  420057.040 ± 239632.056   ops/s
UtilsStringReplaceJMH.testTemplate:·gc.alloc.rate                   thrpt    3    1121.206 ±    637.206  MB/sec
UtilsStringReplaceJMH.testTemplate:·gc.alloc.rate.norm              thrpt    3    3080.000 ±      0.001    B/op
UtilsStringReplaceJMH.testTemplate:·gc.churn.G1_Eden_Space          thrpt    3    1123.233 ±    503.659  MB/sec
UtilsStringReplaceJMH.testTemplate:·gc.churn.G1_Eden_Space.norm     thrpt    3    3086.404 ±   1171.630    B/op
UtilsStringReplaceJMH.testTemplate:·gc.churn.G1_Old_Gen             thrpt    3       0.003 ±      0.003  MB/sec
UtilsStringReplaceJMH.testTemplate:·gc.churn.G1_Old_Gen.norm        thrpt    3       0.009 ±      0.005    B/op
UtilsStringReplaceJMH.testTemplate:·gc.count                        thrpt    3      70.000               counts
UtilsStringReplaceJMH.testTemplate:·gc.time                         thrpt    3      51.000                   ms

// В итоге оказалось, что самое простое решение будет лучше всего работать.
     */


    static String replaceEach(Samples samples) {
        return replaceEach(samples.template,
                new String[]{"${offersTable}", "${shopId}", "${feedId}", "${status}", "${updatedAt}"},
                "[//home/market/testing/pricelabs/v2/shop_offer]",
                184672,
                145232,
                Status.ACTIVE.value(),
                samples.timestamp);
    }

    static String substr(Samples samples) {
        return new StringSubstitutor(Map.of(
                "offersTable", "[//home/market/testing/pricelabs/v2/shop_offer]",
                "shopId", 184672,
                "feedId", 145232,
                "status", Status.ACTIVE.value(),
                "updatedAt", samples.timestamp))
                .replace(samples.template);
    }

    static String template(Samples samples) {
        return samples.plTemplate.yt()
                .table("offersTable", "//home/market/testing/pricelabs/v2/shop_offer")
                .number("shopId", 184672)
                .number("feedId", 145232)
                .number("status", Status.ACTIVE.value())
                .number("updatedAt", samples.timestamp)
                .build();
    }

    @Benchmark
    public String testReplaceEach(Samples samples) {
        return replaceEach(samples);
    }

    @Benchmark
    public String testSubstr(Samples samples) {
        return substr(samples);
    }

    @Benchmark
    public String testTemplate(Samples samples) {
        return template(samples);
    }

    //@Benchmark
//    public void testPrepareSt(Samples samples, Blackhole blackhole) {
//        blackhole.consume(new ST(samples.template, '$', '$'));
//    }

    //@Benchmark
//    public void testReplaceStObjects(Samples samples, Blackhole blackhole) {
//        var template = samples.stTemplate;
//        for (int i = 0; i < 5; i++) {
//            template.add(samples.names[i], samples.valueObjects[i]);
//        }
//        blackhole.consume(samples.stTemplate.render());
//    }

    //@Benchmark
//    public void testReplaceStStrings(Samples samples, Blackhole blackhole) {
//        var template = samples.stTemplate;
//        for (int i = 0; i < 5; i++) {
//            template.add(samples.names[i], samples.valueStrings[i]);
//        }
//        blackhole.consume(samples.stTemplate.render());
//    }

    //    @Benchmark
//    public void testReplaceStPatternObjects(Samples samples, Blackhole blackhole) {
//        blackhole.consume(samples.stTemplate.addAggr(samples.allNames, samples.valueObjects).render());
//    }

    @State(Scope.Thread)
    public static class Samples {
        final long timestamp = System.currentTimeMillis();
        final String template = Utils.readResource("select-offers-sample.txt");
        final Template plTemplate = Template.fromText(template);
//        final ST stTemplate = new ST(template, '$', '$');

        public Samples() {
            var v1o = replaceEach(this);
            Assertions.assertEquals(v1o, substr(this));
            Assertions.assertEquals(v1o, template(this));
        }
    }

    public static String replaceEach(String text, String[] searchList, Object... replacement) {
        String[] strValues = new String[replacement.length];
        for (int i = 0; i < replacement.length; i++) {
            strValues[i] = String.valueOf(replacement[i]);
        }
        return replaceEach(text, searchList, strValues);
    }

    public static String replaceEach(String text, String[] searchList, String... replacement) {
        return StringUtils.replaceEach(text, searchList, replacement);
    }

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(UtilsStringReplaceJMH.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
