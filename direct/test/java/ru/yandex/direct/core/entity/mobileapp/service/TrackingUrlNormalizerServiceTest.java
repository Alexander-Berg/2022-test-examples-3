package ru.yandex.direct.core.entity.mobileapp.service;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.mobileapp.model.MobileAppStoreType;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class TrackingUrlNormalizerServiceTest {

    private final TrackingUrlNormalizerService service = TrackingUrlNormalizerService.instance();

    private final String sourceUrl;
    private final MobileAppStoreType storeType;
    private final String expectedResult;

    @Parameterized.Parameters(name = "[{0}] {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList((Object[][]) new Object[][]{
                {
                        MobileAppStoreType.APPLEAPPSTORE,
                        "http://app.appsflyer.com/id927992143?"
                                + "pid=yandexdirect_int&"
                                + "c=imcat_vip_s_cat_ugg_msk&"
                                + "clickid={LOGID}&"
                                + "ios_ifa={IOSIFA}&"
                                + "yclid=951090815092241&"
                                + "utm_medium=cpc&"
                                + "utm_source=imediaya_cpc&"
                                + "utm_campaign=imcat_vip_s_cat_ugg_msk&"
                                + "utm_content=|c:{campaign_id}|g:{gbid}|b:{banner_id}|k:{phrase_id}|st:{source_type}|a:{addphrases}|s:{source}|t:{position_type}|p:{position}|r:{retargeting_id}",
                        "https://app.appsflyer.com/id927992143?"
                                + "pid=yandexdirect_int&"
                                + "c={campaign_name_lat}&"
                                + "clickid={logid}&"
                                + "ios_ifa={ios_ifa}&"
                                + "utm_medium=cpc&"
                                + "utm_source=imediaya_cpc&"
                                + "utm_campaign={campaign_name_lat}&"
                                + "utm_content=|c:{campaign_id}|g:{gbid}|b:{banner_id}|k:{phrase_id}|st:{source_type}|a:{addphrases}|s:{source}|t:{position_type}|p:{position}|r:{retargeting_id}&"
                                + "idfa={ios_ifa}&"
                                + "af_c_id={campaign_id}&"
                                + "af_adset_id={gbid}&"
                                + "af_ad_id={ad_id}&"
                                + "af_keywords={phrase_id}{retargeting_id}_{keyword}{adtarget_name}&"
                                + "af_siteid={source_type}_{source}"
                },
                {
                        MobileAppStoreType.GOOGLEPLAYSTORE,
                        "https://redirect.appmetrica.yandex.com/serve/601443137595974897?"
                                + "click_id={LOGID}&"
                                + "google_aid={GOOGLEAID}&"
                                + "android_id={ANDROIDID}&"
                                + "android_id_sha1={ANDROID_ID_LC_SH1}&"
                                + "search_term={keyword}&"
                                + "google_aid_sha1={GOOGLE_AID_LC_SH1}&"
                                + "campaign_id={campaign_id}&"
                                + "device_type={device_type}&"
                                + "region_name={region_name}&"
                                + "source_type={source_type}&"
                                + "source={source}&"
                                + "position_type={position_type}&"
                                + "phrase_id={phrase_id}&"
                                + "utm_source=yadirect&"
                                + "utm_medium=cpc&"
                                + "utm_campaign=meta-poiskoviki-keywords-android-tabl_app_rf_rsya&"
                                + "utm_content=640x100_a_3",
                        "https://redirect.appmetrica.yandex.com/serve/601443137595974897?"
                                + "click_id={logid}&"
                                + "google_aid={google_aid}&"
                                + "search_term={keyword}&"
                                + "campaign_id={campaign_id}&"
                                + "device_type={device_type}&"
                                + "region_name={region_name}&"
                                + "source_type={source_type}&"
                                + "source={source}&"
                                + "position_type={position_type}&"
                                + "phrase_id={phrase_id}&"
                                + "utm_source=yadirect&"
                                + "utm_medium=cpc&"
                                + "utm_campaign={campaign_name_lat}&"
                                + "c={campaign_id}_{campaign_name_lat}&"
                                + "adgroup_id={gbid}&"
                                + "creative_id={ad_id}&"
                                + "criteria={phrase_id}{retargeting_id}_{keyword}{adtarget_name}&"
                                + "site_id={source_type}_{source}"
                },
                {
                        MobileAppStoreType.GOOGLEPLAYSTORE,
                        "https://control.kochava.com/v1/cpi/click?"
                                + "charset=utf-8&"
                                + "keyword={keyword}&"
                                + "campaign_id=koavito-6i4a2q5fd6ce0710c20d2&"
                                + "network_id=1517&"
                                + "site_id=none-provided&"
                                + "device_id_type=adid&"
                                + "device_id={GOOGLE_AID_LC_SH1_HEX}&"
                                + "click_id={LOGID}&"
                                + "android_id={ANDROID_ID_LC_SH1_HEX}",
                        "https://control.kochava.com/v1/cpi/click?"
                                + "charset=utf-8&"
                                + "keyword={keyword}&"
                                + "campaign_id={campaign_name_lat}&"
                                + "network_id=1517&"
                                + "site_id={source_type}_{source}&"
                                + "device_id_type=adid&"
                                + "click_id={logid}&"
                                + "adid={google_aid}&"
                                + "device_id={google_aid}&"
                                + "creative_id={ad_id}_{phrase_id}{retargeting_id}_{keyword}{adtarget_name}"
                },
                {
                        MobileAppStoreType.GOOGLEPLAYSTORE,
                        "https://redirect.appmetrica.yandex.com/serve/24952567347889819?"
                                + "my_adgroup={GBID}&"
                                + "device_type={device_type}&"
                                + "source_type={source_type}&"
                                + "source={source}&"
                                + "my_ad={ad_id}&"
                                + "click_id={LOGID}&"
                                + "search_term={keyword}&"
                                + "region_name={region_name}&"
                                + "phrase_id={phrase_id}&"
                                + "android_id={ANDROIDID}&"
                                + "position_type={position_type}&"
                                + "campaign_id={campaign_id}",
                        "https://redirect.appmetrica.yandex.com/serve/24952567347889819?"
                                + "my_adgroup={gbid}&"
                                + "device_type={device_type}&"
                                + "source_type={source_type}&"
                                + "source={source}&"
                                + "my_ad={ad_id}&"
                                + "click_id={logid}&"
                                + "search_term={keyword}&"
                                + "region_name={region_name}&"
                                + "phrase_id={phrase_id}&"
                                + "position_type={position_type}&"
                                + "campaign_id={campaign_id}&"
                                + "google_aid={google_aid}&"
                                + "c={campaign_id}_{campaign_name_lat}&"
                                + "adgroup_id={gbid}&"
                                + "creative_id={ad_id}&"
                                + "criteria={phrase_id}{retargeting_id}_{keyword}{adtarget_name}&"
                                + "site_id={source_type}_{source}"
                },
                {
                        MobileAppStoreType.APPLEAPPSTORE,
                        "https://100700.measurementapi.com/serve?"
                                + "action=click&"
                                + "publisher_id=100700&"
                                + "site_id=50360&"
                                + "publisher_ref_id={LOGID}&"
                                + "ios_ifa_sha1={IDFA_LC_SH1}&"
                                + "ios_ifa=5db86b29-024b-461f-ae71-9dec88898dcd&url",
                        "https://100700.measurementapi.com/serve?"
                                + "action=click&"
                                + "publisher_id=100700&"
                                + "site_id=50360&"
                                + "publisher_ref_id={logid}&"
                                + "ios_ifa={ios_ifa}&"
                                + "sub_campaign={campaign_id}_{campaign_name}&"
                                + "sub_adgroup={gbid}&"
                                + "sub_ad={ad_id}&"
                                + "sub_keyword={phrase_id}{retargeting_id}_{keyword}{adtarget_name}"
                }
        });
    }

    public TrackingUrlNormalizerServiceTest(MobileAppStoreType storeType, String sourceUrl, String expectedResult) {
        this.sourceUrl = sourceUrl;
        this.storeType = storeType;
        this.expectedResult = expectedResult;
    }

    @Test
    public void normalizeTrackingUrl() {
        assertThat(service.normalizeTrackingUrl(sourceUrl, storeType)).isEqualTo(expectedResult);
    }
}
