package ru.yandex.antifraud;

import java.nio.file.Files;

import org.junit.Test;

import ru.yandex.antifraud.aggregates.Aggregates;
import ru.yandex.antifraud.aggregates.Aggregator;
import ru.yandex.antifraud.aggregates.LongStat;
import ru.yandex.antifraud.aggregates.SetStringStat;
import ru.yandex.antifraud.aggregates.Stats;
import ru.yandex.antifraud.aggregates_v2.aggregates_config.AggregatorsConfigBuilder;
import ru.yandex.antifraud.aggregates_v2.aggregates_config.ImmutableAggregatorsConfig;
import ru.yandex.antifraud.currency.CurrencyRateMap;
import ru.yandex.antifraud.data.AggregatedData;
import ru.yandex.antifraud.data.CollapsedAggregatesResponse;
import ru.yandex.antifraud.data.Counters;
import ru.yandex.antifraud.data.ScoringData;
import ru.yandex.antifraud.rbl.RblData;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.lua.util.JsonUtils;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class AggregatesTest extends TestBase {
    public AggregatesTest() {
        super(false, 0L);
    }
    @Test
    public void test() throws Exception {
        final CurrencyRateMap currenciesRatesMap = CurrencyRateMap.make(resource("currencies_rate.json" +
                ".txt/0_upload_file"));

        final ScoringData cybertonicaRequest =
                new ScoringData(TypesafeValueContentHandler.parse(
                        Files.readString(resource("cybertonica-request.json"))).asMap(),
                        currenciesRatesMap);

        final AggregatedData aggregatedData = new AggregatedData();
        aggregatedData.process(TypesafeValueContentHandler.parse(
                Files.readString(resource("storage-aggregation-response.json"))).asMap());

        final RblData rblData = new RblData(TypesafeValueContentHandler.parse(
                Files.readString(resource("rbl-response.json"))).asMap());

        final IniConfig aggregatorsIni = new IniConfig(resource("aggregates.conf"));
        final ImmutableAggregatorsConfig aggregatorsConfig = new AggregatorsConfigBuilder(aggregatorsIni).build();
        aggregatorsIni.checkUnusedKeys();

        final Aggregates aggregates = aggregatedData.calcAggregates(
                aggregatorsConfig,
                cybertonicaRequest,
                CollapsedAggregatesResponse.EMPTY,
                Counters.EMPTY,
                rblData);

        YandexAssert.check(new JsonChecker(Files.readString(resource("structured_agregates.json"))),
                JsonType.NORMAL.toString(JsonUtils.luaAsJson(aggregates.structuredFull())));

        YandexAssert.check(new JsonChecker(Files.readString(resource("agregates.json"))),
                JsonType.NORMAL.toString(JsonUtils.luaAsJson(aggregates.asFull())));
    }

    @Test
    public void merging() throws Exception {
        final Stats stats = new Stats();

        {
            final LongStat stat = new LongStat();
            stat.add(4);
            stats.put(Aggregator.AMOUNT, stat);
        }
        {
            final SetStringStat stat = new SetStringStat("foo");
            stat.add("foobar");
            stat.add("foobar");
            stat.add("foo");
            stats.put(Aggregator.AUTHED_3DS_TNX_COUNT, stat);
        }
        {
            final LongStat stat = new LongStat();
            stat.add(2);
            stats.put(Aggregator.CARDS, stat);
        }
        YandexAssert.check(
                new JsonChecker(Files.readString(resource("before_merge.json"))),
                JsonType.NORMAL.toString(stats.asFullCleanJson()));

        final Stats anotherStats = new Stats();

        {
            final LongStat stat = new LongStat();
            stat.add(38);
            anotherStats.put(Aggregator.AMOUNT, stat);
        }
        {
            final SetStringStat stat = new SetStringStat((String) null);
            stat.add("bar");
            stat.add("bar");
            stat.add("foo");
            anotherStats.put(Aggregator.AUTHED_3DS_TNX_COUNT, stat);
        }
        {
            final LongStat stat = new LongStat();
            stat.add(1);
            anotherStats.put(Aggregator.BIN_COUNTRIES, stat);
        }
        YandexAssert.check(
                new JsonChecker(Files.readString(resource("another_stats.json"))),
                JsonType.NORMAL.toString(anotherStats.asFullCleanJson()));

        stats.mergeWith(anotherStats);

        YandexAssert.check(
                new JsonChecker(Files.readString(resource("merged_stats.json"))),
                JsonType.NORMAL.toString(stats.asFullCleanJson()));
    }
}
