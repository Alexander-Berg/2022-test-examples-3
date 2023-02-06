package ru.yandex.market.logshatter.parser.marketout;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 2019-06-05
 */
public class MarketShowsLogParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new MarketShowsRollbacksLogParser());
        checker.check(
            "{\"reqid\":\"1fefcc134a4fe3a1598625fe1e4d08dd\",\"category_id\":\"611\",\"normalized_by_dnorm_query\":\"комплектующие москитный сетка\",\"vbid\":\"0\",\"geo_id\":\"10732\",\"touch\":\"1\",\"title\":\"Сетка москитная на магнитах\",\"user_type\":\"0\",\"pickup_type\":\"None\",\"store_type\":\"None\",\"original_query\":\"комплектующие для москитных сеток\",\"shop_category\":\"Fulmar:Всё для Дачи\",\"state\":1,\"show_uid\":\"276394459027244855805008\",\"report_version\":\"2018.2.155.0\",\"query_context\":\"комплектующие для москитных сеток\",\"hostname\":\"sas2-2016-sas-market-prod-report--c42-17050.gencfg-c.yandex.net\",\"is_price_from\":\"0\",\"tariff\":\"0\",\"ware_md5\":\"Op1q_GLGnto_nxKpQZTjNg\",\"best_deal\":\"0\",\"shop_id\":\"339430\",\"price\":\"319\",\"sbid\":\"0\",\"filter_list\":[\"8\"],\"courier_type\":\"Country\",\"wprid\":\"1527639431493084-1600371715783517393952508-man1-4471-TCH\",\"icookie\":\"9967690271523022274\",\"yandex_uid\":\"9967690271523022274\",\"cpa_fraud_flags\":\"0\",\"nid\":\"54502\",\"iso_eventtime\":\"2018-05-30 03:17:25\",\"vendor_ds_id\":\"0\",\"clid\":\"0\",\"cooked_query\":\"(комплектующие::690 ^ комлектующие::316392846 ^ комплектущие::316392846 ^ комплектуюшие::316392846 ^ комплектующие::690 ^ комплектующии::316392846 ^ (компьютерные::2744 &&/(-32768 32768) комплектующие::690)) &&/(-3 5) (для::40 ^ for::3299) &&/(-3 5) ((москитных::316392846 ^ антимоскитные::316392846 ^ маскитная::316392846 ^ москитка::316392846 ^ москитов::316392846 ^ противомоскитные::316392846) &/(-64 64) (сеток::10910098 ^ сети::156940 ^ setka::316392846 ^ решетки::340207 ^ сетчатые::4519897 ^ сеточка::888743 ^ сеточный::316392846)) ^ (антимоскитные::316392846 &&/(-32768 32768) сетки::455241)\",\"pof\":\"708\",\"ip\":\"::ffff:46.188.123.118\",\"distr_type\":\"0\",\"vc_bid\":\"0\",\"delivery_type\":\"Priority\",\"super_uid\":\"276394459027244855800008\",\"click_type_id\":\"1\",\"ranked_with\":\"MNA_CommonThreshold_v2_251519_m10_x_287136_0\",\"__ypc__rowid\":\"10001053\",\"min_bid\":\"2\",\"downloadable_type\":\"None\",\"vendor_price\":\"0\",\"home_region\":\"225\",\"rowid\":\"market-search:1016196326:305\",\"show_block_id\":\"2763944590272448558\",\"bid\":\"20\",\"onstock\":\"0\",\"event_time\":\"1527639445\",\"test_buckets\":\"79918,0,44;50723,0,12;78474,0,40;46351,0,16;79090,0,21;79956,0,53;15093,0,65\",\"vendor_click_price\":\"0\",\"goods_count\":\"0\",\"url_type\":\"5\",\"ctr\":\"2256989\",\"autobroker_enabled\":\"1\",\"user_agent_hash\":\"517762881\",\"pp\":\"48\",\"cpa\":\"0\",\"normalized_by_synnorm_query\":\"гнус комплектующий сеть\",\"__ypc__source_id\":\"/20180530_3810-1527652017-53\",\"click_price\":\"10\",\"shop_name\":\"Fulmar\",\"url\":\"//m.market.yandex.ru/offer/Op1q_GLGnto_nxKpQZTjNg?call=1&hid=90690&nid=54502&shop_id=339430\",\"feed_id\":\"431438\",\"normalized_to_lower_query\":\"комплектующие москитных сеток\",\"index_generation\":\"20180529_2207\",\"subrequest_id\":\"1\",\"filter\":\"8\",\"normalized_to_lower_and_sorted_query\":\"комплектующие москитных сеток\",\"mn_ctr\":\"0.403018\",\"server\":\"m.market.yandex.ru\",\"record_type\":\"0\",\"position\":\"8\",\"ip_geo_id\":213}",
            new Date(1527639445000L),
            48, 708, 8, 1, 10732, 0, -1L,
            "", UnsignedLong.valueOf("9617459957249724040"), 611, 5
        );
    }

}