#!/usr/bin/perl

use Direct::Modern;

use Test::More tests => 47;

use BS::ExportMobileContent ();
use Settings;

my $href = "https://redirect.appmetrica.yandex.com?q={Google_aid}";
my $altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'https://redirect.appmetrica.yandex.com?q={Google_aid}&google_aid={google_aid}&click_id={trackid}', 'trackid was added and google_aid was not');

$href = 'https://redirect.appmetrica.yandex.com?q={Google_aid}&click_id={logid}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'https://redirect.appmetrica.yandex.com?q={Google_aid}&google_aid={google_aid}&click_id={trackid}', 'logid changed to trackid');

$href = 'https://redirect.appmetrica.yandex.com?q={Google_aid}&click_id={LOGID}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'https://redirect.appmetrica.yandex.com?q={Google_aid}&google_aid={google_aid}&click_id={trackid}', 'LOGID in upper case changed to trackid');

$href = 'https://redirect.appmetrica.yandex.com';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'https://redirect.appmetrica.yandex.com?google_aid={google_aid}&click_id={trackid}', 'click_id and trackid were added');

$href = 'https://redirect.appmetrica.yandex.com';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 1);
cmp_ok($altered_href, "eq", 'https://redirect.appmetrica.yandex.com?google_aid={google_aid}&click_id={trackid}&click_timestamp={current_unixtime}&device_ip={client_ip}&device_ua={user_agent}&noredirect=1', 'click_id, trackid and s2s params were added');

$href = 'http://hastrk3.com/pub_c?adgroup_id=3601';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'http://hastrk3.com/pub_c?adgroup_id=3601&google_aid={google_aid}&publisher_ref_id={trackid}');

$href = 'http://62218.api-02.com/serve?action=click&publisher_id=62218&site_id=50802&offer_id=309354';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'http://62218.api-02.com/serve?action=click&publisher_id=62218&site_id=50802&offer_id=309354&google_aid={google_aid}&publisher_ref_id={trackid}');

$href = 'http://62218kapi-02.com/serve?action=click&publisher_id=62218&site_id=50802&offer_id=309354';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'http://62218kapi-02.com/serve?action=click&publisher_id=62218&site_id=50802&offer_id=309354');

$href = 'http://app.adj.st/z5zbnp';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'http://app.adj.st/z5zbnp?adj_gps_adid={google_aid}&adj_oaid={oaid}&adj_ya_click_id={trackid}');

$href = 'http://app.adj.st/z5zbnp';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 0);
cmp_ok($altered_href, 'eq', 'http://app.adj.st/z5zbnp?adj_idfa={ios_ifa}&adj_ya_click_id={trackid}');

$href = 'http://app.adj.st/z5zbnp?adjust_gps_adid={google_aid}&adj_ya_click_id={logid}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'http://app.adj.st/z5zbnp?adjust_gps_adid={google_aid}&adj_gps_adid={google_aid}&adj_oaid={oaid}&adj_ya_click_id={trackid}');

$href = 'http://app.adj.st/z5zbnp?adjust_idfa={ios_ifa}&adj_ya_click_id={logid}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 0);
cmp_ok($altered_href, 'eq', 'http://app.adj.st/z5zbnp?adjust_idfa={ios_ifa}&adj_idfa={ios_ifa}&adj_ya_click_id={trackid}');

$href = 'http://app.adj.st/z5zbnp';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 1);
cmp_ok($altered_href, 'eq', 'http://app.adj.st/z5zbnp?adj_gps_adid={google_aid}&adj_oaid={oaid}&adj_ya_click_id={trackid}');

$href = 'http://app.adj.st/z5zbnp';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 1);
cmp_ok($altered_href, 'eq', 'http://app.adj.st/z5zbnp?adj_idfa={ios_ifa}&adj_ya_click_id={trackid}');

$href = 'http://app.adj.st/z5zbnp?adj_ya_click_id={logid}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'http://app.adj.st/z5zbnp?adj_gps_adid={google_aid}&adj_oaid={oaid}&adj_ya_click_id={trackid}');

$href = 'http://app.adj.st/z5zbnp?adj_ya_click_id=123&adj_ya_click_id=456';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'http://app.adj.st/z5zbnp?adj_gps_adid={google_aid}&adj_oaid={oaid}&adj_ya_click_id={trackid}');

$href = 'http://view.adjust.com/impression/z5zbnp';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 2);
cmp_ok($altered_href, 'eq', 'http://view.adjust.com/impression/z5zbnp?gps_adid={google_aid}&oaid={oaid}&ya_click_id={trackid}&ip_address={client_ip}&language={device_lang}&user_agent={user_agent}');

$href = 'http://view.adjust.com/impression/z5zbnp';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 2);
cmp_ok($altered_href, 'eq', 'http://view.adjust.com/impression/z5zbnp?idfa={ios_ifa}&ya_click_id={trackid}&ip_address={client_ip}&language={device_lang}&user_agent={user_agent}');

$href = 'http://view.adjust.com/impression/z5zbnp?ya_click_id={logid}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 2);
cmp_ok($altered_href, 'eq', 'http://view.adjust.com/impression/z5zbnp?gps_adid={google_aid}&oaid={oaid}&ya_click_id={trackid}&ip_address={client_ip}&language={device_lang}&user_agent={user_agent}');

$href = 'http://app.adjust.com/z5zbnp?gps_adid={google_aid}&oaid={oaid}&install_callback=http%3A%2F%2Fpostback.yandexadexchange.net%2Fpostback%3Fwhatever';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'http://app.adjust.com/z5zbnp?gps_adid={google_aid}&oaid={oaid}&ya_click_id={trackid}');

$href = 'http://app.adjust.com/z5zbnp?gps_adid={google_aid}&oaid={oaid}&conversion_callback=http%3A%2F%2Fpostback.yandexadexchange.net%2Fpostback%3Fwhatever';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'http://app.adjust.com/z5zbnp?gps_adid={google_aid}&oaid={oaid}&ya_click_id={trackid}');

$href = 'http://app.adjust.com/z5zbnp?gps_adid={google_aid}&oaid={oaid}&install_callback=https%3A%2F%2Fexample.com%3Fwhatever';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'http://app.adjust.com/z5zbnp?install_callback=https%3A%2F%2Fexample.com%3Fwhatever&gps_adid={google_aid}&oaid={oaid}&ya_click_id={trackid}');

$href = 'http://app.adjust.com/z5zbnp?gps_adid={google_aid}&oaid={oaid}&conversion_callback=https%3A%2F%2Fexample.com%3Fwhatever';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'http://app.adjust.com/z5zbnp?conversion_callback=https%3A%2F%2Fexample.com%3Fwhatever&gps_adid={google_aid}&oaid={oaid}&ya_click_id={trackid}');

$href = 'https://app.appsflyer.com/ru.auto.ara';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 0);
cmp_ok($altered_href, 'eq', 'https://app.appsflyer.com/ru.auto.ara?idfa={ios_ifa}&af_c_id={campaign_id}&clickid={trackid}&pid=yandexdirect_int&c={campaign_name}');

$href = 'https://app.appsflyer.com/ru.auto.ara';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'https://app.appsflyer.com/ru.auto.ara?advertising_id={google_aid}&oaid={oaid}&af_c_id={campaign_id}&clickid={trackid}&pid=yandexdirect_int&c={campaign_name}');

$href = 'https://app.appsflyer.com/ru.auto.ara';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 1);
cmp_ok($altered_href, 'eq', 'https://app.appsflyer.com/ru.auto.ara?idfa={ios_ifa}&af_c_id={campaign_id}&clickid={trackid}&pid=yandexdirect_int&af_ip={client_ip}&af_lang={device_lang}&af_ua={user_agent}&redirect=false&c={campaign_name}');

$href = 'https://app.appsflyer.com/ru.auto.ara';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 1);
cmp_ok($altered_href, 'eq', 'https://app.appsflyer.com/ru.auto.ara?advertising_id={google_aid}&oaid={oaid}&af_c_id={campaign_id}&clickid={trackid}&pid=yandexdirect_int&af_ip={client_ip}&af_lang={device_lang}&af_ua={user_agent}&redirect=false&af_ref=YandexDirectInt_{TRACKID}&c={campaign_name}');

$href = 'https://app.appsflyer.com/ru.auto.ara?c=&pid=something';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 0);
cmp_ok($altered_href, 'eq', 'https://app.appsflyer.com/ru.auto.ara?idfa={ios_ifa}&af_c_id={campaign_id}&clickid={trackid}&pid=yandexdirect_int&c={campaign_name}');

$href = 'https://impression.appsflyer.com/ru.auto.ara?clickid={logid}&pid=something&idfa={ios_ifa}&custom_param_c=custom_value';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 2);
cmp_ok($altered_href, 'eq', 'https://impression.appsflyer.com/ru.auto.ara?custom_param_c=custom_value&idfa={ios_ifa}&af_c_id={campaign_id}&clickid={trackid}&pid=yandexdirect_int&af_ip={client_ip}&af_lang={device_lang}&af_ua={user_agent}&c={campaign_name}');

$href = 'https://impression.appsflyer.com/ru.auto.ara?clickid={logid}&idfa={ios_ifa}&c=whatever&pid=yandexdirect_int';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 2);
cmp_ok($altered_href, 'eq', 'https://impression.appsflyer.com/ru.auto.ara?c=whatever&idfa={ios_ifa}&af_c_id={campaign_id}&clickid={trackid}&pid=yandexdirect_int&af_ip={client_ip}&af_lang={device_lang}&af_ua={user_agent}');

$href = 'https://control.kochava.com/v1/cpi/click?campaign_id=qwerty12';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 0);
cmp_ok($altered_href, 'eq', 'https://control.kochava.com/v1/cpi/click?campaign_id=qwerty12&ios_idfa={ios_ifa}');

$href = 'https://control.kochava.com/v1/cpi/click?campaign_id=qwerty12&clickid={logid}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'https://control.kochava.com/v1/cpi/click?campaign_id=qwerty12&clickid={trackid}&adid={google_aid}&android_id={android_id}');

$href = 'https://ad.apps.fm?i=123456789&l=en';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 0);
cmp_ok($altered_href, 'eq', 'https://ad.apps.fm?i=123456789&l=en&ios_idfa={ios_ifa}&click_id={trackid}');

$href = 'https://app.flurry.com?i=123456789&l=en&click_id={logid}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'https://app.flurry.com?i=123456789&l=en&adid={google_aid}&click_id={trackid}');

$href = 'https://trk.mail.ru/c/qwerty1234';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 0);
cmp_ok($altered_href, 'eq', 'https://trk.mail.ru/c/qwerty1234?mt_idfa={ios_ifa}&clickId={trackid}&regid={trackid}');

$href = 'https://trk.mail.ru/c/qwerty1234?logid={logid}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'https://trk.mail.ru/c/qwerty1234?logid={trackid}&mt_gaid={google_aid}&clickId={trackid}&regid={trackid}');

$href = 'https://trk.mail.ru/c/qwerty1234';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 1);
cmp_ok($altered_href, 'eq', 'https://trk.mail.ru/c/qwerty1234?mt_idfa={ios_ifa}&clickId={trackid}&regid={trackid}');

$href = 'https://trk.mail.ru/c/qwerty1234?clickId={logid}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 1);
cmp_ok($altered_href, 'eq', 'https://trk.mail.ru/c/qwerty1234?mt_gaid={google_aid}&clickId={trackid}&regid={trackid}');

$href = 'https://trk.mail.ru/c/qwerty1234?mt_idfa={$idfa}&regid={logid}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 0);
cmp_ok($altered_href, 'eq', 'https://trk.mail.ru/c/qwerty1234?mt_idfa={ios_ifa}&clickId={trackid}&regid={trackid}');

$href = 'https://some.app.link/qwerty1234';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 0);
cmp_ok($altered_href, 'eq', 'https://some.app.link/qwerty1234?%24idfa={ios_ifa}&~click_id={trackid}&%243p=a_yandex_direct');

$href = 'https://some.app.link/qwerty1234?%243p=foo';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'https://some.app.link/qwerty1234?%24aaid={google_aid}&~click_id={trackid}&%243p=a_yandex_direct');

$href = 'https://some.app.link/qwerty1234?%243p=a_yandex_direct&~click_id={logid}&%24idfa={idfa}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 0);
cmp_ok($altered_href, 'eq', 'https://some.app.link/qwerty1234?%24idfa={ios_ifa}&~click_id={trackid}&%243p=a_yandex_direct');

$href = 'https://absolutely-unknown-tracker.com/id123456789';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 0);
cmp_ok($altered_href, 'eq', 'https://absolutely-unknown-tracker.com/id123456789');

$href = 'https://absolutely-unknown-tracker.com/id123456789?clickid={logid}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'https://absolutely-unknown-tracker.com/id123456789?clickid={trackid}');

$href = 'https://some.sng.link/qwe/rty';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'iOS', 0);
cmp_ok($altered_href, 'eq', 'https://some.sng.link/qwe/rty?idfa={ios_ifa}&cl={trackid}');

$href = 'https://some.sng.link/qwe/rty';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'https://some.sng.link/qwe/rty?aifa={google_aid}&andi={android_id}&oaid={oaid}&cl={trackid}');

$href = 'https://some.sng.link/qwe/rty?andi={ANDROID_ID_LC}&aifa={GOOGLE_AID_LC}&pssn={SRC}&cl={LOGID}&oaid={OAID_LC}';
$altered_href = BS::ExportMobileContent::add_params_to_tracking_href($href, 'Android', 0);
cmp_ok($altered_href, 'eq', 'https://some.sng.link/qwe/rty?pssn={SRC}&aifa={google_aid}&andi={android_id}&oaid={oaid}&cl={trackid}');
