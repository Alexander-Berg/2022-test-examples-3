package ru.yandex.market.aliasmaker.meta.heartbeat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import ru.yandex.market.aliasmaker.AliasMakerService;
import ru.yandex.market.aliasmaker.meta.be.CurrentStateResponse;
import ru.yandex.market.aliasmaker.meta.be.ShardInfo;
import ru.yandex.market.aliasmaker.meta.be.ShardSetState;
import ru.yandex.market.aliasmaker.meta.client.ClientCache;
import ru.yandex.market.http.MonitoringResult;

import static ru.yandex.market.http.MonitoringResult.Status.ERROR;
import static ru.yandex.market.http.MonitoringResult.Status.OK;

/**
 * @author apluhin
 * @created 4/15/22
 */
public class AlivenessCheckerTest {

    private ClientCache clientCache;

    private AlivenessChecker alivenessChecker;

    @Before
    public void setUp() throws Exception {
        clientCache = Mockito.mock(ClientCache.class);
        alivenessChecker = new AlivenessChecker(clientCache);
    }

    @Test
    public void testParallelCheckWithFailed() {
        int countOfServices = 8;
        ShardSetState.Builder builder = new ShardSetState.Builder();

        List<AliasMakerService> aliasMakerServices = IntStream.range(0, countOfServices)
                .boxed()
                .map(this::mockService)
                .collect(Collectors.toList());
        OngoingStubbing<AliasMakerService> when = Mockito.when(clientCache.getServiceByShard(Mockito.any()));

        for (AliasMakerService service : aliasMakerServices) {
            when = when.thenReturn(service);
        }

        IntStream.range(0, countOfServices)
                .boxed()
                .forEachOrdered(id -> builder.updateShard("service1", mockShardInfo(id + "")));

        ShardSetState check = alivenessChecker.check(builder.build());
        Map<Boolean, List<ShardInfo>> resultByStatus = check.getState().values().stream()
                .flatMap(it -> it.values().stream())
                .collect(Collectors.groupingBy(ShardInfo::isLastHeartbeatSuccessful));
        Assertions.assertThat(resultByStatus.get(true).size()).isEqualTo(5);
        Assertions.assertThat(resultByStatus.get(false).size()).isEqualTo(3);
    }

    @Test
    public void testParallelCheckWithSuccess() {
        int countOfServices = 8;
        ShardSetState.Builder builder = new ShardSetState.Builder();

        List<AliasMakerService> aliasMakerServices = IntStream.range(0, countOfServices)
                .boxed()
                .map(id -> mockService(3))
                .collect(Collectors.toList());
        OngoingStubbing<AliasMakerService> when = Mockito.when(clientCache.getServiceByShard(Mockito.any()));

        for (AliasMakerService service : aliasMakerServices) {
            when = when.thenReturn(service);
        }

        IntStream.range(0, countOfServices)
                .boxed()
                .forEachOrdered(id -> builder.updateShard("service1", mockShardInfo(id + "")));

        ShardSetState check = alivenessChecker.check(builder.build());
        Map<Boolean, List<ShardInfo>> resultByStatus = check.getState().values().stream()
                .flatMap(it -> it.values().stream())
                .collect(Collectors.groupingBy(ShardInfo::isLastHeartbeatSuccessful));
        Assertions.assertThat(resultByStatus.get(true).size()).isEqualTo(8);
    }

    private ShardInfo mockShardInfo(String name) {
        return new ShardInfo(new CurrentStateResponse.InstancePart(name, name, name, Collections.emptyList(), null,
                1), name,
                1, true, null);
    }

    private AliasMakerService mockService(int failedCount) {
        AliasMakerService mock = Mockito.mock(AliasMakerService.class);
        OngoingStubbing<MonitoringResult> when = Mockito.when(mock.ping());
        for (int i = 0; i < failedCount; i++) {
            when = when.thenReturn(new MonitoringResult(ERROR, "error"));
        }
        when = when.thenReturn(new MonitoringResult(OK, "ok"));
        return mock;
    }
}
