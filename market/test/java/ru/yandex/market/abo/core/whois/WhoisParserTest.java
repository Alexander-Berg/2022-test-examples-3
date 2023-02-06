package ru.yandex.market.abo.core.whois;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.abo.core.whois.WhoisField.CREATED;
import static ru.yandex.market.abo.core.whois.WhoisField.FREE_DATE;
import static ru.yandex.market.abo.core.whois.WhoisField.NSERVER;
import static ru.yandex.market.abo.core.whois.WhoisField.PAID_TILL;

/**
 * @author komarovns
 * @date 19.11.2020
 */
class WhoisParserTest {
    private static final String RESPONSE = "" +
            "% By submitting a query to RIPN's Whois Service\n" +
            "% you agree to abide by the following terms of use:\n" +
            "% http://www.ripn.net/about/servpol.html#3.2 (in Russian) \n" +
            "% http://www.ripn.net/about/en/servpol.html#3.2 (in English).\n" +
            "\n" +
            "domain:        YANDEX.RU\n" +
            "nserver:       ns1.yandex.ru. 213.180.193.1, 2a02:6b8::1\n" +
            "nserver:       ns2.yandex.ru. 93.158.134.1, 2a02:6b8:0:1::1\n" +
            "nserver:       ns9.z5h64q92x9.net.\n" +
            "state:         REGISTERED, DELEGATED, VERIFIED\n" +
            "org:           YANDEX, LLC.\n" +
            "registrar:     RU-CENTER-RU\n" +
            "admin-contact: https://www.nic.ru/whois\n" +
            "created:       1997-09-23T09:45:07Z\n" +
            "paid-till:     2021-09-30T21:00:00Z\n" +
            "free-date:     2021-11-01\n" +
            "source:        TCI\n" +
            "\n" +
            "Last updated on 2020-11-19T11:36:30Z\n" +
            "\n";

    @Test
    void testParse() {
        var is = new ByteArrayInputStream(RESPONSE.getBytes());
        assertEquals(Map.of(
                NSERVER, List.of(
                        "ns1.yandex.ru. 213.180.193.1, 2a02:6b8::1",
                        "ns2.yandex.ru. 93.158.134.1, 2a02:6b8:0:1::1",
                        "ns9.z5h64q92x9.net."),
                CREATED, List.of("1997-09-23T09:45:07Z"),
                PAID_TILL, List.of("2021-09-30T21:00:00Z"),
                FREE_DATE, List.of("2021-11-01")
        ), WhoisParser.parse(is));
    }
}
