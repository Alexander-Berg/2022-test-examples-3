package ru.yandex.market.logshatter.parser.lotalot;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;


/**
 * @author antontodua
 */
public class LotalotNginxAccessTskvLogParserTest {

    @Test
    public void parseOrders() throws Exception {
        LogParserChecker checker = new LogParserChecker(new LotalotNginxAccessTskvLogParser());
        Date date = Date.from(LocalDateTime.of(2018, 6, 7, 8, 40, 46).toInstant(ZoneOffset.ofHours(3)));
        checker.check("tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2018-06-07T08:40:46\ttimezone=+0300\t" +
                "status=200\tprotocol=HTTP/1.1\tmethod=GET\trequest=/api-v1/dealer/app/orders?" +
                "uuid=1fcd7c1f25e09cbc5203c52337cfd921&deviceId=9bd18bc12276c3c8e3e7a2e8c393ed63&" +
                "uid=36841369&page=1&state=active&rid=cji445mpb00g5mentrytf3mcm\t" +
                "referer=-\tcookies=_ym_uid=1527020799864781625; mda=0; yandexuid=6654893771527020797; " +
                "yp=1842380797.yrts.1527020797; " +
                "i=BZwg+sbw2QIkVNXVcF8fzCRHSq0hIjp7uMraTjITnsrlyRmFP2XHa2Zy9lGcRDTIElrey6V17MDWaBQAJrA+5cGfIj8=\t" +
                "user_agent=okhttp/3.9.1\tvhost=yandex.ru\tip=2a02:6b8:c0e:29:0:577:9ecf:3858\t" +
                "x_forwarded_for=93.159.238.125, 2a02:6b8:b000:163:225:90ff:fe94:9ec0, " +
                "2a02:6b8:c0f:159b:0:4167:8b88:72b0\tx_real_ip=2a02:6b8:b000:163:225:90ff:fe94:9ec0\t" +
                "bytes_sent=8202\tpage_id=-\tpage_type=-\treq_id=1528350046000/b8d79cd46e554cdf61d5f145e3f7b2fc\t" +
                "req_id_seq=-\tupstream_resp_time=0.423\treq_time=0.423\tscheme=http\tdevice_type=-\t" +
                "x_sub_req_id=-\tyandexuid=-\tssl_handshake_time=-\tmarket_buckets=-\t" +
                "upstream_addr=127.0.0.1:32010\tupstream_header_time=0.423\tupstream_status=200\t" +
                "market_req_id=1528350046000/b8d79cd46e554cdf61d5f145e3f7b2fc\tmsec=1528350046.428\ttvm=DISABLED",
            date, checker.getHost(), "orders", "GET", 200, 423, 8202);
    }

    @Test
    public void parseOrderItems() throws Exception {
        LogParserChecker checker = new LogParserChecker(new LotalotNginxAccessTskvLogParser());
        Date date = Date.from(LocalDateTime.of(2018, 6, 8, 11, 31, 33).toInstant(ZoneOffset.ofHours(3)));
        checker.check("tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2018-06-08T11:31:33\ttimezone=+0300\t" +
                "status=200\tprotocol=HTTP/1.1\tmethod=GET\trequest=/api-v1/dealer/app/orders/3802235/items/3989338?" +
                "uuid=941bbb5871ac021f82f5b29dc4fcf9b8&deviceId=4ACFE966-5AFC-462B-90EF-DF5B03F69858&" +
                "uid=648382272&rid=cji5pp33i0014mentu3de1az9\treferer=-\tcookies=_ym_uid=1526650023593974021; " +
                "i=8L7qpTWCv0lyla9qNrixEJeamIkp4vGA/Q1UogGpzzDzsM2msygy3NtPMWCAz9zLJFVrSQKQkyPGvZZvHb7g5xxMgNY=; " +
                "mda=0; yandexuid=9611011681526650023; yp=1842010023.yrts.1526650023\t" +
                "user_agent=Lotalot/803 CFNetwork/811.5.4 Darwin/16.7.0\t" +
                "vhost=yandex.ru\tip=2a02:6b8:c0e:29:0:577:9ecf:3858\tx_forwarded_for=93.158.146.204, " +
                "2a02:6b8:c0e:68:0:604:db7:a515, 2a02:6b8:c0f:159b:0:4167:a490:1e3a\t" +
                "x_real_ip=2a02:6b8:c0e:68:0:604:db7:a515\tbytes_sent=2654\tpage_id=-\tpage_type=-\t" +
                "req_id=1528446692146/f989884e561bbfa623cc29b2c0fc0b5d\treq_id_seq=-\tupstream_resp_time=1.306\t" +
                "req_time=1.306\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=-\tssl_handshake_time=-\t" +
                "market_buckets=-\tupstream_addr=127.0.0.1:32010\tupstream_header_time=1.306\tupstream_status=200\t" +
                "market_req_id=1528446692146/f989884e561bbfa623cc29b2c0fc0b5d\tmsec=1528446693.456\ttvm=DISABLED",
            date, checker.getHost(), "orders_id_items_id", "GET", 200, 1306, 2654);
    }

    @Test
    public void parseCheckout() throws Exception {
        LogParserChecker checker = new LogParserChecker(new LotalotNginxAccessTskvLogParser());
        Date date = Date.from(LocalDateTime.of(2018, 6, 8, 8, 29, 46).toInstant(ZoneOffset.ofHours(3)));
        checker.check("tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2018-06-08T08:29:46\ttimezone=+0300\t" +
                "status=200\tprotocol=HTTP/1.1\tmethod=GET\trequest=/api-v1/dealer/app/checkout/" +
                "cji5j73j60j3umentuhveq0rq?uuid=bceb243d87e3d7ab130a1b29fc7a996f&" +
                "deviceId=1EFAAD73-7ACC-4CB1-A68C-48C126E6155C&uid=221684784&rid=cji5j7csy0j3xmentayk3ng75\t" +
                "referer=-\tcookies=i=IBu+YmfgW99GNc7lQy8A8NKbiyFVnglB9EZSN+eqJcrhuGoBHrQYBCBTwIJ80ljWuzBrmefrNBAvu+" +
                "MC3tHnyrs5qaU=; yandexuid=5588639831528262921\tuser_agent=Lotalot/803 CFNetwork/897.15 " +
                "Darwin/17.5.0\tvhost=yandex.ru\tip=2a02:6b8:c0e:29:0:577:9ecf:3858\tx_forwarded_for=93.159.238.125, " +
                "2a02:6b8:b000:624:922b:34ff:fecf:4158, 2a02:6b8:c0f:71f:0:4167:4baa:cd77\t" +
                "x_real_ip=2a02:6b8:b000:624:922b:34ff:fecf:4158\tbytes_sent=518\tpage_id=-\tpage_type=-\t" +
                "req_id=1528435786665/09f4bc1383cc36a27e8927841a139f3a\treq_id_seq=-\tupstream_resp_time=0.065\t" +
                "req_time=0.065\tscheme=http\tdevice_type=-\tx_sub_req_id=-\tyandexuid=-\tssl_handshake_time=-\t" +
                "market_buckets=-\tupstream_addr=[::1]:32010\tupstream_header_time=0.065\tupstream_status=200\t" +
                "market_req_id=1528435786665/09f4bc1383cc36a27e8927841a139f3a\tmsec=1528435786.737\ttvm=DISABLED",
            date, checker.getHost(), "checkout_id", "GET", 200, 65, 518);
    }
}
