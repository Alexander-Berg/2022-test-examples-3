package ru.yandex.market.pricelabs.tms;

import java.util.Objects;

import org.junit.jupiter.api.Assertions;
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
import ru.yandex.market.pricelabs.tms.services.market_report.model.recommendations.WrappedResult;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 3, time = 5)
public class ParseXmlVsJson {

    /*

Парсер JSON работает почти в 3 раза быстрее как на одиночных запросах, так и на батчах (в 50 элементов)

    На MacOS mid-2017
Benchmark                                                            Mode  Cnt       Score        Error   Units
ParseXmlVsJson.test_parse_batch_json                                thrpt    3    1936.273 ±    500.266   ops/s
ParseXmlVsJson.test_parse_batch_json:·gc.alloc.rate                 thrpt    3     371.249 ±     96.642  MB/sec
ParseXmlVsJson.test_parse_batch_json:·gc.alloc.rate.norm            thrpt    3  221336.043 ±      0.046    B/op
ParseXmlVsJson.test_parse_batch_json:·gc.churn.G1_Eden_Space        thrpt    3     376.532 ±    349.065  MB/sec
ParseXmlVsJson.test_parse_batch_json:·gc.churn.G1_Eden_Space.norm   thrpt    3  224436.734 ± 168505.629    B/op
ParseXmlVsJson.test_parse_batch_json:·gc.churn.G1_Old_Gen           thrpt    3       0.029 ±      0.077  MB/sec
ParseXmlVsJson.test_parse_batch_json:·gc.churn.G1_Old_Gen.norm      thrpt    3      17.465 ±     41.599    B/op
ParseXmlVsJson.test_parse_batch_json:·gc.count                      thrpt    3      34.000               counts
ParseXmlVsJson.test_parse_batch_json:·gc.time                       thrpt    3      20.000                   ms
ParseXmlVsJson.test_parse_batch_xml                                 thrpt    3     710.806 ±    266.543   ops/s
ParseXmlVsJson.test_parse_batch_xml:·gc.alloc.rate                  thrpt    3     439.406 ±    165.327  MB/sec
ParseXmlVsJson.test_parse_batch_xml:·gc.alloc.rate.norm             thrpt    3  713304.118 ±      0.061    B/op
ParseXmlVsJson.test_parse_batch_xml:·gc.churn.G1_Eden_Space         thrpt    3     443.255 ±    698.551  MB/sec
ParseXmlVsJson.test_parse_batch_xml:·gc.churn.G1_Eden_Space.norm    thrpt    3  719006.766 ± 918285.037    B/op
ParseXmlVsJson.test_parse_batch_xml:·gc.churn.G1_Old_Gen            thrpt    3       0.039 ±      0.029  MB/sec
ParseXmlVsJson.test_parse_batch_xml:·gc.churn.G1_Old_Gen.norm       thrpt    3      63.972 ±     53.617    B/op
ParseXmlVsJson.test_parse_batch_xml:·gc.count                       thrpt    3      40.000               counts
ParseXmlVsJson.test_parse_batch_xml:·gc.time                        thrpt    3      21.000                   ms
ParseXmlVsJson.test_parse_single_json                               thrpt    3   86744.020 ±  76204.935   ops/s
ParseXmlVsJson.test_parse_single_json:·gc.alloc.rate                thrpt    3     422.067 ±    371.682  MB/sec
ParseXmlVsJson.test_parse_single_json:·gc.alloc.rate.norm           thrpt    3    5616.001 ±      0.001    B/op
ParseXmlVsJson.test_parse_single_json:·gc.churn.G1_Eden_Space       thrpt    3     420.903 ±    347.304  MB/sec
ParseXmlVsJson.test_parse_single_json:·gc.churn.G1_Eden_Space.norm  thrpt    3    5601.181 ±    824.846    B/op
ParseXmlVsJson.test_parse_single_json:·gc.churn.G1_Old_Gen          thrpt    3       0.001 ±      0.005  MB/sec
ParseXmlVsJson.test_parse_single_json:·gc.churn.G1_Old_Gen.norm     thrpt    3       0.012 ±      0.060    B/op
ParseXmlVsJson.test_parse_single_json:·gc.count                     thrpt    3      38.000               counts
ParseXmlVsJson.test_parse_single_json:·gc.time                      thrpt    3      22.000                   ms
ParseXmlVsJson.test_parse_single_xml                                thrpt    3   26410.966 ±   1919.561   ops/s
ParseXmlVsJson.test_parse_single_xml:·gc.alloc.rate                 thrpt    3     485.185 ±     33.968  MB/sec
ParseXmlVsJson.test_parse_single_xml:·gc.alloc.rate.norm            thrpt    3   21200.003 ±      0.004    B/op
ParseXmlVsJson.test_parse_single_xml:·gc.churn.G1_Eden_Space        thrpt    3     487.413 ±    351.209  MB/sec
ParseXmlVsJson.test_parse_single_xml:·gc.churn.G1_Eden_Space.norm   thrpt    3   21299.615 ±  16740.288    B/op
ParseXmlVsJson.test_parse_single_xml:·gc.churn.G1_Old_Gen           thrpt    3       0.002 ±      0.020  MB/sec
ParseXmlVsJson.test_parse_single_xml:·gc.churn.G1_Old_Gen.norm      thrpt    3       0.070 ±      0.870    B/op
ParseXmlVsJson.test_parse_single_xml:·gc.count                      thrpt    3      44.000               counts
ParseXmlVsJson.test_parse_single_xml:·gc.time                       thrpt    3      22.000                   ms


     */

    //CHECKSTYLE:OFF
    @Benchmark
    public void test_parse_single_json(Configuration cfg, Blackhole blackhole) {
        blackhole.consume(Utils.fromJsonString(cfg.singleJson, WrappedResult.class));
    }

    @Benchmark
    public void test_parse_batch_json(Configuration cfg, Blackhole blackhole) {
        blackhole.consume(Utils.fromJsonString(cfg.batchJson, WrappedResult.class));
    }

    //CHECKSTYLE:OFF

    @State(Scope.Benchmark)
    public static class Configuration {
        private final String singleJson;

        private final String batchJson;

        public Configuration() {
            singleJson = Utils.readResource("report-single.json");

            batchJson = Utils.readResource("report-batch.json");

            check(1, singleJson);
            check(50, batchJson);
        }

        private void check(int expectSize, String json) {
            Assertions.assertEquals(expectSize, Objects.requireNonNull(Utils.fromJsonString(json, WrappedResult.class))
                    .getSearchResults().getResults().size());
        }
    }

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(ParseXmlVsJson.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
