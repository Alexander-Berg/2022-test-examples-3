package ru.yandex.market.logshatter.parser.recommender;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class RecommenderNginxLogParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new RecommenderNginxLogParser());
        checker.checkEmpty(
            "tskv\ttskv_format=tskv-market-default\ttimestamp=2019-02-07T18:36:23\ttimezone=+0300\tstatus=200" +
                "\tprotocol=HTTP/1.1\tmethod=GET\trequest=/ping\treferer=-\tcookies=-\tuser_agent=-\tvhost" +
                "=recommender.vs.market.yandex.net\tip=2a02:6b8:c0e:f:0:577:c351:ea1f\tx_forwarded_for=-\tx_real_ip" +
                "=-\tbytes_sent=320\tpage_id=-\tpage_type=-\treq_id=1549553783193/e8d90db9f3c615f277a508826cde789e" +
                "\treq_id_seq=-\tupstream_resp_time=0.030\treq_time=0" +
                ".014\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=\tssl_handshake_time=-\tmarket_buckets" +
                "=-\tupstream_addr=[::1]:30366\tupstream_header_time=0" +
                ".001\tupstream_status=200\tmarket_req_id=1549553783193/e8d90db9f3c615f277a508826cde789e\tmsec" +
                "=1549553783.197\ttvm=DISABLED\ticookie=-");
        checker.checkEmpty(
            "tskv\ttskv_format=tskv-market-default\ttimestamp=2019-02-07T18:36:23\ttimezone=+0300\tstatus=200" +
                "\tprotocol=HTTP/1.1\tmethod=GET\trequest=/stat\treferer=-\tcookies=-\tuser_agent=-\tvhost" +
                "=recommender.vs.market.yandex.net\tip=2a02:6b8:c01:6f:0:577:cc45:34f3\tx_forwarded_for=-\tx_real_ip" +
                "=-\tbytes_sent=320\tpage_id=-\tpage_type=-\treq_id=1549553783453/2e7d5f3316c29484500af88cb2dd7108" +
                "\treq_id_seq=-\tupstream_resp_time=0.020\treq_time=0" +
                ".020\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=\tssl_handshake_time=-\tmarket_buckets" +
                "=-\tupstream_addr=[::1]:30366\tupstream_header_time=0" +
                ".001\tupstream_status=200\tmarket_req_id=1549553783453/2e7d5f3316c29484500af88cb2dd7108\tmsec" +
                "=1549553783.473\ttvm=DISABLED\ticookie=-");
        checker.check(
            "tskv\ttskv_format=tskv-market-default\ttimestamp=2020-02-07T18:36:23\ttimezone=+0300\tstatus=200" +
                "\tprotocol=HTTP/1.1\tmethod=GET\trequest=/viewedModels?itemCount=100&userId=yandexuid:12345678" +
                "\treferer=-\tcookies=-\tuser_agent=-\tvhost=recommender.vs.market.yandex" +
                ".net\tip=2a02:6b8:c02:45a:0:577:e299:ec6c\tx_forwarded_for=-\tx_real_ip=-\tbytes_sent=320\tpage_id" +
                "=-\tpage_type=-\treq_id=1549553783686/e1fed79291bb832bafee230ec286b99b\treq_id_seq" +
                "=-\tupstream_resp_time=0.001\treq_time=0" +
                ".007\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=\tssl_handshake_time=-\tmarket_buckets" +
                "=-\tupstream_addr=[::1]:30366\tupstream_header_time=0" +
                ".001\tupstream_status=200\tmarket_req_id=1549553783686/e1fed79291bb832bafee230ec286b99b\tmsec" +
                "=1549553783.693\ttvm=DISABLED\ticookie=-",
            new Date(1581089783000L), checker.getHost(), 1, 7, 1, "viewedModels", 200, "1549553783686" +
                "/e1fed79291bb832bafee230ec286b99b");
    }
}
