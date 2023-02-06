/* syntax version 1 */
$script = @@from yamarec1.udfs import *@@;
$parse_market_access_log_record = CustomPython::parse_market_access_log_record($script);

SELECT 
    $parse_market_access_log_record(
        AsDict(
            AsTuple("timestamp", "2017-10-02T20:01:17"),
            AsTuple("timezone", "+0300"),
            AsTuple("vhost", "m.market.yandex.ru"),
            AsTuple("yandexuid", "2612626131506438713"),
            AsTuple("puid", CAST(NULL AS String)),
            AsTuple("request", "/product/9361066"),
            AsTuple("req_id", "3a78353ec4193654888b6d58d207ac9a"),
            AsTuple("parent_reqid_seq", "3a78353ec4193654888b6d58d207ac9a"),
            AsTuple("referer", "https://www.google.ru/")
        )
    )
;
