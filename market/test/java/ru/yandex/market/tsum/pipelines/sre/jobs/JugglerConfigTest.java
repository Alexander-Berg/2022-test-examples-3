package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;

import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.market.tsum.pipelines.sre.resources.balancer.BalancerInfo;

public class JugglerConfigTest {

    private final StartrekTicket startrekTicket = new StartrekTicket("TEST-1");
    private final Properties properties = new Properties();

    @Before
    public void setUp() throws Exception {
        String description = "slb.description = Что-то для теста\n" +
            "slb.fqdn = mbi-crm-proxy.tst.vs.market.yandex.net\n" +
            "slb.port = 55555\n" +
            "slb.realPort = \n" +
            "slb.ssl_backends = Да\n" +
            "slb.type_backends = CONDUCTOR_GROUP\n" +
            "slb.real_servers = market_slb_search-stable,market_corba-stable\n" +
            "slb.health_check_url = /ping\n" +
            "slb.health_check_type = Тело ответа\n" +
            "slb.health_check_text = 0;ok\n" +
            "slb.rps = 20 рпс\n" +
            "slb.ip_version = IPv6-only\n" +
            "access.dynamic = Да\n" +
            "access.human = \n" +
            "access.machine = " +
            "monitor.needMonitor = Да\n" +
            "monitor.resps = yandex_market_dev,yandex_market_admin,le087";
        properties.load(new StringReader(description));
    }

    private Map<String, Object> getParamsMap(BalancerInfo balancerInfo) {
        Map<String, Object> params = new HashMap<>();
        params.put("children", "market_slb_search-testing");
        params.put("balancerInfo", balancerInfo);
        params.put("checkName", "some.tst.vs.market.yandex.net_7777");
        return params;
    }
}
