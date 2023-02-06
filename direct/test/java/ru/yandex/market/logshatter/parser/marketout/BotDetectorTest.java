package ru.yandex.market.logshatter.parser.marketout;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BotDetectorTest {
    @Test
    public void testDetection() throws Exception {
        assertEquals(BotDetector.BotType.BING, BotDetector.detect("Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)"));
        assertEquals(BotDetector.BotType.GOOGLE, BotDetector.detect("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"));
        assertEquals(BotDetector.BotType.YANDEX, BotDetector.detect("Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)"));
        assertEquals(BotDetector.BotType.MAIL_RU, BotDetector.detect("Mozilla/5.0 (compatible; Linux x86_64; Mail.RU_Bot/2.0; +http://go.mail.ru/help/robots)"));
        assertEquals(BotDetector.BotType.GOOGLE, BotDetector.detect("AdsBot-Google (+http://www.google.com/adsbot.html)"));
        assertEquals(BotDetector.BotType.GOOGLE, BotDetector.detect("Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5X Build/MMB29P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.96 Mobile Safari/537.36 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"));
        assertEquals(BotDetector.BotType.GOOGLE, BotDetector.detect("Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1 (compatible; AdsBot-Google-Mobile; +http://www.google.com/mobile/adsbot.html)"));
        assertEquals(BotDetector.BotType.SCRIPT, BotDetector.detect("python-requests/2.18.4"));
        assertEquals(BotDetector.BotType.YANDEX, BotDetector.detect("Mozilla/5.0 (compatible; YaDirectFetcher/1.0; +http://yandex.com/bots)"));
        assertEquals(BotDetector.BotType.GOOGLE, BotDetector.detect("Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; Googlebot/2.1; +http://www.google.com/bot.html) Safari/537.36"));
        assertEquals(BotDetector.BotType.MARKET, BotDetector.detect("Yandex-Market-Cache-Warmer"));
        assertEquals(BotDetector.BotType.YANDEX, BotDetector.detect("Mozilla/5.0 (compatible; YandexImages/3.0; +http://yandex.com/bots)"));
        assertEquals(BotDetector.BotType.SCRIPT, BotDetector.detect("Python-urllib/2.7"));
        assertEquals(BotDetector.BotType.OTHER, BotDetector.detect("Mozilla/5.0 (compatible; MegaIndex.ru/2.0; +http://megaindex.com/crawler)"));
        assertEquals(BotDetector.BotType.GOOGLE, BotDetector.detect("Googlebot-Image/1.0"));
        assertEquals(BotDetector.BotType.MARKET, BotDetector.detect("YMarketTarantino/1.0"));
        assertEquals(BotDetector.BotType.NONE, BotDetector.detect("Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36"));
        assertEquals(BotDetector.BotType.NONE, BotDetector.detect("Opera/9.80 (Windows NT 6.0) Presto/2.12.388 Version/12.14"));
        assertEquals(BotDetector.BotType.OTHER, BotDetector.detect("Sogou web spider/4.0(+http://www.sogou.com/docs/help/webmasters.htm#07)"));
        assertEquals(BotDetector.BotType.SCRIPT, BotDetector.detect("libtorrent/1.1.6.0"));
        assertEquals(BotDetector.BotType.SCRIPT, BotDetector.detect("Java/1.8.0_152"));
        assertEquals(BotDetector.BotType.SCRIPT, BotDetector.detect("curl/7.47.0"));
        assertEquals(BotDetector.BotType.SCRIPT, BotDetector.detect("WinHTTP"));
    }
}
