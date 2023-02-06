package ru.yandex.market.tsum.clients.graphite;

import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 17/10/16
 */
@Ignore
public class GraphiteClientTest {
    @Test
    public void test1() throws Exception {
        GraphiteClient client = new GraphiteClient("https://market-graphite.yandex-team.ru/");
        List<GraphiteMetric> metrics = client.getMetrics(
            Arrays.asList("one_min.HOST.welder0*gt_market_yandex_net.netstat_bytes_eth0.rx"), 1476721745, 1476722334
        ).get();

    }
}
