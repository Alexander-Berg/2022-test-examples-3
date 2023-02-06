package ru.yandex.antifraud;

import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import core.org.luaj.vm2.Globals;
import jse.org.luaj.vm2.lib.jse.JsePlatform;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.antifraud.aggregates.Aggregates;
import ru.yandex.antifraud.aggregates_v2.aggregates_config.AggregatorsConfigBuilder;
import ru.yandex.antifraud.aggregates_v2.aggregates_config.ImmutableAggregatorsConfig;
import ru.yandex.antifraud.artefacts.Artefacts;
import ru.yandex.antifraud.artefacts.Resolution;
import ru.yandex.antifraud.channel.Channel;
import ru.yandex.antifraud.channel.EntriesDeque;
import ru.yandex.antifraud.channel.config.ChannelConfigBuilder;
import ru.yandex.antifraud.currency.Amount;
import ru.yandex.antifraud.data.AggregatedData;
import ru.yandex.antifraud.data.CollapsedAggregatesResponse;
import ru.yandex.antifraud.data.Counters;
import ru.yandex.antifraud.data.ScoringData;
import ru.yandex.antifraud.invariants.ResolutionCode;
import ru.yandex.antifraud.lua_context_manager.PrototypesManager;
import ru.yandex.antifraud.lua_context_manager.UaTraitsTuner;
import ru.yandex.antifraud.lua_context_manager.YasmTuner;
import ru.yandex.antifraud.lua_context_manager.config.ImmutablePrototypesConfig;
import ru.yandex.antifraud.lua_context_manager.config.PrototypesConfigBuilder;
import ru.yandex.antifraud.rbl.RblData;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.lua.util.JsonUtils;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;

public class ExecutorTest extends TestBase {
    public ExecutorTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        final JsonMap cybertonicaData =
                TypesafeValueContentHandler.parse(Files.readString(resource("cybertonica-request.json"))).asMap();
        final ScoringData cybertonicaRequest = new ScoringData(cybertonicaData, Amount::amount);
        final AggregatedData aggregatedData = new AggregatedData();
        aggregatedData.process(TypesafeValueContentHandler.parse(
                Files.readString(resource("storage-aggregation-response.json"))).asMap());

        final UaTraitsTuner uaTraitsTuner = new UaTraitsTuner(resource("metrika/uatraits/data/browser" +
                ".xml"));

        final RblData rblData = new RblData(TypesafeValueContentHandler.parse(
                Files.readString(resource("rbl-data1.json"))));


        final IniConfig aggregatorsIni = new IniConfig(resource("aggregates.conf"));
        final ImmutableAggregatorsConfig aggregatorsConfig = new AggregatorsConfigBuilder(aggregatorsIni).build();
        aggregatorsIni.checkUnusedKeys();

        final Aggregates aggregates = aggregatedData.calcAggregates(
                aggregatorsConfig,
                cybertonicaRequest,
                CollapsedAggregatesResponse.EMPTY,
                Counters.EMPTY,
                rblData);

        final Map<String, ChannelConfigBuilder> scripts = new HashMap<>();
        {
            final ChannelConfigBuilder builder = new ChannelConfigBuilder();
            builder.entry(resource("default.lua"));
            scripts.put("", builder);
        }
        {
            final ChannelConfigBuilder builder = new ChannelConfigBuilder();
            builder.entry(resource("taxi.lua"));
            builder.listDir(Collections.singletonList(resource("lists").toString()));
            builder.channel("taxi");
            builder.subChannel("payment");
            scripts.put("taxi", builder);
        }
        final PrototypesConfigBuilder builder = new PrototypesConfigBuilder();
        builder.channels(scripts);
        final ImmutablePrototypesConfig config = new ImmutablePrototypesConfig(builder.build());

        final Map<String, YasmTuner> yasmTuners = new HashMap<>();
        final PrototypesManager prototypesManager = new PrototypesManager(
                config,
                yasmTuners,
                uaTraitsTuner,
                null);

        {
            final Artefacts artefacts;

            final Channel channel = prototypesManager.getChannel(cybertonicaRequest).getChannel();

            try (final var entry =
                         Objects.requireNonNull(channel.entries().popEntryOrCreate(EntriesDeque.AppRoot.SCORE))) {
                final EntriesDeque.Context context = new EntriesDeque.Context(
                        channel,
                        prototypesManager,
                        cybertonicaRequest,
                        logger);
                context.updateContext(prototypesManager.getListsProvider(), prototypesManager);

                context.updateContext(
                        aggregates,
                        rblData);

                entry.runMain(context);

                artefacts = context.artefacts();
            }
            final Resolution resolution = artefacts.getResolution();

            Assert.assertEquals(ResolutionCode.DENY, resolution.getResolutionCode());
            Assert.assertEquals(Collections.singletonList("just_deny_reason"), resolution.getReason());
            Assert.assertEquals(Collections.emptyList(), resolution.getTags());
            Assert.assertEquals("just_deny_queue", artefacts.getQueues().toString());
        }
    }

    @Test
    public void testJsonToLua() throws Exception {
        final JsonObject json =
                TypesafeValueContentHandler.parse(Files.readString(resource("check_aggrs.json")));

        final Globals globals = JsePlatform.standardGlobals();
        globals.set("json_data", JsonUtils.jsonToLua(json));
        globals.loadfile(resource("check_aggrs.lua").toString()).call();
    }
}
