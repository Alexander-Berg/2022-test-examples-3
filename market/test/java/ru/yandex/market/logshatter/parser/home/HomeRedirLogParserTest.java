package ru.yandex.market.logshatter.parser.home;

import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author Evgeny Bogdanov <a href="mailto:evbogdanov@yandex-team.ru"></a>
 * @date 20/11/17
 */
public class HomeRedirLogParserTest {

    @Test
    @SuppressWarnings("MethodLength")
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new HomeRedirLogParser());
        checker.check(
            "HTTP_REFERER=https://yandex.ru/@@iso_eventtime=2017-11-20 " +
                "12:31:03@@events=%5B%7B%22event%22%3A%22show%22%2C%22parent-path%22%3A%22v14%22%2C%22blocks%22%3A%5B" +
                "%7B%22ctag%22%3A%22voice%22%7D%5D%7D%5D@@timefs=1094@@yandexuid=7442778681504810037@@ip=176.215.53" +
                ".250,176.215.53.250@@dtype=clck@@tld=ru@@statver=2022-07-04.3@@ip=2a02:6b8:0:40c:8d16:83ae:346:5e59" +
                "@@vars=143=2048,1042=chrome_62.0.3202_webkit_537.36_windows_6" +
                ".1_0_0_0,1964=@@_stbx=rt3.sas--redir--redir-log:50@@108236271@@base64:ISsljTM2LxcjlXctOX0rig" +
                "@@1511170665234@@1511170665@@redir-log@@2411683360@@source_uri=prt://redir@sas1-5420.search.yandex" +
                ".net/usr/local/www/logs/current-redir-clickdaemon-18100@@session_id=1511170262.09819.22881" +
                ".23440@@url=@@monitoring=1319@@at=1@@timestamp=1511170263@@timefrs=740@@_logfeller_index_bucket" +
                "=//home/logfeller/index/redir/redir-log/900-1800/1511170800/1511170200@@icookie=7442778681504810037" +
                "@@unixtime=1511170263@@_logfeller_timestamp=1511170263@@uah=3499925429@@installation_info" +
                "=eyJnZW8iOiJtYW4iLCJ2ZXJ0aWNhbCI6Ik1PUkRBIiwiY3R5cGUiOiJwcm9kIn0=",
            new Date(1511170263000L),
            2, "show", "", "v14.voice", "v14", "voice", "", "man", "ru", "2022-07-04.3",
            "2a02:6b8:0:40c:8d16:83ae:346:5e59", 0, "https://yandex.ru/", "1511170262.09819.22881.23440",
            "", UnsignedLong.valueOf(7442778681504810037L), UnsignedLong.valueOf(7442778681504810037L), 740, 1094,
            new Integer[]{},
            "chrome", "62.0.3202", "webkit", "537.36", "windows", "6.1", 0, 0, 0, UnsignedLong.valueOf(
                "12754281975948553851")
        );

        checker.check(
            "HTTP_REFERER=https://yandex.ru/@@iso_eventtime=2017-11-20 " +
                "12:31:03@@events=%5B%7B%22event%22%3A%22show%22%2C%22parent-path%22%3A%22v14%22%2C%22blocks%22%3A%5B" +
                "%7B%22ctag%22%3A%22voice%22%7D%5D%7D%5D@@timefs=1094@@yandexuid=7442778681504810037@@ip=176.215.53" +
                ".250,176.215.53.250@@dtype=clck@@tld=ru@@ip=2a02:6b8:0:40c:8d16:83ae:346:5e59" +
                "@@vars=143=2048,1042=chrome_62.0.3202_webkit_537.36_windows_6" +
                ".1_0_0_0,1964=31153_2525_3345@@_stbx=rt3" +
                ".sas--redir--redir-log:50@@108236271@@base64:ISsljTM2LxcjlXctOX0rig@@1511170665234@@1511170665" +
                "@@redir-log@@2411683360@@source_uri=prt://redir@sas1-5420.search.yandex" +
                ".net/usr/local/www/logs/current-redir-clickdaemon-18100@@session_id=1511170262.09819.22881" +
                ".23440@@url=@@monitoring=1319@@at=1@@timestamp=1511170263@@timefrs=740@@_logfeller_index_bucket" +
                "=//home/logfeller/index/redir/redir-log/900-1800/1511170800/1511170200@@icookie=7442778681504810037" +
                "@@unixtime=1511170263@@_logfeller_timestamp=1511170263@@uah=3499925429@@installation_info" +
                "=eyJnZW8iOiJtYW4iLCJ2ZXJ0aWNhbCI6Ik1PUkRBIiwiY3R5cGUiOiJwcm9kIn0=",
            new Date(1511170263000L),
            2, "show", "", "v14.voice", "v14", "voice", "", "man", "ru", "", "2a02:6b8:0:40c:8d16:83ae:346:5e59", 0,
            "https://yandex.ru/", "1511170262.09819.22881.23440",
            "", UnsignedLong.valueOf(7442778681504810037L), UnsignedLong.valueOf(7442778681504810037L),
            740,
            1094,
            new Integer[]{31153, 2525, 3345},
            "chrome", "62.0.3202", "webkit", "537.36", "windows", "6.1", 0, 0, 0, UnsignedLong.valueOf(
                "12754281975948553851")
        );

        checker.check(
            "HTTP_REFERER=https://yandex.ru/@@dtype=clck" +
                "@@tld=ru@@timefs=6056@@timefrs=5889@@session_id=1511171661.81015" +
                ".20941.31450@@events=%5B%7B%22event%22%3A%22tech%22%2C%22parent-path%22%3A%22v14%22%2C%22blocks%22" +
                "%3A%5B%7B%22ctag%22%3A%22geolocation%22%2C%22children%22%3A%5B%7B%22ctag%22%3A%22autodetection%22%2C" +
                "%22children%22%3A%5B%7B%22ctag%22%3A%22request%22%7D%5D%7D%5D%7D%5D%7D%5D@@at=1@@uah=763090462" +
                "@@icookie=9647230491511166356@@url=@@1511171667@@88.200.136.195,88.200.136.195@@9647230491511166356",
            new Date(1511171667000L),
            2, "tech", "", "v14.geolocation.autodetection.request", "v14", "geolocation", "autodetection", "", "ru", "",
            "", 0, "https://yandex.ru/",
            "1511171661.81015.20941.31450", "", UnsignedLong.valueOf("9647230491511166356"),
            UnsignedLong.valueOf("9647230491511166356"), 5889, 6056,
            new Integer[]{},
            "", "", "", "", "", "", 0, 0, 0, UnsignedLong.valueOf("4906829435630565363")
        );

        checker.check(
            "HTTP_REFERER=https://yandex.ru/@@dtype=clck@@tld=ru@@timefs=1622@@timefrs=1144@@session_id=1511171677." +
                "37368.22894.17604@@events=%5B%7B%22event%22%3A%22show%22%2C%22parent-path%22%3A%22geotouch" +
                ".iphone%22%2C%22blocks%22%3A%5B%7B%22ctag%22%3A%22banner%22%2C%22children%22%3A%5B%7B%22ctag%22%3A" +
                "%22realshow%22%7D%5D%7D%5D%7D%2C%7B%22event%22%3A%22show%22%2C%22parent-path%22%3A%22geotouch" +
                ".iphone%22%2C%22blocks%22%3A%5B%7B%22ctag%22%3A%22header%22%2C%22children%22%3A%5B%7B%22ctag%22%3A" +
                "%22realshow%22%7D%5D%7D%5D%7D%2C%7B%22event%22%3A%22show%22%2C%22parent-path%22%3A%22geotouch" +
                ".iphone%22%2C%22blocks%22%3A%5B%7B%22ctag%22%3A%22logo%22%2C%22children%22%3A%5B%7B%22ctag%22%3A" +
                "%22realshow%22%7D%5D%7D%5D%7D%2C%7B%22event%22%3A%22show%22%2C%22parent-path%22%3A%22geotouch" +
                ".iphone%22%2C%22blocks%22%3A%5B%7B%22ctag%22%3A%22search%22%2C%22children%22%3A%5B%7B%22ctag%22%3A" +
                "%22realshow%22%7D%5D%7D%5D%7D%2C%7B%22event%22%3A%22show%22%2C%22parent-path%22%3A%22geotouch" +
                ".iphone%22%2C%22blocks%22%3A%5B%7B%22ctag%22%3A%22tab%22%2C%22children%22%3A%5B%7B%22ctag%22%3A" +
                "%22realshow%22%7D%5D%7D%5D%7D%2C%7B%22event%22%3A%22show%22%2C%22parent-path%22%3A%22geotouch" +
                ".iphone%22%2C%22blocks%22%3A%5B%7B%22ctag%22%3A%22informer%22%2C%22children%22%3A%5B%7B%22ctag%22%3A" +
                "%22realshow%22%7D%5D%7D%5D%7D%5D@@at=1@@installation_info" +
                "=eyJnZW8iOiJtYW4iLCJ2ZXJ0aWNhbCI6Ik1PUkRBIiwiY3R5cGUiOiJwcm9kIn0=@@uah=570369292@@icookie" +
                "=1197431231499978738@@url=@@1511171678@@83.220.236.28,83.220.236.28@@1197431231499978738",
            new Date(1511171678000L),
            2, "show", "", "geotouch.iphone.informer.realshow", "geotouch", "iphone", "informer", "man", "ru", "", "",
            0, "https://yandex.ru/", "1511171677.37368.22894.17604", "", UnsignedLong.valueOf("1197431231499978738"),
            UnsignedLong.valueOf("1197431231499978738"), 1144, 1622, new Integer[]{},
            "", "", "", "", "", "", 0, 0, 0, UnsignedLong.valueOf("2957325507131464116")
        );

        checker.check(
            "HTTP_REFERER=https://www.yandex" +
                ".ru/@@installation_info=eyJnZW8iOiJtYW4iLCJ2ZXJ0aWNhbCI6Ik1PUkRBIiwiY3R5cGUiOiJwcm9kIn0" +
                "=@@iso_eventtime=2018-02-04 23:15:15@@lid=geotouch.iphone.afisha.links.regular" +
                ".2@@timefs=40513@@yandexuid=9933283421517423124@@ip=2.94.140.181,2.94.140.181@@dtype=clck@@_stbx=rt3" +
                ".vla--redir--redir-log:80@@53123870@@base64:PRJjcJGERN6tdJoNq7k6iw" +
                "@@tld=ru@@statver=2022-07-04-58.3@@ip=2a02:6b8:0:40c:8d16:83ae:346:5e59@@1517775315768@@1517775316@@" +
                "redir-log@@11404747413@@source_uri=prt://redir@vla1-3789.search.yandex" +
                ".net/usr/local/www/logs/current-redir-clickdaemon-18100@@url=https://afisha.yandex" +
                ".ru/kostroma/cinema/zomboiashchik?version=mobile&utm_source=yamain_touch&utm_medium=yamain_afisha" +
                "@@at=0@@timestamp=1517775315@@_logfeller_index_bucket=//home/logfeller/index/redir/redir-log/900" +
                "-1800/1517775000/1517774400@@icookie=9933283421517423124@@unixtime=1517775315@@rnd=1517775315497" +
                "@@keyno=0@@_logfeller_timestamp=1517775315@@uah=4250440070@@sid=1517775274.93215.22884.24370",
            new Date(1517775315000L),
            0, "click",
            "afisha.yandex.ru/kostroma/cinema/zomboiashchik",
            "geotouch.iphone.afisha.links.regular.2",
            "geotouch",
            "iphone",
            "afisha",
            "man",
            "ru",
            "2022-07-04-58.3",
            "2a02:6b8:0:40c:8d16:83ae:346:5e59",
            0,
            "https://www.yandex.ru/",
            "1517775274.93215.22884.24370",
            "https://afisha.yandex.ru/kostroma/cinema/zomboiashchik?version=mobile&utm_source=yamain_touch&utm_medium" +
                "=yamain_afisha",
            UnsignedLong.valueOf("9933283421517423124"),
            UnsignedLong.valueOf("9933283421517423124"),
            0, 40513, new Integer[]{},
            "", "", "", "", "", "", 0, 0, 0, UnsignedLong.valueOf("10660544598162057530")
        );

        checker.check(
            "HTTP_REFERER=https://yandex" +
                ".ru/@@dtype=clck@@tld=ru@@installation_info=eyJnZW8iOiJtYW4iLCJ2ZXJ0aWNhbCI6Ik1PUkRBIiwi" +
                "Y3R5cGUiOiJwcm9kIn0=@@monitoring=1319@@vars=143=2048,1042=msie_11.0_trident_7.0_windows_10.0_0_0_0," +
                "1964=60728_63207_55057@@timefs=16036@@lid=v14@@sid=1517740239.79239.20952" +
                ".42531@@rnd=1517740257041@@at=0@@uah=1029732909@@keyno=0@@icookie=9189650161508349135@@url=https" +
                "://news.yandex.ru/yandsearch?cl4url=www.bfm" +
                ".ru/news/376658&lang=ru&from=main_portal&stid=8esSt5shZjAIyByBR7Pc&t=1517739885&lr=213&msid" +
                "=1517740239.79239.20952.42531&mlid=1517739885.glob_225.d8ee21f8@@1517740256@@90.154.67.203,90.154.67" +
                ".203@@9189650161508349135",
            new Date(1517740256000L),
            0, "click",
            "news.yandex.ru/yandsearch",
            "v14",
            "v14",
            "",
            "",
            "man",
            "ru",
            "",
            "",
            0,
            "https://yandex.ru/",
            "1517740239.79239.20952.42531",
            "https://news.yandex.ru/yandsearch?cl4url=www.bfm" +
                ".ru/news/376658&lang=ru&from=main_portal&stid=8esSt5shZjAIyByBR7Pc&t=1517739885&lr=213&msid" +
                "=1517740239.79239.20952.42531&mlid=1517739885.glob_225.d8ee21f8",
            UnsignedLong.valueOf("9189650161508349135"),
            UnsignedLong.valueOf("9189650161508349135"),
            0, 16036, new Integer[]{60728, 63207, 55057},
            "msie", "11.0", "trident", "7.0", "windows", "10.0", 0, 0, 0, UnsignedLong.valueOf("15130234353917965892")
        );
        checker.check(
            "HTTP_REFERER=https://yandex.ru/@@dtype=clck@@tld=ru@@antiadb=0@@installation_info" +
                "=eyJnZW8iOiJ2bGEiLCJ2ZXJ0aWNhbCI6Ik1PUkRBIiwiY3R5cGUiOiJwcm9kIn0=@@monitoring=1319@@vars=143=2048," +
                "1042=msie_11.0_trident_7.0_windows_10.0_0_0_0," +
                "1964=60728_63207_55057@@timefs=16036@@lid=v14@@sid=1517740239.79239.20952" +
                ".42531@@rnd=1517740257041@@at=0@@uah=1029732909@@keyno=0@@icookie=9189650161508349135@@url=https" +
                "://news.yandex.ru/yandsearch?cl4url=www.bfm" +
                ".ru/news/376658&lang=ru&from=main_portal&stid=8esSt5shZjAIyByBR7Pc&t=1517739885&lr=213&msid" +
                "=1517740239.79239.20952.42531&mlid=1517739885.glob_225.d8ee21f8@@1517740256@@90.154.67.203,90.154.67" +
                ".203@@9189650161508349135",
            new Date(1517740256000L),
            0, "click",
            "news.yandex.ru/yandsearch",
            "v14",
            "v14",
            "",
            "",
            "vla",
            "ru",
            "",
            "",
            0,
            "https://yandex.ru/",
            "1517740239.79239.20952.42531",
            "https://news.yandex.ru/yandsearch?cl4url=www.bfm" +
                ".ru/news/376658&lang=ru&from=main_portal&stid=8esSt5shZjAIyByBR7Pc&t=1517739885&lr=213&msid" +
                "=1517740239.79239.20952.42531&mlid=1517739885.glob_225.d8ee21f8",
            UnsignedLong.valueOf("9189650161508349135"),
            UnsignedLong.valueOf("9189650161508349135"),
            0, 16036, new Integer[]{60728, 63207, 55057},
            "msie", "11.0", "trident", "7.0", "windows", "10.0", 0, 0, 0, UnsignedLong.valueOf("15130234353917965892")
        );
        checker.check(
            "HTTP_REFERER=https://yandex.ru/@@dtype=clck@@tld=ru@@antiadb=1@@installation_info" +
                "=eyJnZW8iOiJ2bGEiLCJ2ZXJ0aWNhbCI6Ik1PUkRBIiwiY3R5cGUiOiJwcm9kIn0=@@monitoring=1319@@vars=143=2048," +
                "1042=msie_11.0_trident_7.0_windows_10.0_0_0_0," +
                "1964=60728_63207_55057@@timefs=16036@@lid=v14@@sid=1517740239.79239.20952" +
                ".42531@@rnd=1517740257041@@at=0@@uah=1029732909@@keyno=0@@icookie=9189650161508349135@@url=https" +
                "://news.yandex.ru/yandsearch?cl4url=www.bfm" +
                ".ru/news/376658&lang=ru&from=main_portal&stid=8esSt5shZjAIyByBR7Pc&t=1517739885&lr=213&msid" +
                "=1517740239.79239.20952.42531&mlid=1517739885.glob_225.d8ee21f8@@1517740256@@90.154.67.203,90.154.67" +
                ".203@@9189650161508349135",
            new Date(1517740256000L),
            0, "click",
            "news.yandex.ru/yandsearch",
            "v14",
            "v14",
            "",
            "",
            "vla",
            "ru",
            "",
            "",
            1,
            "https://yandex.ru/",
            "1517740239.79239.20952.42531",
            "https://news.yandex.ru/yandsearch?cl4url=www.bfm" +
                ".ru/news/376658&lang=ru&from=main_portal&stid=8esSt5shZjAIyByBR7Pc&t=1517739885&lr=213&msid" +
                "=1517740239.79239.20952.42531&mlid=1517739885.glob_225.d8ee21f8",
            UnsignedLong.valueOf("9189650161508349135"),
            UnsignedLong.valueOf("9189650161508349135"),
            0, 16036, new Integer[]{60728, 63207, 55057},
            "msie", "11.0", "trident", "7.0", "windows", "10.0", 0, 0, 0, UnsignedLong.valueOf("15130234353917965892")
        );
    }
}
