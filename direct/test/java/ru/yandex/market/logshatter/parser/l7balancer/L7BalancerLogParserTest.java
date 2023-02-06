package ru.yandex.market.logshatter.parser.l7balancer;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 05.12.16
 */
public class L7BalancerLogParserTest {
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSSSSSZ");

    @Test
    public void parse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new L7BalancerLogParser());
        checker.check("84.201.151.4:51379\t2016-12-02T18:10:49.728370+0300\t\"GET / HTTP/1.1\"\t1.474430s\t\"\"\t\"abo.market.mslb01ht.yandex.ru:4242\"\t[regexp market [proxy pepelac01ht.yandex.ru:443 1.474036s/1.474139s 0/1294 succ 200]]",
            dateFormat.parse("2016-12-02T18:10:49.728370+0300"),
            checker.getHost(),
            "84.201.151.4",
            51379,
            "GET",
            "/",
            "HTTP/1.1",
            1474,
            "",
            "abo.market.mslb01ht.yandex.ru",
            4242,
            "market",
            "pepelac01ht.yandex.ru",
            443,
            "200"
        );
        checker.check("84.201.151.4:51379\t2016-12-02T18:10:51.390131+0300\t\"GET /abo-common.css HTTP/1.1\"\t0.112549s\t\"https://abo.market.mslb01ht.yandex.ru:4242/\"\t\"abo.market.mslb01ht.yandex.ru:4242\"\t[regexp market [proxy pepelac02ht.yandex.ru:443 0.075833s/0.075883s 0/222 succ 304]]",
            dateFormat.parse("2016-12-02T18:10:51.390131+0300"),
            checker.getHost(),
            "84.201.151.4",
            51379,
            "GET",
            "/abo-common.css",
            "HTTP/1.1",
            113,
            "https://abo.market.mslb01ht.yandex.ru:4242/",
            "abo.market.mslb01ht.yandex.ru",
            4242,
            "market",
            "pepelac02ht.yandex.ru",
            443,
            "304"
        );
        checker.check("84.201.151.4:51463\t2016-12-02T18:21:46.106092+0300\t\"GET /gate/route/wishlist/getCount?sk=u6aa9ef35468d43467a50207880210043 HTTP/1.1\"\t1.206637s\t\"https://market.mslb01ht.yandex.ru:4242/\"\t\"market.mslb01ht.yandex.ru:4242\"\t[regexp market [proxy pepelac01ht.yandex.ru:443 1.206325s/1.206378s 0/885 succ 200]]",
            dateFormat.parse("2016-12-02T18:21:46.106092+0300"),
            checker.getHost(),
            "84.201.151.4",
            51463,
            "GET",
            "/gate/route/wishlist/getCount?sk=u6aa9ef35468d43467a50207880210043",
            "HTTP/1.1",
            1207,
            "https://market.mslb01ht.yandex.ru:4242/",
            "market.mslb01ht.yandex.ru",
            4242,
            "market",
            "pepelac01ht.yandex.ru",
            443,
            "200"
        );
        checker.check("87.250.226.120:34054\t2017-03-23T13:23:46.003622+0300\t\"GET /ping HTTP/1.0\"\t0.000030s\t\"\"\t\"87.250.250.22:80\"\t[regexp ping [EMPTY_RESPONSE]]",
            dateFormat.parse("2017-03-23T13:23:46.003622+0300"),
            checker.getHost(),
            "87.250.226.120",
            34054,
            "GET",
            "/ping",
            "HTTP/1.0",
            0,
            "",
            "87.250.250.22",
            80,
            "ping",
            "",
            -1,
            "EMPTY_RESPONSE"
        );
        checker.check("87.250.226.120:34054\t2017-03-23T13:23:46.003622+0300\t\"GET /ping HTTP/1.0\"\t0.000030s\t\"\"\t\"87.250.250.22:80\"\t[regexp ping]",
            dateFormat.parse("2017-03-23T13:23:46.003622+0300"),
            checker.getHost(),
            "87.250.226.120",
            34054,
            "GET",
            "/ping",
            "HTTP/1.0",
            0,
            "",
            "87.250.250.22",
            80,
            "ping",
            "",
            -1,
            ""
        );
        checker.check("146.185.202.37:30974\t2017-04-21T06:30:54.243960+0300\t\"GET /catalog/54714/list?hid=418706&track=fr_cm_vendor&how=dpop&glfilter=1801946%3A11222263&deliveryincluded=0&onstock=1&page=11&lr=213 HTTP/1.1\"\t0.011539s\t\"\"\t\"market.yandex.ru\"\t[regexp market [exp_getter [uaas [proxy localhost:9905 0.000373s/0.000379s 0/1828 succ 200] uaas_answered] [proxy pepelac15g.market.yandex.net:443 0.010988s/0.011039s 0/738 succ 403]]]",
            dateFormat.parse("2017-04-21T06:30:54.243960+0300"),
            checker.getHost(),
            "146.185.202.37",
            30974,
            "GET",
            "/catalog/54714/list?hid=418706&track=fr_cm_vendor&how=dpop&glfilter=1801946%3A11222263&deliveryincluded=0&onstock=1&page=11&lr=213",
            "HTTP/1.1",
            12,
            "",
            "market.yandex.ru",
            -1,
            "market",
            "pepelac15g.market.yandex.net",
            443,
            "403"
        );
        checker.check("146.185.202.37:30974\t2017-04-21T06:30:54.243960+0300\t\"GET /catalog/54714/list?hid=418706&track=fr_cm_vendor&how=dpop&glfilter=1801946%3A11222263&deliveryincluded=0&onstock=1&page=11&lr=213 HTTP/1.1\"\t0.011539s\t\"\"\t\"market.yandex.ru\"\t[regexp market]",
            dateFormat.parse("2017-04-21T06:30:54.243960+0300"),
            checker.getHost(),
            "146.185.202.37",
            30974,
            "GET",
            "/catalog/54714/list?hid=418706&track=fr_cm_vendor&how=dpop&glfilter=1801946%3A11222263&deliveryincluded=0&onstock=1&page=11&lr=213",
            "HTTP/1.1",
            12,
            "",
            "market.yandex.ru",
            -1,
            "market",
            "",
            -1,
            ""
        );
    }
}