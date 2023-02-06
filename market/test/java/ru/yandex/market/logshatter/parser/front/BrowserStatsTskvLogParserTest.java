package ru.yandex.market.logshatter.parser.front;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;


public class BrowserStatsTskvLogParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new BrowserStatsTskvLogParser());
        checker.setOrigin("market-health-dev");
    }

    @Test
    public void parseRealBlueBrowserStatsOfAllTypes() throws Exception {
        String line = "_logfeller_icookie=5538624561521625832\t_logfeller_timestamp=1573575710\t_stbx=rt3.myt--yabs-r" +
            "t--bs-watch-log:103@@118918066@@uo3vCwYQiko47V727dOVYg@@1573575711178@@1573575771@@bs-watch-log@@30465" +
            "91@@1573575714309\tantivirusyes=0\tbrowserinfo=ti:1:nt:3:ns:1573575687398:s:360x640x24:sk:2:adb:2:fpr:21" +
            "6613626101:cn:1:w:360x559:z:180:i:20191112192150:et:1573575710:en:utf-8:c:1:la:ru-ru:ar:1:ls:1369455744" +
            "783:rqn:2641:rn:241814428:hid:785681393:ds:,,,,,,,,,,,,:gdpr:13:fu:3:v:1739:wv:2:rqnl:1:st:1573575710:" +
            "u:1570833875244961165:pp:1960299339:t:Корзина — маркетплейс Беру\tclientip=91.143.40.98\tclientip6=::f" +
            "fff:91.143.40.98\tclientport=0\tcookiegid=20728\tcookiegpauto=55_922965:37_862209:30:1:1573573086\tcook" +
            "iei=5538624561521625832\tcookiel=eHJkegsOBwpLTXlIanhLBQNWdWIHf2cHIDglUUQwSVA\\=.1573569785.14047.3400" +
            "88.d65969a8ba1e7d3bf20c46c14d744e7c\tcookieys=wprid.1573573087774191-1810515090240341942400129-man1-408" +
            "0-TCH\tcounterclass=0\tcounterid=47628343\tcryptaid=7100081613779540526\tdomainuserid=1570833875244961" +
            "165\tdomainzone=yandex.ru\tdonottrack=0\tetag=0\teventtime=1573575710\tfuniqid=6569788570046082883\the" +
            "aderargs=293: _ym_d\\=1573573356; mda\\=0; Session_id\\=3:1573569834.5.0.1573569785633:YiiPWw:4b.1|98" +
            "3384280.0.2|207949.625389.; my\\=YysB4ACRFQA\\=; fuid01\\=5b2c93e56f04af43.M_EUkkIWIZQgMWl_GL6hghfGJKg" +
            "uROKnqAjUkOMESwEa6HWNuPEkJPE5s0uEXyreWO8JE4aKSnFWBAO8kQwEphJ7_nKlcPY-BpCEBtEzcqPJS_yAkXIFO1ijQ2mhIhqE;" +
            " yandexuid\\=5538624561521625832; yandex_gid\\=20728; _ym_isad\\=2; usst\\=EAAAAAAAAADwAQoOCgJkcxIIMjY" +
            "yMjYyMTM,; yabs-frequency\\=/4/00240000002fZSfT/4ujoS4iu_F___nABSd18E277QR1mIpZy_____c6mS4Wu8KRwFMr8E7u" +
            "SlZrjI3XdSi9HS4WuG0YBSd18E603aN9m4pZy____vk1oS0yu_F___-7eSd0BEFp___yPtd9m2pZy____2OPoS0Cu_F___uBUSd03E" +
            "Fp____dFLHmI3WW5ejoSEit_F___mA6Sd3VD_p___yXXd9mI3WW7OPoS4WuG7V2KN3lD_p____DYt9mI3WWKv5oS4WuG2U6Sd19E40" +
            "1muLoS4auW0B___zom55mp3KW0eHoSFp__mJ0X700/; i\\=LdjvEDkPmU6A9srJxw4eAmK/lZoao+MkE/YG+SIi1lmc7tsqsDpH63" +
            "X/HyEKZQL2h4MpyKyLLKtChLOjzJ630/1BXAk\\=; L\\=eHJkegsOBwpLTXlIanhLBQNWdWIHf2cHIDglUUQwSVA\\=.157356978" +
            "5.14047.340088.d65969a8ba1e7d3bf20c46c14d744e7c; sessionid2\\=3:1573569834.5.0.1573569785633:YiiPWw:4" +
            "b.1|983384280.0.2|207949.210614.; _ym_uid\\=152162628934458236; ys\\=wprid.1573573087774191-181051509" +
            "0240341942400129-man1-4080-TCH; yabs-sid\\=1298743181573570824; yandex_login\\=tilisa87; zm\\=m-everyt" +
            "hing_touch.css%3As3home-static_cwttNjpjrttAz3n0EIWtpPn_8BI%3Al; yp\\=1836985832.yrts.1521625832#1589" +
            "323598.szm.2:640x360:360x559#1837236086.ygp.2#1576145600.ygu.1#1883862978.sad.1561830902%3A1568502978%" +
            "3A2#1584855546.sz.640x360x2#1886318589.multib.1#1573745896.gpauto.55_922965%3A37_862209%3A30%3A1%3A15" +
            "73573086#1888929834.udn.cDp0aWxpc2E4Nw%3D%3D\tiso_eventtime=2019-11-12 19:21:50\tlayerid=9\tmetrikae" +
            "xp=\tparams=[{\\\"timestamp\\\":1573575703,\\\"requestId\\\":\\\"1573575688431/ca6e0935bf2a012d79a6efa" +
            "328970500\\\",\\\"info\\\":{\\\"serviceId\\\":\\\"market_front_blue_desktop\\\",\\\"pageId\\\":\\\"blue" +
            "-market:cart\\\",\\\"expFlags\\\":\\\"all_webim-chat_enable,all_edit-order-delivery-date_enab" +
            "le\\\"},\\\"name\\\":\\\"resource\\\",\\\"portion\\\":\\\"requestCount\\\",\\\"groupBy\\\":\\\"dom" +
            "ain\\\",\\\"group\\\":\\\"yastatic.net\\\",\\\"type\\\":\\\"int\\\",\\\"value\\\":10},{\\\"timesta" +
            "mp\\\":1573575703,\\\"requestId\\\":\\\"1573575688431/ca6e0935bf2a012d79a6efa328970500\\\",\\\"in" +
            "fo\\\":{\\\"serviceId\\\":\\\"market_front_blue_desktop\\\",\\\"pageId\\\":\\\"blue-market:car" +
            "t\\\",\\\"expFlags\\\":\\\"all_webim-chat_enable,all_edit-order-delivery-date_enable\\\"},\\\"na" +
            "me\\\":\\\"resource\\\",\\\"portion\\\":\\\"cacheHit\\\",\\\"groupBy\\\":\\\"domain\\\",\\\"gro" +
            "up\\\":\\\"yastatic.net\\\",\\\"type\\\":\\\"float\\\",\\\"value\\\":1},{\\\"timestamp\\\":157357" +
            "5703,\\\"requestId\\\":\\\"1573575688431/ca6e0935bf2a012d79a6efa328970500\\\",\\\"info\\\":{\\\"servic" +
            "eId\\\":\\\"market_front_blue_desktop\\\",\\\"pageId\\\":\\\"blue-market:cart\\\",\\\"expFlags\\\":\\\"a" +
            "ll_webim-chat_enable,all_edit-order-delivery-date_enable\\\"},\\\"name\\\":\\\"resource\\\",\\\"port" +
            "ion\\\":\\\"transferSize\\\",\\\"groupBy\\\":\\\"domain\\\",\\\"group\\\":\\\"yastatic.net\\\",\\\"ty" +
            "pe\\\":\\\"boolean\\\",\\\"value\\\":true}]\tpassportuid=983384280\tprofile=\treferer=https://beru.ru/my" +
            "/cart\tregionid=20728\tremoteip=0.0.0.0\tremoteip6=::\trequestid=00059728A547BFFA00030E8209B" +
            "027FB\tsearchquery=\tsessid=1298743181573570824\tsource_uri=prt://yabs-rt@bsmc32f.yabs.yandex." +
            "ru/mnt/raid/phantom2d/watch_log/move\tsourcebit=0\tsslsessionticketiv=ec5ceae3829e7a059babc5a3211a" +
            "d368\tsslsessionticketkeyname=7d4def76c543d17970381cbfa0e8b629\tsubkey=\ttskv_format=bs-watch-" +
            "log\tuniqid=5538624561521625832\tunixtime=1573575710\tupdatetag=\turl=goal://beru.ru/BROWSER_" +
            "STATS\tuseragent=Mozilla/5.0 (Linux; Android 7.1.2; Redmi 4X) AppleWebKit/537.36 (KHTML, like " +
            "Gecko) Chrome/77.0.3865.92 Mobile Safari/537.36\twapprofile=\twatchid=6719618674241769090\txopera" +
            "miniphoneua=\txwapprofile=\n";

        String[] keys = {"expFlags"};
        String[] values = {"all_webim-chat_enable,all_edit-order-delivery-date_enable"};

        List<Date> expectedDateList = Arrays.asList(
            new Date(1573575703000L),
            new Date(1573575703000L),
            new Date(1573575703000L)
        );

        List<Object[]> expectedFieldsList = Arrays.asList(
            new Object[]{
                checker.getHost(), "91.143.40.98", "5538624561521625832", 20728,
                "1573575688431/ca6e0935bf2a012d79a6efa328970500", "market_front_blue_desktop", "Mozilla/5.0 (Linux; " +
                "Android 7.1.2; Redmi 4X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.92 Mobile Safari/5" +
                "37.36", "beru.ru", "blue-market:cart", "resource", "requestCount", "yastatic.net", "domain",
                BrowserStatsTskvLogParser.ValueType.INT, 1573575703000L, 10, 0.0, false, keys, values
            },

            new Object[]{
                checker.getHost(), "91.143.40.98", "5538624561521625832", 20728,
                "1573575688431/ca6e0935bf2a012d79a6efa328970500", "market_front_blue_desktop", "Mozilla/5.0 (Linux; " +
                "Android 7.1.2; Redmi 4X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.92 Mobile Safari/5" +
                "37.36", "beru.ru", "blue-market:cart", "resource", "cacheHit", "yastatic.net", "domain",
                BrowserStatsTskvLogParser.ValueType.FLOAT, 1573575703000L, 0, 1.0, false, keys, values
            },

            new Object[]{
                checker.getHost(), "91.143.40.98", "5538624561521625832", 20728,
                "1573575688431/ca6e0935bf2a012d79a6efa328970500", "market_front_blue_desktop", "Mozilla/5.0 (Linux; " +
                "Android 7.1.2; Redmi 4X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.92 Mobile Safari/5" +
                "37.36", "beru.ru", "blue-market:cart", "resource", "transferSize", "yastatic.net", "domain",
                BrowserStatsTskvLogParser.ValueType.BOOLEAN, 1573575703000L, 0, 0.0, true, keys, values
            }
        );

        checker.check(line, expectedDateList, expectedFieldsList);
    }

    @Test
    public void parseRealBlueTouchBrowserStatsOfAllTypes() throws Exception {
        String line = "_logfeller_icookie=1270460621575302823\t_logfeller_timestamp=1575303530\t_stbx=rt3.sas--yabs" +
            "-rt--bs-watch-log:80@@101086563@@QOhz6DKhjGX9deJgjS9A4A@@1575303531012@@1575303787@@bs-watch-log@@21" +
            "01771@@1575303532897\tantivirusyes=0\tbrowserinfo=ti:1:ns:1575304522368:s:375x667x32:sk:2:adb:2:fpr:2166" +
            "13626101:cn:1:w:375x553:z:180:i:20191202193524:et:1575304525:en:utf-8:c:1:la:ru-ru:ar:1:ls:123038769571" +
            "2:rqn:690:rn:356779735:hid:488350980:ds:,,,,,,,,,2068,2071,3,:gdpr:8:fu:3:v:1747:wv:2:rqnl:1:st:157530" +
            "4525:u:1575303821552976870:pu:2717998129:fip:9cfb67bbfcc75549ad5789f86d6dbf20-7950ec0297c1232285986092" +
            "2e071362-a81f3b9bcdd80a361c14af38dc09b309-871902bc040e47ac1e7804d815d62f14:t:Купить Портьеры Amore Mi" +
            "o RR 810503-201 на ленте 270 см серая по низкой цене с доставкой из маркетплейса Беру\tclientip=83.22" +
            "0.238.5\tclientip6=::ffff:83.220.238.5\tclientport=0\tcookiegid=0\tcookiegpauto=\tcookiei=12704606" +
            "21575302823\tcookiel=\tcookieys=\tcounterclass=0\tcounterid=47628343\tcryptaid=0\tdomainuserid=15753" +
            "03821552976870\tdomainzone=yandex.ru\tdonottrack=0\tetag=0\teventtime=1575303530\tfuniqid=0\theadera" +
            "rgs=293: i\\=nQ/e6OjHWQpEwUlk8rMqkBQ85+6qrt1dy2zVRxdfDzoHSfv+teIsny9IeNfbQdmNzaP4BV8wU1Y6FR+LxDipQzH" +
            "xfjY\\=; yabs-sid\\=2061178971575302829; yandexuid\\=1270460621575302823; yp\\=1890662829.yrtsi.1575" +
            "302829; usst\\=EAAAAAAAAADwAQoOCgJkcxIIMjYyNTUwMTg,\tiso_eventtime=2019-12-02 19:18:50\tlayerid=9\tm" +
            "etrikaexp=\tparams=[{\\\"timestamp\\\":1575304525,\\\"requestId\\\":\\\"1575303528245/384366ce437203" +
            "508ae736efba980500\\\",\\\"info\\\":{\\\"serviceId\\\":\\\"market_front_blue_touch\\\",\\\"page" +
            "Id\\\":\\\"blue-market:product\\\",\\\"expFlags\\\":\\\"all_webim-chat_enable,dsk_checkout_pickupse" +
            "arch_off\\\"},\\\"name\\\":\\\"resource\\\",\\\"portion\\\":\\\"requestCount\\\",\\\"group" +
            "By\\\":\\\"domain\\\",\\\"group\\\":\\\"yastatic.net\\\",\\\"type\\\":\\\"int\\\",\\\"value\\\":4}" +
            ",{\\\"timestamp\\\":1575304525,\\\"requestId\\\":\\\"1575303528245/384366ce437203508ae736efba9805" +
            "00\\\",\\\"info\\\":{\\\"serviceId\\\":\\\"market_front_blue_touch\\\",\\\"pageId\\\":\\\"blue-mar" +
            "ket:product\\\",\\\"expFlags\\\":\\\"all_webim-chat_enable,dsk_checkout_pickupsearch_off\\\"},\\\"n" +
            "ame\\\":\\\"resource\\\",\\\"portion\\\":\\\"requestCount\\\",\\\"groupBy\\\":\\\"domain\\\",\\\"gr" +
            "oup\\\":\\\"yastat.net\\\",\\\"type\\\":\\\"float\\\",\\\"value\\\":8.5},{\\\"timestamp\\\":15753045" +
            "25,\\\"requestId\\\":\\\"1575303528245/384366ce437203508ae736efba980500\\\",\\\"info\\\":{\\\"ser" +
            "viceId\\\":\\\"market_front_blue_touch\\\",\\\"pageId\\\":\\\"blue-market:product\\\",\\\"expFlag" +
            "s\\\":\\\"all_webim-chat_enable,dsk_checkout_pickupsearch_off\\\"},\\\"name\\\":\\\"resour" +
            "ce\\\",\\\"portion\\\":\\\"requestCount\\\",\\\"groupBy\\\":\\\"domain\\\",\\\"group\\\":\\\"m.ber" +
            "u.ru\\\",\\\"type\\\":\\\"boolean\\\",\\\"value\\\":true}]\tpassportuid=0\t" +
            "profile=\treferer=https://m.ber" +
            "u.ru/product/portery-amore-mio-rr-810503-201-na-" +
            "lente-270-sm-seraia/209986259?show-uid\\=15753034752650809649306006\tregionid=213\tremoteip=0.0.0" +
            ".0\tremoteip6=::\trequestid=000598BAEF5D041900030DC55B54AA47\tsearchquery=\tsessid=20611789715753" +
            "02829\tsource_uri=prt://yabs-rt@bsmc13i.yandex.ru/mnt/raid/phantom2d/watch_log/move\tsourcebit=0\tss" +
            "lsessionticketiv=\tsslsessionticketkeyname=\tsubkey=\ttskv_format=bs-watch-log\tuniqid=127046062157" +
            "5302823\tunixtime=1575303530\tupdatetag=\turl=goal://m.beru.ru/BROWSER_STATS\tuseragent=Mozilla/5.0" +
            " (iPhone; CPU iPhone OS 13_1_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0" +
            ".1 Mobile/15E148 Safari/604.1\twapprofile=\twatchid=7172556261110517189\t" +
            "xoperaminiphoneua=\txwapprofile=\n";

        String[] keys = {"expFlags"};
        String[] values = {"all_webim-chat_enable,dsk_checkout_pickupsearch_off"};

        List<Date> expectedDateList = Arrays.asList(
            new Date(1575304525000L),
            new Date(1575304525000L),
            new Date(1575304525000L)
        );

        List<Object[]> expectedFieldsList = Arrays.asList(
            new Object[]{
                checker.getHost(), "83.220.238.5", "1270460621575302823", 213,
                "1575303528245/384366ce437203508ae736efba980500", "market_front_blue_touch", "Mozilla/5.0 (iPhon" +
                "e; CPU iPhone OS 13_1_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.1 Mo" +
                "bile/15E148 Safari/604.1", "m.beru.ru", "blue-market:product", "resource", "requestCount",
                "yastatic.net", "domain", BrowserStatsTskvLogParser.ValueType.INT, 1575304525000L, 4, 0.0, false,
                keys, values
            },

            new Object[]{
                checker.getHost(), "83.220.238.5", "1270460621575302823", 213,
                "1575303528245/384366ce437203508ae736efba980500", "market_front_blue_touch", "Mozilla/5.0 (iPhon" +
                "e; CPU iPhone OS 13_1_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.1 Mo" +
                "bile/15E148 Safari/604.1", "m.beru.ru", "blue-market:product", "resource", "requestCount",
                "yastat.net", "domain", BrowserStatsTskvLogParser.ValueType.FLOAT, 1575304525000L, 0, 8.5, false,
                keys, values
            },

            new Object[]{
                checker.getHost(), "83.220.238.5", "1270460621575302823", 213,
                "1575303528245/384366ce437203508ae736efba980500", "market_front_blue_touch", "Mozilla/5.0 (iPhon" +
                "e; CPU iPhone OS 13_1_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.1 Mo" +
                "bile/15E148 Safari/604.1", "m.beru.ru", "blue-market:product", "resource", "requestCount", "m.beru.ru",
                "domain", BrowserStatsTskvLogParser.ValueType.BOOLEAN, 1575304525000L, 0, 0.0, true, keys, values
            }
        );

        checker.check(line, expectedDateList, expectedFieldsList);
    }

    @Test
    public void parseRealWhiteBrowserStatsOfAllTypes() throws Exception {
        String line = "_logfeller_icookie=629236511530358192\t_logfeller_timestamp=1575303734\t_stbx=rt3.sas--ya" +
            "bs-rt--bs-watch-log:64@@102813465@@C_yuN6PHVJ7Z9vk8stLvSQ@@1575303734714@@1575303737@@bs-watch-log@@" +
            "2099407@@1575303737309\tantivirusyes=0\tbrowserinfo=ti:1:dp:1:ns:1575303732449:s:1536x864x24:sk:1.25:" +
            "adb:2:fpr:67501995301:cn:1:w:1519x722:z:180:i:20191202192216:et:1575303736:en:utf-8:c:1:la:ru-ru:ntf:" +
            "1:ar:1:ls:1368509262698:rqn:4291:rn:184839160:hid:349679523:ds:,,,,,,,,,3412,3414,10,:gdpr:13:fu:3:v" +
            ":1744:rqnl:1:st:1575303736:u:1532295529600207246:pp:3629563401:t:Выбрать Смартфон Samsung Galaxy A50" +
            " 64GB по выгодной цене на Яндекс.Маркете\tclientip=188.227.113.235\tclientip6=::ffff:188.227.113.2" +
            "35\tclientport=0\tcookiegid=118936\tcookiegpauto=\tcookiei=629236511530358192\tcookiel=Vm4EaHhfbUh" +
            "GbnBeUWltdWl4e2pGamZAFQQXFjclOwMrKiJ6BV9t.1540640248.13666.346680.d1d64e9d14149f3db6bc8f75e3e5108" +
            "2\tcookieys=\tcounterclass=0\tcounterid=160656\tcryptaid=11687214191027300073\tdomainuserid=153229" +
            "5529600207246\tdomainzone=yandex.ru\tdonottrack=0\tetag=0\teventtime=1575303734\tfuniqid=66601517771" +
            "28491040\theaderargs=293: mda\\=0; skid\\=2749885981575303234; Session_id\\=3:1575050174.5.0.15406402" +
            "48304:63HjvA:4c.1|324964154.0.2|208773.441327.; my\\=YwA\\=; fuid01\\=5c6d9cc119ee2820.U7-TGBgLCWHmoH" +
            "Kd32N2dJeSsR65pyWB5YICxkKoAmAShdLCkMdpwdySxCydJEx8SLfG47D88F9TZQFI3zpBJeixHAcEgKV1NjR59jRR6g1ajz0ZOJ" +
            "1fWNOZVBiIpWrt; yandexuid\\=629236511530358192; yandex_gid\\=118936; _ym_isad\\=2; _ym_visorc_503775" +
            "19\\=b; yc\\=1575562429.zen.cach%3A1575306821; yclid_1575303231234\\=:7172477744508341098:62923651153" +
            "0358192; sync_cookie_csrf\\=675597426fake; yabs-frequency\\=/4/3G0105baFrsaAjzR/c4m-RPWu8FLki72REFp" +
            "___zxyZrjZ3WdwrImS9Wu80DLi72OE41_LR1mc3XW55QmS9WuG0vMi72OE201LR1mc3XW_5ImS9auG07xLB1mcJW00XWWfcwOE2" +
            "3BLB1mc3WWx5ImS9WumFHDi72OE20LLB1mc3WWb4gmS9WuW000/; i\\=60rqjvQS3tzfERGnOEYY5Pcgcl7DY/TtQlLfCOuQOK" +
            "lZE2b45Vc3FjGgCDvyFVLG1DH0Es/Y7wBSdSQLODKSRBBvf8A\\=; L\\=Vm4EaHhfbUhGbnBeUWltdWl4e2pGamZAFQQXFjclOw" +
            "MrKiJ6BV9t.1540640248.13666.346680.d1d64e9d14149f3db6bc8f75e3e51082; sessionid2\\=3:1575050174.5.0.154" +
            "0640248304:63HjvA:4c.1|324964154.0.2|208773.925368.; _ym_uid\\=1532295529600207246; _ym_visorc_45411" +
            "513\\=b; yabs-sid\\=2354031551575302544; _ym_visorc_160656\\=b; yp\\=1845718192.yrts.1530358192#184" +
            "5718192.yrtsi.1530358192#1586069065.szm.1_25:1536x864:1536x722#1595971547.p_sw.1564435546#188643613" +
            "3.udn.cDpyb21hbm9ubGluZTIwMDg%3D#1606839224.ygu.1#1575908031.dq.1; yandex_login\\=romanonline2008;" +
            " zm\\=m-white_bender.webp.css-https%3As3home-static_Sxi6N52FJTEJA5ZlHtNBNAyXbuY%3Al\tiso_eventtime" +
            "=2019-12-02 19:22:14\tlayerid=3\tmetrikaexp=\tparams=[{\\\"timestamp\\\":1575303736,\\\"request" +
            "Id\\\":\\\"1575303730799/606d5e917aff79e18fa349fbba980500\\\",\\\"info\\\":{\\\"serviceId\\\":\\\"mar" +
            "ket_front_desktop\\\",\\\"pageId\\\":\\\"market:product\\\",\\\"expFlags\\\":\\\"dsk_search_prem" +
            "ium-offer\\=simple,dsk_km_premium_offer_to_all_tabs_v2\\=ro-do-new_head,dsk_page-product_react-v" +
            "isit-card\\\"},\\\"name\\\":\\\"resource\\\",\\\"portion\\\":\\\"requestCount\\\",\\\"group" +
            "By\\\":\\\"domain\\\",\\\"group\\\":\\\"yastatic.net\\\",\\\"type\\\":\\\"int\\\",\\\"value\\\":2" +
            "6},{\\\"timestamp\\\":1575303736,\\\"requestId\\\":\\\"1575303730799/606d5e917aff79e18fa349fbba98" +
            "0500\\\",\\\"info\\\":{\\\"serviceId\\\":\\\"market_front_desktop\\\",\\\"pageId\\\":\\\"market:" +
            "product\\\",\\\"expFlags\\\":\\\"dsk_search_premium-offer\\=simple,dsk_km_premium_offer_to_all_" +
            "tabs_v2\\=ro-do-new_head,dsk_page-product_react-visit-card\\\"},\\\"name\\\":\\\"resource\\\",\\\"po" +
            "rtion\\\":\\\"cacheHit\\\",\\\"groupBy\\\":\\\"domain\\\",\\\"group\\\":\\\"yastatic.net\\\",\\\"ty" +
            "pe\\\":\\\"float\\\",\\\"value\\\":0.962},{\\\"timestamp\\\":1575303736,\\\"requestId\\\":\\\"15753" +
            "03730799/606d5e917aff79e18fa349fbba980500\\\",\\\"info\\\":{\\\"serviceId\\\":\\\"market_front_de" +
            "sktop\\\",\\\"pageId\\\":\\\"market:product\\\",\\\"expFlags\\\":\\\"dsk_search_premium-offer\\=sim" +
            "ple,dsk_km_premium_offer_to_all_tabs_v2\\=ro-do-new_head,dsk_page-product_react-visit-card\\\"},\\\"na" +
            "me\\\":\\\"resource\\\",\\\"portion\\\":\\\"transferSize\\\",\\\"groupBy\\\":\\\"domain\\\",\\\"gro" +
            "up\\\":\\\"yastatic.net\\\",\\\"type\\\":\\\"boolean\\\",\\\"value\\\":true}]\tpassportuid=324964154\tpr" +
            "ofile=\treferer=https://market.yandex.ru/product--smartfon-samsung-galaxy-a50-64gb/394273081?tra" +
            "ck\\=tabs\tregionid=118936\tremoteip=0.0.0.0\tremoteip6=::\trequestid=000598BAFB82353100030DD70947" +
            "1074\tsearchquery=\tsessid=2354031551575302544\tsource_uri=prt://yabs-rt@bsmc15e.yandex.ru/mnt/raid/p" +
            "hantom2d/watch_log/move\tsourcebit=0\tsslsessionticketiv=6906db4871485e74ef007649c76f324d\tsslsession" +
            "ticketkeyname=14de00102a6927fcc5090ed3234f3ba9\tsubkey=\ttskv_format=bs-watch-log\tuniqid=62923651153" +
            "0358192\tunixtime=1575303734\tupdatetag=noindex\turl=goal://market.yandex.ru/BROWSER_STATS\tuseragent" +
            "=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.1" +
            "08 Safari/537.36\twapprofile=\twatchid=7172609673084997079\txoperaminiphoneua=\txwapprofile=\n";

        String[] keys = {"expFlags"};
        String[] values = {
            "dsk_search_premium-offer=simple," +
                "dsk_km_premium_offer_to_all_tabs_v2=ro-do-new_head," +
                "dsk_page-product_react-visit-card"
        };

        List<Date> expectedDateList = Arrays.asList(
            new Date(1575303736000L),
            new Date(1575303736000L),
            new Date(1575303736000L)
        );

        List<Object[]> expectedFieldsList = Arrays.asList(
            new Object[]{
                checker.getHost(), "188.227.113.235", "629236511530358192", 118936,
                "1575303730799/606d5e917aff79e18fa349fbba980500", "market_front_desktop", "Mozilla/5.0 (Windows NT" +
                " 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36",
                "market.yandex.ru", "market:product", "resource", "requestCount", "yastatic.net", "domain",
                BrowserStatsTskvLogParser.ValueType.INT, 1575303736000L, 26, 0.0, false, keys, values
            },

            new Object[]{
                checker.getHost(), "188.227.113.235", "629236511530358192", 118936,
                "1575303730799/606d5e917aff79e18fa349fbba980500", "market_front_desktop", "Mozilla/5.0 (Windows NT" +
                " 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36",
                "market.yandex.ru", "market:product", "resource", "cacheHit", "yastatic.net", "domain",
                BrowserStatsTskvLogParser.ValueType.FLOAT, 1575303736000L, 0, 0.9620000123977661, false, keys, values
            },

            new Object[]{
                checker.getHost(), "188.227.113.235", "629236511530358192", 118936,
                "1575303730799/606d5e917aff79e18fa349fbba980500", "market_front_desktop", "Mozilla/5.0 (Windows NT" +
                " 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36",
                "market.yandex.ru", "market:product", "resource", "transferSize", "yastatic.net", "domain",
                BrowserStatsTskvLogParser.ValueType.BOOLEAN, 1575303736000L, 0, 0.0, true, keys, values
            }
        );

        checker.check(line, expectedDateList, expectedFieldsList);
    }

    @Test
    public void parseRealWhiteTouchBrowserStatsOfAllTypes() throws Exception {
        String line = "_logfeller_icookie=2102776830676550725\t_logfeller_timestamp=1575303501\t_stbx=rt3.vla--yabs-" +
            "rt--bs-watch-log:46@@47950189@@DkSjq8T1CW7JWswZhrLH-Q@@1575303502078@@1575303698@@bs-watch-log@@7347996@" +
            "@1575303503663\tantivirusyes=0\tbrowserinfo=ti:7:dp:0:nt:3:ns:1575303501640:s:360x640x24:sk:3:adb:2:" +
            "fpr:216613626101:cn:1:w:360x524:z:240:i:20191202201823:et:1575303504:en:utf-8:c:1:la:ru-ru:ar:1:ls:5" +
            "71872036023:rqn:31008:rn:1066936308:hid:603338734:ds:,,,,,,,,,1731,1732,25,:gdpr:13:fu:3:v:1744:rqnl" +
            ":1:st:1575303504:u:1529815078799523844:t:Поиск — Яндекс.Маркет\tclientip=31.171.194.131\tclientip6=::" +
            "ffff:31.171.194.131\tclientport=0\tcookiegid=51\tcookiegpauto=53_30621719360352:50_32767868041992:1600" +
            "_0:2:1575303390\tcookiei=2102776830676550725\tcookiel=Qnd3fQQFB2tJbXl5QXJ0alJIZ2wMA1tARgITbj8OUAkBXid" +
            "J.1575302017.14067.358582.85d38e90f599ae38cfc7fe7cf349c259\tcookieys=ymrefl.FA1C9D690F9D8FA6#svt.1#w" +
            "prid.1575284885264352-1015472921321073967500247-sas1-5660-TCH#uuid.e001b204519559f12ad8f1bc764df5de#gp" +
            "auto.53_30621719360352:50_32767868041992:1600_0:2:1575303390#udn.czoxMzYxODcxNDI6dms6QWxleCAkJC" +
            "Q\\=\tcounterclass=0\tcounterid=722867\tcryptaid=7169103299483098479\tdomainuserid=15298150787995238" +
            "44\tdomainzone=yandex.ru\tdonottrack=0\tetag=0\teventtime=1575303501\tfuniqid=67499316395764190" +
            "74\theaderargs=293: _ym_d\\=1575259516; mda\\=0; Session_id\\=3:1575302017.5.0.1537275935539:mnQ7sA:" +
            "1b.1|726137713.-1.0.2:38026082|208919.2743.; skid\\=6045157501574585699; my\\=YysBMwA\\=; fuid01\\=5d" +
            "ac9310431f5b02.mcG0-FkU6rD-CjFuShh_WVW9pjCF6NY5zcUx9rZsoOoUkRTogAHQk_jiNeLh06FibwNfb-uWlEC0Luoqc7I7d8" +
            "vp78jtaFMrCKySe0XCf5lyhT3K4U0BRJ4g8MOrp0jY; Ya_Music_Player_ID\\=15746186178890.4747168955764247; _y" +
            "m_visorc_44910898\\=b; yandexuid\\=2957547681529825902; yandex_gid\\=51; _ym_visorc_722867\\=b; devic" +
            "e_id\\=\\\"aa6c6555cdc6f2fc6d9857155afe245e4baf02279\\\"; _ym_isad\\=2; usst\\=EAAAAAAAAADwAQoOCgJkcxI" +
            "IMjYyNTUwMDk,; cycada\\=rRZw9HKC7PbAbe6EhRdvugyOsNlM2Jl8BKFaRNn9dj8\\=; yabs-frequency\\=/4/001y000000" +
            "0AORvT/y8F-R6mu8PD__cniE80HXlviR3Y0d7_-R6mu89z__cniE20ebNLjR3Y0egfsRMmuWAEgTcriE22WgdPjR3XW0P-gTcpODU" +
            "1uzdLi-3L0A9LrRFWr/; i\\=OdnyvB7AJLetGnAthfCYhkppm4oAmoII7mxwgCMCnorfb8OMMIXwOYQxIO2o4ZNSE6lMykW1SiMX" +
            "2PwDoaLVD0ZAWoY\\=; active-browser-timestamp\\=1574940033476; L\\=Qnd3fQQFB2tJbXl5QXJ0alJIZ2wMA1tARgITb" +
            "j8OUAkBXidJ.1575302017.14067.358582.85d38e90f599ae38cfc7fe7cf349c259; sessionid2\\=3:1575302017.5.0.15" +
            "37275935539:mnQ7sA:1b.1|726137713.-1.0|208919.611310.; _ym_uid\\=1529815078799523844; ys\\=ymrefl.FA1" +
            "C9D690F9D8FA6#svt.1#wprid.1575284885264352-1015472921321073967500247-sas1-5660-TCH#uuid.e001b204519" +
            "559f12ad8f1bc764df5de#gpauto.53_30621719360352%3A50_32767868041992%3A1600_0%3A2%3A1575303390#udn.cz" +
            "oxMzYxODcxNDI6dms6QWxleCAkJCQ%3D; _ym_visorc_45411513\\=b; yabs-sid\\=1269917121529825906; yp\\=157" +
            "5863926.szm.3%3A640x360%3A360x524#1890662017.udn.czoxMzYxODcxNDI6dms6QWxleCAkJCQ%3D#1853982552.yrt" +
            "si.1538622552#1575476190.gpauto.53_30621719360352%3A50_32767868041992%3A1600_0%3A2%3A1575303390#15" +
            "77255356.sz.640x360x3#2147483647.ygu.0; yandex_login\\=uid-pfbm55ox; instruction\\=1\tiso_eventtime" +
            "=2019-12-02 19:18:21\tlayerid=6\tmetrikaexp=\tparams=[{\\\"timestamp\\\":1575303504,\\\"request" +
            "Id\\\":\\\"1575303499792/17a7c2e4598641eae6bf84edba980500\\\",\\\"info\\\":{\\\"serviceId\\\":\\\"m" +
            "arket_front_touch\\\",\\\"pageId\\\":\\\"touch:list-filters\\\",\\\"expFlags\\\":\\\"touch_cashback" +
            "\\\"},\\\"name\\\":\\\"resource\\\",\\\"portion\\\":\\\"requestCount\\\",\\\"groupBy\\\":\\\"dom" +
            "ain\\\",\\\"group\\\":\\\"yastatic.net\\\",\\\"type\\\":\\\"int\\\",\\\"value\\\":13},{\\\"timest" +
            "amp\\\":1575303504,\\\"requestId\\\":\\\"1575303499792/17a7c2e4598641eae6bf84edba980500\\\",\\\"inf" +
            "o\\\":{\\\"serviceId\\\":\\\"market_front_touch\\\",\\\"pageId\\\":\\\"touch:list-filters\\\",\\\"ex" +
            "pFlags\\\":\\\"touch_cashback\\\"},\\\"name\\\":\\\"resource\\\",\\\"portion\\\":\\\"cacheHi" +
            "t\\\",\\\"groupBy\\\":\\\"domain\\\",\\\"group\\\":\\\"yastatic.net\\\",\\\"type\\\":\\\"flo" +
            "at\\\",\\\"value\\\":1.87},{\\\"timestamp\\\":1575303504,\\\"requestId\\\":\\\"1575303499792/17a7c2e4" +
            "598641eae6bf84edba980500\\\",\\\"info\\\":{\\\"serviceId\\\":\\\"market_front_touch\\\",\\\"page" +
            "Id\\\":\\\"touch:list-filters\\\",\\\"expFlags\\\":\\\"touch_cashback\\\"},\\\"name\\\":\\\"reso" +
            "urce\\\",\\\"portion\\\":\\\"transferSize\\\",\\\"groupBy\\\":\\\"domain\\\",\\\"group\\\":\\\"yas" +
            "tatic.net\\\",\\\"type\\\":\\\"boolean\\\",\\\"value\\\":true}]\tpassportuid=726137713\tprofile=\trefere" +
            "r=https://m.market.yandex.ru/catalog--umnye-chasy-i-braslety-v-samare/56034/filters?hid\\=1049802" +
            "5&text\\=%D1%83%D0%BC%D0%BD%D1%8B%D0%B5%20%D1%87%D0%B0%D1%81%D1%8B&how\\=dprice&local-offers-firs" +
            "t\\=0&glfilter\\=7893318%3A13940810&onstock\\=0\tregionid=51\tremoteip=0.0.0.0\tremoteip6=::\treq" +
            "uestid=000598BAEDA4DC9A00030DBD7707F3A1\tsearchquery=\tsessid=1269917121529825906\tsource_uri=prt" +
            "://yabs-rt@bsmc12m.yabs.yandex.ru/mnt/raid/phantom2d/watch_log/move\tsourcebit=0\tsslsessiontick" +
            "etiv=f6a74ddb08f44f6687c0c40b9ffe7e9e\tsslsessionticketkeyname=14de00102a6927fcc5090ed3234f3b" +
            "a9\tsubkey=\ttskv_format=bs-watch-log\tuniqid=2957547681529825902\tunixtime=1575303501\tupdatet" +
            "ag=noindex\turl=goal://m.market.yandex.ru/BROWSER_STATS\tuseragent=Mozilla/5.0 (Linux; Android 7.0; " +
            "SM-G930F Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.9" +
            "8 Mobile Safari/537.36 YandexSearch/7.70 YandexSearchWebView/7.70\twapprofile=\twatchid=717254869" +
            "5766011325\txoperaminiphoneua=\txwapprofile=";

        String[] keys = {"expFlags"};
        String[] values = {"touch_cashback"};

        List<Date> expectedDateList = Arrays.asList(
            new Date(1575303504000L),
            new Date(1575303504000L),
            new Date(1575303504000L)
        );

        List<Object[]> expectedFieldsList = Arrays.asList(
            new Object[]{
                checker.getHost(), "31.171.194.131", "2957547681529825902", 51,
                "1575303499792/17a7c2e4598641eae6bf84edba980500", "market_front_touch", "Mozilla/5.0 (Linux; Android" +
                " 7.0; SM-G930F Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.316" +
                "3.98 Mobile Safari/537.36 YandexSearch/7.70 YandexSearchWebView/7.70", "m.market.yandex.ru",
                "touch:list-filters", "resource", "requestCount", "yastatic.net", "domain",
                BrowserStatsTskvLogParser.ValueType.INT, 1575303504000L, 13, 0.0, false, keys, values
            },

            new Object[]{
                checker.getHost(), "31.171.194.131", "2957547681529825902", 51,
                "1575303499792/17a7c2e4598641eae6bf84edba980500", "market_front_touch", "Mozilla/5.0 (Linux; Android" +
                " 7.0; SM-G930F Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.316" +
                "3.98 Mobile Safari/537.36 YandexSearch/7.70 YandexSearchWebView/7.70", "m.market.yandex.ru",
                "touch:list-filters", "resource", "cacheHit", "yastatic.net", "domain",
                BrowserStatsTskvLogParser.ValueType.FLOAT, 1575303504000L, 0, 1.8700000047683716, false, keys, values
            },

            new Object[]{
                checker.getHost(), "31.171.194.131", "2957547681529825902", 51,
                "1575303499792/17a7c2e4598641eae6bf84edba980500", "market_front_touch", "Mozilla/5.0 (Linux; Android" +
                " 7.0; SM-G930F Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.316" +
                "3.98 Mobile Safari/537.36 YandexSearch/7.70 YandexSearchWebView/7.70", "m.market.yandex.ru",
                "touch:list-filters", "resource", "transferSize", "yastatic.net", "domain",
                BrowserStatsTskvLogParser.ValueType.BOOLEAN, 1575303504000L, 0, 0.0, true, keys, values
            }
        );

        checker.check(line, expectedDateList, expectedFieldsList);
    }

    @Test
    public void parseWhiteTouchBrowserStatsOfAllTypesWithNewFormat() throws Exception {
        String line = "_logfeller_icookie=2102776830676550725\t_logfeller_timestamp=1575303501\t_stbx=rt3.vla--yabs-" +
            "rt--bs-watch-log:46@@47950189@@DkSjq8T1CW7JWswZhrLH-Q@@1575303502078@@1575303698@@bs-watch-log@@7347996@" +
            "@1575303503663\tantivirusyes=0\tbrowserinfo=ti:7:dp:0:nt:3:ns:1575303501640:s:360x640x24:sk:3:adb:2:" +
            "fpr:216613626101:cn:1:w:360x524:z:240:i:20191202201823:et:1575303504:en:utf-8:c:1:la:ru-ru:ar:1:ls:5" +
            "71872036023:rqn:31008:rn:1066936308:hid:603338734:ds:,,,,,,,,,1731,1732,25,:gdpr:13:fu:3:v:1744:rqnl" +
            ":1:st:1575303504:u:1529815078799523844:t:Поиск — Яндекс.Маркет\tclientip=31.171.194.131\tclientip6=::" +
            "ffff:31.171.194.131\tclientport=0\tcookiegid=51\tcookiegpauto=53_30621719360352:50_32767868041992:1600" +
            "_0:2:1575303390\tcookiei=2102776830676550725\tcookiel=Qnd3fQQFB2tJbXl5QXJ0alJIZ2wMA1tARgITbj8OUAkBXid" +
            "J.1575302017.14067.358582.85d38e90f599ae38cfc7fe7cf349c259\tcookieys=ymrefl.FA1C9D690F9D8FA6#svt.1#w" +
            "prid.1575284885264352-1015472921321073967500247-sas1-5660-TCH#uuid.e001b204519559f12ad8f1bc764df5de#gp" +
            "auto.53_30621719360352:50_32767868041992:1600_0:2:1575303390#udn.czoxMzYxODcxNDI6dms6QWxleCAkJC" +
            "Q\\=\tcounterclass=0\tcounterid=722867\tcryptaid=7169103299483098479\tdomainuserid=15298150787995238" +
            "44\tdomainzone=yandex.ru\tdonottrack=0\tetag=0\teventtime=1575303501\tfuniqid=67499316395764190" +
            "74\theaderargs=293: _ym_d\\=1575259516; mda\\=0; Session_id\\=3:1575302017.5.0.1537275935539:mnQ7sA:" +
            "1b.1|726137713.-1.0.2:38026082|208919.2743.; skid\\=6045157501574585699; my\\=YysBMwA\\=; fuid01\\=5d" +
            "ac9310431f5b02.mcG0-FkU6rD-CjFuShh_WVW9pjCF6NY5zcUx9rZsoOoUkRTogAHQk_jiNeLh06FibwNfb-uWlEC0Luoqc7I7d8" +
            "vp78jtaFMrCKySe0XCf5lyhT3K4U0BRJ4g8MOrp0jY; Ya_Music_Player_ID\\=15746186178890.4747168955764247; _y" +
            "m_visorc_44910898\\=b; yandexuid\\=2957547681529825902; yandex_gid\\=51; _ym_visorc_722867\\=b; devic" +
            "e_id\\=\\\"aa6c6555cdc6f2fc6d9857155afe245e4baf02279\\\"; _ym_isad\\=2; usst\\=EAAAAAAAAADwAQoOCgJkcxI" +
            "IMjYyNTUwMDk,; cycada\\=rRZw9HKC7PbAbe6EhRdvugyOsNlM2Jl8BKFaRNn9dj8\\=; yabs-frequency\\=/4/001y000000" +
            "0AORvT/y8F-R6mu8PD__cniE80HXlviR3Y0d7_-R6mu89z__cniE20ebNLjR3Y0egfsRMmuWAEgTcriE22WgdPjR3XW0P-gTcpODU" +
            "1uzdLi-3L0A9LrRFWr/; i\\=OdnyvB7AJLetGnAthfCYhkppm4oAmoII7mxwgCMCnorfb8OMMIXwOYQxIO2o4ZNSE6lMykW1SiMX" +
            "2PwDoaLVD0ZAWoY\\=; active-browser-timestamp\\=1574940033476; L\\=Qnd3fQQFB2tJbXl5QXJ0alJIZ2wMA1tARgITb" +
            "j8OUAkBXidJ.1575302017.14067.358582.85d38e90f599ae38cfc7fe7cf349c259; sessionid2\\=3:1575302017.5.0.15" +
            "37275935539:mnQ7sA:1b.1|726137713.-1.0|208919.611310.; _ym_uid\\=1529815078799523844; ys\\=ymrefl.FA1" +
            "C9D690F9D8FA6#svt.1#wprid.1575284885264352-1015472921321073967500247-sas1-5660-TCH#uuid.e001b204519" +
            "559f12ad8f1bc764df5de#gpauto.53_30621719360352%3A50_32767868041992%3A1600_0%3A2%3A1575303390#udn.cz" +
            "oxMzYxODcxNDI6dms6QWxleCAkJCQ%3D; _ym_visorc_45411513\\=b; yabs-sid\\=1269917121529825906; yp\\=157" +
            "5863926.szm.3%3A640x360%3A360x524#1890662017.udn.czoxMzYxODcxNDI6dms6QWxleCAkJCQ%3D#1853982552.yrt" +
            "si.1538622552#1575476190.gpauto.53_30621719360352%3A50_32767868041992%3A1600_0%3A2%3A1575303390#15" +
            "77255356.sz.640x360x3#2147483647.ygu.0; yandex_login\\=uid-pfbm55ox; instruction\\=1\tiso_eventtime" +
            "=2019-12-02 19:18:21\tlayerid=6\tmetrikaexp=\tparams={\\\"info\\\":{\\\"pageId\\\":\\\"" +
            "touch:list-filters\\\"," +
            "\\\"serviceId\\\":\\\"market_front_touch\\\",\\\"expFlags\\\":\\\"touch_cashback\\\"},\\\"name\\\":\\\"" +
            "resource\\\",\\\"requestId\\\":\\\"1575303499792/17a7c2e4598641eae6bf84edba980500\\\",\\\"timestamp\\\":" +
            "1575303504,\\\"stats\\\":[{\\\"group\\\":\\\"yastatic.net\\\",\\\"groupBy\\\":\\\"domain\\\",\\\"portion" +
            "\\\":\\\"requestCount\\\",\\\"type\\\":\\\"int\\\",\\\"value\\\":13},{\\\"group\\\":\\\"yastatic.net" +
            "\\\",\\\"groupBy\\\":\\\"domain\\\",\\\"portion\\\":\\\"cacheHit\\\",\\\"type\\\":\\\"float\\\"," +
            "\\\"value\\\":1.87},{\\\"group\\\":\\\"yastatic.net\\\",\\\"groupBy\\\":\\\"domain\\\",\\\"portion\\\":" +
            "\\\"transferSize\\\",\\\"type\\\":\\\"boolean\\\",\\\"value\\\":true}]}\tpassportuid=726137713\tprof" +
            "ile=\treferer=https://m.market.yandex.ru/catalog--umnye-chasy-i-braslety-v-samare/56034/filters?hid\\=" +
            "10498025&text\\=%D1%83%D0%BC%D0%BD%D1%8B%D0%B5%20%D1%87%D0%B0%D1%81%D1%8B&how\\=dprice&local-offers-firs" +
            "t\\=0&glfilter\\=7893318%3A13940810&onstock\\=0\tregionid=51\tremoteip=0.0.0.0\tremoteip6=::\treq" +
            "uestid=000598BAEDA4DC9A00030DBD7707F3A1\tsearchquery=\tsessid=1269917121529825906\tsource_uri=prt" +
            "://yabs-rt@bsmc12m.yabs.yandex.ru/mnt/raid/phantom2d/watch_log/move\tsourcebit=0\tsslsessiontick" +
            "etiv=f6a74ddb08f44f6687c0c40b9ffe7e9e\tsslsessionticketkeyname=14de00102a6927fcc5090ed3234f3b" +
            "a9\tsubkey=\ttskv_format=bs-watch-log\tuniqid=2957547681529825902\tunixtime=1575303501\tupdatet" +
            "ag=noindex\turl=goal://m.market.yandex.ru/BROWSER_STATS\tuseragent=Mozilla/5.0 (Linux; Android 7.0; " +
            "SM-G930F Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.9" +
            "8 Mobile Safari/537.36 YandexSearch/7.70 YandexSearchWebView/7.70\twapprofile=\twatchid=717254869" +
            "5766011325\txoperaminiphoneua=\txwapprofile=";

        String[] keys = {"expFlags"};
        String[] values = {"touch_cashback"};

        List<Date> expectedDateList = Arrays.asList(
            new Date(1575303504000L),
            new Date(1575303504000L),
            new Date(1575303504000L)
        );

        List<Object[]> expectedFieldsList = Arrays.asList(
            new Object[]{
                checker.getHost(), "31.171.194.131", "2957547681529825902", 51,
                "1575303499792/17a7c2e4598641eae6bf84edba980500", "market_front_touch", "Mozilla/5.0 (Linux; Android" +
                " 7.0; SM-G930F Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.316" +
                "3.98 Mobile Safari/537.36 YandexSearch/7.70 YandexSearchWebView/7.70", "m.market.yandex.ru",
                "touch:list-filters", "resource", "requestCount", "yastatic.net", "domain",
                BrowserStatsTskvLogParser.ValueType.INT, 1575303504000L, 13, 0.0, false, keys, values
            },

            new Object[]{
                checker.getHost(), "31.171.194.131", "2957547681529825902", 51,
                "1575303499792/17a7c2e4598641eae6bf84edba980500", "market_front_touch", "Mozilla/5.0 (Linux; Android" +
                " 7.0; SM-G930F Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.316" +
                "3.98 Mobile Safari/537.36 YandexSearch/7.70 YandexSearchWebView/7.70", "m.market.yandex.ru",
                "touch:list-filters", "resource", "cacheHit", "yastatic.net", "domain",
                BrowserStatsTskvLogParser.ValueType.FLOAT, 1575303504000L, 0, 1.8700000047683716, false, keys, values
            },

            new Object[]{
                checker.getHost(), "31.171.194.131", "2957547681529825902", 51,
                "1575303499792/17a7c2e4598641eae6bf84edba980500", "market_front_touch", "Mozilla/5.0 (Linux; Android" +
                " 7.0; SM-G930F Build/NRD90M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.316" +
                "3.98 Mobile Safari/537.36 YandexSearch/7.70 YandexSearchWebView/7.70", "m.market.yandex.ru",
                "touch:list-filters", "resource", "transferSize", "yastatic.net", "domain",
                BrowserStatsTskvLogParser.ValueType.BOOLEAN, 1575303504000L, 0, 0.0, true, keys, values
            }
        );

        checker.check(line, expectedDateList, expectedFieldsList);
    }
}
