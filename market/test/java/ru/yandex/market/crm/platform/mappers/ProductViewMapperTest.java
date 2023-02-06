package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.models.ProductView;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ProductViewMapperTest {

    private ProductViewMapper mapper = new ProductViewMapper();

    private static String logLineWithRequest(String request) {
        return String.format(
                "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2018-07-02T05:18:00\ttimezone=+0300\tstatus=200" +
                        "\t" +
                        "protocol=HTTP/1.1\tmethod=GET\trequest=%s\treferer=-\tcookies=-\t" +
                        "yandexuid=9657821121530497880\tvhost=market.yandex.ru",
                request);
    }

    @Test
    public void testMapLine() {
        String line = "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2018-07-02T05:18:00\ttimezone=+0300" +
                "\tstatus=200\t" +
                "protocol=HTTP/1.1\tmethod=GET\trequest=/product/76644386?show-uid=304978584800705038316003&nid=54544" +
                "&context=search\t" +
                "referer=https://market.yandex.ru/search?cvredirect=2&text=%D0%BD%D0%BE%D1%83%D1%82%D0%B1%D1%83%D0%BA" +
                "%D0%B8%" +
                "20Lenovo%20hdmi%20%D1%80%D0%B0%D0%B7%D1%8A%D0%B5%D0%BC&local-offers-first=0\t" +
                "cookies=yandexuid=2964307181520737401; _ym_uid=152074063711752880; mda=0; my=YwA=; " +
                "fuid01=5aa5595b0f45884c" +
                ".1MHt61gWK6yowSUeaNmDw5P0EgY83kza5U901Iaum97k8lQ1B10ZNlFvKj7oXsnlAk2ASQsSj6P0DJLuVwmgqXbXD6x81NMHp39bU-lola6Eaz3axgoIclC0Gf3szi1U; " +
                "L=AzNnZ1gEa1JofVRSaGZVTUlee0BUQFF+Fjw6IlACOiM7GwoUHkQUDQ==.1520886755.13436.353680" +
                ".849c8ce8497bdcccef8fd2d3678ecab2; " +
                "yandex_login=nm@metricsnet.ru; yandex_gid=213; zm=m-white_bender.webp" +
                ".css-https-www%3Awww_e0Br4ME_ME-g0o82nGwXh8botT8%3Al; " +
                "i=yCNv8lIGAroDC+UhNcp9tuWKFn+LKYoG5ye6EZl2qcNSD6w7anQkxdmrHUZJ1o5BCWEay61+9DK94zO7rWNfpe+lonY=; " +
                "_ym_isad=2; " +
                "Session_id=3:1530445369.5.0.1520886755869:M-mULg:85.1|1130000022078033.0.2|183912.116320" +
                ".XXXXXXXXXXXXXXXXXXXXXXXXXXX; " +
                "sessionid2=3:1530445369.5.0.1520886755869:M-mULg:85.1|1130000022078033.0.2|183912.563402" +
                ".XXXXXXXXXXXXXXXXXXXXXXXXXXX; " +
                "_ym_d=1530497800; yc=1530757002.zen.cach%3A1530501399; " +
                "market_ys=1530497808652548-692753484316406069537142-man1-5378; " +
                "pof=%7B%22clid%22%3A%5B%22703%22%5D%2C%22mclid%22%3Anull%2C%22distr_type%22%3Anull%2C%22vid%22" +
                "%3Anull%2C%22opp%22%3Anull%7D; " +
                "cpa-pof=%7B%22clid%22%3A%5B%22703%22%5D%2C%22mclid%22%3Anull%2C%22distr_type%22%3Anull%2C%22vid%22" +
                "%3Anull%2C%22opp%22%3Anull%7D; " +
                "uid=AABcEls5ixx/GgEQByEoAg==; _ym_visorc_160656=b; " +
                "first_visit_time=2018-07-02T05%3A17%3A03%2B03%3A00; " +
                "yandexmarket=48; _ym_visorc_45411513=b; cart_movement_checked=1; currentRegionId=213; " +
                "currentRegionName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D1%83; " +
                "fonts-loaded=1; head-banner=%7B%22closingCounter%22%3A0%2C%22showingCounter%22%3A3%2C" +
                "%22shownAfterClicked%22%3Afalse%2C%22isClicked%22%3Afalse%7D; " +
                "parent_reqid_seq=e1959b65a6494c65019be9215c07b167%2Ca8d62eebd2747a7bd2b6019ffea56924" +
                "%2Cdc177b6510c149628407d50b4e2e4ea2; " +
                "HISTORY_AUTH_SESSION=ebf069ad; settings-notifications-popup=%7B%22showCount%22%3A1%2C%22showDate%22" +
                "%3A17714%7D\t" +
                "user_agent=Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67" +
                ".0.3396.99 Safari/537.36\tvhost=market.yandex.ru\t" +
                "ip=46.148.226.46\tx_forwarded_for=46.148.226.46\tx_real_ip=46.148.226" +
                ".46\tbytes_sent=288479\tpage_id=market:product\tpage_type=node\t" +
                "req_id=7a02d3dc339808d0fcf133e1f701e9a3\treq_id_seq=e1959b65a6494c65019be9215c07b167," +
                "a8d62eebd2747a7bd2b6019ffea56924,dc177b6510c149628407d50b4e2e4ea2,7a02d3dc339808d0fcf133e1f701e9a3\t" +
                "upstream_resp_time=0.403\treq_time=0" +
                ".403\tscheme=https\tdevice_type=desktop\tx_sub_req_id=-\tyandexuid=2964307181520737401\t" +
                "ssl_handshake_time=0.002\tmarket_buckets=81768,0,51;67036,0," +
                "6\tupstream_addr=[::1]:23432\tupstream_header_time=0.147\t" +
                "upstream_status=200\tmarket_req_id=1530497879629/894fccc149b990ddbc5a6d8d06629e23\tmsec=1530497880" +
                ".032\ttvm=DISABLED";

        List<ProductView> res = mapper.apply(line.getBytes());
        assertEquals(1, res.size());

        ProductView expected = ProductView.newBuilder()
                .setUserIds(UserIds.newBuilder().setPuid(1130000022078033L).setYandexuid("2964307181520737401"))
                .setTimestamp(1530478800000L)
                .addHistory(1530478800000L)
                .setId(76644386)
                .setRgb(RGBType.GREEN)
                .build();

        assertEquals(expected, res.get(0));
    }

    @Test
    public void testRgbIsNotSetWhenHostIsUnknown() {
        String line = "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2018-07-02T05:18:00\ttimezone=+0300" +
                "\tstatus=200\t" +
                "protocol=HTTP/1.1\tmethod=GET\trequest=/product/76644386?show-uid=304978584800705038316003&nid=54544" +
                "&context=search\t" +
                "yandexuid=2964307181520737401\t" +
                "vhost=SOME_STRANGE_HOST";

        List<ProductView> res = mapper.apply(line.getBytes());
        assertEquals(1, res.size());

        ProductView expected = ProductView.newBuilder()
                .setUserIds(UserIds.newBuilder().setYandexuid("2964307181520737401"))
                .setTimestamp(1530478800000L)
                .addHistory(1530478800000L)
                .setId(76644386)
                .build();

        ProductView actual = res.get(0);
        assertEquals(expected, actual);
        assertEquals(RGBType.UNKNOWN_TYPE, actual.getRgb());
    }

    @Test
    public void testNoPuid() {
        String noCookiesLine = "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2018-07-02T05:17:54\ttimezone" +
                "=+0300\tstatus=200\t" +
                "protocol=HTTP/1.1\tmethod=GET\trequest=/product/11145535/?hid=91491\treferer=-\tcookies=-\t" +
                "user_agent=Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)\tvhost=beru.ru\tip=5.255" +
                ".253.36\t" +
                "x_forwarded_for=5.255.253.36\tx_real_ip=5.255.253" +
                ".36\tbytes_sent=24897\tpage_id=market:product\tpage_type=node\t" +
                "req_id=23cb43c65e2c77ecd582503b55988bca\treq_id_seq=23cb43c65e2c77ecd582503b55988bca" +
                "\tupstream_resp_time=2.188\t" +
                "req_time=2.188\tscheme=https\tdevice_type=desktop\tx_sub_req_id=-\tyandexuid=4692995751530497872" +
                "\tssl_handshake_time=0.002\t" +
                "market_buckets=-\tupstream_addr=[::1]:8445\tupstream_header_time=0.435\tupstream_status=200\t" +
                "market_req_id=1530497872114/aa771e43f6c41466170b0d73636eaece\tmsec=1530497874.302\ttvm=DISABLED";

        List<ProductView> res = mapper.apply(noCookiesLine.getBytes());
        assertEquals(1, res.size());

        ProductView expected = ProductView.newBuilder()
                .setUserIds(UserIds.newBuilder().setYandexuid("4692995751530497872"))
                .setTimestamp(1530478800000L)
                .addHistory(1530478800000L)
                .setId(11145535)
                .setRgb(RGBType.BLUE)
                .build();

        assertEquals(expected, res.get(0));
    }

    @Test
    public void testIgnoreDashAsYandexuid() {
        String noCookiesLine = "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2018-07-02T05:17:54\ttimezone" +
                "=+0300\tstatus=200\t" +
                "protocol=HTTP/1.1\tmethod=GET\trequest=/product/11145535/?hid=91491\treferer=-\tcookies=-\t" +
                "user_agent=Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)\tvhost=beru.ru\tip=5.255" +
                ".253.36\t" +
                "x_forwarded_for=5.255.253.36\tx_real_ip=5.255.253" +
                ".36\tbytes_sent=24897\tpage_id=market:product\tpage_type=node\t" +
                "req_id=23cb43c65e2c77ecd582503b55988bca\treq_id_seq=23cb43c65e2c77ecd582503b55988bca" +
                "\tupstream_resp_time=2.188\t" +
                "req_time=2.188\tscheme=https\tdevice_type=desktop\tx_sub_req_id=-\tyandexuid=-\tssl_handshake_time=0" +
                ".002\t" +
                "market_buckets=-\tupstream_addr=[::1]:8445\tupstream_header_time=0.435\tupstream_status=200\t" +
                "market_req_id=1530497872114/aa771e43f6c41466170b0d73636eaece\tmsec=1530497874.302\ttvm=DISABLED";

        List<ProductView> res = mapper.apply(noCookiesLine.getBytes());
        assertTrue(res.isEmpty());
    }

    @Test
    public void testSkipNotProductRequest() {
        String pingRequest = "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2018-07-02T05:17:57\ttimezone=+0300" +
                "\tstatus=200\t" +
                "protocol=HTTP/1.1\tmethod=GET\trequest=/ping\treferer=-\tcookies=-\tuser_agent=-\tvhost=heartbeat" +
                ".market.yandex.ru\t" +
                "ip=2a02:6b8:c01:821:0:577:54e:a3ab\tx_forwarded_for=-\tx_real_ip=-\tbytes_sent=428\tpage_id=market" +
                ":ping\tpage_type=node\t" +
                "req_id=cd7db0b5eef90b4f0d023d6643d52d0e\treq_id_seq=-\tupstream_resp_time=0.001\treq_time=0" +
                ".001\tscheme=https\t" +
                "device_type=desktop\tx_sub_req_id=-\tyandexuid=-\tssl_handshake_time=0" +
                ".025\tmarket_buckets=-\tupstream_addr=[::1]:23432\t" +
                "upstream_header_time=0.001\tupstream_status=200\tmarket_req_id=1530497877955" +
                "/cd7db0b5eef90b4f0d023d6643d52d0e\t" +
                "msec=1530497877.956\ttvm=DISABLED";

        List<ProductView> res = mapper.apply(pingRequest.getBytes());
        assertEquals(0, res.size());
    }

    @Test
    public void testDifferentProductRequestTypes() {
        ProductView.Builder builder = ProductView.newBuilder()
                .setUserIds(UserIds.newBuilder()
                        .setYandexuid("9657821121530497880")
                )
                .setTimestamp(1530478800000L)
                .addHistory(1530478800000L)
                .setRgb(RGBType.GREEN);

        String line = logLineWithRequest("/product/100309816406");
        ProductView expected = builder.setId(100309816406L).build();
        assertEquals(expected, mapper.apply(line.getBytes()).get(0));

        line = logLineWithRequest("/product/282383/spec?nid=54761&track=char");
        expected = builder.setId(282383).build();
        assertEquals(expected, mapper.apply(line.getBytes()).get(0));

        line = logLineWithRequest("/product--viessmann-vitodens-200-w-b2ka039/12566862");
        expected = builder.setId(12566862).build();
        assertEquals(expected, mapper.apply(line.getBytes()).get(0));

        line = logLineWithRequest("/product/76644386?show-uid=304978584800705038316003&nid=54544&context=search");
        expected = builder.setId(76644386).build();
        assertEquals(expected, mapper.apply(line.getBytes()).get(0));
    }

    @Test
    public void testSkipNotSuccessfulRequest() {
        String line = "tskv\ttskv_format=access-log-cs-vs-tools\ttimestamp=2020-08-19T10:29:36\ttimezone=+0300" +
                "\tstatus=404\tprotocol=HTTP/1.1\tmethod=GET\trequest=/product--sokolov-krest-iz-serebra-s-topazami" +
                "-92030508/65629612115978221025932245719116001&from=search&local-offers-first=0&onstock=1&text=%D0%9A" +
                "%D1%80%D0%B5%D1%81%D1%82%20%D0%B8%D0%B7%20%D1%81%D0%B5%D1%80%D0%B5%D0%B1%D1%80%D0%B0%20%D1%81%20%D1" +
                "%82%D0%BE%D0%BF%D0%B0%D0%B7%D0%B0%D0%BC%D0%B8%20SOKOLOV%2092030508\treferer=-\tcookies=euConsent" +
                "=true; BCPermissionLevel=PERSONAL; BC_GDPR=11111; fhCookieConsent=true; gdpr-source=GB; " +
                "gdpr_consent=YES; beget=begetok\tuser_agent=TelegramBot (like TwitterBot)\tvhost=m.market.yandex" +
                ".ru\tip=149.154.161.17\tx_forwarded_for=149.154.161.17\tx_real_ip=149.154.161" +
                ".17\tbytes_sent=53558\tpage_id=touch:error\tpage_type=node\treq_id=dc058cdb1640b632f678e5a4d07950b4" +
                "\treq_id_seq=dc058cdb1640b632f678e5a4d07950b4\tupstream_resp_time=0.004 : 0.076\treq_time=0" +
                ".081\tscheme=https\tdevice_type=touch\tx_sub_req_id=-\tyandexuid=6947127351597822176" +
                "\tssl_handshake_time=0.000\tmarket_buckets=-\tupstream_addr=[::1]:20662 : " +
                "[::1]:20662\tupstream_header_time=0.004 : 0.020\tupstream_status=404 : " +
                "200\tmarket_req_id=1597822176115/9f63c8fdc9ec88e2c6ba38f735ad0500\tmsec=1597822176" +
                ".202\ttvm=DISABLED\ticookie=6947127351597822176\trequest_tags=-\tx_return_code=-\tx_passport_uid" +
                "=-\tremote_addr=2a02:6b8:c02:442:0:577:6580:b0f\n";

        List<ProductView> res = mapper.apply(line.getBytes());
        assertThat(res, empty());
    }
}
