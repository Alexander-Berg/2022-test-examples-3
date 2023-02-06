package ru.yandex.market.logshatter.parser.nginx;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;
import ru.yandex.market.logshatter.parser.trace.NginxCacheStatus;

/**
 * @author Pavel Yelkin <a href="mailto:pashayelkin@yandex-team.ru"></a>
 * @date 31/05/2019
 */
public class GooglebotCacheLogParserTest {

    LogParserChecker checker = new LogParserChecker(new GooglebotCacheLogParser());

    @Test
    public void parse() throws Exception {
        Map<String, NginxCacheStatus> statuses = new HashMap<>();
        statuses.put("MISS", NginxCacheStatus.MISS);
        statuses.put("-", NginxCacheStatus.UNSET);

        for (Map.Entry<String, NginxCacheStatus> status : statuses.entrySet()) {
            String line = String.format("tskv\ttskv_format=tskv-googlebot\ttimestamp=2019-05-29T16:48:51\ttimezone" +
                "=+0300\tstatus=302\tprotocol=HTTP/1.1\tmethod=GET\trequest=/\treferer=-\tcookies=-\tuser_agent=curl" +
                "/7.35.0\tvhost=m.beru.ru\tip=::1\tx_forwarded_for=-\tx_real_ip=-\tbytes_sent=1700\tpage_id=blue" +
                "-market:index\tpage_type=node\treq_id=1559137731494/7ac3377a65ea06a76931d44666e71e27\treq_id_seq" +
                "=d941f56437002274ecf9bf84fc951c7a\tupstream_resp_time=0.080\treq_time=0" +
                ".080\tscheme=https\tdevice_type=market_front_blue_touch\tx_sub_req_id=-\tyandexuid" +
                "=4092353271559137731\tssl_handshake_time=0.002\tmarket_buckets=141043,0,92;88589,0,25;140894,0,47;" +
                "141784,0,81;141766,0,95;140616,0,23;133216,0," +
                "82\tupstream_addr=[2a02:6b8::69]:443\tupstream_header_time=0" +
                ".080\tupstream_status=302\tmarket_req_id=1559137731494/7ac3377a65ea06a76931d44666e71e27\tmsec" +
                "=1559137731.574\ttvm=DISABLED\ticookie=-\tcache=%s\n", status.getKey());

            checker.setOrigin("market-health-dev");
            checker.setParam("logbroker://market-health-dev", "DEVELOPMENT");

            checker.check(
                line,
                1559137731,
                checker.getHost(),
                "m.beru.ru",
                "/",
                "GET",
                302,
                80,
                true,
                "blue-market:index",
                "node",
                "market_front_blue_touch",
                "::1",
                "curl/7.35.0",
                new Integer[]{141043, 88589, 140894, 141784, 141766, 140616, 133216},
                "4092353271559137731",
                "",
                80,
                2,
                1700,
                "1559137731494/7ac3377a65ea06a76931d44666e71e27",
                "",
                "DISABLED",
                "",
                -2,
                "",
                "",
                -2L,
                new String[]{},
                new String[]{},
                status.getValue(),
                Environment.DEVELOPMENT
            );
        }
    }
}
