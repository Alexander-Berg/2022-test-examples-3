package ru.yandex.market.logshatter.parser.front.errorBooster.csp;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.front.errorBooster.Platform;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class CSPLogParserTest {

    LogParserChecker checker = new LogParserChecker(new CSPLogParser());

    @Test
    public void parse() throws Exception {
        String line1 = "tskv\ttskv_format=csp-log\taddr=178.62.34.82\tpath=/csp\tuser-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36\torigin=https://yandex.ru\tcontent=from\\=web4&path\\=690.1893.1894&reqid\\=1557915929479094-1067152693925092756200034-man1-5717&url\\=chrome-extension%3A%2F%2Fgppongmhjkpfnbhagpmjfkannfbllamg%2Fjs%2Finject.js&yandex_login\\=SemanticMedia&project=video&platform=desktop&page=index&yandexuid\\=7937319301523358938\ttimestamp=2019-05-15 13:25:33\ttimezone=+0100";
        checker.setParam("yandexHosts", "[\"yandex.ru\",\"yandex.com\",\"admetrica.ru\"]");

        checker.check(
            line1,
            1557923133,
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36",
            "https://yandex.ru",
            0,
            0,
            "",
            "",
            "chrome-extension://gppongmhjkpfnbhagpmjfkannfbllamg/js/inject.js",
            "",
            "",
            "",
            "",
            "",
            0,
            "web4",
            UnsignedLong.valueOf("7937319301523358938"),
            "1557915929479094-1067152693925092756200034-man1-5717",
            "chrome-extension",
            "gppongmhjkpfnbhagpmjfkannfbllamg",
            false,
            Arrays.asList("path"),
            Arrays.asList("690.1893.1894"),
            "video",
            Platform.DESKTOP,
            "index"
        );

        String line2 = "tskv\ttskv_format=csp-log\taddr=91.129.98.143\tpath=/csp?from\\=TOUCH&version\\=6.2.4&yandexuid\\=1986576921549378696&yandex_login\\=camera345&puid\\=748896775&reqid\\=TOUCH-38088018-1557915932082\tuser-agent=Mozilla/5.0 (Linux; Android 9; SM-A920F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.136 Mobile Safari/537.36\torigin=https://mail.yandex.ru\tcontent={\"csp-report\":{\"document-uri\":\"https://mail.yandex.ru/touch/?skip-app-promo\\=1&show-left-panel\\=1\",\"referrer\":\"https://mail.yandex.ru/touch/folder/1\",\"violated-directive\":\"connect-src\",\"effective-directive\":\"connect-src\",\"original-policy\":\"frame-src yandexmail: yandexadexchange.net *.yandexadexchange.net yastatic.net *.yandex.ru yandex.ru yandexmaps://maps.yandex.ru webattach.mail.yandex.net; object-src 'none'; img-src data: clck.yandex.ru favicon.yandex.net an.yandex.ru mc.admetrica.ru *.yandex.ru yandex.ru avatars-fast.yandex.net betastatic.yastatic.net yastatic.net yapic.yandex.net avatars.yandex.net avatars.mds.yandex.net upics.yandex.net *.disk.yandex.ru downloader.dst.yandex.ru resize.yandex.net resize.rs.yandex.net mc.yandex.ru mc.yandex.ua mc.yandex.by mc.yandex.kz mc.yandex.com.tr mc.yandex.com mc.webvisor.org mc.webvisor.com 'self' webattach.mail.yandex.net; script-src 'unsafe-inline' 'unsafe-eval' 'nonce-KBH8NLP7P0zzR2IClYWReg\\=\\=' blob: an.yandex.ru *.yandex.ru yandex.ru mc.yandex.ru 'self' yastatic.net; style-src 'unsafe-inline' 'self' yastatic.net; connect-src an.yandex.ru mc.admetrica.ru *.disk.yandex.net www.yandex.by www.yandex.kz www.yandex.ua www.yandex.com www.yandex.com.tr www.yandex.ru mc.yandex.ru mc.yandex.ua mc.yandex.by mc.yandex.kz mc.yandex.com.tr mc.yandex.com mc.webvisor.org mc.webvisor.com 'self' yastatic.net *.mail.yandex.net wss://push.yandex.ru:* yandex.ru wss://push.yandex.com:* wss://push.yandex.kz:* wss://push.yandex.com.tr:* wss://push.yandex.ua:* wss://push.yandex.by:* wss://push.yandex.uz:* wss://push.yandex.fr:* wss://push.yandex.tj:* wss://push.yandex.ee:*; font-src data: yastatic.net 'self'; default-src 'self' yastatic.net; report-uri https://csp.yandex.net/csp?from\\=TOUCH&version\\=6.2.4&yandexuid\\=1986576921549378696&yandex_login\\=camera345&puid\\=748896775&reqid\\=TOUCH-38088018-1557915932082;\",\"disposition\":\"enforce\",\"blocked-uri\":\"http://127.0.0.1:30102/p?t\\=UV%7CL7%2C!%22T%5Brwe%26D_%3EZIb%5CaW%2398Y.PC6k\",\"line-number\":-2042097150,\"column-number\":-1054855168,\"source-file\":\"https://mc.yandex.ru/metrika/watch.js\",\"status-code\":0,\"script-sample\":\"\"}}\ttimestamp=2019-05-15 13:25:33\ttimezone=+0100";

        checker.check(
            line2,
            1557923133,
            "Mozilla/5.0 (Linux; Android 9; SM-A920F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.136 Mobile Safari/537.36",
            "https://mail.yandex.ru",
            0,
            0,
            "https://mc.yandex.ru/metrika/watch.js",
            "https://mail.yandex.ru/touch/folder/1",
            "http://127.0.0.1:30102/p?t=UV%7CL7%2C!%22T%5Brwe%26D_%3EZIb%5CaW%2398Y.PC6k",
            "connect-src",
            "connect-src",
            "enforce",
            "https://mail.yandex.ru/touch/?skip-app-promo=1&show-left-panel=1",
            "",
            0,
            "TOUCH",
            UnsignedLong.valueOf("1986576921549378696"),
            "TOUCH-38088018-1557915932082",
            "http",
            "127.0.0.1",
            false,
            Arrays.asList("version", "puid"),
            Arrays.asList("6.2.4", "748896775"),
            "unknown",
            Platform.UNKNOWN,
            ""
        );

        String line3 = "tskv\ttskv_format=csp-log\taddr=178.247.3.130\tpath=/csp?from\\=morda.touch.com.tr&showid\\=1557915929.39958.139782.139680&h\\=vla2-7357-227-vla-portal-morda-31387&csp\\=new&date\\=20190515&yandexuid\\=6026241071557915929\tuser-agent=Mozilla/5.0 (Linux; Android 8.0.0; GM 5 Plus) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.136 Mobile Safari/537.36\torigin=https://yandex.com.tr\tcontent={\"csp-report\":{\"document-uri\":\"https://yandex.com.tr/\",\"referrer\":\"https://www.google.com/\",\"violated-directive\":\"connect-,rc\",\"effective-directive\":\"connect-;src\",\"original-policy\":\"child-src yandex.com.tr passport.yandex.com.tr mc.yandex.ru mc.yandex.com.tr;img-src yabs.yandex.com.tr yandex.ru yandex.com.tr mc.yandex.ru yastatic.net avatars.mds.yandex.net mc.admetrica.ru 'self' passport.yandex.com.tr awaps.yandex.net mc.yandex.com.tr data:;script-src yastatic.net 'nonce-GfmITuxl2B3H5sX7AlpHRQ\\=\\=' mc.yandex.ru yandex.com.tr mc.yandex.com.tr;default-src yastatic.net 'self';report-uri https://csp.yandex.net/csp?from\\=morda.touch.com.tr&showid\\=1557915929.39958.139782.139680&h\\=vla2-7357-227-vla-portal-morda-31387&csp\\=new&date\\=20190515&yandexuid\\=6026241071557915929;style-src 'unsafe-inline';connect-src yandex.com.tr mc.yandex.ru mc.yandex.com.tr yastatic.net mc.admetrica.ru\",\"disposition\":\"report\",\"blocked-uri\":\"https://mc.admetrica.ru/sync_cookie_image_check\",\"line-number\":2042097150,\"column-number\":1054855168,\"source-file\":\"https://mc.yandex.ru/metrika/watch.js\",\"status-code\":404,\"script-sample\":\"\"}}\ttimestamp=2019-05-15 13:25:32\ttimezone=+0100";

        checker.check(
            line3,
            1557923132,
            "Mozilla/5.0 (Linux; Android 8.0.0; GM 5 Plus) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.136 Mobile Safari/537.36",
            "https://yandex.com.tr",
            2042097150,
            1054855168,
            "https://mc.yandex.ru/metrika/watch.js",
            "https://www.google.com/",
            "https://mc.admetrica.ru/sync_cookie_image_check",
            "",
            "",
            "report",
            "https://yandex.com.tr/",
            "",
            404,
            "morda.touch.com.tr",
            UnsignedLong.valueOf("6026241071557915929"),
            "1557915929.39958.139782.139680",
            "https",
            "mc.admetrica.ru",
            true,
            Arrays.asList("h", "csp"),
            Arrays.asList("vla2-7357-227-vla-portal-morda-31387", "new"),
            "unknown",
            Platform.UNKNOWN,
            ""
        );

        String line4 = "tskv\ttskv_format=csp-log\taddr=78.190.0.70\tpath=/csp?from\\=web4%3Adesktop&reqid\\=1557911983060272-690023358795935757400035-sas1-5637&yandexuid\\=2086429361523248582&yandex_login\\=m.pazarcikli\tuser-agent=Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36 Edge/17.17134\torigin=https://www.yandex.com.tr\tcontent={\"csp-report\":{\"document-uri\":\"https://www.yandex.com.tr/search/?text\\=nal%C4%B1ndilber&clid\\=2242162&httpsmsn\\=1&refig\\=cc9d27afc3d34d66be5ff394313ca342\",\"referrer\":\"\",\"blocked-uri\":\"http://i.ytimg.com\",\"violated-directive\":\"img-src 'self' data: https://pbs.twimg.com https://counter.yadro.ru https://www.google-analytics.com https://kraken.rambler.ru https://top-fwz1.mail.ru https://yastat.net https://www.tns-counter.ru https://eda.yandex https://*.eda.yandex https://yandex.st https://mc.admetrica.ru android-webview-video-poster: https://yandex.com.tr https://*.yandex.com.tr https://yandex.ru https://*.yandex.ru https://yastatic.net https://*.yastatic.net https://yandex.net https://*.yandex.net\",\"original-policy\":\"child-src 'self' data: blob: yabrowser: https://yandexadexchange.net https://*.yandexadexchange.net https://*.kinopoisk.ru https://www.youtube.com https://video.khl.ru https://www.video.khl.ru https://1tv.ru https://www.1tv.ru https://stream.1tv.ru https://www.stream.1tv.ru https://player.vgtrk.com https://www.player.vgtrk.com https://my.ntv.ru https://www.my.ntv.ru https://www.ntv.ru https://otr.webcaster.pro https://www.otr.webcaster.pro https://news.sportbox.ru http://yabs.yandex.ru https://paymentcard.yamoney.ru https://yandex.com.tr https://*.yandex.com.tr https://yandex.ru https://*.yandex.ru https://yastatic.net https://*.yastatic.net https://yandex.net https://*.yandex.net;connect-src 'self' wss://*.yandex.net wss://yandex.net wss://*.yandex.ru wss://yandex.ru wss://*.yandex.com.tr wss://yandex.com.tr https://yandexmetrica.com:* https://mc.admetrica.ru https://yandex.com.tr https://*.yandex.com.tr https://yandex.ru https://*.yandex.ru https://yastatic.net https://*.yastatic.net https://yandex.net https://*.yandex.net;default-src 'none';font-src 'self' https://yastatic.net;form-action 'self' https://yandex.com.tr https://*.yandex.com.tr https://yandex.ru https://*.yandex.ru https://yastatic.net https://*.yastatic.net https://yandex.net https://*.yandex.net;img-src 'self' data: https://pbs.twimg.com https://counter.yadro.ru https://www.google-analytics.com https://kraken.rambler.ru https://top-fwz1.mail.ru https://yastat.net https://www.tns-counter.ru https://eda.yandex https://*.eda.yandex https://yandex.st https://mc.admetrica.ru android-webview-video-poster: https://yandex.com.tr https://*.yandex.com.tr https://yandex.ru https://*.yandex.ru https://yastatic.net https://*.yastatic.net https://yandex.net https://*.yandex.net;media-src https://yandex.com.tr https://*.yandex.com.tr https://yandex.ru https://*.yandex.ru https://yastatic.net https://*.yastatic.net https://yandex.net https://*.yandex.net;object-src 'self' https://yandex.com.tr https://*.yandex.com.tr https://yandex.ru https://*.yandex.ru https://yastatic.net https://*.yastatic.net https://yandex.net https://*.yandex.net;script-src 'self' 'unsafe-inline' 'unsafe-eval' https://yandex.com.tr https://*.yandex.com.tr https://yandex.ru https://*.yandex.ru https://yastatic.net https://*.yastatic.net https://yandex.net https://*.yandex.net 'nonce-6871';style-src blob: 'self' 'unsafe-inline' https://yandex.com.tr https://*.yandex.com.tr https://yandex.ru https://*.yandex.ru https://yastatic.net https://*.yastatic.net https://yandex.net https://*.yandex.net;worker-src blob: https://yandex.com.tr https://*.yandex.com.tr https://yandex.ru https://*.yandex.ru https://yastatic.net https://*.yastatic.net https://yandex.net https://*.yandex.net;frame-src 'self' data: blob: yabrowser: https://yandexadexchange.net https://*.yandexadexchange.net https://*.kinopoisk.ru https://www.youtube.com https://video.khl.ru https://www.video.khl.ru https://1tv.ru https://www.1tv.ru https://stream.1tv.ru https://www.stream.1tv.ru https://player.vgtrk.com https://www.player.vgtrk.com https://my.ntv.ru https://www.my.ntv.ru https://www.ntv.ru https://otr.webcaster.pro https://www.otr.webcaster.pro https://news.sportbox.ru http://yabs.yandex.ru https://paymentcard.yamoney.ru https://yandex.com.tr https://*.yandex.com.tr https://yandex.ru https://*.yandex.ru https://yastatic.net https://*.yastatic.net https://yandex.net https://*.yandex.net;frame-ancestors https://yandex.com.tr https://*.yandex.com.tr https://yandex.ru https://*.yandex.ru https://sandbox.toloka.yandex.com https://sandbox.iframe-toloka.com https://iframe-toloka.com;report-uri https://csp.yandex.net/csp?from\\=web4%3Adesktop&reqid\\=1557911983060272-690023358795935757400035-sas1-5637&yandexuid\\=2086429361523248582&yandex_login\\=m.pazarcikli;\",\"effective-directive\":\"img-src\",\"status-code\":200}}\ttimestamp=2019-05-15 13:25:32\ttimezone=+0100";

        checker.check(
            line4,
            1557923132,
            "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36 Edge/17.17134",
            "https://www.yandex.com.tr",
            0,
            0,
            "",
            "",
            "http://i.ytimg.com",
            "img-src",
            "img-src",
            "",
            "https://www.yandex.com.tr/search/?text=nal%C4%B1ndilber&clid=2242162&httpsmsn=1&refig=cc9d27afc3d34d66be5ff394313ca342",
            "",
            200,
            "web4:desktop",
            UnsignedLong.valueOf("2086429361523248582"),
            "1557911983060272-690023358795935757400035-sas1-5637",
            "http",
            "i.ytimg.com",
            false,
            Arrays.asList(),
            Arrays.asList(),
            "unknown",
            Platform.UNKNOWN,
            ""
        );
        String line5 = "tskv\ttskv_format=csp-log\taddr=2a02:6b8:c0b:159:10d:2bbd:0:3c87\tpath=/csp\tuser-agent=Mozilla/5.0 (Windows NT 6.1; Trident/7.0; rv:11.0) like Gecko\torigin=https://robot-serp-bot-53137-ci1.tunneler-si.yandex.ru\tcontent=from\\=web4&path\\=690.1893.1894&reqid\\=1537282435050118NjBmOTlhYi9pZTExLDE1NTc5MTU5MjM4NjksLA%3D%3D-264072975315544858427967-vla1-1411&url\\=&yandex_login\\=&yandexuid\\=2554485841537282435\ttimestamp=2019-05-15 13:25:25\ttimezone=+0100";

        checker.checkEmpty(line5);

        String line6 = "tskv\ttskv_format=csp-log\taddr=87.228.195.122\tpath=/csp?uid\\=8594802281542297179&login\\=&from\\=market.desktop.node&env\\=prod&ext\\=true&reqId\\=1557299848060%2F29f375e3900c13f0c4a2a8234b368ab5&data;\tuser-agent=Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 YaBrowser/19.4.0.2397 Yowser/2.5 Safari/537.36\torigin=https://market.yandex.ru\tcontent={\"csp-report\":{\"document-uri\":\"https://market.yandex.ru/?utm_referrer\\=https%253A%252F%252Fyandex.ru%252F%253Ffrom%253Dalice\",\"referrer\":\"\",\"violated-directive\":\"font-src\",\"effective-directive\":\"font-src\",\"original-policy\":\"default-src 'none'; script-src 'self' 'unsafe-eval' 'nonce-XwCbpGr6ZovG1FXxJ5ggPw\\=\\=' yandex.net yandex.ru yandex.by yandex.ua yandex.kz *.yandex.net *.yandex.ua *.yandex.by *.yandex.kz *.yandex.ru yandex.st yastatic.net yastat.net social.yandex.ru; img-src 'self' data: *.yandex.ru *.yandex.net yandex.ru yandex.ru mc.yandex.ru mc.yandex.ua mc.yandex.by mc.yandex.kz mc.yandex.com.tr mc.yandex.com mc.webvisor.org mc.webvisor.com yandex.st yastatic.net yastat.net www.tns-counter.ru ar.tns-counter.ru ads.adfox.ru ads6.adfox.ru banners.adfox.ru matchid.adfox.yandex.ru fenek.market.yandex.ru fox.market.yandex.ru yabs.yandex.ru bam.nr-data.net mc.admetrica.ru; style-src 'self' 'unsafe-inline' blob: https://yastatic.net yastat.net yandex.st; connect-src 'self' data: yandex.net yandex.ru yandex.by yandex.ua yandex.kz *.yandex.net *.yandex.ua *.yandex.by *.yandex.kz *.yandex.ru mc.yandex.ru mc.yandex.ua mc.yandex.by mc.yandex.kz mc.yandex.com.tr mc.yandex.com mail.yandex.ru yandex.st yastatic.net yastat.net api.easysize.me bam.nr-data.net mc.admetrica.ru; frame-src 'self' blob: data: yandex.net yandex.ru yandex.by yandex.ua yandex.kz *.yandex.net *.yandex.ua *.yandex.by *.yandex.kz *.yandex.ru yastatic.net yastat.net kiks.yandex.ru awaps.yandex.net yandexadexchange.net *.yandexadexchange.net www.youtube-nocookie.com www.youtube.com odna.co; font-src 'self' data: dealer.s3.yandex.net yastatic.net yastat.net; media-src *.yandex.net yandex.st yastatic.net yastat.net; report-uri https://csp.yandex.net/csp?uid\\=8594802281542297179&login\\=&from\\=market.desktop.node&env\\=prod&ext\\=true&reqId\\=1557299848060%2F29f375e3900c13f0c4a2a8234b368ab5;\",\"disposition\":\"enforce\",\"blocked-uri\":\"https://fonts.gstatic.com/s/opensans/v16/mem5YaGs126MiZpBA-UNirkOUuhp.woff2\",\"status-code\":0,\"script-sample\":\"\"}}\ttimestamp=2019-05-15 13:29:18\ttimezone=+0100";

        checker.check(
            line6,
            1557923358,
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 YaBrowser/19.4.0.2397 Yowser/2.5 Safari/537.36",
            "https://market.yandex.ru",
            0,
            0,
            "",
            "",
            "https://fonts.gstatic.com/s/opensans/v16/mem5YaGs126MiZpBA-UNirkOUuhp.woff2",
            "font-src",
            "font-src",
            "enforce",
            "https://market.yandex.ru/?utm_referrer=https%253A%252F%252Fyandex.ru%252F%253Ffrom%253Dalice",
            "",
            0,
            "market.desktop.node",
            UnsignedLong.valueOf("8594802281542297179"),
            "1557299848060/29f375e3900c13f0c4a2a8234b368ab5",
            "https",
            "fonts.gstatic.com",
            false,
            Arrays.asList("env", "ext"),
            Arrays.asList("prod", "true"),
            "unknown",
            Platform.UNKNOWN,
            ""
        );

        String line7 = "tskv\ttskv_format=csp-log\taddr=178.128.126.0\tpath=/csp?from\\=LIZA&version\\=17.1.101&yandexuid\\=9478769571554618539&yandex_login\\=nbusin&puid\\=776417625&reqid\\=LIZA-05370618-1557915902186\tuser-agent=Mozilla/5.0 (Windows NT 6.1; rv:66.0) Gecko/20100101 Firefox/66.0\torigin=\tcontent={\"csp-report\":{\"blocked-uri\":\"inline\",\"column-number\":1,\"document-uri\":\"https://mail.yandex.ru/?uid\\=776417625\",\"line-number\":1,\"original-policy\":\"img-src data: https://*.gemius.pl https://*.tns-counter.ru https://*.adfox.ru https://view.atdmt.com https://ad.adriver.ru https://comscore.com https://s1.countby.com https://bl1.datamind.ru https://*.doubleclick.net https://secure-it.imrworldwide.com https://lamoda25.ru https://omirussia.ru https://amch.questionmarket.com https://jsre.r24-tech.com https://yandex.dsp.redfog.ru https://yandex-bidder.rutarget.ru https://bs.serving-sys.com https://eu-propulsor.sociomantic.com https://tns.ru https://*.yandex.ru https://yandex.ru https://yadi.sk https://resize.yandex.net https://resize.rs.yandex.net https://avatars.mds.yandex.net https://avatars.mdst.yandex.net http://avatars.mds.yandex.net http://avatars.mdst.yandex.net https://*.yandex.net https://*.yastatic.net https://mc.yandex.az https://mc.yandex.by https://mc.yandex.co.il https://mc.yandex.com https://mc.yandex.com.am https://mc.yandex.com.ge https://mc.yandex.com.tr https://mc.yandex.ee https://mc.yandex.fr https://mc.yandex.kg https://mc.yandex.kz https://mc.yandex.lt https://mc.yandex.lv https://mc.yandex.md https://mc.yandex.ru https://mc.yandex.tj https://mc.yandex.tm https://mc.yandex.ua https://mc.yandex.uz https://mc.webvisor.com https://mc.webvisor.org https://yastatic.net 'self' https://mc.admetrica.ru https://yandex.st https://maps.googleapis.com https://webattach.mail.yandex.net https://*.yandex.com; script-src 'unsafe-inline' 'nonce-mVE8fZMQUcuZSmsyJswRCA\\=\\=' 'unsafe-eval' blob: https://ads.adfox.ru https://*.yandex.ru https://yandex.ru https://*.disk.yandex.net https://disk.yandex.ru https://api-maps.yandex.ru https://mc.yandex.az https://mc.yandex.by https://mc.yandex.co.il https://mc.yandex.com https://mc.yandex.com.am https://mc.yandex.com.ge https://mc.yandex.com.tr https://mc.yandex.ee https://mc.yandex.fr https://mc.yandex.kg https://mc.yandex.kz https://mc.yandex.lt https://mc.yandex.lv https://mc.yandex.md https://mc.yandex.ru https://mc.yandex.tj https://mc.yandex.tm https://mc.yandex.ua https://mc.yandex.uz https://mc.webvisor.com https://mc.webvisor.org https://yastatic.net 'self' https://yandex.st https://*.yandex.net; style-src 'unsafe-inline' 'unsafe-eval' 'self' https://yandex.st https://yastatic.net; frame-src blob: https://awaps.yandex.ru https://awaps.yandex.net https://yandexadexchange.net https://*.yandexadexchange.net https://banners.adfox.ru https://*.yandex.ru https://yandex.ru https://*.disk.yandex.net https://mc.yandex.ru 'self' https://yastatic.net https://player.vimeo.com https://www.facebook.com https://webattach.mail.yandex.net; child-src blob: https://*.disk.yandex.net https://mc.yandex.ru 'self' https://yastatic.net; default-src https://an.yandex.ru/system/context.js https://yastatic.net/jquery/1.12.4/jquery.min.js https://yastatic.net/nearest.js https://yastatic.net/mail/_/; connect-src https://an.yandex.ru https://*.adfox.ru https://*.disk.yandex.net https://disk.yandex.ru https://suggest-maps.yandex.ru https://mc.yandex.az https://mc.yandex.by https://mc.yandex.co.il https://mc.yandex.com https://mc.yandex.com.am https://mc.yandex.com.ge https://mc.yandex.com.tr https://mc.yandex.ee https://mc.yandex.fr https://mc.yandex.kg https://mc.yandex.kz https://mc.yandex.lt https://mc.yandex.lv https://mc.yandex.md https://mc.yandex.ru https://mc.yandex.tj https://mc.yandex.tm https://mc.yandex.ua https://mc.yandex.uz https://mc.webvisor.com https://mc.webvisor.org https://mc.admetrica.ru https://yandex.ru 'self' https://yandex.st https://yastatic.net https://*.mail.yandex.net https://disk.yandex.com https://disk.yandex.by https://disk.yandex.kz https://disk.yandex.com.tr https://disk.yandex.ua https://*.video.yandex.net https://streaming.video.yandex.net https://csp.yandex.net https://static-mon.yandex.net wss://push.yandex.ru:* https://mobile.yandex.net; object-src https://*.tns-counter.ru 'self' https://yandex.st https://yastatic.net https://*.youtube.com https://*.yandex.ru https://static.video.yandex.net; media-src https://*.yandex.net 'self' https://yandex.st https://yastatic.net; manifest-src 'self'; font-src https://yastatic.net; report-uri https://csp.yandex.net/csp?from\\=LIZA&version\\=17.1.101&yandexuid\\=9478769571554618539&yandex_login\\=nbusin&puid\\=776417625&reqid\\=LIZA-05370618-1557915902186\",\"referrer\":\"https://mail.yandex.ru/\",\"source-file\":\"https://mail.yandex.ru/?uid\\=776417625\",\"violated-directive\":\"script-src-attr\"}}\ttimestamp=2019-05-15 13:25:03\ttimezone=+0100";

        checker.check(
            line7,
            1557923103,
            "Mozilla/5.0 (Windows NT 6.1; rv:66.0) Gecko/20100101 Firefox/66.0",
            "",
            1,
            1,
            "https://mail.yandex.ru/?uid=776417625",
            "https://mail.yandex.ru/",
            "inline",
            "script-src",
            "",
            "",
            "https://mail.yandex.ru/?uid=776417625",
            "",
            0,
            "LIZA",
            UnsignedLong.valueOf("9478769571554618539"),
            "LIZA-05370618-1557915902186",
            "",
            "",
            false,
            Arrays.asList("version", "puid"),
            Arrays.asList("17.1.101", "776417625"),
            "unknown",
            Platform.UNKNOWN,
            ""
        );

        String line8 = "tskv\ttskv_format=csp-log\taddr=151.226.62.114\tpath=/csp\tuser-agent=Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36\torigin=https://news.yandex.ru\tcontent=from\\=YxNews&path\\=690.1893.1894&reqid\\=1558545266136487-551576292757906841000040-sas1-7898-sas-news-ah-http-adapter-21930-NEWS_STORY&url\\=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPD94bWwgdmVyc2lvbj0iMS4wIiBzdGFuZGFsb25lPSJubyI%2FPgo8IURPQ1RZUEUgc3ZnIFBVQkxJQyAiLS8vVzNDLy9EVEQgU1ZHIDIwMDEwOTA0Ly9FTiIKICJodHRwOi8vd3d3LnczLm9yZy9UUi8yMDAxL1JFQy1TVkctMjAwMTA5MDQvRFREL3N2ZzEwLmR0ZCI%2BCjxzdmcgdmVyc2lvbj0iMS4wIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciCiB3aWR0aD0iMTI4LjAwMDAwMHB0IiBoZWlnaHQ9IjEyOC4wMDAwMDBwdCIgdmlld0JveD0iMCAwIDEyOC4wMDAwMDAgMTI4LjAwMDAwMCIKIHByZXNlcnZlQXNwZWN0UmF0aW89InhNaWRZTWlkIG1lZXQiPgo8bWV0YWRhdGE%2BCkNyZWF0ZWQgYnkgcG90cmFjZSAxLjE1LCB3cml0dGVuIGJ5IFBldGVyIFNlbGluZ2VyIDIwMDEtMjAxNwo8L21ldGFkYXRhPgo8ZyB0cmFuc2Zvcm09InRyYW5zbGF0ZSgwLjAwMDAwMCwxMjguMDAwMDAwKSBzY2FsZSgwLjEwMDAwMCwtMC4xMDAwMDApIgpmaWxsPSIjMDAwMDAwIiBzdHJva2U9Im5vbmUiPgo8cGF0aCBkPSJNNTcwIDk2NSBsMCAtMjU1IC0xMjUgMCBjLTY5IDAgLTEyNSAtNCAtMTI1IC04IDAgLTEyIDMxMCAtMzgyIDMyMAotMzgyIDEwIDAgMzIwIDM3MCAzMjAgMzgyIDAgNCAtNTYgOCAtMTI1IDggbC0xMjUgMCAwIDI1NSAwIDI1NSAtNzAgMCAtNzAgMAowIC0yNTV6Ii8%2BCjxwYXRoIGQ9Ik02MCAzNDAgYzAgLTIwMSA2IC0yMjYgNjMgLTI2MCAzMSAtMTkgNTQgLTIwIDUxNyAtMjAgNDYzIDAgNDg2IDEKNTE3IDIwIDU3IDM0IDYzIDU5IDYzIDI2MCBsMCAxODAgLTcwIDAgLTcwIDAgMCAtMTYwIDAgLTE2MCAtNDQwIDAgLTQ0MCAwIDAKMTYwIDAgMTYwIC03MCAwIC03MCAwIDAgLTE4MHoiLz4KPC9nPgo8L3N2Zz4K&yandex_login\\=undefined&yandexuid\\=6992452201524500474\ttimestamp=2019-05-22 20:14:27\ttimezone=+0800";

        checker.check(
            line8,
            1558527267,
            "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36",
            "https://news.yandex.ru",
            0,
            0,
            "",
            "",
            "data",
            "",
            "",
            "",
            "",
            "",
            0,
            "YxNews",
            UnsignedLong.valueOf("6992452201524500474"),
            "1558545266136487-551576292757906841000040-sas1-7898-sas-news-ah-http-adapter-21930-NEWS_STORY",
            "",
            "",
            false,
            Arrays.asList("path"),
            Arrays.asList("690.1893.1894"),
            "unknown",
            Platform.UNKNOWN,
            ""
        );

        String line9 = "tskv\ttskv_format=csp-log\taddr=2001:470:64b5:9d56:7140:8ea6:25b4:2029\tpath=/csp?from\\=morda.big.kz&showid\\=1558999482.95821.139940.61903&h\\=man2-7019-429-man-portal-morda-29675&csp\\=new&date\\=20190528&yandexuid\\=5160290401558999478\tuser-agent=Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3261.35 Safari/537.36\torigin=null\tcontent={\"csp-report\":{\"document-uri\":\"https://yandex.kz/?ncrnd\\=0.049917661908065\",\"referrer\":\"https://yandex.kz/search/customize\",\"violated-directive\":\"script-src 'nonce-bxagcCRo7hWwI+3sCRHbgQ\\=\\=' an.yandex.ru yandex.kz main.zdevx.yandex.kz mc.yandex.ru zen.yandex.kz mc.yandex.kz yastatic.net\",\"original-policy\":\"child-src *.yandexadexchange.net mc.yandex.ru yandex.kz yandex.ru music.yandex.kz blob: yastatic.net mc.yandex.kz zen.yandex.kz main.zdevx.yandex.kz *.cdn.yandex.net ott-widget.yandex.ru yabs.yandex.kz downloader.yandex.net yandexadexchange.net awaps.yandex.net;font-src yastatic.net zen.yandex.kz main.zdevx.yandex.kz;connect-src an.yandex.ru wss://portal-xiva.yandex.net yandex.kz 'self' mc.yandex.ru frontend.vh.yandex.ru wss://webasr.yandex.net jstracer.yandex.ru mc.admetrica.ru yabs.yandex.ru main.zdevx.yandex.kz mc.yandex.kz portal-xiva.yandex.net push.yandex.ru mobile.yandex.net wss://push.yandex.ru yastatic.net zen.yandex.kz;default-src yastatic.net;style-src zen.yandex.kz 'unsafe-inline' yastatic.net main.zdevx.yandex.kz;media-src video-preview.s3.yandex.net;report-uri https://csp.yandex.net/csp?from\\=morda.big.kz&showid\\=1558999482.95821.139940.61903&h\\=man2-7019-429-man-portal-morda-29675&csp\\=new&date\\=20190528&yandexuid\\=5160290401558999478;script-src 'nonce-bxagcCRo7hWwI+3sCRHbgQ\\=\\=' an.yandex.ru yandex.kz main.zdevx.yandex.kz mc.yandex.ru zen.yandex.kz mc.yandex.kz yastatic.net;img-src ads.adfox.ru tracking.ott.yandex.net mc.yandex.ru yandex.kz yandex.ru avatars.mds.yandex.net 'self' an.yandex.ru strm.yandex.ru gdeby.hit.gemius.pl yastatic.net mc.yandex.kz zen.yandex.kz main.zdevx.yandex.kz mc.admetrica.ru sb.scorecardresearch.com yabs.yandex.kz resize.yandex.net s3.mds.yandex.net data: zen.s3.yandex.net favicon.yandex.net awaps.yandex.net wcm.solution.weborama.fr\",\"blocked-uri\":\"\"}}\ttimestamp=2019-05-28 02:24:43\ttimezone=+0300";

        checker.check(
            line9,
            1558999483,
            "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3261.35 Safari/537.36",
            "",
            0,
            0,
            "",
            "https://yandex.kz/search/customize",
            "",
            "script-src",
            "",
            "",
            "https://yandex.kz/?ncrnd=0.049917661908065",
            "",
            0,
            "morda.big.kz",
            UnsignedLong.valueOf("5160290401558999478"),
            "1558999482.95821.139940.61903",
            "",
            "",
            false,
            Arrays.asList("h", "csp"),
            Arrays.asList("man2-7019-429-man-portal-morda-29675", "new"),
            "unknown",
            Platform.UNKNOWN,
            ""
        );

        String line10 = "tskv\ttskv_format=csp-log\taddr=2001:41d0:604:1cb::\tpath=/csp\tuser-agent=Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:58.0) Gecko/20100101 Firefox/58.0\torigin=\tcontent=------------------------------92b277188639\\r\\nContent-Disposition: form-data; name\\=\"from\"\\r\\n\\r\\nvideo3\\r\\n------------------------------92b277188639\\r\\nContent-Disposition: form-data; name\\=\"path\"\\r\\n\\r\\n690.1893.1894\\r\\n------------------------------92b277188639\\r\\nContent-Disposition: form-data; name\\=\"reqid\"\\r\\n\\r\\n1559004857957132-803178443475169556759502-vla1-2011-V\\r\\n------------------------------92b277188639\\r\\nContent-Disposition: form-data; name\\=\"url\"\\r\\n\\r\\n/direct/aHR0cHM6Ly95YW5kZXgucnUvY2xjay9qY2xjay9yZXFpZD0xNTU5MDA0ODYxNzA2NTQ0LTY3NDA5MTYxNjI3NDE3Njc4NjQ4NTY5LXZsYTEtMTg4OS1WL3JuZD0xNTU5MDA0ODYyNTUyL3l1aWQ9NTUxNjk1NTY3MTU1OTAwNDg1Ny9waWQ9MTk3L2NpZD03MzMwNy9wYXRoPXJlbGF0ZWQtdmlkZW8udGh1bWItc3BlZWQvdmFycz0taW5mbz01NDg4OzA7MTE7NjE7MDsyNTc7NTsxNzU2OzcyNDQ7NzE1OTtodHRwcztwbC5mcmVldmlkZW9wcm94eS5jb207ZGlyZWN0OzEyITU0OTI7MDs5OzY0OzA7Mjg0OzQ7MTgzNjs3MzI4OzY5NzA7aHR0cHM7cGwuZnJlZXZpZGVvcHJveHkuY29tO2RpcmVjdDsxMyE1NTAyOzA7NDs2NzswOzI2OTs2OzE4MTQ7NzMxNjs5OTEwO2h0dHBzO3BsLmZyZWV2aWRlb3Byb3h5LmNvbTtkaXJlY3Q7MTQhNTUyMjswOzQ7NTA7MDsyNDk7NzsxODAxOzczMjM7NzIwMDtodHRwcztwbC5mcmVldmlkZW9wcm94eS5jb207ZGlyZWN0OzE1ITU1MzE7MDs1OzQyOzA7MzA3OzU7MTg2Njs3Mzk3OzczOTA7aHR0cHM7cGwuZnJlZXZpZGVvcHJveHkuY29tO2RpcmVjdDsxNiE1NTM3OzA7MzszOTswOzMzNDszOzIwNTk7NzU5Njs2NzA2O2h0dHBzO3BsLmZyZWV2aWRlb3Byb3h5LmNvbTtkaXJlY3Q7MTchNTU0MTswOzQ7MzY7MDszMjU7NTsyMTA1Ozc2NDY7NzYxNDtodHRwcztwbC5mcmVldmlkZW9wcm94eS5jb207ZGlyZWN0OzE4ITU1NTQ7MDsxOzI2OzA7MzMwOzQ7MjEwMzs3NjU3Ozc1MjM7aHR0cHM7cGwuZnJlZXZpZGVvcHJveHkuY29tO2RpcmVjdDsxOS9kdHlwZT1zdHJlZC9jdHM9MTU1OTAwNDg2MjU0MS8qZGF0YT11cmwlM0RodHRwcyUzQSUyRiUyRnBsLmZyZWV2aWRlb3Byb3h5LmNvbSUyRmRpcmVjdCUyRmFIUjBjSE02THk5NVlXNWtaWGd1Y25VdmRtbGtaVzh2YzJWaGNtTm9QMlpwYkcxSlpEMDFPREl4T1RVeU56RXpORGd5TkRjeE1USTFKblJsZUhROWMyRnVaSEpoSlRJd1ltOXZZbWxsY3lVeU1HaGtKVEl3ZG1sa1pXOXo-+\\r\\n------------------------------92b277188639\\r\\nContent-Disposition: form-data; name\\=\"yandex_login\"\\r\\n\\r\\n\\r\\n------------------------------92b277188639\\r\\nContent-Disposition: form-data; name\\=\"yandexuid\"\\r\\n\\r\\n5516955671559004857\\r\\n------------------------------92b277188639--\\r\\n\ttimestamp=2019-05-28 03:54:25\ttimezone=+0300";

        checker.checkEmpty(line10);
    }

    @Test
    public void getHost() {
        CSPLogParser cspLogParser = new CSPLogParser();

        assertEquals("", cspLogParser.getHost("https://avatars.mds.yandavatars.et-zen_doc/1779163/-6396oc/177916870471/smart_crop_540x405"));
        assertEquals("yandex.ru", cspLogParser.getHost("https://yandex.ru/video"));
        assertEquals("yandex.ru", cspLogParser.getHost("https://yandex.ru"));
        assertEquals("gj.track.uc.cn", cspLogParser.getHost("http://gj.track.uc.cn/collect?uc_param_str=cpfrveladnkt&appid=4e54ac8a118f&lt=event&e_c=bottom_ad&e_a=index&e_n=no_match&conds={%22isNotBlackDomain%22:false}"));
        assertEquals("gj.track.uc.cn", cspLogParser.getHost("http://gj.track.uc.cn/"));
        assertEquals("yandex.ru", cspLogParser.getHost("https://yandex.ru/clck/jclck/dtype=stred/pid=0/cid=2873/lr=58/path=morda_ru_touch.tpah.p0.nah_not_shown.button_by_mouse/user_input=%D0%B3%D0%B5%D1%80%D0%B1%20%D1%81%D0%B0/text=%D0%B3%D0%B5%D1%80%D0%B1%20%D1%81%D0%B0%D0%BB%D0%B5%D1%85%D0%B0%D1%80%D0%B4%D0%B0%20/times=497.132.134.133.242.179.195.192.246/render_times=5.2.1.1.1.1.2.2.1/pos=15/ratio=7.15.8/since_first_change=8498/since_last_change=3300/total_input_time=12525/session=1559289174784/rqs=9/rsp=9/rndr=9/ersp=0/clks=1/cchd=0/suggest_reqid=193949529153933280391521625773588/tpah_log=[[add,p0,0],[add,p0,271],[add,p0,1967],[add,p0,2674],[add,p0,2975],[add,p0,3308],[add,p0,3532],[word,p1,5199],[submit,p0,8497]]/timefs=14266/lid=geotouch.searchgo/sid=1559289351.65208.140294.180609/*data=url%3Dhttp%3A%2F%2Fya.ru/?_=1559289165552"));
        assertEquals("jsl.infostatsvc.com", cspLogParser.getHost("https://jsl.infostatsvc.com/?InitSuccess_8=1164|,|ru|,|sizlsearch|,|dd326d5e-2c19-4840-8c3a-605b457f6c98|,|0|,|1002P|,|Chrome|,|Browser|,|1409744693627|,|adssRU|,|yandex.ru"));
        assertEquals("moneyviking-a.akamaihd.net", cspLogParser.getHost("https://moneyviking-a.akamaihd.net/stats/?InitSuccess_8=62|,|RU|,|Money%20Viking|,|2107cdf5-2b92-4c20-baad-d3b23363e7f1|,|0|,|6YOM6|,|Chrome|,|Browser|,|1451435789315|,|tr1ru|,|yandex.ru"));
        assertEquals("", cspLogParser.getHost("self"));
        assertEquals("", cspLogParser.getHost("https:/yandex.ru"));
        assertEquals("", cspLogParser.getHost("/yandex.ru"));
        assertEquals("yandex.ru", cspLogParser.getHost("//yandex.ru"));

    }

    @Test
    public void getScheme() {
        CSPLogParser cspLogParser = new CSPLogParser();

        assertEquals("https", cspLogParser.getScheme("https://avatars.mds.yandavatars.et-zen_doc/1779163/-6396oc/177916870471/smart_crop_540x405"));
        assertEquals("https", cspLogParser.getScheme("https://yandex.ru/video"));
        assertEquals("https", cspLogParser.getScheme("https://yandex.ru"));
        assertEquals("http", cspLogParser.getScheme("http://gj.track.uc.cn/collect?uc_param_str=cpfrveladnkt&appid=4e54ac8a118f&lt=event&e_c=bottom_ad&e_a=index&e_n=no_match&conds={%22isNotBlackDomain%22:false}"));
        assertEquals("http", cspLogParser.getScheme("http://gj.track.uc.cn/"));
        assertEquals("https", cspLogParser.getScheme("https://yandex.ru/clck/jclck/dtype=stred/pid=0/cid=2873/lr=58/path=morda_ru_touch.tpah.p0.nah_not_shown.button_by_mouse/user_input=%D0%B3%D0%B5%D1%80%D0%B1%20%D1%81%D0%B0/text=%D0%B3%D0%B5%D1%80%D0%B1%20%D1%81%D0%B0%D0%BB%D0%B5%D1%85%D0%B0%D1%80%D0%B4%D0%B0%20/times=497.132.134.133.242.179.195.192.246/render_times=5.2.1.1.1.1.2.2.1/pos=15/ratio=7.15.8/since_first_change=8498/since_last_change=3300/total_input_time=12525/session=1559289174784/rqs=9/rsp=9/rndr=9/ersp=0/clks=1/cchd=0/suggest_reqid=193949529153933280391521625773588/tpah_log=[[add,p0,0],[add,p0,271],[add,p0,1967],[add,p0,2674],[add,p0,2975],[add,p0,3308],[add,p0,3532],[word,p1,5199],[submit,p0,8497]]/timefs=14266/lid=geotouch.searchgo/sid=1559289351.65208.140294.180609/*data=url%3Dhttp%3A%2F%2Fya.ru/?_=1559289165552"));
        assertEquals("https", cspLogParser.getScheme("https://jsl.infostatsvc.com/?InitSuccess_8=1164|,|ru|,|sizlsearch|,|dd326d5e-2c19-4840-8c3a-605b457f6c98|,|0|,|1002P|,|Chrome|,|Browser|,|1409744693627|,|adssRU|,|yandex.ru"));
        assertEquals("https", cspLogParser.getScheme("https://moneyviking-a.akamaihd.net/stats/?InitSuccess_8=62|,|RU|,|Money%20Viking|,|2107cdf5-2b92-4c20-baad-d3b23363e7f1|,|0|,|6YOM6|,|Chrome|,|Browser|,|1451435789315|,|tr1ru|,|yandex.ru"));
        assertEquals("", cspLogParser.getScheme("chromenull://"));
        assertEquals("chromenull", cspLogParser.getScheme("chromenull://sss"));
        assertEquals("mbexec", cspLogParser.getScheme("mbexec://$(window_id)"));
        assertEquals("", cspLogParser.getScheme("mxaddon-pkg://{3c3b2f49-d929-4438-89e5-b8df89edc828}"));
        assertEquals("", cspLogParser.getScheme("self"));
        assertEquals("", cspLogParser.getScheme("https:/yandex.ru"));
        assertEquals("", cspLogParser.getScheme("/yandex.ru"));
        assertEquals("", cspLogParser.getScheme("//yandex.ru"));
    }
}
