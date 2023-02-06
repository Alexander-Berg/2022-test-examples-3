package ru.yandex.market.logshatter.parser.home;

import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author Evgeny Bogdanov <a href="mailto:evbogdanov@yandex-team.ru"></a>
 * @date 20/11/17
 */
public class HomeBlockdisplayLogParserTest {

    @Test
    @SuppressWarnings("MethodLength")
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new HomeBlockdisplayLogParser());
        checker.check(
            "tskv\ttskv_format=blockstat-log-tskv\tcookies=yandexuid=6203574511514390439; " +
                "_ym_uid=1514390441810092825; mda=0; L=VlVZeG1qWl5aX15/fHFrVmhZbEF8CFFzMRk1ZV4hOBJXUVQIXg==" +
                ".1514390512.13360.342813.15a42625e77656b6da0fd73fa09e1b02; yandex_login=kot-misha2004; my=YwA=; " +
                "fuid01=5a469db73a16e331.RQEzEC1LyVvhAWev8ulScYBCIjuvudzygZvkqbJsxcs5xvFmlcnVy0FHNIdhcA" +
                "-gx4S62n2EqY9mSHvl7XJGeSge7dedcWtPfc8QZfDtRE_q7xatdI0ZjuUzSiXwwDxw; " +
                "i=3fLoa0nAgZvj3OZNiQlFphIWoTEAg3GxyTfejr00iHHHa/RvMrQT9UdFsG7m2ArwclAeCjvWPKmC6HYNeDZ1xyoFFU8=; " +
                "Session_id=3:1517164944.5.0.1514390512805:94UY1Q:1c.1|485963641.0.2|176507.987035" +
                ".c8qHP7OcuXtGcEDplJF1v035p80; sessionid2=3:1517164944.5.0.1514390512805:94UY1Q:1c.1|485963641.0" +
                ".2|176507.135456._TLUpvcQI7fGZAttNqTqPq3tSEE; yandex_gid=8; ys=mclid.2291173; _ym_isad=2; " +
                "zm=m-white_bender.webp.css-https-www%3Awww_UmrFV3emWpS2LdKUBytzFUOdM94%3Al; " +
                "yabs-frequency=/4/0m0100J0R5hqF69Q/2IvoS20k8QChxcqWBY2LIh1m8Ix00P5Ai70WBk2KIh1mooty____rpnoS24k8080" +
                "/; yp=1829750512.udn.cDprb3QtbWlzaGEyMDA0#1517680878.clh.2291173#1533018517.szm" +
                ".1_00:1600x900:1600x794#1518030832.ysl.1#1520100088.csc.1#1522612517.ww.1#1547142486.wzrd_sw" +
                ".1515606486#1547142486.dsws.3#1547142486.dswa.0#1547142486.dwsets.3#1519756975.shlos.1#1548700975" +
                ".p_sw.1517164974#1522175341.cnps.1164390364:max#1519842514.ygu" +
                ".1\tunixtime=1517426095\tyandexuid=6203574511514390439\tenabled-test-buckets=63208,0,87;63350,0," +
                "92\tuser_agent=Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/63.0.3239.132 Safari/537.36\tx_real_ip=::ffff:212.124.19" +
                ".168\ticookie=3fLoa0nAgZvj3OZNiQlFphIWoTEAg3GxyTfejr00iHHHa" +
                "/RvMrQT9UdFsG7m2ArwclAeCjvWPKmC6HYNeDZ1xyoFFU8=\ttest-bucket=34317,0,70;64563,0,82;65355,0,46;63350," +
                "0,92;63208,0,87;62646,0,4\trequest=/\tip=::ffff:212.124.19.168\treferer=https://www.yandex" +
                ".ru/\tvhost=www.yandex.ru\tmethod=GET\tyuid_slot_salted=533\texp_config_version=7905" +
                "\taccept_language=ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7\tshow_id=1517426095.30178.20947" +
                ".22830\tblocks=v14\\t12\\tgeo_type=6\\ttest-bucket=34317,0,70;64563,0,82;65355,0,46;63350,0,92;" +
                "63208,0,87;62646,0,4\\tgeo_ip=8\\tbanreq=1\\tskin=UNDEF\\texp=www_yes,plain,banner_horizontal," +
                "fuid_yes,clid_no,L_yes,loc_no,adb_iframewrite,VOD_personal_v14,adb_shadow2,antiadb_desktop," +
                "adb_destroy3,adb_deepmain,zen_spok,adb_origimgurl,5news_animation,adb_rndrows,notifications," +
                "send_beacon_browsers,personal_stream_promo_random_etalon,send_beacon,tvm_mailcounter," +
                "newnet_footer_kinopoisk,adb,weather_map,adb_treernd,adb_replace,login_no," +
                "work_home_no\\texp_config_version=7905\\tgeo=8\\tenabled-test-buckets=63208,0,87;63350,0," +
                "92\\tlogged=1\\tgeoprc=2\\tk=1\\tv14.afisha.links.link0\\t0\\tv14.afisha.links.link1\\t0\\tv14" +
                ".afisha.links.link2\\t0\\tv14.afisha.links.link3\\t0\\tv14.afisha.new\\t0\\tv14.afisha.title" +
                ".link\\t0\\tv14.banner.popup.close\\t0\\tv14.business.adv.link.adv_bottom\\t0\\tv14.business.adv" +
                ".title\\t0\\tv14.business.direct.link.maintest_ru_razmestitrekl\\t0\\tv14.business.direct" +
                ".title\\t0\\tv14.business.kassa.link.footer_kassa\\t0\\tv14.business.kassa.title\\t0\\tv14.business" +
                ".metrika.link.bottom\\t0\\tv14.business.metrika.title\\t0\\tv14.cache.l\\t0\\tv14.dist.popup.browser" +
                ".yabs.no\\t0\\tv14.dist.popup.browser.yabs.yes\\t0\\tv14.dist.searchlink.browser.link\\t0\\tv14.dist" +
                ".teaser.link.browser.atom.image\\t0\\tv14.dist.teaser.link.browser.atom.title\\t0\\tv14.example" +
                ".all\\t1\\tid=305396\\tv14.foot.about\\t0\\tv14.foot.blog\\t0\\tv14.foot.company\\t0\\tv14.foot" +
                ".help\\t0\\tv14.foot.support\\t0\\tv14.foot.up_arrow\\t0\\tv14.foot.vacancies\\t0\\tv14.head.region" +
                ".setup\\t0\\tv14.head.sethome.chrome_ext.link\\t0\\tv14.head.settings\\t0\\tv14.head.settings" +
                ".editmorda\\t0\\tv14.head.settings.other\\t0\\tv14.head.settings.region\\t0\\tv14.head.settings" +
                ".skincatalog\\t0\\tv14.keyboard.close\\t0\\tv14.keyboard.open\\t0\\tv14.logo.default\\t0\\tv14.mail" +
                ".login.usermenu.adduser\\t0\\tv14.mail.login.usermenu.exit\\t0\\tv14.mail.login.usermenu" +
                ".passport\\t0\\tv14.maps.links.bus.bus\\t0\\tv14.maps.links.rasp.rasp\\t0\\tv14.maps.links.taxi" +
                ".taxi_RU-KRS-KUR\\t0\\tv14.maps.links.traffic.traf\\t0\\tv14.maps.setup.region\\t0\\tv14.maps.title" +
                ".regular\\t0\\tv14.mediafooter.infinity_zen.title\\t0\\tv14.mediafooter.kinopoisk.film" +
                ".populyarnoe\\t0\\tv14.mediafooter.kinopoisk.tab.boeviki\\t0\\tv14.mediafooter.kinopoisk.tab" +
                ".detyam\\t0\\tv14.mediafooter.kinopoisk.tab.drami\\t0\\tv14.mediafooter.kinopoisk.tab" +
                ".komedii\\t0\\tv14.mediafooter.kinopoisk.tab.melodrami\\t0\\tv14.mediafooter.kinopoisk.tab" +
                ".populyarnoe\\t0\\tv14.mediafooter.kinopoisk.tab.sovetskoe\\t0\\tv14.mediafooter.kinopoisk" +
                ".title\\t0\\tv14.mediafooter.promo\\t0\\tv14.mediafooter.resize.collapse\\t0\\tv14.news.news.links" +
                ".1\\t0\\tv14.news.news.links.2\\t0\\tv14.news.news.links.3\\t0\\tv14.news.news.links.4\\t0\\tv14" +
                ".news.news.links.animation.1\\t0\\tv14.news.news.links.animation.2\\t0\\tv14.news.news.links" +
                ".animation.3\\t0\\tv14.news.news.links.animation.4\\t0\\tv14.news.news.links.animation.5\\t0\\tv14" +
                ".news.news.links.animation.6\\t0\\tv14.news.news.tabs.link\\t0\\tv14.news.news.tabs.select\\t0\\tv14" +
                ".news.region.tabs.link\\t0\\tv14.news.region.tabs.select\\t0\\tv14.news.time\\t0\\tv14" +
                ".news_rates_manual.id1\\t0\\tv14.news_rates_manual.id1006\\t0\\tv14.news_rates_manual.id23\\t0\\tv14" +
                ".news_rates_manual.more\\t0\\tv14.notifications.mail.login.disk\\t0\\tv14.notifications.mail.login" +
                ".inbox.unread\\t0\\tv14.notifications.mail.login.usermenu.toggle\\t0\\tv14.notifications.mail.login" +
                ".usermenu.toggle-icon\\t0\\tv14.notifications.mail.login.write\\t0\\tv14" +
                ".search\\t3\\tlogged=1\\tserp=1\\tgeo=8\\tv14.sites.autoru.subtitle\\t1\\tid=wrdwrd-16331_r3\\tv14" +
                ".sites.autoru.title\\t1\\tid=wrdwrd-16331_r3\\tv14.sites.collections.subtitle\\t1\\tid=svadba\\tv14" +
                ".sites.collections.title\\t1\\tid=svadba\\tv14.sites.header\\t0\\tv14.sites.health" +
                ".subtitle\\t1\\tid=onlineconsultation\\tv14.sites.health.title\\t1\\tid=onlineconsultation\\tv14" +
                ".sites.market.subtitle\\t1\\tid=desktop_smartphones_huawei_rus\\tv14.sites.market" +
                ".title\\t1\\tid=desktop_smartphones_huawei_rus\\tv14.sites.money_serv.subtitle\\t1\\tid=flat\\tv14" +
                ".sites.money_serv.title\\t1\\tid=flat\\tv14.sites.realty.subtitle\\t1\\tid=pik_9_10705\\tv14.sites" +
                ".realty.title\\t1\\tid=pik_9_10705\\tv14.sites.video.subtitle\\t1\\tid=sign_video_3_sovmult\\tv14" +
                ".sites.video.title\\t1\\tid=sign_video_3_sovmult\\tv14.soft_link\\t0\\tv14.tabs.images\\t0\\tv14" +
                ".tabs.maps\\t0\\tv14.tabs.market\\t0\\tv14.tabs.more\\t0\\tv14.tabs.music\\t0\\tv14.tabs" +
                ".news\\t0\\tv14.tabs.translate\\t0\\tv14.tabs.video\\t0\\tv14.tv.channel.0\\t0\\tv14.tv.channel" +
                ".1\\t0\\tv14.tv.channel.2\\t0\\tv14.tv.channel.3\\t0\\tv14.tv.channel.4\\t0\\tv14.tv.channel" +
                ".5\\t0\\tv14.tv.links.2\\t0\\tv14.tv.links.3\\t0\\tv14.tv.links.4\\t0\\tv14.tv.links.5\\t0\\tv14.tv" +
                ".stream.button\\t0\\tv14.tv.stream.items.0\\t0\\tv14.tv.stream.items.1\\t0\\tv14.tv.title" +
                ".link\\t0\\tv14.weather.grade\\t0\\tv14.weather.grade_later\\t0\\tv14.weather.grade_soon\\t0\\tv14" +
                ".weather.title\\t0\n",
            new Date(1517426095000L),
            "1517426095.30178.20947.22830",
            UnsignedLong.valueOf(6203574511514390439L),
            UnsignedLong.valueOf(0L),
            new Integer[]{34317, 64563, 65355, 63350, 63208, 62646},
            new Integer[]{63208, 63350},
            new String[]{"v14.dist.teaser.link.browser", "v14.logo.default", "v14.afisha.title.link", "v14.news.news" +
                ".links.animation", "v14.weather.title", "v14.dist.popup.browser.yabs", "v14.head.region.setup", "v14" +
                ".head.settings.other", "v14.tabs.maps", "v14.mail.login.usermenu.passport", "v14.news.news.tabs" +
                ".link", "v14.foot.company", "v14.mediafooter.infinity_zen.title", "v14.search", "v14.sites.header",
                "v14.sites.money_serv.subtitle", "v14", "v14.news_rates_manual.more", "v14.maps.links.bus.bus", "v14" +
                ".mediafooter.kinopoisk.tab.boeviki", "v14.business.kassa.link.footer_kassa", "v14.news_rates_manual" +
                ".id1006", "v14.cache.l", "v14.weather.grade_soon", "v14.notifications.mail.login.inbox", "v14" +
                ".business.metrika.title", "v14.tabs.images", "v14.tv.title.link", "v14.business.kassa.title", "v14" +
                ".maps.setup.region", "v14.foot.up_arrow", "v14.news.time", "v14.tv.channel.1", "v14.tv.channel.0",
                "v14.keyboard.open", "v14.sites.health.subtitle", "v14.head.settings", "v14.business.direct.link" +
                ".maintest_ru_razmestitrekl", "v14.mediafooter.kinopoisk.title", "v14.tabs.more", "v14.business" +
                ".metrika.link.bottom", "v14.weather.grade", "v14.business.direct.title", "v14.news.region.tabs" +
                ".select", "v14.tv.channel.5", "v14.foot.help", "v14.mediafooter.kinopoisk.tab.melodrami", "v14.tv" +
                ".channel.4", "v14.tv.channel.3", "v14.tv.channel.2", "v14.mail.login.usermenu.adduser", "v14.foot" +
                ".about", "v14.sites.video.subtitle", "v14.mediafooter.kinopoisk.film.populyarnoe", "v14.mail.login" +
                ".usermenu.exit", "v14.maps.links.traffic.traf", "v14.mediafooter.resize.collapse", "v14.news.region" +
                ".tabs.link", "v14.keyboard.close", "v14.maps.links.taxi.taxi_RU-KRS-KUR", "v14.tabs.video", "v14" +
                ".mediafooter.kinopoisk.tab.detyam", "v14.sites.market.subtitle", "v14.tv.stream.button", "v14" +
                ".example.all", "v14.sites.video.title", "v14.head.settings.region", "v14.mediafooter.kinopoisk.tab" +
                ".populyarnoe", "v14.foot.support", "v14.sites.realty.title", "v14.tabs.market", "v14.sites.autoru" +
                ".subtitle", "v14.tv.links.3", "v14.tv.links.2", "v14.tv.links.5", "v14.tv.links.4", "v14.business" +
                ".adv.title", "v14.mediafooter.kinopoisk.tab.komedii", "v14.banner.popup.close", "v14.business.adv" +
                ".link.adv_bottom", "v14.notifications.mail.login.write", "v14.sites.realty.subtitle", "v14.sites" +
                ".collections.subtitle", "v14.afisha.new", "v14.tabs.music", "v14.news.news.links.2", "v14" +
                ".mediafooter.kinopoisk.tab.drami", "v14.news.news.links.3", "v14.news.news.links.4", "v14.sites" +
                ".collections.title", "v14.head.sethome.chrome_ext.link", "v14.head.settings.editmorda", "v14.tv" +
                ".stream.items.1", "v14.news.news.tabs.select", "v14.tv.stream.items.0", "v14.weather.grade_later",
                "v14.news.news.links.1", "v14.notifications.mail.login.usermenu", "v14.mediafooter.kinopoisk.tab" +
                ".sovetskoe", "v14.maps.title.regular", "v14.afisha.links.link0", "v14.mediafooter.promo", "v14" +
                ".afisha.links.link1", "v14.afisha.links.link2", "v14.foot.vacancies", "v14.afisha.links.link3", "v14" +
                ".dist.searchlink.browser.link", "v14.tabs.news", "v14.maps.links.rasp.rasp", "v14.notifications.mail" +
                ".login.disk", "v14.soft_link", "v14.news_rates_manual.id23", "v14.foot.blog", "v14.news_rates_manual" +
                ".id1", "v14.sites.money_serv.title", "v14.head.settings.skincatalog", "v14.sites.health.title", "v14" +
                ".sites.market.title", "v14.tabs.translate", "v14.sites.autoru.title"},
            0,
            UnsignedLong.valueOf(446458184875060755L)
        );

        checker.check(
            "tskv\ttskv_format=blockstat-log-tskv\trequest=/\tmethod=GET\taccept_language=ru-RU,ru;q=0.8,en-US;q=0.6," +
                "en;q=0.4\tx_real_ip=::ffff:178.88.149" +
                ".231\tunixtime=1517825767\ticookie" +
                "=/HpzcCG9pkUKtXeLeF7sXZ9XIwkc0HxpPBwqPvdwOOdR6aPsEjS5CmExcmcst2tBa5td7aPgkduIEvezG61MGG/QX2Q" +
                "=\tshow_id=1517825766.74628.22896.12282\tip=::ffff:178.88.149" +
                ".231\tyandexuid=2804988441511197641\texp_config_version=7990\tcookies=yandexuid=2804988441511197641;" +
                " i=/HpzcCG9pkUKtXeLeF7sXZ9XIwkc0HxpPBwqPvdwOOdR6aPsEjS5CmExcmcst2tBa5td7aPgkduIEvezG61MGG/QX2Q=; " +
                "mda=0; fuid01=5a181eea00138885" +
                ".FTKeM-J7fLH06ymYEtUy3_ERB" +
                "-1vdXDpmHJdGtNn6zGe277kduxqSqgdTw_MjOhSTU_n87WAh7QBtQjEiZlZSrAgzhS9_b0ef7giGs4Pbc1p-8edoX6QT2ZDA" +
                "-kjpscr; _ym_uid=1511530220807401799; " +
                "yabs-frequency=/4/0G0002h8FLe00000/VawmS1Wk8VjDi70PBc01hqgmS1akG0EFJx1m62x0CqsmS1Wkm000/; my=YwA=; " +
                "yp=1518587832.ysl.1#1530720924.szm.1_00:1366x768:1366x647#1520143836.csc.2#1518054176.dq" +
                ".1\tuser_agent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61" +
                ".0.3163.114 Amigo/61.0.3163.114 MRCHROME SOC Safari/537.36\tvhost=yandex" +
                ".ru\tyuid_slot_salted=523\tblocks=redir\\t4\\texp=one16th," +
                "login_no\\tantiadb=0\\texp_config_version=7990\\tk=16\\tredir.force_https.big.ru\\t0",
            new Date(1517825767000L),
            "1517825766.74628.22896.12282",
            UnsignedLong.valueOf(2804988441511197641L),
            UnsignedLong.valueOf(0L),
            new Integer[]{},
            new Integer[]{},
            new String[]{"redir.force_https.big.ru", "redir"},
            0,
            UnsignedLong.valueOf(1324720564706481237L)
        );
        checker.check(
            "tskv\ttskv_format=blockstat-log-tskv\trequest=/\tmethod=GET\taccept_language=ru-RU,ru;q=0.8,en-US;q=0.6," +
                "en;q=0.4\tx_real_ip=::ffff:178.88.149" +
                ".231\tunixtime=1517825767\ticookie" +
                "=/HpzcCG9pkUKtXeLeF7sXZ9XIwkc0HxpPBwqPvdwOOdR6aPsEjS5CmExcmcst2tBa5td7aPgkduIEvezG61MGG/QX2Q" +
                "=\tshow_id=1517825766.74628.22896.12282\tip=::ffff:178.88.149" +
                ".231\tyandexuid=2804988441511197641\texp_config_version=7990\tcookies=yandexuid=2804988441511197641;" +
                " i=/HpzcCG9pkUKtXeLeF7sXZ9XIwkc0HxpPBwqPvdwOOdR6aPsEjS5CmExcmcst2tBa5td7aPgkduIEvezG61MGG/QX2Q=; " +
                "mda=0; fuid01=5a181eea00138885" +
                ".FTKeM-J7fLH06ymYEtUy3_ERB" +
                "-1vdXDpmHJdGtNn6zGe277kduxqSqgdTw_MjOhSTU_n87WAh7QBtQjEiZlZSrAgzhS9_b0ef7giGs4Pbc1p-8edoX6QT2ZDA" +
                "-kjpscr; _ym_uid=1511530220807401799; " +
                "yabs-frequency=/4/0G0002h8FLe00000/VawmS1Wk8VjDi70PBc01hqgmS1akG0EFJx1m62x0CqsmS1Wkm000/; my=YwA=; " +
                "yp=1518587832.ysl.1#1530720924.szm.1_00:1366x768:1366x647#1520143836.csc.2#1518054176.dq" +
                ".1\tuser_agent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61" +
                ".0.3163.114 Amigo/61.0.3163.114 MRCHROME SOC Safari/537.36\tvhost=yandex" +
                ".ru\tyuid_slot_salted=523\tblocks=redir\\t4\\tantiadb=1\\texp=one16th," +
                "login_no\\texp_config_version=7990\\tk=16\\tredir.force_https.big.ru\\t0",
            new Date(1517825767000L),
            "1517825766.74628.22896.12282",
            UnsignedLong.valueOf(2804988441511197641L),
            UnsignedLong.valueOf(0L),
            new Integer[]{},
            new Integer[]{},
            new String[]{"redir.force_https.big.ru", "redir"},
            1,
            UnsignedLong.valueOf(1324720564706481237L)
        );
    }
}
