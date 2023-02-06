package ru.yandex.market.logshatter.parser.front;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

import static ru.yandex.market.logshatter.parser.trace.Environment.DEVELOPMENT;

public class EventsTskvLogParserTest {
    private LogParserChecker checker;

    @Before
    public void setUp() {
        checker = new LogParserChecker(new EventsTskvLogParser());
        checker.setOrigin("market-health-dev");
        checker.setParam("logbroker://market-health-dev", DEVELOPMENT.toString());
    }

    @Test
    public void parseLegacyEvents() throws Exception {
        String line = "tskv\ttimestamp=1548762574737\tservice=market_front_red_desktop\t" +
            "event=promo_failed\trequest_id=1548762574614/75fc8b6ab7dcc902576fbc78b50d06ff\t" +
            "url=/product/iphonexs?&offer=iphonexsblack&utm_campaign=iphonecampaign\t" +
            "method=GET\tdata={\"pageType\":\"offer\",\"promoType\"" +
            ":\"utm\",\"data\":{\"feedGroupIdHash\":\"MTA1MDMyNTU5MTg5MzMzNjU3MzM\"}}\t" +
            "yandexuid=66294781514371105\tpuid=87937254\tmuid=87937254";
        checker.check(
            line,
            new Date(1548762574737L),
            "market_front_red_desktop",
            "promo_failed",
            "1548762574614/75fc8b6ab7dcc902576fbc78b50d06ff",
            "/product/iphonexs?&offer=iphonexsblack&utm_campaign=iphonecampaign",
            "GET",
            new String[]{},
            new String[]{},
            "66294781514371105",
            "87937254",
            "87937254",
            DEVELOPMENT
        );
    }


    @Test
    public void parseLegacyUnauthorizedEvents() throws Exception {
        String line = "tskv\ttimestamp=1548762574737\tservice=market_front_red_desktop\t" +
            "event=promo_failed\trequest_id=1548762574614/75fc8b6ab7dcc902576fbc78b50d06ff\t" +
            "url=/product/iphonexs?&offer=iphonexsblack&utm_campaign=iphonecampaign\t" +
            "method=GET\tdata={\"pageType\":\"offer\",\"promoType\"" +
            ":\"utm\",\"data\":{\"feedGroupIdHash\":\"MTA1MDMyNTU5MTg5MzMzNjU3MzM\"}}\t" +
            "yandexuid=66294781514371105\tpuid=\tmuid=";
        checker.check(
            line,
            new Date(1548762574737L),
            "market_front_red_desktop",
            "promo_failed",
            "1548762574614/75fc8b6ab7dcc902576fbc78b50d06ff",
            "/product/iphonexs?&offer=iphonexsblack&utm_campaign=iphonecampaign",
            "GET",
            new String[]{},
            new String[]{},
            "66294781514371105",
            "",
            "",
            DEVELOPMENT
        );
    }

    @Test
    public void parseEvents() throws Exception {
        String line = "tskv\ttimestamp=1549355298660\tservice=market_front_red_desktop\t" +
            "event=promo_failed\trequest_id=1549355298491/01387e7f5a9c7ec5835e78705762dbc3\t" +
            "url=/product/Njg3OTk0OTUzOTI5NDE4NjgzNgx?offer=5j20ZZHRqrzmpYISFJz3qw&utm_campaign=kek\t" +
            "method=GET\textra_keys=pageType,promoType,feedGroupIdHash,targetOffer\t" +
            "extra_values=offer,utm,Njg3OTk0OTUzOTI5NDE4NjgzNgx,5j20ZZHRqrzmpYISFJz3qw\t" +
            "yandexuid=66294781514371105\tpuid=87937254\tmuid=87937254";
        checker.check(
            line,
            new Date(1549355298660L),
            "market_front_red_desktop",
            "promo_failed",
            "1549355298491/01387e7f5a9c7ec5835e78705762dbc3",
            "/product/Njg3OTk0OTUzOTI5NDE4NjgzNgx?offer=5j20ZZHRqrzmpYISFJz3qw&utm_campaign=kek",
            "GET",
            new String[]{"pageType","promoType","feedGroupIdHash","targetOffer"},
            new String[]{"offer","utm","Njg3OTk0OTUzOTI5NDE4NjgzNgx","5j20ZZHRqrzmpYISFJz3qw"},
            "66294781514371105",
            "87937254",
            "87937254",
            DEVELOPMENT
        );
    }

    @Test
    public void parseUnauthorizedEvents() throws Exception {
        String line = "tskv\ttimestamp=1549355298660\tservice=market_front_red_desktop\t" +
            "event=promo_failed\trequest_id=1549355298491/01387e7f5a9c7ec5835e78705762dbc3\t" +
            "url=/product/Njg3OTk0OTUzOTI5NDE4NjgzNgx?offer=5j20ZZHRqrzmpYISFJz3qw&utm_campaign=kek\t" +
            "method=GET\textra_keys=pageType,promoType,feedGroupIdHash,targetOffer\t" +
            "extra_values=offer,utm,Njg3OTk0OTUzOTI5NDE4NjgzNgx,5j20ZZHRqrzmpYISFJz3qw\t" +
            "yandexuid=66294781514371105\tpuid=\tmuid=";
        checker.check(
            line,
            new Date(1549355298660L),
            "market_front_red_desktop",
            "promo_failed",
            "1549355298491/01387e7f5a9c7ec5835e78705762dbc3",
            "/product/Njg3OTk0OTUzOTI5NDE4NjgzNgx?offer=5j20ZZHRqrzmpYISFJz3qw&utm_campaign=kek",
            "GET",
            new String[]{"pageType","promoType","feedGroupIdHash","targetOffer"},
            new String[]{"offer","utm","Njg3OTk0OTUzOTI5NDE4NjgzNgx","5j20ZZHRqrzmpYISFJz3qw"},
            "66294781514371105",
            "",
            "",
            DEVELOPMENT
        );
    }

    @Test
    public void parseEventsWithoutExtraData() throws Exception {
        String line = "tskv\ttimestamp=1549355298660\tservice=market_front_red_desktop\t" +
            "event=promo_failed\trequest_id=1549355298491/01387e7f5a9c7ec5835e78705762dbc3\t" +
            "url=/product/Njg3OTk0OTUzOTI5NDE4NjgzNgx?offer=5j20ZZHRqrzmpYISFJz3qw&utm_campaign=kek\t" +
            "method=GET\textra_keys=\textra_values=\t" +
            "yandexuid=66294781514371105\tpuid=\tmuid=";
        checker.check(
            line,
            new Date(1549355298660L),
            "market_front_red_desktop",
            "promo_failed",
            "1549355298491/01387e7f5a9c7ec5835e78705762dbc3",
            "/product/Njg3OTk0OTUzOTI5NDE4NjgzNgx?offer=5j20ZZHRqrzmpYISFJz3qw&utm_campaign=kek",
            "GET",
            new String[]{},
            new String[]{},
            "66294781514371105",
            "",
            "",
            DEVELOPMENT
        );
    }
}
