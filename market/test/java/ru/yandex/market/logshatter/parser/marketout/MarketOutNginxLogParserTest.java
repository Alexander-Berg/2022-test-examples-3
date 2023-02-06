package ru.yandex.market.logshatter.parser.marketout;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;

public class MarketOutNginxLogParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new MarketOutNginxLogParser());

        checker.check(
            "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2016-09-27T02:38:31\ttimezone=+0300\tstatus=200" +
                "\tprotocol=HTTP/1.0\tmethod=GET\trequest=/ping\treferer=-\tcookies=-\tuser_agent=KeepAliveClient" +
                "\tvhost=213.180.204.120:80\tip=87.250.234" +
                ".206\tx_forwarded_for=-\tx_real_ip=-\tbytes_sent=194\treq_id=6959aee785fdf47b4c030c021de7fbf9" +
                "\treq_id_seq=-\tupstream_resp_time=0.000\treq_time=0" +
                ".047\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=-\tssl_handshake_time=-\tmarket_buckets" +
                "=-\tupstream_addr=127.0.0.1:25425\tupstream_header_time=0.000\tupstream_status=200",
            new Date(1474933111000L), Environment.UNKNOWN, checker.getHost(), "ping", 200, 0, false
        );

        checker.check(
            "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2016-09-27T06:35:23\ttimezone=+0300\tstatus=404" +
                "\tprotocol=HTTP/1.1\tmethod=GET\trequest=/gurudaemon/PopularModels?jsonout=1&n=10&region=51" +
                "&yandexuid=9777191361474947323&type=mini&hid=13041512\treferer=-\tcookies=-\tuser_agent=-\tvhost" +
                "=marketgurudaemon.yandex.ru\tip=2a02:6b8:0:2521:875e:8282:53dd:2407\tx_forwarded_for=87.250.232" +
                ".149\tx_real_ip=87.250.232.149\tbytes_sent=290\treq_id=2eddfde9e3fded3a6684958091f5c0db\treq_id_seq" +
                "=-\tupstream_resp_time=0.001\treq_time=0" +
                ".001\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=-\tssl_handshake_time=-\tmarket_buckets" +
                "=-\tupstream_addr=unix:/tmp/fcgi_gurudaemon.sock\tupstream_header_time=0.001\tupstream_status=404",
            new Date(1474947323000L), Environment.UNKNOWN, checker.getHost(), "PopularModelsCategory", 404, 1, false
        );

        checker.check(
            "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2016-09-27T06:35:42\ttimezone=+0300\tstatus=200" +
                "\tprotocol=HTTP/1.1\tmethod=GET\trequest=/gurudaemon/PopularModels?jsonout=1&n=10&region=213" +
                "&yandexuid=4967042471469345614&type=mini\treferer=-\tcookies=-\tuser_agent=-\tvhost=marketgurudaemon" +
                ".yandex.ru\tip=2a02:6b8:0:2521:875e:8282:53dd:2407\tx_forwarded_for=87.250.232.230\tx_real_ip=87.250" +
                ".232.230\tbytes_sent=4431\treq_id=2c502ffcf2d37fc1c6a25d50fd86195b\treq_id_seq=-\tupstream_resp_time" +
                "=0.198\treq_time=0.198\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=-\tssl_handshake_time" +
                "=-\tmarket_buckets=-\tupstream_addr=unix:/tmp/fcgi_gurudaemon.sock\tupstream_header_time=0" +
                ".197\tupstream_status=200",
            new Date(1474947342000L), Environment.UNKNOWN, checker.getHost(), "PopularModelsMain", 200, 198, false
        );
    }

    public void testDevelopmentEnvironment() throws Exception {
        LogParserChecker checker = new LogParserChecker(new MarketOutNginxLogParser());

        checker.setParam("logbroker://market-health-dev", "DEVELOPMENT");
        checker.setParam("logbroker://market-health-testing", "TESTING");
        checker.setParam("logbroker://market-health-prestable", "PRESTABLE");
        checker.setParam("logbroker://market-health-stable", "PRODUCTION");

        checker.setOrigin("logbroker://market-health-dev");
        checker.check(
            "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2016-09-27T02:38:31\ttimezone=+0300\tstatus=200" +
                "\tprotocol=HTTP/1.0\tmethod=GET\trequest=/ping\treferer=-\tcookies=-\tuser_agent=KeepAliveClient" +
                "\tvhost=213.180.204.120:80\tip=87.250.234" +
                ".206\tx_forwarded_for=-\tx_real_ip=-\tbytes_sent=194\treq_id=6959aee785fdf47b4c030c021de7fbf9" +
                "\treq_id_seq=-\tupstream_resp_time=0.000\treq_time=0" +
                ".047\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=-\tssl_handshake_time=-\tmarket_buckets" +
                "=-\tupstream_addr=127.0.0.1:25425\tupstream_header_time=0.000\tupstream_status=200",
            new Date(1474933111000L), Environment.DEVELOPMENT, checker.getHost(), "ping", 200, 0, false
        );

        checker.setOrigin("logbroker://market-health-testing");
        checker.check(
            "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2016-09-27T02:38:31\ttimezone=+0300\tstatus=200" +
                "\tprotocol=HTTP/1.0\tmethod=GET\trequest=/ping\treferer=-\tcookies=-\tuser_agent=KeepAliveClient" +
                "\tvhost=213.180.204.120:80\tip=87.250.234" +
                ".206\tx_forwarded_for=-\tx_real_ip=-\tbytes_sent=194\treq_id=6959aee785fdf47b4c030c021de7fbf9" +
                "\treq_id_seq=-\tupstream_resp_time=0.000\treq_time=0" +
                ".047\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=-\tssl_handshake_time=-\tmarket_buckets" +
                "=-\tupstream_addr=127.0.0.1:25425\tupstream_header_time=0.000\tupstream_status=200",
            new Date(1474933111000L), Environment.TESTING, checker.getHost(), "ping", 200, 0, false
        );

        checker.setOrigin("logbroker://market-health-prestable");
        checker.check(
            "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2016-09-27T02:38:31\ttimezone=+0300\tstatus=200" +
                "\tprotocol=HTTP/1.0\tmethod=GET\trequest=/ping\treferer=-\tcookies=-\tuser_agent=KeepAliveClient" +
                "\tvhost=213.180.204.120:80\tip=87.250.234" +
                ".206\tx_forwarded_for=-\tx_real_ip=-\tbytes_sent=194\treq_id=6959aee785fdf47b4c030c021de7fbf9" +
                "\treq_id_seq=-\tupstream_resp_time=0.000\treq_time=0" +
                ".047\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=-\tssl_handshake_time=-\tmarket_buckets" +
                "=-\tupstream_addr=127.0.0.1:25425\tupstream_header_time=0.000\tupstream_status=200",
            new Date(1474933111000L), Environment.PRESTABLE, checker.getHost(), "ping", 200, 0, false
        );

        checker.setOrigin("logbroker://market-health-stable");
        checker.check(
            "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2016-09-27T02:38:31\ttimezone=+0300\tstatus=200" +
                "\tprotocol=HTTP/1.0\tmethod=GET\trequest=/ping\treferer=-\tcookies=-\tuser_agent=KeepAliveClient" +
                "\tvhost=213.180.204.120:80\tip=87.250.234" +
                ".206\tx_forwarded_for=-\tx_real_ip=-\tbytes_sent=194\treq_id=6959aee785fdf47b4c030c021de7fbf9" +
                "\treq_id_seq=-\tupstream_resp_time=0.000\treq_time=0" +
                ".047\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=-\tssl_handshake_time=-\tmarket_buckets" +
                "=-\tupstream_addr=127.0.0.1:25425\tupstream_header_time=0.000\tupstream_status=200",
            new Date(1474933111000L), Environment.PRODUCTION, checker.getHost(), "ping", 200, 0, false
        );
    }
}
