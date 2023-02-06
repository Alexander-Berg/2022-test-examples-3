package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.models.Click;
import ru.yandex.market.crm.util.CrmStrings;

import static org.junit.Assert.assertEquals;

/**
 * @author apershukov
 */
public class ClickMapperTest {

    @Test
    public void testMapSingleClickRecord() {
        String line = "{\"rowid\":\"mOibvLlMJ4n42uB2Pm_Llw:215849728:0\",\"eventtime\":1526288791000," +
                "\"url\":\"https://irkutsk.nix.ru/autocatalog/ssd_silicon_power/SSD-120-Gb-SATA-6Gb-s-Silicon-Power-" +
                "Slim-S55-SP120GBSS3S55S25-25-TLC_156391.html?_openstat=bWFya2V0LnlhbmRleC5ydTtTU0Qg0LTQuNGB0LogU2ls" +
                "aWNvbiBQb3dlciBTbGltIFM1NSAxMjAg0JPQsSBTUDEyMEdCU1MzUzU1UzI1O3BuaGdCU2ZSVFYzTzZib0QtRU9IR1E7&fromma" +
                "rket=&ymclid=262887867159525446300002\",\"referer\":\"https://yandex.ru/search/?text=SiliconPower%2" +
                "0Slim%20S55&lr=63\",\"cookie\":\"3172680241490264114\",\"show_uid\":\"262887867159525446300002\"," +
                "\"categ_id\":74,\"pp\":405,\"price\":6,\"filter\":0,\"geo_id\":63,\"shop_id\":338424," +
                "\"block_id\":\"2628878671595254463\",\"pof\":1,\"state\":1,\"hyper_id\":9355272," +
                "\"hyper_cat_id\":91033," +
                "\"onstock\":true,\"bid\":6,\"autobroker_enabled\":true,\"ware_id\":\"\"," +
                "\"link_id\":\"262887867159525446300002\",\"ip_geo_id\":63,\"offer_price\":2959,\"test_tag\":\"\"," +
                "\"uah\":\"1819081277\",\"vcluster_id\":-1,\"ware_md5\":\"pnhgBSfRTV3O6boD-EOHGQ\",\"fuid\":\"\"," +
                "\"test_buckets\":\"76083,0,9;63208,0,74;69849,0,38;78264,0,86;27391,0,47;2619,0,20;57191,0,50;57072," +
                "0," +
                "87;60927,0,68;61924,0,23;61921,0,10;61919,0,52;61918,0,25;61915,0,55;61914,0,55;61909,0,57;71352,0," +
                "81;77046,0,2;62472,0,87;77851,0,69;78073,0,20;26587,0,71;45963,0,17;78038,0,16;3057,0,19;56261,0," +
                "98\"," +
                "\"cpa\":true,\"req_id\":\"2123376805720180514140626\"," +
                "\"wprid\":\"1526288786438126-12337680577203567068" +
                "75978-man1-6003\",\"user_type\":0,\"utm_source\":\"\",\"utm_medium\":\"\",\"utm_term\":\"\"," +
                "\"utm_campaign\":\"\",\"touch\":false,\"show_cookie\":\"3172680241490264114\",\"ip6\":\"::" +
                "ffff:109.194.17.185\",\"sbid\":0,\"sub_request_id\":\"\"," +
                "\"bs_block_id\":\"2123376805720180514140626\"," +
                "\"position\":2,\"show_time\":1526288786000,\"nav_cat_id\":55316,\"uuid\":\"undefined\"," +
                "\"best_deal\":" +
                "false,\"hostname\":\"man2-0159-man-market-prod-report--053-17050.gencfg-c.yandex.net\",\"cp_vnd\":0," +
                "\"cb_vnd\":0,\"vnd_id\":470008,\"dtsrc_id\":0,\"type_id\":0,\"is_price_from\":false," +
                "\"pof_raw\":\"1\"" +
                ",\"min_bid\":6,\"bid_type\":\"ybid\",\"feed_id\":\"430087\",\"offer_id\":\"156391\",\"url_type\":0," +
                "\"promo_type\":0,\"icookie\":\"\",\"rgb\":\"GREEN\",\"supplier_type\":\"\"}";

        List<Click> clicks = new ClickMapper().apply(CrmStrings.getBytes(line));

        assertEquals(1, clicks.size());

        Click expected = Click.newBuilder()
                .setUserIds(
                        UserIds.newBuilder()
                                .setYandexuid("3172680241490264114")
                )
                .setTimestamp(1526288791000L)
                .setOfferId("156391")
                .setFeedId(430087)
                .setWareMd5("pnhgBSfRTV3O6boD-EOHGQ")
                .setHid(91033)
                .setHyperId(9355272)
                .setShopId(338424)
                .build();


        assertEquals(expected, clicks.get(0));
    }

    @Test
    public void checkYuid() {
        String line = "{\"rowid\":\"qFgbmFXtNlmQgPkszlRQUQ:447904328:0\",\"eventtime\":1528231169000," +
                "\"url\":\"https://www.svyaznoy.ru/catalog/presents/7257/4051649?city_id=133&utm_medium=cpc" +
                "&utm_content=4051649&utm_campaign=pricelist&utm_source=yandexmarket&utm_term" +
                "=game_FIFA2018_11604Headshot&ymclid=282310890882964133800058\"," +
                "\"referer\":\"https://market.yandex.ru/search?text=%D0%BE%D1%84%D0%B8%D1%86%D0%B8%D0%B0%D0%BB%D1%8C" +
                "%D0%BD%D1%8B%D0%B9%20%D0%BC%D1%8F%D1%87%20%D1%87%D0%B5%D0%BC%D0%BF%D0%B8%D0%BE%D0%BD%D0%B0%D1%82%D0" +
                "%B0%20%D0%BC%D0%B8%D1%80%D0%B0%20%D0%BF%D0%BE%20%D1%84%D1%83%D1%82%D0%B1%D0%BE%D0%BB%D1%83%202018%20" +
                "%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C&clid=545&page=2\"," +
                "\"cookie\":\"\",\"show_uid\":\"282310890882964133800058\",\"categ_id\":28187,\"pp\":7,\"price\":11," +
                "\"filter\":0,\"geo_id\":213,\"shop_id\":3828,\"block_id\":\"2823108908829641338\",\"pof\":545," +
                "\"state\":1," +
                "\"hyper_id\":-1,\"hyper_cat_id\":14304975,\"onstock\":false,\"bid\":11,\"autobroker_enabled\":true," +
                "\"ware_id\":\"\",\"link_id\":\"282310890882964133800058\",\"ip_geo_id\":1,\"offer_price\":680," +
                "\"test_tag\":\"\",\"uah\":\"610901060\",\"vcluster_id\":-1,\"ware_md5\":\"8CvcKKgRWlaGvEfRTMK3Eg\"," +
                "\"fuid\":\"\",\"test_buckets\":\"77741,0,13;67036,0,11\",\"cpa\":false," +
                "\"req_id\":\"f06912a9442b50df25a014265692c979\",\"wprid\":\"\",\"user_type\":0,\"utm_source\":\"\"," +
                "\"utm_medium\":\"\",\"utm_term\":\"\",\"utm_campaign\":\"\",\"touch\":false," +
                "\"show_cookie\":\"5262491151528231088\",\"ip6\":\"::ffff:109.252.15.182\",\"sbid\":0," +
                "\"sub_request_id\":\"1\",\"bs_block_id\":\"\",\"position\":58,\"show_time\":1528231089000," +
                "\"nav_cat_id\":68309,\"uuid\":\"undefined\",\"best_deal\":false," +
                "\"hostname\":\"iva1-2283-iva-market-prod-report--88d-17050.gencfg-c.yandex.net\",\"cp_vnd\":0," +
                "\"cb_vnd\":0,\"vnd_id\":0,\"dtsrc_id\":0,\"type_id\":0,\"is_price_from\":false,\"clid\":0," +
                "\"distr_type\":0,\"pof_raw\":\"{\\\"clid\\\":[\\\"545\\\"],\\\"mclid\\\":null," +
                "\\\"distr_type\\\":null,\\\"vid\\\":null,\\\"opp\\\":null}\",\"min_bid\":3,\"bid_type\":\"mbid\"," +
                "\"feed_id\":\"\",\"offer_id\":\"4051649\",\"url_type\":0,\"promo_type\":0," +
                "\"icookie\":\"5262491151528231088\",\"rgb\":\"GREEN\",\"supplier_type\":\"\"}";

        List<Click> clicks = new ClickMapper().apply(CrmStrings.getBytes(line));

        Click model = clicks.get(0);
        Assert.assertEquals("5262491151528231088", model.getUserIds().getYandexuid());
    }
}
