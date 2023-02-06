package ru.yandex.antifraud;

import java.io.BufferedReader;
import java.nio.file.Files;

import org.junit.Test;

import ru.yandex.antifraud.aggregates.Aggregates;
import ru.yandex.antifraud.aggregates.AggregatesBuilder;
import ru.yandex.antifraud.aggregates_v2.aggregates_config.AggregatorsConfigBuilder;
import ru.yandex.antifraud.aggregates_v2.aggregates_config.ImmutableAggregatorsConfig;
import ru.yandex.antifraud.data.CollapsedAggregates;
import ru.yandex.antifraud.data.CollapsedAggregatesResponse;
import ru.yandex.antifraud.data.Counters;
import ru.yandex.antifraud.data.ScoringData;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.lua.util.JsonUtils;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class CollapsedAggrsTest extends TestBase {
    public CollapsedAggrsTest() {
        super(false, 0L);
    }
    @Test
    public void test() throws Exception {
        final CollapsedAggregatesResponse collapsedAggregates;
        try (final BufferedReader reader =
                     Files.newBufferedReader(resource("collapsed_aggrs.json"))) {
            collapsedAggregates = new CollapsedAggregatesResponse(TypesafeValueContentHandler.parse(reader).asMap());
        }

        final ScoringData request;
        try (final BufferedReader reader =
                     Files.newBufferedReader(resource("collapsed_agrrs_request.json"))) {
            request = new ScoringData(TypesafeValueContentHandler.parse(reader).asMap());
        }

        final IniConfig aggregatorsIni = new IniConfig(resource("aggregates.conf"));
        final ImmutableAggregatorsConfig aggregatorsConfig = new AggregatorsConfigBuilder(aggregatorsIni).build();
        aggregatorsIni.checkUnusedKeys();

        final AggregatesBuilder builder = new AggregatesBuilder(
                request,
                Counters.EMPTY,
                null,
                aggregatorsConfig,
                true,
                true,
                true
        );

        for (CollapsedAggregates aggregates : collapsedAggregates.getAggregatesList()) {
            builder.consume(aggregates);
        }

        final Aggregates aggregates = builder.build();

        YandexAssert.check(
                new JsonChecker(Files.readString(resource("collapsed_aggrs_full.json"))),
                JsonType.NORMAL.toString(new JsonUtils.LuaAsJson(aggregates.structuredFull())));
    }
}
