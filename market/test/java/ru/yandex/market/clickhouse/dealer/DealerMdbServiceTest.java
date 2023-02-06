package ru.yandex.market.clickhouse.dealer;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import yandex.cloud.api.mdb.clickhouse.v1.ClusterOuterClass;
import yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceGrpc;
import yandex.cloud.api.mdb.clickhouse.v1.ClusterServiceOuterClass;

/**
 * @author Aleksei Malygin <a href="mailto:Malygin-Me@yandex-team.ru"></a>
 * Date: 2018-12-07
 */


public class DealerMdbServiceTest {

    private static final String CLUSTER_ID = "mdbtest1234jlkj123";
    private static final String UUID_CLUSTER_ID = "test929d-9091-4087-bee4-4cd8a4cctest";
    private static final String CLUSTER_NAME = "mdb_cluster_name";
    private static final List<String> HOSTS = ImmutableList.of("host1", "host2");

    @Mock
    private ClusterServiceGrpc.ClusterServiceBlockingStub service;

    @InjectMocks
    @Spy
    DealerMdbService dealerMdbService = new DealerMdbService();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(service.get(Mockito.any())).thenReturn(
            ClusterOuterClass.Cluster.newBuilder()
                .setName(CLUSTER_NAME)
                .build()
        );
    }

    @Test
    public void getClusterNameTest() {
        Assert.assertEquals(CLUSTER_NAME, dealerMdbService.getClusterName(CLUSTER_ID));
    }

    @Test
    public void getClickhouseHostsTest() {
        Mockito.when(service.listHosts(Mockito.any())).thenReturn(
            ClusterServiceOuterClass.ListClusterHostsResponse.newBuilder()
                .addAllHosts(
                    HOSTS.stream()
                        .map(h -> ClusterOuterClass.Host.newBuilder()
                            .setName(h)
                            .setType(ClusterOuterClass.Host.Type.CLICKHOUSE)
                            .build())
                        .collect(Collectors.toList())
                )
                .build()
        );

        Assert.assertEquals(HOSTS, dealerMdbService.getClickhouseHosts(CLUSTER_ID));
    }

    @Test
    public void getClickHouseSystemClusterNameTest() {
        Assert.assertEquals(CLUSTER_ID, dealerMdbService.getClickHouseSystemClusterName(CLUSTER_ID));
        Assert.assertNotEquals(CLUSTER_NAME, dealerMdbService.getClickHouseSystemClusterName(CLUSTER_ID));

        Assert.assertEquals(CLUSTER_NAME, dealerMdbService.getClickHouseSystemClusterName(UUID_CLUSTER_ID));
        Assert.assertNotEquals(UUID_CLUSTER_ID, dealerMdbService.getClickHouseSystemClusterName(UUID_CLUSTER_ID));
    }
}
