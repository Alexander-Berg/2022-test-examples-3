package ru.yandex.home.logshatter.parser;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

/**
 * @author Evgeny Bogdanov <a href="mailto:evbogdanov@yandex-team.ru"></a>
 * @date 20/11/17
 */
public class HomeAccessLogParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new HomeAccessLogParser());
        checker.check(
            "tskv\tunixtime=1522363649\ttimestamp=2018-03-29 22:47:29\ttimezone=+0000\ttskv_format=access-log-morda-ext\tip=2a02:6b8:0:845::1:22\tx_forwarded_for=\tmethod=GET\thost=www-v6d0.wdevx.yandex.ru\trequest=/\trequestid=1522363648.97686.29520.227199\taccept_encoding=gzip, deflate, sdch, br\taccept_language=ru,en;q=0.9,uk;q=0.8,cy;q=0.7\tbkflags=desktop_priority1,desktop_priority2_rf100-28,desktop_priority2_rf30-7,desktop_priority2_rf5-1,facebook,kk2017,livejournal,multi_auth,splash_promo,stream_putin,tvonline_recommend,twitter,vkontakte,wrong_date_alert\tcontent_type=text/html;\tcharset=UTF-8\tcookies=_ym_uid=1504094798336582505; fuid01=56d021a40270d13e.BjciQINMiw5Mt2kwUa_DdtbeQOTf5Y5uQuUY21jQoh_peQ6oCA3gwvBYSDojix_x4fkBwO8opLg3xAghvLaYeIqiNSP7hz25jbbLyP36uCHs1OMU--SwkmUBw8pJiLX2; mda=0; yandexuid=9168472601456325673; L=W3sDW3hMbgJHRn1CUWlPSUdVfGx9bXhrPxMgKzo=.1514291253.13359.369721.68418d52965c872ccb38a6df5188013e; yandex_login=firej; my=YycCAAErAgKqxwA=; yandex_gid=213; i=M+pGCbPQ/4iAqHSNu0G5FW9V++EPlzS64hdGEoGRFr4F0DtDqV9eZEQzoQdNqbS2e16TaVLuMBM/E6mAb7okV6E9L2E=; _ym_isad=2; zm=m-white_bender.webp.css-https%3Awww_5xzZ--Lvsy_BTZ6oiFVYtQOmfz8%3Al; ys=def_bro.1#musicchrome.0-0-471#svt.1#wprid.1522363236527524-1746633991498297211059123-man1-1764#cst.enbl; yabs-frequency=/4/200m08YGkredSxfQ/QtroS0Wl8R61Sd08Bo5Gpb1m_2v7/; yp=1551268667.cld.2227208#1523112670.ysl.1#1538102809.szm.2:1280x800:1280x728#2145906000.yb.16_4_0_6108:1955450:1459282338:1522363237:3144:0#1527418071.ww.1#1527256976.cnps.9369887366:max#1525001048.csc.2#1524955238.shlos.1#1526350287.sz.1920x1080x1#1829649745.multib.1#1829651253.udn.cDrQldCy0LPQtdC90LjQuQ%3D%3D#1522605010.clh.1955454#1523953960.ygu.1#1522370818.gpauto.55_666538:37_618881:140:1:1522363618#1553862842.mfc.1\tcrypta_id=1803230000001001180\texp=www_yes,banner_horizontal,widgets,fuid_yes,clid_no,L_yes,loc_gpauto_good,adb_iframewrite,yndx_extracted_points_route,yndx_personal_rubric,adb_treernd,yndx_zen_inserts_tv_kinopoisk,yndx_yandex_internal_test,adb_rndrows,yndx_zen_cache,yndx_tablet_yabs_265881,yndx_adb,yndx_stream_stickers_tech,yndx_zen_lib_lazy_images,antiadb_desktop,send_beacon_browsers,yndx_stream_vod_episodes_lines,weather_map,adb_deepmain,adb_destroy3,yndx_personal_channels_entry,adb_origimgurl,send_beacon,yndx_zen_fast_device,yndx_personal_channels_online_5,adb_replace,login_yes,work_home_yes\tfuid_slot=356\tgeo_prec=1\tgeo_region=213\tgpauto_age=31\tgpauto_sys=desktop+yabr\thostname=v6-wdevx.haze.yandex.net\thttps=1\tm_content=big\tm_language=ru\tm_zone=ru\tpid=227199\tprotocol=HTTP/1.1\tsize=148770\tstatus=200\tsubreq_awaps=1 0 3864\tsubreq_bigb=1 0 8320\tsubreq_blackbox=1 0 2854\tsubreq_cacher=1 0 161\tsubreq_crypta=1 0 153\tsubreq_laas_region=1 0 602\tsubreq_money=1 0 543\tsubreq_personal_request_batch:0=1 0 1905\tsubreq_vod_personal=1 0 1746\tsubreq_weather_forecast_handle=1 0 2719\tsubreq_yabs=1 0 5703\tsubreq_zen_cache=1 0 74\ttemplate=v14w\ttiming=total=0.451\tpl=0.348\tjs=0.101\tua.browserengine=WebKit\tua.browserengineversion=537.36\tua.browsername=YandexBrowser\tua.browserversion=18.3.1.894\tua.osfamily=MacOS\tua.osversion=10.13.3\tua.ismobile=0\tuid=49622348\tuser_agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 YaBrowser/18.3.1.894 (beta) Yowser/2.5 Safari/537.36\tvhost=www-v6d0.wdevx.yandex.ru\twait=87.197\twidgets=1\twiha=1\twiha_db=48:49622348:\tyandex=1\tyandexuid=9168472601456325673\tyuid_days=764\tyuid_slot_salted=56",
            new Date(1522363649000L),
            "www-v6d0.wdevx.yandex.ru", "GET", "1522363648.97686.29520.227199",
            UnsignedLong.valueOf(9168472601456325673L), UnsignedLong.valueOf(0L),
            764, "v6-wdevx.haze.yandex.net", "+0000", "", "/",
            1, "ru", 227199, "", 200,
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 YaBrowser/18.3.1.894 (beta) Yowser/2.5 Safari/537.36",
            "www-v6d0.wdevx.yandex.ru", "total=0.451", 87.197f, 0.0f, "WebKit", "537.36", "YandexBrowser", "18.3.1.894", "MacOS",
            "10.13.3", 0, 0, 0, new Integer[]{}, new Integer[]{},
            new String[]{"www_yes", "banner_horizontal", "widgets", "fuid_yes", "clid_no", "L_yes", "loc_gpauto_good", "adb_iframewrite", "yndx_extracted_points_route", "yndx_personal_rubric", "adb_treernd", "yndx_zen_inserts_tv_kinopoisk", "yndx_yandex_internal_test", "adb_rndrows", "yndx_zen_cache", "yndx_tablet_yabs_265881", "yndx_adb", "yndx_stream_stickers_tech", "yndx_zen_lib_lazy_images", "antiadb_desktop", "send_beacon_browsers", "yndx_stream_vod_episodes_lines", "weather_map", "adb_deepmain", "adb_destroy3", "yndx_personal_channels_entry", "adb_origimgurl", "send_beacon", "yndx_zen_fast_device", "yndx_personal_channels_online_5", "adb_replace", "login_yes", "work_home_yes"},
            "firej", "", "", "", "", "213", "big"
        );

        checker.check(
            "tskv\tunixtime=1522616401\ttimestamp=2018-04-02 00:00:01\ttimezone=+0300\ttskv_format=access-log-morda-ext\tip=::ffff:213.21.33.13\tx_forwarded_for=::ffff:213.21.33.13\tmethod=GET\thost=www.yandex.ru\trequest=/adddata/?wauth=1....\trequestid=1522616400.54461.20951.9324\taccept_encoding=gzip, deflate, br\taccept_language=ru,en;q=0.9\tcontent_type=text/html; charset=UTF-8\tcookies=yandexuid=7675230971494075328; yandex_login=sir.yudin;\tenabled-test-buckets=71899,0,31;63207,0,17\texp=widgets,login_yes\texp_config_version=9211\tfuid_slot=208\tgeo_h=1\tgeo_h_age=425\tgeo_h_loc=55.734089, 37.588493, 140, 1522667295\tgeo_h_region=2\tgeo_prec=2\tgeo_region=2\tgpauto_age=413\tgpauto_sys=desktop+yabr\thostname=s5.wfront.yandex.net\thttps=1\ticookie=7675230971494075328\tm_content=big\tm_language=ru\tm_zone=ru\tpid=9324protocol=HTTP/1.1\treferer=https://www.yandex.ru/\tsize=1238\tskin=piter\tstatus=200\tsubreq_blackbox=1 0 2842\tsubreq_weather_forecast_handle=1 0 2742\ttemplate=v14w\ttest-bucket=54051,0,54;63207,0,17;71899,0,31;73589,0,85\ttiming=total=0.111 pl=0.091 js=0.017\tua.browserengine=WebKit\tua.browserengineversion=537.36\tua.browsername=YandexBrowser\tua.browserversion=18.1.1.839\tua.osfamily=Windows\tua.osversion=10.0\tua.ismobile=0\tuid=75104143\tuser_agent=Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 YaBrowser/18.1.1.839 Yowser/2.5 Safari/537.36\tvhost=www.yandex.ru\twait_avg=1.317\twidgets=1wiha=1\tyandexuid=7675230971494075328\tyuid_days=330\tyuid_slot_salted=614",
            new Date(1522616401000L),
            "www.yandex.ru", "GET", "1522616400.54461.20951.9324",
            UnsignedLong.valueOf(7675230971494075328L), UnsignedLong.valueOf(7675230971494075328L),
            330, "s5.wfront.yandex.net", "+0300", "::ffff:213.21.33.13", "/adddata/?wauth=1....",
            1, "ru", 0, "https://www.yandex.ru/", 200,
            "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 YaBrowser/18.1.1.839 Yowser/2.5 Safari/537.36",
            "www.yandex.ru", "total=0.111 pl=0.091 js=0.017", 0.0f, 1.317f, "WebKit", "537.36", "YandexBrowser", "18.1.1.839", "Windows",
            "10.0", 0, 0, 0, new Integer[]{54051, 63207, 71899, 73589}, new Integer[]{71899, 63207},
            new String[]{"widgets", "login_yes"}, "sir.yudin", "1", "425", "55.734089, 37.588493, 140, 1522667295",
            "2", "2", "big"
        );
    }
}
