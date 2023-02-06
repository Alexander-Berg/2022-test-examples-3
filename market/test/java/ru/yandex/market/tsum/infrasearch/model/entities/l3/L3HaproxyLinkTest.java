package ru.yandex.market.tsum.infrasearch.model.entities.l3;

import org.junit.Test;
import ru.yandex.market.tsum.pipelines.sre.resources.BalancerFlavour;

import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class L3HaproxyLinkTest {

    private static String grafanaHaproxyLink = "https://grafana.yandex-team.ru/d/e7x-VZ5Zz/market-haproxy-backends?" +
        "orgId=1&" +
        "var-balancer_type=m&" +
        "var-dc_label=All&" +
        "var-slb_server=All&" +
        "var-haproxy_backend_name=dash_vs_market_yandex_net_443&" +
        "var-haproxy_frontend_name=dash_vs_market_yandex_net_443&" +
        "var-haproxy_real_name=All&" +
        "var-l3_lb_name=dash_vs_market_yandex_net&" +
        "var-l3_lb_name_dot=dash.vs.market.yandex.net&" +
        "var-l3_lb_port=443";

    @Test
    public void getHaproxyLink() throws URISyntaxException {
        L3HaproxyLink.Builder builder = L3HaproxyLink.Builder.create()
            .withBalancerFlavour(BalancerFlavour.MSLB)
            .withHaproxyBackendName("dash_vs_market_yandex_net_443")
            .withHaproxyFrontendName("dash_vs_market_yandex_net_443")
            .withL3SlbName("dash_vs_market_yandex_net")
            .withL3SlbPort("443")
            .withErrors(new ArrayList<>());

        L3HaproxyLink l3HaproxyLink = new L3HaproxyLink(builder);
        assertEquals(grafanaHaproxyLink, l3HaproxyLink.getHaproxyLink());
    }
}