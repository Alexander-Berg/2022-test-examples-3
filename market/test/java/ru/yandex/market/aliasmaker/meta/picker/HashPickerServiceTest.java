package ru.yandex.market.aliasmaker.meta.picker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.meta.be.CurrentStateResponse;
import ru.yandex.market.aliasmaker.meta.be.ShardInfo;
import ru.yandex.market.aliasmaker.meta.be.ShardSetSnapshot;
import ru.yandex.market.aliasmaker.meta.be.ShardSetState;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

public class HashPickerServiceTest {
    private static final int BIG_CATEGORY = 100001;
    private final StorageKeyValueService storageKeyValueService = Mockito.mock(StorageKeyValueService.class);

    @Test
    public void pickShardProd() {
        PickerService service = new HashPickerService(Collections.singleton(BIG_CATEGORY), 1, 100,
                storageKeyValueService);
        ShardSetState.Builder stateBuilder = new ShardSetState.Builder();
        stateBuilder.updateShard("testing_market_alias_maker_sas", new ShardInfo(
                createInstancePart(
                        "sas0-1234-sas-market-test-alia-fef-20514.gencfg-c.yandex.net",
                        "sas0-1234.search.yandex.net",
                        20514),
                "testing_market_alias_maker_sas", 1, true, null));
        stateBuilder.updateShard("testing_market_alias_maker_sas", new ShardInfo(
                createInstancePart(
                        "sas2-3723-sas-market-test-alia-fef-20514.gencfg-c.yandex.net",
                        "sas2-3723.search.yandex.net",
                        20514),
                "testing_market_alias_maker_sas", 1, true, null));
        stateBuilder.updateShard("testing_market_alias_maker_vla", new ShardInfo(
                createInstancePart(
                        "vla2-8745-cf6-vla-market-test-a-789-8041.gencfg-c.yandex.net",
                        "vla2-8745.search.yandex.net",
                        8041),
                "testing_market_alias_maker_vla", 1, true, null));
        ShardSetState state = stateBuilder.build();
        service.updateFrom(new ShardSetSnapshot(state, state));


        List<Mark> marks = new ArrayList<>();
        marks.add(service.pickShard(91491));
        marks.add(service.pickShard(15625430));
        marks.add(service.pickShard(90533));
        marks.add(service.pickShard(14369615));
        marks.add(service.pickShard(16155466));
        marks.add(service.pickShard(16309373));
        marks.add(service.pickShard(91033));
        marks.add(service.pickShard(BIG_CATEGORY));
        for (int i = 0; i < 100000; ++i) {
            marks.add(service.pickShard(i));
        }
        TreeMap<String, Integer> counters = new TreeMap<>();
        for (Mark mark : marks) {
            counters.merge(
                    mark.getShardInfo().getInstancePart().getHostname(),
                    1, Integer::sum
            );
        }
        Map.Entry<String, Integer> zeroEntry = counters.firstEntry();
        assertThat(zeroEntry.getValue()).isEqualTo(1);
        counters.remove(zeroEntry.getKey());
        counters.values().forEach(x -> assertTrue(x > marks.size() * 0.35));
    }

    @Test
    public void pickShardTest() {
        PickerService service = new HashPickerService(Collections.emptySet(), 1, 100, storageKeyValueService);
        ShardSetState.Builder stateBuilder = new ShardSetState.Builder();
        stateBuilder.updateShard("testing_market_alias_maker_sas", new ShardInfo(
                createInstancePart(
                        "sas2-3723-sas-market-test-alia-fef-20514.gencfg-c.yandex.net",
                        "sas2-3723.search.yandex.net",
                        20514),
                "testing_market_alias_maker_sas", 1, true, null));
        stateBuilder.updateShard("testing_market_alias_maker_vla", new ShardInfo(
                createInstancePart(
                        "vla2-8745-cf6-vla-market-test-a-789-8041.gencfg-c.yandex.net",
                        "vla2-8745.search.yandex.net",
                        8041),
                "testing_market_alias_maker_vla", 1, true, null));
        ShardSetState state = stateBuilder.build();
        service.updateFrom(new ShardSetSnapshot(state, state));


        List<Mark> marks = new ArrayList<>();
        marks.add(service.pickShard(91491));
        marks.add(service.pickShard(15625430));
        marks.add(service.pickShard(90533));
        marks.add(service.pickShard(14369615));
        marks.add(service.pickShard(16155466));
        marks.add(service.pickShard(16309373));
        marks.add(service.pickShard(91033));
        for (int i = 0; i < 100000; ++i) {
            marks.add(service.pickShard(i));
        }
        TreeMap<String, Integer> counters = new TreeMap<>();
        for (Mark mark : marks) {
            counters.merge(
                    mark.getShardInfo().getInstancePart().getHostname(),
                    1, Integer::sum
            );
        }
        counters.values().forEach(x -> assertTrue(x > marks.size() * 0.35));
    }

    @Test
    public void pickRandomAliveShard() {
        PickerService service = new HashPickerService(Collections.singleton(BIG_CATEGORY), 1, 100,
                storageKeyValueService);
        ShardSetState.Builder stateBuilder = new ShardSetState.Builder();
        stateBuilder.updateShard("testing_market_alias_maker_sas", new ShardInfo(
                createInstancePart(
                        "sas2-3723-sas-market-test-alia-fef-20514.gencfg-c.yandex.net",
                        "sas2-3723.search.yandex.net",
                        20514),
                "testing_market_alias_maker_sas", 1, true, null));
        stateBuilder.updateShard("testing_market_alias_maker_vla", new ShardInfo(
                createInstancePart(
                        "vla2-8745-cf6-vla-market-test-a-789-8041.gencfg-c.yandex.net",
                        "vla2-8745.search.yandex.net",
                        8041),
                "testing_market_alias_maker_vla", 1, false, null)); // <-- last ping failed
        ShardSetState state = stateBuilder.build();
        service.updateFrom(new ShardSetSnapshot(state, state.toBuilder().cleanUpFailedHeartbeat().build()));

        for (int i = 0; i < 100000; ++i) {
            ShardInfo randomShardInfo = service.pickRandomShard().getShardInfo();
            assertTrue(randomShardInfo.isLastHeartbeatSuccessful());
            assertThat(randomShardInfo.getServiceName()).isEqualTo("testing_market_alias_maker_sas");
        }
    }

    private CurrentStateResponse.InstancePart createInstancePart(String containerHostname, String host, int port) {
        return CurrentStateResponse.InstancePart.create(containerHostname, host, port);
    }
}
