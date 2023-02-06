package ru.yandex.market.logshatter.parser;

import java.util.Arrays;
import java.util.HashMap;

import com.google.common.primitives.UnsignedLong;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author zoom
 */
public class ParseUtilsTest extends Assertions {

    @Test
    public void shouldSplitPatternToLevelsCorrectlyWhenMethodIsNotDefined() {
        assertArrayEquals(new String[]{"root", "<all>"}, ParseUtils.splitPatternToLevels("root"));
        assertArrayEquals(new String[]{"root", "<all>"}, ParseUtils.splitPatternToLevels("/root"));
        assertArrayEquals(new String[]{"root", "<all>"}, ParseUtils.splitPatternToLevels("root/"));
        assertArrayEquals(new String[]{"root", "<all>"}, ParseUtils.splitPatternToLevels("/root/"));
    }

    @Test
    public void shouldSplitPatternToLevelsCorrectlyWhenMethodIsDefined() {
        assertArrayEquals(new String[]{"root", "GET"}, ParseUtils.splitPatternToLevels("GET:root"));
        assertArrayEquals(new String[]{"root", "POST"}, ParseUtils.splitPatternToLevels("POST:/root"));
        assertArrayEquals(new String[]{"root", "METHOD"}, ParseUtils.splitPatternToLevels("METHOD:root/"));
        assertArrayEquals(new String[]{"root", "HEAD"}, ParseUtils.splitPatternToLevels("HEAD:/root/"));
    }

    @Test
    public void testUrlParamStringExtraction() {
        assertEquals("module=Contacts&action=DetailView",
            ParseUtils.extractParamsSubstring("/index.php?module=Contacts&action=DetailView"));
        assertEquals("module=Contacts&action=DetailView",
            ParseUtils.extractParamsSubstring("/index.php#target?module=Contacts&action=DetailView"));
        assertEquals("module=Contacts",
            ParseUtils.extractParamsSubstring("/index.php?module=Contacts#target&action=DetailView"));
        assertEquals("module=Contacts&action=DetailView",
            ParseUtils.extractParamsSubstring("/index.php?module=Contacts&action=DetailView#target"));
    }

    @Test
    public void testSipHash() {
        assertEquals("2202906307356721367", ParseUtils.sipHash64("").toString());
        assertEquals("17179166182469397862", ParseUtils.sipHash64("yBIQkuh-QY_w1rH-YY5AtQ").toString());
        assertEquals("7469197971809657900", ParseUtils.sipHash64("zaOn1OchUnP7U9PF68rfMw").toString());
        assertEquals("2711373773322570989", ParseUtils.sipHash64("CnVJYkbescuVEx0iNRA8AA").toString());
        assertEquals("156869119347133483", ParseUtils.sipHash64("-1").toString());
    }

    @Test
    public void testStringParamExtraction() {
        assertEquals("2", ParseUtils.extractStringParam("/index.php?campaign_id=1&id=2&_user_id=3&euid=4", "id"));
        assertEquals("2", ParseUtils.extractStringParam("/index.php?id=2&campaign_id=1&_user_id=3&euid=4", "id"));
        assertEquals("3", ParseUtils.extractStringParam("/index.php?campaign_id=1&id=2&_user_id=3&euid=4", "_user_id"));
        assertEquals("4", ParseUtils.extractStringParam("/index.php?campaign_id=1&id=2&_user_id=3&euid=4", "euid"));

    }

    @Test
    public void testExtractPath() {
        assertEquals("", ParseUtils.cutQueryStringAndFragment(""));
        assertEquals("/", ParseUtils.cutQueryStringAndFragment("/"));
        assertEquals("/a", ParseUtils.cutQueryStringAndFragment("/a"));
        assertEquals("/a/b", ParseUtils.cutQueryStringAndFragment("/a/b"));
        assertEquals("/a/b", ParseUtils.cutQueryStringAndFragment("/a/b?c=d"));
    }

    @Test
    public void testIpv4ToLong() {
        assertEquals(0, ParseUtils.ipv4ToLong(""));
        assertEquals(0, ParseUtils.ipv4ToLong("not an ip string"));
        assertEquals(0, ParseUtils.ipv4ToLong("12.12.12"));
        assertEquals(0, ParseUtils.ipv4ToLong("266.0.0.0"));
        assertEquals(0, ParseUtils.ipv4ToLong("2a0c:5247:e17e:3bde:f26b:5e88:468d:371a"));
        assertEquals(0, ParseUtils.ipv4ToLong("::faff:1.0.0.0"));
        assertEquals(3232235777L, ParseUtils.ipv4ToLong("192.168.1.1"));
        assertEquals(4294967295L, ParseUtils.ipv4ToLong("255.255.255.255"));
        assertEquals(16843009L, ParseUtils.ipv4ToLong("1.1.1.1"));
        assertEquals(16777216L, ParseUtils.ipv4ToLong("1.0.0.0"));
        assertEquals(1531920552L, ParseUtils.ipv4ToLong("::ffff:91.79.64.168"));
    }

    @Test
    public void testIpToIpv6() {
        assertEquals("", ParseUtils.ipToipv6(""));
        assertEquals("", ParseUtils.ipToipv6("not an ip string"));
        assertEquals("", ParseUtils.ipToipv6("266.0.0.0"));
        assertEquals("2a0c:5247:e17e:3bde:f26b:5e88:468d:371a",
            ParseUtils.ipToipv6("2a0c:5247:e17e:3bde:f26b:5e88:468d:371a"));
        assertEquals("::faff:100:0", ParseUtils.ipToipv6("::faff:1.0.0.0"));
        assertEquals("::ffff:12.12.12", ParseUtils.ipToipv6("12.12.12"));
        assertEquals("::ffff:192.168.1.1", ParseUtils.ipToipv6("192.168.1.1"));
        assertEquals("::ffff:192.168.1.1", ParseUtils.ipToipv6("::ffff:192.168.1.1"));
        assertEquals("2a02:6b8:b080:7305::1:4", ParseUtils.ipToipv6("2a02:6b8:b080:7305::1:4"));
        assertEquals("2a02:6b8:b080:7305::1:4", ParseUtils.ipToipv6("2a02:6b8:b080:7305:0:0:1:4"));
        assertEquals("2a02:6b8:b080:7305::1:4", ParseUtils.ipToipv6("2a02:06b8:b080:7305:0000:0000:0001:0004"));
        assertEquals("::ae21:ad12", ParseUtils.ipToipv6("0000:0000:0000:0000:0000:0000:ae21:ad12"));
    }

    @Test
    public void testParseLong() {
        assertEquals(Long.valueOf(0), ParseUtils.parseLong("a", 0L));
        assertEquals(Long.valueOf(11111111), ParseUtils.parseLong("11111111", 0L));
        assertEquals(Long.valueOf(0), ParseUtils.parseLong("9863946471548151861", 0L));
    }

    @Test
    public void testParseUnsignedLong() {
        assertEquals(UnsignedLong.valueOf(0), ParseUtils.parseUnsignedLong("a", UnsignedLong.valueOf(0)));
        assertEquals(UnsignedLong.valueOf(0), ParseUtils.parseUnsignedLong("a"));
        assertEquals(UnsignedLong.valueOf(11111111), ParseUtils.parseUnsignedLong("11111111", UnsignedLong.valueOf(0)));
        assertEquals(UnsignedLong.valueOf("9863946471548151861"),
            ParseUtils.parseUnsignedLong("9863946471548151861", UnsignedLong.valueOf(0)));
    }

    @Test
    public void testParseBoolean() {
        assertEquals(Integer.valueOf(0), ParseUtils.parseBoolean("", 0));
        assertEquals(Integer.valueOf(0), ParseUtils.parseBoolean(null, 0));
        assertEquals(Integer.valueOf(0), ParseUtils.parseBoolean("0", 0));
        assertEquals(Integer.valueOf(0), ParseUtils.parseBoolean("false", 0));
        assertEquals(Integer.valueOf(0), ParseUtils.parseBoolean("FALSE", 0));
        assertEquals(Integer.valueOf(0), ParseUtils.parseBoolean(" ", 0));
        assertEquals(Integer.valueOf(0), ParseUtils.parseBoolean(" false", 0));
        assertEquals(Integer.valueOf(0), ParseUtils.parseBoolean(" 0", 0));
        assertEquals(Integer.valueOf(0), ParseUtils.parseBoolean("10", 0));
        assertEquals(Integer.valueOf(0), ParseUtils.parseBoolean("test", 0));

        assertEquals(Integer.valueOf(1), ParseUtils.parseBoolean("true", 0));
        assertEquals(Integer.valueOf(1), ParseUtils.parseBoolean("TRUE", 0));
        assertEquals(Integer.valueOf(1), ParseUtils.parseBoolean("1", 0));
        assertEquals(Integer.valueOf(1), ParseUtils.parseBoolean(" true", 0));
        assertEquals(Integer.valueOf(1), ParseUtils.parseBoolean(" 1", 0));

    }

    @Test
    public void testParseUidFromSessionIdCookie() {
        assertEquals("680700992", ParseUtils.parseUidFromSessionIdCookie(
            "3:1575638600.5.0.1575039371313:LkUrZA:1b.1|680700992.0.2|719237914.599229.2.2:599229|209115.740393.XXX"
        ));
        assertEquals("719237914", ParseUtils.parseUidFromSessionIdCookie(
            "3:1575638600.5.1.1575039371313:LkUrZA:1b.1|680700992.0.2|719237914.599229.2.2:599229|209115.740393.XXX"
        ));
        assertEquals("", ParseUtils.parseUidFromSessionIdCookie(
            "3:1575638600.5.2.1575039371313:LkUrZA:1b.1|680700992.0.2|719237914.599229.2.2:599229|209115.740393.XXX"
        ));
        assertEquals("", ParseUtils.parseUidFromSessionIdCookie(""));
    }

    @Test
    public void jsonObjectHasKey() {
        assertEquals(false, ParseUtils.jsonObjectHasKey(
            JsonParser.parseString("{\"additional\":null}").getAsJsonObject(), "additional"));
        assertEquals(false, ParseUtils.jsonObjectHasKey(
            JsonParser.parseString("{\"additional\":null}").getAsJsonObject(), "unknown"));
        assertEquals(true, ParseUtils.jsonObjectHasKey(
            JsonParser.parseString("{\"additional\":\"a\"}").getAsJsonObject(), "additional"));
    }

    @Test
    public void jsonObjectIsValuePrimitive() {
        assertEquals(false, ParseUtils.jsonObjectIsValuePrimitive(
            JsonParser.parseString("{\"additional\":null}").getAsJsonObject(), "additional"));
        assertEquals(false, ParseUtils.jsonObjectIsValuePrimitive(
            JsonParser.parseString("{\"additional\":null}").getAsJsonObject(), "unknown"));
        assertEquals(false, ParseUtils.jsonObjectIsValuePrimitive(
            JsonParser.parseString("{\"additional\":[1]}").getAsJsonObject(), "additional"));
        assertEquals(false, ParseUtils.jsonObjectIsValuePrimitive(
            JsonParser.parseString("{\"additional\":[\"1\"]}").getAsJsonObject(), "additional"));
        assertEquals(false, ParseUtils.jsonObjectIsValuePrimitive(
            JsonParser.parseString("{\"additional\":{\"1\": \"1\"}}").getAsJsonObject(), "additional"));
        assertEquals(true, ParseUtils.jsonObjectIsValuePrimitive(
            JsonParser.parseString("{\"additional\":false}").getAsJsonObject(), "additional"));
        assertEquals(true, ParseUtils.jsonObjectIsValuePrimitive(
            JsonParser.parseString("{\"additional\":\"a\"}").getAsJsonObject(), "additional"));
        assertEquals(true, ParseUtils.jsonObjectIsValuePrimitive(
            JsonParser.parseString("{\"additional\":1}").getAsJsonObject(), "additional"));
    }

    @Test
    public void testSplitToKeyValue() {
        assertEquals(new HashMap<String, String>() {{
            put("key1", "value1");
            put("key2", "2");
        }}, ParseUtils.splitToKeyValue("key1=value1\tkey2=2"));

        assertEquals(new HashMap<String, String>() {{
            put("key\\tone", "value\\none\\\\");
        }}, ParseUtils.splitToKeyValue("key\\tone=value\\none\\\\"));
    }

    @Test
    public void testSplitToKeyValueUnescape() {
        assertEquals(new HashMap<String, String>() {{
            put("key\tone", "value\none\\");
        }}, ParseUtils.splitToKeyValue("key\\tone=value\\none\\\\", true));
    }

    @Test
    void testSplitSimpleKeyValue() throws Exception {
        assertEquals(new HashMap<String, String>(), ParseUtils.splitSimpleKeyValue(""));

        assertEquals(new HashMap<String, String>() {{
            put("key_one", "value_one");
            put("key_two", "quoted value");
            put("key_three", "");
            put("key_four", "");
        }}, ParseUtils.splitSimpleKeyValue("key_one=value_one key_two=\"quoted value\" key_three=\"\"   key_four="));


        ParserException exception;
        exception = assertThrows(
            ParserException.class,
            () -> ParseUtils.splitSimpleKeyValue("asd fgh qwe")
        );
        assertEquals("Can't find key-value delimiter (=)", exception.getMessage());

        exception = assertThrows(
            ParserException.class,
            () -> ParseUtils.splitSimpleKeyValue("pew=pew no_closing_quote=\"rip")
        );
        assertEquals("Can't find closing quote for key no_closing_quote", exception.getMessage());
    }

    @Test
    public void parseDomainFroUrl() {
        assertEquals("www.yandex.ru", ParseUtils.parseDomainForUrl("https://www.yandex.ru/chrome/newtab"));
        assertEquals("2950.pr.messenger.test.yandex-team.ru", ParseUtils.parseDomainForUrl("https://2950.pr.messenger" +
            ".test.yandex-team.ru/404"));
        assertEquals("news.stable.priemka.yandex.ru", ParseUtils.parseDomainForUrl("https://news.stable.priemka" +
            ".yandex.ru/yandsearch?cl4url=3d576a43465c69f4acd5347a231afee7&title" +
            "=Glava_Minzdrava_poprosila_francuzskij_Falcon_vmesto_SSJ-100&lr=213&lang=ru&stid=5gGz7CvLdjxmIjvDn1I0" +
            "&persistent_id=67709826&rubric=index&from=index&flags=&flags=yxnews_nerpa_story__extended=1"));
        assertEquals("videotemplates.hamster.yandex.ru", ParseUtils.parseDomainForUrl("https://videotemplates.hamster" +
            ".yandex.ru/video/search?request=\"block\""));
        assertEquals("yandex.by", ParseUtils.parseDomainForUrl("https://yandex.by/video/touch/search?service=video" +
            ".yandex&ui=webmobileapp.yandex&appsearch_header=1&app_version=8000502&app_id=ru.yandex" +
            ".searchplugin&clid=2218567&text=битва за " +
            "севастополь&uuid=b8a578887bfb4e6f91cde082bd31aaa7&app_req_id=1566933596208-7-3804c0e9-68f7-4c81-a709" +
            "-70ee1a891f94-LMETA&meta_req_id=1566933596208-7-3804c0e9-68f7-4c81-a709-70ee1a891f94-LMETA"));
        assertEquals("market.yandex.ru", ParseUtils.parseDomainForUrl("https://market.yandex" +
            ".ru/product--smartfon-xiaomi-redmi-note-7-3/368058323?yclid=4985295114814132172&clid=602&utm_source" +
            "=yandex&utm_medium=net&utm_campaign=ym_des2_smart_net_rus&utm_content=cid:43042747|gid:3809762475|aid" +
            ":7457168568|ph:511446|pt:none|pn:1|src:svk-native.ru|st:context|cgcid:2003535&utm_term=svk-native.ru"));
        assertEquals("afisha.yandex.ru", ParseUtils.parseDomainForUrl("https://afisha.yandex" +
            ".ru/rostov-na-donu?marketing=cpc_g.11&utm_medium=search&utm_source=google&utm_campaign=General_rnd_2" +
            "|917588573&utm_term=%D0%B0%D1%84%D0%B8%D1%88%D0%B0%20%D1%80%D0%BE%D1%81%D1%82%D0%BE%D0%B2&utm_content" +
            "=INTid|kwd-298550497321|cid|917588573|aid|217935968310|gid|45639989733|pos|1t1|src|g_|dvc|m|reg|1012013" +
            "|rin||&INTid=45639989733|kwd-298550497321&gclid=EAIaIQobChMIgs3pvOOk5AIVAaaaCh3YvgaREAAYASAAEgI0dPD_BwE"));
        assertEquals("granny-dev-rr-templates.hamster.yandex.ru", ParseUtils.parseDomainForUrl("https://granny-dev-rr" +
            "-templates.hamster.yandex.ru/health/turbo/articles\\\\%d?exp_flags=serp3_granny=1&no-tests=1&test-mode=1" +
            "&"));
        assertEquals("avatars.mds.yandavatars.et-zen_doc", ParseUtils.parseDomainForUrl("https://avatars.mds" +
            ".yandavatars.et-zen_doc/1779163/-6396oc/177916870471/smart_crop_540x405"));
        assertEquals("www.yandex.ru", ParseUtils.parseDomainForUrl("//www.yandex.ru/chrome/newtab"));
        assertEquals("www.yandex.ru", ParseUtils.parseDomainForUrl("www.yandex.ru/chrome/newtab"));
        assertEquals("www.yandex.ru", ParseUtils.parseDomainForUrl("www.yandex.ru"));
        assertEquals("yandex.ru", ParseUtils.parseDomainForUrl("yandex.ru:80"));
        assertEquals("yandex.ru", ParseUtils.parseDomainForUrl("yandex.ru:80/aaa"));
        assertEquals("yandex.ru", ParseUtils.parseDomainForUrl("yandex.ru"));
        assertEquals("yandex.ru", ParseUtils.parseDomainForUrl("yandex.ru?a"));
        assertEquals("yandex.ru", ParseUtils.parseDomainForUrl("yandex.ru#a"));
        assertEquals("yandex.ru", ParseUtils.parseDomainForUrl("yandex.ru&a"));
        assertEquals("yandex.ru", ParseUtils.parseDomainForUrl("yandex.ru:80"));
        assertEquals("self", ParseUtils.parseDomainForUrl("self"));
        assertEquals("", ParseUtils.parseDomainForUrl("/yandex.ru"));
        assertEquals("yandex.ru", ParseUtils.parseDomainForUrl("//yandex.ru"));
    }

    @Test
    public void parseSchemeFromUrl() {
        assertEquals("https", ParseUtils.parseSchemeFromUrl("https://avatars.mds.yandavatars" +
            ".et-zen_doc/1779163/-6396oc/177916870471/smart_crop_540x405"));
        assertEquals("https", ParseUtils.parseSchemeFromUrl("https://yandex.ru/video"));
        assertEquals("https", ParseUtils.parseSchemeFromUrl("https://yandex.ru"));
        assertEquals("http", ParseUtils.parseSchemeFromUrl("http://gj.track.uc.cn/collect?uc_param_str=cpfrveladnkt" +
            "&appid=4e54ac8a118f&lt=event&e_c=bottom_ad&e_a=index&e_n=no_match&conds={%22isNotBlackDomain%22:false}"));
        assertEquals("http", ParseUtils.parseSchemeFromUrl("http://gj.track.uc.cn/"));
        assertEquals("http", ParseUtils.parseSchemeFromUrl("http://127.0.0.1:29009"));
        assertEquals("https", ParseUtils.parseSchemeFromUrl("https://yandex.ru/clck/jclck/dtype=stred/pid=0/cid=2873" +
            "/lr=58/path=morda_ru_touch.tpah.p0.nah_not_shown" +
            ".button_by_mouse/user_input=%D0%B3%D0%B5%D1%80%D0%B1%20%D1%81%D0%B0/text=%D0%B3%D0%B5%D1%80%D0%B1%20%D1" +
            "%81%D0%B0%D0%BB%D0%B5%D1%85%D0%B0%D1%80%D0%B4%D0%B0%20/times=497.132.134.133.242.179.195.192" +
            ".246/render_times=5.2.1.1.1.1.2.2.1/pos=15/ratio=7.15" +
            ".8/since_first_change=8498/since_last_change=3300/total_input_time=12525/session=1559289174784/rqs=9/rsp" +
            "=9/rndr=9/ersp=0/clks=1/cchd=0/suggest_reqid=193949529153933280391521625773588/tpah_log=[[add,p0,0]," +
            "[add,p0,271],[add,p0,1967],[add,p0,2674],[add,p0,2975],[add,p0,3308],[add,p0,3532],[word,p1,5199]," +
            "[submit,p0,8497]]/timefs=14266/lid=geotouch.searchgo/sid=1559289351.65208.140294" +
            ".180609/*data=url%3Dhttp%3A%2F%2Fya.ru/?_=1559289165552"));
        assertEquals("https", ParseUtils.parseSchemeFromUrl("https://jsl.infostatsvc.com/?InitSuccess_8=1164|,|ru|," +
            "|sizlsearch|,|dd326d5e-2c19-4840-8c3a-605b457f6c98|,|0|,|1002P|,|Chrome|,|Browser|,|1409744693627|," +
            "|adssRU|,|yandex.ru"));
        assertEquals("https", ParseUtils.parseSchemeFromUrl("https://moneyviking-a.akamaihd" +
            ".net/stats/?InitSuccess_8=62|,|RU|,|Money%20Viking|,|2107cdf5-2b92-4c20-baad-d3b23363e7f1|,|0|,|6YOM6|," +
            "|Chrome|,|Browser|,|1451435789315|,|tr1ru|,|yandex.ru"));
        assertEquals("chromenull", ParseUtils.parseSchemeFromUrl("chromenull://"));
        assertEquals("chromenull", ParseUtils.parseSchemeFromUrl("chromenull://sss"));
        assertEquals("mbexec", ParseUtils.parseSchemeFromUrl("mbexec://$(window_id)"));
        assertEquals("mxaddon-pkg", ParseUtils.parseSchemeFromUrl("mxaddon-pkg://{3c3b2f49-d929-4438-89e5" +
            "-b8df89edc828}"));
        assertEquals("", ParseUtils.parseSchemeFromUrl("self"));
        assertEquals("", ParseUtils.parseSchemeFromUrl("https:/yandex.ru"));
        assertEquals("", ParseUtils.parseSchemeFromUrl("/yandex.ru"));
        assertEquals("", ParseUtils.parseSchemeFromUrl("//yandex.ru"));
    }

    @Test
    public void parseExpFlags() {
        assertEquals(Arrays.asList(), ParseUtils.parseExpFlags(""));
        assertEquals(Arrays.asList(), ParseUtils.parseExpFlags(" "));
        assertEquals(Arrays.asList(), ParseUtils.parseExpFlags(" ; "));
        assertEquals(Arrays.asList(), ParseUtils.parseExpFlags(";"));
        assertEquals(Arrays.asList("aaa=1"), ParseUtils.parseExpFlags("aaa=1"));
        assertEquals(Arrays.asList("aaa=1", "bb=7", "ccc=yes", "ddd"), ParseUtils.parseExpFlags("aaa=1;bb=7;ccc=yes;" +
            "ddd"));
    }

    @Test
    public void parseTestBuckets() {
        assertArrayEquals(new Integer[0], ParseUtils.parseTestBuckets(""));
        assertArrayEquals(new Integer[0], ParseUtils.parseTestBuckets("-"));
        assertArrayEquals(new Integer[0], ParseUtils.parseTestBuckets("aaa"));
        assertArrayEquals(new Integer[]{0}, ParseUtils.parseTestBuckets("0"));
        assertArrayEquals(new Integer[]{1}, ParseUtils.parseTestBuckets("1,0,79"));
        assertArrayEquals(new Integer[]{19999}, ParseUtils.parseTestBuckets("19999,0,79;"));
        assertArrayEquals(new Integer[]{105520}, ParseUtils.parseTestBuckets("105520"));
        assertArrayEquals(new Integer[]{10552330, 103926}, ParseUtils.parseTestBuckets("10552330,0,79;103926,0,34"));
        assertArrayEquals(new Integer[]{75889, 351003, 345206, 348268, 348602, 348371}, ParseUtils.parseTestBuckets(
            "75889,75889,0;351003,351003,0;345206,345206,0;348268,348268,0;348602,348602,0;348371,348371,0"));
    }

    @Test
    public void parseDcfromHost() {
        assertEquals("SAS", ParseUtils.parseDcFromHost("bvkgjopewv64tusb.sas.yp-c.yandex.net"));
        assertEquals("", ParseUtils.parseDcFromHost("yp-c"));
        assertEquals("VLA", ParseUtils.parseDcFromHost(
            "vla-1361-bef-vla-market-prod--88b-16641.gencfg-c.yandex.net"));
        assertEquals("", ParseUtils.parseDcFromHost("2c"));
    }
}
