package ru.yandex.direct.core.entity.internalads.service;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.utils.UrlUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class InternalAdUrlMacrosServiceTest {

    static Collection<Object[]> params() {
        return asList(new Object[][]{
                {
                        "appsflyer all defaults added",
                        "http://app.appsflyer.com",
                        "http://app.appsflyer.com/?clickid={LOGID}&google_aid={GOOGLEAID}&android_id={ANDROIDID" +
                                "}&ios_ifa={IOSIFA}"
                },
                {
                        "appsflyer partial",
                        "http://app.appsflyer.com/?clickid={LOGID}",
                        "http://app.appsflyer.com/?clickid={LOGID}&google_aid={GOOGLEAID}&android_id={ANDROIDID" +
                                "}&ios_ifa={IOSIFA}"},
                {
                        "appsflyer do not change the existing ones",
                        "http://app.appsflyer.com/?thmth={SOMETHING}",
                        "http://app.appsflyer.com/?thmth={SOMETHING}&clickid={LOGID}&google_aid={GOOGLEAID" +
                                "}&android_id={ANDROIDID}&ios_ifa={IOSIFA}"
                },
                {
                        "appmetrica replace to default",
                        "https://redirect.appmetrica.yandex.com/?click_id={THMTH}",
                        "https://redirect.appmetrica.yandex.com/?click_id={LOGID}&google_aid={GOOGLE_AID_LC}&ios_ifa" +
                                "={IDFA_UC}"
                },
                {
                        "appmetrica do not change the existing ones",
                        "https://redirect.appmetrica.yandex.com/?thmth={SOMETHING}",
                        "https://redirect.appmetrica.yandex" +
                                ".com/?thmth={SOMETHING}&click_id={LOGID}&google_aid={GOOGLE_AID_LC}&ios_ifa={IDFA_UC}"
                },
                {
                        "appmetrica all defaults added",
                        "https://redirect.appmetrica.yandex.com",
                        "https://redirect.appmetrica.yandex.com/?click_id={LOGID}&google_aid={GOOGLE_AID_LC}&ios_ifa" +
                                "={IDFA_UC}"
                },
                {
                        "adjust all defaults added",
                        "https://app.adjust.com",
                        "https://app.adjust.com/?google_aid_lc_sh1={GOOGLE_AID_LC_SH1}&android_id_lc_sh1" +
                                "={ANDROID_ID_LC_SH1}&idfa={idfa}&ua={user_agent}&package_name={app_name}&ip" +
                                "={ip_address}&adjust-adid={adid}&android-id-md5={android_id_md5}&mac-address-sha1" +
                                "={mac_sha1}&mac-address-md5={mac_md5}&idfa-android-id={idfa||android_id}&idfa-gps" +
                                "-adid={idfa||gps_adid}&ios-idfa-base64md5={idfa_md5}&ios-idfa-hexmd5={idfa_md5_hex" +
                                "}&idfv={idfv}&user-activity-type={activity_kind}&click-timestamp={click_time" +
                                "}&conversion-timestamp={installed_at}&connection-type={connection_type}&isp={isp" +
                                "}&region-code={region}&country-code={country}&country-subdivision" +
                                "={country_subdivision}&city={city}&postal-code={postal_code}&device-model" +
                                "={device_name}&device-type={device_type}&os={os_name}&api-level={api_level}&device" +
                                "-sdk-version={sdk_version}&os-version={os_version}&environment={environment}&device" +
                                "-timezone={timezone}&install_callback=http://postback.yandexadexchange" +
                                ".net/postback?reqid={LOGID}"
                },
                {
                        "adjust do not change the existing ones",
                        "https://app.adjust.com/?mcrs={123}",
                        "https://app.adjust.com/?mcrs={123}&google_aid_lc_sh1={GOOGLE_AID_LC_SH1}&android_id_lc_sh1" +
                                "={ANDROID_ID_LC_SH1}&idfa={idfa}&ua={user_agent}&package_name={app_name}&ip" +
                                "={ip_address}&adjust-adid={adid}&android-id-md5={android_id_md5}&mac-address-sha1" +
                                "={mac_sha1}&mac-address-md5={mac_md5}&idfa-android-id={idfa||android_id}&idfa-gps" +
                                "-adid={idfa||gps_adid}&ios-idfa-base64md5={idfa_md5}&ios-idfa-hexmd5={idfa_md5_hex" +
                                "}&idfv={idfv}&user-activity-type={activity_kind}&click-timestamp={click_time" +
                                "}&conversion-timestamp={installed_at}&connection-type={connection_type}&isp={isp" +
                                "}&region-code={region}&country-code={country}&country-subdivision" +
                                "={country_subdivision}&city={city}&postal-code={postal_code}&device-model" +
                                "={device_name}&device-type={device_type}&os={os_name}&api-level={api_level}&device" +
                                "-sdk-version={sdk_version}&os-version={os_version}&environment={environment}&device" +
                                "-timezone={timezone}&install_callback=http://postback.yandexadexchange" +
                                ".net/postback?reqid={LOGID}"
                }
        });
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "params")
    public void normalizeTrackingUrl(String name, String sourceUrl, String expected) {
        String actual = InternalAdUrlMacrosService.normalizeTrackingUrl(sourceUrl);
        List<Pair<String, String>> actualUrlParts = nvl(UrlUtils.laxParseUrl(actual).getParameters(), emptyList());
        List<Pair<String, String>> expectedUrlParts = nvl(UrlUtils.laxParseUrl(expected).getParameters(), emptyList());

        var actualMap = listToMap(actualUrlParts, Pair::getKey, Pair::getValue);
        var expectedMap = listToMap(expectedUrlParts, Pair::getKey, Pair::getValue);
        assertThat(actualMap.size() == expectedMap.size() &&
                actualMap.entrySet().stream()
                        .allMatch(e -> e.getValue().equals(expectedMap.get(e.getKey())))).isTrue();
    }
}
