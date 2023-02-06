package ru.yandex.travel.commons.messaging;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.opentracing.mock.MockTracer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.travel.commons.proto.EMessageCodec;
import ru.yandex.travel.commons.proto.TPrice;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.commons.yt.ClientReplicatedYtProperties;
import ru.yandex.travel.commons.yt.ConnectionFactory;
import ru.yandex.travel.commons.yt.MultiClusterYtAdapter;
import ru.yandex.travel.commons.yt.SingleClusterYtAdapter;
import ru.yandex.travel.commons.yt.YtClusterProperties;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class YtMultiReadWriteManualTests {
    private static MultiClusterYtAdapter cluster;

    private static final String USER = "testing";
    private static final String PATH = "//home/travel/testing/offer_data_storage";
    private static final String TOKEN = "";

    @BeforeClass
    public static void setUp() throws InterruptedException {
        YtClusterProperties clusterProperties = new YtClusterProperties();
        clusterProperties.setTablePath(PATH);
        clusterProperties.setUser(USER);
        clusterProperties.setToken(TOKEN);
        ClientReplicatedYtProperties kvProperties = new ClientReplicatedYtProperties();
        Map<String, YtClusterProperties> clusterMap = ImmutableMap.of("default", clusterProperties);
        kvProperties.setClusters(clusterMap);
        kvProperties.setMinWritesToContinue(2);
        kvProperties.setMinReadsToContinue(2);
        kvProperties.setSinkClusters(Arrays.asList("seneca-sas", "seneca-man", "seneca-vla"));
        kvProperties.setMessageCodec(EMessageCodec.MC_ZLIB);
        ConnectionFactory factory = new ConnectionFactory(kvProperties);
        Retry retryHelper = new Retry(new MockTracer());

        SingleClusterYtAdapter sas = new SingleClusterYtAdapter("seneca-sas", "test",
                kvProperties.getClusterConfigFor("seneca-sas"),
                new CompressionSettings(kvProperties.getMessageCodec(), kvProperties.getCompressionLevel()), retryHelper,
                factory, true, false, false, null, null, false);
        SingleClusterYtAdapter man = new SingleClusterYtAdapter("seneca-man", "test",
                kvProperties.getClusterConfigFor("seneca-man"),
                new CompressionSettings(kvProperties.getMessageCodec(), kvProperties.getCompressionLevel()), retryHelper,
                factory, true, false, false, null, null, false);
        SingleClusterYtAdapter vla = new SingleClusterYtAdapter("seneca-vla", "test",
                kvProperties.getClusterConfigFor("seneca-vla"),
                new CompressionSettings(kvProperties.getMessageCodec(), kvProperties.getCompressionLevel()), retryHelper,
                factory, true, false, false, null, null, false);

        cluster = new MultiClusterYtAdapter(kvProperties, "keyValueStorage", Arrays.asList(sas, man, vla));

        cluster.startHealthCheckThread();
        while (!cluster.isAlive().isUp()) {
            Thread.sleep(10);
        }
    }



    @AfterClass
    public static void tearDown() {
        cluster.close();
    }

    @Test
    public void testIsAlive() {
        assertThat(cluster.isAlive().isUp()).isTrue();
    }

    @Test
    public void testPutThenGet() {
        TPrice price = TPrice.newBuilder().setAmount(4242).build();
        cluster.put("test", price, null).join();
        TPrice m = cluster.get("test", TPrice.class).join();
        assertThat(m).isNotNull();
        assertThat(m).isInstanceOf(TPrice.class);
        assertThat(m.getAmount()).isEqualTo(4242);
    }

    @Test
    public void testMissing() {
        TPrice m = cluster.get("missing", TPrice.class).join();
        assertThat(m).isNull();
    }


}
