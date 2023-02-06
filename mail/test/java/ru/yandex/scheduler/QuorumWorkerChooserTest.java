package ru.yandex.scheduler;

import java.util.List;
import java.util.Set;

import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.alive2.AliveAppInfo;
import ru.yandex.commune.alive2.AliveAppsHolder;
import ru.yandex.commune.bazinga.impl.worker.BazingaHostPort;
import ru.yandex.commune.bazinga.scheduler.WorkerMeta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuorumWorkerChooserTest {
    @Test
    @DisplayName("The most popular version should be selected")
    public void checkPopularVersion() {
        val apps = StreamEx.of(getAliveAppInfo("host1.yandex-team.ru", "version1"),
                getAliveAppInfo("host2.yandex-team.ru", "version2"),
                getAliveAppInfo("host3.yandex-team.ru", "version1")).toImmutableList();

        checkHosts(apps, "host1.yandex-team.ru", "host3.yandex-team.ru");
    }

    @Test
    @DisplayName("The most recent version should be selected in case of equal popularity")
    public void checkRecentVersion() {
        val apps = StreamEx.of(getAliveAppInfo("host1.yandex-team.ru", "version1"),
                getAliveAppInfo("host2.yandex-team.ru", "version2"),
                getAliveAppInfo("host3.yandex-team.ru", "version1"),
                getAliveAppInfo("host4.yandex-team.ru", "version2")).toImmutableList();

        checkHosts(apps,"host2.yandex-team.ru", "host4.yandex-team.ru");
    }

    private static void checkHosts(List<AliveAppInfo> apps, String ... hosts) {
        checkHosts(apps, StreamEx.of(hosts).toImmutableSet());
    }

    private static void checkHosts(List<AliveAppInfo> apps, Set<String> hosts) {
        val workers = getWorkers(apps);
        val chosenHosts = StreamEx.of(new QuorumWorkerChooser(getHolder(apps))
                .choose(null, Cf.toList(workers)))
                .map(Object::toString)
                .toImmutableSet();

        assertThat(chosenHosts).hasSize(1);

        assertThat(chosenHosts)
                .as("Chosen hosts (%s) should be a subset of alive apps hosts (%s)",
                        String.join(", ", chosenHosts), String.join(", ", hosts))
                .allMatch(hosts::contains);
    }

    private static List<WorkerMeta> getWorkers(List<AliveAppInfo> apps) {
        return StreamEx.of(apps).map(AliveAppInfo::getHostname).map(QuorumWorkerChooserTest::getWorkerMeta).toImmutableList();
    }

    private static AliveAppsHolder getHolder(List<AliveAppInfo> apps) {
        val holder = mock(AliveAppsHolder.class);
        when(holder.aliveApps()).thenReturn(Cf.toList(apps));
        return holder;
    }

    private static AliveAppInfo getAliveAppInfo(String host, String version) {
        return new AliveAppInfo("Test service", "worker", Instant.now(), version, host, 0, null, Option.empty());
    }

    private static WorkerMeta getWorkerMeta(String host) {
        val meta = mock(WorkerMeta.class);
        when(meta.getHostPort()).thenReturn(new BazingaHostPort(host, 10000));
        when(meta.toString()).thenReturn(host);
        return meta;
    }
}
