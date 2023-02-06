package ru.yandex.market.logshatter.parser.home;

import java.util.Arrays;
import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class HomePlainErrorLogParserTest {

    @Test
    @SuppressWarnings("MethodLength")
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new HomePlainErrorLogParser());
        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [21333e969dd106366ebe71f692d3fcc5] [subreqfail] " +
                "[games_card_data] [retnum=0] [curl_conn_time=0.0000] [curl_size_download=16.0000] " +
                "[curl_speed_download=83.0000] [curl_starttransfer_time=0.1912] [curl_total_time=0.1912] " +
                "[ip=2a02:6b8:0:3400:0:2e5:1:822f] [runtime=0.191629] [timeout=0.1] [url=http://games-api.browser" +
                ".yandex.ru/api/v1/sa/card?app_id=ru.yandex" +
                ".searchplugin&app_platform=android&app_version=11020001&did=b85fef673e1f6cc01410caa5408b52ae&domain" +
                "=ru&lang=ru&reqid=1594891978.55575.79702.114261&uid=983483385&uuid=21333e969dd106366ebe71f692d3fcc5]" +
                " Bad status code response: code(504) message(Gateway Time-out) " +
                "(mordaxpl:586->Handler::Api:802->Handler::Api:1124->MordaX::HTTP:679->MordaX::HTTP:839) [23b05] " +
                "stable-morda-sas-yp-4 stable portal-morda",
            new Date(1594891978000L),
            "stable-morda-sas-yp-4",
            "portal-morda",
            "stable",
            "1594891978.55575.79702.114261",
            "21333e969dd106366ebe71f692d3fcc5",
            "subreqfail",
            "games_card_data",
            "Bad status code response: code(504) message(Gateway Time-out)",
            "mordaxpl:586->Handler::Api:802->Handler::Api:1124->MordaX::HTTP:679->MordaX::HTTP:839",
            Arrays.asList("retnum", "curl_conn_time", "curl_size_download", "curl_speed_download",
                "curl_starttransfer_time", "curl_total_time", "ip", "runtime", "timeout", "url"),
            Arrays.asList("0", "0.0000", "16.0000", "83.0000", "0.1912", "0.1912", "2a02:6b8:0:3400:0:2e5:1:822f", "0" +
                ".191629", "0.1", "http://games-api.browser.yandex.ru/api/v1/sa/card?app_id=ru.yandex" +
                ".searchplugin&app_platform=android&app_version=11020001&did=b85fef673e1f6cc01410caa5408b52ae&domain" +
                "=ru&lang=ru&reqid=1594891978.55575.79702.114261&uid=983483385&uuid=21333e969dd106366ebe71f692d3fcc5")
        );

        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [242838311594760405] [FastCGI] server (pid 7346): " +
                "accept restart: Resource temporarily unavailable () [d41d8] vla2-1021-rtc assessor efir",
            new Date(1594891978000L),
            "vla2-1021-rtc",
            "efir",
            "assessor",
            "1594891978.55575.79702.114261",
            "242838311594760405",
            "FastCGI",
            "",
            "server (pid 7346): accept restart: Resource temporarily unavailable",
            "",
            Arrays.asList(),
            Arrays.asList()
        );

        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [1209727511543520558] [monitoring] " +
                "[http_progressive] [alias=stream_by_id] " +
                "(MordaX::HTTP:1173->MordaX::HTTP:681->MordaX::HTTP:885->MordaX::HTTP:957->MP::Logit:203) [cfdea] " +
                "prestable-efir-vla-yp-2 prestable efir",
            new Date(1594891978000L),
            "prestable-efir-vla-yp-2",
            "efir",
            "prestable",
            "1594891978.55575.79702.114261",
            "1209727511543520558",
            "monitoring",
            "http_progressive",
            "",
            "MordaX::HTTP:1173->MordaX::HTTP:681->MordaX::HTTP:885->MordaX::HTTP:957->MP::Logit:203",
            Arrays.asList("alias"),
            Arrays.asList("stream_by_id")
        );

        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [] [FastCGI] manager (pid 4075): [reload] send " +
                "SIGTERM to 116053 (mordaxpl:270->FCGI::ProcManager:223->BornAgainKenny:237->BornAgainKenny:237" +
                "->BornAgainKenny::Reload:77) [02814] stable-portal-androidwidget-man-21 stable portal-androidwidget",
            new Date(1594891978000L),
            "stable-portal-androidwidget-man-21",
            "portal-androidwidget",
            "stable",
            "1594891978.55575.79702.114261",
            "",
            "FastCGI",
            "",
            "manager (pid 4075): [reload] send SIGTERM to 116053",
            "mordaxpl:270->FCGI::ProcManager:223->BornAgainKenny:237->BornAgainKenny:237->BornAgainKenny::Reload:77",
            Arrays.asList(),
            Arrays.asList()
        );

        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [] [warning] unknown L7BALANCER: " +
                "2a02:6b8:c15:2688:10e:c5bb:0:2f35 (mordaxpl:499->MordaX::FcgiRequest:34->MordaX::FcgiRequest:106" +
                "->MordaX::FcgiRequest:413->MP::Logit:203) [4f547] vla1-2462-rtc prestable portal-morda-legacy",
            new Date(1594891978000L),
            "vla1-2462-rtc",
            "portal-morda-legacy",
            "prestable",
            "1594891978.55575.79702.114261",
            "",
            "warning",
            "",
            "unknown L7BALANCER: 2a02:6b8:c15:2688:10e:c5bb:0:2f35",
            "mordaxpl:499->MordaX::FcgiRequest:34->MordaX::FcgiRequest:106->MordaX::FcgiRequest:413->MP::Logit:203",
            Arrays.asList(),
            Arrays.asList()
        );

        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [] [interr] [madm] cannot find pm " +
                "[/opt/www/bases/madm/production_ready/covid_chart_image_urls.pm] " +
                "(MordaX::Data_load:235->MordaX::Data_load:81->MordaX::Data_load:206->MordaX::Data_load:142->MP" +
                "::Logit:203) [bd263] sas1-7868-rtc assessor efir-assessors-rc",
            new Date(1594891978000L),
            "sas1-7868-rtc",
            "efir-assessors-rc",
            "assessor",
            "1594891978.55575.79702.114261",
            "",
            "interr",
            "madm",
            "cannot find pm [/opt/www/bases/madm/production_ready/covid_chart_image_urls.pm]",
            "MordaX::Data_load:235->MordaX::Data_load:81->MordaX::Data_load:206->MordaX::Data_load:142->MP::Logit:203",
            Arrays.asList(),
            Arrays.asList()
        );

        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [6417671321572883672] [nodata] [news] no runtime " +
                "data for news key = 'topnews_globtop_225_ru_ru'. fallback to memcache " +
                "(MordaX::Getdata:83->MordaX::Block::Topnews:891->MordaX::Block::Topnews:2362->MordaX::Block::Topnews" +
                ":1475->MordaX::Block::Topnews:319) [9095b] prestable-morda-vla-yp-183 prestable portal-morda",
            new Date(1594891978000L),
            "prestable-morda-vla-yp-183",
            "portal-morda",
            "prestable",
            "1594891978.55575.79702.114261",
            "6417671321572883672",
            "nodata",
            "news",
            "no runtime data for news key = 'topnews_globtop_225_ru_ru'. fallback to memcache",
            "MordaX::Getdata:83->MordaX::Block::Topnews:891->MordaX::Block::Topnews:2362->MordaX::Block::Topnews:1475" +
                "->MordaX::Block::Topnews:319",
            Arrays.asList(),
            Arrays.asList()
        );

        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [4f07ed1d90d093557cd9cf63e8f1c60f] [skipped] " +
                "[author_interr] Original message was skipped by regex 'no region_by_id \\\\[-2\\\\] for func'. " +
                "Description: 'skip \\\"no region_by_id [-2] for func ...\\\" message'. Regex is EXPIRED. " +
                "(MordaX::Block::Route:1071->MordaX::Block::Route:478->Geo::Utils:109->Geo:334->MP::Logit:203) " +
                "[e0622] stable-morda-man-yp-446 stable portal-morda",
            new Date(1594891978000L),
            "stable-morda-man-yp-446",
            "portal-morda",
            "stable",
            "1594891978.55575.79702.114261",
            "4f07ed1d90d093557cd9cf63e8f1c60f",
            "skipped",
            "author_interr",
            "Original message was skipped by regex 'no region_by_id \\\\[-2\\\\] for func'. Description: 'skip \\\"no" +
                " region_by_id [-2] for func ...\\\" message'. Regex is EXPIRED.",
            "MordaX::Block::Route:1071->MordaX::Block::Route:478->Geo::Utils:109->Geo:334->MP::Logit:203",
            Arrays.asList(),
            Arrays.asList()
        );

        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [] [overload] DISABLE\twait=0.278\twait_avg=0" +
                ".097\tload=70.740\trequests=7 (mordaxpl:518->MP::Logit:203) [fa231] " +
                "stable-portal-androidwidget-man-9 stable portal-androidwidget",
            new Date(1594891978000L),
            "stable-portal-androidwidget-man-9",
            "portal-androidwidget",
            "stable",
            "1594891978.55575.79702.114261",
            "",
            "overload",
            "",
            "DISABLE\twait=0.278\twait_avg=0.097\tload=70.740\trequests=7",
            "mordaxpl:518->MP::Logit:203",
            Arrays.asList(),
            Arrays.asList()
        );

        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [4240842351455114455] [blockerror] [home.cache] " +
                "[ip=::ffff:80.249.206.4] Quota exceeded.,meta:{\\\"morda\\\":\\\"yabrotab\\\"} " +
                "prestable-morda-vla-yp-28 prestable portal-morda",
            new Date(1594891978000L),
            "prestable-morda-vla-yp-28",
            "portal-morda",
            "prestable",
            "1594891978.55575.79702.114261",
            "4240842351455114455",
            "blockerror",
            "home.cache",
            "Quota exceeded.,meta:{\\\"morda\\\":\\\"yabrotab\\\"}",
            "",
            Arrays.asList("ip"),
            Arrays.asList("::ffff:80.249.206.4")
        );

        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [3757067311594797903] [tmplerror] Failed to " +
                "process template for request: JST code: -4 (Failed to receive app_host response) " +
                "(Handler::Utils:184->JSTemplate:56->JSTemplate:90->JSTemplate:72->MP::Logit:203) [90c16] " +
                "sas1-7733-rtc assessor portal-morda-assessor",
            new Date(1594891978000L),
            "sas1-7733-rtc",
            "portal-morda-assessor",
            "assessor",
            "1594891978.55575.79702.114261",
            "3757067311594797903",
            "tmplerror",
            "",
            "Failed to process template for request: JST code: -4 (Failed to receive app_host response)",
            "Handler::Utils:184->JSTemplate:56->JSTemplate:90->JSTemplate:72->MP::Logit:203",
            Arrays.asList(),
            Arrays.asList()
        );

        checker.check(
            "[2020-07-16T12:32:58] [] [] [debug]  [origin software=\\\"rsyslogd\\\" swVersion=\\\"8.16.0\\\" " +
                "x-pid=\\\"1491\\\" x-info=\\\"http://www.rsyslog.com\\\"] start (rsyslogd) " +
                "stable-portal-androidwidget-sas-42",
            new Date(1594891978000L),
            "stable-portal-androidwidget-sas-42",
            "",
            "",
            "",
            "",
            "debug",
            "",
            "[origin software=\\\"rsyslogd\\\" swVersion=\\\"8.16.0\\\" x-pid=\\\"1491\\\" x-info=\\\"http://www" +
                ".rsyslog.com\\\"] start",
            "rsyslogd",
            Arrays.asList(),
            Arrays.asList()
        );

        checker.check(
            "[2020-07-16T12:32:58] [] [] [debug] rsyslogd's userid changed to 33 (rsyslogd) stable-morda-sas-yp-329",
            new Date(1594891978000L),
            "stable-morda-sas-yp-329",
            "",
            "",
            "",
            "",
            "debug",
            "",
            "rsyslogd's userid changed to 33",
            "rsyslogd",
            Arrays.asList(),
            Arrays.asList()
        );

        checker.check(
            "[2020-07-16T12:32:58] [] [] [message] np calc post :  5 " +
                "(mordaxpl:233->Start:141->Start:117->MP::Logit:203) [fa47a] stable-morda-sas-yp-294 stable " +
                "portal-morda",
            new Date(1594891978000L),
            "stable-morda-sas-yp-294",
            "portal-morda",
            "stable",
            "",
            "",
            "message",
            "",
            "np calc post :  5",
            "mordaxpl:233->Start:141->Start:117->MP::Logit:203",
            Arrays.asList(),
            Arrays.asList()
        );

        checker.check(
            "[2020-07-16T12:32:58] [] [9948485741594827762] [mock] OVERRIDE Component Mail " +
                "(mordaxpl:586->Handler::Videohub:173->Handler::Utils:146->Handler::Utils:255->MP::Logit:203) [f3e8f]" +
                " sas2-8126-rtc unstable efir",
            new Date(1594891978000L),
            "sas2-8126-rtc",
            "efir",
            "unstable",
            "",
            "9948485741594827762",
            "mock",
            "",
            "OVERRIDE Component Mail",
            "mordaxpl:586->Handler::Videohub:173->Handler::Utils:146->Handler::Utils:255->MP::Logit:203",
            Arrays.asList(),
            Arrays.asList()
        );

        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [8395728161519466852] [serviceerror] [yabrotab] " +
                "[ip=::ffff:176.59.13.246] fetch: TypeError: Failed to fetch stable-morda-sas-yp-113 stable " +
                "portal-morda",
            new Date(1594891978000L),
            "stable-morda-sas-yp-113",
            "portal-morda",
            "stable",
            "1594891978.55575.79702.114261",
            "8395728161519466852",
            "serviceerror",
            "yabrotab",
            "fetch: TypeError: Failed to fetch",
            "",
            Arrays.asList("ip"),
            Arrays.asList("::ffff:176.59.13.246")
        );

        checker.check(
            "[2020-07-16T12:32:58] [1594891978.55575.79702.114261] [8330884811504020250] [clienterror] [unknown] " +
                "[ip=::ffff:83.149.19.157] Unhandled rejection: Failed to fetch (stack:) stable-morda-sas-yp-33 " +
                "stable portal-morda",
            new Date(1594891978000L),
            "stable-morda-sas-yp-33",
            "portal-morda",
            "stable",
            "1594891978.55575.79702.114261",
            "8330884811504020250",
            "clienterror",
            "unknown",
            "Unhandled rejection: Failed to fetch",
            "stack:",
            Arrays.asList("ip"),
            Arrays.asList("::ffff:83.149.19.157")
        );
    }
}
