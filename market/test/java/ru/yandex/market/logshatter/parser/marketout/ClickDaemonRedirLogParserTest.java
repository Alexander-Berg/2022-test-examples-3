package ru.yandex.market.logshatter.parser.marketout;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 28/07/15
 */
public class ClickDaemonRedirLogParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new ClickDaemonRedirLogParser());

        checker.check(
            "HTTP_REFERER=https://market.yandex.ru/product/10547456/reviews@@dtype=cpa@@uid=713711314380920101" +
                "@@link_id=713711314380920101@@categid=490@@price=5090@@hyper_id=10547456@@hyper_cat_id=90560" +
                "@@nav_cat_id=56183@@pp=200@@fee=0.010@@show_time=1438092010@@show_block_id=73178121786411743541" +
                "@@reqid=a13820560278e7bbf1a5cdad26413f4f@@uuid=@@wprid=@@sub_request_id=0" +
                ".6317442785108893@@cpa=1@@test_bits=@@test_tag=@@test_buckets=15092,28,89;15181,53,21;15296,21," +
                "86@@position=1@@shop_id=4757@@pof=@@onstock=1@@geo_id=213@@vcluster_id=-1@@fuid=50ed580131004069" +
                ".LamOfu4r42gj9xMqPf2k2jtkvmX2MHAq-Vor5nBv-8qWfHYo1oGVxhCEKzrVQapuVJ7nCezpKkddowh50XJd2MTncIYsB552e" +
                "-raW6QIbap9vbARjkuly3yGjWzqNqcX@@ware_md5=Oy7chr3ooXKmthtXiAzT3w@@host=msh42g.market.yandex" +
                ".net@@hidden=0@@touch=0@@yandexuid=659583921371548295@@at=0@@uah=607863109@@keyno=1@@url=http" +
                "://doctorhead.ru/product/sony_walkman_nwz_w273sbi/?r1=yandext&r2=&_openstat" +
                "=bWFya2V0LnlhbmRleC5ydTtTb255IFdhbGttYW4gTldaLVcyNzNTIEJsYWNrIEdvbGQ7T3k3Y2hyM29vWEttdGh0WGlBelQzdzs" +
                "@@1438092250@@127.0.0.1,178.236.241.195, 95.108.139.112@@659583921371548295"
        );
    }
}
