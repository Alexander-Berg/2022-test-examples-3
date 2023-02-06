package ru.yandex.market.tpl.db.balancing;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.postgresql.cluster.balancing.ClusterServerStats;
import ru.yandex.postgresql.cluster.datasource.ServerInfo;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConnStringPriorityPostgresClusterConnectionBalancerTest {

    private final String host1 = "b";
    private final String host2 = "c";
    private final String host3 = "a";
    private final String url = "jdbc:postgresql://" + host1 + ":1,"
            + host2 + ":1," + host3 + ":1/test";

    private ConnStringPriorityPostgresClusterConnectionBalancer subject
            = new ConnStringPriorityPostgresClusterConnectionBalancer(url, null, false);

    @Test
    void throwsExceptionForEmptyStats() {
        assertThrows(
                IllegalStateException.class,
                () -> subject.pickDataSource(List.of(), null)
        );
    }

    @Test
    void shouldChooseHost1() {
        ClusterServerStats result = subject.pickDataSource(
                List.of(
                        getStat(host3, false),
                        getStat(host2, false),
                        getStat(host1, false)
                ),
                null
        );

        assertThat(result).isNotNull();
        assertThat(result.server.url).isEqualTo(toUrl(host1));
    }

    @Test
    void shouldChooseHost2IfHost1IsMaster() {
        ClusterServerStats result = subject.pickDataSource(
                List.of(
                        getStat(host3, false),
                        getStat(host2, false),
                        getStat(host1, true)
                ),
                null
        );

        assertThat(result).isNotNull();
        assertThat(result.server.url).isEqualTo(toUrl(host2));
    }

    @Test
    void shouldChooseHost2IfHost1IsMissing() {
        ClusterServerStats result = subject.pickDataSource(
                List.of(
                        getStat(host3, false),
                        getStat(host2, false)
                ),
                null
        );

        assertThat(result).isNotNull();
        assertThat(result.server.url).isEqualTo(toUrl(host2));
    }

    @Test
    void throwsExceptionForUnknownHosts() {
        assertThrows(
                IllegalStateException.class,
                () -> subject.pickDataSource(
                        List.of(
                                getStat("unknown1", false),
                                getStat("unknown2", false)
                        ),
                        null
                )
        );
    }

    private ClusterServerStats getStat(String host, boolean master) {
        return new ClusterServerStats(
                new ServerInfo(host + "-key", toUrl(host)),
                master,
                1F,
                1,
                1,
                1,
                Map.of()
        );
    }

    private String toUrl(String host) {
        return "jdbc:postgresql://" + host + ":1/test";
    }

}
