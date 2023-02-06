package ru.yandex.market.logshatter.parser.trace;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.EnvironmentMapper;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

public class NginxTraceLogParserTest {

    private LogParserChecker checker;

    @Before
    public void before() {
        LogParserChecker checker = new LogParserChecker(new NginxTraceLogParser());
        checker.setParam(EnvironmentMapper.LOGBROKER_PROTOCOL_PREFIX + checker.getOrigin(), "DEVELOPMENT");
        this.checker = checker;
    }

    @Test
    public void testLogParserChecker() throws Exception {
        String line1 = "tskv" +
            "\ttskv_format=access-log-cs-vs-tools" +
            "\ttimestamp=2017-05-18T06:52:34" +
            "\ttimezone=+0300" +
            "\tstatus=200" +
            "\tprotocol=HTTP/1.1" +
            "\tmethod=GET" +
            "\trequest=/ping" +
            "\treferer=-" +
            "\tcookies=-" +
            "\tuser_agent=-" +
            "\tvhost=checkouter.market.http.yandex.net" +
            "\tip=2a02:6b8:c0e:29:0:577:9ecf:3858" +
            "\tx_forwarded_for=-" +
            "\tx_real_ip=-" +
            "\tbytes_sent=350" +
            "\tpage_id=-" +
            "\tpage_type=-" +
            "\treq_id=1495079554328/5bb9a573f8a235b24bd0001259d58b98" +
            "\treq_id_seq=-" +
            "\tupstream_resp_time=0.002" +
            "\treq_time=0.002" +
            "\tscheme=https" +
            "\tdevice_type=-" +
            "\tx_sub_req_id=-" +
            "\tyandexuid=-" +
            "\tssl_handshake_time=0.002" +
            "\tmarket_buckets=-" +
            "\tupstream_addr=127.0.0.1:3900" +
            "\tupstream_header_time=0.001" +
            "\tupstream_status=200" +
            "\tmarket_req_id=1495079554328/5bb9a573f8a235b24bd0001259d58b98" +
            "\tmsec=1495079554.330" +
            "\ttvm=DISABLED";

        checker.check(line1, new Date(1495079554328L),
            1495079554328L,
            "5bb9a573f8a235b24bd0001259d58b98",
            new Integer[0],
            1495079554328L,
            1495079554330L,
            2,
            RequestType.PROXY,
            "nginx",
            "hostname.test",
            "",
            "2a02:6b8:c0e:29:0:577:9ecf:3858",
            "",
            "127.0.0.1:3900",
            Environment.DEVELOPMENT,
            "",
            200,
            1,
            "",
            "https",
            "GET",
            "/ping",
            "",
            "",
            new Object[0],
            new Object[0],
            new String[]{"upstream"},
            new Long[]{1495079554330L},
            new Integer[]{},
            "",
            "",
            350
        );
    }

}
