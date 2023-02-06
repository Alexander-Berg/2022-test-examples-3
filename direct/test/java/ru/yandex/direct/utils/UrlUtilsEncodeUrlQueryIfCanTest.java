package ru.yandex.direct.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.UrlUtils.encodeUrlQueryIfCan;

@RunWith(Parameterized.class)
public class UrlUtilsEncodeUrlQueryIfCanTest {

    @Parameterized.Parameters
    public static String[][] parameters() {
        return new String[][]{
                {
                        "https://play.google.com/store/apps/details?id=com.rovio.abcasual",
                        "https://play.google.com/store/apps/details?id=com.rovio.abcasual"
                },
                {
                        "https://umarket.feed/someting/else/123.xml",
                        "https://umarket.feed/someting/else/123.xml"
                },
                {
                        "https://monitor.highstreet.io/api/output/download/11267832254094639247/###",
                        "https://monitor.highstreet.io/api/output/download/11267832254094639247/###"
                },
                {
                        "https://omsk.vostok.ru/uploads/omsk/export/products2" +
                                ".xml?utm_source=yandex&utm_medium=cpc&utm_campaign=smart-banner&utm_term" +
                                "={keyword}||{phrase_id}&utm_content={source}||{ad_id}&calltouch_tm=yd_c" +
                                ":{campaign_id}_gb:{gbid}_ad:{ad_id}_ph:{phrase_id}_st:{source_type}_pt" +
                                ":{position_type}_p:{position}_s:{source}_dt:{device_type}_reg:{region_id}_ret" +
                                ":{retargeting_id}_apt:{addphrasestext}#123213",
                        "https://omsk.vostok.ru/uploads/omsk/export/products2" +
                                ".xml?utm_source=yandex&utm_medium=cpc&utm_campaign=smart" +
                                "-banner&utm_term=%7Bkeyword%7D%7C%7C%7Bphrase_id%7D" +
                                "&utm_content=%7Bsource%7D%7C%7C%7Bad_id%7D&calltouch_tm" +
                                "=yd_c%3A%7Bcampaign_id%7D_gb%3A%7Bgbid%7D_ad%3A%7Bad_id" +
                                "%7D_ph%3A%7Bphrase_id%7D_st%3A%7Bsource_type%7D_pt%3A" +
                                "%7Bposition_type%7D_p%3A%7Bposition%7D_s%3A%7Bsource%7D_dt%3A" +
                                "%7Bdevice_type%7D_reg%3A%7Bregion_id%7D_ret%3A%7Bretargeting_id" +
                                "%7D_apt%3A%7Baddphrasestext%7D#123213"
                },
                {
                        "http://feed.tools.domaun.yandex/o.cgi?source=mvideo2_moscow&set_utm_source=rtg_yandex" +
                                "&set_utm_medium=cpc&set_utm_content=[id]&set_utm_source=rtg_yandex" +
                                "&set_utm_campaign=DynRmkt_moscow_{%20utm_campaign%20}_mgcom_&set_utm_term" +
                                "=[utm_term]&where_price=%3C3000000|",
                        "http://feed.tools.domaun.yandex/o.cgi?source=mvideo2_moscow&set_utm_source=rtg_yandex" +
                                "&set_utm_medium=cpc&set_utm_content=%5Bid%5D&set_utm_source=rtg_yandex" +
                                "&set_utm_campaign=DynRmkt_moscow_%7B%2520utm_campaign%2520%7D_mgcom_" +
                                "&set_utm_term=%5Butm_term%5D&where_price=%253C3000000%7C"
                },
                {
                        "https://app.adjust.com/tuyq67w?campaign=" +
                                "69992094_Yamarket_UAC_RSYA_Android_General_CPA_RU_MSK_SPE_20220114&" +
                                "adgroup={GBID}&creative=11647300800&idfa={IDFA_UC}&campaign_id=69992094&" +
                                "creative_id=11647300800&cost_amount=&cost_type=&cost_currency=RUB&" +
                                "publisher_id={GBID}&deeplink=https%3A%2F%2Fmarket.yandex.ru%2Fdeals&" +
                                "redirect=https://play.google.com/store/apps/details?id=ru.beru.android&" +
                                "gps_adid={GOOGLE_AID_LC}&oaid={OAID_LC}&ya_click_id={TRACKID}",
                        "https://app.adjust.com/tuyq67w?campaign=" +
                                "69992094_Yamarket_UAC_RSYA_Android_General_CPA_RU_MSK_SPE_20220114&" +
                                "adgroup=%7BGBID%7D&creative=11647300800&idfa=%7BIDFA_UC%7D&campaign_id=69992094&" +
                                "creative_id=11647300800&cost_amount=&cost_type=&cost_currency=RUB&" +
                                "publisher_id=%7BGBID%7D&deeplink=https%253A%252F%252Fmarket.yandex.ru%252Fdeals&" +
                                "redirect=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3F" +
                                "id%3Dru.beru.android&gps_adid=%7BGOOGLE_AID_LC%7D&oaid=%7BOAID_LC%7D&" +
                                "ya_click_id=%7BTRACKID%7D"
                },
                {
                        "https://apps.apple.com/app/id1327721379?mt=8&pt=118743718&ct=Yandex Direct",
                        "https://apps.apple.com/app/id1327721379?mt=8&pt=118743718&ct=Yandex+Direct"
                }
        };
    }

    @Parameterized.Parameter(0)
    public String input;

    @Parameterized.Parameter(1)
    public String expected;

    @Test
    public void encodeUrlQueryIfCanTest() {
        assertThat(encodeUrlQueryIfCan(input)).isEqualTo(expected);
    }

}
