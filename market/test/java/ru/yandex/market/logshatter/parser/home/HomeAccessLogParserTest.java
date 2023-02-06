package ru.yandex.market.logshatter.parser.home;

import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Evgeny Bogdanov <a href="mailto:evbogdanov@yandex-team.ru"></a>
 * @date 20/11/17
 */
public class HomeAccessLogParserTest {

    @Test
    @SuppressWarnings("MethodLength")
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new HomeAccessLogParser());
        checker.check(
            "tskv\tunixtime=1522363649\ttimestamp=2018-03-29 " +
                "22:47:29\ttimezone=+0000\ttskv_format=access-log-morda-ext\tip=2a02:6b8:0:845::1:22\tx_forwarded_for" +
                "=\tmethod=GET\thost=www-v6d0.wdevx.yandex.ru\trequest=/\trequestid=1522363648.97686.29520" +
                ".227199\taccept_encoding=gzip, deflate, sdch, br\ttcp_rtt=0.047540s\taccept_language=ru,en;q=0.9,uk;" +
                "q=0.8,cy;q=0.7\tbkflags=desktop_priority1,desktop_priority2_rf100-28,desktop_priority2_rf30-7," +
                "desktop_priority2_rf5-1,facebook,kk2017,livejournal,multi_auth,splash_promo,stream_putin," +
                "tvonline_recommend,twitter,vkontakte,wrong_date_alert\tcontent_type=text/html;" +
                "\tcharset=UTF-8\tcookies=_ym_uid=1504094798336582505; fuid01=56d021a40270d13e" +
                ".BjciQINMiw5Mt2kwUa_DdtbeQOTf5Y5uQuUY21jQoh_peQ6oCA3gwvBYSDojix_x4fkBwO8opLg3xAghvLaYeIqiNSP7hz25j" +
                "bbLyP36uCHs1OMU--SwkmUBw8pJiLX2; mda=0; yandexuid=9168472601456325673; L=W3sDW3hMbgJHRn1CUWlPSUdVf" +
                "Gx9bXhrPxMgKzo=.1514291253.13359.369721.68418d52965c872ccb38a6df5188013e; yandex_login=firej; my=" +
                "YycCAAErAgKqxwA=; yandex_gid=213; i=M+pGCbPQ/4iAqHSNu0G5FW9V++EPlzS64hdGEoGRFr4F0DtDqV9eZEQzoQdNqb" +
                "S2e16TaVLuMBM/E6mAb7okV6E9L2E=; _ym_isad=2; zm=m-white_bender.webp.css-https%3Awww_5xzZ--Lvsy_BTZ" +
                "6oiFVYtQOmfz8%3Al; ys=def_bro.1#musicchrome.0-0-471#svt.1#wprid.1522363236527524-17466339914982972" +
                "11059123-man1-1764#cst.enbl; yabs-frequency=/4/200m08YGkredSxfQ/QtroS0Wl8R61Sd08Bo5Gpb1m_2v7/; " +
                "yp=1551268667.cld.2227208#1523112670.ysl.1#1538102809.szm.2:1280x800:1280x728#2145906000.yb.16_4_" +
                "0_6108:1955450:1459282338:1522363237:3144:0#1527418071.ww.1#1527256976.cnps.9369887366:max#152500" +
                "1048.csc.2#1524955238.shlos.1#1526350287.sz.1920x1080x1#1829649745.multib.1#1829651253.udn.cDrQld" +
                "Cy0LPQtdC90LjQuQ%3D%3D#1522605010.clh.1955454#1523953960.ygu.1#1522370818.gpauto.55_666538:37_618" +
                "881:140:1:1522363618#1553862842.mfc.1\tcrypta_id=1803230000001001180\texp=www_yes,banner_horizont" +
                "al,widgets,fuid_yes,clid_no,L_yes,loc_gpauto_good,adb_iframewrite,yndx_extracted_points_route,yn" +
                "dx_personal_rubric,adb_treernd,yndx_zen_inserts_tv_kinopoisk,yndx_yandex_internal_test,adb_rndro" +
                "ws,yndx_zen_cache,yndx_tablet_yabs_265881,yndx_adb,yndx_stream_stickers_tech,yndx_zen_lib_lazy_i" +
                "mages,antiadb_desktop,send_beacon_browsers,yndx_stream_vod_episodes_lines,weather_map,adb_deepma" +
                "in,adb_destroy3,yndx_personal_channels_entry,adb_origimgurl,send_beacon,yndx_zen_fast_device,ynd" +
                "x_personal_channels_online_5,adb_replace,login_yes,work_home_yes\tfuid_slot=356\tgeo_prec=1\tgeo_" +
                "region=213\tgpauto_age=31\tgpauto_sys=desktop+yabr\thostname=v6-wdevx.haze.yandex.net\thttps=1\t" +
                "m_content=big\tm_language=ru\tm_zone=ru\tpid=227199\tprotocol=HTTP/1.1\tsize=148770\tstatus=200\t" +
                "subreq_awaps=1 0 3864\tsubreq_bigb=1 0 8320\tsubreq_blackbox=1 0 2854\tsubreq_cacher=1 0 161\tsub" +
                "req_crypta=1 0 153\tsubreq_laas_region=1 0 602\tsubreq_money=1 0 543\tsubreq_personal_request_bat" +
                "ch:0=1 0 1905\tsubreq_vod_personal=1 0 1746\tsubreq_weather_forecast_handle=1 0 2719\tsubreq_yabs" +
                "=1 0 5703\tsubreq_zen_cache=1 0 74\ttemplate=v14w\ttiming=total=0.451\tpl=0.348\tjs=0.101\tua.bro" +
                "wserengine=WebKit\tua.browserengineversion=537.36\tua.browsername=YandexBrowser\tua.browserversio" +
                "n=18.3.1.894\tua.osfamily=MacOS\tua.osversion=10.13.3\tua.ismobile=0\tuid=49622348\tuser_agent=Mo" +
                "zilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0." +
                "3282.186 YaBrowser/18.3.1.894 (beta) Yowser/2.5 Safari/537.36\tvhost=www-v6d0.wdevx.yandex.ru\twa" +
                "it=87.197\twidgets=1\twiha=1\twiha_db=48:49622348:\tyandex=1\tyandexuid=9168472601456325673\tyui" +
                "d_days=764\tyuid_slot_salted=56",
            new Date(1522363649000L),
            "www-v6d0.wdevx.yandex.ru", "GET", "1522363648.97686.29520.227199",
            UnsignedLong.valueOf(9168472601456325673L), UnsignedLong.valueOf(0L),
            764, "v6-wdevx.haze.yandex.net", "+0000", "", "/",
            1, "ru", 227199, "", 200,
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282" +
                ".186 YaBrowser/18.3.1.894 (beta) Yowser/2.5 Safari/537.36",
            "www-v6d0.wdevx.yandex.ru", "total=0.451", 87.197f, 0.0f, "WebKit", "537.36", "YandexBrowser", "18.3.1" +
                ".894", "MacOS",
            "10.13.3", 0, 0, 0, new Integer[]{}, new Integer[]{},
            new String[]{"www_yes", "banner_horizontal", "widgets", "fuid_yes", "clid_no", "L_yes",
                "loc_gpauto_good", "adb_iframewrite", "yndx_extracted_points_route", "yndx_personal_rubric",
                "adb_treernd", "yndx_zen_inserts_tv_kinopoisk", "yndx_yandex_internal_test", "adb_rndrows",
                "yndx_zen_cache", "yndx_tablet_yabs_265881", "yndx_adb", "yndx_stream_stickers_tech",
                "yndx_zen_lib_lazy_images", "antiadb_desktop", "send_beacon_browsers",
                "yndx_stream_vod_episodes_lines", "weather_map", "adb_deepmain", "adb_destroy3",
                "yndx_personal_channels_entry", "adb_origimgurl", "send_beacon", "yndx_zen_fast_device",
                "yndx_personal_channels_online_5", "adb_replace", "login_yes", "work_home_yes"},
            "firej", false, 0, "", 0, 213, "big", 47540, "2a02:6b8:0:845::1:22",
            UnsignedLong.valueOf(8248726775333818950L)
        );

        checker.check(
            "tskv\tunixtime=1522616401\ttimestamp=2018-04-02 " +
                "00:00:01\ttimezone=+0300\ttskv_format=access-log-morda-ext\tip=::ffff:213.21.33" +
                ".13\tx_forwarded_for=::ffff:213.21.33.13\tmethod=GET\thost=www.yandex.ru\trequest=/adddata/?wauth=1." +
                "...\trequestid=1522616400.54461.20951.9324\taccept_encoding=gzip, deflate, br\taccept_language=ru," +
                "en;q=0.9\tcontent_type=text/html; charset=UTF-8\tcookies=yandexuid=7675230971494075328; " +
                "yandex_login=;\tenabled-test-buckets=71899,0,31;63207,0,17\texp=widgets," +
                "login_yes\texp_config_version=9211\tfuid_slot=208\tgeo_h=1\tgeo_h_age=425\tgeo_h_loc=55.734089, 37" +
                ".588493, 140, 1522667295\tgeo_h_region=2\tgeo_prec=2\tgeo_region=2\tgpauto_age=413\tgpauto_sys" +
                "=desktop+yabr\thostname=s5.wfront.yandex" +
                ".net\thttps=1\ticookie=7675230971494075328\tm_content=big\tm_language=ru\tm_zone=ru\tpid" +
                "=9324protocol=HTTP/1.1\treferer=https://www.yandex" +
                ".ru/\tsize=1238\tskin=piter\tstatus=200\tsubreq_blackbox=1 0 2842\tsubreq_weather_forecast_handle=1 " +
                "0 2742\ttemplate=v14w\ttest-bucket=54051,0,54;63207,0,17;71899,0,31;73589,0,85\ttiming=total=0.111 " +
                "pl=0.091 js=0.017\tua.browserengine=WebKit\tua.browserengineversion=537.36\tua" +
                ".browsername=YandexBrowser\tua.browserversion=18.1.1.839\tua.osfamily=Windows\tua.osversion=10.0\tua" +
                ".ismobile=0\tuid=75104143\tuser_agent=Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like " +
                "Gecko) Chrome/63.0.3239.132 YaBrowser/18.1.1.839 Yowser/2.5 Safari/537.36\tvhost=www.yandex" +
                ".ru\twait_avg=1.317\twidgets=1wiha=1\tyandexuid=7675230971494075328\tyuid_days=330\tyuid_slot_salted" +
                "=614",
            new Date(1522616401000L),
            "www.yandex.ru", "GET", "1522616400.54461.20951.9324",
            UnsignedLong.valueOf(7675230971494075328L), UnsignedLong.valueOf(7675230971494075328L),
            330, "s5.wfront.yandex.net", "+0300", "::ffff:213.21.33.13", "/adddata/?wauth=1....",
            1, "ru", 0, "https://www.yandex.ru/", 200,
            "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 YaBrowser/18.1" +
                ".1.839 Yowser/2.5 Safari/537.36",
            "www.yandex.ru", "total=0.111 pl=0.091 js=0.017", 0.0f, 1.317f, "WebKit", "537.36", "YandexBrowser", "18" +
                ".1.1.839", "Windows",
            "10.0", 0, 0, 0, new Integer[]{54051, 63207, 71899, 73589}, new Integer[]{71899, 63207},
            new String[]{"widgets", "login_yes"}, "", true, 425, "55.734089, 37.588493, 140, 1522667295",
            2, 2, "big", 0, "::ffff:213.21.33.13", UnsignedLong.valueOf(2232862485853132742L)
        );

        checker.check(
            "tskv\tunixtime=1575924539\ttimestamp=2019-12-09 " +
                "23:48:59\ttimezone=+0300\ttskv_format=access-log-morda-ext\tip=::ffff:217.118.91" +
                ".65\tx_forwarded_for=::ffff:217.118.91.65\tmethod=POST\thost=yandex" +
                ".ru\trequest=/portal/api/yabrowser/2/weather,topnews,stocks," +
                "search?dialog_onboarding_shows_count=0&afisha_version=3&dp=3" +
                ".375&poiy=1213&size=971%2C1229&app_id=com.yandex.browser&app_version=1906083394&app_version_name=19" +
                ".10.1.100&app_build_number=100&app_platform=android&lib_version=18000000&lib_version_name=18" +
                ".0&lib_build_number=122&deviceid=cd24f3f245e9fd0429731dc5c241be6f&uuid" +
                "=7cb72dfc875284a338e5d5333a9719de&lat=56.838589&lon=60.546970&location_accuracy=484" +
                ".3554992675781&location_recency=302908&location_source=lbs_gsm&extended_location=false&model=SM" +
                "-G935F&os_version=8.0.0&manufacturer=samsung&lang=ru-RU&install_referrer=appmetrica_tracking_id" +
                "%3D601596767955408579%26ym_tracking_id%3D4407413185508280405&weather_card_exp=default&zen_extensions" +
                "=zen_games%3A\trequestid=1575924538.81608.140820.3066\tuser_agent=Mozilla/5.0 (Linux; arm_64; " +
                "Android 8.0.0; SM-G935F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.92 YaBrowser/19.10" +
                ".1.100.00 Mobile Safari/537.36\taccept_encoding=gzip\taccept_language=ru-RU,ru;q=0.9,en-US;q=0.8,en;" +
                "q=0.7\tcontent_type=text/javascript; charset=UTF-8\tcookies=fuid01=59c76cba09ab96a3" +
                ".I0GztUHrVQTTDABOEjRbh3pjl_drjODoc5OTfgbk0WdInGcsyAeYTap56Zu" +
                "-xOhWjwaQYC4Vt632prKk8dxWj7stRCZgPtQMLgCuoavMZTv2Ylla4XWguGcJfLxT6L3C; _ym_uid=1506241905281500727; " +
                "yandex_login=elena.solowewa; my=YwA=; rheftjdd=rheftjddVal; " +
                "ys_fp=form-requestid%3D1544551270309486-1770875719595098324300544-vla1-2547-TCH; font_loaded=YSv1; " +
                "tuid=a:c27d8d64a750b1d0b2350512f5ed0e38b87d39bcc8529000d993b6588814fd40; " +
                "_ym_uid=1506241905281500727; _ym_d=1564344845; " +
                "L=XnoHR2FUcW1bX357X2xWSnREUnpSS04DIDYsBzcfFxZVHCchRCo=.1566326420.13963.329547" +
                ".edd2784a8fc301e66b2b99d40074063e; mda=0; yuidss=7058621811503718794; yandexuid=7058621811503718794;" +
                " yandex_gid=54; ; Session_id=3:1575647356.5.0.1506241722976:V1t22Q:87.1|106490313.26386174.2" +
                ".2:60084698|209119.950766.XXXXXXXXXXXXXXXXXXXXXXXXXXX; sessionid2=3:1575647356.5.0" +
                ".1506241722976:V1t22Q:87.1|106490313.-1.2.2:294695|209119.620562.XXXXXXXXXXXXXXXXXXXXXXXXXXX; " +
                "_csrf=N6b2Iu_SbbnKtHfY8z3sMGuO; " +
                "i=Urod0rt6D3Loe4Z4UNlIgfXJhpQgZY+lVYmf0k3Fu9bBFOpktZAP7DTZMh4sVIzmt5eD9jBwqJXQRqFHBLd94MDM8X8=; " +
                "_ym_isad=2; cycada=QkDcx0rDVQBafFhAKxJb6FuNHWDd/LpheFZxw7apbqM=; ys=svt.1#uuid" +
                ".7cb72dfc875284a338e5d5333a9719de#wprid" +
                ".1575906389625186-376843409345243358300128-vla1-0244-TCH#mclid.2041723; " +
                "yabs-frequency=/4/103R0Ljixbt0ZkHT/7BuzRRGu9yE5Sd2qE600/; yp=2147483648.andrid" +
                ".4ebeea98ade587f8#2147483648.did.cd24f3f245e9fd0429731dc5c241be6f#1585290912.sz" +
                ".569x320x3_375#1590183820.szm.3_375:569x320:320x457#1881686420.udn" +
                ".cDplbGVuYS5zb2xvd2V3YQ%3D%3D#2147483648.ybrod.0#1886314807.yrtsi.1570954807#1886950726.sad" +
                ".1571590726:1571590726:1#1576059463.ygu.1#1576183436.clh.2041723#1575925980.gpauto" +
                ".56_838589:60_54697:484:2:1575925077; _ym_visorc_35260160=w; " +
                "_ym_visorc_249399=w\tcryptaid2=7621011276425722381\texp=send_beacon," +
                "app_android_webcard_assist\texp_config_version=15250\tfuid_slot=999geo_d=3" +
                ".073\tgeo_h=1\tgeo_h_age=-538\tgeo_h_loc=56.838589, 60.546970, 484, " +
                "1575925077\tgeo_h_region=54\tgeo_prec=1\tgeo_region=54\tgpauto_age=-539\tgpauto_sys=android+yabr" +
                "\theaders_size=11981\thostname=vla2-0300-c8c-vla-portal-morda-31387.gencfg-c.yandex" +
                ".net\thttps=1\ticookie=7058621811503718794\tlargest_header_size=4689\tm_content=touch\tm_language=ru" +
                "\tm_zone=ru\tpid=3066\tpost=162\tprotocol=HTTP/1" +
                ".1\trequest_ncuri=/portal/api/yabrowser/2/weather%2Ctopnews%2Cstocks%2Csearch" +
                "?dialog_onboarding_shows_count=0&afisha_version=3&dp=3.375&poiy=1213&size=971%2C1229&app_id=com" +
                ".yandex.browser&app_version=1906083394&app_version_name=19.10.1" +
                ".100&app_build_number=100&app_platform=android&lib_version=18000000&lib_version_name=18" +
                ".0&lib_build_number=122&deviceid=cd24f3f245e9fd0429731dc5c241be6f&uuid" +
                "=7cb72dfc875284a338e5d5333a9719de&lat=56.838589&lon=60.546970&location_accuracy=484" +
                ".3554992675781&location_recency=302908&location_source=lbs_gsm&extended_location=false&model=SM" +
                "-G935F&os_version=8.0.0&manufacturer=samsung&lang=ru-RU&install_referrer=appmetrica_tracking_id" +
                "%3D601596767955408579%26ym_tracking_id%3D4407413185508280405&weather_card_exp=default&zen_extensions" +
                "=zen_games%3A\tsize=123299\tstatus=200\tsubreq_bigb=1 0 18 \tsubreq_blackbox=1 0 3202 " +
                "\tsubreq_blender=1 0 2160 \tsubreq_cacher=1 0 69 \tsubreq_games_card_data=1 0 26183 " +
                "\tsubreq_laas_region=1 0 764 \tsubreq_money=1 0 513 \tsubreq_personal_request_batch:0=1 0 1281 " +
                "\tsubreq_personal_request_batch:1=1 0 48 \tsubreq_weather_deep=1 0 53509 " +
                "\tsubreq_weather_forecast_handle=1 0 3264 \tsubreq_weather_nowcast=1 0 383 " +
                "\ttemplate=api_yabrowser_2\ttest-bucket=193369,0,92;189742,0,43\ttiming=total=0.121\tua" +
                ".browserengine=WebKit\tua.browserengineversion=537.36\tua.browsername=YandexBrowser\tua" +
                ".browserversion=19.10.1.100.00\tua.osfamily=Android\tua.osversion=8.0.0\tua.ismobile=1\tua" +
                ".istablet=0\tuid=106490313\tuuid=7cb72dfc875284a338e5d5333a9719de\tvhost=touch.yandex.ru\twait=0" +
                ".646\twait_avg=0.541wiha=1\tyandexuid=7058621811503718794\tyuid_days=835\tyuid_slot_salted=953",
            new Date(1575924539000L),
            "yandex.ru", "POST", "1575924538.81608.140820.3066",
            UnsignedLong.valueOf(7058621811503718794L), UnsignedLong.valueOf(7058621811503718794L),
            835, "vla2-0300-c8c-vla-portal-morda-31387.gencfg-c.yandex.net", "+0300", "::ffff:217.118.91.65",
            "/portal/api/yabrowser/2/weather,topnews,stocks," +
                "search?dialog_onboarding_shows_count=0&afisha_version=3&dp=3" +
                ".375&poiy=1213&size=971%2C1229&app_id=com.yandex.browser&app_version=1906083394&app_version_name=19" +
                ".10.1.100&app_build_number=100&app_platform=android&lib_version=18000000&lib_version_name=18" +
                ".0&lib_build_number=122&deviceid=cd24f3f245e9fd0429731dc5c241be6f&uuid" +
                "=7cb72dfc875284a338e5d5333a9719de&lat=56.838589&lon=60.546970&location_accuracy=484" +
                ".3554992675781&location_recency=302908&location_source=lbs_gsm&extended_location=false&model=SM" +
                "-G935F&os_version=8.0.0&manufacturer=samsung&lang=ru-RU&install_referrer=appmetrica_tracking_id" +
                "%3D601596767955408579%26ym_tracking_id%3D4407413185508280405&weather_card_exp=default&zen_extensions" +
                "=zen_games%3A",
            1, "ru", 3066, "", 200,
            "Mozilla/5.0 (Linux; arm_64; Android 8.0.0; SM-G935F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0" +
                ".3865.92 YaBrowser/19.10.1.100.00 Mobile Safari/537.36",
            "touch.yandex.ru", "total=0.121", 0.646f, 0.0f, "WebKit", "537.36", "YandexBrowser", "19.10.1.100.00",
            "Android",
            "8.0.0", 1, 0, 0, new Integer[]{193369, 189742}, new Integer[]{},
            new String[]{"send_beacon", "app_android_webcard_assist"}, "elena.solowewa", true, 0, "56.838589, 60" +
                ".546970, 484, 1575925077",
            54, 54, "touch", 0, "::ffff:217.118.91.65", UnsignedLong.valueOf(1010272543848972610L)
        );
    }

    @Test
    public void parseRTT() {
        assertEquals(Integer.valueOf(155), HomeAccessLogParser.parseRTT("0.000155s"));
        assertEquals(Integer.valueOf(0), HomeAccessLogParser.parseRTT("0.000000155s"));
        assertEquals(Integer.valueOf(1), HomeAccessLogParser.parseRTT("0.000000955s"));
        assertEquals(Integer.valueOf(1234567), HomeAccessLogParser.parseRTT("1.234567s"));
        assertEquals(Integer.valueOf(987654321), HomeAccessLogParser.parseRTT("987.654321s"));
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), HomeAccessLogParser.parseRTT("10987.654321s"));
        assertEquals(Integer.valueOf(155), HomeAccessLogParser.parseRTT("0.000155s"));
        assertEquals(Integer.valueOf(155), HomeAccessLogParser.parseRTT("0.000155"));
        assertEquals(Integer.valueOf(0), HomeAccessLogParser.parseRTT("0.000155ms"));
        assertEquals(Integer.valueOf(0), HomeAccessLogParser.parseRTT(""));
    }
}
