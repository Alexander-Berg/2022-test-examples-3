package ru.yandex.market.logshatter.parser.l7balancer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 05.12.16
 */
public class L7BalancerLogParserTest {
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSSSSSZ");

    @Test
    @SuppressWarnings("MethodLength")
    public void parse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new L7BalancerLogParser());
        checker.check("84.201.151.4:51379\t2016-12-02T18:10:49.728370+0300\t\"GET / HTTP/1.1\"\t1" +
                ".474430s\t\"\"\t\"abo.market.mslb01ht.yandex.ru:4242\"\t[regexp market [proxy pepelac01ht.yandex" +
                ".ru:443 1.474036s/1.474139s 0/1294 succ 200]]",
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
        checker.check("84.201.151.4:51379\t2016-12-02T18:10:51.390131+0300\t\"GET /abo-common.css HTTP/1.1\"\t0" +
                ".112549s\t\"https://abo.market.mslb01ht.yandex.ru:4242/\"\t\"abo.market.mslb01ht.yandex" +
                ".ru:4242\"\t[regexp market [proxy pepelac02ht.yandex.ru:443 0.075833s/0.075883s 0/222 succ 304]]",
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
        checker.check("84.201.151.4:51463\t2016-12-02T18:21:46.106092+0300\t\"GET " +
                "/gate/route/wishlist/getCount?sk=u6aa9ef35468d43467a50207880210043 HTTP/1.1\"\t1" +
                ".206637s\t\"https://market.mslb01ht.yandex.ru:4242/\"\t\"market.mslb01ht.yandex.ru:4242\"\t[regexp " +
                "market [proxy pepelac01ht.yandex.ru:443 1.206325s/1.206378s 0/885 succ 200]]",
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
        checker.check("87.250.226.120:34054\t2017-03-23T13:23:46.003622+0300\t\"GET /ping HTTP/1.0\"\t0" +
                ".000030s\t\"\"\t\"87.250.250.22:80\"\t[regexp ping [EMPTY_RESPONSE]]",
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
        checker.check("87.250.226.120:34054\t2017-03-23T13:23:46.003622+0300\t\"GET /ping HTTP/1.0\"\t0" +
                ".000030s\t\"\"\t\"87.250.250.22:80\"\t[regexp ping]",
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
        checker.check("146.185.202.37:30974\t2017-04-21T06:30:54.243960+0300\t\"GET " +
                "/catalog/54714/list?hid=418706&track=fr_cm_vendor&how=dpop&glfilter=1801946%3A11222263" +
                "&deliveryincluded=0&onstock=1&page=11&lr=213 HTTP/1.1\"\t0.011539s\t\"\"\t\"market.yandex" +
                ".ru\"\t[regexp market [exp_getter [uaas [proxy localhost:9905 0.000373s/0.000379s 0/1828 succ 200] " +
                "uaas_answered] [proxy pepelac15g.market.yandex.net:443 0.010988s/0.011039s 0/738 succ 403]]]",
            dateFormat.parse("2017-04-21T06:30:54.243960+0300"),
            checker.getHost(),
            "146.185.202.37",
            30974,
            "GET",
            "/catalog/54714/list?hid=418706&track=fr_cm_vendor&how=dpop&glfilter=1801946%3A11222263&deliveryincluded" +
                "=0&onstock=1&page=11&lr=213",
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
        checker.check("146.185.202.37:30974\t2017-04-21T06:30:54.243960+0300\t\"GET " +
                "/catalog/54714/list?hid=418706&track=fr_cm_vendor&how=dpop&glfilter=1801946%3A11222263" +
                "&deliveryincluded=0&onstock=1&page=11&lr=213 HTTP/1.1\"\t0.011539s\t\"\"\t\"market.yandex" +
                ".ru\"\t[regexp market]",
            dateFormat.parse("2017-04-21T06:30:54.243960+0300"),
            checker.getHost(),
            "146.185.202.37",
            30974,
            "GET",
            "/catalog/54714/list?hid=418706&track=fr_cm_vendor&how=dpop&glfilter=1801946%3A11222263&deliveryincluded" +
                "=0&onstock=1&page=11&lr=213",
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
        checker.check("[2a02:6b8:c0b:6e1e:0:438f:5660:0]:46352\t2019-10-10T22:35:45.213681+0300\t\"POST " +
                "/?output_format_json_quote_64bit_integers=1&database=system&result_overflow_mode=throw HTTP/1.1\"\t0" +
                ".125898s\t\"\"\t\"clickhouse.error.yandex-team.ru\"\t [regexp default [proxy rum-clickhouse-proxy-1" +
                ".iva.yp-c.yandex.net:80 0.125727s/0.125838s 1699/1230 succ 200]]",
            dateFormat.parse("2019-10-10T22:35:45.213681+0300"),
            checker.getHost(),
            "2a02:6b8:c0b:6e1e:0:438f:5660:0",
            46352,
            "POST",
            "/?output_format_json_quote_64bit_integers=1&database=system&result_overflow_mode=throw",
            "HTTP/1.1",
            126,
            "",
            "clickhouse.error.yandex-team.ru",
            -1,
            "default",
            "rum-clickhouse-proxy-1.iva.yp-c.yandex.net",
            80,
            "200"
        );
        checker.check("[2a02:6b8:0:e00::b00a]:55570\t2019-10-10T23:49:41.990554+0300\t\"GET /ping HTTP/1.0\"\t0" +
                ".000054s\t\"\"\t\"error-booster-balancer.yandex-team.ru\"\t [regexp slbping [errordocument succ 200]]",
            dateFormat.parse("2019-10-10T23:49:41.990554+0300"),
            checker.getHost(),
            "2a02:6b8:0:e00::b00a",
            55570,
            "GET",
            "/ping",
            "HTTP/1.0",
            0,
            "",
            "error-booster-balancer.yandex-team.ru",
            -1,
            "slbping",
            "",
            -1,
            "errordocument succ 200"
        );

        checker.check("[2a02:6b8:c08:311c:100:747:0:229f]:51256\t2021-07-29T18:06:11.625821+0300\t\"GET " +
                "/awacs-balancer-health-check HTTP/1.1\"\t0.000118s\t\"\"\t\"2a02:6b8:c1c:10c:0:438f:9baa:0\"\t " +
                "[report u:service_total [cookie_policy u:service_total [regexp awacs-balancer-health-check " +
                "[errordocument succ 200]]]]",
            dateFormat.parse("2021-07-29T18:06:11.625821+0300"),
            checker.getHost(),
            "2a02:6b8:c08:311c:100:747:0:229f",
            51256,
            "GET",
            "/awacs-balancer-health-check",
            "HTTP/1.1",
            0,
            "",
            "2a02:6b8:c1c:10c:0:438f:9baa:0",
            -1,
            "awacs-balancer-health-check",
            "",
            -1,
            "errordocument succ 200"
        );
    }
}
