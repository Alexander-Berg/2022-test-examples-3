package ru.yandex.travel.commons.messaging;

import java.util.Collections;
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
import ru.yandex.travel.commons.yt.SingleClusterYtAdapter;
import ru.yandex.travel.commons.yt.YtClusterProperties;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class YtSingleReadWriteManualTests {
    private static SingleClusterYtAdapter ytSink;
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
        kvProperties.setSinkClusters(Collections.singletonList("seneca-sas"));
        kvProperties.setMessageCodec(EMessageCodec.MC_ZLIB);
        ConnectionFactory factory = new ConnectionFactory(kvProperties);
        Retry retryHelper = new Retry(new MockTracer());

        ytSink = new SingleClusterYtAdapter("seneca-sas", "test",
                kvProperties.getClusterConfigFor("seneca-sas"),
                new CompressionSettings(kvProperties.getMessageCodec(), kvProperties.getCompressionLevel()), retryHelper,
                factory, true, false, false, null, null, false);
        ytSink.startHealthCheckThread();
        while (!ytSink.isAlive().isUp()) {
            Thread.sleep(10);
        }
    }

    @AfterClass
    public static void tearDown() {
        ytSink.stopHealthCheckThread();
    }

    @Test
    public void testIsAlive() {
        assertThat(ytSink.isAlive().isUp()).isTrue();
    }

    @Test
    public void testPutThenGet() {
        TPrice price = TPrice.newBuilder().setAmount(4242).build();
        ytSink.put("test", price, null).join();
        TPrice m = ytSink.get("test", TPrice.class).join();
        assertThat(m).isNotNull();
        assertThat(m).isInstanceOf(TPrice.class);
        assertThat(m.getAmount()).isEqualTo(4242);
    }

    @Test
    public void testMissing() {
        TPrice m = ytSink.get("missing", TPrice.class).join();
        assertThat(m).isNull();
    }


}
